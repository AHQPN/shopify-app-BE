package org.chatapp.customshopify.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

        // General
        INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Internal server error"),
        UNCATEGORIZED(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Uncategorized error"),
        INVALID_ACTION(HttpStatus.BAD_REQUEST, 9998, "Invalid action"),
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 9997, "Unauthorized"),
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, 9996, "Invalid request"),
        
        // Auth & Shopify
        UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, 1001, "Unauthenticated"),
        SHOP_NOT_FOUND(HttpStatus.NOT_FOUND, 1002, "Shop not found"),
        TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, 1003, "Token exchange failed"),
        INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, 1004, "Invalid JWT token"),
        EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, 1005, "JWT token is expired"),
        UNSUPPORT_TOKEN(HttpStatus.BAD_REQUEST, 1006, "JWT token is unsupported"),
        JWT_CLAIMS_EMPTY(HttpStatus.BAD_REQUEST, 1007, "JWT claims string is empty"),
        INVALID_HMAC(HttpStatus.UNAUTHORIZED, 1008, "Invalid HMAC signature"),
        SHOPIFY_API_ERROR(HttpStatus.BAD_GATEWAY, 1009, "Error calling Shopify API"),
        FILE_TYPE_NOT_SUPPORT(HttpStatus.BAD_REQUEST, 1010, "File type not supported"),
        FILE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, 1011, "File limit exceeded"),
        FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, 1012, "File size exceeded");

        private final HttpStatus httpStatus;
        private final int code;
        private final String message;
}
