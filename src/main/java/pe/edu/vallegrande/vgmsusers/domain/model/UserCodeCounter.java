package pe.edu.vallegrande.vgmsusers.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad para manejar contadores de códigos de usuario por organización
 * Garantiza que cada organización tenga su propio contador secuencial
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_code_counters")
@Builder
public class UserCodeCounter {

     @Id
     private String id;

     private String organizationId;

     private Long lastCode;

     @Builder.Default
     private String prefix = "USR";

     public String generateNextCode() {
          this.lastCode = (this.lastCode == null) ? 1 : this.lastCode + 1;
          return String.format("%s%05d", prefix, this.lastCode);
     }

     public String getNextCode() {
          Long nextCode = (this.lastCode == null) ? 1 : this.lastCode + 1;
          return String.format("%s%05d", prefix, nextCode);
     }
}