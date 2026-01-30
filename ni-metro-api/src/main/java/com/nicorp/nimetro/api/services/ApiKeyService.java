package com.nicorp.nimetro.api.services;

import com.nicorp.nimetro.api.entities.UserEntity;
import com.nicorp.nimetro.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private static final String API_KEY_PREFIX = "nmi-";
    private static final int API_KEY_LENGTH = 32;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    public Optional<UserEntity> getUserByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByApiKey(apiKey);
    }
    
    @Transactional
    public void updateLastUsedAt(String apiKey) {
        userRepository.findByApiKey(apiKey).ifPresent(user -> {
            user.setLastUsedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    @Transactional
    public String generateApiKey(UUID userId) {
        return userRepository.findById(userId).map(user -> {
            String apiKey = generateUniqueApiKey();
            user.setApiKey(apiKey);
            userRepository.save(user);
            return apiKey;
        }).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
    
    @Transactional
    public String generateApiKeyForUser(String username) {
        return userRepository.findByUsername(username).map(user -> {
            String apiKey = generateUniqueApiKey();
            user.setApiKey(apiKey);
            userRepository.save(user);
            return apiKey;
        }).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
    
    private String generateUniqueApiKey() {
        String apiKey;
        do {
            StringBuilder sb = new StringBuilder(API_KEY_PREFIX);
            for (int i = 0; i < API_KEY_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
            }
            apiKey = sb.toString();
        } while (userRepository.existsByApiKey(apiKey));
        return apiKey;
    }
}

