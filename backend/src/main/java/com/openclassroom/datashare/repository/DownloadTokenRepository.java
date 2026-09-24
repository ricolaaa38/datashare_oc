package com.openclassroom.datashare.repository;

import com.openclassroom.datashare.entity.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing DownloadToken entities.
 * Provides methods to perform CRUD operations and custom queries on download tokens.
 */
@Repository
public interface DownloadTokenRepository extends JpaRepository<DownloadToken, Long> {
    Optional<DownloadToken> findByTokenHash(String tokenHash);
    Optional<DownloadToken> findByFile_FileId(Long fileId);
}
