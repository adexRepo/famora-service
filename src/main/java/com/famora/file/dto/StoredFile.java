package com.famora.file.dto;

import com.famora.file.helper.FileType;

public record StoredFile(
    String storedName,
    String storagePath,
    String bucketName,
    String objectKey,
    String sha256,
    FileType fileType,
    String mimeType
) {

}
