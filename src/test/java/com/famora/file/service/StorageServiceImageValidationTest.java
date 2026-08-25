package com.famora.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class StorageServiceImageValidationTest {

  private static final byte[] PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @Test
  void acceptsDecodablePngWithMatchingMime() {
    assertThat(StorageService.isSupportedImageContent(PNG, "image/png")).isTrue();
  }

  @Test
  void rejectsClientMimeThatDoesNotMatchMagicBytes() {
    assertThat(StorageService.isSupportedImageContent(PNG, "image/jpeg")).isFalse();
  }

  @Test
  void rejectsSvgAndArbitraryContentEvenWhenDeclaredAsImage() {
    byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'></svg>"
        .getBytes(StandardCharsets.UTF_8);
    assertThat(StorageService.isSupportedImageContent(svg, "image/svg+xml")).isFalse();
    assertThat(StorageService.isSupportedImageContent(new byte[] {1, 2, 3}, "image/png"))
        .isFalse();
  }
}
