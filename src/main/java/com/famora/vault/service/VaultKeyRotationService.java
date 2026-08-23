package com.famora.vault.service;

import com.famora.vault.entity.VaultItem;
import com.famora.vault.repository.VaultItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultKeyRotationService {

  private static final int BATCH_SIZE = 100;

  private final VaultItemRepository repository;
  private final VaultCryptoService cryptoService;

  @Scheduled(cron = "${app.vault.rotation-cron:0 */5 * * * *}")
  @Transactional
  public int reencryptBatch() {
    List<VaultItem> items = repository.findForKeyRotation(
        cryptoService.activeCiphertextPrefix(), PageRequest.of(0, BATCH_SIZE));
    int rotated = 0;
    for (VaultItem item : items) {
      try {
        item.setEncryptedSecret(cryptoService.reencrypt(item.getEncryptedSecret()));
        rotated++;
      } catch (IllegalStateException exception) {
        log.error("Vault item {} could not be re-encrypted with configured rotation keys",
            item.getId());
      }
    }
    repository.saveAll(items.stream()
        .filter(item -> !cryptoService.requiresReencryption(item.getEncryptedSecret()))
        .toList());
    if (rotated > 0) {
      log.info("Re-encrypted {} vault items with the active key", rotated);
    }
    return rotated;
  }
}
