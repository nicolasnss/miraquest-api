package com.miraquest.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.QuestaoLicaoDto;
import com.miraquest.api.dto.ResultadoVerificacaoDto;
import com.miraquest.api.dto.UsuarioProgressoDto;
import com.miraquest.api.dto.VerificarQuestaoRequest;
import com.miraquest.api.model.Questao;
import com.miraquest.api.model.UsuarioProgresso;
import com.miraquest.api.repository.QuestaoRepository;
import com.miraquest.api.repository.UsuarioProgressoRepository;

@Service
public class QuestaoService {

	private final QuestaoRepository questaoRepository;
	private final UsuarioProgressoRepository usuarioProgressoRepository;

	public QuestaoService(QuestaoRepository questaoRepository, UsuarioProgressoRepository usuarioProgressoRepository) {
		this.questaoRepository = questaoRepository;
		this.usuarioProgressoRepository = usuarioProgressoRepository;
	}

	public List<QuestaoLicaoDto> obterLicao(String materia) {
		List<Questao> questoes = new ArrayList<>(questaoRepository.findCincoAleatoriasPorMateria(materia));
		Collections.shuffle(questoes);
		return questoes.stream()
				.map(this::paraLicaoDto)
				.toList();
	}

	public UsuarioProgressoDto obterProgressoUsuario(Long usuarioId) {
		UsuarioProgresso progresso = usuarioProgressoRepository.findById(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
		return new UsuarioProgressoDto(progresso.getVidas(), progresso.getXpTotal(), progresso.getStreakDias());
	}

	@Transactional
	public ResultadoVerificacaoDto verificarQuestao(Long usuarioId, VerificarQuestaoRequest request) {
		if (usuarioId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuarioId é obrigatório");
		}
		Questao questao = questaoRepository.findById(request.questaoId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questão não encontrada"));
		validarMateriaOpcional(request.materia(), questao);
		UsuarioProgresso progresso = usuarioProgressoRepository.findById(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

		boolean sucesso = respostasEquivalentes(request.resposta(), questao.getRespostaCorreta());
		String explicacao = questao.getExplicacao() != null ? questao.getExplicacao() : "";

		if (sucesso) {
			progresso.setXpTotal(progresso.getXpTotal() + 10);
			atualizarStreakPorRespostaCorreta(progresso);
			usuarioProgressoRepository.save(progresso);
			return new ResultadoVerificacaoDto(sucesso, explicacao, 10);
		}

		progresso.setVidas(Math.max(0, progresso.getVidas() - 1));
		progresso.setStreakDias(0);
		usuarioProgressoRepository.save(progresso);
		return new ResultadoVerificacaoDto(sucesso, explicacao, 0);
	}

	private void validarMateriaOpcional(String materiaInformada, Questao questao) {
		if (materiaInformada == null || materiaInformada.isBlank()) {
			return;
		}
		String materiaQuestao = questao.getMateria();
		if (materiaQuestao == null
				|| !normalizarMateria(materiaInformada).equals(normalizarMateria(materiaQuestao))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matéria não corresponde à questão");
		}
	}

	private String normalizarMateria(String materia) {
		return materia.trim().toLowerCase(Locale.ROOT);
	}

	private QuestaoLicaoDto paraLicaoDto(Questao q) {
		return new QuestaoLicaoDto(q.getId(), q.getMateria(), q.getTipo(), q.getEnunciado(),
				formatarOpcoesDistracaoParaApi(q.getOpcoesDistracao()));
	}

	/** Opções separadas por |, normalizadas para o cliente (trim, sem segmentos vazios). */
	private String formatarOpcoesDistracaoParaApi(String bruto) {
		if (bruto == null || bruto.isBlank()) {
			return null;
		}
		String normalizado = Arrays.stream(bruto.split("\\|"))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.joining("|"));
		return normalizado.isEmpty() ? null : normalizado;
	}

	private void atualizarStreakPorRespostaCorreta(UsuarioProgresso progresso) {
		LocalDate hoje = LocalDate.now();
		LocalDate ultima = progresso.getDataUltimaRespostaCorreta();
		if (ultima == null) {
			progresso.setStreakDias(1);
		}
		else if (ultima.equals(hoje)) {
			// mantém o streak do dia
		}
		else if (ultima.equals(hoje.minusDays(1))) {
			progresso.setStreakDias(progresso.getStreakDias() + 1);
		}
		else {
			progresso.setStreakDias(1);
		}
		progresso.setDataUltimaRespostaCorreta(hoje);
	}

	private boolean respostasEquivalentes(String respostaUsuario, String respostaCorreta) {
		if (respostaUsuario == null && respostaCorreta == null) {
			return true;
		}
		if (respostaUsuario == null || respostaCorreta == null) {
			return false;
		}
		return normalizar(respostaUsuario).equals(normalizar(respostaCorreta));
	}

	private String normalizar(String texto) {
		return texto.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

}
