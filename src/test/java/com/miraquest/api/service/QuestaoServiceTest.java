package com.miraquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miraquest.api.dto.QuestaoLicaoDto;
import com.miraquest.api.model.Questao;
import com.miraquest.api.model.TipoQuestao;
import com.miraquest.api.repository.QuestaoRepository;
import com.miraquest.api.repository.UsuarioProgressoRepository;

class QuestaoServiceTest {

	private final QuestaoRepository questaoRepository = mock(QuestaoRepository.class);
	private final UsuarioProgressoRepository progressoRepository = mock(UsuarioProgressoRepository.class);

	private QuestaoService service(long seed) {
		return new QuestaoService(questaoRepository, progressoRepository, new Random(seed));
	}

	private Questao questao(Long id, TipoQuestao tipo, String correta, String distratores) {
		Questao q = new Questao();
		q.setId(id);
		q.setMateria("Redes");
		q.setTipo(tipo);
		q.setEnunciado("Enunciado " + id);
		q.setRespostaCorreta(correta);
		q.setOpcoesDistracao(distratores);
		return q;
	}

	@Test
	void opcoesContemDistratoresMaisCorreta() {
		Questao q = questao(1L, TipoQuestao.CLOZE, "Rede", "Transporte|Física|Aplicação");
		when(questaoRepository.findCincoAleatoriasPorMateria("Redes")).thenReturn(List.of(q));

		List<QuestaoLicaoDto> licao = service(1).obterLicao("Redes");

		assertThat(licao).hasSize(1);
		assertThat(licao.get(0).opcoes()).containsExactlyInAnyOrder("Rede", "Transporte", "Física", "Aplicação");
	}

	@Test
	void licaoExpoeBancaEPegadinha() {
		Questao q = questao(1L, TipoQuestao.CLOZE, "Rede", "Transporte");
		q.setBanca("CESPE");
		q.setPegadinha("Cuidado com o termo absoluto");
		Questao semExtras = questao(2L, TipoQuestao.CLOZE, "Rede", "Transporte");
		when(questaoRepository.findCincoAleatoriasPorMateria("Redes")).thenReturn(List.of(q, semExtras));

		List<QuestaoLicaoDto> licao = service(1).obterLicao("Redes");

		QuestaoLicaoDto com = licao.stream().filter(d -> d.id() == 1L).findFirst().orElseThrow();
		QuestaoLicaoDto sem = licao.stream().filter(d -> d.id() == 2L).findFirst().orElseThrow();
		assertThat(com.banca()).isEqualTo("CESPE");
		assertThat(com.pegadinha()).isEqualTo("Cuidado com o termo absoluto");
		assertThat(sem.banca()).isNull();
		assertThat(sem.pegadinha()).isNull();
	}

	@Test
	void opcoesSaoEmbaralhadas() {
		Questao q = questao(1L, TipoQuestao.MULTIPLA_ESCOLHA, "A", "B|C|D|E|F");
		Set<List<String>> ordens = new HashSet<>();
		for (long seed = 0; seed < 30; seed++) {
			ordens.add(service(seed).montarOpcoes(q));
		}
		assertThat(ordens.size()).isGreaterThan(1);
	}

	@Test
	void correta_naoFicaSempreNaMesmaPosicao() {
		Questao q = questao(1L, TipoQuestao.CLOZE, "Rede", "Transporte|Física|Aplicação");
		Set<Integer> posicoes = new HashSet<>();
		for (long seed = 0; seed < 50; seed++) {
			posicoes.add(service(seed).montarOpcoes(q).indexOf("Rede"));
		}
		assertThat(posicoes.size()).isGreaterThan(1);
	}

	@Test
	void normalizaEspacosESegmentosVaziosEremoveDuplicatas() {
		Questao q = questao(1L, TipoQuestao.CLOZE, " UDP ", " TCP | |HTTP||udp|TCP");

		assertThat(service(1).montarOpcoes(q)).containsExactlyInAnyOrder("TCP", "HTTP", "UDP");
	}

	@Test
	void semDistratoresRetornaApenasCorreta() {
		Questao q = questao(1L, TipoQuestao.CLOZE, "UDP", null);

		assertThat(service(1).montarOpcoes(q)).containsExactly("UDP");
	}

	@Test
	void certoErradoNaoTemOpcoes() {
		Questao q = questao(1L, TipoQuestao.CERTO_ERRADO, "CERTO", null);

		assertThat(service(1).montarOpcoes(q)).isEmpty();
	}

	@Test
	void listarMateriasAgrupaSemDiferenciarCaixaSomaEOrdena() {
		when(questaoRepository.contarPorMateria()).thenReturn(List.of(
				new Object[] { "Redes", 2L },
				new Object[] { "REDES", 5L },
				new Object[] { "Linux", 3L },
				new Object[] { "  banco de dados ", 1L }));

		List<com.miraquest.api.dto.MateriaDto> materias = service(1).listarMaterias();

		assertThat(materias).extracting(m -> m.nome()).containsExactly("banco de dados", "Linux", "REDES");
		assertThat(materias).extracting(m -> m.totalQuestoes()).containsExactly(1L, 3L, 7L);
	}

	@Test
	void listarMateriasSemQuestoesRetornaVazio() {
		when(questaoRepository.contarPorMateria()).thenReturn(List.of());

		assertThat(service(1).listarMaterias()).isEmpty();
	}

	@Test
	void jsonDaLicaoNaoExpoeRespostaCorreta() throws Exception {
		Questao q = questao(1L, TipoQuestao.CLOZE, "Rede", "Transporte|Física");
		when(questaoRepository.findCincoAleatoriasPorMateria("Redes")).thenReturn(List.of(q));

		String json = new ObjectMapper().writeValueAsString(service(1).obterLicao("Redes"));

		assertThat(json).contains("\"opcoes\"");
		assertThat(json).doesNotContain("respostaCorreta").doesNotContain("opcoesDistracao");
	}
}
