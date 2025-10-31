package pe.edu.vallegrande.vgmsusers.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.vgmsusers.application.service.PasswordService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias adicionales para PasswordService
 * Complementa las pruebas parametrizadas para mejorar cobertura
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - PasswordService")
class PasswordServiceImplTest {

     private PasswordService passwordService;

     @BeforeEach
     void setUp() {
          passwordService = new PasswordServiceImpl();
     }

     @Test
     @DisplayName("Debe generar contraseña temporal con características específicas")
     void shouldGenerateTemporaryPasswordWithSpecificCharacteristics() {
          // When
          String password = passwordService.generateTemporaryPassword();

          // Then
          assertNotNull(password, "La contraseña no debe ser null");
          assertEquals(12, password.length(), "La contraseña debe tener 12 caracteres");

          // Verificar que contenga al menos un carácter de cada tipo
          assertTrue(containsLowercase(password), "Debe contener minúsculas");
          assertTrue(containsUppercase(password), "Debe contener mayúsculas");
          assertTrue(containsNumbers(password), "Debe contener números");
          assertTrue(containsSpecialChars(password), "Debe contener caracteres especiales");
     }

     @Test
     @DisplayName("Debe generar contraseñas diferentes en múltiples llamadas")
     void shouldGenerateDifferentPasswordsOnMultipleCalls() {
          // When
          String password1 = passwordService.generateTemporaryPassword();
          String password2 = passwordService.generateTemporaryPassword();
          String password3 = passwordService.generateTemporaryPassword();

          // Then
          assertNotEquals(password1, password2, "Las contraseñas deben ser diferentes");
          assertNotEquals(password1, password3, "Las contraseñas deben ser diferentes");
          assertNotEquals(password2, password3, "Las contraseñas deben ser diferentes");
     }

     @Test
     @DisplayName("Debe codificar contraseña correctamente")
     void shouldEncodePasswordCorrectly() {
          // Given
          String rawPassword = "TestPassword123!";

          // When
          String encodedPassword = passwordService.encodePassword(rawPassword);

          // Then
          assertNotNull(encodedPassword, "La contraseña codificada no debe ser null");
          assertFalse(encodedPassword.isEmpty(), "La contraseña codificada no debe estar vacía");
          // En la implementación actual, retorna la misma contraseña
          assertEquals(rawPassword, encodedPassword);
     }

     @Test
     @DisplayName("Debe manejar contraseña null al codificar")
     void shouldHandleNullPasswordWhenEncoding() {
          // When & Then
          assertDoesNotThrow(() -> {
               String result = passwordService.encodePassword(null);
               // En la implementación actual, podría retornar null
          }, "Debería manejar null sin lanzar excepción");
     }

     @Test
     @DisplayName("Debe manejar contraseña vacía al codificar")
     void shouldHandleEmptyPasswordWhenEncoding() {
          // Given
          String emptyPassword = "";

          // When
          String result = passwordService.encodePassword(emptyPassword);

          // Then
          assertNotNull(result);
          assertEquals(emptyPassword, result);
     }

     @Test
     @DisplayName("Las contraseñas generadas deben usar todos los tipos de caracteres disponibles")
     void shouldUseAllAvailableCharacterTypes() {
          // Given
          int attempts = 50;
          boolean foundLower = false, foundUpper = false, foundNumber = false, foundSpecial = false;

          // When
          for (int i = 0; i < attempts; i++) {
               String password = passwordService.generateTemporaryPassword();

               if (containsLowercase(password))
                    foundLower = true;
               if (containsUppercase(password))
                    foundUpper = true;
               if (containsNumbers(password))
                    foundNumber = true;
               if (containsSpecialChars(password))
                    foundSpecial = true;

               // Si ya encontramos todos los tipos, podemos salir del loop
               if (foundLower && foundUpper && foundNumber && foundSpecial) {
                    break;
               }
          }

          // Then
          assertTrue(foundLower, "Debería generar contraseñas con minúsculas");
          assertTrue(foundUpper, "Debería generar contraseñas con mayúsculas");
          assertTrue(foundNumber, "Debería generar contraseñas con números");
          assertTrue(foundSpecial, "Debería generar contraseñas con caracteres especiales");
     }

     @Test
     @DisplayName("Debe mantener consistencia en longitud de contraseña")
     void shouldMaintainConsistentPasswordLength() {
          // When & Then
          for (int i = 0; i < 20; i++) {
               String password = passwordService.generateTemporaryPassword();
               assertEquals(12, password.length(),
                         "Todas las contraseñas deben tener exactamente 12 caracteres");
          }
     }

     // Métodos auxiliares
     private boolean containsLowercase(String password) {
          return password.chars().anyMatch(Character::isLowerCase);
     }

     private boolean containsUppercase(String password) {
          return password.chars().anyMatch(Character::isUpperCase);
     }

     private boolean containsNumbers(String password) {
          return password.chars().anyMatch(Character::isDigit);
     }

     private boolean containsSpecialChars(String password) {
          String specialChars = "!@#$%^&*()_+";
          return password.chars().anyMatch(ch -> specialChars.indexOf(ch) >= 0);
     }
}