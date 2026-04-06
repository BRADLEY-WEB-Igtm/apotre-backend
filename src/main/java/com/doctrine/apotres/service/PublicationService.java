package com.doctrine.apotres.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.doctrine.apotres.dto.PublicationDTO;
import com.doctrine.apotres.entity.Publication;
import com.doctrine.apotres.entity.Publication.StatutPublication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.repository.CommentaireRepository;
import com.doctrine.apotres.repository.PublicationRepository;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

/**
 * SERVICE PUBLICATION — VERSION CLOUDINARY
 *
 * Simplifié : plus de gestion de fichiers ici.
 * Les URLs Cloudinary arrivent directement dans le Request.
 * On sauvegarde juste les URLs en base de données.
 */
@Service
public class PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private CommentaireRepository commentaireRepository;

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

    // ── SUSPENDRE / PUBLIER / SUPPRIMER ───────────────────────────
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

    public void supprimer(Long id) {
        Publication pub = trouverParId(id);
        /* Les fichiers sont sur Cloudinary — pas besoin de les supprimer du disque */
        publicationRepository.delete(pub);
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