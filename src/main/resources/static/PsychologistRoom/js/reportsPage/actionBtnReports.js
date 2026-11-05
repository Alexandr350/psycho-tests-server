import { getData } from '../../../utility/getData.js';

const REPORT_URL = '/api/CPSKTest/results/one-characteristic';
const SECTOR_URL = '/api/CPSKTest/results/by-sector';

/**
 * Запрос отчёта. mode='survey' — весь опрос (без idCharacteristic/idItem),
 * иначе требуется один из объектов. idRun=null — все волны.
 */
export async function fetchReport({ idSurvey, idRun = null, idCharacteristic = null, idItem = null, mode = 'object' }) {
    if (!idSurvey) {
        return { success: false, message: 'Не передан idSurvey' };
    }
    if (mode !== 'survey' && !idCharacteristic && !idItem) {
        return { success: false, message: 'Выберите характеристику или изображение' };
    }

    const params = new URLSearchParams({ idSurvey });
    if (idRun) params.set('idRun', idRun);
    if (idCharacteristic) params.set('idCharacteristic', idCharacteristic);
    if (idItem) params.set('idItem', idItem);

    const result = await getData(`${REPORT_URL}?${params.toString()}`);

    if (!result.success) {
        if (result.reason === 'unauthorized') {
            return { success: false, message: 'Сессия устарела. Обновите страницу и войдите заново.' };
        }
        return { success: false, message: result.message || 'Не удалось получить отчёт' };
    }
    return result;
}

/**
 * Отчёт по сектору (цвету): содержание сектора, число респондентов и цвет.
 * В отличие от отчёта по объекту здесь не скрыт ни один тип объектов —
 * показывается всё, что попало в сектор.
 */
export async function fetchSectorResult({ idSurvey, idRun = null, sector }) {
    if (!idSurvey) {
        return { success: false, message: 'Не передан idSurvey' };
    }
    if (!Number.isInteger(sector) || sector < 1 || sector > 8) {
        return { success: false, message: 'Сектор должен быть от 1 до 8' };
    }

    const params = new URLSearchParams({ idSurvey, sector: String(sector) });
    if (idRun) params.set('idRun', idRun);

    const result = await getData(`${SECTOR_URL}?${params.toString()}`);

    if (!result.success) {
        if (result.reason === 'unauthorized') {
            return { success: false, message: 'Сессия устарела. Обновите страницу и войдите заново.' };
        }
        return { success: false, message: result.message || 'Не удалось получить отчёт по сектору' };
    }
    return result;
}

/** Список волн исследования для селектора отчёта. */
export async function fetchRuns(idResearch) {
    const result = await getData(`/api/researchs/research/${idResearch}/runs`);
    return result.success && Array.isArray(result.data) ? result.data : [];
}
