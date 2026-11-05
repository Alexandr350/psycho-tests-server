package com.psychotests.psycho_tests_server.integration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.*;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected PsychologistAccountRepository psychologistAccountRepository;
    @Autowired protected ResearchRepository researchRepository;
    @Autowired protected SurveyRepository surveyRepository;
    @Autowired protected CharacteristicsRepository characteristicsRepository;


    protected PsychologistAccount psychologistAccount;

    protected static final String RESEARCH_PART1_JSON = """
            {"nameResearch":"Тест","descriptionResearch":"Описание",
             "customerResearch":"кафедра","typeResearch":"CPSK"}""";

    @BeforeEach
    protected void setUpPsychologist() {
        psychologistAccount = psychologistAccountRepository.save(
                new PsychologistAccount(UUID.randomUUID(), "Тестовый", "Психолог",
                        "test-password", "test@example.com"));
    }

    protected RequestPostProcessor psychologist() {
        PsychologistUserDetails details = new PsychologistUserDetails(psychologistAccount);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        return authentication(auth);
    }
    protected UUID extractId(String json, String field) {
        JsonNode node = objectMapper.readTree(json);
        JsonNode id = node.get(field);
        if (id == null || id.isNull()) {
            throw new AssertionError("Поле " + field + " отсутствует в ответе: " + json);
        }
        return UUID.fromString(id.asString());
    }
    protected MockMultipartFile jpg(String filename) {
        return new MockMultipartFile("files", filename, MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x11});
    }


    /** Создаёт исследование от лица психолога, возвращает idResearch. */
    protected UUID createResearch() throws Exception {
        String json = mockMvc.perform(
                        post("/api/researchs/research").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"nameResearch":"Тест резюме","descriptionResearch":"Описание",
                         "customerResearch":"кафедра","typeResearch":"CPSK"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return extractId(json, "idResearch");
    }

    /** Создаёт опрос для исследования, возвращает idSurvey. */
    protected UUID createSurvey(UUID idResearch) throws Exception {
        String json = mockMvc.perform(
                        post("/api/researchs/survey").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"idResearch":"%s","titleSurvey":"Опрос","descriptionSurvey":"desc",
                         "rulesSurvey":"rules"}""".formatted(idResearch)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return extractId(json, "idSurvey");
    }




}
