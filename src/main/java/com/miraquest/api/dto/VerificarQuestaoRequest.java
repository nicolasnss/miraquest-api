package com.miraquest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerificarQuestaoRequest(
		@JsonProperty("usuarioId") Long usuarioId,
		@JsonProperty("questaoId") Long questaoId,
		@JsonProperty("resposta") String resposta,
		@JsonProperty("materia") String materia) {

}
