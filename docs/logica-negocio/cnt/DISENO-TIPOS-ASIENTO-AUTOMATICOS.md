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
