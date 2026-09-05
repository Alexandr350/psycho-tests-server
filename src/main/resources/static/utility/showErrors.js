export function showErrors(msg, feildError, feildEl){
     const hasErrors = msg && (Array.isArray(msg) ? msg.length > 0 : String(msg).trim().length > 0);
    
    if (hasErrors) {
    
        feildError.innerHTML = msg;
        feildError.classList.add('visible');

    
        if (feildEl){
            feildEl.classList.remove('success');
            feildEl.classList.add('error');
        } 

    } else {

        feildError.textContent = '';
        feildError.classList.remove('visible');
        if (feildEl) {
            feildEl.classList.remove('error');

            if(feildEl.value && feildEl.value.trim().length > 0){
                feildEl.classList.add('success');
            }else{
                feildEl.classList.remove('success');
            }
        }
    }
} 



