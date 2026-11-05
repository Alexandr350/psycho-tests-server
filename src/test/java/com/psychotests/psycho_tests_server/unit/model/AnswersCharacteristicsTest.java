package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты AnswersCharacteristics — ассоциация «ответ респондента —
 * характеристика»: количественный привязывает конкретную характеристику
 * к сессии опроса (два ManyToOne: SurveySession и Characteristics).
 *
 * <p>Что проверяем: контракты конструктора — id, сессия и характеристика
 * обязательны (Objects.requireNonNull с англоязычными сообщениями,
 * в том же стиле, что и AnswersAssociations); numberSector — примитивный
 * int без проверок диапазона: null невозможен по типу, отрицательные
 * значения на уровне сущности не отвергаются — валидация живёт в DTO/сервисе,
 * если появится; делегирование геттеров; равенство только по id
 * (только поле с аннотацией EqualsAndHashCode.Include); дефолтное поведение
 * NoArgsConstructor — id остаётся null, генерации на месте нет.</p>
 *
 * <p>Зафиксированное решение: текстового поля в сущности нет (в отличие от
 * AnswersAssociations) — вся содержательная часть живёт в Characteristics,
 * сущность хранит только связь и номер сектора. Тесты это отражают: здесь
 * некуда и нечего проверять на blank/длину.</p>
 *
 * <p>Граница с другими тестами: связи ManyToOne проверяются здесь только как
 * ссылки (isSameAs/requireNonNull) — целостность ассоциаций в БД (join-колонки
 * id_survey_session и id_characteristic) покрывается @DataJpaTest, который
 * запланирован вместе с SurveySessionRepository. Ссылки подменяются моками:
 * содержимое SurveySession и Characteristics этому классу безразлично.</p>
 */
class AnswersCharacteristicsTest {

    private UUID id;
    private SurveySession session;
    private Characteristics characteristic;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        session = mock(SurveySession.class);
        characteristic = mock(Characteristics.class);
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var answer = new AnswersCharacteristics(
                    id, session, characteristic, 3);

            assertThat(answer.getIdAnswersCharacteristic()).isEqualTo(id);
            assertThat(answer.getSurveySession()).isSameAs(session);
            assertThat(answer.getCharacteristic()).isSameAs(characteristic);
            assertThat(answer.getNumberSector()).isEqualTo(3);
        }

        /** id обязателен — null отвергается с сообщением про idAnswersCharacteristic. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new AnswersCharacteristics(
                    null, session, characteristic, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idAnswersCharacteristic");
        }

        /** Сессия обязательна — запись не существует вне сессии опроса. */
        @Test
        void surveySession_nullRejected() {
            assertThatThrownBy(() -> new AnswersCharacteristics(
                    id, null, characteristic, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("surveySession");
        }

        /** Характеристика обязательна — ответ всегда про конкретную характеристику. */
        @Test
        void characteristic_nullRejected() {
            assertThatThrownBy(() -> new AnswersCharacteristics(
                    id, session, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("characteristic");
        }

        /**
         * Зафиксированное решение: отрицательный numberSector на уровне
         * сущности не отвергается — диапазон валидируется выше по стеку.
         */
        @Test
        void negativeNumberSector_notRejectedByEntity() {
            var answer = new AnswersCharacteristics(
                    id, session, characteristic, -1);
            assertThat(answer.getNumberSector()).isEqualTo(-1);
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два объекта с одинаковым id, но разными
         * полями — это одна и та же строка. Аннотация EqualsAndHashCode
         * с onlyExplicitlyIncluded гарантирует, что остальные поля
         * не протекут в equals.
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new AnswersCharacteristics(id, session, characteristic, 1);
            var otherSession = mock(SurveySession.class);
            var otherCharacteristic = mock(Characteristics.class);
            var b = new AnswersCharacteristics(
                    id, otherSession, otherCharacteristic, 5);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new AnswersCharacteristics(id, session, characteristic, 1);
            var b = new AnswersCharacteristics(
                    UUID.randomUUID(), session, characteristic, 1);

            assertThat(a).isNotEqualTo(b);
        }

        /** equals симметричен по типу: с чужим классом не равен. */
        @Test
        void equals_otherType_false() {
            var answer = new AnswersCharacteristics(id, session, characteristic, 1);
            Object stranger = "не характеристика";
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
            assertThat(new AnswersCharacteristics().getIdAnswersCharacteristic())
                    .isNull();
        }

        /** Сеттеры Lombok работают — включая оба ManyToOne-поля. */
        @Test
        void setters_updateFields() {
            var answer = new AnswersCharacteristics(
                    id, session, characteristic, 1);
            var otherSession = mock(SurveySession.class);
            var otherCharacteristic = mock(Characteristics.class);

            answer.setSurveySession(otherSession);
            answer.setCharacteristic(otherCharacteristic);
            answer.setNumberSector(4);

            assertThat(answer.getSurveySession()).isSameAs(otherSession);
            assertThat(answer.getCharacteristic()).isSameAs(otherCharacteristic);
            assertThat(answer.getNumberSector()).isEqualTo(4);
        }
    }
}
