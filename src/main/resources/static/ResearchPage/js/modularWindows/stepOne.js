import { showErrors } from "../../../utility/showErrors.js";


export function stepOne(validation, normalization){
   
    function collectData() {
        const elTitle = document.getElementById('inputTitleResearch');
        const elType = document.getElementById('inputTypeResearch');
        const elCustomer = document.getElementById('inputCustomerResearch');
        const elDescription = document.getElementById('inputDescriptionResearch');

        return {
            dataSet: {
                nameResearch: normalization.normalizeMonoSpace(elTitle.value),
                customerResearch: normalization.normalizeMonoSpace(elCustomer.value),
                descriptionResearch: normalization.normalizeMultuSpace(elDescription.value),
                typeResearch: elType.value
            },
            elErrorsSet: {
                
                textErrorTitle: document.getElementById('inputErrorTitleResearch'),
                textErrorType: document.getElementById('inputErrorTypeResearch'),
                textErrorCustomer: document.getElementById('inputErrorCustomerResearch'),
                textErrorDescription: document.getElementById('inputErrorDescriptionResearch'),
                
                inputTitle: elTitle,
                inputType: elType,
                inputCustomer: elCustomer,
                inputDescription: elDescription
            }
        };
    }

    function validationData(dataSet, elErrorsSet){
        let isValid = true;
        const titleErrors = validation.checkTitle(dataSet.nameResearch);
        showErrors(titleErrors[0], elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        if(titleErrors.length > 0) {
            isValid = false;
        }

        const typeErrors = validation.checkType(dataSet.typeResearch);
        showErrors(typeErrors[0], elErrorsSet.textErrorType, elErrorsSet.inputType);
        if(typeErrors.length > 0){
            isValid = false;
        }

        const customerErrors = validation.checkCustomer(dataSet.customerResearch);
        showErrors(customerErrors[0], elErrorsSet.textErrorCustomer, elErrorsSet.inputCustomer);
        if(customerErrors.length > 0){
            isValid = false;
        }

        const descriptionErrors = validation.checkDescription(dataSet.descriptionResearch);
        showErrors(descriptionErrors[0], elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);
        if(descriptionErrors.length > 0){
            isValid = false;
        }
        return isValid;

    }

    function clearFeild(elErrorsSet){
        elErrorsSet.inputTitle.value = '';
        elErrorsSet.inputType.selectedIndex  = 0;
        elErrorsSet.inputCustomer.value = '';
        elErrorsSet.inputDescription.value = '';

        showErrors(null, elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        showErrors(null, elErrorsSet.textErrorType, elErrorsSet.inputType);
        showErrors(null, elErrorsSet.textErrorCustomer, elErrorsSet.inputCustomer);
        showErrors(null, elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);

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