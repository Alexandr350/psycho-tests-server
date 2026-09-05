package com.psychotests.psycho_tests_server.controller;
import com.psychotests.psycho_tests_server.dto.request.*;
import com.psychotests.psycho_tests_server.dto.response.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.service.CreateResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/researchs")
@RequiredArgsConstructor
public class ResearchController {
    @Value("${upload.path}")
    private String baseUploadPath;
    private final CreateResearchService researchService;


    @PostMapping("/research")
    public ResponseEntity<String> addInfo(@Valid @RequestBody ResearchRequestInfoDtoPart1 dto){
        String researchId = researchService.addInfoResearch(dto);
        return ResponseEntity.ok(researchId);

    }

    @PostMapping("/survey")
    public ResponseEntity<String> addSurvey(@Valid @RequestBody ResearchRequestSurveyDtoPart2 dto){
        String surveyId = researchService.addInfoSurvey(dto);
        return ResponseEntity.ok(surveyId);
    }

    @PostMapping("/characteristics")
    public ResponseEntity<String> addCharacteristics(@Valid @RequestBody ResearchRequestCharacteristicsDtoPart3 dto){
        researchService.addCharacteristics(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItems(
            @RequestParam("surveyId") String surveyId,
            @RequestParam("files") List<MultipartFile> files){
        if(files == null || files.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        MultipartFile[] filesArray = files.toArray(new MultipartFile[0]);
        List<String> savedPathImages = serviceSavingResearchImages(filesArray, surveyId);
        for (String  s: savedPathImages){
            System.out.println(s);
        }
        ResearchRequestItemsDtoPart4 dtoPart4 = new ResearchRequestItemsDtoPart4(surveyId, savedPathImages);
        researchService.addItems(dtoPart4);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items-sub")
    public ResponseEntity<ResearchResponseInfoDto> addItemSub(
            @RequestParam("surveyId") String surveyId,
            @RequestParam("files")  List<MultipartFile> files){
        if(files == null || files.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        MultipartFile[] filesArray = files.toArray(new MultipartFile[0]);
        List<String> savedPathImagesSub = serviceSavingResearchImages(filesArray, surveyId);
        for (String  s: savedPathImagesSub){
            System.out.println(s);
        }
        ResearchRequestItemsSubDtoPart5 dtoPart5 = new ResearchRequestItemsSubDtoPart5(surveyId, savedPathImagesSub);
        ResearchResponseInfoDto responseInfo = researchService.addItemSub(dtoPart5);
        return ResponseEntity.ok(responseInfo);
    }

    private List<String> serviceSavingResearchImages(MultipartFile[] files, String surveyId) {
        List<String> savedPaths = new ArrayList<>();
        Path dir = Paths.get(baseUploadPath, "uploads", "ResearchPage", "image", surveyId);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось создать директорию", e);
            }
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String fileName = UUID.randomUUID()
                    + (originalFilename.lastIndexOf('.') > 0
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "");

            Path destPath = dir.resolve(fileName);
            try {
                file.transferTo(destPath);
                String webPath = "/uploads/ResearchPage/image/" + surveyId + "/" + fileName;
                savedPaths.add(webPath);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при сохранении файла: " + originalFilename, e);
            }
        }
        return savedPaths;
    }

}






