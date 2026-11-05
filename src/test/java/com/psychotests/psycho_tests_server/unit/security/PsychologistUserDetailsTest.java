package com.psychotests.psycho_tests_server.unit.security;

import java.util.Collection;
import java.util.UUID;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты PsychologistUserDetails — обёртка UserDetails поверх
 * PsychologistAccount для Spring Security.
 *
 * <p>Что проверяем: делегирование полей UserDetails на аккаунт (пароль,
 * имя как username), пустой набор полномочий — и его неизменяемость,
 * поскольку Spring Security в некоторых сценариях (remapping authorities)
 * пытается мутировать коллекцию; доступ к исходному аккаунту через
 * getAccount() (используется сервисами для получения ID психолога);
 * дефолтные реализации UserDetails (isAccountNonExpired и соседние) —
 * они возвращают true по умолчанию, и именно на это полагается вся цепочка
 * аутентификации.</p>
 *
 * <p>Контракты, зафиксированные в тестах:
 * 1) username — это имя психолога (namePsychologist), а не email. Логин
 * в системе выполняется по email (уникальный констрейнт
 * uk_psychologist_email), но UserDetails отдаёт имя. Если вход когда-нибудь
 * переведут на email в этом методе, тест username_isNameNotEmail
 * перевернётся — и этим напомнит, что контракт изменился осознанно.
 * 2) Пароль обязателен: колонка password_psychologist — NOT NULL,
 * конструктор PsychologistAccount проверяет null через
 * Objects.requireNonNull. Поэтому null-сценарий в getPassword не тестируется:
 * валидный объект с null-паролем построить нельзя.</p>
 *
 * <p>Граница с другими тестами: механика аутентификации через этот класс
 * покрыта интеграционно — stranger-сценарий в CreateResearchFlowTest строит
 * UsernamePasswordAuthenticationToken прямо на нём. Здесь проверяется
 * только сам класс без Security-контекста. Тест password_nullRejectedByConstructor
 * частично принадлежит сущности PsychologistAccount — он здесь, потому что
 * фиксирует причину, по которой getPassword не бывает null.</p>
 */
class PsychologistUserDetailsTest {

    private PsychologistAccount account;
    private PsychologistUserDetails details;

    @BeforeEach
    void setUp() {
        account = new PsychologistAccount(
                UUID.randomUUID(), "Анна", "Смирнова", "pwd", "anna@example.com");
        details = new PsychologistUserDetails(account);
    }

    @Nested
    class Credentials {

        /** getPassword делегирует на passwordPsychologist. */
        @Test
        void password_delegatesToAccount() {
            assertThat(details.getPassword()).isEqualTo("pwd");
        }

        /**
         * Контракт конструктора: null-пароль отвергается сразу, с внятным
         * сообщением, а не откладывается до сохранения в БД (колонка
         * password_psychologist — NOT NULL, Objects.requireNonNull в
         * конструкторе). Тест стоит здесь, а не в тестах сущности:
         * он фиксирует основание, по которому getPassword не бывает null.
         */
        @Test
        void password_nullRejectedByConstructor() {
            assertThatThrownBy(() -> new PsychologistAccount(
                    UUID.randomUUID(), "Борис", "Орлов", null, "boris@example.com"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("пароль");
        }

        /**
         * Зафиксированный контракт: username — имя психолога, НЕ email.
         * Логин при этом по email (uk_psychologist_email) — расхождение
         * задокументировано в Javadoc класса.
         */
        @Test
        void username_isNameNotEmail() {
            assertThat(details.getUsername()).isEqualTo("Анна");
        }
    }

    @Nested
    class Authorities {

        /** Полномочий нет — роль пока не используется в системе. */
        @Test
        void authorities_empty() {
            assertThat(details.getAuthorities()).isEmpty();
        }


        /**
         * Неизменяемость: List.of возвращает недоступную для записи
         * коллекцию. Spring Security в некоторых сценариях (например,
         * remapping authorities в провайдере) пытается их мутировать —
         * иммутабельность защищает от скрытых изменений глобального
         * состояния. Компилятор не даёт вызвать add() через
         * wildcard-тип Collection<? extends GrantedAuthority>, поэтому
         * проверка идёт через raw-тип: дженерик стирается, и add(Object)
         * становится доступен. В рантайме это та же самая неизменяемая
         * коллекция из List.of().
         */
        @Test
        @SuppressWarnings({"rawtypes", "unchecked"})
        void authorities_immutable() {
            Collection authorities = details.getAuthorities();

            assertThatThrownBy(() -> authorities.add(new SimpleGrantedAuthority("ROLE_X")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }


    }

    @Nested
    class AccountAccess {

        /** getAccount отдаёт тот же экземпляр, что был передан конструктору. */
        @Test
        void account_sameInstance() {
            assertThat(details.getAccount()).isSameAs(account);
        }
    }

    @Nested
    class DefaultUserDetailsContract {

        /**
         * Дефолтные методы интерфейса UserDetails возвращают true — на этом
         * держится вся цепочка входа. Если кто-то однажды переопределит
         * их неправильно (например, isEnabled станет false), вход сломается
         * без единой ошибки в логах сервиса. Тест ловит это заранее.
         */
        @Test
        void accountStateFlags_defaultTrue() {
            assertThat(details.isAccountNonExpired()).isTrue();
            assertThat(details.isAccountNonLocked()).isTrue();
            assertThat(details.isCredentialsNonExpired()).isTrue();
            assertThat(details.isEnabled()).isTrue();
        }
    }
}
