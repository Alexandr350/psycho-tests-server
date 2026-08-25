package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.*;

public record RegisteringPsychologicalAccountRequestDto(

        @NotBlank(message = "Имя обязательно")
        @Max(20)
        String namePsychologist,

        @NotBlank(message = "Фамилия обязательна")
        @Max(50)
        String lastnamePsychologist,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 50, message = "Пароль должен быть от 8 до 50 символов")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@$!%*?&])[A-Za-z0-9@$!%*?&]{8,}$",
                message = "Пароль должен содержать буквы, цифры и спецсимволы (@$!%*?&), минимум 8 символов")
        String passwordPsychologist,

        @NotBlank(message = "Email обязателен")
        @Email
        String emailPsychologist
) { }
