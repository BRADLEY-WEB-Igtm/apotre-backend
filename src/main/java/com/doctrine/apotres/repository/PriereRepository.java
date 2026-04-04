package com.doctrine.apotres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.doctrine.apotres.entity.Priere;
import com.doctrine.apotres.entity.Priere.StatutPriere;

/**
 * ============================================================
 * REPOSITORY PRIERE
 * Gère les demandes de prière soumises par les visiteurs
 * ============================================================
 */
@Repository
public interface PriereRepository extends JpaRepository<Priere, Long> {

    /**
     * Trouve les prières par statut avec pagination
     */
    Page<Priere> findByStatutOrderByDateCreationDesc(
        StatutPriere statut,
        Pageable pageable
    );

    /**
     * Compte les prières en attente
     * Utilisé pour le badge "13" dans la sidebar du dashboard
     */
    long countByStatut(StatutPriere statut);
}
