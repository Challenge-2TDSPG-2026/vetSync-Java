package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ServicoProfissional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicoProfissionalRepository extends JpaRepository<ServicoProfissional, Long> {
    List<ServicoProfissional> findByServico_IdServicoClinica(Long idServico);
    void deleteByServico_IdServicoClinica(Long idServico);
    boolean existsByServico_IdServicoClinicaAndVeterinario_IdVeterinario(Long idServico, Long idVet);
}
