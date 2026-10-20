import { createFileUploadService } from "./createFileUploadService.js";
import { showErrors } from "../../../utility/showErrors.js";

export function stepFive(validation){
    const dropZoneSelf = document.getElementById('dropZoneSelf');
    const fileInputSelf = document.getElementById('fileInputSelf');
    const previewContainerSelf = document.getElementById('previewContainerSelf');


    const serviceUploadImage = createFileUploadService(dropZoneSelf, fileInputSelf, previewContainerSelf, validation);

    function collectData(surveyId = null){
        const formData = new FormData();
        const files = serviceUploadImage.getFiles();

        files.forEach(file => {
            formData.append('files', file);
        });


        if(surveyId){
            formData.append('surveyId', surveyId);
        }else{
            console.warn("Предупреждение: surveyId не был передан в stepFour!");
        }


        return{
            dataSet: formData,
            elErrorsSet: document.getElementById('inputErrorItemSelf')
        };
    }

    function validationData(dataSet, elErrorsSet){
        const errors = serviceUploadImage.getErrors();
        if (errors.length > 0) {
            showErrors(errors.join(' '), elErrorsSet, dropZoneSelf);
            return false;
        }
        return true;
    }

    function clearFeildInputs() {
        serviceUploadImage.clearFiles();
    }

    return {
        collectData,
        clearFeildInputs,
        validationData
    };
    
}



