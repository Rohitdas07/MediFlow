/**
 * MediFlow backend bridge.
 * The MediFlow workflow/UI is preserved; use this helper for calls that
 * should be persisted through the Spring Boot + MySQL backend.
 */
const API_BASE = (import.meta.env.VITE_MEDIFLOW_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export async function mediflowFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path.startsWith('/') ? path : `/${path}`}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`MediFlow API ${response.status}: ${body || response.statusText}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
