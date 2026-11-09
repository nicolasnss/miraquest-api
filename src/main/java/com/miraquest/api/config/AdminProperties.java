package com.miraquest.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code miraquest.admin.emails} (env ADMIN_EMAILS, separados por vírgula): contas com permissão de administração. */
@ConfigurationProperties(prefix = "miraquest.admin")
public record AdminProperties(List<String> emails) {

	public AdminProperties {
		emails = emails == null ? List.of() : List.copyOf(emails);
	}

	public boolean isAdmin(String email) {
		return email != null && emails.stream().anyMatch(e -> e.trim().equalsIgnoreCase(email.trim()));
	}

}
