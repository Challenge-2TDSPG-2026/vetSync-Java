package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.CarteiraCompartilhada;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.service.CarteiraCompartilhadaService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/pets/{idPet}/carteira-compartilhavel")
@RequiredArgsConstructor
@Tag(name = "Carteira Compartilhável", description = "Link público (QR Code) da carteira de vacinação do pet")
public class CarteiraCompartilhadaController {

    private final CarteiraCompartilhadaService carteiraCompartilhadaService;
    private final TutorService tutorService;

    public record CarteiraCriadaResponse(Long id, String urlPublica, LocalDateTime expiraEm) {}

    public record CarteiraAtivaResponse(
            Long id, LocalDateTime criadaEm, LocalDateTime expiraEm, LocalDateTime ultimoAcessoEm
    ) {}

    private Long idTutorAutenticado(Authentication authentication) {
        Tutor tutor = tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"));
        return tutor.getIdTutor();
    }

    private String montarUrlPublica(HttpServletRequest request, String tokenPuro) {
        return UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(request.getServerPort())
                .path("/carteiras-publicas/{token}")
                .buildAndExpand(tokenPuro)
                .toUriString();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR') and @petSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Criar ou renovar o link público da carteira de vacinação",
            description = "Gera um novo token, revoga qualquer link anterior e devolve a URL pública. " +
                    "O token só é exibido nesta resposta; não é possível recuperá-lo depois.")
    public CarteiraCriadaResponse criarOuRenovar(@PathVariable Long idPet, Authentication authentication,
                                                 HttpServletRequest request) {
        Long idTutor = idTutorAutenticado(authentication);
        var criada = carteiraCompartilhadaService.criarOuRenovar(idPet, idTutor);
        String urlPublica = montarUrlPublica(request, criada.tokenPuro());
        return new CarteiraCriadaResponse(
                criada.carteira().getIdCarteiraCompartilhada(), urlPublica, criada.carteira().getExpiraEm()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('TUTOR') and @petSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Consultar o link ativo do pet, se existir",
            description = "Não devolve a URL/token novamente (só é exibido na criação); " +
                    "use para saber se já existe um link ativo e quando ele expira.")
    public CarteiraAtivaResponse buscarAtiva(@PathVariable Long idPet) {
        CarteiraCompartilhada ativa = carteiraCompartilhadaService.buscarAtivaDoPet(idPet);
        if (ativa == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum link ativo para esse pet");
        }
        return new CarteiraAtivaResponse(
                ativa.getIdCarteiraCompartilhada(), ativa.getCriadaEm(), ativa.getExpiraEm(), ativa.getUltimoAcessoEm()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @petSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Revogar o link público imediatamente", description = "O QR Code anterior deixa de funcionar.")
    public void revogar(@PathVariable Long idPet, @PathVariable Long id) {
        carteiraCompartilhadaService.revogar(idPet, id);
    }
}