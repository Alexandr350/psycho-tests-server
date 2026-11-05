package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchProgressResponseDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.RunListItemDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.RunProgressResponseDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.PublicSurveyAccessResponseDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.repository.RunRepository;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;
import com.psychotests.psycho_tests_server.repository.SurveySessionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.psychotests.psycho_tests_server.repository.UniqueSurveyCodeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResearchService {

    private final EntityManager entityManager;
    private final ResearchRepository researchRepository;
    private final ResearchAccessService researchAccessService;
    private final CurrentPsychologistProvider currentPsychologistProvider;
    private final FileStorageService fileStorageService;
    private final SurveyRepository surveyRepository;
    private final SurveySessionRepository surveySessionRepository;
    private final RunRepository runRepository;
    private static final Duration ACTIVE_SESSION_WINDOW = Duration.ofMinutes(30);
    private final UniqueSurveyCodeRepository uniqueSurveyCodeRepository;


    @Transactional(readOnly = true)
    public List<ResearchResponseInfoDto> findAllResearches() {
        UUID currentId = currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
        return researchRepository.findAllByPsychologistAccountIdPsychologistAccount(currentId).stream()
                .map(ResearchResponseInfoDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResearchResponseInfoDto getResearch(UUID idResearch) {
        Research research = researchAccessService.getOwnedResearch(idResearch);
        Survey survey = surveyRepository
                .findByResearchIdResearchAndPsychologistId(idResearch, currentId())
                .orElse(null);
        return ResearchResponseInfoDto.from(research, survey);
    }

    private UUID currentId() {
        return currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
    }

    @Transactional(readOnly = true)
    public PublicSurveyAccessResponseDto resolvePublicToken(String token) {
        Run run = runRepository.findByPublicToken(token)
                .filter(r -> r.isOpen() && r.getResearch().getStatusResearch() == ResearchStatus.RUNNING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Опрос недоступен"));
        Survey survey = surveyRepository.findByResearchIdResearch(run.getResearch().getIDResearch())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Опрос недоступен"));
        return new PublicSurveyAccessResponseDto(
                survey.getIdSurvey(),
                survey.getTitleSurvey(),
                survey.getDescriptionSurvey(),
                survey.getRulesSurvey());
    }

    public List<RunListItemDto> findRuns(UUID idResearch) {
        // Проверка владения: чужому психологу волны не отдаём
        researchAccessService.getOwnedResearch(idResearch);
        return runRepository.findAllByResearch(idResearch).stream()
                .map(r -> new RunListItemDto(r.getIdRun(), r.getStartTimeRun(), r.getFinishTimeRun(), r.isOpen()))
                .toList();
    }



    @Transactional
    public ResearchResponseInfoDto changeStatus(UUID researchId, ResearchStatus newStatus) {
        Research research = researchAccessService.getOwnedResearch(researchId);
        research.changeStatus(newStatus);   // токен создастся/обнулится внутри домена
        syncRuns(research);                 // запуск открывается/закрывается синхронно

        Survey survey = surveyRepository.findByResearchIdResearchAndPsychologistId(
                researchId,
                currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount()
        ).orElse(null);
        return ResearchResponseInfoDto.from(research, survey);
    }

    @Transactional
    public void deleteResearch(UUID researchId) {
        // Сбрасываем контекст и отсоединяем сущности, чтобы дальнейшие
        // проверки и удаление работали с актуальной копией из БД, а не с
        // объектом, изменённым ранее в этой же транзакции.
        entityManager.flush();
        entityManager.clear();
        Research research = researchAccessService.getOwnedResearch(researchId);

        if (!research.getStatusResearch().canBeDeleted()) {
            throw new IllegalStateException(
                    "Нельзя удалить исследование в статусе " + research.getStatusResearch());
        }

        Survey survey = research.getSurveyResearch();

        if (survey != null) {
            // Блокируем удаление только если прохождение реально идёт прямо сейчас:
            // сессия не завершена и начата недавно. Брошенные сессии старше окна
            // удалению не мешают — при удалении они уйдут каскадом вместе с опросом.
            LocalDateTime cutoff = LocalDateTime.now().minus(ACTIVE_SESSION_WINDOW);
            boolean hasActiveSession = surveySessionRepository.existsActiveSession(
                    survey.getIdSurvey(), SurveySessionStatus.IN_PROGRESS, cutoff);

            if (hasActiveSession) {
                throw new IllegalStateException(
                        "Идёт прохождение опроса. Повторите удаление через %d минут"
                                .formatted(ACTIVE_SESSION_WINDOW.toMinutes()));
            }
        }

        // файлы живут вне БД — их каскад не достанет, поэтому удаляем
        // до сноса строк (после удаления survey idSurvey взять неоткуда)
        if (survey != null) {
            fileStorageService.deleteSurveyImages(survey.getIdSurvey());
        }

        // дальше всю работу делает БД: FK-каскады сносят survey, runs,
        // unique_survey_code, sessions и все answers_*; Hibernate
        // благодаря @OnDelete не грузит и не удаляет детей сам
        researchRepository.delete(research);

        entityManager.flush();
        entityManager.clear();
    }


    @Transactional
    public void updateResearchInfo(UUID researchId, ResearchRequestInfoDtoPart1 dto) {
        Research research = researchAccessService.getOwnedResearch(researchId);

        if (!research.getStatusResearch().canBeEdited()) {
            throw new IllegalStateException(
                    "Нельзя редактировать исследование в статусе " + research.getStatusResearch());
        }

        research.setNameResearch(dto.nameResearch());
        research.setDescriptionResearch(dto.descriptionResearch());
        research.setCustomerResearch(dto.customerResearch());
        research.setTypeResearch(dto.typeResearch());
        // save не нужен: сущность управляемая, flush в конце транзакции сохранит изменения
    }

    private void syncRuns(Research research) {
        switch (research.getStatusResearch()) {
            case RUNNING -> runRepository.findOpenRun(research.getIDResearch())
                    .orElseGet(() -> {
                        var newRun = new Run(UUID.randomUUID(), research, research.getPublicToken());
                        return runRepository.save(newRun);
                    });
            case STOPPED -> runRepository.findOpenRun(research.getIDResearch())
                    .ifPresent(run -> {
                        run.finish();
                        runRepository.save(run);
                    });
            default -> { /* DRAFT/NEW/PAUSE/ARCHIVE — запуски не трогаем */ }
        }
    }

    @Transactional
    public ResearchResponseInfoDto restartResearch(UUID idResearch) {
        Research research = researchAccessService.getOwnedResearch(idResearch);

        // защита: закрываем висячий открытый запуск, иначе ветка RUNNING
        // в syncRuns подхватит его со старым токеном — рестарт станет фикцией
        runRepository.findOpenRun(idResearch).ifPresent(run -> {
            run.finish();
            runRepository.save(run);
        });

        research.restart();   // статус + новый токен — правила в домене
        syncRuns(research);   // RUNNING → создаст Run с новым токеном

        Survey survey = surveyRepository.findByResearchIdResearchAndPsychologistId(
                idResearch, currentId()).orElse(null);
        return ResearchResponseInfoDto.from(research, survey);
    }

    @Transactional(readOnly = true)
    public RunProgressResponseDto getRunProgress(UUID idResearch, UUID idRun) {
        Research research = researchAccessService.getOwnedResearch(idResearch);

        Run run = runRepository.findById(idRun)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запуск не найден"));

        // запуск должен принадлежать исследованию, которое владеет текущий пользователь
        if (!research.getIDResearch().equals(run.getResearch().getIDResearch())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запуск не относится к этому исследованию");
        }

        long completed = surveySessionRepository
                .countByRunIdRunAndStatusSurveySession(idRun, SurveySessionStatus.COMPLETED);
        Long invited = uniqueSurveyCodeRepository.countByRunIdRun(idRun);

        return new RunProgressResponseDto(idRun, completed, invited);

    }

    @Transactional(readOnly = true)
    public List<ResearchProgressResponseDto> getProgressAll() {
        UUID currentId = currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
        List<Research> researches = researchRepository
                .findAllByPsychologistAccountIdPsychologistAccount(currentId);

        Map<UUID, Long> completedBySurvey = surveySessionRepository
                .countByStatusGroupedBySurvey(SurveySessionStatus.COMPLETED).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        return researches.stream()
                .map(r -> {
                    Survey survey = r.getSurveyResearch();
                    long completed = survey == null
                            ? 0
                            : completedBySurvey.getOrDefault(survey.getIdSurvey(), 0L);
                    return new ResearchProgressResponseDto(r.getIDResearch(), completed);
                })
                .toList();
    }








}
