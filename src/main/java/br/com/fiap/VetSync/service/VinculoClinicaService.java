package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VinculoClinicaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DURACAO_SESSAO_MINUTOS = 15;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ClinicaRepository clinicaRepository;
    private final TutorRepository tutorRepository;
    private final EventoSaudeRepository eventoRepository;
    private final CodigoVinculoClinicaRepository codigoRepository;
    private final SessaoVinculoClinicaRepository sessaoRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;

    public record CodigoEmitido(String codigo, Clinica clinica) {}
    public record CodigoValidado(String sessaoVinculo, Clinica clinica, LocalDateTime expiraEm) {}

    public List<Clinica> listarClinicas() {
        return clinicaRepository.findAll(Sort.by(Sort.Direction.ASC, "nmClinica"));
    }

    @Transactional
    public CodigoEmitido emitirCodigo(Long idClinica) {
        Clinica clinica = buscarClinica(idClinica);
        codigoRepository.findAll().stream().filter(c -> c.getClinica().getIdClinica().equals(idClinica) && c.estaAtivo())
                .forEach(c -> { c.setStAtivo("I"); c.setDtRevogacao(LocalDateTime.now()); });
        String codigo = gerarSegredo(18);
        codigoRepository.save(CodigoVinculoClinica.builder().clinica(clinica).dsCodigoHash(hash(codigo)).build());
        return new CodigoEmitido(codigo, clinica);
    }

    @Transactional
    public void definirContrato(Long idClinica, boolean ativo) {
        Clinica clinica = buscarClinica(idClinica);
        clinica.setStContratante(ativo ? "A" : "I");
        clinicaRepository.save(clinica);
    }

    @Transactional
    public CodigoValidado validarCodigo(String codigoInformado) {
        CodigoVinculoClinica codigo = codigoRepository.findByDsCodigoHash(hash(normalizarCodigo(codigoInformado)))
                .orElseThrow(() -> erroCodigo("Código da clínica inválido"));
        if (!codigo.estaAtivo()) throw erroCodigo("Este código da clínica foi revogado");
        if (!codigo.getClinica().estaContratanteAtiva()) throw erroCodigo("Esta clínica não está com contrato ativo");
        String token = gerarSegredo(32);
        LocalDateTime expiraEm = LocalDateTime.now().plusMinutes(DURACAO_SESSAO_MINUTOS);
        sessaoRepository.save(SessaoVinculoClinica.builder().clinica(codigo.getClinica())
                .dsTokenHash(hash(token)).dtExpiracao(expiraEm).build());
        return new CodigoValidado(token, codigo.getClinica(), expiraEm);
    }

    @Transactional
    public Tutor criarTutorComVinculo(Tutor tutor, String sessaoVinculo) {
        Clinica clinica = consumirSessao(sessaoVinculo);
        Tutor salvo = tutorRepository.save(tutor);
        vinculoRepository.save(VinculoTutorClinica.builder().tutor(salvo).clinica(clinica).build());
        return salvo;
    }

    @Transactional
    public VinculoTutorClinica trocarVinculo(String emailTutor, String sessaoVinculo) {
        Tutor tutor = tutorRepository.findByDsEmail(emailTutor).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        if (eventoRepository.existsByPet_Tutor_IdTutorAndDsStatus(tutor.getIdTutor(), StatusEvento.AGENDADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conclua ou cancele todos os atendimentos agendados antes de trocar de clínica");
        }
        Clinica novaClinica = consumirSessao(sessaoVinculo);
        VinculoTutorClinica atual = vinculoRepository.findAtivoPorTutorParaAtualizacao(tutor.getIdTutor()).orElse(null);
        if (atual != null && atual.getClinica().getIdClinica().equals(novaClinica.getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está vinculado a esta clínica");
        }
        if (atual != null) { atual.setDtEncerramento(LocalDateTime.now()); vinculoRepository.save(atual); }
        return vinculoRepository.save(VinculoTutorClinica.builder().tutor(tutor).clinica(novaClinica).build());
    }

    public VinculoTutorClinica buscarVinculoAtivo(Long idTutor) { return vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(idTutor).orElse(null); }
    public boolean temVinculoAtivo(Long idTutor) { return vinculoRepository.existsAtivoEmClinicaContratante(idTutor); }

    public void exigirClinicaAtivaDoTutor(Long idTutor, Long idClinica) {
        VinculoTutorClinica vinculo = buscarVinculoAtivo(idTutor);
        if (vinculo == null || !vinculo.getClinica().estaContratanteAtiva() || !vinculo.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O atendimento deve ser realizado na clínica vinculada ao tutor");
        }
    }

    private Clinica consumirSessao(String token) {
        if (token == null || token.isBlank()) throw erroCodigo("Confirme o código da clínica antes de continuar");
        SessaoVinculoClinica sessao = sessaoRepository.findByDsTokenHash(hash(token)).orElseThrow(() -> erroCodigo("A confirmação do código é inválida ou expirou"));
        if (!sessao.estaDisponivel()) throw erroCodigo("A confirmação do código expirou. Leia ou informe o código novamente");
        if (!sessao.getClinica().estaContratanteAtiva()) throw erroCodigo("Esta clínica não está com contrato ativo");
        sessao.setDtUtilizacao(LocalDateTime.now());
        sessaoRepository.save(sessao);
        return sessao.getClinica();
    }

    private Clinica buscarClinica(Long id) { return clinicaRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada")); }
    private ResponseStatusException erroCodigo(String mensagem) { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensagem); }
    private String gerarSegredo(int tamanho) { StringBuilder valor = new StringBuilder(tamanho); for (int i = 0; i < tamanho; i++) valor.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length()))); return valor.toString(); }
    private String normalizarCodigo(String codigo) { return codigo == null ? "" : codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(); }
    private String hash(String valor) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível", ex); } }
}
