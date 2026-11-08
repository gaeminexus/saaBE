# DISEÑO — Solicitud de crédito V1, autorización de descuento por rol y reglas del otorgamiento

**Fecha:** 2026-11-07 · **Equipo:** `omen-saa-1` · **Árbitro:** `omen-saa-1-arb`
**Ejecutores:** `omen-saa-1-be` (saaBE) y `omen-saa-1-fe` (saaFE).
**Antecede:** `DISENO-IMPRESOS-OTORGAMIENTO.md` (IM1–IM17). Lo que aquí no se dice, sigue como está ahí.
**Contrato:** `API-CICLO-OTORGAMIENTO.md` (se actualiza en el mismo cambio).

**Fuentes del cliente**, entregadas por el usuario el 2026-11-07. Ninguna entra al repo, porque la corrección
trae capturas con los datos de una socia real. En la OMEN están en
`docs/logica-negocio/crd/modelos/`, sin rastrear, igual que los modelos anteriores:
- «CORRECCION EN LA SOLICITUD DE CRÉDITO» (correcciones a la solicitud impresa);
- «FALTANTES EN LA CREACION DE SOLICITUDES DE CRÉDITOS» (reglas del otorgamiento);
- «Solicitud_de_Credito_ASOPREP-FCPC_V1» (formato nuevo de la solicitud);
- «Autorizacion_Descuento_Rol_ASOPREP-FCPC» (formato nuevo).

**Pedido del usuario:** «Me piden estas correcciones y formatos para la generación de nuevos créditos […] El tercer
archivo ya estaba en reporte pero hay que hacer algunas modificaciones. El último es un formato nuevo. Procedemos
todos».

---

## PARTE A — Impresos

### A1. Solicitud `RPRT_PRST_SLCT`: correcciones pedidas (texto del cliente → regla)

| # | Pedido del cliente | Regla | Fuente |
|---|---|---|---|
| SV1 | «Debe salir el empleador que está registrado en actualización de datos → datos de la entidad → filial» | `EMPLEADOR` = nombre de la **filial** de la entidad. Sale en el párrafo inicial, en la sección 1 y en la sección 4. Deja de leerse de `PRTC.PRTCLGRT`. | `CRD.ENTD.FLLLCDGO` → `CRD.FLLL.FLLLNMBR` |
| SV2 | «Debe salir la frecuencia de pago […] se puede poner mensual» | `Frecuencia de pago` = **`MENSUAL`**, sin la palabra «días». Todos los préstamos del sistema son de cuota mensual. | texto fijo |
| SV3 | «Ingreso mensual de rol: actualización de datos → datos del partícipe → remuneración unificada» | `INGRESO_MENSUAL` = remuneración unificada del partícipe. Deja de leerse de `CRD.PREC`. | `CRD.PRTC.PRTCRMUN` (la última `PRTC` de la entidad, como ya hace el `JOIN pt`) |
| SV4 | «En cuotas vigentes, si es novación, el valor de la última cuota del préstamo que está novando; si es nuevo, 0» | `CUOTAS_VIGENTES` = novación/refinanciamiento: la cuota **total** (`DTPRTTLL`) del préstamo original (`PRSTPROR`), la de la primera cuota no pagada; si ya no tiene pendientes (quedó cancelado al aprobar), la de su última cuota. Nuevo: **0,00** (no en blanco). | `CRD.DTPR.DTPRTTLL` del original |
| SV5 | «En monto máximo a otorgar, el valor total de la cuenta individual» | `MONTO_MAXIMO` = `SALDO_CTA_INDIVIDUAL` (100 %). | misma subconsulta de `APRT` que ya existe |
| SV6 | «En saldo de créditos vigentes, el capital pendiente de todos los préstamos vigentes» | `SALDO_CREDITOS_VIGENTES` = suma del capital pendiente de **todos** los préstamos vigentes de la entidad, **sin contar el que se imprime** (incluye el que se nova, ver A1-bis). Se calcula en el `<query>`; **se retira** el parámetro `P_SALDO_CREDITOS_VIGENTES`. ⛔ **No usar `PRSTSLCP`**: es una columna muerta, nadie la actualiza después de la carga Petro. El capital pendiente se calcula con la **misma regla** que `PagoPrestamoService.calcularSaldoCapitalPendiente` (capital de la tabla menos el capital pagado en `PGPR`); el BE la traduce a SQL y, si no se puede de forma exacta, lo dice antes de inventar otra. | `DTPR` + `PGPR` |
| SV7 | «En descuentos de ley y otros, el valor de la o las cuotas que está pagando de los préstamos» | `DESCUENTOS` = suma de la cuota total (`DTPRTTLL` de la primera cuota no pagada) de los préstamos vigentes de la entidad, sin contar el que se imprime e incluyendo el que se nova (A1-bis). Reemplaza la subconsulta que hoy alimenta `CUOTAS_VIGENTES`. | `CRD.DTPR` |
| SV8 | «Capacidad de pago: el 50 % del ingreso mensual rol» | `CAPACIDAD_PAGO` = `INGRESO_MENSUAL × 0,50`, redondeado a 2 decimales. Sin remuneración → en blanco (IM2). | cálculo en el `<query>` |
| SV9 | «Valor líquido: lo que se vaya a depositar; si es novación, la diferencia de lo que cancela del préstamo novado» | Nuevo = monto. Novación = monto − lo que cancela del original. Ya está así (`PRSTMNSL − PRSTVLSS`). El BE confirma que `PRSTVLSS` es lo que se cancela del original **y que el desembolso real usa ese mismo valor**. Si no coinciden, manda el desembolso. | `CRD.PRST.PRSTVLSS` |
| SV10 | «El espacio para la firma no es amplio y suficiente» | Entre el final de la sección 5 y la línea de firma, **mínimo 60 pt** libres (hoy son unos 15). Línea de 235 pt de ancho. Si la página 1 no alcanza, la sección 5 completa, con la firma, **pasa a la página 2** y no se parte. | layout |

### A1-bis. ⛔ La solicitud se imprime también DESPUÉS de aprobar: el préstamo novado ya está cancelado

El lote sale al **aprobar**, y al aprobar `SustitucionPrestamoServiceImpl.cancelarOriginal` pasa el original a
estado 5. Si SV6 y SV7 miraran solo los vigentes, la misma solicitud diría una cosa antes y otra después de aprobar.
Regla: el original (`PRSTPROR`) **siempre** entra en SV6 y SV7, sea cual sea su estado:
- en SV6 aporta su capital pendiente mientras siga vigente, y `PRSTVLSS` del préstamo nuevo cuando ya se canceló
  (en una novación el VS es solo capital: sin mora ni atrasos);
- en SV7 aporta su cuota según SV4.

«Vigentes» = `PRSTIDST IN (2, 8, 10, 11)` (VIGENTE, DE_PLAZO_VENCIDO, VIGENTE_POR_REVISAR, EN_MORA), el mismo conjunto
que `sustituibles`. **No** usar `selectVigentesByEntidad`, que deja pasar 6, 7, 12 y 13.

### A2. Solicitud: cambios del formato V1 contra el borrador (medidos con diff)

| # | Cambio del V1 | Regla |
|---|---|---|
| SV11 | Sección 4: párrafo nuevo **«Plazo de desembolso: …72 horas hábiles…»** | Se copia **literal** debajo de la cuenta bancaria (IM1). |
| SV12 | Requisito 3: «Último rol de pagos o certificado de ingresos» | Se **quita** «(jubilados: último comprobante de pensión)». |
| SV13 | Sección 7: «Monto pre aprobado» y «Plazo pre aprobado» | Pasan a **«Monto aprobado»** y **«Plazo aprobado»**. Siguen en blanco para llenar a mano. |
| SV14 | Sección 7: campos nuevos **Fecha de solicitud**, **Fecha de aprobación**, **Fecha máxima de desembolso** | `FECHA_SOLICITUD` = la misma de la cabecera. `FECHA_APROBACION` = `PRSTFCAP`, en blanco si no está aprobado. `FECHA_DESEMBOLSO` = el diccionario del cliente dice «fecha de acreditación, automático al desembolsar»: la fecha del desembolso real si existe, en blanco si no. **No se calcula** «aprobación + 72 h» (no hay calendario de feriados); ver pregunta P2. |
| SV15 | Diccionario de campos | No se imprime (IM10). |

Todo lo demás del V1 es igual al borrador y ya está en el reporte.

### A3. Formato nuevo `RPRT_PRST_ADRL` — Autorización de descuento por rol de pagos (`FOR-CRE-AUT-01`)

Nombre: `RPRT_PRST_ADRL` (**A**utorización de **D**escuento por **R**o**L**). Sintaxis compacta, `.jasper` compilado
y fill verificado, **y PDF real mirado** (CLAUDE.md, reportes): se llena por `<query>` con `P_PRSTCDGO`, sin
`fontName="Tahoma"`. Logo por `P_IMAGEN`. Pie: «Impreso por: `P_USUARIO` · Fecha de impresión · Página X de Y» (IM15).

| Campo | Regla |
|---|---|
| Texto fijo | **Literal** del modelo (IM1): destinatario «EMPRESA PÚBLICA DE HIDROCARBUROS DEL ECUADOR – EP PETROECUADOR», «Gerencia / Jefatura de Talento Humano – Unidad de Nómina», el párrafo «Yo, …», las cláusulas PRIMERA a NOVENA, «tres (3) ejemplares», la línea de distribución. Ver P3. |
| `NRO_SOLICITUD` | `PRSTCDGO` |
| `NRO_OPERACION` | `PRSTIDAS` (idAsoprep). Antes de aprobar no existe: sale en blanco y la marca «BORRADOR — sin número hasta aprobar», como el pagaré (IM3). |
| `CIUDAD`, `FECHA_LARGA` | `QUITO` fijo (como IM8), y la fecha de aprobación en letras: «Quito, 27 de octubre de 2026». Sin aprobar: la fecha de impresión. |
| `NOMBRES_APELLIDOS`, `CEDULA` | `ENTD.ENTDRZNS`, `ENTD.ENTDNMID` |
| `CODIGO_EMPLEADO` | `CRD.ENTD.ENTDRLPC` («Rol Petro Comercial», el número de rol con el que Petro identifica al empleado). No hay otro campo de código de empleado. |
| `CARGO`, `UNIDAD`, `LUGAR_TRABAJO` | `PRTC.PRTCCRGA`, `PRTC.PRTCUNAD`, `PRTC.PRTCLGRT` (última `PRTC` de la entidad). Vacío → en blanco (IM2). |
| Condición | «Partícipe activo», texto fijo del modelo. |
| `TIPO_PRESTAMO` | Familia del producto, `CRD.TPPR` (IM9), igual que la solicitud. |
| `COD_DESCUENTO` | `CRD.PRDC.PRDCCDPT` (`Producto.codigoPetro`), el mismo que usa `GeneracionArchivoPetroServiceImpl`. Se imprime solo el código, sin «(PQ / PH / PE)». |
| `MONTO`, `TASA`, `NRO_CUOTAS` | `PRSTMNSL`, `PRSTTSAA`, `PRSTPLZO` |
| `CUOTA` | La cuota mensual que se descuenta por rol: **`DTPRTTLL` de la cuota 1** de la tabla (capital + interés + desgravamen + incendio). El modelo dice «la cuota mensual es la de la tabla de amortización». Ojo: el pagaré usa la cuota **sin** desgravamen (IM5) y la autorización **con**. |
| `MES_PRIMER_DESCUENTO`, `MES_ULTIMO_DESCUENTO` | Mes y año, en letras y mayúsculas («NOVIEMBRE 2026»), del vencimiento de la primera y la última cuota de la tabla (`DTPR`). Sin tabla, en blanco. |
| `USUARIO_ELABORA` | `PRSTUSRG`, como en la solicitud. |
| Firma | Línea, nombre, «C.C.», «FIRMA DEL PARTÍCIPE» y, a su derecha, un **recuadro «Huella dactilar»** de unos 70 × 80 pt. Espacio de firma: el mismo mínimo de SV10. |
| Sección 4 | Dos columnas: «ASOPREP-FCPC — Área de Crédito» y «EP PETROECUADOR — Talento Humano / Nómina», con sus líneas y «Fecha: ____/____/________». |
| Diccionario | No se imprime. |

**Cuándo se imprime** (el modelo dice: «junto con la Solicitud… una vez aprobado el crédito, y antes del desembolso»):
- botón nuevo **«Imprimir autorización»** en `prestamo-edit`, con las mismas reglas que el pagaré (IM17): se bloquea en
  RECHAZADO (7) y ANULADO (12), y antes de aprobar sale BORRADOR;
- el **lote** al aprobar y el botón «Imprimir documentos» pasan a cuatro documentos, en este orden:
  **solicitud → autorización → pagaré → tabla**.

---

## PARTE B — Reglas del otorgamiento («FALTANTES»)

### B0. Lo que hay hoy (medido el 2026-11-07)

- **No hay ninguna validación de capacidad de pago ni de cupo.** Las «bandas» que menciona
  `API-CICLO-OTORGAMIENTO.md` §121 (`ClasificadorBandaService`, `CRD.BNDP`) **no son capacidad de pago**: clasifican la
  cartera por días de mora para la contabilidad. `saveSingle`, `generarTablaAmortizacion` y `aprobar` no miran ni la
  remuneración ni la cuenta individual.
- `CRD.CRDT` (`CreditoMontoAprobacion`: monto mínimo, máximo, «idProceso») es CRUD sin uso.
- La tasa (`PRSTTSAA`) se escribe a mano en la pantalla (por defecto 0).
- **No existe garante** en CRD.
- La aprobación es de **un solo paso** (decisión del usuario del 2026-08-31, `API-CICLO-OTORGAMIENTO.md` §16).
- Los simuladores (`simulador-credito`, `simulador-prestamo`) **no simulan novaciones**. La novación real está en
  `SustitucionPrestamoServiceImpl` (`sustituibles`, `evaluar` → VS, `validarAlGuardar` monto > VS,
  `prepararAprobacion` neto = monto − VS, `cancelarOriginal`).

### B1. Un solo evaluador, usado en todos lados

Servicio nuevo `EvaluacionOtorgamientoService` (`@Local`/`@Stateless`, en `ejb/crd`), sin tabla. Hace un único
cálculo que usan **guardar, generar tabla, aprobar, el simulador de novación y la pantalla**, para que nunca den
números distintos.

```
EvaluacionOtorgamiento evaluar(Long idEntidad, Double monto, Double cuotaNueva, Long idPrestamoActual, Long idPrestamoOriginal)
```
Devuelve (DTO en `ejb/crd/service/dto`):

| Campo | Cálculo |
|---|---|
| `remuneracion` | `PRTC.PRTCRMUN` de la última `PRTC` de la entidad |
| `capacidadPago` | `remuneracion × 0,50` |
| `cuotasVigentes` | Σ cuota total (`DTPRTTLL` de la primera cuota no pagada) de los vigentes `(2, 8, 10, 11)` de la entidad, **sin** el préstamo actual y **sin** el original que se nova (se cancela con el nuevo) |
| `cuotaNueva` | la cuota total del préstamo que se evalúa (`DTPRTTLL` de la cuota 1 si ya tiene tabla; si no, la del simulador) |
| `capacidadOk` | `cuotaNueva + cuotasVigentes ≤ capacidadPago` (tolerancia 0,01) |
| `saldoCuentaIndividual` | `SaldoAporteService` (Σ `APRTVLRR` de tipos activos), igual que la solicitud |
| `saldoCapitalVigentes` | Σ capital pendiente de los mismos vigentes, **sin** el actual ni el original (`calcularSaldosEnLote` / `calcularSaldoCapitalPendiente`; ⛔ no `PRSTSLCP`) |
| `cupoOk` | `saldoCapitalVigentes + monto ≤ saldoCuentaIndividual` |
| `mensajes` | uno por regla incumplida, con los números (ej.: «La cuota 128,71 más las cuotas vigentes 310,00 superan la capacidad de pago 400,00 (50 % de 800,00)») |

En una novación el original se excluye de las dos sumas porque el monto nuevo ya incluye el VS que lo cancela. Eso
cumple el pedido: «el saldo capital pendiente de pago y lo que solicita de crédito no supere el 100 % de la cuenta
individual».

### B2. Dónde se aplica

| Momento | Capacidad (≤ 50 %) | Cupo (≤ 100 % cuenta individual) |
|---|---|---|
| `saveSingle` de un préstamo en 6/1 | — (no hay cuota todavía) | **bloquea**, 422 |
| `generarTablaAmortizacion` | **no bloquea**: la tabla se genera y la respuesta trae la evaluación, para que la pantalla la muestre como aviso y se ajuste monto o plazo | informa |
| `aprobar` (antes de numerar el idAsoprep, ~`PrestamoServiceImpl:448`) | **bloquea**, 422 | **bloquea**, 422 |
| Sin remuneración registrada (nula o 0) | al aprobar, 422: «Registre la remuneración unificada del partícipe en Actualización de datos → Datos del partícipe» | — |

- REST nuevo: `GET /prst/evaluarOtorgamiento/{id}` (préstamo guardado) y `POST /prst/evaluarOtorgamiento` (cuerpo
  `{idEntidad, monto, cuotaNueva, idPrestamoOriginal}`, para el simulador). Mismo DTO.
- Los 422 con el formato de siempre, `{ "mensaje": … }`.
- **No pasan** por estas reglas los préstamos que no nacen del otorgamiento: carga Petro, migración y
  reestructuración de un préstamo en mora (`PRSTRSTR = 1`), que no es un crédito nuevo.

### B3. Garante — tabla nueva `CRD.GRPR` (script `crd/sql/369`, lo corre el usuario antes del EAR)

Columnas: `GRPRCDGO` (PK, `SQ_GRPRCDGO`), `PRSTCDGO` (FK, obligatorio), `ENTDCDGO` (FK, si el garante es partícipe),
`GRPRNMBR`, `GRPRNMID` (cédula), `GRPRDRCC`, `GRPRTLFN`, `GRPRCRRE`, `GRPRESTD` (1/0) y la auditoría
`USRG/FCRG/USMD/FCMD`. Un solo garante activo por préstamo (índice único sobre los activos).

- Mapeo con los cinco archivos del estándar (`GarantePrestamo`, constante en `NombreEntidadesCredito`), REST
  `@Path("grpr")` y además `GET /grpr/prestamo/{idPrestamo}` (el activo, o 204).
- Reglas: es **opcional**; solo se agrega, cambia o anula mientras el préstamo está en 6 o 1 (después, 422); la cédula
  del garante no puede ser la del deudor; si viene `ENTDCDGO`, nombre y cédula se toman de `CRD.ENTD`, no de la
  pantalla; quitarlo = `GRPRESTD = 0`, no se borra.
- **Pagaré `RPRT_PRST_PGRE`:** con garante activo se agrega un **segundo bloque de firma** (línea, nombre, «C.C.» y la
  leyenda **«GARANTE»**). Es el bloque que el modelo traía repetido (P1 de `DISENO-IMPRESOS-OTORGAMIENTO`). El texto
  legal **no cambia** (IM1) hasta que el usuario diga si lleva una frase «por aval» (P5). Sin garante, el pagaré queda
  como hoy.
- La solicitud no cambia: el formato V1 no tiene lugar para el garante.

### B4. Niveles de aprobación — ⛔ BLOQUEADO hasta que el cliente los defina

El cliente pide «coordinar la aprobación de acuerdo a los niveles de aprobación», pero no dice cuáles son, y el usuario
decidió el 2026-08-31 que **no** había niveles. Hace falta confirmar que se revierte esa decisión y la tabla del
cliente: rangos de monto → quién aprueba (asistente, jefe de crédito, gerencia, CAD) y cómo se identifica a cada uno en
el sistema (usuario o perfil). Mientras tanto **no se programa nada** de niveles y la sección 7 de la solicitud sigue
con «Nivel de aprobación» en blanco.

### B5. Tasa del 9 %

- Constante única `TASA_OTORGAMIENTO = 9.0` en una interfaz de `com.saa.rubros` (`ParametroOtorgamiento`). La reusa
  `ProcesoMoraPrestamoServiceImpl.TASA_POR_DEFECTO`, que hoy repite el 9 a mano.
- **BE (`saveSingle`, préstamos en 6/1):** tasa nula o 0 → 9. **Otra** tasa → 422: «La tasa de interés de los créditos
  es 9 %». Se escribe en `tasa`, `tasaNominal` y `tasaEfectiva`, como hoy.
- **FE:** el préstamo nuevo arranca con 9,00 y el campo es de **solo lectura**. Los dos simuladores arrancan en 9,00.
- No aplica a reestructuraciones de préstamos en mora ni a préstamos ya aprobados.

### B6. Simulador de novaciones (quirografarios y emergentes)

- **BE:** `POST /prst/simularNovacion` con `{ idPrestamoOriginal, monto, plazo }`:
  1. el original tiene que ser **quirografario o emergente** (`PRDCCDPT IN ('PQ','PE')`) y **sustituible**, con la misma
     validación que `SustitucionPrestamoServiceImpl.evaluar` (sin mora; VS = capital). Si no, 422 con el motivo;
  2. `monto > VS`, como `validarAlGuardar`;
  3. simula la tabla nueva con `simularCreditoNuevo`, a la tasa del 9 % y con el **producto del original**;
  4. responde `{ original: {idPrestamo, idAsoprep, producto, cuotaActual (DTPRTTLL), vs, desgloseVs}, simulacion (como
     /simularCreditoNuevo), valorLiquido = monto − VS, evaluacion (B1, sin el original) }`.

  No graba nada.
- **FE (`simulador-credito`):** pestaña nueva **«Novación»**:
  1. buscar el partícipe;
  2. elegir un préstamo sustituible (`GET /prst/sustituibles/{idEntidad}?tipo=NOVACION`, filtrado a PQ/PE);
  3. escribir monto y plazo;
  4. ver la cuota actual contra la nueva, el VS, el **valor líquido a recibir**, la tabla y el semáforo de capacidad
     y cupo.

  La pestaña del crédito nuevo también muestra la evaluación cuando se elige un partícipe.

### B7. Pantalla del préstamo (`prestamo-edit`)

- Panel **«Evaluación del otorgamiento»** (`GET /prst/evaluarOtorgamiento/{id}`). Muestra:
  - remuneración y capacidad del 50 %;
  - cuotas vigentes y cuota nueva;
  - cuenta individual, saldo de créditos vigentes y monto;
  - dos indicadores (cumple / no cumple) con los mensajes.

  Se refresca al guardar y al generar la tabla. «Aprobar» sigue habilitado: la garantía es el 422 del BE, cuyo
  mensaje se muestra tal cual.
- Sección **«Garante»** (B3): casilla «Tiene garante», y búsqueda de partícipe por cédula o datos a mano de un tercero.
  Editable solo en 6/1.
- La tasa es de solo lectura (B5).
- Botón **«Imprimir autorización»** y el lote de cuatro documentos (A3).

---

## PARTE C — Reparto y orden

| Paso | Quién | Qué |
|---|---|---|
| 1 | BE | A1, A1-bis, A2 (solicitud) y A3 (autorización): `.jrxml` + `.jasper` + fill verificado + **PDF real mirado por el camino del lote**, con un préstamo nuevo y una novación. Solo lectura. Nada en producción. El lote pasa a cuatro documentos. |
| 2 | BE | B1, B2, B5 y B6: evaluador, validaciones, tasa y simulador de novación. |
| 3 | BE | B3: entidad y capas del garante (la tabla la crea el 369) y el bloque «GARANTE» del pagaré. |
| 4 | FE | B5, B6 y B7, contra el contrato actualizado. |
| 5 | BE | `API-CICLO-OTORGAMIENTO.md`: corregir §121 (las bandas no son capacidad de pago) y agregar evaluación, tasa, garante, simulador de novación y la autorización en el lote. Espejo en saaFE `docs/crd/`. |
| — | nadie | B4 (niveles), hasta la respuesta del cliente. |

Despliegue: el **369 antes** del EAR.

## PARTE D — Preguntas al usuario (no bloquean, salvo P1)

- **P1 (bloquea B4).** ¿Se revierte la decisión del 2026-08-31 de no tener niveles de aprobación? Si es así: montos →
  quién aprueba y cómo se le identifica en el sistema.
- **P2.** «Fecha máxima de desembolso»: ¿la fecha real del desembolso (diccionario del cliente; en blanco hasta que
  ocurra) o aprobación + 72 horas hábiles sin feriados? Por defecto, la real.
- **P3.** Tasa del 9 %: ¿todos los productos, también hipotecarios y prendarios? Por defecto, todos los créditos nuevos.
- **P4.** Capacidad de pago de **jubilados** (no tienen rol): ¿su pensión complementaria (`VPPC`) o la remuneración
  unificada registrada? Por defecto, la remuneración unificada para todos; sin ella no se aprueba.
- **P5.** Garante: ¿tiene que ser partícipe o puede ser cualquiera? ¿El pagaré lleva una frase «por aval»? Por defecto,
  cualquiera y sin frase.
- **P6.** La autorización va dirigida, literal, a **EP PETROECUADOR**. Para un partícipe de **otra filial**, ¿el nombre
  de la filial (como SV1) o literal? Por defecto, literal.
