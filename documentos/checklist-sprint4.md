# Checklist da Sprint 4 - Java Advanced (100 pontos)

Use este arquivo para conferir a entrega antes de submeter no portal. Marque cada item.

## Rubrica

| Critério | Pontos | Evidência no repositório / entrega | OK |
|----------|-------:|------------------------------------|----|
| Demonstração técnica: aplicação online (deploy) | 40 | URL do App Service no README, `/actuator/health` respondendo `UP` | [ ] |
| Demonstração técnica: principais fluxos navegados | | Roteiro em `documentos/roteiro-video.md` | [ ] |
| Demonstração técnica: conceitos da disciplina contextualizados | | Spring Security, Flyway, validações, testes, JaCoCo | [ ] |
| Demonstração técnica: boa UI e UX | | Console (`static/index.html`) e páginas públicas (Thymeleaf) | [ ] |
| Narrativa da solução | 20 | Seções "Narrativa" e "Decisões de design" do README | [ ] |
| Integração multidisciplinar | 20 | Tabela do README + `documentos/arquitetura.md`, `script_bd.sql`, pipeline, Postman | [ ] |
| Apresentação oral e comunicação em equipe | 10 | Todos os membros falam no vídeo | [ ] |
| Organização da entrega e da documentação | 10 | README completo, `documentos/`, histórico de commits dos 3 membros | [ ] |

## Artefatos de entrega

- [ ] Repositório no GitHub (público) com README completo
- [ ] Link de acesso à aplicação (App Service)
- [ ] Vídeo de até 15 minutos publicado e linkado no README
- [ ] Entrega feita **no portal e dentro do prazo** (fora do prazo ou do portal = -100)

## Autoauditoria de penalidades

| Penalidade | Desconto | Como conferir |
|------------|----------|---------------|
| Violação evidente de boas práticas (SOLID, Clean Code) | -10 por ocorrência | Revisar classes grandes (ex.: `EventoService`, ~660 linhas) |
| Repetição de código extraível | -5 por ocorrência | Buscar blocos repetidos entre services/controllers |
| Ausência de evidência de colaboração | -10 | `git shortlog -sn` mostra os 3 membros |
| Falhas graves de usabilidade | -5 por ocorrência | Percorrer o console e as páginas públicas antes de gravar |
| Erros visuais ou de fluxo na apresentação | -5 | Ensaiar o roteiro completo |
| Falta de alinhamento com o problema proposto | -10 | Narrativa conectada ao desafio da Clyvo Vet |
| Entrega fora do prazo ou do portal | -100 | Conferir data/hora limite |

## Comandos úteis

```powershell
.\mvnw.cmd clean verify          # compila e roda todos os testes
git shortlog -sn --all           # contribuições por autor
```