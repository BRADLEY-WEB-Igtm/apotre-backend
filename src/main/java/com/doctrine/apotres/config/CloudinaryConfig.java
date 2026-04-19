package com.doctrine.apotres.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================
 * CONFIGURATION CLOUDINARY
 *
 * Déclare le bean Cloudinary injectable partout dans l'application.
 * Lit la variable d'environnement CLOUDINARY_URL définie sur Render.
 *
 * Format CLOUDINARY_URL :
 * cloudinary://API_KEY:API_SECRET@CLOUD_NAME
 * ============================================================
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url}")
    /* Lit la valeur de cloudinary.url depuis application-prod.properties */
    /* application-prod.properties lit lui-même ${CLOUDINARY_URL} depuis Render */
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        /* Crée et configure le bean Cloudinary avec l'URL complète */
        /* Cloudinary parse automatiquement l'URL : clé, secret, cloud name */
        return new Cloudinary(cloudinaryUrl);
    }
}
