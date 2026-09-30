package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.TutorResponsavelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsavelService {

    private final TutorResponsavelRepository tutorResponsavelRepository;
    private final PetRepository petRepository;
    private final PetAcessoService petAcessoService;

    @Transactional(readOnly = true)
    public List<TutorResponsavel> listarAtivos(Long idTutorProprietario) {
        return tutorResponsavelRepository
                .findByTutorProprietario_IdTutorAndDsStatusOrderByDtConcedidoDesc(
                        idTutorProprietario, StatusAcessoPet.ATIVO);
    }

    @Transactional
    public TutorResponsavel conceder(Tutor proprietario, Tutor responsavel, PermissaoPet permissao) {
        validarPermissao(permissao);
        if (proprietario.getIdTutor().equals(responsavel.getIdTutor())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O tutor proprietário não pode ser responsável pela própria conta");
        }

        LocalDateTime agora = LocalDateTime.now();
        TutorResponsavel vinculo = tutorResponsavelRepository
                .findByTutorProprietario_IdTutorAndTutorResponsavel_IdTutor(
                        proprietario.getIdTutor(), responsavel.getIdTutor())
                .map(existente -> {
                    existente.setDsPermissao(permissao);
                    existente.setDsStatus(StatusAcessoPet.ATIVO);
                    existente.setDtConcedido(agora);
                    existente.setDtRevogado(null);
                    return existente;
                })
                .orElseGet(() -> TutorResponsavel.builder()
                        .tutorProprietario(proprietario)
                        .tutorResponsavel(responsavel)
                        .dsPermissao(permissao)
                        .dsStatus(StatusAcessoPet.ATIVO)
                        .dtConcedido(agora)
                        .build());

        TutorResponsavel salvo = tutorResponsavelRepository.save(vinculo);
        petRepository.findByTutor_IdTutor(proprietario.getIdTutor())
                .forEach(pet -> petAcessoService.conceder(
                        pet, responsavel, RelacaoPet.CUIDADOR, permissao));
        return salvo;
    }

    @Transactional
    public void concederAcessosAoNovoPet(Pet pet) {
        if (pet.getTutor() == null || pet.getTutor().getIdTutor() == null) return;
        listarAtivos(pet.getTutor().getIdTutor()).forEach(vinculo ->
                petAcessoService.conceder(
                        pet,
                        vinculo.getTutorResponsavel(),
                        RelacaoPet.CUIDADOR,
                        vinculo.getDsPermissao()));
    }

    @Transactional
    public void revogar(Long idTutorProprietario, Long idResponsavel) {
        TutorResponsavel vinculo = tutorResponsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Responsável não encontrado"));

        if (vinculo.getTutorProprietario() == null
                || !idTutorProprietario.equals(vinculo.getTutorProprietario().getIdTutor())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado");
        }
        if (vinculo.getDsStatus() == StatusAcessoPet.REVOGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse responsável já foi removido");
        }

        vinculo.setDsStatus(StatusAcessoPet.REVOGADO);
        vinculo.setDtRevogado(LocalDateTime.now());
        tutorResponsavelRepository.save(vinculo);
        petAcessoService.revogarDoResponsavel(
                idTutorProprietario, vinculo.getTutorResponsavel().getIdTutor());
    }

    private void validarPermissao(PermissaoPet permissao) {
        if (permissao != PermissaoPet.LEITURA && permissao != PermissaoPet.EDICAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Permissão deve ser LEITURA ou EDICAO");
        }
    }
}
