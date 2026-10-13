# DISEÑO — Impresos del otorgamiento: solicitud de crédito, pagaré (modelo del cliente) y tabla de amortización

**Fecha:** 2026-10-13 · **Equipo:** `omen-saa-1` · **Árbitro:** `omen-saa-1-arb`
**Reemplaza:** el pagaré provisional de `DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md` §4.
**Contrato:** `API-CICLO-OTORGAMIENTO.md` §7.3 y §7.4 (espejo en saaFE `docs/crd/`).
**Modelos del cliente:** los dos Word entregados por el usuario el 2026-10-13 («MODELO_DE_PAGARE_V2» y
«Solicitud_de_Credito_ASOPREP-FCPC_-BORRADOR»). **No están en el repo**, porque el pagaré de ejemplo trae
la cédula y la cuenta de un socio real. En la OMEN, su texto extraído está en
`docs/logica-negocio/crd/modelos/*.txt`, **sin rastrear**: no se commitea.

---

## 1. Pedido del usuario (2026-10-13), textual

> «Estos son los dos formatos que se deben imprimir al momento de generar un préstamo nuevo pero con
> los logos de ASOPREP. Genera los jasper para que se impriman junto con la tabla de amortización al
> otorgar un préstamo.»

## 2. Los tres documentos

| # | Reporte | Qué es | Estado |
|---|---|---|---|
| 1 | `RPRT_PRST_SLCT` | **Solicitud de crédito**, formato `FOR-CRE-SOL-01` del cliente | **Nuevo** |
| 2 | `RPRT_PRST_PGRE` | **Pagaré a la orden**, MODELO V2 del cliente | **Se reescribe**: deja de ser provisional |
| 3 | `RPRT_TBLA_ACML` | Tabla de amortización | Ya existe y no cambia. Solo se verifica que salga con logo |

Los tres llevan el **logo de ASOPREP** con `P_IMAGEN`, que inyecta `ReporteServiceImpl` desde
`/rep/img/logo_aso.jpeg`, igual que hoy. Son `.jrxml` en sintaxis compacta, más el `.jasper` compilado y
con el **fill verificado** (CLAUDE.md, sección de reportes). Ninguno usa `fontName="Tahoma"`.

## 3. Reglas

| # | Regla |
|---|---|
| IM1 | **Texto legal literal.** El texto fijo de los dos modelos se copia **palabra por palabra**, incluidas las formas «(emos)», «(nosotros)» y «mis(nuestos)». No se corrige ni se conjuga: el texto es del cliente y lo validó su asesoría legal. Lo único que cambia son los «CAMPOS». Las erratas evidentes («el atabla», «nuestos», «eximiento») se listan en el reporte del BE y **no se corrigen** sin la palabra del usuario. |
| IM2 | **Un dato sin fuente sale en blanco**, como una línea para llenar a mano (`________`). **Nunca se inventa** ni se pone un valor por defecto que parezca real. Antes de programar, el BE mide la fuente de **cada** campo (tabla y columna, o cálculo existente) y la reporta en una tabla: *campo → fuente → «en blanco» si no hay*. |
| IM3 | **Número del pagaré = idAsoprep** («PAGARÉ A LA ORDEN No.», «Crédito No:» y «tabla de amortización No.»). Antes de aprobar no existe (U6), así que el pagaré **no se imprime antes de aprobar**: el reporte, sin `PRSTIDAS`, sale con la marca «BORRADOR — sin número hasta aprobar» y el número en blanco. **Se elimina `NVL(PRSTIDAS, PRSTCDGO)`**, porque mostraba el código interno como si fuera el número del pagaré. |
| IM4 | **Montos en letras**, con el formato exacto del modelo: `SESENTA Y CUATRO MIL OCHOCIENTOS DÓLARES DE LOS ESTADOS UNIDOS DE AMÉRICA CON 00/100, (USD: 64.800,00)`. Va para el monto y para la cuota. El plazo va como `CIENTO CUARENTA Y CUATRO (144)` y la tasa como `NUEVE CON 00/100 (9.0) %`. Se usa `NumeroALetrasUtil` y, si su salida no coincide, se adapta **ese** utilitario o se agrega una variante, sin duplicar la lógica en el `.jrxml`. |
| IM5 | **La cuota del pagaré** es la cuota **sin desgravamen**, porque el modelo dice «…de USD 746.97, **más** el valor del seguro de desgravamen». El BE mide qué incluye `PRSTVLCT` y, si incluye seguros, toma capital más interés de la cuota fija desde la tabla (`DTPR`). |
| IM6 | **Deudor y cónyuge.** Si el partícipe tiene cónyuge registrado (el BE mide dónde), el pagaré sale como en el modelo: «Yo (nosotros), DEUDOR, cédula…, y CÓNYUGE, cédula…», con las dos firmas. Sin cónyuge sale solo el deudor y solo su firma. El modelo **repite dos veces** el bloque de firmas; se imprime **una vez** hasta que el usuario diga si el segundo es para otra persona (§6). |
| IM7 | **Cuenta de débito** («cuenta AHORROS No. … de BANCO …») = la cuenta bancaria del partícipe, la misma de la sección 4 de la solicitud. El BE mide de dónde sale, y sin cuenta sale en blanco. |
| IM8 | **Texto fijo de lugar:** «en la ciudad de QUITO», los jueces de Pichincha y «EPPETROECUADOR» quedan **fijos** como en el modelo. La **ciudad del domicilio** del deudor sí es campo. |
| IM9 | **Tipo de crédito** = la familia del producto (HIPOTECARIO, PRENDARIO o QUIROGRAFARIO), que sale de `CRD.TPPR`, la misma fuente que la contabilidad. |
| IM10 | **Solicitud, «No. Solicitud»** = `PRSTCDGO`, que existe desde que se guarda. **«Fecha»** = la fecha de registro del préstamo. **«Código: FOR-CRE-SOL-01»** es texto fijo. El **anexo «Diccionario de campos» NO se imprime**: el mismo modelo lo dice. |
| IM11 | **Solicitud, sección 2:** se marca **Nuevo** o **Refinanciamiento/novación**, este último con el número del original (su idAsoprep), según `PRSTPROR` y `PRSTRSTR`/`PRSTESNV`. Una reestructuración también marca la segunda casilla. |
| IM12 | **Solicitud, sección 3 (capacidad de pago):** solo se llenan los valores que el sistema **ya calcula o guarda**: saldo de la cuenta individual, saldo y cuotas de los créditos vigentes, y el valor líquido, que es el monto menos lo que se cancela (el VS de una novación) o el neto que efectivamente se desembolsa. Ingreso, descuentos, capacidad y monto máximo salen **solo si existen**. Si no, van en blanco (IM2). No se crea un cálculo de capacidad nuevo en esta tarea. |
| IM13 | **Solicitud, secciones 6 y 7** (requisitos y resolución): las casillas van **vacías**, para marcar a mano. «Elaborado por» es el usuario que registró el préstamo, si se guarda. «Revisado» y «Aprobado» van en blanco, salvo que el préstamo guarde el usuario aprobador. |
| IM14 | **Casillas ☐.** La fuente por defecto del PDF no tiene el carácter `☐`, así que se dibujan como rectángulos (`kind="rectangle"`) o con texto que sí exista, como `[ X ]` o `[   ]`. Se confirma **mirando el PDF**. |
| IM15 | **Pie de la solicitud:** «Impreso por: `P_USUARIO` · Fecha de impresión: fecha y hora · Página X de Y». |
| IM16 | **Un solo PDF al otorgar.** Al **aprobar** con éxito, la pantalla genera **un PDF con los tres documentos en orden**: solicitud, pagaré y tabla. Así se imprime de una vez y el navegador no bloquea tres ventanas. Para eso hay un endpoint **genérico** de lote en `rprt` (§4), que reusa el llenado actual de cada reporte y concatena los `JasperPrint`. Además quedan los botones individuales: «Imprimir solicitud», «Imprimir pagaré», «Imprimir tabla» y «Imprimir documentos», que saca los tres. |
| IM17 | **Cuándo se puede imprimir cada uno:** la solicitud desde GENERADO (1) o PENDIENTE (6) en adelante, salvo RECHAZADO (7). El pagaré necesita la tabla y se bloquea en RECHAZADO y en ANULADO (12); sin número hasta aprobar (IM3). La tabla como hoy. «Imprimir documentos» solo con idAsoprep, es decir, ya aprobado. |

## 4. Endpoint nuevo — `POST /rest/rprt/generarLote`

```json
{ "modulo": "crd", "formato": "PDF",
  "reportes": [
    { "nombreReporte": "RPRT_PRST_SLCT", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } },
    { "nombreReporte": "RPRT_PRST_PGRE", "parametros": { "P_PRSTCDGO": 5555 } },
    { "nombreReporte": "RPRT_TBLA_ACML", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } }
  ] }
```
- Responde `application/pdf` con un solo documento y los reportes en el orden pedido, igual que
  `/rprt/generar`.
- Solo `PDF`. Con otro formato, o con `reportes` vacío, devuelve **400**.
- Si **uno** falla, falla todo, con el nombre del reporte que falló en el mensaje. No se entrega un PDF
  incompleto.
- Cada reporte se llena **exactamente** como lo haría `/rprt/generar`: la misma inyección de
  `P_IMAGEN`, la misma conversión de tipos y la misma elección entre la conexión JDBC y la colección.
  Después se exporta una sola vez con `SimpleExporterInput.getInstance(List<JasperPrint>)`.

## 5. Archivos

- **BE:**
  - `rep/crd/RPRT_PRST_SLCT.jrxml/.jasper` (nuevo);
  - `rep/crd/RPRT_PRST_PGRE.jrxml/.jasper` (se reescribe);
  - `ReporteServiceImpl` y `ReporteRest` (el lote);
  - `NumeroALetrasUtil`, si hace falta.
- **FE:** `prestamo-edit.component.*` (los botones y la impresión después de aprobar) y el servicio de
  reportes (`generarLote`).

## 6. Preguntas abiertas al usuario (no bloquean la construcción)

- **P1.** El modelo de pagaré trae **dos veces** el bloque de firmas «DEUDOR / CÓNYUGE». ¿El segundo es
  para un **garante** o avalista, o se repitió por error? Mientras no responda, se imprime una vez.
- **P2.** El modelo **no tiene lugar ni fecha de emisión**. Un pagaré normalmente los lleva, con lugar y
  fecha de suscripción. ¿Se agrega «Quito, {fecha}» o se deja como está? Mientras no responda, se deja
  como está (IM1).
- **P3.** Las erratas del texto legal (IM1): ¿se corrigen?
- **P4.** ¿El logo correcto es `rep/img/logo_aso.jpeg`, el que ya sale en certificados y tablas?
