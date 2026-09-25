package br.com.fiap.VetSync.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException retornando 400 e mapa de campos")
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "nome", "Nome é obrigatório");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> res = handler.handleValidation(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, String> campos = (Map<String, String>) res.getBody().get("campos");
        assertThat(campos).containsEntry("nome", "Nome é obrigatório");
    }

    @Test
    @DisplayName("Deve tratar ResponseStatusException com o status e mensagem informados")
    void handleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleResponseStatus(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().status()).isEqualTo(404);
        assertThat(res.getBody().mensagem()).isEqualTo("Pet não encontrado");
    }

    @Test
    @DisplayName("Deve tratar ConstraintViolationException retornando 400")
    void handleConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Formato inválido");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<Map<String, Object>> res = handler.handleConstraintViolation(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> campos = (Map<String, String>) res.getBody().get("campos");
        assertThat(campos).containsEntry("email", "Formato inválido");
    }

    @Test
    @DisplayName("Deve tratar HttpMessageNotReadableException retornando 400")
    void handleNotReadable() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleNotReadable(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().mensagem()).contains("O corpo da requisição é inválido");
    }

    @Test
    void handleMissingRequestPart() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("foto");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleMissingRequestPart(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().mensagem()).isEqualTo("Envie o arquivo no campo 'foto'.");
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentTypeMismatchException retornando 400")
    void handleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("idPet");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleTypeMismatch(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().mensagem()).contains("idPet").contains("abc");
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException retornando 409 Conflict")
    void handleDataIntegrity() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleDataIntegrity(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().erro()).isEqualTo("Conflito de integridade");
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException retornando 400")
    void handleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleIllegalArgument(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().mensagem()).isEqualTo("Argumento inválido");
    }

    @Test
    @DisplayName("Deve tratar AccessDeniedException retornando 403 Forbidden")
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Acesso negado");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleAccessDenied(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Deve tratar Exception genérica retornando 500 sem vazar detalhes internos")
    void handleGenerico() {
        Exception ex = new NullPointerException("Null reference inside internal logic");

        ResponseEntity<GlobalExceptionHandler.ErroResponse> res = handler.handleGenerico(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().status()).isEqualTo(500);
        assertThat(res.getBody().mensagem()).isEqualTo("Ocorreu um erro inesperado. Tente novamente.");
    }
}
