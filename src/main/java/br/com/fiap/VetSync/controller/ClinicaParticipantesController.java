package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.security.ClinicaAdminAccess;
import br.com.fiap.VetSync.security.PermissaoClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/clinica-admin")
@PreAuthorize("hasRole('ADMIN_CLINICA')")
@RequiredArgsConstructor
public class ClinicaParticipantesController {
    private final ClinicaAdminAccess access;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    private final PetRepository petRepository;

    public record ProfissionalDados(Long id, String nome, String tipo) {}
    public record TutorDados(Long id, String nome, String email) {}
    public record PetDados(Long id, String nome, Long idTutor) {}

    @GetMapping("/profissionais")
    public List<ProfissionalDados> profissionais(Authentication auth) {
        Long clinica = access.exigirAlguma(auth, PermissaoClinica.SERVICOS_VER, PermissaoClinica.AGENDA_CRIAR)
                .getClinica().getIdClinica();
        List<ProfissionalDados> todos = new java.util.ArrayList<>();
        veterinarioRepository.findByClinica_IdClinicaOrderByNmVeterinario(clinica)
                .forEach(v -> todos.add(new ProfissionalDados(v.getIdVeterinario(), v.getNmVeterinario(), "VETERINARIO")));
        esteticaRepository.findByClinica_IdClinicaOrderByNmProfissionalEstetica(clinica)
                .forEach(p -> todos.add(new ProfissionalDados(p.getIdProfissionalEstetica(), p.getNmProfissionalEstetica(), "ESTETICA")));
        return todos;
    }

    @GetMapping("/tutores")
    public List<TutorDados> tutores(Authentication auth) {
        Long clinica = access.exigir(auth, PermissaoClinica.MENSAGENS_INICIAR).getClinica().getIdClinica();
        return vinculoRepository.findByClinica_IdClinicaAndDtEncerramentoIsNull(clinica).stream()
                .map(v -> new TutorDados(v.getTutor().getIdTutor(), v.getTutor().getNmTutor(), v.getTutor().getDsEmail()))
                .toList();
    }

    @GetMapping("/pets")
    public List<PetDados> pets(Authentication auth) {
        Long clinica = access.exigir(auth, PermissaoClinica.AGENDA_CRIAR).getClinica().getIdClinica();
        return vinculoRepository.findByClinica_IdClinicaAndDtEncerramentoIsNull(clinica).stream()
                .flatMap(v -> petRepository.findByTutor_IdTutor(v.getTutor().getIdTutor()).stream())
                .map(p -> new PetDados(p.getIdPet(), p.getNmPet(), p.getTutor().getIdTutor())).toList();
    }
}
