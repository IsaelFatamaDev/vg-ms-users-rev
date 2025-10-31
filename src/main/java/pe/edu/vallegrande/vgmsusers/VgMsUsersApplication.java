package pe.edu.vallegrande.vgmsusers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Clase principal del microservicio de autenticación y gestión de usuarios
 * Sistema JASS (Juntas Administradoras de Servicios de Saneamiento)
 */
@SpringBootApplication
@EnableReactiveMongoRepositories
public class VgMsUsersApplication {

    public static void main(String[] args) {
        SpringApplication.run(VgMsUsersApplication.class, args);
    }
}