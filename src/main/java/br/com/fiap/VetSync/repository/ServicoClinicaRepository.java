package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ServicoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServicoClinicaRepository extends JpaRepository<ServicoClinica, Long> {
    List<ServicoClinica> findByClinica_IdClinicaOrderByNmServico(Long idClinica);
    List<ServicoClinica> findByClinica_IdClinicaAndAtivoTrueOrderByNmServico(Long idClinica);
    Optional<ServicoClinica> findByIdServicoClinicaAndClinica_IdClinica(Long id, Long idClinica);
    boolean existsByClinica_IdClinicaAndNmServicoIgnoreCase(Long idClinica, String nome);
    List<ServicoClinica> findByClinica_IdClinicaAndTipoEvento_IdTipoEventoAndAtivoTrue(Long idClinica, Long idTipoEvento);
}
