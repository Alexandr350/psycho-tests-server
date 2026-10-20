import { sendData } from '../../../utility/sendData.js'; 
import { escapeHtml } from '../../../utility/getData.js';

// Глобальное состояние теста
let globalSurveyFlowData = null;
let currentQuestionIndex = 0;
let isTestRunning = false; // Блокирует колесо, пока пользователь на экране анкеты или правил

// Массивы для сбора ответов согласно CPSKSurveyFlowRequestDto
const characteristicAnswers = [];
const imageAnswers = [];
const imageSubAnswers = [];
const associationsAnswers = [];

document.addEventListener("DOMContentLoaded", () => {
    const elStartTest = document.getElementById('image-container');
    if (!elStartTest) return;

    // 1. Извлекаем surveyId из URL страницы
    const urlParams = new URLSearchParams(window.location.search);
    const surveyId = urlParams.get('surveyId');

    if (!surveyId) {
        elStartTest.innerHTML = `
            <div class="error-msg">
                <p><strong>Ошибка:</strong> ID опроса (surveyId) не указан в URL страницы.</p>
            </div>`;
        return;
    }

    // 2. Инициализируем и рендерим анкету в правый блок
    renderForm(elStartTest, surveyId);

    // 3. Настраиваем интерактивное колесо (SVG сектора)
    setupWheelSectors();
});

// Функция рендеринга начальной анкеты
function renderForm(container, surveyId) {
    container.innerHTML = '';
    const formWrapper = document.createElement('div');
    formWrapper.className = 'start-form-wrapper';
    formWrapper.innerHTML = `
        <h3>Анкета участника</h3>
        
        <label class="form-field">
            <span>Возраст:</span>
            <input type="number" id="guest-age" min="1" max="120" required placeholder="Введите возраст">
        </label>

        <label class="form-field">
            <span>Пол:</span>
            <select id="guest-gender" required>
                <option value="" disabled selected>Выберите пол</option>
                <option value="М">Мужской</option>
                <option value="Ж">Женский</option>
            </select>
        </label>

        <button id="btn-start-test">Далее</button>
    `;
    container.appendChild(formWrapper);

    document.getElementById('btn-start-test').addEventListener('click', async () => {
        const ageInput = document.getElementById('guest-age').value;
        const genderSelect = document.getElementById('guest-gender').value;

        if (!ageInput || !genderSelect) {
            alert('Пожалуйста, заполните все поля перед началом теста!');
            return;
        }

        const btn = document.getElementById('btn-start-test');
        btn.disabled = true;
        btn.innerText = 'Загрузка теста...';

        // Отправка реальной анкеты на бэкенд
        const respondentPayload = {
            age: parseInt(ageInput, 10),
            gender: genderSelect
        };

        const endpoint = `/api/CPSKTest/flow/${surveyId}`;
        const result = await sendData(respondentPayload, endpoint);

        btn.disabled = false;
        btn.innerText = 'Далее';

        if (result.success && result.data) {
            globalSurveyFlowData = result.data;
            // Переходим к экрану правил
            renderRulesScreen(container);
        } else {
            alert(`Ошибка при запуске теста: ${result.message || result.errors}`);
        }
    });
}

// Функция рендеринга экрана правил теста
function renderRulesScreen(container) {
    container.innerHTML = '';

    const testContainer = document.createElement('div');
    testContainer.className = 'test-container';

    const title = escapeHtml(globalSurveyFlowData.title || 'Тестирование CPSK');
    const description = globalSurveyFlowData.description ? `<p class="test-description">${escapeHtml(globalSurveyFlowData.description)}</p>` : '';
    const rules = escapeHtml(globalSurveyFlowData.rules || 'Пожалуйста, следуйте инструкциям на экране.');

    testContainer.innerHTML = `
        <h2 class="test-title">${title}</h2>
        ${description}
        <div class="test-rules">
            <h4>Правила прохождения:</h4>
            <p>${rules}</p>
        </div>
        <button id="btn-begin-questions" class="btn-primary">Начать тест</button>
    `;

    container.appendChild(testContainer);

    testContainer.querySelector('#btn-begin-questions').addEventListener('click', () => {
        isTestRunning = true; // Разрешаем клики по колесу
        currentQuestionIndex = 0;
        renderNextQuestion(container);
    });
}

// Функция отображения текущего текстового вопроса
// Модифицированная функция последовательного прохождения теста
function renderNextQuestion(container) {
    const questions = globalSurveyFlowData.textQuestions || [];
    const images = globalSurveyFlowData.images || [];
    const subImages = globalSurveyFlowData.subImages || [];

    // ЭТАП 1: Текстовые вопросы (Характеристики)
    if (currentQuestionIndex < questions.length) {
        const currentQuestion = questions[currentQuestionIndex];
        container.innerHTML = `
            <div class="test-question-text">${escapeHtml(currentQuestion.text)}</div>
        `;
        return;
    }

    // ЭТАП 2: Основные изображения (Items)
    // Вычисляем индекс картинки относительно завершения первого этапа
    const imgIndex = currentQuestionIndex - questions.length;
    if (imgIndex < images.length) {
        const currentImg = images[imgIndex];
        container.innerHTML = `
    
            <div class="image-question-wrapper">
                <img src="${currentImg.imageUrl}">
            </div>
        `;
        return;
    }

    // ЭТАП 3: Дополнительные изображения (SubItems)
    const subImgIndex = currentQuestionIndex - questions.length - images.length;
    if (subImgIndex < subImages.length) {
        const currentSubImg = subImages[subImgIndex];
        container.innerHTML = `
            
            <div class="image-question-wrapper">
                <img src="${currentSubImg.imageUrl}">
            </div>

        `;
        return;
    }

    // ВСЕ ЭТАПЫ ПРОЙДЕНЫ
    isTestRunning = false;
    submitFinalResults(container);
}


// Функция настройки кликов по секторам колеса
function setupWheelSectors() {
    const sectors = document.querySelectorAll('.circle-wrapper svg .sector');
    
    sectors.forEach(sector => {
        sector.addEventListener('click', () => {
            if (!isTestRunning || !globalSurveyFlowData) {
                return;
            }

            const sectorNumber = parseInt(sector.getAttribute('data-info'), 10);
            
            const questions = globalSurveyFlowData.textQuestions || [];
            const images = globalSurveyFlowData.images || [];
            const subImages = globalSurveyFlowData.subImages || [];

            // Логика записи ответа в зависимости от текущего индекса
            if (currentQuestionIndex < questions.length) {
                // Запись характеристики
                characteristicAnswers.push({
                    characteristicId: questions[currentQuestionIndex].id,
                    numberSector: sectorNumber
                });
            } else if ((currentQuestionIndex - questions.length) < images.length) {
                // Запись основного изображения
                const imgIndex = currentQuestionIndex - questions.length;
                imageAnswers.push({
                    imageId: images[imgIndex].id,
                    numberSector: sectorNumber
                });
            } else {
                // Запись суб-изображения
                const subImgIndex = currentQuestionIndex - questions.length - images.length;
                imageSubAnswers.push({
                    imageId: subImages[subImgIndex].id,
                    numberSector: sectorNumber
                });
            }

            // Переходим вперед
            currentQuestionIndex++;
            const container = document.getElementById('image-container');
            renderNextQuestion(container);
        });
    });
}


// Функция отправки финальных результатов теста на сервер
async function submitFinalResults(container) {
    container.innerHTML = '<div class="loader">Сохранение результатов...</div>';

    // Формируем CPSKSurveyFlowRequestDto
    const finalPayload = {
        sessionId: globalSurveyFlowData.sessionId,
        characteristicAnswer: characteristicAnswers,
        imageAnswer: imageAnswers,       // пока пустые, если в тесте только текст
        imageSubAnswer: imageSubAnswers, // пока пустые
        associationsAnswer: associationsAnswers // пока пустые
    };

    const endpoint = `/api/CPSKTest/submit/${globalSurveyFlowData.surveyId}`;
    const result = await sendData(finalPayload, endpoint);

    if (result.success) {
        isTestRunning = false;
        container.innerHTML = `
            <div class="test-success-screen">
                <div class="success-icon">🎉</div>
                <h3>Тест завершен</h3>
                <p>Спасибо за участие!!!</p>
            </div>
        `;
    } else {
        container.innerHTML = `
            <div class="test-error-screen">
                <h3>Ошибка сохранения</h3>
                <p>${escapeHtml(result.message || 'Не удалось отправить результаты.')}</p>
                <button id="btn-retry-submit" class="btn-primary">Повторить отправку</button>
            </div>
        `;
        document.getElementById('btn-retry-submit').addEventListener('click', () => {
            submitFinalResults(container);
        });
    }
}
