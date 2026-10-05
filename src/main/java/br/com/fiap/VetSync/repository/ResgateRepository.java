package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.StatusResgate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResgateRepository extends JpaRepository<Resgate, Long> {
    List<Resgate> findByTutor_IdTutorOrderByDtResgateDesc(Long idTutor);
    List<Resgate> findByDsStatusOrderByDtResgateAsc(StatusResgate status);

    // Escopo por clínica: o saldo e a fila de validação só consideram recompensas da própria clínica.
    List<Resgate> findByTutor_IdTutorAndRecompensa_Clinica_IdClinicaOrderByDtResgateDesc(Long idTutor, Long idClinica);
    List<Resgate> findByDsStatusAndRecompensa_Clinica_IdClinicaOrderByDtResgateAsc(StatusResgate status, Long idClinica);

    // NOVO: usado para decidir entre excluir de vez ou apenas inativar o produto
    boolean existsByRecompensa_IdRecompensa(Long idRecompensa);
}