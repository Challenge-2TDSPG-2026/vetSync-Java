package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.MedicamentoRepository;
import br.com.fiap.VetSync.repository.PrescricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescricaoService {

    private final PrescricaoRepository prescricaoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final AdminRepository adminRepository;
    private final EventoService eventoService;
    private final EmailService emailService;

    public Prescricao solicitar(Long idEvento, Long idMedicamento, String posologia,
                                LocalDate dtInicio, LocalDate dtFim, Integer qtDosesDia,
                                Long idVeterinarioAutenticado) {
        EventoSaude evento = eventoService.buscarPorId(idEvento);
        boolean responsavel = evento.getVeterinario() != null
                && evento.getVeterinario().getIdVeterinario().equals(idVeterinarioAutenticado);
        if (!responsavel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não é o veterinário responsável por esse evento");
        }

        Medicamento medicamento = medicamentoRepository.findById(idMedicamento).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado: " + idMedicamento));

        Prescricao prescricao = Prescricao.builder()
                .evento(evento)
                .medicamento(medicamento)
                .dsPosologia(posologia)
                .dtInicio(dtInicio)
                .dtFim(dtFim)
                .qtDosesDia(qtDosesDia)
                .dsStatus(StatusPrescricao.SOLICITADO)
                .build();
        return prescricaoRepository.save(prescricao);
    }

    public Prescricao buscarPorId(Long id) {
        return prescricaoRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescrição não encontrada com id: " + id));
    }

    public List<Prescricao> listarPorTutor(String email) {
        return prescricaoRepository.findVisiveisParaTutor(email);
    }

    public List<Prescricao> listarPorVeterinario(String email) {
        return prescricaoRepository.findByEvento_Veterinario_DsEmailOrderByIdPrescricaoDesc(email);
    }

    public List<Prescricao> listarPendentes() {
        return prescricaoRepository.findByDsStatusOrderByIdPrescricaoAsc(StatusPrescricao.SOLICITADO);
    }


    public Prescricao liberar(Long idPrescricao, Long idAdmin, boolean aprovado) {
        Prescricao prescricao = buscarPorId(idPrescricao);
        if (prescricao.getDsStatus() != StatusPrescricao.SOLICITADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa prescrição já foi " + prescricao.getDsStatus());
        }
        Admin admin = adminRepository.findById(idAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin não encontrado"));

        prescricao.setAdminValidador(admin);
        prescricao.setDsStatus(aprovado ? StatusPrescricao.LIBERADO : StatusPrescricao.NEGADO);
        Prescricao salva = prescricaoRepository.save(prescricao);

        if (aprovado) {
            notificarTutorLiberacao(salva);
        }
        return salva;
    }

    private void notificarTutorLiberacao(Prescricao prescricao) {
        Pet pet = prescricao.getEvento().getPet();
        Tutor tutor = pet != null ? pet.getTutor() : null;
        if (tutor == null || tutor.getDsEmail() == null) return;

        emailService.enviar(
                tutor.getDsEmail(),
                "Medicamento liberado para " + pet.getNmPet(),
                "Olá, " + tutor.getNmTutor() + "!\n\n"
                        + "O medicamento " + prescricao.getMedicamento().getNmMedicamento()
                        + " foi liberado pela clínica para o(a) " + pet.getNmPet() + ".\n\n"
                        + "Posologia: " + prescricao.getDsPosologia() + "\n"
                        + "Início: " + prescricao.getDtInicio()
                        + (prescricao.getDtFim() != null ? " | Fim: " + prescricao.getDtFim() : "") + "\n\n"
                        + "Qualquer dúvida, procure a clínica."
        );
    }
}