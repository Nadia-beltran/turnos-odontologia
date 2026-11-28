// Creación de elementos: el texto siempre va por textContent, nunca como HTML (regla dura).

type Child = Node | string | null | undefined | false;

interface Props {
  attrs?: Record<string, string>;
  text?: string;
  on?: Partial<{ [K in keyof HTMLElementEventMap]: (event: HTMLElementEventMap[K]) => void }>;
}

export function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  props: Props = {},
  ...children: Child[]
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  for (const [name, value] of Object.entries(props.attrs ?? {})) {
    node.setAttribute(name, value);
  }
  if (props.text !== undefined) {
    node.textContent = props.text;
  }
  for (const [type, handler] of Object.entries(props.on ?? {})) {
    node.addEventListener(type, handler as EventListener);
  }
  node.append(...children.filter((c): c is Node | string => c !== null && c !== undefined && c !== false));
  return node;
}

/** Reemplaza todo el contenido de `parent`. */
export function mount(parent: Element, ...children: (Node | string)[]): void {
  parent.replaceChildren(...children);
}
