package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.CarteiraCompartilhada;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.CarteiraCompartilhadaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarteiraCompartilhadaService {

    private static final String NOME_TIPO_EVENTO_VACINA = "Vacina";
    private static final int TOKEN_BYTES = 32; // mínimo exigido: 32 bytes

    private final CarteiraCompartilhadaRepository carteiraCompartilhadaRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final PetService petService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.carteira-compartilhada.expiracao-dias:30}")
    private int expiracaoDias;

    public record CarteiraCriada(CarteiraCompartilhada carteira, String tokenPuro) {}

    public record VacinaPublica(String nome, LocalDate data, String status) {}

    public record CarteiraPublica(
            String nomePet, String especie, String raca, LocalDateTime atualizadaEm, List<VacinaPublica> vacinas
    ) {}


    public CarteiraCriada criarOuRenovar(Long idPet, Long idTutorCriador) {
        Pet pet = petService.buscarPorId(idPet);

        revogarAtivasDoPet(idPet);

        String tokenPuro = gerarTokenAleatorio();
        String hash = calcularHash(tokenPuro);

        LocalDateTime agora = LocalDateTime.now();
        CarteiraCompartilhada carteira = CarteiraCompartilhada.builder()
                .pet(pet)
                .tokenHash(hash)
                .criadaEm(agora)
                .expiraEm(agora.plusDays(expiracaoDias))
                .idUsuarioCriador(idTutorCriador)
                .build();

        carteira = carteiraCompartilhadaRepository.save(carteira);
        return new CarteiraCriada(carteira, tokenPuro);
    }

    public CarteiraCompartilhada buscarAtivaDoPet(Long idPet) {
        return carteiraCompartilhadaRepository.findByPet_IdPetAndRevogadaEmIsNullOrderByCriadaEmDesc(idPet).stream()
                .filter(CarteiraCompartilhada::isAtiva)
                .max(Comparator.comparing(CarteiraCompartilhada::getCriadaEm))
                .orElse(null);
    }

    public void revogar(Long idPet, Long idCarteira) {
        CarteiraCompartilhada carteira = carteiraCompartilhadaRepository.findById(idCarteira)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carteira compartilhável não encontrada"));

        if (carteira.getPet() == null || !idPet.equals(carteira.getPet().getIdPet())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carteira compartilhável não encontrada para esse pet");
        }
        if (carteira.getRevogadaEm() == null) {
            carteira.setRevogadaEm(LocalDateTime.now());
            carteiraCompartilhadaRepository.save(carteira);
        }
    }

    private void revogarAtivasDoPet(Long idPet) {
        List<CarteiraCompartilhada> ativas = carteiraCompartilhadaRepository
                .findByPet_IdPetAndRevogadaEmIsNullOrderByCriadaEmDesc(idPet);
        LocalDateTime agora = LocalDateTime.now();
        for (CarteiraCompartilhada c : ativas) {
            c.setRevogadaEm(agora);
        }
        carteiraCompartilhadaRepository.saveAll(ativas);
    }


    public CarteiraPublica resolverParaExibicaoPublica(String tokenPuro) {
        if (tokenPuro == null || tokenPuro.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String hash = calcularHash(tokenPuro);
        CarteiraCompartilhada carteira = carteiraCompartilhadaRepository.findByTokenHash(hash).orElse(null);

        if (carteira == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (carteira.getRevogadaEm() != null) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }
        if (carteira.getExpiraEm() == null || !carteira.getExpiraEm().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }

        carteira.setUltimoAcessoEm(LocalDateTime.now());
        carteiraCompartilhadaRepository.save(carteira);

        Pet pet = carteira.getPet();
        List<VacinaPublica> vacinas = eventoSaudeRepository.findByPet_IdPet(pet.getIdPet()).stream()
                .filter(this::isVacina)
                .filter(e -> e.getDsStatus() != StatusEvento.CANCELADO)
                .sorted(Comparator.comparing(EventoSaude::getDtEvento).reversed())
                .map(this::toVacinaPublica)
                .toList();

        String especie = pet.getRaca() != null && pet.getRaca().getEspecie() != null
                ? pet.getRaca().getEspecie().getNmEspecie() : null;
        String raca = pet.getRaca() != null ? pet.getRaca().getNmRaca() : null;

        // A página é montada na hora, a partir dos dados atuais do pet — "atualizada em"
        // reflete o momento deste acesso, não a data de criação do link.
        return new CarteiraPublica(pet.getNmPet(), especie, raca, LocalDateTime.now(), vacinas);
    }

    private boolean isVacina(EventoSaude evento) {
        return evento.getTipoEvento() != null
                && NOME_TIPO_EVENTO_VACINA.equalsIgnoreCase(evento.getTipoEvento().getNmTipoEvento());
    }

    private VacinaPublica toVacinaPublica(EventoSaude evento) {
        String status = switch (evento.getDsStatus()) {
            case CONCLUIDO -> "Realizada";
            case AGENDADO -> evento.getDtEvento() != null && evento.getDtEvento().isBefore(LocalDate.now())
                    ? "Atrasada" : "Agendada";
            case CANCELADO -> "Cancelada";
        };
        return new VacinaPublica(evento.getTipoEvento().getNmTipoEvento(), evento.getDtEvento(), status);
    }

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String calcularHash(String tokenPuro) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(tokenPuro.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }
}