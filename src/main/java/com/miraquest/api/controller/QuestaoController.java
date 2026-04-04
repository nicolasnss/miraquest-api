package com.miraquest.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miraquest.api.dto.QuestaoLicaoDto;
import com.miraquest.api.dto.ResultadoVerificacaoDto;
import com.miraquest.api.dto.UsuarioProgressoDto;
import com.miraquest.api.dto.VerificarQuestaoRequest;
import com.miraquest.api.service.QuestaoService;

@RestController
@RequestMapping("/api")
public class QuestaoController {

	private final QuestaoService questaoService;

	public QuestaoController(QuestaoService questaoService) {
		this.questaoService = questaoService;
	}

	@GetMapping("/questoes/licao")
	public List<QuestaoLicaoDto> obterLicao(@RequestParam String materia) {
		return questaoService.obterLicao(materia);
	}

	/**
	 * Corpo: usuarioId, questaoId, resposta, materia (opcional).
	 */
	@PostMapping("/questoes/verificar")
	public ResultadoVerificacaoDto verificarQuestao(@RequestBody VerificarQuestaoRequest body) {
		return questaoService.verificarQuestao(body.usuarioId(), body);
	}

	@GetMapping("/usuario/{id}")
	public UsuarioProgressoDto obterProgressoUsuario(@PathVariable Long id) {
		return questaoService.obterProgressoUsuario(id);
	}

}
