package com.kanban.kanbanapp.exception;

import com.kanban.kanbanapp.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Centralized exception handler that converts exceptions thrown anywhere in
 * the application into a consistent JSON {@link ErrorResponse} body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle an expired JWT access/refresh token.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 401 with an ErrorResponse body
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(
            ExpiredJwtException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "JWT token has expired",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle a JWT that cannot be parsed.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 400 with an ErrorResponse body
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(
            MalformedJwtException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Malformed JWT token",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle a JWT with an invalid cryptographic signature.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 401 with an ErrorResponse body
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(
            SignatureException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid JWT signature",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle any other Spring Security authentication failure.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 401 with an ErrorResponse body
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentication failed: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle an authorization failure (authenticated but not permitted).
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 403 with an ErrorResponse body
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Access denied",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle invalid login credentials.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 401 with an ErrorResponse body
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid email or password",
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle an attempt to add or move a task past a column's WIP limit.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 409 with an ErrorResponse body
     */
    @ExceptionHandler(WipLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleWipLimitExceededException(
            WipLimitExceededException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Fallback handler for any unhandled runtime exception.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return 500 with an ErrorResponse body
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle a {@link ResponseStatusException} raised explicitly by the
     * application code, preserving its status code and reason.
     *
     * @param ex      the thrown exception
     * @param request the current web request
     * @return the exception's status code with an ErrorResponse body
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
                ResponseStatusException ex, WebRequest request) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                ex.getStatusCode().value(),
                reason,
                reason,
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, ex.getStatusCode());
}

    /**
     * Thrown when a login attempt targets an account that has been locked
     * due to repeated failed attempts.
     */
    public class AccountLockedException extends RuntimeException {
        /**
         * @param message the error message
         */
        public AccountLockedException(String message) {
                super(message);
        }

        /**
         * @param message    the error message
         * @param unlockTime the time at which the account will automatically unlock
         */
        public AccountLockedException(String message, Instant unlockTime) {
                super(message + " Unlock time: " + unlockTime);
        }
    }
}