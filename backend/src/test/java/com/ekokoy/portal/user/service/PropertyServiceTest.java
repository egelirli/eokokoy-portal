package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.AddResidentRequest;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PropertyUserRepository propertyUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private PropertyService propertyService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final UUID propertyId = UUID.randomUUID();
    private final UUID relationId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(actorId.toString());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User makeUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(id + "@ekokoy.com");
        u.setStatus(UserStatus.active);
        return u;
    }

    private Property makeProperty(UUID id) {
        Property p = new Property();
        p.setId(id);
        p.setNumber(1);
        p.setStatus(PropertyStatus.bos);
        return p;
    }

    private Role makeRole(String code) {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setDisplayName(code);
        return r;
    }

    // ------------------------------------------------------------------ //
    //  addResident — ev_sahibi
    // ------------------------------------------------------------------ //

    @Test
    void should_add_ev_sahibi_and_assign_role_when_valid() {
        User actor = makeUser(actorId);
        User target = makeUser(targetUserId);
        Property property = makeProperty(propertyId);
        Role evSahibiRole = makeRole("EV_SAHIBI");

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(propertyUserRepository.sumOwnershipPercentageByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(BigDecimal.ZERO);
        when(propertyUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci)).thenReturn(0L);
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(1L);
        when(roleRepository.findByCode("EV_SAHIBI")).thenReturn(Optional.of(evSahibiRole));
        when(userRoleRepository.existsByUserIdAndRoleId(targetUserId, evSahibiRole.getId())).thenReturn(false);
        when(propertyRepository.save(any())).thenReturn(property);

        var request = new AddResidentRequest(targetUserId, "ev_sahibi", new BigDecimal("50.00"), LocalDate.now(), null);
        var result = propertyService.addResident(propertyId, request);

        assertThat(result).isNotNull();
        assertThat(result.relationType()).isEqualTo("ev_sahibi");
        verify(userRoleRepository).save(any(UserRole.class));
        verify(propertyRepository).save(property);
    }

    @Test
    void should_throw_when_ownership_percentage_null_for_ev_sahibi() {
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(makeProperty(propertyId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(makeUser(targetUserId)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(makeUser(actorId)));

        var request = new AddResidentRequest(targetUserId, "ev_sahibi", null, LocalDate.now(), null);

        assertThatThrownBy(() -> propertyService.addResident(propertyId, request))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("mülkiyet yüzdesi zorunludur");
    }

    @Test
    void should_throw_when_ownership_percentage_exceeds_100() {
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(makeProperty(propertyId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(makeUser(targetUserId)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(makeUser(actorId)));
        when(propertyUserRepository.sumOwnershipPercentageByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(new BigDecimal("70.00"));

        var request = new AddResidentRequest(targetUserId, "ev_sahibi", new BigDecimal("40.00"), LocalDate.now(), null);

        assertThatThrownBy(() -> propertyService.addResident(propertyId, request))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("100.00'ı aşamaz");
    }

    // ------------------------------------------------------------------ //
    //  addResident — kiraci
    // ------------------------------------------------------------------ //

    @Test
    void should_throw_when_active_kiraci_exists() {
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(makeProperty(propertyId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(makeUser(targetUserId)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(makeUser(actorId)));
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci)).thenReturn(1L);

        var request = new AddResidentRequest(targetUserId, "kiraci", null, LocalDate.now(), null);

        assertThatThrownBy(() -> propertyService.addResident(propertyId, request))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("aktif bir kiracı bulunmaktadır");
    }

    // ------------------------------------------------------------------ //
    //  addResident — aile_bireyi
    // ------------------------------------------------------------------ //

    @Test
    void should_throw_when_no_active_ev_sahibi_for_aile_bireyi() {
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(makeProperty(propertyId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(makeUser(targetUserId)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(makeUser(actorId)));
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(0L);

        var request = new AddResidentRequest(targetUserId, "aile_bireyi", null, LocalDate.now(), null);

        assertThatThrownBy(() -> propertyService.addResident(propertyId, request))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("aktif bir ev sahibi bulunmalıdır");
    }

    // ------------------------------------------------------------------ //
    //  endRelation
    // ------------------------------------------------------------------ //

    @Test
    void should_end_relation_and_remove_role_when_no_other_active_relations() {
        User user = makeUser(targetUserId);
        Property property = makeProperty(propertyId);
        Role evSahibiRole = makeRole("EV_SAHIBI");

        PropertyUser pu = new PropertyUser();
        pu.setUser(user);
        pu.setProperty(property);
        pu.setRelationType(RelationType.ev_sahibi);
        pu.setStartDate(LocalDate.now().minusDays(10));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyUserRepository.findById(relationId)).thenReturn(Optional.of(pu));
        when(propertyUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyUserRepository.countActiveByUserIdAndRelationType(targetUserId, RelationType.ev_sahibi)).thenReturn(0L);
        when(roleRepository.findByCode("EV_SAHIBI")).thenReturn(Optional.of(evSahibiRole));
        when(userRoleRepository.existsByUserIdAndRoleId(targetUserId, evSahibiRole.getId())).thenReturn(true);
        when(userRoleRepository.countByUserId(targetUserId)).thenReturn(2L);
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci)).thenReturn(0L);
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(0L);
        when(propertyRepository.save(any())).thenReturn(property);

        var result = propertyService.endRelation(propertyId, relationId);

        assertThat(result.endDate()).isNotNull();
        verify(userRoleRepository).deleteById(any());
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.bos);
    }

    @Test
    void should_throw_when_relation_already_ended() {
        User user = makeUser(targetUserId);
        Property property = makeProperty(propertyId);

        PropertyUser pu = new PropertyUser();
        pu.setUser(user);
        pu.setProperty(property);
        pu.setRelationType(RelationType.kiraci);
        pu.setStartDate(LocalDate.now().minusDays(5));
        pu.setEndDate(LocalDate.now().minusDays(1));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyUserRepository.findById(relationId)).thenReturn(Optional.of(pu));

        assertThatThrownBy(() -> propertyService.endRelation(propertyId, relationId))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("zaten sonlandırılmış");
    }

    @Test
    void should_not_remove_role_when_other_active_relations_exist() {
        User user = makeUser(targetUserId);
        Property property = makeProperty(propertyId);

        PropertyUser pu = new PropertyUser();
        pu.setUser(user);
        pu.setProperty(property);
        pu.setRelationType(RelationType.ev_sahibi);
        pu.setStartDate(LocalDate.now().minusDays(5));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyUserRepository.findById(relationId)).thenReturn(Optional.of(pu));
        when(propertyUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // user still has another active ev_sahibi relation
        when(propertyUserRepository.countActiveByUserIdAndRelationType(targetUserId, RelationType.ev_sahibi)).thenReturn(1L);
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci)).thenReturn(0L);
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.ev_sahibi)).thenReturn(0L);
        when(propertyRepository.save(any())).thenReturn(property);

        propertyService.endRelation(propertyId, relationId);

        verify(userRoleRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------------------ //
    //  getMyProperties
    // ------------------------------------------------------------------ //

    @Test
    void should_return_my_properties_when_authenticated() {
        User user = makeUser(actorId);
        Property property = makeProperty(propertyId);

        PropertyUser pu = new PropertyUser();
        pu.setUser(user);
        pu.setProperty(property);
        pu.setRelationType(RelationType.ev_sahibi);
        pu.setStartDate(LocalDate.now().minusDays(30));
        pu.setOwnershipPercentage(new BigDecimal("100.00"));

        when(propertyUserRepository.findActiveByUserId(actorId)).thenReturn(List.of(pu));

        var result = propertyService.getMyProperties();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().propertyNumber()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ //
    //  Property status güncellemesi
    // ------------------------------------------------------------------ //

    @Test
    void should_set_status_kiralik_when_kiraci_added() {
        User actor = makeUser(actorId);
        User target = makeUser(targetUserId);
        Property property = makeProperty(propertyId);
        Role kiraciRole = makeRole("KIRACI");

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(propertyUserRepository.countActiveByPropertyIdAndRelationType(propertyId, RelationType.kiraci)).thenReturn(0L, 1L);
        when(propertyUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByCode("KIRACI")).thenReturn(Optional.of(kiraciRole));
        when(userRoleRepository.existsByUserIdAndRoleId(any(), any())).thenReturn(false);
        when(propertyRepository.save(any())).thenReturn(property);

        var request = new AddResidentRequest(targetUserId, "kiraci", null, LocalDate.now(), null);
        propertyService.addResident(propertyId, request);

        assertThat(property.getStatus()).isEqualTo(PropertyStatus.kiralik);
    }
}
