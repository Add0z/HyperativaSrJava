package com.hyperativa.javaEspecialista.auth.adapters.out.persistence;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserRoleEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo.UserRepository;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JdbcUserAdapterTest {

    @Mock
    private UserRepository userRepository;

    private JdbcUserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcUserAdapter(userRepository);
    }

    @Test
    void loadUserByUsername_WhenFound_ShouldReturnUser() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity(id.toString(), "user", "pass", Set.of(new UserRoleEntity(1L, "USER")));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(entity));

        Optional<User> result = adapter.loadUserByUsername("user");

        assertTrue(result.isPresent());
        assertEquals(id, result.get().id());
        assertEquals("user", result.get().username());
        assertEquals("pass", result.get().password());
        assertTrue(result.get().roles().contains(Role.USER));
    }

    @Test
    void loadUserByUsername_WhenNotFound_ShouldReturnEmpty() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        Optional<User> result = adapter.loadUserByUsername("user");

        assertTrue(result.isEmpty());
    }

    @Test
    void save_ShouldCallRepository() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "user", "pass", Set.of(Role.USER));

        adapter.save(user);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity captured = captor.getValue();
        assertEquals(id.toString(), captured.getId());
        assertEquals("user", captured.getUsername());
        assertEquals("pass", captured.getPassword());
        assertEquals(1, captured.getRoles().size());
        assertEquals("USER", captured.getRoles().iterator().next().getRole());
    }
}
