/*
 * Estudio Sonrisa: nombre de marca en un solo lugar.
 * Para renombrar el producto, cambiá BRAND_NAME (y BRAND_TAGLINE) acá.
 * Todos los previews y mockups lo toman de los elementos con
 * [data-brand-name] y [data-brand-tagline].
 * En el backend, el nombre de los emails vive en una constante de configuración equivalente (DD-14).
 */
(function () {
  "use strict";

  var BRAND_NAME = "Estudio Sonrisa";
  var BRAND_TAGLINE = "Turnos para tu consultorio, sin huecos vacíos";

  function bindBrand() {
    document.querySelectorAll("[data-brand-name]").forEach(function (el) {
      el.textContent = BRAND_NAME;
    });
    document.querySelectorAll("[data-brand-tagline]").forEach(function (el) {
      el.textContent = BRAND_TAGLINE;
    });
    document.querySelectorAll("[data-brand-alt]").forEach(function (el) {
      el.setAttribute("alt", BRAND_NAME + " (logo provisorio)");
    });
    if (document.title.indexOf("{brand}") !== -1) {
      document.title = document.title.replace("{brand}", BRAND_NAME);
    }
  }

  window.BRAND = Object.freeze({ name: BRAND_NAME, tagline: BRAND_TAGLINE });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", bindBrand);
  } else {
    bindBrand();
  }
})();
