package com.miraquest.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.MateriaDto;
import com.miraquest.api.dto.QuestaoLicaoDto;
import com.miraquest.api.dto.ResultadoVerificacaoDto;
import com.miraquest.api.dto.UsuarioProgressoDto;
import com.miraquest.api.dto.VerificarQuestaoRequest;
import com.miraquest.api.model.Questao;
import com.miraquest.api.model.TipoQuestao;
import com.miraquest.api.model.UsuarioProgresso;
import com.miraquest.api.repository.QuestaoRepository;
import com.miraquest.api.repository.UsuarioProgressoRepository;

@Service
public class QuestaoService {

	private final QuestaoRepository questaoRepository;
	private final UsuarioProgressoRepository usuarioProgressoRepository;

	private final Random random;

	@Autowired
	public QuestaoService(QuestaoRepository questaoRepository, UsuarioProgressoRepository usuarioProgressoRepository) {
		this(questaoRepository, usuarioProgressoRepository, new Random());
	}

	QuestaoService(QuestaoRepository questaoRepository, UsuarioProgressoRepository usuarioProgressoRepository,
			Random random) {
		this.questaoRepository = questaoRepository;
		this.usuarioProgressoRepository = usuarioProgressoRepository;
		this.random = random;
	}

	public List<QuestaoLicaoDto> obterLicao(String materia) {
		List<Questao> questoes = new ArrayList<>(questaoRepository.findCincoAleatoriasPorMateria(materia));
		Collections.shuffle(questoes, random);
		return questoes.stream()
				.map(this::paraLicaoDto)
				.toList();
	}

	/**
	 * Matérias existentes, agrupadas sem diferenciar maiúsculas/minúsculas ("Redes" e "REDES" viram uma só). O nome
	 * exibido é a grafia mais usada; em empate, a primeira em ordem alfabética. Ordenadas por nome.
	 */
	public List<MateriaDto> listarMaterias() {
		Map<String, List<Object[]>> grupos = new LinkedHashMap<>();
		for (Object[] linha : questaoRepository.contarPorMateria()) {
			String nome = ((String) linha[0]).trim();
			grupos.computeIfAbsent(nome.toLowerCase(Locale.ROOT), k -> new ArrayList<>())
					.add(new Object[] { nome, ((Number) linha[1]).longValue() });
		}
		return grupos.values().stream()
				.map(grafias -> {
					long total = grafias.stream().mapToLong(g -> (Long) g[1]).sum();
					String nome = grafias.stream()
							.sorted(Comparator.<Object[], Long>comparing(g -> (Long) g[1]).reversed()
									.thenComparing(g -> (String) g[0]))
							.map(g -> (String) g[0]).findFirst().orElseThrow();
					return new MateriaDto(nome, total);
				})
				.sorted(Comparator.comparing(m -> m.nome().toLowerCase(Locale.ROOT)))
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
			return new ResultadoVerificacaoDto(sucesso, explicacao, 10, questao.getPegadinha());
		}

		progresso.setVidas(Math.max(0, progresso.getVidas() - 1));
		progresso.setStreakDias(0);
		usuarioProgressoRepository.save(progresso);
		return new ResultadoVerificacaoDto(sucesso, explicacao, 0, questao.getPegadinha());
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
				montarOpcoes(q), q.getBanca());
	}

	/** Distratores + resposta correta, embaralhados e sem duplicatas. CERTO_ERRADO não tem opções. */
	List<String> montarOpcoes(Questao q) {
		if (q.getTipo() == TipoQuestao.CERTO_ERRADO) {
			return List.of();
		}
		Set<String> unicas = new LinkedHashSet<>();
		if (q.getOpcoesDistracao() != null) {
			Arrays.stream(q.getOpcoesDistracao().split("\\|"))
					.map(String::trim)
					.filter(o -> !o.isEmpty())
					.forEach(unicas::add);
		}
		String correta = q.getRespostaCorreta() == null ? "" : q.getRespostaCorreta().trim();
		if (!correta.isEmpty()) {
			unicas.removeIf(o -> o.equalsIgnoreCase(correta));
			unicas.add(correta);
		}
		List<String> opcoes = new ArrayList<>(unicas);
		Collections.shuffle(opcoes, random);
		return List.copyOf(opcoes);
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
