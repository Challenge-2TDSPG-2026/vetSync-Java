package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.LinkAgendamentoVetRepository;
import br.com.fiap.VetSync.repository.RelatorioEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RelatorioEsteticaService {

    private static final int DIAS_VALIDADE_LINK = 15;

    private final RelatorioEsteticaRepository relatorioEsteticaRepository;
    private final LinkAgendamentoVetRepository linkAgendamentoVetRepository;
    private final AdminRepository adminRepository;
    private final EventoService eventoService;
    private final EmailService emailService;

    @Value("${app.link.base-url:https://vetsync-theta.vercel.app}")
    private String linkBaseUrl;

    public RelatorioEstetica solicitar(Long idEvento, String dsProblema, Long idProfissionalAutenticado) {
        EventoSaude evento = eventoService.buscarPorId(idEvento);
        boolean responsavel = evento.getProfissionalEstetica() != null
                && evento.getProfissionalEstetica().getIdProfissionalEstetica().equals(idProfissionalAutenticado);
        if (!responsavel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não é o profissional de estética responsável por esse evento");
        }

        RelatorioEstetica relatorio = RelatorioEstetica.builder()
                .evento(evento)
                .profissionalEstetica(evento.getProfissionalEstetica())
                .dsProblema(dsProblema)
                .dsStatus(StatusRelatorioEstetica.SOLICITADO)
                .build();
        return relatorioEsteticaRepository.save(relatorio);
    }

    public RelatorioEstetica buscarPorId(Long id) {
        return relatorioEsteticaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relatório não encontrado com id: " + id));
    }

    public List<RelatorioEstetica> listarPorTutor(String email) {
        return relatorioEsteticaRepository.findByEvento_Pet_Tutor_DsEmailOrderByIdRelatorioDesc(email);
    }

    public List<RelatorioEstetica> listarPorProfissional(String email) {
        return relatorioEsteticaRepository.findByEvento_ProfissionalEstetica_DsEmailOrderByIdRelatorioDesc(email);
    }

    public List<RelatorioEstetica> listarPendentes() {
        return relatorioEsteticaRepository.findByDsStatusOrderByIdRelatorioAsc(StatusRelatorioEstetica.SOLICITADO);
    }

    public RelatorioEstetica liberar(Long idRelatorio, Long idAdmin, boolean aprovado) {
        RelatorioEstetica relatorio = buscarPorId(idRelatorio);
        if (relatorio.getDsStatus() != StatusRelatorioEstetica.SOLICITADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse relatório já foi " + relatorio.getDsStatus());
        }
        Admin admin = adminRepository.findById(idAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin não encontrado"));

        relatorio.setAdminValidador(admin);
        relatorio.setDsStatus(aprovado ? StatusRelatorioEstetica.LIBERADO : StatusRelatorioEstetica.NEGADO);
        RelatorioEstetica salvo = relatorioEsteticaRepository.save(relatorio);

        if (aprovado) {
            criarLinkENotificarTutor(salvo);
        }
        return salvo;
    }

    private void criarLinkENotificarTutor(RelatorioEstetica relatorio) {
        Pet pet = relatorio.getEvento().getPet();
        Tutor tutor = pet != null ? pet.getTutor() : null;
        if (tutor == null || tutor.getDsEmail() == null) return;

        LinkAgendamentoVet link = LinkAgendamentoVet.builder()
                .dsToken(UUID.randomUUID().toString())
                .relatorioEstetica(relatorio)
                .dsStatus(StatusLinkAgendamento.PENDENTE)
                .dtExpiracao(LocalDate.now().plusDays(DIAS_VALIDADE_LINK))
                .build();
        link = linkAgendamentoVetRepository.save(link);

        String url = linkBaseUrl + "/agendamento-retorno/" + link.getDsToken();

        emailService.enviar(
                tutor.getDsEmail(),
                "Observação durante o banho de " + pet.getNmPet(),
                "Olá, " + tutor.getNmTutor() + "!\n\n"
                        + "Durante o banho do(a) " + pet.getNmPet() + ", nossa equipe de estética identificou o seguinte:\n\n"
                        + relatorio.getDsProblema() + "\n\n"
                        + "Se desejar, você pode agendar uma avaliação com um veterinário escolhendo a data e o profissional pelo link abaixo:\n"
                        + url + "\n\n"
                        + "Esse link é válido por " + DIAS_VALIDADE_LINK + " dias."
        );
    }
}