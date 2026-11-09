package com.miraquest.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

	private final SecurityConfig config = new SecurityConfig();

	@Test
	void aceitaSegredoForte() {
		assertThat(config.jwtSecretKey(new JwtProperties("x".repeat(40), 1))).isNotNull();
	}

	@Test
	void rejeitaSegredoAusenteOuCurto() {
		assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(null, 1))).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(" ", 1))).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("curto", 1))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejeitaSegredoPublicoAntigo() {
		assertThatThrownBy(() -> config.jwtSecretKey(
				new JwtProperties("dev-only-secret-change-me-in-production-0123456789", 1)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("público");
	}

}
