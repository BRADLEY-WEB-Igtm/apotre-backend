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

/**
 * ============================================================
 * SERVICE PUBLICATION — VERSION CORRIGÉE
 *
 * CORRECTIONS :
 * 1. creer() et modifier() acceptent List<MultipartFile> pour audios
 *    → nombre illimité de parties audio
 * 2. sauvegarderImage() utilisé pour les images au lieu de sauvegarderAudio()
 *    → évite le rejet des fichiers .jpg/.png
 * 3. convertirEnResponse() construit la liste complète cheminsAudio
 *    en combinant les nouveaux chemins + anciens champs backward compat
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


    /**
     * Crée une nouvelle publication avec audios illimités
     *
     * @param request       Données du formulaire (JSON)
     * @param fichierAudios Liste de fichiers audio (1 à N)
     * @param fichierImage  Image à la une (optionnel)
     * @param fichierPdf    Fichier PDF (optionnel)
     */
    public PublicationDTO.Response creer(
        PublicationDTO.Request request,
        List<MultipartFile> fichierAudios,
        MultipartFile fichierImage,
        MultipartFile fichierPdf
    ) throws IOException {

        Publication publication = new Publication();
        publication.setType(request.getType());
        publication.setTitre(request.getTitre());
        publication.setContenu(request.getContenu());
        publication.setCategorie(request.getCategorie());
        publication.setSousCategorie(request.getSousCategorie());
        publication.setTags(request.getTags());
        publication.setLienVideo(request.getLienVideo());
        publication.setJourZoom(request.getJourZoom());
        publication.setDateSession(request.getDateSession());
        publication.setCommentairesActifs(
            request.getCommentairesActifs() != null ? request.getCommentairesActifs() : true
        );
        if (request.getResume()     != null) publication.setResume(request.getResume());
        if (request.getPredicateur()!= null) publication.setPredicateur(request.getPredicateur());

        // Auteur = admin connecté
        String auteur = SecurityContextHolder.getContext().getAuthentication().getName();
        publication.setAuteur(auteur);

        // Statut
        StatutPublication statut = request.getStatut() != null
            ? request.getStatut()
            : StatutPublication.BROUILLON;
        publication.setStatut(statut);

        if (statut == StatutPublication.PUBLIE) {
            publication.setDatePublication(LocalDateTime.now());
        }

        // ✅ CORRECTION : sauvegarde TOUS les fichiers audio dans la liste
        if (fichierAudios != null && !fichierAudios.isEmpty()) {
            List<String> chemins = new ArrayList<>();

            for (MultipartFile fichier : fichierAudios) {
                // Parcourt chaque fichier audio envoyé par le frontend
                if (fichier != null && !fichier.isEmpty()) {
                    // Sauvegarde le fichier et récupère son chemin
                    String chemin = fichierService.sauvegarderAudio(fichier);
                    chemins.add(chemin);
                    // chemins = ["uploads/audios/p1.mp3", "uploads/audios/p2.mp3", ...]
                }
            }

            publication.setCheminsAudio(chemins);
            // Stocke la liste dans la table publication_audios

            // ── Backward compat : remplit aussi les anciens champs ──
            // Permet aux pages qui lisent encore cheminAudio de fonctionner
            if (chemins.size() >= 1) publication.setCheminAudio(chemins.get(0));
            if (chemins.size() >= 2) publication.setCheminAudio2(chemins.get(1));
            if (chemins.size() >= 3) publication.setCheminAudio3(chemins.get(2));
            // Les parties 4, 5, 6+ sont dans la liste mais pas dans les anciens champs
        }

        // ✅ CORRECTION : utilise sauvegarderImage() pour les images
        // Avant : sauvegarderAudio() était utilisé → rejetait .jpg/.png
        if (fichierImage != null && !fichierImage.isEmpty()) {
            publication.setImageUne(fichierService.sauvegarderImage(fichierImage));
        }

        // PDF
        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            publication.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        Publication sauvegardee = publicationRepository.save(publication);
        return convertirEnResponse(sauvegardee);
    }


    /**
     * Modifie une publication existante
     */
    public PublicationDTO.Response modifier(
        Long id,
        PublicationDTO.Request request,
        List<MultipartFile> fichierAudios,
        MultipartFile fichierImage,
        MultipartFile fichierPdf
    ) throws IOException {

        Publication publication = publicationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Publication introuvable : " + id));

        publication.setTitre(request.getTitre());
        publication.setContenu(request.getContenu());
        publication.setCategorie(request.getCategorie());
        publication.setSousCategorie(request.getSousCategorie());
        publication.setTags(request.getTags());
        publication.setLienVideo(request.getLienVideo());
        publication.setJourZoom(request.getJourZoom());
        publication.setDateSession(request.getDateSession());
        if (request.getResume()      != null) publication.setResume(request.getResume());
        if (request.getPredicateur() != null) publication.setPredicateur(request.getPredicateur());
        if (request.getCommentairesActifs() != null)
            publication.setCommentairesActifs(request.getCommentairesActifs());

        if (request.getStatut() != null && request.getStatut() != publication.getStatut()) {
            publication.setStatut(request.getStatut());
            if (request.getStatut() == StatutPublication.PUBLIE
                && publication.getDatePublication() == null) {
                publication.setDatePublication(LocalDateTime.now());
            }
        }

        // ✅ Remplace les audios si de nouveaux fichiers sont envoyés
        if (fichierAudios != null && !fichierAudios.isEmpty()) {
            // Supprime les anciens fichiers
            for (String oldChemin : publication.getCheminsAudio()) {
                fichierService.supprimerFichier(oldChemin);
            }

            List<String> chemins = new ArrayList<>();
            for (MultipartFile fichier : fichierAudios) {
                if (fichier != null && !fichier.isEmpty()) {
                    chemins.add(fichierService.sauvegarderAudio(fichier));
                }
            }
            publication.setCheminsAudio(chemins);

            // Backward compat
            publication.setCheminAudio(chemins.size() >= 1 ? chemins.get(0) : null);
            publication.setCheminAudio2(chemins.size() >= 2 ? chemins.get(1) : null);
            publication.setCheminAudio3(chemins.size() >= 3 ? chemins.get(2) : null);
        }

        // ✅ Image avec la bonne méthode
        if (fichierImage != null && !fichierImage.isEmpty()) {
            if (publication.getImageUne() != null)
                fichierService.supprimerFichier(publication.getImageUne());
            publication.setImageUne(fichierService.sauvegarderImage(fichierImage));
        }

        if (fichierPdf != null && !fichierPdf.isEmpty()) {
            fichierService.supprimerFichier(publication.getCheminPdf());
            publication.setCheminPdf(fichierService.sauvegarderPdf(fichierPdf));
        }

        return convertirEnResponse(publicationRepository.save(publication));
    }


    public PublicationDTO.Response suspendre(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.SUSPENDU);
        return convertirEnResponse(publicationRepository.save(pub));
    }

    public PublicationDTO.Response publier(Long id) {
        Publication pub = trouverParId(id);
        pub.setStatut(StatutPublication.PUBLIE);
        if (pub.getDatePublication() == null)
            pub.setDatePublication(LocalDateTime.now());
        return convertirEnResponse(publicationRepository.save(pub));
    }

    public void supprimer(Long id) {
        Publication pub = trouverParId(id);
        // Supprime tous les fichiers audio
        for (String chemin : pub.getCheminsAudio()) {
            fichierService.supprimerFichier(chemin);
        }
        fichierService.supprimerFichier(pub.getCheminAudio());
        fichierService.supprimerFichier(pub.getCheminPdf());
        fichierService.supprimerFichier(pub.getImageUne());
        publicationRepository.delete(pub);
    }

    public Page<PublicationDTO.Response> listerPubliees(
        TypePublication type, String categorie, String jourZoom,
        String recherche, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Publication> publications;

        if (recherche != null && !recherche.isBlank()) {
            publications = publicationRepository.rechercherParTitre(
                recherche, StatutPublication.PUBLIE, pageable);
        } else if (type != null && jourZoom != null) {
            publications = publicationRepository
                .findByTypeAndJourZoomAndStatutOrderByDateCreationDesc(
                    type, jourZoom, StatutPublication.PUBLIE, pageable);
        } else if (type != null) {
            publications = publicationRepository
                .findByTypeAndStatutOrderByDateCreationDesc(
                    type, StatutPublication.PUBLIE, pageable);
        } else if (categorie != null) {
            publications = publicationRepository
                .findByCategorieAndStatutOrderByDateCreationDesc(
                    categorie, StatutPublication.PUBLIE, pageable);
        } else {
            publications = publicationRepository
                .findByStatutOrderByDateCreationDesc(StatutPublication.PUBLIE, pageable);
        }

        return publications.map(this::convertirEnResponse);
    }

    public Page<PublicationDTO.Response> listerToutesPourAdmin(
        TypePublication type, StatutPublication statut, int page, int size
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
            commentaireRepository.countByStatut(
                com.doctrine.apotres.entity.Commentaire.StatutCommentaire.EN_ATTENTE),
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

    /**
     * Convertit Publication → DTO
     *
     * ✅ CORRECTION cheminsAudio :
     * Construit la liste finale en fusionnant :
     *   1. La nouvelle liste dynamique (cheminsAudio)
     *   2. Les anciens champs fixes (backward compat pour les vieilles publications)
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

        // Anciens champs (backward compat)
        dto.setCheminAudio(pub.getCheminAudio());
        dto.setCheminAudio2(pub.getCheminAudio2());
        dto.setCheminAudio3(pub.getCheminAudio3());

        // ✅ Construit la liste complète des audios
        List<String> tousLesAudios = new ArrayList<>();

        if (pub.getCheminsAudio() != null && !pub.getCheminsAudio().isEmpty()) {
            // Nouvelle publication → utilise la liste dynamique
            tousLesAudios.addAll(pub.getCheminsAudio());

        } else {
            // Ancienne publication → construit la liste depuis les anciens champs
            // Permet aux publications créées avant la mise à jour de fonctionner
            if (pub.getCheminAudio()  != null) tousLesAudios.add(pub.getCheminAudio());
            if (pub.getCheminAudio2() != null) tousLesAudios.add(pub.getCheminAudio2());
            if (pub.getCheminAudio3() != null) tousLesAudios.add(pub.getCheminAudio3());
        }

        dto.setCheminsAudio(tousLesAudios);
        // Le frontend reçoit ["uploads/audios/p1.mp3","uploads/audios/p2.mp3", ...]

        // Commentaires approuvés
        long nbCommentaires = commentaireRepository.countByPublicationIdAndStatut(
            pub.getId(),
            com.doctrine.apotres.entity.Commentaire.StatutCommentaire.APPROUVE
        );
        dto.setNombreCommentaires((int) nbCommentaires);

        return dto;
    }
}