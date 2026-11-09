package com.miraquest.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class AuthIntegrationTest {

	@LocalServerPort
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> send(String method, String path, String json, String token) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json");
		if (token != null) {
			b.header("Authorization", "Bearer " + token);
		}
		b.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
		return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	private String campo(String json, String nome) {
		Matcher m = Pattern.compile("\"" + nome + "\":\"?([^\",}]+)").matcher(json);
		assertThat(m.find()).as(nome + " em " + json).isTrue();
		return m.group(1);
	}

	@Test
	void fluxoCompletoDeAutenticacaoEAutorizacao() throws Exception {
		assertThat(send("GET", "/api/questoes/licao?materia=Redes", null, null).statusCode()).isEqualTo(401);
		assertThat(send("GET", "/api/questoes/licao?materia=Redes", null, "token-invalido").statusCode())
				.isEqualTo(401);

		String email = "u" + UUID.randomUUID() + "@teste.com";
		String cred = "{\"email\":\"" + email + "\",\"senha\":\"senha-forte-1\"}";
		assertThat(send("POST", "/api/auth/registrar", cred, null).statusCode()).isEqualTo(201);
		assertThat(send("POST", "/api/auth/registrar", cred, null).statusCode()).isEqualTo(409);
		assertThat(send("POST", "/api/auth/registrar", "{\"email\":\"x\",\"senha\":\"1\"}", null).statusCode())
				.isEqualTo(400);
		assertThat(send("POST", "/api/auth/login",
				"{\"email\":\"" + email + "\",\"senha\":\"errada-1234\"}", null).statusCode()).isEqualTo(401);

		HttpResponse<String> login = send("POST", "/api/auth/login", cred, null);
		assertThat(login.statusCode()).isEqualTo(200);
		String token = campo(login.body(), "token");
		String id = campo(login.body(), "usuarioId");

		HttpResponse<String> licao = send("GET", "/api/questoes/licao?materia=Redes", null, token);
		assertThat(licao.statusCode()).isEqualTo(200);
		assertThat(licao.body()).contains("\"opcoes\"").doesNotContain("respostaCorreta");

		assertThat(send("GET", "/api/usuario/" + id, null, token).statusCode()).isEqualTo(200);
		assertThat(send("GET", "/api/usuario/1", null, token).statusCode()).isEqualTo(403);
		assertThat(send("POST", "/api/questoes/verificar",
				"{\"usuarioId\":1,\"questaoId\":1,\"resposta\":\"x\"}", token).statusCode()).isEqualTo(403);
	}

}
