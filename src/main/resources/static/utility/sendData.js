export async function sendData(payload, endpoint) {
    try {

        const isFormData = payload instanceof FormData;
        const response = await fetch(endpoint, {
            method: 'POST',
            credentials: 'include',
            headers: isFormData ? {} : {
                'Content-Type': 'application/json'
            },
            body: isFormData ? payload : JSON.stringify(payload)
        });

        if (response.status === 400 || response.status === 422) {
            const errorData = await response.json().catch(() => ({}));
            return { 
                success: false, 
                reason: 'validation_failed', 
                errors: errorData.errors || 'Ошибка валидации на стороне сервера' 
            };
        }        
     
        if (response.status === 204) {
            return { success: true, data: null, message: 'Данные успешно сохранены' };
        }

        if(response.status === 200 || response.status === 201){
            const rawText = await response.text().catch(() => "");
            let responseData;
            try{
                responseData = JSON.parse(rawText);
                
            } catch{
                responseData = rawText;
            }

            return {
                success: true,
                data: responseData
            }
        }

        
        return {
            success: false,
            reason: `server_error_${response.status}`,
            message: `Сервер вернул ошибку со статусом ${response.status}.`
        };
       
    } catch (error) {
        console.error('Сетевая ошибка при отправке данных:', error);
        return { 
            success: false, 
            reason: 'network_error', 
            message: 'Не удалось связаться с сервером. Проверьте интернет-соединение.' 
        };
    }
}
