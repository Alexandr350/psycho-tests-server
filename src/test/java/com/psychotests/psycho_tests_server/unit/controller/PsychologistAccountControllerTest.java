package com.psychotests.psycho_tests_server.unit.controller;

import com.psychotests.psycho_tests_server.controller.PsychologistAccountController;
import com.psychotests.psycho_tests_server.dto.request.authorization_dto.LoginPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.request.authorization_dto.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.response.authorization_dto.LoginPsychologicalAccountResponseDto;
import com.psychotests.psycho_tests_server.dto.response.authorization_dto.RegisteringPsychologicalAccountResponseDto;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import com.psychotests.psycho_tests_server.service.PsychologistAccountService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Юнит-тест PsychologistAccountController без Spring-контекста: делегирование
 * в сервис, статус-коды, тело через фабрику DTO и логика
 * authenticateUserInSession — инвалидация старой сессии до входа (session
 * fixation) и запись SecurityContext в holder и в НОВУЮ сессию.
 *
 * <p>Важно: после вызова контроллера контекст лежит в сессии, которую
 * контроллер создал заново, — её берём у request, а не по ссылке из setUp
 * (та инвалидируется контроллером).</p>
 */
@ExtendWith(MockitoExtension.class)
class PsychologistAccountControllerTest {

    @Mock
    private PsychologistAccountService service;

    private PsychologistAccountController controller;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new PsychologistAccountController(service);
        session = new MockHttpSession();
        request.setSession(session);
    }

    @AfterEach
    void clearSecurityContext() {
        // Контекст живёт в статическом поле потока — без очистки он протечёт в соседний тест
        SecurityContextHolder.clearContext();
    }

    /** Сессия, в которую контроллер записал контекст (после инвалидации старой). */
    private MockHttpSession currentSession() {
        return (MockHttpSession) request.getSession();
    }


    private SecurityContext storedContext() {
        return (SecurityContext) currentSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }

    private PsychologistAccount newAccount() {
        return new PsychologistAccount(
                UUID.randomUUID(),
                "Анна",
                "Тестовая",
                "$2a$10$" + "x".repeat(53),
                "psychologist-" + UUID.randomUUID() + "@test.local");
    }

    // ---------- register ----------

    @Test
    void registerReturnsCreatedAndAuthenticatesSession() {
        var dto = mock(RegisteringPsychologicalAccountRequestDto.class);
        var account = newAccount();
        when(service.createAccountPsychologist(dto)).thenReturn(account);

        var response = controller.register(dto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .isEqualTo(RegisteringPsychologicalAccountResponseDto.from(account));

        // Сессия: старая уничтожена, контекст — в новой
        assertThat(session.isInvalid()).isTrue();
        assertThat(currentSession()).isNotSameAs(session);
        assertThat(storedContext()).isSameAs(SecurityContextHolder.getContext());

        // В holder: токен, аутентифицирован, пароля в токене нет
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    assertThat(auth.getCredentials()).isNull();
                    assertThat(auth.getPrincipal()).isInstanceOf(PsychologistUserDetails.class);
                });
    }

    // ---------- login ----------

    @Test
    void loginReturnsOkAndAuthenticatesSession() {
        var dto = mock(LoginPsychologicalAccountRequestDto.class);
        var account = newAccount();
        when(service.authorizationAccount(dto)).thenReturn(account);

        var response = controller.login(dto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isEqualTo(LoginPsychologicalAccountResponseDto.from(account));
        verify(service).authorizationAccount(dto);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(storedContext()).isSameAs(SecurityContextHolder.getContext());
    }

    /**
     * Session fixation: подсунутая заранее сессия (cookie) уничтожена до
     * записи нового контекста; контекст оказался в другой, новой сессии.
     */
    @Test
    void loginInvalidatesExistingSessionBeforeWritingContext() {
        var dto = mock(LoginPsychologicalAccountRequestDto.class);
        when(service.authorizationAccount(dto)).thenReturn(newAccount());

        var oldSession = new MockHttpSession();
        request.setSession(oldSession);   // симулируем «подсунутую» сессию

        controller.login(dto, request);

        assertThat(oldSession.isInvalid()).isTrue();
        assertThat(currentSession()).isNotSameAs(oldSession);
        assertThat(storedContext()).isNotNull();
    }

    /** Контроллер не глотает ошибки сервиса — их маппит advice наверху. */
    @Test
    void loginPropagatesServiceException() {
        var dto = mock(LoginPsychologicalAccountRequestDto.class);
        when(service.authorizationAccount(dto))
                .thenThrow(new BadCredentialsException("Неверный пароль"));

        assertThatThrownBy(() -> controller.login(dto, request))
                .isInstanceOf(BadCredentialsException.class);

        // Провалившийся вход не оставляет контекст в holder
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
