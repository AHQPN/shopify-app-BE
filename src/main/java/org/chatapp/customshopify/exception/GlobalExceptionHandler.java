package org.chatapp.customshopify.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumKey);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedExceptions(AccessDeniedException ex) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(HttpStatus.FORBIDDEN.value());
        apiResponse.setMessage(HttpStatus.FORBIDDEN.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);

    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(ex.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);

    }

    // Bắt RuntimeException nói chung
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED.getCode());
        apiResponse.setMessage(ex.getMessage());

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED.getHttpStatus()).body(apiResponse);
    }

    @ExceptionHandler(UnsupportedEncodingException.class)
    public ResponseEntity<String> handleUnsupportedEncodingException(UnsupportedEncodingException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Email encoding error: " + ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleInvalidEnumValue(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {

            if (invalidFormatException.getTargetType().isEnum()) {
                ApiResponse response = new ApiResponse();
                ErrorCode errorCode = ErrorCode.INVALID_ACTION;
                response.setCode(errorCode.getCode());
                response.setMessage(errorCode.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(ErrorCode.UNCATEGORIZED.getCode());
        apiResponse.setMessage("Lỗi đọc dữ liệu: " + ex.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
