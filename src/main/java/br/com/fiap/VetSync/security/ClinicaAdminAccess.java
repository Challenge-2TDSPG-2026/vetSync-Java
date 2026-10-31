package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.exception.Erros;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ClinicaAdminAccess {
    private final AdminRepository adminRepository;

    public Admin atual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw Erros.autenticacaoNecessaria();
        }
        Admin admin = adminRepository.findByDsEmail(authentication.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de equipe necessária"));
        if (admin.ehGlobal() || !Boolean.TRUE.equals(admin.getAtivo())
                || !admin.getClinica().estaContratanteAtiva()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de clínica ativa necessária");
        }
        if (Boolean.TRUE.equals(admin.getTrocaSenhaObrigatoria())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Troque sua senha inicial antes de continuar");
        }
        return admin;
    }

    public Admin exigir(Authentication authentication, PermissaoClinica permissao) {
        Admin admin = atual(authentication);
        if (!Boolean.TRUE.equals(admin.getDono()) &&
                (admin.getCargo() == null || !admin.getCargo().getPermissoes().contains(permissao.name()))) {
            throw Erros.cargoSemPermissao();
        }
        return admin;
    }

    public Admin exigirAlguma(Authentication authentication, PermissaoClinica primeira, PermissaoClinica segunda) {
        Admin admin = atual(authentication);
        if (Boolean.TRUE.equals(admin.getDono()) || admin.getCargo() != null &&
                (admin.getCargo().getPermissoes().contains(primeira.name())
                        || admin.getCargo().getPermissoes().contains(segunda.name()))) {
            return admin;
        }
        throw Erros.cargoSemPermissao();
    }

    public Admin exigirDono(Authentication authentication) {
        Admin admin = atual(authentication);
        if (!Boolean.TRUE.equals(admin.getDono())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva do Dono");
        }
        return admin;
    }
}
