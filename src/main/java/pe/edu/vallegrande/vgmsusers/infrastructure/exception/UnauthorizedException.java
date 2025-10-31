package pe.edu.vallegrande.vgmsusers.infrastructure.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends CustomException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode, 401);
    }
}