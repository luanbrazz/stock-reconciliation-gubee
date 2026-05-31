package com.gubee.stock.infrastructure.web.controller;

import com.gubee.stock.domain.exception.BusinessException;
import com.gubee.stock.domain.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String erro,
            String mensagem,
            List<String> detalhes
    ) {
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            BusinessException ex, Locale locale) {
        String mensagem = messageSource.getMessage(
                ex.getMessageKey(), ex.getArgs(), locale);
        log.warn("Stock not found: {}", mensagem);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 404, "Not Found", mensagem, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        log.warn("Validation error: {}", detalhes);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 400, "Bad Request",
                        "Validation failed", detalhes));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 400, "Bad Request",
                        ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        LocalDateTime.now(), 500, "Internal Server Error",
                        "An unexpected error occurred", null));
    }
}