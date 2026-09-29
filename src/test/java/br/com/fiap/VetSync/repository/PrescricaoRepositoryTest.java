package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PrescricaoRepositoryTest {

    @Autowired
    private PrescricaoRepository prescricaoRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private EventoSaudeRepository eventoSaudeRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EspecieRepository especieRepository;

    @Autowired
    private RacaRepository racaRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private VeterinarioRepository veterinarioRepository;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Test
    @DisplayName("Deve buscar prescrições por status, evento, tutor e veterinário")
    void buscarPrescricoes() {
        Tutor tutor = tutorRepository.save(Tutor.builder().nmTutor("Patricia").dsEmail("patricia.p@teste.com").dsCpf("11133355577").dsSenha("pwd").build());
        Especie esp = especieRepository.save(Especie.builder().nmEspecie("Gato P").build());
        Raca raca = racaRepository.save(Raca.builder().nmRaca("SRD").especie(esp).build());
        Pet pet = petRepository.save(Pet.builder().nmPet("Mingau").dtNascimento(LocalDate.now().minusYears(1)).tutor(tutor).raca(raca).build());
        Clinica clinica = clinicaRepository.save(Clinica.builder().nmClinica("Clinica P").dsCnpj("33344455000188").build());
        Veterinario vet = veterinarioRepository.save(Veterinario.builder().nmVeterinario("Dr. V").dsEmail("vet.p@teste.com").nrCrmv("CRMV-99112").dsSenha("pwd").clinica(clinica).build());
        TipoEvento tipo = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Consulta").dsCategoria("TERAPEUTICO").build());
        EventoSaude ev = eventoSaudeRepository.save(EventoSaude.builder().pet(pet).veterinario(vet).tipoEvento(tipo).dtEvento(LocalDate.now()).dsStatus(StatusEvento.CONCLUIDO).build());
        Medicamento med = medicamentoRepository.save(Medicamento.builder().nmMedicamento("Pomada Cicatrizante").build());

        prescricaoRepository.save(Prescricao.builder()
                .evento(ev)
                .medicamento(med)
                .dsPosologia("Aplicar 2x ao dia")
                .dtInicio(LocalDate.now())
                .dsStatus(StatusPrescricao.SOLICITADO)
                .build());

        List<Prescricao> pendentes = prescricaoRepository.findByDsStatusOrderByIdPrescricaoAsc(StatusPrescricao.SOLICITADO);
        assertThat(pendentes).hasSize(1);

        List<Prescricao> porTutor = prescricaoRepository.findByEvento_Pet_Tutor_DsEmailOrderByIdPrescricaoDesc("patricia.p@teste.com");
        assertThat(porTutor).hasSize(1);

        List<Prescricao> porVet = prescricaoRepository.findByEvento_Veterinario_DsEmailOrderByIdPrescricaoDesc("vet.p@teste.com");
        assertThat(porVet).hasSize(1);
    }
}
