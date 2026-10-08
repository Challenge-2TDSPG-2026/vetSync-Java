package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.service.JwtService;
import br.com.fiap.VetSync.repository.AdminRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final TokenBlacklist tokenBlacklist;
    private final AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!tokenBlacklist.isRevogado(token) && jwtService.tokenValido(token)) {
                String email = jwtService.extrairEmail(token);
                boolean emitidoDepoisDaTroca = userDetailsService.ultimaAlteracaoSenha(email)
                        .map(alteradaEm -> !jwtService.extrairDataEmissao(token).before(
                                java.util.Date.from(alteradaEm.toInstant(java.time.ZoneOffset.UTC))))
                        .orElse(true);
                if (emitidoDepoisDaTroca && !tokenBlacklist.isRevogadoParaUsuario(
                        email, jwtService.extrairDataEmissao(token))) {
                    var user = userDetailsService.loadUserByUsername(email);
                    if (!user.isEnabled()) {
                        chain.doFilter(req, res);
                        return;
                    }
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    boolean trocaPendente = adminRepository.findByDsEmail(email)
                            .map(a -> !a.ehGlobal() && Boolean.TRUE.equals(a.getTrocaSenhaObrigatoria()))
                            .orElse(false);
                    String rota = req.getServletPath();
                    if (trocaPendente && !rota.equals("/auth/me") && !rota.equals("/auth/logout")
                            && !rota.equals("/auth/trocar-senha-inicial")) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Troque sua senha inicial antes de continuar");
                        return;
                    }
                    boolean contaClinica = user.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_CLINICA"));
                    if (contaClinica && !rota.startsWith("/clinica-admin/")
                            && !rota.equals("/mensagens-clinica") && !rota.startsWith("/mensagens-clinica/")
                            && !rota.equals("/auth/me") && !rota.equals("/auth/logout")
                            && !rota.equals("/auth/trocar-senha-inicial")) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Use as operações da sua clínica");
                        return;
                    }
                }
            }
        }
        chain.doFilter(req, res);
    }
}
