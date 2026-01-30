package com.nicorp.nimetro.api.controllers;

import com.nicorp.nimetro.api.dto.ApiKeyResponse;
import com.nicorp.nimetro.api.entities.UserEntity;
import com.nicorp.nimetro.api.services.ApiKeyService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @PostMapping("/api-keys/generate")
    public ResponseEntity<ApiKeyResponse> generateApiKey(@RequestBody GenerateApiKeyRequest request) {
        String apiKey;
        if (request.getUserId() != null) {
            apiKey = apiKeyService.generateApiKey(UUID.fromString(request.getUserId()));
        } else if (request.getUsername() != null) {
            apiKey = apiKeyService.generateApiKeyForUser(request.getUsername());
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(new ApiKeyResponse(apiKey));
    }
    
    @GetMapping("/api-keys/my-key")
    public ResponseEntity<ApiKeyResponse> getMyApiKey(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserEntity user) {
            String apiKey = user.getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                return ResponseEntity.ok(new ApiKeyResponse(apiKey));
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    @Data
    @AllArgsConstructor
    public static class ApiKeyResponse {
        private String apiKey;
    }
    
    @Data
    public static class GenerateApiKeyRequest {
        private String userId;
        private String username;
    }
}

