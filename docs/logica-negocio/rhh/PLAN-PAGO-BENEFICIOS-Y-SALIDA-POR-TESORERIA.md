# Pago de beneficios sociales y salida de dinero de RRHH por tesorería

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-01 · **Árbitro:** `omen-saa-2-arb`
**Estado:** diseño congelado, pendiente de implementación.

> **Todo lo que sigue está verificado contra el código, archivo:línea.** Donde algo no se verificó,
> lo dice explícitamente. No copiar afirmaciones de este documento a otro sin releer la fuente:
> es la lección que `ESTADO-CXP-CXC-TSR-RHH-SRI.md` §9.8 dejó escrita y que motivó este encabezado.

---

## 0. De dónde sale esto

Cuatro pedidos del usuario, 2026-09-01:

1. Debe existir la opción de **pagar los décimos acumulados**.
2. **Toda salida de dinero del sistema sale por TSR**, y RRHH no es la excepción; la contabilidad
   debe seguir alimentándose correctamente aunque el proceso se reparta entre RRHH y TSR.
3. Al sacar el pago por TSR debe **darse de baja la provisión** correspondiente, sea de décimos,
   vacaciones o cualquier otro concepto que provisione.
4. **Validar si el Ministerio de Trabajo exige un reporte** al pagar los décimos y, de existir,
   generarlo.

**Contexto que cambia el riesgo:** según el usuario, **este es el primer mes que la nómina se
procesa desde el sistema**. No hay historia que preservar, pero sí un proceso en estreno que no
conviene desestabilizar. Ese criterio decidió el frente 2 (ver §2).

---

## 1. Diagnóstico — qué existe hoy

### 1.1 El décimo tiene dos caminos y sólo uno llega al final

| Modalidad | Cómo se paga | Estado |
|---|---|---|
| **MENSUALIZADO** | Renglón del rol (`ProcesoNominaServiceImpl:970-1002`, pasos 9 y 10) → viaja dentro del neto → orden de pago `RHH.RDPG` → archivo bancario → asiento + egreso | ✅ **cerrado** |
| **ACUMULADO** | Provisión mensual → `POST /lqbs/generarDecimoTercero/{anio}` crea `RHH.LQBS` → **nada más** | ❌ **se corta** |

**El corte, verificado:**

- `BeneficioSocialServiceImpl:319-320` — el registro nace `valorPagado = 0`, `estado = 1`. **Nadie
  escribe nunca otro valor.** Los únicos `setValorPagado`/`setFechaPago`/`setEstado` de todo el
  proyecto son esos dos y los setters de la entidad. Las columnas `LQBSVLPG`, `LQBSFCPG` y
  `LQBSESTD` existen: el diseño previó el pago y no se implementó.
- `GeneracionOrdenPagoServiceImpl` menciona `LiquidacionBeneficioSocial` **cero veces**. La orden de
  pago se arma sólo desde `Nomina` del período (`:148`); el décimo acumulado vive en `RHH.LQBS`, no
  en `RHH.NMNA`. Nunca entra.
- `OrigenPagoExterno` no tiene un origen para beneficios sociales — sólo `RHH_ANTICIPO_EMPLEADO`.
- **El frontend no lo conoce:** cero apariciones de `lqbs`, `generarDecimo` o `BeneficioSocial` en
  todo `saaFE/src`. Los tres endpoints `generar*` no tienen consumidor; sólo se alcanzan por Postman.

**Consecuencia contable, que es el daño real:** con modalidad ACUMULADO la provisión se acredita
cada mes y **nada la reversa jamás**. `PROVISION_DECIMO_*_POR_PAGAR` crece indefinidamente contra un
pago que en el sistema no ocurre. No se ve como error: se ve como un saldo que no cuadra.

### 1.2 La nómina ya toca tesorería — pero no la bandeja

Corrige una lectura apresurada de este mismo árbitro. La nómina **sí** genera un egreso:

- `GeneracionOrdenPagoServiceImpl:778` → `orden.setEgreso(egreso.getId())`, o sea `TSR.EGRS`.
- `rhh/sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql` ya creó el producto `PGS.PRDP` código `NOMINA`
  exactamente para poder enlazar ese egreso.
- `ContabilizacionNominaServiceImpl:340-355` (`contabilizarPago`) arma el asiento con la plantilla
  `CFNMPLPG` (líneas 50-59 del rubro 214) y **ya lo atribuye a `ModuloSistema.TESORERIA`**.

**Lo que falta es la bandeja de aprobación (`PGS.PGTR`).** La nómina se paga sin pasar por ella.
Así que el frente 2 no es "conectar la contabilidad" — es "insertar el paso de aprobación".

### 1.3 La provisión sólo tiene alta, nunca baja

`ContabilizacionNominaServiceImpl.contabilizarProvisiones` (`:218-250`) genera **únicamente** el
asiento de alta (DEBE `GASTO_PROVISION_*` 30-35 / HABER `PROVISION_*_POR_PAGAR` 40-45). **No existe
ninguna baja de provisión en el sistema.** El punto 3 es código nuevo, no un ajuste.

`RHH.PVNM` (`ProvisionNomina`) tiene columna de estado `PVNMESTD`, pero **no existe un rubro de
estados de provisión** — sólo `RhhTipoProvision` (los 7 tipos). Qué valores usa hoy esa columna es
un punto a verificar antes de escribir sobre ella (ver §6, V3).

### 1.4 El mecanismo de tesorería que hace todo esto viable

`PagoProgramadoService.registrarPagoDeOrigenExterno(...)` (`:191-195`) acepta un **`desglose`** de
`LineaContablePago`. Cada línea imputa parte del pago a un producto de `PGS.PRDP` y produce una
línea DEBE con la cuenta contable del grupo de ese producto. El Javadoc del DTO lo dice textual:
*«CXP no sabe qué representa cada producto: el mapeo concepto → producto lo hace el módulo que
origina el pago.»*

**Y el comportamiento sin desglose está explícitamente soportado:**
`contabilizarSegunOrigen` (`PagoProgramadoServiceImpl:1993-1999`) → con `origenExterno != null`
llama a `contabilizarPagoOrigenExterno`, que **devuelve `null` si el pago no tiene desglose**.
Comentario en `:896`: *«Un pago SIN desglose es válido: al confirmarse no genera asiento ni
movimiento bancario.»*

Eso es exactamente lo que necesita la decisión del §2.

---

## 2. Decisiones del usuario — 2026-09-01

Las tres se tomaron con el análisis de §1 a la vista.

| # | Decisión | Consecuencia |
|---|---|---|
| **D1** | **RRHH sigue contabilizando; el pago viaja a la bandeja SIN desglose.** La bandeja actúa como control y aprobación, no como generador de asientos | Un solo asiento por pago, el de RRHH. No se toca `ContabilizacionNominaServiceImpl.contabilizarPago` ni la plantilla `CFNMPLPG`, que se está estrenando este mes |
| **D2** | El décimo acumulado se paga **consolidado**: un pago por el total, con el detalle por empleado colgando | Hace falta una **cabecera** que agrupe las `LQBS`, igual que `RHH.RDPG` agrupa la nómina. Es la tabla nueva del §3.1 |
| **D3** | Se dan de baja **todas** las provisiones: décimos, fondos de reserva, vacaciones, jubilación patronal y desahucio | Se parte en dos sub-frentes con disparadores distintos — ver §4 |

### 2.1 Dos consecuencias de D1 que hay que aceptar a conciencia

1. **Sin desglose tampoco hay movimiento bancario.** El mismo comentario (`:896`, y el aviso de
   `:1040`) dice que un pago sin desglose no genera asiento **ni movimiento bancario**. O sea: estos
   pagos no crearán `MovimientoBanco`. *Impacto acotado:* el frente J ya estableció que
   `MovimientoBanco` cubre sólo el 1-5% del movimiento real y que `validaDisponibilidad` **no** lo
   usa (usa saldo contable). Aun así, quien concilie por `MovimientoBanco` no verá estos pagos.
2. **La coherencia se sostiene en la disciplina, no en el código.** Nada impide que mañana alguien
   mande un desglose para estos orígenes y produzca el segundo asiento. Por eso va escrito acá y en
   el Javadoc que el agente debe dejar en el servicio.

**Extensión declarada por el árbitro:** D1 se decidió hablando de la nómina. Se aplica **también**
al pago de beneficios sociales del frente 1, por coherencia: un solo criterio de contabilización
para toda salida de RRHH. Si el usuario quisiera lo contrario, es un cambio de este párrafo, no del
diseño entero.

---

## 3. Frente 1 — Pago de décimos acumulados

### 3.1 Modelo de datos

**Tabla nueva `RHH.ODBS` — orden de pago de beneficio social (cabecera consolidada).**

⚠️ **`ODBS` está libre en `src/main/java/com/saa/model/` y en todo `docs/` (verificado 2026-09-01).
FALTA confirmarlo contra `ALL_TABLES` antes de crear** — el código de 4 letras es único en todo el
proyecto, no por esquema (`REGISTRO-RESERVAS-EQUIPOS.md` §3). La sonda está en el script de §6.

| Columna | Tipo | Contenido |
|---|---|---|
| `ODBSCDGO` | `NUMBER` PK | Secuencia `RHH.SQ_ODBSCDGO` |
| `PJRQCDGO` | `NUMBER` FK | Empresa |
| `ODBSTPBN` | `NUMBER` | Tipo de beneficio — rubro `RHH_TIPO_BENEFICIO_SOCIAL` (1 décimo tercero, 2 décimo cuarto, 3 fondos de reserva) |
| `ODBSANOO` | `NUMBER` | Año del beneficio |
| `ODBSRGON` | `NUMBER` null | Región — sólo décimo cuarto (`RhhRegionDecimoCuarto`) |
| `ODBSNMRO` | `VARCHAR2(50)` | Número de la orden |
| `ODBSFCEM` | `DATE` | Fecha de emisión |
| `ODBSFCPG` | `DATE` null | Fecha de acreditación, se escribe al confirmar |
| `ODBSTTAL` | `NUMBER(18,2)` | Total consolidado |
| `ODBSNMEM` | `NUMBER` | Cantidad de empleados incluidos |
| `PGTRCDGO` | `NUMBER` FK null | Pago programado en tesorería, se escribe al enviar |
| `ASNTCDGO` | `NUMBER` null | Asiento de pago, lo escribe RRHH al confirmar |
| `ODBSESTD` | `NUMBER` | Estado — rubro nuevo, ver abajo |
| `ODBSOBSR` | `VARCHAR2(500)` null | Observaciones |
| `ODBSFCHR` / `ODBSUSRR` | `TIMESTAMP` / `VARCHAR2(60)` | Auditoría |

**Columna nueva en `RHH.LQBS`:** `LQBSODBS NUMBER NULL`, FK a `ODBS`. Enlaza cada liquidación con
la orden que la pagó. Nullable: las liquidaciones existentes no tienen orden.

**Rubro nuevo `RHH_ESTADO_ORDEN_BENEFICIO`:** `GENERADA(1)`, `ENVIADA_A_TESORERIA(2)`,
`PAGADA(3)`, `ANULADA(4)`.

**Origen de pago externo nuevo:** `RHH_BENEFICIO_SOCIAL` en `com.saa.rubros.OrigenPagoExterno`.
**Uno solo, no tres** — el tipo concreto ya viaja en `ODBSTPBN`, y la bandeja gana un filtro
legible («Beneficios sociales RRHH») en vez de tres casi iguales.

### 3.2 Ciclo completo

```
1. POST /lqbs/generarDecimoTercero/{anio}        (YA EXISTE — no se toca)
      └─> crea N filas RHH.LQBS, estado=1, valorPagado=0

2. POST /odbs/generar                            (NUEVO)
      └─> agrupa las LQBS sueltas de (empresa, tipo, año[, región])
          crea la cabecera ODBS estado=GENERADA, escribe LQBSODBS en cada una

3. POST /odbs/enviarATesoreria/{id}              (NUEVO)
      └─> registrarPagoDeOrigenExterno(RHH_BENEFICIO_SOCIAL, idOrden, ...,
                                       desglose = null,  <-- D1
                                       idCuentaBancariaOrigen = null)
          el pago nace POR_APROBAR  ->  aparece en la bandeja
          ODBS.estado = ENVIADA_A_TESORERIA, ODBS.PGTRCDGO = idPago

4. POST /pgtr/aprobar                            (YA EXISTE — no se toca)
      └─> tesorería asigna cuenta y forma de pago

5. POST /odbs/confirmarPago/{id}                 (NUEVO)
      └─> exige que el PagoProgramado esté CONFIRMADO
          por cada LQBS: valorPagado = valor, fechaPago, estado = PAGADO
          genera el asiento de baja de provisión (ver §4.1)
          ODBS.estado = PAGADA, ODBSFCPG, ASNTCDGO
```

**Por qué el paso 5 es un endpoint de RRHH y no un gancho en tesorería:** D1 dice que RRHH
contabiliza. Enganchar en `contabilizarSegunOrigen` obligaría a que CXP conozca conceptos de RRHH,
que es justo lo que el Javadoc de `LineaContablePago` declara que no debe pasar. El precio es que
el paso 5 hay que dispararlo; el patrón de `AnticipoEmpleado` (guardar el `PagoProgramado` y leer
su estado) es el precedente de la casa.

### 3.3 Regla de negocio que hay que verificar antes de codificar

⚠️ **`generarDecimoTercero`/`generarDecimoCuarto` ¿filtran por modalidad ACUMULADO?** No se
verificó. Si generan `LQBS` también para contratos MENSUALIZADOS, pagar esa orden **pagaría dos
veces** el mismo décimo: una dentro del rol y otra por esta vía. **Es el defecto más caro que este
frente puede introducir.** El agente de backend debe verificarlo y, si no filtra, **detenerse y
reportar** — la corrección cambia el alcance.

Base legal, y es la razón de que ACUMULADO exista: el empleador paga mensualizado **salvo para los
trabajadores que soliciten por escrito la acumulación** (Código del Trabajo; ver §5). O sea que las
dos modalidades conviven en la misma empresa, y el filtro no es opcional.

---

## 4. Frentes 2 y 3 — salida por tesorería y baja de provisiones

### 4.1 Baja de provisión — dos disparadores distintos

D3 pidió las cinco. No son el mismo problema:

| Provisión | Línea (rubro 214) | Disparador | Frente |
|---|---|---|---|
| Décimo tercero | 40 | Pago de la orden `ODBS` | **3-A** |
| Décimo cuarto | 41 | Pago de la orden `ODBS` | **3-A** |
| Fondos de reserva | 43 | Pago de la orden `ODBS` | **3-A** |
| Vacaciones | 42 | Consumo de vacaciones | **3-B** ⚠️ |
| Jubilación patronal | 44 | Liquidación / finiquito | **3-C** |
| Desahucio | 45 | Liquidación / finiquito | **3-C** |

**Asiento de baja (3-A), al confirmar el pago:**

```
DEBE   PROVISION_DECIMO_TERCERO_POR_PAGAR   (línea 40)     total de la orden
HABER  BANCO                                (línea 51)     total de la orden
```

⚠️ **3-B (vacaciones) tiene riesgo de doble descuento.** Vacaciones ya tiene su propio ciclo de
acreditación y consumo, cerrado en el frente O (`AcreditacionVacacionesServiceImpl`, que además ya
escribe `ProvisionNomina`). **Antes de tocar nada acá hay que levantar cómo se descuenta hoy.** No
se diseña en este documento: se levanta primero. Es trabajo aparte.

### 4.1bis 🔴 Frente 3-C — jubilación patronal y desahucio: doble reconocimiento del gasto

**Levantado y confirmado el 2026-09-01. El usuario decidió corregirlo ya, junto con 3-A.**

`ContabilizacionNominaServiceImpl.lineaDeRubroFiniquito` (`:496-507`) resuelve cinco rubros de
finiquito consecutivos **con dos criterios distintos**:

| Rubro | Línea que devuelve | Efecto sobre el pasivo |
|---|---|---|
| `FINIQUITO_DECIMO_TERCERO` | 40 `PROVISION_DECIMO_TERCERO_POR_PAGAR` | ✅ lo descarga |
| `FINIQUITO_DECIMO_CUARTO` | 41 `PROVISION_DECIMO_CUARTO_POR_PAGAR` | ✅ lo descarga |
| `FINIQUITO_VACACIONES` | 42 `PROVISION_VACACIONES_POR_PAGAR` | ✅ lo descarga |
| **`FINIQUITO_DESAHUCIO`** | **60 `GASTO_DESAHUCIO`** | ❌ el pasivo 45 nunca baja |
| **`FINIQUITO_JUBILACION_PATRONAL`** | **62 `GASTO_JUBILACION_PATRONAL`** | ❌ el pasivo 44 nunca baja |

Y `ProvisionActuarialServiceImpl.cargarProvisionActuarial` (`:60-95`) **sí** escribe esas dos
provisiones en `RHH.PVNM` cada vez que se carga el estudio actuarial, contra las cuentas 44 y 45
(`importesDeProvisiones:757-805`). Nada las consume en ningún punto del código.

**Consecuencia: el gasto se reconoce dos veces** — mensualmente como provisión (líneas 34/35) y otra
vez completo al liquidar (60/62) — y el pasivo crece sin techo.

#### El tratamiento: descargar hasta el saldo, el exceso a gasto

⛔ **No basta con cambiar el mapeo a 44/45.** Si un empleado no tiene provisión acumulada —porque
nunca se cargó el estudio actuarial para él, o entró después de la última carga— debitar la cuenta
de provisión dejaría el pasivo **en negativo** y el gasto sin reconocer. Sería cambiar un defecto
por otro.

**Regla, para los dos rubros:**

```
saldoProvision = SUM(PVNMVLOR) de RHH.PVNM  para (empleado, tipoProvision)
                 tipoProvision = 6 JUBILACION_PATRONAL | 7 DESAHUCIO

parteProvision = min(saldoProvision, valorDelRubroEnElFiniquito)
parteGasto     = valorDelRubroEnElFiniquito - parteProvision

DEBE  línea 44 / 45   por parteProvision   (si > 0)
DEBE  línea 62 / 60   por parteGasto       (si > 0)
```

**Por qué este tratamiento y no otro:** con `saldoProvision = 0` degrada **exactamente** al
comportamiento de hoy (todo a gasto). Eso lo vuelve seguro de desplegar **aunque no se haya medido
antes** si el estudio actuarial está cargado o no — que era la única objeción del árbitro a
corregirlo sin medir. El caso no medido es el caso que no cambia.

**Se suma sin restar consumos** porque hoy nada consume esas provisiones: la suma de `PVNMVLOR` es
el saldo. ⚠️ **El día que algo las consuma, esta fórmula deja de valer** y hay que restar lo ya
descargado. Queda anotado acá porque no se deduce leyendo la fórmula.

**Fuera de alcance de 3-C:** `FINIQUITO_DESPIDO_INTEMPESTIVO` → 61 `GASTO_DESPIDO_INTEMPESTIVO`
**se queda como está**. El despido intempestivo no se provisiona (no hay línea de provisión para él
en el rubro 214, ni tipo en `RhhTipoProvision`): es un gasto que nace el día del despido. Está bien.

---

**3-B (vacaciones) sigue siendo sólo levantamiento.** A diferencia de 3-C, ahí el finiquito **ya**
descarga la provisión (línea 42), así que el problema no es un mapeo faltante sino que **nada la
descarga al consumir vacaciones** (`AcreditacionVacacionesServiceImpl.consumir():268-301` sólo mueve
días en `SaldoVacaciones`, sin efecto contable). Agregar una baja por consumo **sin** enseñarle al
finiquito qué ya se descargó produce doble descuento: no hay ninguna marca que distinga los días ya
reversados de los pendientes. **No se implementa hasta diseñar esa marca.**

*Dato relacionado, encontrado en el mismo levantamiento:* `SaldoVacaciones.diasPagados` existe como
columna real y **nadie la escribe nunca** con valor > 0 — se inicializa en 0
(`AcreditacionVacacionesServiceImpl:128`, `MigracionRhhServiceImpl:456`) y ahí muere. Es el mismo
patrón que `LQBSVLPG`: una columna que aparenta un ciclo cerrado que no existe. No apoyarse en ella.

### 4.2 Frente 2 — la nómina pasa por la bandeja

Cambio acotado, gracias a §1.2:

```
GeneracionOrdenPagoServiceImpl.generar(idPeriodo, idCuentaBancaria, usuario)
   └─> hoy: crea la orden RDPG y el egreso, sin pasar por la bandeja
       nuevo: registrarPagoDeOrigenExterno(RHH_NOMINA, idOrden, ..., desglose = null)
              con idCuentaBancariaOrigen = null  ->  nace POR_APROBAR
```

- **Origen nuevo:** `RHH_NOMINA` en `OrigenPagoExterno`.
- ⛔ **SIN columna nueva en `RHH.RDPG`. Corregido el 2026-09-01 — la versión anterior de este
  documento pedía un `RDPGPGTR`.** El vínculo se resuelve al revés: `PGS.PGTR` ya guarda
  `PGTRORGN` (origen) y `PGTRIDOR` (id del documento de origen), así que el pago de una orden se
  encuentra consultando `origenExterno = 'RHH_NOMINA' AND idOrigen = idOrdenPago`. **No hace falta
  tocar `RHH.RDPG`.**

  **Por qué importa, y por qué se aparta del patrón de `AnticipoEmpleado`** (que sí guarda la FK):
  agregar una columna a `RHH.RDPG` obliga a mapearla en `OrdenPagoNomina`, y por la **regla 9** una
  columna `@Column` que no existe en la base **rompe toda lectura de la entidad con ORA-00904**.
  Eso acopla el despliegue: el `.sql` tendría que correr antes del WAR, y si no corre, la pantalla
  de órdenes de pago de nómina deja de funcionar — **en el primer mes que la nómina se procesa
  desde el sistema**. Es exactamente el defecto `CBR.ANTC` del 2026-08-29, que ya costó una vez.
  Consultar por `origen + idOrigen` no necesita DDL, así que **el frente 2 no tiene dependencia de
  base de datos y se puede desplegar solo**.

  *El frente 1 sí conserva su `ODBS.PGTRCDGO`, y ahí es correcto: `ODBS` es una tabla nueva, se
  crea entera con esa columna incluida y no hay nada existente que romper.*
- **`contabilizarPago` NO se toca** (D1). Sigue armando el asiento con `CFNMPLPG`.
- **`confirmar(idOrdenPago, fechaAcreditacion, usuario)`** pasa a exigir que el pago esté
  `CONFIRMADO` en tesorería antes de contabilizar. Ese es el punto donde la aprobación se vuelve
  obligatoria de verdad; sin eso la bandeja es decorativa.

⚠️ **Trampa conocida, del frente S §8.2:** `POST /pgtr/aprobar` aplica **una sola forma de pago a
todo el lote** (`PagoProgramadoServiceImpl:1170`) sin mirar el origen, y tiene una lista de orígenes
que rechazan transferencia. La nómina **sí** se paga por transferencia (archivo bancario), así que
**no** debe entrar en esa lista. Verificar que no quede atrapada en la restricción de
`:1162-1179`, escrita para caja chica y anticipo de empleado.

---

## 5. Frente 4 — reporte del Ministerio de Trabajo

### 5.1 Sí es obligatorio — confirmado

El empleador debe registrar el pago de la decimotercera y decimocuarta remuneración en el
**SUT / Sistema de Salarios en Línea** (`salarios.trabajo.gob.ec`): se activa región, período y
formulario, **se sube un archivo CSV** como anexo, y el formulario se imprime, se firma
(representante legal y trabajadores) y se envía al MDT.

- **Plazo escalonado por el noveno dígito del RUC.** Dígitos 1-5: del 5 de enero al 5 de febrero.
  Dígitos 6-9 y 0: del 6 de febrero al 6 de marzo.
- **Sanción por incumplimiento:** multa administrativa de hasta **20 SBU** (Art. 628 del Código del
  Trabajo), más procesos sancionatorios por denuncia o inspección.
- **Regla que valida el frente 1:** el pago es mensualizado **salvo para quienes soliciten por
  escrito la acumulación**. Las dos modalidades conviven; ver §3.3.

### 5.2 🔴 Lo que falta y no se puede sacar de internet

**La estructura exacta de columnas del CSV no está publicada.** Se obtiene descargando el archivo
de ejemplo desde el propio SUT, que exige login del empleador. Se intentó el manual oficial de
empleadores (`kva.com.ec/.../manual_de_usuario__Empleadores____Sistema_de_Salarios.pdf`) y el PDF
no resultó extraíble.

**Bloqueante para implementar:** el usuario debe descargar del SUT el **CSV de ejemplo** del
formulario de decimotercera (y el de decimocuarta, si difiere) y dejarlo en
`docs/logica-negocio/rhh/muestras/`. Sin ese archivo, cualquier generador que se escriba es una
adivinanza que va a fallar en la carga, y el error se descubre contra el plazo legal.

**Lo que sí se puede adelantar sin él:** la consulta que reúne los datos —empleado, identificación,
período, valor pagado, fecha de pago, cuenta— es la misma con cualquier formato de salida. Esa
parte no está bloqueada.

---

## 6. Verificaciones previas — antes de escribir código

Van en `docs/logica-negocio/rhh/sql/e2-01-verificacion-previa-beneficios.sql`, todas de sólo
lectura. **Las corre el usuario.**

| # | Qué | Por qué |
|---|---|---|
| **V1** | `ODBS` libre en `ALL_TABLES` | El código de 4 letras es único en todo el proyecto |
| **V2** | Distribución de `LQBS` por `LQBSESTD` y `LQBSTPBN`, y si hay filas con `LQBSVLPG > 0` | Confirma que ninguna liquidación figura pagada, y si hay datos de la corrida de este mes |
| **V3** | Valores distintos de `PVNMESTD` en `RHH.PVNM` | La columna existe sin rubro de estados; hay que saber qué guarda antes de escribirla |
| **V4** | `MAX(PRBRCDGO)` y `MAX(PDTRCDGO)` | Regla 2 del registro de reservas, antes de crear el rubro de §3.1 |
| **V5** | Contratos por modalidad de décimo tercero y cuarto | Cuántos ACUMULADO hay realmente — dimensiona el frente 1 |
| **V6** | `LQBSVLPG`, `LQBSFCPG`, `LQBSESTD` existen en `RHH.LQBS` | Regla 9 del árbitro: una columna `@Column` que no existe rompe toda lectura de la entidad con ORA-00904 |

---

## 7. Orden de ejecución

| Paso | Qué | Depende de |
|---|---|---|
| 1 | Correr las verificaciones de §6 | — |
| 2 | Conseguir el CSV de ejemplo del SUT (§5.2) | usuario |
| 3 | DDL: `RHH.ODBS`, `LQBSODBS`, rubro de estados | V1, V4 |
| 4 | BE frente 1 (§3) | paso 3 |
| 5 | BE frente 2 (§4.2) | **nada — no tiene DDL**, ver §4.2 |
| 6 | BE frente 3-A (§4.1) | paso 4 |
| 7 | FE: pantalla de órdenes de beneficio social | contrato de API congelado |
| 8 | Levantamiento 3-B y 3-C (§4.1) | — (independiente) |
| 9 | Frente 4 (§5) | paso 2 |

**El SQL va antes del WAR**: los pasos 4-6 mapean columnas y tablas nuevas.

---

## 8. Lo que este documento NO decide

- **3-B (vacaciones):** levantamiento hecho; **no se implementa** hasta diseñar la marca que
  distinga los días ya descargados de la provisión. Ver §4.1bis, último bloque.
- ~~**3-C (jubilación/desahucio):** levantamiento pendiente~~ — **levantado y congelado el
  2026-09-01, §4.1bis.** El usuario decidió corregirlo junto con 3-A.
- **Formato del CSV del MDT:** bloqueado por el archivo de ejemplo, §5.2.
- **Si `generarDecimo*` filtra por modalidad:** a verificar por el agente, §3.3. Cambia el alcance.
- **Numeración de `ODBSNMRO`:** si sigue el patrón de `RDPGNMRO` o usa `CBR.NXPE`. A resolver
  copiando lo que haga la orden de pago de nómina.
