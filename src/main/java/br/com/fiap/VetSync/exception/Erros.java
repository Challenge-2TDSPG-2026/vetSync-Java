package br.com.fiap.VetSync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fábrica das exceções HTTP que se repetem pelos services e controllers, para manter status e
 * mensagem em um lugar só. Os métodos devolvem a exceção (não a lançam), então servem tanto em
 * {@code orElseThrow(Erros::petNaoEncontrado)} quanto em {@code throw Erros.petNaoEncontrado()}.
 */
public final class Erros {

    private Erros() {
    }

    // ───────────── 404: cadastro não encontrado ─────────────

    public static ResponseStatusException tutorNaoEncontrado() {
        return naoEncontrado("Tutor não encontrado");
    }

    public static ResponseStatusException tutorAutenticadoNaoEncontrado() {
        return naoEncontrado("Tutor autenticado não encontrado");
    }

    public static ResponseStatusException veterinarioNaoEncontrado() {
        return naoEncontrado("Veterinário não encontrado");
    }

    public static ResponseStatusException veterinarioAutenticadoNaoEncontrado() {
        return naoEncontrado("Veterinário autenticado não encontrado");
    }

    public static ResponseStatusException adminNaoEncontrado() {
        return naoEncontrado("Admin não encontrado");
    }

    public static ResponseStatusException adminAutenticadoNaoEncontrado() {
        return naoEncontrado("Admin autenticado não encontrado");
    }

    public static ResponseStatusException clinicaNaoEncontrada() {
        return naoEncontrado("Clínica não encontrada");
    }

    public static ResponseStatusException petNaoEncontrado() {
        return naoEncontrado("Pet não encontrado");
    }

    public static ResponseStatusException servicoNaoEncontrado() {
        return naoEncontrado("Serviço não encontrado");
    }

    public static ResponseStatusException conviteNaoEncontrado() {
        return naoEncontrado("Convite não encontrado");
    }

    public static ResponseStatusException cargoNaoEncontrado() {
        return naoEncontrado("Cargo não encontrado");
    }

    public static ResponseStatusException disponibilidadeNaoEncontrada() {
        return naoEncontrado("Disponibilidade não encontrada");
    }

    public static ResponseStatusException bloqueioNaoEncontrado() {
        return naoEncontrado("Bloqueio não encontrado");
    }

    public static ResponseStatusException acessoNaoEncontrado() {
        return naoEncontrado("Acesso não encontrado");
    }

    public static ResponseStatusException eventoNaoEncontrado(Long id) {
        return naoEncontrado("Evento não encontrado com id: " + id);
    }

    public static ResponseStatusException anexoNaoEncontrado() {
        return naoEncontrado("Anexo não encontrado");
    }

    // ───────────── 403: conta sem o vínculo exigido ─────────────

    public static ResponseStatusException contaDeTutorNecessaria() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de tutor necessária");
    }

    public static ResponseStatusException tutorSemClinicaVinculada() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada");
    }

    /** Veterinário do token sem cadastro: responde 403 (e não 404) para não revelar a existência de contas. */
    public static ResponseStatusException acessoNegadoVeterinarioNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Veterinário não encontrado");
    }

    private static ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
