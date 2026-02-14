package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Set;

@Table("users")
public class UserEntity implements org.springframework.data.domain.Persistable<String> {
    @Id
    private String id;
    private String username;
    private String password;

    @MappedCollection(idColumn = "user_id")
    private Set<UserRoleEntity> roles;

    @org.springframework.data.annotation.Transient
    private boolean newUser = true;

    public UserEntity() {
    }

    public UserEntity(String id, String username, String password, Set<UserRoleEntity> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newUser;
    }

    public void setNew(boolean newUser) {
        this.newUser = newUser;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<UserRoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserRoleEntity> roles) {
        this.roles = roles;
    }
}
