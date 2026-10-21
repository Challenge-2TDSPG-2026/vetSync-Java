# Contas de demonstração VetSync

Estas são as contas fictícias inseridas pela carga de demonstração em `seed_demo_agenda.sql`.
Os CPFs e telefones abaixo são dados de teste, não pertencem a pessoas reais.

## Tutores

| Nome | E-mail (login) | Telefone de teste | CPF de teste |
|---|---|---|---|
| Rafael Oliveira | `tutor01@demo.vetsync.local` | `11990000001` | `99999990001` |
| Camila Santos | `tutor02@demo.vetsync.local` | `11990000002` | `99999990002` |
| Bruno Almeida | `tutor03@demo.vetsync.local` | `11990000003` | `99999990003` |
| Fernanda Lima | `tutor04@demo.vetsync.local` | `11990000004` | `99999990004` |
| Lucas Ferreira | `tutor05@demo.vetsync.local` | `11990000005` | `99999990005` |
| Mariana Costa | `tutor06@demo.vetsync.local` | `11990000006` | `99999990006` |
| Rafael Oliveira | `tutor07@demo.vetsync.local` | `11990000007` | `99999990007` |
| Camila Santos | `tutor08@demo.vetsync.local` | `11990000008` | `99999990008` |
| Bruno Almeida | `tutor09@demo.vetsync.local` | `11990000009` | `99999990009` |
| Fernanda Lima | `tutor10@demo.vetsync.local` | `11990000010` | `99999990010` |
| Lucas Ferreira | `tutor11@demo.vetsync.local` | `11990000011` | `99999990011` |
| Mariana Costa | `tutor12@demo.vetsync.local` | `11990000012` | `99999990012` |

## Veterinários

| Nome | E-mail (login) | CRMV fictício | Clínica | Especialidade |
|---|---|---|---|---|
| Diego Martins | `vet01@demo.vetsync.local` | `CRMV-SP-DEMO-001` | Clinica Veterinaria Parque | Dermatologia |
| Juliana Carvalho | `vet02@demo.vetsync.local` | `CRMV-SP-DEMO-002` | Clinica Veterinaria Parque | Medicina felina |
| Pedro Henrique Souza | `vet03@demo.vetsync.local` | `CRMV-SP-DEMO-003` | Centro Veterinario Vila Nova | Ortopedia |
| Beatriz Mendes | `vet04@demo.vetsync.local` | `CRMV-SP-DEMO-004` | Centro Veterinario Vila Nova | Clinica geral |
| Ana Paula Ribeiro | `vet05@demo.vetsync.local` | `CRMV-SP-DEMO-005` | Hospital Veterinario Horizonte | Dermatologia |
| Diego Martins | `vet06@demo.vetsync.local` | `CRMV-SP-DEMO-006` | Hospital Veterinario Horizonte | Medicina felina |
| Juliana Carvalho | `vet07@demo.vetsync.local` | `CRMV-SP-DEMO-007` | Clinica Bicho Feliz | Ortopedia |
| Pedro Henrique Souza | `vet08@demo.vetsync.local` | `CRMV-SP-DEMO-008` | Clinica Bicho Feliz | Clinica geral |

## Senha armazenada

O banco guarda apenas este hash BCrypt, igual nas 20 contas:

```text
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJLaR3VQw0
```

Hash de senha não pode ser convertido de volta para a senha em texto claro. Conferi que ele **não** corresponde a `password`; portanto não tenho como informar a senha atual nem garantir que essas contas aceitem login com uma senha conhecida. A sugestão anterior de usar `password` estava errada.

Para habilitar logins de demonstração com uma senha conhecida, será necessário definir uma nova senha e atualizar o hash dessas contas no banco e na carga SQL.
