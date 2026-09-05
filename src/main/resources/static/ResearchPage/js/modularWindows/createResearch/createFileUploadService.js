import { showErrors } from "../../../../utility/showErrors.js";

export function createFileUploadService(dropZone, fileInput, previewContainer, validation){
    let uploadedFiles = [];
    let currentErrors = [];

   
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
            currentErrors = addFiles(e.target.files);

            const errorEl = document.getElementById('inputErrorItem');
            if(currentErrors.length > 0){
                showErrors(currentErrors.join('br'), errorEl, dropZone);
            }else{
                showErrors(null, errorEl, dropZone);
            }
            currentErrors = [];
        }
        

    });


    function addFiles(filesList) {
        const files = Array.from(filesList);
        const allErrors = [];

        files.forEach(file => {
            
            const fileErrors = validation.checkFile(file, uploadedFiles);

            if (fileErrors && fileErrors.length > 0) {
                allErrors.push(...fileErrors);
                return;
            }

            const url = URL.createObjectURL(file);
            uploadedFiles.push({ file, url });
           
        });

      
        renderPreviews(uploadedFiles, previewContainer);
        fileInput.value = '';
        return allErrors;
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

            currentErrors = addFiles(e.dataTransfer.files);

            const errorEl = document.getElementById('inputErrorItem');
            if(currentErrors.length > 0){
                showErrors(currentErrors.join('<br>'), errorEl, dropZone);
            }else{
                showErrors(null, errorEl, dropZone);
            }            
            currentErrors = [];
        });

    }


    
    function updateDropZoneVisibility(){
        if(!dropZone) return;
        dropZone.classList.toggle('has-files', uploadedFiles.length > 0);
    }


    function removeFileByIndex(index){
        if(index < 0 || index >= uploadedFiles.length) return;

        URL.revokeObjectURL(uploadedFiles[index].url);
        uploadedFiles.splice(index, 1);
        renderPreviews(uploadedFiles, previewContainer);
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
                removeFileByIndex(idx);
            }

            previewItem.appendChild(previewImage);
            previewItem.appendChild(removeBtn);
            container.appendChild(previewItem);
            
            
        });
        
    }

    updateDropZoneVisibility();
    dragAndDrop();

    return {
        getFiles: () => uploadedFiles.map(item => item.file),
        clearFiles: () => {
            uploadedFiles.forEach(item => URL.revokeObjectURL(item.url));
            uploadedFiles.length = 0;
            renderPreviews([], previewContainer);
        },
        removeFile: (index) =>{
            removeFileByIndex(index);
        },

        getErrors: () => currentErrors,
        addFiles: addFiles,
        
    };

}