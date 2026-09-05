import { sendData } from "../../utility/sendData.js";
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

export function setupAuthorizarion(elAuthSet, serviciesAuth) {

    function showForm(view) {
        elAuthSet.BTN_SWITCH.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.target === view);
        });


        elAuthSet.FORMS.forEach(form => {
            if (form.dataset.view === view) {
                form.classList.remove('hidden');
            } else {
                form.classList.add('hidden');
            }
        });
    }


    elAuthSet.BTN_SWITCH.forEach(btn => {
        btn.addEventListener('click', () => {
            showForm(btn.dataset.target);
        });
    });


    elAuthSet.BTN_TOGGLE_EYE.forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;

            if (!input || input.tagName !== 'INPUT') return;

            const isPassword = input.type === 'password';
            input.type = isPassword ? 'text' : 'password';


            btn.classList.toggle('visible', !isPassword);
        });
    });

    elAuthSet.BTN_LOGIN.addEventListener('click', async () => {
        const {dataSet, elErrorsSet} = serviciesAuth.login.collectData();
        let isLoginValid = serviciesAuth.login.validationData(dataSet, elErrorsSet);

        if(!isLoginValid){
            console.warn('Валидация данных формы авторизации не пройдена');
            return;           
        }
        await sleep(300);
        try{
            const result = await sendData(dataSet, '/api/psychologists/login');

            if(result.success){
                const userDto = result.data;
                localStorage.setItem('currentPsychlogist', JSON.stringify(userDto));
                serviciesAuth.login.clearFeildInputs();
                window.location.href ='/ResearchPage/html/researchPage.html';
            }else{
                if(result.reason === 'validation_failed'){
                    console.warn(result.errors);
                }
            }
        }catch(e){
            console.error('Критическая ошибка при отправке: ', e);
        }

    });
    
    elAuthSet.BTN_REGISTRATION.addEventListener('click', async () => {

        const { dataSet, elErrorsSet } = serviciesAuth.registration.collectData();
        let isRegistedValid = serviciesAuth.registration.validationData(dataSet, elErrorsSet);

        if (!isRegistedValid) {
            console.warn('Валидация данных формы регистрации не пройдена');
            return;
        }
        await sleep(300);

        try {
            const result = await sendData(dataSet, '/api/psychologists/register');

            if (result.success) {
                const userDto = result.data;
                localStorage.setItem('currentPsychlogist', JSON.stringify(userDto));
                serviciesAuth.registration.clearFeildInputs();
                window.location.href = '/ResearchPage/html/researchPage.html';
            } else {
                if (result.reason === 'validation_failed') {
                    console.warn(result.errors);
                }
            }
        } catch (e) {
            console.error('Критическая ошибка при отправке: ', e);
        }

    });
}