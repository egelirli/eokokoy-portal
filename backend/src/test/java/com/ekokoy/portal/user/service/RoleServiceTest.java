package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.AssignRoleRequest;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.RoleRepository;
import com.ekokoy.portal.user.repository.UserRepository;
import com.ekokoy.portal.user.repository.UserRoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private RoleService roleService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UUID superAdminRoleId = UUID.randomUUID();
    private final UUID evSahibiRoleId = UUID.randomUUID();

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

    @SuppressWarnings("unchecked")
    private void stubAuthoritiesAs(String... authorityNames) {
        Collection<GrantedAuthority> auths = new ArrayList<>();
        for (String a : authorityNames) {
            auths.add(new SimpleGrantedAuthority(a));
        }
        doReturn(auths).when(authentication).getAuthorities();
    }

    private User makeUser(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setStatus(UserStatus.active);
        return u;
    }

    private Role makeRole(UUID id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        r.setDisplayName(code);
        r.setActive(true);
        r.setPermissions(new HashSet<>());
        return r;
    }

    @Test
    void should_assign_role_when_valid_request() {
        // EV_SAHIBI atama → getAuthorities çağrılmaz (SUPER_ADMIN kontrolü bypass edilir)
        User actor = makeUser(actorId, "actor@ekokoy.com");
        User target = makeUser(targetId, "target@ekokoy.com");
        Role evSahibi = makeRole(evSahibiRoleId, "EV_SAHIBI");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(evSahibiRoleId)).thenReturn(Optional.of(evSahibi));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRoleRepository.findAllByUserId(targetId)).thenReturn(List.of());
        when(userRoleRepository.existsByUserIdAndRoleId(targetId, evSahibiRoleId)).thenReturn(false);
        when(userRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = roleService.assignRole(targetId, new AssignRoleRequest(evSahibiRoleId));

        assertThat(result).isNotNull();
        assertThat(result.roleCode()).isEqualTo("EV_SAHIBI");
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void should_throw_when_assigning_super_admin_as_non_super_admin() {
        // SUPER_ADMIN atama → isCurrentUserSuperAdmin() çağrılır
        User target = makeUser(targetId, "target@ekokoy.com");
        Role superAdmin = makeRole(superAdminRoleId, "SUPER_ADMIN");

        stubAuthoritiesAs("ROLE_YONETIM_KURULU");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(superAdminRoleId)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(makeUser(actorId, "actor@ekokoy.com")));

        assertThatThrownBy(() -> roleService.assignRole(targetId, new AssignRoleRequest(superAdminRoleId)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("SUPER_ADMIN rolünü sadece mevcut SUPER_ADMIN atayabilir");
    }

    @Test
    void should_throw_when_removing_last_role() {
        // EV_SAHIBI kaldırma → getAuthorities çağrılmaz
        User target = makeUser(targetId, "target@ekokoy.com");
        Role evSahibi = makeRole(evSahibiRoleId, "EV_SAHIBI");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(evSahibiRoleId)).thenReturn(Optional.of(evSahibi));
        when(userRoleRepository.existsByUserIdAndRoleId(targetId, evSahibiRoleId)).thenReturn(true);
        when(userRoleRepository.countByUserId(targetId)).thenReturn(1L);

        assertThatThrownBy(() -> roleService.removeRole(targetId, evSahibiRoleId))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("son rolü kaldırılamaz");
    }

    @Test
    void should_throw_when_assigning_super_admin_to_user_with_existing_roles() {
        // SUPER_ADMIN atama → isCurrentUserSuperAdmin() çağrılır (geçer), sonra count kontrolü
        User actor = makeUser(actorId, "actor@ekokoy.com");
        User target = makeUser(targetId, "target@ekokoy.com");
        Role superAdmin = makeRole(superAdminRoleId, "SUPER_ADMIN");

        stubAuthoritiesAs("ROLE_SUPER_ADMIN");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(superAdminRoleId)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRoleRepository.countByUserId(targetId)).thenReturn(1L);

        assertThatThrownBy(() -> roleService.assignRole(targetId, new AssignRoleRequest(superAdminRoleId)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("SUPER_ADMIN rolü başka rollerle birleştirilemez");
    }

    @Test
    void should_throw_when_assigning_role_to_super_admin_user() {
        // EV_SAHIBI atama → getAuthorities çağrılmaz, ama target SUPER_ADMIN olduğu için hata
        User actor = makeUser(actorId, "actor@ekokoy.com");
        User target = makeUser(targetId, "target@ekokoy.com");
        Role evSahibi = makeRole(evSahibiRoleId, "EV_SAHIBI");
        Role superAdmin = makeRole(superAdminRoleId, "SUPER_ADMIN");
        UserRole superAdminRole = new UserRole(target, superAdmin, null);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(evSahibiRoleId)).thenReturn(Optional.of(evSahibi));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRoleRepository.findAllByUserId(targetId)).thenReturn(List.of(superAdminRole));

        assertThatThrownBy(() -> roleService.assignRole(targetId, new AssignRoleRequest(evSahibiRoleId)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("SUPER_ADMIN kullanıcısına başka rol atanamaz");
    }

    @Test
    void should_throw_when_role_already_assigned() {
        // EV_SAHIBI atama → getAuthorities çağrılmaz
        User actor = makeUser(actorId, "actor@ekokoy.com");
        User target = makeUser(targetId, "target@ekokoy.com");
        Role evSahibi = makeRole(evSahibiRoleId, "EV_SAHIBI");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findById(evSahibiRoleId)).thenReturn(Optional.of(evSahibi));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRoleRepository.findAllByUserId(targetId)).thenReturn(List.of());
        when(userRoleRepository.existsByUserIdAndRoleId(targetId, evSahibiRoleId)).thenReturn(true);

        assertThatThrownBy(() -> roleService.assignRole(targetId, new AssignRoleRequest(evSahibiRoleId)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("zaten bu role sahip");
    }
}
