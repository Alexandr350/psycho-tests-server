import { setContext } from "./researchPage/setContextResearchPage.js";
import { setupModal } from './createResearchModularWindows/setupModal.js';
import { stepOne } from './createResearchModularWindows/stepOne.js';
import { stepTwo } from './createResearchModularWindows/stepTwo.js';
import { stepThree } from './createResearchModularWindows/stepThree.js';
import { stepFour } from './createResearchModularWindows/stepFour.js';
import { stepFive } from './createResearchModularWindows/stepFive.js';
import { validationInputData } from "../../utility/validationInputData.js";
import { normalizationStrings } from "../../utility/normalizationStrings.js";
import { getData } from "../../utility/getData.js";
import { setProgressCounter } from './researchPage/cardResearchMake.js';
import { initResearchActions } from './researchPage/initResearchActions.js';
import { registerViewRenderer, initNavigation } from './navigationPsychologistRoom/navigation.js';
import { renderHomeView } from './homePage/homeResearchMake.js';

import { renderReportsEmptyState, renderReportsView } from './reportsPage/reportsResearchMake.js';
import { getRememberedResearch, rememberResearchForReports } from './navigationPsychologistRoom/viewState.js';



document.addEventListener('DOMContentLoaded', async () => {
    sessionStorage.setItem('lastCabinetPage', window.location.href);

    // --- Шапка и список карточек ---
    const elementsHeader = {
        elNameAccount: document.getElementById('name-user-account'),
        elEmailAccount: document.getElementById('email-user-account')
    };
    const headData = localStorage.getItem('currentPsychlogist');
    const cardsContainer = document.querySelector('.container-research-card');

    const context = setContext(headData, elementsHeader);
    context.setDataHeader();
    await context.setResearchList(cardsContainer);

    // --- Модалка создания/редактирования ---
    const modalWindow = document.getElementById('createResearchModal');
    const openBtnCreateResearchModal = document.getElementById('createResearch');
    const closeBtn = document.querySelector('.close-modal');
    const backBtn = document.getElementById('backBtn');
    const nextBtn = document.getElementById('nextBtn');
    const stepTitle = document.getElementById('stepTitle');
    const stepsArrayWindows = Array.from(document.querySelectorAll('.step-modal'));

    if (!modalWindow || !openBtnCreateResearchModal) {
        return;
    }

    const validation = validationInputData();
    const normalization = normalizationStrings();

    const stepActions = [
        stepOne(validation, normalization),
        stepTwo(validation, normalization),
        stepThree(validation, normalization),
        stepFour(validation, normalization),
        stepFive(validation, normalization)
    ];

    const stepTitles = [
        "Шаг 1: Информация об исследовании",
        "Шаг 2: Информация об опросе",
        "Шаг 3: Добавьте характеристики",
        "Шаг 4: Добавьте исследуемые объекты",
        "Шаг 5: Добавьте свои объекты"
    ];

    const stepsElements = {
        MODAL_WINDOW: modalWindow,
        BTN_CREATE_RESEARCH: openBtnCreateResearchModal,
        BTN_CLOSE: closeBtn,
        BTN_BACK: backBtn,
        BTN_NEXT: nextBtn,
        TITLE: stepTitle,
        ARRAY_WINDOWS: stepsArrayWindows
    };

    const stepsEndpoints = [
        '/api/researchs/research',
        '/api/researchs/survey',
        '/api/researchs/characteristics',
        '/api/researchs/items',
        '/api/researchs/items-sub',
    ];

    const modalController = setupModal(
        stepsElements, stepTitles, stepsEndpoints, stepActions,
        () => context.setResearchList(cardsContainer)
    );

    // --- Действия карточек: один диспетчер, редактирование открывает модалку ---
    initResearchActions(cardsContainer, {
        onEditReady: (data) => modalController.openForEdit(data),
    });

    // --- Регистрация вью: все рендеры — ДО запуска навигации ---
    registerViewRenderer('researchs', async (mainContainer) => {
        const container = mainContainer.querySelector('.container-research-card');
        if (!container) return;
        await context.setResearchList(container);
        initResearchActions(container, {
            onEditReady: (data) => modalController.openForEdit(data),
        });
    });

    registerViewRenderer('home', (mainContainer) => renderHomeView(mainContainer));

    registerViewRenderer('reports', (mainContainer) => {
        // Есть запомненное исследование (пришли с карточки «Результаты»
        // или выбрали из списка ниже) — сразу строим отчёт...
        const remembered = getRememberedResearch();
        if (remembered) {
            renderReportsView(mainContainer, remembered);
            return;
        }
        // ...иначе — список исследований с кнопкой «Просмотреть отчёт».
        renderReportsEmptyState(mainContainer, async (id) => {
            const result = await getData(`/api/researchs/research/${id}`);
            if (!result.success) {
                alert(result.message || 'Не удалось загрузить данные исследования.');
                return;
            }
            rememberResearchForReports(result.data);
            renderReportsView(mainContainer, result.data);
        });
    });

    initNavigation();


    // --- Счётчик завершившихся: polling ---
    async function refreshCounters() {
        try {
            const result = await getData('/api/researchs/progress');
            if (!result.success || !Array.isArray(result.data)) return;

            result.data.forEach(p => {
                // 1) кеш — чтобы будущие рендеры карточки взяли актуальное число
                setProgressCounter(p.idResearch, p.completedSessions);

                // 2) живая DOM-правка — иначе обновление видно только после перерисовки
                const card = document.querySelector(
                    `.research-card[data-id="${CSS.escape(p.idResearch)}"]`);
                const valueEl = card?.querySelector('.meta-row .value');
                if (valueEl && valueEl.textContent !== String(p.completedSessions)) {
                    valueEl.textContent = p.completedSessions;
                }
            });
        } catch (err) {
            console.error('refreshCounters: не удалось получить прогресс', err);
        }
    }

    refreshCounters();
    setInterval(refreshCounters, 5000);

});
