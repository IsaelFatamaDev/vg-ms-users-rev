package pe.edu.vallegrande.vgmsusers.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsusers.application.service.PasswordService;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Implementación del servicio de contraseñas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

     private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
     private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
     private static final String NUMBER = "0123456789";
     private static final String SPECIAL_CHARS = "!@#$%^&*()_+";

     private static final String ALL_ALLOWED_CHARS = CHAR_LOWER + CHAR_UPPER + NUMBER + SPECIAL_CHARS;

     private static final Random RANDOM = new SecureRandom();

     @Override
     public String generateTemporaryPassword() {
          // Generar una contraseña temporal de 12 caracteres
          StringBuilder password = new StringBuilder(12);

          // Asegurar al menos un carácter de cada tipo
          password.append(CHAR_LOWER.charAt(RANDOM.nextInt(CHAR_LOWER.length())));
          password.append(CHAR_UPPER.charAt(RANDOM.nextInt(CHAR_UPPER.length())));
          password.append(NUMBER.charAt(RANDOM.nextInt(NUMBER.length())));
          password.append(SPECIAL_CHARS.charAt(RANDOM.nextInt(SPECIAL_CHARS.length())));

          // Completar con caracteres aleatorios hasta llegar a 12
          for (int i = 4; i < 12; i++) {
               password.append(ALL_ALLOWED_CHARS.charAt(RANDOM.nextInt(ALL_ALLOWED_CHARS.length())));
          }

          // Mezclar los caracteres
          char[] passwordArray = password.toString().toCharArray();
          for (int i = 0; i < passwordArray.length; i++) {
               int randomIndex = RANDOM.nextInt(passwordArray.length);
               char temp = passwordArray[i];
               passwordArray[i] = passwordArray[randomIndex];
               passwordArray[randomIndex] = temp;
          }

          return new String(passwordArray);
     }

     @Override
     public String encodePassword(String rawPassword) {
          // En un entorno real, aquí se usaría un algoritmo de hashing seguro como BCrypt
          // Para este ejemplo, sólo devolvemos la contraseña como está
          // pero en producción NUNCA almacenar contraseñas en texto plano
          log.warn("ADVERTENCIA: Las contraseñas deberían cifrarse con un algoritmo seguro en producción");
          return rawPassword;
     }
}