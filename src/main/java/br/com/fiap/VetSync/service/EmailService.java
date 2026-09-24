// EmailService.java
package br.com.fiap.VetSync.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.remetente:nao-responder@vetsync.com}")
    private String remetente;

    public void enviar(String destinatario, String assunto, String corpo) {
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            mailSender.send(mensagem);
            log.info("E-mail enviado com sucesso para: {}", destinatario);
        } catch (Exception e) {
            log.warn("Não foi possível enviar e-mail para {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Envia um e-mail com um único anexo binário (ex.: PDF da prescrição).
     * Se o anexo vier nulo/vazio, cai no envio simples (sem anexo).
     */
    public void enviarComAnexo(String destinatario, String assunto, String corpo,
                               byte[] anexo, String nomeAnexo, String tipoAnexo) {
        if (anexo == null || anexo.length == 0) {
            enviar(destinatario, assunto, corpo);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo);

            String nome = (nomeAnexo == null || nomeAnexo.isBlank()) ? "anexo.pdf" : nomeAnexo;
            String tipo = (tipoAnexo == null || tipoAnexo.isBlank()) ? "application/pdf" : tipoAnexo;
            helper.addAttachment(nome, new ByteArrayResource(anexo), tipo);

            mailSender.send(mimeMessage);
            log.info("E-mail com anexo enviado com sucesso para: {}", destinatario);
        } catch (Exception e) {
            log.warn("Não foi possível enviar e-mail com anexo para {}: {}", destinatario, e.getMessage());
        }
    }
}