# Prompt para la sesión de Claude que implementará la consulta al SRI

> Copiar el bloque completo de abajo y pegarlo como primer mensaje en una sesión de Claude Code abierta **en la raíz del proyecto PHP de facturación** (no en `saaBE`).
>
> **Antes de pegarlo:** copiar el archivo `IMPLEMENTACION-CONSULTA-SRI-PHP.md` a la carpeta `docs/` del proyecto PHP, o ajustar la ruta que aparece en el prompt.

---

```
Vas a implementar en este sistema de facturación electrónica PHP la consulta de
comprobantes electrónicos al SRI (Ecuador), tanto en el backend PHP como en las
pantallas Angular.

## Documento guía

Lee COMPLETO y sigue al pie de la letra:

    docs/IMPLEMENTACION-CONSULTA-SRI-PHP.md

Ese documento contiene la especificación de los WS del SRI, el DDL, el código de
referencia de los dos archivos PHP nuevos, el contrato JSON de la API, las reglas
de sincronización, el código Angular y el checklist de pruebas. No re-derives nada
que ya esté ahí; úsalo como fuente de verdad. El código PHP del documento es una
implementación de referencia completa: adáptala a las convenciones reales de este
repositorio en lugar de copiarla a ciegas.

## Fase 0 — Reconocimiento (hazlo ANTES de escribir código)

No asumas rutas ni nombres. Verifica en el repositorio real y repórtame lo que
encuentres:

1. La carpeta física donde viven `gn_autorizacion.php` y `gn_xml_11.php`
   (el documento la llama `documents/`, pero eso viene de los includes, no de una
   ruta confirmada).
2. Que existan `lib/config.php` (define `$db`), `lib/utils.php` (`connect`,
   `salir`, `crearDirectorio`, `getMod11Dv`) y `lib/auth.php`
   (`intentarAutenticacionJWT`, `validarPermisoFacturador`). Si `auth.php` no
   existe o las funciones se llaman distinto, dímelo y adapta el endpoint.
3. Las columnas reales de `fctr`, `ntcr`, `ntdb`, `lqcs`, `rtnc`, `rtv2`
   (¿existe `ambiente`? ¿`facturador`? ¿`clave`? ¿`estadoEmision`?) y de las
   tablas de paths `ptfc`, `ptnc`, `ptnd`, `ptlc`, `ptrt`, `prt2`.
   Confirma el mapa de la sección 3.1 del documento.
4. Si ya existe algún índice sobre la columna `clave` en esas tablas.
5. En el frontend Angular: la librería de UI en uso (Angular Material / PrimeNG /
   ng-bootstrap / otra), la variable de entorno con la URL base del backend PHP,
   y los componentes de las grillas de los 6 tipos de documento.

Reporta los hallazgos y las diferencias contra el documento ANTES de continuar.
Si algo contradice al documento, gana el código real: dímelo y ajusta el plan.

## Fase 1 — Backend PHP

1. Crear `lib/sri_consulta.php` con la librería (sección 5 del documento).
2. Crear `gn_consulta_sri.php` en la carpeta confirmada en la Fase 0
   (sección 6 del documento).
3. Entregar el script `.sql` con el DDL de la sección 4, ajustado a lo que
   realmente exista en la BD (no dupliques índices ya existentes).
   NO ejecutes el DDL: entrégamelo en un archivo `sql/` para que yo lo corra.

Reglas innegociables del backend:

- Cliente SOAP: `SoapClient` nativo con `["trace" => 1]` y `try/catch (SoapFault)`,
  exactamente como `gn_autorizacion*.php`. El fallback cURL de la sección 11.3
  queda documentado pero NO se activa por defecto.
- TODO SQL nuevo va con `prepare` + `bindValue`. Nada de interpolar variables en
  el string del SQL (los scripts viejos lo hacen; no lo repliques).
- Los nombres de tabla salen del mapa whitelist `sriTiposDocumento()`, jamás
  directo del request.
- Un documento en `estado = 5` NUNCA baja de estado por una consulta.
- `estadoConsulta = RECHAZADA` NUNCA modifica la columna `estado`: puede ser solo
  "fecha de emisión fuera del rango permitido", que no implica falta de
  autorización. Esto es lo más importante de toda la implementación.
- La inserción en las tablas de paths es idempotente: verificar que no exista la
  fila `(documento, alterno)` antes de insertar.
- `ANULADO` y `PENDIENTE DE ANULAR` solo se guardan en `estadoSRI`; no se
  inventan valores nuevos para la columna `estado`.
- El endpoint responde SIEMPRE JSON, con los códigos HTTP de la sección 7.4.
- Mantener el estilo de la casa: `error_log()` al inicio de cada operación
  relevante, `ini_set('display_errors', 0)`, `date_default_timezone_set('America/Guayaquil')`,
  `connect($db)` al inicio y `salir($dbConn)` al final de cada rama.

## Fase 2 — Frontend Angular

1. `src/app/models/sri-consulta.model.ts` — interfaces de la sección 9.1.
2. `src/app/services/sri-consulta.service.ts` — servicio de la sección 9.2.
3. Componente modal de consulta (sección 9.3), adaptado a la librería de UI que
   realmente use el proyecto.
4. Integración en las 6 grillas de documentos (sección 10.1): botón "Consultar
   estado en el SRI" por fila, deshabilitado si la fila no tiene `clave`.
   En las pantallas de retenciones enviar `tipoDoc` explícito ('rtnc' o 'rtv2'):
   ambas comparten codDoc 07 y la autodetección no puede distinguirlas.
5. Columna "Estado SRI" en las grillas usando `estadoSRI` / `fechaConsultaSRI`
   (sección 10.2).
6. Pantalla "Sincronización con el SRI" para consulta en lote (sección 10.3),
   con barra de progreso y timeout HTTP >= 180 s para ese endpoint.
7. Panel de estado SRI en el detalle del documento (sección 10.4).

Reglas del frontend:

- `RECHAZADA` no se pinta como error rojo. Distinguir con los helpers
  `esFueraDeRango()` / `esInexistenteEnSRI()` y mostrar el texto explicativo
  correspondiente: "fuera de rango de fechas" NO significa "no autorizado".
- Que una factura no sea comercial negociable es lo NORMAL: no presentarlo como
  fallo.
- El botón "Sincronizar" solo se habilita si el estado del SRI difiere del estado
  local y el usuario tiene permiso.
- Al cerrar el modal tras una sincronización efectiva, recargar la grilla.
- No disparar consultas automáticas al cargar pantallas: el SRI limita por IP.
  Siempre acción explícita del usuario.

## Entrega

Trabaja en una rama nueva. Al terminar cada fase, párate y muéstrame:

- Fase 0: los hallazgos y las diferencias contra el documento.
- Fase 1: los archivos creados, el `.sql`, y los comandos `curl` de la sección
  12.1 listos para que yo los ejecute contra el ambiente de pruebas (ambiente 1,
  celcer.sri.gob.ec).
- Fase 2: los archivos creados/modificados y qué falta conectar manualmente.

No ejecutes el DDL ni pruebes contra producción (ambiente 2). Todas las pruebas
van contra celcer (ambiente 1).

Si algo del documento no cuadra con el repositorio real, dilo explícitamente en
vez de improvisar una solución silenciosa.

Empieza por la Fase 0.
```
