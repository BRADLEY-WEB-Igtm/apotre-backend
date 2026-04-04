package com.doctrine.apotres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ENTITÉ UTILISATEUR
 *
 * Représente un administrateur du dashboard
 * Seuls les utilisateurs enregistrés peuvent :
 * - Se connecter au dashboard (/admin)
 * - Publier du contenu
 * - Modérer les commentaires
 * - Gérer le site
 *
 * Correspond à la page login.html et compte.html du dashboard
 * ============================================================
 */
@Entity
@Table(name = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    // Identifiant unique auto-généré
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // NOM D'UTILISATEUR
    // unique = true → deux admins ne peuvent pas avoir le même username
    // Ex: "fridolinbradley", "admin"
    // ============================================================
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    // ============================================================
    // EMAIL
    // Utilisé pour récupérer le mot de passe ou les notifications
    // ============================================================
    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    // ============================================================
    // MOT DE PASSE HASHÉ
    // JAMAIS stocké en clair — toujours hashé avec BCrypt
    // BCrypt ajoute un "sel" aléatoire → même mot de passe = hash différent
    // ============================================================
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    // ============================================================
    // NOM COMPLET
    // Ex: "Fridolin Bradley"
    // ============================================================
    @Column(name = "nom_complet", length = 200)
    private String nomComplet;

    // ============================================================
    // RÔLE
    // SUPER_ADMIN = accès total (peut gérer les utilisateurs)
    // ADMIN = peut publier et modérer
    // EDITEUR = peut publier mais pas modérer ni supprimer
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleUtilisateur role = RoleUtilisateur.ADMIN;

    // ============================================================
    // ACTIF
    // true = peut se connecter
    // false = compte désactivé (sans suppression)
    // ============================================================
    @Column(name = "actif")
    private Boolean actif = true;

    // Chemin de la photo de profil (optionnel)
    @Lob
    @Column(name = "photo_profil", columnDefinition = "LONGTEXT")
    // LONGTEXT = peut stocker jusqu'à 4GB de texte
    // Nécessaire car une photo en base64 fait 50 000 à 300 000 caractères
    // length=500 était trop petit et causait une erreur SQL
    private String photoProfil;

    // Date de création du compte
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    // Date de dernière connexion
    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    // Exécuté automatiquement avant le INSERT en base
    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    // ============================================================
    // ÉNUMÉRATION — rôles disponibles
    // ============================================================
    public enum RoleUtilisateur {
        SUPER_ADMIN, // Accès total
        ADMIN,       // Accès standard
        EDITEUR      // Accès limité (publication seulement)
    }
}
