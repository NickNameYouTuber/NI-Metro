package com.nicorp.nimetro.api.scripts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicorp.nimetro.api.entities.MapEntity;
import com.nicorp.nimetro.api.repositories.MapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Script to import maps from JSON files.
 * Run with: java -jar app.jar --import.maps.enabled=true
 * Or set property: import.maps.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportMapsScript implements CommandLineRunner {
    
    private final MapRepository mapRepository;
    private final ObjectMapper objectMapper;
    
    // This should be configured via properties in production
    private static final String IMPORT_ENABLED_PROP = "import.maps.enabled";
    private static final String MAPS_DIRECTORY = "../app/src/main/assets/raw";
    
    @Override
    public void run(String... args) {
        String importEnabled = System.getProperty(IMPORT_ENABLED_PROP);
        if (!"true".equalsIgnoreCase(importEnabled)) {
            log.info("Map import is disabled. Set -Dimport.maps.enabled=true to enable.");
            return;
        }
        
        log.info("Starting map import from directory: {}", MAPS_DIRECTORY);
        
        try {
            Path mapsDir = Paths.get(MAPS_DIRECTORY);
            if (!Files.exists(mapsDir)) {
                log.warn("Maps directory does not exist: {}", mapsDir);
                return;
            }
            
            Files.list(mapsDir)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(path -> path.getFileName().toString().startsWith("metromap_"))
                .forEach(this::importMapFile);
            
            log.info("Map import completed.");
        } catch (Exception e) {
            log.error("Error during map import", e);
        }
    }
    
    private void importMapFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            log.info("Importing map from file: {}", fileName);
            
            // Check if already exists
            String baseFileName = fileName.replace(".json", "");
            if (mapRepository.existsByFileName(baseFileName)) {
                log.info("Map {} already exists, skipping", baseFileName);
                return;
            }
            
            // Read and parse JSON
            String jsonContent = Files.readString(filePath);
            Map<String, Object> mapData = objectMapper.readValue(jsonContent, Map.class);
            
            // Extract info
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) mapData.get("info");
            
            String name = info != null ? (String) info.getOrDefault("name", baseFileName) : baseFileName;
            String country = info != null ? (String) info.get("country") : null;
            String version = info != null ? (String) info.getOrDefault("version", "1.0") : "1.0";
            String author = info != null ? (String) info.get("author") : null;
            String iconUrl = info != null ? (String) info.get("icon") : null;
            
            // Create entity
            MapEntity mapEntity = MapEntity.builder()
                .name(name)
                .country(country)
                .version(version)
                .author(author)
                .iconUrl(iconUrl)
                .fileName(baseFileName)
                .data(mapData)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
            
            mapRepository.save(mapEntity);
            log.info("Successfully imported map: {} ({})", name, baseFileName);
            
        } catch (Exception e) {
            log.error("Error importing map from file: {}", filePath, e);
        }
    }
}

