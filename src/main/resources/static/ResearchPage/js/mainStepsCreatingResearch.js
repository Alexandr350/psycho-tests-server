import { setupModal } from "./modularWindows/createResearch/setupModal.js";
import { stepOne } from "./modularWindows/createResearch/stepOne.js";
import { stepTwo } from "./modularWindows/createResearch/stepTwo.js";
import { stepThree } from "./modularWindows/createResearch/stepThree.js";
import { stepFour } from "./modularWindows/createResearch/stepFour.js";
import { stepFive } from "./modularWindows/createResearch/stepFive.js";
import { validationInputData } from "../../utility/validationInputData.js";
import { normalizationStrings } from "../../utility/normalizationStrings.js";


document.addEventListener('DOMContentLoaded', () => {
    
    const modalWindow = document.getElementById('createResearchModal');
    const openBtnCreateResearchModal = document.getElementById('createResearch');
    const closeBtn = document.querySelector('.close-modal');
    const backBtn = document.getElementById('backBtn');
    const nextBtn = document.getElementById('nextBtn');
    const stepTitle = document.getElementById('stepTitle');
    const stepsArrayWindows = Array.from(document.querySelectorAll('.step-modal'));


    if(!modalWindow || !openBtnCreateResearchModal) {
        return;
    }

    const validation = validationInputData();
    const normalization = normalizationStrings();


    const oneWindow = stepOne(validation, normalization);
    const twoWindow = stepTwo(validation, normalization);
    const threeWindow = stepThree(validation, normalization);
    const fourWindow = stepFour(validation, normalization);
    const fiveWindow = stepFive(validation, normalization);

    const stepTitles = [
        "Шаг 1: Информация об исследовании",
        "Шаг 2: Информация об опросе",
        "Шаг 3: Добавьте характеристики",
        "Шаг 4: Добавьте исследуемые объекты",
        "Шаг 5: Добавьте свои объекты"
    ];

    const stepsElements = {
        MODAL_WINDOW: modalWindow,
        BTN_CREATE_RESEARCH: openBtnCreateResearchModal,
        BTN_CLOSE: closeBtn,
        BTN_BACK:  backBtn,
        BTN_NEXT: nextBtn,
        TITLE: stepTitle,
        ARRAY_WINDOWS: stepsArrayWindows
    };


    const stepsEndpoints =[
        '/api/researchs/research',
        '/api/researchs/survey',
        '/api/researchs/characteristics',
        '/api/researchs/items',
        '/api/researchs/items-sub',
    ];

    const stepActions = [
        oneWindow,
        twoWindow,
        threeWindow,
        fourWindow,
        fiveWindow
    ];



    setupModal( 
        stepsElements,
        stepTitles,
        stepsEndpoints,
        stepActions
    );


});



 








