package com.psychotests.psycho_tests_server.unit.exception;

import com.psychotests.psycho_tests_server.exception.EmailAlreadyExistsException;
import com.psychotests.psycho_tests_server.exception.InvalidCredentialsException;
import com.psychotests.psycho_tests_server.exception.ResearchNotFoundException;
import com.psychotests.psycho_tests_server.exception.SurveyNotFoundException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контрактные тесты семейства бизнес-исключений. Логики в этих классах
 * нет — есть три работающих контракта: @ResponseStatus задаёт HTTP-код
 * (страховка на случай, если исключение уйдёт мимо GlobalExceptionHandler),
 * наследование от RuntimeException (unchecked — сервисы не обременены
 * throws) и проброс сообщения в getMessage() (на него опираются тесты
 * хендлера).
 *
 * <p>Каждая секция @Nested — одно исключение и его два контракта:
 * маппинг на HTTP-код и наследование с пробросом сообщения. Формат
 * одинаковый для всех секций: добавить новое исключение в семейство —
 * значит добавить секцию по этому же шаблону.</p>
 *
 * <p>Диспетчеризация исключений в хендлеры покрыта
 * GlobalExceptionHandlerTest; здесь — только сам класс исключения.</p>
 */
class ExceptionContractTest {

    /**
     * Достаёт аннотацию ResponseStatus с учётом мета-аннотаций:
     * возвращает null, если аннотация снята — тогда тест маппинга
     * упадёт с «Expecting actual not to be null», что и сигналит
     * о потери страховки (случай, который был с InvalidCredentials
     * и EmailAlreadyExists до добавления аннотаций).
     */
    private static ResponseStatus responseStatus(Class<?> exceptionClass) {
        return AnnotatedElementUtils.findMergedAnnotation(exceptionClass, ResponseStatus.class);
    }

    @Nested
    class ResearchNotFound {

        /**
         * Аннотация @ResponseStatus(NOT_FOUND) на месте: если хендлер
         * не поймает исключение, Spring вернёт 404, а не 500.
         */
        @Test
        void mapsTo404() {
            assertThat(responseStatus(ResearchNotFoundException.class))
                    .extracting(ResponseStatus::value)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        /**
         * Наследование от RuntimeException (unchecked — сервисы не
         * обременены throws) и проброс сообщения: getMessage()
         * возвращает ровно то, что передано конструктору, — на это
         * опирается тест handleNotFound в GlobalExceptionHandlerTest.
         */
        @Test
        void isRuntimeException_withMessagePropagated() {
            var ex = new ResearchNotFoundException("Research не найден");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("Research не найден");
        }
    }

    @Nested
    class SurveyNotFound {

        /** Та же страховка маппинга, что у ResearchNotFound: 404, не 500. */
        @Test
        void mapsTo404() {
            assertThat(responseStatus(SurveyNotFoundException.class))
                    .extracting(ResponseStatus::value)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        /** unchecked + проброс сообщения — контракт идентичен ResearchNotFound. */
        @Test
        void isRuntimeException_withMessagePropagated() {
            var ex = new SurveyNotFoundException("Survey не найден");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("Survey не найден");
        }
    }

    @Nested
    class InvalidCredentials {

        /**
         * Страховка маппинга: 401, не 500. Аннотация добавлена по итогам
         * прогона этой секции — до этого перехват держался только на
         * GlobalExceptionHandler, и уход исключения мимо хендлера дал бы
         * клиенту 500 вместо 401.
         */
        @Test
        void mapsTo401() {
            assertThat(responseStatus(InvalidCredentialsException.class))
                    .extracting(ResponseStatus::value)
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        /**
         * unchecked + проброс сообщения. Сам текст сообщения клиенту не
         * виден (хендлер отдаёт фиксированное «Неверный email или пароль»
         * — anti-enumeration), поэтому содержание здесь не проверяем —
         * только факт проброса.
         */
        @Test
        void isRuntimeException_withMessagePropagated() {
            var ex = new InvalidCredentialsException("Неверный email или пароль");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("Неверный email или пароль");
        }
    }

    @Nested
    class EmailExists {

        /** Страховка маппинга: 409, не 500 — по образцу InvalidCredentials. */
        @Test
        void mapsTo409() {
            assertThat(responseStatus(EmailAlreadyExistsException.class))
                    .extracting(ResponseStatus::value)
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        /**
         * unchecked + проброс сообщения. Тот факт, что сообщение доходит
         * до клиента, фиксирует GlobalExceptionHandlerTest.handleEmailExists
         * (хендлер пробрасывает getMessage() как есть) — здесь это не
         * проверяется.
         */

        @Test
        void isRuntimeException_withMessagePropagated() {
            var ex = new EmailAlreadyExistsException("Email уже зарегистрирован");

            assertThat(ex).isInstanceOf(RuntimeException.class);
            assertThat(ex.getMessage()).isEqualTo("Email уже зарегистрирован");
        }
    }
}
