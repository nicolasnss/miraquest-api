package com.miraquest.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miraquest.api.model.UsuarioProgresso;

public interface UsuarioProgressoRepository extends JpaRepository<UsuarioProgresso, Long> {

	Optional<UsuarioProgresso> findByEmailUsuarioIgnoreCase(String emailUsuario);

}
