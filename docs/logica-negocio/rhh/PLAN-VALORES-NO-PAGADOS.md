# Valores no pagados en nómina — retener en un mes, devolver en el siguiente

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-08 · **Módulos:** `rhh` (backend + pantalla) · toca `tsr` sólo por la orden de pago que ya existe

**Pedido del usuario, textual:**

> *«Necesito registrar valores que no se pagaron a un empleado en un mes, debe existir una pantalla que me permita registrar por período y por empleado un valor que no se pagó. Lo importante es que este valor no debe afectar la contabilidad de los valores de nómina ni las provisiones. Lo que debe pasar con este valor es que debe restarse sólo en el momento del pago, y contablemente sólo debe quedar acumulado ese valor en remuneraciones por pagar. Este valor debe reflejarse en el rol de pagos como un valor en negativo. Y siempre el siguiente mes debe aparecer en el rol como un valor positivo, darse de baja de los valores no pagados y sólo sumarse el momento del pago para que se afecten remuneraciones por pagar contra bancos.»*

Y la precisión que cierra el diseño: *«la contabilidad del rol de ese mes sí se debe generar por los valores completos, sólo se mueven esos valores en la cuenta de remuneraciones por pagar».*

**Decisiones del usuario, 2026-09-08 (las seis, textuales):**

| # | Pregunta | Respuesta |
|---|---|---|
| 1 | ¿Se recupera siempre completo y al mes siguiente, automático? | **«sí, siempre»** |
| 2 | Si el empleado sale antes de cobrarlo, ¿va al finiquito? | **«sí, al finiquito»** |
| 3 | ¿El registro sólo antes de procesar el rol? | **«sí, antes de procesar el rol»** — **CORREGIDO por el usuario el 2026-09-08 al probar:** *«no me está dejando ingresar los valores en un período calculado. Sí debería, pero debería dejarme recalcular después»*. Regla vigente: se registra con el período **ABIERTO o CALCULADO**; nunca en `EN_CALCULO` (carrera) ni desde `APROBADO` en adelante. Registrar en un período calculado **obliga a recalcular el rol**, y la orden de pago se niega a generarse si encuentra un VNPG sin su renglón en la nómina (§7.2) |
| 4 | ¿Puede superar el neto? | **«jamás superar el neto»** |
| 5 | ¿Un solo valor por empleado y período, con motivo, anulable? | **«así es»** |
| 6 | ¿Afecta IESS o impuesto a la renta? | **«no afecta»** |

---

## 1. Lo que es, en una frase

**Es un anticipo al revés.** El anticipo es plata que sale antes y el rol la recupera; esto es plata
que **se retiene** en un mes y **el pago la devuelve** en el siguiente. La contabilidad de la
nómina no se entera: el rol devenga completo, y lo único que cambia es **cuánto se paga**, que es
lo único que mueve remuneraciones por pagar.

## 2. Por qué el diseño es chico — verificado en el código, no supuesto

- **El asiento del rol** (`ContabilizacionNominaServiceImpl:853`) acredita `SUELDOS_POR_PAGAR` con
  `nomina.getNetoPagar()`, el neto **completo**. No se toca.
- **El asiento del pago** (`ContabilizacionNominaServiceImpl:318`) debita `SUELDOS_POR_PAGAR_DEBE`
  y acredita `BANCO` **con `orden.getTotal()`** — el total de la orden de pago, no el neto de la
  nómina. **Si la orden lleva `neto − X` o `neto + X`, la contabilidad del pago sigue sola.** No se
  toca una línea del contabilizador.
- **El rol imprime desde los renglones** (`GeneracionRolPagoServiceImpl:100`), y el motor
  **suma al neto SÓLO los renglones INGRESO y EGRESO** (`ProcesoNominaServiceImpl:1078-1080`,
  verificado el 2026-09-07 para los décimos). Un renglón **INFORMATIVO** se ve en el rol y **no
  entra al neto, ni a las bases de IESS/IR, ni a las provisiones, ni a la contabilidad**. Es
  exactamente el mecanismo que se usó ayer para el décimo pagado en el mes (`PLAN-PAGO-DECIMOS-EN-EL-MES.md` §7.A).

Con eso, la funcionalidad completa vive en **tres lugares**: una tabla nueva que registra el valor,
**la orden de pago** que lo resta o lo suma, y **dos renglones informativos** que lo muestran en
el rol. Más el finiquito.

## 3. El flujo, mes a mes

Empleado con un valor no pagado **X** registrado para el período **P**.

| | Período P | Período P+1 |
|---|---|---|
| **Motor del rol** | Calcula todo normal: neto **N**. Agrega renglón INFORMATIVO **−X** «Valor no pagado» | Calcula normal: neto **M**. Agrega renglón INFORMATIVO **+X** «Valor no pagado del mes anterior» |
| **Rol impreso** | Ingresos, descuentos, neto N, y la línea −X. | Ingresos, descuentos, neto M, y la línea +X |
| **Asiento del rol** | Remuneraciones por pagar por **N** completo | Remuneraciones por pagar por **M** completo |
| **Orden de pago** | El empleado va por **N − X** | El empleado va por **M + X** |
| **Asiento del pago** | Remuneraciones por pagar contra bancos por **N − X** | Remuneraciones por pagar contra bancos por **M + X** |
| **Saldo en remuneraciones por pagar** | Queda **X** | **Se liquida** |
| **Estado del registro** | `RETENIDO` al generar la orden | `PAGADO` al confirmar el pago |

Un empleado puede tener a la vez el **+X₁** de P (recuperación) y un **−X₂** nuevo de P+1
(retención nueva): la orden lleva `M + X₁ − X₂`. Son dos registros distintos, de dos períodos
distintos, y no se pisan.

## 4. El modelo — `RHH.VNPG`, Valor No Pagado

Tabla nueva. **Nombre verificado libre** contra `src/main/java/com/saa/model/` (cero coincidencias)
y contra este registro; **falta confirmarlo contra `ALL_TABLES`**, y ese control es el **0.1 del
propio DDL**, que detiene el script si devuelve filas. Reservado en `REGISTRO-RESERVAS-EQUIPOS.md` §5.

| Columna | Tipo | Qué |
|---|---|---|
| `VNPGCDGO` | PK | Misma convención de generación que `ODBS` (copiar la entidad, no inventar) |
| `PJRQCDGO` | FK `SCP.PJRQ` | Empresa |
| `MPLDCDGO` | FK `RHH.MPLD` | Empleado |
| `VNPGPRNM` | FK `RHH.PRDN` | **Período en que NO se paga** |
| `VNPGVLOR` | `NUMBER(18,2)` | Valor X. Siempre positivo; el signo lo pone el renglón |
| `VNPGMTVO` | `VARCHAR2(500)` | Motivo. **Obligatorio** |
| `VNPGESTD` | `NUMBER` | Estado, rubro `RHH_ESTADO_VALOR_NO_PAGADO` (§5) |
| `VNPGPRRC` | FK `RHH.PRDN`, nulable | Período en que se recuperó (P+1). Se llena al pagar |
| `VNPGORRT` | FK `RHH.RDPG`, nulable | Orden de pago que lo **retuvo** |
| `VNPGORPG` | FK `RHH.RDPG`, nulable | Orden de pago que lo **devolvió** |
| `VNPGLQDC` | FK a la liquidación de haberes, nulable | Finiquito que lo absorbió |
| `VNPGFCHR`, `VNPGUSRR` | | Registro |
| `VNPGMTAN`, `VNPGFCAN`, `VNPGUSAN` | | Anulación: motivo, fecha, usuario |

**Un solo registro VIVO por (empleado, período)**: índice único funcional sobre los estados
`REGISTRADO` y `RETENIDO`, como `UQ_ODBS_VIVA`. ⛔ **Y la consulta JPQL que lo replica se arma por
rama (`is null` / `= :param`), NO con `nvl`**: `nvl` no es JPQL y este WildFly lo rechaza con
`StrictJpaComplianceViolation` — reventó hoy mismo en el primer clic del ciclo de décimos
(`OrdenBeneficioSocialDaoServiceImpl`, corregido en `f1347d5`).

## 5. Estados — rubro nuevo `RHH_ESTADO_VALOR_NO_PAGADO`

`PRBR` **311**, `PDTR` **1504–1508**, del bloque 310-329 / 1500-1599 de este equipo. Por la §6 del
registro, `PRBRALTR = PRBRCDGO = 311`. El `MAX` se revalida con el usuario **justo antes** de
ejecutar (regla 2), en el BLOQUE 0 del DDL.

| Alterno | Estado | Cuándo |
|---|---|---|
| 1 | `REGISTRADO` | Al crear. El rol de P todavía no se procesó |
| 2 | `RETENIDO` | Al generar la orden de pago de P: el empleado cobró N − X |
| 3 | `PAGADO` | Al confirmar el pago de la orden de P+1: cobró M + X, saldo liquidado |
| 4 | `ANULADO` | Con motivo. **Sólo desde REGISTRADO** |
| 5 | `FINIQUITADO` | Lo absorbió la liquidación de haberes |

## 6. Los dos conceptos y sus roles de motor

Igual que los décimos: dos conceptos **INFORMATIVOS** (`CPNMTPCN = 5`) por empresa, y dos códigos
nuevos en `RhhRolConceptoMotor`:

| Rol motor | Constante | Concepto | Signo en el rol |
|---|---|---|---|
| **34** | `VALOR_NO_PAGADO_RETENIDO` | «Valor no pagado» | **−X** en P |
| **35** | `VALOR_NO_PAGADO_RECUPERADO` | «Valor no pagado del mes anterior» | **+X** en P+1 |

Detalles 34 y 35 del rubro 221 (`RHH ROL DEL CONCEPTO EN EL MOTOR`), y **un concepto por empresa
que corre nómina**: `FROM (SELECT DISTINCT PJRQCDGO FROM RHH.CPNM)`, **nunca `FROM SCP.PJRQ`** —
esa tabla son 786 personas jurídicas, no las empresas, y el error casi crea 1.572 conceptos el
2026-09-07 (`e2-18c`, BLOQUE 2 anulado).

⛔ `CPNMTPCN = 5` es **lo que impide que X entre al neto**. Si alguno de los dos conceptos quedara
con otro tipo, X se descontaría **dos veces** (una en el rol y otra en la orden). El control
posterior del script lo verifica fila por fila.

## 7. Dónde se engancha — cuatro puntos, y el orden importa

### 7.1 El motor (`ProcesoNominaServiceImpl`) — al procesar la nómina de E en P

1. Busca `VNPG(E, P)` en `REGISTRADO` o `RETENIDO` → renglón INFORMATIVO **−X**, rol motor 34.
2. Busca `VNPG(E, P−1)` en `RETENIDO` → renglón INFORMATIVO **+X**, rol motor 35.
3. 🔴 **Validación dura**: si `X > neto calculado` → `IncomeException` con los dos importes y el
   empleado. *«Jamás superar el neto»*. Se valida acá y no al registrar porque al registrar el neto
   todavía no existe (decisión 3: el registro es antes del rol).
4. Idempotente: reprocesar el rol regenera los renglones sin duplicarlos, igual que los demás.

### 7.2 La orden de pago (`GeneracionOrdenPagoServiceImpl:196`) — al generar

```java
Double neto = nomina.getNetoPagar();          // hoy
// pasa a:
Double neto = nomina.getNetoPagar()
            - X de VNPG(E, P)   en REGISTRADO o RETENIDO
            + X de VNPG(E, P-1) en RETENIDO;
```

- Marca `VNPG(E, P)` como **`RETENIDO`** con `VNPGORRT = orden`.
- Deja `VNPG(E, P−1)` en `RETENIDO` **todavía**: pasa a `PAGADO` recién al **confirmar el pago**
  (§7.3). *«Sólo sumarse el momento del pago»*.
- 🔴 Si encuentra `VNPG(E, P−1)` en **`REGISTRADO`** (nunca se retuvo, porque la orden de P no
  se generó), **no adivina**: `IncomeException` nombrando el registro. Es una inconsistencia que
  el usuario tiene que resolver anulándolo, no algo que el sistema pueda decidir solo.
- Regenerar la orden de P (si existe ese camino) tiene que ser idempotente sobre estos estados.

### 7.3 La confirmación del pago (`ContabilizacionNominaServiceImpl.contabilizarPago`)

Cuando la orden de P+1 se acredita: `VNPG(E, P−1)` en `RETENIDO` con `VNPGORPG = orden` →
**`PAGADO`**, `VNPGPRRC = P+1`. **La contabilidad ya salió sola** con `orden.getTotal()`.

### 7.4 El reverso de la orden

**Verificar qué reverso existe hoy para `OrdenPagoNomina`** (anulación, reverso de acreditación).
Donde exista, devolver el estado: `RETENIDO → REGISTRADO` si se revierte la orden de P;
`PAGADO → RETENIDO` si se revierte la de P+1. **Si no existe ningún reverso, decirlo y no
inventarlo** — es otro frente.

### 7.5 El finiquito (`LiquidacionHaberesServiceImpl:342`)

Todo `VNPG` del empleado en `RETENIDO` se suma a **`FINIQUITO_REMUNERACION_PENDIENTE`** (o como
línea propia si la liquidación las distingue — decidirlo leyendo cómo `agrega()` arma los rubros)
y pasa a **`FINIQUITADO`** con `VNPGLQDC`. Decisión 2 del usuario.

## 8. Reglas de registro y anulación

- **Registrar**: empleado activo de la empresa, período **ABIERTO o CALCULADO** (estados 1 ó 3 de
  `RhhEstadoPeriodoNomina`; corrección del usuario del 2026-09-08, ver decisión 3), valor > 0,
  motivo obligatorio, y **ningún otro registro vivo** para ese (empleado, período).
  `EN_CALCULO` se rechaza (registrar en medio de un cálculo es una carrera); desde `APROBADO` en
  adelante se rechaza con mensaje que nombre el estado. **Registrar en un período CALCULADO
  obliga a recalcular el rol**: la respuesta lo advierte, y la orden de pago **se niega a
  generarse** si encuentra un VNPG vivo cuya nómina no tiene el renglón de rol 34 (o 35 para el
  del mes anterior) — sin eso pagaría `N − X` mientras el rol impreso dice `N`, y la validación
  `X ≤ neto` del motor nunca habría corrido.
- Validación blanda al registrar: si `X > salario base del contrato`, **avisar** (no bloquear —
  el neto real puede ser mayor por horas extra). La dura es la del motor (§7.1.3).
- **Anular**: con motivo, **sólo desde `REGISTRADO`**. Un `RETENIDO` ya afectó una orden de pago:
  ahí la vía es revertir la orden (§7.4), no borrar el registro.
- **Nunca se borra**: `ANULADO` es estado. Hoy se encontraron cuatro pantallas de `cxp` que
  «anulan» con `DELETE` físico y una de `tsr` que podía dejar cheques huérfanos por lo mismo.

## 9. La pantalla — RRHH → Procesos → «Valores no pagados»

Con el estándar que se aplicó hoy a todo el inventario de consulta:

- **Registro**: empleado (**buscable tecleando**, el `InlineAutocomplete` de `rrh` con `valorPor`),
  período (sólo los abiertos), valor, motivo.
- **Consulta**: filtros por período, empleado, estado — **al servidor**, nunca traer todo y filtrar
  en el navegador. Default: el período abierto actual.
- **Exportación a CSV**: fechas `yyyy-MM-dd`, números crudos, nombre de archivo con qué y cuándo.
- **Anular** con motivo, sólo en `REGISTRADO`, con el mensaje real del backend si rechaza.
- Cada fila muestra su trazabilidad: en qué orden se retuvo, en qué orden se devolvió, o qué
  finiquito lo absorbió.

## 10. Contrato REST — `@Path("vnpg")`

Los seis estándar (`getAll`, `getId`, `POST`, `PUT`, `DELETE` **deshabilitado** — devuelve 405 con
mensaje, `selectByCriteria`) más:

| Endpoint | Qué |
|---|---|
| `GET /vnpg/listar?idEmpresa&idPeriodo&idEmpleado&estado` | Listado con filtros de servidor. `estado` repetible |
| `POST /vnpg/anular/{id}` | Body `{motivo, idUsuario}`. Sólo desde `REGISTRADO` |

Errores: `IncomeException` con mensaje de negocio; llegan al front como `{"mensaje": "..."}`.

## 11. Orden de despliegue y riesgos

**DDL (`e2-26`) → WAR → FE.** El DDL va primero porque el WAR mapea la tabla (regla §7 del
registro: no mergear un mapeo cuya columna no está en la base).

| # | Riesgo | Mitigación |
|---|---|---|
| 1 | 🔴 Concepto con tipo ≠ 5 → X se descuenta dos veces | Control fila por fila en el script, y el motor rechaza un concepto de rol 34/35 que no sea INFORMATIVO |
| 2 | 🔴 `X > neto` → pago negativo sin error | Validación dura en el motor (§7.1.3) |
| 3 | Orden de P+1 sin que P haya retenido | Excepción nombrando el registro (§7.2) |
| 4 | Reverso de orden que no devuelve el estado | §7.4: verificar qué reverso existe antes de asumir |
| 5 | `nvl`/`coalesce` en el JPQL del índice funcional | Rama `is null` / `= :param` (§4) |
| 6 | Este ciclo, como el de décimos, nace sin ejecutarse | Guion de prueba en dos períodos, con los importes esperados escritos ANTES de correrlo (§12) |

## 12. Cómo se prueba — con los números escritos antes

Empleado con salario 1.000,00, sin otras novedades, IESS 9,45 %. Registrar X = 200,00 en P.

| | P | P+1 |
|---|---|---|
| Neto de nómina | 905,50 | 905,50 |
| Renglón VNPG en el rol | **−200,00** | **+200,00** |
| Orden de pago | **705,50** | **1.105,50** |
| Asiento del rol, remuneraciones por pagar (haber) | 905,50 | 905,50 |
| Asiento del pago, remuneraciones por pagar (debe) | 705,50 | 1.105,50 |
| Saldo acumulado en remuneraciones por pagar | **200,00** | **0,00** |
| Estado del registro | `RETENIDO` | `PAGADO` |

Si algún número no da, no se despliega. La lección de los tres defectos de dinero del 2026-09-07
es que la única verificación que vale es contrastar la salida contra un valor conocido.
