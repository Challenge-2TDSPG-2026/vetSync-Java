package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.PetConviteService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Erros do fluxo de convite, no formato {"message": "..."}. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {PetConviteController.class, ResponsavelController.class})
public class ConviteExceptionHandler {

    @ExceptionHandler(PetConviteService.ConviteException.class)
    public ResponseEntity<Map<String, String>> handleConviteException(PetConviteService.ConviteException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
