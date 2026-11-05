/* ============================================================
 * reportsResearchMake.js
 * Вью «Результаты тестов»: список исследований, селекторы,
 * сборка отчёта из ResultOneCharacteristicResponseDto
 * (GET /results/one-characteristic) и заголовок отчёта.
 * Визуализация — в reportVisuals.js, дриллдаун — в sectorDetail.js.
 * ============================================================ */

import { escapeHtml } from '../../../utility/escapeHtml.js';
import { getData } from '../../../utility/getData.js';
import { rememberResearchForReports } from '../navigationPsychologistRoom/viewState.js';
import { fetchReport, fetchRuns } from './actionBtnReports.js';
import { normalizeResearch } from './researchNormalize.js';
import { renderWheel, renderBars } from './reportVisuals.js';
import { initSectorDrilldown } from './sectorDetail.js';



/* ---------- Список исследований ---------- */

/** Список исследований с кнопкой «Просмотреть отчёт» у каждой строки.
 *  Если onPick не передан — функция работает сама: тянет DTO по id,
 *  запоминает его в viewState и рендерит отчёт. */
export function renderReportsEmptyState(container, onPick) {
    const pick = typeof onPick === 'function'
        ? onPick
        : async (id) => {
            const result = await getData(`/api/researchs/research/${id}`);
            if (!result.success) {
                alert(result.message || 'Не удалось загрузить данные исследования.');
                return;
            }
            rememberResearchForReports(result.data);
            renderReportsView(container, result.data);
        };

    container.innerHTML = `
    <div class="card reports-empty">
        <div class="card-title"><h2>Результаты тестов</h2></div>
        <p class="muted">Выберите исследование — отчёт откроется здесь.</p>
        <div class="reports-pick-list" id="reports-pick-list">
            <p class="muted">Загрузка…</p>
        </div>
    </div>`;

    loadResearchList(container, pick);
}

async function loadResearchList(container, onPick) {
    const box = document.getElementById('reports-pick-list');
    if (!box) return;

    try {
        const result = await getData('/api/researchs/all');

        if (!result.success) {
            console.error('renderReportsEmptyState: сервер вернул ошибку', result.message);
            box.innerHTML = '<p class="error">Не удалось загрузить список исследований</p>';
            return;
        }

        const list = Array.isArray(result.data) ? result.data : [];
        if (list.length === 0) {
            box.innerHTML = '<p class="muted">Исследований пока нет — создайте первое во вкладке «Мои исследования».</p>';
            return;
        }

        box.innerHTML = list.map(r => `
            <div class="reports-pick-row">
                <div class="reports-pick-info">
                    <span class="reports-pick-name">${escapeHtml(r.nameResearch ?? 'Без названия')}</span>
                    <span class="reports-pick-status">${escapeHtml(r.researchStatus ?? '')}</span>
                </div>
                <button type="button"
                        class="btn btn-primary btn-view-report"
                        data-id="${escapeHtml(r.idResearch)}">Просмотреть отчёт</button>
            </div>`).join('');

        box.querySelectorAll('.btn-view-report').forEach(btn => {
            btn.addEventListener('click', async () => {
                btn.disabled = true;
                try {
                    await onPick(btn.dataset.id);
                } finally {
                    btn.disabled = false;
                }
            });
        });
    } catch (err) {
        console.error('renderReportsEmptyState: запрос списка не удался', err);
        box.innerHTML = '<p class="error">Произошла ошибка при загрузке списка</p>';
    }
}

/* ---------- Сборка вью ---------- */

/** Точка входа вью: нормализует DTO, подгружает волны, строит статичную
 *  разметку, наполняет селекторы, вешает обработчики и — если у первого
 *  объекта каталога есть id — сразу строит отчёт по нему. */
export async function renderReportsView(container, research) {
    const view = normalizeResearch(research);
    view.runs = [];
    view.selectedRun = null;

    // Волны: отдельный запрос — DTO исследования их не несёт
    if (view.idResearch) {
        view.runs = await fetchRuns(view.idResearch);
    }

    container.innerHTML = buildReportsHtml(view);
    fillRuns(view);
    fillPicker(view);
    attachActions(container, view);

    // стартовое состояние — общий отчёт по всему опросу, все волны
    makeReport(container, view, { mode: 'survey', idCharacteristic: null, idItem: null });
}


/** Статичная разметка вью: селектор волны, переключатель режима,
 *  селектор объекта, кнопка построения, колесо, панель отчёта,
 *  контейнер панели сектора и сетка карточек секторов. */
function buildReportsHtml(view) {
    return `
    <div class="card">
        <div class="card-title"><h2>Результаты: ${escapeHtml(view.title)}</h2></div>

        <section class="object-picker">
            <label for="run-select">Волна:</label>
            <select id="run-select"></select>
            <div class="picker-mode">
                <button id="mode-char" data-mode="char">По характеристике</button>
                <button id="mode-item" data-mode="item">По изображению</button>
                <button id="mode-survey" class="active" data-mode="survey">Весь опрос</button>
            </div>
            <label id="picker-label" for="object-select" style="display:none;">Характеристика:</label>
            <select id="object-select" style="display:none;"></select>
            <button class="btn-make-report" id="make-report-btn">Сформировать отчёт</button>
            <div class="respondents-note">Ответили респондентов: <b>${view.respondents ?? '—'}</b></div>
        </section>

        <div class="report-error" id="report-error"></div>

        <div class="main-grid">
            <section class="panel">
                <h2>Распределение по секторам</h2>
                <div class="wheel-wrap">
                    <svg id="wheel" class="wheel" width="330" height="330" viewBox="-175 -175 350 350"></svg>
                </div>
            </section>

            <section class="panel" id="report-panel">
                <div class="report-head clearfix" id="report-head"></div>

                <div class="bars-block">
                    <h2>Попадания объекта в секторы</h2>
                    <div class="bars" id="bars"></div>
                </div>

                <div class="about-block">
                    <div class="group-cap">Об исследовании</div>
                    <div class="about-row"><span class="k">Название</span><span class="v">${escapeHtml(view.title)}</span></div>
                    <div class="about-row"><span class="k">Заказчик</span><span class="v">${escapeHtml(view.customer)}</span></div>
                    <div class="about-row"><span class="k">Период проведения</span><span class="v">${escapeHtml(view.period)}</span></div>
                    <div class="about-row"><span class="k">Метод</span><span class="v">${escapeHtml(view.method)}</span></div>
                    <div class="about-row"><span class="k">Статус</span><span class="v">${escapeHtml(view.status)}</span></div>
                    <p class="report-desc" style="margin-top:10px;">${escapeHtml(view.description)}</p>
                    <div class="chips">
                        <span class="chip">Респондентов: <b>${view.respondents ?? '—'}</b></span>
                        <span class="chip">Волн: <b>${view.waves ?? '—'}</b></span>
                        <span class="chip">Завершено сессий: <b>${view.completed ?? '—'}</b></span>
                        <span class="chip">Последнее прохождение: <b>${escapeHtml(view.lastPass ?? '—')}</b></span>
                    </div>
                </div>
            </section>
        </div>
        <div class="sector-detail" id="sector-detail" hidden></div>
    </div>`;
}

/* ---------- Селекторы и действия ---------- */

/** Наполняет селектор характеристиками (режим по умолчанию при сборке вью). */
function fillPicker(view) {
    fillPickerForMode('char', view);
}

/** Перезаполняет селектор списком объектов выбранного режима:
 *  'char' — характеристики, 'item' — изображения, 'survey' — селектор пуст.
 *  value опции — id объекта (или текст/путь, если id ещё нет). */
function fillPickerForMode(mode, view) {
    const select = document.getElementById('object-select');
    if (mode === 'survey') {
        select.innerHTML = '';
        return;
    }
    const list = mode === 'char' ? view.catalog.characteristics : view.catalog.items;
    select.innerHTML = list.map(o =>
        `<option value="${escapeHtml(o.id ?? o.text ?? o.url ?? '')}">${escapeHtml(o.text)}</option>`).join('');
}

/** Селектор волн: «Все волны» + каждый запуск с датой и признаком открытости. */
function fillRuns(view) {
    const sel = document.getElementById('run-select');
    sel.innerHTML = '<option value="">Все волны</option>' + (view.runs ?? []).map(r => {
        const d = r.startTimeRun
            ? new Date(r.startTimeRun).toLocaleString('ru-RU',
                { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' })
            : '—';
        return `<option value="${escapeHtml(r.idRun)}">Волна от ${d}${r.open ? ' — открыта' : ''}</option>`;
    }).join('');
}

/** Вешает обработчики: переключатель режимов, селектор волны, кнопку
 *  «Сформировать отчёт» и дриллдаун по секторам. */
function attachActions(container, view) {
    let mode = 'survey';
    initSectorDrilldown(container, view);

    container.querySelectorAll('.picker-mode button').forEach(btn => {
        btn.addEventListener('click', () => {
            mode = btn.dataset.mode;
            container.querySelectorAll('.picker-mode button').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const isSurvey = mode === 'survey';
            document.getElementById('picker-label').style.display = isSurvey ? 'none' : '';
            document.getElementById('object-select').style.display = isSurvey ? 'none' : '';
            if (!isSurvey) {
                document.getElementById('picker-label').textContent =
                    mode === 'char' ? 'Характеристика:' : 'Изображение:';
                fillPickerForMode(mode, view);
            }
        });
    });

    document.getElementById('run-select').addEventListener('change', (e) => {
        view.selectedRun = e.target.value || null;
    });

    document.getElementById('make-report-btn').addEventListener('click', () => {
        const params = mode === 'survey'
            ? { mode, idCharacteristic: null, idItem: null }
            : (() => {
                const selected = document.getElementById('object-select').value;
                return mode === 'char'
                    ? { mode, idCharacteristic: selected, idItem: null }
                    : { mode, idCharacteristic: null, idItem: selected };
            })();
        makeReport(container, view, params);
    });
}

/* ---------- Построение отчёта ---------- */

/** Основной цикл построения отчёта: запрашивает данные через fetchReport
 *  (с выбранной волной) и рендерит колесо, диаграмму и карточки секторов.
 *  На время запроса блокирует кнопку; ошибки — в плашке report-error. */
async function makeReport(container, view, params) {
    const btn = document.getElementById('make-report-btn');
    const err = document.getElementById('report-error');
    err.classList.remove('show');

    if (!view.idSurvey || (params.mode !== 'survey' && !params.idCharacteristic && !params.idItem)) {
        renderReportHead(view, params);
        err.textContent = 'Выберите объект отчёта.';
        err.classList.add('show');
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Загрузка…';
    try {
        const result = await fetchReport({
            idSurvey: view.idSurvey,
            idRun: view.selectedRun ?? null,
            idCharacteristic: params.idCharacteristic,
            idItem: params.idItem,
            mode: params.mode
        });
        if (!result.success) {
            err.textContent = result.message;
            err.classList.add('show');
            return;
        }
        renderReportHead(view, params);
        renderWheel(result.data.counts);
        renderBars(result.data.counts);
    } catch (e) {
        err.textContent = e.message;
        err.classList.add('show');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Сформировать отчёт';
    }
}

/** Заголовок отчёта: бейдж режима, имя выбранного объекта (для изображения —
 *  превью) и краткое описание. При невозможности построить отчёт показывает
 *  подсказку «Выберите объект отчёта». */
function renderReportHead(view, params) {
    const head = document.getElementById('report-head');
    let name, thumbHtml = '';
    if (params.mode === 'survey') {
        name = 'Весь опрос';
    } else if (params.mode === 'char') {
        name = view.catalog.characteristics.find(c => c.id === params.idCharacteristic)?.text
            ?? params.charText ?? 'Выберите объект отчёта';
    } else {
        const item = view.catalog.items.find(i => i.id === params.idItem);
        name = item?.text ?? 'Выберите объект отчёта';
        thumbHtml = item?.url
            ? `<img class="report-thumb" src="${item.url}" alt="">`
            : `<div class="report-thumb ph">🖼</div>`;
    }
    const badge = params.mode === 'char' ? 'Отчёт по характеристике'
        : params.mode === 'item' ? 'Отчёт по изображению'
            : 'Отчёт по всему опросу';
    head.innerHTML = `
        <div class="report-title">
            <span class="badge">${badge}</span>
            «${escapeHtml(name)}»
        </div>
        ${thumbHtml}
        <p class="report-desc">Сводка попаданий выбранного объекта по восьми секторам колеса ЦПСК
            и содержание каждого сектора: характеристики, изображения, ассоциации.</p>`;
}
