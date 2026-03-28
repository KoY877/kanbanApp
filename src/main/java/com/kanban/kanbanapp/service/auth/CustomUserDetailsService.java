package com.kanban.kanbanapp.service.auth;

import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * CustomUserDetailsService implements Spring Security's UserDetailsService
 * to load user-specific data for authentication.
 * 
 * Loads users by email and returns Spring Security UserDetails.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by email address.
     * 
     * @param email the email address (used as username in this implementation)
     * @return UserDetails object containing user authentication information
     * @throws UsernameNotFoundException if user with given email is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        // Utiliser le rôle du user au lieu de hardcoder
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }
}