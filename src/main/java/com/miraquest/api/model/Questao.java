package com.miraquest.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Questao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String materia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoQuestao tipo;

	@Column(columnDefinition = "TEXT")
	private String enunciado;

	private String respostaCorreta;

	@Column(columnDefinition = "TEXT")
	private String opcoesDistracao;

	@Column(columnDefinition = "TEXT")
	private String explicacao;

}
