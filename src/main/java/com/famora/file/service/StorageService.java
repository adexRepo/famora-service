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
import java.util.HexFormat;
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
      @Value("${famora.minio.endpoint}") String endpoint,
      @Value("${famora.minio.access-key}") String accessKey,
      @Value("${famora.minio.secret-key}") String secretKey,
      @Value("${famora.minio.bucket}") String bucketName,
      @Value("${famora.storage.max-upload-bytes}") long maxUploadBytes,
      @Value("${famora.storage.root}") String root
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
        ? storeToMinio(file, familyId, folder)
        : storeToMft(file, familyId, folder);
    
  }
  
  public StoredFile storeToMinio(MultipartFile file, UUID familyId, String folder) {
    validate(file);
    
    try {
      LocalDate now = LocalDate.now();
      
      String ext = safeExtension(file.getOriginalFilename());
      String storedName = UUID.randomUUID() + ext;
      
      String objectKey = buildObjectKey(
          familyId,
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
                .contentType(mime(file))
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
          detectFileType(file.getContentType(), file.getOriginalFilename()),
          mime(file)
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
      UUID familyId,
      String folder,
      int year,
      int month,
      String storedName
  ) {
    String safeFolder = normalizeFolder(folder);
    
    return "families/%s/%s/%d/%02d/%s".formatted(
        familyId,
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
  
  private void validateObjectKey(String objectKey) {
    String invalidObjectKey = "Invalid object key";
    if (!StringUtils.hasText(objectKey)) {
      throw new AppException(HttpStatus.BAD_REQUEST, invalidObjectKey);
    }
    
    if (objectKey.contains("..") || objectKey.startsWith("/") || objectKey.contains("\\")) {
      throw new AppException(HttpStatus.BAD_REQUEST, invalidObjectKey);
    }
    
    if (!objectKey.startsWith("families/")) {
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
  
  
  private StoredFile storeToMft(MultipartFile file, UUID familyId, String bucket) {
    validate(file);
    try {
      LocalDate now = LocalDate.now();
      String ext = safeExtension(file.getOriginalFilename());
      String storedName = UUID.randomUUID() + ext;
      Path dir = root.resolve("families").resolve(familyId.toString()).resolve(bucket)
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
          detectFileType(file.getContentType(), file.getOriginalFilename()), mime(file));
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
  
  
  private String safeExtension(String name) {
    String clean = StringUtils.cleanPath(name == null ? "" : name);
    int i = clean.lastIndexOf('.');
    
    if (i < 0 || i == clean.length() - 1) {
      return "";
    }
    
    String ext = clean.substring(i).toLowerCase();
    
    return ext.matches("\\.[a-z0-9]{1,10}") ? ext : "";
  }
  
  private String mime(MultipartFile file) {
    return StringUtils.hasText(file.getContentType())
        ? file.getContentType()
        : "application/octet-stream";
  }
}
