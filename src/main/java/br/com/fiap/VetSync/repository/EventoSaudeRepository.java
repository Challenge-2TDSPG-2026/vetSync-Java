package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventoSaudeRepository extends JpaRepository<EventoSaude, Long> {

    interface VacinaPublicaProjection {
        String getNome();
        LocalDate getData();
        StatusEvento getStatus();
    }

    @Query("""
            select e.tipoEvento.nmTipoEvento as nome, e.dtEvento as data, e.dsStatus as status
            from EventoSaude e
            where e.pet.idPet = :idPet
              and lower(e.tipoEvento.nmTipoEvento) = lower(:nomeTipoEvento)
              and e.dsStatus <> :statusCancelado
            order by e.dtEvento desc
            """)
    List<VacinaPublicaProjection> buscarVacinasPublicasPorPet(
            @Param("idPet") Long idPet,
            @Param("nomeTipoEvento") String nomeTipoEvento,
            @Param("statusCancelado") StatusEvento statusCancelado
    );

    List<EventoSaude> findByPet_IdPet(Long idPet);
    List<EventoSaude> findByPet_Tutor_DsEmailOrderByDtEventoDesc(String email);


    @Query("""
            select e from EventoSaude e
            where e.pet.tutor.dsEmail = :email
               or exists (
                   select 1 from PetAcesso a
                   where a.pet = e.pet
                     and a.tutor.dsEmail = :email
                     and a.dsStatus = br.com.fiap.VetSync.entity.StatusAcessoPet.ATIVO
               )
            order by e.dtEvento desc
            """)
    List<EventoSaude> findVisiveisParaTutor(@Param("email") String email);
    List<EventoSaude> findByVeterinario_DsEmailOrderByDtEventoDesc(String email);
    List<EventoSaude> findByVeterinario_IdVeterinarioAndDtEvento(Long idVeterinario, LocalDate dtEvento);
    List<EventoSaude> findByProfissionalEstetica_DsEmailOrderByDtEventoDesc(String email);
    List<EventoSaude> findByProfissionalEstetica_IdProfissionalEsteticaAndDtEvento(Long idProfissionalEstetica, LocalDate dtEvento);
}