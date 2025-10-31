package pe.edu.vallegrande.vgmsusers.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage {
    private String message;
    private String code;
    private String errorCode;
    private int httpStatus;
    private String details;
    private LocalDateTime timestamp;

    public ErrorMessage(String message, String errorCode, int httpStatus) {
    }
}