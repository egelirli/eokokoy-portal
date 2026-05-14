package com.ekokoy.portal.user.service;

import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserRole;
import com.ekokoy.portal.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        for (UserRole ur : user.getUserRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + ur.getRole().getCode()));
            ur.getRole().getPermissions()
                    .forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getCode())));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == com.ekokoy.portal.user.entity.UserStatus.suspended)
                .credentialsExpired(false)
                .disabled(user.getStatus() != com.ekokoy.portal.user.entity.UserStatus.active)
                .build();
    }
}
