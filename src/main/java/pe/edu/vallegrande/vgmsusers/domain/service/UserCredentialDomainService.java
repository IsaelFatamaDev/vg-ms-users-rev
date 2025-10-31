package pe.edu.vallegrande.vgmsusers.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsusers.domain.model.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Domain Service - Servicio de dominio para credenciales
 * Contiene lógica compleja del dominio relacionada con credenciales
 * que no pertenece a ninguna entidad específica
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserCredentialDomainService {

     private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
     private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
     private static final String DIGITS = "0123456789";
     private static final String SPECIAL_CHARS = "@#$%&*";
     private static final String ALL_CHARS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL_CHARS;
     private static final int PASSWORD_LENGTH = 12;

     private final SecureRandom secureRandom = new SecureRandom();

     /**
      * Generar credenciales temporales para un usuario
      * Lógica de dominio para crear credenciales seguras
      */
     public UserCredential generateTemporaryCredential(User user) {
          log.info("Generando credenciales temporales para usuario: {}", user.getUserCode());

          String username = generateUsername(user);
          String temporaryPassword = generateSecurePassword();
          LocalDateTime expirationDate = calculateExpirationDate(user);

          UserCredential credential = UserCredential.builder()
                    .username(username)
                    .temporaryPassword(temporaryPassword)
                    .expiresAt(expirationDate)
                    .mustChangePassword(true)
                    .generatedAt(LocalDateTime.now())
                    .userId(user.getId())
                    .build();

          log.info("Credencial generada - Username: {}, Expira: {}", username, expirationDate);
          return credential;
     }

     /**
      * Generar username basado en reglas de dominio: nombre.apellido@jass.gob.pe
      */
     private String generateUsername(User user) {
          if (user.getPersonalInfo() == null) {
               throw new IllegalArgumentException("Usuario debe tener información personal para generar username");
          }

          String firstName = user.getPersonalInfo().getFirstName();
          String lastName = user.getPersonalInfo().getLastName();

          if (firstName == null || lastName == null) {
               throw new IllegalArgumentException("Usuario debe tener nombre y apellido para generar username");
          }

          // Normalizar nombres para username
          String firstNamePart = normalizeNamePart(firstName);
          String lastNamePart = normalizeNamePart(lastName);

          return firstNamePart + "." + lastNamePart + "@jass.gob.pe";
     }

     /**
      * Normaliza una parte del nombre (quita espacios, acentos, etc.)
      */
     private String normalizeNamePart(String namePart) {
          return namePart.toLowerCase()
                    .trim()
                    .split("\\s+")[0] // Solo primer nombre/apellido
                    .replaceAll("[áàäâ]", "a")
                    .replaceAll("[éèëê]", "e")
                    .replaceAll("[íìïî]", "i")
                    .replaceAll("[óòöô]", "o")
                    .replaceAll("[úùüû]", "u")
                    .replaceAll("[ñ]", "n")
                    .replaceAll("[^a-z]", ""); // Solo letras minúsculas
     }

     /**
      * Generar contraseña segura según reglas de dominio
      */
     private String generateSecurePassword() {
          StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

          // Asegurar al menos un carácter de cada tipo
          password.append(getRandomChar(UPPERCASE));
          password.append(getRandomChar(LOWERCASE));
          password.append(getRandomChar(DIGITS));
          password.append(getRandomChar(SPECIAL_CHARS));

          // Llenar el resto con caracteres aleatorios
          for (int i = 4; i < PASSWORD_LENGTH; i++) {
               password.append(getRandomChar(ALL_CHARS));
          }

          // Mezclar la contraseña
          return shuffleString(password.toString());
     }

     /**
      * Calcular fecha de expiración según tipo de usuario
      * Para contraseñas temporales: 15 minutos según requerimientos
      */
     private LocalDateTime calculateExpirationDate(User user) {
          // CAMBIO: Contraseñas temporales expiran en 15 minutos para todos los
          // usuarios
          return LocalDateTime.now().plusMinutes(15);
     }

     /**
      * Validar si una credencial está vigente
      */
     public boolean isCredentialValid(UserCredential credential) {
          if (credential == null) {
               return false;
          }

          LocalDateTime now = LocalDateTime.now();
          boolean isValid = credential.getExpiresAt().isAfter(now);

          log.debug("Credencial {} está {}", credential.getUsername(),
                    isValid ? "vigente" : "expirada");

          return isValid;
     }

     /**
      * Verificar si el usuario debe cambiar contraseña
      */
     public boolean mustChangePassword(User user, UserCredential credential) {
          if (credential == null) {
               return true;
          }

          // Siempre debe cambiar si es temporal
          if (credential.isMustChangePassword()) {
               return true;
          }

          // Si la credencial está próxima a expirar (7 días)
          LocalDateTime warningDate = credential.getExpiresAt().minusDays(7);
          if (LocalDateTime.now().isAfter(warningDate)) {
               log.info("Usuario {} debe cambiar contraseña - próxima a expirar", user.getUserCode());
               return true;
          }

          return false;
     }

     /**
      * Utilities privados
      */
     private char getRandomChar(String source) {
          return source.charAt(secureRandom.nextInt(source.length()));
     }

     private String shuffleString(String input) {
          char[] chars = input.toCharArray();
          for (int i = chars.length - 1; i > 0; i--) {
               int j = secureRandom.nextInt(i + 1);
               char temp = chars[i];
               chars[i] = chars[j];
               chars[j] = temp;
          }
          return new String(chars);
     }

     /**
      * Value Object para credenciales
      */
     @lombok.Builder
     @lombok.Data
     public static class UserCredential {
          private String username;
          private String temporaryPassword;
          private LocalDateTime expiresAt;
          private boolean mustChangePassword;
          private LocalDateTime generatedAt;
          private String userId;

          public boolean isExpired() {
               return LocalDateTime.now().isAfter(expiresAt);
          }

          public long getDaysUntilExpiration() {
               return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
          }
     }
}