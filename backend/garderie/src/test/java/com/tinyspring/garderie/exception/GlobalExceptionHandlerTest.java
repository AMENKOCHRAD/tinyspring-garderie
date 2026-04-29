package com.tinyspring.garderie.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleIllegalArgumentException() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Operation interdite"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertError(response.getBody(), "Operation interdite");
    }

    @Test
    void shouldHandleValidationException() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationTarget(), "request");
        bindingResult.addError(new FieldError("request", "email", "ne doit pas etre vide"));
        bindingResult.addError(new FieldError("request", "password", "doit contenir au moins 8 caracteres"));

        Method method = ValidationTarget.class.getDeclaredMethod("validate", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertError(response.getBody(), "ne doit pas etre vide");
    }

    private void assertError(Map<String, String> body, String error) {
        assertNotNull(body);
        assertEquals(error, body.get("error"));
    }

    static class ValidationTarget {
        void validate(String value) {
        }
    }
}
