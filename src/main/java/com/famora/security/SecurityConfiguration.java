package com.famora.security;

import com.famora.security.handler.ForbiddenHandler;
import com.famora.security.handler.UnauthorizedHandler;
import com.famora.security.jwt.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The type Security configuration.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
  
  private final UnauthorizedHandler unauthorizedHandler;
  private final ForbiddenHandler forbiddenHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final UserDetailsService userDetailsService;
  
  /**
   * Security filter chain security filter chain.
   *
   * @param http      the http
   * @param publisher the publisher
   * @return the security filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      AuthenticationEventPublisher publisher) {
    http.getSharedObject(AuthenticationManagerBuilder.class)
        .authenticationEventPublisher(publisher);
    
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(handler -> {
          handler.accessDeniedHandler(forbiddenHandler); // when role not match
          handler.authenticationEntryPoint(unauthorizedHandler); // when not login
        }).sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers(HttpMethod.GET,
              "/famora/actuator/health",
              "/famora/api/v1/ping").permitAll();
          auth.requestMatchers(HttpMethod.POST,
              "/famora/api/v1/auth/login",
              "/famora/api/v1/auth/refresh",
              "/famora/api/v1/auth/register").permitAll();
          auth.anyRequest().permitAll();
        })
        .cors(crs -> crs.configurationSource(corsConfigurationSource()))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
  
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }
  
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) {
    return authenticationConfiguration.getAuthenticationManager();
  }
  
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("*"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(
        Arrays.asList("Authorization", "Channel-Type", "Content-Type", "X-Correlation-Id"));
    configuration.setExposedHeaders(List.of("X-Get-Header", "X-Correlation-Id"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
  
  /**
   * Auth success application listener.
   *
   * @return the application listener
   */
  @Bean
  ApplicationListener<AuthenticationSuccessEvent> authSuccess() {
    return event -> {
      var auth = event.getAuthentication();
      LoggerFactory.getLogger(SecurityConfiguration.class)
          .info("Login Successful [{}] - {}", auth.getClass().getSimpleName(),
              auth.getName());
    };
  }
  
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
  
}
