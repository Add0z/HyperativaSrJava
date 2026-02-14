package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_roles")
public class UserRoleEntity {
    @Id
    private Long id;
    private String role;

    public UserRoleEntity() {
    }

    public UserRoleEntity(Long id, String role) {
        this.id = id;
        this.role = role;
    }

    public UserRoleEntity(String role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
