package com.doctrine.apotres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * ENTITÉ PUBLICATION — VERSION CORRIGÉE
 *
 * CORRECTION : les audios sont maintenant stockés dans une liste
 * dynamique (table séparée publication_audios) au lieu de 3
 * colonnes fixes. Supporte un nombre illimité de parties audio.
 *
 * Backward compatible : les anciennes colonnes chemin_audio,
 * chemin_audio2, chemin_audio3 sont conservées pour ne pas
 * perdre les publications déjà en base de données.
 * ============================================================
 */
@Entity
@Table(name = "publications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Le type de publication est obligatoire")
    @Column(name = "type", nullable = false)
    private TypePublication type;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(name = "titre", nullable = false, length = 500)
    private String titre;

    @Lob
    @Column(name = "contenu", columnDefinition = "LONGTEXT")
    private String contenu;

    @Column(name = "categorie", length = 100)
    private String categorie;

    @Column(name = "sous_categorie", length = 200)
    private String sousCategorie;

    @Column(name = "auteur", length = 100)
    private String auteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutPublication statut = StatutPublication.BROUILLON;

    // ============================================================
    // ANCIENS CHAMPS AUDIO — conservés pour backward compatibility
    // Les publications déjà en BD continuent de fonctionner
    // Les NOUVELLES publications utilisent cheminsAudio (ci-dessous)
    // ============================================================

    @Column(name = "chemin_audio", length = 500)
    private String cheminAudio;

    @Column(name = "chemin_audio2", length = 500)
    private String cheminAudio2;

    @Column(name = "chemin_audio3", length = 500)
    private String cheminAudio3;

    // ============================================================
    // NOUVEAU — LISTE DYNAMIQUE D'AUDIOS (nombre illimité)
    //
    // @ElementCollection = crée une table séparée "publication_audios"
    // avec les colonnes : publication_id + chemin + position
    //
    // @OrderColumn = conserve l'ordre des parties (Partie 1, 2, 3...)
    //
    // Quand Spring voit ddl-auto=update, il crée cette table
    // automatiquement au prochain démarrage sans toucher les données
    // ============================================================
    @ElementCollection(fetch = FetchType.EAGER)
    // EAGER = charge les audios en même temps que la publication
    // (pas de requête supplémentaire — important pour les performances)

    @CollectionTable(
        name = "publication_audios",
        // Nom de la table créée automatiquement en BD
        joinColumns = @JoinColumn(name = "publication_id")
        // Colonne de jointure : publication_id → relie à publications.id
    )
    @OrderColumn(name = "position")
    // position = conserve l'ordre d'insertion (Partie 1 avant Partie 2)

    @Column(name = "chemin")
    // Chaque ligne = un chemin audio ex: "uploads/audios/zoom-abc.mp3"
    private List<String> cheminsAudio = new ArrayList<>();
    // ArrayList = liste vide par défaut (pas null)

    // ============================================================
    // AUTRES CHAMPS (inchangés)
    // ============================================================

    @Column(name = "resume", length = 1000)
    private String resume;

    @Column(name = "predicateur", length = 200)
    private String predicateur;

    @Column(name = "image_une", length = 500)
    private String imageUne;

    @Column(name = "chemin_pdf", length = 500)
    private String cheminPdf;

    @Column(name = "lien_video", length = 500)
    private String lienVideo;

    @Column(name = "jour_zoom", length = 20)
    private String jourZoom;

    @Column(name = "date_session", length = 50)
    private String dateSession;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "commentaires_actifs")
    private Boolean commentairesActifs = true;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentaires;

    @PrePersist
    protected void onCreate() {
        this.dateCreation     = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    public enum TypePublication {
        ENSEIGNEMENT,
        AUDIO,
        ZOOM,
        RADIO,
        LIVRE,
        VIDEO
    }

    public enum StatutPublication {
        PUBLIE,
        BROUILLON,
        SUSPENDU
    }
}