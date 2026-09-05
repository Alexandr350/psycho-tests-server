import { validationInputData } from "../../utility/validationInputData.js";
import { normalizationStrings } from "../../utility/normalizationStrings.js";
import { serviceRegistration } from "./registration.js";
import { serviceLogin } from "./login.js";
import { setupAuthorizarion } from "./setupAuthorization.js";


document.addEventListener('DOMContentLoaded', () => {

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





