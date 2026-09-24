// PrescricaoService.java
package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.MedicamentoRepository;
import br.com.fiap.VetSync.repository.PrescricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    private static final String MENSAGEM_PADRAO_PRESCRICAO =
            "\n\n---\n"
                    + "Este é um aviso automático do VetSync. As informações apresentadas têm caráter "
                    + "orientativo e não substituem a avaliação do veterinário responsável.\n"
                    + "Não altere medicamentos, doses ou horários, nem interrompa o tratamento por conta "
                    + "própria. Em caso de dúvidas, efeitos adversos ou alterações no estado de saúde do "
                    + "pet, entre em contato com o veterinário responsável.";

    private static final long TAMANHO_MAXIMO_PDF_BYTES = 5L * 1024 * 1024; // 5MB

    public Prescricao solicitar(Long idEvento, Long idMedicamento, String posologia,
                                LocalDate dtInicio, LocalDate dtFim, Integer qtDosesDia,
                                Long idVeterinarioAutenticado) {
        return solicitar(idEvento, idMedicamento, posologia, dtInicio, dtFim, qtDosesDia,
                idVeterinarioAutenticado, null);
    }

    public Prescricao solicitar(Long idEvento, Long idMedicamento, String posologia,
                                LocalDate dtInicio, LocalDate dtFim, Integer qtDosesDia,
                                Long idVeterinarioAutenticado, MultipartFile anexoPdf) {
        EventoSaude evento = eventoService.buscarPorId(idEvento);
        boolean responsavel = evento.getVeterinario() != null
                && evento.getVeterinario().getIdVeterinario().equals(idVeterinarioAutenticado);
        if (!responsavel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não é o veterinário responsável por esse evento");
        }

        Medicamento medicamento = medicamentoRepository.findById(idMedicamento).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado: " + idMedicamento));

        Prescricao.PrescricaoBuilder builder = Prescricao.builder()
                .evento(evento)
                .medicamento(medicamento)
                .dsPosologia(posologia)
                .dtInicio(dtInicio)
                .dtFim(dtFim)
                .qtDosesDia(qtDosesDia)
                .dsStatus(StatusPrescricao.SOLICITADO);

        if (anexoPdf != null && !anexoPdf.isEmpty()) {
            validarAnexoPdf(anexoPdf);
            try {
                builder.dsAnexoPdf(anexoPdf.getBytes())
                        .dsAnexoPdfNome(anexoPdf.getOriginalFilename())
                        .dsAnexoPdfTipo(anexoPdf.getContentType());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo PDF enviado");
            }
        }

        return prescricaoRepository.save(builder.build());
    }

    private void validarAnexoPdf(MultipartFile anexoPdf) {
        if (anexoPdf.getSize() > TAMANHO_MAXIMO_PDF_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O PDF deve ter no máximo 5MB");
        }
        String tipo = anexoPdf.getContentType();
        if (tipo == null || !tipo.equalsIgnoreCase("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O anexo deve ser um arquivo PDF");
        }
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

        String corpo = "Olá, " + tutor.getNmTutor() + "!\n\n"
                + "O medicamento " + prescricao.getMedicamento().getNmMedicamento()
                + " foi liberado pela clínica para o(a) " + pet.getNmPet() + ".\n\n"
                + "Posologia: " + prescricao.getDsPosologia() + "\n"
                + "Início: " + prescricao.getDtInicio()
                + (prescricao.getDtFim() != null ? " | Fim: " + prescricao.getDtFim() : "") + "\n\n"
                + "Qualquer dúvida, procure a clínica."
                + MENSAGEM_PADRAO_PRESCRICAO;

        emailService.enviarComAnexo(
                tutor.getDsEmail(),
                "Medicamento liberado para " + pet.getNmPet(),
                corpo,
                prescricao.getDsAnexoPdf(),
                prescricao.getDsAnexoPdfNome(),
                prescricao.getDsAnexoPdfTipo()
        );
    }
}