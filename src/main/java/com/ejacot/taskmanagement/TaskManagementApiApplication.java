package com.ejacot.taskmanagement;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagementApiApplication {

	public static void main(String[] args) {
		configureRenderPostgres();
		SpringApplication.run(TaskManagementApiApplication.class, args);
	}

	private static void configureRenderPostgres() {
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:")) {
			return;
		}
		if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
			return;
		}

		URI uri = URI.create(databaseUrl);
		String userInfo = uri.getUserInfo();
		if (userInfo != null && userInfo.contains(":")) {
			String[] credentials = userInfo.split(":", 2);
			System.setProperty("spring.datasource.username", decode(credentials[0]));
			System.setProperty("spring.datasource.password", decode(credentials[1]));
		}

		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String database = uri.getPath() == null ? "" : uri.getPath();
		System.setProperty("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + database);
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
