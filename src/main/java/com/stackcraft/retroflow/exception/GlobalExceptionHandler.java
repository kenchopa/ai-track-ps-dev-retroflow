package com.stackcraft.retroflow.exception;

import com.stackcraft.retroflow.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException exception) {
        return error(exception.getMessage(), HttpStatus.NOT_FOUND, new HttpHeaders());
    }

    @ExceptionHandler(RetroflowException.class)
    public ResponseEntity<Object> handleBusinessRule(RetroflowException exception) {
        return error(exception.getMessage(), HttpStatus.CONFLICT, new HttpHeaders());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var message = exception.getBindingResult().getFieldErrors().stream()
            .map(field -> field.getField() + ": " + field.getDefaultMessage())
            .sorted().collect(Collectors.joining("; "));
        return error("Request " + message, HttpStatus.BAD_REQUEST, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            LOG.error("Controller response validation failed", exception);
            return error("Request: an unexpected server error occurred", HttpStatus.INTERNAL_SERVER_ERROR, headers);
        }
        var message = exception.getAllValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(validation -> result.getMethodParameter().getParameterName() + ": "
                    + validation.getDefaultMessage()))
            .sorted().collect(Collectors.joining("; "));
        return error("Request " + message, HttpStatus.BAD_REQUEST, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return error("Request body: malformed JSON, unknown field, or invalid field type/date",
            HttpStatus.BAD_REQUEST, headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var parameter = exception instanceof MethodArgumentTypeMismatchException mismatch
            ? mismatch.getName() : "parameter";
        return error("Request " + parameter + ": invalid format or out-of-range value", HttpStatus.BAD_REQUEST, headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var reason = HttpStatus.valueOf(status.value()).getReasonPhrase();
        return error("Request: " + reason, status, headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        LOG.error("Unhandled request failure", exception);
        return error("Request: an unexpected server error occurred", HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders());
    }

    private ResponseEntity<Object> error(String message, HttpStatusCode status, HttpHeaders headers) {
        var body = new ApiError(message, status.value(), Instant.now());
        return new ResponseEntity<>(body, headers, status);
    }
}
