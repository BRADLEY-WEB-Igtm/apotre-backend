package com.doctrine.apotres.repository;

import com.doctrine.apotres.entity.Photo;
import com.doctrine.apotres.entity.Photo.StatutPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    Page<Photo> findByStatutOrderByDateCreationDesc(StatutPhoto statut, Pageable pageable);

    Page<Photo> findByCategorieAndStatutOrderByDateCreationDesc(String categorie, StatutPhoto statut, Pageable pageable);

    Page<Photo> findAllByOrderByDateCreationDesc(Pageable pageable);

    long countByStatut(StatutPhoto statut);
}
