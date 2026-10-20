export async function getData(endpoint ,params = null) {

    try{
        let url = formatUrl(endpoint, params);

        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if(response.status === 401 || response.status === 403){
            return {
                success: false,
                reason: 'unauthorized',
                message: 'Сессия устарела или недостаточно прав.'
            };
        }

        if(response.status === 200){
            let rawText;
            try{
                rawText = await response.text();
            }catch(e){
                return{
                    success: false,
                    reason: 'network_error',
                    message: 'Не удалось прочитать ответ сервера'
                }
            }

            let responseData;
            try{
                responseData = JSON.parse(rawText);
            }catch{
                responseData = rawText;
            }
            return{
                success: true,
                data: responseData
            };
        }

        return{
            success: false,
            reason: `server_error_${response.status}`,
            message: `Сервер вернул ошибку со статусом ${response.status}.`        
        };
    }catch(err){
        console.error('Сетевая ошибка при получении данных:', err);
        return{
            success: false,
            reason: 'network_error',
            message: 'Не удалось загрузить данные. Проверьте интернет-соединение.'

        };
    }
    
}


// Про запас
function formatUrl(endpoint ,params = null){

    if(!params || Object.keys(params).length === 0){
        return endpoint;
    }

    const searchParams = new URLSearchParams();
        
    Object.entries(params).forEach(([key, value]) => {
        if(value !== undefined && value !== null){
            searchParams.append(key, value);
        }
    });

    const queryString  = searchParams.toString();
    if(endpoint.includes('?')){
        return `${endpoint}&${queryString}`;
    }
    return `${endpoint}?${queryString}`;;
}


export function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}