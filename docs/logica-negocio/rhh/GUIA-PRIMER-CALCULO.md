# Guía de la primera ejecución del motor de nómina

**Sistema:** SAA · **Módulo:** RRHH · **Documento vigente desde:** 2026-08-19

Esta guía lleva de una base recién cargada a un rol de pagos calculado. Está escrita para que
alguien **sin contexto del proyecto** pueda ejecutar la prueba completa y saber, en cada paso, si
va bien o qué significa el error que le salió.

> **Para qué sirve.** El motor tiene ~1.100 líneas que compilan pero nunca se han ejecutado. El
> criterio de aceptación del §8 del maestro es cuadrar enero de 2026 contra el rol que ASOPREP
> pagó de verdad. Esta guía es el camino más corto hasta ese punto.

---

## 0. Antes de empezar

### 0.1 Los scripts

| Situación | Qué ejecutar |
|---|---|
| Base nueva, nunca se corrió nada | `01` a `09`, en orden |
| Ya se corrieron `01` a `09` antes del 2026-08-19 | Solo `10_DELTA_POST_FASE4.sql` |

Los scripts `07`, `08`, `09` y `10` piden el parámetro `:EMPRESA` y **los cuatro deben recibir el
mismo valor**. Se obtiene con:

```sql
SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ WHERE PJRQNVLL = 1;
```

Anota ese número: aparece en todo el resto de la guía como `:EMPRESA`. **No des por hecho que es
1.** En la instalación de ASOPREP es **1236**: `SCP.PJRQ` es la jerarquía completa del sistema y
la empresa es un nodo dentro de ella, no la primera fila. Los ejemplos de más abajo usan 1236.

### 0.2 Comprobación de que la parametría quedó bien

Cuatro consultas. Si alguna no da lo esperado, **no sigas**: el motor fallará más adelante con un
error menos claro que este.

```sql
-- 1. Parámetros del año. Debe devolver una fila con SBU 482.
SELECT PRNMANOO, PRNMSBUU, PRNMAPPR, PRNMAPPT, PRNMIECE, PRNMSCAP, PRNMFNRS, PRNMDIAS
  FROM RHH.PRNM WHERE PJRQCDGO = :EMPRESA AND PRNMANOO = 2026;

-- 2. Catálogo de conceptos. Debe devolver 44.
SELECT COUNT(*) FROM RHH.CPNM WHERE PJRQCDGO = :EMPRESA AND CPNMESTD = 1;

-- 3. Los 22 roles del motor asignados y sin repetir. Debe devolver 22 filas, roles 1 a 22.
SELECT CPNMROLM, CPNMALTR, CPNMNMBR FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL ORDER BY CPNMROLM;

-- 4. Tabla de impuesto a la renta. Debe devolver 10 tramos.
SELECT COUNT(*) FROM RHH.TBIR WHERE PJRQCDGO = :EMPRESA AND TBIRANOO = 2026;
```

### 0.3 Cómo se llaman los endpoints

Todos cuelgan de `/SaaBE/rest/`. Los ejemplos usan `curl`; sirve igual Postman o el propio
frontend. El CRUD estándar de cualquier tabla es `POST /rest/{tabla}` para crear.

---

## 1. Los cuatro registros mínimos

El motor necesita exactamente cuatro cosas para calcular: una **empresa** (ya existe), un
**tipo de contrato**, un **empleado**, un **contrato** y un **período**. Nada más.

### 1.1 Tipo de contrato — `POST /rest/tpce`

Hace falta porque `CNTE.TPCECDGO` es `NOT NULL`. Es un catálogo, se crea una sola vez.

```json
{
  "empresa":              { "codigo": 1236 },
  "nombre":               "Indefinido tiempo completo",
  "requiereFechaFin":     "N",
  "tipoRelacionLaboral":  1,
  "estado":               "A",
  "fechaRegistro":        "2026-01-01",
  "usuarioRegistro":      "PRUEBA"
}
```

| Campo | De dónde sale el valor |
|---|---|
| `empresa.codigo` | El `:EMPRESA` del paso 0.1 |
| `tipoRelacionLaboral` | Código alterno del rubro 186. `1` = indefinido tiempo completo |
| `estado` | `TPCEESTD` sigue siendo `VARCHAR2`: texto libre, no rubro |

Guarda el `codigo` que devuelve. Aquí se asume **1**.

### 1.2 Empleado — `POST /rest/mpld`

```json
{
  "empresa":                { "codigo": 1236 },
  "identificacion":         "1712345678",
  "apellidos":              "PEREZ LOPEZ",
  "nombres":                "JUAN CARLOS",
  "fechaNacimiento":        "1985-04-12",
  "estado":                 1,
  "fechaIngreso":           "2024-06-01",
  "region":                 1,
  "enfermedadCatastrofica": "N",
  "discapacidad":           "N",
  "fechaRegistro":          "2026-01-01",
  "usuarioRegistro":        "PRUEBA"
}
```

| Campo | De dónde sale, y por qué importa |
|---|---|
| `empresa.codigo` | **Obligatorio.** Sin él, `selectActivosEnPeriodo` no encuentra al empleado y el período sale con cero empleados |
| `estado` | Rubro 185. **`1` = ACTIVO.** Si vale `4` (CESANTE) el motor lo excluye a propósito |
| `fechaIngreso` | La real. **Determina si cobra fondos de reserva**: solo desde el año de servicio cumplido |
| `region` | Rubro 187. `1` = Sierra y Amazonía (Quito), `2` = Costa e Insular. Solo afecta al décimo cuarto anual, no al rol mensual |
| `enfermedadCatastrofica` | `S`/`N`. En `S` amplía el tope de gastos personales a 100 canastas |

Guarda el `codigo`. Aquí se asume **1**.

### 1.3 Contrato — `POST /rest/cnte`

Es el registro que más decide sobre el cálculo. Cada bandera enciende o apaga un paso del motor.

```json
{
  "empleado":                 { "codigo": 1 },
  "tipoContratoEmpleado":     { "codigo": 1 },
  "numero":                   "CT-2024-001",
  "fechaInicio":              "2024-06-01",
  "salarioBase":              800.00,
  "estado":                   "A",
  "tipoRelacionLaboral":      1,
  "jornada":                  1,
  "horasSemanales":           40.00,
  "modalidadDecimoTercero":   1,
  "modalidadDecimoCuarto":    1,
  "modalidadFondosReserva":   1,
  "derechoDecimoCuarto":      "S",
  "aportaIess":               "S",
  "retieneFuente":            "N",
  "fechaRegistro":            "2026-01-01",
  "usuarioRegistro":          "PRUEBA"
}
```

| Campo | Valor | Qué provoca en el motor |
|---|---|---|
| `fechaInicio` | La real del contrato | Junto con `fechaFin` y `fechaTerminacion`, decide si el contrato entra en el período |
| `salarioBase` | El sueldo pactado | Base del prorrateo del paso 4 |
| `tipoRelacionLaboral` | `1` (rubro 186) | `6` = servicios profesionales → honorario sin prorrateo. `5` = por horas → usa `valorHora` |
| `modalidadDecimoTercero` | `1` MENSUALIZADO (rubro 188) | `1` genera renglón de ingreso; `2` ACUMULADO genera provisión |
| `modalidadDecimoCuarto` | `1` MENSUALIZADO (rubro 189) | Igual |
| `modalidadFondosReserva` | `1` MENSUALIZADO (rubro 190) | `1` genera renglón **si ya cumplió un año**; `2` ACUMULADO_EN_EL_IESS genera provisión; `3` NO_APLICA no hace nada |
| `derechoDecimoCuarto` | `S` | En `N` se salta el décimo cuarto por completo |
| `aportaIess` | `S` | En `N` **no se generan los cuatro aportes**. Va en `N` solo en servicios profesionales |
| `retieneFuente` | `N` | En `S` se salta la proyección de IR y se aplica `porcentajeRetencionFuente` sobre el honorario |

**`fechaFin` y `fechaTerminacion` se dejan sin enviar.** Un contrato vigente no las tiene.

### 1.4 Período de nómina — `POST /rest/prdn`

```json
{
  "empresa":         { "codigo": 1236 },
  "anio":            2026,
  "mes":             1,
  "fechaInicio":     "2026-01-01",
  "fechaFin":        "2026-01-31",
  "estado":          1,
  "modo":            1,
  "tipoPeriodo":     1,
  "fechaRegistro":   "2026-01-31",
  "usuarioRegistro": "PRUEBA"
}
```

| Campo | Valor | Por qué |
|---|---|---|
| `estado` | `1` ABIERTO (rubro 182) | Solo ABIERTO, EN_CALCULO o CALCULADO admiten cálculo |
| `modo` | **`1` HISTORICO_SIN_CONTABILIZAR** (rubro 184) | **Es lo que permite calcular sin plan de cuentas.** Con `2` PRODUCTIVO exigiría cuentas contables configuradas |
| `tipoPeriodo` | `1` MENSUAL (rubro 212) | |
| `fechaFin` | El último día del mes | **De aquí sale el año con el que se busca el `PRNM`**, no de `anio` |

Guarda el `codigo`. Aquí se asume **1**.

---

## 2. La secuencia de llamadas

### Paso 1 · Validar — `POST /rest/prdn/validar/1`

```bash
curl -X POST http://localhost:8080/SaaBE/rest/prdn/validar/1
```

**Si va bien:** `[]` — una lista vacía. Significa que se puede calcular.

**Si devuelve mensajes que empiezan por `Aviso:`**, se puede calcular igual: son divergencias
informativas. Cualquier otro mensaje bloquea.

### Paso 2 · Calcular — `POST /rest/prdn/calcular/1`

```bash
curl -X POST "http://localhost:8080/SaaBE/rest/prdn/calcular/1?usuarioRegistro=PRUEBA"
```

**Si va bien:**

```json
{
  "idPeriodo": 1,
  "empleadosProcesados": 1,
  "empleadosConError": 0,
  "totalIngresos": 973.48,
  "totalDescuentos": 75.60,
  "totalNeto": 897.88,
  "totalPatronal": 97.20,
  "errores": []
}
```

Los valores son los del caso de la sección 3. **Si `empleadosProcesados` es 0**, ningún contrato
se solapó con el período: ver el error 2 de la sección 4.

### Paso 3 · Ver el detalle

El resultado del paso 2 da totales. Para ver la cabecera de cada nómina:

```bash
curl http://localhost:8080/SaaBE/rest/nmna/getAll
```

> **No uses `selectByCriteria` para esto.** Construye el JPQL leyendo los operadores (`and`,
> `like`, `between`…) del catálogo `Rubro`/`DetalleRubro`; si esas filas no están cargadas falla
> con un error que no tiene nada que ver con la nómina. Para una primera ejecución, `getAll` o
> SQL directo.

Para los renglones uno por uno conviene ir contra la base:

```sql
SELECT c.CPNMNMBR, c.CPNMTPCN, r.RNGLCANT, r.RNGLBSCL, r.RNGLPRCN, r.RNGLVLRO
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  LEFT JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE n.PRDNCDGO = 1
 ORDER BY r.RNGLORDN;
```

Y las provisiones, que no salen en el rol pero sí en el costo:

```sql
SELECT p.PVNMTPPR, c.CPNMNMBR, p.PVNMBSCL, p.PVNMVLOR
  FROM RHH.PVNM p
  LEFT JOIN RHH.CPNM c ON c.CPNMCDGO = p.CPNMCDGO
 WHERE p.PRDNCDGO = 1 ORDER BY p.PVNMTPPR;
```

**Aquí es donde se compara contra el rol real.** El caso de la sección 3 dice exactamente qué
debe salir.

### Paso 4 · Recalcular si hace falta

`calcularPeriodo` es idempotente: se puede repetir cuantas veces sea necesario tras corregir un
dato. Borra y regenera `NMNA`, `RNGL` y `PVNM`, **preservando los renglones con `RNGLMNAL='S'`**.

Para un solo empleado:

```bash
curl -X POST http://localhost:8080/SaaBE/rest/prdn/recalcularEmpleado \
  -H "Content-Type: application/json" \
  -d '{"idPeriodo":1,"idEmpleado":1,"preservarManuales":true,"usuarioRegistro":"PRUEBA"}'
```

Para probar un cambio **sin persistirlo**:

```bash
curl -X POST http://localhost:8080/SaaBE/rest/prdn/simular \
  -H "Content-Type: application/json" \
  -d '{"idContrato":1,"idPeriodo":1}'
```

### Paso 5 · Aprobar — `POST /rest/prdn/aprobar/1`

```bash
curl -X POST "http://localhost:8080/SaaBE/rest/prdn/aprobar/1?usuarioRegistro=PRUEBA"
```

**Si va bien:** `200` sin cuerpo. El período pasa a APROBADO.

Aquí es donde la divergencia de porcentajes **bloquea** — ver el error 4.

### Paso 6 · Contabilizar — `POST /rest/prdn/contabilizar/1`

```bash
curl -X POST "http://localhost:8080/SaaBE/rest/prdn/contabilizar/1?usuarioRegistro=PRUEBA"
```

**Si va bien:** `204 No Content`, sin cuerpo. En un período histórico **no se genera asiento** y
`PRDNASNT` queda en nulo a propósito, pero el período avanza a CONTABILIZADO.

Que llegue a CONTABILIZADO sin asiento no es una trampa: el estado describe el avance del flujo
de nómina, y en un período histórico ese tramo está deliberadamente vacío. Quien quiera saber si
hubo asiento mira `PRDNASNT`, no el estado.

**Si el período tiene `modo = 2`** (PRODUCTIVO_CONTABILIZA) devuelve error: la contabilización
real es de la fase 6 y todavía no existe. Para la carga histórica el período debe crearse con
`modo = 1`.

### Paso 7 · Cerrar — `POST /rest/prdn/cerrar/1`

```bash
curl -X POST "http://localhost:8080/SaaBE/rest/prdn/cerrar/1?usuarioRegistro=PRUEBA"
```

**Este es el paso que importa de verdad**, porque **`cerrarPeriodo` es el único punto donde se
escriben los acumulados `ACMN`** — y de ellos se calculan la proyección de IR, los décimos, los
fondos de reserva y las vacaciones de todo el año. Sin cerrar enero, febrero saldría mal.

Que sea el único punto es deliberado: evita que recalcular duplique los acumulados.

**Comprobación de que el cierre hizo su trabajo** — deben aparecer ocho filas, una por tipo:

```sql
SELECT a.ACMNTPAC, a.ACMNANOO, a.ACMNMSEE, a.ACMNVLOR, a.ACMNDIAS
  FROM RHH.ACMN a
 WHERE a.PRDNCDGO = 1 ORDER BY a.ACMNTPAC;
```

| Tipo | Nombre | Valor esperado |
|---|---|---|
| 1 | Imponible IESS | 800,00 |
| 2 | Gravado IR | 800,00 |
| 3 | Base décimo tercero | 800,00 |
| 5 | Base fondos de reserva | 800,00 |
| 8 | Aporte personal | 75,60 |
| 10 | Días trabajados | días 30,0000 |

Los tipos 4 (base décimo cuarto) y 9 (retención IR) **no aparecen**: valen cero y `escribeAcumulado`
no graba filas en cero.


### Centinela de la carga histórica

`escribeAcumulado` **no graba filas en cero**. Para sumar da igual —una fila ausente y una en
cero suman lo mismo—, pero tiene una consecuencia que hay que vigilar: **no se puede distinguir
«mes procesado con cero» de «mes que nunca se cerró»**.

En la carga de enero a julio eso es un riesgo real. Si un período falla al cerrar y nadie lo
nota, la proyección de IR de agosto verá menos meses de los que hubo y dará una **retención más
baja, sin error y sin aviso**. El dinero faltante aparecería recién en la liquidación anual.

El centinela es el acumulado de **tipo 10 (`DIAS_TRABAJADOS`)**: es el único que nunca puede
valer cero en un mes efectivamente trabajado, así que su ausencia delata el período que no cerró.

```sql
SELECT a.ACMNANOO, a.ACMNMSEE, COUNT(DISTINCT a.MPLDCDGO) AS EMPLEADOS
  FROM RHH.ACMN a
 WHERE a.ACMNTPAC = 10 AND a.ACMNANOO = 2026
 GROUP BY a.ACMNANOO, a.ACMNMSEE ORDER BY 2;
```

**Al terminar la carga deben salir siete filas, de enero a julio, todas con el mismo número de
empleados.** Un mes que falte, o que tenga menos empleados que los demás, no cerró bien.

Conviene correrla **después de cerrar cada período**, no solo al final: cuanto antes se detecte
el mes que no cerró, menos períodos posteriores hay que reabrir y recalcular.

**El único falso positivo posible** es un empleado con cero días trabajados en todo el mes —una
licencia sin sueldo completa, por ejemplo—. No tendrá fila de tipo 10 y parecerá un cierre
fallido. Es raro y se distingue mirando su `NMNA`: si existe con `NMNADITR = 0`, el período sí
cerró y el empleado simplemente no trabajó.

---

## 3. El caso de prueba, calculado a mano

Esta sección **no se obtuvo ejecutando el código**. Está calculada desde los parámetros del
script 07 y el catálogo del script 08. Es la única forma de distinguir «el motor calcula» de «el
motor calcula bien»: si el resultado difiere, uno de los dos está mal, y hay que averiguar cuál.

### 3.1 Datos de entrada

| Dato | Valor |
|---|---|
| Sueldo base | 800,00 |
| Relación laboral | Indefinido tiempo completo |
| Período | Enero 2026, mes completo |
| Días trabajados | 30 de 30 |
| Fecha de ingreso | 01-jun-2024 → **1 año 7 meses al 31-ene-2026** |
| Décimo tercero | Mensualizado |
| Décimo cuarto | Mensualizado, con derecho |
| Fondos de reserva | Mensualizado |
| Aporta al IESS | Sí |
| Novedades, horas extra, conceptos fijos, descuentos | Ninguno |
| Cargas familiares | Ninguna |
| Gastos personales declarados | Ninguno |

### 3.2 Parámetros que usa (script 07, año 2026)

| Parámetro | Columna | Valor |
|---|---|---|
| SBU | `PRNMSBUU` | 482,00 |
| Aporte personal IESS | `CPNMPRCN` del rol 1 | 9,45 % |
| Aporte patronal IESS | `CPNMPRCN` del rol 2 | 11,15 % |
| IECE | `CPNMPRCN` del rol 3 | 0,50 % |
| SECAP | `CPNMPRCN` del rol 4 | 0,50 % |
| Fondos de reserva | `CPNMPRCN` del rol 5 | 8,33 % |
| Días base del mes | `PRNMDIAS` | 30 |
| Fracción básica IR | `TBIR` tramo 1 | hasta 12.208 → 0 % |

### 3.3 El cálculo, paso por paso

**Paso 4 — sueldo del período**

```
800,00 × 30 / 30 = 800,00
```

**Paso 6 — bases.** Se calculan sobre los renglones existentes en ese momento, que aquí es solo
el sueldo. Sus banderas en el catálogo son `IMIE='S'`, `IMIR='S'`, `APFR='S'`, `BSDT='S'`,
`BSDC='N'`:

| Base | Valor | Por qué |
|---|---|---|
| Imponible IESS | 800,00 | El sueldo es imponible |
| Gravado IR | 800,00 | El sueldo es gravado |
| Fondos de reserva | 800,00 | El sueldo aporta a FR |
| Décimo tercero | 800,00 | El sueldo entra en la base del D3 |
| **Décimo cuarto** | **0,00** | **El sueldo tiene `CPNMBSDC='N'`**: el D4 no se calcula sobre el sueldo sino sobre el SBU |

**Paso 7 — aportes**, sobre la base imponible de 800,00:

```
Aporte personal   800,00 × 9,45 %  = 75,60
Aporte patronal   800,00 × 11,15 % = 89,20
IECE              800,00 × 0,50 %  =  4,00
SECAP             800,00 × 0,50 %  =  4,00
```

**Paso 8 — fondos de reserva.** Ingresó el 01-jun-2024, así que al 31-ene-2026 ya cumplió el año:

```
800,00 × 8,33 % = 66,64
```

**Paso 9 — décimo tercero mensualizado:**

```
800,00 / 12 = 66,6666… → 66,67
```

**Paso 10 — décimo cuarto mensualizado:**

```
(482,00 / 12) × (30 / 30) = 40,1666… → 40,17
```

**Paso 11 — impuesto a la renta.** La proyección anual da:

```
Ingresos proyectados        800,00 × 12            = 9.600,00
Aporte personal proyectado  75,60 × 12             =   907,20
Base imponible              9.600,00 − 907,20      = 8.692,80
```

8.692,80 cae en el primer tramo de la tabla 2026 (hasta 12.208, al 0 %), así que el impuesto
causado es **0,00** y **no se genera renglón de IR**. Con sueldo 800 el empleado está bajo el
umbral, que es lo esperable.

**Pasos 13 y 14 — neto.** El neto es ingresos menos egresos; los patronales no lo tocan:

```
Ingresos    800,00 + 66,64 + 66,67 + 40,17 = 973,48
Egresos                                       75,60
Neto        973,48 − 75,60                 = 897,88
```

### 3.4 El resultado esperado

**Renglones que debe producir el motor — ocho en total, cuatro de ellos patronales:**

| # | Concepto | Rol | Tipo | Base | % | Valor |
|---|---|---|---|---|---|---|
| 1 | Sueldo | — | Ingreso | 800,00 | — | **800,00** |
| 2 | Fondos de reserva | 5 | Ingreso | 800,00 | 8,33 | **66,64** |
| 3 | Décimo tercero mensualizado | 6 | Ingreso | 800,00 | — | **66,67** |
| 4 | Décimo cuarto mensualizado | 7 | Ingreso | 482,00 | — | **40,17** |
| 5 | Aporte personal IESS | 1 | Egreso | 800,00 | 9,45 | **75,60** |
| 6 | Aporte patronal IESS | 2 | Patronal | 800,00 | 11,15 | **89,20** |
| 7 | Aporte IECE | 3 | Patronal | 800,00 | 0,50 | **4,00** |
| 8 | Aporte SECAP | 4 | Patronal | 800,00 | 0,50 | **4,00** |

**Totales de la cabecera `NMNA`:**

| Campo | Columna | Valor |
|---|---|---|
| Total ingresos | `NMNATING` | **973,48** |
| Total descuentos | `NMNATDSC` | **75,60** |
| Neto a pagar | `NMNANETO` | **897,88** |
| Total patronal | `NMNATTPT` | **97,20** |
| Base imponible IESS | `NMNABSIE` | 800,00 |
| Base gravada IR | `NMNABSIR` | 800,00 |
| Base décimo tercero | `NMNABSDT` | 800,00 |
| Base décimo cuarto | `NMNABSDC` | **0,00** |
| Aporte personal | `NMNAAPPR` | 75,60 |
| Fondos de reserva | `NMNAFNRS` | 66,64 |
| Retención IR | `NMNARTIR` | 0,00 |
| Días trabajados | `NMNADITR` | 30,0000 |

**Provisiones que debe generar — una fila en `PVNM`:**

| Tipo | Concepto | Base | Valor |
|---|---|---|---|
| 3 VACACIONES | Provisión vacaciones (rol 19) | 800,00 | **33,33** |

Es la **única** provisión del escenario, y conviene entender por qué:

- **Décimo tercero, décimo cuarto y fondos de reserva no se provisionan** porque en este
  contrato están en modalidad MENSUALIZADO: ya se pagaron como renglón del rol. Si estuvieran en
  ACUMULADO aparecerían aquí y **no** como renglón.
- **Las vacaciones se provisionan siempre**, con independencia de cualquier modalidad. No admiten
  mensualización: el trabajador las goza en días o se le liquidan al salir. Es la única provisión
  que no tiene renglón equivalente en el rol.
- **Jubilación patronal y desahucio no aparecen**: vienen de `cargarProvisionActuarial`, no del
  cálculo mensual, y además `CFNMAPJP` y `CFNMAPDS` están en `N` para ASOPREP.
- **El aporte patronal no se provisiona.** Ya está en el rol como renglón patronal; provisionarlo
  además duplicaría el costo. Ver la nota al final de esta sección.

La provisión sale de `PRNM`, sin ninguna constante en el código:

```
800,00 × PRNMDIVC / PRNMDANO = 800,00 × 15 / 360 = 33,3333… → 33,33
```

**Costo total para el empleador:** 973,48 + 97,20 + 33,33 = **1.104,01**.

> **Por qué el aporte patronal no lleva provisión.** El §4.7 del plan lo listaba entre las
> provisiones mensuales y el rubro 206 tiene un tipo `APORTE_PATRONAL`, pero ni el catálogo del
> script 08 tiene un concepto para él ni el asiento de provisiones (rubro 214, códigos 30–35 y
> 40–45) tiene una línea donde ponerlo. El asiento de **rol** ya lo registra completo: línea 3
> «Gasto aporte patronal IESS» al DEBE y línea 11 «IESS por pagar aporte patronal» al HABER.
> Provisionarlo además contaría el mismo costo dos veces. **El §4.7 estaba equivocado**; el tipo
> 5 del rubro 206 queda sin uso.

### 3.5 Variante: empleado con menos de un año

La mayoría del personal de ASOPREP ingresó en junio de 2025, así que en enero de 2026 **todavía
no tiene derecho a fondos de reserva**. Con `fechaIngreso = 2025-06-01`, y todo lo demás igual:

| Renglón | Con 1 año cumplido | Sin cumplir el año |
|---|---|---|
| Fondos de reserva | 66,64 | **no aparece** |
| Total ingresos | 973,48 | **906,84** |
| Neto | 897,88 | **831,24** |
| Provisión vacaciones | 33,33 | 33,33 — **no cambia** |
| Costo empleador | 1.104,01 | **1.037,37** |

El resto de renglones no cambia. **Si el rol real de enero incluye fondos de reserva para alguien
que ingresó en junio de 2025, el error está en el dato, no en el motor.**

### 3.6 Los tres redondeos que conviene mirar primero

Si el total difiere en centavos, casi siempre es uno de estos tres:

| Concepto | Sin redondear | Redondeado |
|---|---|---|
| Décimo tercero | 66,666666… | **66,67** |
| Décimo cuarto | 40,166666… | **40,17** |
| Fondos de reserva | 66,64 exacto | 66,64 |

El motor redondea **cada renglón** antes de sumar, nunca el total. Si el rol real redondeó solo
al final, aparecerá una diferencia de uno o dos centavos que **no es un error del motor** sino
una diferencia de criterio: hay que decidir cuál se adopta y dejarlo escrito.

---

## 4. Los errores previsibles

### Error 1 · `No existen parametros de nomina (RHH.PRNM) para el anio 2026`

**Qué pasó.** El script 07 no se ejecutó, se ejecutó con otro `:EMPRESA`, o el período tiene una
`fechaFin` de un año que no está cargado.

**Ojo con lo último:** el año se toma de **`PRDNFCHF`**, no del campo `anio`. Un período con
`anio = 2026` pero `fechaFin = 2025-12-31` busca el `PRNM` de 2025.

```sql
SELECT PRNMANOO, PJRQCDGO FROM RHH.PRNM;
SELECT PRDNCDGO, PRDNANOO, PRDNFCHF, PJRQCDGO FROM RHH.PRDN;
```

### Error 2 · `No hay contratos activos que se solapen con el periodo`

O bien `empleadosProcesados: 0`. Cuatro causas posibles, en orden de frecuencia:

1. **El empleado no tiene empresa.** `MPLD.PJRQCDGO` en nulo. La consulta admite nulo, pero si
   tiene una empresa **distinta** a la del período, no entra.
2. **El empleado está CESANTE.** `MPLDESTD = 4` lo excluye a propósito.
3. **Las fechas no se solapan.** El contrato entra si
   `fechaInicio <= fechaFin del período` **y** (`fechaFin` es nulo **o** `>= fechaInicio del período`)
   **y** (`fechaTerminacion` es nulo **o** `>= fechaInicio del período`).
4. **El período no tiene empresa.** `PRDN.PJRQCDGO` en nulo → `validarPeriodo` lo dice
   explícitamente.

```sql
SELECT c.CNTECDGO, c.CNTEFCHI, c.CNTEFCHF, c.CNTEFCTR,
       e.MPLDIDNT, e.MPLDESTD, e.PJRQCDGO
  FROM RHH.CNTE c JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO;
```

### Error 3 · Faltan renglones, o el neto sale corto

Casi siempre es un **rol de concepto sin asignar**. El motor localiza cada concepto especial por
`CPNMROLM`; si el rol está en nulo, ese concepto **no se genera y no hay mensaje de error** — el
renglón simplemente no aparece.

```sql
-- Debe devolver 16 filas, roles 1 a 16 sin huecos
SELECT CPNMROLM, CPNMALTR, CPNMNMBR FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL ORDER BY CPNMROLM;
```

| Falta el rol | Renglón que no aparece |
|---|---|
| 1 | Aporte personal IESS — **el neto sale más alto de lo debido** |
| 2, 3, 4 | Aporte patronal, IECE, SECAP — el neto no cambia, el costo patronal sí |
| 5 | Fondos de reserva |
| 6 | Décimo tercero mensualizado |
| 7 | Décimo cuarto mensualizado |
| 8 | Retención de impuesto a la renta |

Se corrige con los `UPDATE` del paso 5 del script `10`.

### Error 4 · `No se puede aprobar el periodo: el porcentaje de N concepto(s) no coincide con la parametria del anio`

**Qué pasó.** El porcentaje de un concepto en `CPNM.CPNMPRCN` difiere del equivalente en
`RHH.PRNM`. Afecta a cinco parejas: aporte personal, patronal, IECE, SECAP y fondos de reserva.

**Por qué bloquea aquí y no antes.** En `validarPeriodo` es solo un `Aviso:` — se puede simular y
recalcular mientras se decide. En `aprobarPeriodo` bloquea, porque calcular con una tasa vieja se
corrige recalculando, pero aprobarla y contabilizarla no. La aprobación es el último punto
reversible.

**Cuál manda.** El cálculo usa **`CPNMPRCN`**, el del catálogo. `PRNM` es el respaldo y el
contraste. La razón es que `PRNM` no puede distinguir el IECE del SECAP —ambos son 0,50 % sobre
la misma base— mientras que el catálogo sí, porque son dos filas con su propia cuenta contable.

```sql
SELECT c.CPNMROLM, c.CPNMNMBR, c.CPNMPRCN AS EN_CATALOGO,
       CASE c.CPNMROLM WHEN 1 THEN p.PRNMAPPR WHEN 2 THEN p.PRNMAPPT
                       WHEN 3 THEN p.PRNMIECE WHEN 4 THEN p.PRNMSCAP
                       WHEN 5 THEN p.PRNMFNRS END AS EN_PARAMETRIA
  FROM RHH.CPNM c, RHH.PRNM p
 WHERE c.PJRQCDGO = :EMPRESA AND p.PJRQCDGO = :EMPRESA
   AND p.PRNMANOO = 2026 AND c.CPNMROLM BETWEEN 1 AND 5;
```

Corregir el que esté desactualizado, **recalcular**, y volver a aprobar.

### Error 5 · `ORA-00904: "CPNMROLM": identificador no valido`

No se aplicó el script `10`. Aparece en la primera consulta que toque `ConceptoNomina`, no al
desplegar.

### Error 6 · `El neto de PEREZ LOPEZ JUAN CARLOS queda en −X aun tras recortar todos los descuentos recortables`

El empleado tiene más descuentos que ingresos. El motor ya recortó todos los que llevan
`CPNMRCRT='S'`, en orden descendente de `CPNMORDN`. Los de ley —aporte IESS, impuesto a la renta,
retención judicial y préstamos IESS— **nunca se recortan**, y por eso lanza en vez de forzar el
neto a cero. Hay que revisar las novedades y los descuentos recurrentes de ese empleado.

### Error 7 · `El periodo esta en estado N y ya no admite calculo`

Estados que admiten cálculo: `1` ABIERTO, `2` EN_CALCULO, `3` CALCULADO. Si ya está APROBADO hay
que reabrirlo:

```bash
curl -X POST http://localhost:8080/SaaBE/rest/prdn/reabrir/1 \
  -H "Content-Type: application/json" \
  -d '{"motivo":"Correccion de sueldo base","usuarioRegistro":"PRUEBA"}'
```

`reabrirPeriodo` exige motivo y **retira los acumulados que hubiera escrito el cierre**, para que
el recálculo no los duplique.

---

## 5. Qué queda abierto tras esta prueba

| # | Punto | Efecto |
|---|---|---|
| 1 | ~~Un período solo acumula al cerrarse, y cerrar exige contabilizar~~ **Resuelto el 2026-08-19** | La rama histórica de `contabilizarRol` cierra el ciclo completo sin plan de cuentas. Queda en su lugar el riesgo del centinela: un cierre que falla en silencio no deja rastro salvo la fila de tipo 10 que no aparece |
| 2 | Criterio de redondeo del rol real | Si ASOPREP redondeó al total y no por renglón, aparecerán diferencias de céntimos. Hay que fijar cuál se adopta |
| 3 | El rol real de calibración | Es el insumo 6 del §9 del maestro, aún pendiente del cliente |
| 4 | La contabilización productiva sigue sin existir | Un período con `PRDNMODO = 2` no se puede contabilizar. Es la fase 6, y agosto de 2026 la necesita |

> **Regla de mantenimiento:** cualquier cambio en el motor, en los parámetros normativos o en el
> catálogo de conceptos debe actualizar el caso de la sección 3 en el mismo cambio. Si el caso
> deja de cuadrar, deja de servir para lo único que sirve.
