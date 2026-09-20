package com.example.pipelineservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security route configuration.
 *
 * Role matrix:
 * ┌─────────────────────────────────┬─────────────────────────────────────────┐
 * │ Route                           │ Allowed roles                           │
 * ├─────────────────────────────────┼─────────────────────────────────────────┤
 * │ GET  /api/pipeline/**           │ ADMIN, DEVOPS, DEV, AUDITOR (all auth)  │
 * │ POST /api/pipeline/**           │ ADMIN, DEVOPS, DEV                      │
 * │ PUT  /api/pipeline/**           │ ADMIN, DEVOPS, DEV                      │
 * │ DELETE /api/pipeline/**         │ ADMIN, DEVOPS, DEV                      │
 * │ POST /{id}/restore              │ ADMIN only (enforced in service layer)  │
 * │ POST /api/executions/**         │ ADMIN, DEVOPS, DEV                      │
 * │ GET  /api/executions/**         │ ADMIN, DEVOPS, DEV, AUDITOR             │
 * └─────────────────────────────────┴─────────────────────────────────────────┘
 *
 * Fine-grained ownership checks (e.g. DEV sees only own projects) are enforced
 * inside the service layer via AuthorizationHelper, NOT here. This config is
 * the outer gate; the service is the inner gate.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // CORS is handled entirely by the API Gateway — disable it here
                // to prevent the gateway and this service both adding the
                // Access-Control-Allow-Origin header (which causes a duplicate
                // header error in the browser).
                .cors(cors -> cors.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints ──────────────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Jenkins callback — no JWT, called from CI server
                        .requestMatchers(HttpMethod.PUT, "/api/executions/*/status").permitAll()

                        // ── AUDITOR: read-only, blocked from all writes ───────────────
                        // AUDITOR can GET but cannot POST/PUT/DELETE
                        .requestMatchers(HttpMethod.POST, "/api/pipeline/**").hasAnyRole("ADMIN", "DEVOPS", "DEV")
                        .requestMatchers(HttpMethod.PUT,  "/api/pipeline/**").hasAnyRole("ADMIN", "DEVOPS", "DEV")
                        .requestMatchers(HttpMethod.DELETE, "/api/pipeline/**").hasAnyRole("ADMIN", "DEVOPS", "DEV")

                        // AUDITOR cannot trigger executions (also enforced in ExecutionServiceImpl)
                        .requestMatchers(HttpMethod.POST, "/api/executions/**").hasAnyRole("ADMIN", "DEVOPS", "DEV")

                        // ── All authenticated users can GET ───────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/pipeline/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/executions/**").authenticated()

                        // ── Catch-all: require authentication ────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}