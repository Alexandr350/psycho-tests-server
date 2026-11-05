package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultOneCharacteristicRequestDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.*;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.service.CPSKResultService;
import com.psychotests.psycho_tests_server.service.ResearchAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты сервиса {@link CPSKResultService}.
 * <p>
 * Репозитории замоканы через Mockito — тестируется только логика сборки результата,
 * без обращения к БД. ВАЖНО: мок не исполняет default-методы интерфейса,
 * поэтому стабятся только трёхаргументные формы с idRun = null («все волны»).
 * <p>
 * Что проверяется:
 * <ul>
 *   <li>подсчёт количества респондентов по секторам для характеристики и для item;</li>
 *   <li>расчёт процентов с догоняющим (сумма всегда равна 100);</li>
 *   <li>группировка объектов (items, itemsSub, characteristics, ассоциации) по секторам
 *       со схлопыванием дубликатов и подсчётом попаданий каждого объекта в сектор;</li>
 *   <li>переключение веток: пришёл idCharacteristic — content собирается из items,
 *       пришёл idItem — из characteristics; неиспользуемая ветка репозиториев не дёргается;</li>
 *   <li>режим «весь опрос»: оба id = null — счётчик по всем объектам, оба типа контента;</li>
 *   <li>крайний случай: опрос без ответов.</li>
 * </ul>
 * <p>
 * По бизнес-правилу респондент ставит объект в сектор не более одного раза,
 * поэтому несколько попаданий объекта в сектор дают только разные респонденты.
 * <p>
 * Все сценарии по умолчанию используют один сектор ({@link #SECTOR}), кроме тестов,
 * где секторов два-три — чтобы проверить раскладку объектов и проценты.
 */
@ExtendWith(MockitoExtension.class)
class CPSKResultServiceTest {

    @Mock
    private AnswersCharacteristicsRepository characteristicsRepository;
    @Mock
    private AnswersItemsRepository itemsRepository;
    @Mock
    private AnswersItemsSubRepository itemsSubRepository;
    @Mock
    private AnswersAssociationsRepository associationsRepository;
    @Mock
    private ResearchAccessService researchAccessService;

    private CPSKResultService service;

    private static final UUID ID_SURVEY = UUID.randomUUID();
    private static final UUID ID_CHARACTERISTIC = UUID.randomUUID();
    private static final UUID ID_ITEM = UUID.randomUUID();

    // Сценарий по умолчанию: все ответы респондентов — в один сектор
    private static final int SECTOR = 1;
    private static final String SUB_URL = "http://sub1";
    private static final String CHAR_TEXT = "мягкий";

    /**
     * Заглушка проекции счётчика: имитирует строку группировки из репозитория.
     * Реализует SectorCountProjection, поэтому сервис работает с ней как с настоящей.
     */
    private record TestSectorCount(int numberSector, long count) implements SectorCountProjection {
        @Override
        public int getNumberSector() {
            return numberSector;
        }

        @Override
        public long getCount() {
            return count;
        }
    }

    private static Characteristics characteristic(UUID id) {
        Characteristics c = mock(Characteristics.class);
        when(c.getIdCharacteristic()).thenReturn(id);
        when(c.getTextCharacteristic()).thenReturn(CHAR_TEXT);
        return c;
    }

    private static Items item(UUID id, String url) {
        Items i = mock(Items.class);
        when(i.getIdItem()).thenReturn(id);
        when(i.getUrlItem()).thenReturn(url);
        return i;
    }

    private static ItemsSub itemsSub(UUID id) {
        ItemsSub s = mock(ItemsSub.class);
        when(s.getIdItemSub()).thenReturn(id);
        when(s.getUrlItemSub()).thenReturn(SUB_URL);
        return s;
    }

    private static AnswersCharacteristics charAnswer(Characteristics c) {
        AnswersCharacteristics a = mock(AnswersCharacteristics.class);
        when(a.getCharacteristic()).thenReturn(c);
        when(a.getNumberSector()).thenReturn(SECTOR);
        return a;
    }

    private static AnswersItems itemAnswer(Items i, int sector) {
        AnswersItems a = mock(AnswersItems.class);
        when(a.getItem()).thenReturn(i);
        when(a.getNumberSector()).thenReturn(sector);
        return a;
    }

    private static AnswersItemsSubs subAnswer(ItemsSub s) {
        AnswersItemsSubs a = mock(AnswersItemsSubs.class);
        when(a.getItemsSub()).thenReturn(s);
        when(a.getNumberSector()).thenReturn(SECTOR);
        return a;
    }

    private static AnswersAssociations assocAnswer(String text) {
        AnswersAssociations a = mock(AnswersAssociations.class);
        when(a.getAnswer()).thenReturn(text);
        when(a.getNumberSector()).thenReturn(SECTOR);
        return a;
    }

    @BeforeEach
    void setUp() {
        service = new CPSKResultService(
                characteristicsRepository, itemsRepository,
                itemsSubRepository, associationsRepository,
                researchAccessService);
        // Проверка владения вызывается в начале getResult, её результат не используется —
        // стаб не нужен: мок вернёт null, сервис этого не заметит.
    }

    // ---------- Ветка характеристики ----------

    /**
     * Ветка характеристики: подсчёт по секторам корректен, проценты считаются
     * от общего числа респондентов (7 из 10 — 70%, 3 из 10 — 30%).
     * Ответ всегда содержит все 8 секторов: заполненные — со значениями,
     * пустые — с нулями; листы sectors идут строго 1..8.
     */
    @Test
    void byCharacteristic_countsAndContentAreBuilt() {
        // стабим ТРЁХаргументную форму: мок не делегирует default-методы
        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 7L), new TestSectorCount(2, 3L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        assertThat(result.idCharacteristic()).isEqualTo(ID_CHARACTERISTIC);
        assertThat(result.idItem()).isNull();

        assertThat(result.counts()).hasSize(8);
        SectorCountDto sector1 = result.counts().getFirst();
        assertThat(sector1.numberSector()).isEqualTo(1);
        assertThat(sector1.count()).isEqualTo(7);
        assertThat(sector1.percent()).isEqualTo(70);
        SectorCountDto sector2 = result.counts().get(1);
        assertThat(sector2.count()).isEqualTo(3);
        assertThat(sector2.percent()).isEqualTo(30);
        // пустые секторы — нулями
        assertThat(result.counts().get(2))
                .satisfies(c -> {
                    assertThat(c.numberSector()).isEqualTo(3);
                    assertThat(c.count()).isZero();
                    assertThat(c.percent()).isZero();
                });

        assertThat(result.sectors())
                .extracting(SectorContentDto::numberSector)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    /**
     * Догоняющий процент: при трёх равных секторах базовое округление даёт 33 + 33 + 33 = 99.
     * Проверяем, что недостающий процент добавляется самому крупному сектору
     * и сумма процентов становится ровно 100.
     */
    @Test
    void byCharacteristic_dogonyayushiyFixesPercentSum() {
        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 1L), new TestSectorCount(2, 1L), new TestSectorCount(3, 1L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        int sum = result.counts().stream().mapToInt(SectorCountDto::percent).sum();
        assertThat(sum).isEqualTo(100);
        assertThat(result.counts())
                .extracting(SectorCountDto::percent)
                .filteredOn(p -> p > 0)
                .containsExactlyInAnyOrder(34, 33, 33);
    }

    /**
     * Группировка объектов по секторам в ветке характеристики:
     * один и тот же item, выбранный двумя респондентами в один сектор, схлопывается
     * в одну запись с полем hits = 2 (количество попаданий);
     * объект из другого сектора попадает в свой лист, а не в чужой.
     */
    @Test
    void byCharacteristic_objectsAreGroupedBySectorWithHits() {
        Items item1 = item(UUID.randomUUID(), "http://img1");
        Items item2 = item(UUID.randomUUID(), "http://img2");

        // item1 попал в сектор 1 дважды (два респондента), item2 — один раз в сектор 2.
        // Список ответов собираем ДО when: Mockito не позволяет создавать моки,
        // пока предыдущий стаббинг не завершён
        List<AnswersItems> itemAnswers = List.of(
                itemAnswer(item1, 1),
                itemAnswer(item1, 1),
                itemAnswer(item2, 2));

        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 2L), new TestSectorCount(2, 1L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null))
                .thenReturn(itemAnswers);
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        // листы всегда идут строго 1..8 (контракт зафиксирован тестом
        // byCharacteristic_countsAndContentAreBuilt), поэтому индекс 1 = сектор 2
        SectorContentDto sector1 = result.sectors().getFirst();
        assertThat(sector1.items())
                .hasSize(1) // дубликат схлопнулся
                .first()
                .satisfies(h -> {
                    assertThat(h.id()).isEqualTo(item1.getIdItem());
                    assertThat(h.url()).isEqualTo("http://img1");
                    assertThat(h.hits()).isEqualTo(2); // два попадания
                });

        SectorContentDto sector2 = result.sectors().get(1);
        assertThat(sector2.items())
                .hasSize(1)
                .first()
                .satisfies(h -> assertThat(h.hits()).isEqualTo(1));
    }

    // ---------- Ветка item ----------

    /**
     * Ветка item: подсчёт по секторам считается для item,
     * а объекты по секторам собираются из характеристик (зеркальная ветка).
     * Проверяем также ленивость: findAllItemsBySurvey (двухаргументная форма,
     * которую зовёт сервис) в этой ветке не вызывается вовсе.
     */
    @Test
    void byItem_characteristicsAreFetchedInsteadOfItems() {
        Characteristics c1 = characteristic(UUID.randomUUID());

        List<AnswersCharacteristics> charAnswers = List.of(charAnswer(c1));

        when(itemsRepository.countSectorsByItem(ID_ITEM, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 5L)));
        when(characteristicsRepository.findAllCharacteristicsBySurvey(ID_SURVEY, null))
                .thenReturn(charAnswers);
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, null, ID_ITEM));

        assertThat(result.idItem()).isEqualTo(ID_ITEM);
        assertThat(result.counts()).hasSize(8);

        SectorContentDto sector1 = result.sectors().getFirst();
        assertThat(sector1.items()).isEmpty(); // item-объекты в ветке item не собираются
        assertThat(sector1.characteristics())
                .hasSize(1)
                .first()
                .satisfies(h -> {
                    assertThat(h.id()).isEqualTo(c1.getIdCharacteristic());
                    assertThat(h.text()).isEqualTo(CHAR_TEXT);
                    assertThat(h.hits()).isEqualTo(1);
                });

        // основную выборку item-ответов в ветке item не дёргаем;
        // any(), any() — потому что сервис зовёт ДВУХаргументную форму,
        // а матчер одного any() на неё не попадает
        verify(itemsRepository, never()).findAllItemsBySurvey(any(), any());
    }

    /**
     * Ассоциации: одинаковые тексты в одном секторе схлопываются в одну запись,
     * поле hits показывает, сколько респондентов написали этот текст;
     * разные тексты остаются отдельными записями.
     */
    @Test
    void associationsGroupedBySectorAndText() {
        List<AnswersAssociations> assocAnswers = List.of(
                assocAnswer("уютный"),
                assocAnswer("уютный"),
                assocAnswer("тёплый"));

        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(SECTOR, 3L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null))
                .thenReturn(assocAnswers);

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        assertThat(result.sectors().getFirst().associations())
                .hasSize(2)
                .contains(
                        new AssociationHitDto("уютный", 2),
                        new AssociationHitDto("тёплый", 1));
    }

    /**
     * Суб-объекты (itemsSub) тянутся в обеих ветках, а не только в одной;
     * в ответе у них присутствует содержание (urlItemSub).
     */
    @Test
    void subsAreFetchedInBothBranchesWithUrl() {
        ItemsSub sub = itemsSub(UUID.randomUUID());

        List<AnswersItemsSubs> subAnswers = List.of(subAnswer(sub));

        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(SECTOR, 1L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null))
                .thenReturn(subAnswers);
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        assertThat(result.sectors().getFirst().itemsSub())
                .hasSize(1)
                .first()
                .satisfies(h -> {
                    assertThat(h.id()).isEqualTo(sub.getIdItemSub());
                    assertThat(h.url()).isEqualTo(SUB_URL);
                });
    }

    // ---------- Режим «весь опрос» ----------

    /**
     * Режим «весь опрос» (после добавления волн оба id = null — легитимный запрос,
     * а не ошибка): счётчик по секторам собирается слиянием попаданий характеристик
     * и изображений; контент берётся из обоих типов. Проверяем также проценты:
     * 4 из 10 — 40%, 6 из 10 — 60%.
     */
    @Test
    void noIdsPassed_runsInSurveyMode() {
        when(characteristicsRepository.countSectorHitsBySurvey(ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 4L)));
        when(itemsRepository.countSectorHitsBySurvey(ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(2, 6L)));
        when(characteristicsRepository.findAllCharacteristicsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, null, null));

        assertThat(result.idCharacteristic()).isNull();
        assertThat(result.idItem()).isNull();

        assertThat(result.counts()).hasSize(8);
        assertThat(result.counts().get(0).count()).isEqualTo(4);
        assertThat(result.counts().get(0).percent()).isEqualTo(40);
        assertThat(result.counts().get(1).count()).isEqualTo(6);
        assertThat(result.counts().get(1).percent()).isEqualTo(60);
    }

    // ---------- Крайние случаи ----------

    /**
     * Пустой опрос (нет ни одного ответа): counts содержит все 8 секторов с нулями,
     * сумма процентов — 0 (догоняющий не накидывает фальшивые проценты пустому опросу),
     * все 8 листов sectors приходят пустыми.
     */
    @Test
    void emptySurvey_returnsEightZeroSectors() {
        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of());
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        assertThat(result.counts()).hasSize(8);
        assertThat(result.counts())
                .allSatisfy(c -> {
                    assertThat(c.count()).isZero();
                    assertThat(c.percent()).isZero();
                });

        assertThat(result.sectors()).hasSize(8);
        assertThat(result.sectors())
                .allSatisfy(s -> {
                    assertThat(s.characteristics()).isEmpty();
                    assertThat(s.items()).isEmpty();
                    assertThat(s.itemsSub()).isEmpty();
                    assertThat(s.associations()).isEmpty();
                });
    }

    /**
     * Ответы есть не в части секторов: заполненные секторы возвращаются со значениями,
     * пропущенные — нулями; в сумме проценты дают 100.
     * Проценты считаются относительно суммы по заполненным секторам.
     */
    @Test
    void missingSectorsAreZeroFilled() {
        // ответы только в секторах 1, 2, 5; всего 10 размещений
        when(characteristicsRepository.countSectorsByCharacteristic(ID_CHARACTERISTIC, ID_SURVEY, null))
                .thenReturn(List.of(new TestSectorCount(1, 5L), new TestSectorCount(2, 3L), new TestSectorCount(5, 2L)));
        when(itemsRepository.findAllItemsBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(itemsSubRepository.findAllItemsSubBySurvey(ID_SURVEY, null)).thenReturn(List.of());
        when(associationsRepository.findAllAssociationsBySurvey(ID_SURVEY, null)).thenReturn(List.of());

        ResultOneCharacteristicResponseDto result = service.getResult(
                new ResultOneCharacteristicRequestDto(ID_SURVEY, ID_CHARACTERISTIC, null));

        assertThat(result.counts()).hasSize(8);
        Map<Integer, Integer> percents = result.counts().stream()
                .collect(Collectors.toMap(SectorCountDto::numberSector, SectorCountDto::percent));
        assertThat(percents)
                .containsEntry(1, 50)
                .containsEntry(2, 30)
                .containsEntry(5, 20)
                .containsEntry(3, 0)
                .containsEntry(4, 0)
                .containsEntry(6, 0)
                .containsEntry(7, 0)
                .containsEntry(8, 0);

        int sum = percents.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(sum).isEqualTo(100);
    }
}
