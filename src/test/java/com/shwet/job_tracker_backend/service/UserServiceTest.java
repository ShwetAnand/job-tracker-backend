package com.shwet.job_tracker_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shwet.job_tracker_backend.dto.UserRequestDto;
import com.shwet.job_tracker_backend.dto.UserResponseDto;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.exception.EmailAlreadyExistsException;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.rag.ResumeVectorIndex;
import com.shwet.job_tracker_backend.repository.UserRepository;
import com.shwet.job_tracker_backend.service.impl.UserServiceImpl;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ResumeVectorIndex vectorIndex = mock(ResumeVectorIndex.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserServiceImpl service = new UserServiceImpl(userRepository, encoder, vectorIndex);

    private UserRequestDto request(String email) {
        return new UserRequestDto("  Jane ", email, "correct-horse-battery");
    }

    @Test
    void passwordIsStoredAsBcryptHashAndEmailIsNormalised() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponseDto response = service.registerUser(request("  Jane@Example.com "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertThat(saved.getValue().getPassword()).isNotEqualTo("correct-horse-battery").startsWith("$2");
        assertThat(encoder.matches("correct-horse-battery", saved.getValue().getPassword())).isTrue();
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Jane");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(request("JANE@example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void usersCannotReadUpdateOrDeleteOtherAccounts() {
        assertThatThrownBy(() -> service.getUser(1L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateUser(1L, 2L, request("x@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteUser(1L, 2L)).isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).delete(any());
        verifyNoInteractions(vectorIndex);
    }

    @Test
    void deletingOwnAccountAlsoRemovesEmbeddings() {
        User user = User.builder().id(1L).email("a@example.com").name("A").password("x").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.deleteUser(1L, 1L);

        verify(vectorIndex).deleteForUser(1L);
        verify(userRepository).delete(user);
    }
}
