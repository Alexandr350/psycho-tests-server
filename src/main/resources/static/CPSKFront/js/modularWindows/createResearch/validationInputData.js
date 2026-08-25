export function validationInputData(){

    const patternText = /^[A-Za-z0-9_\-.,\s]+$/;
    const titleSize = 100;
    const customerSize = 100;
    const descriptionSize = 1000;
    const rulesSize = 2000;
    const MAX_SIZE_FILE = 30 * 1024 * 1024;

    function checkTitle(data){
        
        if(data.length === 0){
            console.log('Пустое поле названия');
            return {
                success: false,
                message: 'Поле не может быть пустым'
            };
        }

        if(!checkText(data)) return false;

        if(checkSize(data, titleSize)){
            console.log('Мах 100');
            return false;
        }

        return true;
    }

    function checkDescription(data){
        if(!data) return true;
        if(!checkText(data)) return false;
        if(checkSize(data, descriptionSize)){
            console.log('Мах 1000');
            return false;
        }
    
        return true;        
    }

    function checkCustomer(data){
        if(!checkText(data)) return false;

         if(checkSize(data, customerSize)){
            console.log('Мах 100');
            return false;
        }
        return true;
    }

    function checkRules(data){
        if(!checkText(data)) return false;
        if(checkSize(data, rulesSize)){
            console.log('Мах 2000');
            return false;
        }
    
        return true;         
    }


    function fileValidation(file, uploadedFiles) {

        if(!file.type.startsWith('image/')){
            showError(`Файл "${file.name}" не является изображением.`);
            return false;
        }

        if (checkSize(file, MAX_SIZE_FILE)) {
            showError(`Файл "${file.name}" слишком большой (не более ${MAX_SIZE_FILE / (1024 * 1024)} МБ).`);
            return false;
        }

        const isDuplicate = uploadedFiles.some(f => f.file.name === file.name && f.file.size === file.size);
        if (isDuplicate) {
            showError(`Файл "${file.name}" уже добавлен.`);
            return false;
        }

        return true;

    }    



    function checkSize(data, limit){
        return data.length > limit;
    }

    function checkText(data){
        if(!patternText.test(data)){
            console.log("Не допустимые символы");
            return false;
        }
        return true;
    }

    return{
        checkTitle: checkTitle,
        checkDescription: checkDescription,
        checkRules: checkRules,
        checkCustomer: checkCustomer,
        checkFile: fileValidation
    }

}