package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.StatusResgate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResgateRepository extends JpaRepository<Resgate, Long> {
    List<Resgate> findByTutor_IdTutorOrderByDtResgateDesc(Long idTutor);
    List<Resgate> findByDsStatusOrderByDtResgateAsc(StatusResgate status);

    // Escopo por clínica usando a clínica congelada no resgate: o saldo e a fila de validação
    // não mudam se a recompensa for editada depois.
    List<Resgate> findByTutor_IdTutorAndClinica_IdClinicaOrderByDtResgateDesc(Long idTutor, Long idClinica);
    List<Resgate> findByDsStatusAndClinica_IdClinicaOrderByDtResgateAsc(StatusResgate status, Long idClinica);

    // Trava a linha do resgate durante a validação: dois veterinários não validam/negam o mesmo resgate ao mesmo tempo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resgate r where r.idResgate = :idResgate")
    Optional<Resgate> findByIdParaAtualizar(@Param("idResgate") Long idResgate);

    // Indicadores do painel do admin (global ou por clínica, usando a clínica congelada no resgate)
    long countByDsStatus(StatusResgate status);
    long countByDsStatusAndClinica_IdClinica(StatusResgate status, Long idClinica);

    // Usado para decidir entre excluir de vez ou apenas inativar o produto
    boolean existsByRecompensa_IdRecompensa(Long idRecompensa);
}