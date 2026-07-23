document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.sector').forEach(sector => {
        sector.addEventListener('click', async (e) => {
            const sectorValue = e.currentTarget.dataset.info;

            const res = await fetch('/api/move', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({numberSector: Number(sectorValue)})
            })

            if(!res.ok) {
                console.warn('Сервер вернул ошибку:', res.status);
                return;
            }
            const data = await res.json();

            console.log("Путь: ", data.imageUrl);
            
            const imageWrapper  = document.querySelector('.image-wrapper');
            if(!imageWrapper) {
                console.error("Элемент .image-wrapper не найден");
                return;
            }
            imageWrapper.innerHTML = `<img src="${data.imageUrl}">`;
        });
    });
});






