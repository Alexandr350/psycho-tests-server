

const FIELD_LIMITS = {
    title: 100,
    customer: 100,
    description: 1000,
    rules: 2000,
    characteristics: 800,
    nameAccount: 20,
    lastnameAccount: 50,
    emailAccount: 100,
    passwordAccount: 100
};

const MAX_SIZE_FILE = 30 * 1024 * 1024;

const patternText = /^[А-Яа-яЁёA-Za-z0-9_\-.,\s]+$/;
const patternTextExec = /[^А-Яа-яЁёA-Za-z0-9_\-.,\s]/;
const patternEmail = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const patternEmailExec =  /[^a-zA-Z0-9._%+-@.]/;
const patternPassword = /^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@$!%*?&\-])[A-Za-z0-9@$!%*?&\-]{8,}$/;
const patternPasswordExec = /[^A-Za-z0-9@$!%*?&\-]/;

function checkSize(data, limit){
    return data.length > limit;
}

function checkText(data, valid, notValid){
    if(valid.test(data)) return null;
    const match = notValid.exec(data);
    return match ? match[0] : null; 
}


export function validationInputData(){

    function checkType(data){
        const errors = [];
        if (!data || data.length === 0) {
            errors.push('Выберите тип исследования');
        } 
        return errors;
    }

    function checkField(data, fieldName, validField, notValidField){
        const errors =[];
        if(!data) return errors;

        const badChar = checkText(data, validField, notValidField);
        if(badChar) {
            errors.push(`Недопустимый символ: "${badChar}"`);
        } else {
            const limit = FIELD_LIMITS[fieldName];
            if(limit !== undefined && checkSize(data, limit)){
                errors.push(`Превышена длина поля. Максимум: ${limit}`);
            }
        }
        return errors;         
    }




    function checkNameAccount(data){
        if(!data || data.length === 0){
            return ['Поле не может быть пустым'];
        }
        return checkField(data, 'nameAccount', patternText, patternTextExec);
    }
    
    function checkLastnameAccount(data){
        if(!data || data.length === 0){
            return ['Поле не может быть пустым'];
        }
        return checkField(data, 'lastnameAccount', patternText, patternTextExec);
    }

    function checkEmailAccount(data){
        if(!data || data.length === 0){
            return ['Поле не может быть пустым'];
        }
        return checkField(data, 'emailAccount', patternEmail, patternEmailExec);
    }

    function checkPasswordAccount(data){
        if(!data || data.length === 0){
            return ['Поле не может быть пустым'];
        }
        if(data.length < 8){
            return ['Длина должна быть минимум 8 символов'];
        }
        return checkField(data, 'passwordAccount', patternPassword, patternPasswordExec);
    }

    function checkPasswordRepeat(passOne, passTwo) {
        if(passOne !== passTwo) {
            return ['Пароль не совпадает'];
        }
        return [];
    }





    function checkTitle(data){
        if(!data || data.length === 0){
            return ['Поле не может быть пустым'];
        }
        return checkField(data, 'title', patternText, patternTextExec);
    }

    function checkCustomer(data){
        return checkField(data, 'customer', patternText, patternTextExec);
    }    


    function checkDescription(data){
        return checkField(data, 'description', patternText, patternTextExec);
    }


    function checkCharacteristics(data){
    if(!data || data.length === 0){
        return['Поле не может быть пустым'];
    }
    return checkField(data, 'characteristics', patternText, patternTextExec);
    }


    function checkRules(data){
        if(!data || data.length === 0){
            return['Поле не может быть пустым'];
        }
        return checkField(data, 'rules', patternText, patternTextExec);
    }


    function fileValidation(file, uploadedFiles) {


        const errors =[];
        if(!file) return errors;

        if(!file.type.startsWith('image/')){ 
            errors.push(`Файл "${file.name}" не является изображением.`);
        }

        if (file.size > MAX_SIZE_FILE) {
            errors.push(`Файл "${file.name}" слишком большой (не более ${MAX_SIZE_FILE / (1024 * 1024)} МБ).`);
        }

        const isDuplicate = uploadedFiles.some(f => f.file.name === file.name && f.file.size === file.size);
        if (isDuplicate) {
             errors.push(`Файл "${file.name}" уже добавлен.`);
            
        }
        return errors;

    }    



    return{
        checkNameAccount,
        checkLastnameAccount,
        checkEmailAccount,
        checkPasswordAccount,
        checkPasswordRepeat,
        checkTitle,
        checkType,
        checkDescription,
        checkRules,
        checkCustomer,
        checkCharacteristics,
        checkFile: fileValidation
    };

}