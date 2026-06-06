package com.famora.security.limiter;

import org.springframework.security.authentication.AuthenticationProvider;

public class RateLimiterAuthenticationProviderProcessor<T extends AuthenticationProvider> {
  
  private final Class<T> clazz;
  
  public RateLimiterAuthenticationProviderProcessor(Class<T> clazz) {
    this.clazz = clazz;
  }
  
  public <V extends T> V postProcess(V object) {
    if (clazz.isAssignableFrom(object.getClass())) {
      return (V) new RateLimitedAuthenticationProvider(object);
    }
    return object;
  }
}
