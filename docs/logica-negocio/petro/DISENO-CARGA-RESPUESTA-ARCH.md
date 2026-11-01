# Diseño — carga de la respuesta de ARCH (filial 2)

**Estado: CONGELADO para implementar (2026-10-31, §9).** Borrador original de omen-saa-1-arb del
2026-10-29. El usuario ordenó el 2026-10-31 programarlo de inmediato («ya no hagas versiones
puente»), para procesar ARCH de octubre el mismo día. Las decisiones del §6 quedan tomadas con la
recomendación del árbitro (§9.1); el usuario puede vetar cualquiera. Salida del `crd/sql/332`
PARTE B: hay generación ARCH de octubre (GNAP 173, 8 líneas, 1.358,91); 7 entidades de filial 2,
**5 sin rol y 2 con rol 0**; ninguna carga previa de filial 2. El `crd/sql/335` (puente por SQL)
queda **descartado**: no correrlo.

## 1. El pedido

Hoy la respuesta de Petrocomercial (filial 1) se carga, se previsualiza, se gestionan novedades
(«Afectar cuotas», AVPC) y se procesa, con asientos. Con ARCH (filial 2) eso no existe: SAA genera
el archivo de descuentos y ARCH contesta con un PDF. El usuario quiere que la respuesta de ARCH
siga **el mismo ciclo que Petro**: carga → previsualización → novedades → procesar, con el mismo
criterio de aplicación. Cartera puede transcribir el PDF a Excel o TXT.

## 2. Lo que manda SAA y lo que contesta ARCH

**Lo que manda SAA** (`GeneracionArchivoPetroServiceImpl`, `REGLAS-GENERACION-PETRO.md` §3.2):
una línea por partícipe con el monto por producto.

```
IDENTIFICACION;RAZON SOCIAL;AC;AJ;PE;PH;HS;PQ;PP;TOTAL
```

En la base queda una fila `CRD.PDGA` por línea (entidad, préstamo, producto, `PDGAMNEN` enviado) y
el desglose por cuota en `CXPG`.

**Lo que contesta ARCH** (muestra: `ASOCIACION_ASOPREP_AGO_2026.pdf`, «CONSULTA DETALLES POR
RUBRO», agosto 2026):

| No | RUBRO | TIPO | IDENTIFICACIÓN | NOMBRES | MONTO CALCULADO | MONTO DESCONTADO |
|---|---|---|---|---|---|---|
| 1 | D - ASOCIACION ASOPREP | C | 2100517560 | BENITEZ CORDERO VERONICA BEATRIZ | 420,00 | 420,00 |
| … | | | | | | |
| | | | | **Total** | **866,70** | **866,70** |

**La diferencia que manda todo el diseño:** Petro contesta por producto (una línea por rol y
producto). ARCH contesta **un solo total por cédula**, sin desglose. El total calculado debería ser
el `TOTAL` que mandamos y el descontado es lo que realmente se retuvo. Para aplicar «como Petro»
hay que volver a abrir ese total en productos, y la única fuente para hacerlo es **nuestra propia
generación de ese mes** (PDGA).

## 3. Formato que se le pide a Cartera

Una fila por partícipe, copiada tal cual del PDF. Sin fila de total. Columnas en este orden:

```
IDENTIFICACION;NOMBRES;MONTO_CALCULADO;MONTO_DESCONTADO
2100517560;BENITEZ CORDERO VERONICA BEATRIZ;420.00;420.00
1705989604;CASTRO LUNA FERNANDO XAVIER;61.00;61.00
```

- Identificación como **texto**: conserva el 0 inicial (`0802289066`). En Excel, la columna tiene
  que estar en formato Texto o la cédula pierde el cero.
- Montos con punto o coma decimal; el lector acepta los dos.
- Si es Excel (`.xlsx`): primera hoja, encabezado en la fila 1, las mismas cuatro columnas (POI ya
  está en el `pom.xml`).
- El mes y el año **no** van en el archivo: se eligen en la pantalla de carga, igual que en Petro.

> Decisión D2 (§6): Excel, TXT, o los dos.

## 4. Flujo propuesto

Igual que Petro, con un paso de traducción delante. Desde el `DTCA`/`PXCA` en adelante **no se
escribe un proceso nuevo**: se reusa el de Petro.

1. **Carga** (`filial = 2`, mes, año, archivo). Lee las filas y por cada cédula:
   - busca la entidad por `ENTDNMID` (y filial 2);
   - busca la línea del partícipe en la generación ARCH de ese mes (`GNAP` filial 2, mismo
     `GNAPMSPE`/`GNAPANPE`) → sus filas PDGA por producto.
2. **Traducción a la forma Petro:** arma los `DetalleCargaArchivo` (uno por producto: AH, PE, PH,
   HS, PQ, PP) y los `ParticipeXCargaArchivo` (uno por partícipe y producto), con el monto
   descontado repartido por producto según la regla del §5.
3. **De ahí en adelante, el mismo camino de Petro:** validación de fases, previsualización,
   novedades, «Afectar cuotas»/AVPC, transferencias recibidas (`TRCR`), confirmación de recepción,
   procesar, red de recibido = aplicado, asientos.
4. **Trazabilidad:** se llenan `PDGA.PDGAMNDC` (monto descontado) y `PDGA.PDGAFCDC` (fecha de
   descuento). Existen en la entidad desde antes y **nada los escribe hoy**.

## 5. Reparto del descontado en productos

| Caso | Tratamiento propuesto |
|---|---|
| Descontado = calculado = TOTAL enviado (±0,01) | **Automático:** cada producto recibe exactamente lo que se envió en PDGA. Es el caso de toda la muestra de agosto (6 de 6). |
| Descontado < calculado (descuento parcial) | **D1** (§6). Propuesta: novedad **BLOQUEANTE** con el descontado como pozo para «Afectar cuotas». |
| Descontado > calculado | Novedad BLOQUEANTE (exceso), igual que en Petro. |
| Calculado de ARCH ≠ TOTAL que enviamos | Novedad (ARCH calculó sobre otra cosa). Si además descontado = calculado, se trata como descuento parcial o exceso contra lo enviado. |
| Cédula en la respuesta sin línea en la generación | Novedad BLOQUEANTE: dinero recibido sin destino. |
| Cédula que no existe en el padrón | **Aborta la carga**, igual que Petro sin entidad: no se sabe de quién es el dinero. |
| Partícipe en la generación que no aparece en la respuesta | Descontado 0. Mismo tratamiento que Petro cuando falta un rol: no hay dinero que aplicar; queda informativo (préstamo sin pagar). |

## 6. Decisiones abiertas (del usuario)

- **D1 — descuento parcial.** ARCH retiene menos de lo calculado y no dice de qué producto.
  Opciones:
  - (a) novedad bloqueante y reparto a mano (propuesta del árbitro: ARCH tiene pocos partícipes,
    6 en agosto, y es el criterio que Petro ya usa para el dinero sin destino);
  - (b) orden fijo automático (por ejemplo, préstamos de la cuota más antigua primero y aportes al
    final);
  - (c) prorrateo por producto.
- **D2 — formato del archivo de Cartera:** Excel, TXT con `;`, o los dos.
- **D3 — ¿ARCH contesta siempre un solo rubro** («D - ASOCIACION ASOPREP»), o puede mandar varios
  rubros para el mismo partícipe?
- **D4 — el dinero de ARCH**, ¿llega en una transferencia a una cuenta propia (`TRCR`), igual que el
  de Petro? La red recibido = aplicado y el asiento de reparto dependen de eso.

## 7. Riesgo técnico a medir antes de despachar

La carga Petro identifica todo por **rol** (`PXCACDPT`, `selectByCodigoPetro`, `cedulaPorRolPetro`,
tope de AVPC por `codigoPetro`). ARCH contesta con **cédula**. Si las entidades de la filial 2
tienen `ENTDRLPC` lleno, la traducción del §4 le pone a cada `PXCA` el rol de la entidad y todo lo
de abajo funciona sin cambios. Si no lo tienen, hay que identificar por entidad en las rutas que hoy
usan el rol, y eso es un cambio mucho mayor. **Lo dice el bloque B3 del `crd/sql/332`.** El B2
confirma que la generación de agosto cuadra con el PDF.

## 8. Fuera de alcance

- Leer el PDF directamente: es un reporte de pantalla del sistema de ARCH, no un formato estable.
- Cambiar el formato que SAA le envía a ARCH.

## 9. Especificación para implementar (2026-10-31)

### 9.1 Decisiones tomadas (recomendación del árbitro, orden del usuario de programarlo ya)

- **D1, descuento parcial:** la novedad es BLOQUEANTE y el operador reparte a mano en «Afectar
  cuotas». Desde `364dc1c8` las AVPC también se aplican en filas AH.
- **D2, formato:** se aceptan `.xlsx`, `.xls`, `.csv` y `.txt`. En CSV/TXT el separador es `;`
  (también `,` o tabulador si no hay `;`). Columnas en este orden:
  `IDENTIFICACION; NOMBRES; MONTO_CALCULADO; MONTO_DESCONTADO`. Primera fila de encabezado
  opcional. Se ignoran las filas cuya identificación no sea numérica (encabezados, «Total»).
- **D3:** si una cédula aparece más de una vez, se suman sus montos.
- **D4:** la transferencia se registra igual que en Petro (`TRCR`, confirmar recepción).
- **Rol técnico:** la carga identifica por rol en todo su recorrido. A una entidad de ARCH con
  `ENTDRLPC` nulo o 0, el lector le asigna y **graba** `ENTDRLPC = 9000000 + ENTDCDGO`. Es el
  mismo efecto que ya tiene `actualizaCodigoPetroEntidad`: 7 dígitos, por encima de cualquier rol
  real de Petro, único porque ENTDCDGO es único.

### 9.2 Backend

- **Punto de entrada:** `CargaArchivoPetroServiceImpl.validarArchivoPetro` (~1003) y
  `procesarArchivoPetro` (~310). Hoy los dos llaman a `procesarContenido(contenido)`, que lee el
  formato de Petrocomercial. Si `cargaArchivo.getFilial().getCodigo() == Filiales.ARCH`, en su
  lugar se llama a un método nuevo `procesarContenidoArch(byte[] archivo, String fileName,
  CargaArchivo cargaArchivo)`. Ese método devuelve la **misma** `List<ParticipeXCargaArchivo>`
  (cada una con su `DetalleCargaArchivo` temporal por producto), así que `agruparPorAporte`,
  `calcularTotalesGenerales`, `almacenaRegistros` y todo lo de abajo no cambian.
- **Las validaciones propias de Petro** (extensión `.txt`, que empiece con «EP») se saltan para
  ARCH y se reemplazan por las suyas: extensión de la lista de D2, archivo no vacío, al menos una
  fila válida.
- **`procesarContenidoArch`**, por cada cédula con su descontado sumado:
  1. Normaliza la cédula: solo dígitos; si quedan 9, antepone un `0` (Excel come el cero).
  2. Busca la entidad por `ENTDNMID` dentro de filial 2. Si no existe, **aborta** con un mensaje
     que nombra la cédula y el nombre del archivo (igual que «sin entidad» en Petro).
  3. Le asigna el rol técnico si no tiene (§9.1) y lo graba.
  4. Busca la generación: `GNAP` con filial 2, `GNAPMSPE = mesAfectacion` y
     `GNAPANPE = anioAfectacion` de la carga. Si hay varias, la de `GNAPCDGO` mayor. Si no hay
     ninguna, **aborta**: «No existe la generación de descuentos ARCH de {mes}/{año}».
  5. Toma las líneas PDGA de esa entidad en esa generación:
     - **|descontado − Σ enviado| ≤ 0,01:** una fila por línea PDGA. Producto = `PDGACDPT`;
       `totalDescontado = montoDescontar = capitalDescontado = saldoActual = PDGAMNEN`; el resto de
       montos en 0; plazo y meses en 1; `codigoPetro` = rol; `nombre` = `ENTDRZNS`, el de la base y
       no el del archivo, para que no salte «código no coincide con nombre».
     - **Descontado > 0,01 pero distinto de lo enviado, o la entidad no tiene líneas en la
       generación:** UNA fila de producto `AH` con `totalDescontado = montoDescontar = descontado`.
       La validación de aportes la marca como novedad y el operador la reparte (D1).
     - **Descontado ≤ 0,01:** no se crea fila.
  6. Los partícipes de la generación que no vienen en la respuesta no generan fila. Se cuentan en
     el log.
- **Lectura de Excel con POI** (ya está en el `pom.xml`). La cédula puede llegar como celda
  numérica: leerla con `DataFormatter` o como `long`, nunca como `double` con notación científica.
- **Fuera de alcance:** `PDGAMNDC`/`PDGAFCDC`, llenar las líneas no descontadas, y tocar nada del
  proceso de aplicación.

### 9.3 Frontend (`crd/forms/archivos-petro/carga/carga-aporte-back`)

> Corregido el 2026-10-31 a raíz del reporte del FE: la pantalla del menú «Carga Aportes» es
> `carga-aporte-back/`, y es la única que llama a `validarArchivoPetro` (valida `.txt` en el html,
> en la extensión y en el MIME). `carga-aportes/` está fuera del menú, arma las filas en el
> navegador y llama a `procesarArchivoPetro`: no se toca.

- Si la filial elegida es ARCH (código 2): el selector de archivo acepta
  `.xlsx,.xls,.csv,.txt`, y la pantalla muestra el formato esperado (las 4 columnas, la cédula
  como texto) con un botón «Descargar plantilla» que genera en el navegador un CSV con el
  encabezado `IDENTIFICACION;NOMBRES;MONTO_CALCULADO;MONTO_DESCONTADO`.
- **Si la filial es Petrocomercial, nada cambia.**
- **Mismo endpoint y mismo cuerpo:** `POST /rest/asgn/validarArchivoPetro`, multipart con
  `archivo`, `archivoNombre` y `cargaArchivo` (que ya lleva la filial).
- Hay que quitar cualquier validación del FE que exija `.txt` o «EP», pero solo para ARCH.

### 9.4 Aceptación

Cargar la respuesta de octubre como filial ARCH, mes 10, año 2026. La vista previa muestra a los
partícipes con sus productos según la generación 173. Los de descuento completo quedan sin novedad
de monto; los parciales, con novedad bloqueante. Se registra la transferencia y se procesa: el
recibido es igual al aplicado y se genera el asiento.

### 9.5 Arreglos urgentes tras la revisión del 2026-10-31 (antes de procesar ARCH de octubre)

- **A2 (bloquea):** `validarOrdenProcesamiento` (`CargaArchivoPetroServiceImpl` ~5092) exige que la
  carga sea del mes siguiente a la última procesada, pero `CargaArchivoDaoServiceImpl.selectUltimaCargaProcesada`
  no filtra por filial. Con Petro de octubre procesada, ARCH de octubre se rechaza (y al revés).
  Corrección: el orden de meses se evalúa **por filial**, con un método nuevo
  `selectUltimaCargaProcesada(Long estado, Long codigoFilial)` (el de un parámetro queda tal cual por
  si otro lo usa) llamado con la filial de la carga. Si la carga no tiene filial, se usa el
  comportamiento de hoy.
- **A3 (bloquea a un partícipe):** la fase 1 compara los primeros 35 caracteres de la razón social
  contra el nombre de la fila; el lector ARCH pone la razón social completa, así que un nombre de más
  de 35 caracteres sale como «código no coincide con nombre» (bloqueante en filial 2). Corrección: en
  `procesarContenidoArch`, el nombre de la fila es la razón social recortada a 35 caracteres (y vacía
  si es null, sin NPE).
- **Pendientes, no van esta noche:** `.csv` no está en `FileService.EXTENSIONES_PERMITIDAS` (usar
  `.xlsx`); un parcial cercano al aporte no queda bloqueante (A6); las filas PE con atraso salen
  MONTO_INCONSISTENTE (A4); no se usa el préstamo de la línea PDGA (A5); normalización de cédula más
  amplia (A8).
