package com.miraquest.api.dto;

public record AuthResponseDto(String token, Long usuarioId, String email) {

}
