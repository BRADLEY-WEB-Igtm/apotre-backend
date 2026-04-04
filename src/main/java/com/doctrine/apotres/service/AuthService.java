package com.doctrine.apotres.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.doctrine.apotres.config.JwtUtil;
import com.doctrine.apotres.dto.AuthDTO;
import com.doctrine.apotres.entity.Utilisateur;
import com.doctrine.apotres.repository.UtilisateurRepository;

import java.time.LocalDateTime;

/**
 * ============================================================
 * SERVICE AUTHENTIFICATION
 *
 * Gère la connexion des administrateurs au dashboard :
 * 1. Vérifie le username et mot de passe
 * 2. Génère un token JWT en cas de succès
 * 3. Retourne le token + infos admin au frontend
 *
 * @Service = composant de service Spring (logique métier)
 * ============================================================
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    // Gère le processus d'authentification Spring Security

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    // Pour charger et mettre à jour l'utilisateur en BD

    @Autowired
    private JwtUtil jwtUtil;
    // Pour générer le token JWT après connexion réussie

    @Autowired
    private PasswordEncoder passwordEncoder;
    // BCrypt — pour hasher les mots de passe

    /**
     * Connecte un administrateur
     * Appelé depuis AuthController → POST /api/auth/login
     *
     * @param request Les identifiants (username + mot de passe)
     * @return La réponse avec le token JWT et les infos admin
     * @throws BadCredentialsException si les identifiants sont incorrects
     */
    public AuthDTO.LoginResponse connecter(AuthDTO.LoginRequest request) {

        // ---- Étape 1 : Authentifier via Spring Security ----
        // UsernamePasswordAuthenticationToken = encapsule username + mot de passe
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getMotDePasse()
                // Spring Security compare automatiquement avec le hash BCrypt en BD
            )
        );
        // Si les identifiants sont incorrects → exception BadCredentialsException
        // Spring Security gère ça automatiquement

        // ---- Étape 2 : Récupérer l'utilisateur depuis la BD ----
        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() ->
                new BadCredentialsException("Utilisateur non trouvé")
            );

        // ---- Étape 3 : Mettre à jour la date de dernière connexion ----
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
        // save() = UPDATE si l'entité a un ID existant

        // ---- Étape 4 : Générer le token JWT ----
        String token = jwtUtil.genererToken(utilisateur.getUsername());
        // Le token contient le username et expire dans 24h

        // ---- Étape 5 : Retourner la réponse ----
        return new AuthDTO.LoginResponse(
            token,
            "Bearer",
            utilisateur.getId(),
            utilisateur.getUsername(),
            utilisateur.getNomComplet(),
            utilisateur.getRole(),
            86400000L // 24h en millisecondes
        );
    }

    /**
     * Crée le premier compte Super Admin
     * Appelé au démarrage de l'application si aucun admin n'existe
     * (voir DataInitializer)
     *
     * @param username Nom d'utilisateur
     * @param email Email
     * @param motDePasse Mot de passe en clair (sera hashé)
     * @param nomComplet Nom complet
     */
    public Utilisateur creerAdmin(
        String username,
        String email,
        String motDePasse,
        String nomComplet
    ) {
        Utilisateur admin = new Utilisateur();
        admin.setUsername(username);
        admin.setEmail(email);

        // Hash le mot de passe avec BCrypt avant de le stocker
        // JAMAIS stocker un mot de passe en clair !
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));

        admin.setNomComplet(nomComplet);
        admin.setRole(Utilisateur.RoleUtilisateur.SUPER_ADMIN);
        admin.setActif(true);

        return utilisateurRepository.save(admin);
    }
}
