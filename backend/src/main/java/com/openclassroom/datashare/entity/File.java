package com.openclassroom.datashare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "files")
@Data
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(nullable = true)
    private Long ownerId;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = true)
    private String passwordHash;

    @CreationTimestamp
    private OffsetDateTime createdAt;

}
