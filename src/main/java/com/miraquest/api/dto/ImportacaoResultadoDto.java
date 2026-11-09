package com.miraquest.api.dto;

import java.util.List;

/**
 * Resultado da importação CSV. {@code linha} conta registros do arquivo, sendo 1 o cabeçalho.
 * Em simulação nada é gravado: {@code importadas} indica quantas seriam gravadas.
 */
public record ImportacaoResultadoDto(boolean simulacao, int total, int importadas, int duplicadas,
		List<ErroLinha> erros) {

	public record ErroLinha(int linha, String mensagem) {

	}

}
