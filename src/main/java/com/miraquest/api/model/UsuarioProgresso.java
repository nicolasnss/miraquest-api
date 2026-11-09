package com.miraquest.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_progresso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioProgresso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String emailUsuario;

	/** Hash BCrypt; nulo para usuários de seed sem login. */
	private String senhaHash;

	@Column(nullable = false)
	private int vidas = 5;

	@Column(nullable = false)
	private int xpTotal;

	@Column(nullable = false)
	private int streakDias;

	private LocalDate dataUltimaRespostaCorreta;

}
