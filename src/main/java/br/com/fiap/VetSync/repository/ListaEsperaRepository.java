package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ListaEspera;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.StatusListaEspera;
import br.com.fiap.VetSync.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    List<ListaEspera> findByTutorAndStatusInOrderByCriadaEmDesc(Tutor tutor, Collection<StatusListaEspera> status);

    /** Busca já limitada ao tutor: entrada de outro tutor simplesmente "não existe". */
    Optional<ListaEspera> findByIdEsperaAndTutor(Long idEspera, Tutor tutor);

    boolean existsByTutorAndPet_IdPetAndServicoClinica_IdServicoClinicaAndStatusIn(
            Tutor tutor, Long idPet, Long idServicoClinica, Collection<StatusListaEspera> status);

    List<ListaEspera> findByStatusIn(Collection<StatusListaEspera> status);

    /** Fila (mais antigos primeiro) de quem espera o serviço numa data. Profissional e hora são filtrados em Java. */
    @Query("""
            select l from ListaEspera l
              join fetch l.tutor
              join fetch l.pet
              join fetch l.servicoClinica
             where l.status = :status
               and l.servicoClinica.idServicoClinica = :idServico
               and l.dataInicio <= :data
               and l.dataFim >= :data
             order by l.criadaEm asc
            """)
    List<ListaEspera> candidatosParaVaga(@Param("status") StatusListaEspera status,
                                         @Param("idServico") Long idServico,
                                         @Param("data") LocalDate data);

    /** Quantos eventos do pet, nesse serviço e período, estão no status informado. */
    @Query("""
            select count(e) from EventoSaude e
             where e.pet.idPet = :idPet
               and e.servicoClinica.idServicoClinica = :idServico
               and e.dsStatus = :status
               and e.dtEvento between :inicio and :fim
            """)
    long contarEventos(@Param("idPet") Long idPet,
                       @Param("idServico") Long idServico,
                       @Param("status") StatusEvento status,
                       @Param("inicio") LocalDate inicio,
                       @Param("fim") LocalDate fim);
}