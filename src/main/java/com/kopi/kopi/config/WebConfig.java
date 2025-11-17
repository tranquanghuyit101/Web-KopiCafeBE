package com.kopi.kopi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@org.springframework.beans.factory.annotation.Value("${app.frontend.url:https://kopi-coffee-fe.vercel.app}")
	private String frontendUrl;
	@org.springframework.beans.factory.annotation.Value("${LOCAL_FRONTEND:}")
	private String localFrontend;
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/menu").setViewName("forward:/menu.html");
		registry.addViewController("/profile").setViewName("forward:/profile.html");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (localFrontend != null && !localFrontend.isBlank()) {
			registry.addMapping("/**")
				.allowedOrigins(frontendUrl, localFrontend)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("*")
				.allowCredentials(true)
				.maxAge(3600);
		} else {
			registry.addMapping("/**")
				.allowedOrigins(frontendUrl)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("*")
				.allowCredentials(true)
				.maxAge(3600);
		}
	}
} 