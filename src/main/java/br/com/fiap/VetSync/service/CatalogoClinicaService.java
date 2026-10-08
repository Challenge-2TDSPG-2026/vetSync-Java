package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.security.ClinicaAdminAccess;
import br.com.fiap.VetSync.security.PermissaoClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogoClinicaService {
    private final ClinicaAdminAccess access;
    private final ServicoClinicaRepository servicoRepository;
    private final ServicoProfissionalRepository profissionalRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final ServicoEsteticaRepository esteticaServicoRepository;

    public record Modelo(String codigo, String nome, String categoria, String nomeTipoEvento, String baseEstetica) {}
    public record ServicoDados(Long id, String nome, String categoria, int duracaoMinutos, boolean ativo,
                               List<Long> veterinarios, List<Long> profissionaisEstetica) {}

    public static final List<Modelo> MODELOS = List.of(
            new Modelo("CONSULTA", "Consulta de rotina", "PREVENTIVO", "Consulta de rotina", null),
            new Modelo("RETORNO", "Retorno veterinário", "TERAPEUTICO", "Retorno veterinário", null),
            new Modelo("VACINACAO", "Vacinação", "PREVENTIVO", "Vacina", null),
            new Modelo("EXAMES", "Exames", "TERAPEUTICO", "Realizar exames", null),
            new Modelo("PROCEDIMENTOS", "Procedimentos", "TERAPEUTICO", "Procedimentos", null),
            new Modelo("BANHO", "Banho", "BEM_ESTAR", "Banho e tosa", "Banho"),
            new Modelo("TOSA_TESOURA", "Banho e tosa na tesoura", "BEM_ESTAR", "Banho e tosa", "Banho e Tosa na Tesoura"),
            new Modelo("TOSA_MAQUINA", "Banho e tosa na máquina", "BEM_ESTAR", "Banho e tosa", "Banho e Tosa na Máquina")
    );

    public List<Modelo> modelos() { return MODELOS; }

    @Transactional(readOnly = true)
    public List<ServicoDados> listar(Authentication authentication) {
        Admin admin = access.exigirAlguma(authentication, PermissaoClinica.SERVICOS_VER, PermissaoClinica.AGENDA_CRIAR);
        return servicoRepository.findByClinica_IdClinicaOrderByNmServico(admin.getClinica().getIdClinica())
                .stream().map(this::dados).toList();
    }

    @Transactional
    public ServicoDados salvar(Authentication authentication, Long idServico, String codigoPadrao,
                               String nomePersonalizado, String categoria, Integer duracao,
                               List<Long> idsVeterinarios, List<Long> idsEstetica, boolean ativo) {
        Admin admin = access.exigir(authentication, PermissaoClinica.SERVICOS_EDITAR);
        Clinica clinica = admin.getClinica();
        if (duracao == null || duracao < 5 || duracao > 480) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duração deve ficar entre 5 e 480 minutos");
        }
        Modelo modelo = codigoPadrao == null ? null : MODELOS.stream()
                .filter(m -> m.codigo().equals(codigoPadrao)).findFirst().orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serviço padrão desconhecido"));
        String nome = modelo == null ? nomePersonalizado : modelo.nome();
        String categoriaFinal = modelo == null ? categoria : modelo.categoria();
        if (nome == null || nome.isBlank() || nome.length() > 80 || categoriaFinal == null
                || !Set.of("PREVENTIVO", "TERAPEUTICO", "BEM_ESTAR").contains(categoriaFinal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome ou categoria de serviço inválidos");
        }
        List<Long> vets = idsVeterinarios == null ? List.of() : idsVeterinarios.stream().distinct().toList();
        List<Long> estetica = idsEstetica == null ? List.of() : idsEstetica.stream().distinct().toList();
        if (vets.isEmpty() && estetica.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione pelo menos um profissional");
        }
        if (!estetica.isEmpty() && !"BEM_ESTAR".equals(categoriaFinal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profissionais de estética só atendem serviços de bem-estar");
        }
        ServicoEstetica base = modelo == null || modelo.baseEstetica() == null ? null
                : esteticaServicoRepository.findByNmServicoIgnoreCase(modelo.baseEstetica()).orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.CONFLICT, "Serviço base de estética não cadastrado"));
        if (base != null && !vets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Serviços de banho e tosa precisam de profissionais de estética");
        }
        ServicoClinica servico;
        if (idServico == null) {
            if (servicoRepository.existsByClinica_IdClinicaAndNmServicoIgnoreCase(clinica.getIdClinica(), nome.trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Serviço já cadastrado nesta clínica");
            }
            TipoEvento tipo = modelo == null ? null : tipoEventoRepository.findFirstByNmTipoEventoIgnoreCaseAndClinicaIsNull(modelo.nomeTipoEvento())
                    .orElse(null);
            if (tipo == null) {
                tipo = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento(modelo == null ? nome.trim() : modelo.nomeTipoEvento())
                        .dsCategoria(categoriaFinal).dsModalidadeAgendamento("SERVICO_CLINICA")
                        .nrDuracaoMinutos(duracao).clinica(clinica).build());
            }
            servico = ServicoClinica.builder().clinica(clinica).tipoEvento(tipo).baseEstetica(base).build();
        } else {
            servico = servicoRepository.findByIdServicoClinicaAndClinica_IdClinica(idServico, clinica.getIdClinica())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
            if ((modelo != null && !modelo.nomeTipoEvento().equals(servico.getTipoEvento().getNmTipoEvento()))
                    || (modelo == null && servico.getTipoEvento().getClinica() == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo de um serviço existente não pode ser trocado");
            }
            if (!servico.getNmServico().equalsIgnoreCase(nome.trim()) &&
                    servicoRepository.existsByClinica_IdClinicaAndNmServicoIgnoreCase(clinica.getIdClinica(), nome.trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Serviço já cadastrado nesta clínica");
            }
            if (servico.getTipoEvento().getClinica() != null) {
                servico.getTipoEvento().setNmTipoEvento(nome.trim());
                servico.getTipoEvento().setNrDuracaoMinutos(duracao);
                servico.getTipoEvento().setDsCategoria(categoriaFinal);
            }
            servico.setBaseEstetica(base);
        }
        servico.setNmServico(nome.trim());
        servico.setDsCategoria(categoriaFinal);
        servico.setNrDuracaoMinutos(duracao);
        servico.setAtivo(ativo);
        servico = servicoRepository.save(servico);
        profissionalRepository.deleteByServico_IdServicoClinica(servico.getIdServicoClinica());
        profissionalRepository.flush();
        for (Long idVet : vets) {
            Veterinario vet = veterinarioRepository.findById(idVet).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado"));
            if (!vet.getClinica().getIdClinica().equals(clinica.getIdClinica())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veterinário de outra clínica");
            }
            profissionalRepository.save(ServicoProfissional.builder().servico(servico).veterinario(vet).build());
        }
        for (Long idProf : estetica) {
            ProfissionalEstetica prof = esteticaRepository.findById(idProf).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional de estética não encontrado"));
            if (!prof.getClinica().getIdClinica().equals(clinica.getIdClinica())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profissional de outra clínica");
            }
            if (base != null && !prof.getServicos().contains(base)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Profissional de estética não atende o serviço base selecionado");
            }
            profissionalRepository.save(ServicoProfissional.builder().servico(servico).profissionalEstetica(prof).build());
        }
        return dados(servico);
    }

    public ServicoDados dados(ServicoClinica s) {
        var profissionais = profissionalRepository.findByServico_IdServicoClinica(s.getIdServicoClinica());
        return new ServicoDados(s.getIdServicoClinica(), s.getNmServico(), s.getDsCategoria(),
                s.getNrDuracaoMinutos(), Boolean.TRUE.equals(s.getAtivo()),
                profissionais.stream().filter(p -> p.getVeterinario() != null)
                        .map(p -> p.getVeterinario().getIdVeterinario()).toList(),
                profissionais.stream().filter(p -> p.getProfissionalEstetica() != null)
                        .map(p -> p.getProfissionalEstetica().getIdProfissionalEstetica()).toList());
    }
}
