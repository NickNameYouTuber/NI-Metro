package com.nicorp.nimetro.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapListItemResponse {
    private UUID id;
    private String name;
    private String country;
    private String version;
    private String author;
    private String iconUrl;
    private String fileName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

