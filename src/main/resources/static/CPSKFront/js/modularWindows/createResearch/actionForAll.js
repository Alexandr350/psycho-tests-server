

export function setupModal(
    modalWindow,
    openBtnCreateResearchModal,
    closeBtn,
    prevBtn,
    nextBtn,
    stepTitle,
    steps,
    stepTitles,
    service,
    serviceSelf
){
    
    let currentStepIndex = 0;

    function showStep(index){
        steps.forEach((step, i) => {
            step.classList.toggle('active', i === index);
        });

        if(stepTitles[index]){
            stepTitle.textContent = stepTitles[index];
        }
    }

    openBtnCreateResearchModal.addEventListener('click', () => {
        currentStepIndex = 0;
        showStep(currentStepIndex);
        modalWindow.classList.add('active');
    });


    closeBtn.addEventListener('click', () => {
        modalWindow.classList.remove('active');
    });


    window.addEventListener('click', (e) =>{
        if(e.target === modalWindow) {
            modalWindow.classList.remove('active');
            service.clearFiles(); 
            serviceSelf.clearFiles();
        }

    });


    prevBtn.addEventListener('click', () => {
        nextBtn.textContent = 'Далее';
        nextBtn.classList.remove('btn-success');
        if(currentStepIndex > 0){
            currentStepIndex--;
            showStep(currentStepIndex);
        }else{
            modalWindow.classList.remove('active');
        }
    });

    nextBtn.addEventListener('click', () => {
        if(currentStepIndex < steps.length - 1){
            currentStepIndex++;
            if(currentStepIndex === steps.length - 1){
                nextBtn.textContent = 'Создать';
                nextBtn.classList.add('btn-success');
            }else{
                nextBtn.textContent = 'Далее';
                nextBtn.classList.remove('btn-success');
            }
            showStep(currentStepIndex);
        }
        
    });
}
