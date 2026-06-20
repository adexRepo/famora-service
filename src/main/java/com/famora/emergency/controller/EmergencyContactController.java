package com.famora.emergency.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.emergency.dto.EmergencyDtos;
import com.famora.emergency.dto.EmergencyDtos.Request;
import com.famora.emergency.dto.EmergencyDtos.Response;
import com.famora.emergency.helper.EmergencyCategory;
import com.famora.emergency.service.EmergencyContactService;
import com.famora.security.FamilyContextService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {
  
  private final FamilyContextService families;
  private final EmergencyContactService service;
  
  @PostMapping
  public ApiResponse<Response> create(@RequestHeader("X-Family-Id") String familyId,
      @RequestBody Request req) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(EmergencyDtos.Response.from(service.create(req, ctx)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<EmergencyDtos.Response>> list(
      @RequestHeader("X-Family-Id") String familyId, @RequestParam(required = false) String keyword,
      @RequestParam(required = false) EmergencyCategory category,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(PageResponse.from(service.list(ctx, keyword, category,
            PageRequest.of(page, size, Sort.by("createdAt").descending()))
        .map(EmergencyDtos.Response::from)));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<EmergencyDtos.Response> get(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(EmergencyDtos.Response.from(service.get(id, ctx)));
  }
  
  @PutMapping("/{id}")
  public ApiResponse<EmergencyDtos.Response> update(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id, @RequestBody EmergencyDtos.Request req) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(EmergencyDtos.Response.from(service.update(id, req, ctx)));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<String> delete(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    service.delete(id, ctx);
    return ApiResponse.ok("Deleted");
  }
}
