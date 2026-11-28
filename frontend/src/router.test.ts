import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { el } from './shared/dom';
import { paginaNoEncontrada, resolver, startRouter, type Route, type Router } from './router';

const layout = (content: Node): Node => el('main', { attrs: { tabindex: '-1' } }, content);
const rutas: Route[] = [
  { path: '/', layout, render: () => el('div', {}, el('h1', { text: 'Inicio' }), el('a', { attrs: { href: '/panel' }, text: 'Ir' })) },
  { path: '/panel', layout, render: () => el('h1', { text: 'Panel' }) },
  { path: '/r/:slug', layout, render: (p) => el('h1', { text: `Reserva ${p['slug'] ?? ''}` }) },
];

let root: HTMLElement;
let router: Router | undefined;

function clic(target: Element, init: MouseEventInit = {}): MouseEvent {
  const ev = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0, ...init });
  target.dispatchEvent(ev);
  return ev;
}

function arrancar(path: string): void {
  history.replaceState(null, '', path);
  router = startRouter(root, rutas, paginaNoEncontrada(layout));
}

beforeEach(() => {
  root = document.createElement('div');
  document.body.replaceChildren(root);
});

afterEach(() => {
  router?.stop();
  router = undefined;
});

describe('resolver', () => {
  it('extrae segmentos variables', () => {
    expect(resolver(rutas, '/r/estudio-ejemplo')?.params).toEqual({ slug: 'estudio-ejemplo' });
  });

  it('devuelve null para una ruta desconocida', () => {
    expect(resolver(rutas, '/no-existe')).toBeNull();
  });
});

describe('startRouter', () => {
  it('abre directamente una ruta con parámetro', () => {
    arrancar('/r/estudio-ejemplo');
    expect(root.querySelector('h1')?.textContent).toBe('Reserva estudio-ejemplo');
  });

  it('un clic en un enlace interno cambia la vista sin recargar', () => {
    arrancar('/');
    const ev = clic(root.querySelector('a[href="/panel"]')!);
    expect(ev.defaultPrevented).toBe(true);
    expect(location.pathname).toBe('/panel');
    expect(root.querySelector('h1')?.textContent).toBe('Panel');
    expect(document.activeElement).toBe(root.querySelector('main'));
  });

  it('atrás del navegador vuelve a la vista anterior', () => {
    arrancar('/');
    router?.navigate('/panel');
    history.replaceState(null, '', '/');
    window.dispatchEvent(new PopStateEvent('popstate'));
    expect(root.querySelector('h1')?.textContent).toBe('Inicio');
  });

  it('Ctrl+clic no se intercepta', () => {
    arrancar('/');
    const ev = clic(root.querySelector('a[href="/panel"]')!, { ctrlKey: true });
    // En el navegador real abre otra pestaña; el router no debe interceptarlo.
    expect(ev.defaultPrevented).toBe(false);
    expect(root.querySelector('h1')?.textContent).toBe('Inicio');
  });

  it('un enlace con target o a otro origen no se intercepta', () => {
    arrancar('/');
    const conTarget = el('a', { attrs: { href: '/panel', target: '_blank' } });
    const externo = el('a', { attrs: { href: 'https://example.org/x' } });
    root.append(conTarget, externo);
    expect(clic(conTarget).defaultPrevented).toBe(false);
    expect(clic(externo).defaultPrevented).toBe(false);
  });

  it('una ruta inexistente muestra "no encontrada" con enlace al inicio', () => {
    arrancar('/no-existe');
    expect(root.querySelector('h1')?.textContent).toBe('Página no encontrada');
    expect(root.querySelector('a')?.getAttribute('href')).toBe('/');
  });
});
