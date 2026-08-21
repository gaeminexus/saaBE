# Plan de implementación RRHH — Backend

**Repositorio:** `C:\work\saaBE\v1\saaBE` · Jakarta EE 10 · Java 21 · Oracle · WildFly

> **Lee primero `PLAN-IMPLEMENTACION-RRHH-MAESTRO.md`.** Contiene las cinco reglas no
> negociables, las convenciones, el contrato REST y el orden de los scripts SQL. Este documento
> asume que ya lo leíste y solo desarrolla la ejecución del backend.

Recordatorio de la regla más importante: **ningún valor normativo se escribe en Java**. Si al
implementar aparece un número sin lugar donde guardarse, es un hueco del modelo — repórtalo.

---

## Fase 0 · Saneamiento

Siete defectos verificados. Todos son de bajo riesgo y hay que cerrarlos antes de construir.

| # | Archivo | Cambio |
|---|---|---|
| 1 | `ws/rest/rhh/ReglonNominaRest.java:25` | `@Path("rngk")` → `@Path("rngl")` |
| 2 | `ws/rest/rhh/SaldoVacacionesRest.java:25` | `@Path("SLDV")` → `@Path("sldv")` |
| 3 | `ws/rest/rhh/HistorialRest.java:25` | `@Path("hstr")` → `@Path("hscg")`. Colisiona con `crd/HistorialSueldoRest.java:25`, que se deja intacto por ser el más antiguo y estar en producción |
| 4 | `model/rhh/Historial.java:49` | El campo `departamento` es `@ManyToOne` a `DepartamentoCargo` con `@JoinColumn(name="DPRTCDGO")`, pero la PK de esa entidad es `DPTCCDGO`. Renombrar el campo a `departamentoCargo` y apuntar a la nueva columna `DPTCCDGO` (creada en el script 05) |
| 5 | 18 de 23 `ejb/rhh/daoImpl/*.java` | `obtieneCampos()` devuelve campos de otro módulo (`proposicionPagoXCuota`, `nivelAprobacion`, `usuarioAprueba`…). Reescribir con los campos Java reales de cada entidad |
| 6 | `CargoDaoServiceImpl`, `EmpleadoDaoServiceImpl` | Typos `requiositos` → `requisitos` y `apellido` → `apellidos` |
| 7 | `ws/rest/rhh/EmpleadoRest.java:107` | `delete()` llama al DAO directamente saltándose el Service. Redirigir por `empleadoService.remove(...)` |

Además: eliminar `src/main/resources/rep/rrhh/` completa — sus cuatro `.jrxml` consultan
`FROM crd.APRT a, crd.TPAP t, crd.ENTD e`, son copias del reporte de aportes de Crédito. Los
reportes nuevos van a `rep/rhh/`, que es el código de módulo que acepta
`ReporteServiceImpl.esModuloValido()`.

**Verificación de la fase:** llamar `POST /rest/{tabla}/selectByCriteria` en nómina, liquidación
y rol de pago con un criterio cualquiera, y confirmar que no lanza excepción de campo
inexistente.

---

## Fase 1 · Parametría

### 1.1 Ejecutar los scripts 01 a 09

En orden. Los scripts 07, 08 y 09 piden el parámetro `:EMPRESA` al ejecutarlos en DBeaver, y los
tres deben recibir el mismo valor. Ver §4 del maestro.

Tras ejecutar el script 06, **adelantar las secuencias de rubros** o JPA generará PK repetidas
la primera vez que alguien cree un rubro desde la aplicación:

```sql
ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 224;
ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1050;
```

### 1.2 Interfaces de rubros

Crear una interfaz por rubro en `com.saa.rubros`, con el Javadoc `"Interfaz del rubro X (nnn)"`
y las constantes `int` de sus detalles. Los valores son los `PDTRALTR` del script 06.

Agregar las 44 constantes a `com.saa.rubros.Rubros` en una sección nueva. Los rubros 221 y 222
se incorporaron el 2026-08-19; ver la nota al final de este bloque:

```java
// ================= RUBROS RHH =================
int RHH_TIPO_CONCEPTO_NOMINA        = 179;
int RHH_TIPO_CALCULO_CONCEPTO       = 180;
int RHH_BASE_CALCULO                = 181;
int RHH_ESTADO_PERIODO_NOMINA       = 182;
int RHH_ESTADO_NOMINA               = 183;
int RHH_MODO_PERIODO_NOMINA         = 184;
int RHH_ESTADO_EMPLEADO             = 185;
int RHH_TIPO_RELACION_LABORAL       = 186;
int RHH_REGION_DECIMO_CUARTO        = 187;
int RHH_MODALIDAD_DECIMO_TERCERO    = 188;
int RHH_MODALIDAD_DECIMO_CUARTO     = 189;
int RHH_MODALIDAD_FONDOS_RESERVA    = 190;
int RHH_TIPO_HORA_EXTRA             = 191;
int RHH_TIPO_MARCACION              = 192;
int RHH_ORIGEN_MARCACION            = 193;
int RHH_ESTADO_CARGA_MARCACIONES    = 194;
int RHH_CAUSAL_TERMINACION          = 195;
int RHH_ESTADO_LIQUIDACION          = 196;
int RHH_TIPO_DESCUENTO_RECURRENTE   = 197;
int RHH_ESTADO_DESCUENTO_RECURRENTE = 198;
int RHH_TIPO_CUENTA_BANCARIA        = 199;
int RHH_PARENTESCO_CARGA            = 200;
int RHH_TIPO_GASTO_PERSONAL         = 201;
int RHH_TIPO_ACUMULADO              = 202;
int RHH_TIPO_BENEFICIO_SOCIAL       = 203;
int RHH_TIPO_NOVEDAD_IESS           = 204;
int RHH_ESTADO_NOVEDAD_IESS         = 205;
int RHH_TIPO_PROVISION              = 206;
int RHH_TIPO_AUSENCIA               = 207;
int RHH_ESTADO_ORDEN_PAGO           = 208;
int RHH_FORMATO_ARCHIVO_MARCACION   = 209;
int RHH_TIPO_JORNADA                = 210;
int RHH_TIPO_SALDO_APERTURA         = 211;
int RHH_TIPO_PERIODO_NOMINA         = 212;
int RHH_ORIGEN_RENGLON              = 213;
int RHH_LINEA_ASIENTO               = 214;
int RHH_CAMPO_ARCHIVO_MARCACION     = 215;
int RHH_ENTIDAD_RECAUDADORA         = 216;
int RHH_TIPO_CAMBIO_HISTORIAL       = 217;
int RHH_GENERO                      = 218;
int RHH_ESTADO_CIVIL                = 219;
int RHH_NIVEL_INSTRUCCION           = 220;
int RHH_ROL_CONCEPTO_MOTOR          = 221;
int RHH_ESTADO_CUOTA_DESCUENTO      = 222;
```

> **Los dos últimos son posteriores al diseño original.** El 221 es el que permite al motor
> localizar cada concepto sin depender de la terna tipo/cálculo/base — ver la fase 4. El 222
> saca los estados de `CTDSESTD` del comentario del DDL, donde incumplían la regla 2.
> **No reutilizar `EstadoCuotaPrestamo`**: es de CRD y describe otro dominio.

Agregar a `com.saa.rubros.ModuloSistema`:

```java
public static final int RECURSOS_HUMANOS = 5;
```

Agregar a `com.saa.rubros.TipoAsientos` **una sola constante**: el cliente asignó el código
alterno 6 para todos los asientos de RRHH, y lo que distingue a los cuatro es la plantilla, no
el tipo de asiento.

```java
/** Asientos generados por el modulo de RRHH: rol, provisiones, pago y liquidacion. */
public static final int RECURSOS_HUMANOS = 6;
```

> **No confundir** `TipoAsientos.RECURSOS_HUMANOS = 6` con `ModuloSistema.RECURSOS_HUMANOS = 5`.
> El primero es el tipo de asiento contable (segundo argumento de `generarAsiento`); el segundo
> es la etiqueta de módulo (último argumento). Tienen el mismo nombre y distinto valor a
> propósito, porque viven en catálogos distintos.

Las plantillas se referencian por su código alterno, leído de `RHH.CFNM`: `163` rol de pagos,
`164` provisiones, `165` pago, `166` liquidación. **No los escribas como literales en Java** —
salen de `CFNMPLRL`, `CFNMPLPR`, `CFNMPLPG` y `CFNMPLLQ`.

### 1.3 Entidades de parametría y sus cinco capas

Ocho tablas: `CPNM`, `CFNM`, `PRNM`, `TBIR`, `TPGP`, `CSTR`, `FMRC`, `DFMR`.

> **Los nombres de las propiedades no son libres.** El frontend ya construyó sus pantallas de
> fase 1 contra `CONTRATO-DTO-PARAMETRIZACION-RRHH.md`, que fija la propiedad Java de cada
> columna de las ocho tablas y está verificado contra el DDL. **Usa exactamente esos nombres**
> en las entidades y en `obtieneCampos()`.
>
> Un desajuste aquí no rompe la compilación de ninguna capa: el campo simplemente llega vacío o
> no se guarda, y se descubre probando la pantalla. Si necesitas cambiar alguno, actualiza el
> contrato y avisa al frontend en lugar de resolverlo por tu lado.

Para cada una, los siete archivos del maestro §3. Usar como referencia literal una entidad
existente del mismo módulo — por ejemplo `model/rhh/Cargo.java` para la forma general y
`model/rhh/ContratoEmpleado.java` para las relaciones `@ManyToOne`.

**Plantilla de entidad** (adaptar nombres y campos; nótese `IDENTITY`, no secuencia):

```java
@SuppressWarnings("serial")
@Entity
@Table(name = "CPNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ConceptoNominaId",  query = "select e from ConceptoNomina e where e.codigo=:id"),
    @NamedQuery(name = "ConceptoNominaAll", query = "select e from ConceptoNomina e")
})
public class ConceptoNomina implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CPNMCDGO")
    private Long codigo;

    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    @Basic @Column(name = "CPNMNMBR", length = 100)
    private String nombre;

    @Basic @Column(name = "CPNMPRCN")
    private Double porcentaje;

    // ... resto de campos, con getters y setters escritos a mano (sin Lombok)
}
```

**Cuidado con `NombreEntidadesRhh`:** el valor de la constante debe coincidir **exactamente**
con el prefijo de los `@NamedQuery`, porque `EntityDaoImpl` resuelve la consulta concatenando
`entidad + "All"` y `entidad + "Id"`. Un desajuste produce `IllegalArgumentException` en runtime,
no en compilación.

**`obtieneCampos()`** devuelve los nombres de campo **Java** de esa entidad, no de otra:

```java
@Override
protected String[] obtieneCampos() {
    return new String[] { "codigo", "empresa", "nombre", "abreviatura", "codigoAlterno",
                          "tipoConcepto", "tipoCalculo", "baseCalculo", "porcentaje",
                          "orden", "estado" };
}
```

### 1.4 REST de parametría

CRUD estándar, `@Path` en minúsculas. Mantener la traza `System.out.println` al inicio de cada
método y el estilo `catch (Throwable e)` → `Response.status(INTERNAL_SERVER_ERROR)`.

### 1.5 Estado de ejecución — código entregado el 2026-08-19

Los nombres de las constantes de rubro se derivaron **literalmente del `PDTRDSCR` del script
06**, no de la prosa de este plan, porque son las descripciones que quedan en la base. Tres
difieren de cómo se nombraron en el análisis; vale la referencia al escribir la fase 4:

| Rubro | Constante real en Java | Cómo aparece en la prosa del análisis |
|---|---|---|
| 186 | `RhhTipoRelacionLaboral.APRENDIZAJE_O_PASANTIA` | `APRENDIZAJE_PASANTIA` |
| 187 | `RhhRegionDecimoCuarto.SIERRA_Y_AMAZONIA` | `SIERRA_AMAZONIA` |
| 190 | `RhhModalidadFondosReserva.ACUMULADO_EN_EL_IESS` | `ACUMULADO_EN_IESS` |

Los 42 rubros suman **262 constantes**, una por cada `PDTRALTR` del script 06.

Las ocho entidades de parametría se generaron con **mapeo 1:1 verificado contra el DDL del
script 01**: ninguna columna del DDL quedó sin mapear y ningún campo Java apunta a una columna
inexistente. `TIMESTAMP` → `LocalDateTime`, dinero y porcentajes → `Double`, `estado` y los
campos que apuntan a un rubro → `Long`, banderas S/N → `String` de longitud 1.

---

## Fase 2 · Maestro de personal

Entidades nuevas: `CRGF`, `CBEM`, `GSPR`, `CPXM`, `NVIS`, con sus cinco capas.

Ampliar las entidades existentes con los campos del script 05:

- **`Empleado`** — añadir `empresa` (`@ManyToOne` a `Empresa`), tipo de identificación, estado
  civil, género, nacionalidad, nivel de instrucción, profesión, tipo de sangre, discapacidad y
  CONADIS, enfermedad catastrófica, código de afiliación IESS, fecha de ingreso, región del
  décimo cuarto, código biométrico, contacto de emergencia, centro de costo, foto.
- **`ContratoEmpleado`** — tipo de relación laboral, jornada, horas semanales, valor hora,
  modalidades de décimos y fondos de reserva, banderas de aporte y retención, código de
  ocupación MDT, causal y fecha de terminación, centro de costo, turno.
- **`Historial`** — corregir el mapeo (fase 0, punto 4) y añadir tipo de cambio y sueldos.
- **`TipoContratoEmpleado`** y **`Catalogo`** — empresa, tipo de relación laboral / tipo de
  ausencia, límites.

Servicio nuevo:

```java
@Local
public interface NovedadIessService extends EntityService<NovedadIess> {
    /** Genera el aviso de entrada al crear un contrato. Plazo: 15 dias. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    NovedadIess generarAvisoEntrada(Long idContrato, String usuario) throws Throwable;

    /** Genera el aviso de salida al ejecutar una liquidacion. Plazo: 3 dias. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    NovedadIess generarAvisoSalida(Long idLiquidacion, String usuario) throws Throwable;

    /** Genera la novedad de modificacion de sueldo. Plazo: 3 dias. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    NovedadIess generarModificacionSueldo(Long idContrato, Double sueldoAnterior,
            Double sueldoNuevo, LocalDate vigencia, String usuario) throws Throwable;
}
```

Los plazos legales (15 y 3 días) **no se escriben en Java**: se leen del `PDTRVLRN` del detalle
correspondiente del rubro 204, que el script 06 ya carga.

### Estado de ejecución — código entregado el 2026-08-19

Las cinco entidades nuevas y las cinco ampliaciones están hechas, con mapeo 1:1 verificado
contra el script 02 y el script 05. `NovedadIessServiceImpl` lee el plazo con
`DetalleRubroDaoService.selectValorNumericoByRubAltDetAlt(Rubros.RHH_TIPO_NOVEDAD_IESS, tipo)`
y **lanza `IncomeException` si el detalle no existe o tiene `PDTRVLRN` nulo**, en vez de
suponer un valor por defecto. Los tres métodos no se exponen por REST: no están en el contrato
de la §6 del maestro y se invocan desde el alta de contrato y desde la liquidación.

`ContratoEmpleado.salarioBase` se mantiene en `Double`; el `MODIFY (CNTESLRB NUMBER(18,2))` del
script 05 sigue siendo correcto: Oracle guarda decimal exacto y redondea al escribir.

#### Los dos campos de estado — decisión del 2026-08-19

**`MPLDESTD` se convierte a `Long` con el rubro 185.** No es cosmético: el motor tiene que
distinguir CESANTE de ACTIVO para no meter en la nómina a un empleado ya liquidado, y una marca
`'A'`/`'I'` no puede expresar CON_LICENCIA ni JUBILADO. El script 05 hace el `DROP COLUMN` +
`ADD NUMBER DEFAULT 1`, y `selectActivosEnPeriodo` excluye a los CESANTE:

```sql
and (t.empleado.estado is null or t.empleado.estado <> :cesante)
```

Es cinturón y tirantes respecto del filtro por `fechaTerminacion`: cubre el caso de una
liquidación registrada sin fecha de terminación en el contrato.

**`CNTEESTD` se queda como `String`.** El estado real del contrato ya lo llevan `fechaInicio`,
`fechaFin` y `fechaTerminacion`, que es exactamente lo que el motor usa para seleccionar
contratos vigentes. Convertirlo sería churn sin ganancia, y no se crea ningún
`RHH_ESTADO_CONTRATO`.

---

## Fase 3 · Migración de apertura

Entidades: `SLAP`, `ACMN`, `DSRC`, `CTDS`. Ampliar `SaldoVacaciones`.

```java
@Local
public interface MigracionRhhService {

    /** Carga masiva de saldos desde Excel o CSV. Solo inserta en SLAP,
     *  no materializa nada todavia. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int cargarSaldosApertura(InputStream archivo, Long idEmpresa, LocalDate fechaCorte,
            String usuario) throws Throwable;

    /** Valida los SLAP contra el maestro: identificaciones inexistentes,
     *  tipos incompatibles con el contrato, montos negativos, duplicados. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<String> validarSaldosApertura(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

    /** Materializa los SLAP en las tablas operativas. Idempotente por SLAPAPLC. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int aplicarSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;

    /** Deshace la aplicacion usando SLAPRFTB y SLAPRFID. */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int revertirSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;
}
```

**Qué materializa cada tipo de saldo** (rubro 211):

| Tipo | Destino |
|---|---|
| ANTIGUEDAD | `MPLD.MPLDFCIN` |
| VACACIONES_PENDIENTES | Un `SLDV` por período con `SLDVAPRT='S'` |
| DECIMO_TERCERO_ACUMULADO | `ACMN` tipo 3 con `ACMNAPRT='S'` |
| DECIMO_CUARTO_ACUMULADO | `ACMN` tipo 4 con `ACMNAPRT='S'` |
| FONDOS_RESERVA_ACUMULADOS | `ACMN` tipo 5 con `ACMNAPRT='S'` |
| PRESTAMO_IESS / PRESTAMO_INTERNO | Un `DSRC` con `DSRCAPRT='S'` y sus `CTDS` |
| IR_RETENIDO_EN_EL_ANIO | `ACMN` tipo 9 |

Cada materialización graba `SLAPRFTB` y `SLAPRFID` para que la reversión sea exacta. Con 18–25
empleados esto se valida a mano cómodamente.

### Estado de ejecución — código entregado el 2026-08-19

Entidades `ACMN`, `DSRC`, `CTDS` y `SLAP` con sus siete archivos, `SaldoVacaciones` ampliada con
los nueve campos del script 05, y `MigracionRhhServiceImpl` completo.

**El detalle operativo vive en `REGLAS-MIGRACION-APERTURA.md`**, que es el documento de
referencia de este proceso: formato del archivo, mapa de materialización, validaciones,
reversión y consultas de control.

Consultas propias añadidas a los DAO:

| DAO | Método |
|---|---|
| `SaldoAperturaDaoService` | `selectByEmpresaYCorte`, `selectPendientesPorAplicar`, `selectAplicados`, `selectDuplicados` |
| `EmpleadoDaoService` | `selectByIdentificacion(identificacion, idEmpresa)` |
| `ConceptoNominaDaoService` | `selectByCodigoAlterno(codigoAlterno, idEmpresa)`, `selectActivosByEmpresa(idEmpresa)` |

`selectByIdentificacion` compara la empresa con `OR t.empresa is null` porque los empleados
cargados antes de que el script 05 añadiera `MPLDCDGO.PJRQCDGO` todavía no la tienen asignada.
Devuelve `null` si encuentra más de uno: una identificación duplicada es un problema de datos que
la validación reporta, no algo que el DAO deba resolver eligiendo.

Los cuatro endpoints de proceso están en `SaldoAperturaRest`, con las rutas de la §6 del maestro
sin cambios. `aplicar` y `revertir` reciben `Map<String, Object>` como cuerpo, siguiendo el
precedente de `AplicacionPagoCxcRest` y `PagoProgramadoRest`.

---

## Fase 4 · Motor de cálculo

Es la fase más grande y la que define el módulo.

### 4.1 Entidades

`NVNM`, `PVNM`, `PYIR`, `LQBS`, `HREX`, con sus cinco capas. Ampliar `PeriodoNomina`, `Nomina`,
`ReglonNomina`, `RolPago` con los campos del script 05.

### 4.2 DTO

POJO `Serializable` sin `@Entity` en `com.saa.model.rhh`, con la forma definida en §6 del
maestro. Referencia de estilo: `com.saa.model.cnt.RespuestaBalance`.

### 4.3 `ProcesoNominaService`

```java
@Local
public interface ProcesoNominaService {

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<String> validarPeriodo(Long idPeriodoNomina) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    ResultadoCalculoPeriodo calcularPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    ResultadoCalculoNomina recalcularEmpleado(Long idPeriodoNomina, Long idEmpleado,
            boolean preservarManuales, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    ResultadoCalculoNomina simular(Long idContrato, Long idPeriodoNomina) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void aprobarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void reabrirPeriodo(Long idPeriodoNomina, String motivo, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void cerrarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void excluirEmpleado(Long idPeriodoNomina, Long idEmpleado, String motivo,
            String usuario) throws Throwable;
}
```

### 4.4 Algoritmo de `calcularPeriodo`

Es idempotente: borra y regenera `NMNA`, `RNGL` y `PVNM` del período, preservando los `RNGL`
con `RNGLMNAL='S'`. **Los acumulados `ACMN` no se tocan aquí** — se escriben en `cerrarPeriodo`,
para que recalcular no los duplique. Este punto es la causa clásica de décimos inflados.

1. **Cargar parametría.** `PRNM` del año de `PRDNFCHF`, `TBIR`, `TPGP`, `CFNM`. Si falta el
   `PRNM` del año, lanzar `IncomeException` con mensaje explícito.
2. **Seleccionar contratos.** `CNTE` activos que se solapen con el período:
   `CNTEFCHI <= PRDNFCHF AND (CNTEFCHF IS NULL OR CNTEFCHF >= PRDNFCHI)`.
3. **Días trabajados.** `PRNMDIAS` menos los días de ausencia no remunerada del período (suma de
   `RSMN` con `RSMNTPAS` en {FALTA_INJUSTIFICADA, PERMISO_SIN_GOCE}), ajustado por ingreso o
   salida a mitad de mes.
4. **Sueldo del período.**
   - Jornada completa: `CNTESLRB × diasTrabajados / PRNMDIAS`.
   - Parcial o por horas: `horasEfectivas × CNTEVLHR`.
   - Servicios profesionales (`CNTETPRL = 6`): el honorario pactado, **sin prorrateo**.
5. **Recolectar renglones de ingreso.** Conceptos con `CPNMOBLG='S'` y `CPNMTPRL` compatible;
   `CPXM` vigentes en el rango; `NVNM` del período con `NVNMAPRB='S'`; `HREX` con `HREXAPRB='S'`.
   Para las horas extra: `valorHora = CNTESLRB / PRNMHRMS`, y el valor es
   `horas × valorHora × (1 + recargo/100)`, con el recargo del `PDTRVLRN` del rubro 191.
6. **Calcular las bases** en una sola pasada sobre los renglones ya generados: `baseIESS` suma
   los de `CPNMIMIE='S'`, `baseIR` los de `CPNMIMIR='S'`, y análogamente `baseFR`, `baseDec3`,
   `baseDec4`, `baseVac`, `baseUtil`.
7. **Aportes**, solo si `CNTEAPRT='S'`: personal `baseIESS × CPNMPRCN/100` como egreso; patronal
   `× PRNMAPPT/100` e IECE+SECAP `× (PRNMIECE+PRNMSCAP)/100` como conceptos `CPNMPTRN='S'`, que
   **no afectan el neto**.
8. **Fondos de reserva.** Si `CNTEFRMD = MENSUALIZADO` y la antigüedad supera un año
   (`MPLDFCIN + 1 año <= PRDNFCHF`): renglón de ingreso `baseFR × PRNMFNRS/100`. Si es
   `ACUMULADO_EN_EL_IESS`: sin renglón, se genera `PVNM` tipo 4.
9. **Décimo tercero.** Mensualizado: renglón `baseDec3 / 12`. Acumulado: `PVNM` tipo 1.
10. **Décimo cuarto.** Mensualizado: `PRNMSBUU / 12 × (diasTrabajados / PRNMDIAS)`. Acumulado:
    `PVNM` tipo 2. No aplica si `CNTEDCMS='N'`.
11. **Retención de IR.** `RetencionRentaService.obtenerRetencionMensual(...)`, que lee la `PYIR`
    vigente; si no existe, la genera en línea.
12. **Descuentos recurrentes.** `CTDS` con `CTDSFCVN` dentro del período y estado PENDIENTE. Los
    porcentuales (`DSRCPRCN` no nulo) se calculan sobre el neto preliminar.
13. **Neto** = Σ ingresos (`CPNMTPCN=1`) − Σ egresos (`CPNMTPCN=2`). Los tipos 3 y 4 se excluyen.
14. **Protección de neto negativo.** Si el neto queda bajo cero, recortar descuentos con
    `CPNMRCRT='S'` en orden **descendente** de `CPNMORDN` hasta que el neto sea ≥ 0; la cuota
    afectada queda PARCIAL con `CTDSVLDS < CTDSTTAL`. Los de `CPNMRCRT='N'` —aporte IESS,
    impuesto a la renta, retención judicial, préstamos IESS— **nunca se recortan**; si aun así el
    neto es negativo, lanzar `IncomeException` nombrando al empleado.
15. **Persistir** `NMNA` con todos los subtotales, `RNGL` con snapshot completo (incluidas las
    banderas `RNGLIMIE`, `RNGLIMIR`, `RNGLPTRN` y la trazabilidad `RNGLRFTB`/`RNGLRFID`), y `PVNM`.

**Redondeo:** `RedondeoNomina.redondea(...)` en cada renglón antes de sumar, y `RedondeoNomina.suma(...)` para acumular. Ver la regla 4 del maestro.

### 4.5 `RetencionRentaService`

```java
@Local
public interface RetencionRentaService {
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    ResultadoProyeccionIr proyectar(Long idEmpleado, Integer anio, Integer mesDesde,
            String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int proyectarTodos(Long idEmpresa, Integer anio, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Double obtenerRetencionMensual(Long idEmpleado, Integer anio, Integer mes) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    Double calcularImpuestoSegunTabla(Double baseImponible, Integer anio) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    Double calcularTopeGastosPersonales(Long idEmpleado, Integer anio) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    ResultadoProyeccionIr liquidarAnio(Long idEmpleado, Integer anio, String usuario) throws Throwable;
}
```

Algoritmo de `proyectar`:

1. `ingresosRealizados` = Σ `ACMN` tipo 2 de los meses anteriores del año.
2. `ingresosFuturos` = remuneración gravada mensual actual × meses restantes. **Se excluyen
   décimo tercero, décimo cuarto y fondos de reserva**: son exentos por el Art. 9 de la LRTI.
3. `ingresosProyectados` = realizados + futuros.
4. `aportePersonalProyectado` = Σ `ACMN` tipo 8 realizados + (baseIESS mensual × `PRNMAPPR`/100 ×
   meses restantes).
5. **`baseImponible = ingresosProyectados − aportePersonalProyectado`.**
6. `impuestoCausado`: localizar el tramo en `TBIR` donde
   `TBIRFRBS <= base AND (TBIREXCS IS NULL OR base < TBIREXCS)`; resultado
   `TBIRIMFB + (base − TBIRFRBS) × TBIRPRCN / 100`.
7. `tope`: si `MPLDCTSF='S'` → `PRNMCNCT × PRNMCNBS`; si no → `TPGPNCAN × PRNMCNBS` para
   `TPGPNCRG = min(nCargas, máximo de la tabla)`, con
   `nCargas = COUNT(CRGF WHERE CRGFIRRB='S' AND vigente)`.
8. `rebaja = min(gastosDeclarados, tope) × PRNMTPGP / 100`.
9. `impuestoAPagar = max(0, causado − rebaja)`.
10. `retencionMensual = (impuestoAPagar − Σ ACMN tipo 9 del año) / mesesRestantes`, con piso 0.
11. Marcar la `PYIR` anterior con `PYIRVGNT='N'` e insertar la nueva.

Se reproyecta en enero, al ingresar un empleado, al cambiar el sueldo y cuando el empleado
presenta su anexo de gastos personales.

**Servicios profesionales sin dependencia** (`CNTERTFN='S'`) no entran aquí: se les aplica
`honorario × CNTEPRRF / 100` como retención puntual, y su comprobante es una retención emitida
en CXC, no el RDEP.

### Estado de ejecución — código entregado el 2026-08-19

Entregados: las cinco entidades (`NVNM`, `PVNM`, `PYIR`, `LQBS`, `HREX`), las cuatro
ampliaciones (`PRDN`, `NMNA`, `RNGL`, `RLPG`), los siete DTO, ~25 consultas propias de DAO,
`RetencionRentaServiceImpl`, `ProcesoNominaServiceImpl` y los endpoints de proceso de `prdn` y
`pyir`. **Pendiente de la fase: `BeneficioSocialService` y `SaldoVacacionesService` (§4.6) y
`ProvisionNominaService.cargarProvisionActuarial` (§4.7).**

#### Cómo el motor localiza cada concepto: `CPNMROLM`

El motor **no referencia ningún `CPNMALTR`**. Cada concepto especial se localiza por
**`CPNM.CPNMROLM`**, un detalle del rubro 221 `RHH_ROL_CONCEPTO_MOTOR` que declara qué papel
cumple ese concepto dentro del cálculo.

> **Por qué no la terna.** La primera versión los localizaba por
> `(CPNMTPCN, CPNMTPCL, CPNMBSCL)`, que es discriminante en el catálogo del script 08 pero deja
> de serlo en cuanto el cliente agrega conceptos propios — y `CPNM` existe precisamente para
> eso. Un «Bono navideño» definido como `INGRESO · FORMULA · SBU` habría colisionado con el
> décimo cuarto y el motor habría tomado el equivocado sin avisar. Con el rol la identificación
> es explícita, y el índice `UQ_CPNM_ROLM (PJRQCDGO, CPNMROLM)` impide que dos conceptos de la
> misma empresa reclamen el mismo papel.

Los 22 roles: aporte personal, aporte patronal, IECE, SECAP, fondos de reserva, décimo tercero,
décimo cuarto, impuesto a la renta, las tres clases de hora extra, los cinco descuentos
recurrentes (quirografario, hipotecario, anticipo, préstamo interno, retención judicial) y los
seis conceptos de provisión.

> **Los seis roles de provisión (17–22) se añadieron el 2026-08-19.** Sin ellos el motor apuntaba
> la fila de `PVNM` al concepto **mensualizado** en vez de al de **provisión** —«Décimo tercero
> mensualizado» en una fila que es una provisión—, y además la jubilación patronal y el desahucio
> comparten terna (`PROVISION · MANUAL · SUELDO_CONTRATO`), así que no había forma de
> distinguirlos.

Los conceptos sin rol —sueldo, bonos, comisiones, subsidios, multas, provisiones y rubros de
liquidación— quedan en `NULL` y el bucle genérico los trata por su tipo de cálculo y su base.

**Una excepción documentada:** la retención en la fuente por servicios profesionales sigue
localizándose por la terna `EGRESO · PORCENTAJE_SOBRE_BASE · TOTAL_INGRESOS`, porque el rubro 221
no le asignó rol — no forma parte del cálculo ordinario de nómina. Si el catálogo llega a tener
dos egresos porcentuales sobre el total de ingresos, hay que darle su propio rol.

El rol resuelve además dos mapeos que antes no tenían forma limpia: **saldo de apertura →
concepto** (los préstamos migrados) y **descuento recurrente → concepto**.

#### Precedencia del porcentaje: manda `CPNMPRCN`

Cinco porcentajes normativos viven **duplicados** en `PRNM` y en `CPNM`:

| Concepto | En `PRNM` | En `CPNM` (script 08) |
|---|---|---|
| Aporte personal IESS | `PRNMAPPR` = 9,45 | alterno 20, `CPNMPRCN` = 9,45 |
| Aporte patronal IESS | `PRNMAPPT` = 11,15 | alterno 40, `CPNMPRCN` = 11,15 |
| IECE | `PRNMIECE` = 0,50 | alterno 41, `CPNMPRCN` = 0,50 |
| SECAP | `PRNMSCAP` = 0,50 | alterno 42, `CPNMPRCN` = 0,50 |
| Fondos de reserva | `PRNMFNRS` = 8,33 | alterno 7, `CPNMPRCN` = 8,33 |

El §4.4 original decía «`baseIESS × PRNMAPPR/100`». **Se cambió: manda `CPNMPRCN`, con caída a
`PRNM` cuando el concepto no lo trae informado.** La razón es estructural: `PRNM` no puede
distinguir el IECE del SECAP —ambos son 0,50 % sobre la misma base— mientras que el catálogo sí,
porque son dos filas con su propia cuenta contable. Elegir `PRNM` obligaría a identificar esas
dos filas por su `CPNMALTR`, que es justo lo que la regla 1 prohíbe.

**El control tiene dos niveles, a propósito:**

- `validarPeriodo` compara las dos fuentes y devuelve un mensaje con prefijo `Aviso:`. **No
  bloquea**: se acumula en `errores` del `ResultadoCalculoPeriodo` y permite simular y
  recalcular mientras se decide cuál de las dos fuentes corregir.
- **`aprobarPeriodo` lanza `IncomeException` si alguna de las cinco parejas diverge.** Calcular
  con una tasa desactualizada se corrige recalculando; aprobarla y contabilizarla, no. La
  aprobación es el último punto reversible del flujo, y ahí el aviso pasa a ser un bloqueo.

#### Otras decisiones del motor

- **Días trabajados**: se prorratean sobre `PRNMDIAS` (días comerciales), no sobre los del
  calendario, ajustando por ingreso o salida a mitad de período y restando las ausencias de tipo
  `FALTA_INJUSTIFICADA` y `PERMISO_SIN_GOCE`.
- **`calculaContrato` recibe la lista de renglones como parámetro de salida.** No se usa un
  campo de instancia ni un `ThreadLocal`: en un `@Stateless` el contenedor reutiliza instancias
  y hilos, y cualquiera de las dos formas produciría cruces de datos entre empleados bajo carga.
- **Los acumulados `ACMN` solo se escriben en `cerrarPeriodo`**, que además los borra primero,
  de modo que cerrar dos veces no duplica. `reabrirPeriodo` los retira.

### 4.6 `BeneficioSocialService` y `SaldoVacacionesService`

Reglas de cálculo:

- **Décimo tercero acumulado** = Σ `ACMN` tipo 3 del 1-dic al 30-nov ÷ 12, menos `LQBSVLMN` (lo
  ya pagado mensualizado, si hubo cambio de modalidad).
- **Décimo cuarto** = `PRNMSBUU × (días del período / PRNMDANO)`, tope 1 SBU. El período depende
  de `MPLDRGNN`: Sierra y Amazonía 1-ago→31-jul, Costa e Insular 1-mar→28-feb. ASOPREP opera en
  Quito, de modo que el valor por defecto es Sierra, pero se parametriza por empleado.
- **Fondos de reserva** = Σ `ACMN` tipo 5 × `PRNMFNRS`/100, solo meses posteriores al primer año.
- **Vacaciones**: días = `PRNMDIVC + max(0, min(añosCumplidos − (PRNMANVC − 1), PRNMMXVC − PRNMDIVC))`,
  con tope `PRNMMXVC`. El valor del día = Σ `ACMN` tipo 7 de los últimos 12 meses ÷ `PRNMDANO`,
  lo que incluye horas extra y comisiones (Art. 71 CT). Consumo FIFO sobre los saldos más
  antiguos; caducidad a los `PRNMCDVC` años.

### 4.7 `ProvisionNominaService`

Bases mensuales: décimo tercero `baseDec3/12`; décimo cuarto `PRNMSBUU/12`; vacaciones
`baseVac × PRNMDIVC / PRNMDANO`; fondos de reserva `baseFR × PRNMFNRS/100`. Jubilación patronal
y desahucio se cargan desde estudio actuarial externo con `cargarProvisionActuarial(...)`,
condicionadas a `CFNMAPJP` y `CFNMAPDS`.

> **Corrección del 2026-08-19: el aporte patronal NO se provisiona.** Este párrafo lo listaba
> entre las provisiones mensuales, y el rubro 206 tiene un tipo `APORTE_PATRONAL`. Es un error:
> ni el catálogo del script 08 tiene un concepto para él, ni el asiento de provisiones (rubro
> 214, códigos 30–35 y 40–45) tiene línea donde ponerlo. El asiento de **rol** ya lo registra
> completo —línea 3 al DEBE, línea 11 al HABER—, así que provisionarlo contaría el mismo costo
> dos veces. El tipo 5 del rubro 206 queda sin uso.

> **Las vacaciones se provisionan SIEMPRE**, sin depender de ninguna modalidad del contrato: no
> admiten mensualización. Es la única provisión sin renglón equivalente en el rol. El divisor 24
> que suele citarse **no está escrito en el código**: sale de `PRNMDIVC / PRNMDANO`, que con la
> parametría 2026 (15 días sobre 360) da exactamente 1/24.

### Estado de ejecución de §4.6 y §4.7 — entregado el 2026-08-19

**Tres servicios nuevos, no dos.** El plan hablaba de «`BeneficioSocialService` y
`SaldoVacacionesService`», pero `SaldoVacacionesService` ya existe: es el CRUD generado de la
tabla. Meterle la lógica de negocio habría mezclado las dos responsabilidades, así que la
acreditación vive en **`AcreditacionVacacionesService`**, igual que `ProcesoNominaService`
convive con `NominaService`. Mismo criterio para las provisiones actuariales, que van en
**`ProvisionActuarialService`** y no dentro del CRUD de `PVNM`.

| Servicio | Qué resuelve |
|---|---|
| `BeneficioSocialService` | Décimo tercero, décimo cuarto y fondos de reserva acumulados |
| `AcreditacionVacacionesService` | Acreditación anual, consumo FIFO, reversión, caducidad y valor del día |
| `ProvisionActuarialService` | Carga de jubilación patronal y desahucio desde estudio externo |

#### Las tres ventanas de acumulación

Ninguna coincide con el año calendario salvo la de fondos de reserva, y por eso los acumulados
se suman con `sumaValorRango`, que compara `año × 100 + mes`:

| Beneficio | Ventana | Base legal |
|---|---|---|
| Décimo tercero | 1-dic del año anterior → 30-nov | Art. 111 CT |
| Décimo cuarto Sierra y Amazonía | 1-ago del año anterior → 31-jul | Art. 113 CT |
| Décimo cuarto Costa e Insular | 1-mar → último día de febrero | Art. 113 CT |
| Fondos de reserva | Año calendario, desde el mes en que cumple un año de servicio | CT |

El último día de febrero se resuelve con `lengthOfMonth()`, no con un 28 literal: los años
bisiestos habrían dado un día de menos uno de cada cuatro años.

#### La escala de vacaciones, sin números en el código

```
dias = PRNMDIVC + max(0, min(añosCumplidos − (PRNMANVC − 1), PRNMMXVC − PRNMDIVC))
```

Con la parametría 2026 (15 / 5 / 30) da 15 días hasta el cuarto año, 16 al quinto, y sube de uno
en uno hasta 30. Un cambio de norma se resuelve con un `UPDATE`.

El **consumo es FIFO** —del período más antiguo al más reciente, para gastar primero lo que está
por caducar— y la **reversión va en orden inverso**, devolviendo primero al período más reciente,
que es del que se gastó al final. La caducidad usa `PRNMCDVC`; **si ese parámetro está vacío no
se caduca nada**, porque acumular de más es preferible a borrar un derecho por una suposición.

#### Idempotencia

- Los tres generadores de beneficios se apoyan en el unique `UQ_LQBS_BNF`: buscan el beneficio
  del año y lo actualizan si existe. **`LQBSVLPG` —lo ya pagado— nunca se toca al regenerar.**
- `acreditar` recalcula los días asignados sin tocar los ya usados, y caduca antes de arrastrar,
  para que el arrastre no lleve días muertos.
- `cargarProvisionActuarial` es idempotente por `(período, empleado, tipo)`: recargar el estudio
  actualiza el valor, que es lo útil cuando el actuario corrige una cifra.

`cargarProvisionActuarial` **rechaza cualquier tipo que no sea jubilación patronal o desahucio**,
con un mensaje que explica que las demás las genera `calcularPeriodo` a partir de las bases del
período. Y exige `CFNMAPJP`/`CFNMAPDS` en `'S'`: en ASOPREP ambas están en `'N'`, así que el
servicio existe pero rechaza la carga hasta que alguien active la bandera.

#### Endpoints

Los cuatro del contrato, más dos que el contrato no listaba y que hacen falta para operar:

| Método | Ruta | En el contrato |
|---|---|---|
| POST | `/rest/lqbs/generarDecimoTercero/{anio}` | Sí |
| POST | `/rest/lqbs/generarDecimoCuarto/{anio}/{region}` | Sí |
| POST | `/rest/sldv/acreditar` | Sí |
| GET | `/rest/sldv/disponible/{idEmpleado}` | Sí |
| POST | `/rest/lqbs/generarFondosReserva/{anio}` | **No — añadido** |
| POST | `/rest/sldv/caducar` | **No — añadido** |

Los dos añadidos no modifican ninguna ruta existente, solo agregan. Aun así hay que reflejarlos
en la §6 del maestro y avisar al frontend.

---

## Fase 5 · Rol de pago y reportes internos

Ampliar `RolPago` con totales y hash. Generar el PDF por Jasper.

Reportes en `src/main/resources/rep/rhh/`, siguiendo el patrón canónico de
`rep/crd/RPRT_CMPB_PGCT.jrxml`: SQL nativo Oracle, alias en `MAYUSCULA_SNAKE`, `NVL`
sistemático, parámetros `P_*_CODIGO` más `P_IMAGEN` y `P_USUARIO`, una sola consulta plana sin
subreportes.

Reportes de esta fase: rol individual, rol consolidado por período, provisiones, resumen de
aportes. Cada uno lleva su `.md` en `docs/logica-negocio/reportes/`.

Recordatorio útil: `ReporteServiceImpl` compila el `.jrxml` en runtime forzando
`JRJaninoCompiler`, así que **basta con desplegar el `.jrxml`**; el `.jasper` es opcional.

### Estado de ejecución — código entregado el 2026-08-19

Entregados: `GeneracionRolPagoService` con su `ServiceImpl`, dos consultas propias en
`RolPagoDaoService`, los tres endpoints de proceso en `RolPagoRest`, el enganche en
`aprobarPeriodo` y los cuatro `.jrxml` de `rep/rhh/` con su `.md` en
`docs/logica-negocio/reportes/`. **No hubo entidad nueva:** `RolPago` ya traía los seis campos
del script 05 y los mapea todos.

#### El servicio, no el CRUD

La generación vive en `GeneracionRolPagoService`, junto al CRUD `RolPagoService`, con el mismo
criterio con que `ProcesoNominaService` convive con `NominaService` y `AcreditacionVacaciones`
con `SaldoVacaciones`: el proceso en su servicio, el mantenimiento de la fila en el suyo.

#### Cuándo se emite el rol

Al final de `aprobarPeriodo`, que es la **primera de las dos excepciones autorizadas** al
congelamiento del motor. Antes de APROBADO el cálculo todavía puede cambiar y un rol impreso
quedaría desmentido sin que nadie se entere.

`generarRoles` admite además CONTABILIZADO y PAGADO, para la regeneración tras una reapertura y
recálculo. **Rechaza CERRADO** y cualquier estado anterior a la aprobación: desde el cierre el
período ya no admite recálculo, así que regenerar solo cambiaría la fecha de emisión de un
documento ya entregado.

#### Las tres columnas obligatorias, y el marcador técnico

El barrido de `NOT NULL` encontró que `RLPGESTD`, `RLPGNMRO` y `RLPGFCHA` son obligatorias,
declaradas como CHECK con nombre de sistema. Las tres se llenan siempre.

`RLPGESTD` se graba con **`'A'`**, el marcador técnico de fila vigente que usan las tablas de
RHH que conservaron estado `VARCHAR2` (`CRGO`, `DPRT`, `DPTC`, `TPCE`, `TRNO`). No es un valor
normativo —no describe ninguna regla de negocio ni cambia con la ley—, así que no incumple la
regla 1. Se mantiene la decisión de no convertirlo a `Long` ni crear un rubro de estado del rol:
el estado real lo llevan `RLPGFCEN` y `RLPGRCBD`.

`RLPGNMRO` es **`AAAAMM-NNNNNN`**: año y mes del período más el código del empleado. Es
determinista a propósito, y de ahí sale la idempotencia — regenerar produce el mismo número y
actualiza la fila en vez de crear otra con numeración nueva.

#### Qué entra en el hash y qué no

`RLPGHASH` es SHA-256 sobre empleado, período, días trabajados, cada renglón (orden, concepto,
tipo y valor) y los tres totales, en ese orden.

**No entran la fecha de emisión, el usuario ni la entrega.** Son metadatos del documento y no su
contenido: si entraran, regenerar un rol idéntico daría un hash distinto y `verificarIntegridad`
dejaría de significar «la nómina no cambió» para significar «nadie volvió a pulsar el botón».

`verificarIntegridad` devuelve `false` —no lanza— cuando el rol no tiene hash grabado: no se
puede declarar íntegro lo que no se comprobó.

#### Lo que la regeneración nunca toca

`RLPGFCEN` y `RLPGRCBD`. La entrega al empleado es un hecho ocurrido y volver a emitir el
documento no lo deshace. Es el mismo criterio con el que `BeneficioSocialService` no toca
`LQBSVLPG` al regenerar un beneficio.

#### `registrarRecepcion` recibe una lista

Firma acordada con el frontend el 2026-08-19: `int registrarRecepcion(List<Long>, String)`. La
entrega se registra por tandas —el operador marca las firmas que recogió ese día—, siguiendo el
precedente de `/rest/hrex/aprobar`. Pone `RLPGRCBD='S'` y sella `RLPGFCEN` con la fecha del día
**solo si está en nulo**, para no reescribir la fecha de una entrega ya registrada.

Un id inexistente aborta la tanda entera. Marcarla a medias dejaría al operador sin saber
cuáles quedaron registrados.

#### Los cuatro reportes

Van a `src/main/resources/rep/rhh/`, patrón canónico de `rep/crd/RPRT_CMPB_PGCT.jrxml`: SQL
nativo, alias en `MAYUSCULA_SNAKE`, `NVL` sistemático, una sola consulta plana sin subreportes.
**Sin endpoint propio**: los cuatro se piden por `POST /rest/rprt/generar` con `modulo: "rhh"`.
Basta el `.jrxml`; el `.jasper` es opcional porque `ReporteServiceImpl` compila en runtime.

| Reporte | Parámetro | Lee de | Nota |
|---|---|---|---|
| `RPRT_ROLL_INDV` | `P_RLPG_CODIGO` | `RLPG` + `RNGL` | Totales **grabados**, no recalculados: es un documento emitido |
| `RPRT_ROLL_CNSL` | `P_PRDN_CODIGO` | `NMNA` | Lee `NMNA` y no `RLPG`, para poder revisarlo **antes** de aprobar |
| `RPRT_PRVS_PRDO` | `P_PRDN_CODIGO` | `PVNM` | Agrupado por tipo, con subtotal |
| `RPRT_APRT_RSMN` | `P_PRDN_CODIGO` | `NMNA` | El que cuadra contra la planilla del IESS |

Los nombres de parámetro se fijaron con el frontend el 2026-08-19; los tres de período comparten
`P_PRDN_CODIGO`.

**Dos controles impresos**, que valen más que cualquier comentario en el código:

- El rol individual compara la suma de sus renglones contra los totales grabados en `RLPG` y
  avisa si difieren: la nómina cambió tras emitir el rol. Es la versión visible del hash.
- El resumen de aportes calcula `NMNATTPT − (NMNAAPPT + NMNAIESC)` y avisa si alguna nómina
  descuadra. Delata las cabeceras escritas **antes** del reparto por rol del 2026-08-19, que
  llevaban el total patronal en `NMNAAPPT` y `NMNAIESC` en nulo. Se corrigen recalculando.
  Por eso ese reporte lee la cabecera y no suma renglones: sumar daría siempre bien y ocultaría
  justo el defecto que tiene que encontrar.

#### Endpoints nuevos

Los tres del orden 1, ya llevados a la §6 del maestro. `usuarioRegistro` viaja como parámetro de
consulta en los dos POST —precedente de `/rest/prdn/calcular/{id}?usuarioRegistro=`—, porque en
`registrarRecepcion` el cuerpo ya lo ocupa la lista de ids.

| Método | Ruta | Cuerpo | Devuelve |
|---|---|---|---|
| POST | `/rest/rlpg/generar/{idPeriodo}` | — | número de roles generados |
| GET | `/rest/rlpg/verificar/{id}` | — | `true` / `false` |
| POST | `/rest/rlpg/registrarRecepcion` | `List<Long>` | número de roles marcados |


---

## Fase 6 · Contabilización y pago

### 6.1 `ContabilizacionNominaService`

```java
@Local
public interface ContabilizacionNominaService {

    /** Verifica que todas las lineas de plantilla y conceptos con
     *  movimiento tengan una cuenta REAL asignada. Se invoca en la
     *  APROBACION, no en la contabilizacion, para que el problema salga
     *  antes. Ojo: la condicion de "sin configurar" es PLNNCDGO = 9678,
     *  la cuenta marcadora, NO un valor nulo. Ver la nota de abajo. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<String> validarCuentasContables(Long idPeriodoNomina) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarRol(Long idPeriodoNomina, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarProvisiones(Long idPeriodoNomina, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarPago(Long idOrdenPago, LocalDate fechaAcreditacion,
            String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarLiquidacion(Long idLiquidacion, String usuario) throws Throwable;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<LineaAsientoNomina> previsualizar(Long idPeriodoNomina, Long tipoAsiento) throws Throwable;
}
```

### 6.2 El interruptor

```java
PeriodoNomina p = periodoNominaService.selectById(idPeriodoNomina);

if (!Long.valueOf(RhhEstadoPeriodoNomina.APROBADO).equals(p.getEstado())) {
    throw new IncomeException("El periodo debe estar APROBADO para contabilizarse.");
}

if (Long.valueOf(RhhModoPeriodoNomina.HISTORICO_SIN_CONTABILIZAR).equals(p.getModo())) {
    System.out.println("Periodo " + idPeriodoNomina + " en modo HISTORICO: no se genera asiento.");
    p.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO)); // avanza el flujo igual
    p.setObservaciones("Calculado sin contabilizacion (carga historica).");
    periodoNominaService.saveSingle(p);
    return null;
}

List<DetalleAsiento> lineas = armarLineasRol(p);       // ver 6.3
comprobarCuadre(lineas, cfnm.getToleranciaCuadre());   // ver 6.4

Asiento a = asientoContableService.generarAsiento(
        p.getEmpresa().getCodigo(),
        cfnm.getTipoAsientoRol().intValue(),
        p.getFechaContable(),
        "Rol de pagos " + p.getMes() + "/" + p.getAnio(),
        usuario,
        lineas,
        Long.valueOf(ModuloSistema.RECURSOS_HUMANOS));

p.setAsiento(a.getCodigo());
```

> **La cuenta marcadora 9678.** `CNT.DTPL.PLNNCDGO` es `NOT NULL`, así que el script 09 no puede
> dejar las líneas sin cuenta mientras llega el plan de cuentas definitivo: las crea todas
> apuntando a la cuenta **9678**, que es un marcador temporal y no la cuenta correcta de ningún
> asiento.
>
> Por eso `validarCuentasContables` debe buscar `PLNNCDGO = 9678`, **no** `PLNNCDGO IS NULL`, y
> devolver un mensaje por cada línea que siga con el marcador. Ningún período puede
> contabilizarse mientras quede una. El valor no se escribe como literal en Java: se define como
> constante de configuración —lo natural es una columna nueva en `RHH.CFNM`— para respetar la
> regla de que nada se quema en el código.
>
> Si esto se implementa mal, el sistema emitirá asientos cuadrados pero con **todas las líneas
> contra la misma cuenta**, que es peor que no emitirlos: cuadran, pasan `validaDebeHaber` y
> nadie lo nota hasta conciliar el mayor.

`validarCuentasContables` **devuelve lista vacía sin comprobar nada** cuando el modo es
histórico. Es lo que desacopla la carga de enero–julio del plan de cuentas.

### 6.3 Armado de líneas

Para cada línea se busca su definición en la plantilla por `auxiliar1`:

```java
DetallePlantilla dp = detallePlantillaDaoService
        .selectByPlantillaYAuxiliar(idPlantilla, RhhLineaAsiento.SUELDOS_POR_PAGAR);
DetalleAsiento da = new DetalleAsiento();
da.setPlanCuenta(dp.getPlanCuenta());
da.setNumeroCuenta(dp.getPlanCuenta().getCuentaContable());
da.setNombreCuenta(dp.getPlanCuenta().getNombre());
da.setDescripcion(dp.getDescripcion());
if (dp.getMovimiento() == 1) { da.setValorDebe(valor); da.setValorHaber(0.0); }
else                          { da.setValorDebe(0.0);  da.setValorHaber(valor); }
```

Hay que **añadir el método al DAO**, que hoy solo tiene `selectByPlantilla` y
`selectByIdPlanCuenta`:

```java
DetallePlantilla selectByPlantillaYAuxiliar(Long idPlantilla, int auxiliar1) throws Throwable;
```

El desglose DEBE/HABER de los cuatro asientos está en §5 del maestro y en el script 09.

### 6.4 Cuadre

`Σ DEBE = totalIngresos + totalPatronal` y `Σ HABER = totalDescuentos + totalPatronal + neto`;
como `neto = totalIngresos − totalDescuentos`, cuadra por construcción. Aun así hay que
comprobarlo con `RedondeoNomina` **antes** de llamar a `generarAsiento` y ajustar la diferencia por
redondeo (menor a `CFNMTLCD`) contra la línea de cuadre. Sin esa comprobación previa, el usuario
recibe el `IncomeException` genérico de `validaDebeHaber`, que no le dice nada útil.

### 6.5 Pago

`OrdenPagoNominaService` genera `RDPG` y `DRPG` resolviendo la cuenta desde `CBEM` (o el reparto
por `CBEMPRCN`), produce el archivo bancario, y al confirmar la acreditación dispara el asiento
de pago con `ModuloSistema.TESORERIA` y crea un `TSR.EGRS` consolidado enlazado por
`RDPGEGRSCDGO`, para que la conciliación bancaria pueda casarlo con el extracto.

**No construir sobre `DocumentoPago`, `MontoAprobacion` ni `DocumentoCxp`**: están marcados como
deprecados en `docs/pendientes/PLAN_IMPLEMENTACION.md`.

### Estado de ejecución — código entregado el 2026-08-19

Entregadas las entidades `RDPG` y `DRPG` con sus siete archivos cada una,
`ContabilizacionNominaService` completo, `GeneracionOrdenPagoService`, la consulta
`selectByPlantillaYAuxiliar` en el DAO de `DetallePlantilla`, la ampliación de
`ConfiguracionNomina` con la cuenta marcadora y los cinco endpoints de proceso. **Dos huecos del
modelo quedan reportados y sin inventar: el formato del archivo bancario y el titular del egreso
de tesorería.**

Los nombres de propiedad de `RDPG` y `DRPG` son los del anexo «orden de pago (fase 6)» del
contrato, sin desviaciones.

#### Un nombre que cambió: `GeneracionOrdenPagoService`

El §6.5 lo llamaba `OrdenPagoNominaService`, pero ese nombre lo ocupa el CRUD de `RDPG`, que el
checklist por entidad exige. El proceso vive aparte, con el mismo criterio con que
`ProcesoNominaService` convive con `NominaService` y `GeneracionRolPagoService` con
`RolPagoService`.

#### `CTBNCDGO` apunta a una PK que se llama distinto

`RHH.RDPG.CTBNCDGO` referencia `TSR.CNBC`, cuya PK es `CNBCCDGO`. El DDL del script 04 no
declara la FK —solo el comentario—, así que la entidad lo resuelve con
`referencedColumnName = "CNBCCDGO"` explícito. Sin eso Hibernate habría buscado una columna
`CTBNCDGO` en `CNBC` y el arranque habría fallado.

#### Cómo se arma cada asiento

El proceso acumula en un mapa `código de línea del rubro 214 → valor` y solo entonces resuelve
las cuentas. Separar el cálculo del armado tiene una consecuencia útil: **una línea que suma
cero no entra en el asiento ni exige cuenta configurada.**

En la parametría de ASOPREP eso afecta a las líneas **16 y 17** —«fondos de reserva por pagar» y
«décimos por pagar»—, que quedan vacías: con la modalidad MENSUALIZADO esos valores ya viajan
dentro del neto de la línea 18. Existen en la plantilla para las empresas que los paguen por
separado, y si se exigiera cuenta para ellas la contabilización se bloquearía por un rubro que
la empresa no aplica.

La clasificación de cada renglón se hace **por `CPNMROLM`**, nunca por el código alterno ni por
la terna, igual que en el motor:

| Al DEBE | De dónde sale |
|---|---|
| 1 Gasto sueldos y salarios | Ingresos sin línea propia: sueldo, bonos, comisiones, subsidios |
| 2 Gasto horas extra | Roles 9, 10 y 11 |
| 3 Gasto aporte patronal IESS | Rol 2 |
| 4 Gasto IECE y SECAP | Roles 3 y 4 |
| 5, 6, 7 | Roles 5, 6 y 7 |

| Al HABER | De dónde sale |
|---|---|
| 10 IESS aporte personal | Rol 1 |
| 11 IESS aporte patronal | Roles 2, 3 y 4 — **los tres**: el IESS recauda el IECE y el SECAP en la misma planilla |
| 12 IESS préstamos | Roles 12 y 13 |
| 13 SRI retención | Rol 8 |
| 14 Cuentas por cobrar empleados | Roles 14 y 15, y cualquier descuento sin rol |
| 15 Retenciones judiciales | Rol 16 |
| 18 Sueldos por pagar | El neto |

El asiento de provisiones sale de `PVNM`: cada tipo aporta su gasto al DEBE y su provisión por
pagar al HABER por el mismo valor, de modo que cuadra por construcción. El tipo 5
`APORTE_PATRONAL` del rubro 206 no tiene línea y se omite con traza — sigue sin uso, como quedó
decidido.

#### El cuadre, comprobado antes de llamar

Por construcción `DEBE = ingresos + patronal` y `HABER = descuentos + patronal + neto`, iguales
porque `neto = ingresos − descuentos`. Aun así se comprueba con `RedondeoNomina.suma` y
`sonIguales` **antes** de `generarAsiento`: sin eso el usuario recibe el mensaje genérico de
`validaDebeHaber`, que enumera las líneas pero no dice cuál falta. Una diferencia dentro de
`CFNMTLCD` se ajusta contra la línea de cuadre —el neto en el rol, las vacaciones por pagar en
las provisiones, que es la única provisión que existe siempre—; una mayor se rechaza aquí, con
el importe exacto en el mensaje.

#### La cuenta marcadora

`ConfiguracionNomina` no mapeaba `CFNMCTMR`, la columna que agregó el script 13. Se añadió como
**`cuentaMarcadora`** (`Long`), y `validarCuentasContables` la lee de ahí. **El 9678 no aparece
en ninguna parte del código.**

Si la columna está en nulo el servicio **lanza** en vez de suponer un valor: sin cuenta marcadora
ninguna línea se reconocería como pendiente y el sistema emitiría asientos cuadrados con todas
las líneas contra la misma cuenta. Es exactamente el fallo que este control existe para evitar,
y un valor por defecto lo habría reintroducido por la puerta de atrás.

`validarCuentasContables` solo revisa **las líneas que el período usa de verdad**, y sigue
devolviendo lista vacía sin comprobar nada en modo histórico.

#### El interruptor, intacto

No se tocó. El modo histórico sigue avanzando el período a `CONTABILIZADO` sin asiento, con
`PRDNASNT` en nulo a propósito. Se extendió el mismo criterio a los otros dos asientos:
provisiones devuelve `null`, y el pago registra la fecha de acreditación —que es un hecho— pero
no emite asiento.

`previsualizar` **sí funciona en modo histórico**: ahí es la única forma de ver qué asiento se
emitiría cuando el período pase a productivo, y marca en el nombre de la cuenta las líneas que
siguen con el marcador.

#### El reparto entre cuentas del empleado

Con una sola cuenta se acredita todo allí sin mirar el porcentaje —evita que un `CBEMPRCN` mal
cargado parta un pago que no se reparte—. Con varias, se reparte por `CBEMPRCN` y **el residuo
del redondeo va a la cuenta principal**: sin ese ajuste la suma del detalle no sería el neto y el
asiento de pago no cuadraría contra el rol.

Los cinco campos de snapshot se copian al generar y no se releen nunca.

Un empleado sin ninguna cuenta bancaria activa **detiene la orden entera** con su nombre en el
mensaje: emitir una orden a la que le falta gente es peor que no emitirla, porque el descuadre
se descubre en el banco.

#### `contabilizarLiquidacion` queda bloqueado, y lanza diciéndolo

El asiento de liquidación necesita clasificar cada rubro del finiquito en su línea del rubro 214,
y para eso hacen falta dos mapeos que hoy no existen: `DetalleLiquidacion` no declara
`CPNMCDGO` —ni `TMLQTPCN`, `TMLQBSCL`, `TMLQDIAS`, `TMLQORDN`— y `Liquidacion` no declara las
catorce columnas del script 05, entre ellas el desahucio, la indemnización y la jubilación
patronal, que son **tres líneas del asiento**. Cerrar esos mapeos es trabajo de la fase 8 por
decisión del dueño del modelo, así que el método lanza `IncomeException` explicando qué falta en
vez de emitir un asiento incompleto. El resto de la fase 6 está operativo.

#### Los dos huecos del modelo

**1 · El formato del archivo bancario.** No lo tenemos y **no hay dónde guardarlo**:
`RHH.FMRC`/`DFMR` describen el archivo de **entrada** del biométrico, no una salida.
`generarArchivoBancario` lanza `IncomeException` explicando qué falta, en vez de escribir un
formato quemado. Todo lo demás de la orden funciona: el detalle se genera, se consulta y se
contabiliza.

Lo que haría falta parametrizar, para cuando se decida la tabla: nombre y banco del formato;
tipo (ancho fijo o delimitado) y delimitador; líneas de cabecera y de pie con sus literales;
por campo, el orden, la posición o el par inicio/longitud, el relleno (izquierda o derecha, con
qué carácter), el formato de importe (con o sin punto decimal, número de decimales) y el de
fecha; y el mapa de tipo de cuenta del banco. Es la misma forma de `FMRC`/`DFMR` pero de salida.

**2 · El titular del egreso de tesorería.** `TSR.EGRS` exige un `Titular` y un `ProductoPago`, y
la nómina no tiene ninguno de los dos: los empleados no son titulares de tesorería —`RHH.MPLD` y
`CRD.ENTD` siguen separados por decisión del maestro— y no existe un producto de pago de nómina.
Elegir uno cualquiera metería datos válidos del dominio equivocado en la conciliación bancaria,
que es peor que dejar el enlace vacío. **`RDPG.EGRSCDGO` queda en nulo**; el asiento de pago sí se
emite, así que la contabilidad está completa y lo único que falta es el enlace para conciliar.

#### Endpoints nuevos

| Método | Ruta | Cuerpo | Devuelve |
|---|---|---|---|
| POST | `/rest/prdn/contabilizarProvisiones/{idPeriodo}` | `?usuarioRegistro=` | `Asiento` o 204 |
| POST | `/rest/rdpg/generar` | `{idPeriodo, idCuentaBancaria, usuarioRegistro}` | `OrdenPagoNomina` |
| GET | `/rest/rdpg/archivoBancario/{id}` | — | archivo binario (hoy error explicado) |
| POST | `/rest/rdpg/confirmar/{id}` | `{fechaAcreditacion, usuarioRegistro}` | `OrdenPagoNomina` |

Más el CRUD estándar de `/rest/rdpg` y `/rest/drpg`, y
`/rest/prdn/previsualizarAsiento/{idPeriodo}/{tipo}`, que ya estaba en el contrato y ahora tiene
implementación: tipo 1 el rol, tipo 2 las provisiones.

---

## Fase 7 · Asistencia

`FMRC`, `DFMR`, `CRMR` con sus capas; ampliar `Marcaciones` y `ResumenNomina`.

`ImportacionMarcacionesServiceImpl` copia el patrón de
`ejb/tsr/serviceImpl/ImportacionExtractoBancarioServiceImpl.java`, que ya resuelve
previsualizar/confirmar con control antiduplicado por hash.

Reglas del parser:

1. Leer `FMRC` y sus `DFMR` ordenados. Calcular SHA-256 del stream; si ya existe un `CRMR` con
   ese hash y estado distinto de ANULADO, lanzar `IncomeException`.
2. Saltar `FMRCLNCB` líneas de cabecera y `FMRCLNPI` de pie.
3. Extraer cada campo por `DFMRPSCN` (delimitado) o `DFMRINCO`/`DFMRLNGT` (ancho fijo). Parsear
   fecha y hora con el patrón de `FMRCFRFH`, o `FMRCFRFC` + `FMRCFRHR`.
4. Traducir el tipo de marcación con `DFMRMPEO` (formato `origen=destino;origen=destino`).
5. Emparejar empleado por `MPLDCDBM`, con respaldo en `MPLDIDNT`. Sin coincidencia, la línea va
   a error **sin abortar el archivo**.
6. Deduplicar por `(MPLDCDGO, MRCCFCHH)` — los relojes repiten marcaciones. Las duplicadas se
   cuentan en `CRMRLNDP`.
7. Todo en una transacción `REQUIRED`: entra el archivo entero o no entra nada. Los errores de
   línea se acumulan en `CRMRLGGO`, no revierten.

`ConsolidacionMarcacionesService` agrupa por `(empleado, fecha)`: la primera marcación es
entrada, la última salida, y los pares intermedios se restan como almuerzo o permiso. Un número
impar de marcaciones marca el `RSMN` como inconsistente para revisión manual. El turno teórico
sale de `CNTE.CNTETRNO → DTLL` del día de la semana.

- `atrasoMinutos = max(0, entradaReal − entradaTeorica − TRNOMNTS)`
- Suplementarias (50 %): exceso sobre la jornada en día laborable hasta las 24h00, con tope
  `PRNMHRMX` diario y `PRNMHRSX` semanal; el exceso se marca `HREXEXCP='S'`.
- Extraordinarias (100 %): entre 24h00 y 06h00, sábados, domingos y feriados.
- Recargo nocturno (25 %): jornada **ordinaria** entre 19h00 y 06h00. Es un recargo sobre la hora
  ordinaria, no una hora extra.

**El formato del archivo del biométrico es un insumo pendiente.** El diseño lo absorbe: cuando
llegue la muestra, se crea un `FMRC` con sus `DFMR` y no se toca código.

### Estado de ejecución — código entregado el 2026-08-19

Entregados: las entidades `FMBN` y `DFMB` con sus siete archivos cada una y
`generarArchivoBancario` reescrito para leerlas —lo que cierra la fase 6—; la entidad `CRMR`
con sus siete archivos; las ampliaciones de `Marcaciones` y `ResumenNomina` con los nombres
ratificados del anexo; `ImportacionMarcacionesService` con las siete reglas;
`ConsolidacionMarcacionesService`; y los cinco endpoints de proceso.

#### El archivo bancario, cerrado

`generarArchivoBancario` ya no bloquea: lee `RHH.FMBN` y sus `RHH.DFMB`. Cuando la empresa no
tiene formato activo, el mensaje dice que **falta crearlo**, no que falte código.

Once campos del rubro 224, cabecera y pie como plantillas con `{FECHA}`, `{CONTADOR}`,
`{TOTAL}`, `{EMPRESA}` y `{SECUENCIAL}`. En ancho fijo un valor más largo que la longitud **se
recorta**: una línea más larga de lo debido descuadra todas las columnas siguientes y el banco
rechaza el archivo entero. El importe sin separador decimal va en centavos corridos, que es lo
que piden casi todos los formatos de acreditación masiva.

**Un hueco que aparece al implementarlo:** el campo `CODIGO_DEL_BANCO` (alterno 6) sale hoy del
snapshot `DRPGBNCO`, que guarda el **nombre** del banco. `TSR.BNCO` no tiene código de
institución —solo `BNCOCDGO`, que es la PK interna, y `BNCONMBR`—. Si el banco real pide el
código de la Superintendencia, hace falta una columna en `TSR.BNCO` y llevarla al snapshot de
`DRPG`. Para una empresa que paga a un solo banco destino, `LITERAL_FIJO` ya lo resuelve.

#### Las siete reglas del parser, y dónde vive cada una

| Regla | Dónde |
|---|---|
| 1 · Formato, campos y hash antiduplicado | `procesa`, con `selectVigenteByHash` |
| 2 · Saltar cabecera y pie | `procesa`, con `FMRCLNCB` y `FMRCLNPI` |
| 3 · Extraer por posición o por inicio y longitud | `extraeCampos` y `armaFechaHora` |
| 4 · Traducir el tipo con `DFMRMPEO` | `tipoMarcacion` |
| 5 · Emparejar por `MPLDCDBM`, respaldo `MPLDIDNT` | `localizaEmpleado` |
| 6 · Deduplicar por (empleado, fecha-hora) | `procesa`, contra la base **y dentro del archivo** |
| 7 · Una línea mala no aborta el archivo | El `catch` del bucle, que acumula en el log |

La regla 6 deduplica también **dentro del propio archivo**: un reloj repite la misma marcación
en dos líneas del mismo fichero cuando alguien pasa el dedo dos veces, y comprobar solo contra
la base habría dejado pasar la segunda.

**El archivo se lee entero en memoria** porque el hash y el parseo lo recorren dos veces. Un mes
de veinticinco empleados son unas mil quinientas líneas: cabe de sobra, y evita depender de que
el stream admita `reset`, que con un multipart no está garantizado.

**Previsualizar y confirmar comparten todo el recorrido**; lo único que cambia es si al final se
persiste. Es lo que garantiza que lo que el usuario ve en la previsualización es exactamente lo
que va a entrar.

#### Dos decisiones del parser que conviene conocer

- **Una marcación sin hora se rechaza**, en vez de suponer medianoche: suponerla produciría una
  jornada de cero horas sin avisar.
- **Un tipo de marcación sin mapeo entra como ENTRADA** en vez de perder la línea. La
  consolidación reordena el día por hora de todas formas, así que el tipo es informativo.

#### La anulación tiene un freno

`anular` **rechaza la anulación si alguna marcación del lote ya se consolidó** en un resumen
diario: retirarla dejaría el resumen apoyado en datos que ya no existen. Hay que rehacer la
consolidación de esos días primero.

#### La consolidación

Agrupa por `(empleado, fecha)`: la primera marcación es la entrada, la última la salida, y los
pares intermedios se restan como almuerzo o permiso. **Un número impar marca el resumen como
inconsistente y no se adivina nada**: el sistema no puede saber si falta la salida, si sobra una
repetida o si alguien salió sin marcar, y las tres suposiciones producen horas que después se
pagan.

Las tres clases de hora, con sus recargos leídos de `PRNM`:

| Clase | Cuándo | Recargo |
|---|---|---|
| Suplementaria | Exceso sobre la jornada en día laborable, hasta las 24h00 | `PRNMRCSP` |
| Extraordinaria | Después de medianoche, y **todo** lo trabajado en día no laborable | `PRNMRCEX` |
| Recargo nocturno | Jornada **ordinaria** entre 19h00 y 06h00 | `PRNMRCNC` |

El recargo nocturno no es una hora extra: es un recargo sobre la hora ordinaria, y por eso se
cuenta aparte y no se suma a las suplementarias.

**El tope `PRNMHRMX` no recorta las horas**, solo avisa por traza. La hora se trabajó y hay que
pagarla; exceder el tope es una infracción laboral que la empresa tiene que ver, no un motivo
para pagar de menos.

**Las 19h00 y las 06h00 sí están en el código**, como constantes de la clase con su cita del
Art. 49 del Código del Trabajo. No es un valor normativo escondido: es una definición legal, no
un parámetro de la empresa —si cambiara, cambia la ley, no un `UPDATE`—. Los porcentajes, que sí
son parámetro, salen todos de `PRNM`. Se reporta por si el criterio se prefiere al revés.

**Sin turno no se inventa un horario:** el resumen sale con las horas trabajadas y sin atraso,
que es lo único que se puede afirmar.

#### El `FMRC` sintético de prueba

Creado en la base con el CRUD que ya existía —formato 1, delimitado por coma, una línea de
cabecera y una de pie, cinco campos— y con `docs/logica-negocio/rhh/muestra-marcaciones-sintetica.txt`
como archivo. Al empleado de prueba se le asignó el código biométrico `7`.

La muestra ejercita las siete reglas a propósito: un día completo con almuerzo, un día con una
marcación repetida, un día que termina a las 20h40 —franja nocturna—, un día con una sola
marcación —inconsistente—, una línea de un empleado inexistente y una con la hora ilegible.
**Esperado: 12 líneas, 9 ok, 2 con error, 1 duplicada**, del 05 al 08 de enero de 2026.

No se pudo ejecutar todavía: `crmr` devuelve 404 porque las clases nuevas no están desplegadas.
Queda listo para correr en cuanto se recompile.

#### El 405 de `hrex/aprobar`, confirmado

Comprobado contra el desplegado: `POST /rest/hrex/aprobar` devuelve **405**, exactamente como
documentó la prueba del frontend — la ruta `/{id}` del CRUD capturaba la llamada. El endpoint
nuevo, `List<Long>` más `?usuarioRegistro=`, lo resuelve al desplegarse.

#### Los dos defectos de `CargaMarcacionesRest` — corregidos el 2026-08-20

Los encontró el frontend y se verificaron en el código. El primero dejaba **los dos endpoints de
importación inalcanzables**, así que también bloqueaba la prueba del `FMRC` sintético.

1. **`idFormato` e `idEmpresa` se declaraban `@FormParam("...") Long`.** Una conversión de
   `@FormParam` que falla la rechaza RESTEasy **antes de despachar el método**: devuelve 400 sin
   cuerpo y sin dejar traza, que es exactamente lo observado —la línea
   `LLEGA AL SERVICIO previsualizar` no aparecía en el log—. Los dos precedentes multipart de la
   casa los declaran `String` y parsean dentro (`ExtractoBancarioRest:194` y
   `SaldoAperturaRest:144`); ahora `crmr` hace lo mismo, con un `parseId(String)` privado y un
   400 **con mensaje** cuando el campo falta o no es numérico. El diagnóstico del frontend vino
   con dos controles ejecutados con el mismo `curl` —`exbc/importar/validar` y `slap/cargar`,
   ambos llegan al servicio—, así que la diferencia estaba aislada en la firma.
2. **Faltaba `URLDecoder.decode(nombreArchivo, StandardCharsets.UTF_8)`**, que
   `ExtractoBancarioRest:211` sí hace con su comentario sobre el charset del proveedor de
   multipart. Sin él un archivo del reloj con tildes o ñ queda corrupto en `CRMRNMAR`. Es
   cosmético —el nombre es trazabilidad, no clave; la deduplicación va por hash y por
   `(empleado, fecha-hora)`— pero el precedente ya estaba escrito. **El frontend alinea su
   cliente con `encodeURIComponent` en la misma pasada.**

Se barrió el resto de la capa REST buscando la misma familia: **no hay ningún otro `@FormParam`
con tipo distinto de `String` o `InputStream`** en `ws/rest`. Era el único caso.

De paso, los dos métodos ganaron la guarda de archivo ausente que ya tenían los dos precedentes,
y la traza de entrada ahora imprime formato y empresa además del nombre del archivo.

#### Endpoints nuevos

| Método | Ruta | Cuerpo | Devuelve |
|---|---|---|---|
| POST | `/rest/crmr/previsualizar` | multipart: `archivo`, `archivoNombre`, `idFormato`, `idEmpresa` — **los dos ids viajan como texto** | `ResultadoImportacionMarcaciones` |
| POST | `/rest/crmr/confirmar` | igual, más `?usuarioRegistro=` | `ResultadoImportacionMarcaciones` |
| POST | `/rest/crmr/anular/{idCarga}` | `{motivo, usuarioRegistro}` | 200 |
| POST | `/rest/rsmn/consolidar` | `{desde, hasta, usuarioRegistro}` | número de resúmenes |
| POST | `/rest/hrex/aprobar` | `List<Long>`, `?usuarioRegistro=` | número de horas aprobadas |

Más el CRUD estándar de `/rest/crmr`, `/rest/fmbn` y `/rest/dfmb`.

---

## Fase 8 · Liquidación

`LiquidacionHaberesService` con `simular`, `calcular`, `aprobar`, `ejecutarSalida` y
`generarActaFiniquitoSut`. Cada rubro del finiquito genera un `TMLQ` con su `CPNMCDGO`
(conceptos 60 a 67 del script 08).

Reglas, todas leídas de `PRNM` y `CSTR`:

- Remuneración pendiente = `CNTESLRB × díasDelMesTrabajados / PRNMDIAS`.
- Décimos proporcionales y vacaciones no gozadas, vía `BeneficioSocialService`.
- Desahucio (`CSTRDSHC='S'`): `PRNMDSPR/100 × última remuneración × años de servicio`.
- Despido intempestivo (`CSTRDSPD='S'`): si la antigüedad es menor a `PRNMDIAN` años →
  `PRNMDIMN × remuneración`; si no → `remuneración × años`, acotado entre `PRNMDIMN` y `PRNMDIMX`.
- Descuentos: aporte personal sobre lo imponible del último mes y cruce de todos los `DSRC`
  vigentes.
- Si el neto es negativo (deuda del trabajador) se registra igual y se marca para gestión de
  cobro; **no lanzar excepción**.

`ejecutarSalida` cierra el contrato, cesa al empleado, genera el aviso de salida al IESS, cancela
los `DSRC` vigentes cruzando saldos y caduca los `SLDV`.


### Estado de ejecución — código entregado el 2026-08-19

Entregados: los **tres huecos de mapeo cerrados** con los nombres del anexo del contrato,
`LiquidacionHaberesService` con sus cuatro operaciones, `contabilizarLiquidacion` real —que
estaba bloqueado justamente por esos mapeos— y cinco endpoints de proceso.

#### Los tres mapeos

`Liquidacion` gana las 14 columnas del script 05 y **`estado` pasa de `String` a `Long`** con el
rubro 196. Era la misma familia que `MRCCTPOO`: el script 05 hizo `DROP COLUMN` y la recreó como
`NUMBER`, así que la primera escritura habría dado `ORA-01722`. `DetalleLiquidacion` gana las 5,
con `conceptoNomina` a la cabeza. `tipoConcepto` y `baseCalculo` son **snapshot**, mismo criterio
que `RNGL`.

#### Qué rubro corresponde lo decide la causal

No hay ninguna lista en Java. `CSTRDSHC` decide el desahucio, `CSTRDSPD` la indemnización,
`CSTRJBPT` la jubilación patronal, `CSTRDCPR` los décimos proporcionales y `CSTRVCPR` las
vacaciones. Un cambio de criterio legal se resuelve con un `UPDATE` en `RHH.CSTR`.

Los importes de ley salen de `PRNM`: `PRNMDSPR` el desahucio (Art. 185), y `PRNMDIMN`,
`PRNMDIMX` y `PRNMDIAN` la indemnización (Art. 188).

#### Lo que no se recalcula aquí

Los décimos y las vacaciones se piden a `BeneficioSocialService` y a
`AcreditacionVacacionesService`, que ya los saben calcular y están congelados. Reimplementarlos
habría creado una segunda verdad para el mismo número.

**La jubilación patronal entra en cero** cuando la causal la genera: el importe sale del estudio
actuarial, igual que la provisión. Calcularla con una fórmula propia contradiría esa decisión;
el rubro existe para que el usuario lo complete.

#### Un neto negativo se registra

A diferencia del rol, aquí **no lanza**: significa que el trabajador debe dinero a la empresa
—anticipos, préstamos internos— y ese saldo hay que registrarlo para gestionar su cobro, no
hacerlo desaparecer. Queda la traza con el nombre del empleado.

#### `ejecutarSalida` es el punto de no retorno

Exige la liquidación **aprobada**: ejecutarla sobre un finiquito que todavía se puede recalcular
dejaría al empleado cesante con un finiquito que cambia. Cierra el contrato con la fecha de
salida y su causal, pasa al empleado a **CESANTE** —que es lo que hace que el motor deje de
incluirlo—, genera el aviso de salida al IESS si la causal lo exige, cancela los descuentos
recurrentes vigentes —el saldo ya se cruzó en el finiquito; dejarlos vivos los volvería a
descontar de una nómina que no habrá— y caduca los saldos de vacaciones, ya pagados como rubro.

#### El asiento de liquidación

Cada rubro va a su línea del rubro 214 según el código alterno de su concepto: décimos y
vacaciones cancelan su provisión (40, 41, 42), el desahucio, la indemnización y la jubilación son
gasto (60, 61, 62), lo demás es gasto de sueldos de liquidación (63), los descuentos van a
cuentas por cobrar empleados (14) y el neto a liquidaciones por pagar (70), que es además la
línea de cuadre. **Es lo que `TMLQ.CPNMCDGO` hace posible**, y la razón por la que el método
estuvo bloqueado.

#### Los ocho rubros se localizan por `CPNMROLM` — resuelto el 2026-08-19

Se localizaban por `CPNMALTR` 60–67, que es lo que fijaba el §8. Lo reporté porque era la única
parte del módulo que no usaba el rol, y el dueño del modelo decidió extender el rubro:
**`sql/17_DELTA_ROLES_FINIQUITO.sql` añade los roles 23–30** al rubro 221 y se los asigna a los
conceptos 60–67.

**El matiz importa, porque no es el argumento que creó el rubro 221:** `CPNMALTR` **sí es
discriminante**, así que aquí nunca hubo riesgo de tomar el concepto equivocado. Lo que fallaba
era la regla 1 —60..67 en Java son valores de catálogo quemados— y la coherencia: trece sitios
por rol y uno por código alterno acaba en que alguien copia el patrón equivocado más adelante.
**Con esto no queda ningún `CPNMALTR` literal en el módulo.**

La única excepción viva y legítima sigue siendo la retención por servicios profesionales, que se
localiza por la terna porque no forma parte del cálculo ordinario de nómina.

El mapeo rol → línea del rubro 214 no cambió: lo único que cambió es por qué campo se localiza el
concepto. Los dos sitios son `conceptoPorRol` en el servicio y `lineaDeRubroFiniquito` en la
contabilización.

#### Endpoints nuevos

| Método | Ruta | Cuerpo | Devuelve |
|---|---|---|---|
| POST | `/rest/lqdc/simular` | `{idContrato, fechaSalida, idCausal}` | `ResultadoLiquidacion` |
| POST | `/rest/lqdc/calcular` | `{idContrato, fechaSalida, idCausal, observaciones, usuarioRegistro}` | `Liquidacion` |
| POST | `/rest/lqdc/aprobar/{id}` | `?usuarioRegistro=` | 200 |
| POST | `/rest/lqdc/ejecutarSalida/{id}` | `?usuarioRegistro=` | 200 |
| POST | `/rest/lqdc/contabilizar/{id}` | `?usuarioRegistro=` | `Asiento` — **añadido**, no estaba en el contrato |

#### El aporte al IESS del finiquito — corregido el 2026-08-20

Lo encontró el frontend: `simular` devolvía **`totalDescuentos: 0`** en las tres causales sobre un
contrato con `aportaIess = 'S'`. El hueco era del catálogo y del código a la vez: **el script 17
creó los ocho roles del finiquito y los ocho eran de tipo ingreso**; el lado del descuento no
existía en ninguna parte.

**El mecanismo, que es lo que decidió la forma del arreglo.** `LiquidacionHaberesServiceImpl`
**no evalúa `CPNMTPCL` ni `CPNMBSCL`**: calcula cada importe en Java y `agrega(...)` recibe el
valor ya hecho, usando el concepto solo para su código alterno, su nombre y su `CPNMTPCN`. Por eso
no hizo falta ninguna bandera de base nueva ni un equivalente de `RNGLIMPN`:

- **clasificar el signo ya funcionaba** —el bucle de totales resta lo que sea `EGRESO` y
  `DetalleLiquidacion` hereda ese tipo—, y
- **la base ya estaba en la mano**: la regla normativa dice que el aporte se calcula *solo sobre
  la remuneración pendiente*, y `remuneracion` es una variable local calculada tres líneas antes.

Así que el arreglo es **una llamada**, justo detrás del rubro de remuneración pendiente, más el
rol **31** (`FINIQUITO_APORTE_PERSONAL`) y el concepto `CPNMALTR` 68 con `CPNMTPCN = 2` que crea
el script 22.

**Dos decisiones que conviene no deshacer:**

1. **El porcentaje se lee con la precedencia del motor** —`CPNMPRCN` manda, con caída a `PRNM`—
   mediante un `porcentajeAporte` local. No se reutiliza `porcentajeEnParametria` del motor: es
   privado, el motor está congelado y su tabla de roles no contempla el 31, así que devolvería
   `null`. **El concepto nace con `CPNMPRCN` en NULL a propósito**: con 9,45 escrito habría que
   acordarse de dos sitios el día que cambie la ley, y el mensual y el finiquito podrían quedar
   con tasas distintas sin que nada avisara.
2. **Si el concepto del rol 31 no existe, el rubro no se genera y se deja traza.** No se llama a
   `agrega` sin concepto: con `concepto == null` el rubro se etiquetaría como `INGRESO` por
   defecto y el aporte **sumaría al neto en vez de restarlo**, que es peor que no generarlo.

**El caso que manda:** Torres Chávez, salida 15-01-2026 por despido intempestivo, con 1.000,00 de
remuneración pendiente sobre 7.650,91 de ingresos totales. El aporte debe ser **94,50** —el 9,45 %
de 1.000,00— y el neto **7.556,41**. Si sale 723,01, el porcentaje se está aplicando al total.

#### Lo que falta de la fase 8

`generarActaFiniquitoSut`, que es un reporte de `rep/rhh/` y no un servicio. Va con los reportes
de la fase 9.

**Limitación conocida:** el finiquito **no calcula impuesto a la renta**. No hace falta para
enero, pero un finiquito con indemnizaciones altas sí puede generarlo.

**Pendiente para antes del primer commit:** `saldoDescuentos()` se suma a `descuentos` **fuera de
la lista de rubros**, así que reduce el neto sin aparecer como línea en `TMLQ`. El finiquito no es
una pantalla: es un documento que el trabajador firma y que se presenta al Ministerio del Trabajo,
y un acta que no cuadra consigo misma no se puede explicar. No bloquea enero —Torres Chávez no
tiene descuentos recurrentes—.
---

## Fase 9 · Salidas oficiales

Se sigue el patrón del módulo `rpr`, que ya hace exactamente esto para la Superintendencia de
Bancos: **persistir las filas generadas** en su tabla, **registrar cada corrida** en un
`EjecucionReporte` (`RPR.EJRC`, con mes, año, usuario, fecha, tipo, estado y observaciones), y
exportar desde ahí. Da trazabilidad y permite regenerar sin recalcular.

- **RDEP**: XML para el DIMM. El casillero de cada concepto sale de `CPNMRDEP`.
- **Formulario 107**: PDF individual por empleado, casilleros desde `CPNMF107`.
- **Planilla IESS**: códigos desde `CPNMIESS`. *(Pendiente definir si se necesita archivo de
  carga o solo reporte de control.)*
- **Formularios MDT/SUT**: décimo tercero, décimo cuarto, utilidades y acta de finiquito.
- **Utilidades** (`UtilidadService`): `base15 = utilidadContable × PRNMUTPR/100`, repartida en
  `PRNMUTDI` por días trabajados y `PRNMUTCG` por cargas familiares; el excedente sobre
  `PRNMUTSB × PRNMSBUU` por trabajador se transfiere al IESS. Es ingreso gravado de IR pero
  **no** materia gravada del IESS. Todo el flujo se construye aunque `CFNMAPUT='N'` en ASOPREP.


### Estado de ejecución — código entregado el 2026-08-19

Entregadas las tres entidades del anexo con sus siete archivos cada una,
`CalculoUtilidadesService`, `GeneracionSalidasOficialesService` con el RDEP y el registro de
presentación, el rubro 223 con sus siete constantes, cuatro endpoints de proceso y tres reportes
nuevos de `rep/rhh/`.

#### Otro nombre que cambió: `CalculoUtilidadesService`

El §9 lo llamaba `UtilidadService`, y ese nombre lo ocupa el CRUD de `RHH.UTLD`. Es la tercera
vez que pasa —`GeneracionRolPagoService`, `GeneracionOrdenPagoService`—, así que ya es patrón:
**cuando el plan nombra un servicio de proceso igual que su entidad, el proceso lleva el verbo
delante.**

#### Las utilidades

`baseTotal`, `basePorDias` y `basePorCargas`, con los porcentajes leídos de `PRNMUTPR`,
`PRNMUTDI` y `PRNMUTCG`, y el tope de `PRNMUTSB × SBU`. **Se construyó completo aunque
`CFNMAPUT='N'`**: el servicio existe y rechaza la operación mientras la bandera esté apagada,
mismo patrón que `ProvisionActuarialService`.

**Dos divisiones protegidas.** Sin días trabajados o sin cargas en toda la empresa, el
coeficiente correspondiente queda en cero en vez de dividir por cero. Lo de las cargas es un
caso real y frecuente —una empresa donde nadie declara—, y esa parte de la base simplemente no
tiene a quién ir; queda la traza diciéndolo.

**El excedente sobre el tope no se reparte entre los demás:** se acumula en `UTLDEXCD` y
`DTUTEXCD` porque la ley lo transfiere al IESS.

**`DTUTRTIR` se deja en cero aquí.** Las utilidades son ingreso gravado de renta pero **no**
materia gravada del IESS —por eso no hay ninguna columna de aporte en la tabla—, y la retención
la calcula la reproyección de IR del ejercicio, que es donde vive esa lógica.

#### Las salidas oficiales

El RDEP sale de `ACMN`: gravado de IR, aporte personal y retención del ejercicio, la misma
fuente con la que se calculó la nómina, así que el XML no puede divergir del rol. **Un empleado
sin ingreso gravado ni retención no se declara**: incluirlo con ceros ensucia el archivo y el
DIMM lo rechaza. Si ninguno tiene, el mensaje sugiere lo que casi siempre pasa — que los
períodos del ejercicio no se cerraron, y los acumulados solo se escriben al cerrar.

**La idempotencia vive en `selectSalida`**, con los nulos comparados con `is null` y no con
igualdad. En JPQL, como en SQL, `null = null` es desconocido y no verdadero: escrito con
igualdad, la salida anual y la consolidada no se encontrarían nunca y cada generación crearía
una fila nueva. Es la misma familia de fallo silencioso que el `||` de la franja nocturna.

**`registrarGeneracion` no toca la presentación al regenerar.** Si el archivo ya se presentó, el
hecho ocurrió; lo que delata que el contenido cambió después es el hash, no borrar la fecha.

#### Los tres reportes

| Reporte | Parámetros | Nota |
|---|---|---|
| `RPRT_ACTA_FNQT` | `P_LQDC_CODIGO` | Un rubro en cero no se imprime; el neto negativo sí, con su importe |
| `RPRT_F107_INDV` | `P_MPLD_CODIGO`, `P_ANIO` | Agrupa `RNGL` por `CPNMF107`; sin casillero, el concepto no declara |
| `RPRT_IESS_CNTR` | `P_PRDN_CODIGO` | Control, no archivo de carga. Complementa al resumen de aportes: aquel dice si el total está bien, este dónde está la diferencia |

Los tres compilan con el motor JasperReports del propio WAR, igual que los cuatro de la fase 5.

**Los reportes no escriben en `SLOF` por sí mismos** —se piden por el endpoint genérico—, así
que la pantalla llama a `/rest/slof/registrarGeneracion` con su tipo para dejar constancia.

#### Endpoints nuevos

| Método | Ruta | Cuerpo | Devuelve |
|---|---|---|---|
| POST | `/rest/utld/calcular` | `{idEmpresa, anio, utilidadContable, usuarioRegistro}` | `Utilidad` |
| POST | `/rest/slof/generarRdep/{anio}` | `?idEmpresa=&usuarioRegistro=` | XML del RDEP |
| POST | `/rest/slof/registrarGeneracion` | `{idEmpresa, tipoSalida, anio, mes, idEmpleado, nombreArchivo, usuarioRegistro}` | `SalidaOficial` |
| POST | `/rest/slof/registrarPresentacion/{id}` | `{fechaPresentacion, numeroComprobante, usuarioRegistro}` | `SalidaOficial` |

Más el CRUD estándar de `/rest/utld`, `/rest/dtut` y `/rest/slof`.

#### Lo que queda fuera y por qué

**Los formularios del MDT** (décimo tercero, décimo cuarto, utilidades) y el **archivo de carga
de la planilla IESS**. Los tres del MDT son listados de `LQBS` y `DTUT` con el formato que el
Ministerio publica cada año, y ese formato no lo tenemos; el del IESS es el insumo 4, también
pendiente. Los tipos 3 a 6 del rubro 223 existen para registrarlos en cuanto lleguen, y el
registro de generación y presentación ya funciona para ellos sin tocar código.
---

## Checklist por entidad nueva

Antes de dar por terminada cualquier tabla:

- [ ] Entidad JPA con `IDENTITY`, `@Table(schema="RHH")` y los dos `@NamedQuery`
- [ ] Constante en `NombreEntidadesRhh` idéntica al prefijo de los `@NamedQuery`
- [ ] `DaoService` (`@Local`) y `DaoServiceImpl` (`@Stateless`) con `obtieneCampos()` correcto
- [ ] `Service` (`@Local`) y `ServiceImpl` (`@Stateless`) con los seis métodos de `EntityService`
- [ ] `Rest` con `@Path` en minúsculas y los seis endpoints CRUD
- [ ] `System.out.println` de traza al inicio de cada método
- [ ] Dinero en `Double` redondeado con `RedondeoNomina`, estados como `Long` con rubro
- [ ] FK a `Empresa` si es tabla de parametrización
- [ ] Ningún valor normativo escrito en el código
