package com.miraquest.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum TipoQuestao {

	CLOZE,
	CERTO_ERRADO,
	MULTIPLA_ESCOLHA

}
