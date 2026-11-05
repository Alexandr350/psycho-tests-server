package com.psychotests.psycho_tests_server.unit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

import com.psychotests.psycho_tests_server.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты SecurityConfig — проверяются только бины, не требующие
 * Spring-контекста: PasswordEncoder (BCrypt) и CorsConfigurationSource.
 *
 * <p>Что проверяем: пароль кодируется не в открытом виде, код совпадает
 * с исходным паролем при проверке, BCrypt генерирует новый соль на каждый
 * вызов (два кодирования одного пароля дают разные строки), длина хэша
 * влезает в колонку password_psychologist (length = 60); CORS-конфигурация
 * разрешает ровно заявленный origin и набор методов/заголовков, разрешает
 * credentials и применяется к любому пути.</p>
 *
 * <p>Зафиксированное решение: список разрешённых origin'ов сейчас содержит
 * только <a href="http://localhost:8080">...</a> — временная настройка разработки (TODO в
 * коде). Тест corsConfig фиксирует текущий список: при замене на
 * продовый origin тест перевернётся и напомнит, что изменение осознанное.</p>
 *
 * <p>Граница с другими тестами: SecurityFilterChain здесь не тестируется —
 * правила доступа (permitAll/authenticated, entryPoint с 401) проверяются
 * интеграционно через MockMvc вместе с тестами регистрации, поскольку
 * требуют поднятого контекста Spring Security.</p>
 */
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Nested
    class PasswordEncoderBean {

        private PasswordEncoder encoder;

        @BeforeEach
        void setUp() {
            encoder = securityConfig.passwordEncoder();
        }

        /** Кодирование не оставляет пароль в открытом виде. */
        @Test
        void encode_isNotRawPassword() {
            assertThat(encoder.encode("pwd")).isNotEqualTo("pwd");
        }

        /** Кодированный пароль подтверждается исходным. */
        @Test
        void encode_matchesRawPassword() {
            assertThat(encoder.matches("pwd", encoder.encode("pwd"))).isTrue();
        }

        /**
         * BCrypt добавляет случайный соль на каждый вызов: два кодирования
         * одного пароля дают разные строки. На этом держится защита от
         * радужных таблиц — и корректная работа matches.
         */
        @Test
        void encode_samePasswordDifferentHashes() {
            assertThat(encoder.encode("pwd"))
                    .isNotEqualTo(encoder.encode("pwd"));
        }

        /**
         * BCrypt-хэш всегда 60 символов — ровно под колонку
         * password_psychologist (length = 60). Если провайдер когда-нибудь
         * сменится на алгоритм с более длинным хэшем, тест упадёт до
         * DataIntegrityViolationException в проде.
         */
        @Test
        void encode_fitsColumnLength60() {
            assertThat(encoder.encode("pwd")).hasSize(60);
        }
    }

    @Nested
    class CorsConfig {

        private CorsConfiguration configuration;

        @BeforeEach
        void setUp() {
            var request = new MockHttpServletRequest();
            request.setRequestURI("/api/psychologists/register");
            configuration = securityConfig.corsConfigurationSource()
                    .getCorsConfiguration(request);
        }

        /** Конфигурация зарегистрирована для любого пути. */
        @Test
        void corsConfig_appliesToAnyPath() {
            assertThat(configuration).isNotNull();
        }

        /** Разрешён ровно заявленный origin разработки — см. Javadoc класса. */
        @Test
        void corsConfig_allowedOrigins() {
            assertThat(configuration.getAllowedOrigins())
                    .containsExactly("http://localhost:8080");
        }

        /** Методы — стандартный набор для REST без PATCH. */
        @Test
        void corsConfig_allowedMethods() {
            assertThat(configuration.getAllowedMethods())
                    .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        }

        /** Credentials разрешены — cookie-сессии с фронтенда работают. */
        @Test
        void corsConfig_allowCredentials() {
            assertThat(configuration.getAllowCredentials()).isTrue();
        }

        /** Заголовки — заявленный список, без подстановок. */
        @Test
        void corsConfig_allowedHeaders() {
            assertThat(configuration.getAllowedHeaders())
                    .containsExactlyInAnyOrder(
                            "Content-Type", "Authorization", "X-Requested-With",
                            "Accept", "Origin");
        }
    }
}
