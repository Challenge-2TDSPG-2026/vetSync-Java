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

    // ───────────── mensagens repetidas em mais de um ponto ─────────────

    public static ResponseStatusException periodoInvertido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "'de' não pode ser depois de 'ate'");
    }

    public static ResponseStatusException arquivoObrigatorio() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo é obrigatório");
    }

    public static ResponseStatusException codigoInvalidoOuExpirado() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
    }

    public static ResponseStatusException dataFimAnteriorAoInicio() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de fim deve ser igual ou depois da data de início");
    }

    public static ResponseStatusException diaSemanaInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)");
    }

    public static ResponseStatusException emailObrigatorio() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
    }

    public static ResponseStatusException formatoImagemInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem inválido. Envie um arquivo JPEG, PNG ou WEBP");
    }

    public static ResponseStatusException horarioInicioDepoisDoFim() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário de início deve ser antes do horário de fim");
    }

    public static ResponseStatusException imagemIlegivel() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado");
    }

    public static ResponseStatusException emailJaCadastrado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    }

    public static ResponseStatusException acessoJaRevogado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Esse acesso já está revogado");
    }

    public static ResponseStatusException servicoIndisponivel() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Serviço indisponível");
    }

    public static ResponseStatusException servicoJaCadastradoNaClinica() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Serviço já cadastrado nesta clínica");
    }

    public static ResponseStatusException cargoOutraClinica() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Cargo de outra clínica");
    }

    public static ResponseStatusException clinicaContratoInativo() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Clínica com contrato inativo");
    }

    public static ResponseStatusException cargoSemPermissao() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu cargo não permite esta ação");
    }

    public static ResponseStatusException semPermissaoParaAgendarNoPet() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para agendar eventos para esse pet");
    }

    public static ResponseStatusException acessoNaoEncontradoParaPet() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Acesso não encontrado para esse pet");
    }

    public static ResponseStatusException logoNaoCadastrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo não cadastrada");
    }

    public static ResponseStatusException responsavelNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado");
    }

    public static ResponseStatusException usuarioNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
    }

    public static ResponseStatusException arquivoExcedeLimite() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite de 10 MB");
    }

    public static ResponseStatusException autenticacaoNecessaria() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
    }

    public static ResponseStatusException emailOuSenhaInvalidos() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }
}
