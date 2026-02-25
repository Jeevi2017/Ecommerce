package com.example.Ecomm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PasswordGenerator.class);

    // Prevent instantiation
    private PasswordGenerator() {
    }

    public static void main(String[] args) {

        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            logger.error("No password provided. Usage: java PasswordGenerator <rawPassword>");
            return;
        }

        String rawPassword = args[0];

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(rawPassword);

        // 🔐 Do NOT log raw passwords in real systems
        logger.info("Password successfully encoded.");
        logger.info("Encoded Password: {}", encodedPassword);
    }
}
