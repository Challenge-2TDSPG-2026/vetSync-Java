package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventoSaudeRepository extends JpaRepository<EventoSaude, Long> {
    /**
     * Projeção usada na carteira pública. Ela consulta apenas os dados que podem
     * ser exibidos no link compartilhável e não instancia EventoSaude.
     */
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
    boolean existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(Long idPet, String email);
    List<EventoSaude> findByPet_IdPetAndDsStatusOrderByDtEventoDescIdEventoDesc(Long idPet, StatusEvento status);
    List<EventoSaude> findByPet_Tutor_DsEmailOrderByDtEventoDesc(String email);

    /**
     * Eventos visíveis para um tutor: dos pets de que ele é proprietário, OU dos pets em que ele
     * tem acesso ativo (cuidador/cônjuge, LEITURA ou EDICAO) via TB_PET_ACESSO.
     */
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
    boolean existsByPet_Tutor_IdTutorAndDsStatus(Long idTutor, StatusEvento dsStatus);

    /**
     * Agenda do dia para o painel do admin: todos os eventos da data (qualquer status),
     * já com pet, tutor, raça, tipo e profissional carregados para evitar N+1.
     * A ordenação por horário é feita no service (hr_evento é texto HH:mm e pode ser nulo).
     */
    @Query("""
            select e from EventoSaude e
            join fetch e.pet p
            join fetch p.tutor
            join fetch p.raca
            join fetch e.tipoEvento
            left join fetch e.veterinario
            left join fetch e.profissionalEstetica
            left join fetch e.clinica
            where e.dtEvento = :data
            """)
    List<EventoSaude> findAgendaDoDia(@Param("data") LocalDate data);

    List<EventoSaude> findByClinica_IdClinicaAndDtEvento(Long idClinica, LocalDate data);
}