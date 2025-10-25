package com.kopi.kopi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import static org.springframework.security.config.Customizer.withDefaults;


import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.context.annotation.Lazy;
@Configuration
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

  @Bean
  @org.springframework.core.annotation.Order(0)
  public SecurityFilterChain authEndpoints(HttpSecurity http) throws Exception {
      http
              .securityMatcher("/apiv1/auth/**") // chỉ áp cho các path auth
              .csrf(csrf -> csrf.disable())
              .cors(withDefaults())
              .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                      .requestMatchers(
                              "/apiv1/auth/login",
                              "/apiv1/auth/register",     // 🟨 permit đăng ký
                              "/apiv1/auth/verify-otp",   // 🟨 permit xác thực OTP
                              "/apiv1/auth/google",       // 🟨 permit google id token endpoint
                              "/apiv1/auth/forgotPass",
                              "/apiv1/auth/forgot-password",
                              "/apiv1/auth/logout"
                        ).permitAll()
                        .requestMatchers("/apiv1/auth/change-password").authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

  @Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
      var cfg = new org.springframework.web.cors.CorsConfiguration();
      cfg.setAllowedOrigins(java.util.List.of("http://localhost:3000"));
      cfg.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
      cfg.setAllowedHeaders(java.util.List.of("*"));
      cfg.setAllowCredentials(true);
      var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", cfg);
      return source;
  }

  @Bean
  public OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository repo) {
      DefaultOAuth2AuthorizationRequestResolver resolver =
              new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
      resolver.setAuthorizationRequestCustomizer(customizer ->
              customizer.additionalParameters(params -> {
                  params.put("prompt", "select_account");
              })
      );
      return resolver;
  }

  @Bean
  @org.springframework.core.annotation.Order(1)
  public SecurityFilterChain filterChain(HttpSecurity http, @Lazy OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) throws Exception {
      http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/static/**", "/app.js").permitAll()
                        .requestMatchers("/menu", "/menu.html", "/menu.js").permitAll()
                        .requestMatchers("/profile", "/profile.html", "/profile.js").permitAll()
                        .requestMatchers("/apiv1/auth/login", "/apiv1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/apiv1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/apiv1/guest/table-orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/apiv1/guest/tables/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/apiv1/tables/by-qr/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/apiv1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/apiv1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/apiv1/auth/force-change-password").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .oauth2Login(oauth -> oauth.successHandler(oAuth2LoginSuccessHandler)
                );
      
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
