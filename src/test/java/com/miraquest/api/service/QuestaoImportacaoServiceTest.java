package com.miraquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.dto.ImportacaoResultadoDto;
import com.miraquest.api.model.Questao;
import com.miraquest.api.model.TipoQuestao;
import com.miraquest.api.repository.QuestaoRepository;

class QuestaoImportacaoServiceTest {

	private static final String CAB = "materia,tipo,enunciado,resposta,distratores,explicacao,banca,pegadinha\n";

	private final QuestaoRepository repository = mock(QuestaoRepository.class);
	private final QuestaoImportacaoService service = new QuestaoImportacaoService(repository);

	private ImportacaoResultadoDto importar(String csv, boolean simulacao) {
		return service.importar(csv.getBytes(StandardCharsets.UTF_8), simulacao);
	}

	@SuppressWarnings("unchecked")
	private List<Questao> gravadas() {
		ArgumentCaptor<List<Questao>> cap = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(cap.capture());
		return cap.getValue();
	}

	@Test
	void importaTodosOsTiposComTodosOsCampos() {
		String csv = CAB
				+ "Redes,CLOZE,A camada de ___ roteia pacotes.,Rede,Transporte|Física|Aplicação,Camada 3,CESPE,Cuidado com a 4\n"
				+ "Linux,CERTO_ERRADO,ls -a lista ocultos.,certo,,Explica,FCC,\n"
				+ "BD,MULTIPLA_ESCOLHA,Qual é DDL?,CREATE,SELECT|INSERT|UPDATE,,,\n";

		ImportacaoResultadoDto r = importar(csv, false);

		assertThat(r.total()).isEqualTo(3);
		assertThat(r.importadas()).isEqualTo(3);
		assertThat(r.erros()).isEmpty();
		List<Questao> qs = gravadas();
		Questao cloze = qs.get(0);
		assertThat(cloze.getTipo()).isEqualTo(TipoQuestao.CLOZE);
		assertThat(cloze.getRespostaCorreta()).isEqualTo("Rede");
		assertThat(cloze.getOpcoesDistracao()).isEqualTo("Transporte|Física|Aplicação");
		assertThat(cloze.getBanca()).isEqualTo("CESPE");
		assertThat(cloze.getPegadinha()).isEqualTo("Cuidado com a 4");
		Questao ce = qs.get(1);
		assertThat(ce.getRespostaCorreta()).isEqualTo("CERTO");
		assertThat(ce.getOpcoesDistracao()).isNull();
		assertThat(qs.get(2).getBanca()).isNull();
		assertThat(qs.get(2).getExplicacao()).isNull();
	}

	@Test
	void aceitaPontoEVirgulaBomColunasEmOutraOrdemECabecalhoComAcento() {
		String csv = "﻿Tipo;Matéria;Resposta;Enunciado;Distratores;Explicação\n"
				+ "lacuna;Redes;UDP;O ___ é sem conexão.;TCP|HTTP;Sem sessão\n";

		ImportacaoResultadoDto r = importar(csv, false);

		assertThat(r.erros()).isEmpty();
		Questao q = gravadas().get(0);
		assertThat(q.getTipo()).isEqualTo(TipoQuestao.CLOZE);
		assertThat(q.getMateria()).isEqualTo("Redes");
		assertThat(q.getExplicacao()).isEqualTo("Sem sessão");
	}

	@Test
	void camposComAspasVirgulaEQuebraDeLinha() {
		String csv = CAB + "Redes,CLOZE,\"Linha 1,\nlinha 2 com ___ \"\"aspas\"\".\",X,\"A|B\",,,\n";

		ImportacaoResultadoDto r = importar(csv, false);

		assertThat(r.erros()).isEmpty();
		assertThat(gravadas().get(0).getEnunciado()).isEqualTo("Linha 1,\nlinha 2 com ___ \"aspas\".");
	}

	@Test
	void reportaErrosPorLinhaEImportaAsValidas() {
		String csv = CAB
				+ "Redes,CLOZE,Sem lacuna aqui.,X,A|B,,,\n"
				+ "Redes,FOO,Enunciado ___,X,A,,,\n"
				+ "Redes,CERTO_ERRADO,Afirmação,TALVEZ,,,,\n"
				+ "Redes,CLOZE,Falta ___ distrator,X,,,,\n"
				+ "Redes,CLOZE,Distrator igual ___,X,x,,,\n"
				+ ",CLOZE,Sem matéria ___,X,A,,,\n"
				+ "Redes,CLOZE,Válida ___ ok,X,A|B,,,\n";

		ImportacaoResultadoDto r = importar(csv, false);

		assertThat(r.total()).isEqualTo(7);
		assertThat(r.importadas()).isEqualTo(1);
		assertThat(r.erros()).extracting(e -> e.linha()).containsExactly(2, 3, 4, 5, 6, 7);
		assertThat(r.erros().get(0).mensagem()).contains("lacuna");
		assertThat(r.erros().get(1).mensagem()).contains("tipo inválido");
		assertThat(gravadas()).hasSize(1);
	}

	@Test
	void ignoraDuplicadasNoArquivoENoBanco() {
		when(repository.existsByMateriaIgnoreCaseAndEnunciado(anyString(), anyString())).thenReturn(false);
		when(repository.existsByMateriaIgnoreCaseAndEnunciado("Redes", "Já existe ___")).thenReturn(true);
		String csv = CAB
				+ "Redes,CLOZE,Já existe ___,X,A,,,\n"
				+ "Redes,CLOZE,Nova ___,X,A,,,\n"
				+ "REDES,CLOZE,Nova ___,X,A,,,\n";

		ImportacaoResultadoDto r = importar(csv, false);

		assertThat(r.importadas()).isEqualTo(1);
		assertThat(r.duplicadas()).isEqualTo(2);
		assertThat(gravadas()).hasSize(1);
	}

	@Test
	void simulacaoNaoGrava() {
		ImportacaoResultadoDto r = importar(CAB + "Redes,CLOZE,Nova ___,X,A,,,\n", true);

		assertThat(r.simulacao()).isTrue();
		assertThat(r.importadas()).isEqualTo(1);
		verify(repository, never()).saveAll(any());
	}

	@Test
	void distratoresSaoNormalizadosSemRepetirAResposta() {
		importar(CAB + "Redes,MULTIPLA_ESCOLHA,Qual?,UDP,TCP| tcp |udp||HTTP,,,\n", false);

		assertThat(gravadas().get(0).getOpcoesDistracao()).isEqualTo("TCP|HTTP");
	}

	@Test
	void rejeitaArquivoInvalido() {
		assertThatThrownBy(() -> importar("", false)).isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> importar("materia,tipo\nRedes,CLOZE\n", false))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getReason()).contains("enunciado").contains("resposta"));
		assertThatThrownBy(() -> service.importar(new byte[] { (byte) 0xE7, (byte) 0xE3, 'o' }, false))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getReason()).contains("UTF-8"));
	}

}
