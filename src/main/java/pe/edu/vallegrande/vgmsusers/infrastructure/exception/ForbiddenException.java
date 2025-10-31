package pe.edu.vallegrande.vgmsusers.infrastructure.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends CustomException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode, 403);
    }
}