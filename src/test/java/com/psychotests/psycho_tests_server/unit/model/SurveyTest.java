package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты Survey — «опрос»: оболочка исследования из личного кабинета
 * (Создание исследования). Хранит заголовок (название, описание, правила
 * прохождения), жёстко привязан 1:1 к Research (research_id_survey,
 * unique — у одного исследования ровно один опрос) и держит четыре
 * обратные стороны OneToMany: поток сессий (SurveySession) и три
 * структурные коллекции — характеристики, пункты, подпункты.
 *
 * <p>Что проверяем: контракт конструктора — id обязателен (requireNonNull
 * с англоязычным сообщением, как в Respondent); привязка к Research не
 * входит в конструктор — устанавливается сервисом после создания, но до
 * сохранения (nullable = false); заголовочные поля — название
 * обязательно на уровне схемы, описание и правила опциональны;
 * коллекции инициализированы пустыми списками; равенство только по id;
 * NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Зафиксированные решения:
 * 1) id обязателен — конструктор отвергает null явно, а не отдаёт
 * проблему на откуп БД.
 * 2) Валидация названия (непустота, длина ≤ 150) — задача сервиса/DTO;
 * сущность хранит как есть, схему не дублирует.
 * 3) Двухуровневая политика каскадов — ключевое решение класса.
 * Сессии (surveySessions): PERSIST + MERGE, без orphanRemoval — это
 * поток данных, сессии живут независимо и удаляются своими
 * репозиториями; удаление из списка не удаляет строку из БД.
 * Структура (characteristicsSurvey, itemsSurvey, itemsSubsSurvey):
 * ALL + orphanRemoval — опрос полностью владеет своей конфигурацией;
 * она удаляется вместе с опросом и при исключении из списка.
 * 4) Research не в конструкторе: опрос может существовать как объект до
 * привязки к исследованию, но сохранить его без Research нельзя —
 * контроль на уровне сервиса.
 * 5) Именование выровнено: idSurvey (было IDSurvey), surveySessions
 * (было sessionsSurvey) — по образцу Respondent.</p>
 *
 * <p>Граница с другими тестами: flush-поведение каскадов и orphanRemoval,
 * а также уникальность research_id_survey покрываются @DataJpaTest вместе
 * с SurveyRepository; Research, SurveySession, Characteristics, Items,
 * ItemsSub подменяются моками.</p>
 */
class SurveyTest {

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    /** Фабрика полного опроса — заголовок заполнен, Research не привязан. */
    private Survey newSurvey() {
        return new Survey(id, "Шкала тревожности Бека",
                "Краткое описание опроса", "Правила прохождения");
    }

    @Nested
    class ConstructorContracts {

        /** Конструктор задаёт все четыре аргумента; Research остаётся null. */
        @Test
        void constructor_setsAllFields() {
            var survey = newSurvey();

            assertThat(survey.getIdSurvey()).isEqualTo(id);
            assertThat(survey.getTitleSurvey()).isEqualTo("Шкала тревожности Бека");
            assertThat(survey.getDescriptionSurvey()).isEqualTo("Краткое описание опроса");
            assertThat(survey.getRulesSurvey()).isEqualTo("Правила прохождения");
            assertThat(survey.getResearch()).isNull();
        }

        /** id обязателен — null отвергается с сообщением про idSurvey. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new Survey(null, "Название", null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idSurvey");
        }

        /** Описание и правила опциональны — конструктор принимает null. */
        @Test
        void constructor_optionalHeaderFieldsAllowed() {
            var survey = new Survey(id, "Только название", null, null);

            assertThat(survey.getTitleSurvey()).isEqualTo("Только название");
            assertThat(survey.getDescriptionSurvey()).isNull();
            assertThat(survey.getRulesSurvey()).isNull();
        }

        /** title обязателен — колонка title_survey с nullable = false. */
        @Test
        void title_nullRejected() {
            assertThatThrownBy(() -> new Survey(id, null, "Описание", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("titleSurvey");
        }

        /** Пустая строка в названии принимается: конструктор проверяет только null. */
        @Test
        void title_emptyStringAllowed() {
            var survey = new Survey(id, "", null, null);

            assertThat(survey.getTitleSurvey()).isEmpty();
        }

    }

    @Nested
    class Header {

        /** Сеттеры меняют заголовок независимо друг от друга. */
        @Test
        void setters_updateHeader() {
            var survey = newSurvey();

            survey.setTitleSurvey("Новое название");
            survey.setDescriptionSurvey("Новое описание");
            survey.setRulesSurvey(null);

            assertThat(survey.getTitleSurvey()).isEqualTo("Новое название");
            assertThat(survey.getDescriptionSurvey()).isEqualTo("Новое описание");
            assertThat(survey.getRulesSurvey()).isNull();
        }

        /**
         * Сущность хранит название как есть, без обрезки: контроль длины
         * (≤ 150) — задача схемы и сервиса, не конструктора или сеттера.
         */
        @Test
        void title_keptAsIs_validationAtService() {
            var survey = newSurvey();
            var longTitle = "Н".repeat(200);

            survey.setTitleSurvey(longTitle);

            assertThat(survey.getTitleSurvey()).hasSize(200);
        }
    }

    @Nested
    class Collections {

        /** Все четыре коллекции инициализированы пустыми списками, не null. */
        @Test
        void allCollections_defaultEmptyList() {
            var survey = newSurvey();

            assertThat(survey.getSurveySessions()).isNotNull().isEmpty();
            assertThat(survey.getCharacteristicsSurvey()).isNotNull().isEmpty();
            assertThat(survey.getItemsSurvey()).isNotNull().isEmpty();
            assertThat(survey.getItemsSubsSurvey()).isNotNull().isEmpty();
        }

        /** Живая ссылка на сессии: добавление видно через геттер. */
        @Test
        void surveySessions_liveReference() {
            var survey = newSurvey();
            var session = mock(SurveySession.class);

            survey.getSurveySessions().add(session);

            assertThat(survey.getSurveySessions()).containsExactly(session);
        }

        /** Живая ссылка на характеристики. */
        @Test
        void characteristics_liveReference() {
            var survey = newSurvey();
            var characteristic = mock(Characteristics.class);

            survey.getCharacteristicsSurvey().add(characteristic);

            assertThat(survey.getCharacteristicsSurvey()).containsExactly(characteristic);
        }

        /** Живая ссылка на пункты. */
        @Test
        void items_liveReference() {
            var survey = newSurvey();
            var item = mock(Items.class);

            survey.getItemsSurvey().add(item);

            assertThat(survey.getItemsSurvey()).containsExactly(item);
        }

        /** Живая ссылка на подпункты. */
        @Test
        void itemsSubs_liveReference() {
            var survey = newSurvey();
            var itemSub = mock(ItemsSub.class);

            survey.getItemsSubsSurvey().add(itemSub);

            assertThat(survey.getItemsSubsSurvey()).containsExactly(itemSub);
        }

        /** Коллекции независимы: элемент в одной не появляется в других. */
        @Test
        void collections_independent() {
            var survey = newSurvey();
            var item = mock(Items.class);

            survey.getItemsSurvey().add(item);

            assertThat(survey.getSurveySessions()).isEmpty();
            assertThat(survey.getCharacteristicsSurvey()).isEmpty();
            assertThat(survey.getItemsSubsSurvey()).isEmpty();
        }
    }

    @Nested
    class CascadePolicy {

        /**
         * Сессии — поток данных: сессию можно исключить из списка без
         * последствий для строки (orphanRemoval = false). Сессии удаляются
         * своими репозиториями, а не через опрос. Flush-поведение — @DataJpaTest.
         */
        @Test
        void surveySessions_removalIsSafe() {
            var survey = newSurvey();
            var session = mock(SurveySession.class);

            survey.getSurveySessions().add(session);
            survey.getSurveySessions().remove(session);

            assertThat(survey.getSurveySessions()).isEmpty();
        }

        /**
         * Структура опроса полностью принадлежит ему (ALL + orphanRemoval):
         * исключение характеристики из списка — это «удалить навсегда».
         * Тест фиксирует состояние списка; удаление строки при flush
         * проверяется интеграционно.
         */
        @Test
        void structureCollections_ownedBySurvey() {
            var survey = newSurvey();
            var characteristic = mock(Characteristics.class);

            survey.getCharacteristicsSurvey().add(characteristic);
            survey.getCharacteristicsSurvey().remove(characteristic);

            assertThat(survey.getCharacteristicsSurvey()).isEmpty();
        }
    }

    @Nested
    class ResearchLink {

        /**
         * Research не входит в конструктор — привязывается сеттером после
         * создания. unique = true гарантирует 1:1, nullable = false —
         * сохранить без исследования нельзя; контроль — на уровне сервиса.
         */
        @Test
        void research_setAfterConstruction() {
            var survey = newSurvey();
            var research = mock(Research.class);

            survey.setResearch(research);

            assertThat(survey.getResearch()).isSameAs(research);
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два опроса с одинаковым id, но разными
         * заголовками — одна и та же строка. Заголовок не протекает в equals.
         */
        @Test
        void equals_sameIdDifferentHeader_true() {
            var a = newSurvey();
            a.setTitleSurvey("Одно название");
            var b = newSurvey();
            b.setTitleSurvey("Другое название");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные опросы, даже с одинаковым заголовком. */
        @Test
        void equals_differentIds_false() {
            var a = newSurvey();
            var b = new Survey(UUID.randomUUID(), "Шкала тревожности Бека",
                    "Краткое описание опроса", "Правила прохождения");

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) оставляет id и Research null — провайдер
         * и сервис дозаполнят. Тест фиксирует, что конструктор без логики.
         */
        @Test
        void noArgsConstructor_idAndResearchNull() {
            var survey = new Survey();

            assertThat(survey.getIdSurvey()).isNull();
            assertThat(survey.getResearch()).isNull();
        }

        /** NoArgsConstructor инициализирует все четыре коллекции. */
        @Test
        void noArgsConstructor_collectionsEmpty() {
            var survey = new Survey();

            assertThat(survey.getSurveySessions()).isNotNull().isEmpty();
            assertThat(survey.getCharacteristicsSurvey()).isNotNull().isEmpty();
            assertThat(survey.getItemsSurvey()).isNotNull().isEmpty();
            assertThat(survey.getItemsSubsSurvey()).isNotNull().isEmpty();
        }
    }
}
