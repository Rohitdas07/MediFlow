/**
 * Generates a human-readable OPD token once per patient intake.
 * Tokens are sequential per day and care stream so they do not change on re-render.
 * In a multi-kiosk production deployment this should be backed by the hospital HIS;
 * localStorage is used here as the deterministic demo/dev fallback.
 */
import { CareStream } from '../types';
import { generatePermanentPatientId } from './patientIdService';

const TOKEN_COUNTER_PREFIX = 'mediflow:opd-counter:';

function streamPrefix(careStream: CareStream): 'OPD' | 'AYUSH' | 'INT' {
  if (careStream === 'ayurveda') return 'AYUSH';
  if (careStream === 'integrated') return 'INT';
  return 'OPD';
}

function todayKey(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}${m}${d}`;
}

export function generateOpdToken(careStream: CareStream, existingToken?: string): string {
  // Never regenerate an already issued token when the kiosk step is revisited.
  if (existingToken && /^(OPD|AYUSH|INT)-\d{3,6}$/i.test(existingToken.trim())) {
    return existingToken.trim().toUpperCase();
  }

  const prefix = streamPrefix(careStream);
  const key = `${TOKEN_COUNTER_PREFIX}${todayKey()}:${prefix}`;

  let next = 1;
  try {
    const current = Number.parseInt(localStorage.getItem(key) || '0', 10);
    next = Number.isFinite(current) && current > 0 ? current + 1 : 1;
    localStorage.setItem(key, String(next));
  } catch {
    // Private browsing/storage-disabled fallback. A timestamp-derived sequence is
    // still stable for this generated token and avoids an empty OPD number.
    next = (Date.now() % 999999) + 1;
  }

  return `${prefix}-${String(next).padStart(3, '0')}`;
}


export async function issuePersistentOpdToken(
  careStream: CareStream,
  patientId?: string,
  existingToken?: string
): Promise<{ tokenNumber: string; patientCode: string }> {
  const rawPatientId = String(patientId || '').trim();

  // If the kiosk has a permanent Patient ID, keep it available for the
  // response fallback, but the backend remains the source of truth.
  const fallbackPatientCode = /^MFP-\d{4}-[A-Z0-9]{8}$/i.test(rawPatientId)
    ? rawPatientId.toUpperCase()
    : '';

  try {
    const res = await fetch('/api/opd/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ careStream, patientId: rawPatientId, existingToken: existingToken || '' })
    });

    const data = await res.json().catch(() => ({}));
    if (res.ok && data?.success && data?.tokenNumber && data?.patientCode) {
      return {
        tokenNumber: String(data.tokenNumber).toUpperCase(),
        patientCode: String(data.patientCode).toUpperCase()
      };
    }

    throw new Error(data?.error || 'The server could not generate the OPD token.');
  } catch (e) {
    console.error('[MediFlow] OPD token generation failed:', e);
    throw e instanceof Error
      ? e
      : new Error('Unable to generate OPD token. Please check the backend connection.');
  }
}
