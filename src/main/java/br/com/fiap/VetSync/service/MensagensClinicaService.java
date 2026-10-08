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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MensagensClinicaService {
    private final ConversaClinicaRepository conversaRepository;
    private final MensagemClinicaRepository mensagemRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    private final TutorRepository tutorRepository;
    private final ClinicaAdminAccess access;

    public record ConversaDados(Long idConversa, Long idClinica, String nomeClinica,
                                Long idTutor, String nomeTutor, LocalDateTime criadaEm) {}
    public record MensagemDados(Long idMensagem, Long idConversa, String remetente,
                                String texto, LocalDateTime enviadaEm) {}

    @Transactional(readOnly = true)
    public List<ConversaDados> listar(Authentication auth) {
        if (tutor(auth)) {
            Tutor usuario = tutorAtual(auth);
            return conversaRepository.findByTutor_IdTutorOrderByDtCriacaoDesc(usuario.getIdTutor())
                    .stream().map(this::dados).toList();
        }
        Admin admin = access.exigir(auth, PermissaoClinica.MENSAGENS_VER);
        return conversaRepository.findByClinica_IdClinicaOrderByDtCriacaoDesc(admin.getClinica().getIdClinica())
                .stream().map(this::dados).toList();
    }

    @Transactional
    public ConversaDados iniciar(Authentication auth, Long idTutor) {
        Tutor destino;
        Clinica clinica;
        if (tutor(auth)) {
            destino = tutorAtual(auth);
            VinculoTutorClinica vinculo = vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(destino.getIdTutor())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada"));
            clinica = vinculo.getClinica();
        } else {
            Admin admin = access.exigir(auth, PermissaoClinica.MENSAGENS_INICIAR);
            clinica = admin.getClinica();
            if (idTutor == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um tutor");
            }
            destino = tutorRepository.findById(idTutor).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
            if (!vinculoRepository.existsByTutor_IdTutorAndClinica_IdClinicaAndDtEncerramentoIsNull(
                    destino.getIdTutor(), clinica.getIdClinica())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor não vinculado a esta clínica");
            }
        }
        ConversaClinica conversa = conversaRepository.findByClinica_IdClinicaAndTutor_IdTutor(
                clinica.getIdClinica(), destino.getIdTutor()).orElseGet(() -> conversaRepository.save(
                ConversaClinica.builder().clinica(clinica).tutor(destino).build()));
        return dados(conversa);
    }

    @Transactional(readOnly = true)
    public List<MensagemDados> historico(Authentication auth, Long idConversa) {
        ConversaClinica conversa = autorizada(auth, idConversa, PermissaoClinica.MENSAGENS_VER);
        return mensagemRepository.findByConversa_IdConversaOrderByDtEnvioAsc(conversa.getIdConversa())
                .stream().map(this::dados).toList();
    }

    @Transactional
    public MensagemDados enviar(Authentication auth, Long idConversa, String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem deve ter entre 1 e 2000 caracteres");
        }
        ConversaClinica conversa = autorizada(auth, idConversa, PermissaoClinica.MENSAGENS_RESPONDER);
        if (!vinculoRepository.existsByTutor_IdTutorAndClinica_IdClinicaAndDtEncerramentoIsNull(
                conversa.getTutor().getIdTutor(), conversa.getClinica().getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor não está mais vinculado à clínica");
        }
        Admin admin = tutor(auth) ? null : access.atual(auth);
        MensagemClinica mensagem = mensagemRepository.save(MensagemClinica.builder().conversa(conversa).admin(admin)
                .dsRemetente(admin == null ? "TUTOR" : "CLINICA").dsTexto(texto.trim()).build());
        return dados(mensagem);
    }

    private ConversaClinica autorizada(Authentication auth, Long id, PermissaoClinica permissao) {
        ConversaClinica conversa = conversaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada"));
        if (tutor(auth)) {
            if (!conversa.getTutor().getIdTutor().equals(tutorAtual(auth).getIdTutor())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversa de outro tutor");
            }
        } else {
            Admin admin = access.exigir(auth, permissao);
            if (!conversa.getClinica().getIdClinica().equals(admin.getClinica().getIdClinica())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversa de outra clínica");
            }
        }
        return conversa;
    }

    private boolean tutor(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TUTOR"));
    }

    private Tutor tutorAtual(Authentication auth) {
        return tutorRepository.findByDsEmail(auth.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de tutor necessária"));
    }

    private ConversaDados dados(ConversaClinica c) {
        return new ConversaDados(c.getIdConversa(), c.getClinica().getIdClinica(), c.getClinica().getNmClinica(),
                c.getTutor().getIdTutor(), c.getTutor().getNmTutor(), c.getDtCriacao());
    }

    private MensagemDados dados(MensagemClinica m) {
        return new MensagemDados(m.getIdMensagem(), m.getConversa().getIdConversa(),
                m.getDsRemetente(), m.getDsTexto(), m.getDtEnvio());
    }
}
