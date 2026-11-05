/* ============================================================
 * reportVisuals.js
 * Визуализация отчёта: колесо ЦПСК (SVG), горизонтальная диаграмма
 * попаданий и сетка карточек секторов.
 * ============================================================ */

import { SECTOR_COLORS, LIGHT_SECTORS, ROMAN, svgNS } from './reportsConstants.js';

/* ---------- Колесо: сплошные клинья от центра, сектор I от 12 часов ---------- */

/**
 * Рисует колесо ЦПСК: восемь сплошных клиньев от центра, сектор I
 * начинается от 12 часов и идёт по часовой стрелке. Цвет клина — по
 * SECTOR_COLORS; пустой сектор полупрозрачный. Внутри клина — процент,
 * снаружи — римский номер сектора; для светлых секторов (2 и 7) текст
 * тёмный через класс on-light. Каждый клин несёт data-sector для дриллдауна.
 *
 * @param {Array<{numberSector: number, count: number, percent: number}>} counts.
 */
export function renderWheel(counts) {
    const wheel = document.getElementById('wheel');
    wheel.innerHTML = '';
    const R = 150;

    counts.forEach(c => {
        const n = c.numberSector;
        const a0 = (-90 + (n - 1) * 45) * Math.PI / 180;
        const a1 = (-90 + n * 45) * Math.PI / 180;
        const p = ang => [R * Math.cos(ang), R * Math.sin(ang)];
        const [x0, y0] = p(a0), [x1, y1] = p(a1);

        const path = document.createElementNS(svgNS, 'path');
        path.setAttribute('d',
            `M 0 0 L ${x0.toFixed(2)} ${y0.toFixed(2)} A ${R} ${R} 0 0 1 ${x1.toFixed(2)} ${y1.toFixed(2)} Z`);
        path.setAttribute('fill', SECTOR_COLORS[n]);
        path.setAttribute('opacity', c.percent === 0 ? 0.35 : 1);
        path.setAttribute('data-sector', n);
        wheel.appendChild(path);

        const am = (a0 + a1) / 2;
        const rm = R * 0.72;
        const tx = document.createElementNS(svgNS, 'text');
        tx.setAttribute('x', (rm * Math.cos(am)).toFixed(2));
        tx.setAttribute('y', (rm * Math.sin(am) + 5).toFixed(2));
        tx.setAttribute('text-anchor', 'middle');
        tx.setAttribute('class', 'sector-pct'
            + (LIGHT_SECTORS.has(n) ? ' on-light' : '')
            + (c.percent === 0 ? ' zero' : ''));
        tx.textContent = c.percent + '%';
        wheel.appendChild(tx);

        const rn = R + 18;
        const tnum = document.createElementNS(svgNS, 'text');
        tnum.setAttribute('x', (rn * Math.cos(am)).toFixed(2));
        tnum.setAttribute('y', (rn * Math.sin(am) + 5).toFixed(2));
        tnum.setAttribute('text-anchor', 'middle');
        tnum.setAttribute('class', 'sector-num');
        tnum.textContent = ROMAN[n];
        wheel.appendChild(tnum);
    });
}

/* ---------- Диаграмма попаданий ---------- */

/**
 * Рисует горизонтальную диаграмму «попадания по секторам»: строка на
 * каждый сектор — римский номер, полоса, закрашенная цветом сектора,
 * справа счётчик и процент. Полоса с нулевым процентом всё равно
 * получает минимальную ширину 1.5%, чтобы строка была видна.
 * Каждая строка несёт data-sector для дриллдауна.
 */
export function renderBars(counts) {
    document.getElementById('bars').innerHTML = counts.map(c => `
        <div class="bar-row" data-sector="${c.numberSector}">
            <span class="s-num">${ROMAN[c.numberSector]}</span>
            <div class="bar-track">
                <div class="bar-fill" style="width:${Math.max(c.percent, 1.5)}%; background:${SECTOR_COLORS[c.numberSector]}"></div>
            </div>
            <span class="s-val"><b>${c.count}</b> · ${c.percent}%</span>
        </div>`).join('');
}

