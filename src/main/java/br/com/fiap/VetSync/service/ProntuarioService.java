package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Monta o prontuário clínico do pet reunindo atendimentos, orientações, receitas e resultados de exames.
 * Não decide quem pode ver: a autorização é feita antes (controller / link compartilhado).
 *
 * <p>Por privacidade, o prontuário NÃO inclui custos, observações do tutor nem dados pessoais do tutor
 * (nome, telefone, e-mail, endereço, contato de emergência).</p>
 */
@Service
@RequiredArgsConstructor
public class ProntuarioService {

    public static final int VERSAO_FORMATO = 1;

    private final PetService petService;
    private final PerfilSaudeRepository perfilSaudeRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final OrientacaoClinicaRepository orientacaoRepository;
    private final PrescricaoRepository prescricaoRepository;
    private final ResultadoExameRepository exameRepository;

    // ------------------------------------------------------------------ DTOs

    public record PetResumo(Long id, String numero, String nome, String especie, String raca, String sexo,
                            LocalDate nascimento) {}

    public record PerfilResumo(BigDecimal pesoAtual, LocalDate pesoAtualizadoEm, String alergias,
                               String medicamentosContinuos, String restricoesAlimentares,
                               String condicoesPreExistentes, String observacoesImportantes) {}

    public record Atendimento(Long id, LocalDate data, String hora, String tipo, String categoria,
                              String veterinario, String crmv, String clinica,
                              String diagnostico, String conduta, String observacaoClinica) {}

    public record Orientacao(Long id, Long eventoId, LocalDate dataAtendimento, String titulo, String texto,
                             String autor, LocalDateTime criadaEm) {}

    public record Receita(Long id, Long eventoId, String medicamento, String principioAtivo, String posologia,
                          LocalDate inicio, LocalDate fim, Integer dosesPorDia, String status) {}

    public record Exame(Long id, Long eventoId, String nome, String laboratorio, LocalDate coletadoEm,
                        LocalDate resultadoEm, String resultado, String interpretacao, String veterinario,
                        boolean temArquivo, String arquivoNome) {}

    public record EntradaLinhaDoTempo(String tipo, Long id, LocalDate data, String titulo, Long eventoId) {}

    public record Prontuario(int versao, LocalDateTime geradoEm, Set<SecaoProntuario> secoes,
                             LocalDate periodoInicio, LocalDate periodoFim, PetResumo pet,
                             PerfilResumo perfilSaude, List<Atendimento> atendimentos,
                             List<Orientacao> orientacoes, List<Receita> receitas, List<Exame> exames,
                             List<EntradaLinhaDoTempo> linhaDoTempo) {}

    // ------------------------------------------------------------------ montagem

    /** Todas as seções, quando o chamador não restringe. */
    public static Set<SecaoProntuario> todasAsSecoes() {
        return EnumSet.allOf(SecaoProntuario.class);
    }

    /**
     * @param apenasReceitasLiberadas {@code true} para o que sai do sistema (link/exportação): receitas ainda
     *                                SOLICITADAS ou NEGADAS pela clínica não fazem parte do prontuário externo.
     */
    @Transactional(readOnly = true)
    public Prontuario montar(Long idPet, Set<SecaoProntuario> secoes, LocalDate de, LocalDate ate,
                             boolean apenasReceitasLiberadas) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'de' não pode ser depois de 'ate'");
        }
        Set<SecaoProntuario> escopo = (secoes == null || secoes.isEmpty())
                ? todasAsSecoes() : EnumSet.copyOf(secoes);
        Pet pet = petService.buscarPorId(idPet);

        PerfilResumo perfil = escopo.contains(SecaoProntuario.PERFIL_SAUDE) ? perfil(idPet) : null;
        List<Atendimento> atendimentos = escopo.contains(SecaoProntuario.ATENDIMENTOS)
                ? atendimentos(idPet, de, ate) : List.of();
        List<Orientacao> orientacoes = escopo.contains(SecaoProntuario.ORIENTACOES)
                ? orientacoes(idPet, de, ate) : List.of();
        List<Receita> receitas = escopo.contains(SecaoProntuario.RECEITAS)
                ? receitas(idPet, de, ate, apenasReceitasLiberadas) : List.of();
        List<Exame> exames = escopo.contains(SecaoProntuario.EXAMES) ? exames(idPet, de, ate) : List.of();

        return new Prontuario(VERSAO_FORMATO, LocalDateTime.now(), escopo, de, ate, resumoDoPet(pet), perfil,
                atendimentos, orientacoes, receitas, exames,
                linhaDoTempo(atendimentos, orientacoes, receitas, exames));
    }

    private PetResumo resumoDoPet(Pet pet) {
        String especie = pet.getRaca() != null && pet.getRaca().getEspecie() != null
                ? pet.getRaca().getEspecie().getNmEspecie() : null;
        String raca = pet.getRaca() != null ? pet.getRaca().getNmRaca() : null;
        return new PetResumo(pet.getIdPet(), pet.numeroFormatado(), pet.getNmPet(), especie, raca,
                pet.getDsSexo(), pet.getDtNascimento());
    }

    private PerfilResumo perfil(Long idPet) {
        return perfilSaudeRepository.findByPet_IdPet(idPet)
                .map(p -> new PerfilResumo(p.getPesoAtual(), p.getPesoAtualizadoEm(), p.getAlergias(),
                        p.getMedicamentosContinuos(), p.getRestricoesAlimentares(),
                        p.getCondicoesPreExistentes(), p.getObservacoesImportantes()))
                .orElse(null);
    }

    /** Só atendimentos CONCLUÍDOS: agendados e cancelados ainda não têm conteúdo clínico. */
    private List<Atendimento> atendimentos(Long idPet, LocalDate de, LocalDate ate) {
        return eventoSaudeRepository
                .findByPet_IdPetAndDsStatusOrderByDtEventoDescIdEventoDesc(idPet, StatusEvento.CONCLUIDO).stream()
                .filter(e -> dentro(e.getDtEvento(), de, ate))
                .map(e -> new Atendimento(e.getIdEvento(), e.getDtEvento(), e.getHrEvento(),
                        e.getTipoEvento() != null ? e.getTipoEvento().getNmTipoEvento() : null,
                        e.getTipoEvento() != null ? e.getTipoEvento().getDsCategoria() : null,
                        e.getVeterinario() != null ? e.getVeterinario().getNmVeterinario() : null,
                        e.getVeterinario() != null ? e.getVeterinario().getNrCrmv() : null,
                        nomeClinica(e), e.getDsDiagnostico(), e.getDsConduta(), e.getDsObservacaoClinica()))
                .toList();
    }

    private List<Orientacao> orientacoes(Long idPet, LocalDate de, LocalDate ate) {
        return orientacaoRepository.findByEvento_Pet_IdPetOrderByDtCriacaoDesc(idPet).stream()
                .filter(o -> dentro(o.getDtCriacao().toLocalDate(), de, ate))
                .map(o -> new Orientacao(o.getIdOrientacao(), o.getEvento().getIdEvento(),
                        o.getEvento().getDtEvento(), o.getDsTitulo(), o.getDsTexto(),
                        o.getVeterinario() != null ? o.getVeterinario().getNmVeterinario() : o.getDsAtor(),
                        o.getDtCriacao()))
                .toList();
    }

    private List<Receita> receitas(Long idPet, LocalDate de, LocalDate ate, boolean apenasLiberadas) {
        return prescricaoRepository.findByEvento_Pet_IdPetOrderByDtInicioDescIdPrescricaoDesc(idPet).stream()
                .filter(p -> !apenasLiberadas || p.getDsStatus() == StatusPrescricao.LIBERADO)
                .filter(p -> dentro(p.getDtInicio(), de, ate))
                .map(p -> new Receita(p.getIdPrescricao(), p.getEvento().getIdEvento(),
                        p.getMedicamento().getNmMedicamento(), p.getMedicamento().getDsPrincipio(),
                        p.getDsPosologia(), p.getDtInicio(), p.getDtFim(), p.getQtDosesDia(),
                        p.getDsStatus().name()))
                .toList();
    }

    private List<Exame> exames(Long idPet, LocalDate de, LocalDate ate) {
        return exameRepository.findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(idPet).stream()
                .filter(x -> dentro(x.getDtResultado(), de, ate))
                .map(this::toExame)
                .toList();
    }

    public Exame toExame(ResultadoExame x) {
        return new Exame(x.getIdResultado(), x.getEvento() != null ? x.getEvento().getIdEvento() : null,
                x.getNmExame(), x.getNmLaboratorio(), x.getDtColeta(), x.getDtResultado(), x.getDsResultado(),
                x.getDsInterpretacao(), x.getVeterinario() != null ? x.getVeterinario().getNmVeterinario() : null,
                x.temArquivo(), x.getNmArquivo());
    }

    private List<EntradaLinhaDoTempo> linhaDoTempo(List<Atendimento> atendimentos, List<Orientacao> orientacoes,
                                                   List<Receita> receitas, List<Exame> exames) {
        List<EntradaLinhaDoTempo> linha = new ArrayList<>();
        atendimentos.forEach(a -> linha.add(new EntradaLinhaDoTempo("ATENDIMENTO", a.id(), a.data(),
                a.tipo(), a.id())));
        orientacoes.forEach(o -> linha.add(new EntradaLinhaDoTempo("ORIENTACAO", o.id(),
                o.criadaEm().toLocalDate(), o.titulo(), o.eventoId())));
        receitas.forEach(r -> linha.add(new EntradaLinhaDoTempo("RECEITA", r.id(), r.inicio(),
                r.medicamento(), r.eventoId())));
        exames.forEach(x -> linha.add(new EntradaLinhaDoTempo("EXAME", x.id(), x.resultadoEm(),
                x.nome(), x.eventoId())));
        linha.sort(Comparator.comparing(EntradaLinhaDoTempo::data, Comparator.nullsLast(Comparator.reverseOrder())));
        return linha;
    }

    private static String nomeClinica(EventoSaude e) {
        if (e.getClinica() != null) return e.getClinica().getNmClinica();
        if (e.getVeterinario() != null && e.getVeterinario().getClinica() != null) {
            return e.getVeterinario().getClinica().getNmClinica();
        }
        return null;
    }

    private static boolean dentro(LocalDate data, LocalDate de, LocalDate ate) {
        if (data == null) return de == null && ate == null;
        return (de == null || !data.isBefore(de)) && (ate == null || !data.isAfter(ate));
    }
}