package pe.edu.vallegrande.vgmsusers.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsusers.application.service.UserAuthIntegrationService;
import pe.edu.vallegrande.vgmsusers.domain.model.User;
import pe.edu.vallegrande.vgmsusers.infrastructure.client.AuthenticationClient;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ApiResponse;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.request.UserCredentialRequest;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.response.CreateAccountResponse;
import reactor.core.publisher.Mono;

import java.text.Normalizer;

/**
 * Implementación del servicio de integración con el microservicio de
 * autenticación
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthIntegrationServiceImpl implements UserAuthIntegrationService {

     private final AuthenticationClient authenticationClient;

     @Override
     public Mono<ApiResponse<String>> registerUserInAuthService(User user, String temporaryPassword) {
          log.info("Registrando usuario {} en el servicio de autenticación", user.getUserCode());

          // Verificar si el servicio está disponible
          return authenticationClient.isServiceAvailable()
                    .flatMap(available -> {
                         if (!available) {
                              log.warn("El servicio de autenticación no está disponible, se saltará el registro de usuario");
                              return Mono.just(ApiResponse.<String>builder()
                                        .success(false)
                                        .message("El servicio de autenticación no está disponible, el usuario se creará sin credenciales")
                                        .build());
                         }

                         // Generar nombre de usuario
                         String username = generateUsername(
                                   user.getPersonalInfo().getFirstName(),
                                   user.getPersonalInfo().getLastName());

                         // Crear solicitud para el servicio de autenticación
                         UserCredentialRequest request = UserCredentialRequest.builder()
                                   .username(username)
                                   .email(user.getContact().getEmail())
                                   .firstName(user.getPersonalInfo().getFirstName())
                                   .lastName(user.getPersonalInfo().getLastName())
                                   .temporaryPassword(temporaryPassword)
                                   .organizationId(user.getOrganizationId()) // AGREGADO
                                   .roles(user.getRoles())
                                   .userCode(user.getUserCode())
                                   .userId(user.getId())
                                   .build();

                         // Enviar solicitud al servicio de autenticación
                         return authenticationClient.registerUserInKeycloak(request);
                    });
     }

     @Override
     public String generateUsername(String firstName, String lastName) {
          if (firstName == null || lastName == null) {
               return null;
          }

          // Tomar el primer nombre y el primer apellido
          String firstNamePart = firstName.trim().split("\\s+")[0].toLowerCase();
          String lastNamePart = lastName.trim().split("\\s+")[0].toLowerCase();

          // Normalizar (eliminar acentos y caracteres especiales)
          firstNamePart = normalizeString(firstNamePart);
          lastNamePart = normalizeString(lastNamePart);

          // Crear username en formato nombre.apellido@jass.gob.pe
          return firstNamePart + "." + lastNamePart + "@jass.gob.pe";
     }

     /**
      * Normaliza un string eliminando acentos y caracteres especiales
      */
     private String normalizeString(String input) {
          // Convertir a ASCII básico (eliminar acentos)
          String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

          // Mantener solo caracteres ASCII básicos (letras sin acentos y números)
          return normalized.replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("[^a-zA-Z0-9]", "");
     }

     /**
      * Genera username inteligente desde nombres completos RENIEC
      * Maneja casos como "VICTORIA ROSALINA" + "DE LA CRUZ LAURA"
      * Resultado: "victoria.cruz.l@jass.gob.pe"
      */
     private String generateUsernameFromName(String fullFirstName, String fullLastName) {
          log.debug("Generando username desde: '{}' | '{}'", fullFirstName, fullLastName);

          if (fullFirstName == null || fullLastName == null) {
               return "usuario.temporal@jass.gob.pe";
          }

          // Separar apellidos: "DE LA CRUZ LAURA" -> firstLastName="DE LA CRUZ",
          // secondLastName="LAURA"
          String[] lastNameParts = fullLastName.trim().split("\\s+");
          String firstLastName = "";
          String secondLastName = "";

          if (lastNameParts.length >= 2) {
               // Buscar el punto de división más lógico
               // Generalmente el último elemento es el segundo apellido
               secondLastName = lastNameParts[lastNameParts.length - 1];

               // Todo lo anterior es el primer apellido
               String[] firstLastNameParts = new String[lastNameParts.length - 1];
               System.arraycopy(lastNameParts, 0, firstLastNameParts, 0, lastNameParts.length - 1);
               firstLastName = String.join(" ", firstLastNameParts);
          } else {
               firstLastName = fullLastName;
          }

          log.debug("Apellidos separados: firstLastName='{}', secondLastName='{}'", firstLastName, secondLastName);

          // Procesar primer nombre (usar solo el primero si hay varios)
          String primaryFirstName = cleanAndNormalize(fullFirstName.trim().split("\\s+")[0]).toLowerCase();
          log.debug("Primer nombre procesado: '{}'", primaryFirstName);

          // Procesar primer apellido - manejar palabras de 2 letras
          String processedFirstLastName = processLastNameIntelligentlyFixed(firstLastName);
          log.debug("Primer apellido procesado: '{}'", processedFirstLastName);

          // Procesar segundo apellido - solo primera letra si existe
          String secondLastNameInitial = "";
          if (!secondLastName.isEmpty()) {
               String[] secondWords = secondLastName.trim().split("\\s+");
               for (String word : secondWords) {
                    if (!isShortWordFixed(word)) {
                         secondLastNameInitial = "." + cleanAndNormalize(word).toLowerCase().charAt(0);
                         break;
                    }
               }
               // Si todas son palabras cortas, tomar la primera de la última palabra
               if (secondLastNameInitial.isEmpty() && secondWords.length > 0) {
                    secondLastNameInitial = "."
                              + cleanAndNormalize(secondWords[secondWords.length - 1]).toLowerCase().charAt(0);
               }
          }
          log.debug("Inicial segundo apellido: '{}'", secondLastNameInitial);

          String username = primaryFirstName + "." + processedFirstLastName + secondLastNameInitial + "@jass.gob.pe";
          log.info("Username generado: {} para persona: {} {}", username, fullFirstName, fullLastName);

          return username;
     }

     /**
      * Procesa apellidos manejando casos especiales como "DE LA CRUZ LAURA"
      * Si detecta palabras cortas (DE, LA), usa la siguiente palabra válida +
      * inicial
      */
     private String processLastNameIntelligently(String lastName) {
          if (lastName == null || lastName.trim().isEmpty()) {
               return "usuario";
          }

          String[] words = lastName.trim().split("\\s+");

          // Buscar la primera palabra que tenga más de 2 letras
          String mainLastName = null;
          String secondLastNameInitial = null;

          for (int i = 0; i < words.length; i++) {
               if (words[i].length() > 2) {
                    if (mainLastName == null) {
                         mainLastName = words[i];
                         // Si hay más palabras después, tomar inicial de la siguiente palabra
                         // significativa
                         for (int j = i + 1; j < words.length; j++) {
                              if (words[j].length() > 0) {
                                   secondLastNameInitial = String.valueOf(words[j].charAt(0));
                                   break;
                              }
                         }
                         break;
                    }
               }
          }

          // Si no encontramos palabra principal, usar la última
          if (mainLastName == null) {
               mainLastName = words[words.length - 1];
          }

          // Construir resultado
          if (secondLastNameInitial != null) {
               return mainLastName + "." + secondLastNameInitial;
          } else {
               return mainLastName;
          }
     }

     /**
      * Obtiene la primera palabra de un texto
      */
     private String getFirstWord(String text) {
          if (text == null || text.trim().isEmpty()) {
               return "usuario";
          }

          String[] words = text.trim().split("\\s+");
          return words[0];
     }

     /**
      * Limpia y normaliza texto removiendo tildes y caracteres especiales
      */
     private String cleanAndNormalize(String text) {
          if (text == null) {
               return null;
          }

          return text.trim()
                    .replaceAll("Á", "A").replaceAll("É", "E").replaceAll("Í", "I")
                    .replaceAll("Ó", "O").replaceAll("Ú", "U").replaceAll("Ñ", "N")
                    .replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i")
                    .replaceAll("ó", "o").replaceAll("ú", "u").replaceAll("ñ", "n")
                    .replaceAll("[^A-Za-z0-9\\s]", ""); // Remover caracteres especiales
     }

     @Override
     public Mono<CreateAccountResponse> registerUserWithAutoPassword(User user) {
          log.info("🔧 Registrando usuario {} en MS-AUTHENTICATION (contraseña automática)", user.getUserCode());

          // Verificar si el servicio está disponible
          return authenticationClient.isServiceAvailable()
                    .flatMap(available -> {
                         if (!available) {
                              log.warn("El servicio de autenticación no está disponible");
                              return Mono.error(new RuntimeException("MS-AUTHENTICATION no disponible"));
                         }

                         // Usar directamente el email del usuario como username
                         // porque AdminRest.java ya generó el email correctamente con el algoritmo
                         // inteligente
                         String generatedUsername = user.getContact().getEmail();

                         if (generatedUsername == null || generatedUsername.trim().isEmpty()) {
                              // Fallback: generar username si no hay email (caso extremo)
                              generatedUsername = generateUsernameFromName(
                                        user.getPersonalInfo().getFirstName(),
                                        user.getPersonalInfo().getLastName());
                         }

                         log.info("🎯 Username para MS-AUTHENTICATION: {}", generatedUsername);

                         // Crear solicitud CON username generado y contraseña permanente
                         UserCredentialRequest request = UserCredentialRequest.builder()
                                   .username(generatedUsername) // Username generado inteligentemente
                                   .email(user.getContact().getEmail())
                                   .firstName(user.getPersonalInfo().getFirstName())
                                   .lastName(user.getPersonalInfo().getLastName())
                                   .temporaryPassword(user.getPersonalInfo().getDocumentNumber()) // documentNumber
                                                                                                  // como contraseña
                                                                                                  // permanente
                                   .organizationId(user.getOrganizationId())
                                   .roles(user.getRoles())
                                   .userCode(user.getUserCode())
                                   .userId(user.getId())
                                   .build();

                         // Llamar a MS-AUTHENTICATION y obtener la respuesta con username y contraseña
                         // generados
                         return authenticationClient.createAccountWithFullResponse(request)
                                   .map(response -> {
                                        if (response.isSuccess()) {
                                             // MS-AUTHENTICATION devuelve CreateAccountResponse con username y
                                             // contraseña generados
                                             CreateAccountResponse authData = response.getData();
                                             log.info(" Usuario registrado en MS-AUTHENTICATION con username: {} y contraseña: {}",
                                                       authData.username(), authData.temporaryPassword());

                                             return authData; // Devolver directamente la respuesta de MS-AUTHENTICATION
                                        } else {
                                             throw new RuntimeException(
                                                       "Error en MS-AUTHENTICATION: " + response.getMessage());
                                        }
                                   });
                    });
     }

     /**
      * Procesar apellido de forma inteligente (versión corregida)
      * Si encuentra palabras de 2 letras (DE, LA, etc.), busca la siguiente palabra
      * significativa
      * MISMO ALGORITMO QUE AdminRest.java
      */
     private String processLastNameIntelligentlyFixed(String lastName) {
          if (lastName == null || lastName.trim().isEmpty()) {
               return "apellido";
          }

          String[] words = lastName.trim().split("\\s+");
          log.debug("Procesando apellido con palabras: {}", java.util.Arrays.toString(words));

          // Buscar la primera palabra significativa (más de 2 letras)
          for (String word : words) {
               if (!isShortWordFixed(word)) {
                    log.debug("Palabra significativa encontrada: '{}'", word);
                    return cleanAndNormalize(word).toLowerCase();
               }
          }

          // Si todas son palabras cortas, usar la última
          if (words.length > 0) {
               String lastWord = cleanAndNormalize(words[words.length - 1]).toLowerCase();
               log.debug("Usando última palabra: '{}'", lastWord);
               return lastWord;
          }

          return "apellido";
     }

     /**
      * Verificar si una palabra es "corta" (artículos, preposiciones, etc.)
      * MISMO ALGORITMO QUE AdminRest.java
      */
     private boolean isShortWordFixed(String word) {
          if (word == null || word.length() <= 2) {
               return true;
          }

          // Palabras comunes de 3 letras que también consideramos "cortas"
          String upperWord = word.toUpperCase();
          return java.util.Set.of("DE", "LA", "EL", "DEL", "LAS", "LOS", "VON", "VAN", "MAC", "DI").contains(upperWord);
     }
}