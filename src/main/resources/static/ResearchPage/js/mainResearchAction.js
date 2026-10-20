
import { setContext } from "./actionResearch/setContextResearchPage.js";

document.addEventListener('DOMContentLoaded', async () =>{
    const elementsHeader ={
        elNameAccount: document.getElementById('name-user-account'),
        elEmailAccount: document.getElementById('email-user-account')
    }
    const headData = localStorage.getItem('currentPsychlogist');
    const container = document.getElementById('researchs');
    
    const contex = setContext(headData, elementsHeader);
    contex.setDataHeader();
    await contex.setResearchList(container);
    

});

