import { redirectToAuth } from './authGuard.js';

export async function getData(endpoint, params = null) {

    try {
        let url = formatUrl(endpoint, params);

        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (response.status === 401 || response.status === 403) {
            // сессия на сервере мертва (перезапуск, таймаут) — на логин.
            // Возврат объекта ниже нужен, чтобы вызывающий код не упал,
            // пока страница выгружается.
            redirectToAuth();
            return {
                success: false,
                reason: 'unauthorized',
                message: 'Сессия устарела или недостаточно прав.'
            };
        }

        if (response.status === 200) {
            let rawText;
            try {
                rawText = await response.text();
            } catch (e) {
                return {
                    success: false,
                    reason: 'network_error',
                    message: 'Не удалось прочитать ответ сервера'
                }
            }

            let responseData;
            try {
                responseData = JSON.parse(rawText);
            } catch {
                return {
                    success: false,
                    reason: 'invalid_response',
                    message: 'Сервер вернул ответ не в формате JSON.'
                };
            }
            return {
                success: true,
                data: responseData
            };
        }

        return {
            success: false,
            reason: `server_error_${response.status}`,
            message: `Сервер вернул ошибку со статусом ${response.status}.`
        };
    } catch (err) {
        console.error('Сетевая ошибка при получении данных:', err);
        return {
            success: false,
            reason: 'network_error',
            message: 'Не удалось загрузить данные. Проверьте интернет-соединение.'

        };
    }

}


// Про запас
function formatUrl(endpoint, params = null) {

    if (!params || Object.keys(params).length === 0) {
        return endpoint;
    }

    const searchParams = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
            searchParams.append(key, value);
        }
    });

    const queryString = searchParams.toString();
    if (endpoint.includes('?')) {
        return `${endpoint}&${queryString}`;
    }
    return `${endpoint}?${queryString}`;

}