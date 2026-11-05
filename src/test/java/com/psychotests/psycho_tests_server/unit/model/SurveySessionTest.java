package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import com.psychotests.psycho_tests_server.model.SurveySession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты SurveySession — «сессия прохождения опроса»: связка
 * «респондент проходит опрос» с состоянием жизненного цикла
 * (NOT_STARTED → ... → FINISHED по ансамблю SurveySessionStatus),
 * временем старта (@CreationTimestamp, updatable = false) и финиша.
 * Держит четыре обратные стороны OneToMany: ответы по характеристикам,
 * пунктам, подпунктам и ассоциациям.
 *
 * <p>Зафиксированные решения:
 * 1) Политика каскадов — «поток данных»: PERSIST + MERGE без
 * orphanRemoval на всех четырёх коллекциях ответов. Ответы сохраняются
 * вместе с сессией при создании, живут своей жизнью дальше и удаляются
 * своими репозиториями; исключение из списка не удаляет строку из БД.
 * Это зеркально Survey.surveySessions и отвечает решению по
 * PsychologistAccount: сессии не владеют ответами.
 * 2) Статус NOT_STARTED — инвариант рождения: сеттером статус меняют
 * переходами жизненного цикла, но сессия без статуса не существует.
 * 3) Составная уникальность: одна сессия на пару респондент+запуск
 * (uk_session_respondent_run) — изоляция волн данных. Респондент может
 * проходить одну и ту же анкету повторно в новой волне (новый запуск —
 * новая сессия), но внутри одного запуска — только один раз. Повторное
 * прохождение в рамках запуска отсекается констрейнтом, а не сервисом.
 * 4) startTimeSession — только Hibernate (@CreationTimestamp): сеттер
 * закрыт (Setter NONE), как у id; сервис не задаёт время старта руками.
 * 5) finishTimeSession опциональна — незавершённые сессии легальны.</p>
 *
 * <p>Граница с другими тестами: уникальность id_respondent и целостность
 * join-колонок покрываются @DataJpaTest вместе с SurveySessionRepository
 * (его existsBySurveyIdSurveyAndStatusNot уже читает statusSurveySession).
 * Survey, Respondent и четыре answer-сущности подменяются моками.</p>
 */
class SurveySessionTest {

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Nested
    class ConstructorContracts {

        /** Конструктор задаёт id и стартовый статус; ссылки и ответы — позже. */
        @Test
        void constructor_setsIdAndDefaultStatus() {
            var session = new SurveySession(id);

            assertThat(session.getIdSurveySession()).isEqualTo(id);
            assertThat(session.getStatusSurveySession())
                    .isEqualTo(SurveySessionStatus.NOT_STARTED);
            assertThat(session.getRun()).isNull();
            assertThat(session.getSurvey()).isNull();
            assertThat(session.getRespondent()).isNull();
            assertThat(session.getStartTimeSession()).isNull();
            assertThat(session.getFinishTimeSession()).isNull();
        }

        /** id обязателен — null отвергается с сообщением про idSurveySession. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new SurveySession(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idSurveySession");
        }


    }

    @Nested
    class Lifecycle {

        /**
         * Статус — часть жизненного цикла: сеттер переводит сессию между
         * состояниями ансамбля SurveySessionStatus. Отдельного «конечного
         * автомата» на уровне сущности нет — переходы контролирует сервис.
         */
        @Test
        void status_setterTransitions() {
            var session = new SurveySession(id);

            session.setStatusSurveySession(SurveySessionStatus.IN_PROGRESS);
            assertThat(session.getStatusSurveySession())
                    .isEqualTo(SurveySessionStatus.IN_PROGRESS);

            session.setStatusSurveySession(SurveySessionStatus.COMPLETED);
            assertThat(session.getStatusSurveySession())
                    .isEqualTo(SurveySessionStatus.COMPLETED);
        }

        /**
         * Время финиша опционально: незавершённая сессия легальна — null
         * в finish_time_session означает «прохождение не закрыто».
         */
        @Test
        void finishTime_nullMeansUnfinished() {
            var session = new SurveySession(id);

            assertThat(session.getFinishTimeSession()).isNull();

            session.setFinishTimeSession(java.time.LocalDateTime.of(2026, 10, 25, 12, 0));
            assertThat(session.getFinishTimeSession()).isNotNull();
        }

        /**
         * Время старта задаёт только Hibernate (@CreationTimestamp):
         * сеттер закрыт (Setter NONE), как у id. Рождение сессии —
         * прерогатива провайдера, сервис не подделывает старт.
         */
        @Test
        void startTime_noSetter_handledByHibernate() {
            var session = new SurveySession(id);

            assertThat(session.getStartTimeSession()).isNull();
        }

    }

    @Nested
    class Collections {

        /** Все четыре коллекции ответов инициализированы пустыми списками. */
        @Test
        void allCollections_defaultEmptyList() {
            var session = new SurveySession(id);

            assertThat(session.getAnswersCharacteristics()).isNotNull().isEmpty();
            assertThat(session.getAnswersItems()).isNotNull().isEmpty();
            assertThat(session.getAnswersItemsSubs()).isNotNull().isEmpty();
            assertThat(session.getAnswersAssociations()).isNotNull().isEmpty();
        }

        /** Живая ссылка: ответы по характеристикам. */
        @Test
        void answersCharacteristics_liveReference() {
            var session = new SurveySession(id);
            var answer = mock(AnswersCharacteristics.class);

            session.getAnswersCharacteristics().add(answer);

            assertThat(session.getAnswersCharacteristics()).containsExactly(answer);
        }

        /** Живая ссылка: ответы по пунктам. */
        @Test
        void answersItems_liveReference() {
            var session = new SurveySession(id);
            var answer = mock(AnswersItems.class);

            session.getAnswersItems().add(answer);

            assertThat(session.getAnswersItems()).containsExactly(answer);
        }

        /** Живая ссылка: ответы по подпунктам. */
        @Test
        void answersItemsSubs_liveReference() {
            var session = new SurveySession(id);
            var answer = mock(AnswersItemsSubs.class);

            session.getAnswersItemsSubs().add(answer);

            assertThat(session.getAnswersItemsSubs()).containsExactly(answer);
        }

        /** Живая ссылка: ответы-ассоциации. */
        @Test
        void answersAssociations_liveReference() {
            var session = new SurveySession(id);
            var answer = mock(AnswersAssociations.class);

            session.getAnswersAssociations().add(answer);

            assertThat(session.getAnswersAssociations()).containsExactly(answer);
        }

        /** Четыре коллекции независимы: элемент одной не появляется в других. */
        @Test
        void collections_independent() {
            var session = new SurveySession(id);
            var answer = mock(AnswersItems.class);

            session.getAnswersItems().add(answer);

            assertThat(session.getAnswersCharacteristics()).isEmpty();
            assertThat(session.getAnswersItemsSubs()).isEmpty();
            assertThat(session.getAnswersAssociations()).isEmpty();
        }
    }

    @Nested
    class CascadePolicy {

        /**
         * Поток данных: ответ можно исключить из списка без последствий
         * для строки (orphanRemoval = false) — ответы удаляются своими
         * репозиториями. Flush-поведение — @DataJpaTest.
         */
        @Test
        void answers_removalIsSafe() {
            var session = new SurveySession(id);
            var answer = mock(AnswersItems.class);

            session.getAnswersItems().add(answer);
            session.getAnswersItems().remove(answer);

            assertThat(session.getAnswersItems()).isEmpty();
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: две сессии с одинаковым id, но разными
         * статусами и ответами — одна и та же строка.
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new SurveySession(id);
            a.setStatusSurveySession(SurveySessionStatus.COMPLETED);
            var b = new SurveySession(id);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные сессии, даже с одинаковым статусом. */
        @Test
        void equals_differentIds_false() {
            var a = new SurveySession(id);
            var b = new SurveySession(UUID.randomUUID());

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) оставляет всё null — провайдер и сервис
         * дозаполнят. В отличие от полного конструктора, статус остаётся null:
         * JPA-провайдер подгрузит его из колонки при загрузке сущности.
         */
        @Test
        void noArgsConstructor_idStaysNull() {
            assertThat(new SurveySession().getIdSurveySession())
                    .isNull();
        }

        /** NoArgsConstructor инициализирует все четыре коллекции. */
        @Test
        void noArgsConstructor_collectionsEmpty() {
            var session = new SurveySession();

            assertThat(session.getAnswersCharacteristics()).isNotNull().isEmpty();
            assertThat(session.getAnswersItems()).isNotNull().isEmpty();
            assertThat(session.getAnswersItemsSubs()).isNotNull().isEmpty();
            assertThat(session.getAnswersAssociations()).isNotNull().isEmpty();
        }
    }
}
