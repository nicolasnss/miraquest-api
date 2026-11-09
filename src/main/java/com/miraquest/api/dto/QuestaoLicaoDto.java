package com.miraquest.api.dto;

import java.util.List;

import com.miraquest.api.model.TipoQuestao;

/**
 * Questão enviada ao app. {@code opcoes} já vem embaralhada (distratores + correta) e nunca indica qual é a certa.
 * Vazia para CERTO_ERRADO.
 */
public record QuestaoLicaoDto(Long id, String materia, TipoQuestao tipo, String enunciado, List<String> opcoes) {

}
