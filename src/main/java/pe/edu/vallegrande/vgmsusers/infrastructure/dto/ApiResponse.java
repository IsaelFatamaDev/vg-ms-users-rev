package pe.edu.vallegrande.vgmsusers.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta estándar para todos los endpoints
 * Incluye estado de éxito, datos y mensajes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorMessage error;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.message = success ? "Operación exitosa" : "Operación fallida";
        this.error = null;
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.error = null;
    }

    public ApiResponse(boolean success, ErrorMessage error) {
        this.success = success;
        this.data = null;
        this.error = error;
        this.message = error != null ? error.getMessage() : "Error desconocido";
    }


    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operación exitosa")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorMessage error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(error != null ? error.getMessage() : "Error desconocido")
                .error(error)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, ErrorMessage error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .build();
    }
}