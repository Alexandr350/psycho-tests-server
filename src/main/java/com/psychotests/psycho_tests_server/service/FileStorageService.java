package com.psychotests.psycho_tests_server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileStorageService {

    @Value("${upload.path}")
    private String baseUploadPath;

    public List<String> saveResearchImages(UUID idSurvey, List<MultipartFile> files) {
        List<String> savedPaths = new ArrayList<>();
        Path dir = Paths.get(baseUploadPath, "uploads", "ResearchPage", "image", idSurvey.toString());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию", e);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;


            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.lastIndexOf('.') > 0)
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";
            String fileName = UUID.randomUUID() + extension;

            try {
                file.transferTo(dir.resolve(fileName));
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при сохранении файла: " + originalFilename, e);
            }
            savedPaths.add("/uploads/ResearchPage/image/" + idSurvey + "/" + fileName);
        }
        return savedPaths;
    }


    public void deleteSurveyImages(UUID idSurvey){
        Path dirPath = Paths.get(baseUploadPath, "uploads", "ResearchPage", "image", idSurvey.toString());

        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(this::deleteQuietly);
        }catch (IOException e){
            log.warn("Не удалось удалить файлы опроса {}: {}", idSurvey, e.getMessage());
        }
    }

    private void deleteQuietly(Path path){
        try {
            Files.delete(path);
        }catch (IOException e){
            log.warn("Не удалось удалить {}: {}", path, e.getMessage());
        }
    }
}

