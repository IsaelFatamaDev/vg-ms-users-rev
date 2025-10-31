package pe.edu.vallegrande.vgmsusers.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value Object para información de contacto del usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {
    private String phone;
    private String email;
    private AddressUsers address;

    public boolean hasBasicInfo() {
        return (email != null && !email.trim().isEmpty()) ||
                (phone != null && !phone.trim().isEmpty());
    }
}