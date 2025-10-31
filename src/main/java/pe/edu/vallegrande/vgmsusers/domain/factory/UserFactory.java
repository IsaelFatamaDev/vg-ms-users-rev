package pe.edu.vallegrande.vgmsusers.domain.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsusers.domain.model.User;
import pe.edu.vallegrande.vgmsusers.domain.model.PersonalInfo;
import pe.edu.vallegrande.vgmsusers.domain.model.Contact;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Factory Pattern - Responsable de la creación compleja de usuarios
 * Implementa lógica de dominio para crear usuarios según reglas de negocio
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserFactory {

     /**
      * Crear usuario CLIENT con reglas de dominio
      */
     public User createClient(CreateUserCommand command) {
          log.info("UserFactory: Creando CLIENT para organización {}", command.getOrganizationId());

          validateClientCreation(command);

          return User.builder()
                    .userCode(command.getUserCode())
                    .organizationId(command.getOrganizationId())
                    .personalInfo(createPersonalInfo(command))
                    .contact(createContact(command))
                    .roles(Set.of(RolesUsers.CLIENT))
                    .status(UserStatus.PENDING)
                    .registrationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .createdBy(command.getCreatedBy())
                    .build();
     }

     /**
      * Crear usuario ADMIN con reglas de dominio
      */
     public User createAdmin(CreateUserCommand command) {
          log.info("UserFactory: Creando ADMIN para organización {}", command.getOrganizationId());

          validateAdminCreation(command);

          return User.builder()
                    .userCode(command.getUserCode())
                    .organizationId(command.getOrganizationId())
                    .personalInfo(createPersonalInfo(command))
                    .contact(createContact(command))
                    .roles(Set.of(RolesUsers.ADMIN))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .createdBy(command.getCreatedBy())
                    .build();
     }

     /**
      * Crear usuario OPERATOR con reglas de dominio
      */
     public User createOperator(CreateUserCommand command) {
          log.info("UserFactory: Creando OPERATOR para organización {}", command.getOrganizationId());

          validateOperatorCreation(command);

          return User.builder()
                    .userCode(command.getUserCode())
                    .organizationId(command.getOrganizationId())
                    .personalInfo(createPersonalInfo(command))
                    .contact(createContact(command))
                    .roles(Set.of(RolesUsers.OPERATOR))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .createdBy(command.getCreatedBy())
                    .build();
     }

     /**
      * Crear usuario SUPER_ADMIN con reglas de dominio
      */
     public User createSuperAdmin(CreateUserCommand command) {
          log.info("UserFactory: Creando SUPER_ADMIN");

          validateSuperAdminCreation(command);

          return User.builder()
                    .userCode(command.getUserCode())
                    .organizationId(command.getOrganizationId())
                    .personalInfo(createPersonalInfo(command))
                    .contact(createContact(command))
                    .roles(Set.of(RolesUsers.SUPER_ADMIN))
                    .status(UserStatus.ACTIVE)
                    .registrationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .createdBy(command.getCreatedBy())
                    .build();
     }

     /**
      * Crear PersonalInfo con validaciones del dominio
      */
     private PersonalInfo createPersonalInfo(CreateUserCommand command) {
          return PersonalInfo.builder()
                    .firstName(validateAndFormatName(command.getFirstName()))
                    .lastName(validateAndFormatName(command.getLastName()))
                    .documentType(command.getDocumentType())
                    .documentNumber(validateDocumentNumber(command.getDocumentNumber(), command.getDocumentType()))
                    .build();
     }

     /**
      * Crear Contact con validaciones del dominio
      */
     private Contact createContact(CreateUserCommand command) {
          return Contact.builder()
                    .phone(validatePhoneNumber(command.getPhone()))
                    .email(validateEmail(command.getEmail()))
                    .build();
     }

     /**
      * Validaciones para creación de CLIENT
      */
     private void validateClientCreation(CreateUserCommand command) {
          if (command.getOrganizationId() == null || command.getOrganizationId().trim().isEmpty()) {
               throw new IllegalArgumentException("CLIENT debe tener organizationId");
          }

          if (command.getCreatedBy() == null) {
               throw new IllegalArgumentException("CLIENT debe ser creado por un ADMIN");
          }

          log.debug("Validaciones de CLIENT completadas");
     }

     /**
      * Validaciones para creación de ADMIN
      */
     private void validateAdminCreation(CreateUserCommand command) {
          if (command.getOrganizationId() == null || command.getOrganizationId().trim().isEmpty()) {
               throw new IllegalArgumentException("ADMIN debe tener organizationId");
          }

          if (command.getCreatedBy() == null) {
               throw new IllegalArgumentException("ADMIN debe ser creado por un SUPER_ADMIN");
          }

          log.debug("Validaciones de ADMIN completadas");
     }

     /**
      * Validaciones para creación de OPERATOR
      */
     private void validateOperatorCreation(CreateUserCommand command) {
          if (command.getOrganizationId() == null || command.getOrganizationId().trim().isEmpty()) {
               throw new IllegalArgumentException("OPERATOR debe tener organizationId");
          }

          if (command.getCreatedBy() == null) {
               throw new IllegalArgumentException("OPERATOR debe ser creado por un ADMIN o SUPER_ADMIN");
          }

          log.debug("Validaciones de OPERATOR completadas");
     }

     /**
      * Validaciones para creación de SUPER_ADMIN
      */
     private void validateSuperAdminCreation(CreateUserCommand command) {
          // SUPER_ADMIN puede no tener organización específica
          log.debug("Validaciones de SUPER_ADMIN completadas");
     }

     /**
      * Validar y formatear nombres
      */
     private String validateAndFormatName(String name) {
          if (name == null || name.trim().isEmpty()) {
               throw new IllegalArgumentException("Nombre no puede estar vacío");
          }

          if (name.length() < 2 || name.length() > 50) {
               throw new IllegalArgumentException("Nombre debe tener entre 2 y 50 caracteres");
          }

          // Formato: Primera letra mayúscula, resto minúscula
          return name.trim().substring(0, 1).toUpperCase() +
                    name.trim().substring(1).toLowerCase();
     }

     /**
      * Validar número de documento según tipo
      */
     private String validateDocumentNumber(String documentNumber, DocumentType type) {
          if (documentNumber == null || documentNumber.trim().isEmpty()) {
               throw new IllegalArgumentException("Número de documento no puede estar vacío");
          }

          String cleanNumber = documentNumber.trim().replaceAll("\\D", "");

          switch (type) {
               case DNI:
                    if (cleanNumber.length() != 8) {
                         throw new IllegalArgumentException("DNI debe tener 8 dígitos");
                    }
                    break;
               case CARNET_EXTRANJERIA:
                    if (cleanNumber.length() < 6 || cleanNumber.length() > 12) {
                         throw new IllegalArgumentException("Carnet de extranjería debe tener entre 6 y 12 caracteres");
                    }
                    break;
          }

          return cleanNumber;
     }

     /**
      * Validar número de teléfono
      */
     private String validatePhoneNumber(String phone) {
          if (phone == null || phone.trim().isEmpty()) {
               throw new IllegalArgumentException("Teléfono no puede estar vacío");
          }

          String cleanPhone = phone.trim().replaceAll("\\D", "");

          if (cleanPhone.length() != 9) {
               throw new IllegalArgumentException("Teléfono debe tener 9 dígitos");
          }

          return cleanPhone;
     }

     /**
      * Validar email
      */
     private String validateEmail(String email) {
          if (email == null || email.trim().isEmpty()) {
               throw new IllegalArgumentException("Email no puede estar vacío");
          }

          String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
          if (!email.matches(emailRegex)) {
               throw new IllegalArgumentException("Email no tiene formato válido");
          }

          return email.trim().toLowerCase();
     }

     /**
      * Command para creación de usuarios
      */
     @lombok.Builder
     @lombok.Data
     public static class CreateUserCommand {
          private String userCode;
          private String organizationId;
          private String firstName;
          private String lastName;
          private DocumentType documentType;
          private String documentNumber;
          private java.time.LocalDate birthDate;
          private String phone;
          private String email;
          private String createdBy;
     }
}