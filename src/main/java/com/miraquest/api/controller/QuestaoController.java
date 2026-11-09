package com.miraquest.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.MateriaDto;
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

	@GetMapping("/materias")
	public List<MateriaDto> listarMaterias() {
		return questaoService.listarMaterias();
	}

	@GetMapping("/questoes/licao")
	public List<QuestaoLicaoDto> obterLicao(@RequestParam String materia) {
		return questaoService.obterLicao(materia);
	}

	/**
	 * Corpo: usuarioId, questaoId, resposta, materia (opcional).
	 */
	@PostMapping("/questoes/verificar")
	public ResultadoVerificacaoDto verificarQuestao(@AuthenticationPrincipal Jwt jwt,
			@RequestBody VerificarQuestaoRequest body) {
		Long usuarioId = idAutenticado(jwt);
		if (body.usuarioId() != null && !body.usuarioId().equals(usuarioId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "usuarioId não corresponde ao usuário autenticado");
		}
		return questaoService.verificarQuestao(usuarioId, body);
	}

	@GetMapping("/usuario/{id}")
	public UsuarioProgressoDto obterProgressoUsuario(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		if (!id.equals(idAutenticado(jwt))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
		}
		return questaoService.obterProgressoUsuario(id);
	}

	private Long idAutenticado(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
