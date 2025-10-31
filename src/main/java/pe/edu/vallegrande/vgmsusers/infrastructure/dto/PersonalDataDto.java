package pe.edu.vallegrande.vgmsusers.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de datos personales procesados desde RENIEC
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDataDto {
    private String firstName;
    private String firstLastName;
    private String secondLastName;
    private String lastName;
    private String fullName;
    private String documentNumber;
    private String generatedUsername;
}