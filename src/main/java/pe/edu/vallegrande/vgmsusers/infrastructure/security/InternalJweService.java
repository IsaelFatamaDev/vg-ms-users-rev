package pe.edu.vallegrande.vgmsusers.infrastructure.security;

/*
// JWE FUNCTIONALITY COMMENTED OUT - NO LONGER NEEDED FOR INTERNAL APIS

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

/**
 * Servicio para manejo de JWE (JSON Web Encryption)
 * para comunicación segura entre microservicios
 *
 * Características:
 * - Cifrado completo del payload
 * - Configuración segura con variables de entorno
 * - Tokens con expiración
 * - Validación de issuer y audience
 *
 * COMENTADO: Ya no se usa JWE para APIs internas
 */
/*
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalJweService {

     @Value("${internal.security.jwe.secret}")
     private String jweSecret;

     @Value("${internal.security.jwe.expiration}")
     private long expiration;

     @Value("${internal.security.jwe.issuer}")
     private String issuer;

     @Value("${internal.security.jwe.audience}")
     private String audience;

     /**
      * Genera un token JWE para comunicación interna
      *
      * @param subject          Identificador del microservicio emisor
      * @param additionalClaims Claims adicionales si son necesarios
      * @return Token JWE cifrado
      */
/*
     public String generateInternalToken(String subject, String... additionalClaims) {
          try {
               log.debug("🔐 Generando token JWE interno para subject: {}", subject);

               // Crear claims del JWT
               JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                         .subject(subject)
                         .issuer(issuer)
                         .audience(audience)
                         .expirationTime(new Date(Instant.now().toEpochMilli() + expiration * 1000))
                         .issueTime(new Date())
                         .jwtID(java.util.UUID.randomUUID().toString());

               // Agregar claims adicionales si existen
               if (additionalClaims.length > 0) {
                    claimsBuilder.claim("scope", String.join(",", additionalClaims));
               }

               JWTClaimsSet claimsSet = claimsBuilder.build();

               // Crear header JWE
               JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                         .contentType("JWT")
                         .build();

               // Crear JWE cifrado
               EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

               // Cifrar con secret
               DirectEncrypter encrypter = new DirectEncrypter(deriveKey());
               encryptedJWT.encrypt(encrypter);

               String token = encryptedJWT.serialize();
               log.info("✅ Token JWE interno generado exitosamente para: {}", subject);

               return token;

          } catch (Exception e) {
               log.error("❌ Error generando token JWE interno: {}", e.getMessage());
               throw new RuntimeException("Error generando token interno", e);
          }
     }

     /**
      * Valida y extrae claims de un token JWE interno
      *
      * @param token Token JWE a validar
      * @return Claims del token si es válido
      * @throws RuntimeException si el token es inválido
      */
/*
     public JWTClaimsSet validateInternalToken(String token) {
          try {
               log.debug("🔍 Validando token JWE interno");

               // Parsear token JWE
               EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);

               // Descifrar con secret
               DirectDecrypter decrypter = new DirectDecrypter(deriveKey());
               encryptedJWT.decrypt(decrypter);

               // Extraer claims
               JWTClaimsSet claimsSet = encryptedJWT.getJWTClaimsSet();

               // Validar issuer
               if (!issuer.equals(claimsSet.getIssuer())) {
                    throw new RuntimeException("Issuer inválido en token interno");
               }

               // Validar audience
               if (!claimsSet.getAudience().contains(audience)) {
                    throw new RuntimeException("Audience inválido en token interno");
               }

               // Validar expiración
               if (claimsSet.getExpirationTime().before(new Date())) {
                    throw new RuntimeException("Token interno expirado");
               }

               log.info("✅ Token JWE interno válido para subject: {}", claimsSet.getSubject());
               return claimsSet;

          } catch (Exception e) {
               log.error("❌ Error validando token JWE interno: {}", e.getMessage());
               throw new RuntimeException("Token interno inválido", e);
          }
     }

     /**
      * Genera una clave de 256 bits a partir del secret configurado
      * Usa SHA-256 para asegurar que la clave tenga el tamaño correcto
      */
/*
     private SecretKeySpec deriveKey() {
          try {
               MessageDigest digest = MessageDigest.getInstance("SHA-256");
               byte[] keyBytes = digest.digest(jweSecret.getBytes(StandardCharsets.UTF_8));
               return new SecretKeySpec(keyBytes, "AES");
          } catch (Exception e) {
               throw new RuntimeException("Error derivando clave JWE", e);
          }
     }

     /**
      * Genera token para el microservicio actual (MS-users)
      *
      * @return Token JWE para comunicación con otros microservicios
      */
/*
     public String generateMsUsersToken() {
          return generateInternalToken("ms-users", "internal-communication");
     }

     /**
      * Valida si un token es válido para comunicación interna
      *
      * @param token Token a validar
      * @return true si es válido, false si no
      */
/*
     public boolean isValidInternalToken(String token) {
          try {
               validateInternalToken(token);
               return true;
          } catch (Exception e) {
               return false;
          }
     }
}
*/

// Empty class to avoid compilation errors during transition
public class InternalJweService {
     // This service has been disabled - JWE no longer needed for internal APIs
}