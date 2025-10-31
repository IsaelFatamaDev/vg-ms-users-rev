package pe.edu.vallegrande.vgmsusers.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.PersonalDataDto;
import pe.edu.vallegrande.vgmsusers.infrastructure.dto.ReniecResponseDto;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReniecClient {

    private final WebClient webClient;

    @Value("${external.diacolecta_reniec.url}")
    private String reniecApiUrl;

    @Value("${external.diacolecta_reniec.token}")
    private String reniecApiToken;

    public Mono<PersonalDataDto> getPersonalDataByDni(String documentNumber) {
        long startTime = System.currentTimeMillis();

        String url = reniecApiUrl + "?numero=" + documentNumber;

        return webClient
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("Authorization", "Bearer " + reniecApiToken);
                    headers.set("Content-Type", "application/json");
                    headers.set("Accept", "application/json");
                    headers.set("Connection", "keep-alive");
                    headers.set("Cache-Control", "no-cache");
                })
                .retrieve()
                .bodyToMono(ReniecResponseDto.class)
                .doOnNext(reniecData -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                })
                .map(this::processReniecData)
                .doOnNext(result -> {
                    long totalTime = System.currentTimeMillis() - startTime;
                })
                .doOnError(error -> {
                    long errorTime = System.currentTimeMillis() - startTime;
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    String errorMessage = switch (ex.getStatusCode().value()) {
                        case 400 -> "DNI inválido o con formato incorrecto";
                        case 404 -> "No se encontraron datos para el DNI proporcionado";
                        case 503 -> "Servicio de RENIEC no disponible temporalmente";
                        default -> "Error al consultar datos de RENIEC: " + ex.getMessage();
                    };
                    return new RuntimeException(errorMessage);
                })
                .onErrorMap(Exception.class,
                        ex -> new RuntimeException("Error al consultar datos de RENIEC: " + ex.getMessage()));
    }
    
    private PersonalDataDto processReniecData(ReniecResponseDto reniecData) {
        PersonalDataDto personalData = new PersonalDataDto();

        personalData.setFirstName(reniecData.getFirstName());
        personalData.setFirstLastName(reniecData.getFirstLastName());
        personalData.setSecondLastName(reniecData.getSecondLastName());

        String fullLastName = reniecData.getFirstLastName();
        if (reniecData.getSecondLastName() != null && !reniecData.getSecondLastName().trim().isEmpty()) {
            fullLastName += " " + reniecData.getSecondLastName();
        }
        personalData.setLastName(fullLastName);

        personalData.setFullName(reniecData.getFullName());
        personalData.setDocumentNumber(reniecData.getDocumentNumber());

        String generatedUsername = generateUsername(reniecData.getFirstName(), reniecData.getFirstLastName());
        personalData.setGeneratedUsername(generatedUsername);

        return personalData;
    }

    private String generateUsername(String firstName, String firstLastName) {
        if (firstName == null || firstName.trim().isEmpty() ||
                firstLastName == null || firstLastName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre y apellido son requeridos para generar el username");
        }

        String cleanFirstName = firstName.trim().toUpperCase();
        String firstLetter = cleanFirstName.substring(0, 1);

        String cleanLastName = firstLastName.trim().toLowerCase();
        String capitalizedLastName = cleanLastName.substring(0, 1).toUpperCase() +
                cleanLastName.substring(1);

        return firstLetter + capitalizedLastName;
    }

}