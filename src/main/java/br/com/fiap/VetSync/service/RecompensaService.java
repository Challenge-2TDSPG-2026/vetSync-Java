package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.RecompensaRepository;
import br.com.fiap.VetSync.repository.ResgateRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecompensaService {

    private final RecompensaRepository recompensaRepository;
    private final ResgateRepository resgateRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final TutorService tutorService;
    private final PontosService pontosService;
    private final ClinicaService clinicaService;
    private final VinculoClinicaService vinculoClinicaService;
    @Autowired(required = false)
    private AuditoriaService auditoriaService;

    private static final long TAMANHO_MAXIMO_IMAGEM_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /** Resultado da exclusão: true = removido do banco; false = apenas inativado (há resgates vinculados). */
    public record ResultadoExclusao(boolean excluidoDefinitivamente) {}

    public Recompensa criar(String nome, String descricao, Integer custoPontos, TipoRecompensa tipo, Long idClinica) {
        return criar(nome, descricao, custoPontos, tipo, idClinica, null);
    }

    @Transactional
    public Recompensa criar(String nome, String descricao, Integer custoPontos, TipoRecompensa tipo,
                            Long idClinica, MultipartFile imagem) {
        Clinica clinica = clinicaService.buscarObrigatoria(idClinica);
        Recompensa.RecompensaBuilder builder = Recompensa.builder()
                .clinica(clinica)
                .nmRecompensa(nome)
                .dsDescricao(descricao)
                .nrCustoPontos(custoPontos)
                .dsTipo(tipo)
                .flAtivo(true);

        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            try {
                builder.dsImagem(imagem.getBytes())
                        .dsImagemTipo(imagem.getContentType());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado");
            }
        }

        Recompensa salva = recompensaRepository.save(builder.build());
        auditarCatalogo(salva, "CATALOGO_ITEM_CRIADO", null, resumo(salva));
        return salva;
    }

    /**
     * Edita os dados da recompensa DA CLÍNICA informada (a clínica da recompensa não pode ser trocada).
     * Recompensas legadas, que ainda não têm clínica, recebem a clínica informada na primeira edição.
     * Se 'imagem' vier preenchida, substitui a foto atual; se 'removerImagem' for true (e nenhuma imagem nova vier),
     * remove a foto; caso contrário, mantém a atual. Resgates já feitos não mudam: guardam a própria cópia.
     */
    @Transactional
    public Recompensa atualizar(Long id, String nome, String descricao, Integer custoPontos, TipoRecompensa tipo,
                                Long idClinica, Boolean ativo, MultipartFile imagem, boolean removerImagem) {
        Recompensa recompensa = buscarPorId(id);
        if (recompensa.getClinica() != null && !recompensa.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Recompensa " + id + " não pertence à clínica " + idClinica);
        }
        Clinica clinica = recompensa.getClinica() != null
                ? recompensa.getClinica()
                : clinicaService.buscarObrigatoria(idClinica);
        String antes = resumo(recompensa);
        recompensa.setClinica(clinica);
        recompensa.setNmRecompensa(nome);
        recompensa.setDsDescricao(descricao);
        recompensa.setNrCustoPontos(custoPontos);
        recompensa.setDsTipo(tipo);
        if (ativo != null) {
            recompensa.setFlAtivo(ativo);
        }

        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            try {
                recompensa.setDsImagem(imagem.getBytes());
                recompensa.setDsImagemTipo(imagem.getContentType());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado");
            }
        } else if (removerImagem) {
            recompensa.setDsImagem(null);
            recompensa.setDsImagemTipo(null);
        }

        Recompensa salva = recompensaRepository.save(recompensa);
        String depois = resumo(salva);
        if (!antes.equals(depois) || (imagem != null && !imagem.isEmpty()) || removerImagem) {
            String sufixo = (imagem != null && !imagem.isEmpty()) ? "; imagem=substituída" : removerImagem ? "; imagem=removida" : "";
            auditarCatalogo(salva, "CATALOGO_ITEM_ATUALIZADO", antes, depois + sufixo);
        }
        return salva;
    }

    /**
     * Exclui a recompensa DA CLÍNICA informada. Se já existirem resgates vinculados (FK em TB_RESGATE), não é possível
     * apagar sem perder o histórico dos tutores; nesse caso o produto é apenas inativado.
     */
    @Transactional
    public ResultadoExclusao excluir(Long id, Long idClinica) {
        Recompensa recompensa = buscarDaClinica(id, idClinica);
        if (resgateRepository.existsByRecompensa_IdRecompensa(id)) {
            String antes = resumo(recompensa);
            recompensa.setFlAtivo(false);
            recompensaRepository.save(recompensa);
            auditarCatalogo(recompensa, "CATALOGO_ITEM_INATIVADO", antes, resumo(recompensa) + "; motivo=possui resgates");
            return new ResultadoExclusao(false);
        }
        String antes = resumo(recompensa);
        recompensaRepository.delete(recompensa);
        auditarCatalogo(recompensa, "CATALOGO_ITEM_EXCLUIDO", antes, null);
        return new ResultadoExclusao(true);
    }

    /** Busca a recompensa garantindo que ela pertence à clínica informada. */
    private Recompensa buscarDaClinica(Long id, Long idClinica) {
        Recompensa recompensa = buscarPorId(id);
        if (recompensa.getClinica() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa recompensa não está associada a nenhuma clínica. Edite-a para escolher a clínica antes de excluir");
        }
        if (!recompensa.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Recompensa " + id + " não pertence à clínica " + idClinica);
        }
        return recompensa;
    }

    private void validarImagem(MultipartFile imagem) {
        if (imagem.getSize() > TAMANHO_MAXIMO_IMAGEM_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 5MB");
        }
        String contentType = imagem.getContentType();
        if (contentType == null || !TIPOS_IMAGEM_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de imagem inválido. Envie um arquivo JPEG, PNG ou WEBP");
        }
    }

    public List<Recompensa> listarAtivas() {
        return recompensaRepository.findByFlAtivoTrue();
    }

    /** Catálogo de uma clínica (o que o tutor vinculado e o veterinário daquela clínica enxergam). */
    public List<Recompensa> listarAtivasDaClinica(Long idClinica) {
        return recompensaRepository.findByFlAtivoTrueAndClinica_IdClinicaOrderByIdRecompensaAsc(idClinica);
    }

    /** Catálogo completo (ativos e inativos) para a tela administrativa; filtra por clínica quando informada. */
    public List<Recompensa> listarTodas(Long idClinica) {
        return idClinica == null
                ? recompensaRepository.findAllByOrderByIdRecompensaAsc()
                : recompensaRepository.findAllByClinica_IdClinicaOrderByIdRecompensaAsc(idClinica);
    }

    public Recompensa buscarPorId(Long id) {
        return recompensaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recompensa não encontrada com id: " + id)
        );
    }

    /**
     * Saldo resgatável do tutor SOMENTE na clínica informada: pontos de outras clínicas, pendentes, bloqueados e
     * vencidos não entram, e resgates pendentes ficam reservados até serem validados ou negados.
     */
    public int calcularSaldo(Long idTutor, Long idClinica) {
        return pontosService.calcularSaldo(idTutor, idClinica).saldoDisponivel();
    }

    /** Saldo na clínica em que o tutor está vinculado hoje (0 se não houver vínculo ativo). */
    public int calcularSaldoNaClinicaVinculada(Long idTutor) {
        VinculoTutorClinica vinculo = vinculoClinicaService.buscarVinculoAtivo(idTutor);
        if (vinculo == null) {
            return 0;
        }
        return calcularSaldo(idTutor, vinculo.getClinica().getIdClinica());
    }

    @Transactional
    public Resgate solicitarResgate(Long idTutor, Long idRecompensa) {
        Recompensa recompensa = buscarPorId(idRecompensa);
        if (!Boolean.TRUE.equals(recompensa.getFlAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa recompensa não está mais disponível");
        }
        if (recompensa.getClinica() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa recompensa não está disponível em nenhuma clínica");
        }
        Long idClinica = recompensa.getClinica().getIdClinica();
        VinculoTutorClinica vinculo = vinculoClinicaService.buscarVinculoAtivo(idTutor);
        if (vinculo == null || !vinculo.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Essa recompensa pertence a outra clínica. Os pontos só valem na clínica em que foram ganhos");
        }
        int saldo = calcularSaldo(idTutor, idClinica);
        if (saldo < recompensa.getNrCustoPontos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Saldo insuficiente: você tem " + saldo + " pontos, precisa de " + recompensa.getNrCustoPontos());
        }
        Resgate resgate = Resgate.builder()
                .tutor(tutorService.buscarPorId(idTutor))
                .recompensa(recompensa)
                // cópia congelada: editar a recompensa depois não altera este resgate nem o saldo
                .nmRecompensa(recompensa.getNmRecompensa())
                .dsDescricaoRecompensa(recompensa.getDsDescricao())
                .dsTipoRecompensa(recompensa.getDsTipo())
                .nrCustoPontos(recompensa.getNrCustoPontos())
                .clinica(recompensa.getClinica())
                .dsStatus(StatusResgate.PENDENTE)
                .build();
        Resgate salvo = resgateRepository.save(resgate);
        auditarResgate(salvo, "RESGATE_SOLICITADO", null, "PENDENTE; item=" + salvo.getNmRecompensa() + "; custo=" + salvo.custoAplicado());
        return salvo;
    }

    public List<Resgate> listarResgatesDoTutor(Long idTutor) {
        return resgateRepository.findByTutor_IdTutorOrderByDtResgateDesc(idTutor);
    }

    /** Fila de validação do veterinário: só resgates de recompensas da clínica dele. */
    public List<Resgate> listarPendentesDaClinica(Long idClinica) {
        return resgateRepository.findByDsStatusAndClinica_IdClinicaOrderByDtResgateAsc(StatusResgate.PENDENTE, idClinica);
    }

    /**
     * Valida ou nega um resgate PENDENTE. Regras de escopo (a API não confia em IDs enviados pelo aplicativo):
     * <ul>
     *   <li>o veterinário vem do token (nunca do corpo da requisição) e precisa pertencer à clínica do resgate;</li>
     *   <li>a clínica do resgate (congelada na solicitação) precisa ser a mesma clínica da recompensa do catálogo;</li>
     *   <li>os pontos são debitados da clínica do resgate: é por ela que {@link PontosService} calcula o saldo.</li>
     * </ul>
     * Resgate sem clínica (legado) não pode ser validado, pois não debitaria o saldo de nenhuma clínica.
     */
    @Transactional
    public Resgate validar(Long idResgate, Long idVeterinario, boolean aprovado) {
        Resgate resgate = resgateRepository.findByIdParaAtualizar(idResgate).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resgate não encontrado")
        );
        if (resgate.getDsStatus() != StatusResgate.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse resgate já foi " + resgate.getDsStatus());
        }
        Veterinario vet = veterinarioRepository.findById(idVeterinario).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado")
        );

        Long idClinicaResgate = clinicaDoResgateConsistente(resgate);
        Long idClinicaVeterinario = vet.getClinica() != null ? vet.getClinica().getIdClinica() : null;
        if (idClinicaVeterinario == null || !idClinicaVeterinario.equals(idClinicaResgate)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esse resgate pertence a outra clínica");
        }

        resgate.setVeterinarioValidador(vet);
        resgate.setDsStatus(aprovado ? StatusResgate.VALIDADO : StatusResgate.NEGADO);
        Resgate salvo = resgateRepository.save(resgate);
        auditarResgate(salvo, aprovado ? "RESGATE_VALIDADO" : "RESGATE_NEGADO", "PENDENTE",
                salvo.getDsStatus().name() + "; item=" + salvo.getNmRecompensa() + "; custo=" + salvo.custoAplicado());
        return salvo;
    }

    /**
     * Garante que o resgate, a recompensa do catálogo e (portanto) o saldo apontam para a MESMA clínica.
     * Devolve o id dessa clínica; em caso de divergência recusa com 409 em vez de debitar de onde não deve.
     */
    private Long clinicaDoResgateConsistente(Resgate resgate) {
        Clinica clinicaResgate = resgate.getClinica();
        if (clinicaResgate == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse resgate não está associado a nenhuma clínica e não pode ser validado");
        }
        Clinica clinicaCatalogo = resgate.getRecompensa() != null ? resgate.getRecompensa().getClinica() : null;
        if (clinicaCatalogo == null || !clinicaCatalogo.getIdClinica().equals(clinicaResgate.getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A clínica do resgate não corresponde à clínica da recompensa. Os pontos não podem ser debitados");
        }
        return clinicaResgate.getIdClinica();
    }

    private static String resumo(Recompensa r) {
        return "nome=" + r.getNmRecompensa() + "; custo=" + r.getNrCustoPontos() + "; tipo=" + r.getDsTipo()
                + "; ativo=" + r.getFlAtivo() + "; descricao=" + r.getDsDescricao();
    }

    private void auditarCatalogo(Recompensa r, String acao, String anterior, String novo) {
        if (auditoriaService != null && r != null && r.getIdRecompensa() != null) {
            auditoriaService.registrarAcao(AuditoriaTipos.CATALOGO, r.getIdRecompensa(), acao, r.getClinica(), anterior, novo, null, null);
        }
    }

    private void auditarResgate(Resgate r, String acao, String anterior, String novo) {
        if (auditoriaService != null && r != null && r.getIdResgate() != null) {
            auditoriaService.registrarAcao(AuditoriaTipos.RESGATE, r.getIdResgate(), acao, r.getClinica(), anterior, novo, null, null);
        }
    }
}