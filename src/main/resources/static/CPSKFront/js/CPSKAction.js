document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll('.sector').forEach(sector => {
    sector.addEventListener('click', async (e) => {
      const sectorValue = e.currentTarget.dataset.info;

      try {
        const res = await fetch('/api/move', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ numberSector: Number(sectorValue) })
        });

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }

        const data = await res.json();
        console.log('Путь:', data.imageUrl);

        const imageWrapper = document.querySelector('.image-wrapper');
        if (!imageWrapper) {
          console.error('Элемент .image-wrapper не найден');
          return;
        }

        // Создаём img правильно, без innerHTML (защита от XSS)
        const img = document.createElement('img');
        img.src = data.imageUrl;
        img.className = 'display-image';
        img.alt = 'Картинка не загрузилась';

        // Удаляем плейсхолдер, если он есть
        const placeholder = imageWrapper.querySelector('.image-placeholder');
        if (placeholder) {
          placeholder.remove();
        }

        imageWrapper.innerHTML = ''; // очищаем контейнер
        imageWrapper.appendChild(img);
      } catch (err) {
        console.error(err);
        const imageWrapper = document.querySelector('.image-wrapper');
        if (imageWrapper) {
          imageWrapper.innerHTML = '<span class="error">Ошибка загрузки изображения</span>';
        }
      }
    });
  });
});
