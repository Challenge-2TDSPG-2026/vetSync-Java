package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Auditoria;
import br.com.fiap.VetSync.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private final AuditoriaRepository repository;

    @Transactional
    public Auditoria registrar(String entidade, Long idEntidade, String acao, String ator,
                               String perfil, String anterior, String novo, String ip) {
        return repository.save(Auditoria.builder().dsEntidade(entidade).idEntidade(idEntidade)
                .dsAcao(acao).dsAtor(ator).dsPerfil(perfil).dsValorAnterior(anterior)
                .dsValorNovo(novo).dsIp(ip).dtOcorrencia(LocalDateTime.now()).build());
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listar(String entidade, Long idEntidade) {
        return repository.findByDsEntidadeAndIdEntidadeOrderByDtOcorrenciaDesc(entidade, idEntidade);
    }
}
