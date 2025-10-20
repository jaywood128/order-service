package com.streamcart.order.seeder;

import com.streamcart.order.entity.User;
import com.streamcart.order.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds demo users for testing.
 * Only runs in 'dev' profile to avoid creating demo users in production.
 * 
 * Order(2) ensures this runs after ProductDataSeeder.
 */
@Component
@Order(2)
@Profile("dev")  // Only run in development
@RequiredArgsConstructor
@Slf4j
public class DemoUserSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("👥 Seeding demo users (The Office Edition)...");
            seedDemoUsers();
            log.info("✅ Demo users seeded! {} users available for testing.", 
                    userRepository.count());
            log.info("🔑 Login with: mscott/worldsbestboss or dschrute/beetsfacts");
        } else {
            log.info("👥 Users already exist ({} users). Skipping demo user seed.", 
                    userRepository.count());
        }
    }
    
    private void seedDemoUsers() {
        List<User> users = List.of(
            createUser(
                "mscott",
                "michael.scott@dundermifflin.com",
                "worldsbestboss",
                "Michael",
                "Scott"
            ),
            createUser(
                "dschrute",
                "dwight.schrute@dundermifflin.com",
                "beetsfacts",
                "Dwight",
                "Schrute"
            ),
            createUser(
                "jhalpert",
                "jim.halpert@dundermifflin.com",
                "tuna4life",
                "Jim",
                "Halpert"
            ),
            createUser(
                "pbeesly",
                "pam.beesly@dundermifflin.com",
                "fineart2023",
                "Pam",
                "Beesly"
            )
        );
        
        userRepository.saveAll(users);
    }
    
    private User createUser(String username, String email, String password, 
                           String firstName, String lastName) {
        return User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))  // BCrypt encrypt
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}

