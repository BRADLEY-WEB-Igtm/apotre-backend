package com.doctrine.apotres.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.doctrine.apotres.dto.PublicationDTO;
import com.doctrine.apotres.entity.Publication;
import com.doctrine.apotres.entity.Publication.StatutPublication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.repository.CommentaireRepository;
import com.doctrine.apotres.repository.PublicationRepository;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SERVICE PUBLICATION — VERSION CLOUDINARY
 *
 * Gère la création, modification, suppression des publications.
 * Lors de la suppression, les fichiers sont aussi supprimés de Cloudinary
 * pour libérer l'espace de stockage.
 */
@Service
public class PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private Cloudinary cloudinary;
    /* Bean Cloudinary injecté — configuré via CLOUDINARY_URL dans les variables d'environnement */

    // ── CRÉER ──────────────────────────────────────────────────────
    public PublicationDTO.Response creer(PublicationDTO.Request request) {

        Publication pub = new Publication();
        remplirDepuisRequest(pub, request);

        String auteur = SecurityContextHolder.getContext().getAuthentication().getName();
        pub.setAuteur(auteur);

        StatutPublication statut = request.getStatut() != null
            ? request.getStatut() : StatutPublication.BROUILLON;
        pub.setStatut(statut);
        if (statut == StatutPublication.PUBLIE) {
            pub.setDatePublication(LocalDateTime.now());
        }

        /* Sauvegarde les URLs Cloudinary directement */
        pub.setCheminAudio(request.getCheminAudio());
        pub.setCheminAudio2(request.getCheminAudio2());
        pub.setCheminAudio3(request.getCheminAudio3());
        pub.setImageUne(request.getImageUne());
        pub.setCheminPdf(request.getCheminPdf());

        return convertirEnResponse(publicationRepository.save(pub));
    }

    // ── MODIFIER ───────────────────────────────────────────────────
    public PublicationDTO.Response modifier(Long id, PublicationDTO.Request request) {

        Publication pub = publicationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Publication introuvable : " + id));

        remplirDepuisRequest(pub, request);

        if (request.getStatut() != null && request.getStatut() != pub.getStatut()) {
            pub.setStatut(request.getStatut());
            if (request.getStatut() == StatutPublication.PUBLIE && pub.getDatePublication() == null) {
                pub.setDatePublication(LocalDateTime.now());
            }
        }

        /* Met à jour les URLs si de nouvelles sont fournies */
        if (request.getCheminAudio()  != null) pub.setCheminAudio(request.getCheminAudio());
        if (request.getCheminAudio2() != null) pub.setCheminAudio2(request.getCheminAudio2());
        if (request.getCheminAudio3() != null) pub.setCheminAudio3(request.getCheminAudio3());
        if (request.getImageUne()     != null) pub.setImageUne(request.getImageUne());
        if (request.getCheminPdf()    != null) pub.setCheminPdf(request.getCheminPdf());

        return convertirEnResponse(publicationRepository.save(pub));
    }

    // ── Remplit les champs communs depuis le Request ───────────────
    private void remplirDepuisRequest(Publication pub, PublicationDTO.Request req) {
        pub.setType(req.getType());
        pub.setTitre(req.getTitre());
        pub.setContenu(req.getContenu());
        pub.setCategorie(req.getCategorie());
        pub.setSousCategorie(req.getSousCategorie());
        pub.setTags(req.getTags());
        pub.setLienVideo(req.getLienVideo());
        pub.setJourZoom(req.getJourZoom());
        pub.setDateSession(req.getDateSession());
        pub.setCommentairesActifs(req.getCommentairesActifs() != null ? req.getCommentairesActifs() : true);
        if (req.getResume()      != null) pub.setResume(req.getResume());
        if (req.getPredicateur() != null) pub.setPredicateur(req.getPredicateur());
    }

    // ── SUSPENDRE / PUBLIER ────────────────────────────────────────
    public PublicationDTO.Response suspendre(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.SUSPENDU);
        return convertirEnResponse(publicationRepository.save(pub));
    }

    public PublicationDTO.Response publier(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.PUBLIE);
        if (pub.getDatePublication() == null) pub.setDatePublication(LocalDateTime.now());
        return convertirEnResponse(publicationRepository.save(pub));
    }

    // ── SUPPRIMER — avec nettoyage Cloudinary ─────────────────────
    public void supprimer(Long id) {

        Publication pub = trouverParId(id);

        /*
         * SUPPRESSION CLOUDINARY
         *
         * Analogie : c'est comme vider la corbeille après avoir supprimé un fichier.
         * Sans ça, le fichier disparaît de la liste mais reste sur le disque dur.
         *
         * On supprime chaque fichier lié à cette publication :
         * - Audios (cheminAudio, cheminAudio2, cheminAudio3)
         * - Image à la une (imageUne)
         * - PDF (cheminPdf)
         */
        supprimerFichierCloudinary(pub.getCheminAudio(),  "video");
        /* "video" = type Cloudinary pour les fichiers audio aussi */

        supprimerFichierCloudinary(pub.getCheminAudio2(), "video");
        supprimerFichierCloudinary(pub.getCheminAudio3(), "video");
        supprimerFichierCloudinary(pub.getImageUne(),     "image");
        supprimerFichierCloudinary(pub.getCheminPdf(),    "raw");
        /* "raw" = type Cloudinary pour les fichiers PDF */

        /* Supprime l'entrée en base de données après le nettoyage Cloudinary */
        publicationRepository.delete(pub);
    }

    // ── SUPPRESSION D'UN FICHIER CLOUDINARY ───────────────────────
    /**
     * Supprime un fichier sur Cloudinary à partir de son URL.
     *
     * Exemple d'URL Cloudinary :
     * https://res.cloudinary.com/dqmy8sqmg/video/upload/v1776504400/doctrine-apotres/audio123.mp3
     *
     * Pour supprimer, Cloudinary a besoin du "public_id" :
     * → doctrine-apotres/audio123
     * (sans l'extension pour image et video, avec extension pour raw/PDF)
     *
     * @param url          L'URL complète du fichier sur Cloudinary
     * @param resourceType "image", "video", ou "raw" selon le type de fichier
     */
    private void supprimerFichierCloudinary(String url, String resourceType) {

        /* Ne fait rien si l'URL est nulle ou vide */
        if (url == null || url.isBlank()) return;

        /* Vérifie que c'est bien une URL Cloudinary */
        if (!url.contains("cloudinary.com")) return;

        try {
            /* Extrait le public_id depuis l'URL */
            String publicId = extrairePublicId(url, resourceType);

            if (publicId == null || publicId.isBlank()) {
                System.err.println("Impossible d'extraire le public_id depuis : " + url);
                return;
            }

            /* Appelle l'API Cloudinary pour supprimer le fichier */
            Map result = cloudinary.uploader().destroy(
                publicId,
                Map.of("resource_type", resourceType)
                /* resource_type = indique à Cloudinary où chercher le fichier */
            );

            /* Log du résultat pour le débogage */
            String statut = (String) result.get("result");
            if ("ok".equals(statut)) {
                System.out.println("✅ Cloudinary — fichier supprimé : " + publicId);
            } else {
                System.err.println("⚠️ Cloudinary — suppression échouée pour : " + publicId + " → " + statut);
            }

        } catch (Exception e) {
            /*
             * On logge l'erreur mais on ne bloque pas la suppression en BD.
             * Si Cloudinary est indisponible, la publication est quand même supprimée.
             */
            System.err.println("Erreur suppression Cloudinary pour " + url + " : " + e.getMessage());
        }
    }

    // ── EXTRACTION DU PUBLIC_ID DEPUIS UNE URL CLOUDINARY ─────────
    /**
     * Extrait le public_id Cloudinary depuis une URL complète.
     *
     * Format URL : https://res.cloudinary.com/{cloud}/resource_type/upload/v{version}/{public_id}.{ext}
     *
     * Exemples :
     * - https://res.cloudinary.com/dqmy8sqmg/video/upload/v123/doctrine-apotres/audio.mp3
     *   → public_id = "doctrine-apotres/audio"     (sans extension pour video)
     *
     * - https://res.cloudinary.com/dqmy8sqmg/image/upload/v123/doctrine-apotres/photo.jpg
     *   → public_id = "doctrine-apotres/photo"     (sans extension pour image)
     *
     * - https://res.cloudinary.com/dqmy8sqmg/raw/upload/v123/doctrine-apotres/doc.pdf
     *   → public_id = "doctrine-apotres/doc.pdf"   (AVEC extension pour raw/PDF)
     */
    private String extrairePublicId(String url, String resourceType) {

        try {
            /* Sépare l'URL au niveau de "/upload/" */
            String[] parties = url.split("/upload/");
            if (parties.length < 2) return null;

            /* La partie droite contient : v{version}/{dossier}/{fichier}.{ext} */
            String parteDroite = parties[1];

            /* Supprime le préfixe de version s'il existe (ex: "v1776504400/") */
            if (parteDroite.matches("v\\d+/.*")) {
                parteDroite = parteDroite.replaceFirst("v\\d+/", "");
                /* replaceFirst avec regex = supprime "v" suivi de chiffres suivi de "/" */
            }

            /* Pour les fichiers raw (PDF), on garde l'extension */
            if ("raw".equals(resourceType)) {
                return parteDroite;
                /* Ex: "doctrine-apotres/document.pdf" */
            }

            /* Pour image et video, on supprime l'extension */
            int dernierPoint = parteDroite.lastIndexOf('.');
            if (dernierPoint > 0) {
                return parteDroite.substring(0, dernierPoint);
                /* Ex: "doctrine-apotres/audio123" sans ".mp3" */
            }

            return parteDroite;

        } catch (Exception e) {
            System.err.println("Erreur extraction public_id depuis " + url + " : " + e.getMessage());
            return null;
        }
    }

    // ── LISTER ────────────────────────────────────────────────────
    public Page<PublicationDTO.Response> listerPubliees(
        TypePublication type, String categorie, String jourZoom,
        String recherche, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Publication> publications;

        if (recherche != null && !recherche.isBlank()) {
            publications = publicationRepository.rechercherParTitre(recherche, StatutPublication.PUBLIE, pageable);
        } else if (type != null && jourZoom != null) {
            publications = publicationRepository.findByTypeAndJourZoomAndStatutOrderByDateCreationDesc(type, jourZoom, StatutPublication.PUBLIE, pageable);
        } else if (type != null) {
            publications = publicationRepository.findByTypeAndStatutOrderByDateCreationDesc(type, StatutPublication.PUBLIE, pageable);
        } else if (categorie != null) {
            publications = publicationRepository.findByCategorieAndStatutOrderByDateCreationDesc(categorie, StatutPublication.PUBLIE, pageable);
        } else {
            publications = publicationRepository.findByStatutOrderByDateCreationDesc(StatutPublication.PUBLIE, pageable);
        }
        return publications.map(this::convertirEnResponse);
    }

    public Page<PublicationDTO.Response> listerToutesPourAdmin(
        TypePublication type, StatutPublication statut, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Publication> publications;
        if (type != null && statut != null) {
            publications = publicationRepository.findByTypeAndStatutOrderByDateCreationDesc(type, statut, pageable);
        } else if (statut != null) {
            publications = publicationRepository.findByStatutOrderByDateCreationDesc(statut, pageable);
        } else {
            publications = publicationRepository.findAll(pageable);
        }
        return publications.map(this::convertirEnResponse);
    }

    public PublicationDTO.Stats getStats() {
        return new PublicationDTO.Stats(
            publicationRepository.countByType(TypePublication.ENSEIGNEMENT),
            publicationRepository.countByType(TypePublication.AUDIO),
            publicationRepository.countByType(TypePublication.ZOOM),
            publicationRepository.countByType(TypePublication.LIVRE),
            publicationRepository.countByType(TypePublication.VIDEO),
            publicationRepository.countByType(TypePublication.RADIO),
            commentaireRepository.countByStatut(com.doctrine.apotres.entity.Commentaire.StatutCommentaire.EN_ATTENTE),
            0L
        );
    }

    public PublicationDTO.Response trouverParIdDTO(Long id) {
        return convertirEnResponse(trouverParId(id));
    }

    private Publication trouverParId(Long id) {
        return publicationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Publication introuvable : " + id));
    }

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
        dto.setCheminAudio2(pub.getCheminAudio2());
        dto.setCheminAudio3(pub.getCheminAudio3());
        dto.setCheminPdf(pub.getCheminPdf());
        dto.setImageUne(pub.getImageUne());
        dto.setLienVideo(pub.getLienVideo());
        dto.setJourZoom(pub.getJourZoom());
        dto.setDateSession(pub.getDateSession());
        dto.setTags(pub.getTags());
        dto.setResume(pub.getResume());
        dto.setPredicateur(pub.getPredicateur());
        dto.setCommentairesActifs(pub.getCommentairesActifs());
        dto.setDateCreation(pub.getDateCreation());
        dto.setDateModification(pub.getDateModification());
        dto.setDatePublication(pub.getDatePublication());
        long nb = commentaireRepository.countByPublicationIdAndStatut(
            pub.getId(),
            com.doctrine.apotres.entity.Commentaire.StatutCommentaire.APPROUVE
        );
        dto.setNombreCommentaires((int) nb);
        return dto;
    }
}
