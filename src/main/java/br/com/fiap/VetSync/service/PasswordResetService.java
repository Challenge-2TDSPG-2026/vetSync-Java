package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.security.CodigoRedefinicaoStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Regras de negócio do fluxo "Esqueci minha senha":
 * 1) solicitarCodigo  -> gera um código de 6 dígitos e envia por e-mail
 * 2) validarCodigo    -> (opcional, usado pelo front para avançar de tela) confere o código sem gastá-lo
 * 3) redefinirSenha   -> confere o código novamente e efetiva a nova senha
 *
 * Funciona para os 4 tipos de usuário do sistema (Tutor, Veterinário,
 * Profissional de Estética e Admin), pois todos têm ds_email/ds_senha.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CodigoRedefinicaoStore codigoStore;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.reset-senha.expiracao-minutos:15}")
    private int expiracaoMinutos;

    private static final int SENHA_MIN_LENGTH = 6;

    /** Passo 1: se o e-mail existir, gera e envia o código de verificação. */
    public void solicitarCodigo(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
        }
        String emailNormalizado = email.trim().toLowerCase();

        boolean existe = tutorRepository.existsByDsEmail(emailNormalizado)
                || veterinarioRepository.findByDsEmail(emailNormalizado).isPresent()
                || profissionalEsteticaRepository.existsByDsEmail(emailNormalizado)
                || adminRepository.findByDsEmail(emailNormalizado).isPresent();

        // Por segurança, a resposta ao front é sempre igual, exista ou não o e-mail —
        // isso evita que alguém descubra quais e-mails estão cadastrados na base.
        if (!existe) {
            log.info("Pedido de redefinição de senha para e-mail não cadastrado: {}", emailNormalizado);
            return;
        }

        String codigo = gerarCodigo();
        codigoStore.salvar(emailNormalizado, codigo, LocalDateTime.now().plusMinutes(expiracaoMinutos));

        String assunto = "VetSync - Código para redefinição de senha";
        String corpo = """
                Olá!

                Recebemos uma solicitação para redefinir a senha da sua conta VetSync.

                Seu código de verificação é: %s

                Esse código é válido por %d minutos. Se você não solicitou a redefinição de senha, apenas ignore este e-mail.

                Equipe VetSync
                """.formatted(codigo, expiracaoMinutos);

        emailService.enviar(emailNormalizado, assunto, corpo);
    }

    /** Passo 2 (opcional): confere se o código informado é válido, sem consumi-lo. */
    public void validarCodigo(String email, String codigo) {
        buscarRegistroValido(email, codigo);
    }

    /** Passo 3: confere o código novamente e efetiva a troca de senha. */
    public void redefinirSenha(String email, String codigo, String novaSenha, String confirmarSenha) {
        if (novaSenha == null || novaSenha.length() < SENHA_MIN_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A nova senha deve ter pelo menos " + SENHA_MIN_LENGTH + " caracteres");
        }
        if (!novaSenha.equals(confirmarSenha)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem");
        }

        String emailNormalizado = buscarRegistroValido(email, codigo);
        String senhaCodificada = passwordEncoder.encode(novaSenha);

        boolean atualizado = tutorRepository.findByDsEmail(emailNormalizado)
                .map(t -> { t.setDsSenha(senhaCodificada); tutorRepository.save(t); return true; })
                .orElse(false);

        if (!atualizado) {
            atualizado = veterinarioRepository.findByDsEmail(emailNormalizado)
                    .map(v -> { v.setDsSenha(senhaCodificada); veterinarioRepository.save(v); return true; })
                    .orElse(false);
        }
        if (!atualizado) {
            atualizado = profissionalEsteticaRepository.findByDsEmail(emailNormalizado)
                    .map(p -> { p.setDsSenha(senhaCodificada); profissionalEsteticaRepository.save(p); return true; })
                    .orElse(false);
        }
        if (!atualizado) {
            atualizado = adminRepository.findByDsEmail(emailNormalizado)
                    .map(a -> { a.setDsSenha(senhaCodificada); adminRepository.save(a); return true; })
                    .orElse(false);
        }

        if (!atualizado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        codigoStore.remover(emailNormalizado);
        log.info("Senha redefinida com sucesso para: {}", emailNormalizado);
    }

    private String buscarRegistroValido(String email, String codigo) {
        if (email == null || email.isBlank() || codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail e código são obrigatórios");
        }
        String emailNormalizado = email.trim().toLowerCase();

        if (codigoStore.excedeuTentativas(emailNormalizado)) {
            codigoStore.remover(emailNormalizado);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Número máximo de tentativas excedido. Solicite um novo código.");
        }

        CodigoRedefinicaoStore.Registro registro = codigoStore.buscar(emailNormalizado);
        if (registro == null || registro.expiraEm().isBefore(LocalDateTime.now())) {
            codigoStore.remover(emailNormalizado);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
        }
        if (!registro.codigo().equals(codigo.trim())) {
            codigoStore.registrarTentativaFalha(emailNormalizado);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
        }
        return emailNormalizado;
    }

    private String gerarCodigo() {
        int numero = 100000 + random.nextInt(900000); // sempre 6 dígitos: 100000 a 999999
        return String.valueOf(numero);
    }
}