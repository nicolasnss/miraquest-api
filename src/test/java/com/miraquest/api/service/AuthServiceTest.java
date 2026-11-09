package com.miraquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.config.JwtProperties;
import com.miraquest.api.dto.AuthResponseDto;
import com.miraquest.api.dto.CredenciaisRequest;
import com.miraquest.api.model.UsuarioProgresso;
import com.miraquest.api.repository.UsuarioProgressoRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class AuthServiceTest {

	private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes!!";

	private final UsuarioProgressoRepository repository = mock(UsuarioProgressoRepository.class);
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final SecretKeySpec chave = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	private final AuthService service = new AuthService(repository, encoder,
			new NimbusJwtEncoder(new ImmutableSecret<>(chave)), new JwtProperties(SEGREDO, 1),
			new com.miraquest.api.config.AdminProperties(java.util.List.of("Admin@X.com")));

	private UsuarioProgresso salvo(Long id, String email, String senha) {
		UsuarioProgresso u = new UsuarioProgresso();
		u.setId(id);
		u.setEmailUsuario(email);
		u.setSenhaHash(senha == null ? null : encoder.encode(senha));
		return u;
	}

	@Test
	void registrarNormalizaEmailEGeraTokenComIdNoSubject() {
		when(repository.findByEmailUsuarioIgnoreCase("ana@x.com")).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(inv -> {
			UsuarioProgresso u = inv.getArgument(0);
			u.setId(42L);
			return u;
		});

		AuthResponseDto resp = service.registrar(new CredenciaisRequest("  Ana@X.com ", "senha-forte-1"));

		assertThat(resp.usuarioId()).isEqualTo(42L);
		assertThat(resp.email()).isEqualTo("ana@x.com");
		Jwt jwt = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build()
				.decode(resp.token());
		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
	}

	@Test
	void tokenDeAdministradorTemEscopoAdminEDeComumNao() {
		when(repository.findByEmailUsuarioIgnoreCase(any())).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();

		Jwt admin = decoder.decode(service.registrar(new CredenciaisRequest("admin@x.com", "senha-forte-1")).token());
		Jwt comum = decoder.decode(service.registrar(new CredenciaisRequest("user@x.com", "senha-forte-1")).token());

		assertThat(admin.getClaimAsString("scope")).isEqualTo("ADMIN");
		assertThat(comum.getClaimAsString("scope")).isNull();
	}

	@Test
	void registrarNaoGuardaSenhaEmTextoPuro() {
		when(repository.findByEmailUsuarioIgnoreCase("a@x.com")).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.registrar(new CredenciaisRequest("a@x.com", "senha-forte-1"));

		ArgumentCaptor<UsuarioProgresso> cap = ArgumentCaptor.forClass(UsuarioProgresso.class);
		verify(repository).save(cap.capture());
		assertThat(cap.getValue().getSenhaHash()).isNotEqualTo("senha-forte-1");
		assertThat(encoder.matches("senha-forte-1", cap.getValue().getSenhaHash())).isTrue();
	}

	@Test
	void registrarEmailDuplicadoRetorna409() {
		when(repository.findByEmailUsuarioIgnoreCase("a@x.com")).thenReturn(Optional.of(salvo(1L, "a@x.com", "x")));

		assertThatThrownBy(() -> service.registrar(new CredenciaisRequest("a@x.com", "senha-forte-1")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(repository, never()).save(any());
	}

	@Test
	void loginComCredenciaisCorretasRetornaToken() {
		when(repository.findByEmailUsuarioIgnoreCase("a@x.com"))
				.thenReturn(Optional.of(salvo(7L, "a@x.com", "senha-forte-1")));

		AuthResponseDto resp = service.login(new CredenciaisRequest("A@x.com", "senha-forte-1"));

		assertThat(resp.usuarioId()).isEqualTo(7L);
		assertThat(resp.token()).isNotBlank();
	}

	@Test
	void loginInvalidoRetorna401() {
		when(repository.findByEmailUsuarioIgnoreCase("a@x.com"))
				.thenReturn(Optional.of(salvo(7L, "a@x.com", "senha-forte-1")));
		when(repository.findByEmailUsuarioIgnoreCase("seed@x.com"))
				.thenReturn(Optional.of(salvo(1L, "seed@x.com", null)));
		when(repository.findByEmailUsuarioIgnoreCase("nao@x.com")).thenReturn(Optional.empty());

		CredenciaisRequest[] invalidos = { new CredenciaisRequest("a@x.com", "errada-123"),
				new CredenciaisRequest("seed@x.com", "qualquer-123"),
				new CredenciaisRequest("nao@x.com", "qualquer-123") };
		for (CredenciaisRequest r : invalidos) {
			assertThatThrownBy(() -> service.login(r)).isInstanceOfSatisfying(ResponseStatusException.class,
					e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		}
	}

}
