/**
 * Handwriting-aware local OCR helper.
 *
 * This is intentionally a multi-pass recognizer rather than claiming that
 * Tesseract alone can reliably read every doctor's handwriting. The primary
 * handwriting recognizer remains the server multimodal vision endpoint; this
 * helper provides a stronger local fallback and a confidence signal.
 */
import { createWorker } from 'tesseract.js';
import { preprocessPrescriptionImage } from './imagePreprocessing';

export interface HandwritingOcrResult {
  text: string;
  confidence: number;
  passes: number;
  source: 'handwriting_ocr' | 'tesseract_local' | 'none';
}

async function recognizePass(image: string, psm: '6' | '11') {
  const worker = await createWorker('eng', 1);
  try {
    // PSM 6 is useful for structured prescription rows; PSM 11 is useful for
    // sparse handwriting scattered around a page.
    await worker.setParameters({ tessedit_pageseg_mode: psm });
    const result = await worker.recognize(image);
    return {
      text: (result.data.text || '').trim(),
      confidence: typeof result.data.confidence === 'number' ? result.data.confidence : 0
    };
  } finally {
    await worker.terminate();
  }
}

export async function runHandwritingAwareOcr(imageSrc: string): Promise<HandwritingOcrResult> {
  if (!imageSrc) return { text: '', confidence: 0, passes: 0, source: 'none' };

  let processed = imageSrc;
  try {
    const prep = await preprocessPrescriptionImage(imageSrc, {
      maxDimension: 2600,
      enhanceContrast: true,
      sharpen: true
    });
    processed = prep.processedDataUrl || imageSrc;
  } catch (err) {
    console.warn('[HandwritingOCR] preprocessing fallback:', err);
  }

  const candidates: Array<{ text: string; confidence: number }> = [];
  for (const [image, psm] of [[processed, '6'], [processed, '11'], [imageSrc, '6']] as const) {
    try {
      candidates.push(await recognizePass(image, psm));
    } catch (err) {
      console.warn(`[HandwritingOCR] PSM ${psm} pass failed:`, err);
    }
  }

  const usable = candidates.filter(c => c.text.length > 0);
  if (!usable.length) return { text: '', confidence: 0, passes: candidates.length, source: 'none' };

  // Prefer a high-confidence transcription, but add a small completeness
  // bonus so a pass that captures more prescription rows can win.
  usable.sort((a, b) => {
    const score = (c: { text: string; confidence: number }) =>
      c.confidence * 0.8 + Math.min(c.text.length / 400, 1) * 20;
    return score(b) - score(a);
  });

  return {
    text: usable[0].text,
    confidence: Math.round(Math.max(0, Math.min(100, usable[0].confidence))),
    passes: candidates.length,
    source: 'handwriting_ocr'
  };
}
