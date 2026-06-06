package com.famora.security.limiter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;


public class RateLimitedAuthenticationProvider implements AuthenticationProvider {
  
  private final AuthenticationProvider delegateProvider;
  private final Map<String, Instant> cache = new ConcurrentHashMap<>();
  
  public RateLimitedAuthenticationProvider(AuthenticationProvider authenticationProvider) {
    this.delegateProvider = authenticationProvider;
  }
  
  @Override
  public Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    var parentAuthenticate = delegateProvider.authenticate(authentication);
    if (checkLimit(parentAuthenticate)) {
      return parentAuthenticate;
    }
    throw new BadCredentialsException("Are you bot?, so fast!");
  }
  
  private boolean checkLimit(Authentication parentAuthenticate) {
    Instant nowAuthTime = Instant.now();
    Instant previousAuthTime = cache.get(parentAuthenticate.getName());
    cache.put(parentAuthenticate.getName(), nowAuthTime);
    return previousAuthTime == null || previousAuthTime.plus(Duration.ofMinutes(1))
        .isBefore(nowAuthTime);
  }
  
  @Override
  public boolean supports(Class<?> authentication) {
    return delegateProvider.supports(authentication);
  }
}
