package com.psychotests.psycho_tests_server.dto.request.authorization_dto;

import jakarta.validation.constraints.*;

public record RegisteringPsychologicalAccountRequestDto(

        @NotBlank(message = "Имя обязательно")
        @Size(max = 20, message = "Имя не должно превышать 20 символов")
        String namePsychologist,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        String lastnamePsychologist,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 50, message = "Пароль должен быть от 8 до 50 символов")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@$!%*?&-])[A-Za-z0-9@$!%*?&-]{8,50}$",
                message = "Пароль должен содержать буквы, цифры и спецсимволы (@$!%*?&-), от 8 до 50 символов")
        String passwordPsychologist,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат Email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String emailPsychologist
) { }
