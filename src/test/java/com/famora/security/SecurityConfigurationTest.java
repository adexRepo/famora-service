package com.famora.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.famora.common.controller.PingController;
import com.famora.security.config.CorsProperties;
import com.famora.security.handler.ForbiddenHandler;
import com.famora.security.handler.UnauthorizedHandler;
import com.famora.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.stream.Stream;
import java.util.Arrays;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PingController.class, properties = {
    "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "app.vault.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@Import({SecurityConfiguration.class, UnauthorizedHandler.class, ForbiddenHandler.class,
    SecurityConfigurationTest.TestConfig.class})
class SecurityConfigurationTest {

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockitoBean
  private UserDetailsService userDetailsService;
  @MockitoBean
  private AuthenticationEventPublisher authenticationEventPublisher;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void passRequestsThroughJwtFilter() throws Exception {
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2);
      chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
  }

  @ParameterizedTest
  @MethodSource("protectedControllerPaths")
  void everyProtectedControllerRejectsRequestsWithoutToken(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
  }

  @Test
  void actuatorMetricsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  void pingRemainsPublic() throws Exception {
    mockMvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
  }

  @Test
  void websocketHandshakeAndDeletionInformationPageDoNotRequireAccessToken() throws Exception {
    for (String path : List.of("/ws", "/api/v1/account-deletion")) {
      mockMvc.perform(get(path)).andExpect(result -> {
        if (result.getResponse().getStatus() == 401) {
          throw new AssertionError(path + " must reach its own handshake/page authentication");
        }
      });
    }
  }

  @Test
  void configuredAuthEntryPointsRemainPublic() throws Exception {
    for (String path : List.of("/api/v1/auth/login", "/api/v1/auth/register",
        "/api/v1/auth/refresh")) {
      mockMvc.perform(post(path).contentType("application/json").content("{}"))
          .andExpect(result -> {
            if (result.getResponse().getStatus() == 401) {
              throw new AssertionError(path + " must not require an access token");
            }
          });
    }
  }

  static Stream<String> protectedControllerPaths() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    return scanner.findCandidateComponents("com.famora").stream()
        .map(candidate -> loadClass(candidate.getBeanClassName()))
        .map(type -> AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class))
        .filter(java.util.Objects::nonNull)
        .flatMap(mapping -> Arrays.stream(mapping.value()))
        .map(path -> path.replaceAll("\\{[^}]+}",
            "00000000-0000-0000-0000-000000000000"))
        .distinct();
  }

  private static Class<?> loadClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    CorsProperties corsProperties() {
      return new CorsProperties(List.of("https://app.example.test"));
    }
  }
}
