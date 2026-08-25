import { setupModal } from "./modularWindows/createResearch/setupModal.js";
import { createFileUploadService } from "./modularWindows/createResearch/createFileUploadService.js";
import { stepOne } from "./modularWindows/createResearch/stepOne.js";
import { validationInputData } from "./modularWindows/createResearch/validationInputData.js";
import { normalizationStrings } from "./modularWindows/createResearch/normalizationStrings.js";


document.addEventListener('DOMContentLoaded', () => {
    //Модалка
    const modalWindow = document.getElementById('createResearchModal');
    const openBtnCreateResearchModal = document.getElementById('createResearch');
    const closeBtn = document.querySelector('.close-modal');
    const backBtn = document.getElementById('backBtn');
    const nextBtn = document.getElementById('nextBtn');
    const stepTitle = document.getElementById('stepTitle');
    const stepsArrayWindows = Array.from(document.querySelectorAll('.step-modal'));

    //дропзона 4 и 5 шаг
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const previewContainer = document.getElementById('previewContainer');

    const dropZoneSelf = document.getElementById('dropZoneSelf');
    const fileInputSelf = document.getElementById('fileInputSelf');
    const previewContainerSelf = document.getElementById('previewContainerSelf');

    if(!modalWindow || !openBtnCreateResearchModal) {
        return;
    }

    const validation = validationInputData();
    const normalization = normalizationStrings();

    const serviceUploadImage = createFileUploadService(dropZone, fileInput, previewContainer, validation);
    const serviceUploadImageSelf = createFileUploadService(dropZoneSelf, fileInputSelf, previewContainerSelf, validation);  
      


    const oneWindow = stepOne(validation, normalization);

    const stepTitles = [
        "Шаг 1: Информация об исследовании",
        "Шаг 2: Информация об опросе",
        "Шаг 3: Добавьте характеристики",
        "Шаг 4: Добавьте исследуемые объекты",
        "Шаг 5: Добавьте свои объекты"
    ]

    const stepsElements = {
        MODAL_WINDOW: modalWindow,
        BTN_CREATE_RESEARCH: openBtnCreateResearchModal,
        BTN_CLOSE: closeBtn,
        BTN_BACK:  backBtn,
        BTN_NEXT: nextBtn,
        TITLE: stepTitle,
        ARRAY_WINDOWS: stepsArrayWindows
    }


    const stepsEndpoints =[
        '/api/researchs/research',
        '/api/researchs/survey',
        '/api/researchs/characteristics',
        '/api/researchs/items',
        '/api/researchs/items-sub',
    ]

    const stepActions = [
        oneWindow
    ]

    const services = [
        serviceUploadImage,
        serviceUploadImageSelf
    ]


    setupModal( 
        stepsElements,
        stepTitles,
        services,
        stepsEndpoints,
        stepActions
    );


});



 








