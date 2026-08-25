

export function stepOne(validation, normalization){
   
    function validateAndCorrect(){
        const inputTitleResearch = document.getElementById('inputTitleResearch').value;
        const inputTypeResearch = document.getElementById('inputTypeResearch').value;
        const inputCustomerResearch = document.getElementById('inputCustomerResearch').value;
        const inputDescriptionResearch = document.getElementById('inputDescriptionResearch').value;

        const cleanTitle = normalization.normalizeMonoSpace(inputTitleResearch);
        const cleanCustomer = normalization.normalizeMonoSpace(inputCustomerResearch);
        const cleanDescription = normalization.normalizeMultuSpace(inputDescriptionResearch);

        if(!validation.checkTitle(cleanTitle)) return false;
        if(!validation.checkCustomer(cleanCustomer)) return false;
        if(!validation.checkDescription(cleanDescription)) return false;

        const dataSet = {
            title: cleanTitle,
            customer: cleanCustomer,
            description: cleanDescription,
            type: inputTypeResearch
        }
        
        return {
            dataSet: dataSet
        }


       


    }

     return{
            validateAndCorrect: validateAndCorrect
    }


}