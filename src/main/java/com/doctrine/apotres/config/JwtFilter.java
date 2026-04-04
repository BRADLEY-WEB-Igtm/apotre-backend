package com.doctrine.apotres.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 * FILTRE JWT — s'exécute sur CHAQUE requête HTTP reçue
 *
 * Rôle : intercepter chaque requête, lire le token JWT dans
 * le header Authorization, le valider et identifier l'utilisateur.
 *
 * Flux :
 * Requête → JwtFilter → SecurityConfig → Controller
 *
 * OncePerRequestFilter = exécuté une seule fois par requête
 * (certains filtres peuvent s'exécuter plusieurs fois, pas celui-ci)
 * ============================================================
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    // JwtUtil = notre classe pour valider/décoder les tokens

    @Autowired
    private UserDetailsService userDetailsService;
    // UserDetailsService = charge les infos de l'utilisateur depuis la BD

    /**
     * Méthode principale du filtre — exécutée à chaque requête
     *
     * @param request  La requête HTTP entrante
     * @param response La réponse HTTP sortante
     * @param chain    La chaîne de filtres suivants
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        // ---- Étape 1 : Lire le header Authorization ----
        final String authHeader = request.getHeader("Authorization");
        // Le header ressemble à : "Bearer eyJhbGciOiJIUzI1NiJ9..."

        String username = null;
        String token    = null;

        // ---- Étape 2 : Extraire le token du header ----
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // startsWith("Bearer ") = vérifie que c'est bien un token Bearer
            token = authHeader.substring(7);
            // substring(7) = coupe "Bearer " (7 caractères) pour garder seulement le token

            try {
                username = jwtUtil.extraireUsername(token);
                // Lit le nom d'utilisateur contenu dans le token
            } catch (Exception e) {
                // Token malformé ou expiré → on ignore et on continue sans authentifier
                System.out.println("Erreur lecture token JWT : " + e.getMessage());
            }
        }

        // ---- Étape 3 : Authentifier l'utilisateur si le token est valide ----
        // SecurityContextHolder.getContext().getAuthentication() == null
        // = vérifie qu'il n'est pas déjà authentifié (évite de retraiter)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // Charge les infos complètes de l'utilisateur depuis la base de données

            if (jwtUtil.validerToken(token)) {
                // Le token est valide → on crée un objet d'authentification

                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,        // L'utilisateur authentifié
                        null,               // Pas de credentials (déjà validé via JWT)
                        userDetails.getAuthorities() // Ses rôles (ADMIN, SUPER_ADMIN...)
                    );

                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                    // Ajoute les détails de la requête (IP, session...) à l'authentification
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                // Enregistre l'authentification dans le contexte de sécurité
                // → Spring Security sait maintenant qui fait cette requête
            }
        }

        // ---- Étape 4 : Passer à la suite de la chaîne de filtres ----
        chain.doFilter(request, response);
        // Sans ce appel, la requête s'arrête ici et ne parvient jamais au Controller
    }
}
