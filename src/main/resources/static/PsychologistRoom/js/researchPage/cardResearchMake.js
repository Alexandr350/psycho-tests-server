import { statusButtons, STATUS_LABELS } from "./researchCardConstants.js";
import { escapeHtml } from "../../../utility/escapeHtml.js";

/**
 * Блок публичной ссылки. Только RUNNING и только при наличии токена:
 * PAUSE скрывает блок — доступ закрыт статусом на сервере; STOPPED/ARCHIVE
 * не строятся вовсе, потому что токен к этому моменту null в БД.
 */
function buildPublicLinkBlock(cardData) {
    if (cardData.researchStatus !== 'RUNNING' || !cardData.publicToken) {
        return '';
    }
    const url = `${window.location.origin}/Questionnaire/CPSK/html/index.html?token=${encodeURIComponent(cardData.publicToken)}`;
    return `
        <div class="public-link-block">
            <span class="public-link-label">Ссылка для респондентов:</span>
            <a class="public-link" href="${escapeHtml(url)}" target="_blank" rel="noopener">${escapeHtml(url)}</a>
            <button type="button" class="btn-copy-link">Скопировать</button>
        </div>`;
}

const progressCache = new Map();

export function setProgressCounter(idResearch, completedSessions) {
    progressCache.set(idResearch, completedSessions);
}
/**
 * Собирает HTML карточки из данных бэкенда. Единственный источник
 * разметки: renderResearchCard вставляет её в конец, updateResearchCard
 * подменяет существующую карточку на новую — разметка не сможет
 * разойтись между «созданием» и «обновлением».
 *
 * (cardData.descriptionResearch || '') — защита от null: колонка
 * в БД nullable, а escapeHtml от null выдаст текст "null".
 */
function buildCardHtml(cardData) {
    const status = escapeHtml(cardData.researchStatus);
    const completed = progressCache.get(cardData.idResearch) ?? 0;
    const cardHtml = `
        <div class="research-card" data-status="${status}" data-id="${escapeHtml(cardData.idResearch)}">
            <span class="status-indicator-border">
                <span class="status-indicator status-${status}"></span>
            </span>

            <div class="body-card-research">
                <div class="card-info">
                    <h3 class="research-title">${escapeHtml(cardData.nameResearch)}</h3>

                    <div class="research-desc-container">
                        <p class="research-desc">${escapeHtml(cardData.descriptionResearch || '')}</p>
                        <button type="button" class="btn-toggle-desc" style="display: none;">Развернуть</button>
                    </div>

                    <div class="meta-row-info">
                        <span class="label-info">Метод исследования:</span>
                        <span class="value-info">${escapeHtml(cardData.typeResearch)}</span>
                    </div>
                    <div class="meta-row-info">
                        <span class="label-info">Заказчик:</span>
                        <span class="value-info">${escapeHtml(cardData.customerResearch || '—')}</span>
                    </div>
                </div>

                ${buildPublicLinkBlock(cardData)}

                <div class="card-footer">
                    <div class="status-research-card">
                        <div class="container-status">
                            <div class="container-status-label-border">
                                <div class="status-label status-${status}">
                                    <p class="status-research">${STATUS_LABELS[cardData.researchStatus] ?? status}</p>
                                </div>
                            </div>
                        </div>

                        <div class="meta-row">
                            <img src="../icon/iconResp.svg" alt="iconResp" class="label">
                            <span class="value">${completed}</span>
                        </div>
                    </div>

                    <div class="actions-container" data-status="${status}">
                        ${statusButtons[cardData.researchStatus] || ''}
                    </div>
                </div>
            </div>
        </div>
    `;
    return cardHtml;
}

/**
 * Вешает локальные обработчики карточки. Вынос отдельной функцией
 * потому, что её нужно повторять и после вставки новой карточки,
 * и после подмены существующей: узлы новой разметки — новые,
 * старые слушатели вместе с ними умерли.
 *
 * Две независимые части: «Развернуть/Скрыть» для описания и
 * «Скопировать» для публичной ссылки. Отсутствие одной не должно
 * мешать другой — поэтому без раннего return.
 */
function attachDescriptionToggle(card) {
    const descEl = card.querySelector('.research-desc');
    const toggleBtn = card.querySelector('.btn-toggle-desc');
    if (descEl && toggleBtn) {
        if (descEl.scrollHeight > descEl.clientHeight) {
            toggleBtn.style.display = 'inline-block';
        }

        toggleBtn.addEventListener('click', () => {
            const isExpanded = descEl.classList.toggle('expanded');
            toggleBtn.textContent = isExpanded ? 'Скрыть' : 'Развернуть';
        });
    }

    // копирование публичной ссылки — независимая от описания часть карточки
    const copyBtn = card.querySelector('.btn-copy-link');
    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            const link = card.querySelector('.public-link');
            if (!link) return;
            navigator.clipboard.writeText(link.href)
                .then(() => { copyBtn.textContent = 'Скопировано'; })
                .catch(() => alert('Не удалось скопировать ссылку'));
        });
    }
}

/**
 * Создаёт карточку в начале контейнера. container обязателен —
 * без него место вставки не определено.
 */
export function renderResearchCard(cardData, container) {
    if (!container) {
        throw new Error('renderResearchCard: Родительский контейнер не передан');
    }

    container.insertAdjacentHTML('afterbegin', buildCardHtml(cardData));

    // CSS.escape — id приходит с сервера, но подстраховка от спецсимволов
    // в селекторе ничего не стоит
    const currentCard = container.querySelector(`[data-id="${CSS.escape(cardData.idResearch)}"]`);
    if (!currentCard) return;

    attachDescriptionToggle(currentCard);
    return currentCard;
}

/**
 * Обновляет существующую карточку на месте, без перезагрузки списка.
 * Применяется после PATCH шапки (редактирование) и после смены статуса —
 * текст, мета, статус и блок публичной ссылки берутся из тех же данных,
 * что и при создании (ResearchResponseInfoDto).
 *
 * <p>Реализация через подмену всего узла, а не точечное обновление полей:
 * статус размазан по четырём местам разметки (data-status корня, классы
 * индикатора и лейбла, набор кнопок, блок ссылки), и при точечном
 * обновлении легко забыть одно из них. Подмена переиспользует
 * buildCardHtml — карточка после обновления байт-в-байт совпадает с той,
 * что построил бы свежий рендер из GET /all. Слушатели делегированы
 * на контейнер, поэтому кнопки в новом узле работают без перевешивания;
 * локальные слушатели переустанавливает attachDescriptionToggle.</p>
 *
 * @param {string} id — idResearch обновляемого исследования
 * @param {Object} cardData — ResearchResponseInfoDto с новыми данными
 * @returns {Element|null} новый узел карточки или null, если карточка не найдена
 */
export function updateResearchCard(id, cardData) {
    if (!id || !cardData) {
        console.error('updateResearchCard: не передан id или данные карточки', { id, cardData });
        return null;
    }

    const existing = document.querySelector(`.research-card[data-id="${CSS.escape(id)}"]`);
    if (!existing) {
        console.warn(`updateResearchCard: карточка исследования "${id}" не найдена`);
        return null;
    }

    const tpl = document.createElement('template');
    tpl.innerHTML = buildCardHtml(cardData).trim();
    const newCard = tpl.content.firstElementChild;

    existing.replaceWith(newCard);
    attachDescriptionToggle(newCard);
    return newCard;
}
