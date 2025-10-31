package pe.edu.vallegrande.vgmsusers.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users_demo")
@Builder
public class User {

    @Id
    private String id;
    private String userCode;
    private String username;
    private String organizationId;
    private PersonalInfo personalInfo;
    private Contact contact;
    private Set<RolesUsers> roles;
    private UserStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;

    /**
     * Verifica si el usuario está activo
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status) && this.deletedAt == null;
    }

    /**
     * Verifica si el usuario está eliminado (soft delete)
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Verifica si el usuario puede hacer login
     */
    public boolean canLogin() {
        return isActive() && (UserStatus.ACTIVE.equals(this.status) || UserStatus.PENDING.equals(this.status));
    }

    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean hasRole(RolesUsers role) {
        return this.roles != null && this.roles.contains(role);
    }

    /**
     * Verifica si el usuario tiene alguno de los roles especificados
     */
    public boolean hasAnyRole(RolesUsers... roles) {
        if (this.roles == null || this.roles.isEmpty()) {
            return false;
        }
        for (RolesUsers role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario es administrador (tiene rol ADMIN o SUPER_ADMIN)
     */
    public boolean isAdmin() {
        return hasAnyRole(RolesUsers.ADMIN, RolesUsers.SUPER_ADMIN);
    }

    /**
     * Verifica si el usuario es super administrador
     */
    public boolean isSuperAdmin() {
        return hasRole(RolesUsers.SUPER_ADMIN);
    }

    /**
     * Verifica si el usuario es cliente
     */
    public boolean isClient() {
        return hasRole(RolesUsers.CLIENT);
    }

    /**
     * Verifica si el usuario es operario
     */
    public boolean isOperator() {
        return hasRole(RolesUsers.OPERATOR);
    }
}