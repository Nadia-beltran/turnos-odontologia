import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './api';

const fetchMock = vi.fn<typeof fetch>();

function borrarCookies(): void {
  for (const c of document.cookie.split(';')) {
    const nombre = c.split('=')[0]?.trim();
    if (nombre) document.cookie = `${nombre}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  }
}

function json(status: number, body: unknown, type = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': type } });
}

function ultimaRequest(): { url: string; init: RequestInit; headers: Headers } {
  const [url, init] = fetchMock.mock.calls.at(-1) ?? [];
  return { url: String(url), init: init ?? {}, headers: new Headers(init?.headers) };
}

async function rechazo(p: Promise<unknown>): Promise<ApiError> {
  try {
    await p;
  } catch (e) {
    if (e instanceof ApiError) return e;
    throw e;
  }
  throw new Error('se esperaba un rechazo');
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  borrarCookies();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('api: mismo origen y sesión', () => {
  it('usa la ruta relativa e incluye las cookies del mismo origen', async () => {
    fetchMock.mockResolvedValue(json(200, { ok: true }));
    const r = await api<{ ok: boolean }>('/api/algo');
    expect(r).toEqual({ ok: true });
    const { url, init } = ultimaRequest();
    expect(url).toBe('/api/algo');
    expect(init.credentials).toBe('same-origin');
    expect(init.method).toBe('GET');
  });
});

describe('api: header CSRF', () => {
  it('POST con cookie XSRF-TOKEN envía X-XSRF-TOKEN', async () => {
    document.cookie = 'XSRF-TOKEN=abc; path=/';
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await api('/api/algo', { method: 'POST', body: { a: 1 } });
    const { headers, init } = ultimaRequest();
    expect(headers.get('X-XSRF-TOKEN')).toBe('abc');
    expect(headers.get('Content-Type')).toBe('application/json');
    expect(init.body).toBe('{"a":1}');
  });

  it('GET no envía X-XSRF-TOKEN aunque exista la cookie', async () => {
    document.cookie = 'XSRF-TOKEN=abc; path=/';
    fetchMock.mockResolvedValue(json(200, {}));
    await api('/api/algo');
    expect(ultimaRequest().headers.has('X-XSRF-TOKEN')).toBe(false);
  });

  it('POST sin cookie sale sin header y el 403 se informa como error', async () => {
    fetchMock.mockResolvedValue(
      json(403, { type: 'about:blank', title: 'Forbidden', status: 403, detail: 'No tenés permiso para esta acción.' }, 'application/problem+json'),
    );
    const e = await rechazo(api('/api/algo', { method: 'POST' }));
    expect(ultimaRequest().headers.has('X-XSRF-TOKEN')).toBe(false);
    expect(e.status).toBe(403);
    expect(e.detail).toBe('No tenés permiso para esta acción.');
  });
});

describe('api: respuestas', () => {
  it('204 resuelve sin valor', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(api('/api/algo', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('400 Problem Details expone status y errors por campo', async () => {
    fetchMock.mockResolvedValue(
      json(
        400,
        {
          type: 'about:blank',
          title: 'Bad Request',
          status: 400,
          detail: 'Datos inválidos.',
          errors: [{ field: 'email', message: 'formato inválido' }, { basura: 1 }],
        },
        'application/problem+json',
      ),
    );
    const e = await rechazo(api('/api/algo', { method: 'POST', body: {} }));
    expect(e.status).toBe(400);
    expect(e.title).toBe('Bad Request');
    expect(e.errors).toEqual([{ field: 'email', message: 'formato inválido' }]);
  });

  it('502 sin Problem Details da un detail genérico sin exponer el cuerpo', async () => {
    fetchMock.mockResolvedValue(
      new Response('<html>Bad Gateway nginx</html>', { status: 502, headers: { 'Content-Type': 'text/html' } }),
    );
    const e = await rechazo(api('/api/algo'));
    expect(e.status).toBe(502);
    expect(e.detail).not.toContain('nginx');
    expect(e.detail).toMatch(/No pudimos/);
  });

  it('falla de red rechaza con status 0 y mensaje de conexión', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));
    const e = await rechazo(api('/api/algo'));
    expect(e.status).toBe(0);
    expect(e.detail).toMatch(/conexión/);
  });
});
