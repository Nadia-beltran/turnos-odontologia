// Arranque: design system (sin copiarlo, design D2), estilos base y router.
import '../../design-system/tokens.css';
import '../../design-system/brand.js';
import './styles/base.css';

import { layoutPanel, layoutPublico } from './layouts';
import { paginaNoEncontrada, startRouter, type Route } from './router';
import { el } from './shared/dom';

// Rutas de muestra: C-07 y C-16 las reemplazan por las pantallas reales.
const rutas: Route[] = [
  {
    path: '/',
    layout: layoutPublico,
    render: () =>
      el(
        'section',
        {},
        el('h1', { text: window.BRAND.name }),
        el('p', { text: window.BRAND.tagline }),
        el('a', { attrs: { href: '/panel' }, text: 'Ir al panel del consultorio' }),
      ),
  },
  {
    path: '/panel',
    layout: layoutPanel,
    render: () =>
      el('section', {}, el('h1', { text: 'Panel del consultorio' }), el('a', { attrs: { href: '/' }, text: 'Volver al inicio' })),
  },
];

const root = document.getElementById('app');
if (!root) throw new Error('Falta el elemento #app en index.html');
startRouter(root, rutas, paginaNoEncontrada(layoutPublico));
