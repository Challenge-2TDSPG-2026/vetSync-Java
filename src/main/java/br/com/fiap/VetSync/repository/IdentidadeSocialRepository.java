package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.IdentidadeSocial;
import br.com.fiap.VetSync.social.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentidadeSocialRepository extends JpaRepository<IdentidadeSocial, Long> {

    Optional<IdentidadeSocial> findByProvedorAndSubject(SocialProvider provedor, String subject);

    boolean existsByTutor_IdTutorAndProvedor(Long idTutor, SocialProvider provedor);
}