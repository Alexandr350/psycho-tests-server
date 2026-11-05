package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты AnswersItems — ассоциация «ответ респондента — пункт»:
 * фиксирует выбор конкретного пункта (Items) в конкретной сессии опроса
 * и сектор, в котором он был выбран (два ManyToOne: SurveySession и Items).
 *
 * <p>Что проверяем: контракты конструктора — id, сессия и пункт обязательны
 * (Objects.requireNonNull с англоязычными сообщениями, в том же стиле,
 * что и AnswersAssociations / AnswersCharacteristics); numberSector —
 * примитивный int без проверок диапазона: null невозможен по типу,
 * отрицательные значения на уровне сущности не отвергаются — валидация
 * живёт в DTO/сервисе, если появится; делегирование геттеров; равенство
 * только по id (поле с EqualsAndHashCode.Include); NoArgsConstructor —
 * id остаётся null, генерации на месте нет.</p>
 *
 * <p>Зафиксированное решение: сущность хранит только связь и номер сектора,
 * без текстовых полей — содержательная часть живёт в Items. Пары
 * (сессия, пункт) без ограничения уникальности: если один пункт может
 * быть выбран в нескольких секторах одной сессии, дубликат — легальный
 * кейс, и конструктор его не отвергает.</p>
 *
 * <p>Граница с другими тестами: связи ManyToOne проверяются здесь только
 * как ссылки (isSameAs/requireNonNull) — целостность ассоциаций в БД
 * (id_survey_session, id_item) покрывается @DataJpaTest, запланированным
 * вместе с SurveySessionRepository. Ссылки подменяются моками: содержимое
 * SurveySession и Items этому классу безразлично.</p>
 */
class AnswersItemsTest {

    private UUID id;
    private SurveySession session;
    private Items item;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        session = mock(SurveySession.class);
        item = mock(Items.class);
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var answer = new AnswersItems(id, session, item, 3);

            assertThat(answer.getIdAnswersItems()).isEqualTo(id);
            assertThat(answer.getSurveySession()).isSameAs(session);
            assertThat(answer.getItem()).isSameAs(item);
            assertThat(answer.getNumberSector()).isEqualTo(3);
        }

        /** id обязателен — null отвергается с сообщением про idAnswersItems. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new AnswersItems(null, session, item, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idAnswersItems");
        }

        /** Сессия обязательна — запись не существует вне сессии опроса. */
        @Test
        void surveySession_nullRejected() {
            assertThatThrownBy(() -> new AnswersItems(id, null, item, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("surveySession");
        }

        /** Пункт обязателен — ответ всегда про конкретный пункт. */
        @Test
        void item_nullRejected() {
            assertThatThrownBy(() -> new AnswersItems(id, session, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("item");
        }

        /**
         * Зафиксированное решение: отрицательный numberSector на уровне
         * сущности не отвергается — диапазон валидируется выше по стеку.
         */
        @Test
        void negativeNumberSector_notRejectedByEntity() {
            var answer = new AnswersItems(id, session, item, -1);
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
            var a = new AnswersItems(id, session, item, 1);
            var b = new AnswersItems(
                    id, mock(SurveySession.class), mock(Items.class), 5);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new AnswersItems(id, session, item, 1);
            var b = new AnswersItems(
                    UUID.randomUUID(), session, item, 1);

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
            assertThat(new AnswersItems().getIdAnswersItems())
                    .isNull();
        }

        /** Сеттеры Lombok работают — включая оба ManyToOne-поля. */
        @Test
        void setters_updateFields() {
            var answer = new AnswersItems(id, session, item, 1);
            var otherSession = mock(SurveySession.class);
            var otherItem = mock(Items.class);

            answer.setSurveySession(otherSession);
            answer.setItem(otherItem);
            answer.setNumberSector(4);

            assertThat(answer.getSurveySession()).isSameAs(otherSession);
            assertThat(answer.getItem()).isSameAs(otherItem);
            assertThat(answer.getNumberSector()).isEqualTo(4);
        }
    }
}
