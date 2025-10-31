package pe.edu.vallegrande.vgmsusers.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsusers.application.service.UserCodeService;
import pe.edu.vallegrande.vgmsusers.domain.model.UserCodeCounter;
import pe.edu.vallegrande.vgmsusers.infrastructure.repository.UserCodeCounterRepository;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCodeServiceImpl implements UserCodeService {

     private final UserCodeCounterRepository userCodeCounterRepository;

     @Override
     public Mono<String> generateUserCode(String organizationId) {
          log.info("Generando código de usuario para organización: {}", organizationId);

          return userCodeCounterRepository.findByOrganizationId(organizationId)
                    .switchIfEmpty(createNewCounter(organizationId))
                    .flatMap(this::incrementAndSave)
                    .map(counter -> String.format("%s%05d", counter.getPrefix(), counter.getLastCode()))
                    .doOnSuccess(code -> log.info("Código generado: {} para organización: {}", code, organizationId))
                    .doOnError(error -> log.error("Error generando código para organización {}: {}", organizationId,
                              error.getMessage()));
     }

     @Override
     public Mono<String> getNextUserCode(String organizationId) {
          return userCodeCounterRepository.findByOrganizationId(organizationId)
                    .switchIfEmpty(createNewCounter(organizationId))
                    .map(UserCodeCounter::getNextCode);
     }

     @Override
     public Mono<String> getLastUserCode(String organizationId) {
          return userCodeCounterRepository.findByOrganizationId(organizationId)
                    .map(counter -> {
                         if (counter.getLastCode() == null || counter.getLastCode() == 0) {
                              return "Ningún código generado aún";
                         }
                         return String.format("%s%05d", counter.getPrefix(), counter.getLastCode());
                    })
                    .defaultIfEmpty("Ningún código generado aún");
     }

     @Override
     public Mono<Void> resetCounter(String organizationId) {
          log.warn("Reiniciando contador de códigos para organización: {}", organizationId);

          return userCodeCounterRepository.findByOrganizationId(organizationId)
                    .flatMap(counter -> {
                         counter.setLastCode(0L);
                         return userCodeCounterRepository.save(counter);
                    })
                    .then();
     }

     /**
      * Crea un nuevo contador para una organización
      */
     private Mono<UserCodeCounter> createNewCounter(String organizationId) {
          log.info("Creando nuevo contador para organización: {}", organizationId);

          UserCodeCounter newCounter = UserCodeCounter.builder()
                    .organizationId(organizationId)
                    .lastCode(0L)
                    .prefix("USR")
                    .build();

          return userCodeCounterRepository.save(newCounter);
     }

     /**
      * Incrementa el contador y lo guarda
      */
     private Mono<UserCodeCounter> incrementAndSave(UserCodeCounter counter) {
          counter.setLastCode(counter.getLastCode() + 1);
          return userCodeCounterRepository.save(counter);
     }
}