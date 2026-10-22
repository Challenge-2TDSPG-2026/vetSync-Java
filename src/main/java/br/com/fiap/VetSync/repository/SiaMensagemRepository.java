package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.SiaMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiaMensagemRepository extends JpaRepository<SiaMensagem, Long> {
    List<SiaMensagem> findByConversa_IdOrderByIdAsc(String conversaId);
}
