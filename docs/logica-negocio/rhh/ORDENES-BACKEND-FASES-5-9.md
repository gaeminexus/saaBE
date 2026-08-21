# Órdenes al backend — fases 5 a 9

**Fecha:** 2026-08-19 · **Emite:** dueño del modelo de datos · **Ejecuta:** agente backend

> Lee antes `ESTADO-RRHH.md` y `PLAN-IMPLEMENTACION-RRHH-MAESTRO.md`. Este documento no
> reemplaza al `PLAN-IMPLEMENTACION-RRHH-BACKEND.md`: lo ordena, resuelve las decisiones que
> estaban abiertas y fija lo que se puede tocar y lo que no.

## Por qué se levanta el bloqueo

El rol real de ASOPREP de enero de 2026 va a demorar. Esperarlo con el backend detenido no
tiene sentido: la verificación de enero valida el **motor de cálculo**, que es la fase 4 y ya
está entregada. Las fases 5 a 9 no cambian un solo número de ese cálculo — construyen lo que
pasa **después** de calcular: el documento, el asiento, el pago, la asistencia, el finiquito y
las salidas al SRI, al IESS y al MDT.

Se construyen las cinco fases completas. Cuando llegue el rol, se verifica enero contra un
motor que no se movió, y a partir de ahí se pule todo lo demás con datos reales.

---

## Regla que gobierna este paquete: **el motor está congelado**

`ProcesoNominaServiceImpl`, `RetencionRentaServiceImpl`, `BeneficioSocialServiceImpl`,
`AcreditacionVacacionesServiceImpl` y `ProvisionActuarialServiceImpl` **no se modifican**.

Si mientras construyes las fases 5 a 9 encuentras lo que parece un defecto del motor:
**repórtalo con el archivo, la línea y el porqué, y sigue adelante. No lo corrijas.** La
verificación de enero tiene que hacerse contra un motor estable; si el motor se mueve mientras
tanto, no sabremos si un descuadre viene del cálculo o de un cambio nuestro. Yo decido si se
corrige antes o después de la verificación.

**Excepciones autorizadas, y solo estas dos:**

1. Añadir al final de `aprobarPeriodo` la llamada que genera los roles de pago (orden 1). Es una
   adición al flujo, no un cambio en la aritmética.
2. **El reparto de los tres campos de aporte en la cabecera de `NMNA`**, resuelto abajo. Es una
   asignación de solo escritura en el paso 15; no toca ningún renglón, ninguna base ni ninguno de
   los cuatro totales.

### Resolución del 2026-08-19 · el aporte patronal en la cabecera de `NMNA`

El backend reportó, leyendo el motor, que el paso 15 hace
`nomina.setAportePatronal(patronal)` con el **total** de renglones patronales, y que
`aporteIeceSecap` no se asigna en ninguna parte. Verificado: es correcto, y hay un segundo
defecto de la misma familia que el reporte no menciona.

**Los tres campos son distintos y hoy dos están mal:**

| Columna | Debe llevar | Hoy lleva | En el caso de enero |
|---|---|---|---|
| `NMNATTPT` (`totalPatronal`) | Todo el costo patronal | Correcto | 97,20 |
| `NMNAAPPT` (`aportePatronal`) | **Solo el aporte patronal IESS** | El total, 97,20 | **89,20** |
| `NMNAIESC` (`aporteIeceSecap`) | **IECE + SECAP** | Nada: queda en `NULL` | **8,00** |

`NMNAIESC` admite nulo y tiene `DEFAULT 0`, así que **no es una cuarta parada** — el `DEFAULT`
no se aplica igual, porque Hibernate incluye la columna en el `INSERT` con `NULL` explícito.

**El segundo defecto:** `sumaAporte(renglones, prnm, personal)` localiza los aportes por la terna
`baseCalculo = IMPONIBLE_IESS` + `tipoConcepto`, que es exactamente lo que la decisión del
2026-08-19 prohibió — el motor localiza por `CPNMROLM`, nunca por la terna. Hoy no falla porque
en el catálogo del script 08 la terna todavía discrimina, pero deja de hacerlo en cuanto el
cliente agregue un egreso propio sobre la base imponible del IESS. Y el parámetro `prnm` que
recibe no se usa.

**Qué hacer:** sustituir `sumaAporte` por un `sumaPorRol(List<ReglonNomina>, int rolConcepto)`
que sume los renglones cuyo `conceptoNomina.rolMotor` sea el rol pedido, y asignar:

```java
nomina.setAportePersonal(sumaPorRol(renglones, RhhRolConceptoMotor.APORTE_PERSONAL));
nomina.setAportePatronal(sumaPorRol(renglones, RhhRolConceptoMotor.APORTE_PATRONAL));
nomina.setAporteIeceSecap(RedondeoNomina.suma(
        sumaPorRol(renglones, RhhRolConceptoMotor.IECE),
        sumaPorRol(renglones, RhhRolConceptoMotor.SECAP)));
```

`NMNAAPPR` no cambia de valor en el caso de enero —75,60 por las dos vías—, pero pasa a
localizarse como manda la decisión ya tomada.

**Por qué se autoriza antes de verificar enero, y no después:** es la cabecera contra la que se
va a construir el **resumen de aportes** de la fase 5, que es el reporte con el que el cliente
cuadra contra la planilla del IESS —criterio de aceptación 2 del maestro—. Dejarlo mal significa
construir ese reporte sobre un dato equivocado y descubrirlo dos veces. Y no es una decisión de
diseño nueva: es aplicar la que ya estaba tomada.

**El caso de prueba de enero se amplía con estos cuatro valores de cabecera**, que ahora también
hay que reportar: `NMNAAPPR` 75,60 · `NMNAAPPT` 89,20 · `NMNAIESC` 8,00 · `NMNATTPT` 97,20.

---

## Orden 0 · Antes de escribir código de fase

Cuatro cosas, en este orden. Ninguna toma mucho y las cuatro evitan repetir lo que ya pasó.

### 0.1 · Base de datos — **hecho el 2026-08-19**

| Script | Qué cerró |
|---|---|
| `sql/12_DDL_LIMPIEZA_RBRO.sql` | `RBROCDGO` en `RNGL` y `TMLQ`. Sin esto no se podía insertar ni un renglón de nómina ni un detalle de liquidación |
| `sql/13_DDL_FASES_5_9.sql` | `CFNM.CFNMCTMR` (cuenta marcadora), rubro alterno **223** `RHH_TIPO_SALIDA_OFICIAL` y la tabla `RHH.SLOF` |

Los dos están ejecutados. Confírmalo con los bloques 1 y 2 de
`sql/VERIFICACION_12_13_Y_BARRIDO.sql` antes de darlo por hecho.

### 0.2 · El barrido de `NOT NULL` ocultos — hazlo tú, ahora

Es lo que ocultó los tres defectos que costaron tres arranques de la prueba de enero. En este
esquema los `NOT NULL` están declarados como CHECK con nombre de sistema, así que
`all_tab_columns.nullable` dice `Y` y **auditar con esa vista da un falso negativo**.

Las consultas están en **`sql/VERIFICACION_12_13_Y_BARRIDO.sql`**, que solo consulta y no
modifica nada. **Tú no tienes acceso a la base: pídele al usuario que lo ejecute y te pase la
salida completa**, en un solo pedido, no consulta por consulta.

El bloque 3 es el que importa: lista las columnas obligatorias de las diez tablas en las que van
a escribir las fases 5 a 9, más `SLOF`, por las dos vías en que este esquema las declara —CHECK
con nombre de sistema y atributo de columna—. Contrasta **cada columna obligatoria** contra las
que tu código va a llenar. Una columna obligatoria que ninguna entidad JPA mapee es un bloqueo de
inserción: **repórtala antes de escribir el servicio que la usa**, no después de que el
`ORA-02290` aparezca en la prueba.

El bloque 4 lista las FK de esas mismas tablas y su tabla padre. `RBROCDGO` bloqueaba por ser
obligatoria **y** apuntar a un catálogo vacío: si ves una FK hacia una tabla sin filas, es el
mismo patrón.

### 0.3 · El desajuste de tipo que tiene esperando al frontend

El delta 11 convirtió `MRCCTPOO`, `MRCCORGN` y `RSMNFNTE` a `NUMBER`; las entidades
`Marcaciones` y `ResumenNomina` siguen declarándolas `String`. **Pásalas a `Long`** y avisa
cuando esté recompilado: el frontend tiene tres tareas paradas esperando exactamente eso.

### 0.4 · Comprueba que Eclipse publica

Antes de cualquier prueba, compara marcas de tiempo. Se estuvo probando código de cuatro horas
antes y nadie lo notó:

```bash
ls -l /c/wildfly38/standalone/deployments/SaaBE.war/WEB-INF/classes/com/saa/ejb/rhh/serviceImpl/ProcesoNominaServiceImpl.class
ls -l /c/work/saaBE/v1/saaBE/target/classes/com/saa/ejb/rhh/serviceImpl/ProcesoNominaServiceImpl.class
```

Si no coinciden, la prueba no vale. Y averigua de una vez por qué el adaptador no publica.

---

## Orden 1 · Fase 5 — Rol de pago y reportes internos

**Entrega la carga de enero–julio 2026:** el documento que el empleado firma.

`RolPago` ya tiene los campos del script 05 (`RLPGTTIN`, `RLPGTTDS`, `RLPGNETO`, `RLPGHASH`,
`RLPGFCEN`, `RLPGRCBD`). No hay entidad nueva en esta fase.

### 1.1 · `GeneracionRolPagoService`

Servicio nuevo, junto al CRUD `RolPagoService`, con el mismo criterio con que
`ProcesoNominaService` convive con `NominaService`.

```java
@Local
public interface GeneracionRolPagoService {

    /** Genera un RLPG por cada NMNA del periodo. Idempotente: si ya existe
     *  el rol de esa nomina lo actualiza. Solo con el periodo APROBADO. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int generarRoles(Long idPeriodoNomina, String usuario) throws Throwable;

    /** Recalcula el hash de un rol y lo compara con RLPGHASH. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    boolean verificarIntegridad(Long idRolPago) throws Throwable;

    /** Marca los roles como entregados al empleado. Recibe la lista de ids
     *  porque la pantalla opera por seleccion multiple; el servidor pone
     *  recibido='S' y sella fechaEnvio con la fecha del dia si esta en nulo. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int registrarRecepcion(List<Long> idsRolPago, String usuario) throws Throwable;
}
```

> **Firma cambiada el 2026-08-19**: `registrarRecepcion` era `(Long idRolPago, LocalDate
> fechaEnvio, String usuario)` y pasa a recibir `List<Long>`. El frontend ya construyó la
> pantalla con selección múltiple enviando la lista de ids, siguiendo el precedente de
> `/rest/hrex/aprobar`, y es el diseño correcto: la recepción se registra por lotes. La fecha no
> viaja: la pone el servidor.

**Cuándo se genera:** al final de `aprobarPeriodo`, no antes. El rol es el documento que se
entrega, y no debe existir mientras el cálculo todavía se puede recalcular. `generarRoles`
también se expone suelto para regenerar mientras el período no esté `CERRADO`.

**El hash** es SHA-256 sobre el contenido del rol —empleado, período, cada renglón con su
concepto y su valor, y los tres totales— en un orden determinista. Sirve para detectar que un
rol impreso no corresponde a lo que hoy tiene la base. `RLPGNMRO` se arma con período y
empleado, y es único.

### 1.2 · Decisión: `RLPGESTD` se queda como `String`

No se convierte a `Long` ni se crea un rubro de estado del rol. Es el mismo criterio que se
aplicó a `CNTEESTD`: el estado real del rol ya lo llevan `RLPGFCEN` (enviado) y `RLPGRCBD`
(recibido), que es lo que la operación consulta. Un rubro nuevo sería churn sin ganancia.
Queda cerrado; retíralo de los cabos sueltos de `ESTADO-RRHH.md`.

### 1.3 · Los cuatro reportes

Van a `src/main/resources/rep/rhh/` —la carpeta existe y está vacía; `rep/rrhh/` ya se
eliminó—. Patrón canónico: `rep/crd/RPRT_CMPB_PGCT.jrxml`. SQL nativo Oracle, alias en
`MAYUSCULA_SNAKE`, `NVL` sistemático, parámetros `P_*_CODIGO` más `P_IMAGEN` y `P_USUARIO`,
**una sola consulta plana sin subreportes**.

| Reporte | Parámetros | Qué muestra |
|---|---|---|
| Rol individual | `P_RLPG_CODIGO` | El rol de un empleado, renglón por renglón, con firma |
| Rol consolidado | `P_PRDN_CODIGO` | Todos los empleados del período, con totales |
| Provisiones | `P_PRDN_CODIGO` | `PVNM` del período agrupado por tipo |
| Resumen de aportes | `P_PRDN_CODIGO` | Personal, patronal, IECE y SECAP por empleado, para cuadrar contra la planilla del IESS |

> **Nombres de parámetro fijados el 2026-08-19**, siguiendo el patrón `P_<TABLA>_CODIGO` del
> canónico (`RPRT_CMPB_PGCT` usa `P_DTPR_CODIGO`): `P_RLPG_CODIGO` y `P_PRDN_CODIGO`, más
> `P_USUARIO` y `P_IMAGEN` en los cuatro. **El frontend ya envía `P_PRDN_CODIGO` + `P_USUARIO`**
> en los tres reportes de período: usa exactamente esos nombres o los parámetros llegan en nulo
> sin dar error. `P_IMAGEN` puede no venir: `ReporteServiceImpl` inyecta el logo por defecto
> cuando falta.

**No inventes endpoints:** los cuatro se piden por el `POST /rest/rprt/generar` que ya existe,
con `modulo = "rhh"`. `ReporteServiceImpl` compila el `.jrxml` en runtime con `JRJaninoCompiler`,
así que **basta con desplegar el `.jrxml`**; el `.jasper` es opcional.

Cada reporte lleva su `.md` en `docs/logica-negocio/reportes/`.

### 1.4 · Endpoints nuevos — avisa al frontend

No están en la §6 del maestro. Agrégalos ahí y díselo al frontend por mi conducto:

| Método | Ruta | Devuelve |
|---|---|---|
| POST | `/rest/rlpg/generar/{idPeriodo}` | número de roles generados |
| GET | `/rest/rlpg/verificar/{id}` | `true`/`false` de integridad |
| POST | `/rest/rlpg/registrarRecepcion` | 200 o error |

---

## Orden 2 · Fase 6 — Contabilización y pago

**Entrega agosto 2026 en producción.** Se construye **completa**, aunque el plan de cuentas del
cliente no haya llegado: lo que falta es un dato, no una decisión de diseño. El código queda
listo y **bloqueado por la cuenta marcadora**, que es exactamente donde debe estar el freno.

### 2.1 · La cuenta marcadora sale de `CFNM`, nunca del código

El script 13 agrega `RHH.CFNM.CFNMCTMR`, con `9678` como valor actual.
`validarCuentasContables` lee esa columna y reporta **una línea por cada `DTPL` que todavía
apunte a ella**. No escribas `9678` en Java: incumple la regla 1 del maestro.

Si esto se implementa mal, el sistema emitirá asientos **cuadrados con todas las líneas contra
la misma cuenta**. Cuadran, pasan `validaDebeHaber`, y nadie lo nota hasta conciliar el mayor.
Es peor que no emitirlos.

### 2.2 · Completar `ContabilizacionNominaService`

Hoy solo tiene la rama histórica de `contabilizarRol` y un `validarCuentasContables` que
devuelve un mensaje de "no implementado". Completa la interfaz del §6.1 del plan de backend:
`contabilizarRol` real, `contabilizarProvisiones`, `contabilizarPago`, `contabilizarLiquidacion`
y `previsualizar`.

- **El interruptor se conserva tal cual está.** El modo histórico sigue avanzando el período a
  `CONTABILIZADO` sin asiento y `validarCuentasContables` sigue devolviendo lista vacía sin
  comprobar nada. Eso es lo que desacopla la carga de enero–julio del plan de cuentas: no lo
  toques.
- **Las líneas se localizan por `DTPLAXL1`**, que lleva el código alterno del detalle del rubro
  214. Agrega al DAO `DetallePlantilla selectByPlantillaYAuxiliar(Long idPlantilla, int auxiliar1)`.
- **Las plantillas se leen de `CFNMPLRL`, `CFNMPLPR`, `CFNMPLPG` y `CFNMPLLQ`.** Los códigos
  163–166 no se escriben en Java.
- **Comprueba el cuadre antes de llamar a `generarAsiento`**, con `RedondeoNomina.suma` y
  `sonIguales`, y ajusta la diferencia por redondeo (tolerancia `CFNMTLCD`) contra la línea de
  cuadre. Sin eso el usuario recibe el `IncomeException` genérico de `validaDebeHaber`, que no
  le dice nada.
- `moduloSistema` = `ModuloSistema.RECURSOS_HUMANOS` en rol, provisiones y liquidación;
  `ModuloSistema.TESORERIA` en el asiento de pago.
- `PRDNASNT`, `PRDNASPR` y `PRDNASPG` ya existen en `PeriodoNomina`: **rol y provisiones son dos
  asientos distintos y se guardan por separado.**

### 2.3 · `OrdenPagoNominaService` — entidades `RDPG` y `DRPG`

Las dos tablas existen en el script 04 y **no tienen entidad**. Créalas con sus siete archivos
cada una, siguiendo el checklist del plan de backend.

El servicio genera la orden y su detalle resolviendo la cuenta desde `CBEM` —incluido el reparto
por `CBEMPRCN` cuando el empleado divide su sueldo entre cuentas—, produce el archivo bancario,
y al confirmar la acreditación dispara el asiento de pago y crea un `TSR.EGRS` consolidado
enlazado por `RDPGEGRSCDGO`, para que la conciliación bancaria pueda casarlo con el extracto.

**No construyas sobre `DocumentoPago`, `MontoAprobacion` ni `DocumentoCxp`:** están deprecados
en `docs/pendientes/PLAN_IMPLEMENTACION.md`.

**El formato del archivo bancario es un insumo que no tenemos.** No lo inventes rígido: hazlo
parametrizable o, si no hay dónde guardarlo, **repórtame el hueco** antes de escribir un formato
quemado. Es exactamente el caso de la regla 1.

### 2.4 · Endpoints

Los del contrato: `/rest/prdn/contabilizar/{idPeriodo}` y
`/rest/prdn/previsualizarAsiento/{idPeriodo}/{tipo}`, más los tres de `rdpg`.
Agrega `/rest/prdn/contabilizarProvisiones/{idPeriodo}` —el contrato solo preveía uno y los
asientos son dos— y avísalo.

---

## Orden 3 · Fase 7 — Asistencia

`FMRC` y `DFMR` ya tienen entidad. Falta **`CRMR`** con sus siete archivos, y las dos
ampliaciones de `Marcaciones` y `ResumenNomina` (que empiezan por el punto 0.3).

**Los nombres de propiedad de las ampliaciones ya están ratificados en el contrato** —anexo
«asistencia manual y rol de pago» de `CONTRATO-DTO-PARAMETRIZACION-RRHH.md`—, porque el frontend
ya construyó sus modelos con ellos: `Marcaciones` suma `cargaMarcaciones`, `dispositivo`,
`lineaArchivo` y `procesado`; `ResumenNomina` suma `horasTrabajadas`, `horasSuplementarias`,
`horasExtraordinarias`, `horasNocturnas`, `minutosSalidaAnticipada`, `entradaReal`, `salidaReal`,
`inconsistente`, `procesado` y `justificacion` (`tipoAusencia` ya existe). **Usa exactamente
esos**: hoy la pantalla manual escribe `procesado` y `justificacion` y se pierden en silencio
porque la entidad no los mapea.

**El archivo del biométrico sigue pendiente y no bloquea nada.** El diseño lo absorbe: el
formato vive en `FMRC`/`DFMR` como dato. Para probar el parser, crea tú un `FMRC` de prueba con
sus `DFMR` y un archivo sintético que lo cumpla; cuando llegue la muestra real se crea otro
`FMRC` y **no se toca código**. Si al llegar el archivo real hiciera falta tocar código, es que
el modelo tiene un hueco: ese es el resultado que quiero de esta fase.

`ImportacionMarcacionesServiceImpl` copia el patrón de
`ejb/tsr/serviceImpl/ImportacionExtractoBancarioServiceImpl.java`, que ya resuelve
previsualizar/confirmar con control antiduplicado por hash. Las siete reglas del parser están en
el §7 del plan de backend y no se negocian: hash SHA-256 contra `CRMR` no anulado, saltar
cabecera y pie, extraer por posición o ancho fijo, traducir con `DFMRMPEO`, emparejar por
`MPLDCDBM` con respaldo en `MPLDIDNT`, deduplicar por `(empleado, fecha-hora)`, y **una línea
mala no aborta el archivo**: va al log de `CRMRLGGO`.

`ConsolidacionMarcacionesService` agrupa por `(empleado, fecha)`. Un número impar de marcaciones
marca el `RSMN` como inconsistente para revisión manual —no lo adivines—. Los cuatro cálculos
—atraso, suplementarias al 50 %, extraordinarias al 100 % y recargo nocturno al 25 %— salen de
`PRNM` y del rubro 191; ningún porcentaje en Java.

Endpoints del contrato: `/rest/crmr/previsualizar`, `/rest/crmr/confirmar`,
`/rest/crmr/anular/{idCarga}`, `/rest/rsmn/consolidar`, `/rest/hrex/aprobar`.

---

## Orden 4 · Fase 8 — Liquidación

`Liquidacion` (`LQDC`) y `DetalleLiquidacion` (`TMLQ`) ya tienen entidad, y el script 12
desbloquea `TMLQ`. **Verifica que corrió antes de escribir una línea de esta fase**, o vas a
repetir el `ORA-02290` de enero.

**Tres huecos de mapeo verificados el 2026-08-19, que esta fase cierra primero** (dos los
reportó el backend en el orden 0; el tercero salió al verificarlos):

1. `DetalleLiquidacion` no mapea `CPNMCDGO`, `TMLQTPCN`, `TMLQBSCL`, `TMLQDIAS` ni `TMLQORDN`,
   agregados por el script 05. Sin el primero, «cada rubro del finiquito genera un `TMLQ` con su
   `CPNMCDGO`» es imposible.
2. `Liquidacion` no mapea 14 columnas del script 05: `CSTRCDGO`, `LQDCFCIN`, `LQDCANSR`,
   `LQDCULRM`, `LQDCTTIN`, `LQDCTTDS`, `LQDCDSHC`, `LQDCDSPD`, `LQDCJBPT`, `LQDCACSU`,
   `LQDCFCSU`, `ASNTCDGO`, `LQDCFCAP`, `LQDCUSAP`.
3. **`Liquidacion.estado` sigue siendo `String` y la columna ya no lo es.** El script 05 hizo
   `DROP COLUMN LQDCESTD` y la recreó como `NUMBER DEFAULT 1` (rubro `RHH_ESTADO_LIQUIDACION`).
   Es la misma familia que `MRCCTPOO`: pasa a `Long`. La primera escritura con `String`
   habría dado `ORA-01722`.

Los nombres de propiedad de los tres cierres quedan fijados en el contrato de DTO **antes** de
escribirlos: pídeme la extensión al llegar aquí, igual que para las tablas nuevas.

`LiquidacionHaberesService` con `simular`, `calcular`, `aprobar`, `ejecutarSalida` y
`generarActaFiniquitoSut`. Cada rubro del finiquito genera un `TMLQ` con su `CPNMCDGO`, de los
conceptos 60 a 67 del script 08.

Todas las reglas se leen de `PRNM` y `CSTR` —desahucio, despido intempestivo, sus topes y el
umbral de antigüedad—; ver el §8 del plan de backend. Los décimos proporcionales y las
vacaciones no gozadas se piden a `BeneficioSocialService` y a `AcreditacionVacacionesService`:
**no reimplementes ese cálculo**, que además está congelado.

Dos cosas que se olvidan:

- **Neto negativo se registra igual** y se marca para gestión de cobro. Aquí sí, a diferencia
  del rol. No lances excepción.
- `ejecutarSalida` cierra el contrato, pasa al empleado a `CESANTE`, **genera el aviso de salida
  al IESS** con `NovedadIessService.generarAvisoSalida` (ya existe), cancela los `DSRC` vigentes
  cruzando saldos y caduca los `SLDV`.

Endpoints del contrato: `/rest/lqdc/simular`, `/calcular`, `/aprobar/{id}`,
`/ejecutarSalida/{id}`. `generarActaFiniquitoSut` es un reporte de `rep/rhh/`.

---

## Orden 5 · Fase 9 — Salidas oficiales

Entidades nuevas: **`UTLD`, `DTUT`** (existen en el script 04, sin entidad) y **`SLOF`** (la crea
el script 13).

### 5.1 · Decisión: no se duplican los datos, se registra la presentación

Las filas del RDEP y del formulario 107 **ya están persistidas** en `RNGL`, `ACMN` y `LQBS`, y
los casilleros salen de `CPNMRDEP`, `CPNMF107` y `CPNMIESS`. Regenerar un archivo es
determinístico y no hace falta copiar los datos a otra tabla.

Lo que no existía en ninguna parte es **el hecho de la presentación**: cuándo se generó, quién,
con qué hash, y sobre todo si ya se presentó al organismo y con qué número de comprobante. Eso
es lo único que persiste `RHH.SLOF`. Se descartó reusar `RPR.EJRC` porque su tipo de ejecución
es un rubro del módulo de reportes a la Superintendencia y no tiene empleado, que el formulario
107 necesita porque se emite uno por persona.

`SLOF` es **idempotente por servicio**, no por unique: el índice `IX_SLOF_BUSQ` no es único a
propósito, porque `SLOFMESS` y `MPLDCDGO` son nulos en las salidas anuales y consolidadas y
Oracle no considera duplicadas dos filas donde alguna columna de la clave es nula. Busca la
salida del período y actualízala si existe.

### 5.2 · Lo que hay que construir

- **RDEP**: XML para el DIMM, casilleros desde `CPNMRDEP`.
- **Formulario 107**: PDF individual por empleado, casilleros desde `CPNMF107`. Reporte de
  `rep/rhh/`.
- **Planilla IESS**: códigos desde `CPNMIESS`. **El formato sigue pendiente del cliente:**
  construye el reporte de control, que sirve igual para cuadrar, y deja la generación del
  archivo de carga para cuando llegue la definición.
- **Formularios MDT/SUT**: décimo tercero, décimo cuarto, utilidades y acta de finiquito.
- **`UtilidadService`**: `base15 = utilidadContable × PRNMUTPR/100`, repartida en `PRNMUTDI` por
  días trabajados y `PRNMUTCG` por cargas familiares; el excedente sobre `PRNMUTSB × PRNMSBUU`
  por trabajador se transfiere al IESS. Es ingreso gravado de IR pero **no** materia gravada del
  IESS. **Se construye completo aunque `CFNMAPUT='N'` en ASOPREP**, igual que se hizo con
  `ProvisionActuarialService`: el servicio existe y rechaza la operación mientras la bandera esté
  en `'N'`.

Endpoint del contrato: `/rest/utld/calcular`. Los de `SLOF` son CRUD estándar en `/rest/slof`,
más los de generación de cada salida, que defines tú y me reportas para llevarlos al contrato.

---

## Cómo quiero la entrega

**Fase por fase, en el orden 1 → 2 → 3 → 4 → 5.** Al terminar cada una:

1. Actualiza el bloque «Estado de ejecución» de esa fase en
   `PLAN-IMPLEMENTACION-RRHH-BACKEND.md`, con lo que decidiste y por qué —igual que están
   escritas las fases 1 a 4—.
2. Actualiza `ESTADO-RRHH.md` **y su espejo en `saaFE/docs/rrh/`**.
3. Lleva al maestro §6 todo endpoint que hayas agregado, y dime cuáles son para avisar al
   frontend.
4. Dime qué hay que recompilar y publicar, y **avísame explícitamente si algo quedó a la espera
   de un script SQL**, para que yo lo escriba.

No commitees nada. El árbol de trabajo se revisa completo antes del primer commit.

**Checklist por entidad nueva** (del plan de backend, sin excepciones): entidad JPA con
`IDENTITY`, `@Table(schema="RHH")` y los dos `@NamedQuery`; constante en `NombreEntidadesRhh`
idéntica al prefijo de los `@NamedQuery`; DAO y DaoImpl con `obtieneCampos()` **de esa entidad**;
Service y ServiceImpl; Rest con `@Path` en minúsculas; traza `System.out.println` al inicio de
cada método; dinero en `Double` con `RedondeoNomina`; estados como `Long` con su rubro; FK a
`Empresa` si es tabla de parametrización.

**Y la regla que está por encima de todas:** si aparece un número, un plazo, un tope o un
formato que no tiene dónde guardarse, **es un hueco del modelo. Repórtamelo. No lo escribas en
Java.**
