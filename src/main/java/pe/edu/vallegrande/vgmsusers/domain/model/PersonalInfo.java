package pe.edu.vallegrande.vgmsusers.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsusers.domain.enums.DocumentType;

/**
 * Value Object para información personal del usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalInfo {
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String fullName;

    public String getFullDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        return null;
    }
}