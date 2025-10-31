package pe.edu.vallegrande.vgmsusers.infrastructure.exception;

import lombok.Getter;

@Getter
public class ValidationException extends CustomException {

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
    }

    public ValidationException(String message, String field) {
        super(String.format("Error de validación en campo '%s': %s", field, message), "VALIDATION_ERROR", 400);
    }

    public ValidationException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus);
    }
}