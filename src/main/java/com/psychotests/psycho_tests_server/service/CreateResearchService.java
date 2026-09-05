package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.*;
import com.psychotests.psycho_tests_server.dto.response.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.exception.UnauthorizedException;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.psychotests.psycho_tests_server.service.Utility.createUUID;
import static com.psychotests.psycho_tests_server.service.Utility.splitTokens;

@Service
@RequiredArgsConstructor
public class CreateResearchService {

    private final ResearchRepository researchRepository;
    private final SurveyRepository surveyRepository;
    private final CharacteristicsRepository characteristicsRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsSubRepository itemsSubRepository;

    @NonNull
    private PsychologistAccount getCurrentPsychologist(){
        Authentication currentPsychologistAuth = SecurityContextHolder.getContext().getAuthentication();

        if(currentPsychologistAuth == null || !currentPsychologistAuth.isAuthenticated()){
            throw new UnauthorizedException("Пользователь не авторизован");
        }

        if (!(currentPsychologistAuth.getPrincipal() instanceof PsychologistUserDetails psychologistDetails)) {
            throw new UnauthorizedException("Нет аккаунта");
        }

        return psychologistDetails.getAccount();
    }


    @Transactional
    public String addInfoResearch(ResearchRequestInfoDtoPart1 dtoPart1){
        Research research = new Research(
                createUUID(),
                dtoPart1.nameResearch(),
                dtoPart1.descriptionResearch(),
                dtoPart1.customerResearch(),
                dtoPart1.typeResearch()
        );
        research.setPsychologistAccount(getCurrentPsychologist());
        Research researchSaved = researchRepository.save(research);
        return String.valueOf(researchSaved.getIDResearch());

    }
    @Transactional
    public String addInfoSurvey(ResearchRequestSurveyDtoPart2 dtoPart2){
        Research research = researchRepository.findById(UUID.fromString(dtoPart2.researchId()))
                .orElseThrow(() -> new IllegalArgumentException("Исследование не найдено"));
        Survey survey = new Survey(
                createUUID(),
                dtoPart2.titleSurvey(),
                dtoPart2.descriptionSurvey(),
                dtoPart2.rulesSurvey()
        );
        survey.setResearch(research);
        Survey surveySaved = surveyRepository.save(survey);
        return String.valueOf(surveySaved.getIDSurvey());

    }

    @Transactional
    public void addCharacteristics(ResearchRequestCharacteristicsDtoPart3 dtoPart3){
        Survey survey = surveyRepository.findById(UUID.fromString(dtoPart3.surveyId()))
                .orElseThrow(() -> new IllegalArgumentException("Опрос не найден"));

        List<String> tokens = splitTokens(dtoPart3.characteristics());

        for(String txt: tokens){
            Characteristics characteristics = new Characteristics(createUUID(), txt);
            characteristics.setSurvey(survey);
            characteristicsRepository.save(characteristics);
        }

    }

    @Transactional
    public void addItems(ResearchRequestItemsDtoPart4 dtoPart4){
        Survey survey = surveyRepository.findById(UUID.fromString(dtoPart4.surveyId()))
                .orElseThrow(() -> new IllegalArgumentException("Опрос не найден"));

        for(String path: dtoPart4.itemPaths()){
            System.out.println(path);
            Items item = new Items(createUUID(), path);
            item.setSurvey(survey);
            itemsRepository.save(item);

        }
    }

    @Transactional
    public ResearchResponseInfoDto addItemSub(ResearchRequestItemsSubDtoPart5 dtoPart5){
        Survey survey = surveyRepository.findById(UUID.fromString(dtoPart5.surveyId()))
                .orElseThrow(() -> new IllegalArgumentException("Опрос не найден"));

        for(String path: dtoPart5.itemSubPath()){
            ItemsSub itemSub = new ItemsSub(createUUID(), path);
            itemSub.setSurvey(survey);
            itemsSubRepository.save(itemSub);
        }

        Research research = survey.getResearch();
        research.setStatusResearch(ResearchStatus.NEW);
        Research finalizedResearch = researchRepository.save(research);
        return ResearchResponseInfoDto.from(finalizedResearch);
    }



    public void createResearch(){}
    public void deleteResearch(){}
    public void showResultResearch(){}
    public void startResearch(){}

    
}
