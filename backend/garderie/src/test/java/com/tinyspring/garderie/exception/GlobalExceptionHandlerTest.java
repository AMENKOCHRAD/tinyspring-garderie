package com.tinyspring.garderie.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Demande introuvable");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertErrorResponse(response.getBody(), 404, "Not Found", List.of("Demande introuvable"));
    }

    @Test
    void shouldHandleBusinessException() {
        BusinessException exception = new BusinessException("Operation interdite");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusiness(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorResponse(response.getBody(), 400, "Bad Request", List.of("Operation interdite"));
    }

    @Test
    void shouldHandleValidationException() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationTarget(), "request");
        bindingResult.addError(new FieldError("request", "email", "ne doit pas etre vide"));
        bindingResult.addError(new FieldError("request", "password", "doit contenir au moins 8 caracteres"));

        Method method = ValidationTarget.class.getDeclaredMethod("validate", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorResponse(
                response.getBody(),
                400,
                "Bad Request",
                List.of(
                        "email : ne doit pas etre vide",
                        "password : doit contenir au moins 8 caracteres"
                )
        );
    }

    @Test
    void shouldHandleUnexpectedException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertErrorResponse(response.getBody(), 500, "Internal Server Error", List.of("Une erreur interne est survenue"));
    }

    private void assertErrorResponse(ApiErrorResponse body, int status, String error, List<String> details) {
        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertTrue(body.timestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertEquals(status, body.status());
        assertEquals(error, body.error());
        assertEquals(details, body.details());
    }

    static class ValidationTarget {
        void validate(String value) {
        }
    }
}
