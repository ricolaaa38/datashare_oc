package com.openclassroom.datashare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
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

    /**
     * FILE_TAG association of the conceptual data model. Excluded from
     * equals/hashCode/toString so that Lombok-generated methods never trigger
     * lazy loading.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "file_tag", joinColumns = @JoinColumn(name = "file_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<FileTag> tags = new LinkedHashSet<>();

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }
}
