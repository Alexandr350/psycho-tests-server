import { getData } from "../../utility/getData.js";

export function actionBtnList(){
 
    async function removeResearch(id) {
        if(!id){
            //пробросить
            console.error('removeResearch: id не передан');
            return;
        }

        if(!confirm('Удалить исследование?')) return;
        const result = await getData(`/api/researchs/delete/${id}`); // проверить
        if(!result.success){
            alert(result.messege); // сделать проброс ошибки 
            return;
        }
        document.querySelector(`[data-id="${id}"]`)?.remove();

    }

    async function startResearch(id) {
        if(!id) throw new Error('startResearch: id не передан');
        

        const result = await getData(`/api/researchs/start/${id}`); // проверить
        
        if(!result.success){
            alert(result.messege); // сделать проброс ошибки 
            return;
        }
       
    }
    
    return{
        removeResearch,
        startResearch
    };

    
}