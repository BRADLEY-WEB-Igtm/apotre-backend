
package com.doctrine.apotres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ENTITÉ PHOTO
 * Stocke les métadonnées des photos publiées dans la galerie.
 * Les fichiers images sont hébergés sur Cloudinary.
 */
@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(name = "titre", nullable = false, length = 300)
    private String titre;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "url_photo", nullable = false, length = 500)
    private String urlPhoto;
    /* URL Cloudinary de l'image */

    @Column(name = "categorie", length = 100)
    private String categorie;
    /* Ex: "culte", "zoom", "evenement", "autre" */

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutPhoto statut = StatutPhoto.PUBLIE;

    @Column(name = "auteur", length = 100)
    private String auteur;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation     = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    public enum StatutPhoto {
        PUBLIE,
        BROUILLON,
        SUPPRIME
    }
}