package com.miraquest.api.dto;

import com.miraquest.api.model.TipoQuestao;

public record QuestaoLicaoDto(Long id, String materia, TipoQuestao tipo, String enunciado, String opcoesDistracao) {

}
