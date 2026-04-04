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
 * ENTITÉ COMMENTAIRE
 *
 * Représente un commentaire laissé par un visiteur
 * sur une publication du site (article, zoom, audio...)
 *
 * Les commentaires nécessitent une approbation de l'admin
 * avant d'être visibles sur le site — c'est le système de
 * modération géré depuis le dashboard admin
 * ============================================================
 */
@Entity
@Table(name = "commentaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commentaire {

    // Identifiant unique auto-généré par MySQL
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // NOM DU VISITEUR
    // @NotBlank = obligatoire, ne peut pas être vide
    // ============================================================
    @NotBlank(message = "Le nom est obligatoire")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    // ============================================================
    // EMAIL DU VISITEUR
    // @Email = vérifie que le format est valide (contient @)
    // Non obligatoire — certains visiteurs préfèrent rester anonymes
    // ============================================================
    @Email(message = "Format d'email invalide")
    @Column(name = "email", length = 150)
    private String email;

    // ============================================================
    // TEXTE DU COMMENTAIRE
    // @Lob = texte long (le visiteur peut écrire beaucoup)
    // ============================================================
    @NotBlank(message = "Le message est obligatoire")
    @Lob
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    // ============================================================
    // RELATION AVEC LA PUBLICATION
    // @ManyToOne = plusieurs commentaires pour une publication
    // @JoinColumn = nom de la colonne clé étrangère en base
    //   → crée une colonne "publication_id" dans la table commentaires
    // FetchType.LAZY = la publication n'est chargée que si nécessaire
    // ============================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id")
    private Publication publication;

    // ============================================================
    // SOURCE DU COMMENTAIRE
    // Indique depuis quelle page le commentaire a été envoyé
    // Ex: "zoom", "enseignements-audios", "emissions-radios"
    // Correspond aux différents fetch('/api/commentaires/...') du frontend
    // ============================================================
    @Column(name = "source", length = 100)
    private String source;

    // ============================================================
    // STATUT DU COMMENTAIRE
    // EN_ATTENTE = nouveau commentaire, pas encore modéré
    // APPROUVE = approuvé par l'admin → visible sur le site
    // REJETE = refusé par l'admin → non visible
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutCommentaire statut = StatutCommentaire.EN_ATTENTE;
    // Par défaut = EN_ATTENTE (tout commentaire doit être modéré)

    // ============================================================
    // ADRESSE IP DU VISITEUR
    // Utile pour détecter les spams ou abus
    // ============================================================
    @Column(name = "adresse_ip", length = 50)
    private String adresseIp;

    // Date de soumission du commentaire
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    // Date de modération (quand l'admin a approuvé/rejeté)
    @Column(name = "date_moderation")
    private LocalDateTime dateModeration;

    // Qui a modéré (nom de l'admin)
    @Column(name = "modere_par", length = 100)
    private String moderePar;

    // Exécuté automatiquement avant le INSERT en base
    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    // ============================================================
    // ÉNUMÉRATION — statuts possibles d'un commentaire
    // ============================================================
    public enum StatutCommentaire {
        EN_ATTENTE, // Nouveau — en attente de modération
        APPROUVE,   // Approuvé — visible sur le site
        REJETE      // Rejeté — non visible
    }
}
