package com.openclassroom.datashare.repository;

import com.openclassroom.datashare.entity.FileTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {

    Optional<FileTag> findByUserIdAndNameIgnoreCase(Long userId, String name);

    Page<FileTag> findAllByUserIdOrderByNameAsc(Long userId, Pageable pageable);
}
