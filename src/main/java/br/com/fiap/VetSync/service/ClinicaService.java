package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClinicaService {

    private final ClinicaRepository clinicaRepository;

    /** Busca a clínica escolhida pelo admin. O id é obrigatório e precisa existir. */
    public Clinica buscarObrigatoria(Long idClinica) {
        if (idClinica == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idClinica é obrigatório");
        }
        return clinicaRepository.findById(idClinica).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada com id: " + idClinica));
    }

    /** Cadastra uma clinica (inativa por padrao); recusa CNPJ repetido e campos acima do limite. */
    public Clinica criar(String nome, String cnpj, String cidade, String uf) {
        if (nome.length() > 150 || cidade.length() > 80 || clinicaRepository.existsByDsCnpj(cnpj)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Clínica ou CNPJ inválido ou já cadastrado");
        }
        return clinicaRepository.save(Clinica.builder().nmClinica(nome.trim()).dsCnpj(cnpj)
                .dsCidade(cidade.trim()).dsUf(uf.toUpperCase(Locale.ROOT)).build());
    }
}
