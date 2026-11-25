# Entrega del proyecto de cuadre al árbitro (24-nov-2026)

Respuesta a lo que pidió el árbitro omen1 en `PLAN-CONTABILIDAD-PRODUCCION-2026-11-24.md`. Son copias del expediente
del cuadre (`cuadreSuper`, privado), sin datos de partícipes: las operaciones van solo por número (`PRSTIDAS`).

## 1. Ramas

| Rama | Estado |
|---|---|
| `origin/cuadre-c19-balances-oficiales` (`6e85db88`, C19b) | **Subida.** El remoto no admite `cuadre/…` porque existe la rama `cuadre`, así que se publican con guion. Base `a4d6842c`, que ya está en main. Ojo con el choque con C38 (`CNT.CRRP`, `/esrp`), anotado en el §1.2 del plan |
| `origin/cuadre-c45-bloqueantes-produccion` (5 commits sobre `90fa777f`) | **Subida.** Cada commit compila (`mvn -q -o package -DskipTests`). No se desplegó ni se probó por REST. Los commits:<br>• 2.1 cobro duplicado (`109b3707`): bloqueo `FOR UPDATE NOWAIT`, evento suelto y clave compuesta para «9»/«09».<br>• 3.2 precancelación en efectivo (`cd067268`).<br>• 2.4 D-151 (`1ddcc63e`, semilla C45-02).<br>• 6.1 D-053 en el cierre de cartera y la nómina (`083e7994`, semilla C45-03, obligatoria antes del EAR).<br>• 6.2 fechas de reversos y contra-movimientos (`2f616981`).<br>La nómina (`rhh`) y CxP tocan otro equipo. El crédito por última cuota no se cambió: son repartos que suman exacto. En `reversarAporte` cambia el criterio de C17: si el mes del aporte sigue abierto, el reverso va con la fecha del aporte (lo valida el líder). Quedan abiertos:<br>• el contra-APRT de `DevolucionAporteServiceImpl`;<br>• las reversiones de CxP sin fecha;<br>• el bloqueo en `anularCobro` y en `reversarProceso`;<br>• el redondeo ⑧ contra la G48. |

## 2. Archivos

| Archivo | Qué es |
|---|---|
| `C27b-INTERES-ANTICIPADO.md` | Diagnóstico del cuadre: conciliación de 1.4.02 al centavo por mes y cuenta, causas, residuo y regularización |
| `plantilla-conciliacion-1402.sql` | Consulta de referencia de la prueba de aceptación 1. Se reemplaza `__CORTE__` por `DATE 'AAAA-MM-DD'` (fin de mes); el ancla queda fija en 2025-05-31. El bloque 04 da el anticipado por cuota |
| `AUDITORIA-CONTABILIDAD-PRODUCCION.md` | Auditoría de los motores contables en `origin/main` `222ead04`, flujo por flujo. Base del encargo |
| `cuentas-saldo-contrario.sql` | Control del punto 6.3: cuentas de movimiento con saldo contrario a su naturaleza al corte (activo acreedor salvo .99; pasivo o patrimonio deudor). Se espera 0 filas |
| `02-semilla-b17-4.8.99.08.sql` | Script U6 (ver §3) |

## 3. U1–U7 del pase a producción

Corren en producción **antes del EAR unificado**, cada uno con sus controles antes y después, en el cliente JDBC
(`cuadreSuper/01-seguimiento/PASE-A-PRODUCCION.md`, paso 0).

| # | Script | Dónde está | Por qué |
|---|---|---|---|
| U1 | `cnt/sql/D043-01-asnh-observacion-2000.sql` | saaBE `docs/logica-negocio/cnt/sql/` | Desmayorización con glosas largas (ORA-12899). Si `ASNHOBSR` ya mide 2000, se salta |
| U2 | `cnt/sql/C25-01-seguimiento-novedades.sql` | saaBE | Tablas de la pantalla de novedades |
| U3 | `cnt/sql/C25-02-revision-con-el-cliente.sql` | saaBE | Columnas y `CNT.RVNM` de C25b |
| U4 | `rpr/sql/C27-01-ddl-ccpb.sql` | saaBE | **Obligatorio:** la generación del CCPM escribe en `RPR.CCPB` |
| U5 | `crd/sql/378_LINEAS_APORTES_PATRONALES_PLANTILLA_21.sql` | saaBE | Cruce de aportes patronales 13/14 (C29) |
| U6 | `02-semilla-b17-4.8.99.08.sql` | **esta carpeta** | Fila `CNT.CRRP` de 4.8.99.08 al renglón 489090 del B17 (D-053) |
| U7 | Dato `CRD.IGFN.IGFNCMNM` = `SI` o `NO` (la base de trabajo usa `NO`) | — | Sin él la G40 no se genera |

Además, del §4 del pase:
- **4.18:** cuenta 4.8.90.90.45 y línea 70 de la plantilla 25 (D-176).
- **4.22:** cuenta 4.8.99.9 GND MULTAS, con su fila CRRP 489090 (D-180).
- **4.20:** el traslado de las condonaciones de 2026 y la anulación del asiento 8527 (D-181).

Los scripts están en `cuadreSuper`. Si los quieres en saaBE, se agregan a esta carpeta.

## 4. C27b: ancla y base de transición

**`saa.cartera.anclaArranque = 2025-05-31`.** Es la corrida de arranque de cartera `CRD.CRCT` 26 (año 2025, mes 5,
`CRCTFCCR` 2025-05-31, `CRCTIDST` 2, «Arranque de cartera al 31-may-2025, re-proceso 2 D-070»). La base re-procesada
reemplaza a producción y trae esa misma corrida.

**`saa.cartera.devengoCompletoDesde` y `saa.cartera.anticipadoBaseTransicion`: los decide el líder (D3).** La propuesta
del cuadre:
- El re-proceso sigue con el asiento manual C27b mensual hasta noviembre 2026 (D-172), con el EAR del re-proceso y sin
  la propiedad.
- En producción, `devengoCompletoDesde` = el primer corte que se cierre con el código nuevo después de que la base
  re-procesada reemplace a producción. Tentativamente 2026-12-31.
- La base de transición es el objetivo del ⑪ al corte anterior (30-nov-2026), por `TPPRCDGO`. Es igual a lo que el
  asiento manual C27b de noviembre 2026 deje en 2.9.01.05. **El cuadre entrega el valor al cerrar noviembre 2026**; antes
  no existe.
- Cálculo: bloque 04 de `plantilla-conciliacion-1402.sql` (anticipado de cuotas por vencer al corte) agrupado por la
  cuenta de la plantilla 17. Equivalencia cuenta → tipo: 1.4.02.05 quirografario (`TPPRCDGO` 1), 1.4.02.15 hipotecario
  (2) y 1.4.02.10 prendario (3, 4, 5). La propiedad lleva las tres cifras repartidas por tipo, por ejemplo
  `1:<Q>;2:<H>;3:<P>;4:0;5:0` si todo el prendario cae en el tipo 3; se confirma al calcularla.

Si el líder decide activar el código nuevo ya en el re-proceso (desde octubre 2025), se avisa:
- La base sería **0 explícito** por tipo, porque septiembre 2025 se cerró sin asiento C27b.
- Haría falta un único asiento manual en octubre con lo de junio a septiembre que nunca pasó a ingreso.
- Antes tendría que estar el lote 1b, que es condición de activación.
