package com.famora.common.exception;

import com.famora.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @Value("${app.print-stack-trace.enabled}")
  private Boolean enabledPrintStackTrace;
  
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Map<String, Object>> handle(BusinessException ex) {
    return ResponseEntity.status(ex.getStatus()).body(
        Map.of("success", false, "code", ex.getCode(), "message", ex.getMessage(), "timestamp",
            LocalDateTime.now()));
  }
  
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }
  
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(ex.status(), ex.getMessage(), request);
  }
  
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }
  
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiErrorResponse> handleSecurityException(SecurityException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }
  
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
  }
  
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request);
  }
  
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    
    printStackTrace(ex);
    
    Map<String, String> fields = new HashMap<>();
    
    ex.getBindingResult().getFieldErrors()
        .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
    
    ApiErrorResponse response = ApiErrorResponse.builder().timestamp(OffsetDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value()).error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message("Validation failed").path(request.getRequestURI()).details(fields).build();
    
    return ResponseEntity.badRequest().body(response);
  }
  
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }
  
  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      HttpMessageConversionException.class,
      MethodArgumentTypeMismatchException.class,
      MissingRequestHeaderException.class
  })
  public ResponseEntity<ApiErrorResponse> handleBadRequestPayload(Exception ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request payload or parameter", request);
  }
  
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
        "Uploaded file is too large. Please reduce the image size and try again.", request);
  }
  
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
  }
  
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex,
      HttpServletRequest request) {
    printStackTrace(ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
  }
  
  private void printStackTrace(Throwable e) {
    if (Boolean.TRUE.equals(enabledPrintStackTrace)) {
      e.printStackTrace();
    }
  }
  
  private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
      HttpServletRequest request) {
    ApiErrorResponse response = ApiErrorResponse.builder().timestamp(OffsetDateTime.now())
        .status(status.value()).error(status.getReasonPhrase()).message(message)
        .path(request.getRequestURI()).build();
    
    return ResponseEntity.status(status).body(response);
  }
}
