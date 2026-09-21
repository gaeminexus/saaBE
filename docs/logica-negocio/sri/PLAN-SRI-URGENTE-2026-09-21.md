# PLAN — cerrar los pendientes urgentes de `sri` para declarar el ATS hoy

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-21 · **Encargo del usuario:** *«necesitamos procesar
el ATS sin errores hoy mismo»* · **Alcance:** `sri` + el registro manual de nota de venta (`cxp`)

---

## 0. ⚠️ Lo primero: tres pendientes que este plan NO tiene, porque ya estaban cerrados

El 2026-09-21, al arrancar, el árbitro reportó como abiertos tres frentes de `sri` leyendo
`DIAGNOSTICO-ATS-BASES-Y-RETENCIONES.md` (escrito el 2026-09-11). **Medidos contra el código el
mismo día, los tres estaban cerrados.** Queda escrito acá para que nadie los vuelva a abrir:

| Lo que decía el diagnóstico | Lo medido el 2026-09-21 |
|---|---|
| §3 + `ANEXO A`: *«el reparto de bases por tarifa no existe en ninguna parte del sistema»* | **Falso desde `774dc0e0` (2026-09-11)**, el mismo día que se escribió el anexo. `ProcesoCargaDocumentosServiceImpl` reparte la base 0% desde el detalle en los cuatro documentos: factura `:1697`, NC `:2848`, ND `:2979`, liquidación `:3117` |
| §5: *«el filtro de Consulta de Documentos no permite ver las notas de venta»* | **Cerrado.** `consulta-documentos.component.ts:61,228,285` trae las notas de venta (`TABLA_NOTA_VENTA`) y las rotula |
| §1 (retenciones desde la tabla equivocada) y §2 (doble conteo de la base 0%) | Cerrados antes: `fd3265a8` y `baseGravadaCompra` (`GeneradorAtsServiceImpl:282`) |

> **Lo que hay que llevarse, y ya van varias:** un documento de diagnóstico describe el sistema **el
> día que se escribió**. El `ANEXO A` quedó desactualizado en **horas** — se midió a la mañana y se
> arregló a la tarde. Antes de reportar un pendiente que sale de un `.md`, medirlo en el código.

---

## 1. Lo que SÍ queda abierto, medido

### 1.1 🔴 La nota de venta se declara con base GRAVADA — es el §4, que estaba sin medir

**Medido el 2026-09-21, los tres eslabones:**

1. La pantalla arranca con `subcero: 0` y `codigoIVASRI: ''` por línea
   (`nota-venta-compra-manual.component.ts:83,191,302`), y manda
   `codigoIVASRI: f.codigoIVASRI || undefined`.
2. El registro **graba lo que llega sin recalcular**: `FacturaCompraServiceImpl:453`
   `setSubcero(nvlDouble(solicitud.getSubcero()))` y `:356` deja el código de IVA en `NULL` cuando
   el payload no lo manda. Es deliberado — el contrato §1 dice *«NO se recalculan los totales de
   cabecera desde el detalle: manda el documento físico»*.
3. El ATS escribe `baseImponible = SUBCERO` y `baseImpGrav = SUBTOTAL − SUBCERO`
   (`GeneradorAtsServiceImpl:320`).

**Resultado:** una nota de venta entra con `SUBCERO = 0` y el ATS declara **toda** su base como
gravada. Una nota de venta es de régimen simplificado y no traslada IVA
(`API-NOTA-VENTA-COMPRA-MANUAL.md:93`).

**⛔ Y por qué el `e2-52` no las corrigió, aunque corre sobre la misma tabla:** su criterio de base
0% es `NVL(CODIGOIVASRI,-1) = 0` y el de gravada es `NVL(CODIGOIVASRI,-1) NOT IN (0,6,7)`. **Un
detalle con el código en `NULL` cae en el segundo.** El `e2-52` está bien para lo que fue escrito
—documentos que vienen del XML, que siempre traen el código—; el hueco es el de los que se tipean.

**Arreglo en dos mitades, y son independientes:**

| Mitad | Qué | Quién | Necesita WAR |
|---|---|---|---|
| **Datos** (arregla el ATS de HOY) | `sri/sql/e2-53-nota-de-venta-base-cero.sql` | el usuario lo corre | **No** |
| **Código** (que no vuelva a entrar mal) | `BE-1` y `FE-1` de abajo | los ejecutores | Sí |

### 1.2 🟠 Compra de proveedor con pasaporte: `tipoProv` / `denoProv` no se emiten

`GeneradorAtsServiceImpl:~810` sólo **avisa**. Es el mismo caso que `denoCli` del lado ventas, que
el SRI **rechazó** el 2026-09-15 y se corrigió en `b3ad967a`. Acá no se emite todavía porque el
nombre exacto del elemento no está confirmado: `CATALOGO-ATS.md:30-31` dice `DenoProv` y
`LEVANTAMIENTO-ATS-103-104.md:204` dice `denopr`. **No hay ningún `.xsd` en el repositorio**
(verificado con `find`), pero sí está `docs/logica-negocio/cxp/Catalogo_ATS.xls`, que es la fuente
de la que salió el `CATALOGO-ATS.md` y todavía no se leyó directo. → `BE-2`.

**Sólo aplica si el período declarado tiene una compra con proveedor con pasaporte.** El aviso del
propio generador lo dice: si al regenerar no aparece, este ítem no bloquea el ATS de hoy.

### 1.3 ⚪ Decisión pendiente del usuario, no la tomamos nosotros

Los cuadres 103/104 (`ReporteCuadreSriServiceImpl`) **no** excluyen las facturas de intermediario;
el ATS sí, desde `5901c8ef`. Preguntado el 2026-09-15, sin respuesta. Mientras no se decida, los dos
reportes del mismo sistema y el mismo período declaran distinto.

---

## 2. Ítems a despachar

### BE-1 — el registro de nota de venta calcula la base 0% en vez de creerle al payload

**Archivo:** `src/main/java/com/saa/ejb/cxp/serviceImpl/FacturaCompraServiceImpl.java`
(`registrarNotaVentaManual`, la zona de `:320-356` y `:440-480`).

1. En el bucle de validación (`:346-356`), cuando `d.getCodigoIVASRI()` viene vacío, el código de la
   línea es **`0`** (0%), no `null`. Una nota de venta no traslada IVA.
2. Después de resolver las líneas y **antes** de grabar la cabecera, calcular
   `base0 = Σ baseImponible` de las líneas cuyo código quedó en `0`, `6` o `7`, y grabar
   `factura.setSubcero(base0)` en vez de `solicitud.getSubcero()`.
3. **Excepción explícita:** si la solicitud trae `vIVA > 0` **o** alguna línea con un código gravado
   (distinto de `0`, `6`, `7`), NO se recalcula nada — se graba lo que llega, como hoy. Ese caso es
   raro y el operador está diciendo algo que no vamos a sobreescribir.
4. Mismo criterio aritmético que el `e2-53`, a propósito: los dos tienen que clasificar igual.

⛔ **Esto cambia el contrato** (`docs/logica-negocio/cxp/API-NOTA-VENTA-COMPRA-MANUAL.md`, §1 y la
fila de `subcero` de la tabla de campos): el `subcero` del payload deja de ser la fuente cuando el
documento no tiene IVA. **El árbitro actualiza el contrato en el mismo commit.**

### BE-2 — `tipoProv` / `denoProv` para el proveedor con pasaporte

**Archivo:** `src/main/java/com/saa/ejb/sri/serviceImpl/GeneradorAtsServiceImpl.java:~786-815`.

1. **Primero medir, y de la fuente primaria:** leer `docs/logica-negocio/cxp/Catalogo_ATS.xls` y
   sacar de ahí el nombre exacto del elemento y su posición en la secuencia de `detalleCompras`.
   Es un `.xls` — se lee con lo que haya, o se convierte; **no se decide entre `DenoProv` y `denopr`
   a ojo.**
2. Si el catálogo lo resuelve sin ambigüedad: emitirlo con el mismo patrón que `denoCli` de
   `writeDetalleVenta` (razón social del titular, o su nombre si no tiene), sólo cuando
   `tpIdProv = "03"`, y en la posición que diga el catálogo. Corregir el comentario que afirma que
   no existe en el esquema.
3. **Si el catálogo NO lo resuelve, PARAR y reportar.** Se queda el aviso de hoy. Un elemento con el
   nombre equivocado hace rechazar el anexo entero, que es peor que el aviso.

### FE-1 — la pantalla de nota de venta manual no deja el código de IVA vacío

**Archivo:** `src/app/modules/cxp/forms/procesos/nota-venta-compra-manual/nota-venta-compra-manual.component.ts`.

1. Cada línea nueva arranca con `codigoIVASRI: '0'` (0%) en vez de `''` (`:191`).
2. El `subcero` de la cabecera se calcula como la suma de las bases de las líneas al 0% —el mismo
   patrón que ya tiene `this.form.vIVA = this.sumaDetalleIva` (`:233`)— en vez de quedarse en `0`.
3. No se toca nada más de la pantalla.

---

## 3. Criterio de aceptación

1. `e2-53` corrido: el BLOQUE 4 da `GRAVADA_AHORA = 0` en todas las notas de venta que el BLOQUE 1
   clasificó como `CORREGIBLE`.
2. ATS del período regenerado: en el talón, `NOTA DE VENTA` aparece en `BI tarifa 0%` y **no** en
   `BI tarifa diferente 0%`.
3. Una nota de venta registrada después del WAR queda en la base con `SUBCERO = SUBTOTAL` y su
   detalle con `CODIGOIVASRI = 0`, sin que el operador tenga que tipear nada.
4. Ninguna de las tres cosas de arriba necesita a las otras dos: la 1 arregla hoy, la 3 arregla
   mañana.
