package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.PlanoTratamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanoTratamentoRepository extends JpaRepository<PlanoTratamento, Long> {
    List<PlanoTratamento> findByPet_Tutor_DsEmailOrderByDtCriacaoDesc(String email);
    List<PlanoTratamento> findByVeterinario_DsEmailOrderByDtCriacaoDesc(String email);


    @Query("""
            select p from PlanoTratamento p
            where p.pet.tutor.dsEmail = :email
               or exists (
                   select 1 from PetAcesso a
                   where a.pet = p.pet
                     and a.tutor.dsEmail = :email
                     and a.dsStatus = br.com.fiap.VetSync.entity.StatusAcessoPet.ATIVO
               )
            order by p.dtCriacao desc
            """)
    List<PlanoTratamento> findVisiveisParaTutor(@Param("email") String email);
}