# Listado de cambios al SAA (repositorio `saaBE`) — BACKEND

Versión 1 — 1-oct-2026. Para el equipo de agentes de desarrollo. Cada cambio sale de la evidencia del
diagnóstico (`03-trabajo/fase1-balance-arranque/`) y de la proyección de junio
(`03-trabajo/meses/2025-06/PROYECCION-JUNIO.md`).

## Reglas para todos los cambios

- Se rigen por el `CLAUDE.md` del repositorio `saaBE` (capas, nomenclatura, `mvn -q compile` donde haya Maven, reportes con su `.jasper`).
- **Todo se prueba contra la base del proyecto** (`saa-oracle-cuadre`, puerto **1522**), nunca contra producción. Antes de probar un cambio que modifica datos, restaurar el punto cero si hace falta (`cuadreSuper/entorno/README.md`).
- Los procesos de re-ejecución deben ser **idempotentes y reversibles**: correr dos veces da el mismo resultado; todo lo que se anula queda marcado (no se borra) y es trazable.
- **Las reglas de negocio son las del SAA vigente** (desde agosto 2026) aplicadas a todos los meses: devengo de aportes por prelación desde jun-2025 (`distribuirAportePorDevengo`), precancelación con el capital completo sobre una sola cuota. Ver `PLAN-DE-TRABAJO.md`, reglas de método.
- Cada cambio entrega su criterio de aceptación **verificado con una consulta** sobre la base del proyecto.

## Resumen y orden

| # | Cambio | Para | Prioridad | Depende de |
|---|---|---|---|---|
| C1 | Períodos contables de 2025 | Junio | 1 — inmediato | — |
| C2 | Saldo de apertura contable al 31-may-2025 | Junio | 1 | C1 |
| C3 | Volver al ancla de mayo (cartera, cuentas individuales, jubilados) | Junio | 1 | — |
| C4 | Re-procesar una carga Petro existente | Junio | 1 | C1, C3 |
| C5 | Cargador de asientos manuales | Junio | 2 | C1 |
| C6 | Fecha contable en el resto de motores (nómina, CxC, CxP, tesorería, crédito manual) | Junio en adelante | 2 | C1 |
| C7 | Generador del B17 | Cierre de junio | 2 | C2 |
| C8 | Estructuras G a fecha de corte | Cierre de junio | 2 | C3, C4 |
| C9 | Reporte de cuadratura B17 ↔ G | Cierre de junio | 3 | C7, C8 |
| C10 | Flujo de efectivo y demás reportes a la Super | Cierre de junio | 3 | lista del cliente (F1) |

---

## C1 — Períodos contables de 2025

**Por qué:** en `CNT.PRDO` solo existen los períodos de 2026. Los motores generan asientos con fecha del mes de la carga (`CobroPetroContableServiceImpl`, `fechaService.ultimoDiaMesAnioLocal(...)`), pero un asiento de junio 2025 no tiene período donde caer.

**Qué hacer:** crear los períodos de **junio a diciembre 2025** de la empresa 1236 con el mismo mecanismo con que se crean los de 2026 (`PeriodoServiceImpl`), en estado abierto. Revisar qué valida `AsientoContableService.generarAsiento` sobre el período (abierto, mayorizado) y documentarlo.

**Aceptación:** `SELECT PRDOANNN, PRDOMSSS, PRDOESTD FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2025` devuelve 7 períodos abiertos; un asiento de prueba con fecha 30-jun-2025 se registra en el período de junio 2025.

## C2 — Saldo de apertura contable al 31-may-2025

**Por qué:** el SAA no tiene contabilidad anterior al 28-feb-2026. Junio 2025 tiene que arrancar con el B17 de mayo.

**Qué hacer:** un proceso (o script revisable, ver `CLAUDE.md`: correcciones de datos como documento con SELECTs de control) que registre **un asiento de apertura al 31-may-2025** con las 414 cuentas hoja del B17 de mayo llevadas a las cuentas de movimiento del SAA, usando el mapeo `03-trabajo/fase1-balance-arranque/MAPEO-PLAN-CUENTAS.csv`. Donde una cuenta del B17 tiene varias subcuentas de movimiento en el SAA (bancos por cuenta bancaria, cartera por bandas de días), el reparto se toma de los mayores de mayo (`02-informacion-cliente/01-BalanceArranque-Mayo2025/Mayores-Contables/`); si un reparto no se puede determinar, el saldo va a la subcuenta que indique el responsable y queda registrado.

**Aceptación:** el balance de comprobación del SAA al 31-may-2025, agregado por cuenta del B17, es igual al B17 de mayo en las 534 cuentas (suma de control 900.189.987,57; ecuación contable en 0,00).

## C3 — Volver al ancla de mayo 2025

**Por qué:** el estado operativo del SAA al 31-may no reproduce la G48 ni la G42 (cuotas migradas sin fecha de pago, pagos con valores errados, saldos migrados distintos). El ancla correcta ya está en el SAA: `RPR.HM48` (G48), `RPR.HM42` (G42), `RPR.HM44` (G44), verificadas al centavo.

**Qué hacer**, en la base del proyecto:
1. **Cartera:** para cada operación de `RPR.HM48`, dejar sus cuotas en el estado que da el saldo de la G48 al 31-may (capital por vencer + vencido = `HM48VPVN + HM48VLVN`): cuotas con vencimiento hasta mayo pagadas o vencidas según la G48, el resto pendientes. Las operaciones con saldo en el SAA que **no** están en la G48 (2.940, USD 18,5 M) quedan canceladas antes de junio 2025. Anular (marcar, no borrar) los pagos de `CRD.PGPR` con fecha o carga posterior al 31-may.
2. **Cuentas individuales:** registrar por partícipe y tipo un **ajuste de apertura al 31-may-2025** que lleve el saldo del SAA al de `RPR.HM42`, y anular los ajustes "POR DEPURACIÓN DE MIGRACIÓN" fechados en junio 2025 (la proyección muestra que no son actividad de junio). Anular los aportes con caja desde junio 2025 que provengan de cargas o migraciones a re-procesar.
3. **Jubilados:** los datos de `RPR.HM44` son la referencia; registrar en `CRD.VPPC` la pensión de los 19 jubilados que no la tienen (dato del cliente, pendiente N10).

**Aceptación:** al 31-may-2025, la cartera del SAA por operación = `RPR.HM48` (1.594 operaciones, 21.947.991,43) y el saldo de cuenta individual por partícipe = `RPR.HM42` (4.585 partícipes, 52.844.299,10), en el 100 % de los casos.

## C4 — Re-procesar una carga Petro existente

**Por qué:** las 16 cargas Petro de jun-2025 a sep-2026 ya están en el SAA con su detalle por partícipe (`CRD.CRAR`/`DTCA`/`PXCA`). Las de jun-2025 a jul-2026 se aplicaron con una versión anterior del proceso: pagos fechados con el día del proceso, sin enlace a la carga y sin asiento. El proceso **actual** ya fecha pagos y asientos al último día del mes de la carga (`ProcesoCargaPetroServiceImpl` líneas 469/519; `CobroPetroContableServiceImpl` líneas 429/639/1079), así que **no hay que reescribir el motor: hay que poder volver a correrlo**.

**Qué hacer:** una operación "re-procesar carga" que, para una carga dada: (1) anula la aplicación anterior (pagos, aportes y asientos que esa carga generó, identificados por `CRARCDGO`, por `APRTIDAS` y, para los pagos viejos sin enlace, por la observación "Carga NNN"); (2) vuelve a correr las fases de aplicación y contabilización con la lógica vigente. Se usa en orden cronológico, después de C3.

**Aceptación (carga 352, junio 2025):** después de re-procesar, la suma aplicada en pagos de préstamos + aportes + seguro = total descontado del retorno (535.477,39) menos lo de los 14 roles Petro sin partícipe en el SAA (4.872,04), que quedan reportados como novedad y no se aplican; todos los pagos con fecha 30-jun-2025 y `CRARCDGO = 352`; el aporte de junio por partícipe coincide con el producto AH del retorno (161.257,56 en los 2.061 partícipes identificados); asientos generados en el período de junio 2025.

## C5 — Cargador de asientos manuales

**Por qué:** activos fijos, inversiones no privativas y otros grupos no tienen módulo en el SAA; el cliente los entrega como asientos.

**Qué hacer:** carga de un archivo con el formato de `03-trabajo/plantillas/PLANTILLA-ASIENTOS-MANUALES.csv` que registre asientos manuales (tipo DIARIO) con validaciones: cada asiento cuadra, las cuentas existen y son de movimiento (se acepta el código del catálogo y se traduce a la cuenta del SAA), el período está abierto. Rechazo total del archivo si una validación falla, con el detalle de los errores. Origen trazable (nombre del archivo y número de asiento del cliente en la observación).

**Aceptación:** el archivo de ejemplo de la plantilla se carga y genera 2 asientos cuadrados en junio 2025; un archivo con un asiento descuadrado se rechaza completo.

## C6 — Fecha contable en el resto de motores

**Por qué:** los motores usan la fecha del sistema en ~90 lugares (crédito, nómina, Petro, devoluciones, pensiones). El de Petro ya está resuelto (ver C4); los demás no.

**Qué hacer:** auditar cada motor que genera asientos o movimientos y permitir una **fecha contable** explícita para el re-proceso, sin cambiar el comportamiento normal (por defecto, la de hoy). Prioridad: pago de pensión complementaria, pagos manuales de préstamos (`MotorPagoPrestamoServiceImpl` usa `LocalDateTime.now()` en líneas 229 y 765), abonos y precancelaciones, devolución de aportes, desembolso de créditos, nómina (`ContabilizacionNominaServiceImpl`), CxP, CxC y tesorería. Entregar la tabla motor → dónde se fija la fecha → cambio hecho.

**Aceptación:** por cada motor, un movimiento de prueba con fecha contable 30-jun-2025 genera sus filas y su asiento en junio 2025.

## C7 — Generador del B17

**Por qué:** no existe. Hay que poder regenerar el B17 de cualquier mes desde el SAA.

**Qué hacer:** reporte que genere el archivo B17 con el formato de lo presentado en mayo (`B17M396831052025.txt`: cabecera `B17 · 3968 · fecha de corte · número de líneas incluida la cabecera · suma de control`, una línea por cuenta del catálogo con su saldo), a partir del balance de comprobación del mes, agregando las cuentas del SAA según `MAPEO-PLAN-CUENTAS.csv` y las asignaciones validadas de `ASIGNACIONES-PENDIENTES.csv`. Replicar la convención de la cuenta 7 (se reporta en 0).

**Aceptación:** el B17 generado para mayo 2025 (con el saldo de apertura de C2) es idéntico byte a byte al presentado, salvo la fecha de generación si la hubiera.

## C8 — Estructuras G a fecha de corte

**Por qué:** los generadores reproducen solo el mes en curso. Hallazgos del diagnóstico:
- **G41** depende de una bandera en `CRD.ENTD` que se consume al generar → usar fechas (ingreso, cambio de estado).
- **G44** toma los jubilados de hoy; excluye a los **jubilados pasivos** (estado 7), que la G44 de mayo sí reportó; cuenta imposiciones con aportes, y los jubilados no tienen aportes mensuales migrados → imposiciones = `RPR.HM44.HM44IAJB` + meses aportados desde junio 2025.
- **G48** usa el estado actual de la cuota → calcular el estado a la fecha de corte desde los pagos (`CRD.PGPR`, ya re-procesados por C4); **el orden de columnas del archivo debe ser el oficial** (Excel de la G48 de mayo: … valor vencido, costos operativos, interés ordinario, interés sobre mora, … provisión requerida, provisión constituida, valor total cuenta individual, valor sujeto a provisión …), que no coincide con el documentado en el generador.
- **G42** filtra por fecha de caja; definir con el líder del proyecto si el corte es por caja o por devengo.
- El SAA no guarda historial de estados del partícipe → hace falta registrarlo (fecha de cambio) para G41/G43/G44 de meses pasados (dato del cliente, N11).

**Aceptación:** generadas para mayo 2025 sobre la base con el ancla (C3), las G42, G44 y G48 coinciden con `RPR.HM42`, `HM44` y `HM48`.

## C9 — Reporte de cuadratura B17 ↔ G

**Qué hacer:** reporte por mes que compare: cuenta 21 vs total G42; cuenta 13 + 1399 vs cartera bruta G48; 1399 vs provisión G48; altas G46 y cancelaciones G49 vs movimiento de cartera del mes; G41/G43 vs variación del G42. Resultado por línea: cuadra / diferencia.

**Aceptación:** para mayo 2025 todas las líneas cuadran.

## C10 — Flujo de efectivo y demás reportes a la Super

Pendiente de la lista oficial de reportes mensuales que debe entregar el cliente (F1). Hay un formato de flujo de caja en `02-informacion-cliente/07-Formatos/`.
