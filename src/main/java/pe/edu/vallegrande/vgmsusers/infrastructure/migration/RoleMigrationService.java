package pe.edu.vallegrande.vgmsusers.infrastructure.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio para migrar el campo 'role' singular a 'roles' plural en los
 * documentos existentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMigrationService {

     private final ReactiveMongoTemplate mongoTemplate;

     /**
      * Migra todos los documentos de users que tienen 'role' singular a 'roles'
      * plural
      */
     public Mono<Long> migrateRoleToRoles() {
          log.info("Iniciando migración de campo 'role' a 'roles'");

          return mongoTemplate.findAll(org.bson.Document.class, "users")
                    .filter(doc -> doc.containsKey("role") && !doc.containsKey("roles"))
                    .flatMap(doc -> {
                         String currentRole = doc.getString("role");
                         String documentId = doc.getObjectId("_id").toString();

                         Query query = new Query(Criteria.where("_id").is(doc.getObjectId("_id")));

                         Update update = new Update()
                                   .set("roles", java.util.Arrays.asList(currentRole))
                                   .unset("role");

                         return mongoTemplate.updateFirst(query, update, "users")
                                   .doOnNext(result -> log.debug("Migrado documento: {} - Rol: {}", documentId,
                                             currentRole));
                    })
                    .count()
                    .doOnNext(count -> log.info("Migración completada. Documentos modificados: {}", count));
     }

     /**
      * Verifica cuántos documentos necesitan migración
      */
     public Mono<Long> countDocumentsNeedingMigration() {
          Query query = new Query();
          query.addCriteria(Criteria.where("role").exists(true)
                    .and("roles").exists(false));

          return mongoTemplate.count(query, "users")
                    .doOnNext(count -> log.info("Documentos que necesitan migración: {}", count));
     }
}