import { sendData } from "./sendData.js";

export function setupModal(
    stepsElements,
    stepTitles,
    services,
    stepsEndpoints,
    stepActions
){
    
    let currentStepIndex = 0;

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

    stepsElements.BTN_CREATE_RESEARCH.addEventListener('click', () => {
        currentStepIndex = 0;
        showStep(currentStepIndex);
        stepsElements.MODAL_WINDOW.classList.add('active');
    });


    stepsElements.BTN_CLOSE.addEventListener('click', () => {
        stepsElements.MODAL_WINDOW.classList.remove('active');
    });


    window.addEventListener('click', (e) =>{
        if(e.target === stepsElements.MODAL_WINDOW) {
            stepsElements.MODAL_WINDOW.classList.remove('active');
            services.forEach(i => i.clearFiles());
        }

    });


    stepsElements.BTN_BACK.addEventListener('click', () => {
        stepsElements.BTN_NEXT.textContent = 'Далее';
        stepsElements.BTN_NEXT.classList.remove('btn-success');
        if(currentStepIndex > 0){
            currentStepIndex--;
            showStep(currentStepIndex);
        }else{
            stepsElements.MODAL_WINDOW.classList.remove('active');
        }
    });


    stepsElements.BTN_NEXT.addEventListener('click', async () => {
        if(currentStepIndex < stepsElements.ARRAY_WINDOWS.length - 1){
             const endpoint = stepsEndpoints[currentStepIndex];
             const action = stepActions[currentStepIndex];

            if(!action || !endpoint){
                currentStepIndex++;
                updateNextBtn();
                showStep(currentStepIndex);
                return;
            }

            try{
                const result = await sendData(action, endpoint);

                if(result.success){
                    currentStepIndex++;
                    updateNextBtn();
                    showStep(currentStepIndex);
                }else{
                    console.warn('Валидация не пройдена: Ошибки');
                }
            }catch(e){
                console.error('Ошибка отправки: ', e);
            }

        }
        
    });
}
