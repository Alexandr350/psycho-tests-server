import { showErrors } from "../../../utility/showErrors.js";


/**
 * Логика шага 1 мастера создания/редактирования исследования:
 * сбор данных с формы, их валидация, очистка и предзаполнение.
 *
 * <p>Файлы-инпуты берутся по id прямо из DOM при каждом вызове —
 * не кэшируются на момент создания фабрики. Это осознанное решение:
 * модальное окно живёт всё время работы страницы, но элементы внутри
 * шагов могут перерисовываться, и кэшированные ссылки рискуют стать
 * «мёртвыми». Цена — повторный поиск по id на каждый collectData,
 * что для формы из четырёх полей несущественно.</p>
 *
 * @param {Object} validation — утилита правил валидации (checkTitle, checkType и др.),
 *                              каждая возвращает массив сообщений об ошибках
 * @param {Object} normalization — утилита нормализации строк:
 *                              normalizeMonoSpace — схлопывает повторные пробелы (однострочные поля),
 *                              normalizeMultuSpace — то же для многострочного текста
 *                              (опечатка «Multu» из исходного кода сохранена —
 *                              переименование сломает вызывающий код)
 * @returns {Object} публичный API шага: collectData, validationData, clearFeildInputs, fillFields
 */
export function stepOne(validation, normalization){
   
    /**
     * Считывает текущее состояние формы и собирает из него два объекта.
     *
     * <p>dataSet повторяет поля ResearchRequestInfoDtoPart1 бэкенда
     * (nameResearch, customerResearch, descriptionResearch, typeResearch) —
     * имена подобраны так, чтобы объект уходил в sendData как есть, без
     * промежуточного маппинга. Строковые поля прогоняются через
     * нормализацию ДО валидации: правила вроде @NotBlank и ограничения
     * длины работают уже с финальным значением, которое уйдёт на сервер.</p>
     *
     * <p>elErrorsSet собирает пары «узел для текста ошибки + сам инпут» —
     * отдаётся наружу вместе с dataSet, потому что validationData и
     * clearFeildInputs работают с той же группой элементов.</p>
     *
     * @param {string} [idToPass] — id исследования/опроса для шагов 2–5;
     *                              шагом 1 игнорируется (совместимость
     *                              с единым контрактом stepActions)
     * @returns {{dataSet: Object, elErrorsSet: Object}} данные формы и ссылки на элементы ошибок
     */
    function collectData() {
        const elTitle = document.getElementById('inputTitleResearch');
        const elType = document.getElementById('inputTypeResearch');
        const elCustomer = document.getElementById('inputCustomerResearch');
        const elDescription = document.getElementById('inputDescriptionResearch');

        return {
            dataSet: {
                nameResearch: normalization.normalizeMonoSpace(elTitle.value),
                customerResearch: normalization.normalizeMonoSpace(elCustomer.value),
                descriptionResearch: normalization.normalizeMultuSpace(elDescription.value),
                typeResearch: elType.value
            },
            elErrorsSet: {
                
                textErrorTitle: document.getElementById('inputErrorTitleResearch'),
                textErrorType: document.getElementById('inputErrorTypeResearch'),
                textErrorCustomer: document.getElementById('inputErrorCustomerResearch'),
                textErrorDescription: document.getElementById('inputErrorDescriptionResearch'),
                
                inputTitle: elTitle,
                inputType: elType,
                inputCustomer: elCustomer,
                inputDescription: elDescription
            }
        };
    }

    /**
     * Прогоняет собранный dataSet по всем правилам валидации шага 1 и
     * показывает первую ошибку каждого поля рядом с ним (showErrors —
     * по одному сообщению на поле).
     *
     * <p>Правила: заголовок — обязательный + ограничения длины (бэкенд
     * держит name_research до 100 символов — фронт не должен пропускать
     * больше); тип — обязательный выбор из enum; заказчик — ограничения
     * длины (customer_research до 100); описание — ограничения длины
     * (description_research до 1000).</p>
     *
     * <p>Возвращает единый флаг валидности для всего шага: мастер
     * переходит дальше только при isValid === true, ошибки при этом
     * уже отрисованы на форме.</p>
     *
     * @param {Object} dataSet — результат collectData (уже нормализован)
     * @param {Object} elErrorsSet — элементы для отрисовки ошибок из collectData
     * @returns {boolean} true, если все четыре поля прошли проверки
     */
    function validationData(dataSet, elErrorsSet){
        let isValid = true;
        const titleErrors = validation.checkTitle(dataSet.nameResearch);
        showErrors(titleErrors[0], elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        if(titleErrors.length > 0) {
            isValid = false;
        }

        const typeErrors = validation.checkType(dataSet.typeResearch);
        showErrors(typeErrors[0], elErrorsSet.textErrorType, elErrorsSet.inputType);
        if(typeErrors.length > 0){
            isValid = false;
        }

        const customerErrors = validation.checkCustomer(dataSet.customerResearch);
        showErrors(customerErrors[0], elErrorsSet.textErrorCustomer, elErrorsSet.inputCustomer);
        if(customerErrors.length > 0){
            isValid = false;
        }

        const descriptionErrors = validation.checkDescription(dataSet.descriptionResearch);
        showErrors(descriptionErrors[0], elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);
        if(descriptionErrors.length > 0){
            isValid = false;
        }
        return isValid;

    }

    /**
     * Внутренняя функция очистки: обнуляет значения всех инпутов шага и
     * гасит уже показанные ошибки (showErrors(null, ...) снимает сообщение
     * и подсветку с каждого поля). Селект возвращается к первой опции.
     *
     * <p>Отдельная от clearFeildInputs потому, что принимает elErrorsSet
     * параметром: это позволяет вызвать очистку на уже собранном наборе
     * элементов без повторного опроса DOM.</p>
     *
     * @param {Object} elErrorsSet — набор элементов из collectData
     */
    function clearFeild(elErrorsSet){
        elErrorsSet.inputTitle.value = '';
        elErrorsSet.inputType.selectedIndex  = 0;
        elErrorsSet.inputCustomer.value = '';
        elErrorsSet.inputDescription.value = '';

        showErrors(null, elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        showErrors(null, elErrorsSet.textErrorType, elErrorsSet.inputType);
        showErrors(null, elErrorsSet.textErrorCustomer, elErrorsSet.inputCustomer);
        showErrors(null, elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);

    }


    /**
     * Публичная очистка шага: сама собирает elErrorsSet и передаёт
     * clearFeild. Вызывается извне (setupModal) при закрытии модалки,
     * сбросе мастера и после успешной отправки шага — так форма всегда
     * встречает следующий сценарий «с нуля».
     */
    function clearFeildInputs(){
        clearFeild(collectData().elErrorsSet);
    }

    /**
     * Предзаполнение формы данными существующего исследования — вход
     * в режим редактирования. data — ResearchResponseInfoDto из
     * GET /api/researchs/research/{id}: idResearch, nameResearch,
     * descriptionResearch, customerResearch, typeResearch, researchStatus.
     *
     * <p>Null у descriptionResearch/customerResearch — норма: колонки
     * в БД nullable, подставляем пустую строку. Перед записью значений
     * гасим возможные показанные ошибки — после открытий формы с
     * валидационной подсветкой поля должны встречать пользователя чисто.</p>
     *
     * <p>Вызывается только setupModal.openForEdit — в режиме создания
     * шаг остаётся пустым, и clearFeildInputs здесь не нужен.</p>
     *
     * @param {Object} data — ResearchResponseInfoDto текущего исследования
     */
    function fillFields(data){
        const elErrorsSet = collectData().elErrorsSet;

        elErrorsSet.inputTitle.value = data.nameResearch || '';
        elErrorsSet.inputType.value = data.typeResearch || '';
        elErrorsSet.inputCustomer.value = data.customerResearch || '';
        elErrorsSet.inputDescription.value = data.descriptionResearch || '';

        // на случай, если в открытой форме остались показанные ошибки
        showErrors(null, elErrorsSet.textErrorTitle, elErrorsSet.inputTitle);
        showErrors(null, elErrorsSet.textErrorType, elErrorsSet.inputType);
        showErrors(null, elErrorsSet.textErrorCustomer, elErrorsSet.inputCustomer);
        showErrors(null, elErrorsSet.textErrorDescription, elErrorsSet.inputDescription);
    }

     return{
            collectData,
            validationData,
            clearFeildInputs,
            fillFields
    };


}
