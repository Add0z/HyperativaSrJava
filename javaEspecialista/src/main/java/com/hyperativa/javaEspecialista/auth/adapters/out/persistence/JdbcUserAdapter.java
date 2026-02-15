package com.hyperativa.javaEspecialista.auth.adapters.out.persistence;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserRoleEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo.UserRepository;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JdbcUserAdapter
        implements LoadUserPort, SaveUserPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserAdapter.class);

    private final UserRepository userRepository;

    public JdbcUserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> loadUserByUsername(String username) {
        log.debug("Loading user by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username)
                .map(this::toDomain);
        if (user.isEmpty()) {
            log.warn("User not found: {}", username);
        } else {
            log.debug("User found: {}", username);
        }
        return user;
    }

    @Override
    public Optional<User> loadUserById(UUID id) {
        log.debug("Loading user by id: {}", id);
        Optional<User> user = userRepository.findById(id.toString())
                .map(this::toDomain);
        if (user.isEmpty()) {
            log.warn("User not found by id: {}", id);
        } else {
            log.debug("User found by id: {}", id);
        }
        return user;
    }

    @Override
    public void save(User user) {
        UserEntity entity = new UserEntity(
                user.id().toString(),
                user.username(),
                user.password(),
                user.roles().stream()
                        .map(role -> new UserRoleEntity(null, role.name()))
                        .collect(Collectors.toSet()));
        userRepository.save(entity);
        log.info("User details saved for: {}", user.username());
    }

    private User toDomain(UserEntity entity) {
        return new User(
                UUID.fromString(entity.getId()),
                entity.getUsername(),
                entity.getPassword(),
                entity.getRoles().stream()
                        .map(roleEntity -> Role.valueOf(roleEntity.getRole()))
                        .collect(Collectors.toSet()));
    }
}
