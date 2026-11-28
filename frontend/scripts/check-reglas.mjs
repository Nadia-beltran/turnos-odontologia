// Chequeo de reglas duras del frontend (design D6): sin innerHTML y similares, sin `any` explícito.
// Uso: node scripts/check-reglas.mjs <directorio>
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const REGLAS = [
  [/\b(innerHTML|outerHTML|insertAdjacentHTML)\b/, 'innerHTML/outerHTML/insertAdjacentHTML: usar textContent o src/shared/dom.ts'],
  [/:\s*any\b|\bas\s+any\b|<any>/, '`any` explícito: tipar el valor'],
];

function* archivos(dir) {
  for (const nombre of readdirSync(dir)) {
    const ruta = join(dir, nombre);
    if (statSync(ruta).isDirectory()) yield* archivos(ruta);
    else if (/\.(ts|js|mjs)$/.test(nombre)) yield ruta;
  }
}

const dir = process.argv[2] ?? 'src';
let fallas = 0;
for (const ruta of archivos(dir)) {
  readFileSync(ruta, 'utf8').split('\n').forEach((linea, i) => {
    for (const [patron, motivo] of REGLAS) {
      if (patron.test(linea)) {
        console.error(`${ruta}:${i + 1}: ${motivo}\n  ${linea.trim()}`);
        fallas++;
      }
    }
  });
}
if (fallas > 0) {
  console.error(`\n${fallas} violación(es) de reglas duras.`);
  process.exit(1);
}
console.log(`Reglas duras OK (${dir}).`);
