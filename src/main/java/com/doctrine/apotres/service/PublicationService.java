package com.doctrine.apotres.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.doctrine.apotres.dto.PublicationDTO;
import com.doctrine.apotres.entity.Publication;
import com.doctrine.apotres.entity.Publication.StatutPublication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.repository.CommentaireRepository;
import com.doctrine.apotres.repository.PublicationRepository;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * SERVICE PUBLICATION — logique métier
 *
 * Contient toute la logique de gestion des publications :
 * - Créer une publication (avec ou sans fichier audio/PDF)
 * - Modifier / Suspendre / Republier
 * - Supprimer (et le fichier associé)
 * - Lister avec filtres et pagination
 *
 * @Service = composant Spring gérant la logique métier
 * ============================================================
 */
@Service
public class PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private FichierService fichierService;
    // Pour sauvegarder les fichiers audio et PDF uploadés

    /**
     * Crée une nouvelle publication
     * Appelé depuis PublicationController → POST /api/publications
     *
     * @param request Les données du formulaire publication.html
     * @param fichierAudio Fichier audio optionnel (pour type AUDIO, ZOOM, RADIO)
     * @param fichierPdf Fichier PDF optionnel (pour type LIVRE)
     * @return La publication créée
     */
    public PublicationDTO.Response creer(
        PublicationDTO.Request request,
        MultipartFile fichierAudio,
        MultipartFile fichierAudio2,
        MultipartFile fichierAudio3,
        MultipartFile fichierImage,
        MultipartFile fichierPdf
    ) throws IOException {

        // ---- Crée l'entité Publication ----
        Publication publication = new Publication();
        publication.setType(request.getType());
        publication.setTitre(request.getTitre());
        publication.setContenu(request.getContenu());
        publication.setCategorie(request.getCategorie());
        publication.setSousCategorie(request.getSousCategorie());
        publication.setTags(request.getTags());
        if (request.getResume() != null) publication.setResume(request.getResume());
        if (request.getPredicateur() != null) publication.setPredicateur(request.getPredicateur());
        publication.setLienVideo(request.getLienVideo());
        publication.setJourZoom(request.getJourZoom());
        publication.setDateSession(request.getDateSession());
        publication.setCommentairesActifs(
            request.getCommentairesActifs() != null ? request.getCommentairesActifs() : true
        );

        // Récupère le nom de l'admin connecté depuis le contexte de sécurité
        String auteur = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        // getName() = retourne le username de l'utilisateur authentifié
        publication.setAuteur(auteur);

        // ---- Gère le statut ----
        StatutPublication statut = request.getStatut() != null
            ? request.getStatut()
            : StatutPublication.BROUILLON;
        publication.setStatut(statut);

        if (statut == StatutPublication.PUBLIE) {
            publication.setDatePublication(LocalDateTime.now());
        }

        // ---- Sauvegarde les fichiers audio (parties 1, 2, 3) ----
        if (fichierAudio != null && !fichierAudio.isEmpty()) {
            publication.setCheminAudio(fichierService.sauvegarderAudio(fichierAudio));
        }
        if (fichierAudio2 != null && !fichierAudio2.isEmpty()) {
            publication.setCheminAudio2(fichierService.sauvegarderAudio(fichierAudio2));
        }
        if (fichierAudio3 != null && !fichierAudio3.isEmpty()) {
            publication.setCheminAudio3(fichierService.sauvegarderAudio(fichierAudio3));
        }
        // ---- Sauvegarde l'image à la une si présente ----
        if (fichierImage != null && !fichierImage.isEmpty()) {
            publication.setImageUne(fichierService.sauvegarderAudio(fichierImage));
        }
        // ---- Sauvegarde le fichier PDF si présent ----
        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            publication.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        // ---- Sauvegarde en base de données ----
        Publication sauvegardee = publicationRepository.save(publication);
        // save() = INSERT → retourne l'entité avec son ID généré

        return convertirEnResponse(sauvegardee);
    }

    /**
     * Modifie une publication existante
     * PUT /api/publications/{id}
     */
    public PublicationDTO.Response modifier(
        Long id,
        PublicationDTO.Request request,
        MultipartFile fichierAudio,
        MultipartFile fichierPdf
    ) throws IOException {

        // Cherche la publication — lève une exception si non trouvée
        Publication publication = publicationRepository.findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("Publication introuvable : " + id)
            );

        // Met à jour les champs
        publication.setTitre(request.getTitre());
        publication.setContenu(request.getContenu());
        publication.setCategorie(request.getCategorie());
        publication.setSousCategorie(request.getSousCategorie());
        publication.setTags(request.getTags());
        if (request.getResume() != null) publication.setResume(request.getResume());
        if (request.getPredicateur() != null) publication.setPredicateur(request.getPredicateur());
        publication.setLienVideo(request.getLienVideo());
        publication.setJourZoom(request.getJourZoom());
        publication.setDateSession(request.getDateSession());

        if (request.getCommentairesActifs() != null) {
            publication.setCommentairesActifs(request.getCommentairesActifs());
        }

        // Gère le changement de statut
        if (request.getStatut() != null &&
            request.getStatut() != publication.getStatut()) {

            publication.setStatut(request.getStatut());

            if (request.getStatut() == StatutPublication.PUBLIE &&
                publication.getDatePublication() == null) {
                publication.setDatePublication(LocalDateTime.now());
            }
        }

        // Remplace le fichier audio si un nouveau est fourni
        if (fichierAudio != null && !fichierAudio.isEmpty()) {
            // Supprime l'ancien fichier audio du serveur
            fichierService.supprimerFichier(publication.getCheminAudio());
            // Sauvegarde le nouveau
            publication.setCheminAudio(fichierService.sauvegarderAudio(fichierAudio));
        }

        // Remplace le PDF si un nouveau est fourni
        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            fichierService.supprimerFichier(publication.getCheminPdf());
            publication.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        Publication sauvegardee = publicationRepository.save(publication);
        return convertirEnResponse(sauvegardee);
    }

    /**
     * Suspend une publication (dépublie sans supprimer)
     * PUT /api/publications/{id}/suspendre
     */
    public PublicationDTO.Response suspendre(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.SUSPENDU);
        return convertirEnResponse(publicationRepository.save(pub));
    }

    /**
     * Republie une publication suspendue ou brouillon
     * PUT /api/publications/{id}/publier
     */
    public PublicationDTO.Response publier(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.PUBLIE);
        if (pub.getDatePublication() == null) {
            pub.setDatePublication(LocalDateTime.now());
        }
        return convertirEnResponse(publicationRepository.save(pub));
    }

    /**
     * Supprime définitivement une publication et ses fichiers
     * DELETE /api/publications/{id}
     */
    public void supprimer(Long id) {
        Publication pub = trouverParId(id);

        // Supprime les fichiers physiques du serveur
        fichierService.supprimerFichier(pub.getCheminAudio());
        fichierService.supprimerFichier(pub.getCheminPdf());

        // Supprime la publication (et ses commentaires par CASCADE)
        publicationRepository.delete(pub);
    }

    /**
     * Récupère les publications publiées avec filtres et pagination
     * GET /api/publications?type=ZOOM&page=0&size=10
     * Utilisé par le site client pour afficher le contenu
     */
    public Page<PublicationDTO.Response> listerPubliees(
        TypePublication type,
        String categorie,
        String jourZoom,
        String recherche,
        int page,
        int size
    ) {
        // PageRequest = paramètres de pagination
        // page = numéro de page (commence à 0)
        // size = nombre d'éléments par page
        Pageable pageable = PageRequest.of(page, size);

        Page<Publication> publications;

        if (recherche != null && !recherche.isBlank()) {
            // Recherche par titre
            publications = publicationRepository.rechercherParTitre(
                recherche, StatutPublication.PUBLIE, pageable
            );
        } else if (type != null && jourZoom != null) {
            // Filtre par type ET jour (pour les Zoom lundi/jeudi)
            publications = publicationRepository
                .findByTypeAndJourZoomAndStatutOrderByDateCreationDesc(
                    type, jourZoom, StatutPublication.PUBLIE, pageable
                );
        } else if (type != null) {
            // Filtre par type uniquement
            publications = publicationRepository
                .findByTypeAndStatutOrderByDateCreationDesc(
                    type, StatutPublication.PUBLIE, pageable
                );
        } else if (categorie != null) {
            // Filtre par catégorie
            publications = publicationRepository
                .findByCategorieAndStatutOrderByDateCreationDesc(
                    categorie, StatutPublication.PUBLIE, pageable
                );
        } else {
            // Toutes les publications publiées
            publications = publicationRepository
                .findByStatutOrderByDateCreationDesc(
                    StatutPublication.PUBLIE, pageable
                );
        }

        // Convertit chaque Publication en PublicationDTO.Response
        // .map() = applique une fonction à chaque élément de la Page
        return publications.map(this::convertirEnResponse);
    }

    /**
     * Récupère TOUTES les publications pour le dashboard admin
     * (publiées, brouillons et suspendues)
     * GET /api/admin/publications
     */
    public Page<PublicationDTO.Response> listerToutesPourAdmin(
        TypePublication type,
        StatutPublication statut,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Publication> publications;

        if (type != null && statut != null) {
            publications = publicationRepository
                .findByTypeAndStatutOrderByDateCreationDesc(type, statut, pageable);
        } else if (statut != null) {
            publications = publicationRepository
                .findByStatutOrderByDateCreationDesc(statut, pageable);
        } else {
            // Toutes sans filtre de statut
            publications = publicationRepository
                .findAll(pageable);
        }

        return publications.map(this::convertirEnResponse);
    }

    /**
     * Récupère les statistiques pour les cartes du dashboard
     * GET /api/admin/stats
     */
    public PublicationDTO.Stats getStats() {
        return new PublicationDTO.Stats(
            publicationRepository.countByType(TypePublication.ENSEIGNEMENT),
            publicationRepository.countByType(TypePublication.AUDIO),
            publicationRepository.countByType(TypePublication.ZOOM),
            publicationRepository.countByType(TypePublication.LIVRE),
            publicationRepository.countByType(TypePublication.VIDEO),
            publicationRepository.countByType(TypePublication.RADIO),
            commentaireRepository.countByStatut(
                com.doctrine.apotres.entity.Commentaire.StatutCommentaire.EN_ATTENTE
            ),
            0L // Prières — géré séparément
        );
    }

    /**
     * Récupère une publication par son ID
     * GET /api/publications/{id}
     */
    public PublicationDTO.Response trouverParIdDTO(Long id) {
        return convertirEnResponse(trouverParId(id));
    }

    // ---- Méthodes privées utilitaires ----

    /**
     * Cherche une publication par ID — lève une exception si non trouvée
     */
    private Publication trouverParId(Long id) {
        return publicationRepository.findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("Publication introuvable : " + id)
            );
    }

    /**
     * Convertit une entité Publication en DTO Response
     * Le DTO est ce qu'on envoie au frontend (pas l'entité directement)
     */
    private PublicationDTO.Response convertirEnResponse(Publication pub) {
        PublicationDTO.Response dto = new PublicationDTO.Response();
        dto.setId(pub.getId());
        dto.setType(pub.getType());
        dto.setTitre(pub.getTitre());
        dto.setContenu(pub.getContenu());
        dto.setCategorie(pub.getCategorie());
        dto.setSousCategorie(pub.getSousCategorie());
        dto.setAuteur(pub.getAuteur());
        dto.setStatut(pub.getStatut());
        dto.setCheminAudio(pub.getCheminAudio());
        dto.setCheminPdf(pub.getCheminPdf());
        dto.setLienVideo(pub.getLienVideo());
        dto.setJourZoom(pub.getJourZoom());
        dto.setDateSession(pub.getDateSession());
        dto.setTags(pub.getTags());
        dto.setCheminAudio2(pub.getCheminAudio2());
        dto.setCheminAudio3(pub.getCheminAudio3());
        dto.setResume(pub.getResume());
        dto.setPredicateur(pub.getPredicateur());
        dto.setImageUne(pub.getImageUne());
        dto.setCommentairesActifs(pub.getCommentairesActifs());
        dto.setDateCreation(pub.getDateCreation());
        dto.setDateModification(pub.getDateModification());
        dto.setDatePublication(pub.getDatePublication());

        // Compte les commentaires approuvés de CETTE publication spécifiquement
        long nbCommentaires = commentaireRepository
            .countByPublicationIdAndStatut(
                pub.getId(),
                com.doctrine.apotres.entity.Commentaire.StatutCommentaire.APPROUVE
            );
        dto.setNombreCommentaires((int) nbCommentaires);

        return dto;
    }
}