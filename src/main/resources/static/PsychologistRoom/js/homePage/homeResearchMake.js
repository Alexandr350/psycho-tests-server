/* ============================================================
 * homeResearchMake.js
 * Вью «Главная»: сводка, быстрые действия, активность,
 * лента статей, новости, подсказка дня, недавние исследования.
 * ============================================================ */
import { escapeHtml } from '../../../utility/escapeHtml.js';
import { getData } from '../../../utility/getData.js';
import { rememberResearchForReports } from '../navigationPsychologistRoom/viewState.js';
import { goTo } from '../navigationPsychologistRoom/navigation.js';

/* Endpoint списка исследований — сверьте с тем, что использует
   setContextResearchPage.js в setResearchList */
const RESEARCH_LIST_URL = '/api/researchs/all';
const PROGRESS_URL = '/api/researchs/progress';

/* ---------- Контентные заглушки: заменить на бэкенд позже ---------- */

/* Лента статей: пока статичный массив. Когда появится таблица —
   сюда встаёт fetch и тот же рендер. */
const ARTICLES = [
    {
        tag: 'Методика',
        title: 'Сколько нужно респондентов для надёжных процентов',
        text: 'При малых выборках один ответ сдвигает процент на 10–20. Ориентир для ЦПСК — от 30 сессий на волну.'
    },
    {
        tag: 'Методика',
        title: 'Как формулировать характеристики',
        text: 'Короткие, взаимоисключающие по смыслу пары дают более чистое распределение по секторам колеса.'
    },
    {
        tag: 'Практика',
        title: 'Что читается в отчёте «по изображению»',
        text: 'Скрытие пункта из своего сектора — намеренная асимметрия: смотрите, куда объект попадает у других.'
    }
];

/* Подсказка дня: ротация по дате — каждый день новая строка */
const TIPS = [
    'Проценты в отчёте нормируются до суммы 100 — остаток после округления уходит сектору с максимумом.',
    'Статус «Приостановлено» сохраняет ссылку: респонденты, начавшие сессию, смогут её завершить.',
    'Рестарт создаёт новую волну с новой публичной ссылкой — старая ссылка перестаёт работать.',
    'Ассоциации группируются по точному тексту: «Радость» и « радость» — разные записи.',
    'Отчёт по пункту скрывает его собственные попадания — это защита от самонаведения.'
];

/**
 * Точка входа вью «Главная»: строит разметку и запускает загрузку данных.
 * @param {HTMLElement} container — <main> кабинета.
 */
export function renderHomeView(container) {
    container.innerHTML = buildHomeHtml();
    loadHomeData();
}

function buildHomeHtml() {
    return `
    <div class="card hero-card">
        <div class="hero-text">
            <h2>Добро пожаловать в кабинет психолога</h2>
            <p>Создавайте исследования ЦПСК, следите за сбором ответов и открывайте отчёты.</p>
            <div class="hero-actions">
                <button id="createResearch" class="action-btn btn-create">+ Создать исследование</button>
                <button id="goAllResearchs" class="btn btn-secondary">Мои исследования</button>
            </div>
        </div>
    </div>

    <div class="summary-grid" id="summary-grid"></div>

    <div class="home-columns">
        <div class="home-col-main">
            <div class="card">
                <div class="card-title"><h3>Активность по исследованиям</h3></div>
                <div id="activity-list" class="activity-list">
                    <p class="muted">Загрузка…</p>
                </div>
            </div>

            <div class="card">
                <div class="card-title"><h3>Статьи и материалы</h3></div>
                <div id="articles-list" class="articles-list"></div>
            </div>
        </div>

        <div class="home-col-side">
            <div class="card">
                <div class="card-title"><h3>Подсказка дня</h3></div>
                <p class="tip-text" id="tip-text"></p>
            </div>

            <div class="card">
                <div class="card-title"><h3>Новости психологии</h3></div>
                <p class="muted">Скоро здесь появятся свежие материалы из мира психологии —
                    подборка готовится.</p>
            </div>

            <div class="card">
                <div class="card-title"><h3>Недавние исследования</h3></div>
                <div id="recent-list" class="recent-list">
                    <p class="muted">Загрузка…</p>
                </div>
            </div>
        </div>
    </div>`;
}

/**
 * Тянет список исследований и прогресс, заполняет сводку, активность
 * и блок недавних. Список — источник имён; прогресс — счётчиков.
 */
async function loadHomeData() {
    const [listRes, progressRes] = await Promise.all([
        getData(RESEARCH_LIST_URL).catch(() => ({ success: false, data: [] })),
        getData(PROGRESS_URL).catch(() => ({ success: false, data: [] }))
    ]);

    const researchs = listRes.success && Array.isArray(listRes.data) ? listRes.data : [];
    const progress = progressRes.success && Array.isArray(progressRes.data) ? progressRes.data : [];

    renderSummary(researchs);
    renderActivity(researchs, progress);
    renderRecent(researchs);
    renderArticles();
    renderTip();
    bindActions();
}

/** Сводка: четыре цифры из списка исследований — без нового эндпоинта. */
function renderSummary(researchs) {
    const grid = document.getElementById('summary-grid');
    const total = researchs.length;
    const running = researchs.filter(r => r.researchStatus === 'RUNNING').length;
    const completed = researchs.reduce((sum, r) => sum + (r.completed ?? 0), 0);

    grid.innerHTML = `
        <div class="summary-card"><span class="num">${total}</span><span class="cap">Исследований всего</span></div>
        <div class="summary-card"><span class="num">${running}</span><span class="cap">Идёт сбор</span></div>
        <div class="summary-card"><span class="num">${completed}</span><span class="cap">Завершено сессий</span></div>
        <div class="summary-card"><span class="num">${running}</span><span class="cap">Активных ссылок</span></div>`;
}

/** Активность: строка на исследование с прогрессом; клик открывает отчёт. */
function renderActivity(researchs, progress) {
    const box = document.getElementById('activity-list');
    if (!researchs.length) {
        box.innerHTML = '<p class="muted">Пока нет исследований — создайте первое.</p>';
        return;
    }

    const byId = new Map(progress.map(p => [p.idResearch, p.completedSessions]));
    box.innerHTML = researchs.slice(0, 6).map(r => `
        <div class="activity-row" data-id="${escapeHtml(r.idResearch)}">
            <div class="activity-main">
                <span class="activity-name">${escapeHtml(r.nameResearch ?? 'Без названия')}</span>
                <span class="activity-status">${escapeHtml(r.researchStatus ?? '')}</span>
            </div>
            <span class="activity-count">${byId.get(r.idResearch) ?? 0} сессий</span>
        </div>`).join('');

    box.querySelectorAll('.activity-row').forEach(row => {
        row.addEventListener('click', async () => {
            const id = row.dataset.id;
            const res = await getData(`/api/researchs/research/${id}`);
            if (res.success) {
                rememberResearchForReports(res.data);
                goTo('reports');
            } else {
                alert(res.message || 'Не удалось открыть отчёт.');
            }
        });
    });
}

/** Три последних исследования — компактный список, ссылка «все». */
function renderRecent(researchs) {
    const box = document.getElementById('recent-list');
    if (!researchs.length) {
        box.innerHTML = '<p class="muted">Здесь появятся ваши исследования.</p>';
        return;
    }
    box.innerHTML = researchs.slice(0, 3).map(r => `
        <div class="recent-row" data-id="${escapeHtml(r.idResearch)}">
            ${escapeHtml(r.nameResearch ?? 'Без названия')}
        </div>`).join('') +
        '<button class="btn btn-secondary recent-all" id="goAllResearchs2">Смотреть все</button>';

    box.querySelectorAll('.recent-row').forEach(row =>
        row.addEventListener('click', async () => {
            const res = await getData(`/api/researchs/research/${row.dataset.id}`);
            if (res.success) {
                rememberResearchForReports(res.data);
                goTo('reports');
            }
        }));
    box.querySelector('#goAllResearchs2')?.addEventListener('click', () => goTo('researchs'));
}

/** Статьи — статичный массив до появления бэкенда. */
function renderArticles() {
    document.getElementById('articles-list').innerHTML = ARTICLES.map(a => `
        <div class="article-row">
            <span class="article-tag">${escapeHtml(a.tag)}</span>
            <div>
                <span class="article-title">${escapeHtml(a.title)}</span>
                <p class="article-text">${escapeHtml(a.text)}</p>
            </div>
        </div>`).join('');
}

/** Подсказка дня: индекс растёт вместе с датой — «новая» каждый день. */
function renderTip() {
    const dayIndex = Math.floor(Date.now() / 86400000);
    document.getElementById('tip-text').textContent = TIPS[dayIndex % TIPS.length];
}

/** Локальные кнопки вью: переход к списку. Кнопка «Создать» живёт в setupModal. */
function bindActions() {
    document.getElementById('goAllResearchs')?.addEventListener('click', () => goTo('researchs'));
}
