/* ============================================================
 * navigation.js — переходы кабинета: сайдбар → вью в <main>
 * Единственный модуль, который вешает обработчики на .nav-btn.
 * ============================================================ */
import { navState } from './viewState.js';
import { renderReportsView, renderReportsEmptyState } from '../reportsPage/reportsResearchMake.js';

const main = document.querySelector('main');
let homeSnapshot = null;
const VIEW_ORDER = ['home', 'researchs', 'reports', 'settings'];

/* Рендеры внешних вью регистрируются снаружи (mainResearchAction.js) —
   так navigation не зависит от путей к модулям карточек. */
const renderers = { home: null, researchs: null };

export function registerViewRenderer(view, fn) {
    renderers[view] = fn;
}

/** Программный переход — например, из кнопки «Результаты» на карточке. */
export function goTo(view) {
    activate(view);
}

function activate(name) {
    if (name === navState.current || !main) return;

    // подсветка активной кнопки сайдбара
    const btns = document.querySelectorAll('.sidebar-nav .nav-btn');
    btns.forEach(b => b.classList.remove('active'));
    const btn = [...btns].find(b => b.dataset.view === name)
        ?? btns[VIEW_ORDER.indexOf(name)];
    if (btn) btn.classList.add('active');

    navState.current = name;

    switch (name) {
        case 'home':
            if (renderers.home) renderers.home(main);
            break;
        case 'researchs':
            // восстанавливаем исходную разметку (с кнопкой «Создать исследование»),
            // а список карточек пересобирает зарегистрированный рендер — со свежими
            // данными и живыми слушателями
            if (homeSnapshot !== null) main.innerHTML = homeSnapshot;
            if (renderers.researchs) renderers.researchs(main);
            break;

        case 'reports':
            navState.lastResearch
                ? renderReportsView(main, navState.lastResearch)
                : renderReportsEmptyState(main);
            break;

        case 'settings':
            main.innerHTML = `
            <div class="card">
                <div class="card-title"><h2>Настройки профиля</h2></div>
                <p>Раздел в разработке.</p>
            </div>`;
            break;
    }
}

export function initNavigation() {
    // исходный DOM кабинета — это и есть вью «Главная»
    if (main) homeSnapshot = main.innerHTML;

    const btns = document.querySelectorAll('.sidebar-nav .nav-btn');
    btns.forEach(btn => btn.addEventListener('click', () => {
        const name = btn.dataset.view ?? VIEW_ORDER[[...btns].indexOf(btn)];
        activate(name);
    }));
}
