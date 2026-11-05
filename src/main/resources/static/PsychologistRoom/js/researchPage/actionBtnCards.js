import { getData } from "../../../utility/getData.js";
import { sendData } from "../../../utility/sendData.js";
import { BTN } from "./researchCardConstants.js";


// кнопка -> целевой статус (по ALLOWED-переходам ResearchStatus)
// BTN.RESULT здесь больше нет: это навигация к результатам, не смена статуса

const STATUS_TARGET = {
    [BTN.START]: 'RUNNING',
    [BTN.PAUSE]: 'PAUSE',
    [BTN.NEXT]:  'RUNNING',
    [BTN.STOP]:  'STOPPED',
    [BTN.ARCHIVE]: 'ARCHIVE',
};


export function actionBtnList({ onEditReady, onResultsReady } = {}) {

    /**
     * Смена статуса. Возвращает полный ResearchResponseInfoDto из ответа
     * сервера (с publicToken) — карточка обновляется целиком из него,
     * либо null при неудаче. Источник правды — сервер: токен в ответе
     * уже создан или уже обнулён под новый статус.
     */
    async function changeStatus(id, targetStatus) {
        if (!id || !targetStatus) {
            console.error('changeStatus: не передан id или статус', { id, targetStatus });
            return null;
        }

        const result = await sendData(
            { idResearch: id, status: targetStatus },
            `/api/researchs/research/${id}/status`,
            'PATCH'
        );

        if (!result.success) {
            alert(result.message || 'Не удалось изменить статус исследования');
            return null;
        }
        // фолбэк нужен только если бэк ещё отдаёт пустое тело — тогда
        // хотя бы статус на карточке обновится
        return result.data || { idResearch: id, researchStatus: targetStatus };
    }

    async function startResearch(id)   { return changeStatus(id, STATUS_TARGET[BTN.START]); }
    async function stopResearch(id)    { return changeStatus(id, STATUS_TARGET[BTN.STOP]); }
    async function pauseResearch(id)   { return changeStatus(id, STATUS_TARGET[BTN.PAUSE]); }
    async function resumeResearch(id)  { return changeStatus(id, STATUS_TARGET[BTN.NEXT]); }
    async function archiveResearch(id) { return changeStatus(id, STATUS_TARGET[BTN.ARCHIVE]); }
    async function restartResearch(id) { return changeStatus(id, STATUS_TARGET[BTN.RESTART]); }

    /**
     * Переход к результатам: статус не меняет, ничего не отправляет.
     * Загружает полный DTO исследования и отдаёт его через onResultsReady —
     * тот рендерит вью отчёта в main-контейнер кабинета.
     */
    async function viewResults(id) {
        if (!id) {
            console.error('viewResults: id не передан');
            return;
        }
        const result = await getData(`/api/researchs/research/${id}`);
        if (!result.success) {
            if (result.reason === 'unauthorized') {
                alert('Сессия устарела. Обновите страницу и войдите заново.');
            } else {
                alert(result.message || 'Не удалось открыть результаты.');
            }
            return;
        }
        if (typeof onResultsReady === 'function') {
            onResultsReady(result.data);
        } else {
            console.error('viewResults: не передан onResultsReady — некуда рендерить отчёт');
        }
    }

    async function removeResearch(id) {
        if (!id) {
            console.error('removeResearch: id не передан');
            return false;
        }
        if (!confirm('Удалить исследование?')) return false;

        const result = await sendData(null, `/api/researchs/research/${id}`, 'DELETE');
        if (!result.success) {
            alert(result.message || 'Не удалось удалить исследование');
            return false;
        }
        return true;
    }

    /**
     * Рестарт исследования — новая волна: сервер закрывает старый запуск,
     * создаёт новый Run с новым publicToken. Ответ — полный DTO,
     * карточка обновляется целиком из него (статус, кнопки, ссылка).
     */
    async function restartResearch(id) {
        if (!id) {
            console.error('restartResearch: id не передан');
            return null;
        }
        const result = await sendData(null, `/api/researchs/research/${id}/restart`, 'POST');
        if (!result.success) {
            alert(result.message || 'Не удалось перезапустить исследование');
            return null;
        }
        return result.data;
    }


    async function editResearch(id) {
        if (!id) {
            console.error('editResearch: id не передан');
            return false;
        }

        const result = await getData(`/api/researchs/research/${id}`);
        if (result.success) {
            if (typeof onEditReady === 'function') {
                onEditReady(result.data);
                return true;
            }
            console.error('editResearch: не передан onEditReady — модалку открыть некому');
            return false;
        }
        if (result.reason === 'unauthorized') {
            alert('Сессия устарела. Обновите страницу и войдите заново.');
        } else {
            alert(result.message || 'Не удалось загрузить данные исследования.');
        }
        return false;
    }


    return { startResearch, restartResearch, stopResearch, pauseResearch, resumeResearch,
         archiveResearch, viewResults, removeResearch, editResearch };

}
