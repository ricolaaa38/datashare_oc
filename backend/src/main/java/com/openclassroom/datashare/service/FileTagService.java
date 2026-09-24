package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ConflictException;
import com.openclassroom.datashare.repository.FileTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service for managing file tags.
 * Provides methods to list, create, and resolve tags associated with users.
 */
@Service
@RequiredArgsConstructor
public class FileTagService {

    private final FileTagRepository fileTagRepository;

    /**
     * Lists the tags for a given user with pagination.
     *
     * @param userId the ID of the user
     * @param page   the page number (0-based)
     * @param size   the page size
     * @return a list of FileTag entities
     */
    @Transactional(readOnly = true)
    public List<FileTag> listTags(Long userId, int page, int size) {
        return fileTagRepository.findAllByUserIdOrderByNameAsc(userId, PageRequest.of(page, size)).getContent();
    }

    /**
     * Creates a new tag for the specified user.
     *
     * @param userId  the ID of the user
     * @param rawName the raw name of the tag
     * @return the created FileTag entity
     * @throws ConflictException if a tag with the same name already exists
     */
    @Transactional
    public FileTag createTag(Long userId, String rawName) {
        String name = normalize(rawName);
        fileTagRepository.findByUserIdAndNameIgnoreCase(userId, name).ifPresent(existing -> {
            throw new ConflictException("A tag named '" + name + "' already exists");
        });
        try {
            return fileTagRepository.save(new FileTag(userId, name));
        } catch (DataIntegrityViolationException e) {
            // lost the race against a concurrent request creating the same tag
            throw new ConflictException("A tag named '" + name + "' already exists");
        }
    }

    /**
     * Resolves the supplied tag names for the user, creating the missing ones.
     * Returns an empty set for anonymous uploads, which have no owner to attach
     * tags to.
     *
     * @param userId   the ID of the user
     * @param rawNames the collection of raw tag names
     * @return a set of resolved FileTag entities
     */
    @Transactional
    public Set<FileTag> resolveOrCreate(Long userId, Collection<String> rawNames) {
        if (userId == null || rawNames == null || rawNames.isEmpty()) {
            return Set.of();
        }
        Set<FileTag> resolved = new LinkedHashSet<>();
        for (String uniqueName : distinctNormalized(rawNames)) {
            resolved.add(fileTagRepository.findByUserIdAndNameIgnoreCase(userId, uniqueName)
                    .orElseGet(() -> fileTagRepository.save(new FileTag(userId, uniqueName))));
        }
        return resolved;
    }

    /**
     * Parses the comma-separated {@code tags} field accepted by the multipart
     * upload endpoint.
     *
     * @param rawTags the raw comma-separated tag names
     * @return a list of parsed tag names
     */
    public static List<String> parseCommaSeparated(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }
        return List.of(rawTags.split(","));
    }

    /**
     * Returns a set of distinct normalized tag names from the provided collection.
     * Normalization includes trimming whitespace and ensuring uniqueness in a
     * case-insensitive manner.
     *
     * @param rawNames the collection of raw tag names
     * @return a set of distinct normalized tag names
     */
    private Set<String> distinctNormalized(Collection<String> rawNames) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> lowercased = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = normalize(rawName);
            if (lowercased.add(name.toLowerCase(Locale.ROOT))) {
                seen.add(name);
            }
        }
        return seen;
    }

    /**
     * Normalizes a raw tag name by trimming whitespace and validating its length.
     *
     * @param rawName the raw tag name
     * @return the normalized tag name
     * @throws BadRequestException if the tag name is blank or exceeds the maximum length
     */
    private String normalize(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("A tag name must not be blank");
        }
        if (name.length() > FileTag.MAX_NAME_LENGTH) {
            throw new BadRequestException(
                    "A tag name must not exceed " + FileTag.MAX_NAME_LENGTH + " characters");
        }
        return name;
    }
}
