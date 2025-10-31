package pe.edu.vallegrande.vgmsusers.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pe.edu.vallegrande.vgmsusers.domain.enums.Privileges;
import pe.edu.vallegrande.vgmsusers.domain.enums.RolesUsers;
import pe.edu.vallegrande.vgmsusers.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad de credenciales de autenticación para RBAC
 * Almacena información de autenticación y roles de usuarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "auth_credentials")
public class AuthCredential {

    @Id
    private String authCredentialId;
    private String userId;
    private String username;
    private String passwordHash;
    private List<RolesUsers> roles;
    private List<Privileges> privileges;
    private UserStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordChangedAt;
    private boolean mustChangePassword;

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    public boolean isLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean hasRole(RolesUsers role) {
        return this.roles != null && this.roles.contains(role);
    }

    public boolean canViewAdmins() {
        return hasRole(RolesUsers.SUPER_ADMIN);
    }

    public boolean canManageClients() {
        return hasRole(RolesUsers.ADMIN) || hasRole(RolesUsers.SUPER_ADMIN);
    }


    public boolean isClient() {
        return hasRole(RolesUsers.CLIENT);
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;

        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasPrivilege(Privileges privilege) {
        if (this.privileges != null && this.privileges.contains(privilege)) {
            return true;
        }

        if (this.roles != null) {
            for (RolesUsers role : this.roles) {
                Privileges[] rolePrivileges = Privileges.getDefaultPrivilegesForRole(role);
                for (Privileges rolePrivilege : rolePrivileges) {
                    if (rolePrivilege.equals(privilege)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public List<Privileges> getAllPrivileges() {
        List<Privileges> allPrivileges = new java.util.ArrayList<>();

        if (this.privileges != null) {
            allPrivileges.addAll(this.privileges);
        }

        if (this.roles != null) {
            for (RolesUsers role : this.roles) {
                Privileges[] rolePrivileges = Privileges.getDefaultPrivilegesForRole(role);
                for (Privileges privilege : rolePrivileges) {
                    if (!allPrivileges.contains(privilege)) {
                        allPrivileges.add(privilege);
                    }
                }
            }
        }

        return allPrivileges;
    }

    public void addPrivilege(Privileges privilege) {
        if (this.privileges == null) {
            this.privileges = List.of(privilege);
        } else if (!this.privileges.contains(privilege)) {
            this.privileges = List.copyOf(this.privileges);
            this.privileges.add(privilege);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void addRole(RolesUsers role) {
        if (this.roles == null) {
            this.roles = List.of(role);
        } else if (!this.roles.contains(role)) {
            this.roles = List.copyOf(this.roles);
            this.roles.add(role);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removeRole(RolesUsers role) {
        if (this.roles != null && this.roles.contains(role)) {
            this.roles = this.roles.stream()
                    .filter(r -> !r.equals(role))
                    .toList();
        }
        this.updatedAt = LocalDateTime.now();
    }
}