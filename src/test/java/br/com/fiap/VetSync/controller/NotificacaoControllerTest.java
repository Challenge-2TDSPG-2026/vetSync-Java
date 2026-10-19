package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificacaoControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired TutorRepository tutorRepository;
    @Autowired NotificacaoRepository notificacaoRepository;
    @Autowired DispositivoPushRepository dispositivoRepository;

    // ---------- helpers ----------

    private Tutor criarTutor(String prefixo) {
        int n = SEQ.incrementAndGet();
        return tutorRepository.save(Tutor.builder()
                .nmTutor("Tutor " + prefixo)
                .dsEmail((prefixo + n + "@teste.com").toLowerCase(Locale.ROOT))
                .dsCpf(String.format("%011d", n))
                .dsSenha("senha-hash")
                .build());
    }

    private String bearer(Tutor tutor) {
        return "Bearer " + jwtService.gerarToken(tutor.getDsEmail());
    }

    private Notificacao criarNotificacao(Tutor tutor, String titulo, boolean lida) {
        return notificacaoRepository.save(Notificacao.builder()
                .tutor(tutor)
                .tipo(TipoNotificacao.EVENTO_PROXIMO)
                .titulo(titulo)
                .mensagem("A vacinação está agendada para 14:00.")
                .referenciaTipo("EVENTO")
                .referenciaId(10L)
                .lida(lida)
                .build());
    }

    private static String corpoDispositivo(String token, String plataforma, String nome) {
        return """
                {"token":"%s","plataforma":"%s","nomeDispositivo":"%s","fusoHorario":"America/Sao_Paulo"}
                """.formatted(token, plataforma, nome);
    }

    private long contarDispositivos(String token) {
        return dispositivoRepository.findAll().stream()
                .filter(d -> d.getToken().equals(token)).count();
    }

    // ---------- listagem ----------

    @Test
    @DisplayName("Tutor lista suas notificações em formato paginado")
    void tutorListaNotificacoes() throws Exception {
        Tutor tutor = criarTutor("lista");
        criarNotificacao(tutor, "Vacina de Luna amanhã", false);

        mockMvc.perform(get("/notificacoes").param("page", "0").param("size", "50")
                        .header("Authorization", bearer(tutor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("EVENTO_PROXIMO"))
                .andExpect(jsonPath("$.content[0].titulo").value("Vacina de Luna amanhã"))
                .andExpect(jsonPath("$.content[0].referenciaTipo").value("EVENTO"))
                .andExpect(jsonPath("$.content[0].referenciaId").value(10))
                .andExpect(jsonPath("$.content[0].lida").value(false))
                .andExpect(jsonPath("$.content[0].criadaEm").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Lista vazia retorna content: []")
    void listaVazia() throws Exception {
        Tutor tutor = criarTutor("vazio");

        mockMvc.perform(get("/notificacoes").header("Authorization", bearer(tutor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Filtro lida=false retorna apenas não lidas")
    void filtroLidaFalse() throws Exception {
        Tutor tutor = criarTutor("filtro");
        criarNotificacao(tutor, "Não lida", false);
        criarNotificacao(tutor, "Já lida", true);

        mockMvc.perform(get("/notificacoes").param("lida", "false").param("page", "0").param("size", "50")
                        .header("Authorization", bearer(tutor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Não lida"))
                .andExpect(jsonPath("$.content[0].lida").value(false));
    }

    @Test
    @DisplayName("Tutor não vê notificações de outro tutor")
    void listagemIsolada() throws Exception {
        Tutor a = criarTutor("isolaA");
        Tutor b = criarTutor("isolaB");
        criarNotificacao(b, "Só do B", false);

        mockMvc.perform(get("/notificacoes").header("Authorization", bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------- marcar como lida ----------

    @Test
    @DisplayName("Tutor marca uma notificação como lida")
    void marcarComoLida() throws Exception {
        Tutor tutor = criarTutor("lida");
        Notificacao n = criarNotificacao(tutor, "Ler", false);

        mockMvc.perform(patch("/notificacoes/{id}/lida", n.getIdNotificacao())
                        .header("Authorization", bearer(tutor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(n.getIdNotificacao()))
                .andExpect(jsonPath("$.lida").value(true));

        assertThat(notificacaoRepository.findById(n.getIdNotificacao()).orElseThrow().isLida()).isTrue();
    }

    @Test
    @DisplayName("Marcar notificação de outro tutor retorna 404 e não altera o registro")
    void marcarNotificacaoDeOutroTutor() throws Exception {
        Tutor dono = criarTutor("dono");
        Tutor intruso = criarTutor("intruso");
        Notificacao n = criarNotificacao(dono, "Privada", false);

        mockMvc.perform(patch("/notificacoes/{id}/lida", n.getIdNotificacao())
                        .header("Authorization", bearer(intruso)))
                .andExpect(status().isNotFound());

        assertThat(notificacaoRepository.findById(n.getIdNotificacao()).orElseThrow().isLida()).isFalse();
    }

    @Test
    @DisplayName("Marcar notificação inexistente retorna 404")
    void marcarNotificacaoInexistente() throws Exception {
        Tutor tutor = criarTutor("inexistente");

        mockMvc.perform(patch("/notificacoes/{id}/lida", 99999999L)
                        .header("Authorization", bearer(tutor)))
                .andExpect(status().isNotFound());
    }

    // ---------- marcar todas ----------

    @Test
    @DisplayName("Marcar todas como lidas atualiza só as do tutor autenticado")
    void marcarTodasComoLidas() throws Exception {
        Tutor tutor = criarTutor("todas");
        Tutor outro = criarTutor("outroTodas");
        Notificacao n1 = criarNotificacao(tutor, "Um", false);
        Notificacao n2 = criarNotificacao(tutor, "Dois", false);
        Notificacao alheia = criarNotificacao(outro, "Alheia", false);

        mockMvc.perform(patch("/notificacoes/lidas").header("Authorization", bearer(tutor)))
                .andExpect(status().isOk());

        assertThat(notificacaoRepository.findById(n1.getIdNotificacao()).orElseThrow().isLida()).isTrue();
        assertThat(notificacaoRepository.findById(n2.getIdNotificacao()).orElseThrow().isLida()).isTrue();
        assertThat(notificacaoRepository.findById(alheia.getIdNotificacao()).orElseThrow().isLida()).isFalse();

        mockMvc.perform(get("/notificacoes").param("lida", "false").header("Authorization", bearer(tutor)))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- dispositivos ----------

    @Test
    @DisplayName("Tutor registra dispositivo push")
    void registrarDispositivo() throws Exception {
        Tutor tutor = criarTutor("disp");
        String token = "ExpoPushToken[abc123]";

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo(token, "ANDROID", "Dispositivo móvel")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.plataforma").value("ANDROID"))
                .andExpect(jsonPath("$.nomeDispositivo").value("Dispositivo móvel"))
                .andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(contarDispositivos(token)).isEqualTo(1);
    }

    @Test
    @DisplayName("Registro repetido não duplica e atualiza nome/fuso")
    void registroRepetidoNaoDuplica() throws Exception {
        Tutor tutor = criarTutor("repete");
        String token = "ExpoPushToken[repetido]";

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo(token, "IOS", "Primeiro nome")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo(token, "IOS", "Segundo nome")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeDispositivo").value("Segundo nome"))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(contarDispositivos(token)).isEqualTo(1);
    }

    @Test
    @DisplayName("Registro aceita WEB e rejeita plataforma inválida com 400")
    void plataformas() throws Exception {
        Tutor tutor = criarTutor("plat");

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo("tokenWeb", "WEB", "Navegador")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plataforma").value("WEB"));

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo("tokenX", "WINDOWS_PHONE", "Antigo")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tutor remove (desativa) o próprio dispositivo sem afetar o de outro tutor")
    void removerDispositivo() throws Exception {
        Tutor tutor = criarTutor("rem");
        Tutor outro = criarTutor("outroRem");
        String token = "tokenRemover";

        DispositivoPush meu = dispositivoRepository.save(DispositivoPush.builder()
                .tutor(tutor).token(token).plataforma(PlataformaPush.ANDROID).build());
        DispositivoPush alheio = dispositivoRepository.save(DispositivoPush.builder()
                .tutor(outro).token("tokenAlheio").plataforma(PlataformaPush.ANDROID).build());

        mockMvc.perform(delete("/notificacoes/dispositivos/{token}", token)
                        .header("Authorization", bearer(tutor)))
                .andExpect(status().isNoContent());

        assertThat(dispositivoRepository.findById(meu.getIdDispositivo()).orElseThrow().isAtivo()).isFalse();
        assertThat(dispositivoRepository.findById(alheio.getIdDispositivo()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Tutor não desativa o dispositivo de outro tutor pelo token")
    void removerTokenDeOutroTutor() throws Exception {
        Tutor dono = criarTutor("donoDisp");
        Tutor intruso = criarTutor("intrusoDisp");
        DispositivoPush d = dispositivoRepository.save(DispositivoPush.builder()
                .tutor(dono).token("tokenDoDono").plataforma(PlataformaPush.IOS).build());

        mockMvc.perform(delete("/notificacoes/dispositivos/{token}", "tokenDoDono")
                        .header("Authorization", bearer(intruso)))
                .andExpect(status().isNoContent());

        assertThat(dispositivoRepository.findById(d.getIdDispositivo()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Mesmo aparelho em nova conta desativa o token na conta anterior")
    void aparelhoTrocaDeConta() throws Exception {
        Tutor antigo = criarTutor("antigo");
        Tutor novo = criarTutor("novo");
        String token = "ExpoPushToken[compartilhado]";
        DispositivoPush doAntigo = dispositivoRepository.save(DispositivoPush.builder()
                .tutor(antigo).token(token).plataforma(PlataformaPush.ANDROID).build());

        mockMvc.perform(post("/notificacoes/dispositivos")
                        .header("Authorization", bearer(novo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo(token, "ANDROID", "Celular")))
                .andExpect(status().isCreated());

        assertThat(dispositivoRepository.findById(doAntigo.getIdDispositivo()).orElseThrow().isAtivo()).isFalse();
    }

    // ---------- preferências ----------

    @Test
    @DisplayName("Preferências: cria padrão no GET e persiste alterações no PUT")
    void preferencias() throws Exception {
        Tutor tutor = criarTutor("pref");

        mockMvc.perform(get("/usuarios/preferencias/notificacoes").header("Authorization", bearer(tutor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushAtivo").value(true))
                .andExpect(jsonPath("$.lembreteSeteDias").value(true))
                .andExpect(jsonPath("$.lembreteUmDia").value(true))
                .andExpect(jsonPath("$.lembreteDuasHoras").value(false))
                .andExpect(jsonPath("$.vacinasVencendo").value(true))
                .andExpect(jsonPath("$.retornosPendentes").value(true))
                .andExpect(jsonPath("$.convitesDeAcesso").value(true))
                .andExpect(jsonPath("$.resgates").value(true));

        mockMvc.perform(put("/usuarios/preferencias/notificacoes")
                        .header("Authorization", bearer(tutor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pushAtivo":false,"lembreteSeteDias":false,"lembreteUmDia":true,
                                 "lembreteDuasHoras":true,"vacinasVencendo":false,"retornosPendentes":true,
                                 "convitesDeAcesso":false,"resgates":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushAtivo").value(false))
                .andExpect(jsonPath("$.lembreteDuasHoras").value(true));

        mockMvc.perform(get("/usuarios/preferencias/notificacoes").header("Authorization", bearer(tutor)))
                .andExpect(jsonPath("$.pushAtivo").value(false))
                .andExpect(jsonPath("$.lembreteSeteDias").value(false))
                .andExpect(jsonPath("$.vacinasVencendo").value(false))
                .andExpect(jsonPath("$.convitesDeAcesso").value(false));
    }

    // ---------- segurança ----------

    @Test
    @DisplayName("Sem autenticação: 401 em todas as rotas de notificação")
    void semAutenticacao() throws Exception {
        mockMvc.perform(get("/notificacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/notificacoes/1/lida")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/notificacoes/lidas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/notificacoes/dispositivos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo("t", "ANDROID", "x")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/notificacoes/dispositivos/{token}", "t")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/usuarios/preferencias/notificacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT inválido retorna 401")
    void jwtInvalido() throws Exception {
        mockMvc.perform(get("/notificacoes").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    @DisplayName("Veterinário recebe 403 em notificações e preferências de tutor")
    void veterinarioNaoAcessa() throws Exception {
        mockMvc.perform(get("/notificacoes")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/notificacoes/lidas")).andExpect(status().isForbidden());
        mockMvc.perform(post("/notificacoes/dispositivos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDispositivo("t", "ANDROID", "x")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios/preferencias/notificacoes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "semtutor@teste.com", roles = "TUTOR")
    @DisplayName("Usuário com role TUTOR mas sem registro de tutor recebe 403 do service")
    void semRegistroDeTutor() throws Exception {
        mockMvc.perform(get("/notificacoes")).andExpect(status().isForbidden());
    }
}
