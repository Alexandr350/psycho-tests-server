package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест контура «загрузка файла → раздача по HTTP».
 * Проверяет стыковку двух компонентов, чьи пути легко разъехаться:
 *  - FileStorageService пишет файлы в каталог upload.path/uploads/...;
 *  - WebMvcConfig раздаёт этот каталог по URL /uploads/**.
 * Именно их рассинхронизация даёт 404 на картинках в проде/деве,
 * поэтому тест сохраняет файл продовым сервисом и читает его тем же
 * URL, который сервис вернул в ответе.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileServingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FileStorageService fileStorageService;


    @Test
    void uploadedFileIsServedByResourceHandler() throws Exception {
        // Валидный минимальный JPEG (1×1 пиксель), закодированный в base64:
        // resource handler проверяет не содержимое, а тип и размер, поэтому
        // достаточно настоящего заголовка файла, а не полноценной картинки.
        byte[] jpeg = Base64.getDecoder().decode(
                "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0a" +
                        "HBwcJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAA" +
                        "AAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==");

        // Имя части "files" должно совпадать с параметром контроллера загрузки,
        // через который обычно вызывается saveResearchImages.
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", MediaType.IMAGE_JPEG_VALUE, jpeg);

        UUID idSurvey = UUID.randomUUID(); // изолированный каталог на каждый прогон теста

        // Шаг 1. Сохраняем через продовый сервис — как это делает контроллер загрузки.
        List<String> paths = fileStorageService.saveResearchImages(idSurvey, List.of(file));

        // Шаг 2. Запрашиваем ровно тот URL, который сервис вернул.
        // Если пути сервиса и resource handler совпали — получаем 200 с JPEG.
        mockMvc.perform(get(paths.getFirst()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));

        // Шаг 3. Подчищаем за собой: deleteSurveyImages удаляет весь каталог опроса.
        fileStorageService.deleteSurveyImages(idSurvey);
    }
}
