package com.psychotests.psycho_tests_server.dto.request.authorization_dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginPsychologicalAccountRequestDto(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат Email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,

        @NotBlank(message = "Что бы авторизироваться введите пароль")
        String password
) { }
