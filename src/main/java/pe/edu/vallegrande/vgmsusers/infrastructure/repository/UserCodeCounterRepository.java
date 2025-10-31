package pe.edu.vallegrande.vgmsusers.infrastructure.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.vgmsusers.domain.model.UserCodeCounter;
import reactor.core.publisher.Mono;

@Repository
public interface UserCodeCounterRepository extends ReactiveMongoRepository<UserCodeCounter, String> {

     /**
      * Busca el contador de códigos por organización
      */
     Mono<UserCodeCounter> findByOrganizationId(String organizationId);

     /**
      * Verifica si existe un contador para la organización
      */
     Mono<Boolean> existsByOrganizationId(String organizationId);
}