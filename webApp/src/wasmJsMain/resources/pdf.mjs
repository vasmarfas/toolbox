import { GlobalWorkerOptions, getDocument } from './vendor/pdfjs/build/pdf.min.mjs';

const BASE = new URL('./vendor/pdfjs/', import.meta.url).href;
GlobalWorkerOptions.workerSrc = BASE + 'build/pdf.worker.min.mjs';

export async function open(bytes) {
    const document = await getDocument({
        data: bytes,
        cMapUrl: BASE + 'cmaps/',
        cMapPacked: true,
        standardFontDataUrl: BASE + 'standard_fonts/',
        wasmUrl: BASE + 'wasm/',
        iccUrl: BASE + 'iccs/',
    }).promise;
    return {
        count: document.numPages,
        async render(index, width) {
            const page = await document.getPage(index + 1);
            const scale = width / page.getViewport({ scale: 1 }).width;
            const viewport = page.getViewport({ scale });
            const canvas = window.document.createElement('canvas');
            canvas.width = width;
            canvas.height = Math.max(1, Math.round(viewport.height));
            const context = canvas.getContext('2d', { willReadFrequently: true });
            context.fillStyle = '#ffffff';
            context.fillRect(0, 0, canvas.width, canvas.height);
            await page.render({ canvas, viewport }).promise;
            page.cleanup();
            const data = context.getImageData(0, 0, canvas.width, canvas.height).data;
            return { width: canvas.width, height: canvas.height, pixels: new Int32Array(data.buffer) };
        },
        close() {
            document.destroy();
        },
    };
}
