package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.Survey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты Items — «объект опроса»: элемент с URL (картинка и т. п.),
 * привязанный к опросу (ManyToOne на Survey) и собирающий ответы
 * респондентов через обратную сторону связи (OneToMany на AnswersItems,
 * mappedBy).
 *
 * <p>Что проверяем: контракты обоих конструкторов — id обязателен;
 * в трёхарговом конструкторе survey обязателен (колонка survey_item —
 * nullable = false); URL обязателен в обоих конструкторах — колонка
 * url_item помечена nullable = false (унификация с ItemsSub: URL —
 * смысловая суть таблицы); длина URL не ограничивается конструктором
 * (в колонке length не задана); коллекция ответов инициализируется
 * пустым списком, а не null; равенство только по id; NoArgsConstructor —
 * id остаётся null.</p>
 *
 * <p>Зафиксированное решение: URL обязателен и по коду, и по схеме —
 * унифицировано с ItemsSub. Пустая строка принимается: конструктор
 * проверяет только null, формат URL валидируется выше по стеку,
 * если появится. Коллекция ответов мутируема и это живая ссылка:
 * каскад MERGE/PERSIST протягивает добавленные ответы; orphanRemoval
 * выключен — удаление из коллекции не удаляет строку из БД.</p>
 *
 * <p>Граница с другими тестами: обратная сторона OneToMany здесь проверяется
 * как коллекция в памяти — загрузка ленивая, целостность join-колонок
 * покрывается @DataJpaTest вместе с SurveySessionRepository. Survey
 * подменяется моком: содержимое этому классу безразлично.</p>
 */
class ItemsTest {

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
            var item = new Items(id, survey, "https://example.com/pic.png");

            assertThat(item.getIdItem()).isEqualTo(id);
            assertThat(item.getSurvey()).isSameAs(survey);
            assertThat(item.getUrlItem()).isEqualTo("https://example.com/pic.png");
        }

        /** Двухарговый конструктор: survey не проставлен — до persist его нужно установить. */
        @Test
        void constructorWithoutSurvey_surveyStaysNull() {
            var item = new Items(id, "https://example.com/pic.png");

            assertThat(item.getSurvey()).isNull();
            assertThat(item.getUrlItem()).isEqualTo("https://example.com/pic.png");
        }

        /** id обязателен — null отвергается с сообщением про idItem. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new Items(null, survey, "url"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idItem");

            assertThatThrownBy(() -> new Items(null, "url"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idItem");
        }

        /** Survey обязателен в трёхарговом конструкторе — колонка nullable = false. */
        @Test
        void survey_nullRejected() {
            assertThatThrownBy(() -> new Items(id, null, "url"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("survey");
        }

        /**
         * URL обязателен — это смысловая суть таблицы: объект без URL
         * не имеет смысла (решение продукта, зафиксировано в ItemsSub,
         * где конструктор изначально строже). Проверяется и конструктором,
         * и схемой — колонка url_item теперь nullable = false.
         */
        @Test
        void url_nullRejected() {
            assertThatThrownBy(() -> new Items(id, survey, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("urlItem");

            assertThatThrownBy(() -> new Items(id, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("urlItem");
        }

        /**
         * Зафиксированное решение: пустая строка в URL принимается —
         * конструктор проверяет только null. Единообразно с ItemsSub.
         */
        @Test
        void url_emptyStringAllowed() {
            var item = new Items(id, survey, "");
            var shortForm = new Items(id, "");
            assertThat(item.getUrlItem()).isEmpty();
            assertThat(shortForm.getUrlItem()).isEmpty();
        }


    }

    @Nested
    class AnswersCollection {

        /** Коллекция ответов инициализирована пустым списком, а не null. */
        @Test
        void answers_defaultEmptyList() {
            var item = new Items(id, survey, "url");

            assertThat(item.getAnswersItems())
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * Живая ссылка: добавление в коллекцию видно через геттер — на этом
         * держится каскад PERSIST при сохранении пункта с ответами.
         */
        @Test
        void answers_liveReference() {
            var item = new Items(id, survey, "url");
            var answer = mock(AnswersItems.class);

            item.getAnswersItems().add(answer);

            assertThat(item.getAnswersItems())
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
            var a = new Items(id, survey, "url-1");
            var b = new Items(id, mock(Survey.class), "url-2");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new Items(id, survey, "url");
            var b = new Items(UUID.randomUUID(), survey, "url");

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
            assertThat(new Items().getIdItem())
                    .isNull();
        }

        /**
         * NoArgsConstructor инициализирует и коллекцию ответов — JPA-провайдер
         * подменяет её при загрузке, но до этого момента null недопустим.
         */
        @Test
        void noArgsConstructor_answersEmptyList() {
            assertThat(new Items().getAnswersItems())
                    .isNotNull()
                    .isEmpty();
        }

        /** Сеттеры Lombok работают — включая survey и URL. */
        @Test
        void setters_updateFields() {
            var item = new Items(id, survey, "url");
            var otherSurvey = mock(Survey.class);

            item.setSurvey(otherSurvey);
            item.setUrlItem("https://example.com/other.png");

            assertThat(item.getSurvey()).isSameAs(otherSurvey);
            assertThat(item.getUrlItem()).isEqualTo("https://example.com/other.png");
        }
    }
}
