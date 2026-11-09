package com.miraquest.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.miraquest.api.config.AdminProperties;
import com.miraquest.api.config.JwtProperties;
import com.miraquest.api.dto.AuthResponseDto;
import com.miraquest.api.dto.CredenciaisRequest;
import com.miraquest.api.model.UsuarioProgresso;
import com.miraquest.api.repository.UsuarioProgressoRepository;

@Service
public class AuthService {

	private final UsuarioProgressoRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final AdminProperties adminProperties;

	public AuthService(UsuarioProgressoRepository repository, PasswordEncoder passwordEncoder,
			JwtEncoder jwtEncoder, JwtProperties jwtProperties, AdminProperties adminProperties) {
		this.adminProperties = adminProperties;
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public AuthResponseDto registrar(CredenciaisRequest request) {
		String email = normalizar(request.email());
		if (repository.findByEmailUsuarioIgnoreCase(email).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
		}
		UsuarioProgresso usuario = new UsuarioProgresso();
		usuario.setEmailUsuario(email);
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario = repository.save(usuario);
		return resposta(usuario);
	}

	@Transactional(readOnly = true)
	public AuthResponseDto login(CredenciaisRequest request) {
		UsuarioProgresso usuario = repository.findByEmailUsuarioIgnoreCase(normalizar(request.email()))
				.filter(u -> u.getSenhaHash() != null && passwordEncoder.matches(request.senha(), u.getSenhaHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
		return resposta(usuario);
	}

	private AuthResponseDto resposta(UsuarioProgresso usuario) {
		Instant agora = Instant.now();
		JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
				.subject(String.valueOf(usuario.getId()))
				.issuedAt(agora)
				.expiresAt(agora.plus(Duration.ofHours(jwtProperties.expiracaoHoras())));
		if (adminProperties.isAdmin(usuario.getEmailUsuario())) {
			builder.claim("scope", "ADMIN");
		}
		JwtClaimsSet claims = builder.build();
		String token = jwtEncoder
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		return new AuthResponseDto(token, usuario.getId(), usuario.getEmailUsuario());
	}

	private String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
