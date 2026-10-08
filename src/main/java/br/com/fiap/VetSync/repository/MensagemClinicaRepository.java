package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.MensagemClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensagemClinicaRepository extends JpaRepository<MensagemClinica, Long> {
    List<MensagemClinica> findByConversa_IdConversaOrderByDtEnvioAsc(Long idConversa);
}
