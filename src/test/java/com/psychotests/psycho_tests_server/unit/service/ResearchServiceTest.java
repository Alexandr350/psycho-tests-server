package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.repository.RunRepository;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;
import com.psychotests.psycho_tests_server.repository.SurveySessionRepository;
import com.psychotests.psycho_tests_server.service.CurrentPsychologistProvider;
import com.psychotests.psycho_tests_server.service.FileStorageService;
import com.psychotests.psycho_tests_server.service.ResearchAccessService;
import com.psychotests.psycho_tests_server.service.ResearchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты ResearchService — чтение, смена статуса и удаление исследований.
 *
 * <p>Что проверяем: findAllResearches возвращает исследования только текущего
 * психолога в виде dto; getResearch и changeStatus делегируют проверку владения
 * ResearchAccessService; resolvePublicToken — публичный вход респондента
 * по токену запуска (четыре ветви: валидный токен, закрытый запуск, research
 * не в RUNNING, неизвестный токен — все кроме первой дают 404); deleteResearch
 * — сложнейший метод класса, здесь фиксируются три его контракта. Первый —
 * порядок каскада: ведущий flush+clear, файлы опроса, research, финальный
 * flush+clear. Survey сервисом не удаляется — его сносит FK-каскад БД
 * (@OnDelete) вместе с runs, кодами, сессиями и ответами. Второй — отказы
 * до удаления данных: запрет по статусу и запрет при наличии незавершённых
 * сессий не трогают репозитории и файлы; ведущий flush/clear при этом
 * выполняется безусловно — он стоит до всех проверок. Третий — очистка
 * persistence context: flush+clear вызываются до загрузки research и после
 * удаления, всего по два раза. Это не украшение, а рабочее поведение:
 * интеграционный тест DeleteResearchFlowTest доказал, что без ведущего
 * clear() метод получает из кэша первого уровня объект, изменённый ранее
 * в той же транзакции, и проверки срабатывают на устаревшем состоянии.
 * Контракт «работа с актуальной копией из БД» подтверждён интеграционно
 * и зафиксирован здесь.</p>
 *
 * <p>Как работает: Spring-контекст не поднимается, все зависимости — моки.
 * flush()/clear() — void-методы: не стабятся, только верифицируются.
 * Research — spy: реальный changeStatus заглушен через doNothing, чтобы тест
 * делегирования не зависел от допустимости конкретных переходов; правила
 * переходов — территория тестов Research и ResearchStatus. Для syncRuns
 * статус сущности после doNothing-стаба выставляется напрямую
 * (setStatusResearch) — тесты не зависят от легальности перехода.
 * Run в resolvePublicToken/syncRuns — мок: у него доменное условие
 * isOpen(), которое удобнее настраивать на моке, чем воспроизводить
 * через реальные поля времени.</p>
 */
@ExtendWith(MockitoExtension.class)
class ResearchServiceTest {

    @Mock private ResearchRepository researchRepository;
    @Mock private ResearchAccessService researchAccessService;
    @Mock private CurrentPsychologistProvider currentPsychologistProvider;
    @Mock private FileStorageService fileStorageService;
    @Mock private SurveyRepository surveyRepository;
    @Mock private SurveySessionRepository surveySessionRepository;
    @Mock private RunRepository runRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private ResearchService service;

    private PsychologistAccount psychologist;
    private Research research;

    @BeforeEach
    void setUp() {
        psychologist = new PsychologistAccount(
                UUID.randomUUID(), "Анна", "Иванова", "$2a$10$hashedvalue", "psy@mail.ru");
        research = spy(new Research(
                UUID.randomUUID(), "Исследование", "Описание", "Заказчик", TypeResearch.CPSK));
        // Все восемь зависимостей сервиса конструкторные (@RequiredArgsConstructor),
        // @InjectMocks раскладывает их сам — ручная подстановка не нужна.
    }

    private void stubCurrentPsychologist() {
        when(currentPsychologistProvider.getPsychologistAccount()).thenReturn(psychologist);
    }

    private void stubOwnedResearch(UUID researchId) {
        when(researchAccessService.getOwnedResearch(researchId)).thenReturn(research);
    }

    @Nested
    class FindAllResearches {

        /**
         * Happy path: репозиторий возвращает список по id текущего психолога,
         * сервис маппит каждую сущность в dto. Заодно проверяем, что в
         * репозиторий ушёл именно id текущего психолога, а не кого-то ещё.
         */
        @Test
        void returnsMappedDtosOfCurrentPsychologist() {
            stubCurrentPsychologist();
            var second = new Research(
                    UUID.randomUUID(), "Второе", "Описание", "Заказчик", TypeResearch.CPSK);
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of(research, second));

            var result = service.findAllResearches();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).idResearch())
                    .isEqualTo(research.getIDResearch());
            assertThat(result.get(1).nameResearch()).isEqualTo("Второе");
        }

        /** Нет исследований — пустой список, без исключений. */
        @Test
        void noResearches_returnsEmptyList() {
            stubCurrentPsychologist();
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of());

            assertThat(service.findAllResearches()).isEmpty();
        }
    }

    @Nested
    class GetResearch {

        /** Happy path без опроса: dto собран из research, резюме-поля пустые. */
        @Test
        void returnsDtoOfOwnedResearch() {
            var researchId = research.getIDResearch();
            stubOwnedResearch(researchId);
            stubCurrentPsychologist();

            var result = service.getResearch(researchId);

            assertThat(result.idResearch()).isEqualTo(researchId);
            assertThat(result.nameResearch()).isEqualTo(research.getNameResearch());
            assertThat(result.idSurvey()).isNull();
            assertThat(result.characteristics()).isEmpty();
            assertThat(result.items()).isEmpty();
        }

        /** Опрос найден по FK: dto отдаёт его idSurvey — контракт PATCH шага 2. */
        @Test
        void withSurvey_returnsIdSurvey() {
            var researchId = research.getIDResearch();
            var idSurvey = UUID.randomUUID();
            var survey = new Survey(idSurvey, "Опрос", "d", "r");
            stubOwnedResearch(researchId);
            stubCurrentPsychologist();
            when(surveyRepository.findByResearchIdResearchAndPsychologistId(
                    researchId, psychologist.getIdPsychologistAccount()))
                    .thenReturn(Optional.of(survey));

            var result = service.getResearch(researchId);

            assertThat(result.idSurvey()).isEqualTo(idSurvey);
            assertThat(result.characteristics()).isEmpty();  // в юните коллекции опроса пустые
        }
    }

    @Nested
    class ResolvePublicToken {

        /** Валидный токен: открытый запуск, research в RUNNING — отдаётся idSurvey. */
        @Test
        void validToken_returnsIdSurvey() {
            var runResearch = new Research(
                    UUID.randomUUID(), "Исследование", "d", "c", TypeResearch.CPSK);
            runResearch.setStatusResearch(ResearchStatus.RUNNING);
            var survey = new Survey(UUID.randomUUID(), "Опрос", "d", "r");
            var run = mock(Run.class);
            when(run.isOpen()).thenReturn(true);
            when(run.getResearch()).thenReturn(runResearch);
            when(runRepository.findByPublicToken("tok")).thenReturn(Optional.of(run));
            when(surveyRepository.findByResearchIdResearch(runResearch.getIDResearch()))
                    .thenReturn(Optional.of(survey));

            var result = service.resolvePublicToken("tok");

            assertThat(result.idSurvey()).isEqualTo(survey.getIdSurvey());
        }

        /** Запуск закрыт (finish вызван): 404 «Опрос недоступен». */
        @Test
        void closedRun_returns404() {
            var run = mock(Run.class);
            when(run.isOpen()).thenReturn(false);
            when(runRepository.findByPublicToken("tok")).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.resolvePublicToken("tok"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос недоступен");

            verifyNoInteractions(surveyRepository);
        }

        /** Research уже не в RUNNING (STOPPED/PAUSE/...): 404, survey не ищется. */
        @Test
        void researchNotRunning_returns404() {
            var runResearch = new Research(
                    UUID.randomUUID(), "Исследование", "d", "c", TypeResearch.CPSK);
            runResearch.setStatusResearch(ResearchStatus.STOPPED);
            var run = mock(Run.class);
            when(run.isOpen()).thenReturn(true);
            when(run.getResearch()).thenReturn(runResearch);
            when(runRepository.findByPublicToken("tok")).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.resolvePublicToken("tok"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос недоступен");

            verifyNoInteractions(surveyRepository);
        }

        /** Неизвестный токен: 404, до survey дело не доходит. */
        @Test
        void unknownToken_returns404() {
            when(runRepository.findByPublicToken("tok")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolvePublicToken("tok"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос недоступен");

            verifyNoInteractions(surveyRepository);
        }
    }

    @Nested
    class DeleteResearch {


        /**
         * Полный каскад: research со survey, где нет активных сессий
         * (репозиторий отвечает false на existsActiveSession). Порядок
         * фиксируется InOrder: flush+clear → файлы опроса → research →
         * flush+clear. Survey сервисом не удаляется — его сносит FK-каскад
         * БД (@OnDelete); файлы удаляются до сноса строк, потому что после
         * удаления survey idSurvey взять неоткуда.
         */
        @Test
        void withSurvey_cascadeFilesSurveyResearch_thenFlushClear() {
            var researchId = research.getIDResearch();
            var idSurvey = UUID.randomUUID();
            var survey = new Survey(idSurvey, "Опрос", "Описание", "Правила");
            research.setStatusResearch(ResearchStatus.DRAFT);
            research.setSurveyResearch(survey);
            stubOwnedResearch(researchId);
            when(surveySessionRepository.existsActiveSession(
                    eq(idSurvey), eq(SurveySessionStatus.IN_PROGRESS), any(LocalDateTime.class)))
                    .thenReturn(false);

            service.deleteResearch(researchId);

            InOrder cascade = inOrder(entityManager, fileStorageService, researchRepository);
            cascade.verify(entityManager).flush();
            cascade.verify(entityManager).clear();
            cascade.verify(fileStorageService).deleteSurveyImages(idSurvey);
            cascade.verify(researchRepository).delete(research);
            cascade.verify(entityManager).flush();
            cascade.verify(entityManager).clear();
        }

        /** Research без survey: удаляется только он, файлы и survey не трогаются. */
        @Test
        void withoutSurvey_deletesOnlyResearch() {
            var researchId = research.getIDResearch();
            research.setStatusResearch(ResearchStatus.DRAFT);
            research.setSurveyResearch(null);
            stubOwnedResearch(researchId);

            service.deleteResearch(researchId);

            verify(researchRepository).delete(research);
            verify(entityManager, times(2)).flush();
            verify(entityManager, times(2)).clear();
            verifyNoInteractions(fileStorageService, surveyRepository, surveySessionRepository);
        }

        /**
         * Статус запрещает удаление (RUNNING — canBeDeleted() == false,
         * подтверждено enum-тестами и интеграционно) — удаление данных
         * не начинается: ни файлы, ни survey, ни research не затронуты.
         * Ведущий flush/clear при этом выполняется всегда: он стоит до
         * всех проверок и в отказном сценарии тоже срабатывает — это
         * часть контракта, зафиксированная двумя verify ниже.
         */
        @Test
        void runningStatus_blocksDeletion_beforeAnyDataDeletion() {
            var researchId = research.getIDResearch();
            research.setStatusResearch(ResearchStatus.RUNNING);
            stubOwnedResearch(researchId);

            assertThatThrownBy(() -> service.deleteResearch(researchId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Нельзя удалить");

            verify(researchRepository, never()).delete(research);
            verifyNoInteractions(fileStorageService, surveyRepository, surveySessionRepository);
            verify(entityManager).flush();
            verify(entityManager).clear();
        }


        /**
         * Активная сессия (IN_PROGRESS, начата внутри окна) блокирует
         * удаление до любых удалений данных. Сервис не отличает, «насколько
         * недавно» начата сессия — это знание репозитория; сервис лишь
         * передаёт границу окна и реагирует на true.
         */
        @Test
        void activeSession_blocksDeletion() {
            var researchId = research.getIDResearch();
            var idSurvey = UUID.randomUUID();
            var survey = new Survey(idSurvey, "Опрос", "Описание", "Правила");
            research.setStatusResearch(ResearchStatus.DRAFT);
            research.setSurveyResearch(survey);
            stubOwnedResearch(researchId);
            when(surveySessionRepository.existsActiveSession(
                    eq(idSurvey), eq(SurveySessionStatus.IN_PROGRESS), any(LocalDateTime.class)))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.deleteResearch(researchId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Идёт прохождение опроса");

            verify(researchRepository, never()).delete(research);
            verifyNoInteractions(fileStorageService);
        }

        /**
         * Контракт окна: сервис передаёт границу «сейчас минус 30 минут».
         * Проверяем captured cutoff с допуском ±2 секунды — точность до
         * миллисекунды не часть контракта, а «примерно 30 минут назад» —
         * часть: брошенные сессии старше окна удалению не мешают.
         */
        @Test
        void cutoff_isNowMinus30Minutes() {
            var researchId = research.getIDResearch();
            var idSurvey = UUID.randomUUID();
            var survey = new Survey(idSurvey, "Опрос", "Описание", "Правила");
            research.setStatusResearch(ResearchStatus.DRAFT);
            research.setSurveyResearch(survey);
            stubOwnedResearch(researchId);
            when(surveySessionRepository.existsActiveSession(
                    eq(idSurvey), eq(SurveySessionStatus.IN_PROGRESS), any(LocalDateTime.class)))
                    .thenReturn(false);

            service.deleteResearch(researchId);

            ArgumentCaptor<LocalDateTime> cutoffCaptor =
                    ArgumentCaptor.forClass(LocalDateTime.class);
            verify(surveySessionRepository).existsActiveSession(
                    eq(idSurvey), eq(SurveySessionStatus.IN_PROGRESS), cutoffCaptor.capture());

            LocalDateTime expected = LocalDateTime.now().minus(Duration.ofMinutes(30));
            assertThat(cutoffCaptor.getValue())
                    .isCloseTo(expected, within(java.time.Duration.ofSeconds(2)));
        }

        /**
         * Сессии без активных в окне — каскад выполняется штатно. Файлы
         * опроса удаляются до сноса строк; COMPLETED-сессии любого возраста
         * и брошенные IN_PROGRESS старше окна блокировкой не являются.
         */
        @Test
        void staleOrCompletedSessions_doNotBlockDeletion() {
            var researchId = research.getIDResearch();
            var idSurvey = UUID.randomUUID();
            var survey = new Survey(idSurvey, "Опрос", "Описание", "Правила");
            research.setStatusResearch(ResearchStatus.DRAFT);
            research.setSurveyResearch(survey);
            stubOwnedResearch(researchId);
            when(surveySessionRepository.existsActiveSession(
                    eq(idSurvey), eq(SurveySessionStatus.IN_PROGRESS), any(LocalDateTime.class)))
                    .thenReturn(false);

            service.deleteResearch(researchId);

            verify(researchRepository).delete(research);
            verify(fileStorageService).deleteSurveyImages(idSurvey);
        }
    }

    @Nested
    class UpdateResearchInfo {

        /**
         * DRAFT: шапка обновляется целиком, репозиторий не трогается —
         * сущность управляемая, сохранение делает flush транзакции.
         * Исследование в setUp — spy, но стабить нечего: сеттеры и
         * геттеры делегируют реальным методам, поля читаются честно.
         */
        @Test
        void draftStatus_updatesAllFields_withoutRepositoryCall() {
            var researchId = research.getIDResearch();
            stubOwnedResearch(researchId);

            service.updateResearchInfo(researchId, new ResearchRequestInfoDtoPart1(
                    "Новое имя", "Новое описание", "Новый заказчик", TypeResearch.CPSK));

            assertThat(research.getNameResearch()).isEqualTo("Новое имя");
            assertThat(research.getDescriptionResearch()).isEqualTo("Новое описание");
            assertThat(research.getCustomerResearch()).isEqualTo("Новый заказчик");
            assertThat(research.getTypeResearch()).isEqualTo(TypeResearch.CPSK);
            verify(researchRepository, never()).save(any());
        }

        /** NEW — второй разрешённый статус; проверяем обе ветки условия canBeEdited. */
        @Test
        void newStatus_updatesFields() {
            var researchId = research.getIDResearch();
            research.setStatusResearch(ResearchStatus.NEW);
            stubOwnedResearch(researchId);

            service.updateResearchInfo(researchId, new ResearchRequestInfoDtoPart1(
                    "Новое имя", "Новое описание", "Новый заказчик", TypeResearch.CPSK));

            assertThat(research.getNameResearch()).isEqualTo("Новое имя");
            verify(researchRepository, never()).save(any());
        }

        /**
         * Запрещённые статусы: исключение бросается ДО сеттеров —
         * поля остаются старыми. Перечень через EnumSource сознательно
         * явный: если завтра в canBeEdited() кто-то добавит статус,
         * тест упадёт и заставит решить, куда этот статус попадает.
         */
        @ParameterizedTest(name = "статус {0}: редактирование запрещено, поля не меняются")
        @EnumSource(value = ResearchStatus.class,
                names = {"RUNNING", "PAUSE", "STOPPED", "ARCHIVE"})
        void forbiddenStatus_throws_beforeMutation(ResearchStatus status) {
            var researchId = research.getIDResearch();
            research.setStatusResearch(status);
            stubOwnedResearch(researchId);

            assertThatThrownBy(() -> service.updateResearchInfo(researchId,
                    new ResearchRequestInfoDtoPart1("Новое", "d", "c", TypeResearch.CPSK)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(status.name());

            assertThat(research.getNameResearch()).isEqualTo("Исследование");
            verifyNoInteractions(researchRepository);
        }

        /** Проверка владения выполняется первой — до любой мутации полей. */
        @Test
        void ownershipChecked_first() {
            stubOwnedResearch(research.getIDResearch());

            service.updateResearchInfo(research.getIDResearch(),
                    new ResearchRequestInfoDtoPart1("Н", "d", "c", TypeResearch.CPSK));

            verify(researchAccessService).getOwnedResearch(research.getIDResearch());
        }

        /** Чужое/несуществующее: исключение access-сервиса пробрасывается, репозиторий не тронут. */
        @Test
        void accessError_propagates_noRepositoryInteraction() {
            when(researchAccessService.getOwnedResearch(any()))
                    .thenThrow(new IllegalStateException("Исследование не найдено или нет доступа"));

            assertThatThrownBy(() -> service.updateResearchInfo(
                    UUID.randomUUID(), new ResearchRequestInfoDtoPart1("Н", "d", "c", TypeResearch.CPSK)))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(researchRepository);
        }
    }

    @Nested
    class GetProgressAll {

        /**
         * Happy path: два исследования — без опроса и с опросом. Сервис
         * группирует завершённые сессии по idSurvey и маппит только
         * исследования текущего психолога: без опроса — 0, с опросом —
         * счёт из группированной выборки.
         */
        @Test
        void mapsCompletedCounts_toOwnedResearches() {
            stubCurrentPsychologist();
            var withoutSurvey = new Research(
                    UUID.randomUUID(), "Без опроса", "d", "c", TypeResearch.CPSK);
            withoutSurvey.setSurveyResearch(null);

            var idSurvey = UUID.randomUUID();
            var withSurvey = new Research(
                    UUID.randomUUID(), "С опросом", "d", "c", TypeResearch.CPSK);
            withSurvey.setSurveyResearch(new Survey(idSurvey, "Опрос", "d", "r"));

            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of(withoutSurvey, withSurvey));
            when(surveySessionRepository.countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED))
                    .thenReturn(List.<Object[]>of(new Object[]{idSurvey, 5L}));

            var result = service.getProgressAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).idResearch()).isEqualTo(withoutSurvey.getIDResearch());
            assertThat(result.get(0).completedSessions()).isZero();
            assertThat(result.get(1).idResearch()).isEqualTo(withSurvey.getIDResearch());
            assertThat(result.get(1).completedSessions()).isEqualTo(5L);
        }

        /** Нет исследований — пустой список, группировка всё равно вызывается. */
        @Test
        void noResearches_returnsEmptyList() {
            stubCurrentPsychologist();
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of());
            when(surveySessionRepository.countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED))
                    .thenReturn(List.of());

            assertThat(service.getProgressAll()).isEmpty();
        }

        /**
         * Опрос есть, завершённых сессий нет: группировка отдаёт только
         * опросы с завершившимися — для остальных сервер обязан показать 0,
         * а не ошибку или отсутствие строки в ответе.
         */
        @Test
        void surveyWithoutCompletedSessions_defaultsToZero() {
            stubCurrentPsychologist();
            research.setSurveyResearch(
                    new Survey(UUID.randomUUID(), "Опрос", "d", "r"));
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of(research));
            when(surveySessionRepository.countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED))
                    .thenReturn(List.of());

            var result = service.getProgressAll();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().completedSessions()).isZero();
        }

        /**
         * Чужие сессии не прилипают: группировка содержит счёт по другому
         * idSurvey — моему исследованию достаётся 0. Это прямая проверка
         * маппинга по ключу группировки, самой вероятной точки ошибки.
         */
        @Test
        void foreignCompletedSessions_notCounted() {
            stubCurrentPsychologist();
            var idSurvey = UUID.randomUUID();
            research.setSurveyResearch(new Survey(idSurvey, "Опрос", "d", "r"));
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of(research));
            when(surveySessionRepository.countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{UUID.randomUUID(), 7L}));   // чужой опрос

            var result = service.getProgressAll();

            assertThat(result.getFirst().completedSessions()).isZero();
        }

        /** В репозиторий исследований уходит именно id текущего психолога. */
        @Test
        void repositoryCalledWithCurrentPsychologistId() {
            stubCurrentPsychologist();
            when(researchRepository.findAllByPsychologistAccountIdPsychologistAccount(
                    psychologist.getIdPsychologistAccount()))
                    .thenReturn(List.of());
            when(surveySessionRepository.countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED))
                    .thenReturn(List.of());

            service.getProgressAll();

            verify(researchRepository)
                    .findAllByPsychologistAccountIdPsychologistAccount(
                            psychologist.getIdPsychologistAccount());
        }
    }

}
