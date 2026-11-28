// design-system/brand.js define window.BRAND (fuente única del nombre de marca).
interface Window {
  readonly BRAND: Readonly<{ name: string; tagline: string }>;
}
