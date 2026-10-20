import { createFileUploadService } from "./createFileUploadService.js";
import { showErrors } from "../../../utility/showErrors.js";

export function stepFour(validation){
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const previewContainer = document.getElementById('previewContainer');


    const serviceUploadImage = createFileUploadService(dropZone, fileInput, previewContainer, validation);

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
            elErrorsSet: document.getElementById('inputErrorItem')
        };
    }

    function validationData(dataSet, elErrorsSet){
        const errors = serviceUploadImage.getErrors();
        if (errors.length > 0) {
            return false;
        }
        return true;
    }

    function clearFeildInputs() {
        serviceUploadImage.clearFiles();
        const elErrorsSet = document.getElementById('inputErrorItem');
        showErrors(null, elErrorsSet, dropZone);
    }

    return {
        collectData,
        clearFeildInputs,
        validationData
    };
    
}



