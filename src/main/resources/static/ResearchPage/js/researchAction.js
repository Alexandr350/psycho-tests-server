document.addEventListener('DOMContentLoaded',() =>{
    const elNameAccount = document.getElementById('name-user-account');
    const elEmailAccount = document.getElementById('email-user-account');

    const headData = localStorage.getItem('currentPsychlogist');

    if(headData){
        try{
            const psychologistsAccountHead = JSON.parse(headData);
            elNameAccount.textContent = `${psychologistsAccountHead.name}  ${psychologistsAccountHead.lastname}`;
            elEmailAccount.textContent = `${psychologistsAccountHead.email}`;

        }catch(error){
            console.error('Ошибка при парсинге данных из localStorage:', error);
        }
    }else{
        console.warn('Данные пользователя не найдены в localStorage. Возможно, пользователь не авторизован.');
        window.location.href = '/AuthorizationPage/html/authorization.html';  
        return;
    }

    window.renderResearchCard = function(cardData){
        const container = document.getElementById('container-research-card');
        if(!container) return;

        if(document.querySelector(`[data-id="${cardData.idResearch}"]`)) return;

        const cardHtml = `
            <div class="research-card" data-status="${cardData.status}" data-id="${cardData.idResearch}">
                <span class="status-indicator-border">
                    <span class="status-indicator status-${cardData.status}"></span>
                </span>

                <div class="body-card-research">
                    <div class="card-info">
                        <h3 class="research-title">${cardData.nameResearch}</h3>
                        <p class="research-desc">${cardData.descriptionResearch}</p>
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
                                    <div class="status-label status-${cardData.status}">
                                        <p class="status-research">${cardData.statusText}</p>
                                    </div>
                                </div>
                            </div>

                            <div class="meta-row">
                                <img src="../icon/iconResp.svg" alt="iconResp" class="label">
                                <span class="value">0</span>
                            </div>
                        </div>

                        <div class="actions-container" data-status="${cardData.status}">
                            ${cardData.status === 'new' 
                                ? `<button class="btn btn-primary">Запустить</button>
                                   <button class="btn btn-secondary">Редактировать</button>`
                                : `<button class="btn btn-primary">Продолжить</button>`
                            }
                            <button class="btn btn-danger">Удалить</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.insertAdjacentHTML('afterbegin', cardHtml);
    };

});