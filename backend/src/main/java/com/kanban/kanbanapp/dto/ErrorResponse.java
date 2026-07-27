package com.kanban.kanbanapp.dto;

import java.time.LocalDateTime;

/**
 * Response DTO representing a standardized API error body.
 *
 * @param timestamp the time the error occurred
 * @param status    the HTTP status code
 * @param error     the short error label (e.g. "Unauthorized")
 * @param message   a human-readable description of the error
 * @param path      the request URI that produced the error
 */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
}
