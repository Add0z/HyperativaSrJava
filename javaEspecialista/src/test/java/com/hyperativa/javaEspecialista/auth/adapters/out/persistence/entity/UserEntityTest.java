package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void testUserEntityGettersAndSetters() {
        UserEntity user = new UserEntity();
        Set<UserRoleEntity> roles = new HashSet<>();
        roles.add(new UserRoleEntity("ROLE_USER"));

        user.setId("1");
        user.setUsername("user");
        user.setPassword("pass");
        user.setRoles(roles);
        user.setNew(false);

        assertEquals("1", user.getId());
        assertEquals("user", user.getUsername());
        assertEquals("pass", user.getPassword());
        assertEquals(roles, user.getRoles());
        assertFalse(user.isNew());
    }

    @Test
    void testUserEntityConstructor() {
        Set<UserRoleEntity> roles = new HashSet<>();
        roles.add(new UserRoleEntity("ROLE_USER"));
        UserEntity user = new UserEntity("1", "user", "pass", roles);

        assertEquals("1", user.getId());
        assertEquals("user", user.getUsername());
        assertEquals("pass", user.getPassword());
        assertEquals(roles, user.getRoles());
        assertTrue(user.isNew());
    }

    @Test
    void testUserRoleEntity() {
        UserRoleEntity role = new UserRoleEntity();
        role.setId(1L);
        role.setRole("ROLE_ADMIN");

        assertEquals(1L, role.getId());
        assertEquals("ROLE_ADMIN", role.getRole());

        UserRoleEntity role2 = new UserRoleEntity(2L, "ROLE_USER");
        assertEquals(2L, role2.getId());
        assertEquals("ROLE_USER", role2.getRole());

        UserRoleEntity role3 = new UserRoleEntity("ROLE_GUEST");
        assertEquals("ROLE_GUEST", role3.getRole());
    }
}
