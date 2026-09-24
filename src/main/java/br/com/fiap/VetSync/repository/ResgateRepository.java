package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.StatusResgate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResgateRepository extends JpaRepository<Resgate, Long> {
    List<Resgate> findByTutor_IdTutorOrderByDtResgateDesc(Long idTutor);
    List<Resgate> findByDsStatusOrderByDtResgateAsc(StatusResgate status);

    // NOVO: usado para decidir entre excluir de vez ou apenas inativar o produto
    boolean existsByRecompensa_IdRecompensa(Long idRecompensa);
}