package com.tinyspring.garderie.security;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldLoadUserDetailsWithRoleAuthority() {
        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        User user = new User("Parent Test", "parent@test.tn", "encoded-password", true, new Role(RoleName.PARENT));
        when(userRepository.findByEmailIgnoreCase("parent@test.tn")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("parent@test.tn");

        assertEquals("parent@test.tn", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertEquals("ROLE_PARENT", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        when(userRepository.findByEmailIgnoreCase("missing@test.tn")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@test.tn")
        );

        assertEquals("User not found", exception.getMessage());
    }
}
