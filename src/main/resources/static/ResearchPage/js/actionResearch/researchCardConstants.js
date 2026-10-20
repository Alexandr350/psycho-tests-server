export const BTN = {
    START: 'btn-start-research',
    STOP: 'btn-stop-research',
    EDIT: 'btn-edit-research',
    REMOVE: 'btn-remove-research',
};

export const statusButtons = {
        'NEW': `
            <button class="btn btn-primary btn-start-research">Запустить</button>
            <button class="btn btn-primary btn-edit-research">Редактировать</button>
            <button class="btn btn-danger btn-remove-research">Удалить</button>
        `,
        'DRAFT': `
            <button class="btn btn-primary btn-edit-research">Редактировать</button>
            <button class="btn btn-danger btn-remove-research">Удалить</button>
        `,
        'RUNNING': `
            <button class="btn btn-primary btn-stop-research">Остановить</button>
            <button class="btn btn-secondary btn-pause-research">Пауза</button>
        `,
        'PAUSE': `
            <button class="btn btn-primary btn-stop-research">Остановить</button>
            <button class="btn btn-secondary btn-next-research">Продолжить</button>
        `,
        'STOPPED': `
            <button class="btn btn-primary btn-result-research">Результаты</button>
            <button class="btn btn-danger btn-remove-research">Удалить</button>
        `
    };

export function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}