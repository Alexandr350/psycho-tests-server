import { statusButtons } from "../actionResearch/researchCardConstants.js"

export function renderResearchCard(cardData, container){
    if(!container) {
        throw new Error('renderResearchCard: Родительский контейнер не передан');
    };

    if(document.querySelector(`[data-id="${cardData.idResearch}"]`)) {
        throw new Error(`renderResearchCard: Карточка с id: "${cardData.idResearch}" уже существует на странице`);
    };

    const cardHtml = `
        <div class="research-card" data-status="${cardData.researchStatus}" data-id="${cardData.idResearch}">
            <span class="status-indicator-border">
                <span class="status-indicator status-${cardData.researchStatus}"></span>
            </span>

            <div class="body-card-research">
                <div class="card-info">
                    <h3 class="research-title">${cardData.nameResearch}</h3>
                    
                    <div class="research-desc-container">
                        <p class="research-desc">${cardData.descriptionResearch}</p>
                        <button type="button" class="btn-toggle-desc" style="display: none;">Развернуть</button>
                    </div>

                    <div class="meta-row-info">
                        <span class="label-info">Метод исследования:</span>
                        <span class="value-info">${cardData.typeResearch}</span>
                    </div>
                    <div class="meta-row-info">
                        <span class="label-info">Заказчик:</span>
                        <span class="value-info">${cardData.customerResearch}</span>
                    </div>
                </div>

                <div class="card-footer">
                    <div class="status-research-card">
                        <div class="container-status">
                            <div class="container-status-label-border">
                                <div class="status-label status-${cardData.researchStatus}">
                                    <p class="status-research">${cardData.researchStatus}</p>
                                </div>
                            </div>
                        </div>

                        <div class="meta-row">
                            <img src="../icon/iconResp.svg" alt="iconResp" class="label">
                            <span class="value">0</span>
                        </div>
                    </div>

                    <div class="actions-container" data-status="${cardData.researchStatus}">
                        ${statusButtons[cardData.researchStatus] || ''}
                    </div>
                </div>
            </div>
        </div>
    `;
    container.insertAdjacentHTML('afterbegin', cardHtml);
    
    const currentCard = container.querySelector(`[data-id="${cardData.idResearch}"]`);
    const descEl = currentCard.querySelector('.research-desc');
    const toggleBtn = currentCard.querySelector('.btn-toggle-desc');


    if (descEl.scrollHeight > descEl.clientHeight) {
        toggleBtn.style.display = 'inline-block'; 
    }

   
    toggleBtn.addEventListener('click', () => {
        const isExpanded = descEl.classList.toggle('expanded');
        toggleBtn.textContent = isExpanded ? 'Скрыть' : 'Развернуть';
    });

}