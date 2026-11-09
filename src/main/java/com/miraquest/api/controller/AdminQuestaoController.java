package com.miraquest.api.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.ImportacaoResultadoDto;
import com.miraquest.api.service.QuestaoImportacaoService;

/** Endpoints administrativos (exigem o escopo ADMIN no token; ver {@code miraquest.admin.emails}). */
@RestController
@RequestMapping("/api/admin/questoes")
public class AdminQuestaoController {

	private final QuestaoImportacaoService importacaoService;

	public AdminQuestaoController(QuestaoImportacaoService importacaoService) {
		this.importacaoService = importacaoService;
	}

	/**
	 * Importa questões de um CSV (multipart, campo {@code arquivo}). Com {@code simulacao=true} apenas valida,
	 * sem gravar.
	 */
	@PostMapping("/importar")
	public ImportacaoResultadoDto importar(@RequestParam("arquivo") MultipartFile arquivo,
			@RequestParam(defaultValue = "false") boolean simulacao) {
		try {
			return importacaoService.importar(arquivo.getBytes(), simulacao);
		}
		catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo");
		}
	}

}
