package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.security.ClinicaAdminAccess;
import br.com.fiap.VetSync.security.PermissaoClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EquipeClinicaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final AdminRepository adminRepository;
    private final ClinicaRepository clinicaRepository;
    private final CargoClinicaRepository cargoRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final ClinicaAdminAccess access;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public record CargoDados(Long idCargo, String nome, Set<String> permissoes) {}
    public record MembroDados(Long idAdmin, String nome, String email, Long idCargo, String cargo,
                              boolean dono, boolean ativo, boolean trocaSenhaObrigatoria) {}

    @Transactional
    public MembroDados criarDono(Long idClinica, String nome, String email) {
        Clinica clinica = clinicaRepository.findById(idClinica).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));
        if (adminRepository.existsByClinica_IdClinicaAndDonoTrue(idClinica)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A clínica já possui Dono");
        }
        validarEmailLivre(email);
        String senha = senhaAleatoria();
        Admin admin = adminRepository.save(Admin.builder().nmAdmin(nome.trim()).dsEmail(normalizar(email))
                .dsSenha(passwordEncoder.encode(senha)).clinica(clinica).dono(true)
                .trocaSenhaObrigatoria(true).build());
        criarModelosIniciais(clinica);
        enviarCredenciais(admin, senha, true);
        return dados(admin);
    }

    @Transactional
    public MembroDados convidar(Authentication authentication, String nome, String email, Long idCargo,
                                boolean senhaTemporaria, String senhaInformada) {
        Admin solicitante = access.exigir(authentication, PermissaoClinica.EQUIPE_CONVIDAR);
        CargoClinica cargo = cargoRepository.findById(idCargo).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo não encontrado"));
        if (!cargo.getClinica().getIdClinica().equals(solicitante.getClinica().getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cargo de outra clínica");
        }
        validarEmailLivre(email);
        String senha = senhaTemporaria ? senhaAleatoria() : senhaInformada;
        if (senha == null || senha.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 6 caracteres");
        }
        Admin admin = adminRepository.save(Admin.builder().nmAdmin(nome.trim()).dsEmail(normalizar(email))
                .dsSenha(passwordEncoder.encode(senha)).clinica(solicitante.getClinica()).cargo(cargo)
                .trocaSenhaObrigatoria(senhaTemporaria).build());
        enviarCredenciais(admin, senha, senhaTemporaria);
        return dados(admin);
    }

    @Transactional(readOnly = true)
    public List<MembroDados> listarMembros(Authentication authentication) {
        Admin admin = access.exigir(authentication, PermissaoClinica.EQUIPE_VER);
        return adminRepository.findByClinica_IdClinicaOrderByNmAdmin(admin.getClinica().getIdClinica())
                .stream().map(this::dados).toList();
    }

    @Transactional(readOnly = true)
    public List<CargoDados> listarCargos(Authentication authentication) {
        Admin admin = access.exigir(authentication, PermissaoClinica.EQUIPE_VER);
        return cargoRepository.findByClinica_IdClinicaOrderByNmCargo(admin.getClinica().getIdClinica())
                .stream().map(this::dados).toList();
    }

    @Transactional
    public CargoDados salvarCargo(Authentication authentication, Long idCargo, String nome, Set<String> permissoes) {
        Admin dono = access.exigirDono(authentication);
        if (nome == null || nome.isBlank() || nome.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do cargo inválido");
        }
        Set<String> validas = new HashSet<>();
        for (String permissao : permissoes == null ? Set.<String>of() : permissoes) {
            try {
                validas.add(PermissaoClinica.valueOf(permissao).name());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permissão desconhecida: " + permissao);
            }
        }
        if (validas.contains("PERFIL_EDITAR")) validas.add("PERFIL_VER");
        if (validas.contains("SERVICOS_EDITAR")) validas.add("SERVICOS_VER");
        if (validas.stream().anyMatch(p -> p.startsWith("AGENDA_") && !p.equals("AGENDA_VER"))) validas.add("AGENDA_VER");
        if (validas.stream().anyMatch(p -> p.startsWith("MENSAGENS_") && !p.equals("MENSAGENS_VER"))) validas.add("MENSAGENS_VER");
        if (validas.contains("PONTOS_LANCAR")) validas.add("PONTOS_VER");
        if (validas.contains("EQUIPE_CONVIDAR")) validas.add("EQUIPE_VER");
        CargoClinica cargo;
        if (idCargo == null) {
            if (cargoRepository.existsByClinica_IdClinicaAndNmCargoIgnoreCase(dono.getClinica().getIdClinica(), nome.trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cargo já cadastrado");
            }
            cargo = CargoClinica.builder().clinica(dono.getClinica()).build();
        } else {
            cargo = cargoRepository.findById(idCargo).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo não encontrado"));
            if (!cargo.getClinica().getIdClinica().equals(dono.getClinica().getIdClinica())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cargo de outra clínica");
            }
        }
        cargo.setNmCargo(nome.trim());
        cargo.setPermissoes(validas);
        return dados(cargoRepository.save(cargo));
    }

    @Transactional
    public MembroDados alterarMembro(Authentication authentication, Long idAdmin, Long idCargo, boolean ativo) {
        Admin dono = access.exigirDono(authentication);
        Admin membro = adminRepository.findById(idAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        if (membro.ehGlobal() || !membro.getClinica().getIdClinica().equals(dono.getClinica().getIdClinica())
                || Boolean.TRUE.equals(membro.getDono())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membro não pode ser alterado");
        }
        CargoClinica cargo = cargoRepository.findById(idCargo).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo não encontrado"));
        if (!cargo.getClinica().getIdClinica().equals(dono.getClinica().getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cargo de outra clínica");
        }
        membro.setCargo(cargo);
        membro.setAtivo(ativo);
        return dados(adminRepository.save(membro));
    }

    private void validarEmailLivre(String email) {
        String e = normalizar(email);
        if (e.isBlank() || adminRepository.findByDsEmail(e).isPresent() || tutorRepository.existsByDsEmail(e)
                || veterinarioRepository.existsByDsEmail(e) || esteticaRepository.existsByDsEmail(e)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado ou inválido");
        }
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String senhaAleatoria() {
        StringBuilder valor = new StringBuilder(16);
        for (int i = 0; i < 16; i++) valor.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
        return valor.toString();
    }

    private void enviarCredenciais(Admin admin, String senha, boolean temporaria) {
        try {
            emailService.enviarObrigatorio(admin.getDsEmail(), "Seu acesso à clínica no VetSync",
                    "Olá, " + admin.getNmAdmin() + "!\n\nSeu acesso à clínica " + admin.getClinica().getNmClinica()
                            + " foi criado.\nE-mail: " + admin.getDsEmail() + "\nSenha: " + senha
                            + (temporaria ? "\nTroque a senha no primeiro acesso." : ""));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o convite. Nenhuma conta foi criada.");
        }
    }

    private void criarModelosIniciais(Clinica clinica) {
        cargoRepository.save(CargoClinica.builder().clinica(clinica).nmCargo("Recepção").permissoes(Set.of(
                "PERFIL_VER", "SERVICOS_VER", "AGENDA_VER", "AGENDA_CRIAR", "AGENDA_REAGENDAR", "AGENDA_CANCELAR",
                "MENSAGENS_VER", "MENSAGENS_INICIAR", "MENSAGENS_RESPONDER", "PONTOS_VER", "PONTOS_LANCAR", "EQUIPE_VER")).build());
        cargoRepository.save(CargoClinica.builder().clinica(clinica).nmCargo("Veterinário").permissoes(Set.of(
                "PERFIL_VER", "SERVICOS_VER", "AGENDA_VER", "AGENDA_REAGENDAR", "MENSAGENS_VER", "MENSAGENS_RESPONDER")).build());
        cargoRepository.save(CargoClinica.builder().clinica(clinica).nmCargo("Estética").permissoes(Set.of(
                "PERFIL_VER", "SERVICOS_VER", "AGENDA_VER", "AGENDA_REAGENDAR", "MENSAGENS_VER", "MENSAGENS_RESPONDER")).build());
    }

    private CargoDados dados(CargoClinica cargo) {
        return new CargoDados(cargo.getIdCargo(), cargo.getNmCargo(), Set.copyOf(cargo.getPermissoes()));
    }

    private MembroDados dados(Admin admin) {
        return new MembroDados(admin.getIdAdmin(), admin.getNmAdmin(), admin.getDsEmail(),
                admin.getCargo() == null ? null : admin.getCargo().getIdCargo(),
                admin.getCargo() == null ? (Boolean.TRUE.equals(admin.getDono()) ? "Dono" : null) : admin.getCargo().getNmCargo(),
                Boolean.TRUE.equals(admin.getDono()), Boolean.TRUE.equals(admin.getAtivo()),
                Boolean.TRUE.equals(admin.getTrocaSenhaObrigatoria()));
    }
}
