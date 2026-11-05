package com.psychotests.psycho_tests_server.unit.exception;

import java.util.Map;
import java.util.Objects;

import com.psychotests.psycho_tests_server.exception.EmailAlreadyExistsException;
import com.psychotests.psycho_tests_server.exception.GlobalExceptionHandler;
import com.psychotests.psycho_tests_server.exception.InvalidCredentialsException;
import com.psychotests.psycho_tests_server.exception.ResearchNotFoundException;
import com.psychotests.psycho_tests_server.exception.SurveyNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import lombok.Getter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты GlobalExceptionHandler — центральный маппинг исключений на
 * HTTP-ответы. Хендлеры вызываются напрямую, без Spring-контекста:
 * проверяются статус и тело, а не диспетчеризация (её обеспечивает
 * аннотация RestControllerAdvice, а покрывается она @WebMvcTest-слоем).
 *
 * <p>Что проверяем: 401 с фиксированным сообщением независимо от текста
 * исключения (anti-enumeration — клиент не отличает «нет email» от
 * «неверный пароль»); 400 ProblemDetail на нечитаемый JSON; 409 с
 * сообщением исключения на занятый email и IllegalState (невозможный
 * переход статуса); 404 на оба not-found-исключения; 404 без тела на
 * отсутствующий статический ресурс (NoResourceFoundException не должен
 * проваливаться в общий Exception-хендлер и маскироваться под 500 —
 * именно так раньше скрывалась причина «файла нет»); 400 со списком
 * ошибок полей («поле: сообщение», через «; ») на валидацию;
 * ResponseStatusException — статус и reason пробрасываются как есть,
 * без reason — fallback на getMessage(); всё прочее — 500 с общим
 * сообщением без утечки внутренностей (детали уходят в лог).</p>
 *
 * <p>Зафиксированные решения:
 * 1) Фиксированное сообщение в 401 — защита от перечисления email.
 * 2) Фиксированные сообщения (401, notReadable, fallback валидации,
 * 500) — на английском; сообщения из исключений — на их родном языке.
 * 3) Фиксированное сообщение в 500 — внутренние детали не утекают клиенту.
 * 4) Три формы тела ответа сосуществуют (ErrorResponse / ProblemDetail /
 * Map) — поведение зафиксировано как есть; при унификации на ErrorResponse
 * тесты notReadable и responseStatus переворачиваются и напомнят.
 * 5) Специфичность выбора хендлера (ResponseStatusException и
 * NoResourceFoundException не попадают в Exception-ловушку) — зона
 * ответственности диспетчера Spring, не юнита.</p>
 *
 * <p>Граница с другими тестами: сквозная маршрутизация исключений из
 * контроллеров (какой хендлер получит какое исключение из какого метода)
 * — @WebMvcTest; поведение бизнес-исключений внутри сервисов — их юниты.</p>
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    /** Копеечный бин для MethodArgumentNotValidException: два поля, оба финальные. */
    @Getter
    static class SampleForm {
        private final String email = "";
        private final String password = "";
    }

    /** Метод-носитель для MethodParameter валидационного исключения. */
    @SuppressWarnings("unused")
    private static void validationTarget(String email) {
    }

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    class InvalidCredentials {

        /**
         * 401 с фиксированным сообщением: текст исключения намеренно
         * игнорируется — клиент не может перечислять существующие email,
         * отличая «пользователя нет» от «пароль неверен».
         */
        @Test
        void returns401_fixedMessageEvenWhenExceptionExplains() {
            var ex = new InvalidCredentialsException("Пользователь ivan@mail.ru не найден в базе");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleInvalidCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Invalid email or password")
                    .doesNotContain("ivan@mail.ru");
        }
    }

    @Nested
    class NotReadable {

        /** Битый JSON / неизвестные поля / битый enum — 400 в форме ProblemDetail. */
        @Test
        void returns400_problemDetail() {
            var ex = new HttpMessageNotReadableException(
                    "JSON parse error", new MockHttpInputMessage(new byte[0]));

            ProblemDetail detail = handler.handleNotReadable(ex);

            assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(detail.getDetail()).isEqualTo("Malformed request body");
        }
    }

    @Nested
    class EmailExists {

        /** Занятый email — 409 с сообщением исключения (оно рассчитано на клиента). */
        @Test
        void returns409_withExceptionMessage() {
            var ex = new EmailAlreadyExistsException("Email уже зарегистрирован");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleEmailExists(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Email уже зарегистрирован");
        }
    }

    @Nested
    class NotFound {

        /** Нет исследования — 404 с сообщением исключения. */
        @Test
        void researchNotFound_returns404() {
            var ex = new ResearchNotFoundException("Research не найден");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Research не найден");
        }

        /** Нет опроса — тот же хендлер, тот же 404. */
        @Test
        void surveyNotFound_returns404() {
            var ex = new SurveyNotFoundException("Survey не найден");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Survey не найден");
        }
    }

    @Nested
    class InvalidTransition {

        /** Невозможный переход статуса (IllegalState из сервисов) — 409, не 500. */
        @Test
        void returns409_withExceptionMessage() {
            var ex = new IllegalStateException("Нельзя вернуться из статуса FINISHED");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleInvalidTransition(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Нельзя вернуться из статуса FINISHED");
        }
    }

    @Nested
    class NoResourceFound {

        /**
         * Отсутствующий статический ресурс — честная 404 без тела. Ключевое
         * свойство: исключение НЕ проваливается в общий Exception-хендлер
         * (иначе маскировка под 500, на которой мы уже обжигались — см.
         * историю с 500 на js-модули). Проверяется и статус, и отсутствие
         * тела: любой body здесь был бы признаком неверного маппинга.
         */
        @Test
        void missingStaticResource_returns404WithoutBody() {
            var ex = new NoResourceFoundException(
                    org.springframework.http.HttpMethod.GET,
                    "ResearchPage/js/modularWindows/setupModal.js", null);

            ResponseEntity<Void> response = handler.handleNoResource(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    class Validation {

        /**
         * Ошибки полей склеиваются в «поле: сообщение; поле: сообщение» —
         * фронт получает всё за один запрос, а не по одной ошибке.
         * Тексты сообщений полей приходят из аннотаций DTO — их язык
         * хендлеру не принадлежит, тест принимает любые.
         */
        @Test
        void returns400_joinedFieldErrors() throws NoSuchMethodException {
            var bindingResult = new BeanPropertyBindingResult(new SampleForm(), "form");
            bindingResult.rejectValue("email", "NotBlank", "не должен быть пустым");
            bindingResult.rejectValue("password", "Size", "от 8 до 64 символов");

            var parameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class), 0);
            var ex = new MethodArgumentNotValidException(parameter, bindingResult);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .contains("email: не должен быть пустым")
                    .contains("password: от 8 до 64 символов")
                    .contains("; ");
        }
    }

    @Nested
    class ResponseStatus {

        /** Status + reason пробрасываются как есть — форма Map с ключом message. */
        @Test
        void statusAndReason_passedThrough() {
            var ex = new ResponseStatusException(HttpStatus.CONFLICT, "Код уже использован");

            ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(Objects.requireNonNull(response.getBody()).get("message"))
                    .isEqualTo("Код уже использован");
        }

        /** Без reason — fallback на getMessage() (не пустой, не null). */
        @Test
        void noReason_fallsBackToMessage() {
            var ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);

            ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(Objects.requireNonNull(response.getBody()).get("message"))
                    .isNotNull()
                    .isNotEqualTo("");
        }
    }

    @Nested
    class Unexpected {

        /**
         * Всё прочее — 500 с общим сообщением: настоящая причина (детали
         * БД, стек) не утекает клиенту, только в лог.
         */
        @Test
        void returns500_genericMessageWithoutLeak() {
            var ex = new RuntimeException("jdbc:secret-host:3306/db — доступ запрещён");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleUnexpected(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Internal server error")
                    .doesNotContain("jdbc")
                    .doesNotContain("secret-host");
        }
    }

    @Nested
    class DataIntegrity {

        /**
         * Нарушение ограничений БД — 409 с обобщённым сообщением: по
         * DataIntegrityViolationException нельзя надёжно определить,
         * какое ограничение сработало (уникальный ключ, FK, NOT NULL),
         * поэтому текст не привязан к конкретному случаю. Реальная
         * причина уходит в лог (log.warn), а не клиенту.
         */
        @Test
        void returns409_genericMessageWithoutLeak() {
            var ex = new DataIntegrityViolationException(
                    "Duplicate entry ... for key 'survey.UKt8o9pju3a6qydoitjf6et2u4l'");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleDataIntegrity(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(Objects.requireNonNull(response.getBody()).message())
                    .isEqualTo("Data conflict: the resource already exists or violates constraints")
                    .doesNotContain("survey.UKt8o9pju3a6qydoitjf6et2u4l");
        }
    }

}
