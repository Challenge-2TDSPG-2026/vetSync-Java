package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.security.ClinicaAdminAccess;
import br.com.fiap.VetSync.security.PermissaoClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClinicaPerfilService {
    private final ClinicaAdminAccess access;
    private final ClinicaRepository clinicaRepository;
    private final HorarioClinicaRepository horarioRepository;
    private final TutorRepository tutorRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;

    public record HorarioDados(int diaSemana, String inicio, String fim) {}
    public record PerfilDados(Long idClinica, String nome, String endereco, String telefone,
                              String cidade, String uf, boolean possuiLogo, List<HorarioDados> horarios) {}

    @Transactional(readOnly = true)
    public PerfilDados perfil(Authentication authentication) {
        Admin admin = access.exigir(authentication, PermissaoClinica.PERFIL_VER);
        return dados(admin.getClinica());
    }

    @Transactional
    public PerfilDados atualizar(Authentication authentication, String nome, String endereco,
                                 String telefone, List<HorarioDados> horarios) {
        Admin admin = access.exigir(authentication, PermissaoClinica.PERFIL_EDITAR);
        Clinica clinica = admin.getClinica();
        if (nome == null || nome.isBlank() || nome.length() > 150 || endereco == null || endereco.isBlank()
                || endereco.length() > 240 || telefone == null || !telefone.matches("\\d{10,11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, endereço ou telefone inválido");
        }
        validarHorarios(horarios);
        clinica.setNmClinica(nome.trim());
        clinica.setDsEndereco(endereco.trim());
        clinica.setDsTelefone(telefone);
        clinicaRepository.save(clinica);
        horarioRepository.deleteByClinica_IdClinica(clinica.getIdClinica());
        for (HorarioDados h : horarios) {
            horarioRepository.save(HorarioClinica.builder().clinica(clinica).nrDiaSemana(h.diaSemana())
                    .hrInicio(h.inicio()).hrFim(h.fim()).build());
        }
        return dados(clinica);
    }

    @Transactional
    public void atualizarLogo(Authentication authentication, String mime, byte[] conteudo) {
        Admin admin = access.exigir(authentication, PermissaoClinica.PERFIL_EDITAR);
        if (conteudo == null || conteudo.length == 0 || conteudo.length > 2_000_000
                || !Set.of("image/png", "image/jpeg", "image/webp").contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo deve ser PNG, JPEG ou WebP com até 2 MB");
        }
        Clinica clinica = admin.getClinica();
        clinica.setDsLogoMime(mime);
        clinica.setDsLogo(conteudo);
        clinicaRepository.save(clinica);
    }

    @Transactional(readOnly = true)
    public Clinica logo(Authentication authentication) {
        Admin admin = access.exigir(authentication, PermissaoClinica.PERFIL_VER);
        if (admin.getClinica().getDsLogo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo não cadastrada");
        }
        return admin.getClinica();
    }

    @Transactional(readOnly = true)
    public PerfilDados perfilDoTutor(Authentication authentication) {
        return dados(clinicaVinculada(authentication));
    }

    @Transactional(readOnly = true)
    public Clinica logoDoTutor(Authentication authentication) {
        Clinica clinica = clinicaVinculada(authentication);
        if (clinica.getDsLogo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo não cadastrada");
        }
        return clinica;
    }

    private Clinica clinicaVinculada(Authentication authentication) {
        Tutor tutor = tutorRepository.findByDsEmail(authentication.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de tutor necessária"));
        VinculoTutorClinica vinculo = vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(tutor.getIdTutor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada"));
        if (!vinculo.getClinica().estaContratanteAtiva()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clínica com contrato inativo");
        }
        return vinculo.getClinica();
    }

    private PerfilDados dados(Clinica c) {
        return new PerfilDados(c.getIdClinica(), c.getNmClinica(), c.getDsEndereco(), c.getDsTelefone(),
                c.getDsCidade(), c.getDsUf(), c.getDsLogo() != null,
                horarioRepository.findByClinica_IdClinicaOrderByNrDiaSemanaAscHrInicioAsc(c.getIdClinica())
                        .stream().map(h -> new HorarioDados(h.getNrDiaSemana(), h.getHrInicio(), h.getHrFim())).toList());
    }

    private void validarHorarios(List<HorarioDados> horarios) {
        if (horarios == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os horários da clínica");
        }
        Map<Integer, List<HorarioDados>> porDia = new HashMap<>();
        for (HorarioDados h : horarios) {
            if (h.diaSemana() < 1 || h.diaSemana() > 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana inválido");
            }
            try {
                if (!LocalTime.parse(h.inicio()).isBefore(LocalTime.parse(h.fim()))) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intervalo de horário inválido");
            }
            porDia.computeIfAbsent(h.diaSemana(), ignored -> new ArrayList<>()).add(h);
        }
        for (var intervalos : porDia.values()) {
            intervalos.sort(Comparator.comparing(HorarioDados::inicio));
            for (int i = 1; i < intervalos.size(); i++) {
                if (intervalos.get(i - 1).fim().compareTo(intervalos.get(i).inicio()) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horários da clínica não podem se sobrepor");
                }
            }
        }
    }
}
