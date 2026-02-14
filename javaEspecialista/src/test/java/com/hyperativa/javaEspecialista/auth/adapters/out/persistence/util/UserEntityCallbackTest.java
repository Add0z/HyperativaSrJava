package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.util;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityCallbackTest {

    @Test
    void shouldSetNewToFalseAfterConvert() {
        UserEntityCallback callback = new UserEntityCallback();
        UserEntity entity = new UserEntity();
        entity.setNew(true);

        UserEntity result = callback.onAfterConvert(entity);

        assertFalse(result.isNew(), "Entity should not be marked as new after convert");
        assertSame(entity, result, "The callback should return the same instance");
    }
}
