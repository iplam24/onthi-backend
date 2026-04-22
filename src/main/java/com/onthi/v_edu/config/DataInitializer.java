package com.onthi.v_edu.config;

import com.onthi.v_edu.user.entity.Role;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.RoleRepository;
import com.onthi.v_edu.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        List.of("ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN").forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                System.out.println("Initialized role: " + roleName);
            }
        });

        if (userRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN was not initialized"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@v-edu.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(adminRole);
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);

            System.out.println("Initialized admin account: admin");
        }
    }
}
