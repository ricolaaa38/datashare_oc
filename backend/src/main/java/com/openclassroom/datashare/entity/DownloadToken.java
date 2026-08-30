package com.openclassroom.datashare.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class DownloadToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @OneToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
