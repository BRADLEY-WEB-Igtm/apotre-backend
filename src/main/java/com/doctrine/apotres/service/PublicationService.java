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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private FichierService fichierService;

    // ================================================================
    // CRÉER
    // ================================================================
    public PublicationDTO.Response creer(
        PublicationDTO.Request request,
        List<MultipartFile> fichiersAudio,
        MultipartFile fichierImage,
        MultipartFile fichierPdf
    ) throws IOException {

        Publication pub = new Publication();
        pub.setType(request.getType());
        pub.setTitre(request.getTitre());
        pub.setContenu(request.getContenu());
        pub.setCategorie(request.getCategorie());
        pub.setSousCategorie(request.getSousCategorie());
        pub.setTags(request.getTags());
        pub.setLienVideo(request.getLienVideo());
        pub.setJourZoom(request.getJourZoom());
        pub.setDateSession(request.getDateSession());
        pub.setCommentairesActifs(request.getCommentairesActifs() != null ? request.getCommentairesActifs() : true);
        if (request.getResume()      != null) pub.setResume(request.getResume());
        if (request.getPredicateur() != null) pub.setPredicateur(request.getPredicateur());

        String auteur = SecurityContextHolder.getContext().getAuthentication().getName();
        pub.setAuteur(auteur);

        StatutPublication statut = request.getStatut() != null ? request.getStatut() : StatutPublication.BROUILLON;
        pub.setStatut(statut);
        if (statut == StatutPublication.PUBLIE) pub.setDatePublication(LocalDateTime.now());

        // Sauvegarde les audios (1, 2 ou 3 parties)
        sauvegarderAudios(pub, fichiersAudio);

        // Image à la une — méthode dédiée (accepte jpg/png/webp)
        if (fichierImage != null && !fichierImage.isEmpty()) {
            pub.setImageUne(fichierService.sauvegarderImage(fichierImage));
        }

        // PDF
        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            pub.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        return convertirEnResponse(publicationRepository.save(pub));
    }

    // ================================================================
    // MODIFIER — signature corrigée pour accepter List<MultipartFile>
    // ================================================================
    public PublicationDTO.Response modifier(
        Long id,
        PublicationDTO.Request request,
        List<MultipartFile> fichiersAudio,
        MultipartFile fichierImage,
        MultipartFile fichierPdf
    ) throws IOException {

        Publication pub = publicationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Publication introuvable : " + id));

        pub.setTitre(request.getTitre());
        pub.setContenu(request.getContenu());
        pub.setCategorie(request.getCategorie());
        pub.setSousCategorie(request.getSousCategorie());
        pub.setTags(request.getTags());
        pub.setLienVideo(request.getLienVideo());
        pub.setJourZoom(request.getJourZoom());
        pub.setDateSession(request.getDateSession());
        if (request.getResume()      != null) pub.setResume(request.getResume());
        if (request.getPredicateur() != null) pub.setPredicateur(request.getPredicateur());
        if (request.getCommentairesActifs() != null) pub.setCommentairesActifs(request.getCommentairesActifs());

        if (request.getStatut() != null && request.getStatut() != pub.getStatut()) {
            pub.setStatut(request.getStatut());
            if (request.getStatut() == StatutPublication.PUBLIE && pub.getDatePublication() == null) {
                pub.setDatePublication(LocalDateTime.now());
            }
        }

        // Remplace les audios si de nouveaux sont fournis
        if (fichiersAudio != null && !fichiersAudio.isEmpty()) {
            fichierService.supprimerFichier(pub.getCheminAudio());
            fichierService.supprimerFichier(pub.getCheminAudio2());
            fichierService.supprimerFichier(pub.getCheminAudio3());
            pub.setCheminAudio(null);
            pub.setCheminAudio2(null);
            pub.setCheminAudio3(null);
            sauvegarderAudios(pub, fichiersAudio);
        }

        // Remplace l'image si une nouvelle est fournie
        if (fichierImage != null && !fichierImage.isEmpty()) {
            fichierService.supprimerFichier(pub.getImageUne());
            pub.setImageUne(fichierService.sauvegarderImage(fichierImage));
        }

        // Remplace le PDF si un nouveau est fourni
        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            fichierService.supprimerFichier(pub.getCheminPdf());
            pub.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        return convertirEnResponse(publicationRepository.save(pub));
    }

    // ================================================================
    // MÉTHODE PRIVÉE — sauvegarde 1, 2 ou 3 audios
    // ================================================================
    private void sauvegarderAudios(Publication pub, List<MultipartFile> fichiers) throws IOException {
        if (fichiers == null || fichiers.isEmpty()) return;

        List<MultipartFile> valides = fichiers.stream()
            .filter(f -> f != null && !f.isEmpty())
            .collect(Collectors.toList());

        if (valides.size() >= 1) pub.setCheminAudio(fichierService.sauvegarderAudio(valides.get(0)));
        if (valides.size() >= 2) pub.setCheminAudio2(fichierService.sauvegarderAudio(valides.get(1)));
        if (valides.size() >= 3) pub.setCheminAudio3(fichierService.sauvegarderAudio(valides.get(2)));
    }

    // ================================================================
    // SUSPENDRE / PUBLIER / SUPPRIMER
    // ================================================================
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
        fichierService.supprimerFichier(pub.getCheminAudio());
        fichierService.supprimerFichier(pub.getCheminAudio2());
        fichierService.supprimerFichier(pub.getCheminAudio3());
        fichierService.supprimerFichier(pub.getCheminPdf());
        fichierService.supprimerFichier(pub.getImageUne());
        publicationRepository.delete(pub);
    }

    // ================================================================
    // LISTER
    // ================================================================
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

    // ================================================================
    // UTILITAIRES PRIVÉS
    // ================================================================
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