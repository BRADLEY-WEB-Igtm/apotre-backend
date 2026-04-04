package com.doctrine.apotres.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ============================================================
 * CONFIGURATION WEB MVC
 *
 * Configure le serveur pour servir les fichiers uploadés
 * (audios MP3, PDFs) comme des ressources statiques accessibles
 * directement via une URL publique.
 *
 * Sans cette config, les fichiers dans le dossier "uploads/"
 * ne seraient pas accessibles depuis le navigateur.
 *
 * Exemple :
 *   Fichier sur le serveur : uploads/audios/zoom-abc123.mp3
 *   URL publique           : http://localhost:8080/uploads/audios/zoom-abc123.mp3
 * ============================================================
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String dossierUpload;
    // "uploads" — depuis application.properties

    /**
     * Configure les handlers de ressources statiques
     * addResourceHandlers = ajoute des mappings URL → dossier physique
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Mappe toutes les URLs /uploads/** vers le dossier uploads/ du serveur
        registry
            .addResourceHandler("/uploads/**")
            // addResourceHandler = pattern d'URL à intercepter

            .addResourceLocations("file:" + dossierUpload + "/");
            // addResourceLocations = dossier physique où chercher les fichiers
            // "file:" = préfixe pour indiquer un chemin absolu ou relatif sur le disque
            // (sans "file:" Spring chercherait dans le classpath = dans le JAR)
    }
}
