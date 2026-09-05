import { showErrors } from "../../../../utility/showErrors.js";
export function serviceRegistration(validation, normalization){

    function collectData(){
        const elNameReg = document.getElementById('reg-name');
        const elLastnameReg = document.getElementById('reg-lastname');
        const elEmailReg = document.getElementById('reg-email');
        const elPasswordReg = document.getElementById('reg-password');
        const elPasswordRegRepeat = document.getElementById('reg-password-repeat');
        

        return{
            dataSet: {
                namePsychologist: normalization.normalizeMonoSpace(elNameReg.value),
                lastnamePsychologist: normalization.normalizeMonoSpace(elLastnameReg.value),
                passwordPsychologist: normalization.normalizeMonoSpace(elPasswordReg.value),
                emailPsychologist: normalization.normalizeMonoSpace(elEmailReg.value)
            },
            elErrorsSet: {
                passwordRegReput: normalization.normalizeMonoSpace(elPasswordRegRepeat.value),

                textErrorName: document.getElementById('inputErrorName'),
                textErrorLastname: document.getElementById('inputErrorLastname'),
                textErrorEmail: document.getElementById('inputErrorEmailRegistr'),
                textErrorPassword: document.getElementById('inputErrorPasswordRegistr'),
                textErrorPasswordRepeat: document.getElementById('inputErrorPasswordRepeat'),

                inputName: elNameReg,
                inputLastname: elLastnameReg,
                inputEmail: elEmailReg,
                inputPassword: elPasswordReg,
                inputPasswordRepeat: elPasswordRegRepeat
            }
        };
    }

    function validationData(dataSet, elErrorsSet){
        let isValid = true;

        const nameErrors = validation.checkNameAccount(dataSet.namePsychologist);
        showErrors(nameErrors[0], elErrorsSet.textErrorName, elErrorsSet.inputName);
        if(nameErrors.length > 0) isValid = false;

        const lastnameErrors = validation.checkLastnameAccount(dataSet.lastnamePsychologist);
        showErrors(lastnameErrors[0], elErrorsSet.textErrorLastname, elErrorsSet.inputLastname);
        if(lastnameErrors.length > 0) isValid = false;

        const emailErrors = validation.checkEmailAccount(dataSet.emailPsychologist);
        showErrors(emailErrors[0], elErrorsSet.textErrorEmail, elErrorsSet.inputEmail);
        if(emailErrors.length > 0) isValid = false;

        const passwordErrors = validation.checkPasswordAccount(dataSet.passwordPsychologist);
        showErrors(passwordErrors[0], elErrorsSet.textErrorPassword, elErrorsSet.inputPassword);
        if(passwordErrors.length > 0) isValid = false;

        const passwordRepeat = validation.checkPasswordRepeat(dataSet.passwordPsychologist, elErrorsSet.passwordRegReput);
        showErrors(passwordRepeat[0], elErrorsSet.textErrorPasswordRepeat, elErrorsSet.inputPasswordRepeat);
        if(passwordRepeat.length > 0) isValid = false;

        return isValid;

    }

    function clearFeild(elErrorsSet){
        elErrorsSet.inputName = '';
        elErrorsSet.inputLastname = '';
        elErrorsSet.inputEmail = '';
        elErrorsSet.inputPassword = '';
        elErrorsSet.inputPasswordRepeat = '';

        showErrors(null, elErrorsSet.textErrorName, elErrorsSet.inputName);
        showErrors(null, elErrorsSet.textErrorLastname, elErrorsSet.inputLastname);
        showErrors(null, elErrorsSet.textErrorEmail, elErrorsSet.inputEmail);
        showErrors(null, elErrorsSet.textErrorPassword, elErrorsSet.inputPassword);
        showErrors(null, elErrorsSet.textErrorPasswordRepeat, elErrorsSet.inputPasswordRepeat);
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