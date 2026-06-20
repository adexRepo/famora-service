package com.famora.common.helper;

import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PagingHelper {
  
  public static PageRequest buildPageRequest(int page, int size, String... sort) {
    
    List<String> validProperties = Stream.of(sort)
        .filter(prop -> prop != null && !prop.isBlank())
        .toList();
    
    return PageRequest.of(page, size,
        Sort.by(validProperties.toArray(new String[0])).descending());
  }
}
