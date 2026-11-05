package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Survey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты Characteristics — «характеристика объекта»: элемент опроса
 * с текстовым описанием, привязанный к опросу (ManyToOne на Survey)
 * и собирающий ответы респондентов через обратную сторону связи
 * (OneToMany на AnswersCharacteristics, mappedBy).
 *
 * <p>Что проверяем: контракты обоих конструкторов — id обязателен;
 * в трёхарговом конструкторе survey обязателен (колонка
 * id_survey_characteristics — nullable = false); текст может быть null —
 * колонка text_characteristic не помечена nullable = false, null-текст
 * легален по схеме; длина текста не ограничивается конструктором
 * (length = 50 — забота Hibernate/схемы, не сущности); коллекция ответов
 * инициализируется пустым списком, а не null; равенство только по id;
 * NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Зафиксированные решения:
 * 1) null-текст разрешён — если продукт решит сделать текст обязательным,
 * добавить nullable = false и requireNonNull, тест text_nullAllowed
 * перевернётся и напомнит.
 * 2) Коллекция ответов мутируема и это живая ссылка: каскад MERGE/PERSIST
 * протягивает добавленные ответы; orphanRemoval выключен — удаление из
 * коллекции не удаляет строку из БД.</p>
 *
 * <p>Граница с другими тестами: обратная сторона OneToMany здесь проверяется
 * как коллекция в памяти (пустой старт, живая ссылка) — загрузка ленивая,
 * целостность join-колонок покрывается @DataJpaTest вместе с
 * SurveySessionRepository. Survey подменяется моком: содержимое этому
 * классу безразлично.</p>
 */
class CharacteristicsTest {

    private UUID id;
    private Survey survey;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        survey = mock(Survey.class);
    }

    @Nested
    class ConstructorContracts {

        /** Трёхарговый конструктор: все поля делегируются без изменений. */
        @Test
        void constructorWithSurvey_setsAllFields() {
            var characteristic = new Characteristics(id, survey, "открытость");

            assertThat(characteristic.getIdCharacteristic()).isEqualTo(id);
            assertThat(characteristic.getSurvey()).isSameAs(survey);
            assertThat(characteristic.getTextCharacteristic()).isEqualTo("открытость");
        }

        /** Двухарговый конструктор: survey не проставлен — до persist его нужно установить. */
        @Test
        void constructorWithoutSurvey_surveyStaysNull() {
            var characteristic = new Characteristics(id, "открытость");

            assertThat(characteristic.getSurvey()).isNull();
            assertThat(characteristic.getTextCharacteristic()).isEqualTo("открытость");
        }

        /** id обязателен — null отвергается с сообщением про idCharacteristic. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new Characteristics(null, survey, "текст"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idCharacteristic");

            assertThatThrownBy(() -> new Characteristics(null, "текст"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idCharacteristic");
        }

        /** Survey обязателен в трёхарговом конструкторе — колонка nullable = false. */
        @Test
        void survey_nullRejected() {
            assertThatThrownBy(() -> new Characteristics(id, null, "текст"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("survey");
        }

        /**
         * Зафиксированное решение: null-текст легален — колонка
         * text_characteristic без nullable = false.
         */
        @Test
        void text_nullAllowed() {
            var characteristic = new Characteristics(id, survey, null);
            assertThat(characteristic.getTextCharacteristic()).isNull();
        }

        /**
         * Зафиксированное решение: длина текста не проверяется конструктором —
         * length = 50 ловит Hibernate при сохранении.
         */
        @Test
        void text_overColumnLength_notRejectedByEntity() {
            var longText = "о".repeat(80);
            var characteristic = new Characteristics(id, survey, longText);
            assertThat(characteristic.getTextCharacteristic()).hasSize(80);
        }
    }

    @Nested
    class AnswersCollection {

        /** Коллекция ответов инициализирована пустым списком, а не null. */
        @Test
        void answers_defaultEmptyList() {
            var characteristic = new Characteristics(id, survey, "текст");

            assertThat(characteristic.getAnswersCharacteristics())
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * Живая ссылка: добавление в коллекцию видно через геттер — на этом
         * держится каскад PERSIST при сохранении характеристики с ответами.
         */
        @Test
        void answers_liveReference() {
            var characteristic = new Characteristics(id, survey, "текст");
            var answer = mock(AnswersCharacteristics.class);

            characteristic.getAnswersCharacteristics().add(answer);

            assertThat(characteristic.getAnswersCharacteristics())
                    .containsExactly(answer);
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
            var a = new Characteristics(id, survey, "текст");
            var b = new Characteristics(id, mock(Survey.class), "другой текст");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new Characteristics(id, survey, "текст");
            var b = new Characteristics(UUID.randomUUID(), survey, "текст");

            assertThat(a).isNotEqualTo(b);
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
            assertThat(new Characteristics().getIdCharacteristic())
                    .isNull();
        }

        /**
         * NoArgsConstructor инициализирует и коллекцию ответов — JPA-провайдер
         * подменяет её при загрузке, но до этого момента null недопустим.
         */
        @Test
        void noArgsConstructor_answersEmptyList() {
            assertThat(new Characteristics().getAnswersCharacteristics())
                    .isNotNull()
                    .isEmpty();
        }

        /** Сеттеры Lombok работают — включая survey и текст. */
        @Test
        void setters_updateFields() {
            var characteristic = new Characteristics(id, survey, "текст");
            var otherSurvey = mock(Survey.class);

            characteristic.setSurvey(otherSurvey);
            characteristic.setTextCharacteristic("новый текст");

            assertThat(characteristic.getSurvey()).isSameAs(otherSurvey);
            assertThat(characteristic.getTextCharacteristic()).isEqualTo("новый текст");
        }
    }
}
