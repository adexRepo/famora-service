package com.famora.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountDeletionPageControllerTest {

  @Test
  void refusesToPublishPageWithoutMonitoredSupportContact() {
    var response = new AccountDeletionPageController("").accountDeletionPage();

    assertThat(response.getStatusCode().value()).isEqualTo(500);
  }

  @Test
  void escapesConfiguredSupportContactInPublicPage() {
    var response = new AccountDeletionPageController("support+delete@example.com")
        .accountDeletionPage();

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("mailto:support+delete@example.com")
        .contains("Delete your Famora account");
  }
}
