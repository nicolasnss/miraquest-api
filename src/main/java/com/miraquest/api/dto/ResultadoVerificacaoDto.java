package com.miraquest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta JSON com chaves exatas: sucesso, explicacao, xpGanho, pegadinha (nula se a questão não tiver). */
public record ResultadoVerificacaoDto(
		@JsonProperty("sucesso") boolean sucesso,
		@JsonProperty("explicacao") String explicacao,
		@JsonProperty("xpGanho") int xpGanho,
		@JsonProperty("pegadinha") String pegadinha) {

}
