import { showErrors } from "../../../../utility/showErrors.js";
export function serviceLogin(validation, normalization){

    function collectData(){
        const elEmailReg = document.getElementById('login-email');
        const elPasswordReg = document.getElementById('login-password');


        return{
            dataSet: {
                password: normalization.normalizeMonoSpace(elPasswordReg.value),
                email: normalization.normalizeMonoSpace(elEmailReg.value)
            },
            elErrorsSet: {
                textErrorEmail: document.getElementById('inputErrorEmailLogin'),
                textErrorPassword: document.getElementById('inputErrorPasswordLogin'),

                inputEmail: elEmailReg,
                inputPassword: elPasswordReg,
            }
        };
    }

    function validationData(dataSet, elErrorsSet){
        let isValid = true;

        const emailErrors = validation.checkEmailAccount(dataSet.email);
        showErrors(emailErrors[0], elErrorsSet.textErrorEmail, elErrorsSet.inputEmail);
        if(emailErrors.length > 0) isValid = false;

        const passwordErrors = validation.checkPasswordAccount(dataSet.password);
        showErrors(passwordErrors[0], elErrorsSet.textErrorPassword, elErrorsSet.inputPassword);
        if(passwordErrors.length > 0) isValid = false;


        return isValid;

    }

    function clearFeild(elErrorsSet){
        elErrorsSet.inputEmail = '';
        elErrorsSet.inputPassword = '';

        showErrors(null, elErrorsSet.textErrorEmail, elErrorsSet.inputEmail);
        showErrors(null, elErrorsSet.textErrorPassword, elErrorsSet.inputPassword);
    }

    function clearFeildInputs(){
        clearFeild(collectData().elErrorsSet);
    }

    return{
        collectData,
        validationData,
        clearFeildInputs
    };

}