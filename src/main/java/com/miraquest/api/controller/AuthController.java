package com.miraquest.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miraquest.api.dto.AuthResponseDto;
import com.miraquest.api.dto.CredenciaisRequest;
import com.miraquest.api.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registrar")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponseDto registrar(@Valid @RequestBody CredenciaisRequest body) {
		return authService.registrar(body);
	}

	@PostMapping("/login")
	public AuthResponseDto login(@Valid @RequestBody CredenciaisRequest body) {
		return authService.login(body);
	}

}
