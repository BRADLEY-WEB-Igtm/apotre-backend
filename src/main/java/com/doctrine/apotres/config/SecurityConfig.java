package com.doctrine.apotres.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ============================================================
 * CONFIGURATION SPRING SECURITY
 *
 * Définit :
 * - Quelles routes sont publiques (accessibles sans token)
 * - Quelles routes sont protégées (nécessitent un token JWT)
 * - La configuration CORS (qui peut appeler l'API)
 * - L'encodage des mots de passe (BCrypt)
 *
 * @Configuration = classe de configuration Spring
 * @EnableWebSecurity = active la sécurité web Spring
 * ============================================================
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
// @EnableMethodSecurity = active @PreAuthorize sur les méthodes des controllers
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;
    // Notre filtre JWT à ajouter dans la chaîne de sécurité

    @Autowired
    private UserDetailsService userDetailsService;
    // Service qui charge les utilisateurs depuis la BD

    // Origines autorisées à appeler l'API (depuis application.properties)
    @Value("${app.cors.allowed-origins:https://doctrinedesapotres.com,https://www.doctrinedesapotres.com,http://localhost:5500,http://localhost:5501}")
    private String allowedOrigins;
    // Valeur par défaut = domaines autorisés si variable non définie

    /**
     * Configuration principale de la sécurité HTTP
     * Définit quelles routes sont publiques et lesquelles sont protégées
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ---- Désactive CSRF ----
            // CSRF = protection contre les attaques cross-site
            // On la désactive car on utilise JWT (tokens stateless)
            // Les APIs REST n'ont pas besoin de CSRF
            .csrf(csrf -> csrf.disable())

            // ---- Configure CORS ----
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ---- Définit les règles d'accès par route ----
            .authorizeHttpRequests(auth -> auth

                // Routes PUBLIQUES — accessibles sans token
                // Le site client peut les appeler librement

                // Connexion admin — évidemment publique
                .requestMatchers("/api/auth/login").permitAll()

                // Commentaires — les visiteurs peuvent en soumettre
                .requestMatchers(HttpMethod.POST, "/api/commentaires/**").permitAll()

                // Lectures publiques — le site client lit les publications
                .requestMatchers(HttpMethod.GET, "/api/publications/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/commentaires/approuves/**").permitAll()

                // Prières — les visiteurs peuvent en soumettre
                .requestMatchers(HttpMethod.POST, "/api/prieres").permitAll()

                // Fichiers uploadés — accessibles publiquement (images, audios, PDFs)
                .requestMatchers("/uploads/**").permitAll()

                /* Routes admin — nécessitent un token JWT valide ET un rôle admin */
                .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "EDITEUR")
                /* hasAnyRole = vérifie que l'utilisateur a l'un de ces rôles */

                /* Toutes les autres routes nécessitent au minimum d'être authentifié */
                .anyRequest().authenticated()
            )

            // ---- Session STATELESS ----
            // On n'utilise PAS les sessions HTTP côté serveur
            // Chaque requête doit contenir son token JWT
            // C'est le principe des APIs REST modernes
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ---- Ajoute notre filtre JWT ----
            // Le filtre JWT s'exécute AVANT le filtre d'authentification standard
            // UsernamePasswordAuthenticationFilter = filtre de login par formulaire (qu'on remplace par JWT)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS
     * CORS = Cross-Origin Resource Sharing
     * Définit quels domaines peuvent appeler notre API
     *
     * Sans CORS configuré, le navigateur bloque les appels depuis
     * un domaine différent (ex: doctrinedesapotres.com → api.doctrinedesapotres.com)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées (domaines qui peuvent appeler l'API)
        // split(",") + trim() = évite les espaces autour des virgules
        List<String> origines = java.util.Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .collect(java.util.stream.Collectors.toList());
        config.setAllowedOrigins(origines);
        // Ex: ["http://localhost:5501", "https://doctrinedesapotres.com"]

        // Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // OPTIONS = requis pour les requêtes "preflight" du navigateur

        // Headers autorisés dans les requêtes
        config.setAllowedHeaders(Arrays.asList(
            "Authorization",    // Pour le token JWT : "Bearer ..."
            "Content-Type",     // Type du contenu : "application/json"
            "Accept"            // Type de réponse attendu
        ));

        // Autorise l'envoi des cookies et credentials (nécessaire pour certains cas)
        config.setAllowCredentials(true);

        // Durée de mise en cache de la config CORS (en secondes)
        // Le navigateur ne refait pas la vérification pendant 3600s = 1h
        config.setMaxAge(3600L);

        // Applique cette configuration à toutes les routes (/api/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * Encodeur de mots de passe BCrypt
     * BCrypt hash les mots de passe de façon sécurisée
     * Strength 12 = nombre de "rounds" de hachage (plus élevé = plus sécurisé mais plus lent)
     * Standard = 10-12 en production
     *
     * @Bean = Spring gère cette instance (injectable partout)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Fournisseur d'authentification
     * Relie le UserDetailsService (charge l'user depuis BD) et
     * le PasswordEncoder (vérifie le mot de passe hashé)
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // Charge l'utilisateur par username depuis la BD
        provider.setPasswordEncoder(passwordEncoder());
        // Vérifie le mot de passe avec BCrypt
        return provider;
    }

    /**
     * AuthenticationManager — gère le processus d'authentification complet
     * Utilisé dans AuthService pour authentifier lors du login
     */
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
