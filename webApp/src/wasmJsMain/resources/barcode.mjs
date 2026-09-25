import { prepareZXingModule, readBarcodes } from './vendor/zxing/es/reader/index.js';

const WASM = new URL('./vendor/zxing/reader/zxing_reader.wasm', import.meta.url).href;
prepareZXingModule({ overrides: { locateFile: (path, prefix) => (path.endsWith('.wasm') ? WASM : prefix + path) } });

const OPTIONS = { formats: [], tryHarder: true, maxNumberOfSymbols: 8 };

function found(results) {
    return results.filter((result) => result.isValid).map((result) => ({ text: result.text, format: result.format }));
}

export async function scanBytes(bytes) {
    return found(await readBarcodes(new Blob([bytes]), OPTIONS));
}

export function cameraAvailable() {
    return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
}

// the video element is never attached to the page: its frames are copied out for the preview on the
// Compose canvas and handed to the reader, so nothing has to be laid over the canvas
export async function openCamera() {
    const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
    });
    const video = document.createElement('video');
    video.muted = true;
    video.playsInline = true;
    video.srcObject = stream;
    await video.play();
    const preview = new OffscreenCanvas(1, 1);
    const previewContext = preview.getContext('2d', { willReadFrequently: true });
    const full = new OffscreenCanvas(1, 1);
    const fullContext = full.getContext('2d', { willReadFrequently: true });
    return {
        frame(maxSide) {
            const width = video.videoWidth;
            const height = video.videoHeight;
            if (!width || !height) return null;
            const scale = Math.min(1, maxSide / Math.max(width, height));
            preview.width = Math.max(1, Math.round(width * scale));
            preview.height = Math.max(1, Math.round(height * scale));
            previewContext.drawImage(video, 0, 0, preview.width, preview.height);
            const data = previewContext.getImageData(0, 0, preview.width, preview.height).data;
            return { width: preview.width, height: preview.height, pixels: new Int32Array(data.buffer) };
        },
        async scan() {
            if (!video.videoWidth) return [];
            full.width = video.videoWidth;
            full.height = video.videoHeight;
            fullContext.drawImage(video, 0, 0);
            return found(await readBarcodes(fullContext.getImageData(0, 0, full.width, full.height), OPTIONS));
        },
        close() {
            stream.getTracks().forEach((track) => track.stop());
            video.srcObject = null;
        },
    };
}
