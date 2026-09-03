package com.openclassroom.datashare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TAG entity of the conceptual data model: a tag always belongs to exactly one
 * user, and the pair (userId, name) is unique.
 */
@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(name = "uk_tag_user_name", columnNames = { "user_id",
        "name" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTag {

    public static final int MAX_NAME_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    public FileTag(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}
