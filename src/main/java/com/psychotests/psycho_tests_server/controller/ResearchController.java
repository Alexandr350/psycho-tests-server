package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.*;
import com.psychotests.psycho_tests_server.dto.response.ResearchResponseInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.response.ResearchResponseSurveyDtoPart2;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/researchs")
@RequiredArgsConstructor
public class ResearchController {

    private final CreateResearchService researchService;

    @PostMapping("/research")
    public ResponseEntity<ResearchResponseInfoDtoPart1> addInfo(@Valid @RequestBody ResearchRequestInfoDtoPart1 dto){
        researchService.addInfoResearch(dto);
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/survey")
    public ResponseEntity<ResearchResponseSurveyDtoPart2> addSurvey(@Valid @RequestBody ResearchRequestSurveyDtoPart2 dto){
        researchService.addInfoSurvey(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/characteristics")
    public ResponseEntity<Void> addCharacteristics(@Valid @RequestBody ResearchRequestCharacteristicsDtoPart3 dto){
        researchService.addCharacteristics(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItems(@Valid @RequestBody ResearchRequestItemsDtoPart4 dto){
        researchService.addItems(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items-sub")
    public ResponseEntity<Void> addItemSub(@Valid @RequestBody ResearchRequestItemsSubDtoPart5 dto){
        researchService.addItemSub(dto);
        return ResponseEntity.noContent().build();
    }





}
