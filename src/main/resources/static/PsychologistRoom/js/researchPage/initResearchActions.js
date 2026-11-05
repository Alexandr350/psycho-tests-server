
import { BTN } from "./researchCardConstants.js";
import { actionBtnList } from "./actionBtnCards.js";
import { updateResearchCard } from "./cardResearchMake.js";
import { rememberResearchForReports } from "../navigationPsychologistRoom/viewState.js";
import { goTo } from '../navigationPsychologistRoom/navigation.js';

export function initResearchActions(container, { onEditReady } = {}) {
    const actions = actionBtnList({
        onEditReady,
        // переход к результатам: запоминаем DTO, навигация рендерит отчёт в main
        onResultsReady: (researchDto) => {
            rememberResearchForReports(researchDto);
            goTo('reports');
        },
    });


    container.addEventListener('click', async (event) => {
        const btn = event.target.closest('.research-card .actions-container button');
        if (!btn) return;

        const card = btn.closest('.research-card');
        const researchId = card?.dataset.id;
        if (!researchId) {
            console.warn('initResearchActions: у кнопки нет карточки с data-id');
            return;
        }

        const handlers = {
            [BTN.START]:   actions.startResearch,
            [BTN.RESTART]: actions.restartResearch,
            [BTN.STOP]:    actions.stopResearch,
            [BTN.PAUSE]:   actions.pauseResearch,
            [BTN.NEXT]:    actions.resumeResearch,
            [BTN.ARCHIVE]: actions.archiveResearch,
        };

        const handler = Object.keys(handlers).find(c => btn.classList.contains(c));

        if (handler) {
            btn.disabled = true;
            const freshData = await handlers[handler](researchId);
            btn.disabled = false;

            if (freshData) updateResearchCard(researchId, freshData);
            return;
        }

        // «Результаты» — рендер отчёта в main без смены статуса и перерисовки карточки
        if (btn.classList.contains(BTN.RESULT)) {
            btn.disabled = true;               // запрос DTO — блокируем на время загрузки
            await actions.viewResults(researchId);
            btn.disabled = false;
            return;
        }

        if (btn.classList.contains(BTN.REMOVE)) {
            const removed = await actions.removeResearch(researchId);
            if (removed) card.remove();
            return;
        }

        if (btn.classList.contains(BTN.EDIT)) {
            actions.editResearch(researchId);
            return;
        }

        console.warn(`initResearchActions: неизвестная кнопка "${btn.className}"`);
    });
}
