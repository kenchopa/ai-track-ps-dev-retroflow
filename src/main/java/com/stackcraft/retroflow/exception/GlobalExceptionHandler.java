package com.stackcraft.retroflow.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps exceptions to the StackCraft error format (standards 2.2 and 4.1).
 * Messages never contain class names, stack traces or database errors (4.2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String RETRY_AFTER_SECONDS = "60";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiError> handleInvalidInput(InvalidInputException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RetroflowException.class)
    public ResponseEntity<ApiError> handleBusinessRule(RetroflowException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * The summary model failed: 504 timeout, 503 busy or not configured (with Retry-After when busy),
     * 502 failed or unusable answer. The message never contains provider details (4.2).
     */
    @ExceptionHandler(SummaryUnavailableException.class)
    public ResponseEntity<ApiError> handleSummaryUnavailable(SummaryUnavailableException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case RATE_LIMITED, NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
            case PROVIDER_ERROR, INVALID_ANSWER -> HttpStatus.BAD_GATEWAY;
        };
        ResponseEntity<ApiError> response = error(status, ex.getMessage());
        if (ex.getReason() == SummaryUnavailableException.Reason.RATE_LIMITED) {
            return ResponseEntity.status(status).header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS).body(response.getBody());
        }
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String objectName = ex.getBindingResult().getObjectName();
        String subject = entityName(objectName) + " " + identifier(objectName, request);
        String reasons = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, subject + ": " + reasons);
    }

    /**
     * Two concurrent requests can both pass a service-level uniqueness check;
     * the database constraint then catches the second. Never expose its message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return error(HttpStatus.CONFLICT, "Request -: conflicts with existing data");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(HandlerMethodValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request -: invalid request parameter");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request -: invalid value for '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request -: malformed JSON or invalid field value");
    }

    /**
     * Last resort. Spring's own web exceptions (unknown route, wrong method,
     * ...) keep their status code; anything else is an unexpected 500 and is
     * logged, because the client only sees a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return error(status, "Request -: " + HttpStatus.valueOf(status.value()).getReasonPhrase());
        }
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Request -: unexpected server error");
    }

    private static String describe(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }

    /**
     * "createTeamRequest" becomes "Team", "updateFeedbackItemRequest" becomes "FeedbackItem".
     */
    private static String entityName(String objectName) {
        return objectName.replaceFirst("^[a-z]+", "").replaceFirst("Request$", "");
    }

    /**
     * "new" for create requests; otherwise the {id} path variable of the resource being changed.
     */
    @SuppressWarnings("unchecked")
    private static String identifier(String objectName, HttpServletRequest request) {
        if (objectName.startsWith("create")) {
            return "new";
        }
        var pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return pathVariables == null ? "-" : pathVariables.getOrDefault("id", "-");
    }

    private static ResponseEntity<ApiError> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(new ApiError(message, status.value(), Instant.now()));
    }

}
