package com.example.cafe_manager_api;

import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@SpringBootApplication
public class CafeManagerApiApplication {
    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(CafeManagerApiApplication.class, args);
    }

    private static void loadDotEnv() {
        try {
            if (Files.exists(Paths.get(".env"))) {
                List<String> lines = Files.readAllLines(Paths.get(".env"));
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Remove surrounding quotes if any
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not read .env file: " + e.getMessage());
        }
    }

    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository, com.example.cafe_manager_api.repository.PromotionRepository promotionRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                long now = System.currentTimeMillis();

                // Seed admin
                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPasswordHash(hashSha256("admin123"));
                admin.setFullName("Quản trị viên");
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                admin.setCreatedAt(now);
                admin.setUpdatedAt(now);
                userRepository.save(admin);

                // Seed manager
                UserEntity manager = new UserEntity();
                manager.setUsername("manager");
                manager.setPasswordHash(hashSha256("manager123"));
                manager.setFullName("Quản lý Demo");
                manager.setRole("MANAGER");
                manager.setIsActive(true);
                manager.setCreatedAt(now);
                manager.setUpdatedAt(now);
                userRepository.save(manager);

                // Seed staff
                UserEntity staff = new UserEntity();
                staff.setUsername("staff");
                staff.setPasswordHash(hashSha256("123456"));
                staff.setFullName("Nhân viên Demo");
                staff.setRole("STAFF");
                staff.setIsActive(true);
                staff.setCreatedAt(now);
                staff.setUpdatedAt(now);
                userRepository.save(staff);

                System.out.println("Database seeded with default users (legacy SHA-256 hashes) successfully.");
            }

            if (promotionRepository.count() == 0) {
                com.example.cafe_manager_api.entity.PromotionEntity promo = new com.example.cafe_manager_api.entity.PromotionEntity();
                promo.setCode("CAFE10K");
                promo.setType("FIXED_AMOUNT");
                promo.setValue(10000.0);
                promo.setIsActive(true);
                promo.setExpiresAt(0L);
                promo.setCreatedAt(System.currentTimeMillis());
                promotionRepository.save(promo);
                System.out.println("Database seeded with test promotion CAFE10K successfully.");
            }
        };
    }

    private String hashSha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
