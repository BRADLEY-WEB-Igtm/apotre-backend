package com.doctrine.apotres.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.doctrine.apotres.dto.AuthDTO;
import com.doctrine.apotres.service.AuthService;

import java.util.Map;

/**
 * ============================================================
 * CONTROLLER AUTHENTIFICATION
 *
 * Endpoints de connexion pour les administrateurs du dashboard.
 *
 * @RestController = retourne du JSON automatiquement
 * @RequestMapping = toutes les URLs commencent par /api/auth
 * @CrossOrigin    = autorise les appels depuis le frontend (CORS)
 * ============================================================
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;
    // Service d'authentification — vérifie les identifiants et génère le JWT

    /**
     * Connexion administrateur
     *
     * Méthode  : POST
     * URL      : /api/auth/login
     * Accès    : Public (sans token)
     * Body JSON: { "username": "fridolinbradley", "motDePasse": "Admin@2026!" }
     * Réponse  : { "token": "eyJ...", "username": "fridolinbradley", "role": "SUPER_ADMIN", ... }
     *
     * @Valid        = valide le body selon les contraintes de AuthDTO.LoginRequest
     * @RequestBody  = désérialise le JSON reçu en objet Java
     */
    @PostMapping("/login")
    public ResponseEntity<?> connecter(
        @Valid @RequestBody AuthDTO.LoginRequest request
    ) {
        try {
            // Délègue au service — vérifie les identifiants et génère le JWT
            AuthDTO.LoginResponse response = authService.connecter(request);

            // HTTP 200 OK avec le token dans le body
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // Identifiants incorrects → HTTP 401 Unauthorized
            return ResponseEntity
                .status(401)
                .body(Map.of(
                    "erreur",  "Identifiants incorrects",
                    "message", "Nom d'utilisateur ou mot de passe invalide"
                ));

        } catch (Exception e) {
            // Erreur serveur inattendue → HTTP 500
            System.err.println("Erreur login : " + e.getMessage());
            return ResponseEntity
                .status(500)
                .body(Map.of(
                    "erreur",  "Erreur interne du serveur",
                    "message", e.getMessage()
                ));
        }
    }

    /**
     * Vérifie si le token JWT est encore valide
     *
     * Méthode : GET
     * URL     : /api/auth/verifier
     * Accès   : Protégé (token JWT requis dans le header Authorization)
     * Réponse : { "valide": true, "username": "fridolinbradley" }
     *
     * Le frontend l'appelle au chargement du dashboard pour vérifier
     * que la session est toujours active (token non expiré)
     */
    @GetMapping("/verifier")
    public ResponseEntity<?> verifierToken() {
        // Si on arrive ici, JwtFilter a déjà validé le token
        // SecurityContextHolder.getContext() = contexte de sécurité de la requête courante
        String username = SecurityContextHolder
            .getContext()           // Contexte de sécurité Spring
            .getAuthentication()    // L'authentification de l'utilisateur courant
            .getName();             // Son username (contenu dans le token JWT)

        return ResponseEntity.ok(Map.of(
            "valide",   true,
            "username", username
        ));
    }
}
