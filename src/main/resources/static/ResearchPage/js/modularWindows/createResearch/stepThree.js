import { showErrors } from "../../../../utility/showErrors.js";


export function stepThree(validation, normalization){
   
    function collectData(surveyId = null) {
        const elCharacteristics = document.getElementById('inputCharacteristics');

        return {
            dataSet: {
                surveyId: surveyId,
                characteristics: normalization.normalizeMonoSpace(elCharacteristics.value),

            },
            elErrorsSet: {
                textErrorCharacteristics: document.getElementById('inputErrorCharacteristics'),

                inputCharacteristics: elCharacteristics,
            }
        };
    }

    function validationData(dataSet, elErrorsSet){
        let isValid = true;

        const characteristicsErrors = validation.checkCharacteristics(dataSet.characteristics);
        showErrors(characteristicsErrors[0], elErrorsSet.textErrorCharacteristics, elErrorsSet.inputCharacteristics);
        if(characteristicsErrors.length > 0) {
            isValid = false;
        }
        return isValid;

    }

    function clearFeild(elErrorsSet){
        elErrorsSet.inputCharacteristics.value = '';

        showErrors(null, elErrorsSet.textErrorCharacteristics, elErrorsSet.inputCharacteristics);
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