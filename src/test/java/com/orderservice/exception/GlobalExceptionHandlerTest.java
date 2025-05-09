package com.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void handleDuplicateOrderException_ShouldReturnConflictStatus() {
        DuplicateOrderException exception = new DuplicateOrderException("Pedido duplicado");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleDuplicateOrderException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Pedido duplicado", response.getBody().get("error"));
        assertEquals("Pedido duplicado", response.getBody().get("message"));
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequestStatus() {
        FieldError fieldError1 = new FieldError("orderDTO", "orderNumber", "número de pedido é obrigatório");
        FieldError fieldError2 = new FieldError("orderDTO", "items", "items não podem ser vazios");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(methodArgumentNotValidException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("número de pedido é obrigatório", response.getBody().get("orderNumber"));
        assertEquals("items não podem ser vazios", response.getBody().get("items"));
    }

    @Test
    void handleGeneralExceptions_ShouldReturnInternalServerErrorStatus() {
        Exception exception = new RuntimeException("Erro inesperado");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGeneralExceptions(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro interno do servidor", response.getBody().get("error"));
        assertEquals("Erro inesperado", response.getBody().get("message"));
    }
}