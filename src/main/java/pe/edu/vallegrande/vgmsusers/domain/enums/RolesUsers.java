package pe.edu.vallegrande.vgmsusers.domain.enums;

public enum RolesUsers {
    SUPER_ADMIN("SUPER_ADMIN", "Administrador del sistema - gestiona organizaciones y administradores"),
    ADMIN("ADMIN", "Administrador de organización - gestiona clientes de su organización"),
    OPERATOR("OPERATOR", "Operario - usuario técnico con permisos específicos de operación"),
    CLIENT("CLIENT", "Cliente - usuario final con acceso limitado a sus datos");

    private final String code;
    private final String description;

    RolesUsers(String code, String description) {
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
     * Obtiene el rol por código
     */
    public static RolesUsers fromCode(String code) {
        for (RolesUsers role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + code);
    }
}