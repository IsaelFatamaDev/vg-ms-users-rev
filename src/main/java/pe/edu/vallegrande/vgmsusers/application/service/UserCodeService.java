package pe.edu.vallegrande.vgmsusers.application.service;

import reactor.core.publisher.Mono;

/**
 * Servicio para generar códigos únicos de usuarios por organización
 */
public interface UserCodeService {

     /**
      * Genera el siguiente código de usuario para la organización especificada
      */
     Mono<String> generateUserCode(String organizationId);

     /**
      * Obtiene el próximo código que se generaría sin incrementar el contador
      */
     Mono<String> getNextUserCode(String organizationId);

     /**
      * Obtiene el último código generado para una organización
      */
     Mono<String> getLastUserCode(String organizationId);

     /**
      * Reinicia el contador de códigos para una organización (solo para testing o
      * casos especiales)
      */
     Mono<Void> resetCounter(String organizationId);
}