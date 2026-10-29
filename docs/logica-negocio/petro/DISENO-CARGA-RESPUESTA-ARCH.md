# Diseño — carga de la respuesta de ARCH (filial 2)

**Estado: BORRADOR, con decisiones abiertas (§6).** Lo escribió omen-saa-1-arb el 2026-10-29 a
pedido del usuario. **No despachar a los ejecutores hasta cerrar el §6 y tener la salida del
`crd/sql/332` (PARTE B).**

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
