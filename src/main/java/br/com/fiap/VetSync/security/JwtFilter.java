package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.service.JwtService;
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
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(req, res);
    }
}