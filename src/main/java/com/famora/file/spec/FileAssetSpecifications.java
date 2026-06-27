package com.famora.file.spec;

import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import org.springframework.data.jpa.domain.Specification;

public final class FileAssetSpecifications {
  
  private FileAssetSpecifications() {
  }
  
  public static Specification<FileAsset> keyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) {
        return cb.conjunction();
      }
      
      String like = "%" + keyword.trim().toLowerCase() + "%";
      
      return cb.or(
          cb.like(cb.lower(root.get("originalName")), like),
          cb.like(cb.lower(cb.coalesce(root.get("category"), "")), like),
          cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), like)
      );
    };
  }
  
  public static Specification<FileAsset> category(String category) {
    return (root, query, cb) -> {
      if (category == null || category.isBlank()) {
        return cb.conjunction();
      }
      
      return cb.equal(
          cb.lower(cb.coalesce(root.get("category"), "")),
          category.trim().toLowerCase()
      );
    };
  }
  
  public static Specification<FileAsset> fileType(FileType fileType) {
    return (root, query, cb) -> {
      if (fileType == null) {
        return cb.conjunction();
      }
      
      return cb.equal(root.get("fileType"), fileType);
    };
  }
}
