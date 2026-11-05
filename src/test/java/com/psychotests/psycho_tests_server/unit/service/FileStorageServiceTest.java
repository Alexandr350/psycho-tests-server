package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Тесты FileStorageService против реальной файловой системы.
 * ФС мокать не нужно — она дешёвая и быстрая: @TempDir выдаёт каждому тесту
 * свою временную директорию и удаляет её после. baseUploadPath внедряется
 * полем через @Value, поэтому в тесте выставляется через ReflectionTestUtils.
 * Ошибочные ветки (IOException) покрываются mockStatic(Files) для создания
 * директории и моком MultipartFile для ошибки transferTo — реальный диск там
 * не нужен.
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private final FileStorageService service = new FileStorageService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUploadPath", tempDir.toString());
    }

    /** Директория на диске, куда сервис реально кладёт файлы для idSurvey. */
    private Path imageDir(UUID idSurvey) {
        return tempDir.resolve("uploads/ResearchPage/image/" + idSurvey);
    }

    /** Собирает список файлов (List<MultipartFile>) из MockMultipartFile. */
    private List<MultipartFile> files(MockMultipartFile... parts) {
        return List.of(parts);
    }

    @Nested
    class SaveResearchImages {

        /**
         * Happy path: файлы сохраняются на диск, метод возвращает публичные
         * пути в URL-формате (прямой слэш независимо от ОС), и на диске
         * по каждому пути реально лежит файл с тем же именем.
         */
        @Test
        void savesFiles_returnsPublicPaths_andFilesOnDisk() throws IOException {
            var idSurvey = UUID.randomUUID();
            var saved = service.saveResearchImages(idSurvey, files(
                    new MockMultipartFile("f", "photo.png", "image/png", "data1".getBytes()),
                    new MockMultipartFile("f", "doc.jpeg", "image/jpeg", "data2".getBytes())));

            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(p ->
                    assertThat(p).startsWith("/uploads/ResearchPage/image/" + idSurvey + "/"));

            // имена на диске совпадают с именами в возвращённых путях
            var namesOnDisk = listFileNames(imageDir(idSurvey));
            var namesFromPaths = saved.stream()
                    .map(p -> p.substring(p.lastIndexOf('/') + 1))
                    .collect(Collectors.toSet());
            assertThat(namesOnDisk).containsExactlyInAnyOrderElementsOf(namesFromPaths);
        }

        /**
         * Пустой файл (isEmpty) молча пропускается: возвращается пустой
         * список, директория при этом всё равно создана — контракт метода.
         */
        @Test
        void emptyFile_isSkipped() {
            var idSurvey = UUID.randomUUID();
            var empty = new MockMultipartFile("f", "empty.png", "image/png", new byte[0]);

            var savedPaths = service.saveResearchImages(idSurvey, files(empty));

            assertThat(savedPaths).isEmpty();
            assertThat(imageDir(idSurvey)).exists();
        }

        /**
         * Ошибка создания директории (например, нет прав на запись) —
         * метод оборачивает IOException в RuntimeException с внятным
         * сообщением и не доходит до сохранения файлов.
         */
        @Test
        void createDirectoryFails_throwsRuntimeException() {
            var idSurvey = UUID.randomUUID();
            try (var files = mockStatic(Files.class)) {
                files.when(() -> Files.createDirectories(any(Path.class)))
                        .thenThrow(new IOException("disk full"));

                assertThatThrownBy(() -> service.saveResearchImages(idSurvey,
                        List.of(new MockMultipartFile("f", "a.png", "image/png", "x".getBytes()))))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Не удалось создать директорию")
                        .hasCauseInstanceOf(IOException.class);
            }
        }

        /**
         * Ошибка сохранения конкретного файла (transferTo) — RuntimeException
         * с именем исходного файла в сообщении.
         */
        @Test
        void transferToFails_throwsRuntimeExceptionWithFileName() throws IOException {
            var idSurvey = UUID.randomUUID();
            var file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("broken.jpg");
            doThrow(new IOException("io error")).when(file).transferTo(any(Path.class));

            assertThatThrownBy(() -> service.saveResearchImages(idSurvey, List.of(file)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Ошибка при сохранении файла: broken.jpg")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    class DeleteSurveyImages {

        /** Директория с файлами удаляется целиком — файлы и каталог. */
        @Test
        void removesDirectoryWithAllFiles() throws IOException {
            var idSurvey = UUID.randomUUID();
            var dir = imageDir(idSurvey);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("one.png"), "a");
            Files.writeString(dir.resolve("two.png"), "b");

            service.deleteSurveyImages(idSurvey);

            assertThat(dir).doesNotExist();
        }

        /** Нет директории — метод не падает: ошибка ловится и логируется. */
        @Test
        void missingDirectory_doesNotThrow() {
            assertThatCode(() -> service.deleteSurveyImages(UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }
    }

    private List<String> listFileNames(Path dir) throws IOException {
        try (var walk = Files.list(dir)) {
            return walk.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        }
    }
}
