package com.psychotests.psycho_tests_server.unit.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.psychotests.psycho_tests_server.controller.ResearchController;
import com.psychotests.psycho_tests_server.dto.request.change_research_dto.ChangeStatusRequestDto;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestCharacteristicsDtoPart3;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestSurveyDtoPart2;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchCreatedResponseDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.SurveyCreatedResponseDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import com.psychotests.psycho_tests_server.service.FileStorageService;
import com.psychotests.psycho_tests_server.service.ResearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты контроллера исследований (ResearchController).
 *
 * Что проверяем: контроллер — «тонкий» слой между HTTP и сервисами.
 * Его зона ответственности — маппинг путей и параметров, HTTP-статусы,
 * форма ответа (тело или его отсутствие) и порядок вызовов сервисов
 * (например: файлы на диск раньше метаданных в базу). Бизнес-логику —
 * статусную модель, права доступа, хранение файлов — здесь НЕ проверяем:
 * для неё есть свои тесты сервисов и интеграционные тесты.
 *
 * Как устроено: чистый Mockito без Spring-контекста — три зависимости
 * замоканы, контроллер создан через @InjectMocks, вызовы идут напрямую
 * в методы контроллера (без MockMvc, валидации и сериализации JSON).
 * Валидация @Valid и конвертация тела в DTO — зона Spring MVC, она
 * покрыта интеграционными тестами.
 *
 * Организация: тесты сгруппированы в @Nested-классы по группам
 * эндпойнтов — чтение, мутации, мастер создания (Part1–Part5).
 */
@ExtendWith(MockitoExtension.class)
class ResearchControllerTest {

    private static final UUID ID_RESEARCH = UUID.randomUUID();
    private static final UUID ID_SURVEY = UUID.randomUUID();

    @Mock
    private CreateResearchService createResearchService;
    @Mock
    private ResearchService researchService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ResearchController controller;

    private final ResearchRequestInfoDtoPart1 part1 = new ResearchRequestInfoDtoPart1(
            "Название", "Описание", "кафедра", TypeResearch.CPSK);
    private final ResearchRequestSurveyDtoPart2 part2 = new ResearchRequestSurveyDtoPart2(
            ID_RESEARCH, "Опрос", "desc", "rules");
    private final ResearchRequestCharacteristicsDtoPart3 part3 = new ResearchRequestCharacteristicsDtoPart3(
            ID_SURVEY, "тревога, внимание");

    /** Фабрика multipart-файла: имя поля «files» совпадает с @RequestParam контроллера. */
    private MockMultipartFile jpg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    /** Список мок-файлов под тип List<MultipartFile>, ожидаемый контроллером. */
    private List<MultipartFile> toMultipart(List<MockMultipartFile> files) {
        return new ArrayList<>(files);
    }


    /**
     * Собирает DTO исследования через фабрику сервиса, а не конструктором:
     * не зависим от набора полей record, а changeStatus гарантирует
     * легальный переход в нужный статус. Переходы прокладываются по всей
     * цепочке статусной машины (DRAFT → NEW → ...), «перепрыгивание»
     * запрещено и честно бросает IllegalStateException — фабрика обязана
     * повторять пользовательский путь.
     */
    private ResearchResponseInfoDto infoDto(ResearchStatus status) {
        Research research = new Research(ID_RESEARCH, "Название", "Описание",
                "кафедра", TypeResearch.CPSK);
        if (status != ResearchStatus.DRAFT) {
            research.changeStatus(ResearchStatus.NEW);   // первый легальный шаг всегда
        }
        if (status != ResearchStatus.DRAFT && status != ResearchStatus.NEW) {
            research.changeStatus(status);
        }
        return ResearchResponseInfoDto.from(research);
    }



    // ---------- Чтение ----------

    @Nested
    @DisplayName("GET /all, GET /research/{idResearch}")
    class ReadTests {

        /**
         * Делает: вызывает getAllResearches() с замоканным списком из сервиса.
         * Проверяет: контроллер возвращает ровно тот список, что отдал сервис,
         * без фильтрации, сортировки и оборачивания — «прозрачная труба».
         */
        @Test
        void getAllResearches_returnsServiceList() {
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.DRAFT);
            when(researchService.findAllResearches()).thenReturn(List.of(dto));

            List<ResearchResponseInfoDto> result = controller.getAllResearches();

            assertThat(result).containsExactly(dto);
        }

        /**
         * Делает: вызывает getResearch(id) и сверяет переданный UUID.
         * Проверяет: делегирование без преобразований — в сервис уходит
         * тот же идентификатор, что пришёл из пути, и возвращается его DTO.
         */
        @Test
        void getResearch_delegatesId() {
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.NEW);
            when(researchService.getResearch(ID_RESEARCH)).thenReturn(dto);

            ResearchResponseInfoDto result = controller.getResearch(ID_RESEARCH);

            assertThat(result).isEqualTo(dto);
            verify(researchService).getResearch(ID_RESEARCH);
        }
    }

    // ---------- Смена статуса, удаление, PATCH ----------

    @Nested
    @DisplayName("PATCH /status, DELETE, PATCH /research/{idResearch}")
    class MutationTests {

        /**
         * Делает: вызывает changeStatus(id, dto) с запросом RUNNING.
         * Проверяет: сервис получает статус из тела запроса (распаковка DTO),
         * ответ — 200 OK с телом: полный ResearchResponseInfoDto, включая
         * publicToken — фронт по нему показывает публичную ссылку на карточке.
         * Раньше эндпойнт отвечал 204 без тела; контракт изменён вместе
         * с токен-фичей, интеграционные тесты проверяют JSON-сериализацию.
         */
        @Test
        void changeStatus_delegatesAndReturnsDto() {
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.RUNNING);
            when(researchService.changeStatus(ID_RESEARCH, ResearchStatus.RUNNING))
                    .thenReturn(dto);

            ResponseEntity<ResearchResponseInfoDto> response = controller.changeStatus(
                    ID_RESEARCH, new ChangeStatusRequestDto(ID_RESEARCH, ResearchStatus.RUNNING));

            ResearchResponseInfoDto body = response.getBody();
            assertThat(body).isSameAs(dto);
            assertThat(body).isNotNull();
            // infoDto прошёл через RUNNING: токен сгенерирован доменным changeStatus
            assertThat(body.publicToken()).isNotNull();
            verify(researchService).changeStatus(ID_RESEARCH, ResearchStatus.RUNNING);
        }



        /**
         * Делает: вызывает deleteResearch(id).
         * Проверяет: сервису уходит ровно id из пути, ответ — 204 без тела.
         */
        @Test
        void deleteResearch_delegatesAndReturns204() {
            ResponseEntity<Void> response = controller.deleteResearch(ID_RESEARCH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(researchService).deleteResearch(ID_RESEARCH);
        }

        /**
         * Делает: вызывает updateInfo(id, dto) — частичное редактирование полей.
         * Проверяет: ответ 204 и передача DTO в сервис без изменений.
         * Проверка статуса (DRAFT/NEW — иначе 409) живёт в сервисе.
         */
        @Test
        void updateInfo_delegatesAndReturns204() {
            ResponseEntity<Void> response = controller.updateInfo(ID_RESEARCH, part1);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(researchService).updateResearchInfo(ID_RESEARCH, part1);
        }

        /**
         * Делает: вызывает updateSurvey(idSurvey, dto) — PATCH-обновление
         * опроса при резюме черновика из мастера.
         * Проверяет: ответ 200 с телом (ResearchResponseInfoDto, возвращённый
         * сервисом) и передача DTO в сервис без изменений. Проверка статуса
         * (только DRAFT) и владения опросом живёт в сервисе и
         * ResearchAccessService соответственно.
         */
        @Test
        void updateSurvey_delegatesAndReturnsDto() {
            Research research = new Research(ID_RESEARCH, "Название", "Описание",
                    "кафедра", TypeResearch.CPSK);
            Survey survey = new Survey(ID_SURVEY, "Опрос", "desc", "rules");

            ResearchResponseInfoDto updated = ResearchResponseInfoDto.from(research, survey);
            when(createResearchService.updateSurvey(ID_SURVEY, part2)).thenReturn(updated);

            ResponseEntity<ResearchResponseInfoDto> response =
                    controller.updateSurvey(ID_SURVEY, part2);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(updated);
            verify(createResearchService).updateSurvey(ID_SURVEY, part2);
        }

        /**
         * Контракт идентификатора: путь — авторитетный источник. Поле
         * idResearch в теле контроллер игнорирует. Тест фиксирует это
         * сознательным решением: подсовываем в тело чужой id и проверяем,
         * что сервис получил id из пути. Если решите вместо этого
         * валидировать совпадение (400) или убрать поле из dto — тест
         * заставит перенести решение туда, где оно принято.
         */
        @Test
        void changeStatus_pathIdWinsOverDtoId() {
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.RUNNING);
            when(researchService.changeStatus(ID_RESEARCH, ResearchStatus.RUNNING))
                    .thenReturn(dto);

            controller.changeStatus(ID_RESEARCH,
                    new ChangeStatusRequestDto(UUID.randomUUID(), ResearchStatus.RUNNING));

            verify(researchService).changeStatus(ID_RESEARCH, ResearchStatus.RUNNING);
        }




    }

    // ---------- Мастер создания (Part1–Part5) ----------

    @Nested
    @DisplayName("POST: research, survey, characteristics, items, items-sub")
    class CreateFlowTests {

        /**
         * Делает: мокает addInfoResearch() → UUID и вызывает addInfo().
         * Проверяет: статус 201 Created и тело с idResearch от сервиса —
         * клиент получает идентификатор для следующего шага мастера.
         */
        @Test
        void addInfo_returns201WithId() {
            when(createResearchService.addInfoResearch(part1)).thenReturn(ID_RESEARCH);

            ResponseEntity<ResearchCreatedResponseDto> response = controller.addInfo(part1);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().idResearch()).isEqualTo(ID_RESEARCH);
        }

        /**
         * Делает: мокает addInfoSurvey() → UUID и вызывает addSurvey().
         * Проверяет: статус 201 и тело с idSurvey — аналогично шагу 1,
         * но для сущности опроса.
         */
        @Test
        void addSurvey_returns201WithId() {
            when(createResearchService.addInfoSurvey(part2)).thenReturn(ID_SURVEY);

            ResponseEntity<SurveyCreatedResponseDto> response = controller.addSurvey(part2);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().idSurvey()).isEqualTo(ID_SURVEY);
        }

        /**
         * Делает: вызывает addCharacteristics(part3).
         * Проверяет: ответ 204 без тела — характеристики создаются «внутри»
         * опроса, отдельный ресурс не порождается.
         */
        @Test
        void addCharacteristics_returns204() {
            ResponseEntity<Void> response = controller.addCharacteristics(part3);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(createResearchService).addCharacteristics(part3);
        }

        /**
         * Делает: мокает сохранение файлов на диск и вызывает addItems().
         * Проверяет порядок и связность через InOrder: сначала
         * fileStorageService.saveResearchImages, затем createResearchService.addItems
         * с теми путями, что вернул сторедж. Список файлов создаётся один раз
         * и стабится по same(): MockMultipartFile не переопределяет equals,
         * поэлементное сравнение двух копий списка не совпадает — стаб по
         * ссылке исключает эту ловушку.
         */
        @Test
        void addItems_savesFilesThenDelegatesPaths() {
            List<MultipartFile> files = toMultipart(List.of(jpg("img1.jpg"), jpg("img2.jpg")));
            List<String> paths = List.of("p/img1.jpg", "p/img2.jpg");
            when(fileStorageService.saveResearchImages(eq(ID_SURVEY), same(files)))
                    .thenReturn(paths);

            ResponseEntity<Void> response = controller.addItems(ID_SURVEY, files);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            InOrder order = inOrder(fileStorageService, createResearchService);
            order.verify(fileStorageService).saveResearchImages(eq(ID_SURVEY), same(files));
            order.verify(createResearchService).addItems(ID_SURVEY, paths);
        }


        /**
         * Делает: мокает сторедж и addItemSub() → DTO исследования.
         * Проверяет: статус 200 OK (не 201 — шаг 5 не создаёт ресурс, а завершает
         * мастер) и что клиенту возвращается полный DTO исследования, включая
         * researchStatus — фронт по нему обновляет UI.
         */
        @Test
        void addItemSub_returns200WithResearchDto() {
            List<MultipartFile> files = toMultipart(List.of(jpg("sub1.jpg")));
            List<String> paths = List.of("p/sub1.jpg");
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.NEW);
            when(fileStorageService.saveResearchImages(eq(ID_SURVEY), same(files)))
                    .thenReturn(paths);
            when(createResearchService.addItemSub(ID_SURVEY, paths)).thenReturn(dto);

            ResponseEntity<ResearchResponseInfoDto> response =
                    controller.addItemSub(ID_SURVEY, files);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(dto);
        }


        /**
         * Делает: вызывает addItemSub() с пустым списком файлов.
         * Проверяет: пустая загрузка — легальный сценарий («шаг завершён без файлов»):
         * файлы на диск не сохраняются, сервису передаётся пустой список путей,
         * в ответе — 200 и DTO исследования в статусе NEW.
         */
        @Test
        void addItemSub_withEmptyFiles_delegatesEmptyPathsAndReturns200() {
            ResearchResponseInfoDto dto = infoDto(ResearchStatus.NEW);
            when(createResearchService.addItemSub(ID_SURVEY, List.of())).thenReturn(dto);

            ResponseEntity<ResearchResponseInfoDto> response = controller.addItemSub(ID_SURVEY, List.of());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(dto);
            verify(createResearchService).addItemSub(ID_SURVEY, List.of());
            verifyNoInteractions(fileStorageService);
        }

    }
}
