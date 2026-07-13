package com.famora.file.service;

import com.famora.common.exception.AppException;
import com.famora.file.dto.StoredFile;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
  
  private final Path root;
  private final long maxUploadBytes;
  private final MinioClient minioClient;
  private final String bucketName;
  
  public StorageService(
      @Value("${app.minio.endpoint}") String endpoint,
      @Value("${app.minio.access-key}") String accessKey,
      @Value("${app.minio.secret-key}") String secretKey,
      @Value("${app.minio.bucket}") String bucketName,
      @Value("${app.storage.max-upload-bytes}") long maxUploadBytes,
      @Value("${app.storage.root}") String root
  ) {
    this.minioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build();
    
    this.root = Paths.get(root).toAbsolutePath().normalize();
    this.bucketName = bucketName;
    this.maxUploadBytes = maxUploadBytes;
    
    ensureBucketExists();
  }
  
  public StoredFile store(StorageType storageType, MultipartFile file, UUID familyId,
      String folder) {
    return storageType.equals(StorageType.MINIO)
        ? storeToMinio(file, "families", familyId, folder)
        : storeToMft(file, "families", familyId, folder);
    
  }
  
  public StoredFile storeBusiness(StorageType storageType, MultipartFile file, UUID businessId,
      String folder) {
    return storageType.equals(StorageType.MINIO)
        ? storeToMinio(file, "businesses", businessId, folder)
        : storeToMft(file, "businesses", businessId, folder);
  }
  
  public StoredFile storeToMinio(MultipartFile file, UUID ownerId, String folder) {
    return storeToMinio(file, "families", ownerId, folder);
  }
  
  private StoredFile storeToMinio(MultipartFile file, String scope, UUID ownerId, String folder) {
    validate(file);
    UploadMetadata metadata = inspect(file);
    
    try {
      LocalDate now = LocalDate.now();
      
      String ext = safeStoredExtension(metadata.originalExtension());
      String storedName = UUID.randomUUID() + ext;
      
      String objectKey = buildObjectKey(
          scope,
          ownerId,
          folder,
          now.getYear(),
          now.getMonthValue(),
          storedName
      );
      
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      
      try (
          InputStream rawInputStream = file.getInputStream();
          DigestInputStream digestInputStream = new DigestInputStream(rawInputStream, digest)
      ) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(digestInputStream, file.getSize(), -1L)
                .contentType(metadata.resolvedMimeType())
                .build()
        );
      }
      
      String hash = HexFormat.of().formatHex(digest.digest());
      
      return new StoredFile(
          storedName,
          null,
          bucketName,
          objectKey,
          hash,
          metadata.fileType(),
          metadata.resolvedMimeType(),
          metadata.originalName(),
          metadata.originalExtension(),
          metadata.originalMimeType(),
          metadata.toJson()
      );
      
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to storeToMinio file: " + e.getMessage()
      );
    }
  }
  
  public Resource load(StorageType storageType, String bucketName, String objectKeyOrPath) {
    if (storageType == StorageType.MINIO) {
      return loadMinio(bucketName, objectKeyOrPath);
    }
    
    return loadFromMft(objectKeyOrPath);
  }
  
  private Resource loadMinio(String bucketName, String objectKey) {
    try {
      validateObjectKey(objectKey);
      
      InputStream inputStream = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)
              .object(objectKey)
              .build()
      );
      
      return new InputStreamResource(inputStream);
      
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.NOT_FOUND, "File not found");
    }
  }
  
  private void ensureBucketExists() {
    try {
      boolean exists = minioClient.bucketExists(
          BucketExistsArgs.builder()
              .bucket(bucketName)
              .build()
      );
      
      if (!exists) {
        minioClient.makeBucket(
            MakeBucketArgs.builder()
                .bucket(bucketName)
                .build()
        );
      }
    } catch (Exception e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to initialize MinIO bucket: " + e.getMessage()
      );
    }
  }
  
  private String buildObjectKey(
      String scope,
      UUID ownerId,
      String folder,
      int year,
      int month,
      String storedName
  ) {
    String safeFolder = normalizeFolder(folder);
    
    return "%s/%s/%s/%d/%02d/%s".formatted(
        normalizeScope(scope),
        ownerId,
        safeFolder,
        year,
        month,
        storedName
    );
  }
  
  private String normalizeFolder(String folder) {
    if (!StringUtils.hasText(folder)) {
      return "files";
    }
    
    String clean = folder.trim().toLowerCase();
    
    if (!clean.matches("[a-z0-9_-]{1,50}")) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid storage folder");
    }
    
    return clean;
  }
  
  private String normalizeScope(String scope) {
    if ("businesses".equals(scope)) {
      return "businesses";
    }
    return "families";
  }
  
  private void validateObjectKey(String objectKey) {
    String invalidObjectKey = "Invalid object key";
    if (!StringUtils.hasText(objectKey)) {
      throw new AppException(HttpStatus.BAD_REQUEST, invalidObjectKey);
    }
    
    if (objectKey.contains("..") || objectKey.startsWith("/") || objectKey.contains("\\")) {
      throw new AppException(HttpStatus.BAD_REQUEST, invalidObjectKey);
    }
    
    if (!objectKey.startsWith("families/") && !objectKey.startsWith("businesses/")) {
      throw new AppException(HttpStatus.FORBIDDEN, invalidObjectKey);
    }
  }
  
  private FileType detectFileType(String mime, String name) {
    String m = mime == null ? "" : mime.toLowerCase();
    String n = name == null ? "" : name.toLowerCase();
    
    if (m.startsWith("image/")) {
      return FileType.IMAGE;
    }
    
    if (m.startsWith("video/")) {
      return FileType.VIDEO;
    }
    
    if (m.equals("application/pdf") || n.endsWith(".pdf")) {
      return FileType.PDF;
    }
    
    if (m.startsWith("audio/")) {
      return FileType.AUDIO;
    }
    
    if (n.matches(".*\\.(zip|rar|7z|tar|gz)$")) {
      return FileType.ARCHIVE;
    }
    
    if (n.matches(".*\\.(doc|docx|xls|xlsx|ppt|pptx|txt|csv)$")) {
      return FileType.DOCUMENT;
    }
    
    return FileType.OTHER;
  }
  
  
  private StoredFile storeToMft(MultipartFile file, String scope, UUID ownerId, String bucket) {
    validate(file);
    UploadMetadata metadata = inspect(file);
    try {
      LocalDate now = LocalDate.now();
      String ext = safeStoredExtension(metadata.originalExtension());
      String storedName = UUID.randomUUID() + ext;
      Path dir = root.resolve(normalizeScope(scope)).resolve(ownerId.toString()).resolve(bucket)
          .resolve(String.valueOf(now.getYear())).resolve("%02d".formatted(now.getMonthValue()))
          .normalize();
      if (!dir.startsWith(root)) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Invalid storage path");
      }
      Files.createDirectories(dir);
      Path target = dir.resolve(storedName).normalize();
      if (!target.startsWith(dir)) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Invalid stored filename");
      }
      String hash;
      try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(target,
          StandardOpenOption.CREATE_NEW)) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
          digest.update(buf, 0, len);
          out.write(buf, 0, len);
        }
        hash = HexFormat.of().formatHex(digest.digest());
      }
      return new StoredFile(storedName, target.toString(), null, null, hash,
          metadata.fileType(), metadata.resolvedMimeType(), metadata.originalName(),
          metadata.originalExtension(), metadata.originalMimeType(), metadata.toJson());
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to storeToMinio com.famora.file: " + e.getMessage());
    }
  }
  
  private Resource loadFromMft(String storagePath) {
    try {
      Path path = Paths.get(storagePath).toAbsolutePath().normalize();
      if (!path.startsWith(root)) {
        throw new AppException(HttpStatus.FORBIDDEN, "Invalid storage path");
      }
      Resource res = new UrlResource(path.toUri());
      if (!res.exists() || !res.isReadable()) {
        throw new AppException(HttpStatus.NOT_FOUND, "Physical com.famora.file not found");
      }
      return res;
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.NOT_FOUND, "File not found");
    }
  }
  
  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "File is required");
    }
    
    if (file.getSize() > maxUploadBytes) {
      throw new AppException(HttpStatus.BAD_REQUEST, "File exceeds max upload size");
    }
    
    String name = StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
    );
    
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid filename");
    }
  }
  
  public void validateMaxUploadSize(MultipartFile file, long maxBytes, String label) {
    if (file == null || file.isEmpty()) {
      throw new AppException(HttpStatus.BAD_REQUEST, label + " is required");
    }
    if (file.getSize() > maxBytes) {
      throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE,
          "%s exceeds max upload size of %d bytes".formatted(label, maxBytes));
    }
  }
  
  public boolean isSupportedImage(MultipartFile file) {
    return inspect(file).fileType() == FileType.IMAGE;
  }
  
  private UploadMetadata inspect(MultipartFile file) {
    String originalName = StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
    );
    String originalExtension = originalExtension(originalName);
    String originalMimeType = mime(file);
    String resolvedMimeType = detectMimeType(file, originalMimeType, originalExtension);
    FileType fileType = detectFileType(resolvedMimeType, originalName);
    return new UploadMetadata(originalName, originalExtension, originalMimeType,
        resolvedMimeType, fileType, file.getSize());
  }
  
  private String detectMimeType(MultipartFile file, String originalMimeType,
      String originalExtension) {
    byte[] header = firstBytes(file, 16);
    if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
      return "image/jpeg";
    }
    if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
      return "image/png";
    }
    if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) {
      return "image/gif";
    }
    if (startsWithAscii(header, "RIFF") && header.length >= 12
        && startsWithAscii(Arrays.copyOfRange(header, 8, 12), "WEBP")) {
      return "image/webp";
    }
    if (startsWithAscii(header, "%PDF-")) {
      return "application/pdf";
    }
    if (startsWith(header, 0x50, 0x4B, 0x03, 0x04)) {
      return officeZipMime(originalExtension);
    }
    return StringUtils.hasText(originalMimeType) ? originalMimeType : "application/octet-stream";
  }
  
  private String officeZipMime(String extension) {
    return switch (extension) {
      case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
      default -> "application/zip";
    };
  }
  
  private byte[] firstBytes(MultipartFile file, int maxBytes) {
    try (InputStream inputStream = file.getInputStream()) {
      return inputStream.readNBytes(maxBytes);
    } catch (Exception e) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file");
    }
  }
  
  private static boolean startsWith(byte[] actual, int... expected) {
    if (actual.length < expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if ((actual[i] & 0xFF) != expected[i]) {
        return false;
      }
    }
    return true;
  }
  
  private static boolean startsWithAscii(byte[] actual, String expected) {
    byte[] expectedBytes = expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    if (actual.length < expectedBytes.length) {
      return false;
    }
    for (int i = 0; i < expectedBytes.length; i++) {
      if (actual[i] != expectedBytes[i]) {
        return false;
      }
    }
    return true;
  }
  
  private String originalExtension(String name) {
    String clean = StringUtils.cleanPath(name == null ? "" : name);
    int i = clean.lastIndexOf('.');
    if (i < 0 || i == clean.length() - 1) {
      return null;
    }
    String ext = clean.substring(i + 1).toLowerCase(Locale.ROOT);
    return ext.matches("[a-z0-9]{1,10}") ? ext : null;
  }
  
  private String safeStoredExtension(String originalExtension) {
    return StringUtils.hasText(originalExtension) ? "." + originalExtension : "";
  }
  
  private String mime(MultipartFile file) {
    return StringUtils.hasText(file.getContentType())
        ? file.getContentType()
        : "application/octet-stream";
  }
  
  private record UploadMetadata(
      String originalName,
      String originalExtension,
      String originalMimeType,
      String resolvedMimeType,
      FileType fileType,
      long originalFileSize
  ) {
    
    Map<String, Object> toJson() {
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("originalName", originalName);
      metadata.put("originalExtension", originalExtension);
      metadata.put("originalMimeType", originalMimeType);
      metadata.put("resolvedMimeType", resolvedMimeType);
      metadata.put("fileType", fileType.name());
      metadata.put("originalFileSize", originalFileSize);
      metadata.put("serverCompressed", false);
      return metadata;
    }
  }
}
