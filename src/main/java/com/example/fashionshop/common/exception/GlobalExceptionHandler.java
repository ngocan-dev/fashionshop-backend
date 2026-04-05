package com.example.fashionshop.common.exception;

import com.example.fashionshop.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<ApiResponse<Object>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(HomeDataLoadException.class)
    public ResponseEntity<ApiResponse<Object>> handleHomeDataLoad(HomeDataLoadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProductDetailLoadException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductDetailLoad(ProductDetailLoadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProductListLoadException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductListLoad(ProductListLoadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }


    @ExceptionHandler(OrderDetailLoadException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderDetailLoad(OrderDetailLoadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(DashboardLoadException.class)
    public ResponseEntity<ApiResponse<Object>> handleDashboardLoad(DashboardLoadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAccountDeletionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidAccountDeletion(InvalidAccountDeletionException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountDeletionException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountDeletionFailure(AccountDeletionException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CustomerAccountRetrievalException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomerAccountRetrieval(CustomerAccountRetrievalException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountCreationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountCreation(AccountCreationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationSystemException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationSystem(AuthenticationSystemException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid order id"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        boolean hasMissingRequiredField = false;
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            if ("NotBlank".equals(error.getCode())) {
                hasMissingRequiredField = true;
            }
        }
        if (hasMissingRequiredField) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please fill in all required fields"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected error: " + ex.getMessage()));
    }
}
