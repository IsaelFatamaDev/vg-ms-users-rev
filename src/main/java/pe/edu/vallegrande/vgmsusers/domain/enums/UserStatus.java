package pe.edu.vallegrande.vgmsusers.domain.enums;

/**
 * Enumeración que define los estados de usuario en el sistema
 */
public enum UserStatus {
    ACTIVE("ACTIVE", "Usuario activo"),
    INACTIVE("INACTIVE", "Usuario inactivo"),
    SUSPENDED("SUSPENDED", "Usuario suspendido"),
    PENDING("PENDING", "Usuario pendiente de activación");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Obtiene el estado por código
     */
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + code);
    }
}