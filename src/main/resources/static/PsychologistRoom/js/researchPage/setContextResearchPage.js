import { getData } from "../../../utility/getData.js";
import { renderResearchCard } from "./cardResearchMake.js";


export function setContext(headData, elements) {

    return {
        setDataHeader() {
            // Ошибка конфигурации, а не ожидаемое состояние: без данных
            // шапка будет пустой всегда, продолжать смысла нет.
            if (!headData) {
                throw new Error('setContext: headData не передан — заголовок не заполнен');
            }

            // Битый localStorage не должен ронять весь DOMContentLoaded:
            // конфигурацию не смогли прочитать — сообщаем об этом и останавливаемся.
            let data;
            try {
                data = JSON.parse(headData);
            } catch (err) {
                console.error('setContext: не удалось разобрать headData из localStorage', err);
                throw new Error('setContext: headData повреждён — заголовок не заполнен');
            }
            if (!data || typeof data.name !== 'string' || typeof data.lastname !== 'string' || typeof data.email !== 'string') {
                console.error('setContext: headData неполный — шапка не заполнена', { data });
                throw new Error('setContext: headData неполный — заголовок не заполнен');
            }

            elements.elNameAccount.textContent = `${data.name} ${data.lastname}`;
            elements.elEmailAccount.textContent = data.email;
        },

        async setResearchList(container) {
            // Отсутствие контейнера — ошибка разметки или вызывающего кода,
            // не ожидаемое состояние: молча пропускать нельзя.
            if (!container) {
                throw new Error('setResearchList: контейнер карточек не передан');
            }

            const load = async () => {
                container.innerHTML = '<div class="loading">Загрузка...</div>';
                try {
                    const result = await getData('/api/researchs/all');
                    if (!result.success) {
                        // Статичный текст в DOM — детали только в консоль:
                        // серверное сообщение не прогоняем через HTML.
                        console.error('setResearchList: сервер вернул ошибку', result.message);

                        // Сеть недоступна — сессия, возможно, жива: даём
                        // перезагрузить список на месте, без ухода со страницы.
                        if (result.reason === 'network_error') {
                            container.innerHTML = `
                                <div class="error">
                                    Нет связи с сервером.
                                    <button type="button" class="btn btn-secondary" id="retryLoadResearchs">Повторить</button>
                                </div>`;
                            container.querySelector('#retryLoadResearchs')
                                ?.addEventListener('click', load);
                            return;
                        }

                        container.innerHTML = '<div class="error">Не удалось загрузить список исследований</div>';
                        return;
                    }

                    container.innerHTML = '';
                    // Бэкенд отдаёт List<ResearchResponseInfoDto> — ждём массив.
                    // Фолбэк на result.data?.array убран: он маскировал расхождение
                    // в формате ответа вместо того, чтобы его показать.
                    const list = Array.isArray(result.data) ? result.data : [];
                    if (list.length === 0) {
                        container.innerHTML = '<div class="empty">Исследований пока нет — создайте первое</div>';
                        return;
                    }

                    // Сбой одной карточки не должен прятать остальные:
                    // логируем и продолжаем рендер.
                    list.forEach(item => {
                        try {
                            renderResearchCard(item, container);
                        } catch (err) {
                            console.error('setResearchList: карточка не отрисовалась', err, item);
                        }
                    });
                } catch (err) {
                    console.error('setResearchList: запрос списка не удался', err);
                    container.innerHTML = '<div class="error">Произошла ошибка при загрузке списка</div>';
                }
            };

            await load();
        }

    };

}
