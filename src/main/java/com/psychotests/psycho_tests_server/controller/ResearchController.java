package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestCharacteristicsDtoPart3;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestSurveyDtoPart2;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchCreatedResponseDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.dto.request.change_research_dto.ChangeStatusRequestDto;
import com.psychotests.psycho_tests_server.dto.response.research_dto.SurveyCreatedResponseDto;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import com.psychotests.psycho_tests_server.service.FileStorageService;
import com.psychotests.psycho_tests_server.service.ResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/researchs")
@RequiredArgsConstructor
public class ResearchController {

    private final CreateResearchService createResearchService;
    private final ResearchService researchService;
    private final FileStorageService fileStorageService;


    // ---------- Чтение ----------

    @GetMapping("/all")
    public List<ResearchResponseInfoDto> getAllResearches() {
        return researchService.findAllResearches();
    }

    @GetMapping("/research/{researchId}")
    public ResearchResponseInfoDto getResearch(@PathVariable UUID researchId) {
        return researchService.getResearch(researchId);
    }

    // ---------- Смена статуса и удаление ----------

    @PatchMapping("/research/{researchId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable UUID researchId,
            @Valid @RequestBody ChangeStatusRequestDto dto) {
        researchService.changeStatus(researchId, dto.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/research/{researchId}")
    public ResponseEntity<Void> deleteResearch(@PathVariable UUID researchId) {
        researchService.deleteResearch(researchId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Мастер создания (Part1–Part5) ----------

    @PostMapping("/research")
    public ResponseEntity<ResearchCreatedResponseDto> addInfo(
            @Valid @RequestBody ResearchRequestInfoDtoPart1 dto) {
        ResearchCreatedResponseDto body = ResearchCreatedResponseDto.from(
                createResearchService.addInfoResearch(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/survey")
    public ResponseEntity<SurveyCreatedResponseDto> addSurvey(
            @Valid @RequestBody ResearchRequestSurveyDtoPart2 dto) {
        SurveyCreatedResponseDto body = SurveyCreatedResponseDto.from(
                createResearchService.addInfoSurvey(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/characteristics")
    public ResponseEntity<Void> addCharacteristics(
            @Valid @RequestBody ResearchRequestCharacteristicsDtoPart3 dto) {
        createResearchService.addCharacteristics(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItems(
            @RequestParam("surveyId") UUID surveyId,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> paths = fileStorageService.saveResearchImages(surveyId, files);
        createResearchService.addItems(surveyId, paths);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items-sub")
    public ResponseEntity<ResearchResponseInfoDto> addItemSub(
            @RequestParam("surveyId") UUID surveyId,
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Не переданы файлы");
        }
        List<String> paths = fileStorageService.saveResearchImages(surveyId, files);
        return ResponseEntity.ok(createResearchService.addItemSub(surveyId, paths));
    }
}
