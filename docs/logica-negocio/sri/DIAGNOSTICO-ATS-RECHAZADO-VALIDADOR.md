# Por qué el validador del SRI rechaza nuestro ATS — diagnóstico contra un archivo autorizado

**Equipo:** `omen-saa-2` · **Fecha:** 2026-09-09 · **Encargo:** el usuario corrió el ATS generado
por `POST /rest/ats/generar` contra el validador oficial del SRI y fue rechazado.

**Insumo que cambia todo:** el usuario entregó un ATS **realmente autorizado por el SRI** —
`AT-072026 ASoprep.xml` (julio 2026, 79 compras, 19 ventas)—. Hasta hoy el generador se había
escrito **contra la ficha técnica de 93 páginas**, no contra un archivo aceptado. Este documento
compara los dos, elemento por elemento.

> ⛔ **Regla que sale de esto, y es la más cara del frente:** *una especificación no reemplaza un
> ejemplar aceptado.* El generador es sintácticamente razonable, cita la ficha técnica en cada
> decisión, y aun así emite tres elementos que **no existen** en el esquema real y le pone otro
> nombre a cuatro más. Ninguna lectura de la ficha lo habría detectado. **Un archivo autorizado es
> la fuente de verdad; la ficha es la explicación.**

---

## 1. Los errores del validador, y a qué corresponden

| Error del validador | Causa real |
|---|---|
| `El elemento 'razonSocial' contiene un valor incorrecto: 'ASOCIACION DEL FONDO COMPLEMENTARIO…'` | Mandamos la razón social completa (**148 caracteres**). El autorizado manda **`ASOPREP`** |
| `El elemento 'parteRel' contiene un valor incorrecto: ''` (×28) | `Titular.parteRelacionada` está **NULL en todos los proveedores** — nunca se capturó ni se hizo backfill (fase 3, §8.2 del levantamiento: *"quedan en NULL hasta que alguien los capture a mano"*). El XSD exige `SI`/`NO` |
| `Contenido Invalido se encontro 'tipoProv' cuando se esperaba '{fechaRegistro}'` (×28) | **`tipoProv` no va en `<detalleCompras>`.** El archivo autorizado tiene **cero** ocurrencias de `tipoProv` en sus 79 compras. Después de `parteRel` el esquema espera `fechaRegistro` |
| `El elemento 'tipoProv' contiene un valor incorrecto: ''` (×28) | Consecuencia del anterior: además de sobrar, va vacío |

**Los cuatro se arreglan en el generador. Uno de ellos además necesita dato en la base** (`parteRel`).

---

## 2. `<detalleCompras>` — nuestro vs. el autorizado

Secuencia **real** del archivo autorizado (`AT-072026 ASoprep.xml`):

```
codSustento · tpIdProv · idProv · tipoComprobante · parteRel · fechaRegistro ·
establecimiento · puntoEmision · secuencial · fechaEmision · autorizacion ·
baseNoGraIva · baseImponible · baseImpGrav · baseImpExe · montoIce · montoIva ·
valRetBien10 · valRetServ20 · valorRetBienes · valRetServ50 · valorRetServicios ·
valRetServ100 · valorRetencionNc · totbasesImpReemb ·
pagoExterior{ pagoLocExt · paisEfecPago · aplicConvDobTrib · pagExtSujRetNorLeg } ·
[formasDePago{ formaPago }] ·
air{ detalleAir{ codRetAir · baseImpAir · porcentajeAir · valRetAir } } ·
estabRetencion1 · ptoEmiRetencion1 · secRetencion1 · autRetencion1 · fechaEmiRet1
```

Contra `GeneradorAtsServiceImpl.writeDetalleCompra()` (líneas 478-509):

| Elemento | Nuestro | Autorizado | Veredicto |
|---|---|---|---|
| `codSustento` … `tipoComprobante` | ✅ | ✅ | igual |
| `parteRel` | **vacío** | `NO` | 🔴 **valor** |
| `tipoProv` | se escribe, vacío | **no existe** | 🔴 **sobra — rompe la secuencia** |
| `denopr` | se escribe | **no existe** | 🔴 **sobra.** Y el nombre está mal escrito: la ficha lo llama `denoProv`, no `denopr` |
| `fechaRegistro` … `montoIva` | ✅ | ✅ | igual |
| `valRetBien10`, `valRetServ20`, `valorRetBienes`, `valRetServ50`, `valorRetServicios`, `valRetServ100`, `valorRetencionNc`, `totbasesImpReemb` | **no se escriben** | todos, con `0.00` | 🟠 el XSD los aceptó ausentes, pero ver §5 |
| `pagoExterior` (4 hijos) | **no se escribe** | siempre presente | 🟠 |
| `formasDePago` | **no se escribe** | en 35 de 79 | 🟠 |
| `air` / `detalleAir` | **no se escribe** | en las que hubo retención | 🔴 **ver §5 — es el corazón del anexo** |
| `estabRetencion1`, `ptoEmiRetencion1`, `secRetencion1`, `autRetencion1`, `fechaEmiRet1` | **no se escriben** | presentes | 🔴 **ver §5** |

---

## 3. `<detalleVentas>` — el validador no llegó, pero está peor

Secuencia real del autorizado:

```
tpIdCliente · idCliente · parteRelVtas · tipoComprobante · tipoEmision ·
numeroComprobantes · baseNoGraIva · baseImponible · baseImpGrav · montoIva ·
montoIce · valorRetIva · valorRetRenta · formasDePago{ formaPago }
```

Contra `writeDetalleVenta()` (líneas 512-536):

| Nuestro | Autorizado | Veredicto |
|---|---|---|
| `parteRel` | **`parteRelVtas`** | 🔴 **nombre distinto** |
| `tipoCliente` | **no existe** | 🔴 sobra |
| `denoCli` | **no existe** | 🔴 sobra |
| `tipoEm` = `"E"` | **`tipoEmision`** = `F` | 🔴 nombre **y** valor: las 19 ventas del autorizado son `F`, y nuestro código fija `"E"` con un comentario que dice *"confirmado como default para esta empresa"* |
| `numeroComprob` | **`numeroComprobantes`** | 🔴 nombre distinto |
| — | `valorRetIva`, `valorRetRenta` | 🔴 faltan |
| — | `formasDePago` | 🟠 falta |

> **La lista de errores que trajo el usuario llega hasta la línea 689 y no menciona ventas.** O el
> validador abortó, o el pegado está cortado. **Hace falta el ZIP que generamos para confirmarlo** —
> pero el código emite nombres que no existen en el archivo autorizado, así que esto no es una
> sospecha: es una diferencia medida contra un ejemplar aceptado.

---

## 4. Dos secciones enteras que no generamos

`grep` sobre `GeneradorAtsServiceImpl.java`: **cero ocurrencias** de una y de otra.

| Sección | Qué trae el autorizado | Nota |
|---|---|---|
| `<ventasEstablecimiento>` | `ventaEst{ codEstab=001 · ventasEstab=26445.17 · ivaComp=0.00 }` | Va después de `<ventas>`. El dato ya lo tenemos: `contarEstablecimientosActivos()` ya cuenta establecimientos y `totalVentas` ya se calcula |
| `<rendFinancieros>` | 1 registro: retención que **le practicaron a ASOPREP** sobre rendimientos de inversiones — `retenido` · `idRetenido` (RUC de la financiera) · `parteRelFid` · `ahorroPN` · `ctaExenta` · `retenciones{ detRet{ … airRend{ detalleAirRen{ codRetAir=323Q · deposito · baseImpAir · porcentajeAir=3.00 · valRetAir } } } }` | **Aplica a ASOPREP por ser FCPC con inversiones.** El dato no está en el modelo hoy: es la retención que el banco le hace, no una que ella emita |

---

## 5. 🔴 Lo que preocupa más que el rechazo

**El archivo puede llegar a validar y seguir siendo una declaración falsa.**

El XSD **aceptó** que omitiéramos `<air>`, `estabRetencion1..fechaEmiRet1` y los ocho campos de
retención de IVA — el validador no se quejó de ninguno. Pero eso significa que nuestro ATS
**declara CERO retenciones practicadas**, y ASOPREP es agente de retención: el archivo autorizado
de julio trae una retención por casi cada compra (`codRetAir` 303, 320, 3440…, con su comprobante
de retención completo).

El comentario que las omite está en el código, y es honesto sobre lo que hizo:

```java
// Retenciones de IVA/renta por documento, pago/exterior (Tabla 13), reembolsos
// detallados, banano, dividendos: fuera de alcance de esta ronda, ver §10 -- no se
// escriben (son opcionales cuando no aplican).
```

**«Opcionales cuando no aplican» es cierto para banano y dividendos. Para las retenciones de un
agente de retención, no aplican nunca es falso.** Un ATS que valida y declara cero retenciones es
peor que uno que se rechaza: el rechazo avisa, esto no.

---

## 6. El arreglo

### 6.1 Sin decisión de negocio — el generador

1. **`razonSocial`**: mandar el nombre corto. `Facturador` ya tiene `nombreComercial` (columna
   `NOMBRECOMERCIAL`); usar ese y caer a `razonSocial` sólo si viene vacío. ⚠️ **Confirmar con un
   `SELECT` qué tiene hoy `NOMBRECOMERCIAL` de este facturador** antes de darlo por bueno.
2. **`<detalleCompras>`**: borrar `tipoProv` y `denopr`.
3. **`<detalleVentas>`**: `parteRel`→`parteRelVtas`, `tipoEm`→`tipoEmision`,
   `numeroComprob`→`numeroComprobantes`; borrar `tipoCliente` y `denoCli`; agregar `valorRetIva` y
   `valorRetRenta`.
4. **Completar `<detalleCompras>`** con los ocho campos de retención de IVA, `pagoExterior`,
   `formasDePago`, `air`/`detalleAir` y el bloque `estabRetencion1..fechaEmiRet1`, tomando los
   datos de la retención de compra que ya está en el modelo (`CBR.RTV2`/`DRV2` y las de compra).
5. **Agregar `<ventasEstablecimiento>`**.

### 6.2 Con decisión de negocio

| Qué | Por qué no lo decide un agente |
|---|---|
| **`parteRel` / `parteRelVtas`** | Hoy `NULL` en todos los titulares. En el autorizado son `NO` los 79 + los 19. Poner `NO` por defecto es una **afirmación tributaria**, no un default técnico: la tiene que respaldar contabilidad. Y hay que decidir si además se captura en la pantalla de titulares para marcar las excepciones |
| **`tipoEmision`** | El autorizado dice `F` en las 19 ventas; nuestro código fija `E`. Hay que confirmar cuál corresponde |
| **`<rendFinancieros>`** | Es un frente aparte: el dato (retenciones que le practican a ASOPREP sobre inversiones) no está en el modelo. Decidir si entra ahora o después |

---

## 7. Lo que NO se toca

- El `codSustento` y su resolución automática: quedó validado por el propio validador (ningún error
  lo menciona) y los valores `01`/`02` del autorizado coinciden con los que produce nuestra regla.
- La pantalla de generación (`cxc/reportes/ats`), que hizo lo suyo bien: generó, empaquetó y
  descargó.

---

## 8. Nota de alcance

`sri` **no es alcance formal de `omen-saa-2`** (según `REGISTRO-RESERVAS-EQUIPOS.md` §2 el bloque es
de `omen-saa-3` / `lap-saa-1`). Este diagnóstico se hace por pedido directo del usuario. El paquete
`com.saa.ejb.sri` no lo toca nadie desde el **2026-08-30** (`f08e92b2`), así que no hay riesgo de
pisar trabajo ajeno — pero si el arreglo se implementa, conviene que los otros árbitros lo sepan.

---

# ANEXO — Especificación de implementación (2026-09-09, urgente)

El usuario ordenó arrancar todo: **las declaraciones se suben hoy**. Este anexo es el contrato que
implementa el BE; todo lo de acá está medido contra `AT-072026 ASoprep.xml`, no supuesto.

## A.1 🔴 Dos hallazgos nuevos, posteriores al diagnóstico

### A.1.1 En `<ventas>` la factura NO es `01`, es `18`

El autorizado tiene **18 ventas con `tipoComprobante` = `18`** y **1 con `04`** (nota de crédito).
Sus 79 compras usan `01`. Nuestro `acumularVenta()` pasa `f.getTipoComprobante()` **crudo**, o sea
que emitiría `01` en ventas.

**`CATALOGO-ATS.md` §4 no cubre esto**: su Tabla 4 documenta el lado compras. El archivo autorizado
es la única evidencia que tenemos.

**Mapeo a aplicar en `<ventas>` (y sólo ahí):** factura → `18`; nota de crédito → `04`; nota de
débito → `05`. Cualquier otro valor: dejarlo como está **y emitir un aviso**, no inventar.

### A.1.2 Las retenciones que nos practican en ventas no están en el modelo

`<detalleVentas>` pide `valorRetIva` y `valorRetRenta` — la retención que **el cliente nos hizo**.
En el autorizado no son cero: 3 ventas con retención de IVA (180 · 3.45 · 461.41) y 2 con retención
de renta (11.50 · 66.38).

`CBR.RTV2` **no sirve**: tiene `FACTURADOR` + `PROVEEDOR`, es una retención que **emitimos
nosotros**, no una recibida. **Buscar primero** si existe otra tabla de retenciones recibidas sobre
ventas. Si no existe: emitir `0.00`, **agregar un aviso por cada venta afectada** y **reportarlo** —
no inventar el valor y no dejarlo silencioso.

## A.2 De dónde sale cada dato que falta en `<compras>`

Fuente: `PGS.RCV2` (`RetencionCompraV2`, el comprobante de retención que emitimos al proveedor) y
`PGS.DRC2` (`DetalleRetencionCompraV2`, sus líneas).

**Enlace documento ↔ retención:** `DRC2.DOCRESAUTORIZACION` contra la `AUTORIZACION` del documento
de compra. Si un documento no tiene retención, todos los campos van en `0.00` y el bloque `air` y
`estabRetencion1..fechaEmiRet1` **no se escriben** (así está en el autorizado).

| Elemento ATS | Origen |
|---|---|
| `valRetBien10` `valRetServ20` `valorRetBienes` `valRetServ50` `valorRetServicios` `valRetServ100` | Líneas de `DRC2` con `CODIMPUESTO` = IVA, repartidas por `CODRETENCION` según la Tabla 11 (ver A.3). El resto en `0.00` |
| `valorRetencionNc`, `totbasesImpReemb` | `0.00` — sin fuente hoy, igual que en el autorizado |
| `pagoExterior` | Constante: `pagoLocExt`=`01`, `paisEfecPago`=`NA`, `aplicConvDobTrib`=`NA`, `pagExtSujRetNorLeg`=`NA`. Es así en las 79 compras del autorizado |
| `formasDePago` / `formaPago` | `DRC2.DOCRESFORPAGO` (Tabla 13). En el autorizado aparece en 35 de 79: **si no hay dato, no se escribe el bloque** |
| `air` / `detalleAir` (`codRetAir` `baseImpAir` `porcentajeAir` `valRetAir`) | Líneas de `DRC2` con `CODIMPUESTO` = RENTA → `CODRETENCION`, `BASEIMPONIBLE`, `PORCENTAJERETEN`, `VALORRETEN`. Una `<detalleAir>` por línea |
| `estabRetencion1` `ptoEmiRetencion1` `secRetencion1` `autRetencion1` `fechaEmiRet1` | Cabecera `RCV2`: `NUMESTABLECIMIENTO`, `NUMPTOEMISION`, `SECUENCIAL`, `AUTORIZACION`, `FECHA` |

## A.3 Tabla 11 — el reparto de la retención de IVA en seis campos

| `CODRETENCION` | % | Campo ATS |
|---|---|---|
| 9 | 10% | `valRetBien10` |
| 10 | 20% | `valRetServ20` |
| 1 | 30% | `valorRetBienes` |
| 11 | 50% | `valRetServ50` |
| 2 | 70% | `valorRetServicios` |
| 3 | 100% | `valRetServ100` |

⚠️ **En el autorizado el único campo distinto de cero es `valRetServ100`** — ASOPREP retiene el
100% del IVA. Los otros cinco existen y van en `0.00`. **Un `CODRETENCION` que no esté en esta tabla
va a un aviso, no a un campo elegido a dedo.**

## A.4 `<ventasEstablecimiento>` — estructura exacta

```xml
<ventasEstablecimiento><ventaEst>
  <codEstab>001</codEstab><ventasEstab>26445.17</ventasEstab><ivaComp>0.00</ivaComp>
</ventaEst></ventasEstablecimiento>
```

Va **después** de `</ventas>`. Un `<ventaEst>` por establecimiento activo. `ivaComp` en `0.00`.

⚠️ En el autorizado `totalVentas` = `26445.16` y `ventasEstab` = `26445.17`: **difieren en un
centavo**. Es redondeo del declarante, no una regla — calcular los dos del mismo total y no forzar
que coincidan al centavo con el autorizado.

## A.5 Decisiones que el usuario tomó al ordenar «arranca todo»

| Campo | Valor | Reversible |
|---|---|---|
| `parteRel` / `parteRelFid` (compras) y `parteRelVtas` (ventas) | **`NO`** cuando el titular está en `NULL` | Sí — se cambia en `Titular.parteRelacionada` en cuanto contabilidad marque las excepciones |
| `tipoEmision` (ventas) | **`F`**, como el autorizado (hoy el código fija `"E"`) | Sí |
| `razonSocial` | `Facturador.nombreComercial` si no viene vacío; si viene vacío, `razonSocial` **truncada a 100 caracteres** y un aviso | Sí |
| `<rendFinancieros>` | **NO entra hoy.** El dato (retenciones que los bancos nos practican sobre inversiones) no está en el modelo | Frente aparte |

**El default `NO` de `parteRel` es una afirmación tributaria, no un default técnico.** Se aplica
porque el archivo autorizado de julio declara `NO` en los 79 proveedores y los 19 clientes, y porque
sin él no hay declaración hoy. Queda escrito acá para que contabilidad lo pueda desmentir.
