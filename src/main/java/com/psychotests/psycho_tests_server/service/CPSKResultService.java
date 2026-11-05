package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultOneCharacteristicRequestDto;
import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultSectorRequestDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.*;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import com.psychotests.psycho_tests_server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CPSKResultService {

    private static final int TOTAL_SECTORS = 8;

    /** Цвета секторов колеса: сектор -> HEX. Синхронизировано с фронтом
     *  (PsychologistRoom/js/reportsPage/reportsResearchMake.js, SECTOR_COLORS). */
    private static final Map<Integer, String> SECTOR_COLORS = Map.of(
            1, "#e41e26", 2, "#fbee23", 3, "#009245", 4, "#92278f",
            5, "#2e64b0", 6, "#753b22", 7, "#939598", 8, "#000000");


    private final AnswersCharacteristicsRepository characteristicsRepository;
    private final AnswersItemsRepository itemsRepository;
    private final AnswersItemsSubRepository itemsSubRepository;
    private final AnswersAssociationsRepository associationsRepository;
    private final ResearchAccessService researchAccessService;

    /**
     * Отчёт по сектору (цвету): все объекты, попавшие в сектор, по всем
     * (или выбранной) волнам. В отличие от getResult объект не запрашивается —
     * скрытия типа «запрошенный объект» здесь нет.
     * totalRespondents — уникальные сессии, положившие в сектор хотя бы один объект.
     */
    public ResultSectorResponseDto getSectorResult(ResultSectorRequestDto request) {
        int sector = request.sector();
        if (sector < 1 || sector > TOTAL_SECTORS) {
            throw new IllegalArgumentException(
                    "Сектор должен быть в диапазоне 1..8, получено: " + sector);
        }
        researchAccessService.getOwnedSurvey(request.idSurvey());

        UUID idSurvey = request.idSurvey();
        UUID idRun = request.idRun();

        // Отчёту по сектору нужны все четыре типа ответов — фильтруем по сектору после выборки.
        List<AnswersCharacteristics> characteristicAnswers = inSector(
                characteristicsRepository.findAllCharacteristicsBySurvey(idSurvey, idRun),
                sector,
                AnswersCharacteristics::getNumberSector);
        List<AnswersItems> itemAnswers = inSector(
                itemsRepository.findAllItemsBySurvey(idSurvey, idRun),
                sector,
                AnswersItems::getNumberSector);
        List<AnswersItemsSubs> subAnswers = inSector(
                itemsSubRepository.findAllItemsSubBySurvey(idSurvey, idRun),
                sector,
                AnswersItemsSubs::getNumberSector);
        List<AnswersAssociations> associationAnswers = inSector(
                associationsRepository.findAllAssociationsBySurvey(idSurvey, idRun),
                sector,
                AnswersAssociations::getNumberSector);

        long totalRespondents = countRespondents(
                characteristicAnswers, itemAnswers, subAnswers, associationAnswers);

        return new ResultSectorResponseDto(
                sector,
                SECTOR_COLORS.get(sector),
                totalRespondents,
                buildSectorContent(sector, characteristicAnswers, itemAnswers, subAnswers, associationAnswers));
    }

    /**
     * Фильтр ответов по сектору. Функция-извлекатель сектора передаётся
     * типом ответа, что снимает дублирование обёрток; null в numberSector
     * безопасно отфильтровывается.
     */
    private static <T> List<T> inSector(List<T> answers, int sector, Function<T, Integer> sectorOf) {
        return answers.stream()
                .filter(a -> {
                    Integer s = sectorOf.apply(a);
                    return s != null && s == sector;
                })
                .toList();
    }

    /**
     * Уникальные респонденты сектора: сессии, положившие в сектор
     * хотя бы один объект любого из четырёх типов. Сессия, попавшая
     * в сектор пятью объектами, считается один раз.
     */
    private static long countRespondents(
            List<AnswersCharacteristics> characteristicAnswers,
            List<AnswersItems> itemAnswers,
            List<AnswersItemsSubs> subAnswers,
            List<AnswersAssociations> associationAnswers) {

        Set<UUID> sessionIds = new HashSet<>();
        for (AnswersCharacteristics a : characteristicAnswers) {
            sessionIds.add(a.getSurveySession().getIdSurveySession());
        }
        for (AnswersItems a : itemAnswers) {
            sessionIds.add(a.getSurveySession().getIdSurveySession());
        }
        for (AnswersItemsSubs a : subAnswers) {
            sessionIds.add(a.getSurveySession().getIdSurveySession());
        }
        for (AnswersAssociations a : associationAnswers) {
            sessionIds.add(a.getSurveySession().getIdSurveySession());
        }
        return sessionIds.size();
    }

    /**
     * Содержание сектора: те же листы, что в getResult, но только для одного сектора.
     * Справочники собираются по отфильтрованным ответам (объект попадает в сектор
     * только с ответами о себе), попадания считаются теми же хелперами, что в getResult.
     */
    private static SectorContentDto buildSectorContent(
            int sector,
            List<AnswersCharacteristics> characteristicAnswers,
            List<AnswersItems> itemAnswers,
            List<AnswersItemsSubs> subAnswers,
            List<AnswersAssociations> associationAnswers) {

        Map<UUID, Characteristics> characteristicsById = toDictionary(
                characteristicAnswers,
                a -> a.getCharacteristic().getIdCharacteristic(),
                AnswersCharacteristics::getCharacteristic);
        Map<UUID, Items> itemsById = toDictionary(
                itemAnswers,
                a -> a.getItem().getIdItem(),
                AnswersItems::getItem);
        Map<UUID, ItemsSub> subsById = toDictionary(
                subAnswers,
                a -> a.getItemsSub().getIdItemSub(),
                AnswersItemsSubs::getItemsSub);

        // Сами ответы уже отфильтрованы по сектору, поэтому группировка
        // по numberSector даст одну группу — но переиспользуем общий хелпер для единообразия.
        Map<Integer, Map<UUID, Long>> characteristicsHits = buildHits(
                characteristicAnswers,
                AnswersCharacteristics::getNumberSector,
                a -> a.getCharacteristic().getIdCharacteristic());
        Map<Integer, Map<UUID, Long>> itemsHits = buildHits(
                itemAnswers,
                AnswersItems::getNumberSector,
                a -> a.getItem().getIdItem());
        Map<Integer, Map<UUID, Long>> subsHits = buildHits(
                subAnswers,
                AnswersItemsSubs::getNumberSector,
                a -> a.getItemsSub().getIdItemSub());
        Map<Integer, Map<String, Long>> associationsHits = associationAnswers.stream()
                .collect(Collectors.groupingBy(
                        AnswersAssociations::getNumberSector,
                        Collectors.groupingBy(
                                AnswersAssociations::getAnswer,
                                Collectors.counting())));

        return new SectorContentDto(
                sector,
                orIdMap(characteristicsHits.get(sector)).entrySet().stream()
                        .map(e -> {
                            Characteristics c = characteristicsById.get(e.getKey());
                            return new CharacteristicHitDto(
                                    c.getIdCharacteristic(), c.getTextCharacteristic(), e.getValue());
                        })
                        .toList(),
                orIdMap(itemsHits.get(sector)).entrySet().stream()
                        .map(e -> {
                            Items i = itemsById.get(e.getKey());
                            return new ItemHitDto(i.getIdItem(), i.getUrlItem(), e.getValue());
                        })
                        .toList(),
                orIdMap(subsHits.get(sector)).entrySet().stream()
                        .map(e -> {
                            ItemsSub s = subsById.get(e.getKey());
                            return new ItemSubHitDto(s.getIdItemSub(), s.getUrlItemSub(), e.getValue());
                        })
                        .toList(),
                orAssociationMap(associationsHits.get(sector)).entrySet().stream()
                        .map(e -> new AssociationHitDto(e.getKey(), e.getValue()))
                        .toList());
    }

    /**
     * Отчёт по объекту или «весь опрос» (оба id = null).
     */
    public ResultOneCharacteristicResponseDto getResult(ResultOneCharacteristicRequestDto request) {
        UUID idSurvey = request.idSurvey();
        UUID idRun = request.idRun();
        UUID idCharacteristic = request.idCharacteristic();
        UUID idItem = request.idItem();
        boolean surveyMode = idCharacteristic == null && idItem == null;
        boolean byCharacteristic = idCharacteristic != null;
        researchAccessService.getOwnedSurvey(request.idSurvey());

        // 1. Колесо. «Весь опрос» — попадания всех объектов по секторам (сколько раз
        //    что-либо попало в сектор). «Объект» — число респондентов (сессий),
        //    поместивших объект в сектор; при уникальности объекта в сессии это
        //    совпадает с числом попаданий, distinct — страховка от аномалий.
        List<SectorCountDto> counts = surveyMode
                ? buildCounts(mergeSurveyCounts(idSurvey, idRun))
                : buildCounts(toCountsMap(byCharacteristic
                ? characteristicsRepository.countSectorsByCharacteristic(idCharacteristic, idSurvey, idRun)
                : itemsRepository.countSectorsByItem(idItem, idSurvey, idRun)));

        // 2. Все 8 секторов — всегда.
        List<Integer> sectors = IntStream.rangeClosed(1, TOTAL_SECTORS)
                .boxed()
                .toList();

        // 3. Содержание: «весь опрос» — оба типа; «объект» — только противоположный
        //    тип (главный объект уже отражён в колесе).
        List<AnswersCharacteristics> characteristicAnswers =
                (surveyMode || !byCharacteristic)
                        ? characteristicsRepository.findAllCharacteristicsBySurvey(idSurvey, idRun)
                        : List.of();

        List<AnswersItems> itemAnswers =
                (surveyMode || byCharacteristic)
                        ? itemsRepository.findAllItemsBySurvey(idSurvey, idRun)
                        : List.of();

        List<AnswersItemsSubs> subAnswers =
                itemsSubRepository.findAllItemsSubBySurvey(idSurvey, idRun);

        List<AnswersAssociations> associationAnswers =
                associationsRepository.findAllAssociationsBySurvey(idSurvey, idRun);

        // 4–6. Справочники, попадания, ассоциации — без изменений
        Map<UUID, Characteristics> characteristicsById = toDictionary(
                characteristicAnswers,
                a -> a.getCharacteristic().getIdCharacteristic(),
                AnswersCharacteristics::getCharacteristic);

        Map<UUID, Items> itemsById = toDictionary(
                itemAnswers,
                a -> a.getItem().getIdItem(),
                AnswersItems::getItem);

        Map<UUID, ItemsSub> subsById = toDictionary(
                subAnswers,
                a -> a.getItemsSub().getIdItemSub(),
                AnswersItemsSubs::getItemsSub);

        Map<Integer, Map<UUID, Long>> characteristicsHits = buildHits(
                characteristicAnswers,
                AnswersCharacteristics::getNumberSector,
                a -> a.getCharacteristic().getIdCharacteristic());

        Map<Integer, Map<UUID, Long>> itemsHits = buildHits(
                itemAnswers,
                AnswersItems::getNumberSector,
                a -> a.getItem().getIdItem());

        Map<Integer, Map<UUID, Long>> subsHits = buildHits(
                subAnswers,
                AnswersItemsSubs::getNumberSector,
                a -> a.getItemsSub().getIdItemSub());

        Map<Integer, Map<String, Long>> associationsHits = associationAnswers.stream()
                .collect(Collectors.groupingBy(
                        AnswersAssociations::getNumberSector,
                        Collectors.groupingBy(
                                AnswersAssociations::getAnswer,
                                Collectors.counting())));

        // 7. Сборка по секторам — без изменений
        List<SectorContentDto> sectorContents = sectors.stream()
                .map(sector -> new SectorContentDto(
                        sector,
                        orIdMap(characteristicsHits.get(sector)).entrySet().stream()
                                .map(e -> {
                                    Characteristics c = characteristicsById.get(e.getKey());
                                    return new CharacteristicHitDto(
                                            c.getIdCharacteristic(), c.getTextCharacteristic(), e.getValue());
                                })
                                .toList(),
                        orIdMap(itemsHits.get(sector)).entrySet().stream()
                                .map(e -> {
                                    Items i = itemsById.get(e.getKey());
                                    return new ItemHitDto(i.getIdItem(), i.getUrlItem(), e.getValue());
                                })
                                .toList(),
                        orIdMap(subsHits.get(sector)).entrySet().stream()
                                .map(e -> {
                                    ItemsSub s = subsById.get(e.getKey());
                                    return new ItemSubHitDto(s.getIdItemSub(), s.getUrlItemSub(), e.getValue());
                                })
                                .toList(),
                        orAssociationMap(associationsHits.get(sector)).entrySet().stream()
                                .map(e -> new AssociationHitDto(e.getKey(), e.getValue()))
                                .toList()))
                .toList();

        return new ResultOneCharacteristicResponseDto(
                idCharacteristic,
                idItem,
                counts,
                sectorContents);
    }

    // ---------- Подсчёты и проценты ----------

    /** Проекция -> карта сектор/счётчик (объединяет дубликаты на всякий случай). */
    private static Map<Integer, Long> toCountsMap(List<SectorCountProjection> rows) {
        Map<Integer, Long> map = new HashMap<>();
        for (SectorCountProjection r : rows) {
            map.merge(r.getNumberSector(), r.getCount(), Long::sum);
        }
        return map;
    }

    /** Режим «весь опрос»: попадания характеристик + попадания изображений. */
    private Map<Integer, Long> mergeSurveyCounts(UUID idSurvey, UUID idRun) {
        Map<Integer, Long> merged = new HashMap<>();
        for (SectorCountProjection r : characteristicsRepository.countSectorHitsBySurvey(idSurvey, idRun)) {
            merged.merge(r.getNumberSector(), r.getCount(), Long::sum);
        }
        for (SectorCountProjection r : itemsRepository.countSectorHitsBySurvey(idSurvey, idRun)) {
            merged.merge(r.getNumberSector(), r.getCount(), Long::sum);
        }
        return merged;
    }

    /** Колесо из карты сектор/счётчик с процентами и балансировкой суммы до 100. */
    private List<SectorCountDto> buildCounts(Map<Integer, Long> bySectorRaw) {
        long total = bySectorRaw.values().stream().mapToLong(Long::longValue).sum();

        List<SectorCountDto> counts = new ArrayList<>();
        for (int sector = 1; sector <= TOTAL_SECTORS; sector++) {
            long count = bySectorRaw.getOrDefault(sector, 0L);
            counts.add(new SectorCountDto(
                    sector,
                    count,
                    total == 0 ? 0 : (int) Math.round(count * 100.0 / total)));
        }

        return fixPercentSum(counts);
    }

    private static List<SectorCountDto> fixPercentSum(List<SectorCountDto> sectors) {
        if (sectors.isEmpty()) {
            return sectors;
        }

        int sum = sectors.stream()
                .mapToInt(SectorCountDto::percent)
                .sum();

        if (sum == 0) {
            return sectors;
        }

        int diff = 100 - sum;
        if (diff == 0) {
            return sectors;
        }

        SectorCountDto biggest = sectors.stream()
                .max(Comparator.comparingLong(SectorCountDto::count))
                .orElseThrow();

        return sectors.stream()
                .map(s -> s.numberSector() == biggest.numberSector()
                        ? new SectorCountDto(s.numberSector(), s.count(), s.percent() + diff)
                        : s)
                .toList();
    }

    // ---------- Хелперы ----------

    // Справочник: уникальный объект по id (дубликаты ответов схлопываются)
    private static <T, E> Map<UUID, E> toDictionary(List<T> answers,
                                                    Function<T, UUID> key,
                                                    Function<T, E> value) {
        return answers.stream()
                .collect(Collectors.toMap(key, value, (first, second) -> first));
    }

    // Попадания: сектор -> (id объекта -> количество)
    private static <T> Map<Integer, Map<UUID, Long>> buildHits(List<T> answers,
                                                               Function<T, Integer> sector,
                                                               Function<T, UUID> id) {
        return answers.stream()
                .collect(Collectors.groupingBy(
                        sector,
                        Collectors.groupingBy(id, Collectors.counting())));
    }

    // Типизированные замены null на пустую карту — иначе компилятор ругается на capture<?>
    private static Map<UUID, Long> orIdMap(Map<UUID, Long> map) {
        return map == null ? Map.of() : map;
    }

    private static Map<String, Long> orAssociationMap(Map<String, Long> map) {
        return map == null ? Map.of() : map;
    }
}
