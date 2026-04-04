package com.doctrine.apotres.entity;

// Jakarta Persistence = bibliothèque standard Java pour mapper
// les classes Java vers des tables de base de données
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lombok — génère automatiquement getters, setters, constructeurs
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * ENTITÉ PUBLICATION
 *
 * Cette entité représente TOUT le contenu publié sur le site :
 * - Enseignements (textes bibliques)
 * - Sessions Zoom (audios lundi/jeudi)
 * - Émissions Radios
 * - Livres / PDFs
 * - Vidéos
 *
 * @Entity = dit à Hibernate de créer une table "publications" en BD
 * @Table  = nom explicite de la table en base
 * ============================================================
 */
@Entity
@Table(name = "publications")
@Data               // Lombok : génère tous les getters et setters
@NoArgsConstructor  // Lombok : génère un constructeur sans arguments
@AllArgsConstructor // Lombok : génère un constructeur avec tous les arguments
public class Publication {

    // ============================================================
    // CLÉ PRIMAIRE
    // @Id = colonne identifiant unique de la table
    // @GeneratedValue = la BD génère automatiquement la valeur (1, 2, 3...)
    // IDENTITY = utilise l'AUTO_INCREMENT de MySQL
    // ============================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // TYPE DE PUBLICATION
    // Enum = liste de valeurs fixes autorisées
    // Un type ne peut être QUE l'une de ces valeurs
    // ============================================================
    @Enumerated(EnumType.STRING)
    // STRING = stocke le nom du type en texte ("ENSEIGNEMENT", "ZOOM"...)
    // au lieu d'un nombre (0, 1, 2...) — plus lisible en base
    @NotNull(message = "Le type de publication est obligatoire")
    @Column(name = "type", nullable = false)
    private TypePublication type;

    // ============================================================
    // TITRE
    // @NotBlank = ne peut pas être null ni vide ni que des espaces
    // @Column = configure la colonne en base de données
    // ============================================================
    @NotBlank(message = "Le titre est obligatoire")
    @Column(name = "titre", nullable = false, length = 500)
    private String titre;

    // ============================================================
    // CONTENU TEXTE
    // @Lob = Large Object — pour les textes très longs (articles, sermons)
    // @Column(columnDefinition = "LONGTEXT") = type MySQL pour grands textes
    // ============================================================
    @Lob
    @Column(name = "contenu", columnDefinition = "LONGTEXT")
    private String contenu;

    // ============================================================
    // CATÉGORIE
    // Ex: "Évangiles", "Enseignements", "Zoom", "Émissions Radios"
    // ============================================================
    @Column(name = "categorie", length = 100)
    private String categorie;

    // ============================================================
    // SOUS-CATÉGORIE
    // Ex: "Comment faire pour être sauvé ?", "Les textes"
    // ============================================================
    @Column(name = "sous_categorie", length = 200)
    private String sousCategorie;

    // ============================================================
    // AUTEUR
    // Nom de l'administrateur qui a publié le contenu
    // Ex: "fridolinbradley", "admin"
    // ============================================================
    @Column(name = "auteur", length = 100)
    private String auteur;

    // ============================================================
    // STATUT
    // PUBLIE = visible sur le site
    // BROUILLON = sauvegardé mais pas visible
    // SUSPENDU = dépublié temporairement
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutPublication statut = StatutPublication.BROUILLON;
    // Par défaut = BROUILLON (sécurité : pas de publication accidentelle)

    // ============================================================
    // CHEMIN DU FICHIER AUDIO (pour les audios, zoom, radio)
    // Ex: "uploads/audios/zoom-2026-03-20.mp3"
    // ============================================================
    @Column(name = "chemin_audio", length = 500)
    private String cheminAudio;

    // CHEMIN DU 2ème FICHIER AUDIO (partie 2)
    @Column(name = "chemin_audio2", length = 500)
    private String cheminAudio2;

    // CHEMIN DU 3ème FICHIER AUDIO (partie 3)
    @Column(name = "chemin_audio3", length = 500)
    private String cheminAudio3;

    // RÉSUMÉ / EXTRAIT COURT
    @Column(name = "resume", length = 1000)
    private String resume;

    // PRÉDICATEUR / ORATEUR
    @Column(name = "predicateur", length = 200)
    private String predicateur;

    // IMAGE À LA UNE (chemin du fichier)
    @Column(name = "image_une", length = 500)
    private String imageUne;

    // ============================================================
    // CHEMIN DU FICHIER PDF (pour les livres)
    // Ex: "uploads/pdfs/marque-de-la-bete.pdf"
    // ============================================================
    @Column(name = "chemin_pdf", length = 500)
    private String cheminPdf;

    // ============================================================
    // LIEN VIDÉO (pour les vidéos YouTube)
    // Ex: "https://www.youtube.com/watch?v=..."
    // ============================================================
    @Column(name = "lien_video", length = 500)
    private String lienVideo;

    // ============================================================
    // JOUR DE ZOOM (pour les sessions Zoom)
    // Ex: "LUNDI" ou "JEUDI"
    // ============================================================
    @Column(name = "jour_zoom", length = 20)
    private String jourZoom;

    // ============================================================
    // DATE DE LA SESSION (pour les Zoom et Radio)
    // Ex: "20/03/2026"
    // ============================================================
    @Column(name = "date_session", length = 50)
    private String dateSession;

    // ============================================================
    // TAGS / MOTS-CLÉS
    // Ex: "dime,loi,grace,doctrine"
    // Stocké en une seule chaîne séparée par des virgules
    // ============================================================
    @Column(name = "tags", length = 500)
    private String tags;

    // ============================================================
    // AUTORISER LES COMMENTAIRES
    // true = les visiteurs peuvent commenter cette publication
    // false = les commentaires sont désactivés
    // ============================================================
    @Column(name = "commentaires_actifs")
    private Boolean commentairesActifs = true;

    // ============================================================
    // DATES AUTOMATIQUES
    // @Column(updatable = false) = cette valeur n'est jamais modifiée
    // ============================================================
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    // ============================================================
    // RELATION AVEC LES COMMENTAIRES
    // Une publication peut avoir plusieurs commentaires
    // mappedBy = "publication" → la colonne de jointure est dans Commentaire
    // CascadeType.ALL = si on supprime la publication, ses commentaires sont supprimés
    // FetchType.LAZY = les commentaires ne sont chargés QUE si on les demande
    // (évite de charger inutilement)
    // ============================================================
    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentaires;

    // ============================================================
    // MÉTHODES AUTOMATIQUES (@PrePersist / @PreUpdate)
    // Ces méthodes sont appelées automatiquement par Hibernate
    // AVANT d'insérer ou de mettre à jour en base
    // ============================================================

    @PrePersist
    // @PrePersist = exécuté juste avant le INSERT en base
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        // LocalDateTime.now() = date et heure actuelles
        this.dateModification = LocalDateTime.now();
    }

    @PreUpdate
    // @PreUpdate = exécuté juste avant le UPDATE en base
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }


    // ============================================================
    // ÉNUMÉRATIONS — valeurs fixes autorisées
    // ============================================================

    /**
     * Types de publication disponibles dans le dashboard
     * Correspond aux types de la page publication.html
     */
    public enum TypePublication {
        ENSEIGNEMENT,   // Article ou texte biblique
        AUDIO,          // Fichier audio d'enseignement
        ZOOM,           // Session Zoom (lundi ou jeudi)
        RADIO,          // Émission radio
        LIVRE,          // Livre / PDF téléchargeable
        VIDEO           // Vidéo YouTube
    }

    /**
     * Statuts possibles d'une publication
     */
    public enum StatutPublication {
        PUBLIE,     // Visible sur le site par tous les visiteurs
        BROUILLON,  // Sauvegardé mais pas encore publié
        SUSPENDU    // Dépublié temporairement par l'admin
    }
}