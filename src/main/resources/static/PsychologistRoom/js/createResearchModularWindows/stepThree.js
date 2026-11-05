import { showErrors } from "../../../utility/showErrors.js";


export function stepThree(validation, normalization){

    function collectData(idSurvey = null) {
        const elCharacteristics = document.getElementById('inputCharacteristics');

        return {
            dataSet: {
                idSurvey: idSurvey,
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

    /**
     * Предзаполнение характеристик из DTO резюме. Бэк отдаёт массив строк,
     * пользователь вводит их в textarea через запятую — склеиваем обратно.
     * Null/не-массив — легальный случай (шаг не пройден), подставляем пусто.
     */
    function fillFields(data){
        const elErrorsSet = collectData().elErrorsSet;

        const chars = Array.isArray(data.characteristics) ? data.characteristics : [];
        elErrorsSet.inputCharacteristics.value = chars.join(', ');

        showErrors(null, elErrorsSet.textErrorCharacteristics, elErrorsSet.inputCharacteristics);
    }

     return{
            collectData,
            validationData,
            clearFeildInputs,
            fillFields
    };


}
