package com.paypulse.auth.service;

import com.paypulse.auth.entity.User;
import com.paypulse.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);
        
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("User not found: {}", username);
                        return new UsernameNotFoundException("User not found: " + username);
                    });
            
            var authorities = java.util.Arrays.asList(user.getRoles().split(","))
                    .stream()
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            log.debug("User details loaded successfully for username: {} with roles: {}", 
                    username, user.getRoles());
            
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    authorities
            );
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }
}