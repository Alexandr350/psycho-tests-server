package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.exception.ResearchNotFoundException;
import com.psychotests.psycho_tests_server.exception.UnauthorizedException;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;
import com.psychotests.psycho_tests_server.service.CurrentPsychologistProvider;
import com.psychotests.psycho_tests_server.service.ResearchAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты ResearchAccessService.
 *
 * <p>Что проверяем: два метода — getOwnedResearch() и getOwnedSurvey() —
 * единую точку контроля владения для всех операций с исследованиями и
 * опросами. Создание, чтение и изменение сущностей проходят через эти
 * методы, поэтому здесь фиксируется их контракт: сущность возвращается
 * только её владельцу, а всем остальным — единый отказ
 * ResearchNotFoundException. «Не существует» и «не ваше» для клиента
 * неотличимы — так защита от перечисления чужих id (enumeration):
 * чужой id не выдаёт наличие объекта.</p>
 *
 * <p>Как работает: Spring-контекст не поднимается. Зависимости
 * (два репозитория и CurrentPsychologistProvider) заменены моками
 * Mockito (@Mock + @InjectMocks). Сам сервис владение не вычисляет —
 * оно зашито в запросы репозитория: findByIdAndPsychologistAccountId...
 * и findByIdAndResearchPsychologistAccountId... («survey принадлежит
 * владельцу родительского research, собственного владельца у него нет»).
 * Поэтому тест проверяет не логику фильтрации, а то, что сервис передаёт
 * в репозиторий правильную пару аргументов — id сущности и id текущего
 * психолога — и корректно обрабатывает пустой результат. Общий стаб
 * провайдера вынесен в хелпер stubCurrentPsychologist() и вызывается
 * в каждом тесте явно: так каждый тест самодостаточен, а strict stubbing
 * Mockito не ругается на неиспользованные стабы из setUp.</p>
 *
 * <p>Детали: локальные переменные вида var researchId = UUID.randomUUID()
 * объявлены один раз и используются и в стабе, и в вызове метода — два
 * независимых вызова randomUUID() дали бы рассинхрон аргументов, который
 * strict stubbing ловит как PotentialStubbingProblem (мы на этом уже
 * обжигались). Ветка «провайдер не смог определить текущего психолога»
 * проверена только для research: механика у обоих методов одинакова,
 * дублирование теста ради покрытия строк не оправдано. Отдельно проверено,
 * что при недоступном провайдере репозиторий не вызывается вовсе:
 * отказ должен происходить до обращения к БД.</p>
 *
 * <p>Граница с другими тестами: форма исключений (какое сообщение, какой
 * тип) — здесь; превращение их в HTTP-коды 403/404 — в тестах
 * контроллера и ExceptionHandler; поведение CurrentPsychologistProvider
 * в его собственных тестах; реальная фильтрация по владельцу на уровне
 * SQL — в интеграционных тестах репозиториев.</p>
 */

@ExtendWith(MockitoExtension.class)
class ResearchAccessServiceTest {

    @Mock private ResearchRepository researchRepository;
    @Mock private SurveyRepository surveyRepository;
    @Mock private CurrentPsychologistProvider psychologistProvider;

    @InjectMocks
    private ResearchAccessService service;

    private PsychologistAccount psychologist;
    private Research research;

    @BeforeEach
    void setUp() {
        psychologist = new PsychologistAccount(
                UUID.randomUUID(), "Анна", "Иванова", "$2a$10$hashedvalue", "psy@mail.ru");
        research = new Research(
                UUID.randomUUID(), "Исследование", "Описание", "Заказчик", TypeResearch.CPSK);
    }

    /** Стабит провайдер: текущий психолог — psychologist из setUp. */
    private void stubCurrentPsychologist() {
        when(psychologistProvider.getPsychologistAccount()).thenReturn(psychologist);
    }

    @Nested
    class GetOwnedResearch {

        /**
         * Happy path: репозиторий нашёл research по связке
         * «id research + id текущего психолога» — она возвращается как есть.
         * Проверяем не только возврат, но и то, что сервис передал в
         * репозиторий ровно пару (researchId, idPsychologistAccount):
         * подмена id психолога здесь означала бы доступ к чужим данным.
         */
        @Test
        void returnsResearch_whenOwnedByCurrentUser() {
            stubCurrentPsychologist();
            var researchId = research.getIDResearch();
            when(researchRepository.findByIdAndPsychologistAccountIdPsychologistAccount(
                    researchId, psychologist.getIdPsychologistAccount()))
                    .thenReturn(Optional.of(research));

            var result = service.getOwnedResearch(researchId);

            assertThat(result).isSameAs(research);
        }

        /**
         * Research не найден: либо его нет, либо он принадлежит другому
         * психологу — сервис отвечает одним и тем же отказом.
         * Так «не существует» и «не ваше» неотличимы для клиента
         * (защита от перечисления чужих id).
         */
        @Test
        void researchNotFound_throwsResearchNotFoundException() {
            stubCurrentPsychologist();
            var researchId = UUID.randomUUID();
            when(researchRepository.findByIdAndPsychologistAccountIdPsychologistAccount(
                    researchId, psychologist.getIdPsychologistAccount()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOwnedResearch(researchId))
                    .isInstanceOf(ResearchNotFoundException.class)
                    .hasMessage("Исследование не найдено");
        }


        /**
         * Репозиторий вообще не должен вызываться, если не удалось
         * определить текущего психолога: запрос отклоняется до обращения к БД.
         */
        @Test
        void noCurrentPsychologist_propagatesException_withoutRepositoryCall() {
            when(psychologistProvider.getPsychologistAccount())
                    .thenThrow(new UnauthorizedException("Пользователь не авторизован"));

            assertThatThrownBy(() -> service.getOwnedResearch(UUID.randomUUID()))
                    .isInstanceOf(UnauthorizedException.class);

            verifyNoInteractions(researchRepository);
        }
    }

    @Nested
    class GetOwnedSurvey {

        /**
         * Happy path: survey найден по связке «id survey + id психолога,
         * владеющего родительским research». Возврат — как есть, isSameAs.
         */
        @Test
        void returnsSurvey_whenOwnedByCurrentUser() {
            stubCurrentPsychologist();
            var survey = new Survey(UUID.randomUUID(), "Опрос", "Описание", "Правила");
            var idSurvey = survey.getIdSurvey();
            when(surveyRepository.findByIdAndResearchPsychologistAccountIdPsychologistAccount(
                    idSurvey, psychologist.getIdPsychologistAccount()))
                    .thenReturn(Optional.of(survey));

            var result = service.getOwnedSurvey(idSurvey);

            assertThat(result).isSameAs(survey);
        }

        /**
         * Survey не найден — тот же единый отказ, что и для research:
         * клиент не может отличить «нет такого survey» от «не ваш survey».
         */
        @Test
        void surveyNotFound_throwsResearchNotFoundException() {
            stubCurrentPsychologist();
            var idSurvey = UUID.randomUUID();
            when(surveyRepository.findByIdAndResearchPsychologistAccountIdPsychologistAccount(
                    idSurvey, psychologist.getIdPsychologistAccount()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOwnedSurvey(idSurvey))
                    .isInstanceOf(ResearchNotFoundException.class)
                    .hasMessage("Опрос не найден");
        }

    }
}
