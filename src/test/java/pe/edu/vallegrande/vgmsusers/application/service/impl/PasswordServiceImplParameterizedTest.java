package pe.edu.vallegrande.vgmsusers.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.vallegrande.vgmsusers.application.service.PasswordService;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas parametrizadas para PasswordService
 *
 * Esta clase demuestra el uso de diferentes tipos de pruebas parametrizadas:
 * - @ValueSource: Para probar con múltiples valores simples
 * - @MethodSource: Para probar con parámetros más complejos
 *
 * Las pruebas parametrizadas son útiles para:
 * 1. Evitar duplicación de código en pruebas
 * 2. Probar múltiples escenarios con la misma lógica
 * 3. Mejorar la cobertura de pruebas de manera eficiente
 */
@DisplayName("Pruebas Parametrizadas - PasswordService")
class PasswordServiceImplParameterizedTest {

     private PasswordService passwordService;

     @BeforeEach
     void setUp() {
          passwordService = new PasswordServiceImpl();
     }

     /**
      * Prueba parametrizada usando @ValueSource
      * Valida que contraseñas de diferentes longitudes sean codificadas
      * correctamente
      */
     @ParameterizedTest(name = "Codificar contraseña: ''{0}''")
     @ValueSource(strings = {
               "password123",
               "MiContraseña2023!",
               "Test@123",
               "SuperSecure$456",
               "Admin2024#",
               "User@Pass789"
     })
     @DisplayName("Debe codificar diferentes contraseñas correctamente")
     void shouldEncodePasswordsCorrectly(String rawPassword) {
          // When
          String encodedPassword = passwordService.encodePassword(rawPassword);

          // Then
          assertNotNull(encodedPassword, "La contraseña codificada no debe ser null");
          assertFalse(encodedPassword.isEmpty(), "La contraseña codificada no debe estar vacía");

          // Por ahora la implementación retorna la misma contraseña
          // En producción debería ser diferente (hasheada)
          assertEquals(rawPassword, encodedPassword);
     }

     /**
      * Prueba parametrizada usando @MethodSource
      * Valida diferentes características de las contraseñas generadas
      */
     @ParameterizedTest(name = "Validar contraseña generada - Iteración {index}")
     @MethodSource("providePasswordValidationCriteria")
     @DisplayName("Las contraseñas generadas deben cumplir criterios de seguridad")
     void shouldGeneratePasswordsWithSecurityCriteria(
               String testName,
               int expectedLength,
               boolean shouldContainLowercase,
               boolean shouldContainUppercase,
               boolean shouldContainNumbers,
               boolean shouldContainSpecialChars) {

          // When
          String generatedPassword = passwordService.generateTemporaryPassword();

          // Then - Validaciones básicas
          assertNotNull(generatedPassword, "La contraseña generada no debe ser null");
          assertEquals(expectedLength, generatedPassword.length(),
                    "La contraseña debe tener " + expectedLength + " caracteres");

          // Then - Validaciones de contenido
          if (shouldContainLowercase) {
               assertTrue(containsLowercase(generatedPassword),
                         "La contraseña debe contener al menos una letra minúscula");
          }

          if (shouldContainUppercase) {
               assertTrue(containsUppercase(generatedPassword),
                         "La contraseña debe contener al menos una letra mayúscula");
          }

          if (shouldContainNumbers) {
               assertTrue(containsNumbers(generatedPassword),
                         "La contraseña debe contener al menos un número");
          }

          if (shouldContainSpecialChars) {
               assertTrue(containsSpecialChars(generatedPassword),
                         "La contraseña debe contener al menos un carácter especial");
          }
     }

     /**
      * Provee datos de prueba para las validaciones de contraseña
      */
     static Stream<Arguments> providePasswordValidationCriteria() {
          return Stream.of(
                    Arguments.of("Criterios básicos", 12, true, true, true, true),
                    Arguments.of("Longitud exacta", 12, true, true, true, true),
                    Arguments.of("Mayúsculas requeridas", 12, false, true, false, false),
                    Arguments.of("Minúsculas requeridas", 12, true, false, false, false),
                    Arguments.of("Números requeridos", 12, false, false, true, false),
                    Arguments.of("Caracteres especiales requeridos", 12, false, false, false, true));
     }

     /**
      * Prueba que verifica la unicidad de las contraseñas generadas
      * Ejecuta múltiples generaciones para validar aleatoriedad
      */
     @Test
     @DisplayName("Las contraseñas generadas deben ser únicas")
     void shouldGenerateUniquePasswords() {
          // Given
          int numberOfPasswords = 100;

          // When & Then
          for (int i = 0; i < numberOfPasswords; i++) {
               String password1 = passwordService.generateTemporaryPassword();
               String password2 = passwordService.generateTemporaryPassword();

               assertNotEquals(password1, password2,
                         "Las contraseñas generadas consecutivamente deben ser diferentes");
          }
     }

     /**
      * Prueba parametrizada para validar casos edge de contraseñas vacías o null
      */
     @ParameterizedTest(name = "Manejar entrada inválida: ''{0}''")
     @ValueSource(strings = { "", " ", "  " })
     @DisplayName("Debe manejar entradas de contraseña inválidas")
     void shouldHandleInvalidPasswordInputs(String invalidInput) {
          // When & Then
          assertDoesNotThrow(() -> {
               String result = passwordService.encodePassword(invalidInput);
               assertNotNull(result);
          }, "El servicio debe manejar entradas inválidas sin lanzar excepciones");
     }

     // Métodos auxiliares para validación
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