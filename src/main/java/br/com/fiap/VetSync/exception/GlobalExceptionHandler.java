package br.com.fiap.VetSync.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErroResponse(LocalDateTime timestamp, int status, String erro, String mensagem) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(new ErroResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), ex.getReason()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("erro", "Dados inválidos");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String property = violation.getPropertyPath().toString();
            campos.put(property, violation.getMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("erro", "Restrições violadas");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Corpo da requisição inválido",
                "O corpo da requisição é inválido, malformado ou contém valores de tipos incompatíveis."
        ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> handleMissingRequestPart(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Arquivo obrigatório ausente",
                "Envie o arquivo no campo '" + ex.getRequestPartName() + "'."
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Parâmetro inválido",
                "O parâmetro '" + ex.getName() + "' recebeu um valor incompatível: " + ex.getValue()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.CONFLICT.value(), "Conflito de integridade",
                "A operação violou uma restrição de integridade de dados."
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Argumento inválido",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErroResponse(
                LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "Acesso negado",
                "Você não tem permissão para executar esta ação."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        return ResponseEntity.internalServerError().body(new ErroResponse(
                LocalDateTime.now(), 500, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente."
        ));
    }
}
