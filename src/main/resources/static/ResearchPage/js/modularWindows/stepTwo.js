import { showErrors } from "../../../utility/showErrors.js";


export function stepTwo(validation, normalization){
   
    function collectData(researchId = null) {
        const elTitle = document.getElementById('inputTitleSurvey');
        const elDescription = document.getElementById('inputDescriptionSurvey');
        const elRules = document.getElementById('inputRulesSurvey');

        return {
            dataSet: {
                researchId: researchId,
                titleSurvey: normalization.normalizeMonoSpace(elTitle.value),
                descriptionSurvey: normalization.normalizeMultuSpace(elDescription.value),
                rulesSurvey: normalization.normalizeMonoSpace(elRules.value),
            },
            elErrorsSet: {
                
                textErrorTitle: document.getElementById('inputErrorTitleSurvey'),
                textErrorDescription: document.getElementById('inputErrorDescriptionSurvey'),
                textErrorRules: document.getElementById('inputErrorRulesSurvey'),
                
                inputTitle: elTitle,
                inputRules: elRules,
                inputDescription: elDescription
            }
        };
    }

    function validationData(dataSet, elErrorsSet){
        let isValid = true;
        const titleErrors = validation.checkTitle(dataSet.titleSurvey);
        showErrors(titleErrors[0], elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        if(titleErrors.length > 0) {
            isValid = false;
        }


        const descriptionErrors = validation.checkDescription(dataSet.descriptionSurvey);
        showErrors(descriptionErrors[0], elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);
        if(descriptionErrors.length > 0){
            isValid = false;
        }

        const rulesErrors = validation.checkRules(dataSet.rulesSurvey);
        showErrors(rulesErrors[0], elErrorsSet.textErrorRules, elErrorsSet.inputRules);
        if(rulesErrors.length > 0) {
            isValid  = false;
        }
        return isValid;

    }

    function clearFeild(elErrorsSet){
        elErrorsSet.inputTitle.value = '';
        elErrorsSet.inputDescription.value = '';
        elErrorsSet.inputRules.value = '';

        showErrors(null, elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        showErrors(null, elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);
        showErrors(null, elErrorsSet.textErrorRules, elErrorsSet.inputRules);

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