package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.*;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.*;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CPSKTestService {

    private static final int SECTOR_COUNT = 8;
    private static final int MAX_ASSOCIATIONS_PER_SECTOR = 5;


    private final SurveyRepository surveyRepository;
    private final SurveySessionRepository surveySessionRepository;
    private final RespondentRepository respondentRepository;
    private final UniqueSurveyCodeRepository uniqueSurveyCodeRepository;
    private final CharacteristicsRepository characteristicsRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsSubRepository itemsSubRepository;



    @Transactional
    public SurveyFlowStartResponseDto createSession(UUID surveyId, UUID codeId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Опрос не найден"));

        Respondent respondent;
        if (codeId != null) {
            UniqueSurveyCode code = uniqueSurveyCodeRepository.findById(codeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Код не найден"));
            if (!code.getResearch().equals(survey.getResearch())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Код не относится к этому исследованию");
            }
            respondent = code.getRespondent();
            if (respondent == null) {
                respondent = new Respondent(UUID.randomUUID());
                code.setRespondent(respondent);
            }
        } else {
            respondent = new Respondent(UUID.randomUUID());
        }
        respondentRepository.save(respondent);


        SurveySession session = new SurveySession(UUID.randomUUID());
        session.setSurvey(survey);
        session.setRespondent(respondent);
        session.setStatusSurveySession(SurveySessionStatus.NOT_STARTED);
        surveySessionRepository.save(session);
        return new SurveyFlowStartResponseDto(session.getIDSurveySession());
    }

    @Transactional
    public SurveyFlowResponseDto startFlow(UUID surveyId, SurveyFlowQuestionnaireRequestDto requestDto) {
        SurveySession session = requireSession(requestDto.idSession(), surveyId);

        if (session.getStatusSurveySession() != SurveySessionStatus.NOT_STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сессия уже начата");
        }

        session.getRespondent().setAgeRespondent(requestDto.age());
        session.getRespondent().setGenderRespondent(requestDto.gender());
        session.setStatusSurveySession(SurveySessionStatus.IN_PROGRESS);
        return buildSurveyFlowResponseDto(session.getSurvey());
    }


    @Transactional
    public void submit(UUID surveyId, SurveyFlowSubmitRequestDto requestDto) {

        SurveySession session = requireSession(requestDto.idSession(), surveyId);

        if (session.getStatusSurveySession() != SurveySessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Результаты уже отправлены или сессия не начата");
        }
        validateAssociations(requestDto.associationsAnswer());

        Survey survey = session.getSurvey();
        saveCharacteristicAnswers(session, survey, requestDto.characteristicAnswer());
        saveImageAnswers(session, survey, requestDto.imageAnswer());
        saveImageSubAnswers(session, survey, requestDto.imageSubAnswer());
        saveAssociationsAnswers(session, requestDto.associationsAnswer());

        session.setStatusSurveySession(SurveySessionStatus.COMPLETED);
        session.setFinishTimeSession(LocalDateTime.now());
    }

    private void validateAssociations(List<AssociationAnswerDto> dtos) {
        dtos.stream()
                .collect(Collectors.groupingBy(AssociationAnswerDto::numberSector, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > MAX_ASSOCIATIONS_PER_SECTOR)
                .min(Map.Entry.comparingByKey())          // детерминированный порядок
                .ifPresent(e -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Сектор " + e.getKey() + ": получено " + e.getValue()
                                    + " ассоциаций, максимум " + MAX_ASSOCIATIONS_PER_SECTOR);
                });
    }


    private void saveCharacteristicAnswers(SurveySession session, Survey survey,
                                           List<CharacteristicAnswerDto> dtos) {
        Map<UUID, Characteristics> allowed = characteristicsRepository.findBySurvey(survey).stream()
                .collect(Collectors.toMap(Characteristics::getIdCharacteristic, c -> c));

        Set<UUID> sentIds = ids(dtos, CharacteristicAnswerDto::idCharacteristic);
        requireNoDuplicates(dtos, sentIds, "характеристикам");
        requireFullCoverage(allowed.keySet(), sentIds, "характеристикам");

        session.getAnswersCharacteristics().addAll(dtos.stream()
                .map(dto -> new AnswersCharacteristics(
                        UUID.randomUUID(), session, allowed.get(dto.idCharacteristic()), dto.numberSector()))
                .toList());
    }

    private void saveImageAnswers(SurveySession session, Survey survey, List<ImageAnswerDto> dtos) {
        Map<UUID, Items> allowed = itemsRepository.findBySurvey(survey).stream()
                .collect(Collectors.toMap(Items::getIdItem, i -> i));

        Set<UUID> sentIds = ids(dtos, ImageAnswerDto::idImage);
        requireNoDuplicates(dtos, sentIds, "изображениям");
        requireFullCoverage(allowed.keySet(), sentIds, "изображениям");

        session.getAnswersItems().addAll(dtos.stream()
                .map(dto -> new AnswersItems(
                        UUID.randomUUID(), session, allowed.get(dto.idImage()), dto.numberSector()))
                .toList());
    }

    private void saveImageSubAnswers(SurveySession session, Survey survey, List<ImageAnswerDto> dtos) {
        Map<UUID, ItemsSub> allowed = itemsSubRepository.findBySurvey(survey).stream()
                .collect(Collectors.toMap(ItemsSub::getIdItemSub, i -> i));

        Set<UUID> sentIds = ids(dtos, ImageAnswerDto::idImage);
        requireNoDuplicates(dtos, sentIds, "суб-изображениям");
        requireFullCoverage(allowed.keySet(), sentIds, "суб-изображениям");

        session.getAnswersItemsSubs().addAll(dtos.stream()
                .map(dto -> new AnswersItemsSub(
                        UUID.randomUUID(), session, allowed.get(dto.idImage()), dto.numberSector()))
                .toList());
    }

    private void saveAssociationsAnswers(SurveySession session, List<AssociationAnswerDto> dtos) {
        Map<Integer, List<AssociationAnswerDto>> bySector = dtos.stream()
                .collect(Collectors.groupingBy(AssociationAnswerDto::numberSector));

        bySector.forEach((sector, texts) -> {
            if (texts.size() > MAX_ASSOCIATIONS_PER_SECTOR) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Сектор " + sector + ": получено " + texts.size()
                                + " ассоциаций, максимум " + MAX_ASSOCIATIONS_PER_SECTOR);
            }
        });

        if (dtos.isEmpty()) return;

        session.getAnswersAssociations().addAll(
                IntStream.range(0, dtos.size())
                        .mapToObj(idx -> new AnswersAssociations(
                                UUID.randomUUID(),
                                session,
                                dtos.get(idx).text().trim(),
                                dtos.get(idx).numberSector(),
                                idx))
                        .toList());
    }

    private SurveyFlowResponseDto buildSurveyFlowResponseDto(Survey survey) {
        List<TextQuestionResponseDto> characteristics = characteristicsRepository.findBySurvey(survey)
                .stream()
                .map(c -> new TextQuestionResponseDto(c.getIdCharacteristic(), c.getTextCharacteristic()))
                .toList();

        List<ImageResponseDto> items = itemsRepository.findBySurvey(survey)
                .stream()
                .map(i -> new ImageResponseDto(i.getIdItem(), i.getUrlItem()))
                .toList();

        List<ImageResponseDto> itemsSub = itemsSubRepository.findBySurvey(survey)
                .stream()
                .map(s -> new ImageResponseDto(s.getIdItemSub(), s.getUrlItemSub()))
                .toList();

        InfoResponseDto info = new InfoResponseDto(SECTOR_COUNT, MAX_ASSOCIATIONS_PER_SECTOR);
        return new SurveyFlowResponseDto(characteristics, items, itemsSub, info);
    }


    private SurveySession requireSession(UUID sessionId, UUID surveyId) {
        SurveySession session = surveySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не найдена"));
        if (!session.getSurvey().getIDSurvey().equals(surveyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не относится к этому опросу");
        }
        return session;
    }

    private <T> Set<UUID> ids(List<T> dtos, Function<T, UUID> getter) {
        return dtos.stream().map(getter).collect(Collectors.toSet());
    }

    private <T> void requireNoDuplicates(List<T> dtos, Set<UUID> sentIds, String what) {
        if (dtos.size() != sentIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дублирующиеся ответы " + what);
        }
    }

    private void requireFullCoverage(Set<UUID> expected, Set<UUID> sent, String what) {
        if (!expected.equals(sent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ответы не соответствуют " + what + " теста");
        }
    }
}
