export { rememberResearchForReports } from '../navigationPsychologistRoom/viewState.js';


/* ============================================================
 * initReportsResearch.js
 * Навигацией теперь рулит navigationPsychologistRoom/navigation.js,
 * поэтому собственных обработчиков на .nav-btn здесь больше нет.
 * ============================================================ */
let lastResearch = null;

export function rememberResearchForReports(researchDto) {
    lastResearch = researchDto;
}

export function initReportsResearch(mainContainer) {
    const navBtns = document.querySelectorAll('.sidebar-nav .nav-btn');
    const target = navBtns[2]; // кнопка «Результаты тестов»
    if (!target) {
        console.warn('initReportsResearch: кнопка «Результаты тестов» не найдена в сайдбаре');
        return;
    }

    target.addEventListener('click', () => {
        // подсветка активной кнопки сайдбара
        navBtns.forEach(b => b.classList.remove('active'));
        target.classList.add('active');

        if (lastResearch) {
            renderReportsView(mainContainer, lastResearch);
        } else {
            renderReportsEmptyState(mainContainer);
        }
    });
}
