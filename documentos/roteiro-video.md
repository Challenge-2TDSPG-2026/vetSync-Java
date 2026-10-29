# Roteiro do vídeo de apresentação (máx. 15 min)

Regras da entrega: **todos os membros participam**, com clareza, objetividade e domínio do que
está sendo demonstrado. O vídeo precisa passar pelos 5 critérios da rubrica.

Antes de gravar: abra em abas separadas a aplicação online, o Swagger, o repositório, o workflow
do GitHub Actions / Azure DevOps, o Azure Portal (App Service) e o `script_bd.sql`.
Teste o roteiro uma vez inteiro (inclusive o deploy) para evitar erro visual ou de fluxo.

| Tempo | Quem | Conteúdo | Critério (pontos) |
|-------|------|----------|-------------------|
| 0:00 - 1:30 | Membro 1 | Problema da Clyvo Vet (saúde do pet fragmentada), proposta e para quem gera valor (tutor, veterinário, clínica, pet) | Narrativa (20) |
| 1:30 - 5:30 | Membro 2 | **Demonstração online**: Swagger + console. Fluxo A: tutor registra, cadastra pet, agenda evento de vacina; veterinário conclui; pontos liberados; resgate validado pela clínica | Demonstração técnica (40) |
| 5:30 - 7:30 | Membro 3 | Fluxo B: convite de acesso compartilhado (expiração, permissão, revogação) e carteira pública por link/QR. Fluxo C: SIA com preview e confirmação | Demonstração técnica (40) |
| 7:30 - 9:30 | Membro 1 | Conceitos da disciplina no código: Spring Security (JWT, roles, regra por propriedade - mostrar um 403 ao acessar pet de outro tutor), Flyway (`flyway_schema_history`), validações, tratamento global de exceções, testes + JaCoCo | Demonstração técnica (40) |
| 9:30 - 11:00 | Membro 2 | Decisões de design e justificativas (tabela do README) e o que tem de original: compartilhamento granular, pontos com validade, IA com humano no controle, auditoria | Narrativa (20) |
| 11:00 - 13:30 | Membro 3 | **Integração multidisciplinar**: Banco (`script_bd.sql`, migrations), DevOps (pipeline, Key Vault, deploy), Mobile (app consumindo a API), IA/SIA, QA (Postman / Azure Boards) | Integração (20) |
| 13:30 - 14:30 | Todos | Mostrar commit -> pipeline -> deploy -> `/actuator/health`; encerramento | Oral e equipe (10) |

Dicas:
- Fale com calma e olhe o que está mostrando; evite ler o README.
- As contas de demonstração do seed não têm senha conhecida (veja `scripts/usuarios_demo.md`).
  Registre contas novas pelo fluxo de registro durante a gravação.
- Deixe o vídeo **não listado** no YouTube e cole o link no README (tabela "Links da entrega").