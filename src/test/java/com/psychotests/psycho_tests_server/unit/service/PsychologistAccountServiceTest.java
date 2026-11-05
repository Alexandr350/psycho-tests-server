package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.dto.request.authorization_dto.LoginPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.request.authorization_dto.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.exception.EmailAlreadyExistsException;
import com.psychotests.psycho_tests_server.exception.InvalidCredentialsException;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.repository.PsychologistAccountRepository;
import com.psychotests.psycho_tests_server.service.PsychologistAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Юнит-тесты {@link PsychologistAccountService}: логин и регистрация
 * психолога — без Spring-контекста, базы данных и реального BCrypt.
 *
 * <p>Внешние зависимости — моки Mockito: репозиторий аккаунтов и
 * {@link PasswordEncoder}. Сервис тестируется как чистый класс, поэтому
 * ошибки локализуются в его логике, а не в конфигурации или БД.</p>
 *
 * <h2>Как устроены заглушки</h2>
 * <ul>
 *   <li><b>Аккаунт — настоящая сущность</b>, а не мок: собирается в
 *       {@code @BeforeEach} конструктором с реальными полями (имя, фамилия,
 *       хэш-заглушка пароля, email). В отличие от stateful-моков это даёт
 *       проверку возвращаемого объекта через {@code isSameAs} — сервис
 *       обязан вернуть ровно тот объект, который дал репозиторий,
 *       без пересборки сущности.</li>
 *   <li><b>Криптография подменяется.</b> {@code matches()} и {@code encode()}
 *       стабятся явно; настоящие BCrypt-хэширование и сравнение на этом
 *       уровне не проверяются — они вынесены в интеграционные тесты
 *       логина и регистрации.</li>
 * </ul>
 *
 * <h2>Сквозная тема — защита от timing-атак</h2>
 * <p>Часть проверок фиксирует не внешний контракт, а порядок и полноту
 * операций, чтобы время ответа не выдавало состояние email:</p>
 * <ul>
 *   <li>при несуществующем email BCrypt-сравнение всё равно выполняется
 *       с заглушкой хэша ({@code authorization_unknownEmail_stillRunsBcrypt});</li>
 *   <li>при регистрации на занятый email {@code encode()} вызывается
 *       ДО отказа — обе ветки несут одинаковую стоимость хэширования
 *       ({@code register_emailTaken_throwsEmailExists_butRunsBcryptFirst}).</li>
 * </ul>
 * <p>Такие регрессии не меняют ответы API и не ловятся интеграционными
 * тестами — их ловит именно verify.</p>
 *
 * <h2>Что покрывают секции</h2>
 * <ul>
 *   <li><b>Логин</b> — happy path с проверкой идентичности объекта,
 *       неверный пароль и несуществующий email с одинаковым универсальным
 *       текстом ошибки (неотличимость отказов), нормализация email
 *       (trim + lower case) с verify на нормализованный запрос.</li>
 *   <li><b>Регистрация</b> — happy path с тремя контрактами (нормализованный
 *       email в сущности, хэш вместо сырого пароля, сгенерированный id),
 *       отказ при занятом email, перевод {@link DataIntegrityViolationException}
 *       в доменное {@link EmailAlreadyExistsException} при гонке
 *       check-then-save (констрейнт сработал между проверкой и save).</li>
 * </ul>
 *
 * <p>По умолчанию {@code MockitoExtension} работает в strict-режиме:
 * лишние или несовпавшие стабы роняют тест — в отличие от более раннего
 * {@code CPSKTestServiceTest}, здесь {@code Strictness.LENIENT} не нужен,
 * потому что каждый тест стабит только то, что реально использует.</p>
 */


@ExtendWith(MockitoExtension.class)
class PsychologistAccountServiceTest {

    @Mock
    private PsychologistAccountRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PsychologistAccountService service;

    private PsychologistAccount account;

    @BeforeEach
    void setUp() {
        account = new PsychologistAccount(
                UUID.randomUUID(), "Анна", "Иванова", "$2a$10$hashedvalue", "psy@mail.ru");
    }

    // --- Логин ---

    /**
     * Счастливый путь логина: валидный email и пароль приводят к возврату аккаунта.
     * Мок репозитория возвращает Optional с тестовым аккаунтом, мок энкодера —
     * совпадение пароля. Проверяется, что сервис вернёт ровно тот же объект,
     * что дал репозиторий (isSameAs, а не equals): без копирования и пересборки
     * сущности. Реальное сравнение BCrypt на этом уровне не проверяется —
     * оно покрыто интеграционным тестом логина.
     */
    @Test
    void authorization_withValidCredentials_returnsAccount() {
        var dto = new LoginPsychologicalAccountRequestDto("psy@mail.ru", "Password1!");
        when(repository.findByEmailPsychologist("psy@mail.ru")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password1!", "$2a$10$hashedvalue")).thenReturn(true);

        var result = service.authorizationAccount(dto);

        assertThat(result).isSameAs(account);
    }

    /**
     * Ветка «пользователь найден, пароль неверный»: сервис бросает
     * InvalidCredentialsException с универсальным текстом «Неверный email или пароль».
     * matches() замокан на false для любых аргументов — проверяется именно
     * реакция сервиса на отрицательный результат сравнения, а не криптография.
     * Фиксация точного текста защищает политику неотличимости отказов
     * (неверный пароль и несуществующий email выглядят для клиента одинаково).
     */
    @Test
    void authorization_withWrongPassword_throwsInvalidCredentials() {
        var dto = new LoginPsychologicalAccountRequestDto("psy@mail.ru", "WrongPass1!");
        when(repository.findByEmailPsychologist("psy@mail.ru")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.authorizationAccount(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Неверный email или пароль");
    }

    /**
     * Ветка «email не найден»: клиент получает то же исключение и тот же текст,
     * что и при неверном пароле (неотличимость отказов).
     * Главная проверка — verify: passwordEncoder.matches() обязан быть вызван
     * даже для несуществующего email (с заглушкой DUMMY_HASH). Это фиксирует
     * защиту от timing-атак: если сравнение BCrypt выполняется в обеих ветках,
     * время ответа не выдаёт, какие email зарегистрированы. Регрессия вида
     * «бросаем исключение до сравнения» не меняет внешний контракт,
     * но ломает выравнивание — и ловится именно этим verify.
     */
    @Test
    void authorization_unknownEmail_stillRunsBcrypt() {
        when(repository.findByEmailPsychologist(any())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(any(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.authorizationAccount(
                new LoginPsychologicalAccountRequestDto("ghost@mail.ru", "Password1!")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Неверный email или пароль");

        // Ключевая проверка: BCrypt прогонялся даже для несуществующего email
        verify(passwordEncoder).matches(eq("Password1!"), anyString());
    }

    /**
     * Нормализация email при логине: «  PSY@Mail.RU  » превращается в «psy@mail.ru».
     * Проверка двойная: логин успешно возвращает аккаунт, а verify фиксирует,
     * что репозиторий был спрошен именно нормализованным адресом (trim +
     * toLowerCase). Защищает от регрессии, когда пользователь вводит email
     * с пробелами или в другом регистре — вход всё равно должен работать.
     */
    @Test
    void authorization_emailNormalized_caseInsensitive() {
        var dto = new LoginPsychologicalAccountRequestDto("  PSY@Mail.RU ", "Password1!");
        when(repository.findByEmailPsychologist("psy@mail.ru")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        var result = service.authorizationAccount(dto);

        assertThat(result).isSameAs(account);
        verify(repository).findByEmailPsychologist("psy@mail.ru");
    }

    // --- Регистрация ---

    /**
     * Счастливый путь регистрации: свободный email приводит к сохранению аккаунта.
     * Три контракта: в сущность уходит нормализованный email (psy@mail.ru,
     * а не «PSY@Mail.RU  »), в поле пароля — результат encode(), а не сырой
     * пароль, и id сгенерирован сервисом (мок save возвращает переданный
     * объект, как это делает JPA). Проверка existsByEmail замокана на
     * нормализованный адрес — нормализация тестируется заодно с основным
     * сценарием. Реальное BCrypt-хэширование не проверяется — оно покрыто
     * интеграционным тестом регистрации.
     */
    @Test
    void register_emailFree_savesAccountWithNormalizedEmail() {
        var dto = new RegisteringPsychologicalAccountRequestDto(
                "Анна", "Иванова", "Strong1!", "PSY@Mail.RU ");
        when(repository.existsByEmailPsychologist("psy@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode("Strong1!")).thenReturn("$2a$10$hash");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createAccountPsychologist(dto);

        assertThat(result.getEmailPsychologist()).isEqualTo("psy@mail.ru");
        assertThat(result.getPasswordPsychologist()).isEqualTo("$2a$10$hash");
        assertThat(result.getIdPsychologistAccount()).isNotNull();
    }

    /**
     * Ветка «email занят»: сервис бросает EmailAlreadyExistsException с текстом,
     * который контроллер превращает в 409.
     * Две гарантии порядка операций:
     * 1) passwordEncoder.encode() вызван ДО отказа — обе ветки (занят и свободен)
     *    выполняют дорогой BCrypt, время ответа не выдаёт, какие email заняты;
     * 2) save() не вызывается ни разу — отказ не оставляет мусорных записей.
     * Регрессия «проверка занятости до хэширования» не меняет внешний контракт,
     * но ломает выравнивание времени — и ловится именно verify.
     */
    @Test
    void register_emailTaken_throwsEmailExists_butRunsBcryptFirst() {
        var dto = new RegisteringPsychologicalAccountRequestDto(
                "Анна", "Иванова", "Strong1!", "psy@mail.ru");
        when(repository.existsByEmailPsychologist("psy@mail.ru")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");

        assertThatThrownBy(() -> service.createAccountPsychologist(dto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Психолог с таким email уже зарегистрирован");

        // Параноидальная гарантия: хэширование выполнено даже в отказной ветке
        verify(passwordEncoder).encode("Strong1!");
        verify(repository, never()).save(any());
    }

    /**
     * Гонка check-then-save: existsByEmail вернул «свободен», но save падает
     * из-за unique-констрейнта uk_psychologist_email (email занял другой запрос
     * между проверкой и сохранением).
     * Контракт: DataIntegrityViolationException переводится в доменное
     * EmailAlreadyExistsException с тем же текстом, что и в простой ветке —
     * клиент получает неотличимый 409 при обоих отказах. Реальное существование
     * констрейнта проверяется интеграционным тестом с двумя запросами;
     * здесь фиксируется поведение сервиса при срабатывании констрейнта.
     */
    @Test
    void register_emailTakenBetweenCheckAndSave_throwsEmailExists() {
        var dto = new RegisteringPsychologicalAccountRequestDto(
                "Анна", "Иванова", "Strong1!", "psy@mail.ru");
        when(repository.existsByEmailPsychologist("psy@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(repository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_psychologist_email"));

        assertThatThrownBy(() -> service.createAccountPsychologist(dto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Психолог с таким email уже зарегистрирован");
    }
}
