package com.famora.file.service;

import com.famora.file.entity.FileAsset;

public interface FileDeletionListener {

  void beforeDelete(FileAsset file);
}
