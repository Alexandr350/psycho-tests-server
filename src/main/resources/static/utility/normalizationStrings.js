export function normalizationStrings(){
    
    function normalizeMonoSpace(raw){
        const str = String(raw ?? '');
        return str.replace(/\u00A0/g, ' ')
                .trim()
                .replace(/\s+/g, ' ')
                .normalize('NFC');
    }          

    function normalizeMultuSpace(raw) {
        const str = String(raw ?? '');
        return str
            .replace(/\u00A0/g, ' ')
            .trim();
    }

    return{
        normalizeMonoSpace: normalizeMonoSpace,
        normalizeMultuSpace: normalizeMultuSpace
    }

}