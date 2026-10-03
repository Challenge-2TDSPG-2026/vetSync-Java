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
import java.util.Set;

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
    public record ClinicaListada(Clinica clinica, boolean codigoAtivo) {}
    private record SessaoConsumida(Clinica clinica, CodigoVinculoClinica codigo) {}

    public List<ClinicaListada> listarClinicas() {
        Set<Long> comCodigoAtivo = codigoRepository.findIdsClinicasComCodigoAtivo();
        return clinicaRepository.findAll(Sort.by(Sort.Direction.ASC, "nmClinica")).stream()
                .map(c -> new ClinicaListada(c, comCodigoAtivo.contains(c.getIdClinica())))
                .toList();
    }

    @Transactional
    public CodigoEmitido emitirCodigo(Long idClinica) {
        Clinica clinica = buscarClinicaParaAtualizacao(idClinica);
        // Só clínicas com contrato confirmado podem ter código de vínculo.
        if (!clinica.estaContratanteAtiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ative o contrato da clínica antes de emitir um código de vínculo");
        }
        // Emitir outro código revoga o anterior (e as sessões ainda abertas dele).
        revogar(codigoRepository.findByClinica_IdClinicaAndStAtivo(idClinica, "A"));
        String codigo = gerarSegredo(18);
        codigoRepository.saveAndFlush(CodigoVinculoClinica.builder().clinica(clinica).dsCodigoHash(hash(codigo)).build());
        return new CodigoEmitido(codigo, clinica);
    }

    @Transactional
    public void revogarCodigo(Long idClinica) {
        buscarClinicaParaAtualizacao(idClinica);
        List<CodigoVinculoClinica> ativos = codigoRepository.findByClinica_IdClinicaAndStAtivo(idClinica, "A");
        if (ativos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A clínica não possui código ativo para revogar");
        }
        revogar(ativos);
    }

    @Transactional
    public void definirContrato(Long idClinica, boolean ativo) {
        Clinica clinica = buscarClinicaParaAtualizacao(idClinica);
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
        sessaoRepository.save(SessaoVinculoClinica.builder().clinica(codigo.getClinica()).codigoVinculo(codigo)
                .dsTokenHash(hash(token)).dtExpiracao(expiraEm).build());
        return new CodigoValidado(token, codigo.getClinica(), expiraEm);
    }

    @Transactional
    public Tutor criarTutorComVinculo(Tutor tutor, String sessaoVinculo) {
        SessaoConsumida sessao = consumirSessao(sessaoVinculo);
        Tutor salvo = tutorRepository.save(tutor);
        vinculoRepository.save(VinculoTutorClinica.builder().tutor(salvo)
                .clinica(sessao.clinica()).codigoVinculo(sessao.codigo()).build());
        return salvo;
    }

    @Transactional
    public VinculoTutorClinica trocarVinculo(String emailTutor, String sessaoVinculo) {
        Tutor tutor = tutorRepository.findByDsEmail(emailTutor).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        if (eventoRepository.existsByPet_Tutor_IdTutorAndDsStatus(tutor.getIdTutor(), StatusEvento.AGENDADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conclua ou cancele todos os atendimentos agendados antes de trocar de clínica");
        }
        SessaoConsumida sessao = consumirSessao(sessaoVinculo);
        Clinica novaClinica = sessao.clinica();
        VinculoTutorClinica atual = vinculoRepository.findAtivoPorTutorParaAtualizacao(tutor.getIdTutor()).orElse(null);
        if (atual != null && atual.getClinica().getIdClinica().equals(novaClinica.getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está vinculado a esta clínica");
        }
        if (atual != null) { atual.setDtEncerramento(LocalDateTime.now()); vinculoRepository.save(atual); }
        return vinculoRepository.save(VinculoTutorClinica.builder().tutor(tutor)
                .clinica(novaClinica).codigoVinculo(sessao.codigo()).build());
    }

    public VinculoTutorClinica buscarVinculoAtivo(Long idTutor) { return vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(idTutor).orElse(null); }
    public boolean temVinculoAtivo(Long idTutor) { return vinculoRepository.existsAtivoEmClinicaContratante(idTutor); }

    public void exigirClinicaAtivaDoTutor(Long idTutor, Long idClinica) {
        VinculoTutorClinica vinculo = buscarVinculoAtivo(idTutor);
        if (vinculo == null || !vinculo.getClinica().estaContratanteAtiva() || !vinculo.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O atendimento deve ser realizado na clínica vinculada ao tutor");
        }
    }

    /**
     * Consome a sessão de forma atômica (UPDATE condicional). Se qualquer validação posterior falhar,
     * a exceção desfaz a transação inteira e a sessão volta a ficar disponível.
     */
    private SessaoConsumida consumirSessao(String token) {
        if (token == null || token.isBlank()) throw erroCodigo("Confirme o código da clínica antes de continuar");
        String hashToken = hash(token);
        if (sessaoRepository.consumir(hashToken, LocalDateTime.now()) == 0) {
            throw erroCodigo("A confirmação do código é inválida, expirou ou já foi utilizada. Leia ou informe o código novamente");
        }
        SessaoVinculoClinica sessao = sessaoRepository.findByDsTokenHash(hashToken)
                .orElseThrow(() -> erroCodigo("A confirmação do código é inválida ou expirou"));
        CodigoVinculoClinica codigo = sessao.getCodigoVinculo();
        if (codigo == null || !codigo.estaAtivo()) throw erroCodigo("Este código da clínica foi revogado");
        if (!sessao.getClinica().estaContratanteAtiva()) throw erroCodigo("Esta clínica não está com contrato ativo");
        return new SessaoConsumida(sessao.getClinica(), codigo);
    }

    private void revogar(List<CodigoVinculoClinica> codigos) {
        if (codigos.isEmpty()) return;
        LocalDateTime agora = LocalDateTime.now();
        codigos.forEach(c -> { c.setStAtivo("I"); c.setDtRevogacao(agora); });
        codigoRepository.saveAllAndFlush(codigos);
        sessaoRepository.expirarPendentesDosCodigos(codigos, agora);
    }

    private Clinica buscarClinicaParaAtualizacao(Long id) { return clinicaRepository.findByIdParaAtualizacao(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada")); }
    private ResponseStatusException erroCodigo(String mensagem) { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensagem); }
    private String gerarSegredo(int tamanho) { StringBuilder valor = new StringBuilder(tamanho); for (int i = 0; i < tamanho; i++) valor.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length()))); return valor.toString(); }
    private String normalizarCodigo(String codigo) { return codigo == null ? "" : codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(); }
    private String hash(String valor) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível", ex); } }
}