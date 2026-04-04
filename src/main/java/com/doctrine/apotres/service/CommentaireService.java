package com.doctrine.apotres.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.doctrine.apotres.dto.CommentaireDTO;
import com.doctrine.apotres.entity.Commentaire;
import com.doctrine.apotres.entity.Commentaire.StatutCommentaire;
import com.doctrine.apotres.entity.Publication;
import com.doctrine.apotres.repository.CommentaireRepository;
import com.doctrine.apotres.repository.PublicationRepository;

import java.time.LocalDateTime;

/**
 * ============================================================
 * SERVICE COMMENTAIRE — logique métier
 *
 * Gère :
 * - Soumission d'un commentaire par un visiteur
 * - Modération (approuver / rejeter) par l'admin
 * - Listage pour le dashboard et pour le site client
 * ============================================================
 */
@Service
public class CommentaireService {

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    /**
     * Soumet un nouveau commentaire depuis le site client
     * POST /api/commentaires ou POST /api/commentaires/{source}
     *
     * Correspond aux fetch() dans dimes.js, zoom.js, radio.js, etc.
     *
     * @param request Les données du formulaire de commentaire
     * @param source La page source ("zoom", "emissions-radios", etc.)
     * @param httpRequest La requête HTTP (pour récupérer l'IP du visiteur)
     */
    public CommentaireDTO.Response soumettre(
        CommentaireDTO.Request request,
        String source,
        HttpServletRequest httpRequest
    ) {
        Commentaire commentaire = new Commentaire();
        commentaire.setNom(request.getNom());
        commentaire.setEmail(request.getEmail());
        commentaire.setMessage(request.getMessage());
        commentaire.setSource(source);

        // Récupère l'adresse IP du visiteur pour détecter les spams
        commentaire.setAdresseIp(obtenirAdresseIp(httpRequest));

        // Associe à une publication si l'ID est fourni
        if (request.getPublicationId() != null) {
            Publication publication = publicationRepository
                .findById(request.getPublicationId())
                .orElse(null);
            // orElse(null) = si non trouvée, on laisse null (pas d'exception)
            commentaire.setPublication(publication);
        }

        // Statut initial = EN_ATTENTE (modération obligatoire)
        commentaire.setStatut(StatutCommentaire.EN_ATTENTE);

        Commentaire sauvegarde = commentaireRepository.save(commentaire);
        return convertirEnResponse(sauvegarde);
    }

    /**
     * Approuve un commentaire — le rend visible sur le site
     * PUT /api/admin/commentaires/{id}/approuver
     */
    public CommentaireDTO.Response approuver(Long id) {
        Commentaire commentaire = trouverParId(id);
        commentaire.setStatut(StatutCommentaire.APPROUVE);
        commentaire.setDateModeration(LocalDateTime.now());

        // Récupère le nom de l'admin connecté
        commentaire.setModerePar(
            SecurityContextHolder.getContext().getAuthentication().getName()
        );

        return convertirEnResponse(commentaireRepository.save(commentaire));
    }

    /**
     * Rejette un commentaire — non visible sur le site
     * PUT /api/admin/commentaires/{id}/rejeter
     */
    public CommentaireDTO.Response rejeter(Long id) {
        Commentaire commentaire = trouverParId(id);
        commentaire.setStatut(StatutCommentaire.REJETE);
        commentaire.setDateModeration(LocalDateTime.now());
        commentaire.setModerePar(
            SecurityContextHolder.getContext().getAuthentication().getName()
        );

        return convertirEnResponse(commentaireRepository.save(commentaire));
    }

    /**
     * Supprime définitivement un commentaire
     * DELETE /api/admin/commentaires/{id}
     */
    public void supprimer(Long id) {
        Commentaire commentaire = trouverParId(id);
        commentaireRepository.delete(commentaire);
    }

    /**
     * Liste les commentaires pour la modération (dashboard admin)
     * GET /api/admin/commentaires?statut=EN_ATTENTE&page=0
     */
    public Page<CommentaireDTO.Response> listerPourAdmin(
        StatutCommentaire statut,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Commentaire> commentaires;

        if (statut != null) {
            commentaires = commentaireRepository
                .findByStatutOrderByDateCreationDesc(statut, pageable);
        } else {
            // Tous les commentaires sans filtre de statut
            commentaires = commentaireRepository.findAll(pageable);
        }

        return commentaires.map(this::convertirEnResponse);
    }

    /**
     * Liste les commentaires approuvés d'une publication
     * GET /api/commentaires/approuves/{publicationId}
     * Utilisé par le site client pour afficher les commentaires sous un article
     */
    public Page<CommentaireDTO.Response> listerApprouvesPourPublication(
        Long publicationId,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return commentaireRepository
            .findByPublicationIdAndStatutOrderByDateCreationDesc(
                publicationId, StatutCommentaire.APPROUVE, pageable
            )
            .map(this::convertirEnResponse);
    }

    // ---- Méthodes privées utilitaires ----

    private Commentaire trouverParId(Long id) {
        return commentaireRepository.findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("Commentaire introuvable : " + id)
            );
    }

    /**
     * Récupère l'adresse IP réelle du visiteur
     * Prend en compte les proxies (X-Forwarded-For)
     */
    private String obtenirAdresseIp(HttpServletRequest request) {
        // X-Forwarded-For = header ajouté par les proxies avec l'IP réelle
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            // RemoteAddr = IP directe si pas de proxy
        }

        // X-Forwarded-For peut contenir plusieurs IPs séparées par ","
        // On prend la première (l'IP originale du client)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Convertit une entité Commentaire en DTO Response
     */
    private CommentaireDTO.Response convertirEnResponse(Commentaire com) {
        CommentaireDTO.Response dto = new CommentaireDTO.Response();
        dto.setId(com.getId());
        dto.setNom(com.getNom());
        dto.setEmail(com.getEmail());
        dto.setMessage(com.getMessage());
        dto.setSource(com.getSource());
        dto.setStatut(com.getStatut());
        dto.setDateCreation(com.getDateCreation());

        // Titre de la publication associée (si disponible)
        if (com.getPublication() != null) {
            dto.setArticleTitre(com.getPublication().getTitre());
            dto.setPublicationId(com.getPublication().getId());
        }

        return dto;
    }
}
