package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestCharacteristicsDtoPart3;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestSurveyDtoPart2;
import com.psychotests.psycho_tests_server.dto.response.research_dto.*;
import com.psychotests.psycho_tests_server.dto.request.change_research_dto.ChangeStatusRequestDto;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import com.psychotests.psycho_tests_server.service.FileStorageService;
import com.psychotests.psycho_tests_server.service.ResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/research/{idResearch}")
    public ResearchResponseInfoDto getResearch(@PathVariable UUID idResearch) {
        return researchService.getResearch(idResearch);
    }

    // ---------- Смена статуса и удаление ----------

    @PatchMapping("/research/{idResearch}/status")
    public ResponseEntity<ResearchResponseInfoDto> changeStatus(
            @PathVariable UUID idResearch,
            @Valid @RequestBody ChangeStatusRequestDto dto) {
        return ResponseEntity.ok(researchService.changeStatus(idResearch, dto.status()));
    }



    @DeleteMapping("/research/{idResearch}")
    public ResponseEntity<Void> deleteResearch(@PathVariable UUID idResearch) {
        researchService.deleteResearch(idResearch);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/research/{idResearch}")
    public ResponseEntity<Void> updateInfo(
            @PathVariable UUID idResearch,
            @Valid @RequestBody ResearchRequestInfoDtoPart1 dto) {
        researchService.updateResearchInfo(idResearch, dto); // внутри — проверка статуса DRAFT/NEW
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/survey/{idSurvey}")
    public ResponseEntity<ResearchResponseInfoDto> updateSurvey(
            @PathVariable UUID idSurvey,
            @Valid @RequestBody ResearchRequestSurveyDtoPart2 dto) {
        return ResponseEntity.ok(createResearchService.updateSurvey(idSurvey, dto));
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
            @RequestParam("idSurvey") UUID idSurvey,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> paths = fileStorageService.saveResearchImages(idSurvey, files);
        createResearchService.addItems(idSurvey, paths);
        return ResponseEntity.noContent().build();
    }

    /** «Запустить снова» на остановленном исследовании: открывает новую
     *  волну (Run) в том же research без возврата в NEW. */
    @PostMapping("/research/{idResearch}/restart")
    public ResponseEntity<ResearchResponseInfoDto> restart(
            @PathVariable UUID idResearch) {
        return ResponseEntity.ok(researchService.restartResearch(idResearch));
    }


    @PostMapping("/items-sub")
    public ResponseEntity<ResearchResponseInfoDto> addItemSub(
            @RequestParam("idSurvey") UUID idSurvey,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        List<String> paths = (files == null || files.isEmpty())
                ? List.of()
                : fileStorageService.saveResearchImages(idSurvey, files);
        return ResponseEntity.ok(createResearchService.addItemSub(idSurvey, paths));
    }


    @GetMapping("/{idResearch}/runs/{idRun}/progress")
    public RunProgressResponseDto getRunProgress(@PathVariable UUID idResearch,
                                                 @PathVariable UUID idRun) {
        return researchService.getRunProgress(idResearch, idRun);
    }

    @GetMapping("/progress")
    public List<ResearchProgressResponseDto> getProgressAll() {
        return researchService.getProgressAll();
    }

    /** Список волн (запусков) исследования — для селектора в отчёте. */
    @GetMapping("/research/{idResearch}/runs")
    public List<RunListItemDto> getRuns(@PathVariable UUID idResearch) {
        return researchService.findRuns(idResearch);
    }


}
