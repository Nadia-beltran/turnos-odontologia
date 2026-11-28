// Router con History API (spec frontend-routing, design D3).
import { el, mount } from './shared/dom';

export type Params = Record<string, string>;
export type Layout = (content: Node) => Node;

export interface Route {
  path: string;
  layout: Layout;
  render: (params: Params) => Node;
}

interface Compilada {
  route: Route;
  regex: RegExp;
  claves: string[];
}

function compilar(route: Route): Compilada {
  const claves: string[] = [];
  const patron = route.path
    .split('/')
    .map((seg) => {
      if (seg.startsWith(':')) {
        claves.push(seg.slice(1));
        return '([^/]+)';
      }
      return seg.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    })
    .join('/');
  return { route, regex: new RegExp(`^${patron}/?$`), claves };
}

export function resolver(
  rutas: readonly Route[],
  pathname: string,
): { route: Route; params: Params } | null {
  for (const { route, regex, claves } of rutas.map(compilar)) {
    const m = regex.exec(pathname);
    if (!m) continue;
    const params: Params = {};
    claves.forEach((clave, i) => {
      params[clave] = decodeURIComponent(m[i + 1] ?? '');
    });
    return { route, params };
  }
  return null;
}

export interface Router {
  navigate: (to: string) => void;
  render: () => void;
  stop: () => void;
}

export function startRouter(root: Element, rutas: readonly Route[], noEncontrada: Route): Router {
  const render = (): void => {
    const hit = resolver(rutas, location.pathname) ?? { route: noEncontrada, params: {} };
    mount(root, hit.route.layout(hit.route.render(hit.params)));
  };

  const navigate = (to: string): void => {
    history.pushState(null, '', to);
    render();
    // Foco al contenido nuevo para lectores de pantalla.
    root.querySelector<HTMLElement>('main')?.focus();
  };

  const onClick = (event: MouseEvent): void => {
    if (event.defaultPrevented || event.button !== 0) return;
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const a = (event.target as Element | null)?.closest?.('a');
    if (!a || !a.hasAttribute('href') || a.hasAttribute('target') || a.hasAttribute('download')) return;
    const url = new URL(a.href, location.href);
    if (url.origin !== location.origin) return;
    event.preventDefault();
    navigate(url.pathname + url.search + url.hash);
  };

  document.addEventListener('click', onClick);
  window.addEventListener('popstate', render);
  render();

  return {
    navigate,
    render,
    stop: () => {
      document.removeEventListener('click', onClick);
      window.removeEventListener('popstate', render);
    },
  };
}

export const paginaNoEncontrada = (layout: Layout): Route => ({
  path: '*',
  layout,
  render: () =>
    el(
      'section',
      { attrs: { class: 'vista' } },
      el('h1', { text: 'Página no encontrada' }),
      el('p', { text: 'La dirección que abriste no existe o cambió.' }),
      el('a', { attrs: { href: '/' }, text: 'Volver al inicio' }),
    ),
});
