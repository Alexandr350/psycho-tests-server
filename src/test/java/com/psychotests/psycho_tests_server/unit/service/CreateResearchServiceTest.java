package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestCharacteristicsDtoPart3;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestSurveyDtoPart2;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import com.psychotests.psycho_tests_server.service.CurrentPsychologistProvider;
import com.psychotests.psycho_tests_server.service.ResearchAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.psychotests.psycho_tests_server.service.Utility.splitTokens;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 * Юнит-тесты CreateResearchService.
 *
 * <p>Что проверяем: сервис создаёт исследование по шагам — сначала карточку
 * исследования (research), затем опросник (survey), затем характеристики,
 * изображения и sub-изображения — и каждый шаг должен сохранить сущность
 * с корректными полями, привязанную к правильному родителю. Финальный шаг
 * (addItemsSub) дополнительно переводит исследование из статуса DRAFT в NEW.</p>
 *
 * <p>Как работает: весь Spring-контекст не поднимается. Все зависимости
 * сервиса (репозитории, ResearchAccessService, CurrentPsychologistProvider)
 * заменены моками Mockito (@Mock + @InjectMocks). Так как id генерирует сам
 * сервис через UUID.randomUUID(), а не база, метод save() репозиториев
 * стабится как identity — возвращает переданный объект, как это делает JPA
 * для сущности с уже назначенным id. Реально сохранённые сущности извлекаются
 * типизированными ArgumentCaptor'ами (@Captor) и проверяются assertj-ом:
 * поля из dto, привязка к survey/research, количество сохранённых элементов.</p>
 *
 * <p>Ожидания «владения» (что survey/research принадлежат текущему
 * психологу) не проверяются здесь намеренно: сервис делегирует их
 * ResearchAccessService, и в этих тестах он просто стабирован хелперами
 * stubOwnedResearch / stubOwnedSurvey. Отказы (не владеет, не найден) —
 * территория тестов ResearchAccessService.</p>
 *
 * <p>Исследователь спай: в тестах финального шага Research заменяется
 * spy-объектом, чтобы зафиксировать вызов changeStatus(NEW) как контракт
 * сервиса. Внутренние правила переходов статусов (guard'ы сущности)
 * здесь не проверяются — их место в тестах самой сущности Research.</p>
 *
 * <p>Особенность отладки: статический метод ResearchResponseInfoDto.from()
 * читает поля research геттерами, и на spy эти чтения считаются
 * взаимодействиями — поэтому verifyNoMoreInteractions на research не
 * применяется; вместо него проверяется содержимое возвращённого dto.</p>
 */

@ExtendWith(MockitoExtension.class)
class CreateResearchServiceTest {

    @Mock private ResearchRepository researchRepository;
    @Mock private SurveyRepository surveyRepository;
    @Mock private CharacteristicsRepository characteristicsRepository;
    @Mock private ItemsRepository itemsRepository;
    @Mock private ItemsSubRepository itemsSubRepository;
    @Mock private ResearchAccessService researchAccessService;
    @Mock private CurrentPsychologistProvider currentPsychologistProvider;

    @InjectMocks
    private CreateResearchService service;

    // Типизированные кэпторы: компилятор знает, что внутри List<Characteristics> и List<Items>
    @Captor private ArgumentCaptor<List<Characteristics>> characteristicsCaptor;
    @Captor private ArgumentCaptor<List<Items>> itemsCaptor;
    @Captor private ArgumentCaptor<List<ItemsSub>> itemsSubCaptor;

    private PsychologistAccount psychologist;
    private Research research;
    private Survey survey;

    @BeforeEach
    void setUp() {

        psychologist = new PsychologistAccount(
                UUID.randomUUID(), "Анна", "Иванова", "$2a$10$hashedvalue", "psy@mail.ru");
        research = new Research(
                UUID.randomUUID(), "Исследование", "Описание", "Заказчик", TypeResearch.CPSK);
        research.setPsychologistAccount(psychologist);
        survey = new Survey(UUID.randomUUID(), "Опрос", "Описание опроса", "Правила");
        survey.setResearch(research);
    }

    // --- Хелперы: стабы «владения» (void — возврат нигде не используется) ---

    /** Регистрирует, что research с данным id принадлежит текущему психологу. */
    private void stubOwnedResearch(UUID researchId) {
        when(researchAccessService.getOwnedResearch(researchId)).thenReturn(research);
    }

    /** Регистрирует, что survey с данным id принадлежит текущему психологу. */
    private void stubOwnedSurvey(UUID idSurvey) {
        when(researchAccessService.getOwnedSurvey(idSurvey)).thenReturn(survey);
    }

    @Nested
    class AddInfoResearch {

        /**
         * Happy path: research создаётся с полями из dto и привязывается
         * к текущему психологу из CurrentPsychologistProvider.
         * save() стабится как identity (возвращает переданный объект — как JPA),
         * поэтому entityId извлекается ArgumentCaptor'ом из реально сохранённой
         * сущности: id генерирует сервис, а не база.
         */
        @Test
        void savesResearchWithDtoFields_andLinkedToCurrentUser() {
            var dto = new ResearchRequestInfoDtoPart1(
                    "Название", "Описание", "Заказчик", TypeResearch.CPSK);
            when(currentPsychologistProvider.getPsychologistAccount()).thenReturn(psychologist);
            when(researchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var entityId = service.addInfoResearch(dto);

            var captor = ArgumentCaptor.forClass(Research.class);
            verify(researchRepository).save(captor.capture());
            var saved = captor.getValue();

            assertThat(entityId).isEqualTo(saved.getIDResearch());
            assertThat(saved.getNameResearch()).isEqualTo("Название");
            assertThat(saved.getDescriptionResearch()).isEqualTo("Описание");
            assertThat(saved.getCustomerResearch()).isEqualTo("Заказчик");
            assertThat(saved.getPsychologistAccount()).isSameAs(psychologist);
        }
    }

    @Nested
    class AddInfoSurvey {

        /**
         * Happy path: survey создаётся, привязывается к research, id возвращается.
         * Привязка проверяется isSameAs — сервис обязан положить именно ту
         * сущность, которую вернул researchAccessService, без пересборки.
         */
        @Test
        void savesSurveyLinkedToOwnedResearch() {
            var researchId = UUID.randomUUID();
            stubOwnedResearch(researchId);
            when(currentPsychologistProvider.getPsychologistAccount()).thenReturn(psychologist); // ← добавлено
            var dto = new ResearchRequestSurveyDtoPart2(
                    researchId, "Заголовок", "Описание", "Правила");
            when(surveyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var idSurvey = service.addInfoSurvey(dto);

            var captor = ArgumentCaptor.forClass(Survey.class);
            verify(surveyRepository).save(captor.capture());
            var saved = captor.getValue();

            assertThat(idSurvey).isEqualTo(saved.getIdSurvey());
            assertThat(saved.getTitleSurvey()).isEqualTo("Заголовок");
            assertThat(saved.getResearch()).isSameAs(research);
        }
    }

    @Nested
    class AddCharacteristics {

        /**
         * Характеристики приходят одной строкой (с валидацией по regexp
         * на dto), splitTokens делит её на токены, каждый токен становится
         * отдельной Characteristics, привязанной к survey.
         * Ожидаемое количество токенов берётся из самой splitTokens — так тест
         * проверяет делегирование, не дублируя её внутреннюю семантику
         * (какие разделители и как обрезаются пробелы — знание Utility).
         */
        @Test
        void savesOneCharacteristicsPerToken_linkedToSurvey() {
            var idSurvey = UUID.randomUUID();
            stubOwnedSurvey(idSurvey);
            var input = "тревожность, стресс";

            service.addCharacteristics(new ResearchRequestCharacteristicsDtoPart3(idSurvey, input));

            verify(characteristicsRepository).saveAll(characteristicsCaptor.capture());
            var saved = characteristicsCaptor.getValue();

            assertThat(saved).hasSameSizeAs(splitTokens(input));
            assertThat(saved).allSatisfy(c ->
                    assertThat(c.getSurvey()).isSameAs(survey));
        }

        /**
         * Пустой ввод не сохраняет ни одной характеристики.
         * Если тест упадёт — значит, splitTokens("") возвращает не пустой
         * список (например, список с одной пустой строкой): это раскрытие
         * реальной семантики Utility, а не ошибка теста.
         */
        @Test
        void emptyInput_savesNothing() {
            var idSurvey = UUID.randomUUID();
            stubOwnedSurvey(idSurvey);

            service.addCharacteristics(new ResearchRequestCharacteristicsDtoPart3(idSurvey, ""));

            verify(characteristicsRepository).saveAll(characteristicsCaptor.capture());
            assertThat(characteristicsCaptor.getValue()).isEmpty();
        }
    }

    @Nested
    class AddItems {

        /** Каждый путь из списка превращается в Items, привязанный к survey. */
        @Test
        void savesOneItemPerPath_linkedToSurvey() {
            var idSurvey = UUID.randomUUID();
            stubOwnedSurvey(idSurvey);
            var paths = List.of("img/1.png", "img/2.png");

            service.addItems(idSurvey, paths);

            verify(itemsRepository).saveAll(itemsCaptor.capture());
            var saved = itemsCaptor.getValue();

            assertThat(saved).hasSize(paths.size());
            assertThat(saved).allSatisfy(item ->
                    assertThat(item.getSurvey()).isSameAs(survey));
        }
    }

    @Nested
    class AddItemSub {

        /**
         * Финальный шаг создания: sub-изображения сохраняются, и research
         * переводится из DRAFT в NEW — это единственный метод из пяти,
         * который меняет статус (остальные работают внутри DRAFT).
         * Research — spy: проверяем ровно один вызов changeStatus(NEW)
         * как контракт сервиса, не влезая в guard'ы переходов
         * (их место — тесты сущности Research).
         */
        @Test
        void savesItemsSub_andTransitionsDraftToNew() {
            var idSurvey = UUID.randomUUID();
            var researchSpy = spy(research);
            survey.setResearch(researchSpy);
            stubOwnedSurvey(idSurvey);
            var paths = List.of("sub/1.png");

            var result = service.addItemSub(idSurvey, paths);

            verify(itemsSubRepository).saveAll(itemsSubCaptor.capture());
            assertThat(itemsSubCaptor.getValue()).allSatisfy(i ->
                    assertThat(i.getSurvey()).isSameAs(survey));
            verify(researchSpy, times(1)).changeStatus(ResearchStatus.NEW);

            // DTO собран из того же research: геттеры, которые дергает from()
            assertThat(result).isNotNull();
            assertThat(result.idResearch()).isEqualTo(research.getIDResearch());
            assertThat(result.nameResearch()).isEqualTo(research.getNameResearch());
        }

    }

    @Nested
    class UpdateSurvey {

        /**
         * Happy path: существующий survey опроса перезаписывается полями
         * из dto, а не создаётся заново (резюме черновика — PATCH /survey).
         * Сервис берёт managed-сущность через ResearchAccessService и мутирует
         * её поля — сохранение происходит за счёт dirty checking внутри
         * транзакции, явный surveyRepository.save не вызывается. Поэтому
         * проверяется состояние самой сущности, которую вернул стаб, а не
         * факт вызова save.
         */

        @Test
        void updatesFieldsOfExistingSurvey() {
            var idSurvey = UUID.randomUUID();
            stubOwnedSurvey(idSurvey);
            var dto = new ResearchRequestSurveyDtoPart2(
                    research.getIDResearch(), "Новый заголовок", "Новое описание", "Новые правила");

            service.updateSurvey(idSurvey, dto);

            // сервис обязан обновить именно ту сущность, которую вернул
            // ResearchAccessService, — пересборка здесь была бы ошибкой
            assertThat(survey.getTitleSurvey()).isEqualTo("Новый заголовок");
            assertThat(survey.getDescriptionSurvey()).isEqualTo("Новое описание");
            assertThat(survey.getRulesSurvey()).isEqualTo("Новые правила");
            verify(researchAccessService).getOwnedSurvey(idSurvey);
        }



        /**
         * Guard статуса: опрос правится только у черновика. Research
         * переводится в NEW напрямую через changeStatus сущности (гарантия
         * легального перехода), после чего updateSurvey обязан отбить
         * изменение исключением — и репозиторий не трогать вовсе.
         * Заметка: основное доказательство отклонения — исключение;
         * verify(never) остаётся регрессией на случай возврата save в код.
         */

        @Test
        void rejectsSurveyUpdate_whenResearchNotDraft() {
            research.changeStatus(ResearchStatus.NEW);
            var idSurvey = UUID.randomUUID();
            stubOwnedSurvey(idSurvey);
            var dto = new ResearchRequestSurveyDtoPart2(
                    research.getIDResearch(), "Заголовок", "Описание", "Правила");

            assertThatThrownBy(() -> service.updateSurvey(idSurvey, dto))
                    .isInstanceOf(IllegalStateException.class);

            verify(surveyRepository, never()).save(any());
        }
    }

}
