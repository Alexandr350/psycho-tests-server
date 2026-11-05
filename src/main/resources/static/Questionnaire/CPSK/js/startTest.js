import { sendData } from '../../../utility/sendData.js';
import { getData } from '../../../utility/getData.js';
import { escapeHtml } from '../../../utility/escapeHtml.js';

// Глобальное состояние теста
let globalSurveyFlowData = null;
let currentIdSession = null;
let currentIdSurvey = null;
let currentQuestionIndex = 0;
let isTestRunning = false; // Блокирует колесо, пока пользователь на экране анкеты или правил
let isAnswering = false;   // Защита от дабл-клика по сектору между ответом и перерендером
let publicSurveyInfo = null;
let selectedSectorColor = '';
let selectedSectorNumber = null; // сектор, выбранный на этапе ассоциаций

let ASSOCIATIONS_MAX_PER_SECTOR = 5; // синхронизировано с MAX_ASSOCIATIONS_PER_SECTOR на бэке
let associationsBySector = {};       // { 1: [...], 2: [...], ... } — счётчик по секторам
let associationsStageDone = false;   // этап завершён кнопкой «Завершить»

// Массивы для сбора ответов согласно SurveyFlowSubmitRequestDto
const characteristicAnswers = [];
const imageAnswers = [];
const imageSubAnswers = [];
const associationsAnswers = [];

document.addEventListener("DOMContentLoaded", async () => {
    const elStartTest = document.getElementById('image-container');
    if (!elStartTest) return;

    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (!token) {
        elStartTest.innerHTML = `
            <div class="error-msg">
                <p><strong>Ошибка:</strong> Ссылка недействительна: токен не указан.</p>
            </div>`;
        return;
    }

    // Резолвим токен: PublicSurveyAccessResponseDto — idSurvey + тексты опроса
    const resolved = await getData(`/api/public/research/${token}`);
    if (!resolved.success || !resolved.data || !resolved.data.idSurvey) {
        elStartTest.innerHTML = `
            <div class="error-msg">
                <p><strong>Опрос сейчас недоступен.</strong> Возможно, он приостановлен или завершён.</p>
            </div>`;
        return;
    }
    publicSurveyInfo = resolved.data;
    renderForm(elStartTest, resolved.data.idSurvey);
    setupWheelSectors();
});

// Функция рендеринга начальной анкеты
function renderForm(container, idSurvey) {
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

        // min/max в HTML — только подсказка браузеру, валидируем сами
        const age = parseInt(ageInput, 10);
        if (!ageInput || !genderSelect || Number.isNaN(age) || age < 1 || age > 120) {
            alert('Пожалуйста, заполните все поля: возраст от 1 до 120, пол выбран.');
            return;
        }

        const btn = document.getElementById('btn-start-test');
        btn.disabled = true;
        btn.innerText = 'Загрузка теста...';

        // 1) GET создаёт сессию и отдаёт её id (SurveyFlowStartResponseDto)
        const flowStart = await getData(`/api/CPSKTest/flow/${idSurvey}`);
        if (!flowStart.success || !flowStart.data || !flowStart.data.idSession) {
            btn.disabled = false;
            btn.innerText = 'Далее';
            alert(`Ошибка при загрузке теста: ${flowStart.message || flowStart.errors}`);
            return;
        }
        currentIdSession = flowStart.data.idSession;
        currentIdSurvey = idSurvey;

        // 2) POST анкеты — SurveyFlowQuestionnaireRequestDto
        const respondentPayload = {
            idSession: currentIdSession,
            age: age,
            gender: genderSelect
        };
        const result = await sendData(respondentPayload, `/api/CPSKTest/flow/${idSurvey}`);

        btn.disabled = false;
        btn.innerText = 'Далее';

        if (result.success && result.data) {
            globalSurveyFlowData = result.data;
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

    // Тексты опроса приходят из публичного резолва (publicSurveyInfo)
    const title = escapeHtml(publicSurveyInfo.titleSurvey || 'Тестирование CPSK');
    const description = publicSurveyInfo.descriptionSurvey
        ? `<p class="test-description">${escapeHtml(publicSurveyInfo.descriptionSurvey)}</p>` : '';
    const rules = escapeHtml(publicSurveyInfo.rulesSurvey || 'Пожалуйста, следуйте инструкциям на экране.');

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
        isTestRunning = true;
        currentQuestionIndex = 0;
        renderNextQuestion(container);
    });
}

// Функция отображения текущего вопроса (последовательные этапы)
function renderNextQuestion(container) {
    // SurveyFlowResponseDto: characteristics / items / itemsSub / associations
    const questions = globalSurveyFlowData.characteristics || [];
    const images = globalSurveyFlowData.items || [];
    const subImages = globalSurveyFlowData.itemsSub || [];

    // ЭТАП 1: Текстовые вопросы (Характеристики)
    if (currentQuestionIndex < questions.length) {
        const currentQuestion = questions[currentQuestionIndex];
        container.innerHTML = `
            <div class="test-question-text">${escapeHtml(currentQuestion.question)}</div>
        `;
        return;
    }

    // ЭТАП 2: Основные изображения (Items)
    const imgIndex = currentQuestionIndex - questions.length;
    if (imgIndex < images.length) {
        const currentImg = images[imgIndex];
        container.innerHTML = `
            <div class="image-question-wrapper">
                <img src="${escapeHtml(currentImg.imageUrl)}" alt="">
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
                <img src="${escapeHtml(currentSubImg.imageUrl)}" alt="">
            </div>
        `;
        return;
    }

    // ЭТАП 4: Ассоциации
    if (!associationsStageDone) {
        renderAssociationQuestion(container);
        return;
    }


    // ВСЕ ЭТАПЫ ПРОЙДЕНЫ
    isTestRunning = false;
    submitFinalResults(container);
}


// Этап ассоциаций: участник выбирает сектор кликом, затем пишет слова.
// Ограничение — не более 5 ассоциаций на сектор (MAX_ASSOCIATIONS_PER_SECTOR на бэке).
function renderAssociationQuestion(container) {
    container.innerHTML = `
        <div class="test-question-text">Выберите сектор и напишите до ${ASSOCIATIONS_MAX_PER_SECTOR} ассоциаций к нему</div>
        <div id="association-sector-info" class="association-sector-info">Сектор не выбран</div>
        <input type="text" id="association-input" class="association-input"
               maxlength="200" autocomplete="off"
               placeholder="Сначала выберите сектор" disabled>
        <div id="association-counter" class="test-instruction-hint"></div>
        <button id="btn-finish-associations" class="btn-primary">Завершить тест</button>
    `;

    document.getElementById('btn-finish-associations').addEventListener('click', () => {
        if (Object.values(associationsBySector).flat().length === 0) {
            alert('Добавьте хотя бы одну ассоциацию, прежде чем завершить тест.');
            return;
        }
        associationsStageDone = true;
        selectedSectorNumber = null;
        clearSectorHighlight();
        renderNextQuestion(container);
    });

    updateAssociationPanel();
}

// Добавляет непустой текст из поля в текущий сектор
function commitAssociation() {
    if (selectedSectorNumber === null) return;
    const input = document.getElementById('association-input');
    const text = input.value.trim();
    if (!text) return;

    const bucket = (associationsBySector[selectedSectorNumber] ??= []);
    if (bucket.length >= ASSOCIATIONS_MAX_PER_SECTOR) {
        alert(`Для этого сектора уже добавлено ${ASSOCIATIONS_MAX_PER_SECTOR} ассоциаций — выберите другой.`);
        return;
    }
    bucket.push(text);
    associationsAnswers.push({ numberSector: selectedSectorNumber, text });
    input.value = '';
    updateAssociationPanel();
}

function clearSectorHighlight() {
    document.querySelectorAll('.sector.sector-selected')
        .forEach(s => s.classList.remove('sector-selected'));
}

// Функция настройки кликов по секторам колеса
function setupWheelSectors() {
    const sectors = document.querySelectorAll('.circle-wrapper svg .sector');

    // Enter на этапе ассоциаций добавляет слово без смены сектора
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && selectedSectorNumber !== null) {
            const input = document.getElementById('association-input');
            // Enter работает только когда поле видно и активно
            if (input && !input.disabled) {
                e.preventDefault();
                commitAssociation();
            }
        }
    });

    sectors.forEach(sector => {
        sector.addEventListener('click', () => {
            if (!isTestRunning || !globalSurveyFlowData || isAnswering) {
                return;
            }

            const questions = globalSurveyFlowData.characteristics || [];
            const images = globalSurveyFlowData.items || [];
            const subImages = globalSurveyFlowData.itemsSub || [];

            const sectorNumber = parseInt(sector.getAttribute('data-info'), 10);
            if (Number.isNaN(sectorNumber)) {
                return; // у сектора нет data-info — такой клик не регистрируем
            }

            const inAssociationsStage =
                currentQuestionIndex >= questions.length + images.length + subImages.length;

            if (!inAssociationsStage) {
                // Имена полей — строго по DTO ответов:
                // CharacteristicAnswerDto: idCharacteristic, numberSector
                // ImageAnswerDto: idImage, numberSector (и для суб-изображений)
                if (currentQuestionIndex < questions.length) {
                    characteristicAnswers.push({
                        idCharacteristic: questions[currentQuestionIndex].idCharacteristic,
                        numberSector: sectorNumber
                    });
                } else if ((currentQuestionIndex - questions.length) < images.length) {
                    imageAnswers.push({
                        idImage: images[currentQuestionIndex - questions.length].idItem,
                        numberSector: sectorNumber
                    });
                } else {
                    imageSubAnswers.push({
                        idImage: subImages[currentQuestionIndex - questions.length - images.length].idItem,
                        numberSector: sectorNumber
                    });
                }

                currentQuestionIndex++;

                // Флажок на время перерендера: быстрый дабл-клик не запишет
                // второй ответ за ещё не показанный вопрос
                isAnswering = true;
                const container = document.getElementById('image-container');
                renderNextQuestion(container);
                isAnswering = false;
                return;
            }

            // Этап ассоциаций: клик по сектору выбирает его.
            // Если в поле остался ненаписанный текст — сохраняем его в прежний сектор
            if (selectedSectorNumber !== null) {
                commitAssociation();
            }
            selectedSectorNumber = sectorNumber;
            selectedSectorColor = sector.style.fill || getComputedStyle(sector).fill || '#e0e0e0';
            selectedSectorNumber = sectorNumber;
            clearSectorHighlight();
            sector.classList.add('sector-selected');
            updateAssociationPanel();
            clearSectorHighlight();
            sector.classList.add('sector-selected');
            updateAssociationPanel();

            const input = document.getElementById('association-input');
            input.disabled = false;
            input.focus();
        });
    });
}


// Панель справа: цвет, номер выбранного сектора и счётчик слов
function updateAssociationPanel() {
    const info = document.getElementById('association-sector-info');
    const input = document.getElementById('association-input');
    const counter = document.getElementById('association-counter');
    if (!info || !input || !counter) return;

    if (selectedSectorNumber === null) {
        info.innerHTML = 'Сектор не выбран';
        input.disabled = true;
        input.placeholder = 'Сначала выберите сектор';
        counter.textContent = '';
    } else {
        const used = (associationsBySector[selectedSectorNumber] || []).length;
        info.innerHTML = `
            <span class="sector-color-chip"
                  style="background-color: ${selectedSectorColor}"></span>
            Сектор ${selectedSectorNumber}
        `;
        input.disabled = false;
        input.placeholder = 'Ваша ассоциация';
        counter.textContent = `Ассоциации: ${used} из ${ASSOCIATIONS_MAX_PER_SECTOR}`;
    }
}


// Функция отправки финальных результатов теста на сервер
async function submitFinalResults(container) {
    container.innerHTML = '<div class="loader">Сохранение результатов...</div>';

    // SurveyFlowSubmitRequestDto: idSession + четыре списка ответов
    const finalPayload = {
        idSession: currentIdSession,
        characteristicAnswer: characteristicAnswers,
        imageAnswer: imageAnswers,
        imageSubAnswer: imageSubAnswers,
        associationsAnswer: associationsAnswers
    };

    const result = await sendData(finalPayload, `/api/CPSKTest/submit/${currentIdSurvey}`);

    if (result.success) {
        isTestRunning = false;
        container.innerHTML = `
            <div class="test-success-screen">
                <div class="success-icon">🎉</div>
                <h3>Тест завершен</h3>
                <p>Спасибо за участие!</p>
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
