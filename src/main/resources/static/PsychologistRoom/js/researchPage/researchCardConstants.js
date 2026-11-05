export const BTN = {
    START:   'btn-start-research',
    RESTART: 'btn-restart-research',  // повторный запуск завершённого исследования
    STOP:    'btn-stop-research',
    EDIT:    'btn-edit-research',
    REMOVE:  'btn-remove-research',
    PAUSE:   'btn-pause-research',
    NEXT:    'btn-next-research',
    RESULT:  'btn-result-research',   // просмотр результатов (отдельный эндпоинт)
    ARCHIVE: 'btn-archive-research',  // перевод в архив (PATCH статуса)
};


export const statusButtons = {
    'NEW': `
        <button class="btn btn-primary ${BTN.START}">Запустить</button>
        <button class="btn btn-primary ${BTN.EDIT}">Редактировать</button>
        <button class="btn btn-danger ${BTN.REMOVE}">Удалить</button>
    `,
    'DRAFT': `
        <button class="btn btn-primary ${BTN.EDIT}">Редактировать</button>
        <button class="btn btn-danger ${BTN.REMOVE}">Удалить</button>
    `,
    'RUNNING': `
        <button class="btn btn-primary ${BTN.STOP}">Остановить</button>
        <button class="btn btn-secondary ${BTN.PAUSE}">Пауза</button>
    `,
    'PAUSE': `
        <button class="btn btn-primary ${BTN.STOP}">Остановить</button>
        <button class="btn btn-secondary ${BTN.NEXT}">Продолжить</button>
    `,
    'STOPPED': `
        <button class="btn btn-primary ${BTN.RESULT}">Результаты</button>
        <button class="btn btn-primary ${BTN.RESTART}">Запустить снова</button>
        <button class="btn btn-primary ${BTN.ARCHIVE}">В архив</button>
        <button class="btn btn-danger ${BTN.REMOVE}">Удалить</button>
    `,
    'ARCHIVE': `
        <button class="btn btn-primary ${BTN.RESULT}">Результаты</button>
        <button class="btn btn-danger ${BTN.REMOVE}">Удалить</button>
    `,
};

export const STATUS_LABELS = {
    DRAFT: 'Черновик', NEW: 'Новая', RUNNING: 'В работе',
    PAUSE: 'Пауза', STOPPED: 'Завершено', ARCHIVE: 'Архив',
};
