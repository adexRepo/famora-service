package com.famora.user.controller;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class AccountDeletionPageController {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$");

  private final String supportEmail;

  public AccountDeletionPageController(
      @Value("${app.account-deletion.support-email:}") String supportEmail) {
    this.supportEmail = supportEmail == null ? "" : supportEmail.trim();
  }

  @GetMapping(value = "/api/v1/account-deletion", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> accountDeletionPage() {
    if (!StringUtils.hasText(supportEmail) || !EMAIL_PATTERN.matcher(supportEmail).matches()) {
      return ResponseEntity.internalServerError()
          .contentType(MediaType.TEXT_PLAIN)
          .body("Account deletion support contact is not configured");
    }
    String safeEmail = HtmlUtils.htmlEscape(supportEmail);
    String html = """
        <!doctype html>
        <html lang="en">
        <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Delete your Famora account</title></head>
        <body><main>
        <h1>Delete your Famora account</h1>
        <p>In the Famora app, open Profile, choose Delete account, enter your current password,
        and confirm DELETE. The app will show any family or business ownership that must be
        transferred first.</p>
        <p>If you cannot access the app, email <a href="mailto:%s?subject=Famora%%20account%%20deletion">%s</a>
        from the address registered to your Famora account. Support will verify ownership before
        processing the request.</p>
        <p>Your profile and authentication sessions are removed or anonymized. Shared family and
        business records may be retained for other members, legal obligations, fraud prevention,
        and security audit purposes.</p>
        </main></body></html>
        """.formatted(safeEmail, safeEmail);
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }
}
