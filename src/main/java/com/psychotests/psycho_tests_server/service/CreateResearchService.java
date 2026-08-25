package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.*;
import com.psychotests.psycho_tests_server.exception.UnauthorizedException;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.List;

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

    private Research researchSaved;
    private Survey surveySaved;


    public void addInfoResearch(ResearchRequestInfoDtoPart1 dtoPart1){
        Research research = new Research(
                createUUID(),
                dtoPart1.nameResearch(),
                dtoPart1.descriptionResearch(),
                dtoPart1.customerResearch(),
                dtoPart1.typeResearch()
        );
        research.setPsychologistAccount(getCurrentPsychologist());
        researchSaved = researchRepository.save(research);

    }

    public void addInfoSurvey(ResearchRequestSurveyDtoPart2 dtoPart2){
        Survey survey = new Survey(
                createUUID(),
                dtoPart2.titleSurvey(),
                dtoPart2.descriptionSurvey(),
                dtoPart2.rulesSurvey()
        );
        survey.setResearch(researchSaved);
        surveySaved = surveyRepository.save(survey);

    }

    public void addCharacteristics(ResearchRequestCharacteristicsDtoPart3 dtoPart3){
        List<String> tokens = splitTokens(dtoPart3.characteristics());

        for(String txt: tokens){
            Characteristics characteristics = new Characteristics(createUUID(), txt);
            characteristics.setSurvey(surveySaved);
            characteristicsRepository.save(characteristics);
        }

    }

    public void addItems(ResearchRequestItemsDtoPart4 dtoPart4){
        for(String path: dtoPart4.itemPaths()){
            Items item = new Items(createUUID(), path);
            item.setSurvey(surveySaved);
            itemsRepository.save(item);

        }
    }

    public void addItemSub(ResearchRequestItemsSubDtoPart5 dtoPart5){
        for(String path: dtoPart5.itemSubPath()){
            ItemsSub itemSub = new ItemsSub(createUUID(), path);
            itemSub.setSurvey(surveySaved);
            itemsSubRepository.save(itemSub);
        }
    }





    public void createResearch(){}
    public void deleteResearch(){}
    public void showResultResearch(){}
    public void startResearch(){}

    
}
