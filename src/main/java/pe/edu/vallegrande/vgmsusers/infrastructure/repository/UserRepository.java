package pe.edu.vallegrande.vgmsusers.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;
import pe.edu.vallegrande.vgmsusers.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

     // Búsquedas básicas
     @Query("{'userCode': ?0, 'deletedAt': null}")
     Mono<User> findByUserCodeAndDeletedAtIsNull(String userCode);

     @Query("{'_id': ?0, 'deletedAt': null}")
     Mono<User> findByIdAndDeletedAtIsNull(String id);

     // NUEVO: Búsqueda por username para MS-AUTHENTICATION
     @Query("{'username': ?0, 'deletedAt': null}")
     Mono<User> findByUsernameAndDeletedAtIsNull(String username);

     // Búsquedas por organización
     @Query("{'organizationId': ?0, 'deletedAt': null}")
     Flux<User> findByOrganizationIdAndDeletedAtIsNull(String organizationId, Pageable pageable);

     @Query("{'organizationId': ?0, 'deletedAt': null}")
     Flux<User> findByOrganizationIdAndDeletedAtIsNull(String organizationId);

     // NUEVO: Obtener TODOS los usuarios de una organización (activos e inactivos)
     @Query("{'organizationId': ?0}")
     Flux<User> findByOrganizationId(String organizationId);

     @Query(value = "{'organizationId': ?0, 'deletedAt': null}", count = true)
     Mono<Long> countByOrganizationIdAndDeletedAtIsNull(String organizationId);

     // Búsquedas por organización y estado
     @Query("{'organizationId': ?0, 'status': ?1, 'deletedAt': null}")
     Flux<User> findByOrganizationIdAndStatusAndDeletedAtIsNull(String organizationId, UserStatus status);

     @Query("{'organizationId': ?0, 'status': ?1}")
     Flux<User> findByOrganizationIdAndStatus(String organizationId, UserStatus status);

     // Búsquedas por rol (usando $in para buscar dentro del Set de roles)
     @Query("{'organizationId': ?0, 'roles': {'$in': [?1]}, 'deletedAt': null}")
     Flux<User> findByOrganizationIdAndRoleAndDeletedAtIsNull(String organizationId, RolesUsers role);

     // Validaciones de existencia
     @Query(value = "{'personalInfo.documentNumber': ?0, 'deletedAt': null}", exists = true)
     Mono<Boolean> existsByPersonalInfoDocumentNumberAndDeletedAtIsNull(String documentNumber);

     @Query(value = "{'contact.email': ?0, 'deletedAt': null}", exists = true)
     Mono<Boolean> existsByContactEmailAndDeletedAtIsNull(String email);

     @Query(value = "{'contact.phone': ?0, 'deletedAt': null}", exists = true)
     Mono<Boolean> existsByContactPhoneAndDeletedAtIsNull(String phone);

     // Búsquedas adicionales
     @Query("{'contact.email': ?0, 'deletedAt': null}")
     Mono<User> findByContactEmailAndDeletedAtIsNull(String email);

     @Query("{'contact.phone': ?0, 'deletedAt': null}")
     Mono<User> findByContactPhoneAndDeletedAtIsNull(String phone);

     @Query("{'personalInfo.documentNumber': ?0, 'deletedAt': null}")
     Mono<User> findByPersonalInfoDocumentNumberAndDeletedAtIsNull(String documentNumber);

     // NUEVO: Contar usuarios SUPER_ADMIN
     @Query(value = "{'roles': {'$in': [?0]}, 'deletedAt': null}", count = true)
     Mono<Long> countByRolesContainingAndDeletedAtIsNull(RolesUsers role);
}