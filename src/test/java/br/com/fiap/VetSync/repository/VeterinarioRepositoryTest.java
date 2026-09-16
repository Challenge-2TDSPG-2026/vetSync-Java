package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Veterinario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VeterinarioRepositoryTest {

    @Autowired
    private VeterinarioRepository veterinarioRepository;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Test
    @DisplayName("Deve buscar e verificar existência de veterinário por email e CRMV")
    void buscarEVerificarExistencia() {
        Clinica clinica = clinicaRepository.save(Clinica.builder()
                .nmClinica("Clínica Repo Test")
                .dsCnpj("99888777000166")
                .build());

        Veterinario vet = Veterinario.builder()
                .nmVeterinario("Dr. Fernando")
                .dsEmail("fernando.repo@teste.com")
                .nrCrmv("SP-99999")
                .dsSenha("segredo123")
                .clinica(clinica)
                .build();
        veterinarioRepository.save(vet);

        Optional<Veterinario> opt = veterinarioRepository.findByDsEmail("fernando.repo@teste.com");
        assertThat(opt).isPresent();
        assertThat(opt.get().getNmVeterinario()).isEqualTo("Dr. Fernando");

        assertThat(veterinarioRepository.existsByDsEmail("fernando.repo@teste.com")).isTrue();
        assertThat(veterinarioRepository.existsByNrCrmv("SP-99999")).isTrue();
        assertThat(veterinarioRepository.existsByNrCrmv("SP-00000")).isFalse();
    }
}
