
import { BTN } from "../actionResearch/researchCardConstants.js"

export function initResearchActions(container){
    container.addEventListener('click', (event) => {
        const btn = event.target.closest('.research-card button');
        // сделать проброс
        if(!btn) return;

        const card = btn.closest('.research-card');
        const researchId = card?.dataset.id;

        const actions = {
            [BTN.START]: () => startResearch(researchId),
            [BTN.STOP]: () => stopResearch(researchId),
            [BTN.EDIT]: () => editResearch(researchId),
            [BTN.REMOVE]: () => removeResearch(researchId),
        };

        for(const className of Object.keys(actions)){
            if(btn.classList.contains(className)){
                actions[className]();
                return;
            }
        }
    });
}