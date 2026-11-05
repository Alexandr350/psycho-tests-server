package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.exception.UnauthorizedException;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import com.psychotests.psycho_tests_server.service.CurrentPsychologistProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты CurrentPsychologistProvider.
 *
 * <p>Что проверяем: метод getPsychologistAccount() — единую точку доступа
 * к аккаунту текущего психолога для всего сервера. Сервис-методы, которым
 * нужен владелец сущности (создание research, доступ к survey и т. д.),
 * вызывают этот провайдер и получают аккаунт из контекста безопасности.
 * Здесь фиксируются все четыре ветки его контракта: пустой контекст,
 * неаутентифицированный запрос, principal чужого типа (например,
 * анонимный доступ, где principal — строка) и happy path с возвратом
 * аккаунта из PsychologistUserDetails.</p>
 *
 * <p>Как работает: Spring-контекст не поднимается. Провайдер создаётся
 * напрямую (new CurrentPsychologistProvider()), зависимостей у него нет.
 * Вместо моков контекста безопасности — реальный SecurityContextHolder:
 * в каждый тест кладётся заранее собранный Authentication, а после теста
 * контекст очищается в @AfterEach (SecurityContextHolder — глобальное
 * состояние потока; без очистки контекст протекал бы между тестами,
 * и результат зависел бы от порядка их выполнения). Разные ветки входа
 * задаются разными видами Authentication: мок для «неаутентифицирован»
 * и «principal чужого типа», реальный UsernamePasswordAuthenticationToken
 * с настоящим PsychologistUserDetails для happy path. Правила того, как
 * Authentication вообще попадает в контекст (фильтры Spring Security,
 * JWT и т. п.) — за пределами юнита: их место в тестах security-конфигурации.</p>
 *
 * <p>Граница с другими тестами: ветки отказов здесь — про форму входных
 * данных контекста, а не про авторизацию в целом. Проверки того, кому
 * какая сущность принадлежит (403/404), — в ResearchAccessServiceTest;
 * проверки цепочки «запрос → фильтры → контекст» — в интеграционных
 * тестах security-конфигурации.</p>
 *
 * <p>Детали: PsychologistUserDetails в happy path — реальный объект,
 * а не мок: это простой носитель аккаунта, и прохождение через него
 * честнее, чем стаб. Из-за глобального состояния SecurityContextHolder
 * тесты не следует запускать в одном потоке с параллельной конфигурацией
 * без изоляции (по умолчанию JUnit выполняет тесты последовательно —
 * этого достаточно).</p>
 */

class CurrentPsychologistProviderTest {

    private final CurrentPsychologistProvider provider = new CurrentPsychologistProvider();

    /**
     * SecurityContextHolder — глобальное состояние потока: после каждого теста
     * очищаем его, иначе контекст протечёт в другие тесты и порядок выполнения
     * начнёт влиять на результат.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** Подменяет authentication в контексте безопасности текущего потока. */
    private void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    class GetPsychologistAccount {

        /**
         * Нет authentication вообще (контекст пуст) — запрос не проходил
         * через цепочку фильтров аутентификации. Ожидаем отказ.
         */
        @Test
        void noAuthentication_throwsUnauthorized() {
            // контекст пуст: setUp ничего не кладёт, clearContext() после прошлого теста

            assertThatThrownBy(provider::getPsychologistAccount)
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Пользователь не авторизован");
        }

        /**
         * authentication есть, но не аутентифицирован — редкая ветка для
         * продакшена (фильтры обычно режут раньше), но контракт метода
         * требует отказ, и тест её фиксирует.
         */
        @Test
        void notAuthenticated_throwsUnauthorized() {
            var authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(false);
            setAuthentication(authentication);

            assertThatThrownBy(provider::getPsychologistAccount)
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Пользователь не авторизован");
        }

        /**
         * Principal не PsychologistUserDetails. Типичный продакшен-случай:
         * анонимный доступ, где principal — строка "anonymousUser". Сюда же
         * попадает любой другой тип principal (админская сессия, system-вызовы).
         */
        @Test
        void principalOfAnotherType_throwsUnauthorized() {
            var authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            setAuthentication(authentication);

            assertThatThrownBy(provider::getPsychologistAccount)
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Нет аккаунта");
        }

        /**
         * Happy path: principal — PsychologistUserDetails, метод возвращает
         * именно тот аккаунт, что лежит в details (isSameAs — без пересборки).
         * PsychologistUserDetails конструируется реальный, а не мок: он —
         * простой носитель аккаунта, и проверка через него честнее, чем stub.
         * // FIXME: сверьте конструктор с вашей реализацией UserDetails
         */
        @Test
        void returnsAccountFromUserDetails() {
            var account = new PsychologistAccount(
                    UUID.randomUUID(), "Анна", "Иванова", "$2a$10$hashedvalue", "psy@mail.ru");
            var userDetails = new PsychologistUserDetails(account);
            setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));

            var result = provider.getPsychologistAccount();

            assertThat(result).isSameAs(account);
        }
    }


    /**
     * Реальный анонимный токен Spring: isAuthenticated() у него true,
     * поэтому первую проверку провайдера он проходит и отсекается
     * только проверкой типа principal. В отличие от мока выше,
     * здесь поведение проверяется на настоящем объекте, которым
     * Spring подкладывает неаутентифицированные запросы.
     */
    @Test
    void anonymousToken_throwsUnauthorized() {
        setAuthentication(new AnonymousAuthenticationToken(
                "anonymous", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(provider::getPsychologistAccount)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Нет аккаунта");
    }
}
