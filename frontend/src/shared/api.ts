// Cliente de la API: mismo origen (DD-03), cookie de sesión, CSRF y errores Problem Details (RFC 9457).

export interface FieldError {
  field: string;
  message: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly errors: FieldError[];

  constructor(status: number, title: string, detail: string, errors: FieldError[] = []) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.errors = errors;
  }
}

type Method = 'GET' | 'HEAD' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface Options {
  method?: Method;
  body?: unknown;
}

const MODIFICAN: ReadonlySet<Method> = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

const DETALLE_GENERICO = 'No pudimos completar la operación. Probá de nuevo en unos minutos.';
const DETALLE_RED = 'No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.';

function leerCookie(nombre: string): string | null {
  for (const parte of document.cookie.split(';')) {
    const [clave, ...valor] = parte.trim().split('=');
    if (clave === nombre) return decodeURIComponent(valor.join('='));
  }
  return null;
}

export async function api<T = unknown>(path: `/api/${string}`, options: Options = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const headers = new Headers({ Accept: 'application/json, application/problem+json' });
  if (options.body !== undefined) headers.set('Content-Type', 'application/json');
  if (MODIFICAN.has(method)) {
    const token = leerCookie('XSRF-TOKEN');
    if (token !== null) headers.set('X-XSRF-TOKEN', token);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, 'Sin conexión', DETALLE_RED);
  }

  if (!response.ok) throw await aError(response);
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T;
  }
  const texto = await response.text();
  return (texto === '' ? undefined : JSON.parse(texto)) as T;
}

async function aError(response: Response): Promise<ApiError> {
  const generico = new ApiError(response.status, response.statusText || 'Error', DETALLE_GENERICO);
  if (!(response.headers.get('Content-Type') ?? '').includes('application/problem+json')) {
    return generico;
  }
  try {
    const p: unknown = await response.json();
    if (typeof p !== 'object' || p === null) return generico;
    const r = p as Record<string, unknown>;
    const errors = Array.isArray(r['errors'])
      ? r['errors'].filter(esFieldError)
      : [];
    return new ApiError(
      response.status,
      typeof r['title'] === 'string' ? r['title'] : generico.title,
      typeof r['detail'] === 'string' ? r['detail'] : DETALLE_GENERICO,
      errors,
    );
  } catch {
    return generico;
  }
}

function esFieldError(e: unknown): e is FieldError {
  if (typeof e !== 'object' || e === null) return false;
  const r = e as Record<string, unknown>;
  return typeof r['field'] === 'string' && typeof r['message'] === 'string';
}
