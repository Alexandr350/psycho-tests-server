import { sendData } from "../../../../utility/sendData.js";
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

export function setupModal(
    stepsElements,
    stepTitles,
    stepsEndpoints,
    stepActions
){
    
    let currentStepIndex = 0;
    let activeResearchId = null;
    let activeSurveyId  = null;

    function showStep(index){
        stepsElements.ARRAY_WINDOWS.forEach((step, i) => {
            step.classList.toggle('active', i === index);
        });

        if(stepTitles[index]){
            stepsElements.TITLE.textContent = stepTitles[index];
          
        }
    }

    function updateNextBtn() {
        if(currentStepIndex === stepsElements.ARRAY_WINDOWS.length - 1){
            stepsElements.BTN_NEXT.textContent = 'Создать';
            stepsElements.BTN_NEXT.classList.add('btn-success');
        }else{
            stepsElements.BTN_NEXT.textContent = 'Далее';
            stepsElements.BTN_NEXT.classList.remove('btn-success');
        }
    }

    function goToNextStep() {
        currentStepIndex++;
        updateNextBtn();
        showStep(currentStepIndex);
    }

    function clearFeilds(){
        stepActions.forEach(i => i.clearFeildInputs());
        currentStepIndex = 0;
        activeResearchId = null;
        activeSurveyId = null;
        updateNextBtn();
        showStep(currentStepIndex);
    }


    function closeModalWindow(){
        stepsElements.MODAL_WINDOW.classList.remove('active');
        clearFeilds();
    }


    stepsElements.BTN_CREATE_RESEARCH.addEventListener('click', () => {
        currentStepIndex = 0;
        showStep(currentStepIndex);
        stepsElements.MODAL_WINDOW.classList.add('active');
    });


    stepsElements.BTN_CLOSE.addEventListener('click', () => {
        closeModalWindow();
    });


    window.addEventListener('click', (e) =>{
        if(e.target === stepsElements.MODAL_WINDOW) {
            closeModalWindow();
        }

    });


    stepsElements.BTN_BACK.addEventListener('click', () => {
        stepsElements.BTN_NEXT.textContent = 'Далее';
        stepsElements.BTN_NEXT.classList.remove('btn-success');
        if(currentStepIndex > 0){
            currentStepIndex--;
            showStep(currentStepIndex);
        }else{ closeModalWindow(); }
    });


    stepsElements.BTN_NEXT.addEventListener('click', async () => {
        if(currentStepIndex < stepsElements.ARRAY_WINDOWS.length){
             const endpoint = stepsEndpoints[currentStepIndex];
             const action = stepActions[currentStepIndex];

            if(!action || !endpoint){
                goToNextStep();
                return;
            }


            let idToPass = null;
            if (currentStepIndex === 1) {
                idToPass = activeResearchId; 
            } else if (currentStepIndex >= 2) {
                idToPass = activeSurveyId;   
            }

            let isStepValid = true; 
            const{dataSet, elErrorsSet} = action.collectData(idToPass);


            isStepValid = action.validationData(dataSet, elErrorsSet);

            if(!isStepValid){
                console.warn(`Валидация шага ${currentStepIndex} не пройдена.`);
                return;
            }

            await sleep(300)

            try{
                console.log('данные ушли' , dataSet);
                const result = await sendData(dataSet, endpoint);

                if(result.success){

                    if(currentStepIndex === 0){
                        activeResearchId = (result.data && typeof result.data === 'object')
                            ? result.data.idResearch
                            : result.data;
                    }else if(currentStepIndex === 1){
                        activeSurveyId  = (result.data && typeof result.data === 'object')
                            ? result.data.idSurvey 
                            : result.data;
            
                    }else if(currentStepIndex === 4){
                        const cardInfo = result.data;
                        console.log("Финальная карточка исследования создана:", cardInfo);
                        renderResearchCarв(cardInfo);
                    }

                    action.clearFeildInputs();

                    if(currentStepIndex === stepsElements.ARRAY_WINDOWS.length - 1){
                        closeModalWindow();
                    }else{
                        goToNextStep();
                    }
            
                    
                }else{
                    if (result.reason === 'validation_failed') {
                        console.warn(result.errors); 
                    }
                }
            }catch(e){
                console.error('Критический сбой отправки: ', e);
            }


        }else{
            closeModalWindow();
        }
        
    });
}
