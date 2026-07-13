package com.famora.common.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MultipartUploadConfig {
  
  @Bean
  public MultipartConfigElement multipartConfigElement(
      @Value("${spring.servlet.multipart.max-file-size:50MB}") String maxFileSize,
      @Value("${spring.servlet.multipart.max-request-size:60MB}") String maxRequestSize
  ) {
    MultipartConfigFactory factory = new MultipartConfigFactory();
    factory.setMaxFileSize(DataSize.parse(maxFileSize));
    factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
    return factory.createMultipartConfig();
  }
}
