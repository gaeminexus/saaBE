# Tipos de asiento automáticos IN-A / EG-A / DI-A y numeración por módulo (P5, desde el 01-11-2026)

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-27 por `omen-saa-2-arb`, **antes** de despachar (regla 7).
**Reemplaza** la regla del P5 de `PEDIDOS-CONTABILIDAD-2026-10-12.md` (que decía «sin banco → tipo del módulo, nunca
Diario»). **Fecha de vigencia: asientos con fecha desde el 2026-11-01.**

## 1. Decisiones del usuario (2026-10-27), textuales

> *«En los bancos debe existir un check que indique si es banco real o no. Para todos los asientos contables que
> tengan la cuenta contable de una cuenta bancaria de un banco real, en caso de ser ingreso de dinero, entonces se debe
> generar un asiento tipo INGRESO, si es salida de dinero, se debe generar un EGRESO, y si en el asiento no hay una
> cuenta contable de un banco real entonces debe salir tipo diario. Esta numeración debe ser mensual igual que antes por
> tipo asiento sistema. Pero ahora se desea que el módulo sea otro dato en los asientos automáticos. Para conseguir esto
> vamos a crear los tipos de asiento IN-A (INGRESO AUTOMATICO) con código alterno 7, EG-A (EGRESO AUTOMATICO) con código
> alterno 8, DI-A (DIARIO AUTOMATICO) con código alterno 9. Pero además se debe crear un nuevo campo que indique el módulo
> origen del asiento y que también tenga una numeración mensual por módulo, para tener doble control.»*

- Caja chica (no toca bancos): **DI-A**.
- Reversos de asientos anteriores al 01-11: **conservan el tipo del original**.
- **Tipos ya creados por el usuario en Tipos de Asientos del Sistema** (captura del 27-10): `T-INGRESOS` 4, `T-EGRESOS` 5,
  `RECURSOS HUMANOS` 6, `CXC` 2, `CREDITOS` 1, `CXP` 3, **`IN-A (INGRESO AUTOMATICO)` 7, `EG-A (EGRESO AUTOMATICO)` 8,
  `DI-A (DIARIO AUTOMATICO)` 9**, todos activos.

## 2. La regla

Para **todo asiento automático** (el que arma el sistema, no el manual de contabilidad) con **fecha ≥ 2026-11-01**:

1. **Banco real:** una línea es «de banco real» si su cuenta contable es la `planCuenta` de una **cuenta bancaria propia**
   (`TSR.CNBC`) cuyo **banco** (`TSR.BNCO`) tiene el check nuevo **«banco real»** encendido.
2. **Tipo:**
   - hay líneas de banco real y el **neto** de esas líneas (debe − haber) es **> 0** → **IN-A (7)**;
   - neto **< 0** → **EG-A (8)**;
   - neto **= 0** con líneas de banco a los dos lados (transferencia entre cuentas propias) → **EG-A (8)** *(default del
     árbitro; decidible)*;
   - **sin** líneas de banco real → **DI-A (9)**.
3. **Numeración por tipo:** mensual, como hoy (`asignaNumeroAlterno`). ⚠️ Hoy el prefijo son **las 3 primeras letras del
   nombre**: con `IN-A (INGRESO AUTOMATICO)` saldría `IN--2026-11-0001`. Regla nueva: si el nombre tiene `" ("`, el prefijo
   es lo que está antes (`IN-A`, `EG-A`, `DI-A`) → `IN-A-2026-11-0001`. Los nombres viejos no cambian de prefijo.
4. **Módulo origen (campo nuevo) y su numeración mensual (doble control):** cada asiento automático guarda el módulo que
   lo generó y un consecutivo mensual **por módulo y empresa**, con su número alterno (`CXP-2026-11-0001`). Módulos:
   `CRE` créditos · `CXC` · `CXP` · `TSR` tesorería · `RRH` RR.HH. · `CNT` contabilidad. De dónde sale el módulo de cada
   asiento lo decide el inventario del ítem 0 (§4): hoy los llamadores pasan un `codigoAlterno` (1–6) que es casi el
   módulo, pero con cruces (p. ej. el anticipo de proveedor de CxP pasa 5 = T-EGRESOS).
5. **Reversos** de asientos con fecha < 2026-11-01 → tipo del original (como hoy). Reversos de asientos nuevos → mismo tipo
   que el original (IN-A/EG-A/DI-A) y mismo módulo.
6. **Asientos manuales** de contabilidad: no cambian.
7. **Antes del 2026-11-01:** todo sigue como hoy.

## 3. Datos (script `e2-111`, a escribir con el inventario en la mano)

- `TSR.BNCO`: columna nueva «banco real» (NUMBER(1), 0/1). El default y qué bancos van en 0 se deciden con el listado de
  bancos y sus cuentas (el script lo muestra primero).
- `CNT.ASNT`: columnas nuevas para módulo origen, consecutivo mensual por módulo y número alterno por módulo.
- Los tipos 7/8/9 ya existen (los creó el usuario); el script solo los **verifica** por empresa.

## 4. Lo que falta medir (ítem 0 del BE, antes de diseñar el resto)

- Todos los llamadores de `AsientoContableServiceImpl.generarAsiento` (6 y 7 argumentos), con el `codigoAlterno` y el
  `moduloSistema` que pasan, y su módulo real.
- Todo lugar que crea un asiento **sin** pasar por `generarAsiento` (legacy de tsr `insertarCabeceraAsiento`, cierres,
  reversos, procesos de `crd`, copia de asiento, etc.).
- Cómo se crean los asientos manuales (para excluirlos).
- La relación `CNBC` → `BNCO` → `planCuenta`, y qué filas de `BNCO` existen.
- Dónde se calcula `numeroMesTipo` y si el consecutivo por módulo puede seguir el mismo patrón (MAX+1) sin colisiones.

## 5. Impacto en otros equipos

El cambio es **central** (`generarAsiento`): cambia el tipo de **todos** los asientos automáticos, también los de `crd`
(`omen-saa-1`, que hoy usa `CREDITOS`) y los que toca `lap-saa-1`. Hay que avisarles con autorización del usuario antes de
subirlo. `omen-saa-1` además está cambiando validaciones de usuario en `cnt`.

## 6. Lo que midió el inventario (omen-saa-2-be, 2026-10-27) y lo que define

- **H1 — el tipo se busca solo por empresa + alterno** (`TipoAsientoDaoService.selectByAlterno`; `PLNTSSTM` no lo lee
  nadie). El alterno **7 ya lo usaba `TipoAsientos.TRANSFERENCIAS`** (`generaCabeceraTransferencia`, legacy de tsr). Si
  en una empresa hay otra fila con alterno 7, IN-A choca con ella. → El `e2-111` BLOQUE 0a lo mide. **La búsqueda de
  7/8/9 se hace con un método nuevo que exige `PLNTSSTM = 1`**; el método viejo no se toca.
- **H2 — los `codigoAlterno` que pasan los llamadores no sirven para saber el módulo** (3 = todas las compras y la
  retención V2 emitida; 4 y 5 mezclan tesorería, cxc y cxp). **H3 — el módulo que se graba hoy es CXC por defecto**
  (todo crd, todas las compras). Por eso el módulo origen **no** se deduce de lo que se graba hoy.
- **Módulo origen, regla definitiva:**
  - los métodos de negocio de `AsientoContableServiceImpl` llaman a una sobrecarga nueva de `generarAsiento` **con el
    módulo explícito** (tabla de abajo);
  - los llamadores externos que usan `generarAsiento` directo (6/7 args) se resuelven así, sin tocar su código:
    alterno **1 → CRE** (todo crd), alterno **6 → RRH** (rhh), y si no, por el `moduloSistema` que pasan
    (TESORERIA → TSR, CXP → CXP, RRHH → RRH, CXC → CXC). Así **no se toca código de `crd`**.

  | Método | Módulo |
  |---|---|
  | factura, NC, ND, retención v1/v2 emitidas, anticipo de cliente, aplicación de anticipo de cliente, cobro por transferencia, retención recibida (v1/v2) | CXC |
  | factura/NC/ND/liquidación de compra (incl. intermediario y reembolso), anticipo de proveedor, devolución de saldo a favor, aplicación de anticipo de proveedor, pago por transferencia a proveedor | CXP |
  | egreso e ingreso de tesorería, egreso contra anticipo, gasto/aplicación/reposición/ajuste de caja chica | TSR |
  | anticipo de empleado | RRH |
  | liquidación de compra emitida por cxc | CXC |

- **Asientos que no pasan por `generarAsiento`:** el reverso conserva tipo **y módulo** del original y toma **su propio**
  consecutivo de módulo; la copia (`generaCopia`) y los manuales (`POST/PUT /asnt`) quedan **sin módulo**; el cierre de
  período no cambia. El legacy de tsr (`insertarCabeceraAsiento`: cheques, cobros, depósitos, débitos/créditos, pagos,
  transferencias) crea la cabecera **antes** que las líneas: en esta pasada **no** se le aplica la regla; se mide si se usa
  todavía (asientos de octubre de esos tipos) y se decide aparte.
- **No hay nada que distinga un asiento manual de uno automático** en la entidad: la regla se aplica solo en el camino de
  `generarAsiento`, que es por definición automático.
- **Consecutivos sin protección:** `numeroMesTipo` es MAX+1 sin bloqueo ni UK; el de módulo copia el mismo patrón (mismo
  riesgo de carrera, ya existente). No se agrega UK en esta pasada.
