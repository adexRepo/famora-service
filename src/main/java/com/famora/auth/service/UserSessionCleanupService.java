package com.famora.auth.service;

import com.famora.user.repository.UserSessionRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionCleanupService {

  private final UserSessionRepository userSessionRepository;

  @Scheduled(cron = "${app.security.session-cleanup-cron:0 17 3 * * *}")
  @Transactional
  public void deleteExpiredSessions() {
    int deleted = userSessionRepository.deleteExpiredBefore(OffsetDateTime.now());
    if (deleted > 0) {
      log.info("Deleted {} expired user sessions", deleted);
    }
  }
}
