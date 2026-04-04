package com.doctrine.apotres.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * ============================================================
 * UTILITAIRE JWT — JSON Web Token
 *
 * Le JWT est un token sécurisé qui permet à l'admin de rester
 * connecté sans envoyer son mot de passe à chaque requête.
 *
 * Fonctionnement :
 * 1. L'admin se connecte → le backend génère un token JWT
 * 2. Le frontend stocke ce token (localStorage)
 * 3. À chaque requête, le frontend envoie ce token dans le header
 *    → Authorization: Bearer <token>
 * 4. Le backend vérifie le token → autorise ou refuse la requête
 *
 * @Component = composant Spring, injectable partout avec @Autowired
 * ============================================================
 */
@Component
public class JwtUtil {

    // Clé secrète lue depuis application.properties
    // @Value = injecte la valeur de la propriété dans ce champ
    @Value("${app.jwt.secret:DoctrineDesApotres@2026#SecureKey!JWT$Token*Spring&Boot%Admin}")
    private String jwtSecret;

    // Durée de validité du token (en ms) depuis application.properties
    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Génère un token JWT pour un utilisateur
     * Appelé juste après une connexion réussie
     *
     * @param username Nom d'utilisateur de l'admin
     * @return Le token JWT en chaîne de caractères
     */
    public String genererToken(String username) {

        return Jwts.builder()
            // Subject = qui est ce token (le nom d'utilisateur)
            .setSubject(username)

            // Date d'émission = maintenant
            .setIssuedAt(new Date())

            // Date d'expiration = maintenant + durée configurée
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))

            // Signature avec la clé secrète (algorithme HS256)
            .signWith(getCleSecrete(), SignatureAlgorithm.HS256)

            // Construit et retourne le token en String
            .compact();
    }

    /**
     * Extrait le nom d'utilisateur contenu dans un token
     * Utilisé pour identifier qui fait la requête
     *
     * @param token Le token JWT reçu dans le header Authorization
     * @return Le username contenu dans le token
     */
    public String extraireUsername(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getCleSecrete())  // Vérifie avec la même clé secrète
            .build()
            .parseClaimsJws(token)           // Parse et vérifie le token
            .getBody()
            .getSubject();                   // Retourne le subject (username)
    }

    /**
     * Vérifie si un token est valide
     * Un token est valide si :
     * - La signature est correcte (pas modifié)
     * - Il n'est pas expiré
     *
     * @param token Le token à vérifier
     * @return true si valide, false sinon
     */
    public boolean validerToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getCleSecrete())
                .build()
                .parseClaimsJws(token);
            // Si pas d'exception → le token est valide
            return true;

        } catch (ExpiredJwtException e) {
            // Le token a expiré (après 24h)
            System.out.println("Token JWT expiré : " + e.getMessage());
        } catch (MalformedJwtException e) {
            // Le token est malformé (modifié manuellement)
            System.out.println("Token JWT invalide : " + e.getMessage());
        } catch (SignatureException e) {
            // La signature ne correspond pas (mauvaise clé secrète)
            System.out.println("Signature JWT invalide : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Le token est vide ou null
            System.out.println("Token JWT vide : " + e.getMessage());
        }
        return false;
    }

    /**
     * Convertit la clé secrète en objet Key utilisable par JJWT
     * Keys.hmacShaKeyFor() convertit les bytes en clé HMAC-SHA
     */
    private Key getCleSecrete() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
