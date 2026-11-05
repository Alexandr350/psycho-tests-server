import { sendData } from "../../../utility/sendData.js";

/**
 * Контроллер мастера создания/редактирования исследования: управляет
 * шагами модального окна, отправкой данных каждого шага и входом
 * в режим редактирования существующего исследования.
 *
 * <p>Режимы: создание (все 5 шагов) и редактирование. В редактировании
 * у пройденных шагов активные id (activeResearchId/activeSurveyId) —
 * тогда шаг отправляется PATCH-ом по существующему ресурсу, а не POST-ом,
 * иначе БД честно отвечает нарушением уникального ключа research_id_survey.</p>
 *
 * <p>Состояние мастера живёт в замыкании. Поля форм после успешной отправки
 * шага НЕ очищаются: данные актуальны, и возврат «назад» должен показывать
 * их как введённые. Полная очистка — только при закрытии модалки
 * (closeModalWindow) и повторном «Создать».</p>
 *
 * <p>Список карточек — единственный источник правды о составе: при любом
 * закрытии мастера вызывается колбэк onClosed, который перезагружает
 * список с сервера. Прямая отрисовка карточки из ответов шагов не ведётся.</p>
 *
 * @param {Object} stepsElements — DOM-узлы мастера: MODAL_WINDOW,
 *        BTN_CREATE_RESEARCH, BTN_CLOSE, BTN_BACK, BTN_NEXT, TITLE,
 *        ARRAY_WINDOWS (массив шагов)
 * @param {string[]} stepTitles — заголовки шагов для TITLE
 * @param {string[]} stepsEndpoints — базовый эндпоинт каждого шага
 * @param {Object[]} stepActions — API каждого шага:
 *        collectData / validationData / clearFeildInputs / fillFields
 * @param {Element} cardsContainer — контейнер списка карточек
 * @param {Function} [onClosed] — колбэк перезагрузки списка после закрытия
 * @returns {Object} публичный API: { openForEdit }
 */
export function setupModal(
    stepsElements,
    stepTitles,
    stepsEndpoints,
    stepActions,
    onClosed
){

    let currentStepIndex = 0;    // активный шаг мастера (0–4)
    let activeResearchId = null; // id исследования, созданного на шаге 1
    let activeSurveyId  = null;  // id опроса, созданного на шаге 2
    let editMode = false;        // true — мастер открыт в режиме редактирования
    let editStatus = null;       // статус исследования на входе в редактирование: DRAFT | NEW

    /**
     * Показывает шаг по индексу: скрывает все окна (снимая класс active)
     * и активирует нужное; обновляет заголовок TITLE, если для шага
     * он задан. Не трогает кнопку «Далее» — за неё отвечает updateNextBtn.
     */
    function showStep(index){
        stepsElements.ARRAY_WINDOWS.forEach((step, i) => {
            step.classList.toggle('active', i === index);
        });

        if(stepTitles[index]){
            stepsElements.TITLE.textContent = stepTitles[index];
        }
    }

    /**
     * Синхронизирует кнопку «Далее» с контекстом мастера:
     * - редактирование NEW — «Сохранить» (единственный шаг, финальное действие);
     * - последний шаг создания — «Создать» + подсветка btn-success;
     * - остальные — «Далее» без подсветки.
     */
    function updateNextBtn() {
        if(editMode && editStatus === 'NEW'){
            stepsElements.BTN_NEXT.textContent = 'Сохранить';
            stepsElements.BTN_NEXT.classList.add('btn-success');
        }else if(currentStepIndex === stepsElements.ARRAY_WINDOWS.length - 1){
            stepsElements.BTN_NEXT.textContent = 'Создать';
            stepsElements.BTN_NEXT.classList.add('btn-success');
        }else{
            stepsElements.BTN_NEXT.textContent = 'Далее';
            stepsElements.BTN_NEXT.classList.remove('btn-success');
        }
    }

    /**
     * Вычисляет шаг мастера, с которого продолжать черновик:
     * резюм = первый незаполненный шаг. Шаг 1 пройден всегда
     * (исследование существует), поэтому его индекс не возвращается никогда.
     * Поле idSurvey заполняется GET /api/researchs/research/{id}
     * (ResearchResponseInfoDto с опросными полями).
     */
    function determineResumeStep(data) {
        if (!data.idSurvey) return 1;                                  // шаг 2
        const chars = Array.isArray(data.characteristics) ? data.characteristics : [];
        if (chars.length === 0) return 2;                              // шаг 3
        const items = Array.isArray(data.items) ? data.items : [];
        if (items.length === 0) return 3;                              // шаг 4
        return 4;                                                      // шаг 5
    }

    /**
     * Вход в режим редактирования — единственная точка извне
     * (вызывается actionBtnList через onEditReady).
     *
     * <p>researchData — ResearchResponseInfoDto из GET /api/researchs/research/{id}.
     * editStatus запоминает статус на входе: для NEW редактирование ограничено
     * шагом 1 (опроса у NEW нет), для DRAFT мастер продолжается с первого
     * незаполненного шага, а пройденные шаги предзаполняются и отправляют
     * PATCH по существующим id.</p>
     *
     * @param {Object} researchData — ResearchResponseInfoDto текущего исследования
     */
    function openForEdit(researchData){
        editMode = true;
        editStatus = researchData.researchStatus;
        activeResearchId = researchData.idResearch;
        activeSurveyId = researchData.idSurvey ?? null;

        stepActions[0].fillFields(researchData);

        if (editStatus === 'NEW') {
            // у NEW опроса нет — правка шапки с первого шага
            currentStepIndex = 0;
        } else {
            // DRAFT — продолжаем с места остановки
            currentStepIndex = determineResumeStep(researchData);
            if (researchData.idSurvey) {
                stepActions[1].fillFields(researchData);
                stepActions[2].fillFields(researchData);
            }
        }

        updateNextBtn();
        showStep(currentStepIndex);
        stepsElements.MODAL_WINDOW.classList.add('active');
    }

    /**
     * Полный сброс мастера к исходному состоянию: очистка всех форм
     * шагов (clearFeildInputs каждого action), сброс id и указателя шага,
     * обновление кнопки и отрисовка первого шага.
     */
    function clearFeilds(){
        stepActions.forEach(i => i.clearFeildInputs());
        currentStepIndex = 0;
        activeResearchId = null;
        activeSurveyId = null;
        updateNextBtn();
        showStep(currentStepIndex);
    }

    /**
     * Закрытие модалки со сбросом: окно скрывается, состояние мастера
     * обнуляется, флаги редактирования снимаются. Снимаются именно здесь,
     * а не только в clearFeilds: иначе после редактирования следующее
     * «Создать» отправило бы PATCH вместо POST.
     *
     * <p>Сюда сходят все пути закрытия: крестик, клик по затемнению,
     * «Назад» с нулевого шага, завершение мастера на последнем шаге
     * и финал редактирования NEW. После сброса вызывается onClosed —
     * список карточек перезагружается с сервера.</p>
     */
    function closeModalWindow(){
        stepsElements.MODAL_WINDOW.classList.remove('active');
        clearFeilds();
        editMode = false;
        editStatus = null;

        // список перезагружаем при любом закрытии: черновик мог быть создан
        // на шаге 1, исследование — отредактировано PATCH-ами. Сервер —
        // единственный источник правды о составе карточек
        if (typeof onClosed === 'function') onClosed();
    }


    /**
     * Открытие мастера кнопкой «Создать» — через делегирование:
     * кнопка с id="createResearch" пересоздаётся при переходах между
     * вью (главная/список), прямая привязка на узле умирала бы
     * после первой навигации.
     */
    document.addEventListener('click', (e) => {
        if (e.target.closest('#createResearch')) {
            editMode = false;
            editStatus = null;
            clearFeilds();
            stepsElements.MODAL_WINDOW.classList.add('active');
        }
    });

    /** Крестик модалки — штатное закрытие со сбросом. */
    stepsElements.BTN_CLOSE.addEventListener('click', () => {
        closeModalWindow();
    });

    /**
     * Клик по затемнению вокруг модалки — тоже закрытие со сбросом.
     * Проверка e.target === MODAL_WINDOW отсеивает клики по содержимому.
     */
    window.addEventListener('click', (e) =>{
        if(e.target === stepsElements.MODAL_WINDOW) {
            closeModalWindow();
        }
    });

    /**
     * «Назад»: в редактировании NEW — закрытие (шаг один, идти некуда);
     * иначе — откат шага. На нулевом шаге «Назад» закрывает модалку.
     * Поля шагов НЕ очищаются: возврат должен показывать данные
     * как введённые.
     */
    stepsElements.BTN_BACK.addEventListener('click', () => {
        if(editMode && editStatus === 'NEW'){
            closeModalWindow();
            return;
        }
        if(currentStepIndex > 0){
            currentStepIndex--;
            updateNextBtn();   // подпись и подсветку считает та же функция, что и вперёд
            showStep(currentStepIndex);
        }else{
            closeModalWindow();
        }
    });

    /**
     * Главная кнопка мастера — отправка текущего шага.
     *
     * <p>Выбор метода: если у шага уже есть активный id (возврат назад
     * или резюме черновика) — PATCH по существующему ресурсу вместо POST.
     * Шаг 1 — PATCH /research/{idResearch}, шаг 2 — PATCH /survey/{idSurvey}.
     * Повторный POST на существующий опрос упирается в уникальный ключ
     * research_id_survey (409 от бэкенда) — защитник снимает этот класс
     * ошибок целиком.</p>
     *
     * <p>Разбор результата: шаг 1 сохраняет id исследования, шаг 2 — id
     * опроса; существующие id пустотой не затираются. Последний шаг
     * закрывает мастер, список перезагружается в closeModalWindow
     * через onClosed.</p>
     *
     * <p>Отказы: серверная валидация (validation_failed) показывается
     * alert-ом и в консоль, прочие ошибки — общий alert. Кнопка
     * разблокируется в finally — при любом исходе.</p>
     */
    stepsElements.BTN_NEXT.addEventListener('click', async () => {
        if(stepsElements.BTN_NEXT.disabled){
            return; // запрос уже в полёте — защита от двойного клика
        }

        if(currentStepIndex >= stepsElements.ARRAY_WINDOWS.length){
            closeModalWindow();
            return;
        }

        const action = stepActions[currentStepIndex];
        let endpoint = stepsEndpoints[currentStepIndex];
        let requestMethod = 'POST';

        // шаг 1: исследование уже существует (резюме/возврат назад) — PATCH
        if(currentStepIndex === 0 && activeResearchId){
            endpoint = `/api/researchs/research/${activeResearchId}`;
            requestMethod = 'PATCH';
        }
        // шаг 2: опрос уже существует — PATCH, не POST (иначе 409 по UK)
        if(currentStepIndex === 1 && activeSurveyId){
            endpoint = `/api/researchs/survey/${activeSurveyId}`;
            requestMethod = 'PATCH';
        }

        if(!action || !endpoint){
            goToNextStep();
            return;
        }

        // id для шагов-зависимостей: шаг 2 — id исследования,
        // шаги 3–5 — id опроса (contract: idToPass -> collectData)
        let idToPass = null;
        if (currentStepIndex === 1) {
            idToPass = activeResearchId;
        } else if (currentStepIndex >= 2) {
            idToPass = activeSurveyId;
        }

        let isStepValid = true;
        const {dataSet, elErrorsSet} = action.collectData(idToPass);

        isStepValid = action.validationData(dataSet, elErrorsSet);

        if(!isStepValid){
            console.warn(`Валидация шага ${currentStepIndex} не пройдена.`);
            return;
        }

        stepsElements.BTN_NEXT.disabled = true;

        try{
            const result = await sendData(dataSet, endpoint, requestMethod);

            if(result.success){

                if(currentStepIndex === 0){
                    // id затираем только если сервер вернул новый
                    const newId = (result.data && typeof result.data === 'object')
                        ? result.data.idResearch
                        : result.data;
                    if(newId){
                        activeResearchId = newId;
                    }

                    if(editMode && editStatus === 'NEW'){
                        // редактирование NEW закончено: закрыть мастер
                        closeModalWindow();
                        return;
                    }
                }

                if(currentStepIndex === 1){
                    const newId = (result.data && typeof result.data === 'object')
                        ? result.data.idSurvey
                        : result.data;
                    if(newId){
                        activeSurveyId = newId;
                    }
                }

                // последний шаг мастера: закрываем окно, список перезагрузится
                // в closeModalWindow через onClosed
                if(currentStepIndex === stepsElements.ARRAY_WINDOWS.length - 1){
                    closeModalWindow();
                }else{
                    currentStepIndex++;
                    updateNextBtn();
                    showStep(currentStepIndex);
                }

            }else{
                if (result.reason === 'validation_failed') {
                    console.warn(result.errors);
                    alert(typeof result.errors === 'string'
                        ? result.errors
                        : 'Сервер отклонил данные: проверьте заполнение полей');
                }else{
                    alert(result.message || 'Не удалось сохранить данные. Попробуйте позже.');
                }
            }
        }catch(e){
            console.error('Критический сбой отправки: ', e);
            alert('Не удалось сохранить данные. Попробуйте позже.');
        }finally{
            stepsElements.BTN_NEXT.disabled = false;
        }
    });

    return {
        openForEdit
    };
}
