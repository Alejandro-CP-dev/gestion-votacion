/**
 * js/app.js — El único script del sitio.
 *
 * Progresivo a propósito: cada widget se activa solo si sus atributos
 * data-* están presentes, y cada uno tiene una salida sin JavaScript
 * (el <noscript> del token, los <form> que igual funcionan sin el modal).
 */
(function () {
  'use strict';

  document.querySelectorAll('[data-tema-boton]').forEach(iniciarTema);
  document.querySelectorAll('[data-generar-token]').forEach(iniciarGenerarToken);
  document.querySelectorAll('[data-token]').forEach(iniciarToken);
  document.querySelectorAll('form[data-confirmar]').forEach(iniciarConfirmacion);
  document.querySelectorAll('[data-copiar]').forEach(iniciarCopiar);
  document.querySelectorAll('[data-opciones]').forEach(iniciarOpciones);
  document.querySelectorAll('[data-verificacion]').forEach(iniciarVerificacion);
  document.querySelectorAll('form[data-envio-simple]').forEach(iniciarEnvioSimple);
  document.querySelectorAll('[data-abrir-lateral]').forEach(iniciarPanelLateral);
  document.querySelectorAll('[data-imprimir]').forEach(function (boton) {
    boton.addEventListener('click', function () { window.print(); });
  });

  /* ==================== Tema claro / oscuro ====================
   * El atributo data-tema en <html> ya lo puso, si hacía falta, el script
   * inline del <head> (antes de que la página pinte, para no parpadear).
   * Aquí solo se atiende el clic: se decide el tema actual mirando el
   * atributo y, si no está, las preferencias del sistema, y se guarda la
   * elección para que sobreviva a la próxima visita. */

  function iniciarTema(boton) {
    boton.addEventListener('click', function () {
      var raiz = document.documentElement;
      var prefiereOscuro = window.matchMedia
        && window.matchMedia('(prefers-color-scheme: dark)').matches;
      var esOscuroAhora = raiz.getAttribute('data-tema') === 'oscuro'
        || (!raiz.getAttribute('data-tema') && prefiereOscuro);

      var nuevoTema = esOscuroAhora ? 'claro' : 'oscuro';
      raiz.setAttribute('data-tema', nuevoTema);

      try {
        localStorage.setItem('svis-tema', nuevoTema);
      } catch (e) {
        // Almacenamiento bloqueado (navegación privada, política del
        // navegador): el tema igual cambia, solo no sobrevive a un recargo.
      }
    });
  }

  /* ==================== Generar mi token (papeleta) ====================
   * Sin esto el bloque queda siempre visible (ver el comentario en
   * papeleta.php): la version sin JavaScript no puede reaccionar a que se
   * elija una opcion, asi que se queda mostrando el boton todo el tiempo.
   * Aqui, con JS disponible, se oculta hasta que hay una opcion marcada. */

  function iniciarGenerarToken(contenedor) {
    var boton = contenedor.querySelector('[data-boton-generar]');
    var resultado = contenedor.querySelector('[data-resultado-token]');
    var codigo = document.getElementById('tokenGenerado');
    var errorEl = contenedor.querySelector('[data-error-token]');
    var opciones = document.querySelectorAll('input[name="opcionId"]');

    if (!boton || !opciones.length) return;

    contenedor.hidden = true;
    opciones.forEach(function (opcion) {
      opcion.addEventListener('change', function () {
        contenedor.hidden = false;
      });
    });

    boton.addEventListener('click', function () {
      var textoOriginal = boton.textContent;
      boton.disabled = true;
      boton.textContent = 'Generando…';
      errorEl.hidden = true;

      var csrf = document.querySelector('input[name="_csrf"]');
      var datos = new URLSearchParams();
      datos.set('_csrf', csrf ? csrf.value : '');
      datos.set('encuestaId', contenedor.dataset.encuestaId);

      fetch('generar_token.php', {
        method: 'POST',
        credentials: 'same-origin',
        body: datos
      })
        .then(function (resp) { return resp.json(); })
        .then(function (datos) {
          if (!datos.ok) {
            throw new Error(datos.mensaje || 'No fue posible generar tu token.');
          }
          codigo.textContent = datos.token;
          resultado.hidden = false;
          boton.hidden = true;
        })
        .catch(function (error) {
          errorEl.textContent = error.message || 'Fallo de red. Intenta de nuevo.';
          errorEl.hidden = false;
          boton.disabled = false;
          boton.textContent = textoOriginal;
        });
    });
  }

  /* ==================== Token segmentado ==================== */

  function iniciarToken(contenedor) {
    var celdas  = Array.prototype.slice.call(contenedor.querySelectorAll('[data-celda]'));
    var destino = document.getElementById(contenedor.dataset.destino);
    var boton   = document.getElementById(contenedor.dataset.boton);

    function ensamblar() {
      var valor = celdas.map(function (c) { return c.value; }).join('');
      if (destino) destino.value = valor;
      if (boton) boton.disabled = valor.length < celdas.length;
      contenedor.classList.remove('es-error');
    }

    celdas.forEach(function (celda, indice) {
      celda.addEventListener('input', function () {
        celda.value = celda.value.toUpperCase().slice(-1);
        celda.classList.toggle('es-lleno', celda.value !== '');
        if (celda.value && celdas[indice + 1]) celdas[indice + 1].focus();
        ensamblar();
      });

      celda.addEventListener('keydown', function (evento) {
        if (evento.key === 'Backspace' && !celda.value && celdas[indice - 1]) {
          celdas[indice - 1].focus();
        }
      });

      celda.addEventListener('paste', function (evento) {
        var texto = (evento.clipboardData || window.clipboardData)
          .getData('text').toUpperCase().replace(/[^A-Z0-9]/g, '');
        if (!texto) return;
        evento.preventDefault();
        celdas.forEach(function (c, i) { c.value = texto[i] || ''; c.classList.toggle('es-lleno', !!c.value); });
        var siguiente = celdas[Math.min(texto.length, celdas.length - 1)];
        if (siguiente) siguiente.focus();
        ensamblar();
      });
    });

    if (boton) boton.disabled = true;
  }

  /* ==================== Modal de confirmación ====================
   * Un formulario con data-confirmar="idDelTelon" no se envía al primer
   * submit: se detiene, muestra el telón, y solo se envía de verdad si la
   * persona pulsa [data-aceptar] dentro de ese telón. */

  function iniciarConfirmacion(formulario) {
    var telon = document.getElementById(formulario.dataset.confirmar);
    if (!telon) return;

    var eco = telon.querySelector('[data-eco]');

    formulario.addEventListener('submit', function (evento) {
      if (formulario.dataset.confirmado === 'si') return; // ya se aceptó, deja pasar
      evento.preventDefault();

      if (eco) {
        var elegido = formulario.querySelector('input[name="opcionId"]:checked');
        eco.textContent = elegido ? elegido.dataset.etiqueta || '' : '';
      }

      telon.classList.add('es-visible');
    });

    var cancelar = telon.querySelector('[data-cancelar]');
    if (cancelar) {
      cancelar.addEventListener('click', function () {
        telon.classList.remove('es-visible');
      });
    }

    var aceptar = telon.querySelector('[data-aceptar]');
    if (aceptar) {
      aceptar.addEventListener('click', function () {
        formulario.dataset.confirmado = 'si';
        ponerCargando(formulario);
        telon.classList.remove('es-visible');
        formulario.requestSubmit ? formulario.requestSubmit() : formulario.submit();
      });
    }
  }

  /** Formularios sin modal (una sola acción, sin nada que confirmar). */
  function iniciarEnvioSimple(formulario) {
    formulario.addEventListener('submit', function () {
      ponerCargando(formulario);
    });
  }

  function ponerCargando(formulario) {
    var boton = formulario.querySelector('button[type="submit"][data-cargando]');
    if (!boton) return;
    boton.disabled = true;
    boton.classList.add('es-cargando');
    var texto = boton.querySelector('[data-texto]');
    if (texto) texto.textContent = boton.dataset.cargando;
  }

  /* ==================== Copiar al portapapeles ==================== */

  function iniciarCopiar(boton) {
    boton.addEventListener('click', function () {
      var origen = document.getElementById(boton.dataset.copiar);
      if (!origen || !navigator.clipboard) return;

      navigator.clipboard.writeText(origen.textContent.trim()).then(function () {
        var textoOriginal = boton.textContent;
        boton.textContent = '¡Copiado!';
        boton.classList.add('es-listo');
        setTimeout(function () {
          boton.textContent = textoOriginal;
          boton.classList.remove('es-listo');
        }, 2000);
      });
    });
  }

  /* ==================== Opciones dinámicas (crear votación) ==================== */

  function iniciarOpciones(contenedor) {
    var agregar = document.querySelector('[data-agregar-opcion]');
    var minimo = parseInt(contenedor.dataset.minimo || '2', 10);

    function renumerar() {
      var filas = contenedor.querySelectorAll('[data-fila-opcion]');
      filas.forEach(function (fila, indice) {
        // OJO: el número va en un <span> aparte, nunca en el <label> mismo.
        // El label mezcla ese texto con el <input> como hijos; asignar
        // textContent al label entero borraría el input junto con el texto.
        var etiqueta = fila.querySelector('[data-numero-opcion]');
        if (etiqueta) etiqueta.textContent = 'Opción ' + (indice + 1);
        var quitar = fila.querySelector('[data-quitar-opcion]');
        if (quitar) quitar.hidden = filas.length <= minimo;
      });
    }

    contenedor.addEventListener('click', function (evento) {
      var quitar = evento.target.closest('[data-quitar-opcion]');
      if (!quitar) return;
      var fila = quitar.closest('[data-fila-opcion]');
      if (fila && contenedor.querySelectorAll('[data-fila-opcion]').length > minimo) {
        fila.remove();
        renumerar();
      }
    });

    if (agregar) {
      agregar.addEventListener('click', function () {
        var filas = contenedor.querySelectorAll('[data-fila-opcion]');
        var plantilla = filas[filas.length - 1].cloneNode(true);
        plantilla.querySelectorAll('input').forEach(function (campo) { campo.value = ''; });
        contenedor.appendChild(plantilla);
        renumerar();
        plantilla.querySelector('input').focus();
      });
    }

    renumerar();
  }

  /* ==================== Lista con "seleccionar todos" (padrón) ==================== */

  function iniciarVerificacion(contenedor) {
    var maestra = contenedor.querySelector('[data-marcar-todos]');
    var hijas   = Array.prototype.slice.call(contenedor.querySelectorAll('[data-marcable]'));
    var contador = document.querySelector('[data-contador-seleccion]');

    function actualizarContador() {
      var marcadas = hijas.filter(function (c) { return c.checked; }).length;
      if (contador) contador.textContent = marcadas;
      if (maestra) maestra.checked = marcadas === hijas.length && hijas.length > 0;
    }

    if (maestra) {
      maestra.addEventListener('change', function () {
        hijas.forEach(function (c) { c.checked = maestra.checked; });
        actualizarContador();
      });
    }

    hijas.forEach(function (c) { c.addEventListener('change', actualizarContador); });
    actualizarContador();
  }

  /* ==================== Menú lateral del admin (móvil) ==================== */

  function iniciarPanelLateral(boton) {
    var lateral = document.getElementById(boton.getAttribute('aria-controls'));
    var fondo = document.querySelector('[data-cerrar-lateral]');
    if (!lateral) return;

    function cerrar() {
      lateral.classList.remove('es-abierto');
      if (fondo) fondo.classList.remove('es-visible');
      document.documentElement.classList.remove('sin-scroll');
      boton.setAttribute('aria-expanded', 'false');
    }

    function abrir() {
      lateral.classList.add('es-abierto');
      if (fondo) fondo.classList.add('es-visible');
      document.documentElement.classList.add('sin-scroll');
      boton.setAttribute('aria-expanded', 'true');
    }

    boton.setAttribute('aria-expanded', 'false');
    boton.addEventListener('click', function () {
      lateral.classList.contains('es-abierto') ? cerrar() : abrir();
    });

    if (fondo) fondo.addEventListener('click', cerrar);

    document.addEventListener('keydown', function (evento) {
      if (evento.key === 'Escape') cerrar();
    });
  }
})();
