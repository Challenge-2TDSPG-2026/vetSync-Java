package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
}