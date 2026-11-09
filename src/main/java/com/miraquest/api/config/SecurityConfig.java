package com.miraquest.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ JwtProperties.class, AdminProperties.class })
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(h -> h.frameOptions(f -> f.sameOrigin()))
				.authorizeHttpRequests(a -> a
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/**", "/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
								"/h2-console/**")
						.permitAll()
						.requestMatchers("/api/admin/**").hasAuthority("SCOPE_ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Segredos que já estiveram versionados e, portanto, são públicos: nunca aceitar. */
	private static final Set<String> SEGREDOS_PUBLICOS = Set.of("dev-only-secret-change-me-in-production-0123456789");

	@Bean
	SecretKey jwtSecretKey(JwtProperties props) {
		if (props.secret() == null || props.secret().isBlank()) {
			throw new IllegalStateException("miraquest.jwt.secret (JWT_SECRET) é obrigatório");
		}
		if (SEGREDOS_PUBLICOS.contains(props.secret())) {
			throw new IllegalStateException("miraquest.jwt.secret é um valor público antigo do repositório; gere outro");
		}
		byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("miraquest.jwt.secret precisa ter pelo menos 32 bytes");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

}
