import { validationInputData } from "../../utility/validationInputData.js";
import { normalizationStrings } from "../../utility/normalizationStrings.js";
import { serviceRegistration } from "./registration.js";
import { serviceLogin } from "./login.js";
import { setupAuthorizarion } from "./setupAuthorization.js";


document.addEventListener('DOMContentLoaded', async () => {

  // Уже вошедший психолог не должен видеть форму логина:
  // проверяем живую сессию и возвращаем в кабинет.
  const saved = localStorage.getItem('currentPsychlogist');
  if (saved) {
    // Проверка сырым fetch, а не getData: getData при 401 сам
    // дернет redirectToAuth, а нам нужен тихий ответ без редиректа.
    try {
      const probe = await fetch('/api/researchs/all', { credentials: 'include' });

      if (probe.ok) {
        window.location.replace(
          sessionStorage.getItem('lastCabinetPage') ||
          '/PsychologistRoom/html/PsychologistRoom.html'
        );
        return; // формы не инициализируем
      }
      if (probe.status === 401 || probe.status === 403) {
        localStorage.removeItem('currentPsychlogist'); // сессия мертва — чистим
      }
      // любой другой статус — показываем форму, ничего не трогаем
    } catch (e) {
      // сервер недоступен — остаёмся на форме, ничего не чистим
    }
  }

  const btnRegistration = document.getElementById('btn-registration');
  const btnLogin = document.getElementById('btn-login');


  const switchBtns = document.querySelectorAll('.switch-btn');
  const forms = document.querySelectorAll('.auth-form');
  const toggleBtns = document.querySelectorAll('.toggle-password-btn');

  const validation = validationInputData();
  const normalization = normalizationStrings();

  const registration = serviceRegistration(validation, normalization);
  const login = serviceLogin(validation, normalization);

  const serviciesAuth = {
    registration: registration,
    login: login
  };

  const elAuthSet = {
    BTN_REGISTRATION: btnRegistration,
    BTN_LOGIN: btnLogin,
    FORMS: forms,
    BTN_SWITCH: switchBtns,
    BTN_TOGGLE_EYE: toggleBtns

  };

  setupAuthorizarion(elAuthSet, serviciesAuth);

});
