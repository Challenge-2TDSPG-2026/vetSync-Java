package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.RecompensaRepository;
import br.com.fiap.VetSync.repository.ResgateRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecompensaService {

    private final RecompensaRepository recompensaRepository;
    private final ResgateRepository resgateRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final TutorService tutorService;
    private final PontosService pontosService;

    private static final long TAMANHO_MAXIMO_IMAGEM_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public Recompensa criar(String nome, String descricao, Integer custoPontos, TipoRecompensa tipo) {
        return criar(nome, descricao, custoPontos, tipo, null);
    }

    public Recompensa criar(String nome, String descricao, Integer custoPontos, TipoRecompensa tipo, MultipartFile imagem) {
        Recompensa.RecompensaBuilder builder = Recompensa.builder()
                .nmRecompensa(nome)
                .dsDescricao(descricao)
                .nrCustoPontos(custoPontos)
                .dsTipo(tipo)
                .flAtivo(true);

        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            try {
                builder.dsImagem(imagem.getBytes())
                        .dsImagemTipo(imagem.getContentType());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado");
            }
        }

        return recompensaRepository.save(builder.build());
    }

    private void validarImagem(MultipartFile imagem) {
        if (imagem.getSize() > TAMANHO_MAXIMO_IMAGEM_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 5MB");
        }
        String contentType = imagem.getContentType();
        if (contentType == null || !TIPOS_IMAGEM_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de imagem inválido. Envie um arquivo JPEG, PNG ou WEBP");
        }
    }

    public List<Recompensa> listarAtivas() {
        return recompensaRepository.findByFlAtivoTrue();
    }

    public Recompensa buscarPorId(Long id) {
        return recompensaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recompensa não encontrada com id: " + id)
        );
    }




    public int calcularSaldo(Long idTutor) {
        int ganhos = pontosService.calcularPontosLiberados(idTutor);

        int gastos = resgateRepository.findByTutor_IdTutorOrderByDtResgateDesc(idTutor).stream()
                .filter(r -> r.getDsStatus() == StatusResgate.VALIDADO)
                .mapToInt(r -> r.getRecompensa().getNrCustoPontos())
                .sum();

        return ganhos - gastos;
    }



    public Resgate solicitarResgate(Long idTutor, Long idRecompensa) {
        Recompensa recompensa = buscarPorId(idRecompensa);
        if (!Boolean.TRUE.equals(recompensa.getFlAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa recompensa não está mais disponível");
        }
        int saldo = calcularSaldo(idTutor);
        if (saldo < recompensa.getNrCustoPontos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Saldo insuficiente: você tem " + saldo + " pontos, precisa de " + recompensa.getNrCustoPontos());
        }
        Resgate resgate = Resgate.builder()
                .tutor(tutorService.buscarPorId(idTutor))
                .recompensa(recompensa)
                .dsStatus(StatusResgate.PENDENTE)
                .build();
        return resgateRepository.save(resgate);
    }

    public List<Resgate> listarResgatesDoTutor(Long idTutor) {
        return resgateRepository.findByTutor_IdTutorOrderByDtResgateDesc(idTutor);
    }

    public List<Resgate> listarPendentes() {
        return resgateRepository.findByDsStatusOrderByDtResgateAsc(StatusResgate.PENDENTE);
    }

    public Resgate validar(Long idResgate, Long idVeterinario, boolean aprovado) {
        Resgate resgate = resgateRepository.findById(idResgate).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resgate não encontrado")
        );
        if (resgate.getDsStatus() != StatusResgate.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse resgate já foi " + resgate.getDsStatus());
        }
        Veterinario vet = veterinarioRepository.findById(idVeterinario).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado")
        );
        resgate.setVeterinarioValidador(vet);
        resgate.setDsStatus(aprovado ? StatusResgate.VALIDADO : StatusResgate.NEGADO);
        return resgateRepository.save(resgate);
    }
}