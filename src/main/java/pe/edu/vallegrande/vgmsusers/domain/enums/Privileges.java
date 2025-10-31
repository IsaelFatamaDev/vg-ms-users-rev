package pe.edu.vallegrande.vgmsusers.domain.enums;

/**
 * Enumeración de privilegios específicos del sistema
 * Define acciones granulares que pueden realizar los usuarios
 */
public enum Privileges {

    // ========== PRIVILEGIOS DE GESTIÓN GENERAL (SUPER_ADMIN) ==========
    CREATE_SUPER_ADMIN("Crear otros super administradores"),
    CREATE_ADMIN("Crear usuarios administradores"),
    DELETE_ADMIN("Eliminar usuarios administradores"),
    VIEW_ADMIN("Ver usuarios administradores"),
    VIEW_ALL_ORGANIZATIONS("Ver todas las organizaciones"),
    MANAGE_SYSTEM_CONFIG("Gestionar configuración del sistema"),
    VIEW_SYSTEM_LOGS("Ver logs del sistema"),
    MANAGE_GLOBAL_SETTINGS("Gestionar configuraciones globales"),

    // ========== PRIVILEGIOS DE ADMINISTRACIÓN (ADMIN) ==========
    VIEW_ORGANIZATION_CLIENTS("Ver clientes de la organización"),
    VIEW_CLIENT_DETAILS("Ver detalles de clientes"),
    UPDATE_CLIENT_CONTACT("Actualizar contacto de clientes"),
    UPDATE_CLIENT_ADDRESS("Actualizar dirección de clientes"),
    CREATE_CLIENT("Crear usuarios cliente"),

    UPDATED_CLIENT_STATUS("Actualizar estado de clientes"),
    DELETE_CLIENT_LOGICAL("Eliminar lógicamente cliente"),
    RESTORE_CLIENT_LOGICAL("Restaurar lógicamente cliente"),
    DELETE_CLIENT_PHYSICAL("Eliminar físicamente cliente (dev)"),
    PARTIAL_UPDATE_CLIENT("Actualizar parcialmente cliente"),
    VIEW_ORGANIZATION_DATA("Ver datos de la organización"),
    MANAGE_ORGANIZATION_CONFIG("Gestionar configuración de organización"),
    VIEW_ORGANIZATION_REPORTS("Ver reportes de organización"),
    ASSIGN_WATER_BOXES("Asignar cajas de agua"),
    MANAGE_WATER_SERVICES("Gestionar servicios de agua"),

    // ========== PRIVILEGIOS DE CLIENTE (CLIENT) ==========
    VIEW_OWN_PROFILE("Ver perfil propio"),
    UPDATE_OWN_PROFILE("Actualizar perfil propio"),
    VIEW_OWN_ORGANIZATION("Ver organización propia"),
    VIEW_OWN_WATER_BOXES("Ver propias cajas de agua"),
    VIEW_OWN_BILLS("Ver propias facturas"),
    VIEW_OWN_CONSUMPTION("Ver propio consumo"),
    VIEW_OWN_REQUESTS("Ver propias solicitudes"),
    VIEW_OWN_SETTINGS("Ver propia configuración"),
    UPDATE_OWN_SETTINGS("Actualizar propia configuración"),
    CREATE_REQUESTS("Crear solicitudes"),
    MAKE_PAYMENTS("Realizar pagos"),
    REQUEST_SUPPORT("Solicitar soporte"),

    // ========== PRIVILEGIOS COMUNES ==========
    CHANGE_PASSWORD("Cambiar contraseña"),
    VIEW_NOTIFICATIONS("Ver notificaciones"),
    UPDATE_CONTACT_INFO("Actualizar información de contacto"),

    // ========== PRIVILEGIOS DE AUTENTICACIÓN ==========
    LOGIN("Iniciar sesión"),
    LOGOUT("Cerrar sesión"),
    REFRESH_TOKEN("Renovar token");

    private final String description;

    Privileges(String description) {
        this.description = description;
    }

    /**
     * Obtiene los privilegios por defecto para cada rol
     */
    public static Privileges[] getDefaultPrivilegesForRole(RolesUsers role) {
        return switch (role) {
            case SUPER_ADMIN -> new Privileges[] {
                    CREATE_SUPER_ADMIN, CREATE_ADMIN, CREATE_CLIENT, DELETE_ADMIN, VIEW_ADMIN,
                    VIEW_ALL_ORGANIZATIONS, MANAGE_SYSTEM_CONFIG, VIEW_SYSTEM_LOGS, MANAGE_GLOBAL_SETTINGS,
                    CHANGE_PASSWORD, VIEW_NOTIFICATIONS, UPDATE_CONTACT_INFO,
                    LOGIN, LOGOUT, REFRESH_TOKEN
            };

            case ADMIN -> new Privileges[] {
                    // Privilegios de administración (ADMIN)
                    VIEW_ORGANIZATION_CLIENTS, VIEW_CLIENT_DETAILS, UPDATE_CLIENT_CONTACT,
                    UPDATE_CLIENT_ADDRESS, DELETE_CLIENT_LOGICAL, RESTORE_CLIENT_LOGICAL,
                    DELETE_CLIENT_PHYSICAL, PARTIAL_UPDATE_CLIENT, VIEW_ORGANIZATION_DATA,
                    MANAGE_ORGANIZATION_CONFIG, VIEW_ORGANIZATION_REPORTS,
                    ASSIGN_WATER_BOXES, MANAGE_WATER_SERVICES,
                    // Privilegios propios (pueden ser clientes también)
                    VIEW_OWN_PROFILE, UPDATE_OWN_PROFILE,
                    CHANGE_PASSWORD, VIEW_NOTIFICATIONS, UPDATE_CONTACT_INFO,
                    LOGIN, LOGOUT, REFRESH_TOKEN
            };

            case CLIENT -> new Privileges[] {
                    VIEW_OWN_PROFILE, UPDATE_OWN_PROFILE, VIEW_OWN_WATER_BOXES,
                    VIEW_OWN_BILLS, VIEW_OWN_CONSUMPTION, VIEW_OWN_REQUESTS,
                    VIEW_OWN_SETTINGS, UPDATE_OWN_SETTINGS, CREATE_REQUESTS,
                    MAKE_PAYMENTS, REQUEST_SUPPORT, VIEW_OWN_ORGANIZATION,
                    CHANGE_PASSWORD, VIEW_NOTIFICATIONS, UPDATE_CONTACT_INFO,
                    LOGIN, LOGOUT, REFRESH_TOKEN
            };

            case OPERATOR -> new Privileges[] {
                    // Privilegios del operario - técnico con permisos específicos de operación
                    VIEW_ORGANIZATION_CLIENTS, VIEW_CLIENT_DETAILS, UPDATE_CLIENT_CONTACT,
                    VIEW_ORGANIZATION_DATA, VIEW_ORGANIZATION_REPORTS,
                    ASSIGN_WATER_BOXES, MANAGE_WATER_SERVICES,
                    // Privilegios propios
                    VIEW_OWN_PROFILE, UPDATE_OWN_PROFILE,
                    CHANGE_PASSWORD, VIEW_NOTIFICATIONS, UPDATE_CONTACT_INFO,
                    LOGIN, LOGOUT, REFRESH_TOKEN
            };
        };
    }

    public String getDescription() {
        return description;
    }
}