/* ============================================================
 * sectorDetail.js
 * Дриллдаун по секторам и панель «отчёт по сектору»
 * (GET /results/by-sector).
 * ============================================================ */

import { escapeHtml } from '../../../utility/escapeHtml.js';
import { LIGHT_SECTORS, ROMAN } from './reportsConstants.js';
import { renderContentGroups } from './sectorContentRender.js';
import { fetchSectorResult } from './actionBtnReports.js';

/** view текущего отчёта — обновляется при каждом рендере,
 *  слушатель дриллдауна всегда берёт свежий. */
let drilldownView = null;

/**
 * Дриллдаун по сектору: клик по клину колеса или строке диаграммы
 * фильтрует карточки сектора и подгружает полный отчёт по сектору
 * с сервера (включая объекты, скрытые в отчёте по характеристике).
 * Повторный клик по выбранному сектору — возвращает все восемь
 * карточек и прячет панель.
 */
export function initSectorDrilldown(container, view) {
    drilldownView = view;
    if (container.dataset.drilldownBound === '1') return;
    container.dataset.drilldownBound = '1';

    container.addEventListener('click', async (e) => {
        const target = e.target.closest('.bar-row, .wheel path[data-sector]');
        if (!target) return;
        const sector = Number(target.dataset.sector);
        if (!Number.isInteger(sector)) return;

        const panel = document.getElementById('sector-detail');
        if (!panel) return;

        // повторный клик по тому же сектору — прячет панель
        if (!panel.hidden && panel.dataset.sector === String(sector)) {
            panel.hidden = true;
            panel.dataset.sector = '';
            return;
        }

        await loadSectorDetail(drilldownView, sector);
    });
}


/** Полный отчёт по сектору с сервера: цвет, респонденты, содержание. */
async function loadSectorDetail(view, sector) {
    const panel = document.getElementById('sector-detail');
    if (!panel) return;
    panel.hidden = false;
    panel.dataset.sector = String(sector);
    panel.innerHTML = '<p class="muted">Загрузка отчёта по сектору…</p>';
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

    const result = await fetchSectorResult({
        idSurvey: view.idSurvey,
        idRun: view.selectedRun ?? null,
        sector
    });
    if (!result.success) {
        panel.innerHTML = `<p class="error">${escapeHtml(result.message)}</p>`;
        return;
    }
    renderSectorDetailData(panel, result.data);
}

function renderSectorDetailData(panel, dto) {
    const light = LIGHT_SECTORS.has(dto.numberSector) ? 'light' : 'dark';
    const body = renderContentGroups(dto.content ?? {});

    panel.innerHTML = `
        <div class="sector-detail-head ${light}" style="background:${dto.color}">
            <span class="roman">${ROMAN[dto.numberSector]}</span>
            <span class="sector-detail-title">Сектор ${ROMAN[dto.numberSector]}</span>
            <span class="sector-detail-meta">Респондентов: <b>${dto.totalRespondents}</b></span>
        </div>
        <div class="sector-detail-body">${body}</div>`;
}
