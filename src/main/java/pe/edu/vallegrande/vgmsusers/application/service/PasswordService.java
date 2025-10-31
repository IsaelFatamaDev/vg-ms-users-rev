package pe.edu.vallegrande.vgmsusers.application.service;

/**
 * Servicio para generación y gestión de contraseñas temporales
 */
public interface PasswordService {

     /**
      * Genera una contraseña temporal aleatoria
      *
      * @return La contraseña temporal generada
      */
     String generateTemporaryPassword();

     /**
      * Codifica una contraseña para almacenamiento seguro
      *
      * @param rawPassword La contraseña en texto plano
      * @return La contraseña codificada
      */
     String encodePassword(String rawPassword);
}