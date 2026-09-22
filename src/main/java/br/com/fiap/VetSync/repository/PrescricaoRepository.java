package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Prescricao;
import br.com.fiap.VetSync.entity.StatusPrescricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {
    List<Prescricao> findByEvento_IdEvento(Long idEvento);
    List<Prescricao> findByEvento_Pet_Tutor_DsEmailOrderByIdPrescricaoDesc(String email);
    List<Prescricao> findByEvento_Veterinario_DsEmailOrderByIdPrescricaoDesc(String email);
    List<Prescricao> findByDsStatusOrderByIdPrescricaoAsc(StatusPrescricao status);


    @Query("""
            select p from Prescricao p
            where p.evento.pet.tutor.dsEmail = :email
               or exists (
                   select 1 from PetAcesso a
                   where a.pet = p.evento.pet
                     and a.tutor.dsEmail = :email
                     and a.dsStatus = br.com.fiap.VetSync.entity.StatusAcessoPet.ATIVO
               )
            order by p.idPrescricao desc
            """)
    List<Prescricao> findVisiveisParaTutor(@Param("email") String email);
}