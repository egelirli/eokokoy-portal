package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyUserRepository propertyUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           PropertyUserRepository propertyUserRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyUserRepository = propertyUserRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** Tüm konutları listeler. */
    public List<PropertyResponse> listProperties() {
        return propertyRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getNumber(), b.getNumber()))
                .map(PropertyResponse::from)
                .toList();
    }

    /** Belirtilen konuta ait detayları ve aktif sakinleri döner. */
    public PropertyDetailResponse getProperty(UUID propertyId) {
        Property property = findPropertyOrThrow(propertyId);
        List<ResidentResponse> residents = propertyUserRepository.findActiveByPropertyId(propertyId).stream()
                .map(ResidentResponse::from)
                .toList();
        return PropertyDetailResponse.from(property, residents);
    }

    /** Giriş yapmış kullanıcının aktif konut ilişkilerini döner. */
    public List<UserPropertyResponse> getMyProperties() {
        UUID userId = currentUserId();
        return propertyUserRepository.findActiveByUserId(userId).stream()
                .map(UserPropertyResponse::from)
                .toList();
    }

    /**
     * Konuta sakin ekler. İş kuralları:
     * - Bir konutta max 1 aktif kiraci
     * - Aile bireyi için aktif ev_sahibi şart
     * - ev_sahibi için ownership_percentage zorunlu, toplam 100 aşamaz
     */
    @Transactional
    public ResidentResponse addResident(UUID propertyId, AddResidentRequest request) {
        Property property = findPropertyOrThrow(propertyId);
        User user = findUserOrThrow(request.userId());
        User actor = findUserOrThrow(currentUserId());

        RelationType relationType = parseRelationType(request.relationType());

        switch (relationType) {
            case ev_sahibi -> validateEvSahibiAddition(propertyId, request.ownershipPercentage());
            case kiraci    -> validateKiraciAddition(propertyId);
            case aile_bireyi -> validateAileBireyiAddition(propertyId);
        }

        PropertyUser pu = new PropertyUser();
        pu.setProperty(property);
        pu.setUser(user);
        pu.setRelationType(relationType);
        pu.setOwnershipPercentage(request.ownershipPercentage());
        pu.setStartDate(request.startDate() != null ? request.startDate() : LocalDate.now());
        pu.setCreatedBy(actor);
        pu.setNotes(request.notes());

        propertyUserRepository.save(pu);

        assignRoleIfMissing(user, actor, roleCodeFor(relationType));
        updatePropertyStatus(property, propertyId);
        propertyRepository.save(property);

        return ResidentResponse.from(pu);
    }

    /** İlişkiyi sonlandırır. Başka aktif konut yoksa rol kaldırılır. */
    @Transactional
    public ResidentResponse endRelation(UUID propertyId, UUID relationId) {
        findPropertyOrThrow(propertyId);

        PropertyUser pu = propertyUserRepository.findById(relationId)
                .filter(r -> r.getProperty().getId().equals(propertyId))
                .orElseThrow(() -> new EkokoyException("RELATION_NOT_FOUND", "İlişki bulunamadı.", 404));

        if (pu.getEndDate() != null) {
            throw new EkokoyException("RELATION_ALREADY_ENDED", "İlişki zaten sonlandırılmış.", 422);
        }

        pu.setEndDate(LocalDate.now());
        propertyUserRepository.save(pu);

        removeRoleIfNoActiveRelations(pu.getUser(), pu.getRelationType());

        Property property = pu.getProperty();
        updatePropertyStatus(property, propertyId);
        propertyRepository.save(property);

        return ResidentResponse.from(pu);
    }

    /** Konuttaki tüm sakin geçmişini (aktif ve sonlanmış) döner. */
    public List<ResidentResponse> getPropertyHistory(UUID propertyId) {
        findPropertyOrThrow(propertyId);
        return propertyUserRepository.findAllByPropertyId(propertyId).stream()
                .map(ResidentResponse::from)
                .toList();
    }

    /** Bir kullanıcının tüm aktif konut ilişkilerini döner. */
    public List<UserPropertyResponse> getUserProperties(UUID userId) {
        findUserOrThrow(userId);
        return propertyUserRepository.findActiveByUserId(userId).stream()
                .map(UserPropertyResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------ //
    //  Validasyon yardımcıları
    // ------------------------------------------------------------------ //

    private void validateEvSahibiAddition(UUID propertyId, BigDecimal ownershipPercentage) {
        if (ownershipPercentage == null || ownershipPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EkokoyException(
                    "OWNERSHIP_PERCENTAGE_REQUIRED",
                    "Ev sahibi için mülkiyet yüzdesi zorunludur ve sıfırdan büyük olmalıdır.",
                    422
            );
        }
        BigDecimal current = propertyUserRepository.sumOwnershipPercentageByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi);
        if (current.add(ownershipPercentage).compareTo(new BigDecimal("100.00")) > 0) {
            throw new EkokoyException(
                    "OWNERSHIP_PERCENTAGE_EXCEEDED",
                    "Mülkiyet yüzdesi toplamı 100.00'ı aşamaz. Mevcut toplam: " + current,
                    422
            );
        }
    }

    private void validateKiraciAddition(UUID propertyId) {
        long activeKiraci = propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci);
        if (activeKiraci > 0) {
            throw new EkokoyException(
                    "ACTIVE_TENANT_EXISTS",
                    "Konutta zaten aktif bir kiracı bulunmaktadır.",
                    422
            );
        }
    }

    private void validateAileBireyiAddition(UUID propertyId) {
        long activeEvSahibi = propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi);
        if (activeEvSahibi == 0) {
            throw new EkokoyException(
                    "NO_ACTIVE_OWNER",
                    "Aile bireyi eklemek için konutta aktif bir ev sahibi bulunmalıdır.",
                    422
            );
        }
    }

    // ------------------------------------------------------------------ //
    //  Rol yönetimi
    // ------------------------------------------------------------------ //

    private void assignRoleIfMissing(User user, User actor, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new EkokoyException("ROLE_NOT_FOUND", "Rol bulunamadı: " + roleCode, 500));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(new UserRole(user, role, actor));
        }
    }

    private void removeRoleIfNoActiveRelations(User user, RelationType relationType) {
        long remaining = propertyUserRepository.countActiveByUserIdAndRelationType(user.getId(), relationType);
        if (remaining > 0) {
            return;
        }
        String roleCode = roleCodeFor(relationType);
        roleRepository.findByCode(roleCode).ifPresent(role -> {
            if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
                long totalRoles = userRoleRepository.countByUserId(user.getId());
                if (totalRoles > 1) {
                    userRoleRepository.deleteById(new com.ekokoy.portal.user.entity.UserRoleId(user.getId(), role.getId()));
                }
                // Son rol ise kaldırmıyoruz — sisteme erişim kaybolmasın
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Durum güncellemesi
    // ------------------------------------------------------------------ //

    private void updatePropertyStatus(Property property, UUID propertyId) {
        long kiraciCount = propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci);
        long evSahibiCount = propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi);

        if (kiraciCount > 0) {
            property.setStatus(PropertyStatus.kiralik);
        } else if (evSahibiCount > 0) {
            property.setStatus(PropertyStatus.sahipli);
        } else {
            property.setStatus(PropertyStatus.bos);
        }
    }

    // ------------------------------------------------------------------ //
    //  Yardımcı metotlar
    // ------------------------------------------------------------------ //

    private Property findPropertyOrThrow(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EkokoyException("PROPERTY_NOT_FOUND", "Konut bulunamadı.", 404));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new EkokoyException("USER_NOT_FOUND", "Kullanıcı bulunamadı.", 404));
    }

    private RelationType parseRelationType(String value) {
        try {
            return RelationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new EkokoyException(
                    "INVALID_RELATION_TYPE",
                    "Geçersiz ilişki türü: " + value + ". Geçerli değerler: ev_sahibi, kiraci, aile_bireyi",
                    422
            );
        }
    }

    private String roleCodeFor(RelationType relationType) {
        return switch (relationType) {
            case ev_sahibi   -> "EV_SAHIBI";
            case kiraci      -> "KIRACI";
            case aile_bireyi -> "AILE_BIREYI";
        };
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString((String) auth.getPrincipal());
    }
}
