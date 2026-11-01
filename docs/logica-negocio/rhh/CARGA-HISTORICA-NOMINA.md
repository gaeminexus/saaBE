# Carga histórica de nómina (C13)

Cuadre Superintendencia ASOPREP, decisión D-012: la nómina de **jun-2025 → sep-2026** se reconstruye en el módulo de
nómina y en la contabilidad **desde los roles del contador**, sin recalcular, con los motores contables del SAA. Mismo
patrón que la carga de CxC/CxP (`docs/logica-negocio/cxp/CARGA-HISTORICA-CXC-CXP.md`): un lote JSON por mes generado
fuera del SAA, un endpoint que valida y otro que carga, un renglón de resultado por paso.

- Generador de lotes y análisis: repositorio `cuadreSuper`, `03-trabajo/nomina/` (`scripts/02-genera-lotes.js`,
  `ANALISIS-NOMINA.md`, `lotes/VALIDACION.md`). Los `lote.json` traen cédulas y **no se versionan**.
- Código: `com.saa.ejb.historico` — `CargaHistoricaNominaService` (orquestador, sin transacción) y
  `CargaHistoricaNominaPasoService` (un paso por transacción, REQUIRES_NEW); modelo `com.saa.model.historico.LoteHistoricoNomina`;
  REST en `CargaHistoricaCxcCxpRest` (`@Path("historico")`).

## 1. Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/SaaBE/rest/historico/validarHistoricoNomina` | Fuerza `soloValidar = true`: valida el lote y cada paso, no graba nada |
| POST | `/SaaBE/rest/historico/cargarHistoricoNomina` | Carga el lote |

Respuesta: `ResultadoCargaHistorica` (el mismo de C11). HTTP 200 = lote procesado (puede haber pasos RECHAZADOS en
`items`); 400 = `erroresLote` no vacío (nada se intentó); 500 = error inesperado.

```json
{ "mes": "2025-06", "exito": true, "soloValidar": false, "erroresLote": [], "avisosLote": [],
  "creados": 37, "omitidos": 6, "rechazados": 0, "validados": 0,
  "items": [ { "tipo": "empleado|periodo|asiento-rol|asiento-provisiones|pago|planilla-iess|cierre",
               "ref": "E01 | 2025-06 | PAGO-2025-06-1 | IESS-2025-06-APORTES", "documento": null,
               "estado": "CREADO|OMITIDO|RECHAZADO|VALIDADO", "mensaje": "...", "id": 123,
               "idAsiento": 456, "asiento": "DI-A-2025-06-0007 / RRH-2025-06-0001" } ] }
```

Los renglones de empleados usan el código interno `Ennn` del inventario de roles, nunca nombre ni cédula.

## 2. Contrato del lote

```json
{
  "mes": "2025-06", "etiqueta": "roles contador", "idEmpresa": 1236, "idUsuario": 38, "usuario": null,
  "soloValidar": false, "aceptarInferidos": false,
  "observacion": "CARGA HISTORICA [roles contador] mes 2025-06", "centroCosto": "1.01",
  "fuente": "05-silvia/ROLES/ROLES 2025 MAY - DIC/ROL DE PAGOS JUNIO 2025.xlsx",
  "periodo": { "anio": 2025, "mes": 6, "fechaInicio": "2025-06-01", "fechaFin": "2025-06-30", "fechaContable": "2025-06-30" },
  "empleados": [ { "ref": "E01", "identificacion": "…", "apellidos": "…", "nombres": "…", "idEmpleadoSaa": null,
                   "fechaIngreso": "2025-01-15", "fechaSalida": "2025-06-20", "sueldo": 1000, "cargo": "…",
                   "modalidadDecimoTercero": 2, "modalidadDecimoCuarto": 2, "modalidadFondosReserva": 1 } ],
  "roles": [ { "ref": "E01", "identificacion": "…", "dias": 20, "sueldo": 1000, "totalIngresos": 666.67,
               "totalDescuentos": 63.00, "neto": 603.67, "baseIess": 666.67, "aportePersonal": 63.00,
               "aportePatronal": 81.00, "fondosReserva": 0,
               "renglones": [ { "concepto": "SUELDO", "valor": 666.67 }, { "concepto": "APIESS", "valor": 63.00 },
                              { "concepto": "APPATR", "valor": 81.00 } ] } ],
  "provisiones": [ { "ref": "E01", "identificacion": "…", "tipo": "D13", "concepto": "PRVD3", "valor": 55.56 } ],
  "pagos": [ { "ref": "PAGO-2025-06-1", "fecha": "2025-07-01", "idCuentaBancaria": 417, "banco": "INTERNACIONAL",
               "inferida": false, "motivo": "fecha del banco (EXTRACTO)", "total": 1500.00,
               "detalle": [ { "ref": "E02", "identificacion": "…", "valor": 1500.00 } ] } ],
  "planillasIess": [ { "ref": "IESS-2025-06-APORTES", "tipo": 1, "numeroComprobante": "<número del comprobante IESS>",
                       "fechaMaximaPago": "2025-07-15", "valorIess": 4000.00,
                       "componentes": [ { "concepto": "Aporte personal (rol)", "conceptoTipo": 1, "valor": 1800.00 } ],
                       "pago": { "fecha": "2025-07-14", "idCuentaBancaria": 418,
                                 "lineas": [ { "cuenta": "2.5.11", "valor": 1800.00, "descripcion": "Aporte personal IESS" },
                                             { "cuenta": "2.5.10", "valor": 2200.00, "descripcion": "Aporte patronal IESS y CCC" } ] } } ],
  "control": { "…": "totales del generador; el servicio los ignora" }
}
```

- `renglones[].concepto` es `RHH.CPNM.CPNMABRV`. Ingresos (tipo 1) − descuentos (tipo 2) = `neto` (±0,01); `APPATR`
  (tipo 3) va aparte. La extensión de cónyuges va como un segundo renglón `APIESS` con su glosa.
- `provisiones[].tipo`: `D13`, `D14`, `VAC`, `FR` (rubro 206: 1, 2, 3, 4).
- `pagos[].inferida = true`: fecha sin evidencia directa (ni banco ni conciliación del contador). Se **rechaza** salvo
  `aceptarInferidos: true`, que exige una decisión registrada en el expediente.
- `planillasIess[].tipo`: rubro 330 (1 aportes, 2 quirografarios, 3 hipotecarios, 4 fondos de reserva). `pago.lineas`
  van al DEBE; el HABER es la cuenta contable de la cuenta bancaria por la suma. Sin `pago`, la planilla se registra sin pagar.
- Campos desconocidos se ignoran (`@JsonIgnoreProperties(ignoreUnknown = true)`).

## 3. Pasos (orden y transacción)

Cada paso corre en su propia transacción; un paso RECHAZADO no deshace los anteriores. Si el período no se crea, no se
intentan los siguientes.

| # | Paso | Qué hace | Idempotencia |
|---|---|---|---|
| 0 | `validarLote` | Empresa, usuario (38 por defecto), período del lote = mes, **períodos contables abiertos** de la fecha contable y de todas las fechas de pago, `RHH.CFNM`, conceptos, centro de costo activo, cuentas bancarias de la empresa con cuenta contable, cuentas de los pagos de planillas de movimiento y activas, cada rol/pago con su empleado en el lote | — |
| 1 | `cargarEmpleado` (uno por persona) | Si no existe por cédula: `MPLD` (estado 1, o 4 cesante si tiene salida; región 1; centro de costo del lote) y `CNTE` (`CT-<cédula>`, tipo 1, desde el ingreso hasta la salida, sueldo, modalidades de décimos y fondos, `ACTIVO`/`CERRADO`) | Existe por cédula → OMITIDO (avisa si ningún contrato cubre el mes) |
| 2 | `cargarPeriodo` | `PRDN` **modo 2 (productivo)**, mensual, `PRDNFCCN` = fecha contable, totales; una `NMNA` por rol (estado APROBADA) con su contrato del mes; `RNGL` por renglón (origen 6 ingreso manual, `RNGLRFTB = CARGA_HISTORICA`); `PVNM` por provisión. Queda **APROBADO** sin pasar por `calcularPeriodo` ni `aprobarPeriodo` | Período del mes con la traza en `PRDNOBSR` → OMITIDO (se retoma); sin la traza → **RECHAZADO** |
| 3 | `contabilizarRol` | `ContabilizacionNominaService.contabilizarRol` (plantilla alterno 163) → período CONTABILIZADO; después centro de costo en las líneas `4.3.*` y traza en la observación del asiento | `PRDNASNT` lleno → OMITIDO |
| 4 | `contabilizarProvisiones` | `contabilizarProvisiones` (plantilla 164), centro de costo y traza | `PRDNASPR` lleno → OMITIDO |
| 5 | `cargarPago` (uno por grupo fecha + cuenta) | `RDPG` `OPH-<ref>` con `CTBNCDGO` y su `DRPG` por empleado (estado PAGADO); `contabilizarPago(orden, fecha, usuario)` → asiento debe 2.5.01 / haber cuenta del banco con la fecha del banco, período PAGADO; orden CONFIRMADA; NMNA PAGADA | Orden `OPH-<ref>` del período → OMITIDO |
| 6 | `cargarPlanillaIess` | Si la base tiene `RHH.PLIS/DLIS`: los graba (estado 2, o 3 pagada) con sus componentes. El pago: `AsientoContableService.generarAsiento` (alterno 6, módulo TESORERÍA, origen RRH) con las líneas del lote y el banco al HABER | Planilla (empresa, tipo, comprobante) existente → OMITIDO; sin tablas, asiento activo con la traza del pago → OMITIDO |
| 7 | `cerrarPeriodo` | `ProcesoNominaService.cerrarPeriodo` (acumulados `ACMN`) si quedó PAGADO | CERRADO → OMITIDO. Si el motor lo bloquea (p. ej. novedades IESS sin reportar), queda como **aviso** y el período sigue PAGADO |

**Por qué así (y no por los endpoints normales):**

- `PRDNMODO = 1` (histórico) no genera ningún asiento; por eso el período va en modo 2 con fecha contable del pasado.
- `calcularPeriodo` recalcularía con las reglas del motor (que difieren del rol del contador) y `aprobarPeriodo` emite
  roles; se graban los valores del Excel y se pasa a APROBADO directo, que es lo único que exige la contabilización.
- `/rdpg/generar` en modo productivo manda cada pago a la bandeja de Tesorería y `/rdpg/confirmar` rechaza órdenes
  productivas nuevas; la orden se crea aquí y se contabiliza con `contabilizarPago`, que no exige estado.
- `PlanillaIessServiceImpl.registrar` exige el período CALCULADO y `pagar` crea un pago de Tesorería por débito
  automático; aquí se graba directo y el asiento sale con las líneas del lote.
- Ningún asiento de nómina lleva centro de costo (`CFNMDCCS` no se usa): se impone después, como C11.

## 4. Contabilidad que resulta

| Asiento | Fecha | Tipo (vigencia 1-jun-2025, C12) | Líneas |
|---|---|---|---|
| Rol | último día del mes | DI-A, `RRH-AAAA-MM-nnnn` | D 4.3.01.05 sueldos · 4.3.01.25 fondos pagados en rol · 4.3.01.15.13/.14 décimos mensualizados · 4.3.01.20 aporte patronal / H 2.5.11 aporte personal · 2.5.10 aporte patronal · 2.5.12 / 2.5.13 préstamos IESS · 1.4.03.10.02 anticipos · 2.5.05 IR · 2.5.01 neto |
| Provisiones | último día del mes | DI-A | D 4.3.01.15.13 · .15.14 · 4.3.01.90.05 · 4.3.01.25 / H 2.5.08 · 2.5.09 · 2.5.14 · 2.5.04 |
| Pago de líquidos | fecha del banco | EG-A | D 2.5.01 / H banco de la orden (417 → 1.1.02.05.80; 418 → 1.1.02.05.65) |
| Pago de planilla IESS | fecha del débito | EG-A | D 2.5.11 · 2.5.10 · 2.5.12 · 2.5.13 · 2.5.04 / H 1.1.02.05.65 |

Centro de costo 1.01 en todas las líneas `4.3.*`. Diferencias de ±0,01 entre los líquidos del Excel y ingresos −
descuentos las absorbe el motor contra 2.5.01 (`CFNMTLCD`).

## 5. Uso

```bash
# 1. lote del mes (cuadreSuper)
node 03-trabajo/nomina/scripts/00-correr-sql.js sql/02-catalogos-c13.sql
node 03-trabajo/nomina/scripts/01-inventario-roles.js
node 03-trabajo/nomina/scripts/02-genera-lotes.js 2025-06
# 2. validar y cargar
curl -s -X POST http://localhost:8080/SaaBE/rest/historico/validarHistoricoNomina -H "Content-Type: application/json" --data @03-trabajo/nomina/lotes/2025-06/lote.json
curl -s -X POST http://localhost:8080/SaaBE/rest/historico/cargarHistoricoNomina  -H "Content-Type: application/json" --data @03-trabajo/nomina/lotes/2025-06/lote.json
```

Mes a mes y en orden: el cierre de cada mes escribe los acumulados que usa el siguiente.

## 6. Controles después de cargar (solo SELECT)

```sql
-- período, totales y asientos
SELECT PRDNCDGO, PRDNESTD, PRDNMODO, PRDNFCCN, PRDNTTIN, PRDNTTDS, PRDNTTNT, PRDNTTPT, PRDNNMEM, PRDNASNT, PRDNASPR, PRDNASPG
FROM RHH.PRDN WHERE PJRQCDGO = 1236 AND PRDNANOO = 2025 AND PRDNMSEE = 6;
-- nóminas = rol del contador
SELECT COUNT(*), SUM(NMNATING), SUM(NMNATDSC), SUM(NMNANETO), SUM(NMNAAPPT) FROM RHH.NMNA n JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
WHERE p.PJRQCDGO = 1236 AND p.PRDNANOO = 2025 AND p.PRDNMSEE = 6;
-- asientos de la carga del mes: tipo, numeración por módulo, cuadre y centro de costo
SELECT a.ASNTCDGO, a.ASNTFCHA, t.PLNTNMBR, a.ASNTNMAL, a.ASNTNAMD, SUM(d.DTASDBEE) debe, SUM(d.DTASHBRR) haber,
       SUM(CASE WHEN p.PLNNCNTA LIKE '4.3.%' AND d.CNCSCDGO IS NULL THEN 1 ELSE 0 END) gasto_sin_centro
FROM CNT.ASNT a JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
WHERE a.PJRQCDGO = 1236 AND a.ASNTOBSR LIKE '%CARGA HISTORICA [roles contador] mes 2025-06%'
GROUP BY a.ASNTCDGO, a.ASNTFCHA, t.PLNTNMBR, a.ASNTNMAL, a.ASNTNAMD ORDER BY a.ASNTFCHA, a.ASNTCDGO;
-- saldo de 2.5.01 del mes (lo que queda por pagar: líquidos sin pago identificado o inferidos no aceptados)
SELECT SUM(d.DTASHBRR) - SUM(d.DTASDBEE) FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD = 1 AND p.PLNNCNTA = '2.5.01' AND a.ASNTOBSR LIKE '%mes 2025-06%';
```

## 7. Límites conocidos

- Un período del mes que no sea de la carga (ene–sep 2026 ya existen en producción) se **rechaza**: reemplazarlo es
  una decisión (N-1 del análisis); el cargador no anula ni descontabiliza nada.
- No carga finiquitos (no están en los roles) ni las comisiones bancarias de los débitos del IESS (van con la
  conciliación bancaria).
- Sin `RHH.PLIS/DLIS` en la base (script `lap1-08` sin aplicar en producción) las planillas no quedan en el módulo,
  solo el asiento de su pago.
- `contabilizarPago` dispara el cierre de cuotas de descuentos recurrentes del período (`REQUIRES_NEW`); con empleados
  sin descuentos recurrentes no hace nada.
