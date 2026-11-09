package com.miraquest.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.ImportacaoResultadoDto;
import com.miraquest.api.dto.ImportacaoResultadoDto.ErroLinha;
import com.miraquest.api.model.Questao;
import com.miraquest.api.model.TipoQuestao;
import com.miraquest.api.repository.QuestaoRepository;

/**
 * Importa questões de um CSV com as colunas: materia, tipo, enunciado, resposta, distratores (separados por
 * {@code |}), explicacao, banca, pegadinha. Aceita delimitador {@code ,}, {@code ;} ou tab (detectado no
 * cabeçalho), UTF-8 (com ou sem BOM) e colunas em qualquer ordem. Linhas inválidas são reportadas e as válidas
 * são importadas; questões já existentes (mesma matéria + enunciado) são ignoradas.
 */
@Service
public class QuestaoImportacaoService {

	static final int MAX_LINHAS = 5000;
	private static final int MAX_CAMPO_CURTO = 255;
	private static final Pattern LACUNA = Pattern.compile("_{2,}");
	private static final List<String> OBRIGATORIAS = List.of("materia", "tipo", "enunciado", "resposta");

	private final QuestaoRepository questaoRepository;

	public QuestaoImportacaoService(QuestaoRepository questaoRepository) {
		this.questaoRepository = questaoRepository;
	}

	@Transactional
	public ImportacaoResultadoDto importar(byte[] conteudo, boolean simulacao) {
		List<CSVRecord> registros = lerRegistros(conteudo);
		Map<String, Integer> colunas = mapearCabecalho(registros.get(0));
		if (registros.size() - 1 > MAX_LINHAS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo excede " + MAX_LINHAS + " questões");
		}

		List<Questao> validas = new ArrayList<>();
		List<ErroLinha> erros = new ArrayList<>();
		Set<String> vistas = new HashSet<>();
		int duplicadas = 0;

		for (int i = 1; i < registros.size(); i++) {
			int linha = i + 1;
			try {
				Questao q = montar(registros.get(i), colunas);
				String chave = q.getMateria().toLowerCase(Locale.ROOT) + "\u0000" + q.getEnunciado();
				if (!vistas.add(chave)
						|| questaoRepository.existsByMateriaIgnoreCaseAndEnunciado(q.getMateria(), q.getEnunciado())) {
					duplicadas++;
					continue;
				}
				validas.add(q);
			}
			catch (IllegalArgumentException e) {
				erros.add(new ErroLinha(linha, e.getMessage()));
			}
		}

		if (!simulacao && !validas.isEmpty()) {
			questaoRepository.saveAll(validas);
		}
		return new ImportacaoResultadoDto(simulacao, registros.size() - 1, validas.size(), duplicadas, erros);
	}

	private List<CSVRecord> lerRegistros(byte[] conteudo) {
		String texto = new String(conteudo, StandardCharsets.UTF_8);
		if (texto.indexOf('�') >= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo deve estar codificado em UTF-8");
		}
		if (texto.startsWith("﻿")) {
			texto = texto.substring(1);
		}
		CSVFormat formato = CSVFormat.DEFAULT.builder()
				.setDelimiter(detectarDelimitador(texto))
				.setIgnoreEmptyLines(true)
				.setTrim(true)
				.build();
		List<CSVRecord> registros;
		try (CSVParser parser = CSVParser.parse(texto, formato)) {
			registros = parser.getRecords();
		}
		catch (IOException | UncheckedIOException | IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV malformado: " + e.getMessage());
		}
		if (registros.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo está vazio");
		}
		return registros;
	}

	private char detectarDelimitador(String texto) {
		int fim = texto.indexOf('\n');
		String cabecalho = fim < 0 ? texto : texto.substring(0, fim);
		long virgulas = cabecalho.chars().filter(c -> c == ',').count();
		long pontoVirgulas = cabecalho.chars().filter(c -> c == ';').count();
		long tabs = cabecalho.chars().filter(c -> c == '\t').count();
		if (pontoVirgulas > virgulas && pontoVirgulas >= tabs) {
			return ';';
		}
		if (tabs > virgulas) {
			return '\t';
		}
		return ',';
	}

	private Map<String, Integer> mapearCabecalho(CSVRecord cabecalho) {
		Map<String, Integer> colunas = new HashMap<>();
		for (int i = 0; i < cabecalho.size(); i++) {
			colunas.putIfAbsent(semAcento(cabecalho.get(i)).toLowerCase(Locale.ROOT).trim(), i);
		}
		List<String> faltando = OBRIGATORIAS.stream().filter(c -> !colunas.containsKey(c)).toList();
		if (!faltando.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Colunas obrigatórias ausentes no cabeçalho: " + String.join(", ", faltando));
		}
		return colunas;
	}

	private Questao montar(CSVRecord r, Map<String, Integer> colunas) {
		String materia = campo(r, colunas, "materia");
		String enunciado = campo(r, colunas, "enunciado");
		String resposta = campo(r, colunas, "resposta");
		String distratoresBrutos = campo(r, colunas, "distratores");
		String explicacao = campo(r, colunas, "explicacao");
		String banca = campo(r, colunas, "banca");
		String pegadinha = campo(r, colunas, "pegadinha");

		if (materia.isEmpty()) {
			throw new IllegalArgumentException("materia é obrigatória");
		}
		if (enunciado.isEmpty()) {
			throw new IllegalArgumentException("enunciado é obrigatório");
		}
		if (resposta.isEmpty()) {
			throw new IllegalArgumentException("resposta é obrigatória");
		}
		limitar("materia", materia);
		limitar("resposta", resposta);
		limitar("banca", banca);
		TipoQuestao tipo = interpretarTipo(campo(r, colunas, "tipo"));

		String distratores = null;
		if (tipo == TipoQuestao.CERTO_ERRADO) {
			resposta = interpretarCertoErrado(resposta);
		}
		else {
			if (tipo == TipoQuestao.CLOZE && !LACUNA.matcher(enunciado).find()) {
				throw new IllegalArgumentException("enunciado CLOZE precisa conter a lacuna (___)");
			}
			List<String> lista = interpretarDistratores(distratoresBrutos, resposta);
			if (lista.isEmpty()) {
				throw new IllegalArgumentException("informe ao menos um distrator (separados por |)");
			}
			distratores = String.join("|", lista);
		}

		Questao q = new Questao();
		q.setMateria(materia);
		q.setTipo(tipo);
		q.setEnunciado(enunciado);
		q.setRespostaCorreta(resposta);
		q.setOpcoesDistracao(distratores);
		q.setExplicacao(explicacao.isEmpty() ? null : explicacao);
		q.setBanca(banca.isEmpty() ? null : banca);
		q.setPegadinha(pegadinha.isEmpty() ? null : pegadinha);
		return q;
	}

	private String campo(CSVRecord r, Map<String, Integer> colunas, String nome) {
		Integer idx = colunas.get(nome);
		if (idx == null || idx >= r.size()) {
			return "";
		}
		return r.get(idx).trim();
	}

	private void limitar(String nome, String valor) {
		if (valor.length() > MAX_CAMPO_CURTO) {
			throw new IllegalArgumentException(nome + " excede " + MAX_CAMPO_CURTO + " caracteres");
		}
	}

	private TipoQuestao interpretarTipo(String bruto) {
		String t = semAcento(bruto).toUpperCase(Locale.ROOT).trim().replaceAll("[\\s\\-/]+", "_");
		return switch (t) {
			case "CLOZE", "LACUNA" -> TipoQuestao.CLOZE;
			case "CERTO_ERRADO", "CERTOERRADO" -> TipoQuestao.CERTO_ERRADO;
			case "MULTIPLA_ESCOLHA", "MULTIPLA" -> TipoQuestao.MULTIPLA_ESCOLHA;
			default -> throw new IllegalArgumentException("tipo inválido '" + bruto
					+ "' (use CLOZE, CERTO_ERRADO ou MULTIPLA_ESCOLHA)");
		};
	}

	private String interpretarCertoErrado(String bruto) {
		return switch (semAcento(bruto).toUpperCase(Locale.ROOT).trim()) {
			case "CERTO", "C", "VERDADEIRO", "V" -> "CERTO";
			case "ERRADO", "E", "FALSO", "F" -> "ERRADO";
			default -> throw new IllegalArgumentException("resposta de CERTO_ERRADO deve ser CERTO ou ERRADO");
		};
	}

	/** Separa por |, tira vazios e duplicatas (sem diferenciar maiúsculas) e remove quem repete a resposta. */
	private List<String> interpretarDistratores(String bruto, String resposta) {
		Map<String, String> unicos = new LinkedHashMap<>();
		for (String parte : bruto.split("\\|")) {
			String d = parte.trim();
			if (!d.isEmpty() && !d.equalsIgnoreCase(resposta)) {
				unicos.putIfAbsent(d.toLowerCase(Locale.ROOT), d);
			}
		}
		return new ArrayList<>(unicos.values());
	}

	private String semAcento(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

}
