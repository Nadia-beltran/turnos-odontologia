// Layouts base (design D9): esqueletos sin mockup, por decisión de la usuaria solo en C-02.
import logoUrl from '../../design-system/assets/logo.svg?url';
import { el } from './shared/dom';
import type { Layout } from './router';

function header(modificador: string): HTMLElement {
  return el(
    'header',
    { attrs: { class: `app-header app-header--${modificador}` } },
    el(
      'a',
      { attrs: { class: 'app-brand', href: '/' } },
      // alt vacío: el nombre de la marca ya es el texto del enlace.
      el('img', { attrs: { src: logoUrl, alt: '' } }),
      el('span', { attrs: { class: 'app-brand__name' }, text: window.BRAND.name }),
    ),
  );
}

function main(modificador: string, content: Node): HTMLElement {
  return el('main', { attrs: { class: `app-main app-main--${modificador}`, tabindex: '-1' } }, content);
}

/** Páginas del paciente: primero el celular, columna angosta. */
export const layoutPublico: Layout = (content) =>
  el('div', { attrs: { class: 'app' } }, header('publico'), main('publico', content));

/** Panel del consultorio: contenido ancho. */
export const layoutPanel: Layout = (content) =>
  el('div', { attrs: { class: 'app' } }, header('panel'), main('panel', content));
