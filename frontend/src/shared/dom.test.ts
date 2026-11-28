import { describe, expect, it, vi } from 'vitest';
import { el, mount } from './dom';

describe('dom', () => {
  it('trata el texto con marcado como texto literal', () => {
    const p = el('p', { text: '<script>alert(1)</script>' });
    expect(p.children).toHaveLength(0);
    expect(p.textContent).toBe('<script>alert(1)</script>');
  });

  it('los hijos string también quedan como texto', () => {
    const p = el('p', {}, '<b>x</b>');
    expect(p.querySelector('b')).toBeNull();
  });

  it('asigna atributos, eventos y omite hijos vacíos', () => {
    const onClick = vi.fn();
    const b = el('button', { attrs: { type: 'button' }, on: { click: onClick } }, null, false, 'Ok');
    b.click();
    expect(b.getAttribute('type')).toBe('button');
    expect(onClick).toHaveBeenCalledOnce();
    expect(b.textContent).toBe('Ok');
  });

  it('mount reemplaza el contenido', () => {
    const root = el('div', {}, el('span'));
    mount(root, el('p'));
    expect(root.children).toHaveLength(1);
    expect(root.firstElementChild?.tagName).toBe('P');
  });
});
