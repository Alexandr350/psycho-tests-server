import { renderResearchCard } from "./cardResearchMake.js";
import { getData } from "../../../utility/getData.js";

export function setContext(headData, elements){
    
    return {
        setDataHeader() {
            if (!headData) {
                //сделать throw new Error
                console.error("setContext: headData не передан — заголовок не заполнен");
                return};
            const data = JSON.parse(headData);
            elements.elNameAccount.textContent = `${data.name} ${data.lastname}`;
            elements.elEmailAccount.textContent = data.email;
        },
        async setResearchList(container) {
            if (!container) {
                //сделать throw new Error
                console.error('Контейнер не найден');
                return;
            }
            container.innerHTML = '<div class="loading">Загрузка...</div>';
            try {
                const result = await getData('/api/researchs/all');
                if (!result.success) {
                    container.innerHTML = `<div class="error">${result.message}</div>`;
                    return;
                }
                container.innerHTML = '';
                const list = Array.isArray(result.data) ? result.data : (result.data?.array || []);
                list.forEach(item => renderResearchCard(item, container));
            } catch (err) {
                console.error(err);
                container.innerHTML = '<div class="error">Произошла ошибка</div>';
            }
        }
    };
    
}