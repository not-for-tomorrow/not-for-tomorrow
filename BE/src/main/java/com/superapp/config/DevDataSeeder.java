package com.superapp.config;

import com.superapp.auth.Role;
import com.superapp.auth.UserAccount;
import com.superapp.auth.UserAccountRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevDataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seed(
                userRepository,
                passwordEncoder,
                "admin@huyverse.dev",
                "Admin Demo",
                "Password123",
                set(Role.ADMIN, Role.EDITOR, Role.MEMBER),
                true
            );
            seed(
                userRepository,
                passwordEncoder,
                "editor@huyverse.dev",
                "Editor Demo",
                "Password123",
                set(Role.EDITOR, Role.MEMBER),
                false
            );
        };
    }

    private void seed(
        UserAccountRepository repo,
        PasswordEncoder encoder,
        String email,
        String displayName,
        String rawPassword,
        Set<Role> roles,
        boolean mfaEnabled
    ) {
        if (repo.existsByEmail(email)) {
            return;
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setRoles(roles);
        user.setMfaEnabled(mfaEnabled);
        repo.save(user);
    }

    private Set<Role> set(Role... roles) {
        Set<Role> values = new HashSet<Role>();
        for (Role role : roles) {
            values.add(role);
        }
        return values;
    }
}
