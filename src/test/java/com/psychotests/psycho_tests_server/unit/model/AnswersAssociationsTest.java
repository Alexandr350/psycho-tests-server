package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты AnswersAssociations — "ассоциации" - «ответ респондента»,
 * связывающая ответ с сессией опроса (ManyToOne на SurveySession).
 *
 * <p>Что проверяем: контракты конструктора — id, сессия и текст ответа
 * обязательны (Objects.requireNonNull с англоязычными сообщениями,
 * в отличие от PsychologistAccount, где сообщения на русском); делегирование
 * геттеров на переданные значения; равенство только по id
 * (onlyExplicitlyIncluded) — два ответа с разными полями, но одним id,
 * считаются одним и тем же объектом; дефолтные значения int-полей при
 * Lombok-конструкторе без аргументов.</p>
 *
 * <p>Зафиксированные решения:
 * 1) numberSector и associationOrder — примитивные int без проверок:
 * null невозможен по типу, а отрицательные значения на уровне сущности
 * не отвергаются — валидация диапазона живёт в DTO/сервисе (если появится).
 * 2) Пустая строка в answer конструктором принимается: @NotBlank
 * ожидается на уровне DTO — сущность не дублирует валидацию.
 * 3) Длина answer не ограничена конструктором, хотя колонка — length = 500:
 * переполнение поймает Hibernate при сохранении; ограничение в конструкторе
 * не добавляем, пока продукт не решит иначе.</p>
 *
 * <p>Граница с другими тестами: связь с SurveySession тестируется здесь
 * только как ссылка (isSameAs/requireNonNull) — целостность ассоциаций
 * (join-колонка id_survey_session, @OnDelete(CASCADE) на FK сессии — сессия
 * сносит ответы, равно как и удаления по цепочке research → survey →
 * session) покрывается @DataJpaTest, который мы запланировали вместе
 * с SurveySessionRepository. NoArgsConstructor не тестируется как контракт —
 * он существует для JPA и не используется в прикладном коде; тест below-only
 * фиксирует, что id у него остаётся null.</p>
 */
class AnswersAssociationsTest {

    private SurveySession session;
    private UUID id;

    @BeforeEach
    void setUp() {
        // Сессия строится продакшен-конструктором (как в сервисах):
        // не через JPA-конструктор, чтобы тест не зависел от дефолтного статуса
        session = new SurveySession(UUID.randomUUID());
        id = UUID.randomUUID();
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var answer = new AnswersAssociations(
                    id, session, "море", 3, 2);

            assertThat(answer.getIdAnswersAssociations()).isEqualTo(id);
            assertThat(answer.getSurveySession()).isSameAs(session);
            assertThat(answer.getAnswer()).isEqualTo("море");
            assertThat(answer.getNumberSector()).isEqualTo(3);
            assertThat(answer.getAssociationOrder()).isEqualTo(2);
        }

        /** id обязателен — null отвергается с сообщением про idAnswersAssociations. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new AnswersAssociations(
                    null, session, "море", 1, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idAnswersAssociations");
        }

        /** Сессия обязательна — ответ не существует вне сессии опроса. */
        @Test
        void surveySession_nullRejected() {
            assertThatThrownBy(() -> new AnswersAssociations(
                    id, null, "море", 1, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("surveySession");
        }

        /** Текст ответа обязателен — null отвергается. */
        @Test
        void answer_nullRejected() {
            assertThatThrownBy(() -> new AnswersAssociations(
                    id, session, null, 1, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("answer");
        }

        /**
         * Зафиксированное решение: пустая строка принимается конструктором.
         * Не-пустота проверяется на уровне DTO (@NotBlank в запросе регистрации
         * — аналогично для ответов); сущность не дублирует валидацию.
         * Если решите добавить requireNonNull-стиль проверку на blank —
         * тест перевернётся и напомнит.
         */
        @Test
        void answer_emptyStringAllowed() {
            var answer = new AnswersAssociations(id, session, "", 1, 1);
            assertThat(answer.getAnswer()).isEmpty();
        }

        /**
         * Зафиксированное решение: отрицательные numberSector/associationOrder
         * на уровне сущности не отвергаются — диапазон валидируется выше по стеку.
         */
        @Test
        void negativeNumbers_notRejectedByEntity() {
            var answer = new AnswersAssociations(id, session, "море", -1, -1);
            assertThat(answer.getNumberSector()).isEqualTo(-1);
            assertThat(answer.getAssociationOrder()).isEqualTo(-1);
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два ответа с одинаковым id, но разными
         * полями — это один и тот же объект. onlyExplicitlyIncluded в
         * @EqualsAndHashCode гарантирует, что поля не протекут в equals.
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new AnswersAssociations(id, session, "море", 1, 1);
            var b = new AnswersAssociations(id, session, "лес", 5, 9);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new AnswersAssociations(id, session, "море", 1, 1);
            var b = new AnswersAssociations(
                    UUID.randomUUID(), session, "море", 1, 1);

            assertThat(a).isNotEqualTo(b);
        }

        /** equals симметричен по типу: с чужим классом не равен. */
        @Test
        void equals_otherType_false() {
            var answer = new AnswersAssociations(id, session, "море", 1, 1);
            Object stranger = "не ответ";
            assertThat(answer).isNotEqualTo(stranger);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) создаёт объект с null-id — провайдер
         * заполнит его сам при persist. Тест фиксирует, что конструктор
         * не содержит скрытой логики: id не генерируется на месте.
         */
        @Test
        void noArgsConstructor_idStaysNull() {
            assertThat(new AnswersAssociations().getIdAnswersAssociations())
                    .isNull();
        }

        /** Сеттеры Lombok работают — включая сеттер на сессию. */
        @Test
        void setters_updateFields() {
            var answer = new AnswersAssociations(
                    id, session, "море", 1, 1);
            var otherSession = new SurveySession(UUID.randomUUID());

            answer.setAnswer("лес");
            answer.setNumberSector(4);
            answer.setAssociationOrder(7);
            answer.setSurveySession(otherSession);

            assertThat(answer.getAnswer()).isEqualTo("лес");
            assertThat(answer.getNumberSector()).isEqualTo(4);
            assertThat(answer.getAssociationOrder()).isEqualTo(7);
            assertThat(answer.getSurveySession()).isSameAs(otherSession);
        }
    }
}
