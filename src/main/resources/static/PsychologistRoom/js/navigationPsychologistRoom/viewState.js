/* ============================================================
 * viewState.js — состояние навигации кабинета
 * ============================================================ */
export const navState = {
    current: 'home',
    lastResearch: null   // последнее исследование, открытое через «Результаты» на карточке
};

export function rememberResearchForReports(researchDto) {
    navState.lastResearch = researchDto;
}

export function getRememberedResearch() {
    return navState.lastResearch;
}
