package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.StatusResgate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResgateRepository extends JpaRepository<Resgate, Long> {
    List<Resgate> findByTutor_IdTutorOrderByDtResgateDesc(Long idTutor);
    List<Resgate> findByDsStatusOrderByDtResgateAsc(StatusResgate status);

    // Escopo por clínica usando a clínica congelada no resgate: o saldo e a fila de validação
    // não mudam se a recompensa for editada depois.
    List<Resgate> findByTutor_IdTutorAndClinica_IdClinicaOrderByDtResgateDesc(Long idTutor, Long idClinica);
    List<Resgate> findByDsStatusAndClinica_IdClinicaOrderByDtResgateAsc(StatusResgate status, Long idClinica);

    // Usado para decidir entre excluir de vez ou apenas inativar o produto
    boolean existsByRecompensa_IdRecompensa(Long idRecompensa);
}