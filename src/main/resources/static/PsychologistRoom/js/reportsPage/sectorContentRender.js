/* ============================================================
 * sectorContentRender.js
 * Читатели полей попаданий и рендеры групп контента.
 * Используются и карточками секторов, и панелью отчёта по сектору.
 * ============================================================ */

import { escapeHtml } from '../../../utility/escapeHtml.js';

/* Терпимые читатели полей попаданий: text/textCharacteristic, hits/count */
export const readText = h => h.text ?? h.textCharacteristic ?? h.answer ?? '—';
export const readHits = h => h.hits ?? h.count ?? 0;
export const readUrl = h => h.url ?? h.urlItem ?? null;

const groupHtml = (cap, list, render) => list.length ? `
    <div class="content-group">
        <div class="group-cap">${cap}</div>
        <div class="hit-list">${list.map(render).join('')}</div>
    </div>` : '';

const charRow = h => `<div class="hit"><span>${escapeHtml(readText(h))}</span><span class="hits">${readHits(h)} раз</span></div>`;

const itemRow = h => {
    const url = readUrl(h);
    return url
        ? `<div class="hit"><img class="thumb" src="${url}" alt=""><span>Изображение</span><span class="hits">${readHits(h)} раз</span></div>`
        : `<div class="hit"><span class="thumb ph">🖼</span><span>Изображение</span><span class="hits">${readHits(h)} раз</span></div>`;
};

const assocRow = h => `<div class="hit"><span>«${escapeHtml(readText(h))}»</span><span class="hits">${readHits(h)} раз</span></div>`;

/** Рендерит четыре группы попаданий; если всё пусто — заглушка. */
export function renderContentGroups(content) {
    return (
        groupHtml('Характеристики', content.characteristics ?? [], charRow) +
        groupHtml('Изображения', content.items ?? [], itemRow) +
        groupHtml('Дополнительные изображения', content.itemsSub ?? [], itemRow) +
        groupHtml('Ассоциации', content.associations ?? [], assocRow)
    ) || 'Пусто — ни одного попадания';
}
