/**
 * Гардиан авторизации. При мёртвой сессии чистит localStorage
 * и переводит на страницу входа. Флаг не даёт нескольким
 * одновременным запросам устроить несколько редиректов подряд.
 */
let redirecting = false;

export function redirectToAuth() {
    if (redirecting) return;
    redirecting = true;

    localStorage.removeItem('currentPsychlogist');
    // replace, а не href: страница логина не попадёт в историю,
    // «назад» вернёт туда, где человек был до редиректа
    window.location.replace('/AuthorizationPage/html/authorization.html');
}
