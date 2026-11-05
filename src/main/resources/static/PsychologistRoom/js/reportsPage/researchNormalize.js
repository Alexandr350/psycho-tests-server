/* ============================================================
 * researchNormalize.js
 * Нормализация ResearchResponseInfoDto под вью отчётов.
 * ============================================================ */

const STATUS_LABELS = {
    NEW: 'Новая', RUNNING: 'Идёт сбор', PAUSE: 'Приостановлено',
    PAUSED: 'Приостановлено', STOPPED: 'Остановлено', ARCHIVE: 'В архиве'
};

/**
 * Приводит DTO исследования к плоской форме вью:
 * заголовок, заказчик, метод, статус (переводится в человекочитаемый),
 * счётчики и каталог объектов отчёта.
 *
 * Каталог терпит обе формы полей: старую (строки) и новую ({id, text} / {id, url}).
 * Для строк id = null — такие объекты не могут быть отправлены в отчёт,
 * вью покажет плашку с пояснением.
 *
 * @param {Object} research — ResearchResponseInfoDto из ответа сервера.
 * @returns {Object} — нормализованный объект вью.
 */
export function normalizeResearch(research) {
    return {
        idResearch: research.idResearch,
        idSurvey: research.idSurvey,
        title: research.nameResearch ?? '',
        customer: research.customerResearch ?? '',
        method: research.typeResearch ?? '',
        status: STATUS_LABELS[research.researchStatus] ?? research.researchStatus ?? '',
        description: research.descriptionSurvey ?? '',
        period: '—',
        respondents: research.respondents,
        waves: research.waves,
        completed: research.completed,
        lastPass: research.lastPass,
        // каталог объектов отчёта; id появятся, когда бэк начнёт их отдавать
        catalog: {
            characteristics: (research.characteristics ?? [])
                .map(c => typeof c === 'string'
                    ? { id: null, text: c }
                    : { id: c.idCharacteristic ?? c.id ?? null, text: c.text ?? c.textCharacteristic ?? '' }),
            items: (research.items ?? [])
                .map((u, i) => typeof u === 'string'
                    ? { id: null, text: `Изображение №${i + 1}`, url: u }
                    : { id: u.idItem ?? u.id ?? null, text: u.text ?? `Изображение №${i + 1}`, url: u.url ?? u })
        }
    };
}
