package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Monta e envia o e-mail de convite de acesso a um pet (ou a todos os pets do tutor). */
@Service
@RequiredArgsConstructor
public class ConviteEmailService {

    private final EmailService emailService;
    private final TutorRepository tutorRepository;

    @Value("${app.convite.web-invite-url}")
    private String webInviteUrl;

    public void enviar(PetConvite convite, String tokenPuro, int expiracaoHoras) {
        String link = webInviteUrl + "?token=" + tokenPuro;
        boolean contaExistente = tutorRepository.existsByDsEmail(convite.getDsEmailDestino());
        String escopo = convite.isTodosPets()
                ? "todos os pets de " + convite.getTutorOrigem().getNmTutor()
                : convite.getPet().getNmPet();
        String assunto = convite.isTodosPets()
                ? "Você foi convidado como responsável no VetSync"
                : "Você foi convidado para cuidar de " + convite.getPet().getNmPet() + " no VetSync";
        String corpo = "Olá!\n\n"
                + convite.getTutorOrigem().getNmTutor() + " convidou você para acompanhar "
                + escopo + " no VetSync"
                + (convite.isTodosPets() ? ".\n\n" : ", como " + rotuloRelacao(convite.getDsRelacao()) + ".\n\n")
                + (contaExistente
                    ? "Você já tem conta no VetSync com este e-mail. Abra o app (ou o link abaixo), entre na sua conta e aceite o convite:\n"
                    : "Para aceitar o convite, acesse o link abaixo e finalize seu cadastro:\n")
                + link + "\n\n"
                + "Este link expira em " + expiracaoHoras + " horas.\n"
                + "Se você não esperava este convite, pode ignorar este e-mail.";
        emailService.enviarObrigatorio(convite.getDsEmailDestino(), assunto, corpo);
    }

    private String rotuloRelacao(RelacaoPet relacao) {
        return switch (relacao) {
            case CUIDADOR -> "cuidador(a)";
            case CONJUGE -> "cônjuge";
            case OUTRO -> "pessoa de confiança";
        };
    }
}
