package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.*;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.service.CPSKTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты {@link CPSKTestService}: жизненный цикл сессии опроса
 * (создание, старт, сабмит ответов) — без Spring-контекста и базы данных.
 *
 * <h2>Модель изоляции волн</h2>
 * Сессия рождается внутри запуска (Run): сервис ищет открытый запуск
 * исследования и привязывает к нему и код доступа, и сессию. Код другого
 * запуска или код без запуска — 403; research не в RUNNING или открытого
 * запуска нет — 404 «Опрос недоступен». Повторный вход под тем же кодом
 * возвращает ту же сессию (уникальность респондент+запуск).
 *
 * <h2>Как устроены заглушки</h2>
 * <ul>
 *   <li><b>Сессия — stateful-мок.</b> {@code getStatusSurveySession} читает
 *       поле через {@code AtomicReference}, {@code setStatusSurveySession}
 *       его меняет — переходы статусов (NOT_STARTED → IN_PROGRESS →
 *       COMPLETED) проверяются реалистично, как на живой JPA-сущности.</li>
 *   <li><b>Коллекции ответов</b> — реальные изменяемые списки,
 *       пересоздаваемые в {@code @BeforeEach}; сервис добавляет в них
 *       сущности, тесты проверяют содержимое.</li>
 *   <li><b>Эхо-стабы save</b> ({@code surveySessionRepository} и
 *       {@code respondentRepository}): JPA-репозиторий возвращает
 *       сохраняемую сущность — мок делает то же. Без этого
 *       {@code orElseGet} в createSession раздаёт null, и ответ строится
 *       от null. Проверки «save не звался» (never) это не отменяют:
 *       эхо-стаб влияет только на возвращаемое значение.</li>
 * </ul>
 *
 * <h2>Хелперы и их роли</h2>
 * <ul>
 *   <li>{@code session(...)} — собирает сессию с заданным опросом и статусом;
 *       запоминает опрос в {@code sessionSurvey} для стабов контента и стабит
 *       research опроса в RUNNING (гейт доступности в startFlow).</li>
 *   <li>{@code stubSurveyAndCode(...)} — сборка «research + открытый запуск +
 *       опрос + код»; параметризуется статусом research и принадлежностью
 *       кода к запуску. Запоминает запуск в поле {@code run}.</li>
 *   <li>{@code stubHappyPath()} — happy path: research в RUNNING, код
 *       относится к открытому запуску.</li>
 *   <li>{@code stubCodeWithoutRun()} — код с run = null: исторические коды
 *       с nullable id_run; сервис обязан отвечать 403, а не NPE.</li>
 *   <li>{@code stubFlowContent()} — наполняет опрос характеристиками,
 *       изображениями и суб-изображениями для тестов старта и сабмита.</li>
 *   <li>{@code assertRejected(...)} — единая проверка «ожидаем 400 с таким
 *       текстом и ноль сохранений сессии».</li>
 * </ul>
 *
 * <h2>Что покрывают вложенные классы</h2>
 * <ul>
 *   <li>{@code CreateSession} — 404 на неизвестный опрос/код и на
 *       недоступный опрос (research не в RUNNING / открытого запуска нет),
 *       403 на код чужого запуска и код без запуска, создание и
 *       переиспользование респондента, анонимный вход.</li>
 *   <li>{@code StartFlow} — 404/409 на невалидные переходы статусов,
 *       запись возраста и пола респондента, состав флоу на выходе.</li>
 *   <li>{@code Submit} — валидация покрытия и дубликатов для всех четырёх
 *       типов ответов, порядок проверок, границы лимита ассоциаций,
 *       happy path с сохранением и завершением сессии.</li>
 * </ul>
 *
 * <p>Граница с другими тестами: flush-поведение, составная уникальность
 * сессии (респондент + запуск) и каскады — {@code @DataJpaTest}. Контроллер и
 * маппинг DTO — веб-слой. Копилка отложенных решений: имя компонента
 * {@code minAssociationsPerSector} в InfoResponseDto лжёт о семантике
 * (значение — максимум на сектор, 5).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CPSKTestServiceTest {

    /** Кортеж стабов «код + опрос + research + запуск»: собирается вместе,
     * чтобы тесты могли проверять согласованность привязок (isSameAs). */
    private record CodeStub(UniqueSurveyCode code, Survey survey, Research research, Run run) {}

    private static final UUID SURVEY_ID = UUID.randomUUID();
    private static final UUID OTHER_SURVEY_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CODE_ID = UUID.randomUUID();
    private static final UUID RUN_ID = UUID.randomUUID();

    private static final UUID CH1 = UUID.randomUUID();
    private static final UUID CH2 = UUID.randomUUID();
    private static final UUID I1 = UUID.randomUUID();
    private static final UUID I2 = UUID.randomUUID();
    private static final UUID S1 = UUID.randomUUID();
    private static final UUID S2 = UUID.randomUUID();

    @Mock private SurveyRepository surveyRepository;
    @Mock private SurveySessionRepository surveySessionRepository;
    @Mock private RespondentRepository respondentRepository;
    @Mock private UniqueSurveyCodeRepository uniqueSurveyCodeRepository;
    @Mock private CharacteristicsRepository characteristicsRepository;
    @Mock private ItemsRepository itemsRepository;
    @Mock private ItemsSubRepository itemsSubRepository;
    @Mock private RunRepository runRepository;

    @InjectMocks private CPSKTestService service;

    // Состояние, пересобираемое на каждый тест
    private Survey sessionSurvey;
    private Run run;
    private Respondent respondent;
    private List<AnswersCharacteristics> characteristicsAnswers;
    private List<AnswersItems> itemsAnswers;
    private List<AnswersItemsSubs> subsAnswers;
    private List<AnswersAssociations> associationsAnswers;

    private Characteristics c1;
    private Characteristics c2;
    private Items it1;
    private Items it2;
    private ItemsSub itS1;
    private ItemsSub itS2;

    @BeforeEach
    void resetState() {
        run = null;
        respondent = mock(Respondent.class);
        c1 = characteristic(CH1, "Оптимист");
        c2 = characteristic(CH2, "Спокойный");
        it1 = item(I1, "/i1.jpg");
        it2 = item(I2, "/i2.jpg");
        itS1 = subItem(S1, "/s1.jpg");
        itS2 = subItem(S2, "/s2.jpg");
        characteristicsAnswers = new ArrayList<>();
        itemsAnswers = new ArrayList<>();
        subsAnswers = new ArrayList<>();
        associationsAnswers = new ArrayList<>();

        // Эхо-стабы: JPA save возвращает сохраняемую сущность — мок так же.
        // Без них orElseGet в createSession раздаёт null, и ответ строится
        // от null. Проверки «save не звался» (never) это не отменяют.
        when(surveySessionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(respondentRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- builders ----------

    /**
     * Stateful-мок сессии: getStatusSurveySession читает поле, setStatus —
     * меняет. Research опроса стабится в RUNNING — сервис в startFlow
     * проверяет гейт доступности после проверки статуса сессии, и без
     * стаба успешные тесты падали бы с NPE.
     */
    private SurveySession session(UUID idSurvey, SurveySessionStatus initial) {
        Research runningResearch = mock(Research.class);
        when(runningResearch.getStatusResearch()).thenReturn(ResearchStatus.RUNNING);

        sessionSurvey = mock(Survey.class);
        when(sessionSurvey.getIdSurvey()).thenReturn(idSurvey);
        when(sessionSurvey.getResearch()).thenReturn(runningResearch);

        SurveySession session = mock(SurveySession.class);
        AtomicReference<SurveySessionStatus> status = new AtomicReference<>(initial);
        when(session.getIdSurveySession()).thenReturn(SESSION_ID);
        when(session.getSurvey()).thenReturn(sessionSurvey);
        when(session.getRespondent()).thenReturn(respondent);
        when(session.getStatusSurveySession()).thenAnswer(inv -> status.get());
        doAnswer(inv -> { status.set(inv.getArgument(0)); return null; })
                .when(session).setStatusSurveySession(any());
        when(session.getAnswersCharacteristics()).thenReturn(characteristicsAnswers);
        when(session.getAnswersItems()).thenReturn(itemsAnswers);
        when(session.getAnswersItemsSubs()).thenReturn(subsAnswers);
        when(session.getAnswersAssociations()).thenReturn(associationsAnswers);
        return session;
    }

    /** Сессия доступна по id: стаб findById на общий SESSION_ID. */
    private void sessionExists(SurveySession session) {
        when(surveySessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
    }

    private Characteristics characteristic(UUID id, String text) {
        Characteristics c = mock(Characteristics.class);
        when(c.getIdCharacteristic()).thenReturn(id);
        when(c.getTextCharacteristic()).thenReturn(text);
        return c;
    }

    private Items item(UUID id, String url) {
        Items i = mock(Items.class);
        when(i.getIdItem()).thenReturn(id);
        when(i.getUrlItem()).thenReturn(url);
        return i;
    }

    private ItemsSub subItem(UUID id, String url) {
        ItemsSub s = mock(ItemsSub.class);
        when(s.getIdItemSub()).thenReturn(id);
        when(s.getUrlItemSub()).thenReturn(url);
        return s;
    }

    /** Полный набор характеристик — покрытие для happy-path сабмита. */
    private List<CharacteristicAnswerDto> allCharacteristics() {
        return List.of(new CharacteristicAnswerDto(CH1, 1), new CharacteristicAnswerDto(CH2, 2));
    }

    /** Полный набор изображений — покрытие для happy-path сабмита. */
    private List<ImageAnswerDto> allImages() {
        return List.of(new ImageAnswerDto(I1, 1), new ImageAnswerDto(I2, 2));
    }

    /** Полный набор суб-изображений (один) — покрытие для happy-path сабмита. */
    private List<ImageAnswerDto> allSubImages() {
        return List.of(new ImageAnswerDto(S1, 1));
    }

    private SurveyFlowSubmitRequestDto submitDto(List<CharacteristicAnswerDto> ch,
                                                 List<ImageAnswerDto> img,
                                                 List<ImageAnswerDto> sub,
                                                 List<AssociationAnswerDto> assoc) {
        return new SurveyFlowSubmitRequestDto(SESSION_ID, ch, img, sub, assoc);
    }

    /** Контент опроса согласован с набором allXxx(): полное покрытие проходит. */
    private void stubFlowContent() {
        when(characteristicsRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(c1, c2));
        when(itemsRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(it1, it2));
        when(itemsSubRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(itS1));
    }

    /**
     * Единая проверка отказа валидации: 400 с точным текстом и ноль
     * сохранений сессии — валидация обязана срабатывать до записи.
     */
    private void assertRejected(String reason, Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo(reason);
                });
        verify(surveySessionRepository, never()).save(any());
    }

    private SurveyFlowQuestionnaireRequestDto questionnaire() {
        return new SurveyFlowQuestionnaireRequestDto(SESSION_ID, 30, "Ж");
    }

    /** Сессия в IN_PROGRESS, доступна по id — база для тестов сабмита. */
    private SurveySession givenActiveSession() {
        SurveySession session = session(SURVEY_ID, SurveySessionStatus.IN_PROGRESS);
        sessionExists(session);
        return session;
    }

    /** count ассоциаций в одном секторе — генератор для проверок лимита. */
    private List<AssociationAnswerDto> associations(int sector, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new AssociationAnswerDto(sector, "a" + i))
                .toList();
    }

    /**
     * Собирает «research + открытый запуск + опрос + код». Код привязывается
     * к запуску или к чужому моку запуска — так проверяется изоляция волн.
     * Запоминает запуск в поле run для проверок привязки сессии.
     */
    private CodeStub stubSurveyAndCode(ResearchStatus researchStatus, boolean codeFromSameRun) {
        Research research = mock(Research.class);
        when(research.getIDResearch()).thenReturn(UUID.randomUUID());
        when(research.getStatusResearch()).thenReturn(researchStatus);

        Survey survey = mock(Survey.class);
        when(survey.getIdSurvey()).thenReturn(SURVEY_ID);
        when(survey.getResearch()).thenReturn(research);
        when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(survey));

        run = mock(Run.class);
        when(run.getIdRun()).thenReturn(RUN_ID);
        when(runRepository.findOpenRun(research.getIDResearch())).thenReturn(Optional.of(run));

        UniqueSurveyCode code = mock(UniqueSurveyCode.class);
        when(code.getRun()).thenReturn(codeFromSameRun ? run : mock(Run.class));
        when(uniqueSurveyCodeRepository.findById(CODE_ID)).thenReturn(Optional.of(code));
        return new CodeStub(code, survey, research, run);
    }

    /** Happy path: research в RUNNING, код относится к открытому запуску. */
    private CodeStub stubHappyPath() {
        return stubSurveyAndCode(ResearchStatus.RUNNING, true);
    }

    /**
     * Код без привязки к запуску: колонка id_run nullable, исторические коды
     * законны. Точка проверки — guard сервиса: code.getRun() == null ||
     * !run.getIdRun().equals(...) → 403, а не NPE.
     */
    private CodeStub stubCodeWithoutRun() {
        var stub = stubSurveyAndCode(ResearchStatus.RUNNING, false);
        when(stub.code().getRun()).thenReturn(null);
        return stub;
    }

    // ================= createSession =================

    @Nested
    class CreateSession {

        /** Опрос не найден: 404, до кода и сессии дело не доходит. */
        @Test
        void unknownSurvey_returns404() {
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, null))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос не найден");

            verify(surveySessionRepository, never()).save(any());
        }

        /**
         * Research не в RUNNING: 404 «Опрос недоступен». Гейт стоит до
         * поиска запуска и кода — ни респондент, ни сессия не создаются.
         */
        @Test
        void researchNotRunning_returns404() {
            stubSurveyAndCode(ResearchStatus.NEW, true);

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, null))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос недоступен");

            verify(surveySessionRepository, never()).save(any());
        }

        /**
         * Открытого запуска нет (закрыт/не создан): 404 «Опрос недоступен».
         * Перезапись стаба хелпера Optional.empty() — падение происходит
         * именно на втором гейте, после RUNNING-проверки.
         */
        @Test
        void noOpenRun_returns404() {
            stubHappyPath();
            when(runRepository.findOpenRun(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, null))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Опрос недоступен");

            verify(surveySessionRepository, never()).save(any());
        }

        /**
         * Код не найден в репозитории: 404 «Код не найден», респондент и
         * сессия не создаются. Хелпер стабит оба гейта доступности, чтобы
         * падение произошло именно на проверке кода.
         */
        @Test
        void unknownCode_returns404() {
            stubHappyPath();
            when(uniqueSurveyCodeRepository.findById(CODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, CODE_ID))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Код не найден");

            verify(respondentRepository, never()).save(any());
        }

        /**
         * Код из другого запуска: 403 — защита от прохода по чужой волне.
         * Изоляция данных живёт на Run: код выпуска конкретного запуска.
         */
        @Test
        void codeFromAnotherRun_returns403() {
            stubSurveyAndCode(ResearchStatus.RUNNING, false);

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, CODE_ID))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("не относится к этому запуску");

            verify(respondentRepository, never()).save(any());
        }

        /**
         * Код без привязки к запуску: 403, а не 500. Колонка id_run у кода
         * nullable — исторические коды без запуска законны. Guard сервиса
         * проверяет code.getRun() == null первой частью ||, иначе — NPE на
         * code.getRun().getIdRun() (ловилось этим тестом дважды).
         */
        @Test
        void codeWithoutRun_returns403() {
            stubCodeWithoutRun();

            assertThatThrownBy(() -> service.createSession(SURVEY_ID, CODE_ID))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("не относится к этому запуску");

            verify(respondentRepository, never()).save(any());
            verify(surveySessionRepository, never()).save(any());
        }

        /**
         * Код без респондента: создаётся новый респондент, привязывается
         * к коду (code.setRespondent) и к новой сессии в статусе NOT_STARTED.
         * Сессия привязывается и к запуску — изоляция волн. id сессии в
         * ответе совпадает с id сохранённой (эхо-стаб делает save идемпотентным
         * по ссылке — isSameAs корректен).
         */
        @Test
        void codeWithoutRespondent_createsAndLinksRespondent() {
            var stub = stubHappyPath();
            when(stub.code().getRespondent()).thenReturn(null);

            var result = service.createSession(SURVEY_ID, CODE_ID);

            ArgumentCaptor<Respondent> savedRespondent = ArgumentCaptor.forClass(Respondent.class);
            verify(respondentRepository).save(savedRespondent.capture());
            verify(stub.code()).setRespondent(savedRespondent.getValue()); // тот же респондент уехал в код

            ArgumentCaptor<SurveySession> savedSession = ArgumentCaptor.forClass(SurveySession.class);
            verify(surveySessionRepository).save(savedSession.capture());
            assertThat(savedSession.getValue().getStatusSurveySession()).isEqualTo(SurveySessionStatus.NOT_STARTED);
            assertThat(savedSession.getValue().getSurvey()).isSameAs(stub.survey());
            assertThat(savedSession.getValue().getRun()).isSameAs(stub.run());
            assertThat(savedSession.getValue().getRespondent()).isSameAs(savedRespondent.getValue());
            assertThat(result.idSession()).isEqualTo(savedSession.getValue().getIdSurveySession());
        }

        /**
         * Код с уже привязанным респондентом: повторный вход под тем же кодом
         * переиспользует респондента, а не плодит нового (setRespondent не зовётся).
         */
        @Test
        void codeWithRespondent_reusesExistingRespondent() {
            Respondent existing = mock(Respondent.class);
            var stub = stubHappyPath();
            when(stub.code().getRespondent()).thenReturn(existing);

            service.createSession(SURVEY_ID, CODE_ID);

            verify(respondentRepository).save(existing);
            verify(stub.code(), never()).setRespondent(any());
        }

        /**
         * Без кода: анонимный респондент. Сервис не обращается к репозиторию
         * уникальных кодов вовсе, создаёт нового респондента и сохраняет
         * сессию в NOT_STARTED с привязкой к запуску. Гейты доступности
         * (RUNNING + открытый запуск) при этом всё равно проверяются.
         */
        @Test
        void withoutCode_savesAnonymousRespondent() {
            var stub = stubHappyPath();

            service.createSession(SURVEY_ID, null);

            verify(uniqueSurveyCodeRepository, never()).findById(any());
            verify(respondentRepository).save(any(Respondent.class));

            ArgumentCaptor<SurveySession> savedSession = ArgumentCaptor.forClass(SurveySession.class);
            verify(surveySessionRepository).save(savedSession.capture());
            assertThat(savedSession.getValue().getStatusSurveySession()).isEqualTo(SurveySessionStatus.NOT_STARTED);
            assertThat(savedSession.getValue().getRun()).isSameAs(stub.run());
        }
    }

    // ================= startFlow =================

    @Nested
    class StartFlow {

        /** Сессия не найдена: 404 «Сессия не найдена». */
        @Test
        void unknownSession_returns404() {
            when(surveySessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startFlow(SURVEY_ID, questionnaire()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Сессия не найдена");
        }

        /**
         * Сессия существует, но относится к другому опросу: 404. Скрытие
         * факта существования сессии (а не 403) — осознанная политика:
         * чужой id не отличим от несуществующего.
         */
        @Test
        void sessionFromAnotherSurvey_returns404() {
            sessionExists(session(OTHER_SURVEY_ID, SurveySessionStatus.NOT_STARTED));

            assertThatThrownBy(() -> service.startFlow(SURVEY_ID, questionnaire()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("не относится к этому опросу");
        }

        /** Сессия уже в IN_PROGRESS: 409 «Сессия уже начата» — повторный старт запрещён. */
        @Test
        void sessionInProgress_returns409() {
            sessionExists(session(SURVEY_ID, SurveySessionStatus.IN_PROGRESS));

            assertThatThrownBy(() -> service.startFlow(SURVEY_ID, questionnaire()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("Сессия уже начата");
        }

        /** Завершённая сессия (COMPLETED) тоже не может стартовать заново: 409. */
        @Test
        void sessionCompleted_returns409() {
            sessionExists(session(SURVEY_ID, SurveySessionStatus.COMPLETED));

            assertThatThrownBy(() -> service.startFlow(SURVEY_ID, questionnaire()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("Сессия уже начата");
        }

        /**
         * Успешный старт: возраст и пол пишутся в респондента, статус
         * меняется на IN_PROGRESS, ответ содержит полный состав флоу —
         * характеристики, изображения, суб-изображения и константы
         * (8 секторов, максимум 5 ассоциаций).
         */
        @Test
        void success_setsRespondentData_andReturnsFlowStructure() {
            SurveySession session = session(SURVEY_ID, SurveySessionStatus.NOT_STARTED);
            sessionExists(session);
            stubFlowContent();

            var result = service.startFlow(SURVEY_ID, questionnaire());

            verify(respondent).setAgeRespondent(30);
            verify(respondent).setGenderRespondent("Ж");
            assertThat(session.getStatusSurveySession()).isEqualTo(SurveySessionStatus.IN_PROGRESS);

            assertThat(result.characteristics()).hasSize(2);
            assertThat(result.characteristics().getFirst().question()).isEqualTo("Оптимист");
            assertThat(result.items()).hasSize(2);
            assertThat(result.items().getFirst().imageUrl()).isEqualTo("/i1.jpg");
            assertThat(result.itemsSub()).hasSize(1);
            assertThat(result.info().sectorCount()).isEqualTo(8);
            // Имя компонента minAssociationsPerSector историческое; семантика —
            // максимум (копилка: переименовать в maxAssociationsPerSector)
            assertThat(result.info().minAssociationsPerSector()).isEqualTo(5);
        }
    }

    // ================= submit =================

    @Nested
    class Submit {

        /** Сессия не найдена: 404, ничего не сохраняется. */
        @Test
        void unknownSession_returns404() {
            when(surveySessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(allCharacteristics(), allImages(), allSubImages(), List.of())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("Сессия не найдена");

            verify(respondentRepository, never()).save(any());
        }

        /** Сессия от другого опроса: 404 «Сессия не относится к этому опросу». */
        @Test
        void sessionFromAnotherSurvey_returns404() {
            sessionExists(session(OTHER_SURVEY_ID, SurveySessionStatus.IN_PROGRESS));

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(allCharacteristics(), allImages(), allSubImages(), List.of())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("не относится к этому опросу");
        }

        /** Сессия ещё не начата (NOT_STARTED): 409 — нельзя сдавать ответы до старта. */
        @Test
        void sessionNotStarted_returns409() {
            SurveySession session = session(SURVEY_ID, SurveySessionStatus.NOT_STARTED);
            sessionExists(session);

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(allCharacteristics(), allImages(), allSubImages(), List.of())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("Результаты уже отправлены или сессия не начата");
        }

        /** Повторный сабмит завершённой сессии: 409 — данные не перезаписываются. */
        @Test
        void sessionAlreadyCompleted_returns409() {
            sessionExists(session(SURVEY_ID, SurveySessionStatus.COMPLETED));

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(allCharacteristics(), allImages(), allSubImages(), List.of())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("Результаты уже отправлены или сессия не начата");
        }

        /**
         * Шесть ассоциаций в одном секторе: 400 с номером сектора, лимитом (5)
         * и фактическим количеством (6) в тексте ошибки. Валидация проходит
         * до сохранений: статус не тронут, ничего не записано.
         */
        @Test
        void tooManyAssociations_returns400_withSectorNumber() {
            SurveySession session = givenActiveSession();
            List<AssociationAnswerDto> six = associations(3, 6);

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(List.of(), List.of(), List.of(), six)))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                    .hasMessageContaining("Сектор 3")
                    .hasMessageContaining("6")
                    .hasMessageContaining("5");

            assertThat(session.getStatusSurveySession()).isEqualTo(SurveySessionStatus.IN_PROGRESS);
            assertThat(associationsAnswers).isEmpty();
        }

        /**
         * Порядок валидаций: проверка ассоциаций выполняется ПЕРЕД проверками
         * характеристик. При двух нарушениях сразу клиент видит ошибку
         * сектора — детерминированный контракт для фронта.
         */
        @Test
        void associationCheckRunsBeforeOtherValidations() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));

            List<AssociationAnswerDto> six = associations(1, 6);

            assertThatThrownBy(() -> service.submit(SURVEY_ID,
                    submitDto(List.of(new CharacteristicAnswerDto(CH1, 1),
                                    new CharacteristicAnswerDto(CH1, 2)), // дубликат
                            List.of(), List.of(), six)))
                    .hasMessageContaining("Сектор 1"); // не про дубликаты
        }

        /**
         * Дубликат характеристики (один id дважды): 400 «Дублирующиеся ответы
         * характеристикам». Дубликат ловится сравнением размера списка и set.
         */
        @Test
        void duplicateCharacteristics_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1, c2));

            assertRejected("Дублирующиеся ответы характеристикам",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1),
                                            new CharacteristicAnswerDto(CH1, 2)),
                                    List.of(), List.of(), List.of())));
        }

        /**
         * Неполное покрытие характеристик (отправлен только CH1 из двух): 400
         * «Ответы не соответствуют характеристикам теста». Сюда же попадает и
         * неизвестный id: sentIds != allowed — один контракт на оба случая.
         */
        @Test
        void incompleteCharacteristics_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1, c2));

            assertRejected("Ответы не соответствуют характеристикам теста",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1)),
                                    List.of(), List.of(), List.of())));
        }

        /**
         * Неизвестный id характеристики (нет среди разрешённых): тот же
         * контракт полного покрытия — 400, чужие id не сохраняются.
         */
        @Test
        void unknownCharacteristicId_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));

            assertRejected("Ответы не соответствуют характеристикам теста",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(UUID.randomUUID(), 1)),
                                    List.of(), List.of(), List.of())));
        }

        /** Дубликат изображения: 400 «Дублирующиеся ответы изображениям». */
        @Test
        void duplicateImages_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));
            when(itemsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(it1, it2));

            assertRejected("Дублирующиеся ответы изображениям",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1)),
                                    List.of(new ImageAnswerDto(I1, 1), new ImageAnswerDto(I1, 2)),
                                    List.of(), List.of())));
        }

        /** Неполное покрытие изображений: 400 «Ответы не соответствуют изображениям». */
        @Test
        void incompleteImages_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));
            when(itemsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(it1, it2));

            assertRejected("Ответы не соответствуют изображениям теста",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1)),
                                    List.of(new ImageAnswerDto(I1, 1)),
                                    List.of(), List.of())));
        }

        /**
         * Дубликат суб-изображения: 400 «Дублирующиеся ответы суб-изображениям».
         * Суб-изображения приходят тем же ImageAnswerDto — контракт тот же.
         */
        @Test
        void duplicateSubImages_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));
            when(itemsRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(it1));
            when(itemsSubRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(itS1));

            assertRejected("Дублирующиеся ответы суб-изображениям",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1)),
                                    List.of(new ImageAnswerDto(I1, 1)),
                                    List.of(new ImageAnswerDto(S1, 1), new ImageAnswerDto(S1, 2)),
                                    List.of())));
        }

        /** Неполное покрытие суб-изображений: 400 «Ответы не соответствуют суб-изображениям». */
        @Test
        void incompleteSubImages_returns400() {
            givenActiveSession();
            when(characteristicsRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(c1));
            when(itemsRepository.findBySurvey(sessionSurvey)).thenReturn(List.of(it1));
            when(itemsSubRepository.findBySurvey(sessionSurvey))
                    .thenReturn(List.of(itS1, itS2));

            assertRejected("Ответы не соответствуют суб-изображениям теста",
                    () -> service.submit(SURVEY_ID,
                            submitDto(List.of(new CharacteristicAnswerDto(CH1, 1)),
                                    List.of(new ImageAnswerDto(I1, 1)),
                                    List.of(new ImageAnswerDto(S1, 1)),
                                    List.of())));
        }

        /**
         * Успешный сабмит: все четыре типа ответов записаны в коллекции сессии,
         * тексты ассоциаций обрезаны от пробелов, статус становится COMPLETED,
         * фиксируется время завершения. Геттер стаба answersItemsSubs обязан
         * совпадать с реальным именем коллекции сущности — иначе список
         * «подменяется» мимо стаба и тест ловит 0 вместо 1.
         */
        @Test
        void success_savesAllAnswers_andCompletes() {
            SurveySession session = givenActiveSession();
            stubFlowContent();

            service.submit(SURVEY_ID, submitDto(
                    List.of(new CharacteristicAnswerDto(CH1, 1), new CharacteristicAnswerDto(CH2, 2)),
                    List.of(new ImageAnswerDto(I1, 1), new ImageAnswerDto(I2, 2)),
                    List.of(new ImageAnswerDto(S1, 1)),
                    List.of(new AssociationAnswerDto(1, "  солнце "),
                            new AssociationAnswerDto(1, "море"),
                            new AssociationAnswerDto(2, "лес"))));

            assertThat(characteristicsAnswers).hasSize(2);
            assertThat(itemsAnswers).hasSize(2);
            assertThat(subsAnswers).hasSize(1);
            assertThat(associationsAnswers).hasSize(3);
            assertThat(associationsAnswers.getFirst().getAnswer()).isEqualTo("солнце"); // trim сработал
            assertThat(session.getStatusSurveySession()).isEqualTo(SurveySessionStatus.COMPLETED);
            verify(session).setFinishTimeSession(any(LocalDateTime.class));
        }

        /**
         * Ровно пять ассоциаций в секторе — на границе лимита — сохраняются:
         * ограничение «максимум 5» строгое сверху, пятая проходит.
         */
        @Test
        void exactlyFiveAssociations_ok() {
            SurveySession session = givenActiveSession();
            stubFlowContent();

            List<AssociationAnswerDto> five = associations(1, 5);

            service.submit(SURVEY_ID, submitDto(allCharacteristics(), allImages(), allSubImages(), five));

            assertThat(associationsAnswers).hasSize(5);
            assertThat(session.getStatusSurveySession()).isEqualTo(SurveySessionStatus.COMPLETED);
        }

        /** Пустой список ассоциаций — валидный кейс: опрос без ассоциаций завершается. */
        @Test
        void emptyAssociations_stillCompletes() {
            SurveySession session = givenActiveSession();
            stubFlowContent();

            service.submit(SURVEY_ID, submitDto(allCharacteristics(), allImages(), allSubImages(), List.of()));

            assertThat(associationsAnswers).isEmpty();
            assertThat(session.getStatusSurveySession()).isEqualTo(SurveySessionStatus.COMPLETED);
        }
    }
}
