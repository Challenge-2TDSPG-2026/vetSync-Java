package br.com.fiap.VetSync.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrosTest {

    @Test
    @DisplayName("Cadastros não encontrados respondem 404 com a mensagem esperada")
    void naoEncontrado_Cadastros_Retorna404ComMensagem() {
        assertErro(Erros.petNaoEncontrado(), HttpStatus.NOT_FOUND, "Pet não encontrado");
        assertErro(Erros.clinicaNaoEncontrada(), HttpStatus.NOT_FOUND, "Clínica não encontrada");
        assertErro(Erros.tutorAutenticadoNaoEncontrado(), HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado");
        assertErro(Erros.eventoNaoEncontrado(7L), HttpStatus.NOT_FOUND, "Evento não encontrado com id: 7");
    }

    @Test
    @DisplayName("Conta sem o vínculo exigido responde 403 com a mensagem esperada")
    void proibido_ContaSemVinculo_Retorna403ComMensagem() {
        assertErro(Erros.contaDeTutorNecessaria(), HttpStatus.FORBIDDEN, "Conta de tutor necessária");
        assertErro(Erros.tutorSemClinicaVinculada(), HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada");
        assertErro(Erros.acessoNegadoVeterinarioNaoEncontrado(), HttpStatus.FORBIDDEN, "Veterinário não encontrado");
    }

    private static void assertErro(ResponseStatusException erro, HttpStatus status, String mensagem) {
        assertThat(erro.getStatusCode().value()).isEqualTo(status.value());
        assertThat(erro.getReason()).isEqualTo(mensagem);
    }
}
