package com.nicorp.nimetro.api.repositories;

import com.nicorp.nimetro.api.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByApiKey(String apiKey);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByApiKey(String apiKey);
}

