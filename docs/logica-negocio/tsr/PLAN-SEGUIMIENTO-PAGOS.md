# PLAN — Seguimiento de un pago (una sola pantalla) y número de pago buscable en tesorería

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-15 · **Estado:** diseño y contrato en disco, despachado por fases
**Contrato:** `tsr/API-SEGUIMIENTO-PAGOS.md` (espejado a `saaFE/docs/tsr/`)
**Origen:** pedidos 1 y 3 del usuario, derivados por `omen-saa-1-arb` y confirmados por el usuario (urgentes).

---

## 0. Los pedidos

> **1.** *«Para encontrar un pago tienen que entrar a todas las pantallas. Una pantalla muy amigable y moderna que diga
> exactamente en qué estado está un pago: el origen, en qué lugar del proceso está, ir a la pantalla del proceso donde
> está parado, o anular desde ahí según el proceso en el que se quedó.»*
>
> **3.** *«En todas las pantallas de tesorería el número de pago se vea claro y existan buscadores para encontrarlo.»*
> (Motivo: el botón «Reemitir pago» de crd responde «tesorería debe reversar primero la orden N° X».)

## 1. Lo medido (2026-09-15, solo lectura)

- **Todo pago termina en `PGS.PGTR`**, con 6 estados (`EstadoPagoProgramado`: 0 POR_APROBAR, 1 REGISTRADO, 2 EN_ARCHIVO,
  3 CONFIRMADO, 4 RECHAZADO, 5 ANULADO) y **14 orígenes**: 4 con FK (factura, liquidación, egreso, anticipo a proveedor) y
  10 externos por `PGTRORGN` + `PGTRIDOR` (`OrigenPagoExterno`: devolución de aportes, pensión y seguro de jubilados,
  desembolso de préstamo, devolución a cliente, caja chica, anticipo a empleado, nómina, beneficio social, planilla IESS).
- **No existe ningún estado «ratificado»** en el circuito de pagos; la «aprobación» es `POR_APROBAR` o la del módulo de origen.
- **Transiciones y pantallas** (FE, prefijo `menutesoreria` salvo solicitud):

  | Etapa (estado PGTR) | Qué la mueve | Pantalla |
  |---|---|---|
  | 0 POR_APROBAR | `POST /pgtr/aprobar` | `pagos/aprobacion` |
  | 1 REGISTRADO | `POST /pgtr/lote` (transferencia) · `confirmarManual` | `pagos/archivo-banco` · `pagos/confirmacion` |
  | 2 EN_ARCHIVO | `POST /pgtr/lote/{id}/respuesta` · `confirmarManual` | `pagos/confirmacion` |
  | 3 CONFIRMADO | `POST /pgtr/revertirConfirmado/{id}` | `pagos/consulta` |
  | Anular (0,1,2; no cheque) | `POST /pgtr/anular/{id}` | `pagos/aprobacion` (0) · `pagos/consulta` (1,2) |

- **Buscar hoy:** `pagos/consulta` no muestra el número de pago, filtra por proveedor/tipo/fechas/concepto **en el cliente**,
  su combo no tiene POR_APROBAR, y el concepto **no** busca en la observación. El backend **ya** busca texto en observación,
  beneficiario y titular (`PagoProgramadoDaoServiceImpl:141-145`, `?texto=`) y nadie lo usa desde consulta.
- **`GET /pgtr/getId/{id}`** trae el pago con lote, asiento y documento con FK embebidos; para un origen externo sólo trae
  `origenExterno` + `idOrigen`. No existe nada que resuelva un origen externo a su documento.

## 2. 🔴 Tres defectos encontrados al mapear — plata que puede salir dos veces o no salir

| # | Defecto | Evidencia (verificada por el árbitro) | Ítem |
|---|---|---|---|
| **S1** | **Anular una orden de beneficio social ENVIADA a tesorería deja su pago vivo y pagable** | ✔ `OrdenBeneficioSocialServiceImpl.anular:440-475` sólo bloquea PAGADA; pasa a ANULADA sin tocar el PGTR | BE-19 |
| **S2** | **Anular un anticipo a empleado APROBADO deja su pago vivo** (sólo bloquea si está CONFIRMADO) | ✔ `AnticipoEmpleadoServiceImpl:282-300` | BE-19 |
| **S3** | **Registrar un anticipo a proveedor falla siempre**: el backend exige `idCuentaBancaria`; la pantalla manda `idCuentaDestinoTitular` y no la cuenta | ✔ `AnticipoProveedorServiceImpl:212`, ✔ `anticipos-proveedores.component.ts:305-314` | BE-20 |

Mismo mecanismo que el que `omen-saa-1` corrigió en devoluciones de aportes: **el documento de origen se anula y su orden de
pago sigue en la bandeja de tesorería**. La regla que se aplica en S1 y S2: anular el origen anula el PGTR si está en 0 o 1;
si está en 2 (en un archivo del banco) o 3 (pagado), **rechaza** con el número de pago.

## 3. Diseño

### 3.1 Backend — un endpoint de seguimiento, en `cxp` (donde vive `PagoProgramado`)

`GET /rest/pgtr/seguimiento/{idPago}` y `GET /rest/pgtr/seguimiento?numero=|texto=|origen=&idOrigen=` (contrato en el API).

- **Resolución de origen por registro**, no por `if` encadenado: una interfaz `ResolutorOrigenPago` con una implementación por
  origen, elegida por la clave del origen. **Un origen sin resolutor devuelve el pago igual**, con `origen.documento = null` y
  `origen.resuelto = false` — nunca un 500. Es el contraejemplo del §24 del estado: el origen N+1 no revienta ni pasa en
  silencio, se muestra como «origen sin detalle».
- **Los resolutores de `crd` leen, no escriben**: consultan las entidades de `crd` por `EntityManager` desde `cxp`, sin tocar
  archivos de `crd` (módulo de otro equipo).
- **Acciones** calculadas en el backend con las mismas reglas que ya aplican `anularPago` y `revertirPagoConfirmado`: la
  pantalla **no** duplica la regla de qué se puede anular (§34ter del estado).
- `selectById` inexistente → **404**, no 500.

### 3.2 Frontend — pantalla nueva `menutesoreria/pagos/seguimiento`

- Buscador único arriba: número de pago, o texto (beneficiario, observación, número de documento).
- Resultado: tarjeta con **N° de pago grande**, estado con color, origen con su documento y estado, línea de tiempo de etapas
  (hecha / actual / pendiente), lote y asiento.
- Botones que salen del backend: **Ir a la pantalla de la etapa**, **Anular** (llama a `/pgtr/anular/{id}` con motivo),
  **Revertir** (llama a `/pgtr/revertirConfirmado/{id}`). Botón deshabilitado → tooltip con el motivo que manda el backend.
- Entrada en el menú de tesorería.

### 3.3 Pedido 3 — número visible y buscable en las pantallas que ya existen

`pagos/consulta`, `pagos/aprobacion`, `pagos/archivo-banco`, `pagos/confirmacion`: columna **N° pago** primera y filtro por
número. En `consulta`: `POR_APROBAR` en el combo, el filtro de texto va al backend (`?texto=`), etiquetas de los 4 orígenes
que faltan, y el asiento del pago (`pago.asiento`) para los orígenes externos. Cada fila con enlace a Seguimiento.

## 4. Fases y despacho

| Fase | Ítems | Depende de |
|---|---|---|
| **A** (ya) | **FE-7** pedido 3 · **BE-19** S1+S2 · **BE-20** S3 | nada |
| **B** | **BE-21** endpoint de seguimiento | contrato |
| **C** | **FE-8** pantalla de seguimiento | BE-21 commiteado (el FE puede arrancar contra el contrato) |

Sin DDL en ninguna fase.
