package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.Survey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты ItemsSub — «дополнительный объект опроса»: элемент с URL,
 * привязанный к опросу (ManyToOne на Survey) и собирающий ответы
 * респондентов через обратную сторону связи (OneToMany на
 * AnswersItemsSub, mappedBy = "itemsSub").
 *
 * <p>Что проверяем: контракт единственного конструктора — id, survey и
 * URL обязательны (Objects.requireNonNull с англоязычными сообщениями).
 * URL обязателен и по коду, и по схеме: колонка url_item_sub помечена
 * nullable = false — URL является смысловой сутью таблицы (решение
 * продукта), унифицировано с Items, где после выравнивания требования
 * код и схема согласованы так же. Пустая строка в URL принимается:
 * конструктор проверяет только null, формат URL валидируется выше
 * по стеку, если появится. Коллекция ответов инициализируется пустым
 * списком; равенство только по id; NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Зафиксированные решения:
 * 1) orphanRemoval в OneToMany не задан — значит, действует дефолт false:
 * удаление ответа из коллекции не удаляет строку из БД; каскад
 * MERGE/PERSIST протягивает добавленные ответы при сохранении.
 * 2) Имя таблицы задано явно: @Table(name = "items_sub") — согласуется
 * с @JoinColumn(name = "id_items_sub") в AnswersItemsSubs.</p>
 *
 * <p>Граница с другими тестами: обратная сторона OneToMany здесь
 * проверяется как коллекция в памяти — целостность join-колонок
 * (id_survey, id_items_sub) покрывается @DataJpaTest вместе с
 * SurveySessionRepository. Survey подменяется моком.</p>
 */
class ItemsSubTest {

    private UUID id;
    private Survey survey;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        survey = mock(Survey.class);
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var itemsSub = new ItemsSub(id, survey, "https://example.com/sub.png");

            assertThat(itemsSub.getIdItemSub()).isEqualTo(id);
            assertThat(itemsSub.getSurvey()).isSameAs(survey);
            assertThat(itemsSub.getUrlItemSub()).isEqualTo("https://example.com/sub.png");
        }

        /** id обязателен — null отвергается с сообщением про idItemSub. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new ItemsSub(null, survey, "url"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idItemSub");
        }

        /** Survey обязателен — колонка id_survey с nullable = false. */
        @Test
        void survey_nullRejected() {
            assertThatThrownBy(() -> new ItemsSub(id, null, "url"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("survey");
        }

        /**
         * URL обязателен — это смысловая суть таблицы: объект без URL
         * не имеет смысла. Требование продублировано схемой: колонка
         * url_item_sub помечена nullable = false.
         */
        @Test
        void url_nullRejected() {
            assertThatThrownBy(() -> new ItemsSub(id, survey, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("urlItemSub");
        }

        /**
         * Зафиксированное решение: пустая строка в URL принимается —
         * конструктор проверяет только null, формат URL валидируется
         * выше по стеку, если появится.
         */
        @Test
        void url_emptyStringAllowed() {
            var itemsSub = new ItemsSub(id, survey, "");
            assertThat(itemsSub.getUrlItemSub()).isEmpty();
        }
    }

    @Nested
    class AnswersCollection {

        /** Коллекция ответов инициализирована пустым списком, а не null. */
        @Test
        void answers_defaultEmptyList() {
            var itemsSub = new ItemsSub(id, survey, "url");

            assertThat(itemsSub.getAnswersItemsSubList())
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * Живая ссылка: добавление в коллекцию видно через геттер — на этом
         * держится каскад PERSIST при сохранении объекта с ответами.
         * orphanRemoval не задан и действует дефолт false: удаление из
         * коллекции не удаляет строку из БД.
         */
        @Test
        void answers_liveReference() {
            var itemsSub = new ItemsSub(id, survey, "url");
            var answer = mock(AnswersItemsSubs.class);

            itemsSub.getAnswersItemsSubList().add(answer);

            assertThat(itemsSub.getAnswersItemsSubList())
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
            var a = new ItemsSub(id, survey, "url-1");
            var b = new ItemsSub(id, mock(Survey.class), "url-2");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new ItemsSub(id, survey, "url");
            var b = new ItemsSub(UUID.randomUUID(), survey, "url");

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
            assertThat(new ItemsSub().getIdItemSub())
                    .isNull();
        }

        /**
         * NoArgsConstructor инициализирует и коллекцию ответов — JPA-провайдер
         * подменяет её при загрузке, но до этого момента null недопустим.
         */
        @Test
        void noArgsConstructor_answersEmptyList() {
            assertThat(new ItemsSub().getAnswersItemsSubList())
                    .isNotNull()
                    .isEmpty();
        }

        /** Сеттеры Lombok работают — включая survey и URL. */
        @Test
        void setters_updateFields() {
            var itemsSub = new ItemsSub(id, survey, "url");
            var otherSurvey = mock(Survey.class);

            itemsSub.setSurvey(otherSurvey);
            itemsSub.setUrlItemSub("https://example.com/other.png");

            assertThat(itemsSub.getSurvey()).isSameAs(otherSurvey);
            assertThat(itemsSub.getUrlItemSub()).isEqualTo("https://example.com/other.png");
        }
    }
}
