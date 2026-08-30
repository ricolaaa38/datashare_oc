package com.openclassroom.datashare.repository;

import com.openclassroom.datashare.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {
    Page<File> findAllByOwnerId(Long ownerId, Pageable pageable);

    Optional<File> findByFileIdAndOwnerId(Long fileId, Long ownerId);
}
