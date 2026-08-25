export async function sendData(action, endpoint) {

    const validationResult = action.validateAndCorrect();

    if(!validationResult) {
        console.warn('Валидация не пройдена, отправка отменена', validationResult);
        return { success: false, reason: 'validation_failed' };
    }

    const payload = validationResult.dataSet;
    try{
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });



        if(response.status === 204){
            return { success: true, data: null, message: 'Данные успешно сохранены' };
            
        }
       
    }catch(error){
        console.error('Отправка данных не удалась:', error);
        throw error;
    }
} 