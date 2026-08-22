export function createFileUploadService(dropZone, fileInput, previewContainer){
    let uploadedFiles = [];
    const MAX_SIZE = 30 * 1024 * 1024;
   
    dropZone.addEventListener('click', (e) => {
        if (e.target.closest('.preview-container') 
            || e.target.closest('.preview-item') 
            || e.target.closest('.remove-img-btn')) {
            return;
        }
        fileInput.click();
    });

    fileInput.addEventListener('change', (e) => {
        if(e.target.files.length > 0){
            addFiles(e.target.files);
        }
        

    });

    function fileValidation(file) {

        if(!file.type.startsWith('image/')){
            showError(`Файл "${file.name}" не является изображением.`);
            return false;
        }

        if (file.size > MAX_SIZE) {
            showError(`Файл "${file.name}" слишком большой (не более ${MAX_SIZE / (1024 * 1024)} МБ).`);
            return false;
        }

        const isDuplicate = uploadedFiles.some(f => f.file.name === file.name && f.file.size === file.size);
        if (isDuplicate) {
            showError(`Файл "${file.name}" уже добавлен.`);
            return false;
        }

        return true;

    }

    function addFiles(filesList) {
        const files = Array.from(filesList);
        let hasError = false;

        files.forEach(file => {
            if (!fileValidation(file)) {
                return;
            }

            const url = URL.createObjectURL(file);
            uploadedFiles.push({ file, url });
           
        });

      
        renderPreviews(uploadedFiles, previewContainer);
        fileInput.value = '';
        return !hasError;
    }

    function dragAndDrop(){
        if (!dropZone) {
        console.error('Элемент #drop-zone не найден');
        return;
        }
        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.add('dragover'); 
        });


        dropZone.addEventListener('dragleave', (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.remove('dragover');
        });

        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.remove('dragover');

            addFiles(e.dataTransfer.files);
        });

    }


    function showError(msg) {
        if (!dropZone) return;

        let el = document.querySelector('.drop-zone-error');
        if (!el) {
            el = document.createElement('div');
            el.className = 'drop-zone-error';
            el.style.cssText = `
            color: #d32f2f; background: #ffebee; padding: 10px 12px;
            border-radius: 4px; margin-top: 8px; font-size: 0.875rem;
            line-height: 1.4; display: none;
            `;
            dropZone.appendChild(el);
        }
        el.textContent = msg;
        el.style.display = 'block';
        setTimeout(() => el.style.display = 'none', 2000);
    }
    
    function updateDropZoneVisibility(){
        if(!dropZone) return;
        dropZone.classList.toggle('has-files', uploadedFiles.length > 0);
    }

    function renderPreviews(files, container){
        container.replaceChildren();

        if(files.length === 0) {
            updateDropZoneVisibility();
            return;
        }

        files.forEach((item, idx) => {
            const previewItem = document.createElement('div');
            previewItem.className = 'preview-item';

            const previewImage = document.createElement('img');
            previewImage.src = item.url;
            previewImage.alt = item.file.name;
            previewImage.className = 'preview-image';

            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'remove-img-btn';
            removeBtn.textContent = 'x';
            removeBtn.dataset.idx = idx;

            removeBtn.onclick = (e) => {
                e.stopPropagation();
                URL.revokeObjectURL(item.url);
                uploadedFiles.splice(idx, 1);
                renderPreviews(uploadedFiles, previewContainer);
            }

            previewItem.appendChild(previewImage);
            previewItem.appendChild(removeBtn);
            container.appendChild(previewItem);
            
            
        });
        
    }

    updateDropZoneVisibility();
    dragAndDrop();
    return {
        getFiles: () => [...uploadedFiles].map(item => item.file),
        clearFiles: () => {
            uploadedFiles.forEach(item => URL.revokeObjectURL(item.url));
            uploadedFiles.length = 0;
            renderPreviews([], previewContainer);
        },
        removeFile: (index) =>{
            URL.revokeObjectURL(item.url);
            uploadedFiles.splice(index, 1);
            renderPreviews(uploadedFiles, previewContainer);
        },

        addFiles: addFiles
    };

}