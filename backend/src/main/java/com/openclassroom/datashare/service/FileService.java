package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ForbiddenException;
import com.openclassroom.datashare.exception.PayloadTooLargeException;
import com.openclassroom.datashare.exception.ResourceNotFoundException;
import com.openclassroom.datashare.repository.FileRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service class for managing file operations such as upload, retrieval, update, and deletion.
 * This class handles business logic related to files, including validation and interaction with the repository and storage services.
 */
@Service
public class FileService {

    public static final long MAX_SIZE_BYTES = 1024L * 1024L * 1024L; // 1 GB
    public static final int MAX_EXPIRATION_DAYS = 7;
    public static final int DEFAULT_EXPIRATION_DAYS = 7;
    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final FileRepository fileRepository;
    private final DownloadTokenService downloadTokenService;
    private final FileTagService fileTagService;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final List<String> forbiddenExtensions;

    /**
     * Constructs a FileService with the necessary dependencies.
     *
     * @param fileRepository       the repository for managing file entities
     * @param downloadTokenService the service for managing download tokens
     * @param fileTagService       the service for managing file tags
     * @param storageService       the service for handling file storage operations
     * @param passwordEncoder      the encoder for hashing passwords
     * @param forbiddenExtensions  a comma-separated list of forbidden file extensions
     */
    public FileService(FileRepository fileRepository,
            DownloadTokenService downloadTokenService,
            FileTagService fileTagService,
            FileStorageService storageService,
            PasswordEncoder passwordEncoder,
            @Value("${app.upload.forbidden-extensions:.exe,.bat,.cmd,.sh,.msi,.dll,.com,.scr,.jar,.vbs,.ps1}") String forbiddenExtensions) {
        this.fileRepository = fileRepository;
        this.downloadTokenService = downloadTokenService;
        this.fileTagService = fileTagService;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
        this.forbiddenExtensions = normalizeExtensions(forbiddenExtensions);
    }

    /**
     * Uploads a file based on the provided command.
     *
     * @param command the upload command containing file and metadata
     * @return the result of the upload operation, including the file entity and download token
     */
    @Transactional
    public UploadResult upload(UploadCommand command) {
        MultipartFile file = command.file();
        String originalName = resolveOriginalName(command.originalName(), file);
        int expiresInDays = command.expiresInDays() == null ? DEFAULT_EXPIRATION_DAYS : command.expiresInDays();

        validateUpload(file, originalName, expiresInDays, command.password());

        String storageKey = "files/" + UUID.randomUUID() + "-" + sanitize(originalName);
        String mimeType = resolveMimeType(command.mimeType(), file);
        Set<FileTag> tags = fileTagService.resolveOrCreate(command.ownerId(), command.tags());

        storageService.store(file, storageKey, mimeType);
        try {
            File entity = new File();
            entity.setOwnerId(command.ownerId());
            entity.setOriginalName(originalName);
            entity.setMimeType(mimeType);
            entity.setSizeBytes(file.getSize());
            entity.setStorageKey(storageKey);
            entity.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
            entity.setTags(tags);
            if (hasPassword(command.password())) {
                entity.setPasswordHash(passwordEncoder.encode(command.password()));
            }

            entity = fileRepository.save(entity);
            return new UploadResult(entity, downloadTokenService.issueFor(entity));
        } catch (RuntimeException e) {
            storageService.delete(storageKey);
            throw e;
        }
    }

    /**
     * Lists files owned by a specific user, with optional filtering by tag and search query.
     *
     * @param ownerId the ID of the owner
     * @param page    the page number for pagination
     * @param size    the number of items per page
     * @param tag     an optional tag to filter files
     * @param q       an optional search query to filter files by original name
     * @return a paginated list of files matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<File> listFiles(Long ownerId, int page, int size, String tag, String q) {
        Specification<File> spec = ownedBy(ownerId);

        if (tag != null && !tag.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(cb.lower(root.join("tags", JoinType.INNER).get("name")), tag.toLowerCase(Locale.ROOT));
            });
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("originalName")),
                    "%" + q.toLowerCase(Locale.ROOT) + "%"));
        }

        return fileRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

   /**
     * Retrieves a file owned by a specific user, ensuring that the file exists and is owned by the user.
     *
     * @param fileId  the ID of the file to retrieve
     * @param ownerId the ID of the owner
     * @return the file entity if found and owned by the user
     * @throws ResourceNotFoundException if the file does not exist or is not owned by the user
     */
    @Transactional(readOnly = true)
    public File getOwnedFile(Long fileId, Long ownerId) {
        return fileRepository.findByFileIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }


    /**
     * Retrieves a file owned by a specific user, even if the file has expired.
     * Ensures that the file exists and is owned by the user.
     *
     * @param fileId  the ID of the file to retrieve
     * @param ownerId the ID of the owner
     * @return the file entity if found and owned by the user
     * @throws ResourceNotFoundException if the file does not exist
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional(readOnly = true)
    public File requireOwnedFileEvenIfExpired(Long fileId, Long ownerId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!java.util.Objects.equals(ownerId, file.getOwnerId())) {
            throw new ForbiddenException("You do not own this file");
        }
        return file;
    }

    /**
     * Retrieves a file owned by a specific user, ensuring that the file exists, is owned by the user, and has not expired.
     *
     * @param fileId  the ID of the file to retrieve
     * @param ownerId the ID of the owner
     * @return the file entity if found, owned by the user, and not expired
     * @throws ResourceNotFoundException if the file does not exist or has expired
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional(readOnly = true)
    public File requireOwnedFile(Long fileId, Long ownerId) {
        File file = requireOwnedFileEvenIfExpired(fileId, ownerId);
        if (file.isExpired(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("File not found");
        }
        return file;
    }

    /**
     * Creates a download token for a file owned by a specific user.
     *
     * @param fileId  the ID of the file for which to create a download token
     * @param ownerId the ID of the owner
     * @return the issued download token
     * @throws ResourceNotFoundException if the file does not exist or has expired
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional
    public DownloadTokenService.IssuedToken createDownloadToken(Long fileId, Long ownerId) {
        return downloadTokenService.issueFor(requireOwnedFile(fileId, ownerId));
    }

    /**
     * Updates the metadata of a file owned by a specific user.
     *
     * @param fileId        the ID of the file to update
     * @param ownerId       the ID of the owner
     * @param originalName  the new original name for the file (optional)
     * @param expiresInDays the new expiration time in days (optional)
     * @param password      the new password for the file (optional)
     * @param tags          the new list of tags for the file (optional)
     * @return the updated file entity
     * @throws ResourceNotFoundException if the file does not exist or has expired
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional
    public File updateFile(Long fileId, Long ownerId, String originalName,
            Integer expiresInDays, String password, List<String> tags) {
        File file = requireOwnedFile(fileId, ownerId);

        if (originalName != null && !originalName.isBlank()) {
            file.setOriginalName(originalName.trim());
        }
        if (expiresInDays != null) {
            validateExpiration(expiresInDays);
            file.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
        }
        if (password != null) {
            if (password.isBlank()) {
                file.setPasswordHash(null);
            } else {
                validatePassword(password);
                file.setPasswordHash(passwordEncoder.encode(password));
            }
        }
        if (tags != null) {
            file.setTags(fileTagService.resolveOrCreate(ownerId, tags));
        }
        return fileRepository.save(file);
    }

    /**
     * Replaces the tags of a file owned by a specific user.
     *
     * @param fileId  the ID of the file for which to replace tags
     * @param ownerId the ID of the owner
     * @param tags    the new list of tags to associate with the file
     * @return the updated file entity with replaced tags
     * @throws ResourceNotFoundException if the file does not exist or has expired
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional
    public File replaceTags(Long fileId, Long ownerId, List<String> tags) {
        File file = requireOwnedFile(fileId, ownerId);
        file.setTags(fileTagService.resolveOrCreate(ownerId, tags == null ? List.of() : tags));
        return fileRepository.save(file);
    }

    /**
     * Deletes a file owned by a specific user, removing its metadata, associated tags, download token, and stored object.
     *
     * @param fileId  the ID of the file to delete
     * @param ownerId the ID of the owner
     * @throws ResourceNotFoundException if the file does not exist or has expired
     * @throws ForbiddenException        if the file is not owned by the user
     */
    @Transactional
    public void deleteFile(Long fileId, Long ownerId) {
        purge(requireOwnedFileEvenIfExpired(fileId, ownerId));
    }

    /**
     * Purges a file from the system, removing its metadata, associated tags, download token, and stored object.
     * This method is intended for internal use and does not perform ownership checks.
     *
     * @param file the file entity to purge
     */
    @Transactional
    public void purge(File file) {
        downloadTokenService.deleteFor(file.getFileId());
        file.getTags().clear();
        fileRepository.delete(file);
        fileRepository.flush();
        storageService.delete(file.getStorageKey());
    }

    /**
     * Finds expired files that have an expiration date before the specified time.
     *
     * @param now       the current time to compare against file expiration dates
     * @param batchSize the maximum number of expired files to retrieve
     * @return a list of expired files
     */
    @Transactional(readOnly = true)
    public List<File> findExpired(OffsetDateTime now, int batchSize) {
        return fileRepository.findAllByExpiresAtBefore(now, PageRequest.of(0, batchSize));
    }

    /**
     * Validates the uploaded file and its associated metadata.
     *
     * @param file          the uploaded file to validate
     * @param originalName  the original name of the file
     * @param expiresInDays the expiration time in days for the file
     * @param password      the password associated with the file (optional)
     * @throws BadRequestException       if any validation fails
     * @throws PayloadTooLargeException  if the file size exceeds the maximum allowed size
     */
    private void validateUpload(MultipartFile file, String originalName, int expiresInDays, String password) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The uploaded file must not be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new PayloadTooLargeException("Maximum file size is 1 GB");
        }
        validateExpiration(expiresInDays);
        if (hasPassword(password)) {
            validatePassword(password);
        }
        if (isForbiddenExtension(originalName)) {
            throw new BadRequestException("This file type is not allowed");
        }
    }

    /**
     * Validates the expiration time in days for a file upload.
     *
     * @param expiresInDays the expiration time in days to validate
     * @throws BadRequestException if the expiration time is not within the allowed range
     */
    private void validateExpiration(int expiresInDays) {
        if (expiresInDays < 1 || expiresInDays > MAX_EXPIRATION_DAYS) {
            throw new BadRequestException("expiresInDays must be between 1 and " + MAX_EXPIRATION_DAYS);
        }
    }

    /**
     * Validates the password for a file upload.
     *
     * @param password the password to validate
     * @throws BadRequestException if the password is shorter than the minimum required length
     */
    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException(
                    "password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }

    /**
     * Checks if a password is provided and not blank.
     *
     * @param password the password to check
     * @return true if the password is not null and not blank, false otherwise
     */
    private boolean hasPassword(String password) {
        return password != null && !password.isBlank();
    }

    /**
     * Checks if the filename has a forbidden extension.
     *
     * @param filename the filename to check
     * @return true if the filename ends with a forbidden extension, false otherwise
     */
    private boolean isForbiddenExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return forbiddenExtensions.stream().anyMatch(lower::endsWith);
    }

    /**
     * Resolves the original name of the uploaded file based on the requested name and the file's original filename.
     *
     * @param requestedName the requested original name for the file (optional)
     * @param file          the uploaded file
     * @return the resolved original name for the file
     */
    private String resolveOriginalName(String requestedName, MultipartFile file) {
        String candidate = requestedName != null && !requestedName.isBlank()
                ? requestedName
                : (file == null ? null : file.getOriginalFilename());
        if (candidate == null || candidate.isBlank()) {
            return "file";
        }
        String baseName = Paths.get(candidate.replace('\\', '/')).getFileName().toString().trim();
        return baseName.isEmpty() ? "file" : baseName;
    }

    /**
     * Resolves the MIME type of the uploaded file based on the requested MIME type and the file's content type.
     *
     * @param requestedMimeType the requested MIME type for the file (optional)
     * @param file              the uploaded file
     * @return the resolved MIME type for the file
     */
    private String resolveMimeType(String requestedMimeType, MultipartFile file) {
        if (requestedMimeType != null && !requestedMimeType.isBlank()) {
            return requestedMimeType;
        }
        String contentType = file == null ? null : file.getContentType();
        return contentType != null && !contentType.isBlank() ? contentType : DEFAULT_MIME_TYPE;
    }

    /**
     * Sanitizes a filename by replacing invalid characters with underscores.
     *
     * @param name the filename to sanitize
     * @return the sanitized filename
     */
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Creates a JPA Specification to filter files owned by a specific user.
     *
     * @param ownerId the ID of the owner
     * @return a Specification for filtering files by owner ID
     */
    private static Specification<File> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    /**
     * Normalizes a comma-separated list of file extensions by trimming whitespace, converting to lowercase,
     * and ensuring each extension starts with a dot.
     *
     * @param rawExtensions the raw comma-separated list of file extensions
     * @return a list of normalized file extensions
     */
    private static List<String> normalizeExtensions(String rawExtensions) {
        return Arrays.stream(rawExtensions.split(","))
                .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                .filter(extension -> !extension.isEmpty())
                .map(extension -> extension.startsWith(".") ? extension : "." + extension)
                .toList();
    }

    /**
     * Represents a command for uploading a file, containing the file and its associated metadata.
     *
     * @param file          the uploaded file
     * @param originalName  the original name of the file (optional)
     * @param mimeType      the MIME type of the file (optional)
     * @param expiresInDays the expiration time in days for the file (optional)
     * @param password      the password for the file (optional)
     * @param tags          a list of tags associated with the file (optional)
     * @param ownerId       the ID of the owner of the file
     */
    public record UploadCommand(MultipartFile file, String originalName, String mimeType,
            Integer expiresInDays, String password, List<String> tags, Long ownerId) {
    }

    /**
     * Represents the result of a file upload operation, containing the uploaded file entity and the issued download token.
     *
     * @param file          the uploaded file entity
     * @param downloadToken the issued download token for accessing the file
     */
    public record UploadResult(File file, DownloadTokenService.IssuedToken downloadToken) {
    }
}
