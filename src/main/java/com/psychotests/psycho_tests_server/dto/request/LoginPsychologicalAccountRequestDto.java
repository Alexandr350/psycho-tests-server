package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginPsychologicalAccountRequestDto(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат Email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,

        @NotBlank(message = "Что бы авторизироваться введите пароль")
        @Size(min = 8, max = 50, message = "Неверный пароль")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@$!%*?&\\\\-])[A-Za-z0-9@$!%*?&\\\\-]{8,50}$",
                message = "Неверный пароль")
        String password
) { }
