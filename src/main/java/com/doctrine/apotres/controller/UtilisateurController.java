package com.doctrine.apotres.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.doctrine.apotres.entity.Utilisateur;
import com.doctrine.apotres.repository.UtilisateurRepository;
import com.doctrine.apotres.service.AuthService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONTROLLER UTILISATEUR
 *
 * Gère les comptes administrateurs :
 * - Voir / modifier son propre profil
 * - Changer son mot de passe
 * - SUPER_ADMIN : créer / désactiver d'autres admins
 *
 * Réponse à la question sur les multi-admins :
 * Seul le SUPER_ADMIN peut créer de nouveaux comptes admin.
 * Il crée le compte via le dashboard, définit un mot de passe
 * temporaire, et le nouvel admin le change dès sa première
 * connexion via la page Sécurité.
 * ============================================================
 */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@CrossOrigin
public class UtilisateurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    // ============================================================
    // PROFIL DE L'ADMIN CONNECTÉ
    // ============================================================

    /**
     * Récupère les infos du profil de l'admin connecté
     * GET /api/admin/utilisateurs/profil
     */
    @GetMapping("/profil")
    public ResponseEntity<?> getProfil() {
        String username = SecurityContextHolder
            .getContext().getAuthentication().getName();

        Utilisateur admin = utilisateurRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Map<String, Object> profil = new HashMap<>();
        profil.put("id",         admin.getId());
        profil.put("username",   admin.getUsername());
        profil.put("email",      admin.getEmail() != null ? admin.getEmail() : "");
        profil.put("nomComplet", admin.getNomComplet() != null ? admin.getNomComplet() : "");
        profil.put("role",       admin.getRole().name());
        profil.put("photo",      admin.getPhotoProfil() != null ? admin.getPhotoProfil() : "");
        // photo = base64 de la photo de profil

        return ResponseEntity.ok(profil);
    }

    /**
     * Met à jour le profil de l'admin connecté
     * PUT /api/admin/utilisateurs/profil
     */
    @PutMapping("/profil")
    public ResponseEntity<?> mettreAJourProfil(
        @RequestBody MettreAJourProfilRequest request
    ) {
        String username = SecurityContextHolder
            .getContext().getAuthentication().getName();

        Utilisateur admin = utilisateurRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifie unicité du nouveau username
        if (request.getUsername() != null
            && !request.getUsername().isBlank()
            && !request.getUsername().equals(admin.getUsername())
            && utilisateurRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Ce nom d'utilisateur est déjà utilisé"));
        }

        // Vérifie unicité du nouvel email
        if (request.getEmail() != null
            && !request.getEmail().isBlank()
            && !request.getEmail().equals(admin.getEmail())
            && utilisateurRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Cet email est déjà utilisé"));
        }

        if (request.getNomComplet() != null && !request.getNomComplet().isBlank())
            admin.setNomComplet(request.getNomComplet());
        if (request.getEmail() != null && !request.getEmail().isBlank())
            admin.setEmail(request.getEmail());
        if (request.getUsername() != null && !request.getUsername().isBlank())
            admin.setUsername(request.getUsername());
        if (request.getPhoto() != null && !request.getPhoto().isBlank())
            admin.setPhotoProfil(request.getPhoto());
        // Photo stockée en base64 dans la colonne photo_profil

        Utilisateur sauvegarde = utilisateurRepository.save(admin);

        return ResponseEntity.ok(Map.of(
            "message",    "Profil mis à jour avec succès",
            "username",   sauvegarde.getUsername(),
            "nomComplet", sauvegarde.getNomComplet() != null ? sauvegarde.getNomComplet() : "",
            "email",      sauvegarde.getEmail() != null ? sauvegarde.getEmail() : ""
        ));
    }

    /**
     * Changer son propre mot de passe
     * PUT /api/admin/utilisateurs/changer-mot-de-passe
     */
    @PutMapping("/changer-mot-de-passe")
    public ResponseEntity<?> changerMotDePasse(
        @Valid @RequestBody ChangerMotDePasseRequest request
    ) {
        String username = SecurityContextHolder
            .getContext().getAuthentication().getName();

        Utilisateur admin = utilisateurRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifie l'ancien mot de passe
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), admin.getMotDePasse())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Le mot de passe actuel est incorrect"));
        }

        // Vérifie que le nouveau est différent
        if (passwordEncoder.matches(request.getNouveauMotDePasse(), admin.getMotDePasse())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Le nouveau mot de passe doit être différent de l'ancien"));
        }

        // Hash et sauvegarde
        admin.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(admin);

        return ResponseEntity.ok(Map.of(
            "message", "Mot de passe modifié avec succès. Veuillez vous reconnecter."
        ));
    }

    // ============================================================
    // GESTION DES AUTRES ADMINS — SUPER_ADMIN SEULEMENT
    // ============================================================

    /**
     * Liste tous les administrateurs
     * GET /api/admin/utilisateurs
     * Accès : SUPER_ADMIN uniquement
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    // @PreAuthorize = vérifie le rôle AVANT d'exécuter la méthode
    public ResponseEntity<?> listerAdmins() {
        List<Map<String, Object>> admins = utilisateurRepository.findAll()
            .stream()
            .map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id",            u.getId());
                m.put("username",      u.getUsername());
                m.put("email",         u.getEmail() != null ? u.getEmail() : "");
                m.put("nomComplet",    u.getNomComplet() != null ? u.getNomComplet() : "");
                m.put("role",          u.getRole().name());
                m.put("actif",         u.getActif());
                m.put("dateCreation",  u.getDateCreation() != null ? u.getDateCreation().toString() : "");
                m.put("derniereConnexion", u.getDerniereConnexion() != null ? u.getDerniereConnexion().toString() : "Jamais");
                // On ne retourne JAMAIS le mot de passe hashé
                return m;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(admins);
    }

    /**
     * Crée un nouvel administrateur
     * POST /api/admin/utilisateurs/creer
     * Accès : SUPER_ADMIN uniquement
     *
     * Fonctionnement multi-admins :
     * 1. Le SUPER_ADMIN va dans le dashboard → Utilisateurs → Créer
     * 2. Il remplit nom, email, username, mot de passe temporaire, rôle
     * 3. Le nouvel admin reçoit ses identifiants
     * 4. Il se connecte et change son mot de passe via Sécurité
     */
    @PostMapping("/creer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> creerAdmin(
        @Valid @RequestBody CreerAdminRequest request
    ) {
        // Vérifie que le username n'existe pas déjà
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Ce nom d'utilisateur est déjà utilisé"));
        }

        // Vérifie que l'email n'existe pas déjà
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Cet email est déjà utilisé"));
        }

        // Détermine le rôle — par défaut ADMIN si non spécifié
        Utilisateur.RoleUtilisateur role;
        try {
            role = request.getRole() != null
                ? Utilisateur.RoleUtilisateur.valueOf(request.getRole())
                : Utilisateur.RoleUtilisateur.ADMIN;
        } catch (IllegalArgumentException e) {
            role = Utilisateur.RoleUtilisateur.ADMIN;
        }

        // Crée le compte
        Utilisateur nouvelAdmin = new Utilisateur();
        nouvelAdmin.setUsername(request.getUsername());
        nouvelAdmin.setEmail(request.getEmail());
        nouvelAdmin.setNomComplet(request.getNomComplet());
        nouvelAdmin.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        // Mot de passe temporaire — l'admin le changera à sa première connexion
        nouvelAdmin.setRole(role);
        nouvelAdmin.setActif(true);

        Utilisateur sauvegarde = utilisateurRepository.save(nouvelAdmin);

        return ResponseEntity.status(201).body(Map.of(
            "message",   "Compte admin créé avec succès",
            "id",        sauvegarde.getId(),
            "username",  sauvegarde.getUsername(),
            "role",      sauvegarde.getRole().name()
        ));
    }

    /**
     * Active ou désactive un compte admin
     * PUT /api/admin/utilisateurs/{id}/actif
     * Accès : SUPER_ADMIN uniquement
     */
    @PutMapping("/{id}/actif")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleActif(@PathVariable Long id) {
        Utilisateur admin = utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        // Empêche de désactiver son propre compte
        String moi = SecurityContextHolder.getContext().getAuthentication().getName();
        if (admin.getUsername().equals(moi)) {
            return ResponseEntity.status(400)
                .body(Map.of("message", "Vous ne pouvez pas désactiver votre propre compte"));
        }

        admin.setActif(!admin.getActif());
        // Toggle : actif → inactif, inactif → actif
        utilisateurRepository.save(admin);

        return ResponseEntity.ok(Map.of(
            "message", admin.getActif() ? "Compte activé" : "Compte désactivé",
            "actif",   admin.getActif()
        ));
    }

    // ============================================================
    // CLASSES INTERNES — body JSON des requêtes
    // ============================================================

    @Data
    public static class ChangerMotDePasseRequest {
        @NotBlank(message = "L'ancien mot de passe est obligatoire")
        private String ancienMotDePasse;

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Minimum 8 caractères")
        private String nouveauMotDePasse;
    }

    @Data
    public static class MettreAJourProfilRequest {
        private String nomComplet;
        private String email;
        private String username;
        private String photo;
        // base64 de la photo de profil
    }

    @Data
    public static class CreerAdminRequest {
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        private String username;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format email invalide")
        private String email;

        @NotBlank(message = "Le nom complet est obligatoire")
        private String nomComplet;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Minimum 8 caractères")
        private String motDePasse;
        // Mot de passe temporaire — l'admin le changera à la première connexion

        private String role;
        // "SUPER_ADMIN", "ADMIN" ou "EDITEUR" — défaut : "ADMIN"
    }
}
