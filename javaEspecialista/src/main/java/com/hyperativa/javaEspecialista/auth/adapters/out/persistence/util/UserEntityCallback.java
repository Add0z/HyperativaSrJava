package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.util;

import org.springframework.data.relational.core.mapping.event.AfterConvertCallback;
import org.springframework.stereotype.Component;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;

@Component
public class UserEntityCallback implements AfterConvertCallback<UserEntity> {
    @Override
    public UserEntity onAfterConvert(UserEntity aggregate) {
        aggregate.setNew(false);
        return aggregate;
    }
}
