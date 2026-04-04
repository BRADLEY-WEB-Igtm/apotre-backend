package com.doctrine.apotres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ENTITÉ PRIERE
 *
 * Représente une demande de prière soumise par un visiteur
 * Visible et gérable depuis le dashboard admin (prieres.html)
 * Correspond aux 13 prières visibles dans le dashboard WordPress
 * ============================================================
 */
@Entity
@Table(name = "prieres")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Priere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom de la personne qui demande la prière
    @NotBlank(message = "Le nom est obligatoire")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    // Email (optionnel — pour répondre à la personne)
    @Column(name = "email", length = 150)
    private String email;

    // Sujet de la prière (court résumé)
    @Column(name = "sujet", length = 200)
    private String sujet;

    // Texte complet de la demande de prière
    @NotBlank(message = "Le message est obligatoire")
    @Lob
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    // Demande anonyme ? (true = nom non affiché publiquement)
    @Column(name = "anonyme")
    private Boolean anonyme = false;

    // Statut de la demande
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutPriere statut = StatutPriere.EN_ATTENTE;

    // Date de soumission
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum StatutPriere {
        EN_ATTENTE, // Pas encore traitée
        PRISE_EN_CHARGE, // L'admin l'a vue et prie pour
        ARCHIVEE    // Archivée
    }
}
