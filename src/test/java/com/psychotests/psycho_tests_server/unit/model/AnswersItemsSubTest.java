package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты AnswersItemsSub — ассоциация «ответ респондента —
 * дополнительный объект»: фиксирует выбор дополнительного пункта
 * (ItemsSub) в сессии опроса и сектор выбора (два ManyToOne:
 * SurveySession и ItemsSub).
 *
 * <p>Что проверяем: контракты конструктора — id, сессия и дополнительный
 * пункт обязательны (Objects.requireNonNull с англоязычными сообщениями,
 * единый стиль с AnswersAssociations / AnswersCharacteristics / AnswersItems);
 * numberSector — примитивный int без проверок диапазона; делегирование
 * геттеров; равенство только по id; NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Граница с другими тестами: связи ManyToOne проверяются здесь только
 * как ссылки — целостность ассоциаций в БД (id_survey_session, id_items_sub)
 * покрывается @DataJpaTest вместе с SurveySessionRepository. Ссылки
 * подменяются моками: содержимое SurveySession и ItemsSub этому классу
 * безразлично.</p>
 */
class AnswersItemsSubTest {

    private UUID id;
    private SurveySession session;
    private ItemsSub itemsSub;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        session = mock(SurveySession.class);
        itemsSub = mock(ItemsSub.class);
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var answer = new AnswersItemsSubs(id, session, itemsSub, 3);

            assertThat(answer.getIdAnswersItemsSub()).isEqualTo(id);
            assertThat(answer.getSurveySession()).isSameAs(session);
            assertThat(answer.getItemsSub()).isSameAs(itemsSub);
            assertThat(answer.getNumberSector()).isEqualTo(3);
        }

        /** id обязателен — null отвергается с сообщением про idAnswersItemsSub. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new AnswersItemsSubs(null, session, itemsSub, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idAnswersItemsSub");
        }

        /** Сессия обязательна — запись не существует вне сессии опроса. */
        @Test
        void surveySession_nullRejected() {
            assertThatThrownBy(() -> new AnswersItemsSubs(id, null, itemsSub, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("surveySession");
        }

        /** Дополнительный пункт обязателен — ответ всегда про конкретный объект. */
        @Test
        void itemsSub_nullRejected() {
            assertThatThrownBy(() -> new AnswersItemsSubs(id, session, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("itemsSub");
        }

        /**
         * Зафиксированное решение: отрицательный numberSector на уровне
         * сущности не отвергается — диапазон валидируется выше по стеку.
         */
        @Test
        void negativeNumberSector_notRejectedByEntity() {
            var answer = new AnswersItemsSubs(id, session, itemsSub, -1);
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
            var a = new AnswersItemsSubs(id, session, itemsSub, 1);
            var b = new AnswersItemsSubs(
                    id, mock(SurveySession.class), mock(ItemsSub.class), 5);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные объекты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new AnswersItemsSubs(id, session, itemsSub, 1);
            var b = new AnswersItemsSubs(
                    UUID.randomUUID(), session, itemsSub, 1);

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
            assertThat(new AnswersItemsSubs().getIdAnswersItemsSub())
                    .isNull();
        }

        /** Сеттеры Lombok работают — включая оба ManyToOne-поля. */
        @Test
        void setters_updateFields() {
            var answer = new AnswersItemsSubs(id, session, itemsSub, 1);
            var otherSession = mock(SurveySession.class);
            var otherItemsSub = mock(ItemsSub.class);

            answer.setSurveySession(otherSession);
            answer.setItemsSub(otherItemsSub);
            answer.setNumberSector(4);

            assertThat(answer.getSurveySession()).isSameAs(otherSession);
            assertThat(answer.getItemsSub()).isSameAs(otherItemsSub);
            assertThat(answer.getNumberSector()).isEqualTo(4);
        }
    }
}
