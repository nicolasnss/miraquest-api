package com.miraquest.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "miraquest.admin.emails=admin-import@teste.com")
@ActiveProfiles("h2")
class ImportacaoIntegrationTest {

	@LocalServerPort
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private String tokenDe(String email) throws Exception {
		String cred = "{\"email\":\"" + email + "\",\"senha\":\"senha-forte-1\"}";
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/registrar"))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(cred)).build();
		String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
		Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(body);
		assertThat(m.find()).as(body).isTrue();
		return m.group(1);
	}

	private HttpResponse<String> upload(String query, String csv, String token) throws Exception {
		String limite = "----limite" + UUID.randomUUID();
		String corpo = "--" + limite + "\r\n"
				+ "Content-Disposition: form-data; name=\"arquivo\"; filename=\"q.csv\"\r\n"
				+ "Content-Type: text/csv\r\n\r\n" + csv + "\r\n--" + limite + "--\r\n";
		HttpRequest.Builder b = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + "/api/admin/questoes/importar" + query))
				.header("Content-Type", "multipart/form-data; boundary=" + limite)
				.POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));
		if (token != null) {
			b.header("Authorization", "Bearer " + token);
		}
		return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path, String token) throws Exception {
		return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void somenteAdminImportaEQuestoesFicamDisponiveis() throws Exception {
		String materia = "Mat" + UUID.randomUUID().toString().substring(0, 8);
		String csv = "materia;tipo;enunciado;resposta;distratores;explicacao;banca;pegadinha\n"
				+ materia + ";CLOZE;O protocolo ___ é sem conexão.;UDP;TCP|HTTP;Sem sessão;CESPE;Não é TCP\n"
				+ materia + ";CLOZE;sem lacuna;X;A;;;\n";

		assertThat(upload("", csv, null).statusCode()).isEqualTo(401);

		String comum = tokenDe("comum-" + UUID.randomUUID() + "@teste.com");
		assertThat(upload("", csv, comum).statusCode()).isEqualTo(403);

		String admin = tokenDe("admin-import@teste.com");
		HttpResponse<String> simulacao = upload("?simulacao=true", csv, admin);
		assertThat(simulacao.statusCode()).isEqualTo(200);
		assertThat(simulacao.body()).contains("\"simulacao\":true").contains("\"importadas\":1");
		assertThat(get("/api/questoes/licao?materia=" + materia, admin).body()).isEqualTo("[]");

		HttpResponse<String> real = upload("", csv, admin);
		assertThat(real.statusCode()).isEqualTo(200);
		assertThat(real.body()).contains("\"importadas\":1").contains("\"linha\":3");

		String licao = get("/api/questoes/licao?materia=" + materia, admin).body();
		assertThat(licao).contains("O protocolo ___").contains("\"UDP\"").contains("\"TCP\"")
				.doesNotContain("respostaCorreta");

		String materias = get("/api/materias", admin).body();
		assertThat(materias).contains("\"nome\":\"" + materia + "\"", "\"nome\":\"Redes\"");
		assertThat(materias).containsPattern("\"nome\":\"" + materia + "\",\"totalQuestoes\":1");

		assertThat(upload("", csv, admin).body()).contains("\"importadas\":0").contains("\"duplicadas\":1");
		assertThat(upload("", "so,cabecalho\n", admin).statusCode()).isEqualTo(400);
	}

}
