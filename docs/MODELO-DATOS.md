# MODELO DE DATOS — Mapeo Entidades JPA ↔ Tablas Oracle

> Documento generado automáticamente a partir de las anotaciones JPA de los paquetes
> `com.saa.model.*` (2026-08-07). Refleja exactamente lo declarado en el código:
> `@Table`, `@Column`, `@Id`, `@SequenceGenerator`, `@ManyToOne`/`@JoinColumn`, etc.
> Si se agregan o modifican entidades, regenerar/actualizar este archivo.

## Convenciones del mapeo

- **Tabla** = código de 4 letras mayúsculas dentro de su schema (`@Table(name="XXXX", schema="MOD")`).
- **Columna** = 8 caracteres: código de tabla + código de campo (`ASNTCDGO` = ASNT + CDGO).
- **PK**: campo `codigo` (`Long`) sobre columna `XXXXCDGO`; generada por `SEQUENCE`
  (`MOD.SQ_XXXXCDGO`, se indica bajo el título de cada entidad) o por `IDENTITY` (tablas más nuevas).
- **FK**: campo objeto con `@ManyToOne` + `@JoinColumn`; la columna FK es la indicada y referencia
  la PK de la entidad destino (se muestra `FK → Entidad (columna referenciada)`).
- **Tipos Java ↔ Oracle**: `Long`→`NUMBER`, `Double`→`NUMBER(18,2)` (decimales), `String`→`VARCHAR2(length)`,
  `LocalDate`→`DATE`, `LocalDateTime`/`Date`→`TIMESTAMP`/`DATE`.
- Cada módulo tiene una interface `NombreEntidades{Modulo}` con las constantes de nombre de entidad
  usadas para resolver las NamedQueries `{Entidad}All` / `{Entidad}Id`.
- Las clases marcadas *sin `@Entity`* son DTOs o clases auxiliares que no mapean a tabla.

## Resumen por módulo

| Paquete | Módulo | Schemas | Entidades |
|---|---|---|---|
| `com.saa.model.cnt` | Contabilidad | CNT, AOT | 29 |
| `com.saa.model.crd` | Créditos | CRD | 80 |
| `com.saa.model.cxc` | Cuentas por Cobrar | CBR | 49 |
| `com.saa.model.cxp` | Cuentas por Pagar | PGS | 60 |
| `com.saa.model.reporte` | Motor de reportes (DTOs) | — | 0 |
| `com.saa.model.rhh` | Recursos Humanos | RHH | 23 |
| `com.saa.model.rpr` | Reportes regulatorios/cartera | RPR | 33 |
| `com.saa.model.scp` | Sistema / Core | SCP, **CRD** (solo `Pais` → `CRD.PSSS`, ver su ficha) | 6 |
| `com.saa.model.tsr` | Tesorería | TSR | 59 |

---

## CNT — Contabilidad (`com.saa.model.cnt`)

Constantes de entidades: `NombreEntidadesContabilidad`

### `AnioMotor` → tabla **`AOT.ANIO`**

Secuencia PK: `AOT.SQ_ANIOCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ANIOCDGO` | **PK**, SEQUENCE |
| `anio` | `Long` | `ANIOANIO` |  |
| `estado` | `Long` | `ANIOESTD` |  |

### `Asiento` → tabla **`CNT.ASNT`**

Secuencia PK: `CNT.SQ_ASNTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ASNTCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `tipoAsiento` | `TipoAsiento` | `PLNTCDGO` | FK → `TipoAsiento` (PLNTCDGO), ManyToOne |
| `fechaAsiento` | `LocalDate` | `ASNTFCHA` |  |
| `numero` | `Long` | `ASNTNMRO` |  |
| `estado` | `Long` | `ASNTESTD` |  |
| `observaciones` | `String` | `ASNTOBSR` | length=2000 |
| `nombreUsuario` | `String` | `ASNTUSRO` | length=50 |
| `idReversion` | `Long` | `ASNTASRV` |  |
| `numeroMes` | `Long` | `ASNTPRDO` |  |
| `numeroAnio` | `Long` | `ASNTANOO` |  |
| `moneda` | `Long` | `ASNTMNDA` |  |
| `mayorizacion` | `Mayorizacion` | `MYRZCDGO` | FK → `Mayorizacion` (MYRZCDGO), ManyToOne |
| `rubroModuloClienteP` | `Long` | `ASNTRYYA` |  |
| `rubroModuloClienteH` | `Long` | `ASNTRZZA` |  |
| `fechaIngreso` | `LocalDateTime` | `ASNTFCPR` |  |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `rubroModuloSistemaP` | `Long` | `ASNTRYYB` |  |
| `rubroModuloSistemaH` | `Long` | `ASNTRZZB` |  |
| `numeroAlterno` | `String` | `ASNTNMAL` | length=100 |
| `numeroMesTipo` | `Long` | `ASNTNMMS` |  |
| `motivoAnulacion` | `String` | `ASNTMTAN` | length=1000 |
| `fechaAnulacion` | `LocalDateTime` | `ASNTFCAN` |  |
| `usuarioAnulacion` | `String` | `ASNTUSAN` | length=200 |

### `CentroCosto` → tabla **`CNT.CNCS`**

Secuencia PK: `CNT.SQ_CNCSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNCSCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `CNCSNMBR` | length=100 |
| `numero` | `String` | `CNCSNMRO` | length=50 |
| `tipo` | `Long` | `CNCSTPOO` |  |
| `nivel` | `Long` | `CNCSNVLL` |  |
| `idPadre` | `Long` | `CNCSCDPD` |  |
| `estado` | `Long` | `CNCSESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaInactivo` | `Date` | `CNCSFCDS` |  |
| `fechaIngreso` | `Date` | `CNCSFCHA` |  |

### `DesgloseMayorizacionCC` → tabla **`CNT.DTMC`**

Secuencia PK: `CNT.SQ_DTMCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTMCCDGO` | **PK**, SEQUENCE |
| `detalleMayorizacionCC` | `DetalleMayorizacionCC` | `MYCCCDGO` | FK → `DetalleMayorizacionCC` (MYCCCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `valorDebe` | `Double` | `DTMCDBEE` |  |
| `valorHaber` | `Double` | `DTMCHBRR` |  |
| `numeroCuenta` | `String` | `DTMCCTCN` | length=50 |
| `codigoPadreCuenta` | `Long` | `PLNNCDPD` |  |
| `nombreCuenta` | `String` | `PLNNNMBR` | length=100 |
| `tipoCuenta` | `Long` | `PLNNTPOO` |  |
| `nivelCuenta` | `Long` | `PLNNNVLL` |  |

### `DetalleAsiento` → tabla **`CNT.DTAS`**

Secuencia PK: `CNT.SQ_DTASCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTASCDGO` | **PK**, SEQUENCE |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `descripcion` | `String` | `DTASDSCR` | length=200 |
| `valorDebe` | `Double` | `DTASDBEE` |  |
| `valorHaber` | `Double` | `DTASHBRR` |  |
| `nombreCuenta` | `String` | `DTASNMCT` | length=200 |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroCuenta` | `String` | `DTASCNTA` | length=50 |

### `DetalleMayorAnalitico` → tabla **`CNT.DTMA`**

Secuencia PK: `CNT.SQ_DTMACDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTMACDGO` | **PK**, SEQUENCE |
| `mayorAnalitico` | `MayorAnalitico` | `MYANCDGO` | FK → `MayorAnalitico` (MYANCDGO), ManyToOne |
| `fechaAsiento` | `LocalDate` | `DTMAFCHA` |  |
| `numeroAsiento` | `Long` | `DTMANMRO` |  |
| `descripcionAsiento` | `String` | `DTMADSCR` | length=200 |
| `valorDebe` | `Double` | `DTMADBEE` |  |
| `valorHaber` | `Double` | `DTMAHBRR` |  |
| `saldoActual` | `Double` | `DTMASLAC` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `estadoAsiento` | `Long` | `ASNTESTD` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `nombreCosto` | `String` | `CNCSNMBR` | length=500 |
| `numeroCentroCosto` | `String` | `CNCSNMRO` | length=50 |

### `DetalleMayorizacion` → tabla **`CNT.DTMY`**

Secuencia PK: `CNT.SQ_DTMYCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTMYCDGO` | **PK**, SEQUENCE |
| `mayorizacion` | `Mayorizacion` | `MYRZCDGO` | FK → `Mayorizacion` (MYRZCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `saldoAnterior` | `Double` | `DTMYSLAN` |  |
| `valorDebe` | `Double` | `DTMYDBEE` |  |
| `valorHaber` | `Double` | `DTMYHBRR` |  |
| `saldoActual` | `Double` | `DTMYSLAC` |  |
| `numeroCuenta` | `String` | `DTMYCTCN` | length=50 |
| `codigoPadreCuenta` | `Long` | `PLNNCDPD` |  |
| `nombreCuenta` | `String` | `PLNNNMBR` | length=100 |
| `tipoCuenta` | `Long` | `PLNNTPOO` |  |
| `nivelCuenta` | `Long` | `PLNNNVLL` |  |

### `DetalleMayorizacionCC` → tabla **`CNT.MYCC`**

Secuencia PK: `CNT.SQ_MYCCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MYCCCDGO` | **PK**, SEQUENCE |
| `mayorizacionCC` | `MayorizacionCC` | `MYRCCDGO` | FK → `MayorizacionCC` (MYRCCDGO), ManyToOne |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroCC` | `String` | `MYCCNMRO` | length=50 |
| `nombreCC` | `String` | `MYCCNMBR` | length=100 |
| `saldoAnterior` | `Double` | `MYCCSLAN` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `saldoActual` | `Double` | `MYCCSLAC` |  |
| `valorDebe` | `Double` | `MYCCDBEE` |  |
| `valorHaber` | `Double` | `MYCCHBRR` |  |

### `DetallePlantilla` → tabla **`CNT.DTPL`**

Secuencia PK: `CNT.SQ_DTPLCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTPLCDGO` | **PK**, SEQUENCE |
| `plantilla` | `Plantilla` | `PLNSCDGO` | FK → `Plantilla` (PLNSCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `descripcion` | `String` | `DTPLDSCR` | length=200 |
| `movimiento` | `Long` | `DTPLMVMN` |  |
| `fechaDesde` | `LocalDateTime` | `DTPLFCIN` |  |
| `fechaHasta` | `LocalDateTime` | `DTPLFCFN` |  |
| `auxiliar1` | `Long` | `DTPLAXL1` |  |
| `auxiliar2` | `Long` | `DTPLAXL2` |  |
| `auxiliar3` | `Long` | `DTPLAXL3` |  |
| `auxiliar4` | `Long` | `DTPLAXL4` |  |
| `auxiliar5` | `Long` | `DTPLAXL5` |  |
| `estado` | `Long` | `DTPLESTD` |  |
| `fechaInactivo` | `LocalDateTime` | `DTPLFCDS` |  |

### `DetalleReporteContable` → tabla **`CNT.DTRP`**

Secuencia PK: `CNT.SQ_DTRPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTRPCDGO` | **PK**, SEQUENCE |
| `reporteContable` | `ReporteContable` | `RPRTCDGO` | FK → `ReporteContable` (RPRTCDGO), ManyToOne |
| `cuentaDesde` | `PlanCuenta` | `PLNNCDDS` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `numeroDesde` | `String` | `NUMCTADS` | length=50 |
| `nombreDesde` | `String` | `NMBCTADS` | length=100 |
| `cuentaHasta` | `PlanCuenta` | `PLNNCDHS` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `numeroHasta` | `String` | `NUMCTAHS` | length=50 |
| `nombreHasta` | `String` | `NMBCTAHS` | length=100 |
| `signo` | `Long` | `DTRPSGNO` |  |

### `DetalleReporteCuentaCC` → tabla **`CNT.RDTC`**

Secuencia PK: `CNT.SQ_RDTCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RDTCCDGO` | **PK**, SEQUENCE |
| `reporteCuentaCC` | `ReporteCuentaCC` | `RCNCCDGO` | FK → `ReporteCuentaCC` (RCNCCDGO), ManyToOne |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `nombreCosto` | `String` | `CNCSNMBR` | length=200 |
| `numeroCosto` | `String` | `CNCSNMRO` | length=50 |
| `debe` | `Double` | `RDTCDBEE` |  |
| `haber` | `Double` | `RDTCHBRR` |  |

### `HistAsiento` → tabla **`CNT.ASNH`**

Secuencia PK: `CNT.SQ_ASNHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ASNHCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `tipoAsiento` | `TipoAsiento` | `PLNTCDGO` | FK → `TipoAsiento` (PLNTCDGO), ManyToOne |
| `fechaAsiento` | `LocalDate` | `ASNHFCHA` |  |
| `numero` | `Long` | `ASNHNMRO` |  |
| `estado` | `Long` | `ASNHESTD` |  |
| `observaciones` | `String` | `ASNHOBSR` | length=200 |
| `nombreUsuario` | `String` | `ASNHUSRO` | length=50 |
| `idReversion` | `Long` | `ASNHASRV` |  |
| `numeroMes` | `Long` | `ASNHPRDO` |  |
| `numeroAnio` | `Long` | `ASNHANOO` |  |
| `moneda` | `Long` | `ASNHMNDA` |  |
| `histMayorizacion` | `HistMayorizacion` | `MYRHCDGO` | FK → `HistMayorizacion` (MYRHCDGO), ManyToOne |
| `rubroModuloClienteP` | `Long` | `ASNHRYYA` |  |
| `rubroModuloClienteH` | `Long` | `ASNHRZZA` |  |
| `fechaIngreso` | `LocalDateTime` | `ASNHFCPR` |  |
| `rubroModuloSistemaP` | `Long` | `ASNHRYYB` |  |
| `rubroModuloSistemaH` | `Long` | `ASNHRZZB` |  |
| `idAsientoOriginal` | `Long` | `ASNHASNT` |  |
| `numeroAlterno` | `String` | `ASNHNMAL` | length=100 |
| `numeroMesTipo` | `Long` | `ASNHNMMS` |  |

### `HistDetalleAsiento` → tabla **`CNT.DTAH`**

Secuencia PK: `CNT.SQ_DTAHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTAHCDGO` | **PK**, SEQUENCE |
| `histAsiento` | `HistAsiento` | `ASNHCDGO` | FK → `HistAsiento` (ASNHCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `descripcion` | `String` | `DTAHDSCR` | length=200 |
| `valorDebe` | `Double` | `DTAHDBEE` |  |
| `valorHaber` | `Double` | `DTAHHBRR` |  |
| `nombreCuenta` | `String` | `DTAHNMCT` | length=200 |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroCuenta` | `String` | `DTAHCNTA` | length=50 |

### `HistDetalleMayorizacion` → tabla **`CNT.DTMH`**

Secuencia PK: `CNT.SQ_DTMHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTMHCDGO` | **PK**, SEQUENCE |
| `histMayorizacion` | `HistMayorizacion` | `MYRHCDGO` | FK → `HistMayorizacion` (MYRHCDGO), ManyToOne |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `saldoAnterior` | `Double` | `DTMHSLAN` |  |
| `valorDebe` | `Double` | `DTMHDBEE` |  |
| `valorHaber` | `Double` | `DTMHHBRR` |  |
| `saldoActual` | `Double` | `DTMHSLAC` |  |
| `numeroCuenta` | `String` | `DTMHCTCN` | length=50 |
| `codigoPadreCuenta` | `Long` | `PLNNCDPD` |  |
| `nombreCuenta` | `String` | `PLNNNMBR` | length=100 |
| `tipoCuenta` | `Long` | `PLNNTPOO` |  |
| `nivelCuenta` | `Long` | `PLNNNVLL` |  |
| `mayorizacion` | `Mayorizacion` | `MYRZCDGO` | FK → `Mayorizacion` (MYRZCDGO), ManyToOne |

### `HistMayorizacion` → tabla **`CNT.MYRH`**

Secuencia PK: `CNT.SQ_MYRHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MYRHCDGO` | **PK**, SEQUENCE |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `MYRHFCHA` |  |
| `idMayorizacion` | `Long` | `MYRZCDGO` |  |

### `MatchCuenta` → tabla **`CNT.MTCH`**

Secuencia PK: `CNT.SQ_MTCHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MTCHCDGO` | **PK**, SEQUENCE |
| `empresaOrigen` | `Empresa` | `PJRQCDOR` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `cuentaOrigen` | `PlanCuenta` | `PLNNCDOR` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `empresaDestino` | `Empresa` | `PJRQCDFN` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `cuentaDestino` | `PlanCuenta` | `PLNNCDFN` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `estado` | `Long` | `MTCHESTD` |  |

### `MayorAnalitico` → tabla **`CNT.MYAN`**

Secuencia PK: `CNT.SQ_MYANCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MYANCDGO` | **PK**, SEQUENCE |
| `secuencial` | `Long` | `MYANSCNC` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `numeroCuenta` | `String` | `MYANCNTA` | length=50 |
| `nombreCuenta` | `String` | `MYANNMBR` | length=100 |
| `saldoAnterior` | `Double` | `MYANSLAN` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `observacion` | `String` | `MYANOBSR` | length=300 |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |

### `Mayorizacion` → tabla **`CNT.MYRZ`**

Secuencia PK: `CNT.SQ_MYRZCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MYRZCDGO` | **PK**, SEQUENCE |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `MYRZFCHA` |  |

### `MayorizacionCC` → tabla **`CNT.MYRC`**

Secuencia PK: `CNT.SQ_MYRCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MYRCCDGO` | **PK**, SEQUENCE |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `MYRCFCHA` |  |

### `NaturalezaCuenta` → tabla **`CNT.NTRL`**

Secuencia PK: `CNT.SQ_NTRLCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `NTRLCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `NTRLNMBR` | length=100 |
| `tipo` | `Long` | `NTRLTPOO` |  |
| `numero` | `Long` | `NTRLNMRO` |  |
| `estado` | `Long` | `NTRLESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `manejaCentroCosto` | `Long` | `NTRLCNCS` |  |

### `ParametrosBalance` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `fechaInicio` | `LocalDate` | `*(sin @Column)*` |  |
| `fechaFin` | `LocalDate` | `*(sin @Column)*` |  |
| `empresa` | `Long` | `*(sin @Column)*` |  |
| `codigoAlterno` | `Long` | `*(sin @Column)*` |  |
| `acumulacion` | `Integer` | `*(sin @Column)*` |  |
| `incluyeCentrosCosto` | `Boolean` | `*(sin @Column)*` |  |
| `reporteDistribuido` | `Boolean` | `*(sin @Column)*` |  |
| `eliminarSaldosCero` | `Boolean` | `*(sin @Column)*` |  |

### `ParametrosMayorAnalitico` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `fechaInicio` | `LocalDate` | `*(sin @Column)*` |  |
| `fechaFin` | `LocalDate` | `*(sin @Column)*` |  |
| `empresa` | `Long` | `*(sin @Column)*` |  |
| `cuentaInicio` | `String` | `*(sin @Column)*` |  |
| `cuentaFin` | `String` | `*(sin @Column)*` |  |
| `tipoDistribucion` | `Integer` | `*(sin @Column)*` |  |
| `centroInicio` | `String` | `*(sin @Column)*` |  |
| `centroFin` | `String` | `*(sin @Column)*` |  |
| `tipoAcumulacion` | `Integer` | `*(sin @Column)*` |  |

### `Periodo` → tabla **`CNT.PRDO`**

Secuencia PK: `CNT.SQ_PRDOCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRDOCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `mes` | `Long` | `PRDOMSSS` |  |
| `anio` | `Long` | `PRDOANNN` |  |
| `nombre` | `String` | `PRDONMBR` | length=100 |
| `estado` | `Long` | `PRDOESTD` |  |
| `idMayorizacion` | `Long` | `PRDOMYRZ` |  |
| `idDesmayorizacion` | `Long` | `PRDODSMY` |  |
| `idMayorizacionCierre` | `Long` | `PRDOMYCR` |  |
| `idDesmayorizacionCierre` | `Long` | `PRDODMCR` |  |
| `periodoCierre` | `Long` | `PRDOCRRE` |  |
| `primerDia` | `LocalDate` | `PRDOINCO` |  |
| `ultimoDia` | `LocalDate` | `PRDOFNN` |  |

### `PlanCuenta` → tabla **`CNT.PLNN`**

Secuencia PK: `CNT.SQ_PLNNCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PLNNCDGO` | **PK**, SEQUENCE |
| `naturalezaCuenta` | `NaturalezaCuenta` | `NTRLCDGO` | FK → `NaturalezaCuenta` (NTRLCDGO), ManyToOne |
| `cuentaContable` | `String` | `PLNNCNTA` | length=50 |
| `nombre` | `String` | `PLNNNMBR` | length=100 |
| `tipo` | `Long` | `PLNNTPOO` |  |
| `nivel` | `Long` | `PLNNNVLL` |  |
| `idPadre` | `Long` | `PLNNCDPD` |  |
| `estado` | `Long` | `PLNNESTD` |  |
| `fechaInactivo` | `LocalDate` | `PLNNFCDS` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaUpdate` | `LocalDate` | `PLNNFCHA` |  |

### `Plantilla` → tabla **`CNT.PLNS`**

Secuencia PK: `CNT.SQ_PLNSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PLNSCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `PLNSNMBR` | length=100 |
| `codigoAlterno` | `Long` | `PLNSCDAL` |  |
| `estado` | `Long` | `PLNSESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `observacion` | `String` | `PLNSOBSR` | length=500 |
| `fechaInactivo` | `LocalDateTime` | `PLNSFCDS` |  |
| `sistema` | `Long` | `PLNSSSTM` |  |

### `ReporteContable` → tabla **`CNT.RPRT`**

Secuencia PK: `CNT.SQ_RPRTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RPRTCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `nombreReporte` | `String` | `RPRTNMBR` | length=100 |
| `estado` | `Long` | `RPRTESTD` |  |
| `codigoAlterno` | `Long` | `RPRTALTR` |  |

### `ReporteCuentaCC` → tabla **`CNT.RCNC`**

Secuencia PK: `CNT.SQ_RCNCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RCNCCDGO` | **PK**, SEQUENCE |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `nombreCuenta` | `String` | `RCNCNMBR` | length=200 |
| `numeroCuenta` | `String` | `RCNCNMRO` | length=50 |
| `secuencia` | `Long` | `RCNCSCNC` |  |
| `saldoAnterio` | `Long` | `RCNCSLAN` |  |
| `debe` | `Long` | `RCNCDBEE` |  |
| `haber` | `Long` | `RCNCHBRR` |  |
| `saldoActual` | `Long` | `RCNCSLAC` |  |

### `RespuestaBalance` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `idEjecucion` | `Long` | `*(sin @Column)*` |  |
| `totalRegistros` | `Integer` | `*(sin @Column)*` |  |
| `fechaProceso` | `LocalDateTime` | `*(sin @Column)*` |  |
| `mensaje` | `String` | `*(sin @Column)*` |  |
| `exitoso` | `Boolean` | `*(sin @Column)*` |  |

### `RespuestaMayorAnalitico` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `secuencialReporte` | `Long` | `*(sin @Column)*` |  |
| `totalCabeceras` | `Integer` | `*(sin @Column)*` |  |
| `totalDetalles` | `Integer` | `*(sin @Column)*` |  |
| `fechaProceso` | `LocalDateTime` | `*(sin @Column)*` |  |
| `mensaje` | `String` | `*(sin @Column)*` |  |
| `exitoso` | `Boolean` | `*(sin @Column)*` |  |

### `Saldos` → tabla **`CNT.SLDS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `SLDSCDGO` | **PK**, IDENTITY |
| `planCuenta` | `Long` | `PLNNCDGO` | NOT NULL |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `valor` | `Double` | `SLDSVLRR` |  |
| `fechaIngreso` | `LocalDateTime` | `SLDSFCHN` | NOT NULL |
| `estado` | `Long` | `SLDSSTDO` | NOT NULL |

### `SubdetalleAsiento` → tabla **`CNT.SDAS`**

Secuencia PK: `CNT.SQ_SDASCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `SDASCDGO` | **PK**, SEQUENCE |
| `detalleAsiento` | `DetalleAsiento` | `DTASCDGO` | FK → `DetalleAsiento` (DTASCDGO), ManyToOne |
| `codigoActivo` | `String` | `SDASCDAC` | length=50 |
| `nombreBien` | `String` | `SDASNMBR` | length=200 |
| `categoria` | `String` | `SDASCTGR` | length=100 |
| `tipo` | `String` | `SDASTIPO` | length=100 |
| `fechaAdquisicion` | `LocalDate` | `SDASFCAD` |  |
| `costoAdquisicion` | `Double` | `SDASCSAD` |  |
| `mejorasCapitalizadas` | `Double` | `SDASMJCP` |  |
| `valorResidual` | `Double` | `SDASVLRS` |  |
| `baseDepreciar` | `Double` | `SDASBSDP` |  |
| `vidaUtilTotal` | `Integer` | `SDASVTMS` |  |
| `vidaUtilRemanente` | `Integer` | `SDASVRMS` |  |
| `porcentajeDepreciacion` | `Double` | `SDASPRDP` |  |
| `cuotaDepreciacion` | `Double` | `SDASCTDP` |  |
| `depreciacionAcumulada` | `Double` | `SDASDPAC` |  |
| `valorNetoLibros` | `Double` | `SDASVLNL` |  |
| `ubicacionGeneral` | `String` | `SDASUBGN` | length=150 |
| `ubicacionEspecifica` | `String` | `SDASUBES` | length=150 |
| `responsable` | `String` | `SDASRSPN` | length=150 |
| `estadoFisico` | `String` | `SDASESTF` | length=50 |
| `factura` | `String` | `SDASFACT` | length=150 |
| `observaciones` | `String` | `SDASOBSR` | length=200 |

### `TempReportes` → tabla **`CNT.DTMT`**

Secuencia PK: `CNT.SQ_DTMTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTMTCDGO` | **PK**, SEQUENCE |
| `secuencia` | `Long` | `DTMTSCRP` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `saldoCuenta` | `Double` | `DTMTSLAN` |  |
| `valorDebe` | `Double` | `DTMTDBEE` |  |
| `valorHaber` | `Double` | `DTMTHBRR` |  |
| `valorActual` | `Double` | `DTMTSLAC` |  |
| `cuentaContable` | `String` | `DTMTCTCN` |  |
| `codigoCuentaPadre` | `Long` | `PLNNCDPD` |  |
| `nombreCuenta` | `String` | `PLNNNMBR` |  |
| `tipo` | `Long` | `PLNNTPOO` |  |
| `nivel` | `Long` | `PLNNNVLL` |  |
| `mayorizacion` | `Mayorizacion` | `MYRZCDGO` | FK → `Mayorizacion` (MYRZCDGO), ManyToOne |
| `centroCosto` | `CentroCosto` | `DTMTCSCG` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `nombreCentroCosto` | `String` | `DTMTCSNB` |  |
| `numeroCentroCosto` | `String` | `DTMTCSNM` |  |

### `TipoAsiento` → tabla **`CNT.PLNT`**

Secuencia PK: `CNT.SQ_PLNTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PLNTCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `PLNTNMBR` | length=100 |
| `codigoAlterno` | `Long` | `PLNTCDAL` |  |
| `estado` | `Long` | `PLNTESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `observacion` | `String` | `PLNTOBSR` | length=500 |
| `fechaInactivo` | `Date` | `PLNTFCDS` |  |
| `sistema` | `Long` | `PLNTSSTM` |  |

---

## CRD — Créditos (`com.saa.model.crd`)

Constantes de entidades: `NombreEntidadesCredito`

### `Adjunto` → tabla **`CRD.ADJN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ADJNCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `idReferencia` | `Long` | `ADJNIDRF` |  |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `idSolicitudCambio` | `Long` | `ADJNISCA` |  |
| `tipoAdjunto` | `TipoAdjunto` | `TPDJCDGO` | FK → `TipoAdjunto` (TPDJCDGO), ManyToOne |
| `nombreArchivo` | `String` | `ADJNNMAR` | length=2000 |
| `urlArchivo` | `String` | `ADJNURLA` | length=2000 |
| `observacion` | `String` | `ADJNOBSR` | length=2000 |
| `mimeType` | `String` | `ADJNMMTY` | length=200 |
| `estado` | `Long` | `ADJNIDST` |  |
| `fechaRegistro` | `LocalDateTime` | `ADJNFCRG` |  |
| `usuarioRegistro` | `String` | `ADJNUSRG` | length=200 |

### `AfectacionValoresParticipeCarga` → tabla **`CRD.AVPC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `AVPCCDGO` | **PK**, IDENTITY |
| `novedadParticipeCarga` | `NovedadParticipeCarga` | `NVPCCDGO` | FK → `NovedadParticipeCarga` (NVPCCDGO), ManyToOne |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `detallePrestamo` | `DetallePrestamo` | `DTPRCDGO` | FK → `DetallePrestamo` (DTPRCDGO), ManyToOne |
| `valorCuotaOriginal` | `Double` | `AVPCVLCT` |  |
| `capitalCuotaOriginal` | `Double` | `AVPCVLCP` |  |
| `interesCuotaOriginal` | `Double` | `AVPCVLIN` |  |
| `desgravamenCuotaOriginal` | `Double` | `AVPCVLDG` |  |
| `valorAfectar` | `Double` | `AVPCVAFA` |  |
| `capitalAfectar` | `Double` | `AVPCCPAF` |  |
| `interesAfectar` | `Double` | `AVPCINAF` |  |
| `desgravamenAfectar` | `Double` | `AVPCDGAF` |  |
| `diferenciaTotal` | `Double` | `AVPCDFTL` |  |
| `diferenciaCapital` | `Double` | `AVPCDFCP` |  |
| `diferenciaInteres` | `Double` | `AVPCDFIN` |  |
| `diferenciaDesgravamen` | `Double` | `AVPCDFDG` |  |
| `fechaAfectacion` | `LocalDate` | `AVPCFCAF` |  |
| `usuarioRegistro` | `String` | `AVPCUSAR` | length=50 |
| `fechaCreacionRegistro` | `LocalDateTime` | `AVPCFCRG` |  |
| `observaciones` | `String` | `AVPCOBSR` | length=1000 |
| `estado` | `Long` | `AVPCESTD` |  |

### `Aporte` → tabla **`CRD.APRT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `APRTCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `contrato` | `Contrato` | `CNTRCDGO` | FK → `Contrato` (CNTRCDGO), ManyToOne |
| `tipoAporte` | `TipoAporte` | `TPAPCDGO` | FK → `TipoAporte` (TPAPCDGO), ManyToOne |
| `fechaTransaccion` | `LocalDateTime` | `APRTFCTR` |  |
| `glosa` | `String` | `APRTGLSA` | length=2000 |
| `valor` | `Double` | `APRTVLRR` |  |
| `valorPagado` | `Double` | `APRTVLPG` |  |
| `saldo` | `Double` | `APRTSLDO` |  |
| `idAsoprep` | `Long` | `APRTIDAS` |  |
| `fechaRegistro` | `LocalDateTime` | `APRTFCRG` |  |
| `usuarioRegistro` | `String` | `APRTUSRG` | length=50 |
| `estado` | `Long` | `APRTIDST` |  |

### `AporteAsoprep` → tabla **`CRD.APAS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `cuenta` | `Long` | `APASCNTA` | **PK** |
| `institucion` | `Long` | `APASNSTT` | **PK** |
| `producto` | `Long` | `APASPRDT` |  |
| `numeroCuenta` | `Long` | `APASNMRC` |  |
| `fechaApertura` | `LocalDate` | `APASFCHP` |  |
| `saldoCuenta` | `BigDecimal` | `APASSLDC` |  |
| `saldoAporte` | `BigDecimal` | `APASSLDA` |  |
| `saldoInteres` | `BigDecimal` | `APASSLDI` |  |
| `fechaUltProvision` | `LocalDate` | `APASFCHV` |  |
| `observaciones` | `String` | `APASOBSR` | length=60 |
| `estado` | `Long` | `APASSTDO` |  |
| `creadoPor` | `Long` | `APASCRDP` |  |
| `fechaCreado` | `LocalDateTime` | `APASFCHC` |  |
| `actualizadoPor` | `Long` | `APASACTP` |  |
| `fechaActualiza` | `LocalDateTime` | `APASFCHA` |  |
| `cliente` | `Long` | `APASCLNT` |  |
| `fechaLiquida` | `LocalDate` | `APASFCHL` |  |
| `fechaUltMovimiento` | `LocalDate` | `APASFCHM` |  |
| `regimen` | `Long` | `APASRGMN` |  |
| `valorUltAporte` | `BigDecimal` | `APASVLRA` |  |
| `numeroAporte` | `Long` | `APASNMAP` |  |
| `fechaRenuncia` | `LocalDate` | `APASFCHR` |  |
| `numeroSolicitud` | `String` | `APASNMSL` | length=30 |
| `tipoLiquidacion` | `Long` | `APASTPLQ` |  |
| `acumular` | `Long` | `APASACML` |  |

### `Auditoria` → tabla **`CRD.ADTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ADTRCDGO` | **PK**, IDENTITY |
| `fechaEvento` | `LocalDateTime` | `ADTRFCHA` |  |
| `sistema` | `String` | `ADTRSSTM` | length=50, NOT NULL |
| `modulo` | `String` | `ADTRMDLO` | length=100, NOT NULL |
| `accion` | `String` | `ADTRACCN` | length=50, NOT NULL |
| `entidad` | `String` | `ADTRNTDD` | length=100, NOT NULL |
| `idEntidad` | `String` | `ADTRNTID` | length=100 |
| `usuario` | `String` | `ADTRUSRO` | length=100 |
| `rol` | `String` | `ADTRRLL` | length=100 |
| `ipCliente` | `String` | `ADTRIPCL` | length=45 |
| `userAgent` | `String` | `ADTRAGNT` | length=255 |
| `motivo` | `String` | `ADTRRSN` | length=1000, NOT NULL |
| `nombreCampoAnterior` | `String` | `ADTRNMNA` | length=100 |
| `valorAnterior` | `Long` | `ADTRVLNA` |  |
| `nombreCampoNuevo` | `String` | `ADTRNMNN` | length=100 |
| `valorNuevo` | `Double` | `ADTRVLNN` |  |
| `fechaCreacion` | `LocalDateTime` | `ADTRFCIN` |  |

### `BaseInicialParticipes` → tabla **`CRD.BIPR`**

Secuencia PK: `CRD.ISEQ$$_87710`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numero` | `Long` | `BIPRNMRO` | **PK**, SEQUENCE |
| `nombre` | `String` | `BIPRNMBR` | length=200, NOT NULL |
| `cesantiaPatronal` | `Double` | `BIPRCSNP` | NOT NULL |
| `cesantiaPersonal` | `Double` | `BIPRCSPR` | NOT NULL |
| `cesantiaRetiroVoluntario` | `Double` | `BIPRCSRV` | NOT NULL |
| `jubilacionPatronal` | `Double` | `BIPRJBPT` | NOT NULL |
| `jubilacionPersonal` | `Double` | `BIPRJBPR` | NOT NULL |
| `jubilacionRetiroVoluntario` | `Double` | `BIPRJBRV` | NOT NULL |
| `pensionComplementaria` | `Double` | `BIPRPNSN` | NOT NULL |
| `rendimientoCesantiaPatronal` | `Double` | `BIPRRNCP` | NOT NULL |
| `rendimientoCesantiaPersonal` | `Double` | `BIPRRNPS` | NOT NULL |
| `rendimientoJubilacionPatronal` | `Double` | `BIPRRNJP` | NOT NULL |
| `rendimientoJubilacionPersonal` | `Double` | `BIPRRNJR` | NOT NULL |
| `totalGeneral` | `Double` | `BIPRTTLG` | NOT NULL |
| `idSaa` | `Long` | `BIPRIDSA` |  |
| `cedula` | `String` | `BIPRNMCD` |  |

### `BioProfile` → tabla **`CRD.BPRF`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `BPRFCDGO` | **PK**, IDENTITY |
| `identificacion` | `String` | `BPRFIDNT` | length=50 |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `BPRFFCHR` |  |
| `estado` | `Long` | `BPRFIDST` |  |

### `BotOpcion` → tabla **`CRD.BTPC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `BTPCCDGO` | **PK**, IDENTITY |
| `estado` | `Long` | `BTPCIDST` |  |
| `codigoPadre` | `Long` | `BTPCIDPC` |  |
| `nombre` | `String` | `BTPCNMBR` | length=50 |
| `numero` | `Long` | `BTPCNMRP` |  |

### `CambioAporte` → tabla **`CRD.CMBP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CMBPCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `sueldo` | `Double` | `CMBPSLDO` |  |
| `porcentajeCesantia` | `Long` | `CMBPPRCS` |  |
| `porcentajeJubilacion` | `Long` | `CMBPPRJB` |  |
| `estadoActual` | `Long` | `CMBPESAC` |  |
| `fechaSolicitud` | `LocalDateTime` | `CMBPFCSL` |  |
| `fechaAprobacion` | `LocalDateTime` | `CMBPFCAP` |  |
| `solicitudWeb` | `Long` | `CMBPESWB` |  |
| `usuarioIngreso` | `String` | `CMBPUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `CMBPFCIN` |  |
| `estado` | `Long` | `CMBPESTD` |  |

### `Canton` → tabla **`CRD.CNTN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNTNCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `CNTNNMBR` | length=2000 |
| `codigoAuxiliar` | `String` | `CNTNCDGX` | length=50 |
| `idEstado` | `Long` | `CNTNIDST` | NOT NULL |

### `CargaArchivo` → tabla **`CRD.CRAR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CRARCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `CRARNMBR` | length=2000 |
| `fechaCarga` | `LocalDateTime` | `CRARFCCR` |  |
| `usuarioCarga` | `Usuario` | `CRARUSCD` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `rutaArchivo` | `String` | `CRARRTAS` | length=2000 |
| `mesAfectacion` | `Long` | `CRARMSAF` |  |
| `anioAfectacion` | `Long` | `CRARANAF` |  |
| `totalSaldoActual` | `Double` | `CRARTTSA` |  |
| `totalInteresAnual` | `Double` | `CRARTTIA` |  |
| `totalValorSeguro` | `Double` | `CRARTTVS` |  |
| `totalDescontar` | `Double` | `CRARTTDS` |  |
| `totalCapitalDescontado` | `Double` | `CRARTTCD` |  |
| `totalInteresDescontado` | `Double` | `CRARTTID` |  |
| `totalSeguroDescontado` | `Double` | `CRARTTSD` |  |
| `totalDescontado` | `Double` | `CRARTTDO` |  |
| `totalCapitalNoDescontado` | `Double` | `CRARTCND` |  |
| `totalInteresNoDescontado` | `Double` | `CRARTIND` |  |
| `totalDesgravamenNoDescontado` | `Double` | `CRARTDND` |  |
| `estado` | `Long` | `CRARESTD` |  |
| `numeroTransferencia` | `Long` | `CRARNMTF` |  |
| `usuarioContabilidadConfirma` | `Usuario` | `CRARUSCC` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaAutorizacionContabilidad` | `LocalDateTime` | `CRARFCAC` |  |
| `usuarioAnulacion` | `Usuario` | `CRARUSAN` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `motivoAnulacion` | `String` | `CRARMTAN` | length=2000 |
| `fechaAnulacion` | `LocalDateTime` | `CRARFCAN` |  |

### `Cesantia` → tabla **`CRD.CSNT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CSNTCDGO` | **PK**, IDENTITY |
| `tipoCesantia` | `TipoCesantia` | `TPCSCDGO` | FK → `TipoCesantia` (TPCSCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `CSNTFCHA` |  |
| `idSolicitante` | `Long` | `CSNTIDSL` |  |
| `fechaLiquidacion` | `LocalDateTime` | `CSNTFCLQ` |  |
| `fechaSalida` | `LocalDateTime` | `CSNTFCSL` |  |
| `esFallecido` | `Long` | `CSNTESFL` |  |
| `aplicaDesgravamen` | `Long` | `CSNTAPDS` |  |
| `totalIngresos` | `Double` | `CSNTTTIN` |  |
| `totalEgresos` | `Double` | `CSNTTTEG` |  |
| `saldoPagar` | `Double` | `CSNTSLPG` |  |
| `saldoCobrar` | `Double` | `CSNTSLCB` |  |
| `fechaRegistro` | `LocalDateTime` | `CSNTFCRG` |  |
| `usuarioRegistro` | `String` | `CSNTUSRG` | length=50 |
| `estadoId` | `Long` | `CSNTIDST` |  |
| `estado` | `Long` | `CSNTESTD` |  |
| `valorDescontado` | `Long` | `CSNTVLDS` |  |
| `valorPagado` | `Long` | `CSNTVLPG` |  |
| `fechaEntregaCheque` | `LocalDateTime` | `CSNTFCEC` |  |

### `Ciudad` → tabla **`CRD.CDDD`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CDDDCDGO` | **PK**, IDENTITY |
| `provincia` | `Provincia` | `PRVNCDGO` | FK → `Provincia` (PRVNCDGO), ManyToOne |
| `nombre` | `String` | `CDDDNMBR` | length=2000 |
| `codigoAlterno` | `String` | `CDDDCDAL` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `CDDDFCIN` |  |
| `usuarioIngreso` | `String` | `CDDDUSIN` | length=50 |
| `codigoExterno` | `String` | `CDDDCDEX` | length=50 |
| `estado` | `Long` | `CDDDIDST` |  |

### `Comentario` → tabla **`CRD.CMNT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CMNTCDGO` | **PK**, IDENTITY |
| `fecha` | `LocalDateTime` | `CMNTFCHA` |  |
| `funcionario` | `String` | `CMNTFNCN` | length=250 |
| `observacion` | `String` | `CMNTOBSR` | length=2000 |
| `estadoTexto` | `String` | `CMNTESTD` | length=100 |
| `estado` | `Long` | `CMNTIDST` |  |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |

### `Contrato` → tabla **`CRD.CNTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNTRCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `tipoContrato` | `TipoContrato` | `TPCNCDGO` | FK → `TipoContrato` (TPCNCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `fechaInicio` | `LocalDateTime` | `CNTRFCIN` |  |
| `porcentajeAporteIndividual` | `Long` | `CNTRPRAI` |  |
| `porcentajeAporteJubilacion` | `Long` | `CNTRPRAJ` |  |
| `montoAporteAdicional` | `Double` | `CNTRMNAA` |  |
| `fechaTerminacion` | `LocalDateTime` | `CNTRFCTR` |  |
| `motivoTerminacion` | `String` | `CNTRMTTR` | length=2000 |
| `observacion` | `String` | `CNTROBSR` | length=2000 |
| `estado` | `Long` | `CNTRESTD` |  |
| `fechaAprobacion` | `LocalDateTime` | `CNTRFCAP` |  |
| `usuarioAprobacion` | `String` | `CNTRUSAP` | length=50 |
| `fechaReporte` | `LocalDateTime` | `CNTRFCRP` |  |
| `fechaRegistro` | `LocalDateTime` | `CNTRFCRG` |  |
| `usuarioRegistro` | `String` | `CNTRUSRG` | length=50 |
| `idEstado` | `Long` | `CNTRIDST` |  |

### `Conyuge` → tabla **`CRD.CNYG`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNYGCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `nombres` | `String` | `CNYGNMBR` | length=500 |
| `cedula` | `String` | `CNYGCDLA` | length=20 |
| `correo` | `String` | `CNYGCRREO` | length=500 |
| `estado` | `Long` | `CNYGIDST` |  |

### `CreditoMontoAprobacion` → tabla **`CRD.CRDT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CRDTCDGO` | **PK**, IDENTITY |
| `estado` | `Long` | `CRDTIDST` |  |
| `idProceso` | `BigDecimal` | `CRDTIDPR` |  |
| `montoMaximo` | `Double` | `CRDTMNMX` |  |
| `montoMinimo` | `Double` | `CRDTMNMN` |  |

### `CuentaBancariaParticipe` → tabla **`CRD.CNBP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNBPCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `tipoCuenta` | `Long` | `CNBPTPCN` |  |
| `numeroCuenta` | `String` | `CNBPNMRO` | length=100 |
| `estado` | `Long` | `CNBPIDST` |  |

### `CuotaXParticipeGeneracion` → tabla **`CRD.CXPG`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CXPGCDGO` | **PK**, IDENTITY |
| `participeDetalleGeneracion` | `ParticipeDetalleGeneracionArchivo` | `PDGACDGO` | FK → `ParticipeDetalleGeneracionArchivo` (PDGACDGO), ManyToOne |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `tipoAporte` | `TipoAporte` | `TPAPCDGO` | FK → `TipoAporte` (TPAPCDGO), ManyToOne |
| `numeroCuota` | `Integer` | `CXPGNNCT` |  |
| `valorCuota` | `Double` | `CXPGVLCT` |  |
| `usuarioIngreso` | `String` | `CXPGUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `CXPGFCIN` |  |
| `usuarioModificacion` | `String` | `CXPGUSMD` | length=50 |
| `fechaModificacion` | `LocalDateTime` | `CXPGFCMD` |  |

### `CxcKardexParticipe` → tabla **`CRD.CXCK`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CXCKCDGO` | **PK**, IDENTITY |
| `cxcpParticipe` | `CxcParticipe` | `CXCPCDGO` | FK → `CxcParticipe` (CXCPCDGO), ManyToOne |
| `idTransaccion` | `Long` | `CXCKIDTR` |  |
| `idCuenta` | `Long` | `CXCKIDCN` |  |
| `totalDebito` | `Double` | `CXCKTTDB` |  |
| `totalCredito` | `Double` | `CXCKTTCR` |  |
| `saldoActual` | `Double` | `CXCKSLAC` |  |
| `concepto` | `String` | `CXCKCNCP` | length=2000 |
| `fechaCreado` | `LocalDateTime` | `CXCKFCCR` |  |

### `CxcParticipe` → tabla **`CRD.CXCP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CXCPCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `idCliente` | `Long` | `CXCPIDCL` |  |
| `idCuenta` | `Long` | `CXCPIDCN` |  |
| `saldoCuenta` | `Long` | `CXCPSLCN` |  |
| `fechaCreacion` | `LocalDateTime` | `CXCPFCHC` |  |

### `DatosPrestamo` → tabla **`CRD.DTSP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTSPCDGO` | **PK**, IDENTITY |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `totalSalario` | `Double` | `DTSPTTSL` |  |
| `totalEgresos` | `Double` | `DTSPTTEG` |  |
| `otrosIngresosRol` | `Double` | `DTSPOTIR` |  |
| `otrosIngresosExternos` | `Double` | `DTSPOTIE` |  |
| `fechaRegistro` | `LocalDateTime` | `DTSPFCRG` |  |
| `usuarioRegistro` | `String` | `DTSPUSRG` | length=50 |
| `estado` | `Long` | `DTSPESTD` |  |

### `DetalleCargaArchivo` → tabla **`CRD.DTCA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTCACDGO` | **PK**, IDENTITY |
| `cargaArchivo` | `CargaArchivo` | `CRARCDGO` | FK → `CargaArchivo` (CRARCDGO), ManyToOne |
| `codigoPetroProducto` | `String` | `DTCACDPP` | length=2000 |
| `nombreProductoPetro` | `String` | `DTCANMPP` | length=2000 |
| `totalParticipes` | `Double` | `DTCATPXP` |  |
| `totalSaldoActual` | `Double` | `DTCATTSA` |  |
| `totalInteresAnual` | `Double` | `DTCATTIA` |  |
| `totalValorSeguro` | `Double` | `DTCATTVS` |  |
| `totalDescontar` | `Double` | `DTCATTDS` |  |
| `totalCapitalDescontado` | `Double` | `DTCATTCD` |  |
| `totalInteresDescontado` | `Double` | `DTCATTID` |  |
| `totalSeguroDescontado` | `Double` | `DTCATTSD` |  |
| `totalDescontado` | `Double` | `DTCATTDO` |  |
| `totalCapitalNoDescontado` | `Double` | `DTCATCND` |  |
| `totalInteresNoDescontado` | `Double` | `DTCATIND` |  |
| `totalDesgravamenNoDescontado` | `Double` | `DTCATDND` |  |
| `estado` | `Long` | `DTCAESTD` |  |

### `DetalleGeneracionArchivo` → tabla **`CRD.DTGA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTGACDGO` | **PK**, IDENTITY |
| `generacionArchivoPetro` | `GeneracionArchivoPetro` | `GNAPCDGO` | FK → `GeneracionArchivoPetro` (GNAPCDGO), ManyToOne |
| `codigoProductoPetro` | `String` | `DTGACDPT` | length=2 |
| `descripcionProducto` | `String` | `DTGADSCR` | length=200 |
| `totalRegistros` | `Long` | `DTGATLRG` |  |
| `totalMonto` | `Double` | `DTGAMNTO` |  |
| `usuarioIngreso` | `String` | `DTGAUSIN` | length=50 |
| `fechaIngreso` | `LocalDate` | `DTGAFCIN` |  |
| `usuarioModificacion` | `String` | `DTGAUSMD` | length=50 |
| `fechaModificacion` | `LocalDate` | `DTGAFCMD` |  |

### `DetallePrestamo` → tabla **`CRD.DTPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTPRCDGO` | **PK**, IDENTITY |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `numeroCuota` | `Double` | `DTPRNMCT` |  |
| `fechaVencimiento` | `LocalDateTime` | `DTPRFCVN` |  |
| `capital` | `Double` | `DTPRCPTL` |  |
| `interes` | `Double` | `DTPRINTR` |  |
| `mora` | `Double` | `DTPRMRAA` |  |
| `interesVencido` | `Double` | `DTPRINVN` |  |
| `saldoCapital` | `Double` | `DTPRSLCP` |  |
| `saldoInteres` | `Double` | `DTPRSLIN` |  |
| `saldoMora` | `Double` | `DTPRSLMR` |  |
| `saldoInteresVencido` | `Double` | `DTPRSLIV` |  |
| `fechaPagado` | `LocalDateTime` | `DTPRFCPG` |  |
| `abono` | `Double` | `DTPRABNO` |  |
| `capitalPagado` | `Double` | `DTPRCPPG` |  |
| `interesPagado` | `Double` | `DTPRINPG` |  |
| `desgravamen` | `Double` | `DTPRDSGR` |  |
| `cuota` | `Double` | `DTPRCTAA` |  |
| `saldo` | `Double` | `DTPRSLDO` |  |
| `saldoOtros` | `Double` | `DTPRSLOT` |  |
| `desgravamenFirmado` | `Double` | `DTPRDSFR` |  |
| `desgravamenDiferido` | `Double` | `DTPRDSDF` |  |
| `desgravamenOriginal` | `Double` | `DTPRDSOR` |  |
| `valorDiferido` | `Double` | `DTPRVLDF` |  |
| `total` | `Double` | `DTPRTTLL` |  |
| `moraPagado` | `Double` | `DTPRMRPG` |  |
| `desgravamenPagado` | `Double` | `DTPRDSPG` |  |
| `interesVendidoPagado` | `Double` | `DTPRINVP` |  |
| `moraCalculada` | `Double` | `DTPRMRCL` |  |
| `diasMora` | `Long` | `DTPRDSMR` |  |
| `estado` | `Long` | `DTPRESTD` |  |
| `fechaRegistro` | `LocalDateTime` | `DTPRFCRG` |  |
| `usuarioRegistro` | `String` | `DTPRUSRG` | length=200 |
| `idEstado` | `Long` | `DTPRIDST` |  |
| `codigoExterno` | `Long` | `DTPRCDEX` |  |
| `otrosSeguros` | `Double` | `DTPROTSG` |  |
| `totalConSeguro` | `Double` | `DTPRTTCS` |  |
| `valorSeguroIncendio` | `Double` | `DTPRVLSI` |  |
| `saldoInicialCapital` | `Double` | `DTPRSICP` |  |

### `Direccion` → tabla **`CRD.DRCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DRCCCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `parroquia` | `Parroquia` | `PRRQCDGO` | FK → `Parroquia` (PRRQCDGO), ManyToOne |
| `descripcion` | `String` | `DRCCDSCR` | length=2000 |
| `referencia` | `String` | `DRCCRFRN` | length=2000 |
| `telefono` | `String` | `DRCCTLFN` | length=20 |
| `celular` | `String` | `DRCCCLLR` | length=20 |
| `latitud` | `Long` | `DRCCLTTD` |  |
| `longitud` | `Long` | `DRCCLNGT` |  |
| `porDefecto` | `Long` | `DRCCPRDF` |  |
| `callePrincipal` | `String` | `DRCCCLPR` | length=500 |
| `calleSecundaria` | `String` | `DRCCCLSC` | length=500 |
| `numero` | `String` | `DRCCNMRO` | length=50 |
| `trabajo` | `Long` | `DRCCTRBJ` |  |
| `usuarioIngreso` | `String` | `DRCCUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `DRCCFCIN` |  |
| `estado` | `Long` | `DRCCIDST` |  |

### `DireccionTrabajo` → tabla **`CRD.DRTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DRTRCDGO` | **PK**, IDENTITY |
| `direccion` | `Direccion` | `DRCCCDGO` | FK → `Direccion` (DRCCCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |

### `DocumentoCredito` → tabla **`CRD.DCMN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DCMNCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `tipoPrestamo` | `TipoPrestamo` | `TPPRCDGO` | FK → `TipoPrestamo` (TPPRCDGO), ManyToOne |
| `tipoAdjunto` | `TipoAdjunto` | `TPDJCDGO` | FK → `TipoAdjunto` (TPDJCDGO), ManyToOne |
| `cantidad` | `Long` | `DCMNCNTD` |  |
| `opcional` | `Long` | `DCMNOPCN` |  |
| `usuarioIngreso` | `String` | `DCMNUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `DCMNFCIN` |  |
| `estado` | `Long` | `DCMNIDST` |  |

### `Entidad` → tabla **`CRD.ENTD`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ENTDCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `tipoHidrocarburifica` | `TipoHidrocarburifica` | `TPHDCDGO` | FK → `TipoHidrocarburifica` (TPHDCDGO), ManyToOne |
| `tipoIdentificacion` | `TipoIdentificacion` | `TPDNCDGO` | FK → `TipoIdentificacion` (TPDNCDGO), ManyToOne |
| `numeroIdentificacion` | `String` | `ENTDNMID` | length=50 |
| `razonSocial` | `String` | `ENTDRZNS` | length=2000 |
| `cargasFamiliares` | `Long` | `ENTDNMCF` |  |
| `nombreComercial` | `String` | `ENTDNMCM` | length=2000 |
| `fechaNacimiento` | `String` | `ENTDFCNC` |  |
| `tipoVivienda` | `TipoVivienda` | `TPVVCDGO` | FK → `TipoVivienda` (TPVVCDGO), ManyToOne |
| `sectorPublico` | `Long` | `ENTDSCPB` |  |
| `correoPersonal` | `String` | `ENTDCRPR` | length=2000 |
| `correoInstitucional` | `String` | `ENTDCRIN` | length=2000 |
| `telefono` | `String` | `ENTDTLFN` | length=50 |
| `tieneCorreoPersonal` | `Long` | `ENTDCPVR` |  |
| `tieneCorreoTrabajo` | `Long` | `ENTDCIVR` |  |
| `tieneTelefono` | `Long` | `ENTDTLVR` |  |
| `migrado` | `Long` | `ENTDMGRD` |  |
| `movil` | `String` | `ENTDMVLI` | length=50 |
| `idCiudad` | `String` | `ENTDIDCD` | length=50 |
| `porcentajeSimilitud` | `Long` | `ENTDPRSM` |  |
| `busqueda` | `String` | `ENTDBSQD` | length=2000 |
| `ipIngreso` | `String` | `ENTDIPIN` | length=50 |
| `usuarioIngreso` | `String` | `ENTDUSIN` | length=50 |
| `fechaIngreso` | `String` | `ENTDFCIN` |  |
| `ipModificacion` | `String` | `ENTDIPMD` | length=50 |
| `usuarioModificacion` | `String` | `ENTDUSMD` | length=50 |
| `idEstado` | `Long` | `ENTDIDST` |  |
| `urlFotoLogo` | `String` | `ENTDURFL` | length=2000 |
| `rolPetroComercial` | `Long` | `ENTDRLPC` |  |
| `estadoCivil` | `EstadoCivil` | `ESCVCDGO` | FK → `EstadoCivil` (ESCVCDGO), ManyToOne |

### `EstadoCesantia` → tabla **`CRD.ESCS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ESCSCDGO` | **PK**, IDENTITY |
| `descripcion` | `String` | `ESCSDSCR` | length=2000, NOT NULL |
| `idEstado` | `Long` | `ESCSIDST` | NOT NULL |

### `EstadoCivil` → tabla **`CRD.ESCV`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ESCVCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `ESCVNMBR` | length=2000, NOT NULL |
| `codigoAuxiliar` | `Long` | `ESCVCAUX` |  |
| `idEstado` | `Long` | `ESCVIDST` | NOT NULL |

### `EstadoCuotaPrestamo` → tabla **`CRD.ESCP`**

Secuencia PK: `CRD.SQ_ESCPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ESCPCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `ESCPNMBR` | length=200 |
| `codigoAlterno` | `Long` | `ESCPCDAL` |  |
| `estado` | `Long` | `ESCPESTD` |  |

### `EstadoParticipe` → tabla **`CRD.ESPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ESPRCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `ESPRNMBR` | length=2000, NOT NULL |
| `codigoExterno` | `Long` | `ESPRCDEX` |  |
| `idEstado` | `Long` | `ESPRIDST` | NOT NULL |

### `EstadoPrestamo` → tabla **`CRD.ESPS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ESPSCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `ESPSNMBR` | length=2000, NOT NULL |
| `codigoExterno` | `Long` | `ESPSCDEX` |  |
| `idEstado` | `Long` | `ESPSIDST` | NOT NULL |

### `Exter` → tabla **`CRD.EXTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `cedula` | `String` | `EXTRCDLA` | **PK**, length=2000 |
| `nombre` | `String` | `EXTRNMBR` | length=2000 |
| `estado` | `String` | `EXTRESTD` | length=2000 |
| `fechaNacimiento` | `LocalDateTime` | `EXTRFCNC` |  |
| `estadoCivil` | `String` | `EXTRESCV` | length=2000 |
| `nivelEstudios` | `String` | `EXTRNVES` | length=2000 |
| `edad` | `String` | `EXTREDDD` | length=2000 |
| `profesion` | `String` | `EXTRPRFS` | length=2000 |
| `genero` | `String` | `EXTRGNRO` | length=2000 |
| `fechaDefuncion` | `LocalDateTime` | `EXTRFCDF` |  |
| `nacionalidad` | `String` | `EXTRNCNL` | length=2000 |
| `provincia` | `String` | `EXTRPRVN` | length=2000 |
| `canton` | `String` | `EXTRCNTN` | length=2000 |
| `movil` | `String` | `EXTRMVLL` | length=2000 |
| `telefono` | `String` | `EXTRTLFN` | length=2000 |
| `correoPrincipal` | `String` | `EXTRCRPR` | length=2000 |
| `correoInstitucional` | `String` | `EXTRCRIN` | length=2000 |
| `celular1` | `String` | `EXTRCLL1` | length=2000 |
| `celular2` | `String` | `EXTRCLL2` | length=2000 |
| `correoExtra` | `String` | `EXTRCREX` | length=2000 |
| `telefonoLaboralIE` | `String` | `EXTRTLIE` | length=2000 |
| `correoIE` | `String` | `EXTRCRIE` | length=2000 |
| `salarioFijo` | `Double` | `EXTRSLFJ` |  |
| `salarioVariable` | `Double` | `EXTRSLVR` |  |
| `salarioTotal` | `Double` | `EXTRSLTT` |  |
| `sumadosIngresos` | `Double` | `EXTRSMCI` |  |
| `sumadosEgresos` | `Double` | `EXTRSMSC` |  |
| `disponible` | `Double` | `EXTRDSPN` |  |

### `Filial` → tabla **`CRD.FLLL`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `FLLLCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `FLLLNMBR` | length=2000 |
| `codigoAlterno` | `String` | `FLLLCDAL` | length=50 |
| `estado` | `Long` | `FLLLIDST` |  |

### `GeneracionArchivoPetro` → tabla **`CRD.GNAP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GNAPCDGO` | **PK**, IDENTITY |
| `mesPeriodo` | `Long` | `GNAPMSPE` |  |
| `anioPeriodo` | `Long` | `GNAPANPE` |  |
| `fechaGeneracion` | `LocalDate` | `GNAPFCGN` |  |
| `usuarioGeneracion` | `String` | `GNAPUSGN` | length=50 |
| `totalRegistros` | `Long` | `GNAPTLRG` |  |
| `totalMontoEnviado` | `Double` | `GNAPMTEN` |  |
| `estado` | `Long` | `GNAPESTD` |  |
| `rutaArchivo` | `String` | `GNAPRTAA` | length=500 |
| `nombreArchivo` | `String` | `GNAPNMAR` | length=200 |
| `fechaEnvio` | `LocalDate` | `GNAPFCEN` |  |
| `fechaProcesamiento` | `LocalDate` | `GNAPFCPR` |  |
| `observaciones` | `String` | `GNAPOBSR` | length=4000 |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `usuarioIngreso` | `String` | `GNAPUSIN` | length=50 |
| `fechaIngreso` | `LocalDate` | `GNAPFCIN` |  |
| `usuarioModificacion` | `String` | `GNAPUSMD` | length=50 |
| `fechaModificacion` | `LocalDate` | `GNAPFCMD` |  |

### `HistorialSueldo` → tabla **`CRD.HSTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `HSTRCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `sueldo` | `Double` | `HSTRSLDO` |  |
| `porcentajeCesantia` | `Long` | `HSTRPRCN` |  |
| `porcentajeJubilacion` | `Long` | `HSTRPRJB` |  |
| `montoJubilacion` | `Double` | `HSTRMNAJ` |  |
| `montoCesantia` | `Double` | `HSTRMNAC` |  |
| `montoAdicional` | `Double` | `HSTRMNAA` |  |
| `usuarioIngreso` | `String` | `HSTRUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `HSTRFCIN` |  |
| `estado` | `Long` | `HSTRESTD` |  |

### `HistoricoDesgloseAporteParticipe` → tabla **`CRD.HDAP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `orden` | `Long` | `HDAPORDE` | **PK**, IDENTITY |
| `codigoInterno` | `String` | `HDAPCODI` | length=20 |
| `cedula` | `String` | `HDAPCEDU` | length=15 |
| `aporteJubilacion` | `Double` | `HDAPJUBI` |  |
| `aporteCesantia` | `Double` | `HDAPCESA` |  |
| `totalBeneficios` | `Double` | `HDAPTOBE` |  |
| `prestamoEmergente` | `Double` | `HDAPEMER` |  |
| `prestamoQuirografario` | `Double` | `HDAPQUIR` |  |
| `prestamoHipotecario` | `Double` | `HDAPHIPO` |  |
| `prestamoPrendario` | `Double` | `HDAPPREN` |  |
| `totalPrestamos` | `Double` | `HDAPTOPR` |  |
| `prestamoVehicular` | `Double` | `HDAPVEHI` |  |
| `seguroIncendios` | `Double` | `HDAPINCE` |  |
| `tonsupa` | `Double` | `HDAPTONS` |  |
| `descuentoTotal` | `Double` | `HDAPDSCT` |  |
| `idCarga` | `Long` | `HDAPIDCA` |  |
| `fechaCarga` | `Date` | `HDAPFCTR` |  |
| `usuarioCarga` | `String` | `HDAPUSAR` | length=30 |
| `estado` | `Integer` | `HDAPESTD` |  |

### `InformacionGeneralFondo` → tabla **`CRD.IGFN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `IGFNCDGO` | **PK**, IDENTITY |
| `tipoIdentificacionFcpc` | `String` | `IGFNTIDF` | length=50 |
| `identificacionFcpc` | `String` | `IGFNIDFC` | length=50 |
| `numeroResolucion` | `String` | `IGFNNMRS` | length=100 |
| `fechaResolucion` | `LocalDate` | `IGFNFCRS` |  |
| `provincia` | `String` | `IGFNPRVN` | length=100 |
| `canton` | `String` | `IGFNCNTN` | length=100 |
| `direccion` | `String` | `IGFNDRCC` | length=500 |
| `telefonos` | `String` | `IGFNTLFN` | length=100 |
| `correoElectronico` | `String` | `IGFNCEFD` | length=200 |
| `tipoSistema` | `String` | `IGFNTPSS` | length=50 |
| `tipoPrestacion` | `String` | `IGFNTPPR` | length=50 |
| `tipoAporte` | `String` | `IGFNTPAP` | length=50 |
| `tipoAdministracion` | `String` | `IGFNTPAD` | length=50 |
| `fechaTraspaso` | `LocalDate` | `IGFNFCTR` |  |
| `tipoFcpc` | `String` | `IGFNTPFC` | length=50 |
| `numeroResolucionCambioEstatuto` | `String` | `IGFNNRCE` | length=100 |
| `fechaResolucionCambioEstatuto` | `LocalDate` | `IGFNFRCE` |  |
| `cambioNombre` | `String` | `IGFNCMNM` | length=200 |
| `porcentajeAportePatronalCesantia` | `Double` | `IGFNPAPC` |  |
| `porcentajeAportePersonalCesantia` | `Double` | `IGFNPARC` |  |
| `porcentajeAportePatronalJubilacion` | `Double` | `IGFNPAPJ` |  |
| `porcentajeAportePersonalJubilacion` | `Double` | `IGFNPARJ` |  |
| `valorAportePersonalCesantia` | `Double` | `IGFNVARC` |  |
| `valorAportePersonalJubilacion` | `Double` | `IGFNVARJ` |  |
| `estado` | `Long` | `IGFNESTD` |  |
| `usuarioModificacion` | `String` | `IGFNUSRM` | length=50 |
| `fechaModificacion` | `LocalDate` | `IGFNFCMD` |  |

### `MetodoPago` → tabla **`CRD.MTDP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MTDPCDGO` | **PK**, IDENTITY |
| `codigoSbs` | `String` | `MTDPCSBC` | length=50 |
| `nombre` | `String` | `MTDPNMBR` | length=2000 |
| `estado` | `Long` | `MTDPIDST` |  |

### `MoraPrestamo` → tabla **`CRD.MRPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MRPRCDGO` | **PK**, IDENTITY |
| `tipoPrestamo` | `TipoPrestamo` | `TPPRCDGO` | FK → `TipoPrestamo` (TPPRCDGO), ManyToOne |
| `diasMinimo` | `Long` | `MRPRDSMN` |  |
| `diasMaximo` | `Long` | `MRPRDSMX` |  |
| `porcentajeMora` | `Long` | `MRPRPRMR` |  |
| `tasaAnual` | `Long` | `MRPRTSAN` |  |

### `MotivoPrestamo` → tabla **`CRD.MTVP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MTVPCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `MTVPNMBR` | length=2000 |
| `estado` | `Long` | `MTVPIDST` |  |

### `NivelEstudio` → tabla **`CRD.NVLS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `NVLSCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `NVLSNMBR` | length=2000 |
| `estado` | `Long` | `NVLSIDST` |  |

### `NovedadParticipeCarga` → tabla **`CRD.NVPC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `NVPCCDGO` | **PK**, IDENTITY |
| `participeXCargaArchivo` | `ParticipeXCargaArchivo` | `PXCACDGO` | FK → `ParticipeXCargaArchivo` (PXCACDGO), ManyToOne |
| `tipoNovedad` | `Long` | `NVPCTPNV` |  |
| `descripcion` | `String` | `NVPCDSCR` | length=4000 |
| `codigoProducto` | `Long` | `NVPCCDPR` |  |
| `codigoPrestamo` | `Long` | `NVPCCDPS` |  |
| `codigoCuota` | `Long` | `NVPCCDCT` |  |
| `montoEsperado` | `Double` | `NVPCMNES` |  |
| `montoRecibido` | `Double` | `NVPCMNRC` |  |
| `montoDiferencia` | `Double` | `NVPCMNDF` |  |
| `codigoCargaArchivo` | `Long` | `NVPCCDCA` |  |
| `idAsoprepPrestamo` | `Long` | `NVPCIASP` |  |
| `estado` | `Long` | `NVPCESTD` |  |

### `OrdenAfectacionValorPrestamo` → tabla **`CRD.OAVP`**

Secuencia PK: `CRD.SQ_OAVPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `OAVPCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `OAVPNMBR` | length=200 |
| `orden` | `Long` | `OAVPORDN` |  |
| `estado` | `Long` | `OAVPESTD` |  |

### `PagoAporte` → tabla **`CRD.PGAP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PGAPCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `aporte` | `Aporte` | `APRTCDGO` | FK → `Aporte` (APRTCDGO), ManyToOne |
| `valor` | `Double` | `PGAPVLRR` |  |
| `fechaContable` | `LocalDateTime` | `PGAPFCCN` |  |
| `numeroAsiento` | `Long` | `PGAPNMAS` |  |
| `concepto` | `String` | `PGAPCNCP` | length=2000 |
| `fechaRegistro` | `LocalDateTime` | `PGAPFCRG` |  |
| `usuarioRegistro` | `String` | `PGAPUSRG` | length=200 |
| `estado` | `Long` | `PGAPIDST` |  |

### `PagoPrestamo` → tabla **`CRD.PGPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PGPRCDGO` | **PK**, IDENTITY |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `detallePrestamo` | `DetallePrestamo` | `DTPRCDGO` | FK → `DetallePrestamo` (DTPRCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `PGPRFCHA` |  |
| `valor` | `Double` | `PGPRVLRR` |  |
| `numeroCuota` | `Double` | `PGPRNMCT` |  |
| `capitalPagado` | `Double` | `PGPRCPPG` |  |
| `interesPagado` | `Double` | `PGPRINPG` |  |
| `moraPagada` | `Double` | `PGPRMRPG` |  |
| `interesVencidoPagado` | `Double` | `PGPRINVP` |  |
| `desgravamen` | `Double` | `PGPRDSGR` |  |
| `saldoOtros` | `Double` | `PGPRSLOT` |  |
| `valorSeguroIncendio` | `Double` | `PGPRVLSI` |  |
| `observacion` | `String` | `PGPROBSR` | length=2000 |
| `tipo` | `String` | `PGPRTPOO` | length=50 |
| `estado` | `Long` | `PGPRESTD` |  |
| `fechaRegistro` | `LocalDateTime` | `PGPRFCRG` |  |
| `usuarioRegistro` | `String` | `PGPRUSRG` | length=200 |
| `idEstado` | `Long` | `PGPRIDST` | NOT NULL |

> **La tabla `CRD.PSSS` (países) sigue en este esquema, pero su clase Java ya no está en este
> paquete**: se movió a `com.saa.model.scp.Pais` el 2026-08-24, porque `TSR.Titular` la
> importaba y el sistema se comercializa sin `crd`. **La tabla no se migró.**
> La ficha está en la sección SCP, junto a la clase. Ver también
> `docs/general/sql/MIGRACION-PAIS-CRD-A-SCP.md`, marcada NO APLICADA.

### `Parroquia` → tabla **`CRD.PRRQ`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRRQCDGO` | **PK**, IDENTITY |
| `ciudad` | `Ciudad` | `CDDDCDGO` | FK → `Ciudad` (CDDDCDGO), ManyToOne |
| `nombre` | `String` | `PRRQNMBR` | length=2000 |
| `usuarioIngreso` | `String` | `PRRQUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `PRRQFCIN` |  |
| `codigoExterno` | `String` | `PRRQCDEX` | length=50 |
| `estado` | `Long` | `PRRQIDST` |  |

### `Participe` → tabla **`CRD.PRTC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRTCCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `tipoParticipante` | `TipoParticipe` | `TPPCCDGO` | FK → `TipoParticipe` (TPPCCDGO), ManyToOne |
| `tipoCalificacion` | `Long` | `TPCLCDGO` |  |
| `codigoAlterno` | `Long` | `PRTCCDAL` |  |
| `remuneracionUnificada` | `Long` | `PRTCRMUN` |  |
| `fechaIngresoTrabajo` | `LocalDateTime` | `PRTCFCIT` |  |
| `lugarTrabajo` | `String` | `PRTCLGRT` | length=50 |
| `unidadAdministrativa` | `String` | `PRTCUNAD` | length=2000 |
| `cargoActual` | `String` | `PRTCCRGA` | length=2000 |
| `nivelEstudios` | `String` | `PRTCNVES` | length=2000 |
| `ingresoAdicionalMensual` | `Long` | `PRTCIAMM` |  |
| `ingresoAdicionalActividad` | `String` | `PRTCIAAC` | length=2000 |
| `fechaIngresoFondo` | `LocalDateTime` | `PRTCFCIF` |  |
| `estadoActual` | `Long` | `PRTCESAC` |  |
| `fechaFallecimiento` | `LocalDateTime` | `PRTCFCHF` |  |
| `causaFallecimiento` | `String` | `PRTCCSFL` | length=2000 |
| `motivoSalida` | `String` | `PRTCMTSL` | length=2000 |
| `fechaSalida` | `LocalDateTime` | `PRTCFCSL` |  |
| `estadoCesante` | `Long` | `PRTCESCS` |  |
| `fechaIngreso` | `LocalDateTime` | `PRTCFCIN` |  |
| `idEstado` | `Long` | `PRTCIDST` |  |
| `tipoAporte` | `TipoAporte` | `TPAPCDGO` | FK → `TipoAporte` (TPAPCDGO), ManyToOne |

### `ParticipeAsoprep` → tabla **`CRD.PRAS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `PRASIDDD` | **PK** |
| `cedula` | `String` | `PRASCDLA` | length=20 |
| `codigoCdgf` | `String` | `PRASCDGF` | length=10 |
| `codigoCdga` | `String` | `PRASCDGA` | length=10 |
| `estadoParticipante` | `String` | `PRASSTDP` | length=30 |
| `apellidos` | `String` | `PRASAPLL` | length=100 |
| `nombres` | `String` | `PRASNMBR` | length=100 |
| `fechaNacimiento` | `LocalDateTime` | `PRASFCHN` |  |
| `genero` | `String` | `PRASGNRO` | length=1 |
| `estadoCivil` | `String` | `PRASSTDC` | length=20 |
| `provincia` | `String` | `PRASPRVD` | length=60 |
| `canton` | `String` | `PRASCNTD` | length=60 |
| `ciudad` | `String` | `PRASCDDD` | length=80 |
| `parroquia` | `String` | `PRASPRQD` | length=100 |
| `direccion` | `String` | `PRASDRCD` | length=350 |
| `telefonoFijo` | `String` | `PRASTLFD` | length=60 |
| `telefonoCelular` | `String` | `PRASTLFC` | length=30 |
| `correoElectronico` | `String` | `PRASCRRE` | length=90 |
| `banco` | `String` | `PRASBNCO` | length=80 |
| `tipoCuentaBancaria` | `String` | `PRASTPCB` | length=30 |
| `cuentaBancaria` | `String` | `PRASCTAB` | length=40 |
| `fechaCcs` | `LocalDateTime` | `PRASFCCS` |  |
| `fechaJubilacion` | `LocalDateTime` | `PRASFCJB` |  |
| `fechaIngreso` | `LocalDateTime` | `PRASFCIN` |  |
| `fechaCsn` | `LocalDateTime` | `PRASFCSN` |  |
| `fechaCjc` | `LocalDateTime` | `PRASFCJC` |  |
| `institucion` | `String` | `PRASNSTT` | length=40 |
| `localidad` | `String` | `PRASLCCN` | length=50 |
| `region` | `String` | `PRASRGNN` | length=50 |
| `edad` | `String` | `PRASNDAD` | length=200 |
| `regimen` | `String` | `PRASRGMN` | length=30 |
| `telefonoTrabajo` | `String` | `PRASTLFT` | length=20 |
| `cargo` | `String` | `PRASCRGO` | length=200 |
| `codigoCdlg` | `String` | `PRASCDLG` | length=15 |

### `ParticipeDetalleGeneracionArchivo` → tabla **`CRD.PDGA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PDGACDGO` | **PK**, IDENTITY |
| `detalleGeneracionArchivo` | `DetalleGeneracionArchivo` | `DTGACDGO` | FK → `DetalleGeneracionArchivo` (DTGACDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `rolPetrocomercial` | `Long` | `PDGARLPC` |  |
| `codigoProductoPetro` | `String` | `PDGACDPT` | length=2 |
| `montoEnviado` | `Double` | `PDGAMNEN` |  |
| `numeroLinea` | `Long` | `PDGANNLN` |  |
| `observaciones` | `String` | `PDGAOBSR` | length=500 |
| `estado` | `Integer` | `PDGAESTD` |  |
| `montoDescontado` | `Double` | `PDGAMNDC` |  |
| `fechaDescuento` | `LocalDateTime` | `PDGAFCDC` |  |
| `usuarioIngreso` | `String` | `PDGAUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `PDGAFCIN` |  |
| `usuarioModificacion` | `String` | `PDGAUSMD` | length=50 |
| `fechaModificacion` | `LocalDateTime` | `PDGAFCMD` |  |

### `ParticipeXCargaArchivo` → tabla **`CRD.PXCA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PXCACDGO` | **PK**, IDENTITY |
| `detalleCargaArchivo` | `DetalleCargaArchivo` | `DTCACDGO` | FK → `DetalleCargaArchivo` (DTCACDGO), ManyToOne |
| `codigoPetro` | `Long` | `PXCACDPT` |  |
| `nombre` | `String` | `PXCANMBR` | length=2000 |
| `plazoInicial` | `Long` | `PXCAPLIN` |  |
| `mesesPlazo` | `Long` | `PXCAMSPL` |  |
| `saldoActual` | `Double` | `PXCASLAC` |  |
| `interesAnual` | `Double` | `PXCAINAN` |  |
| `valorSeguro` | `Double` | `PXCAVLSG` |  |
| `montoDescontar` | `Double` | `PXCADSCT` |  |
| `capitalDescontado` | `Double` | `PXCACPDS` |  |
| `interesDescontado` | `Double` | `PXCAINDS` |  |
| `seguroDescontado` | `Double` | `PXCASGDS` |  |
| `totalDescontado` | `Double` | `PXCADSDO` |  |
| `capitalNoDescontado` | `Double` | `PXCACPND` |  |
| `interesNoDescontado` | `Double` | `PXCAITND` |  |
| `desgravamenNoDescontado` | `Double` | `PXCADSND` |  |
| `estadoRevision` | `Long` | `PXCAESRV` |  |
| `novedadesCarga` | `Long` | `PXCANVCA` |  |
| `estado` | `Long` | `PXCAESTD` |  |
| `novedadesFinancieras` | `Long` | `PXCANVFN` |  |

### `PerfilEconomico` → tabla **`CRD.PREC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRECCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `salarioFijo` | `Double` | `PRECSLFJ` |  |
| `salarioVariable` | `Double` | `PRECSLVR` |  |
| `origenOtrosIngresos` | `String` | `PRECOROI` | length=2000 |
| `otrosIngresos` | `Double` | `PRECOTIN` |  |
| `totalIngresos` | `Double` | `PRECTTIN` |  |
| `gastosMensuales` | `Double` | `PRECGSMS` |  |
| `totalBienes` | `Double` | `PRECTTBN` |  |
| `totalVehiculos` | `Double` | `PRECTTVH` |  |
| `totalOtrosActivos` | `Double` | `PRECTTOA` |  |
| `totalActivos` | `Double` | `PRECTTAC` |  |
| `totalDeudas` | `Double` | `PRECTTDD` |  |
| `patrimonioNeto` | `Double` | `PRECPTNT` |  |
| `fechaActualizacion` | `LocalDateTime` | `PRECFCAC` |  |
| `fechaRegistro` | `LocalDateTime` | `PRECFCRG` |  |
| `usuarioRegistro` | `String` | `PRECUSRG` | length=200 |
| `estado` | `Long` | `PRECIDST` |  |
| `fechaIngresoTrabajo` | `LocalDateTime` | `PRECFCIT` |  |
| `fechaRegistroTrabajo` | `LocalDateTime` | `PRECFCRT` |  |
| `salarioNeto` | `Double` | `PRECSLNT` |  |
| `periodo` | `Periodo` | `PRECPRDO` | length=2000 |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |

### `PersonaNatural` → tabla **`CRD.PRSN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRSNCDGO` | **PK**, IDENTITY |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `nombres` | `String` | `PRSNNMBR` | length=2000 |
| `apellidos` | `String` | `PRSNAPLL` | length=2000 |
| `estadoCivil` | `EstadoCivil` | `PRSNESCV` | FK → `EstadoCivil` (ESCVCDGO), ManyToOne |
| `genero` | `String` | `PRSNGNRO` | length=200 |
| `usuarioIngreso` | `String` | `PRSNUSIN` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `PRSNFCIN` |  |
| `estado` | `Long` | `PRSNIDST` |  |

### `Prestamo` → tabla **`CRD.PRST`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRSTCDGO` | **PK**, IDENTITY |
| `idAsoprep` | `Long` | `PRSTIDAS` |  |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `producto` | `Producto` | `PRDCCDGO` | FK → `Producto` (PRDCCDGO), ManyToOne |
| `tipoAmortizacion` | `Long` | `PRSTTPAM` |  |
| `amortizacion` | `String` | `PRSTAMRT` | length=50 |
| `fecha` | `LocalDateTime` | `PRSTFCHA` |  |
| `fechaInicio` | `LocalDateTime` | `PRSTFCIN` |  |
| `fechaFin` | `LocalDateTime` | `PRSTFCFN` |  |
| `interesNominal` | `Double` | `PRSTINNM` |  |
| `montoSolicitado` | `Double` | `PRSTMNSL` |  |
| `valorCuota` | `Double` | `PRSTVLCT` |  |
| `plazo` | `Long` | `PRSTPLZO` |  |
| `montoLiquidacion` | `Double` | `PRSTMNLD` |  |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `estadoPrestamo` | `Long` | `ESPSCDGO` |  |
| `tasa` | `Double` | `PRSTTSAA` |  |
| `totalPagado` | `Double` | `PRSTTTPG` |  |
| `totalCapital` | `Double` | `PRSTTTCP` |  |
| `totalInteres` | `Double` | `PRSTTTIN` |  |
| `totalMora` | `Double` | `PRSTTTMR` |  |
| `totalInteresVencido` | `Double` | `PRSTTTIV` |  |
| `totalSeguros` | `Double` | `PRSTTTSG` |  |
| `totalPrestamo` | `Double` | `PRSTTTPR` |  |
| `saldoPorVencer` | `Double` | `PRSTSLXV` |  |
| `saldoVencido` | `Double` | `PRSTSLVN` |  |
| `saldoTotal` | `Double` | `PRSTSLTT` |  |
| `fechaRegistro` | `LocalDateTime` | `PRSTFCRG` |  |
| `usuarioRegistro` | `String` | `PRSTUSRG` | length=2000 |
| `fechaModificacion` | `LocalDateTime` | `PRSTFCMD` |  |
| `usuarioModificacion` | `String` | `PRSTUSMD` | length=500 |
| `observacion` | `String` | `PRSTOBSR` | length=2000 |
| `motivoPrestamo` | `MotivoPrestamo` | `MTVPCDGO` | FK → `MotivoPrestamo` (MTVPCDGO), ManyToOne |
| `estadoOperacion` | `Long` | `PRSTESOP` |  |
| `tasaNominal` | `Double` | `PRSTTSNM` |  |
| `tasaEfectiva` | `Double` | `PRSTTSEF` |  |
| `esNovacion` | `Long` | `PRSTESNV` |  |
| `reprocesado` | `Long` | `PRSTRPRC` |  |
| `reestructurado` | `Long` | `PRSTRSTR` |  |
| `refinanciado` | `Long` | `PRSTRFNN` |  |
| `saldoCapital` | `Double` | `PRSTSLCP` |  |
| `saldoOtros` | `Double` | `PRSTSLOT` |  |
| `saldoInteres` | `Double` | `PRSTSLIN` |  |
| `moraCalculada` | `Double` | `PRSTMRCL` |  |
| `diasVencido` | `Long` | `PRSTDSVN` |  |
| `montoNovacion` | `Double` | `PRSTMNNV` |  |
| `interesVariable` | `Double` | `PRSTINVR` |  |
| `usuarioAprobacion` | `String` | `PRSTUSAP` | length=50 |
| `fechaAprobacion` | `LocalDateTime` | `PRSTFCAP` |  |
| `fechaAdjudicacion` | `LocalDateTime` | `PRSTFCAD` |  |
| `usuarioRechazo` | `String` | `PRSTUSRC` | length=200 |
| `fechaRechazo` | `LocalDateTime` | `PRSTFCRC` |  |
| `usuarioLegalizacion` | `String` | `PRSTUSLG` | length=200 |
| `fechaLegalizacion` | `LocalDateTime` | `PRSTFCLG` |  |
| `usuarioAcreditacion` | `String` | `PRSTUSAC` | length=200 |
| `fechaAcreditacion` | `LocalDateTime` | `PRSTFCAC` |  |
| `ajusteAportes` | `Long` | `PRSTAJAP` |  |
| `mesesACobrar` | `Long` | `PRSTMSCB` |  |
| `idEstado` | `Long` | `PRSTIDST` |  |
| `firmadoTitular` | `Long` | `PRSTFRTT` |  |
| `valorAsegurado` | `Double` | `PRSTVLAS` |  |
| `tasaSeguroIncendio` | `Double` | `PRSTTSIN` |  |
| `primaSeguroIncendio` | `Double` | `PRSTPRIN` |  |

### `ProcesamientoCargaArchivo` → tabla **`CRD.PRCA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRCACDGO` | **PK**, IDENTITY |
| `participeXCargaArchivo` | `ParticipeXCargaArchivo` | `PXCACDGO` | FK → `ParticipeXCargaArchivo` (PXCACDGO), ManyToOne |
| `fechaProcesamiento` | `LocalDateTime` | `PRCAFCPR` |  |
| `procesado` | `Integer` | `PRCAPRCS` |  |
| `novedadProcesamiento` | `Integer` | `PRCANVPR` |  |
| `idPagoGenerado` | `Long` | `PRCAIDPG` |  |
| `idCuotaProcesada` | `Long` | `PRCAIDCT` |  |
| `idAporteGenerado` | `Long` | `PRCAIDAP` |  |
| `idPrestamoProcessado` | `Long` | `PRCAIDPS` |  |
| `saldoCapitalPendiente` | `Double` | `PRCASLCP` |  |
| `saldoInteresPendiente` | `Double` | `PRCASLIN` |  |
| `estadoCuotaDeterminado` | `Integer` | `PRCAESDQ` |  |
| `observaciones` | `String` | `PRCAOBSR` | length=4000 |
| `error` | `String` | `PRCAERRO` | length=4000 |
| `fechaRegistro` | `LocalDateTime` | `PRCAFCRG` |  |
| `usuarioRegistro` | `String` | `PRCAUSRG` | length=200 |
| `estado` | `Integer` | `PRCAESTD` |  |

### `Producto` → tabla **`CRD.PRDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRDCCDGO` | **PK**, IDENTITY |
| `codigoSBS` | `String` | `PRDCCSPB` | length=50, NOT NULL |
| `nombre` | `String` | `PRDCNMBR` | length=2000 |
| `filial` | `Filial` | `FLLLCDGO` | FK → `Filial` (FLLLCDGO), ManyToOne |
| `tipoPrestamo` | `TipoPrestamo` | `TPPRCDGO` | FK → `TipoPrestamo` (TPPRCDGO), ManyToOne |
| `codigoExterno` | `Long` | `PRDCCDEX` |  |
| `fechaRegistro` | `Date` | `PRDCFCRG` |  |
| `usuarioRegistro` | `String` | `PRDCUSRG` | length=2000 |
| `ipRegistro` | `String` | `PRDCIPRG` | length=50 |
| `fechaModificacion` | `Date` | `PRDCFCMD` |  |
| `usuarioModificacion` | `String` | `PRDCUSMD` | length=2000 |
| `ipModificacion` | `String` | `PRDCIPMD` | length=50 |
| `estado` | `Long` | `PRDCESTD` |  |
| `codigoPetro` | `String` | `PRDCCDPT` |  |

### `Profesion` → tabla **`CRD.PRFS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRFSCDGO` | **PK**, IDENTITY |
| `codigoSBS` | `String` | `PRFSCSBC` | length=50 |
| `nombre` | `String` | `PRFSNMBR` | length=2000 |
| `estado` | `Long` | `PRFSIDST` |  |

### `Provincia` → tabla **`CRD.PRVN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRVNCDGO` | **PK**, IDENTITY |
| `pais` | `Pais` | `PSSSCDGO` | FK → `Pais` (PSSSCDGO), ManyToOne |
| `nombre` | `String` | `PRVNNMBR` | length=2000 |
| `codigoAlterno` | `String` | `PRVNCDAL` | length=50 |
| `codigoExterno` | `String` | `PRVNCDEX` | length=50 |
| `estado` | `Long` | `PRVNIDST` |  |

### `ReferenciaFamiliar` → tabla **`CRD.RRFF`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RRFFCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `nombres` | `String` | `RRFFNMBR` | length=500 |
| `cedula` | `String` | `RRFFCDLA` | length=20 |
| `contacto` | `String` | `RRFFCNTC` | length=50 |
| `parentesco` | `String` | `RRFFPRNT` | length=200 |
| `estado` | `Long` | `RRFFIDST` |  |

### `ReferenciaPersonal` → tabla **`CRD.RRPP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RRPPCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `nombres` | `String` | `RRPPNMBR` | length=500 |
| `cedula` | `String` | `RRPPCDLA` | length=20 |
| `contacto` | `String` | `RRPPCNTC` | length=50 |
| `parentesco` | `String` | `RRPPPRNT` | length=200 |
| `estado` | `Long` | `RRPPIDST` |  |

### `RelacionPrestamo` → tabla **`CRD.RLPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RLPRCDGO` | **PK**, IDENTITY |
| `prestamoHijo` | `Prestamo` | `PRSTCDGH` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `prestamoPadre` | `Prestamo` | `PRSTCDGP` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `tipo` | `String` | `RLPRTPPR` | length=500 |
| `fechaRegistro` | `LocalDateTime` | `RLPRFCRG` |  |
| `usuarioRegistro` | `String` | `RLPRUSRG` | length=200 |
| `estado` | `Long` | `RLPRIDST` |  |

### `RequisitosPrestamo` → tabla **`CRD.RQPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RQPRCDGO` | **PK**, IDENTITY |
| `prestamo` | `Prestamo` | `PRSTCDGO` | FK → `Prestamo` (PRSTCDGO), ManyToOne |
| `tipoRequisitoPrestamo` | `TipoRequisitoPrestamo` | `TPRQCDGO` | FK → `TipoRequisitoPrestamo` (TPRQCDGO), ManyToOne |
| `validado` | `Long` | `RQPRVLDO` |  |
| `alerta` | `Long` | `RQPRALRT` |  |
| `descripcion` | `String` | `RQPRDSCR` | length=2000 |
| `observacion` | `String` | `RQPROBSR` | length=2000 |
| `usuarioRegistro` | `String` | `RQPRUSRG` | length=200 |
| `fechaRegistro` | `LocalDateTime` | `RQPRFCRG` |  |
| `estado` | `Long` | `RQPRESTD` |  |

### `TasaPrestamo` → tabla **`CRD.TSPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TSPRCDGO` | **PK**, IDENTITY |
| `codigoSbs` | `String` | `TSPRCSBC` | length=50 |
| `nombre` | `String` | `TSPRNMBR` | length=1000 |
| `tasaNominal` | `Long` | `TSPRTSNM` |  |
| `tasaEfectiva` | `Long` | `TSPRTSEF` |  |
| `producto` | `Producto` | `PRDCCDGO` | FK → `Producto` (PRDCCDGO), ManyToOne |
| `estado` | `Long` | `TSPRIDST` |  |
| `plazoMinimo` | `Long` | `TSPRPLMN` |  |
| `plazoMaximo` | `Long` | `TSPRPLMX` |  |
| `montoMinimo` | `Long` | `TSPRMNMN` |  |
| `montoMaximo` | `Long` | `TSPRMNMX` |  |

### `TipoAdjunto` → tabla **`CRD.TPDJ`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPDJCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPDJNMBR` | length=2000 |
| `estado` | `Long` | `TPDJIDST` |  |

### `TipoAporte` → tabla **`CRD.TPAP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPAPCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPAPNMBR` | length=2000 |
| `codigoSBS` | `String` | `TPAPCSBC` | length=50 |
| `estado` | `Long` | `TPAPIDST` |  |

### `TipoCalificacionCredito` → tabla **`CRD.TPCL`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPCLCDGO` | **PK**, IDENTITY |
| `codigoSBS` | `String` | `TPCLCSPB` | length=50 |
| `nombre` | `String` | `TPCLNMBR` | length=2000 |
| `categoria` | `String` | `TPCLCTGR` | length=2000 |
| `provision` | `Double` | `TPCLPRVS` |  |
| `estado` | `Long` | `TPCLIDST` |  |

### `TipoCesantia` → tabla **`CRD.TPCS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPCSCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPCSNMBR` | length=2000 |
| `codigoSBS` | `String` | `TPCSCSPB` | length=50 |
| `estado` | `Long` | `TPCSIDST` |  |

### `TipoContrato` → tabla **`CRD.TPCN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPCNCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPCNNMBR` | length=2000 |
| `codigoSBS` | `String` | `TPCNCSPB` | length=50 |
| `estado` | `Long` | `TPCNIDST` |  |

### `TipoGenero` → tabla **`CRD.TPGN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPGNCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPGNNMBR` | length=2000 |
| `codigoExterno` | `Long` | `TPGNCDEX` |  |
| `idEstado` | `Long` | `TPGNIDST` |  |

### `TipoHidrocarburifica` → tabla **`CRD.TPHD`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPHDCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `codigoExterno` | `Long` | `TPHDCDEX` |  |
| `estado` | `Long` | `TPHDIDST` |  |

### `TipoIdentificacion` → tabla **`CRD.TPDN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPDNCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPDNNMBR` | length=2000 |
| `estado` | `Long` | `TPDNIDST` |  |

### `TipoPago` → tabla **`CRD.TPPG`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPPGCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPPGNMBR` | length=2000 |
| `codigoSbs` | `String` | `TPPGCSPB` | length=50 |
| `tipo` | `String` | `TPPGTPOO` | length=10 |
| `observacion` | `String` | `TPPGOBSR` | length=2000 |
| `estado` | `Long` | `TPPGIDST` |  |

### `TipoParticipe` → tabla **`CRD.TPPC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPPCCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPPCNMBR` | length=2000 |
| `codigoSuperBancos` | `String` | `TPPCCSPB` | length=50 |
| `estado` | `Long` | `TPPCIDST` |  |

### `TipoPrestamo` → tabla **`CRD.TPPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPPRCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPPRNMBR` | length=2000 |
| `codigoSBS` | `String` | `TPPRCSPB` | length=50 |
| `tipo` | `String` | `TPPRTPOO` | length=50 |
| `tasa` | `Long` | `TPPRTSAA` |  |
| `estado` | `Long` | `TPPRIDST` |  |

### `TipoRequisitoPrestamo` → tabla **`CRD.TPRQ`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPRQCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPRQNMBR` | length=2000 |
| `valor` | `Long` | `TPRQVLRR` |  |
| `estado` | `Long` | `TPRQESTD` |  |

### `TipoVivienda` → tabla **`CRD.TPVV`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPVVCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPVVNMBR` | length=2000 |
| `estado` | `Long` | `TPVVIDST` |  |

### `TransaccionesAsoprep` → tabla **`CRD.TRAS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TRASTRRR` | **PK** |
| `cuentaId` | `Long` | `TRASCNTA` |  |
| `concepto` | `String` | `TRASCNCP` | length=150 |
| `saldoAnterior` | `BigDecimal` | `TRASSLDN` |  |
| `totalDebito` | `BigDecimal` | `TRASTTLD` |  |
| `totalCredito` | `BigDecimal` | `TRASTTLC` |  |
| `saldoActual` | `BigDecimal` | `TRASSLDA` |  |
| `saldoCalculado` | `BigDecimal` | `TRASSLDC` |  |
| `diferencia` | `BigDecimal` | `TRASDFRN` |  |
| `fechaDeposito` | `LocalDateTime` | `TRASFCHD` |  |
| `fechaAporte` | `LocalDateTime` | `TRASFCHP` |  |
| `debito` | `BigDecimal` | `TRASDBBB` |  |
| `credito` | `BigDecimal` | `TRASCRRR` |  |

### `ValorPagoPensionComplementaria` → tabla **`CRD.VPPC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `VPPCCDGO` | **PK**, IDENTITY |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `valorPagar` | `Double` | `VPPCVLRR` |  |
| `numeroCuotas` | `Long` | `VPPCNMCT` |  |
| `tienePrestamo` | `Long` | `VPPCTNPR` |  |
| `valorSeguro` | `Double` | `VPPCVLSR` |  |
| `estado` | `Long` | `VPPCIDST` |  |
| `usuarioIngreso` | `String` | `VPPCUSRG` | length=50 |
| `fechaIngreso` | `LocalDateTime` | `VPPCFCRG` |  |
| `usuarioModificacion` | `String` | `VPPCUSMD` | length=50 |
| `fechaModificacion` | `LocalDateTime` | `VPPCCFMD` |  |

---

## CXC — Cuentas por Cobrar (`com.saa.model.cxc`)

Constantes de entidades: `NombreEntidadesCobro`

### `AnticipoCliente` → tabla **`CBR.ANTC`**

Secuencia PK: `CBR.SQ_ANTCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `fechaAnticipo` | `LocalDate` | `FECHAANTICIPO` |  |
| `fechaRecepcion` | `LocalDate` | `FECHARECEPCION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `FECHAREGISTRO` |  |
| `numeroDoc` | `String` | `NUMERODOC` | length=100 |
| `valor` | `Double` | `VALOR` |  |
| `saldo` | `Double` | `ANTCSALD` | Saldo DISPONIBLE de este anticipo (valor − cruces activos) |
| `formaPago` | `Long` | `ANTCFPAG` |  |
| `referencia` | `String` | `ANTCREFR` | length=200 |
| `banco` | `String` | `ANTCBANC` | length=200 |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `estado` | `Long` | `ESTADO` |  |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `observacion` | `String` | `OBSERVACION` | length=2000 |

### `AplicacionPagoCxc` → tabla **`CBR.APLC`**

Secuencia PK: `CBR.SQ_APLCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `APLCCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `APLCPJRQ` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `factura` | `Factura` | `APLCFCTR` | FK → `Factura` (ID), ManyToOne |
| `liquidacion` | `LiquidacionCompra` | `APLCLQCS` | FK → `LiquidacionCompra` (ID), ManyToOne |
| `tipoDocPago` | `Long` | `APLCTDPG` |  |
| `notaCredito` | `NotaCredito` | `APLCNTCR` | FK → `NotaCredito` (ID), ManyToOne |
| `retencion` | `RetencionCompra` | `APLCRTCM` | FK → `RetencionCompra` (ID), ManyToOne |
| `anticipo` | `AnticipoCliente` | `APLCANTC` | FK → `AnticipoCliente` (ID), ManyToOne. Movimiento negativo histórico |
| `anticipoOrigen` | `AnticipoCliente` | `APLCANTO` | FK → `AnticipoCliente` (ID), ManyToOne. Anticipo del que sale el cruce |
| `formaPago` | `Long` | `APLCFPAG` |  |
| `referencia` | `String` | `APLCREFR` | length=200 |
| `banco` | `String` | `APLCBANC` | length=200 |
| `montoAplicado` | `Double` | `APLCMAPL` |  |
| `fechaAplicacion` | `LocalDate` | `APLCFAPL` |  |
| `observacion` | `String` | `APLCOBSR` | length=2000 |
| `estado` | `Long` | `APLCESTD` |  |
| `usuario` | `Usuario` | `APLCUSAR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `asiento` | `Asiento` | `APLCASNT` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `APLCFCRG` |  |

### `ComposicionCuotaInicialCobro` → tabla **`CBR.CCIC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CCICCDGO` | **PK**, IDENTITY |
| `resumenValorDocumentoCobro` | `ResumenValorDocumentoCobro` | `RVDCCDGO` | FK → `ResumenValorDocumentoCobro` (RVDCCDGO), ManyToOne |
| `valor` | `Double` | `CCICVLRR` |  |
| `valorResumen` | `Double` | `CCICVLRV` |  |
| `financiacionXDocumentoCobro` | `FinanciacionXDocumentoCobro` | `FXDCCDGO` | FK → `FinanciacionXDocumentoCobro` (FXDCCDGO), ManyToOne |

### `CuotaXFinanciacionCobro` → tabla **`CBR.CXDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CXDCCDGO` | **PK**, IDENTITY |
| `financiacionXDocumentoCobro` | `FinanciacionXDocumentoCobro` | `FXDCCDGO` | FK → `FinanciacionXDocumentoCobro` (FXDCCDGO), ManyToOne |
| `fechaIngreso` | `LocalDateTime` | `CXDCFCIN` |  |
| `fechaVencimiento` | `LocalDateTime` | `CXDCFCVN` |  |
| `tipo` | `Long` | `CXDCTPOO` |  |
| `valor` | `Double` | `CXDCVLRR` |  |
| `numeroSecuencial` | `Long` | `CXDCNMSC` |  |
| `numeroCuotaLetra` | `Long` | `CXDCNMCL` |  |
| `numeroTotalCuotas` | `Long` | `CXDCNMTC` |  |
| `totalAbono` | `Double` | `CXDCABNO` |  |
| `saldo` | `Double` | `CXDCSLDO` |  |

### `DetalleDocumentoCobro` → tabla **`CBR.DTDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTDCCDGO` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `documentoCobro` | `DocumentoCobro` | `DCMCCDGO` | FK → `DocumentoCobro` (DCMCCDGO), ManyToOne |
| `descripcion` | `String` | `PRDCCDGO` | FK → `String` (PRDCCDGO), ManyToOne |
| `cantidad` | `Double` | `DTDCCNTD` |  |
| `precioUnitario` | `Double` | `DTDCPRUN` |  |
| `subtotal` | `Double` | `DTDCSBTT` |  |
| `totalImpuesto` | `Double` | `DTDCTTIM` |  |
| `total` | `Double` | `DTDCTTLL` |  |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroLinea` | `Long` | `DTDCNMLN` |  |
| `estado` | `Long` | `DTDCESTD` |  |
| `fechaIngreso` | `Date` | `DTDCFCIN` |  |

### `DetalleFactura` → tabla **`CBR.DTFC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `Factura` | `FACTURA` | FK → `Factura` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `ProductoCobro` | `PRODUCTO` | FK → `ProductoCobro` (ID), ManyToOne |
| `codigoIVASRI` | `Long` | `CODIGOIVASRI` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleLiquidacionCompra` → tabla **`CBR.DTLC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompra` | `LIQUIDACION` | FK → `LiquidacionCompra` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `Long` | `PRODUCTO` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleNotaCredito` → tabla **`CBR.DTNC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaCredito` | `NotaCredito` | `NOTACREDITO` | FK → `NotaCredito` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `Long` | `PRODUCTO` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleNotaDebito` → tabla **`CBR.DTND`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaDebito` | `NotaDebito` | `NOTADEBITO` | FK → `NotaDebito` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleRetencion` → tabla **`CBR.DTRT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencion` | `Retencion` | `RETENCION` | FK → `Retencion` (ID), ManyToOne |
| `tipoDocReten` | `String` | `TIPODOCRETEN` | length=2 |
| `numDocReten` | `String` | `NUMDOCRETEN` | length=100 |
| `fechaEmiDoc` | `LocalDate` | `FECHAEMIDOC` |  |
| `codImpuesto` | `String` | `CODIMPUESTO` | length=2 |
| `codRetencion` | `String` | `CODRETENCION` | length=100 |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeReten` | `Double` | `PORCENTAJERETEN` |  |
| `valorReten` | `Double` | `VALORRETEN` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleRetencionV2` → tabla **`CBR.DRV2`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencionV2` | `RetencionV2` | `RETENCIONV2` | FK → `RetencionV2` (ID), ManyToOne |
| `tipoDocReten` | `String` | `TIPODOCRETEN` | length=2 |
| `numDocReten` | `String` | `NUMDOCRETEN` | length=100 |
| `fechaEmiDoc` | `LocalDate` | `FECHAEMIDOC` |  |
| `fechaReg` | `LocalDate` | `FECHAREG` |  |
| `docResAutorizacion` | `String` | `DOCRESAUTORIZACION` | length=100 |
| `docResTotalSinImpuestos` | `Double` | `DOCRESTSINIMPUESTOS` |  |
| `docResIvaCero` | `Double` | `DOCRESIVACERO` |  |
| `docResPorIva` | `Double` | `DOCRESPORIVA` |  |
| `docResTotalIva` | `Double` | `DOCRESTOTALIVA` |  |
| `docResTotal` | `Double` | `DOCRESTOTAL` |  |
| `docResForPago` | `String` | `DOCRESFORPAGO` | length=2 |
| `codImpuesto` | `String` | `CODIMPUESTO` | length=2 |
| `codRetencion` | `String` | `CODRETENCION` | length=100 |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeReten` | `Double` | `PORCENTAJERETEN` |  |
| `valorReten` | `Double` | `VALORRETEN` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DocumentoCobro` → tabla **`CBR.DCMC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DCMCCDGO` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `sRITipoDocumento` | `SRITipoDocumento` | `STDCCDGO` | FK → `SRITipoDocumento` (STDCCDGO), ManyToOne |
| `fechaDocumento` | `LocalDateTime` | `DCMCFCDC` |  |
| `razonSocial` | `String` | `DCMCRZSC` |  |
| `ruc` | `String` | `DCMCRUCC` |  |
| `direccion` | `String` | `DCMCDRCC` |  |
| `diasVencimiento` | `Long` | `DCMCDSVN` |  |
| `fechaVencimiento` | `LocalDateTime` | `DCMCFCVN` |  |
| `numeroSerie` | `String` | `DCMCSREE` |  |
| `numeroDocumentoString` | `String` | `DCMCNMRO` |  |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `mes` | `Long` | `DCMCMSSS` |  |
| `anio` | `Long` | `DCMCANOO` |  |
| `numeroAutorizacion` | `Long` | `DCMCNMAU` |  |
| `fechaAutorizacion` | `LocalDateTime` | `DCMCFCAU` |  |
| `numeroResolucion` | `String` | `DCMCNMRS` |  |
| `total` | `Double` | `DCMCTTLL` |  |
| `abono` | `Double` | `DCMCABNN` |  |
| `saldo` | `Double` | `DCMCSLDD` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `idFisico` | `Long` | `DCMCIDFS` |  |
| `tipoFormaCobro` | `Long` | `DCMCFRPG` |  |
| `numeroDocumentoNumber` | `Long` | `DCMCNMRN` |  |
| `rubroEstadoP` | `Long` | `DCMCRYYA` |  |
| `rubroEstadoH` | `Long` | `DCMCRZZA` |  |
| `detalleDocumentoCobros` | `List<DetalleDocumentoCobro>` | `—` | 1:N inversa (mappedBy=`documentoCobro`) |
| `valorImpuestoDocumentoCobros` | `List<ValorImpuestoDocumentoCobro>` | `—` | 1:N inversa (mappedBy=`documentoCobro`) |
| `resumenValorDocumentoCobros` | `List<ResumenValorDocumentoCobro>` | `—` | 1:N inversa (mappedBy=`documentoCobro`) |
| `financiacionXDocumentoCobros` | `List<FinanciacionXDocumentoCobro>` | `—` | 1:N inversa (mappedBy=`documentoCobro`) |

### `Establecimiento` → tabla **`CBR.ESTB`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `codigo` | `String` | `CODIGO` | length=250 |
| `nombre` | `String` | `NOMBRE` | length=250 |
| `descripcion` | `String` | `DESCRIPCION` | length=250 |
| `direccion` | `String` | `DIRECCION` | length=1000 |
| `telefono` | `String` | `TELEFONO` | length=1000 |
| `mail` | `String` | `MAIL` | length=45 |
| `logo` | `String` | `LOGO` | length=1000 |
| `creacion` | `LocalDateTime` | `CREACION` |  |
| `matriz` | `Long` | `MATRIZ` |  |
| `estado` | `Long` | `ESTADO` |  |

### `Factura` → tabla **`CBR.FCTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `titular` | `Titular` | `COMPRADOR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDate` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `subtotal5` | `Double` | `SUBTOTAL5` |  |
| `subtotal8` | `Double` | `SUBTOTAL8` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vIVA5` | `Double` | `VIVA5` |  |
| `vIVA8` | `Double` | `VIVA8` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `totalSinSub` | `Double` | `TOTALSINSUB` |  |
| `ahorroSub` | `Double` | `AHORROSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `formaPago` | `Long` | `FORMAPAGO` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoPago` | `Long` | `FCTREPAG` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `motivoAnulacion` | `String` | `MOTIVOANULACION` | length=1000 |
| `fechaAnulacion` | `java.time.LocalDateTime` | `FECHAANULACION` |  |
| `usuarioAnulacion` | `String` | `USUARIOANULACION` | length=200 |

### `Facturador` → tabla **`CBR.FCDR`**

Secuencia PK: `CBR.SQ_FCDR`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `numDoc` | `String` | `NUMDOC` | length=50 |
| `nombre` | `String` | `NOMBRE` | length=1000 |
| `razonSocial` | `String` | `RAZONSOCIAL` | length=1000 |
| `nombreComercial` | `String` | `NOMBRECOMERCIAL` | length=500 |
| `mail` | `String` | `MAIL` | length=200 |
| `telefono` | `String` | `TELEFONO` | length=45 |
| `direccion` | `String` | `DIRECCION` | length=1000 |
| `creado` | `LocalDateTime` | `CREADO` |  |
| `logo` | `String` | `LOGO` | length=1000 |
| `firma` | `String` | `FIRMA` | length=1000 |
| `claveFirma` | `String` | `CLAVEFIRMA` | length=500 |
| `empresaFirma` | `Long` | `EMPRESAFIRMA` |  |
| `codClave` | `String` | `CODCLAVE` | length=100 |
| `contabilidad` | `Long` | `CONTABILIDAD` |  |
| `agenteRetencion` | `String` | `AGENTERETENCION` | length=1000 |
| `contribuyenteEspecial` | `String` | `CONTRIBUYENTEESPECIAL` | length=1000 |
| `artesano` | `String` | `ARTESANO` | length=1000 |
| `microEmpresa` | `Long` | `MICROEMPRESA` |  |
| `rimpe` | `Long` | `RIMPE` |  |
| `popularRimpe` | `Long` | `POPULARRIMPE` |  |
| `turistico` | `String` | `TURISTICO` | length=1000 |
| `inicia` | `LocalDateTime` | `INICIA` |  |
| `vence` | `LocalDateTime` | `VENCE` |  |
| `docEmitidos` | `Long` | `DOCEMITIDOS` |  |
| `docPermitidos` | `Long` | `DOCPERMITIDOS` |  |
| `impCodProd` | `Long` | `IMPCODPROD` |  |
| `inventario` | `Long` | `INVENTARIO` |  |
| `empTransporte` | `Long` | `EMPTRANSPORTE` |  |
| `sinLimiteConsFinal` | `Long` | `SINLIMITECONSFINAL` |  |
| `estado` | `Long` | `ESTADO` |  |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `generaConta` | `Long` | `GENERACONTA` |  |

### `FinanciacionXDocumentoCobro` → tabla **`CBR.FXDC`**

Secuencia PK: `CBR.SQ_FXDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `FXDCCDGO` | **PK**, IDENTITY |
| `documentoCobro` | `DocumentoCobro` | `DCMCCDGO` | FK → `DocumentoCobro` (DCMCCDGO), ManyToOne |
| `tipoFinanciacion` | `Long` | `FXDCTPFN` |  |
| `aplicaInteres` | `Long` | `FXDCAPIN` |  |
| `porcentajeInteres` | `Double` | `FXDCPRIN` |  |
| `factorInteres` | `Double` | `FXDCFTIN` |  |
| `aplicaCuotaInicial` | `Long` | `FXDCAPCI` |  |
| `valorPorcentaCI` | `Double` | `FXDCVPRC` |  |
| `valorFijoCI` | `Double` | `FXDCVNMC` |  |
| `tipoCuotaInicial` | `Long` | `FXDCTPCI` |  |
| `valorInicialCI` | `Double` | `FXDCVCIB` |  |
| `valorTotalCI` | `Double` | `FXDCVTCI` |  |
| `numeroCobros` | `Long` | `FXDCNMPG` |  |
| `tipoCobros` | `Long` | `FXDCTPCA` |  |
| `rubroPeriodicidadP` | `Long` | `FXDCRYYA` |  |
| `rubroPeriodicidadH` | `Long` | `FXDCRZZA` |  |
| `tipoPeriodicidadCobro` | `Long` | `FXDCTPDP` |  |
| `dependeOtraFinanciacion` | `Long` | `FXDCDPOF` |  |
| `numeroDocumentoDepende` | `String` | `FXDCNDCD` |  |
| `idDepende` | `Long` | `FXDCIDDC` |  |

### `FormaPagoFactura` → tabla **`CBR.FPFC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `Factura` | `FACTURA` | FK → `Factura` (ID), ManyToOne |
| `formaPago` | `String` | `FORMAPAGO` | length=15 |
| `valor` | `Double` | `VALOR` |  |
| `plazo` | `Long` | `PLAZO` |  |
| `unidadTiempo` | `String` | `UNIDADTIEMPO` | length=100 |

### `FormaPagoLiquidacion` → tabla **`CBR.FPLC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompra` | `LIQUIDACION` | FK → `LiquidacionCompra` (ID), ManyToOne |
| `formaPago` | `String` | `FORMAPAGO` | length=15 |
| `valor` | `Double` | `VALOR` |  |
| `plazo` | `Long` | `PLAZO` |  |
| `unidadTiempo` | `String` | `UNIDADTIEMPO` | length=100 |

### `GrupoProductoCobro` → tabla **`CBR.GRPC`**

Secuencia PK: `CBR.SQ_GRPCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GRPCCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `GRPCNMBR` |  |
| `rubroTipoGrupoP` | `Long` | `GRPCRYYA` |  |
| `rubroTipoGrupoH` | `Long` | `GRPCRZZA` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `estado` | `Long` | `GRPCESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |

### `ImpuestoXGrupoCobro` → tabla **`CBR.IXGC`**

Secuencia PK: `CBR.SQ_IXGCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `IXGCCDGO` | **PK**, IDENTITY |
| `grupoProductoCobro` | `GrupoProductoCobro` | `GRPCCDGO` | FK → `GrupoProductoCobro` (GRPCCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `estado` | `Long` | `IXGCESTD` |  |

### `LiquidacionCompra` → tabla **`CBR.LQCS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `totalSinSub` | `Double` | `TOTALSINSUB` |  |
| `ahorroSub` | `Double` | `AHORROSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |

### `Lsri` → tabla **`CBR.LSRI`**

Secuencia PK: `CBR.SQ_LSRI`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `tabla` | `String` | `TABLA` | length=100 |
| `detalle` | `String` | `DETALLE` | length=500 |
| `estado` | `Long` | `ESTADO` |  |

### `NotaCredito` → tabla **`CBR.NTCR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `tipoDocModificado` | `String` | `TIPODOCMODIFICADO` | length=45 |
| `numDocModificado` | `String` | `NUMDOCMODIFICADO` | length=500 |
| `fechaEmisionDM` | `LocalDateTime` | `FECHAEMISIONDM` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `factura` | `Factura` | `FACTURA` | FK → `Factura` (ID), ManyToOne |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `motivoAnulacion` | `String` | `MOTIVOANULACION` | length=1000 |
| `fechaAnulacion` | `java.time.LocalDateTime` | `FECHAANULACION` |  |
| `usuarioAnulacion` | `String` | `USUARIOANULACION` | length=200 |

### `NotaDebito` → tabla **`CBR.NTDB`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `tipoDocModificado` | `String` | `TIPODOCMODIFICADO` | length=45 |
| `numDocModificado` | `String` | `NUMDOCMODIFICADO` | length=500 |
| `fechaEmisionDM` | `LocalDateTime` | `FECHAEMISIONDM` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `factura` | `Factura` | `FACTURA` | FK → `Factura` (ID), ManyToOne |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `motivoAnulacion` | `String` | `MOTIVOANULACION` | length=1000 |
| `fechaAnulacion` | `java.time.LocalDateTime` | `FECHAANULACION` |  |
| `usuarioAnulacion` | `String` | `USUARIOANULACION` | length=200 |

### `NumeracionPuntoEmision` → tabla **`CBR.NXPE`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numActual` | `Long` | `NUMACTUAL` |  |

### `PagosArbitrariosXFinanciacionCobro` → tabla **`CBR.PAFC`**

Secuencia PK: `CBR.SQ_PAFCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PAFCCDGO` | **PK**, IDENTITY |
| `financiacionXDocumentoCobro` | `FinanciacionXDocumentoCobro` | `FXDCCDGO` | FK → `FinanciacionXDocumentoCobro` (FXDCCDGO), ManyToOne |
| `diaCobro` | `Long` | `PAFCDAAA` |  |
| `mesCobro` | `Long` | `PAFCMSSS` |  |
| `anioCobro` | `Long` | `PAFCANOO` |  |
| `fechaCobro` | `LocalDateTime` | `PAFCFCPG` |  |
| `valor` | `Double` | `PAFCVLRR` |  |

### `PathFactura` → tabla **`CBR.PTFC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `Factura` | `FACTURA` | FK → `Factura` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathLiquidacionCompra` → tabla **`CBR.PTLC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompra` | `LIQUIDACION` | FK → `LiquidacionCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathNotaCredito` → tabla **`CBR.PTNC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaCredito` | `NotaCredito` | `NOTACREDITO` | FK → `NotaCredito` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathNotaDebito` → tabla **`CBR.PTND`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaDebito` | `NotaDebito` | `NOTADEBITO` | FK → `NotaDebito` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathRetencion` → tabla **`CBR.PTRT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencion` | `Retencion` | `RETENCION` | FK → `Retencion` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathRetencionV2` → tabla **`CBR.PRT2`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencionV2` | `RetencionV2` | `RETENCIONV2` | FK → `RetencionV2` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `ProductoCobro` → tabla **`CBR.PRDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `grupoProducto` | `GrupoProductoCobro` | `GRUPOPRODUCTO` | FK → `GrupoProductoCobro` (GRPCCDGO), ManyToOne |
| `nombre` | `String` | `NOMBRE` | length=1000 |
| `codigo` | `String` | `CODIGO` | length=500 |
| `codigoAux` | `String` | `CODIGOAUX` | length=500 |
| `precioUnitario` | `Double` | `PRECIOUNITARIO` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `tipoDescuento` | `Long` | `TIPODESCUENTO` |  |
| `incluyeIVA` | `Long` | `INCLUYEIVA` |  |
| `tipoIVA` | `Long` | `TIPOIVA` |  |
| `tipoICE` | `Long` | `TIPOICE` |  |
| `ice` | `Double` | `ICE` |  |
| `descripcion` | `String` | `DESCRIPCION` | length=1000 |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `irbpnr` | `Double` | `IRBPNR` |  |
| `multiPrecio` | `Long` | `MULTIPRECIO` |  |
| `stock` | `Long` | `STOCK` |  |
| `manejaUnidad` | `Long` | `MANEJAUNIDAD` |  |
| `unidad` | `Long` | `UNIDAD` |  |
| `estado` | `Long` | `ESTADO` |  |

### `PuntoEmision` → tabla **`CBR.PTEM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `codigo` | `String` | `CODIGO` | length=45 |
| `establecimiento` | `Establecimiento` | `ESTABLECIMIENTO` | FK → `Establecimiento` (ID), ManyToOne |
| `nombre` | `String` | `NOMBRE` | length=500 |
| `creado` | `LocalDateTime` | `CREADO` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `transportista` | `Long` | `TRANSPORTISTA` |  |
| `estado` | `Long` | `ESTADO` |  |

### `ResumenValorDocumentoCobro` → tabla **`CBR.RVDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RVDCCDGO` | **PK**, IDENTITY |
| `documentoCobro` | `DocumentoCobro` | `DCMCCDGO` | FK → `DocumentoCobro` (DCMCCDGO), ManyToOne |
| `codigoAlternoTipoValor` | `Long` | `RVDCCATP` |  |
| `valor` | `Double` | `RVDCVLRR` |  |

### `Retencion` → tabla **`CBR.RTNC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `proveedor` | `Titular` | `PROVEEDOR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `periodoFiscal` | `String` | `PERIODOFISCAL` | length=50 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `motivoAnulacion` | `String` | `MOTIVOANULACION` | length=1000 |
| `fechaAnulacion` | `java.time.LocalDateTime` | `FECHAANULACION` |  |
| `usuarioAnulacion` | `String` | `USUARIOANULACION` | length=200 |

### `RetencionV2` → tabla **`CBR.RTV2`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `facturador` | `Facturador` | `FACTURADOR` | FK → `Facturador` (ID), ManyToOne |
| `proveedor` | `Titular` | `PROVEEDOR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `periodoFiscal` | `String` | `PERIODOFISCAL` | length=50 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `PuntoEmision` | `PTOEMISION` | FK → `PuntoEmision` (ID), ManyToOne |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `motivoAnulacion` | `String` | `MOTIVOANULACION` | length=1000 |
| `fechaAnulacion` | `LocalDateTime` | `FECHAANULACION` |  |
| `usuarioAnulacion` | `String` | `USUARIOANULACION` | length=200 |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `TempComposicionCuotaInicialCobro` → tabla **`CBR.TCIC`**

Secuencia PK: `CBR.SQ_TCICCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCICCDGO` | **PK**, SEQUENCE |
| `tempResumenValorDocumentoCobro` | `TempResumenValorDocumentoCobro` | `TRDCCDGO` | FK → `TempResumenValorDocumentoCobro` (TRDCCDGO), ManyToOne |
| `valor` | `Double` | `TCICVLRR` |  |
| `valorResumen` | `Double` | `TCICVLRV` |  |
| `tempFinanciacionXDocumentoCobro` | `TempFinanciacionXDocumentoCobro` | `TFDCCDGO` | FK → `TempFinanciacionXDocumentoCobro` (TFDCCDGO), ManyToOne |

### `TempCuotaXFinanciacionCobro` → tabla **`CBR.TCDC`**

Secuencia PK: `CBR.SQ_TCDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCDCCDGO` | **PK**, SEQUENCE |
| `tempFinanciacionXDocumentoCobro` | `TempFinanciacionXDocumentoCobro` | `TFDCCDGO` | FK → `TempFinanciacionXDocumentoCobro` (TFDCCDGO), ManyToOne |
| `fechaIngreso` | `LocalDateTime` | `TCDCFCIN` |  |
| `fechaVencimiento` | `LocalDateTime` | `TCDCFCVN` |  |
| `tipo` | `Long` | `TCDCTPOO` |  |
| `valor` | `Double` | `TCDCVLRR` |  |
| `numeroSecuencial` | `Long` | `TCDCNMSC` |  |
| `numeroCuotaLetra` | `Long` | `TCDCNMCL` |  |
| `numeroTotalCuotas` | `Long` | `TCDCNMTC` |  |
| `totalAbono` | `Double` | `TCDCABNO` |  |
| `saldo` | `Double` | `TCDCSLDO` |  |

### `TempDetalleDocumentoCobro` → tabla **`CBR.TDTC`**

Secuencia PK: `CBR.SQ_TDTCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TDTCCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `tempDocumentoCobro` | `TempDocumentoCobro` | `TDCCCDGO` | FK → `TempDocumentoCobro` (TDCCCDGO), ManyToOne |
| `descripcion` | `String` | `PRDCCDGO` | FK → `String` (PRDCCDGO), ManyToOne |
| `cantidad` | `Double` | `TDTCCNTD` |  |
| `precioUnitario` | `Double` | `TDTCPRUN` |  |
| `subtotal` | `Double` | `TDTCSBTT` |  |
| `totalImpuesto` | `Double` | `TDTCTTIM` |  |
| `total` | `Double` | `TDTCTTLL` |  |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroLinea` | `Long` | `TDTCNMLN` |  |
| `estado` | `Long` | `TDTCESTD` |  |
| `fechaIngreso` | `Date` | `TDTCFCIN` |  |

### `TempDocumentoCobro` → tabla **`CBR.TDCC`**

Secuencia PK: `CBR.SQ_TDCCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TDCCCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `sRITipoDocumento` | `SRITipoDocumento` | `STDCCDGO` | FK → `SRITipoDocumento` (STDCCDGO), ManyToOne |
| `fechaDocumento` | `LocalDateTime` | `TDCCFCDC` |  |
| `razonSocial` | `String` | `TDCCRZSC` |  |
| `ruc` | `String` | `TDCCRUCC` |  |
| `direccion` | `String` | `TDCCDRCC` |  |
| `diasVencimiento` | `Long` | `TDCCDSVN` |  |
| `fechaVencimiento` | `LocalDateTime` | `TDCCFCVN` |  |
| `numeroSerie` | `String` | `TDCCSREE` |  |
| `numeroDocumentoString` | `String` | `TDCCNMRO` |  |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `mes` | `Long` | `TDCCMSSS` |  |
| `anio` | `Long` | `TDCCANIO` |  |
| `numeroAutorizacion` | `Long` | `TDCCNMAT` |  |
| `fechaAutorizacion` | `LocalDateTime` | `TDCCFCAT` |  |
| `numeroResolucion` | `String` | `TDCCRSLN` |  |
| `total` | `Double` | `TDCCTOTT` |  |
| `abono` | `Double` | `TDCCABNO` |  |
| `saldo` | `Double` | `TDCCSLDO` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `idFisico` | `Long` | `TDCCFISC` |  |
| `tipoFormaCobro` | `Long` | `TDCCTPFO` |  |
| `numeroDocumentoNumber` | `Long` | `TDCCNMNR` |  |
| `rubroEstadoP` | `Long` | `TDCCRBE1` |  |
| `rubroEstadoH` | `Long` | `TDCCRBE2` |  |

### `TempFinanciacionXDocumentoCobro` → tabla **`CBR.TFDC`**

Secuencia PK: `CBR.SQ_TFDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TFDCCDGO` | **PK**, SEQUENCE |
| `tempDocumentoCobro` | `TempDocumentoCobro` | `TDCCCDGO` | FK → `TempDocumentoCobro` (TDCCCDGO), ManyToOne |
| `tipoFinanciacion` | `Long` | `TFDCTPFN` |  |
| `aplicaInteres` | `Long` | `TFDCAPIN` |  |
| `porcentajeInteres` | `Double` | `TFDCPRIN` |  |
| `factorInteres` | `Double` | `TFDCFTIN` |  |
| `aplicaCuotaInicial` | `Long` | `TFDCAPCI` |  |
| `valorPorcentaCI` | `Double` | `TFDCVPRC` |  |
| `valorFijoCI` | `Double` | `TFDCVNMC` |  |
| `tipoCuotaInicial` | `Long` | `TFDCTPCI` |  |
| `valorInicialCI` | `Double` | `TFDCVCIB` |  |
| `valorTotalCI` | `Double` | `TFDCVTCL` |  |
| `numeroCobros` | `Long` | `TFDCNMCO` |  |
| `tipoCobros` | `Long` | `TFDCTPCB` |  |
| `rubroPeriodicidadP` | `Long` | `TFDCRBRP` |  |
| `rubroPeriodicidadH` | `Long` | `TFDCRBRH` |  |
| `tipoPeriodicidadCobro` | `Long` | `TFDCTPPD` |  |
| `dependeOtraFinanciacion` | `Long` | `TFDCDPOF` |  |
| `numeroDocumentoDepende` | `String` | `TFDCNMDP` |  |
| `idDepende` | `Long` | `TFDCDPFD` |  |
| `tempCuotaXFinanciacionCobros` | `List<TempCuotaXFinanciacionCobro>` | `—` | 1:N inversa (mappedBy=`tempFinanciacionXDocumentoCobro`) |

### `TempPagosArbitrariosXFinanciacionCobro` → tabla **`CBR.TPFC`**

Secuencia PK: `CBR.SQ_TPFCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPFCCDGO` | **PK**, SEQUENCE |
| `tempFinanciacionXDocumentoCobro` | `TempFinanciacionXDocumentoCobro` | `TFDCCDGO` | FK → `TempFinanciacionXDocumentoCobro` (TFDCCDGO), ManyToOne |
| `diaCobro` | `Long` | `TPFCDAAA` |  |
| `mesCobro` | `Long` | `TPFCMSSS` |  |
| `anioCobro` | `Long` | `TPFCANOO` |  |
| `fechaCobro` | `LocalDateTime` | `TPFCFCPG` |  |
| `valor` | `Double` | `TPFCVLRR` |  |

### `TempResumenValorDocumentoCobro` → tabla **`CBR.TRDC`**

Secuencia PK: `CBR.SQ_TRDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TRDCCDGO` | **PK**, IDENTITY |
| `tempDocumentoCobro` | `TempDocumentoCobro` | `TDCCCDGO` | FK → `TempDocumentoCobro` (TDCCCDGO), ManyToOne |
| `codigoAlternoTipoValor` | `Long` | `TRDCCATP` |  |
| `valor` | `Double` | `TRDCVLRR` |  |

### `TempValorImpuestoDetalleCobro` → tabla **`CBR.TITC`**

Secuencia PK: `CBR.SQ_TITCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TITCCDGO` | **PK**, SEQUENCE |
| `tempDetalleDocumentoCobro` | `TempDetalleDocumentoCobro` | `TDTCCDGO` | FK → `TempDetalleDocumentoCobro` (TDTCCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `TITCNMBR` |  |
| `porcentaje` | `Double` | `TITCPRCT` |  |
| `valorBase` | `Double` | `TITCVLSA` |  |
| `valor` | `Double` | `TITCVLRR` |  |

### `TempValorImpuestoDocumentoCobro` → tabla **`CBR.TIDC`**

Secuencia PK: `CBR.SQ_TIDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TIDCCDGO` | **PK**, SEQUENCE |
| `tempDocumentoCobro` | `TempDocumentoCobro` | `TDCCCDGO` | FK → `TempDocumentoCobro` (TDCCCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `TIDCNMBR` |  |
| `porcentaje` | `Double` | `TIDCPRCT` |  |
| `codigoAlternoValor` | `Long` | `TIDCCAVD` |  |
| `valorBase` | `Double` | `TIDCVLSA` |  |
| `valor` | `Double` | `TIDCVLRR` |  |

### `Tsri` → tabla **`CBR.TSRI`**

Secuencia PK: `CBR.SQ_TSRI`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `lsri` | `Lsri` | `LSRI` | FK → `Lsri` (TABLA), ManyToOne |
| `codigo` | `String` | `CODIGO` | length=500 |
| `detalle` | `String` | `DETALLE` | length=1000 |
| `porcentaje` | `Double` | `PORCENTAJE` |  |
| `valor` | `Double` | `VALOR` |  |
| `texto` | `String` | `TEXTO` | length=1000 |
| `estado` | `Long` | `ESTADO` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |

### `ValorImpuestoDetalleCobro` → tabla **`CBR.VITC`**

Secuencia PK: `CBR.SQ_VITCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `VITCCDGO` | **PK**, SEQUENCE |
| `detalleDocumentoCobro` | `DetalleDocumentoCobro` | `DTDCCDGO` | FK → `DetalleDocumentoCobro` (DTDCCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `VITCNMBR` |  |
| `porcentaje` | `Double` | `VITCPRCT` |  |
| `valorBase` | `Double` | `VITCVLSA` |  |
| `valor` | `Double` | `VITCVLRR` |  |

### `ValorImpuestoDocumentoCobro` → tabla **`CBR.VIDC`**

Secuencia PK: `CBR.SQ_VIDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `VIDCCDGO` | **PK**, SEQUENCE |
| `documentoCobro` | `DocumentoCobro` | `DCMCCDGO` | FK → `DocumentoCobro` (DCMCCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `VIDCNMBR` |  |
| `porcentaje` | `Double` | `VIDCPRCT` |  |
| `codigoAlternoValor` | `Long` | `VIDCCAVD` |  |
| `valorBase` | `Double` | `VIDCVLSA` |  |
| `valor` | `Double` | `VIDCVLRR` |  |

---

## CXP — Cuentas por Pagar (`com.saa.model.cxp`)

Constantes de entidades: `NombreEntidadesCompra
NombreEntidadesPago`

### `AdendumNegociacion` → tabla **`PGS.ADNG`**

Secuencia PK: `PGS.SQ_ADNGCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `negociacion` | `NegociacionProveedor` | `NEGOCIACION` | FK → `NegociacionProveedor` (ID), ManyToOne |
| `numAdendum` | `String` | `NUMADENDUM` | length=200 |
| `fechaAdendum` | `LocalDate` | `FECHAADENDUM` |  |
| `descripcion` | `String` | `DESCRIPCION` | length=2000 |
| `valorAjuste` | `Double` | `VALORAJUSTE` |  |
| `valorTotalResultante` | `Double` | `VALORTOTALRESULTANTE` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `estado` | `Long` | `ESTADO` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `FECHAREGISTRO` |  |

### `AnticipoProveedor` → tabla **`PGS.ANTP`**

Secuencia PK: `PGS.SQ_ANTPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ANTPCDGO` | **PK**, SEQUENCE |
| `titular` | `Titular` | `ANTPTTLR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `empresa` | `Empresa` | `ANTPPJRQ` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaAnticipo` | `LocalDate` | `ANTPFANT` |  |
| `fechaRecepcion` | `LocalDate` | `ANTPFRCP` |  |
| `numeroDoc` | `String` | `ANTPNDOC` | length=100 |
| `valor` | `Double` | `ANTPVLOR` |  |
| `saldo` | `Double` | `ANTPSALD` | Saldo DISPONIBLE de este anticipo (valor − cruces activos) |
| `formaPago` | `Long` | `ANTPFPAG` |  |
| `referencia` | `String` | `ANTPREFR` | length=200 |
| `banco` | `String` | `ANTPBANC` | length=200 |
| `observacion` | `String` | `ANTPOBSR` | length=2000 |
| `estado` | `Long` | `ANTPESTD` |  |
| `usuario` | `Usuario` | `ANTPUSAR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `asiento` | `Asiento` | `ANTPASNT` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `ANTPFCRG` |  |

### `AplicacionPagoCxp` → tabla **`PGS.APLP`**

Secuencia PK: `PGS.SQ_APLPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `APLPCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `APLPPJRQ` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `facturaCompra` | `FacturaCompra` | `APLPFCTC` | FK → `FacturaCompra` (ID), ManyToOne |
| `tipoDocPago` | `Long` | `APLPTDPG` |  |
| `notaCredito` | `NotaCreditoCompra` | `APLPNTCC` | FK → `NotaCreditoCompra` (ID), ManyToOne |
| `retencion` | `Retencion` | `APLPRTNC` | FK → `Retencion` (ID), ManyToOne |
| `anticipo` | `AnticipoProveedor` | `APLPANTP` | FK → `AnticipoProveedor` (ANTPCDGO), ManyToOne. Movimiento negativo histórico |
| `anticipoOrigen` | `AnticipoProveedor` | `APLPANTO` | FK → `AnticipoProveedor` (ANTPCDGO), ManyToOne. Anticipo del que sale el cruce |
| `formaPago` | `Long` | `APLPFPAG` |  |
| `referencia` | `String` | `APLPREFR` | length=200 |
| `banco` | `String` | `APLPBANC` | length=200 |
| `montoAplicado` | `Double` | `APLPMAPL` |  |
| `fechaAplicacion` | `LocalDate` | `APLPFAPL` |  |
| `observacion` | `String` | `APLPOBSR` | length=2000 |
| `estado` | `Long` | `APLPESTD` |  |
| `usuario` | `Usuario` | `APLPUSAR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `asiento` | `Asiento` | `APLPASNT` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `APLPFCRG` |  |

### `AprobacionXMonto` → tabla **`PGS.APXM`**

Secuencia PK: `PGS.SQ_APXMCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `APXMCDGO` | **PK**, SEQUENCE |
| `montoAprobacion` | `MontoAprobacion` | `MNAPCDGO` | FK → `MontoAprobacion` (MNAPCDGO), ManyToOne |
| `nombreNivel` | `String` | `APXMNMBR` |  |
| `estado` | `Long` | `APXMESTD` |  |
| `fechaIngreso` | `Date` | `APXMFCIN` |  |
| `usuarioIngresa` | `String` | `APXMNUIN` |  |
| `ordenAprobacion` | `Long` | `APXMORDN` |  |
| `seleccionaBanco` | `Long` | `APXMSLBN` |  |

### `AprobacionXProposicionPago` → tabla **`PGS.AXPR`**

Secuencia PK: `PGS.SQ_AXPRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `AXPRCDGO` | **PK**, SEQUENCE |
| `proposicionPagoXCuota` | `ProposicionPagoXCuota` | `PRPDCDGO` | FK → `ProposicionPagoXCuota` (PRPDCDGO), ManyToOne |
| `fechaAprobacion` | `Date` | `AXPRFCAP` |  |
| `nivelAprobacion` | `Long` | `AXPRNVAP` |  |
| `usuarioAprueba` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `nombreUsuarioAprueba` | `String` | `AXPRNMUS` |  |
| `estado` | `Long` | `AXPRESTD` |  |
| `observacion` | `String` | `AXPROBSR` |  |

### `CargaArchivoTxt` → tabla **`PGS.CRTX`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `CRTXCDGO` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `CRTXPJRQ` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `usuario` | `Usuario` | `CRTXUSAR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaCarga` | `LocalDateTime` | `CRTXFCGA` |  |
| `nombreArchivo` | `String` | `CRTXNARV` | length=500 |
| `totalRegistros` | `Long` | `CRTXTTLR` |  |
| `registrosNuevos` | `Long` | `CRTXRGNV` |  |
| `registrosDuplicados` | `Long` | `CRTXRGDP` |  |
| `registrosNovedad` | `Long` | `CRTXRGND` |  |
| `estado` | `Long` | `CRTXESTD` |  |
| `observacion` | `String` | `CRTXOBSR` | length=2000 |
| `periodoContable` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |

### `ComposicionCuotaInicialPago` → tabla **`PGS.CCIP`**

Secuencia PK: `PGS.SQ_CCIPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CCIPCDGO` | **PK**, SEQUENCE |
| `resumenValorDocumentoPago` | `ResumenValorDocumentoPago` | `RVDPCDGO` | FK → `ResumenValorDocumentoPago` (RVDPCDGO), ManyToOne |
| `valor` | `Double` | `CCIPVLRR` |  |
| `valorResumen` | `Double` | `CCIPVLRV` |  |
| `financiacionXDocumentoPago` | `FinanciacionXDocumentoPago` | `FXDPCDGO` | FK → `FinanciacionXDocumentoPago` (FXDPCDGO), ManyToOne |

### `CuotaXFinanciacionPago` → tabla **`PGS.CXDP`**

Secuencia PK: `PGS.SQ_CXDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CXDPCDGO` | **PK**, SEQUENCE |
| `financiacionXDocumentoPago` | `FinanciacionXDocumentoPago` | `FXDPCDGO` | FK → `FinanciacionXDocumentoPago` (FXDPCDGO), ManyToOne |
| `fechaIngreso` | `Date` | `CXDPFCIN` |  |
| `fechaVencimiento` | `Date` | `CXDPFCVN` |  |
| `tipo` | `Long` | `CXDPTPOO` |  |
| `valor` | `Double` | `CXDPVLRR` |  |
| `numeroSecuencial` | `Long` | `CXDPNMSC` |  |
| `numeroCuotaLetra` | `Long` | `CXDPNMCL` |  |
| `numeroTotalCuotas` | `Long` | `CXDPNMTC` |  |
| `totalAbono` | `Double` | `CXDPABNO` |  |
| `saldo` | `Double` | `CXDPSLDO` |  |

### `DetalleCargaTxt` → tabla **`PGS.DCTX`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `DCTXCDGO` | **PK**, IDENTITY |
| `cargaTxt` | `CargaArchivoTxt` | `CRTXCDGO` | FK → `CargaArchivoTxt` (CRTXCDGO), ManyToOne |
| `documento` | `DocumentoCxp` | `DCXPCDGO` | FK → `DocumentoCxp` (DCXPCDGO), ManyToOne |
| `valorSinImpuestosCarga` | `Double` | `DCTXVSIM` |  |
| `ivaCarga` | `Double` | `DCTXIVAA` |  |
| `importeTotalCarga` | `Double` | `DCTXIMTT` |  |
| `fechaAutorizacionCarga` | `LocalDateTime` | `DCTXFAUT` |  |
| `fechaEmisionCarga` | `LocalDateTime` | `DCTXFEMS` |  |
| `resultado` | `Long` | `DCTXRSLT` |  |
| `observacion` | `String` | `DCTXOBSR` | length=2000 |

### `DetalleDocumentoPago` → tabla **`PGS.DTDP`**

Secuencia PK: `PGS.SQ_DTDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTDPCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `documentoPago` | `DocumentoPago` | `DCMPCDGO` | FK → `DocumentoPago` (DCMPCDGO), ManyToOne |
| `descripcion` | `String` | `PRDPCDGO` | FK → `String` (PRDPCDGO), ManyToOne |
| `cantidad` | `Double` | `DTDPCNTD` |  |
| `precioUnitario` | `Double` | `DTDPPRUN` |  |
| `subtotal` | `Double` | `DTDPSBTT` |  |
| `totalImpuesto` | `Double` | `DTDPTTIM` |  |
| `total` | `Double` | `DTDPTTLL` |  |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroLinea` | `Long` | `DTDPNMLN` |  |
| `estado` | `Long` | `DTDPESTD` |  |
| `fechaIngreso` | `Date` | `DTDPFCIN` |  |

### `DetalleFacturaCompra` → tabla **`PGS.DFCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `FacturaCompra` | `FACTURA` | FK → `FacturaCompra` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `Long` | `PRODUCTO` |  |
| `codigoIVASRI` | `Long` | `CODIGOIVASRI` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleLiquidacionCompraCompra` → tabla **`PGS.DLCM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompraCompra` | `LIQUIDACION` | FK → `LiquidacionCompraCompra` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `Long` | `PRODUCTO` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleNotaCreditoCompra` → tabla **`PGS.DTCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaCredito` | `NotaCreditoCompra` | `NOTACREDITO` | FK → `NotaCreditoCompra` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `producto` | `Long` | `PRODUCTO` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleNotaDebitoCompra` → tabla **`PGS.DTDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaDebito` | `NotaDebitoCompra` | `NOTADEBITO` | FK → `NotaDebitoCompra` (ID), ManyToOne |
| `descripcion` | `String` | `DESCRIPCION` | length=500 |
| `cantidad` | `Double` | `CANTIDAD` |  |
| `valor` | `Double` | `VALOR` |  |
| `subTotal` | `Double` | `SUBTOTAL` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeIVA` | `Long` | `PORCENTAJEIVA` |  |
| `valorIVA` | `Double` | `VALORIVA` |  |
| `porcentajeICE` | `Long` | `PORCENTAJEICE` |  |
| `valorICE` | `Double` | `VALORICE` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleRetencionCompra` → tabla **`PGS.DRCM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencion` | `RetencionCompra` | `RETENCION` | FK → `RetencionCompra` (ID), ManyToOne |
| `tipoDocReten` | `String` | `TIPODOCRETEN` | length=2 |
| `numDocReten` | `String` | `NUMDOCRETEN` | length=100 |
| `fechaEmiDoc` | `LocalDate` | `FECHAEMIDOC` |  |
| `codImpuesto` | `String` | `CODIMPUESTO` | length=2 |
| `codRetencion` | `String` | `CODRETENCION` | length=100 |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeReten` | `Double` | `PORCENTAJERETEN` |  |
| `valorReten` | `Double` | `VALORRETEN` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DetalleRetencionCompraV2` → tabla **`PGS.DRC2`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencionCompraV2` | `RetencionCompraV2` | `RETENCIONV2` | FK → `RetencionCompraV2` (ID), ManyToOne |
| `tipoDocReten` | `String` | `TIPODOCRETEN` | length=2 |
| `numDocReten` | `String` | `NUMDOCRETEN` | length=100 |
| `fechaEmiDoc` | `LocalDate` | `FECHAEMIDOC` |  |
| `fechaReg` | `LocalDate` | `FECHAREG` |  |
| `docResAutorizacion` | `String` | `DOCRESAUTORIZACION` | length=100 |
| `docResTotalSinImpuestos` | `Double` | `DOCRESTSINIMPUESTOS` |  |
| `docResIvaCero` | `Double` | `DOCRESIVACERO` |  |
| `docResPorIva` | `Double` | `DOCRESPORIVA` |  |
| `docResTotalIva` | `Double` | `DOCRESTOTALIVA` |  |
| `docResTotal` | `Double` | `DOCRESTOTAL` |  |
| `docResForPago` | `String` | `DOCRESFORPAGO` | length=2 |
| `codImpuesto` | `String` | `CODIMPUESTO` | length=2 |
| `codRetencion` | `String` | `CODRETENCION` | length=100 |
| `baseImponible` | `Double` | `BASEIMPONIBLE` |  |
| `porcentajeReten` | `Double` | `PORCENTAJERETEN` |  |
| `valorReten` | `Double` | `VALORRETEN` |  |
| `estado` | `Long` | `ESTADO` |  |

### `DocumentoCxp` → tabla **`PGS.DCXP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `DCXPCDGO` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `DCXPPJRQ` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `rucEmisor` | `String` | `DCXPRCEM` | length=20 |
| `razonSocialEmisor` | `String` | `DCXPRSEM` | length=500 |
| `tipoComprobante` | `String` | `DCXPTPCM` | length=100 |
| `serieComprobante` | `String` | `DCXPSRCM` | length=50 |
| `claveAcceso` | `String` | `DCXPCLAC` | length=100, UNIQUE |
| `fechaAutorizacion` | `LocalDateTime` | `DCXPFAUT` |  |
| `fechaEmision` | `LocalDateTime` | `DCXPFEMS` |  |
| `identificacionReceptor` | `String` | `DCXPIDRC` | length=20 |
| `valorSinImpuestos` | `Double` | `DCXPVSIM` |  |
| `iva` | `Double` | `DCXPIVAA` |  |
| `importeTotal` | `Double` | `DCXPIMTT` |  |
| `numeroDocumentoModificado` | `String` | `DCXPNDMD` | length=50 |
| `estadoDocumento` | `Long` | `DCXPESTD` |  |
| `pathXml` | `String` | `DCXPPXML` | length=2000 |
| `fechaCargaXml` | `LocalDateTime` | `DCXPFCXM` |  |
| `usuarioCargaXml` | `Usuario` | `DCXPUCXM` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `idDocumentoBD` | `Long` | `DCXPIDBD` |  |
| `tipoTablaDestino` | `String` | `DCXPTBTD` | length=50 |
| `fechaRegistroBD` | `LocalDateTime` | `DCXPFRBD` |  |
| `usuarioRegistroBD` | `Usuario` | `DCXPURBD` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaReversion` | `LocalDateTime` | `DCXPFRVS` |  |
| `usuarioReversion` | `Usuario` | `DCXPURVS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `novedad` | `String` | `DCXPNVDD` | length=2000 |
| `estadoNovedad` | `Long` | `DCXPENOV` |  |
| `observacion` | `String` | `DCXPOBSR` | length=2000 |
| `periodoContable` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |

### `DocumentoPago` → tabla **`PGS.DCMP`**

Secuencia PK: `PGS.SQ_DCMPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DCMPCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `sRITipoDocumento` | `SRITipoDocumento` | `STDCCDGO` | FK → `SRITipoDocumento` (STDCCDGO), ManyToOne |
| `fechaDocumento` | `Date` | `DCMPFCDC` |  |
| `razonSocial` | `String` | `DCMPRZSC` |  |
| `ruc` | `String` | `DCMPRUCC` |  |
| `direccion` | `String` | `DCMPDRCC` |  |
| `diasVencimiento` | `Long` | `DCMPDSVN` |  |
| `fechaVencimiento` | `Date` | `DCMPFCVN` |  |
| `numeroSerie` | `String` | `DCMPSREE` |  |
| `numeroDocumentoString` | `String` | `DCMPNMRO` |  |
| `perido` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `mes` | `Long` | `DCMPMSSS` |  |
| `anio` | `Long` | `DCMPANOO` |  |
| `numeroAutorizacion` | `Long` | `DCMPNMAU` |  |
| `fechaAutorizacion` | `Date` | `DCMPFCAU` |  |
| `numeroResolucion` | `String` | `DCMPNMRS` |  |
| `total` | `Double` | `DCMPTTLL` |  |
| `abono` | `Double` | `DCMPABNN` |  |
| `saldo` | `Double` | `DCMPSLDD` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `idFisico` | `Long` | `DCMPIDFS` |  |
| `tipoFormaPago` | `Long` | `DCMPFRPG` |  |
| `numeroDocumentoNumber` | `Long` | `DCMPNMRN` |  |
| `rubroEstadoP` | `Long` | `DCMPRYYA` |  |
| `rubroEstadoH` | `Long` | `DCMPRZZA` |  |

### `FacturaCompra` → tabla **`PGS.FCTC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `subtotal5` | `Double` | `SUBTOTAL5` |  |
| `subtotal8` | `Double` | `SUBTOTAL8` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vIVA5` | `Double` | `VIVA5` |  |
| `vIVA8` | `Double` | `VIVA8` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `totalSinSub` | `Double` | `TOTALSINSUB` |  |
| `ahorroSub` | `Double` | `AHORROSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `formaPago` | `Long` | `FORMAPAGO` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `estadoPago` | `Long` | `FCTCEPAG` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `FinanciacionXDocumentoPago` → tabla **`PGS.FXDP`**

Secuencia PK: `PGS.SQ_FXDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `FXDPCDGO` | **PK**, SEQUENCE |
| `documentoPago` | `DocumentoPago` | `DCMPCDGO` | FK → `DocumentoPago` (DCMPCDGO), ManyToOne |
| `tipoFinanciacion` | `Long` | `FXDPTPFN` |  |
| `aplicaInteres` | `Long` | `FXDPAPIN` |  |
| `porcentajeInteres` | `Double` | `FXDPPRIN` |  |
| `factorInteres` | `Double` | `FXDPFTIN` |  |
| `aplicaCuotaInicial` | `Long` | `FXDPAPCI` |  |
| `valorPorcentaCI` | `Double` | `FXDPVPRC` |  |
| `valorFijoCI` | `Double` | `FXDPVNMC` |  |
| `tipoCuotaInicial` | `Long` | `FXDPTPCI` |  |
| `valorInicialCI` | `Double` | `FXDPVCIB` |  |
| `valorTotalCI` | `Double` | `FXDPVTCI` |  |
| `numeroPagos` | `Long` | `FXDPNMPG` |  |
| `tipoPagos` | `Long` | `FXDPTPCA` |  |
| `rubroPeriodicidadP` | `Long` | `FXDPRYYA` |  |
| `rubroPeriodicidadH` | `Long` | `FXDPRZZA` |  |
| `tipoPeriodicidadPago` | `Long` | `FXDPTPDP` |  |
| `dependeOtraFinanciacion` | `Long` | `FXDPDPOF` |  |
| `numeroDocumentoDepende` | `String` | `FXDPNDCD` |  |
| `idDepende` | `Long` | `FXDPIDDC` |  |

### `FormaPagoFacturaCompra` → tabla **`PGS.FPFM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `FacturaCompra` | `FACTURA` | FK → `FacturaCompra` (ID), ManyToOne |
| `formaPago` | `String` | `FORMAPAGO` | length=15 |
| `valor` | `Double` | `VALOR` |  |
| `plazo` | `Long` | `PLAZO` |  |
| `unidadTiempo` | `String` | `UNIDADTIEMPO` | length=100 |

### `FormaPagoLiquidacionCompraCompra` → tabla **`PGS.FPLM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompraCompra` | `LIQUIDACION` | FK → `LiquidacionCompraCompra` (ID), ManyToOne |
| `formaPago` | `String` | `FORMAPAGO` | length=15 |
| `valor` | `Double` | `VALOR` |  |
| `plazo` | `Long` | `PLAZO` |  |
| `unidadTiempo` | `String` | `UNIDADTIEMPO` | length=100 |

### `FormaPagoNegociacion` → tabla **`PGS.FPNG`**

Secuencia PK: `PGS.SQ_FPNGCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `negociacion` | `NegociacionProveedor` | `NEGOCIACION` | FK → `NegociacionProveedor` (ID), ManyToOne |
| `numeroCuota` | `Long` | `NUMEROCUOTA` |  |
| `descripcion` | `String` | `DESCRIPCION` | length=1000 |
| `fechaPago` | `LocalDate` | `FECHAPAGO` |  |
| `porcentaje` | `Double` | `PORCENTAJE` |  |
| `valorCuota` | `Double` | `VALORCUOTA` |  |
| `estado` | `Long` | `ESTADO` |  |
| `orden` | `Long` | `ORDEN` |  |

### `GrupoProductoPago` → tabla **`PGS.GRPP`**

Secuencia PK: `PGS.SQ_GRPPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GRPPCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `GRPPNMBR` |  |
| `rubroTipoGrupoP` | `Long` | `GRPPRYYA` |  |
| `rubroTipoGrupoH` | `Long` | `GRPPRZZA` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `estado` | `Long` | `GRPPESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |

### `ImpuestoXGrupoPago` → tabla **`PGS.IXGP`**

Secuencia PK: `PGS.SQ_IXGPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `IXGPCDGO` | **PK**, SEQUENCE |
| `grupoProductoPago` | `GrupoProductoPago` | `GRPPCDGO` | FK → `GrupoProductoPago` (GRPPCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `estado` | `Long` | `IXGPESTD` |  |

### `LiquidacionCompraCompra` → tabla **`PGS.LQCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `totalSinSub` | `Double` | `TOTALSINSUB` |  |
| `ahorroSub` | `Double` | `AHORROSUB` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `Lsri` → tabla **`PGS.LSRI`**

Secuencia PK: `PGS.SQ_LSRI`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `tabla` | `String` | `TABLA` | length=100 |
| `detalle` | `String` | `DETALLE` | length=500 |
| `estado` | `Long` | `ESTADO` |  |

### `MontoAprobacion` → tabla **`PGS.MNAP`**

Secuencia PK: `PGS.SQ_MNAPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MNAPCDGO` | **PK**, SEQUENCE |
| `valorDesde` | `Double` | `MNAPVLDS` |  |
| `valorHasta` | `Double` | `MNAPVLHS` |  |
| `fechaIngreso` | `Date` | `MNAPFCIN` |  |
| `usuarioIngresa` | `String` | `MNAPNUIN` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |

### `NegociacionProveedor` → tabla **`PGS.NGCP`**

Secuencia PK: `PGS.SQ_NGCPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `fechaNegociacion` | `LocalDate` | `FECHANEGOCIACION` |  |
| `fechaInicio` | `LocalDate` | `FECHAINICIO` |  |
| `fechaFin` | `LocalDate` | `FECHAFIN` |  |
| `numContrato` | `String` | `NUMCONTRATO` | length=200 |
| `descripcion` | `String` | `DESCRIPCION` | length=2000 |
| `valorTotal` | `Double` | `VALORTOTAL` |  |
| `tipoFinanciacion` | `String` | `TIPOFINANCIACION` | length=50 |
| `numeroPagos` | `Long` | `NUMEROPAGOS` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `estado` | `Long` | `ESTADO` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `FECHAREGISTRO` |  |
| `usuarioModif` | `Usuario` | `USUARIOMODIF` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaModif` | `LocalDateTime` | `FECHAMODIF` |  |

### `NotaCreditoCompra` → tabla **`PGS.NTCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `tipoDocModificado` | `String` | `TIPODOCMODIFICADO` | length=45 |
| `numDocModificado` | `String` | `NUMDOCMODIFICADO` | length=500 |
| `fechaEmisionDM` | `LocalDateTime` | `FECHAEMISIONDM` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `NotaDebitoCompra` → tabla **`PGS.NTDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TITULAR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `tipoDocModificado` | `String` | `TIPODOCMODIFICADO` | length=45 |
| `numDocModificado` | `String` | `NUMDOCMODIFICADO` | length=500 |
| `fechaEmisionDM` | `LocalDateTime` | `FECHAEMISIONDM` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `subtotal` | `Double` | `SUBTOTAL` |  |
| `subcero` | `Double` | `SUBCERO` |  |
| `pIVA` | `Double` | `PIVA` |  |
| `vIVA` | `Double` | `VIVA` |  |
| `vICE` | `Double` | `VICE` |  |
| `vIRBPNR` | `Double` | `VIRBPNR` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `porDescuento` | `Double` | `PORDESCUENTO` |  |
| `propina` | `Double` | `PROPINA` |  |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `PagoNegociacion` → tabla **`PGS.PGNG`**

Secuencia PK: `PGS.SQ_PGNGCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `formaPago` | `FormaPagoNegociacion` | `FORMAPAGO` | FK → `FormaPagoNegociacion` (ID), ManyToOne |
| `fechaPago` | `LocalDate` | `FECHAPAGO` |  |
| `valorPago` | `Double` | `VALORPAGO` |  |
| `descripcion` | `String` | `DESCRIPCION` | length=1000 |
| `tipoPago` | `String` | `TIPOPAGO` | length=50 |
| `facturaCompra` | `FacturaCompra` | `FACTURACOMPRA` | FK → `FacturaCompra` (ID), ManyToOne |
| `facturado` | `Long` | `FACTURADO` |  |
| `pagado` | `Long` | `PAGADO` |  |
| `refComprobante` | `String` | `REFCOMPROBANTE` | length=200 |
| `estado` | `Long` | `ESTADO` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaRegistro` | `LocalDateTime` | `FECHAREGISTRO` |  |

### `PagosArbitrariosXFinanciacionPago` → tabla **`PGS.PAFP`**

Secuencia PK: `PGS.SQ_PAFPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PAFPCDGO` | **PK**, SEQUENCE |
| `financiacionXDocumentoPago` | `FinanciacionXDocumentoPago` | `FXDPCDGO` | FK → `FinanciacionXDocumentoPago` (FXDPCDGO), ManyToOne |
| `diaPago` | `Long` | `PAFPDAAA` |  |
| `mesPago` | `Long` | `PAFPMSSS` |  |
| `anioPago` | `Long` | `PAFPANOO` |  |
| `fechaPago` | `Date` | `PAFPFCPG` |  |
| `valor` | `Double` | `PAFPVLRR` |  |

### `PathFacturaCompra` → tabla **`PGS.PFCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `factura` | `FacturaCompra` | `FACTURA` | FK → `FacturaCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathLiquidacionCompraCompra` → tabla **`PGS.PLCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `liquidacion` | `LiquidacionCompraCompra` | `LIQUIDACION` | FK → `LiquidacionCompraCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathNegociacion` → tabla **`PGS.PTNG`**

Secuencia PK: `PGS.SQ_PTNGCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `negociacion` | `NegociacionProveedor` | `NEGOCIACION` | FK → `NegociacionProveedor` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `nombreDoc` | `String` | `NOMBREDOC` | length=500 |
| `tipoDoc` | `String` | `TIPODOC` | length=50 |
| `principal` | `Long` | `PRINCIPAL` |  |
| `adendum` | `AdendumNegociacion` | `ADENDUM` | FK → `AdendumNegociacion` (ID), ManyToOne |

### `PathNotaCreditoCompra` → tabla **`PGS.PTCV`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaCredito` | `NotaCreditoCompra` | `NOTACREDITO` | FK → `NotaCreditoCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathNotaDebitoCompra` → tabla **`PGS.PTDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `notaDebito` | `NotaDebitoCompra` | `NOTADEBITO` | FK → `NotaDebitoCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `PathRetencionCompra` → tabla **`PGS.PRCM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `retencion` | `RetencionCompra` | `RETENCION` | FK → `RetencionCompra` (ID), ManyToOne |
| `path` | `String` | `PATH` | length=1000 |
| `alterno` | `Long` | `ALTERNO` |  |

### `ProductoPago` → tabla **`PGS.PRDP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `grupoProducto` | `GrupoProductoPago` | `GRUPOPRODUCTO` | FK → `GrupoProductoPago` (GRPPCDGO), ManyToOne |
| `nombre` | `String` | `NOMBRE` | length=1000 |
| `codigo` | `String` | `CODIGO` | length=500 |
| `codigoAux` | `String` | `CODIGOAUX` | length=500 |
| `precioUnitario` | `Double` | `PRECIOUNITARIO` |  |
| `descuento` | `Double` | `DESCUENTO` |  |
| `tipoDescuento` | `Long` | `TIPODESCUENTO` |  |
| `incluyeIVA` | `Long` | `INCLUYEIVA` |  |
| `tipoIVA` | `Long` | `TIPOIVA` |  |
| `tipoICE` | `Long` | `TIPOICE` |  |
| `ice` | `Double` | `ICE` |  |
| `descripcion` | `String` | `DESCRIPCION` | length=1000 |
| `subsidio` | `Double` | `SUBSIDIO` |  |
| `precioSinSub` | `Double` | `PRECIOSINSUB` |  |
| `irbpnr` | `Double` | `IRBPNR` |  |
| `multiPrecio` | `Long` | `MULTIPRECIO` |  |
| `stock` | `Long` | `STOCK` |  |
| `manejaUnidad` | `Long` | `MANEJAUNIDAD` |  |
| `unidad` | `Long` | `UNIDAD` |  |
| `estado` | `Long` | `ESTADO` |  |

### `ProposicionPagoXCuota` → tabla **`PGS.PRPD`**

Secuencia PK: `PGS.SQ_PRPDCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRPDCDGO` | **PK**, SEQUENCE |
| `cuotaXFinanciacionPago` | `CuotaXFinanciacionPago` | `CXDPCDGO` | FK → `CuotaXFinanciacionPago` (CXDPCDGO), ManyToOne |
| `valorCuota` | `Double` | `PRPDVLCT` |  |
| `valorPropuesto` | `Double` | `PRPDVLRR` |  |
| `fechaIngreso` | `Date` | `PRPDFCIN` |  |
| `tipo` | `Long` | `PRPDTPPR` |  |
| `numeroAbono` | `Long` | `PRPDNMAB` |  |
| `estado` | `Long` | `PRPDESTD` |  |
| `fechaPago` | `Date` | `PRPDFCPG` |  |
| `nombreUsuario` | `String` | `PRPDUSPR` |  |
| `aprobacionesRealizadas` | `Long` | `PRPDNMAP` |  |

### `ResumenValorDocumentoPago` → tabla **`PGS.RVDP`**

Secuencia PK: `PGS.SQ_RVDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RVDPCDGO` | **PK**, SEQUENCE |
| `documentoPago` | `DocumentoPago` | `DCMPCDGO` | FK → `DocumentoPago` (DCMPCDGO), ManyToOne |
| `codigoAlternoTipoValor` | `Long` | `RVDPCATP` |  |
| `valor` | `Double` | `RVDPVLRR` |  |

### `RetencionCompra` → tabla **`PGS.RTCM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `proveedor` | `Titular` | `PROVEEDOR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `periodoFiscal` | `String` | `PERIODOFISCAL` | length=50 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `RetencionCompraV2` → tabla **`PGS.RCV2`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, IDENTITY |
| `tipoComprobante` | `String` | `TIPOCOMPROBANTE` | length=10 |
| `empresa` | `Empresa` | `EMPRESA` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `proveedor` | `Titular` | `PROVEEDOR` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoDoc` | `String` | `TIPODOC` | length=10 |
| `periodoFiscal` | `String` | `PERIODOFISCAL` | length=50 |
| `numero` | `String` | `NUMERO` | length=100 |
| `numEstablecimiento` | `String` | `NUMESTABLECIMIENTO` | length=500 |
| `numPtoEmision` | `String` | `NUMPTOEMISION` | length=500 |
| `secuencial` | `String` | `SECUENCIAL` | length=1000 |
| `ambiente` | `Long` | `AMBIENTE` |  |
| `clave` | `String` | `CLAVE` | length=100 |
| `fecha` | `LocalDateTime` | `FECHA` |  |
| `observacion` | `String` | `OBSERVACION` | length=2000 |
| `total` | `Double` | `TOTAL` |  |
| `ptoEmision` | `Long` | `PTOEMISION` |  |
| `usuario` | `Usuario` | `USUARIO` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `pathGen` | `String` | `PATHGEN` | length=2000 |
| `autorizacion` | `String` | `AUTORIZACION` | length=1000 |
| `fechaAutorizacion` | `LocalDateTime` | `FECHAAUTORIZACION` |  |
| `estado` | `Long` | `ESTADO` |  |
| `estadoEmision` | `Long` | `ESTADOEMISION` |  |
| `asiento` | `Asiento` | `ASIENTO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `TempAprobacionXMonto` → tabla **`PGS.TAPX`**

Secuencia PK: `PGS.SQ_TAPXCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TAPXCDGO` | **PK**, SEQUENCE |
| `tempMontoAprobacion` | `TempMontoAprobacion` | `TMNACDGO` | FK → `TempMontoAprobacion` (TMNACDGO), ManyToOne |
| `nombreNivel` | `String` | `TAPXNMBR` |  |
| `estado` | `Long` | `TAPXESTD` |  |
| `fechaIngreso` | `Date` | `TAPXFCIN` |  |
| `usuarioIngresa` | `String` | `TAPXNUIN` |  |
| `ordenAprobacion` | `Long` | `TAPXORDN` |  |
| `seleccionaBanco` | `Long` | `TAPXSLBN` |  |

### `TempComposicionCuotaInicialPago` → tabla **`PGS.TCIP`**

Secuencia PK: `PGS.SQ_TCIPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCIPCDGO` | **PK**, SEQUENCE |
| `tempResumenValorDocumentoPago` | `TempResumenValorDocumentoPago` | `TRDPCDGO` | FK → `TempResumenValorDocumentoPago` (TRDPCDGO), ManyToOne |
| `valor` | `Double` | `TCIPVLRR` |  |
| `valorResumen` | `Double` | `TCIPVLRV` |  |
| `tempFinanciacionXDocumentoPago` | `TempFinanciacionXDocumentoPago` | `TFDPCDGO` | FK → `TempFinanciacionXDocumentoPago` (TFDPCDGO), ManyToOne |

### `TempCuotaXFinanciacionPago` → tabla **`PGS.TCDP`**

Secuencia PK: `PGS.SQ_TCDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCDPCDGO` | **PK**, SEQUENCE |
| `tempFinanciacionXDocumentoPago` | `TempFinanciacionXDocumentoPago` | `TFDPCDGO` | FK → `TempFinanciacionXDocumentoPago` (TFDPCDGO), ManyToOne |
| `fechaIngreso` | `Date` | `TCDPFCIN` |  |
| `fechaVencimiento` | `Date` | `TCDPFCVN` |  |
| `tipo` | `Long` | `TCDPTPOO` |  |
| `valor` | `Double` | `TCDPVLRR` |  |
| `numeroSecuencial` | `Long` | `TCDPNMSC` |  |
| `numeroCuotaLetra` | `Long` | `TCDPNMCL` |  |
| `numeroTotalCuotas` | `Long` | `TCDPNMTC` |  |
| `totalAbono` | `Double` | `TCDPABNO` |  |
| `saldo` | `Double` | `TCDPSLDO` |  |

### `TempDetalleDocumentoPago` → tabla **`PGS.TDTP`**

Secuencia PK: `PGS.SQ_TDTPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TDTPCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `tempDocumentoPago` | `TempDocumentoPago` | `TDCPCDGO` | FK → `TempDocumentoPago` (TDCPCDGO), ManyToOne |
| `productoPago` | `ProductoPago` | `PRDPCDGO` | FK → `ProductoPago` (PRDPCDGO), ManyToOne |
| `descripcion` | `String` | `TDTPDSCR` |  |
| `cantidad` | `Double` | `TDTPCNTD` |  |
| `precioUnitario` | `Double` | `TDTPPRUN` |  |
| `subtotal` | `Double` | `TDTPSBTT` |  |
| `totalImpuesto` | `Double` | `TDTPTTIM` |  |
| `total` | `Double` | `TDTPTTLL` |  |
| `centroCosto` | `CentroCosto` | `CNCSCDGO` | FK → `CentroCosto` (CNCSCDGO), ManyToOne |
| `numeroLinea` | `Long` | `TDTPNMLN` |  |
| `estado` | `Long` | `TDTPESTD` |  |
| `fechaIngreso` | `Date` | `TDTPFCIN` |  |

### `TempDocumentoPago` → tabla **`PGS.TDCP`**

Secuencia PK: `PGS.SQ_TDCPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TDCPCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `sRITipoDocumento` | `SRITipoDocumento` | `STDCCDGO` | FK → `SRITipoDocumento` (STDCCDGO), ManyToOne |
| `fechaDocumento` | `Date` | `TDCPFCDC` |  |
| `razonSocial` | `String` | `TDCPRZSC` |  |
| `ruc` | `String` | `TDCPRUCC` |  |
| `direccion` | `String` | `TDCPDRCC` |  |
| `diasVencimiento` | `Long` | `TDCPDSVN` |  |
| `fechaVencimiento` | `Date` | `TDCPFCVN` |  |
| `numeroSerie` | `String` | `TDCPSREE` |  |
| `numeroDocumentoString` | `String` | `TDCPNMRO` |  |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `mes` | `Long` | `TDCPMSSS` |  |
| `anio` | `Long` | `TDCPANOO` |  |
| `numeroAutorizacion` | `Long` | `TDCPNMAU` |  |
| `fechaAutorizacion` | `Date` | `TDCPFCAU` |  |
| `numeroResolucion` | `String` | `TDCPNMRS` |  |
| `total` | `Double` | `TDCPTTLL` |  |
| `abono` | `Double` | `TDCPABNN` |  |
| `saldo` | `Double` | `TDCPSLDD` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `idFisico` | `Long` | `TDCPIDFS` |  |
| `tipoFormaPago` | `Long` | `TDCPFRPG` |  |
| `numeroDocumentoNumber` | `Long` | `TDCPNMRN` |  |
| `rubroEstadoP` | `Long` | `TDCPRYYA` |  |
| `rubroEstadoH` | `Long` | `TDCPRZZA` |  |

### `TempFinanciacionXDocumentoPago` → tabla **`PGS.TFDP`**

Secuencia PK: `PGS.SQ_TFDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TFDPCDGO` | **PK**, SEQUENCE |
| `tempDocumentoPago` | `TempDocumentoPago` | `TDCPCDGO` | FK → `TempDocumentoPago` (TDCPCDGO), ManyToOne |
| `tipoFinanciacion` | `Long` | `TFDPTPFN` |  |
| `aplicaInteres` | `Long` | `TFDPAPIN` |  |
| `porcentajeInteres` | `Double` | `TFDPPRIN` |  |
| `factorInteres` | `Double` | `TFDPFTIN` |  |
| `aplicaCuotaInicial` | `Long` | `TFDPAPCI` |  |
| `valorPorcentaCI` | `Double` | `TFDPVPRC` |  |
| `valorFijoCI` | `Double` | `TFDPVNMC` |  |
| `tipoCuotaInicial` | `Long` | `TFDPTPCI` |  |
| `valorInicialCI` | `Double` | `TFDPVCIB` |  |
| `valorTotalCI` | `Double` | `TFDPVTCI` |  |
| `numeroPagos` | `Long` | `TFDPNMPG` |  |
| `tipoPagos` | `Long` | `TFDPTPCA` |  |
| `rubroPeriodicidadP` | `Long` | `TFDPRYYA` |  |
| `rubroPeriodicidadH` | `Long` | `TFDPRZZA` |  |
| `tipoPeriodicidadPago` | `Long` | `TFDPTPDP` |  |
| `dependeOtraFinanciacion` | `Long` | `TFDPDPOF` |  |
| `numeroDocumentoDepende` | `String` | `TFDPNDCD` |  |
| `idDepende` | `Long` | `TFDPIDDC` |  |

### `TempMontoAprobacion` → tabla **`PGS.TMNA`**

Secuencia PK: `PGS.SQ_TMNACDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TMNACDGO` | **PK**, SEQUENCE |
| `valorDesde` | `Double` | `TMNAVLDS` |  |
| `valorHasta` | `Double` | `TMNAVLHS` |  |
| `fechaIngreso` | `Date` | `TMNAFCIN` |  |
| `usuarioIngresa` | `String` | `TMNANUIN` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |

### `TempPagosArbitrariosXFinanciacionPago` → tabla **`PGS.TPFP`**

Secuencia PK: `PGS.SQ_TPFPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPFPCDGO` | **PK**, SEQUENCE |
| `tempFinanciacionXDocumentoPago` | `TempFinanciacionXDocumentoPago` | `TFDPCDGO` | FK → `TempFinanciacionXDocumentoPago` (TFDPCDGO), ManyToOne |
| `diaPago` | `Long` | `TPFPDAAA` |  |
| `mesPago` | `Long` | `TPFPMSSS` |  |
| `anioPago` | `Long` | `TPFPANOO` |  |
| `fechaPago` | `Date` | `TPFPFCPG` |  |
| `valor` | `Double` | `TPFPVLRR` |  |

### `TempResumenValorDocumentoPago` → tabla **`PGS.TRDP`**

Secuencia PK: `PGS.SQ_TRDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TRDPCDGO` | **PK**, SEQUENCE |
| `tempDocumentoPago` | `TempDocumentoPago` | `TDCPCDGO` | FK → `TempDocumentoPago` (TDCPCDGO), ManyToOne |
| `codigoAlternoTipoValor` | `Long` | `TRDPCATP` |  |
| `valor` | `Double` | `TRDPVLRR` |  |

### `TempUsuarioXAprobacion` → tabla **`PGS.TUXA`**

Secuencia PK: `PGS.SQ_TUXACDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TUXACDGO` | **PK**, SEQUENCE |
| `tempAprobacionXMonto` | `TempAprobacionXMonto` | `TAPXCDGO` | FK → `TempAprobacionXMonto` (TAPXCDGO), ManyToOne |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaIngreso` | `Date` | `TUXAFCIN` |  |

### `TempValorImpuestoDetallePago` → tabla **`PGS.TITP`**

Secuencia PK: `PGS.SQ_TITPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TITPCDGO` | **PK**, SEQUENCE |
| `tempDetalleDocumentoPago` | `TempDetalleDocumentoPago` | `TDTPCDGO` | FK → `TempDetalleDocumentoPago` (TDTPCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `TITPNMBR` |  |
| `porcentaje` | `Double` | `TITPPRCT` |  |
| `valorBase` | `Double` | `TITPVLSA` |  |
| `valor` | `Double` | `TITPVLRR` |  |

### `TempValorImpuestoDocumentoPago` → tabla **`PGS.TIDP`**

Secuencia PK: `PGS.SQ_TIDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TIDPCDGO` | **PK**, SEQUENCE |
| `tempDocumentoPago` | `TempDocumentoPago` | `TDCPCDGO` | FK → `TempDocumentoPago` (TDCPCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `TIDPNMBR` |  |
| `porcentaje` | `Double` | `TIDPPRCT` |  |
| `codigoAlternoValor` | `Long` | `TIDPCAVD` |  |
| `valorBase` | `Double` | `TIDPVLSA` |  |
| `valor` | `Double` | `TIDPVLRR` |  |

### `Tsri` → tabla **`PGS.TSRI`**

Secuencia PK: `PGS.SQ_TSRI`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `ID` | **PK**, SEQUENCE |
| `lsri` | `Lsri` | `LSRI` | FK → `Lsri` (TABLA), ManyToOne |
| `codigo` | `String` | `CODIGO` | length=500 |
| `detalle` | `String` | `DETALLE` | length=1000 |
| `porcentaje` | `Double` | `PORCENTAJE` |  |
| `valor` | `Double` | `VALOR` |  |
| `texto` | `String` | `TEXTO` | length=1000 |
| `estado` | `Long` | `ESTADO` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |

### `UsuarioXAprobacion` → tabla **`PGS.UXAP`**

Secuencia PK: `PGS.SQ_UXAPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `UXAPCDGO` | **PK**, SEQUENCE |
| `aprobacionXMonto` | `AprobacionXMonto` | `APXMCDGO` | FK → `AprobacionXMonto` (APXMCDGO), ManyToOne |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaIngreso` | `Date` | `UXAPFCIN` |  |

### `ValorImpuestoDetallePago` → tabla **`PGS.VITP`**

Secuencia PK: `PGS.SQ_VITPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `VITPCDGO` | **PK**, SEQUENCE |
| `detalleDocumentoPago` | `DetalleDocumentoPago` | `DTDPCDGO` | FK → `DetalleDocumentoPago` (DTDPCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `VITPNMBR` |  |
| `porcentaje` | `Double` | `VITPPRCT` |  |
| `valorBase` | `Double` | `VITPVLSA` |  |
| `valor` | `Double` | `VITPVLRR` |  |

### `ValorImpuestoDocumentoPago` → tabla **`PGS.VIDP`**

Secuencia PK: `PGS.SQ_VIDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `VIDPCDGO` | **PK**, SEQUENCE |
| `documentoPago` | `DocumentoPago` | `DCMPCDGO` | FK → `DocumentoPago` (DCMPCDGO), ManyToOne |
| `detalleImpuesto` | `DetalleImpuesto` | `DTIMCDGO` | FK → `DetalleImpuesto` (DTIMCDGO), ManyToOne |
| `nombre` | `String` | `VIDPNMBR` |  |
| `porcentaje` | `Double` | `VIDPPRCT` |  |
| `codigoAlternoValor` | `Long` | `VIDPCAVD` |  |
| `valorBase` | `Double` | `VIDPVLSA` |  |
| `valor` | `Double` | `VIDPVLRR` |  |

---

## REPORTE — Motor de reportes (DTOs) (`com.saa.model.reporte`)

### `ReporteRequest` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `modulo` | `String` | `*(sin @Column)*` |  |
| `nombreReporte` | `String` | `*(sin @Column)*` |  |
| `formato` | `String` | `*(sin @Column)*` |  |
| `parametros` | `Map<String, Object>` | `*(sin @Column)*` |  |

### `ReporteResponse` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `exito` | `boolean` | `*(sin @Column)*` |  |
| `mensaje` | `String` | `*(sin @Column)*` |  |
| `nombreArchivo` | `String` | `*(sin @Column)*` |  |

---

## RHH — Recursos Humanos (`com.saa.model.rhh`)

Constantes de entidades: `NombreEntidadesRhh`

### `AnexoContrato` → tabla **`RHH.NXOO`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `NXOOCDGO` | **PK**, IDENTITY |
| `contratoEmpleado` | `ContratoEmpleado` | `CNTECDGO` | FK → `ContratoEmpleado` (CNTECDGO), ManyToOne |
| `tipo` | `String` | `NXOOTPOO` |  |
| `fechaAnexo` | `LocalDate` | `NXOOFCHA` | NOT NULL |
| `detalle` | `String` | `NXOODTLL` |  |
| `nuevoSalario` | `Double` | `NXOOSLRN` |  |
| `nuevaFechaFin` | `LocalDate` | `NXOOFCHF` |  |
| `fechaRegistro` | `LocalDate` | `NXOOFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `NXOOUSRR` |  |

### `AportesRetenciones` → tabla **`RHH.PRTE`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRTECDGO` | **PK**, IDENTITY |
| `nomina` | `Nomina` | `NMNACDGO` | FK → `Nomina` (NMNACDGO), ManyToOne |
| `entidad` | `String` | `PRTEENTD` |  |
| `concepto` | `String` | `PRTECNCP` |  |
| `baseCalculo` | `Double` | `PRTEBSEE` |  |
| `porcentaje` | `Double` | `PRTEPRCN` |  |
| `valor` | `Double` | `PRTEVLRO` |  |
| `fechaRegistro` | `LocalDate` | `PRTEFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `PRTEUSRR` |  |

### `Cargo` → tabla **`RHH.CRGO`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CRGOCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `CRGONMBR` |  |
| `descripcion` | `String` | `CRGODSCR` |  |
| `requisitos` | `String` | `CRGORQST` |  |
| `estado` | `String` | `CRGOESTD` |  |
| `fechaRegistro` | `LocalDate` | `CRGOFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `CRGOUSRR` |  |

### `Catalogo` → tabla **`RHH.CTLG`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CTLGCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `CTLGNMBR` |  |
| `requiereDocumento` | `String` | `CTLGRQDC` |  |
| `conGoce` | `String` | `CTLGGCEE` |  |
| `estado` | `String` | `CTLGESTD` |  |
| `fechaRegistro` | `LocalDate` | `CTLGFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `CTLGUSRR` |  |

### `ContratoEmpleado` → tabla **`RHH.CNTE`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNTECDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `tipoContratoEmpleado` | `TipoContratoEmpleado` | `TPCECDGO` | FK → `TipoContratoEmpleado` (TPCECDGO), ManyToOne |
| `numero` | `String` | `CNTENMRO` |  |
| `fechaInicio` | `LocalDate` | `CNTEFCHI` | NOT NULL |
| `fechaFin` | `LocalDate` | `CNTEFCHF` |  |
| `salarioBase` | `Double` | `CNTESLRB` |  |
| `estado` | `String` | `CNTEESTD` |  |
| `fechaFirma` | `LocalDate` | `CNTEFRMA` |  |
| `observacion` | `String` | `CNTEOBSR` |  |
| `fechaRegistro` | `LocalDate` | `CNTEFCHR` |  |
| `usuarioRegistro` | `String` | `CNTEUSRR` |  |

### `Departamento` → tabla **`RHH.DPRT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DPRTCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `DPRTNMBR` |  |
| `estado` | `String` | `DPRTESTD` |  |
| `fechaRegistro` | `LocalDate` | `DPRTFCHR` |  |
| `usuarioRegistro` | `String` | `DPRTUSRR` |  |

### `DepartamentoCargo` → tabla **`RHH.DPTC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DPTCCDGO` | **PK**, IDENTITY |
| `departamento` | `Departamento` | `DPRTCDGO` | FK → `Departamento` (DPRTCDGO), ManyToOne |
| `cargo` | `Cargo` | `CRGOCDGO` | FK → `Cargo` (CRGOCDGO), ManyToOne |
| `estado` | `String` | `DPTCESTD` |  |
| `fechaRegistro` | `LocalDate` | `DPTCFCHR` |  |
| `usuarioRegistro` | `String` | `DPTCUSRR` |  |

### `DetalleLiquidacion` → tabla **`RHH.TMLQ`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TMLQCDGO` | **PK**, IDENTITY |
| `liquidacion` | `Liquidacion` | `LQDCCDGO` | FK → `Liquidacion`, ManyToOne |
| `valor` | `Double` | `TMLQVLRO` |  |
| `descripcion` | `String` | `TMLQDSCR` |  |
| `fechaRegistro` | `LocalDate` | `TMLQFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `TMLQUSRR` |  |

### `DetalleTurno` → tabla **`RHH.DTLL`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTLLCDGO` | **PK**, IDENTITY |
| `turno` | `Turno` | `TRNOCDGO` | FK → `Turno` (TRNOCDGO), ManyToOne |
| `diaSemana` | `Integer` | `DTLLDIAA` | NOT NULL |
| `horaEntrada` | `String` | `DTLLENTR` |  |
| `horaSalida` | `String` | `DTLLSLDA` |  |
| `laborable` | `String` | `DTLLLBRB` |  |
| `fechaRegistro` | `LocalDate` | `DTLLFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `DTLLUSRR` |  |

### `Empleado` → tabla **`RHH.MPLD`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MPLDCDGO` | **PK**, IDENTITY |
| `identificacion` | `String` | `MPLDIDNT` |  |
| `apellidos` | `String` | `MPLDAPLL` |  |
| `nombres` | `String` | `MPLDNMBR` |  |
| `fechaNacimiento` | `LocalDate` | `MPLDFCHN` |  |
| `email` | `String` | `MPLDEMAI` |  |
| `telefono` | `String` | `MPLDTLFN` |  |
| `direccion` | `String` | `MPLDDRCC` |  |
| `estado` | `String` | `MPLDESTD` |  |
| `fechaRegistro` | `LocalDate` | `MPLDFCHR` |  |
| `usuarioRegistro` | `String` | `MPLDUSRR` |  |

### `Historial` → tabla **`RHH.HSTR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `HSTRCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `departamento` | `DepartamentoCargo` | `DPRTCDGO` | FK → `DepartamentoCargo` (DPRTCDGO), ManyToOne |
| `cargo` | `Cargo` | `CRGOCDGO` | FK → `Cargo` (CRGOCDGO), ManyToOne |
| `fechaInicio` | `LocalDate` | `HSTRFCHI` | NOT NULL |
| `fechaFin` | `LocalDate` | `HSTRFCHF` |  |
| `actual` | `String` | `HSTRACTL` |  |
| `observacion` | `String` | `HSTROBSR` |  |
| `fechaRegistro` | `LocalDate` | `HSTRFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `HSTRUSRR` |  |

### `Liquidacion` → tabla **`RHH.LQDC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `LQDCCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `contratoEmpleado` | `ContratoEmpleado` | `CNTECDGO` | FK → `ContratoEmpleado` (CNTECDGO), ManyToOne |
| `fechaSalida` | `LocalDate` | `LQDCFCHS` | NOT NULL |
| `motivo` | `String` | `LQDCMTVO` |  |
| `neto` | `Double` | `LQDCNETO` |  |
| `estado` | `String` | `LQDCESTD` |  |
| `fechaRegistro` | `LocalDate` | `LQDCFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `LQDCUSRR` |  |

### `Marcaciones` → tabla **`RHH.MRCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MRCCCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `fechaHora` | `LocalDateTime` | `MRCCFCHH` | NOT NULL |
| `tipo` | `String` | `MRCCTPOO` |  |
| `origen` | `String` | `MRCCORGN` |  |
| `observacion` | `String` | `MRCCOBSR` |  |
| `fechaRegistro` | `LocalDate` | `MRCCFCHR` |  |
| `usuarioRegistro` | `String` | `MRCCUSRR` |  |

### `Nomina` → tabla **`RHH.NMNA`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `NMNACDGO` | **PK**, IDENTITY |
| `periodoNomina` | `PeriodoNomina` | `PRDNCDGO` | FK → `PeriodoNomina` (PRDNCDGO), ManyToOne |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `contratoEmpleado` | `ContratoEmpleado` | `CNTECDGO` | FK → `ContratoEmpleado` (CNTECDGO), ManyToOne |
| `salarioBase` | `Double` | `NMNASLRB` |  |
| `totalIngresos` | `Double` | `NMNATING` |  |
| `totalDescuentos` | `Double` | `NMNATDSC` |  |
| `netoPagar` | `Double` | `NMNANETO` |  |
| `estado` | `String` | `NMNAESTD` |  |
| `fechaRegistro` | `LocalDate` | `NMNAFCHR` |  |
| `usuarioRegistro` | `String` | `NMNAUSRR` |  |

### `PeriodoNomina` → tabla **`RHH.PRDN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRDNCDGO` | **PK**, IDENTITY |
| `anio` | `Integer` | `PRDNANOO` |  |
| `mes` | `Integer` | `PRDNMSEE` |  |
| `fechaInicio` | `LocalDate` | `PRDNFCHI` |  |
| `fechaFin` | `LocalDate` | `PRDNFCHF` |  |
| `estado` | `String` | `PRDNESTD` |  |
| `fechaRegistro` | `LocalDate` | `PRDNFCHR` |  |
| `usuarioRegistro` | `String` | `PRDNUSRR` |  |

### `Peticiones` → tabla **`RHH.PTCN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PTCNCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado` (MPLDCDGO), ManyToOne |
| `catalogo` | `Catalogo` | `CTLGCDGO` | FK → `Catalogo` (CTLGCDGO), ManyToOne |
| `fechaDesde` | `LocalDate` | `PTCNFCHD` |  |
| `fechaHasta` | `LocalDate` | `PTCNFCHH` |  |
| `horas` | `Double` | `PTCNHRAS` |  |
| `motivo` | `String` | `PTCNMTVO` |  |
| `documento` | `String` | `PTCNDOCC` |  |
| `estado` | `String` | `PTCNESTD` |  |
| `usuarioAprobador` | `String` | `PTCNAPRB` |  |
| `observacion` | `String` | `PTCNOBSR` |  |
| `fechaRegistro` | `LocalDate` | `PTCNFCHR` |  |
| `usuarioRegistro` | `String` | `PTCNUSRR` |  |

### `ReglonNomina` → tabla **`RHH.RNGL`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RNGLCDGO` | **PK**, IDENTITY |
| `nomina` | `Nomina` | `NMNACDGO` | FK → `Nomina`, ManyToOne |
| `cantidad` | `BigDecimal` | `RNGLCANT` |  |
| `valor` | `BigDecimal` | `RNGLVLRO` |  |
| `imponible` | `String` | `RNGLIMPN` |  |
| `orden` | `Integer` | `RNGLORDN` |  |
| `fechaRegistro` | `LocalDate` | `RNGLFCHR` |  |
| `usuarioRegistro` | `String` | `RNGLUSRR` |  |

### `ResumenNomina` → tabla **`RHH.RSMN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RSMNCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado`, ManyToOne |
| `fecha` | `LocalDate` | `RSMNFCHA` | NOT NULL |
| `horaEntrada` | `String` | `RSMNENTR` |  |
| `horaSalida` | `String` | `RSMNSLDA` |  |
| `minutosTarde` | `Integer` | `RSMNTRDE` |  |
| `minutosExtra` | `Integer` | `RSMNEXTR` |  |
| `ausencia` | `String` | `RSMNASNT` |  |
| `justificado` | `String` | `RSMNJSTF` |  |
| `fuente` | `String` | `RSMNFNTE` |  |
| `fechaRegistro` | `LocalDate` | `RSMNFCHR` |  |
| `usuarioRegistro` | `String` | `RSMNUSRR` |  |

### `RolPago` → tabla **`RHH.RLPG`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `RLPGCDGO` | **PK**, IDENTITY |
| `nomina` | `Nomina` | `NMNACDGO` | FK → `Nomina`, ManyToOne |
| `numero` | `String` | `RLPGNMRO` |  |
| `fechaEmision` | `LocalDate` | `RLPGFCHA` |  |
| `rutaPdf` | `String` | `RLPGPDFO` |  |
| `estado` | `String` | `RLPGESTD` |  |
| `fechaRegistro` | `LocalDate` | `RLPGFCHR` |  |
| `usuarioRegistro` | `String` | `RLPGUSRR` |  |

### `SaldoVacaciones` → tabla **`RHH.SLDV`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `SLDVCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado`, ManyToOne |
| `anio` | `Integer` | `SLDVANOO` | NOT NULL |
| `diasAsignados` | `Double` | `SLDVASGN` |  |
| `diasUsados` | `Double` | `SLDVUSDO` |  |
| `diasPendientes` | `Double` | `SLDVPNDE` |  |
| `fechaRegistro` | `LocalDate` | `SLDVFCHR` |  |
| `usuarioRegistro` | `String` | `SLDVUSRR` |  |

### `SolicitudVacaciones` → tabla **`RHH.SLCT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `SLCTCDGO` | **PK**, IDENTITY |
| `empleado` | `Empleado` | `MPLDCDGO` | FK → `Empleado`, ManyToOne |
| `fechaDesde` | `LocalDate` | `SLCTFCHD` | NOT NULL |
| `fechaHasta` | `LocalDate` | `SLCTFCHH` | NOT NULL |
| `diasSolicitados` | `Double` | `SLCTDIAS` |  |
| `estado` | `String` | `SLCTESTD` |  |
| `usuarioAprobacion` | `String` | `SLCTAPRB` |  |
| `observacion` | `String` | `SLCTOBSR` |  |
| `fechaAprobacion` | `LocalDate` | `SLCTFHAP` |  |
| `fechaRegistro` | `LocalDate` | `SLCTFCHR` | NOT NULL |
| `usuarioRegistro` | `String` | `SLCTUSRR` | length=60 |

### `TipoContratoEmpleado` → tabla **`RHH.TPCE`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPCECDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TPCENMBR` |  |
| `requiereFechaFin` | `String` | `TPCERQRE` |  |
| `estado` | `String` | `TPCEESTD` |  |
| `fechaRegistro` | `LocalDate` | `TPCEFCHR` |  |
| `usuarioRegistro` | `String` | `TPCEUSRR` |  |

### `Turno` → tabla **`RHH.TRNO`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TRNOCDGO` | **PK**, IDENTITY |
| `nombre` | `String` | `TRNONMBR` |  |
| `horaEntrada` | `String` | `TRNOENTR` |  |
| `horaSalida` | `String` | `TRNOSLDA` |  |
| `minutosTolerancia` | `Integer` | `TRNOMNTS` |  |
| `estado` | `String` | `TRNOESTD` |  |
| `fechaRegistro` | `LocalDate` | `TRNOFCHR` |  |
| `usuarioRegistro` | `String` | `TRNOUSRR` |  |

---

## RPR — Reportes regulatorios y de cartera (`com.saa.model.rpr`)

Constantes de entidades: `NombreEntidadesReporte`

### `CancelacionG49` → tabla **`RPR.CG49`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG49CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG49TIDS` | length=50 |
| `identificacion` | `String` | `CG49IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG49NMOP` | length=50 |
| `fechaCancelacion` | `LocalDate` | `CG49FCCN` |  |
| `formaCancelacion` | `String` | `CG49FMCN` | length=100 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG49EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `CreditoCuotasPrestamosMensual` → tabla **`RPR.CCPM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CCPMCDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CCPMTIDS` | length=50 |
| `identificacion` | `String` | `CCPMIDSJ` | length=50 |
| `numeroOperacion` | `String` | `CCPMNMOP` | length=50 |
| `tipoCredito` | `String` | `CCPMTPCR` | length=50 |
| `diasMorosidad` | `Long` | `CCPMDDMR` |  |
| `calificacionPropia` | `String` | `CCPMCLPR` | length=50 |
| `tasaInteres` | `Double` | `CCPMTDIN` |  |
| `valorPorVencer` | `Double` | `CCPMVPVN` |  |
| `valorVencido` | `Double` | `CCPMVLVN` |  |
| `costosOperativos` | `Double` | `CCPMCSPR` |  |
| `interesOrdinario` | `Double` | `CCPMINRD` |  |
| `interesMora` | `Double` | `CCPMISMR` |  |
| `interesMoraDelMes` | `Double` | `CCPMISMD` |  |
| `interesOrdinarioDelMes` | `Double` | `CCPMINRM` |  |
| `valorDemandaJudicial` | `Double` | `CCPMVEDJ` |  |
| `carteraCastigada` | `Double` | `CCPMCRCS` |  |
| `provisionRequeridaOriginal` | `Double` | `CCPMPRRO` |  |
| `provisionConstituida` | `Double` | `CCPMPRCN` |  |
| `valorTotalCuentaIndividual` | `Double` | `CCPMVTCI` |  |
| `valorSujetoProvision` | `Double` | `CCPMVSAP` |  |
| `tipoSistemaAmortizacion` | `String` | `CCPMTDSA` | length=50 |
| `cuotaCredito` | `Double` | `CCPMCDCR` |  |
| `dividendo` | `Double` | `CCPMDVDN` |  |
| `fechaExigibilidad` | `LocalDate` | `CCPMFDEC` |  |
| `valorDesgravamen` | `Double` | `CCPMVLDG` |  |
| `valorIncendio` | `Double` | `CCPMVLIN` |  |
| `capitalPorVencer1a30` | `Double` | `CCPMCV30` |  |
| `capitalPorVencer31a90` | `Double` | `CCPMCV90` |  |
| `capitalPorVencer91a180` | `Double` | `CCPMCV180` |  |
| `capitalPorVencer181a360` | `Double` | `CCPMCV360` |  |
| `capitalPorVencerMas360` | `Double` | `CCPMCVMAS` |  |
| `estadoDesglose` | `Long` | `CCPMCVES` |  |
| `fechaPrestamo` | `LocalDate` | `CCPMFCPR` |  |
| `ejecucionReporte` | `EjecucionReporteCartera` | `CCPMEJCC` | FK → `EjecucionReporteCartera` (EJCCCDGO), ManyToOne |

### `CreditoG40` → tabla **`RPR.CG40`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG40CDGO` | **PK**, IDENTITY |
| `tipoIdentificacionFcpc` | `String` | `CG40TIDF` | length=50 |
| `identificacionFcpc` | `String` | `CG40IDFC` | length=50 |
| `numeroResolucion` | `String` | `CG40NMRS` | length=100 |
| `fechaResolucion` | `LocalDate` | `CG40FCRS` |  |
| `provincia` | `String` | `CG40PRVN` | length=100 |
| `canton` | `String` | `CG40CNTN` | length=100 |
| `direccion` | `String` | `CG40DRCC` | length=500 |
| `telefonos` | `String` | `CG40TLFN` | length=100 |
| `correoElectronico` | `String` | `CG40CEFD` | length=200 |
| `tipoSistema` | `String` | `CG40TPSS` | length=50 |
| `tipoPrestacion` | `String` | `CG40TPPR` | length=50 |
| `tipoAporte` | `String` | `CG40TPAP` | length=50 |
| `tipoAdministracion` | `String` | `CG40TPAD` | length=50 |
| `fechaTraspaso` | `LocalDate` | `CG40FCTR` |  |
| `tipoFcpc` | `String` | `CG40TPFC` | length=50 |
| `numeroResolucionCambioEstatuto` | `String` | `CG40NRCE` | length=100 |
| `fechaResolucionCambioEstatuto` | `LocalDate` | `CG40FRCE` |  |
| `cambioNombre` | `String` | `CG40CMNM` | length=200 |
| `porcentajeAportePatronalCesantia` | `Double` | `CG40PAPC` |  |
| `porcentajeAportePersonalCesantia` | `Double` | `CG40PARC` |  |
| `porcentajeAportePatronalJubilacion` | `Double` | `CG40PAPJ` |  |
| `porcentajeAportePersonalJubilacion` | `Double` | `CG40PARJ` |  |
| `valorAportePersonalCesantia` | `Double` | `CG40VARC` |  |
| `valorAportePersonalJubilacion` | `Double` | `CG40VARJ` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG40EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `CreditoJubiladosMensual` → tabla **`RPR.CJBM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CJBMCDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CJBMTIDJ` | length=50 |
| `identificacion` | `String` | `CJBMIDJB` | length=50 |
| `tipoJubilacion` | `String` | `CJBMTPJB` | length=50 |
| `fechaJubilacion` | `LocalDate` | `CJBMFCJB` |  |
| `imposicionesAcumuladas` | `Long` | `CJBMIAJB` |  |
| `valorPension` | `Double` | `CJBMVLPN` |  |
| `valorNetoRecibir` | `Double` | `CJBMVNAR` |  |
| `saldoCuenta` | `Double` | `CJBMSCJB` |  |
| `valoresCompensados` | `Double` | `CJBMVCAP` |  |
| `jubilacionIess` | `String` | `CJBMJEIS` | length=50 |
| `valorJubilacion` | `Double` | `CJBMVLJB` |  |
| `valorSeguro` | `Double` | `CJBMVLSG` |  |
| `ejecucionReporte` | `EjecucionReporteCartera` | `CJBMEJCC` | FK → `EjecucionReporteCartera` (EJCCCDGO), ManyToOne |

### `CreditoParticipesMensual` → tabla **`RPR.CPRM`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CPRMCDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CPRMTIDP` | length=50 |
| `identificacion` | `String` | `CPRMIDPR` | length=50 |
| `tipoAporte` | `TipoAporte` | `TPAPCDGO` | FK → `TipoAporte` (TPAPCDGO), ManyToOne |
| `total` | `Double` | `CPRMTTAL` |  |
| `ejecucionReporte` | `EjecucionReporteCartera` | `CPRMEJCC` | FK → `EjecucionReporteCartera` (EJCCCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |
| `nombreEstado` | `String` | `CPRMSTEN` | length=50 |

### `DetalleEjecucionReporte` → tabla **`RPR.EJRD`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `EJRDCDGO` | **PK**, IDENTITY |
| `ejecucionReporte` | `EjecucionReporte` | `EJRCCDGO` | FK → `EjecucionReporte` (EJRCCDGO), ManyToOne |
| `tipoReporte` | `String` | `EJRDTPRP` | length=10 |
| `estado` | `Long` | `EJRDESTD` |  |
| `fechaGeneracion` | `LocalDate` | `EJRDFCGN` |  |
| `cantidadRegistros` | `Long` | `EJRDCNRG` |  |
| `novedades` | `String` | `EJRDNVDD` | length=1000 |
| `detalleOriginal` | `DetalleEjecucionReporte` | `EJRDEJRO` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `EjecucionReporte` → tabla **`RPR.EJRC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `EJRCCDGO` | **PK**, IDENTITY |
| `mes` | `Long` | `EJRCMESS` |  |
| `anio` | `Long` | `EJRCANOO` |  |
| `usuario` | `String` | `EJRCUSRO` | length=50 |
| `fechaGeneracion` | `LocalDate` | `EJRCFCGN` |  |
| `tipoEjecucion` | `Long` | `EJRCTPEJ` |  |
| `estado` | `Long` | `EJRCESTD` |  |
| `observaciones` | `String` | `EJRCOBSR` | length=500 |

### `EjecucionReporteCartera` → tabla **`RPR.EJCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `EJCCCDGO` | **PK**, IDENTITY |
| `mes` | `Long` | `EJCCMESS` |  |
| `anio` | `Long` | `EJCCANOO` |  |
| `usuario` | `String` | `EJCCUSRO` | length=50 |
| `fechaGeneracion` | `LocalDate` | `EJCCFCGN` |  |
| `observaciones` | `String` | `EJCCOBSR` | length=500 |

### `GaranteG50` → tabla **`RPR.CG50`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG50CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG50TIDS` | length=50 |
| `identificacion` | `String` | `CG50IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG50NMOP` | length=50 |
| `tipoIdentificacionGarante` | `String` | `CG50TIDG` | length=50 |
| `identificacionGarante` | `String` | `CG50IDGR` | length=50 |
| `tipoGarante` | `String` | `CG50TPGR` | length=50 |
| `fechaEliminacion` | `LocalDate` | `CG50FDEG` |  |
| `causaEliminacion` | `String` | `CG50CDEG` | length=200 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG50EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `GarantiaRealG51` → tabla **`RPR.CG51`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG51CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG51TIDS` | length=50 |
| `identificacion` | `String` | `CG51IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG51NMOP` | length=50 |
| `numeroGarantia` | `String` | `CG51NMGR` | length=50 |
| `tipoGarantia` | `String` | `CG51TPGR` | length=50 |
| `descripcionGarantia` | `String` | `CG51DDLG` | length=500 |
| `valorAvaluo` | `Double` | `CG51VDAT` |  |
| `fechaAvaluo` | `LocalDate` | `CG51FDAV` |  |
| `numeroRegistroGarantia` | `String` | `CG51NDRG` | length=100 |
| `fechaContabilizacion` | `LocalDate` | `CG51FDLC` |  |
| `porcentajeCubre` | `Double` | `CG51PQCG` |  |
| `estadoRegistro` | `String` | `CG51EDLR` | length=50 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG51EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `HistoricoCCPM` → tabla **`RPR.HMCP`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HMCPNMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HMCPTIDS` | length=50 |
| `identificacion` | `String` | `HMCPIDSJ` | length=50 |
| `tipoCredito` | `String` | `HMCPTPCR` | length=50 |
| `diasMorosidad` | `Long` | `HMCPDDMR` |  |
| `calificacionPropia` | `String` | `HMCPCLPR` | length=50 |
| `tasaInteres` | `Double` | `HMCPTDIN` |  |
| `valorPorVencer` | `Double` | `HMCPVPVN` |  |
| `valorVencido` | `Double` | `HMCPVLVN` |  |
| `costosOperativos` | `Double` | `HMCPCSPR` |  |
| `interesOrdinario` | `Double` | `HMCPINRD` |  |
| `interesSobreMora` | `Double` | `HMCPISMR` |  |
| `valorDemandaJudicial` | `Double` | `HMCPVEDJ` |  |
| `carteraCastigada` | `Double` | `HMCPCRCS` |  |
| `provisionRequeridaOriginal` | `Double` | `HMCPPRRO` |  |
| `provisionConstituida` | `Double` | `HMCPPRCN` |  |
| `valorTotalCuentaIndividual` | `Double` | `HMCPVTCI` |  |
| `valorSujetoProvision` | `Double` | `HMCPVSAP` |  |
| `tipoSistemaAmortizacion` | `String` | `HMCPTDSA` | length=50 |
| `cuotaCredito` | `Double` | `HMCPCDCR` |  |
| `dividendo` | `Double` | `HMCPDVDN` |  |
| `fechaExigibilidadCuota` | `String` | `HMCPFDEC` | length=100 |
| `valorDesgravamen` | `Double` | `HMCPVLDG` |  |
| `valorIncendio` | `Double` | `HMCPVLIN` |  |

### `HistoricoCJBM` → tabla **`RPR.HMJB`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HMJBIDJB` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HMJBTIDJ` | length=50 |
| `tipoJubilacion` | `String` | `HMJBTPJB` | length=50 |
| `fechaJubilacion` | `String` | `HMJBFCJB` | length=100 |
| `imposicionesAcumuladas` | `Long` | `HMJBIAJB` |  |
| `valorPension` | `Double` | `HMJBVLPN` |  |
| `valorNetoRecibir` | `Double` | `HMJBVNAR` |  |
| `saldoCuenta` | `Double` | `HMJBSCJB` |  |
| `valoresCompensados` | `Double` | `HMJBVCAP` |  |
| `jubilacionIess` | `String` | `HMJBJEIS` | length=50 |

### `HistoricoCPRM` → tabla **`RPR.HMPR`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `HMPRCDGO` | **PK**, IDENTITY |
| `identificacion` | `String` | `HMPRIDPR` | length=50 |
| `tipoIdentificacion` | `String` | `HMPRTIDP` | length=50 |
| `tipoAporte` | `TipoAporte` | `TPAPCDGO` | FK → `TipoAporte` (TPAPCDGO), ManyToOne |
| `total` | `Double` | `HMPRTTL` |  |

### `HistoricoG40` → tabla **`RPR.HM40`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM40IDFC` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM40TIDF` | length=50 |
| `numeroResolucion` | `String` | `HM40NMRS` | length=100 |
| `fechaResolucion` | `String` | `HM40FCRS` | length=100 |
| `provincia` | `String` | `HM40PRVN` | length=100 |
| `canton` | `String` | `HM40CNTN` | length=100 |
| `direccion` | `String` | `HM40DRCC` | length=500 |
| `telefonos` | `String` | `HM40TLFN` | length=100 |
| `correoElectronico` | `String` | `HM40CEFD` | length=200 |
| `tipoSistema` | `String` | `HM40TPSS` | length=50 |
| `tipoPrestacion` | `String` | `HM40TPPR` | length=50 |
| `tipoAporte` | `String` | `HM40TPAP` | length=50 |
| `tipoAdministracion` | `String` | `HM40TPAD` | length=50 |
| `fechaTraspaso` | `String` | `HM40FCTR` | length=100 |
| `tipoFcpc` | `String` | `HM40TPFC` | length=50 |
| `numeroResolucionCambioEstatuto` | `String` | `HM40NRCE` | length=100 |
| `fechaResolucionCambioEstatuto` | `String` | `HM40FRCE` | length=100 |
| `cambioNombre` | `String` | `HM40CMNM` | length=200 |
| `porcentajeAportePatronalCesantia` | `Double` | `HM40PAPC` |  |
| `porcentajeAportePersonalCesantia` | `Double` | `HM40PARC` |  |
| `porcentajeAportePatronalJubilacion` | `Double` | `HM40PAPJ` |  |
| `porcentajeAportePersonalJubilacion` | `Double` | `HM40PARJ` |  |
| `valorAportePersonalCesantia` | `Double` | `HM40VARC` |  |
| `valorAportePersonalJubilacion` | `Double` | `HM40VARJ` |  |

### `HistoricoG41` → tabla **`RPR.HM41`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM41IDPR` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM41TIDP` | length=50 |
| `genero` | `String` | `HM41GNPR` | length=50 |
| `estadoCivil` | `String` | `HM41ECDP` | length=50 |
| `fechaNacimiento` | `String` | `HM41FNDP` | length=100 |
| `fechaIngreso` | `String` | `HM41FIDP` | length=100 |
| `estadoParticipe` | `String` | `HM41ESPR` | length=50 |
| `tipoSistema` | `String` | `HM41TPSS` | length=50 |
| `baseCalculoAportacion` | `Double` | `HM41BCPA` |  |
| `tipoRelacionLaboral` | `String` | `HM41TRLP` | length=100 |
| `estadoRegistro` | `String` | `HM41ESRG` | length=50 |
| `fechaActualizacionEstado` | `String` | `HM41FADE` | length=100 |

### `HistoricoG42` → tabla **`RPR.HM42`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM42IDPR` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM42TIDP` | length=50 |
| `tipoPrestacion` | `String` | `HM42TPPR` | length=50 |
| `aportePatronal` | `Double` | `HM42APPT` |  |
| `aportePersonal` | `Double` | `HM42APPR` |  |
| `aporteVoluntario` | `Double` | `HM42APVL` |  |
| `saldoAportePatronal` | `Double` | `HM42SAPP` |  |
| `saldoAportePersonal` | `Double` | `HM42SAPE` |  |
| `saldoAporteVoluntario` | `Double` | `HM42SAVL` |  |
| `rendimiento` | `Double` | `HM42RNDM` |  |

### `HistoricoG43` → tabla **`RPR.HM43`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM43IDPR` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM43TIDP` | length=50 |
| `fechaTerminoRelacionLaboral` | `String` | `HM43FTRL` | length=100 |
| `numeroImposicionesPersonales` | `Long` | `HM43NIAP` |  |
| `numeroImposicionesPatronales` | `Long` | `HM43NIAT` |  |
| `fechaLiquidacion` | `String` | `HM43FCLQ` | length=100 |
| `saldoCuentaIndividual` | `Double` | `HM43SDCI` |  |
| `valoresCompensados` | `Double` | `HM43VCAP` |  |
| `valoresPagados` | `Double` | `HM43VPAP` |  |

### `HistoricoG44` → tabla **`RPR.HM44`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM44IDJB` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM44TIDJ` | length=50 |
| `tipoJubilacion` | `String` | `HM44TPJB` | length=50 |
| `fechaJubilacion` | `String` | `HM44FCJB` | length=100 |
| `imposicionesAcumuladas` | `Long` | `HM44IAJB` |  |
| `valorPension` | `Double` | `HM44VLPN` |  |
| `valorNetoRecibir` | `Double` | `HM44VNAR` |  |
| `saldoCuenta` | `Double` | `HM44SCJB` |  |
| `valoresCompensados` | `Double` | `HM44VCAP` |  |
| `jubilacionIess` | `String` | `HM44JEIS` | length=50 |

### `HistoricoG45` → tabla **`RPR.HM45`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `identificacion` | `String` | `HM45IDSJ` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM45TIDS` | length=50 |
| `tipoParticipe` | `String` | `HM45TPPR` | length=50 |
| `actividadEconomica` | `String` | `HM45AEDS` | length=100 |
| `patrimonio` | `Double` | `HM45PTSJ` |  |
| `provincia` | `String` | `HM45PRVN` | length=100 |
| `canton` | `String` | `HM45CNTN` | length=100 |
| `parroquia` | `String` | `HM45PRRQ` | length=100 |
| `genero` | `String` | `HM45GNRO` | length=50 |
| `estadoCivil` | `String` | `HM45ESCV` | length=50 |
| `fechaNacimiento` | `String` | `HM45FDNC` | length=100 |
| `profesion` | `String` | `HM45PRFS` | length=100 |
| `cargasFamiliares` | `Long` | `HM45CRFM` |  |
| `origenIngresos` | `String` | `HM45ODIN` | length=100 |

### `HistoricoG46` → tabla **`RPR.HM46`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HM46NMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM46TIDS` | length=50 |
| `identificacion` | `String` | `HM46IDSJ` | length=50 |
| `tipoCredito` | `String` | `HM46TDCP` | length=50 |
| `estadoOperacion` | `String` | `HM46EDLO` | length=50 |
| `situacionOperacion` | `String` | `HM46SDLO` | length=50 |
| `destinoProvincia` | `String` | `HM46DGPR` | length=100 |
| `destinoCanton` | `String` | `HM46DGCN` | length=100 |
| `destinoParroquia` | `String` | `HM46DGPS` | length=100 |
| `fechaConcesion` | `String` | `HM46FCCN` | length=100 |
| `fechaVencimiento` | `String` | `HM46FCVN` | length=100 |
| `valorOperacion` | `Double` | `HM46VDLO` |  |
| `tasaInteresNominal` | `Double` | `HM46TDIN` |  |
| `periodicidadPago` | `String` | `HM46PDDP` | length=50 |
| `frecuenciaRevision` | `String` | `HM46FDRV` | length=50 |
| `garantesGarantias` | `String` | `HM46GOGR` | length=200 |

### `HistoricoG47` → tabla **`RPR.HM47`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HM47NMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM47TIDS` | length=50 |
| `identificacion` | `String` | `HM47IDSJ` | length=50 |
| `numeroOperacionAnterior` | `String` | `HM47NDOA` | length=50 |
| `fechaNovacion` | `String` | `HM47FDNR` | length=100 |

### `HistoricoG48` → tabla **`RPR.HM48`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HM48NMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM48TIDS` | length=50 |
| `identificacion` | `String` | `HM48IDSJ` | length=50 |
| `tipoCredito` | `String` | `HM48TPCR` | length=50 |
| `diasMorosidad` | `Long` | `HM48DDMR` |  |
| `calificacionPropia` | `String` | `HM48CLPR` | length=50 |
| `tasaInteres` | `Double` | `HM48TDIN` |  |
| `valorPorVencer` | `Double` | `HM48VPVN` |  |
| `valorVencido` | `Double` | `HM48VLVN` |  |
| `costosOperativos` | `Double` | `HM48CSPR` |  |
| `interesOrdinario` | `Double` | `HM48INRD` |  |
| `interesSobreMora` | `Double` | `HM48ISMR` |  |
| `valorDemandaJudicial` | `Double` | `HM48VEDJ` |  |
| `carteraCastigada` | `Double` | `HM48CRCS` |  |
| `provisionRequeridaOriginal` | `Double` | `HM48PRRO` |  |
| `provisionConstituida` | `Double` | `HM48PRCN` |  |
| `valorTotalCuentaIndividual` | `Double` | `HM48VTCI` |  |
| `valorSujetoProvision` | `Double` | `HM48VSAP` |  |
| `tipoSistemaAmortizacion` | `String` | `HM48TDSA` | length=50 |
| `cuotaCredito` | `Double` | `HM48CDCR` |  |
| `dividendo` | `Double` | `HM48DVDN` |  |
| `fechaExigibilidadCuota` | `String` | `HM48FDEC` | length=100 |

### `HistoricoG49` → tabla **`RPR.HM49`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HM49NMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM49TIDS` | length=50 |
| `identificacion` | `String` | `HM49IDSJ` | length=50 |
| `fechaCancelacion` | `String` | `HM49FCCN` | length=100 |
| `formaCancelacion` | `String` | `HM49FMCN` | length=100 |

### `HistoricoG50` → tabla **`RPR.HM50`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroOperacion` | `String` | `HM50NMOP` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM50TIDS` | length=50 |
| `identificacion` | `String` | `HM50IDSJ` | length=50 |
| `tipoIdentificacionGarante` | `String` | `HM50TIDG` | length=50 |
| `identificacionGarante` | `String` | `HM50IDGR` | length=50 |
| `tipoGarante` | `String` | `HM50TPGR` | length=50 |
| `fechaEliminacionGarante` | `String` | `HM50FDEG` | length=100 |
| `causaEliminacionGarante` | `String` | `HM50CDEG` | length=200 |

### `HistoricoG51` → tabla **`RPR.HM51`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `numeroGarantia` | `String` | `HM51NMGR` | **PK**, length=50 |
| `tipoIdentificacion` | `String` | `HM51TIDS` | length=50 |
| `identificacion` | `String` | `HM51IDSJ` | length=50 |
| `numeroOperacion` | `String` | `HM51NMOP` | length=50 |
| `tipoGarantia` | `String` | `HM51TPGR` | length=50 |
| `descripcionGarantia` | `String` | `HM51DDLG` | length=500 |
| `valorAvaluo` | `Double` | `HM51VDAT` |  |
| `fechaAvaluo` | `String` | `HM51FDAV` | length=100 |
| `numeroRegistroGarantia` | `String` | `HM51NDRG` | length=100 |
| `fechaContabilizacion` | `String` | `HM51FDLC` | length=100 |
| `porcentajeCubreGarantia` | `Double` | `HM51PQCG` |  |
| `estadoRegistro` | `String` | `HM51EDLR` | length=50 |

### `NovacionG47` → tabla **`RPR.CG47`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG47CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG47TIDS` | length=50 |
| `identificacion` | `String` | `CG47IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG47NMOP` | length=50 |
| `numeroOperacionAnterior` | `String` | `CG47NDOA` | length=50 |
| `fechaNovacion` | `LocalDate` | `CG47FDNR` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG47EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `NuevoParticipeG45` → tabla **`RPR.CG45`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG45CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG45TIDS` | length=50 |
| `identificacion` | `String` | `CG45IDSJ` | length=50 |
| `tipoParticipe` | `String` | `CG45TPPR` | length=50 |
| `actividadEconomica` | `String` | `CG45AEDS` | length=100 |
| `patrimonio` | `Double` | `CG45PTSJ` |  |
| `provincia` | `String` | `CG45PRVN` | length=100 |
| `canton` | `String` | `CG45CNTN` | length=100 |
| `parroquia` | `String` | `CG45PRRQ` | length=100 |
| `genero` | `String` | `CG45GNRO` | length=50 |
| `estadoCivil` | `String` | `CG45ESCV` | length=50 |
| `fechaNacimiento` | `LocalDate` | `CG45FDNC` |  |
| `profesion` | `String` | `CG45PRFS` | length=100 |
| `cargasFamiliares` | `Long` | `CG45CRFM` |  |
| `origenIngresos` | `String` | `CG45ODIN` | length=100 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG45EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `NuevoPrestamoG46` → tabla **`RPR.CG46`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG46CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG46TIDS` | length=50 |
| `identificacion` | `String` | `CG46IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG46NMOP` | length=50 |
| `tipoCredito` | `String` | `CG46TDCP` | length=50 |
| `estadoOperacion` | `String` | `CG46EDLO` | length=50 |
| `situacionOperacion` | `String` | `CG46SDLO` | length=50 |
| `destinoProvincia` | `String` | `CG46DGPR` | length=100 |
| `destinoCanton` | `String` | `CG46DGCN` | length=100 |
| `destinoParroquia` | `String` | `CG46DGPS` | length=100 |
| `fechaConcesion` | `LocalDate` | `CG46FCCN` |  |
| `fechaVencimiento` | `LocalDate` | `CG46FCVN` |  |
| `valorOperacion` | `Double` | `CG46VDLO` |  |
| `tasaInteresNominal` | `Double` | `CG46TDIN` |  |
| `periodicidadPago` | `String` | `CG46PDDP` | length=50 |
| `frecuenciaRevision` | `String` | `CG46FDRV` | length=50 |
| `garantias` | `String` | `CG46GOGR` | length=200 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG46EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `ParticipeActivoG41` → tabla **`RPR.CG41`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG41CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG41TIDP` | length=50 |
| `identificacion` | `String` | `CG41IDPR` | length=50 |
| `genero` | `String` | `CG41GNPR` | length=50 |
| `estadoCivil` | `String` | `CG41ECDP` | length=50 |
| `fechaNacimiento` | `LocalDate` | `CG41FNDP` |  |
| `fechaIngreso` | `LocalDate` | `CG41FIDP` |  |
| `estadoParticipe` | `String` | `CG41ESPR` | length=50 |
| `tipoSistema` | `String` | `CG41TPSS` | length=50 |
| `baseCalculoAportacion` | `String` | `CG41BCPA` | length=10 |
| `tipoRelacionLaboral` | `String` | `CG41TRLP` | length=100 |
| `estadoRegistro` | `String` | `CG41ESRG` | length=50 |
| `fechaActualizacionEstado` | `LocalDate` | `CG41FADE` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG41EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `ParticipeCesanteG43` → tabla **`RPR.CG43`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG43CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG43TIDP` | length=50 |
| `identificacion` | `String` | `CG43IDPR` | length=50 |
| `fechaTerminoRelacionLaboral` | `LocalDate` | `CG43FTRL` |  |
| `numeroImposicionesPersonales` | `Long` | `CG43NIAP` |  |
| `numeroImposicionesPatronales` | `Long` | `CG43NIAT` |  |
| `fechaLiquidacion` | `LocalDate` | `CG43FCLQ` |  |
| `saldoCuentaIndividual` | `Double` | `CG43SDCI` |  |
| `valoresCompensados` | `Double` | `CG43VCAP` |  |
| `valoresPagados` | `Double` | `CG43VPAP` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG43EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `ParticipeJubiladoG44` → tabla **`RPR.CG44`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG44CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG44TIDJ` | length=50 |
| `identificacion` | `String` | `CG44IDJB` | length=50 |
| `tipoJubilacion` | `String` | `CG44TPJB` | length=50 |
| `fechaJubilacion` | `LocalDate` | `CG44FCJB` |  |
| `imposicionesAcumuladas` | `Long` | `CG44IAJB` |  |
| `valorPension` | `Double` | `CG44VLPN` |  |
| `valorNetoRecibir` | `Double` | `CG44VNAR` |  |
| `saldoCuenta` | `Double` | `CG44SCJB` |  |
| `valoresCompensados` | `Double` | `CG44VCAP` |  |
| `jubilacionIess` | `String` | `CG44JEIS` | length=50 |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG44EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

### `SaldoCuentaG42` → tabla **`RPR.CG42`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG42CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG42TIDP` | length=50 |
| `identificacion` | `String` | `CG42IDPR` | length=50 |
| `tipoPrestacion` | `String` | `CG42TPPR` | length=50 |
| `aportePatronal` | `Double` | `CG42APPT` |  |
| `aportePersonal` | `Double` | `CG42APPR` |  |
| `aporteVoluntario` | `Double` | `CG42APVL` |  |
| `saldoAportePatronal` | `Double` | `CG42SAPP` |  |
| `saldoAportePersonal` | `Double` | `CG42SAPE` |  |
| `saldoAporteVoluntario` | `Double` | `CG42SAVL` |  |
| `rendimiento` | `Double` | `CG42RNDM` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG42EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |
| `entidad` | `Entidad` | `ENTDCDGO` | FK → `Entidad` (ENTDCDGO), ManyToOne |

### `SaldoOperacionG48` → tabla **`RPR.CG48`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CG48CDGO` | **PK**, IDENTITY |
| `tipoIdentificacion` | `String` | `CG48TIDS` | length=50 |
| `identificacion` | `String` | `CG48IDSJ` | length=50 |
| `numeroOperacion` | `String` | `CG48NMOP` | length=50 |
| `tipoCredito` | `String` | `CG48TPCR` | length=50 |
| `diasMorosidad` | `Long` | `CG48DDMR` |  |
| `calificacionPropia` | `String` | `CG48CLPR` | length=50 |
| `tasaInteres` | `Double` | `CG48TDIN` |  |
| `valorPorVencer` | `Double` | `CG48VPVN` |  |
| `valorVencido` | `Double` | `CG48VLVN` |  |
| `costosOperativos` | `Double` | `CG48CSPR` |  |
| `interesOrdinario` | `Double` | `CG48INRD` |  |
| `interesMora` | `Double` | `CG48ISMR` |  |
| `valorDemandaJudicial` | `Double` | `CG48VEDJ` |  |
| `carteraCastigada` | `Double` | `CG48CRCS` |  |
| `provisionRequeridaOriginal` | `Double` | `CG48PRRO` |  |
| `provisionConstituida` | `Double` | `CG48PRCN` |  |
| `valorTotalCuentaIndividual` | `Double` | `CG48VTCI` |  |
| `valorSujetoProvision` | `Double` | `CG48VSAP` |  |
| `tipoSistemaAmortizacion` | `String` | `CG48TDSA` | length=50 |
| `cuotaCredito` | `Double` | `CG48CDCR` |  |
| `dividendo` | `Double` | `CG48DVDN` |  |
| `fechaExigibilidad` | `LocalDate` | `CG48FDEC` |  |
| `detalleEjecucion` | `DetalleEjecucionReporte` | `CG48EJRD` | FK → `DetalleEjecucionReporte` (EJRDCDGO), ManyToOne |

---

## SCP — Sistema / Core (`com.saa.model.scp`)

Constantes de entidades: `NombreEntidadesSistema`

### `DetalleRubro` → tabla **`SCP.PDTR`**

Secuencia PK: `SCP.SQ_PDTRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PDTRCDGO` | **PK**, SEQUENCE |
| `rubro` | `Rubro` | `PRBRCDGO` | FK → `Rubro` (PRBRCDGO), ManyToOne |
| `descripcion` | `String` | `PDTRDSCR` |  |
| `valorNumerico` | `Double` | `PDTRVLRN` |  |
| `valorAlfanumerico` | `String` | `PDTRVLRV` |  |
| `codigoAlterno` | `Long` | `PDTRALTR` |  |
| `estado` | `Long` | `PDTRESTD` |  |

### `Empresa` → tabla **`SCP.PJRQ`**

Secuencia PK: `SCP.SQ_PJRQCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PJRQCDGO` | **PK**, SEQUENCE |
| `jerarquia` | `Jerarquia` | `PGSPCDGO` | FK → `Jerarquia` (PGSPCDGO), ManyToOne |
| `nombre` | `String` | `PJRQNMBR` |  |
| `nivel` | `Long` | `PJRQNVLL` |  |
| `codigoPadre` | `Long` | `PJRQCDPD` |  |
| `ingresado` | `Long` | `PJRQINGR` |  |

### `Jerarquia` → tabla **`SCP.PGSP`**

Secuencia PK: `SCP.SQ_PGSPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PGSPCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `PGSPNMBR` |  |
| `nivel` | `Long` | `PGSPNVLL` |  |
| `codigoPadre` | `Long` | `PGSPCDPD` |  |
| `descripcion` | `String` | `PGSPDSCP` |  |
| `ultimoNivel` | `Long` | `PGSPULNV` |  |
| `rubroTipoEstructuraP` | `Long` | `PGSPRYYA` |  |
| `rubroTipoEstructuraH` | `Long` | `PGSPRZZA` |  |
| `codigoAlterno` | `Long` | `PGSPCDAL` |  |
| `rubroNivelCaracteristicaP` | `Long` | `PGSPRYYB` |  |
| `rubroNivelCaracteristicaH` | `Long` | `PGSPRZZB` |  |

### `Pais` → tabla **`CRD.PSSS`**  ⚠️ paquete `scp`, esquema `CRD`

> **La clase está en `com.saa.model.scp.Pais` pero la tabla es `CRD.PSSS`. NO coinciden, y es
> a propósito. No lo "arregles".**

Es la única entidad del sistema donde el paquete Java y el esquema de base no se corresponden.
Las dos decisiones son independientes y se tomaron por separado:

| | Qué pasó | Estado |
|---|---|---|
| **Paquete Java** | Se movió de `com.saa.model.crd` a `com.saa.model.scp` el 2026-08-24 | **Aplicado, vigente** |
| **Esquema de base** | Se intentó migrar `CRD.PSSS` → `SCP.PSSS` el 2026-08-24 | **Falló en producción. NO aplicado** |

**Por qué se movió el paquete:** `com.saa.model.tsr.Titular` importaba
`com.saa.model.crd.Pais`. Era la única dependencia `tsr → crd` del backend y dejaba a `tsr`
sin compilar si se retiraba el módulo `crd`. El país no es un concepto de créditos: es un
catálogo de núcleo, como `Empresa`, `Usuario`, `Rubro` y `DetalleRubro`.

**Por qué la tabla se quedó donde estaba:** la migración de datos se corrió en producción, no
salió bien, y se decidió no reintentarla por ahora. El arreglo de compilación **no depende**
del esquema, así que revertir solo el `@Table` alcanzó para dejar todo consistente sin
deshacer el refactor de Java. Ver `docs/general/sql/MIGRACION-PAIS-CRD-A-SCP.md`, marcada
**NO APLICADA**.

**Lo que sigue pendiente:** la FK `TSR.TTLR.PSSSCDGO → CRD.PSSS`. Es lo único que falta para
poder extraer `crd`: la fuga de *compilación* ya está resuelta, la de *integridad
referencial* no.

El `@Path("psss")` del REST no cambió en ningún momento, así que la URL sigue siendo
`/SaaBE/rest/psss/...`.

Capas: `com.saa.basico.ejb/ejbImpl.PaisDaoService*` y `PaisService*`,
`com.saa.ws.rest.basico.PaisRest`, constante en `NombreEntidadesSistema.PAIS`.

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PSSSCDGO` | **PK**, IDENTITY |
| `codigoAlterno` | `String` | `PSSSCDAL` | length=10 |
| `nombre` | `String` | `PSSSNMBR` | length=2000 |
| `nacionalidad` | `String` | `PSSSNCNL` | length=2000 |
| `codigoNacionalidad` | `String` | `PSSSCDNC` | length=10 |
| `codigoExterno` | `String` | `PSSSCDEX` | length=50 |
| `estado` | `Long` | `PSSSIDST` |  |

### `Rubro` → tabla **`SCP.PRBR`**

Secuencia PK: `SCP.SQ_PRBRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRBRCDGO` | **PK**, SEQUENCE |
| `descripcion` | `String` | `PRBRDSCR` |  |
| `fechaIngreso` | `Date` | `PRBRFCHA` |  |
| `codigoAlterno` | `Long` | `PRBRALTR` |  |
| `tipo` | `Long` | `PRBRTPOO` |  |

### `Usuario` → tabla **`SCP.PJRQ`**

Secuencia PK: `SCP.SQ_PJRQCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PJRQCDGO` | **PK**, SEQUENCE |
| `jerarquia` | `Jerarquia` | `PGSPCDGO` | FK → `Jerarquia` (PGSPCDGO), ManyToOne |
| `nombre` | `String` | `PJRQNMBR` |  |
| `nivel` | `Long` | `PJRQNVLL` |  |
| `codigoPadre` | `Long` | `PJRQCDPD` |  |
| `ingresado` | `Long` | `PJRQINGR` |  |

---

## TSR — Tesorería (`com.saa.model.tsr`)

Constantes de entidades: `NombreEntidadesTesoreria`

### `AuxDepositoBanco` → tabla **`TSR.ADTD`**

Secuencia PK: `TSR.SQ_ADTDCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ADTDCDGO` | **PK**, SEQUENCE |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `valor` | `Double` | `ADTDVLRR` |  |
| `valorEfectivo` | `Double` | `ADTDVLEF` |  |
| `valorCheque` | `Double` | `ADTDVLCH` |  |

### `AuxDepositoCierre` → tabla **`TSR.ACPD`**

Secuencia PK: `TSR.SQ_ACPDCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `ACPDCDGO` | **PK**, SEQUENCE |
| `cierreCaja` | `CierreCaja` | `CRCJCDGO` | FK → `CierreCaja` (CRCJCDGO), ManyToOne |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `montoEfectivo` | `Double` | `ACPDMNEF` |  |
| `montoCheque` | `Double` | `ACPDMNCH` |  |
| `seleccionado` | `Long` | `ACPDSLCC` |  |
| `montoDeposito` | `Double` | `ACPDMNDP` |  |
| `montoTotalCierre` | `Double` | `ACPDMNTT` |  |
| `fechaCierre` | `LocalDateTime` | `ACPDFCCR` |  |
| `nombreCaja` | `String` | `ACPDNMCJ` | length=500 |

### `AuxDepositoDesglose` → tabla **`TSR.APDS`**

Secuencia PK: `TSR.SQ_APDSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `APDSCDGO` | **PK**, SEQUENCE |
| `tipo` | `Long` | `APDSTPOO` |  |
| `valor` | `Double` | `APDSVLRR` |  |
| `seleccionado` | `Long` | `APDSSLCC` |  |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `numeroCheque` | `Long` | `CCHQNMRO` |  |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `cobroCheque` | `CobroCheque` | `CCHQCDGO` | FK → `CobroCheque` (CCHQCDGO), ManyToOne |

### `Banco` → tabla **`TSR.BNCO`**

Secuencia PK: `TSR.SQ_BNCOCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `BNCOCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `BNCONMBR` |  |
| `conciliaDescuadre` | `Long` | `BNCOCNDS` |  |
| `estado` | `Long` | `BNCOESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `rubroTipoBancoP` | `Long` | `BNCORYYA` |  |
| `rubroTipoBancoH` | `Long` | `BNCORZZA` |  |
| `fechaIngreso` | `LocalDateTime` | `BNCOFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `BNCOFCDS` |  |

### `BancoExterno` → tabla **`TSR.BEXT`**

Secuencia PK: `TSR.SQ_BEXTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `BEXTCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `BEXTNMBR` |  |
| `tarjeta` | `Long` | `BEXTTRJT` |  |
| `estado` | `Long` | `BEXTESTD` |  |
| `fechaIngreso` | `LocalDateTime` | `BEXTFCIN` |  |

### `CajaFisica` → tabla **`TSR.CJAA`**

Secuencia PK: `TSR.SQ_CJAACDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CJAACDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `nombre` | `String` | `CJAANMBR` | length=500 |
| `fechaIngreso` | `LocalDateTime` | `CJAAFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `CJAAFCDS` |  |
| `estado` | `Long` | `CJAAESTD` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |

### `CajaLogica` → tabla **`TSR.CJCN`**

Secuencia PK: `TSR.SQ_CJCNCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CJCNCDGO` | **PK**, SEQUENCE |
| `grupoCaja` | `GrupoCaja` | `CJINCDGO` | FK → `GrupoCaja` (CJINCDGO), ManyToOne |
| `nombre` | `String` | `CJCNNMBR` | length=500 |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `cuentaContable` | `String` | `CJCNCNCT` | length=100 |
| `fechaIngreso` | `LocalDateTime` | `CJCNFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `CJCNFCDS` |  |
| `estado` | `Long` | `CJCNESTD` |  |

### `CajaLogicaPorCajaFisica` → tabla **`TSR.CCXC`**

Secuencia PK: `TSR.SQ_CCXCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CCXCCDGO` | **PK**, SEQUENCE |
| `cajaLogica` | `CajaLogica` | `CJCNCDGO` | FK → `CajaLogica` (CJCNCDGO), ManyToOne |
| `cajaFisica` | `CajaFisica` | `CJAACDGO` | FK → `CajaFisica` (CJAACDGO), ManyToOne |
| `estado` | `Long` | `CCXCESTD` |  |
| `fechaIngreso` | `LocalDateTime` | `CCXCFCIN` |  |

### `Cheque` → tabla **`TSR.DTCH`**

Secuencia PK: `TSR.SQ_DTCHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTCHCDGO` | **PK**, SEQUENCE |
| `chequera` | `Chequera` | `CHQRCDGO` | FK → `Chequera` (CHQRCDGO), ManyToOne |
| `numero` | `Long` | `DTCHNMRO` |  |
| `egreso` | `Long` | `DTCHEGRS` |  |
| `fechaUso` | `LocalDateTime` | `DTCHFUSO` |  |
| `fechaCaduca` | `LocalDateTime` | `DTCHFCDC` |  |
| `fechaAnulacion` | `LocalDateTime` | `DTCHFANL` |  |
| `rubroEstadoChequeP` | `Long` | `DTCHRYYA` |  |
| `rubroEstadoChequeH` | `Long` | `DTCHRZZA` |  |
| `fechaImpresion` | `LocalDateTime` | `DTCHFIMP` |  |
| `fechaEntrega` | `LocalDateTime` | `DTCHFENT` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `titular` | `Titular` | `PRSNCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `valor` | `Double` | `DTCHVLRR` |  |
| `rubroMotivoAnulacionP` | `Long` | `DTCHRYYB` |  |
| `rubroMotivoAnulacionH` | `Long` | `DTCHRZZB` |  |
| `beneficiario` | `String` | `DTCHBNFC` |  |
| `idBeneficiario` | `Titular` | `DTCHIDBN` | FK → `Titular` (TTLRCDGO), ManyToOne |

### `Chequera` → tabla **`TSR.CHQR`**

Secuencia PK: `TSR.SQ_CHQRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CHQRCDGO` | **PK**, SEQUENCE |
| `fechaSolicitud` | `LocalDateTime` | `CHQRFCSL` |  |
| `fechaEntrega` | `LocalDateTime` | `CHQRFCEN` |  |
| `numeroCheques` | `Long` | `CHQRNMRO` |  |
| `comienza` | `Long` | `CHQRCMNZ` |  |
| `finaliza` | `Long` | `CHQRFNLZ` |  |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `rubroEstadoChequeraP` | `Long` | `CHQRRYYA` |  |
| `rubroEstadoChequeraH` | `Long` | `CHQRRZZA` |  |

### `CierreCaja` → tabla **`TSR.CRCJ`**

Secuencia PK: `TSR.SQ_CRCJCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CRCJCDGO` | **PK**, SEQUENCE |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `fechaCierre` | `LocalDateTime` | `CRCJFCHA` |  |
| `nombreUsuario` | `String` | `CRCJUSRO` | length=50 |
| `monto` | `Double` | `CRCJMNTO` |  |
| `rubroEstadoP` | `Long` | `CRCJRYYA` |  |
| `rubroEstadoH` | `Long` | `CRCJRZZA` |  |
| `montoEfectivo` | `Double` | `CRCJMNEF` |  |
| `montoCheque` | `Double` | `CRCJMNCH` |  |
| `montoTarjeta` | `Double` | `CRCJMNTJ` |  |
| `montoTransferencia` | `Double` | `CRCJMNTR` |  |
| `montoRetencion` | `Double` | `CRCJMNRT` |  |
| `deposito` | `Deposito` | `DPSTCDGO` | FK → `Deposito` (DPSTCDGO), ManyToOne |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `Cobro` → tabla **`TSR.CBRO`**

Secuencia PK: `TSR.SQ_CBROCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CBROCDGO` | **PK**, SEQUENCE |
| `tipoId` | `Long` | `CBROTPID` |  |
| `numeroId` | `String` | `CBROIDNT` | length=20 |
| `cliente` | `String` | `CBROCLNT` | length=200 |
| `descripcion` | `String` | `CBRODSCR` |  |
| `fecha` | `LocalDateTime` | `CBROFCHA` |  |
| `nombreUsuario` | `String` | `CBROUSRO` |  |
| `valor` | `Double` | `CBROVLRR` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `cierreCaja` | `CierreCaja` | `CRCJCDGO` | FK → `CierreCaja` (CRCJCDGO), ManyToOne |
| `fechaInactivo` | `LocalDateTime` | `CBROFCDS` |  |
| `rubroMotivoAnulacionP` | `Long` | `CBRORYYB` |  |
| `rubroMotivoAnulacionH` | `Long` | `CBRORZZB` |  |
| `rubroEstadoP` | `Long` | `CBRORYYA` |  |
| `rubroEstadoH` | `Long` | `CBRORZZA` |  |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `cajaLogica` | `CajaLogica` | `CJCNCDGO` | FK → `CajaLogica` (CJCNCDGO), ManyToOne |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `deposito` | `Deposito` | `DPSTCDGO` | FK → `Deposito` (DPSTCDGO), ManyToOne |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoCobro` | `Long` | `CBROTPCB` |  |
| `numeroAsiento` | `Long` | `CBRONMAS` |  |

### `CobroCheque` → tabla **`TSR.CCHQ`**

Secuencia PK: `TSR.SQ_CCHQCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CCHQCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `numero` | `Long` | `CCHQNMRO` |  |
| `valor` | `Double` | `CCHQVLRR` |  |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `estado` | `Long` | `CCHQESTD` |  |

### `CobroEfectivo` → tabla **`TSR.CEFC`**

Secuencia PK: `TSR.SQ_CEFCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CEFCCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `valor` | `Double` | `CEFCVLRR` |  |

### `CobroRetencion` → tabla **`TSR.CRTN`**

Secuencia PK: `TSR.SQ_CRTNCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CRTNCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `plantilla` | `Plantilla` | `PLNSCDGO` | FK → `Plantilla` (PLNSCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `valor` | `Double` | `CRTNVLRR` |  |
| `numero` | `String` | `CRTNNMRO` | length=50 |

### `CobroTarjeta` → tabla **`TSR.CTRJ`**

Secuencia PK: `TSR.SQ_CTRJCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CTRJCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `numero` | `Long` | `CTRJNMRO` |  |
| `valor` | `Double` | `CTRJVLRR` |  |
| `numeroVoucher` | `Long` | `CTRJVCHR` |  |
| `fechaCaducidad` | `LocalDateTime` | `CTRJFCDC` |  |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |

### `CobroTransferencia` → tabla **`TSR.CTRN`**

Secuencia PK: `TSR.SQ_CTRNCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CTRNCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `cuentaOrigen` | `String` | `CTRNCTOR` | length=50 |
| `numeroTransferencia` | `Long` | `CTRNNMRO` |  |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `cuentaDestino` | `String` | `CTRNCTDS` | length=50 |
| `valor` | `Double` | `CTRNVLRR` |  |

### `Conciliacion` → tabla **`TSR.CNCL`**

Secuencia PK: `TSR.SQ_CNCLCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNCLCDGO` | **PK**, SEQUENCE |
| `idPeriodo` | `Long` | `CNCLPRDO` |  |
| `usuario` | `Usuario` | `CNCLUSRR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `CNCLFCHA` |  |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `inicialSistema` | `Double` | `CNCLSILB` |  |
| `depositoSistema` | `Double` | `CNCLDPST` |  |
| `creditoSistema` | `Double` | `CNCLNCRD` |  |
| `chequeSistema` | `Double` | `CNCLCHQE` |  |
| `debitoSistema` | `Double` | `CNCLNTDB` |  |
| `finalSistema` | `Double` | `CNCLSLDF` |  |
| `saldoEstadoCuenta` | `Double` | `CNCLSLDE` |  |
| `depositoTransito` | `Double` | `CNCLDPTR` |  |
| `chequeTransito` | `Double` | `CNCLCHNC` |  |
| `creditoTransito` | `Double` | `CNCLNCTR` |  |
| `debitoTransito` | `Double` | `CNCLNDTR` |  |
| `saldoBanco` | `Double` | `CNCLSLDB` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `rubroEstadoP` | `Long` | `CNCLRYYA` |  |
| `rubroEstadoH` | `Long` | `CNCLRZZA` |  |
| `transferenciaDebitoTransito` | `Double` | `CNCLTDTR` |  |
| `transferenciaCreditoTransito` | `Double` | `CNCLTCTR` |  |
| `transferenciaDebitoSistema` | `Double` | `CNCLTRDB` |  |
| `transferenciaCreditoSistema` | `Double` | `CNCLTRCR` |  |

### `ConciliacionContable` → tabla **`TSR.CNCT`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNCTCDGO` | **PK**, IDENTITY |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria`, ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo`, ManyToOne |
| `estadoRevision` | `Long` | `CNCTESTR` |  |
| `totalGrupos` | `Long` | `CNCTTTGR` |  |
| `totalPendientesExtracto` | `Long` | `CNCTPDEX` |  |
| `totalPendientesAsiento` | `Long` | `CNCTPDAS` |  |
| `usuarioVerifica` | `String` | `CNCTUSVR` | length=50 |
| `fechaVerificacion` | `LocalDateTime` | `CNCTFCVR` |  |
| `fechaCreacion` | `LocalDateTime` | `CNCTFCRG` |  |
| `estado` | `Long` | `CNCTESTD` |  |

### `ControlExtractoBancario` → tabla **`TSR.CTEB`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CTEBCDGO` | **PK**, IDENTITY |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa`, ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo`, ManyToOne |
| `mes` | `Long` | `CTEBMSSS` |  |
| `anio` | `Long` | `CTEBANOO` |  |
| `fechaVencimiento` | `LocalDate` | `CTEBFVNC` |  |
| `totalCuentas` | `Long` | `CTEBTOTC` |  |
| `cuentasCargadas` | `Long` | `CTEBCARG` |  |
| `cuentasConciliadas` | `Long` | `CTEBCONC` |  |
| `observaciones` | `String` | `CTEBOBSR` | length=1000 |
| `fechaCreacion` | `LocalDateTime` | `CTEBFCRG` |  |
| `estado` | `Long` | `CTEBESTD` |  |
| `cerrado` | `Long` | `CTEBCRRE` |  |
| `usuarioCierre` | `String` | `CTEBUSCR` | length=50 |
| `fechaCierre` | `LocalDateTime` | `CTEBFCCR` |  |

### `CuentaBancaria` → tabla **`TSR.CNBC`**

Secuencia PK: `TSR.SQ_BNCOCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNBCCDGO` | **PK**, SEQUENCE |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `numeroCuenta` | `String` | `CNBCNMRO` |  |
| `rubroTipoCuentaP` | `Long` | `CNBCRYYA` |  |
| `rubroTipoCuentaH` | `Long` | `CNBCRZZA` |  |
| `saldoInicial` | `Double` | `CNBCSLIN` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `fechaCreacion` | `LocalDateTime` | `CNBCFCCR` |  |
| `titular` | `String` | `CNBCTTLR` |  |
| `rubroTipoMonedaP` | `Long` | `CNBCRYYB` |  |
| `rubroTipoMonedaH` | `Long` | `CNBCRZZB` |  |
| `oficialCuenta` | `String` | `CNBCOFCL` |  |
| `telefono1` | `String` | `CNBCTLF1` |  |
| `telefono2` | `String` | `CNBCTLF2` |  |
| `celular` | `String` | `CNBCCLLR` |  |
| `fax` | `String` | `CNBCFXXX` |  |
| `email` | `String` | `CNBCEMLL` |  |
| `direccion` | `String` | `CNBCDRCC` |  |
| `observacion` | `String` | `CNBCOBSR` |  |
| `estado` | `Long` | `CNBCESTD` |  |
| `fechaIngreso` | `LocalDateTime` | `CNBCFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `CNBCFCDS` |  |
| `cuentaApertura` | `PlanCuenta` | `PLNNORGN` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `cobroCredito` | `Long` | `CNBCCBCR` |  |

### `CuentaBancariaTitular` → tabla **`TSR.CTBN`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CTBNCDGO` | **PK**, IDENTITY |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular`, ManyToOne |
| `banco` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno`, ManyToOne |
| `tipoCuenta` | `Long` | `CTBNTPCT` |  |
| `numeroCuenta` | `String` | `CTBNNMCT` | length=50 |
| `observaciones` | `String` | `CTBNOBSR` | length=500 |
| `estado` | `Long` | `CTBNESTD` |  |
| `fechaCreacion` | `LocalDateTime` | `CTBNFCRG` |  |
| `usuarioCreacion` | `String` | `CTBNUSAR` | length=50 |

### `DebitoCredito` → tabla **`TSR.DBCR`**

Secuencia PK: `TSR.SQ_DBCRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DBCRCDGO` | **PK**, SEQUENCE |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `descripcion` | `String` | `DBCRDSCR` | length=500 |
| `tipo` | `Long` | `DSCRTPOO` |  |
| `numeroAsiento` | `Long` | `DBCRNMAS` |  |
| `nombreUsuario` | `String` | `DBCRUSRO` | length=200 |
| `fecha` | `LocalDateTime` | `DBCRFCHA` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `movimientoBanco` | `MovimientoBanco` | `MVCBCDGO` | FK → `MovimientoBanco` (MVCBCDGO), ManyToOne |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `estado` | `Long` | `DBCRESTD` |  |

### `Deposito` → tabla **`TSR.DPST`**

Secuencia PK: `TSR.SQ_DPSTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DPSTCDGO` | **PK**, SEQUENCE |
| `totalEfectivo` | `Double` | `DPSTEFCT` |  |
| `totalCheque` | `Double` | `DPSTCHQS` |  |
| `totalDeposito` | `Double` | `DPSTTOTL` |  |
| `nombreUsuario` | `String` | `DPSTUSRO` | length=50 |
| `fechaDeposito` | `LocalDateTime` | `DPSTFCHA` |  |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `estado` | `Long` | `DPSTESTD` |  |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `asiento` | `Asiento` | `ASNTRTCD` | FK → `Asiento` (ASNTCDGO), ManyToOne |

### `DesgloseDetalleDeposito` → tabla **`TSR.DSDT`**

Secuencia PK: `TSR.SQ_DSDTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DSDTCDGO` | **PK**, SEQUENCE |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `tipo` | `Long` | `DSDTTPOO` |  |
| `valor` | `Double` | `DSDTVLRR` |  |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `numeroCheque` | `Long` | `CCHQNMRO` |  |
| `cobroCheque` | `CobroCheque` | `CCHQCDGO` | FK → `CobroCheque` (CCHQCDGO), ManyToOne |

### `DetalleCierre` → tabla **`TSR.DTCR`**

Secuencia PK: `TSR.SQ_DTCRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTCRCDGO` | **PK**, SEQUENCE |
| `cierreCaja` | `CierreCaja` | `CRCJCDGO` | FK → `CierreCaja` (CRCJCDGO), ManyToOne |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `nombreCliente` | `String` | `CBROCLNT` | length=200 |
| `fechaCobro` | `LocalDateTime` | `CBROFCHA` |  |
| `valorEfectivo` | `Double` | `DTCREFCT` |  |
| `valorCheque` | `Double` | `DTCRCHQQ` |  |
| `valorTarjeta` | `Double` | `DTCRTRJC` |  |
| `valorTransferencia` | `Double` | `DTCRTRNS` |  |
| `valorRetencion` | `Double` | `DTCRRTNC` |  |
| `valorTotal` | `Double` | `CBROVLRR` |  |

### `DetalleConciliacion` → tabla **`TSR.DTCL`**

Secuencia PK: `TSR.SQ_DTCLCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTCLCDGO` | **PK**, SEQUENCE |
| `conciliacion` | `Conciliacion` | `CNCLCDGO` | FK → `Conciliacion` (CNCLCDGO), ManyToOne |
| `descripcion` | `String` | `DTCLDSCR` | length=200 |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `valor` | `Double` | `DTCLVLRR` |  |
| `conciliado` | `Long` | `DTCLCNCL` |  |
| `numeroCheque` | `Long` | `DTCLCHQN` |  |
| `rubroTipoMovimientoP` | `Long` | `DTCLRYYA` |  |
| `rubroTipoMovimientoH` | `Long` | `DTCLRZZA` |  |
| `estado` | `Long` | `DTCLESTD` |  |
| `numeroAsiento` | `Long` | `DTCLNMAS` |  |
| `fechaRegistro` | `LocalDate` | `DTCLFRGS` |  |
| `idMovimiento` | `Long` | `DTCLIDMV` |  |
| `cheque` | `Cheque` | `DTCHCDGO` | FK → `Cheque` (DTCHCDGO), ManyToOne |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `numeroMes` | `Long` | `DTCLMSSS` |  |
| `numeroAnio` | `Long` | `DTCLANOO` |  |
| `rubroOrigenP` | `Long` | `DTCLRYYB` |  |
| `rubroOrigenH` | `Long` | `DTCLRZZB` |  |

### `DetalleCumplimientoCuenta` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `cuentaBancaria` | `CuentaBancaria` | `*(sin @Column)*` |  |
| `cargada` | `boolean` | `*(sin @Column)*` |  |
| `conciliada` | `boolean` | `*(sin @Column)*` |  |

### `DetalleDebitoCredito` → tabla **`TSR.DTDC`**

Secuencia PK: `TSR.SQ_DTDCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTDCCDGO` | **PK**, SEQUENCE |
| `debitoCredito` | `DebitoCredito` | `DBCRCDGO` | FK → `DebitoCredito` (DBCRCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `descripcion` | `String` | `DTDCDSCR` | length=500 |
| `valor` | `Double` | `DTDCVLRR` |  |

### `DetalleDeposito` → tabla **`TSR.DTDP`**

Secuencia PK: `TSR.SQ_DTDPCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DTDPCDGO` | **PK**, SEQUENCE |
| `deposito` | `Deposito` | `DPSTCDGO` | FK → `Deposito` (DPSTCDGO), ManyToOne |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `valor` | `Double` | `DTDPVLRR` |  |
| `valorEfectivo` | `Double` | `DTDPVLEF` |  |
| `valorCheque` | `Double` | `DTDPVLCH` |  |
| `estado` | `Long` | `DTDPESTD` |  |
| `fechaEnvio` | `LocalDateTime` | `DTDPFCEN` |  |
| `fechaRatificacion` | `LocalDateTime` | `DTDPFCRT` |  |
| `numeroDeposito` | `String` | `DTDPNMDP` | length=500 |
| `asiento` | `Asiento` | `ASNTRTFC` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `usuario` | `Usuario` | `USRORTFC` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `nombreUsuario` | `String` | `USRTNMBR` | length=500 |

### `DetalleExtractoBancario` → tabla **`TSR.DEXB`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DEXBCDGO` | **PK**, IDENTITY |
| `extractoBancario` | `ExtractoBancario` | `EXBCCDGO` | FK → `ExtractoBancario`, ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria`, ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo`, ManyToOne |
| `fechaTransaccion` | `LocalDate` | `DEXBFTRN` |  |
| `fechaContable` | `LocalDate` | `DEXBFCNT` |  |
| `descripcion` | `String` | `DEXBDSCR` | length=500 |
| `referencia` | `String` | `DEXBREFR` | length=100 |
| `codigoMovimiento` | `String` | `DEXBCDMV` | length=20 |
| `debito` | `Double` | `DEXBDBTO` |  |
| `credito` | `Double` | `DEXBCRDT` |  |
| `saldo` | `Double` | `DEXBSLDO` |  |
| `hash` | `String` | `DEXBHASH` | length=64 |
| `numeroFila` | `Long` | `DEXBNFIL` |  |
| `filaCruda` | `String` | `DEXBCRDO` | LOB |
| `movimientoConciliado` | `MovimientoBanco` | `DEXBCNCL` | FK → `MovimientoBanco`, ManyToOne |
| `estadoRevision` | `Long` | `DEXBESTR` |  |
| `fechaCreacion` | `LocalDateTime` | `DEXBFCRG` |  |
| `usuarioCreacion` | `String` | `DEXBUSAR` | length=50 |
| `estado` | `Long` | `DEXBESTD` |  |

### `DireccionPersona` → tabla **`TSR.PDRC`**

Secuencia PK: `TSR.SQ_PDRCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PDRCCDGO` | **PK**, SEQUENCE |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `rubroTipoDireccionP` | `Long` | `PDRCRYYA` |  |
| `rubroTipoDireccionH` | `Long` | `PDRCRZZA` |  |
| `ubicacion` | `String` | `PDRCDRCC` | length=100 |
| `principal` | `Long` | `PDRCPRNC` |  |

### `ExtractoBancario` → tabla **`TSR.EXBC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `EXBCCDGO` | **PK**, IDENTITY |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria`, ManyToOne |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa`, ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo`, ManyToOne |
| `archivoNombre` | `String` | `EXBCARCH` | length=255 |
| `archivoHash` | `String` | `EXBCHASH` | length=64 |
| `formato` | `String` | `EXBCFRMT` | length=10 |
| `parser` | `String` | `EXBCPRSR` | length=50 |
| `fechaDesde` | `LocalDate` | `EXBCFDSD` |  |
| `fechaHasta` | `LocalDate` | `EXBCFHST` |  |
| `saldoInicial` | `Double` | `EXBCSLIN` |  |
| `saldoFinal` | `Double` | `EXBCSLFN` |  |
| `estadoCarga` | `Long` | `EXBCESTP` |  |
| `observaciones` | `String` | `EXBCOBSR` | length=1000 |
| `fechaCreacion` | `LocalDateTime` | `EXBCFCRG` |  |
| `usuarioCreacion` | `String` | `EXBCUSAR` | length=50 |
| `estado` | `Long` | `EXBCESTD` |  |

### `GrupoCaja` → tabla **`TSR.CJIN`**

Secuencia PK: `TSR.SQ_CJINCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CJINCDGO` | **PK**, SEQUENCE |
| `nombre` | `String` | `CJINNMBR` | length=500 |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaIngreso` | `LocalDateTime` | `CJINFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `CJINFCDS` |  |
| `estado` | `Long` | `CJINESTD` |  |

### `GrupoConciliacionAsiento` → tabla **`TSR.GCAS`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GCASCDGO` | **PK**, IDENTITY |
| `grupo` | `GrupoConciliacionContable` | `GRCCCDGO` | FK → `GrupoConciliacionContable`, ManyToOne |
| `detalleAsiento` | `DetalleAsiento` | `DTASCDGO` | FK → `DetalleAsiento`, ManyToOne |

### `GrupoConciliacionContable` → tabla **`TSR.GRCC`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GRCCCDGO` | **PK**, IDENTITY |
| `conciliacionContable` | `ConciliacionContable` | `CNCTCDGO` | FK → `ConciliacionContable`, ManyToOne |
| `valorExtracto` | `Double` | `GRCCVLEX` |  |
| `valorAsiento` | `Double` | `GRCCVLAS` |  |
| `diferencia` | `Double` | `GRCCDIFF` |  |
| `fechaMinima` | `LocalDate` | `GRCCFCMN` |  |
| `fechaMaxima` | `LocalDate` | `GRCCFCMX` |  |
| `toleranciaDiasAplicada` | `Long` | `GRCCTOLD` |  |
| `usuarioConcilia` | `String` | `GRCCUSCN` | length=50 |
| `fechaConciliacion` | `LocalDateTime` | `GRCCFCCN` |  |
| `observaciones` | `String` | `GRCCOBSR` | length=500 |
| `estado` | `Long` | `GRCCESTD` |  |

### `GrupoConciliacionExtracto` → tabla **`TSR.GCEX`**

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `GCEXCDGO` | **PK**, IDENTITY |
| `grupo` | `GrupoConciliacionContable` | `GRCCCDGO` | FK → `GrupoConciliacionContable`, ManyToOne |
| `detalleExtractoBancario` | `DetalleExtractoBancario` | `DEXBCDGO` | FK → `DetalleExtractoBancario`, ManyToOne |

### `HistConciliacion` → tabla **`TSR.CNCH`**

Secuencia PK: `TSR.SQ_CNCHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CNCHCDGO` | **PK**, SEQUENCE |
| `idPeriodo` | `Long` | `CNCHPRDO` |  |
| `usuario` | `Usuario` | `CNCHUSRR` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fecha` | `LocalDateTime` | `CNCHFCHA` |  |
| `estado` | `Long` | `CNCHESTD` |  |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `inicialSistema` | `Double` | `CNCHSILB` |  |
| `depositoSistema` | `Double` | `CNCHDPST` |  |
| `creditoSistema` | `Double` | `CNCHNCRD` |  |
| `chequeSistema` | `Double` | `CNCHCHQE` |  |
| `debitoSistema` | `Double` | `CNCHNTDB` |  |
| `finalSistema` | `Double` | `CNCHSLDF` |  |
| `saldoEstadoCuenta` | `Double` | `CNCHSLDE` |  |
| `depositoTransito` | `Double` | `CNCHDPTR` |  |
| `chequeTransito` | `Double` | `CNCHCHNC` |  |
| `creditoTransito` | `Double` | `CNCHNCTR` |  |
| `debitoTransito` | `Double` | `CNCHNDTR` |  |
| `saldoBanco` | `Double` | `CNCHSLDB` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `transferenciaDebitoTransito` | `Double` | `CNCHTDTR` |  |
| `transferenciaCreditoTransito` | `Double` | `CNCHTCTR` |  |
| `transferenciaDebitoSistema` | `Double` | `CNCHTRDB` |  |
| `transferenciaCreditoSistema` | `Double` | `CNCHTRCR` |  |
| `idConciliacionOrigen` | `Long` | `CNCHCNCD` |  |

### `HistDetalleConciliacion` → tabla **`TSR.DCHI`**

Secuencia PK: `TSR.SQ_DCHICDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `DCHICDGO` | **PK**, SEQUENCE |
| `histConciliacion` | `HistConciliacion` | `CNCHCDGO` | FK → `HistConciliacion` (CNCHCDGO), ManyToOne |
| `rubroTipoMovimientoP` | `Long` | `DCHIRYYA` |  |
| `rubroTipoMovimientoH` | `Long` | `DCHIRZZA` |  |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `valor` | `Double` | `DCHIVLRR` |  |
| `conciliado` | `Long` | `DCHICNCL` |  |
| `numeroCheque` | `String` | `DCHICHQN` | length=50 |
| `rubroOrigenP` | `Long` | `DCHIRYYB` |  |
| `rubroOrigenH` | `Long` | `DCHIRZZB` |  |
| `estado` | `Long` | `DCHIESTD` |  |
| `numeroAsiento` | `Long` | `DCHINMAS` |  |
| `descripcion` | `String` | `DCHIDSCR` | length=200 |
| `fechaRegistro` | `LocalDate` | `DCHIFRGS` |  |
| `idMovimiento` | `Long` | `DCHIIDMV` |  |
| `cheque` | `Cheque` | `DTCHCDGO` | FK → `Cheque` (DTCHCDGO), ManyToOne |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `numeroMes` | `Long` | `DCHIMSSS` |  |
| `numeroAnio` | `Long` | `DCHIANOO` |  |

### `MotivoCobro` → tabla **`TSR.CMTV`**

Secuencia PK: `TSR.SQ_CMTVCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `CMTVCDGO` | **PK**, SEQUENCE |
| `cobro` | `Cobro` | `CBROCDGO` | FK → `Cobro` (CBROCDGO), ManyToOne |
| `descripcion` | `String` | `CMTVDSCR` | length=200 |
| `valor` | `Double` | `CMTVVLRR` |  |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |

### `MotivoPago` → tabla **`TSR.PMTV`**

Secuencia PK: `TSR.SQ_PMTVCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PMTVCDGO` | **PK**, SEQUENCE |
| `pago` | `Pago` | `PGSSCDGO` | FK → `Pago` (PGSSCDGO), ManyToOne |
| `plantilla` | `Plantilla` | `PLNSCDGO` | FK → `Plantilla` (PLNSCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `descripcion` | `String` | `PMTVDSCR` | length=200 |
| `valor` | `Double` | `PMTVVLRR` |  |

### `MovimientoBanco` → tabla **`TSR.MVCB`**

Secuencia PK: `TSR.SQ_MVCBCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `MVCBCDGO` | **PK**, SEQUENCE |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `descripcion` | `String` | `MVCBDSCR` | length=200 |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `valor` | `Double` | `MVCBVLRR` |  |
| `conciliado` | `Long` | `MVCBCNCL` |  |
| `fechaConciliacion` | `LocalDateTime` | `MVCBFCCN` |  |
| `numeroCheque` | `Long` | `MVCBCHQN` |  |
| `conciliacion` | `Conciliacion` | `CNCLCDGO` | FK → `Conciliacion` (CNCLCDGO), ManyToOne |
| `rubroTipoMovimientoP` | `Long` | `MVCBRYYA` |  |
| `rubroTipoMovimientoH` | `Long` | `MVCBRZZA` |  |
| `fechaRegistro` | `LocalDate` | `MVCBFRGS` |  |
| `numeroAsiento` | `Long` | `MVCBNMAS` |  |
| `idMovimiento` | `Long` | `MVCBIDMV` |  |
| `estado` | `Long` | `MVCBESTD` |  |
| `cheque` | `Cheque` | `DTCHCDGO` | FK → `Cheque` (DTCHCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `detalleDeposito` | `DetalleDeposito` | `DTDPCDGO` | FK → `DetalleDeposito` (DTDPCDGO), ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `numeroMes` | `Long` | `MVCBMSSS` |  |
| `numeroAnio` | `Long` | `MVCBANOO` |  |
| `rubroOrigenP` | `Long` | `MVCBRYYB` |  |
| `rubroOrigenH` | `Long` | `MVCBRZZB` |  |

### `Pago` → tabla **`TSR.PGSS`**

Secuencia PK: `TSR.SQ_PGSSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PGSSCDGO` | **PK**, SEQUENCE |
| `tipoId` | `Long` | `PGSSTPID` |  |
| `numeroId` | `String` | `PGSSIDNT` | length=20 |
| `proveedor` | `String` | `PGSSPRVD` | length=200 |
| `descripcion` | `String` | `PGSSDSCR` | length=300 |
| `fechaPago` | `LocalDateTime` | `PGSSFCHA` |  |
| `nombreUsuario` | `String` | `PGSSUSRO` | length=50 |
| `valor` | `Double` | `PGSSVLRR` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaInactivo` | `LocalDateTime` | `PGSSFCDS` |  |
| `rubroMotivoAnulacionP` | `Long` | `PGSSRYYB` |  |
| `rubroMotivoAnulacionH` | `Long` | `PGSSRZZB` |  |
| `rubroEstadoP` | `Long` | `PGSSRYYA` |  |
| `rubroEstadoH` | `Long` | `PGSSRZZA` |  |
| `cheque` | `Cheque` | `DTCHCDGO` | FK → `Cheque` (DTCHCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `numeroAsiento` | `Long` | `PGSSNMAS` |  |
| `tipoPago` | `Long` | `PGSSTPPG` |  |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `idTempPago` | `Long` | `TPGSCDGO` |  |

### `PersonaCuentaContable` → tabla **`TSR.PRCC`**

Secuencia PK: `TSR.SQ_PRCCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRCCCDGO` | **PK**, SEQUENCE |
| `personaRol` | `PersonaRol` | `PRRLCDGO` | FK → `PersonaRol` (PRRLCDGO), ManyToOne |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `tipoCuenta` | `Long` | `PRCCTPOO` |  |
| `tipoPersona` | `Long` | `PRCCCLPR` |  |
| `planCuenta` | `PlanCuenta` | `PLNNCDGO` | FK → `PlanCuenta` (PLNNCDGO), ManyToOne |
| `saldoInicial` | `Double` | `PRCCSLIN` |  |

### `PersonaRol` → tabla **`TSR.PRRL`**

Secuencia PK: `TSR.SQ_PRRLCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PRRLCDGO` | **PK**, SEQUENCE |
| `titular` | `Titular` | `PRSNCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `rubroRolPersonaP` | `Long` | `PRRLRYYA` |  |
| `rubroRolPersonaH` | `Long` | `PRRLRZZA` |  |
| `diasVencimientoFactura` | `Long` | `PRRLDSVF` |  |
| `calificacionRiesgo` | `String` | `PRRLCLRS` |  |
| `estado` | `Long` | `PRRLESTD` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `diasCredito` | `Long` | `PRRLDSSC` |  |

### `ResumenConciliacionCuenta` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `cuentaBancaria` | `CuentaBancaria` | `*(sin @Column)*` |  |
| `idConciliacionContable` | `Long` | `*(sin @Column)*` |  |
| `estadoRevision` | `Long` | `*(sin @Column)*` |  |
| `totalPendientesExtracto` | `Long` | `*(sin @Column)*` |  |
| `totalPendientesAsiento` | `Long` | `*(sin @Column)*` |  |
| `usuarioVerifica` | `String` | `*(sin @Column)*` |  |
| `fechaVerificacion` | `LocalDateTime` | `*(sin @Column)*` |  |
| `extractoCargado` | `Boolean` | `*(sin @Column)*` |  |

### `ResumenImportacionExtracto` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `idCuentaBancaria` | `Long` | `*(sin @Column)*` |  |
| `idPeriodo` | `Long` | `*(sin @Column)*` |  |
| `nombrePeriodo` | `String` | `*(sin @Column)*` |  |
| `nombreBanco` | `String` | `*(sin @Column)*` |  |
| `numeroCuenta` | `String` | `*(sin @Column)*` |  |
| `archivoNombre` | `String` | `*(sin @Column)*` |  |
| `formatoDetectado` | `String` | `*(sin @Column)*` |  |
| `fechaDesde` | `LocalDate` | `*(sin @Column)*` |  |
| `fechaHasta` | `LocalDate` | `*(sin @Column)*` |  |
| `saldoInicial` | `Double` | `*(sin @Column)*` |  |
| `saldoFinal` | `Double` | `*(sin @Column)*` |  |
| `totalFilas` | `Integer` | `*(sin @Column)*` |  |
| `totalDebito` | `Double` | `*(sin @Column)*` |  |
| `totalCredito` | `Double` | `*(sin @Column)*` |  |
| `advertencias` | `List<String>` | `*(sin @Column)*` |  |
| `totalTransaccionesFueraPeriodo` | `Integer` | `*(sin @Column)*` |  |
| `transaccionesFueraPeriodo` | `List<String>` | `*(sin @Column)*` |  |
| `archivoYaCargado` | `boolean` | `*(sin @Column)*` |  |
| `idExtractoExistente` | `Long` | `*(sin @Column)*` |  |
| `idExtractoCreado` | `Long` | `*(sin @Column)*` |  |

### `SaldoBanco` → tabla **`TSR.SLCB`**

Secuencia PK: `TSR.SQ_SLCBCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `SLCBCDGO` | **PK**, SEQUENCE |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `periodo` | `Periodo` | `PRDOCDGO` | FK → `Periodo` (PRDOCDGO), ManyToOne |
| `numeroMes` | `Long` | `PRDOMSSS` |  |
| `numeroAnio` | `Long` | `PRDOANNN` |  |
| `saldoAnterior` | `Double` | `SLCBANTR` |  |
| `valorEgreso` | `Double` | `SLCBDBTO` |  |
| `valorIngreso` | `Double` | `SLCBCRDT` |  |
| `valorND` | `Double` | `SLCBNTDB` |  |
| `valorNC` | `Double` | `SLCBNTCR` |  |
| `saldoFinal` | `Double` | `SLCBFNLL` |  |
| `valorTransferenciaD` | `Double` | `SLCBTRDB` |  |
| `valorTransferenciaC` | `Double` | `SLCBTRCR` |  |

### `SolicitudConciliarGrupo` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `idCuentaBancaria` | `Long` | `*(sin @Column)*` |  |
| `idPeriodo` | `Long` | `*(sin @Column)*` |  |
| `idsDetalleExtracto` | `List<Long>` | `*(sin @Column)*` |  |
| `idsDetalleAsiento` | `List<Long>` | `*(sin @Column)*` |  |
| `usuario` | `String` | `*(sin @Column)*` |  |

### `SolicitudUsuario` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `usuario` | `String` | `*(sin @Column)*` |  |

### `SugerenciaConciliacionContable` — *sin `@Entity` (DTO / clase auxiliar, no mapea tabla)*

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `idsDetalleExtracto` | `List<Long>` | `*(sin @Column)*` |  |
| `idsDetalleAsiento` | `List<Long>` | `*(sin @Column)*` |  |
| `valorExtracto` | `Double` | `*(sin @Column)*` |  |
| `valorAsiento` | `Double` | `*(sin @Column)*` |  |
| `fechaMinima` | `LocalDate` | `*(sin @Column)*` |  |
| `fechaMaxima` | `LocalDate` | `*(sin @Column)*` |  |
| `descripcionResumen` | `String` | `*(sin @Column)*` |  |

### `TelefonoDireccion` → tabla **`TSR.PCNT`**

Secuencia PK: `TSR.SQ_PCNTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `PCNTCDGO` | **PK**, SEQUENCE |
| `direccionPersona` | `DireccionPersona` | `PDRCCDGO` | FK → `DireccionPersona` (PDRCCDGO), ManyToOne |
| `rubroTipoTelefonoP` | `Long` | `PCNTRYYA` |  |
| `rubroTipoTelefonoH` | `Long` | `PCNTRZZA` |  |
| `telefono` | `String` | `PCNTTLFN` | length=15 |
| `principal` | `Long` | `PCNTPRNC` |  |
| `nombreContacto` | `String` | `PCNTCNTC` | length=100 |
| `rubroPrefijoTelefonoP` | `Long` | `PCNTRYYB` |  |
| `rubroPrefijoTelefonoH` | `Long` | `PCNTRZZB` |  |

### `TempCobro` → tabla **`TSR.TCBR`**

Secuencia PK: `TSR.SQ_TCBRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCBRCDGO` | **PK**, SEQUENCE |
| `tipoId` | `Long` | `TCBRTPID` |  |
| `numeroId` | `String` | `TCBRIDNT` | length=20 |
| `cliente` | `String` | `TCBRCLNT` | length=200 |
| `descripcion` | `String` | `TCBRDSCR` | length=300 |
| `fecha` | `LocalDateTime` | `TCBRFCHA` |  |
| `nombreUsuario` | `String` | `TCBRUSRO` | length=50 |
| `valor` | `Double` | `TCBRVLRR` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `usuarioPorCaja` | `UsuarioPorCaja` | `USXCCDGO` | FK → `UsuarioPorCaja` (USXCCDGO), ManyToOne |
| `cierreCaja` | `CierreCaja` | `CRCJCDGO` | FK → `CierreCaja` (CRCJCDGO), ManyToOne |
| `fechaInactivo` | `LocalDateTime` | `TCBRFCDS` |  |
| `rubroMotivoAnulacionP` | `Long` | `TCBRRYYB` |  |
| `rubroMotivoAnulacionH` | `Long` | `TCBRRZZB` |  |
| `rubroEstadoP` | `Long` | `TCBRRYYA` |  |
| `rubroEstadoH` | `Long` | `TCBRRZZA` |  |
| `cajaLogica` | `CajaLogica` | `CJCNCDGO` | FK → `CajaLogica` (CJCNCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `tipoCobro` | `Long` | `TCBRTPCB` |  |

### `TempCobroCheque` → tabla **`TSR.TCCH`**

Secuencia PK: `TSR.SQ_TCCHCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCCHCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `numero` | `Long` | `TCCHNMRO` |  |
| `valor` | `Double` | `TCCHVLRR` |  |

### `TempCobroEfectivo` → tabla **`TSR.TCEF`**

Secuencia PK: `TSR.SQ_TCEFCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCEFCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `valor` | `Double` | `TCEFVLRR` |  |

### `TempCobroRetencion` → tabla **`TSR.TCRT`**

Secuencia PK: `TSR.SQ_TCRTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCRTCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `plantilla` | `Plantilla` | `PLNSCDGO` | FK → `Plantilla` (PLNSCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `valor` | `Double` | `TCRTVLRR` |  |
| `numero` | `String` | `TCRTNMRO` | length=50 |

### `TempCobroTarjeta` → tabla **`TSR.TCTJ`**

Secuencia PK: `TSR.SQ_TCTJCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCTJCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `numero` | `Long` | `TCTJNMRO` |  |
| `valor` | `Double` | `TCTJVLRR` |  |
| `numeroVoucher` | `Long` | `TCTJVCHR` |  |
| `fechaCaducidad` | `LocalDateTime` | `TCTJFCDC` |  |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |

### `TempCobroTransferencia` → tabla **`TSR.TCTR`**

Secuencia PK: `TSR.SQ_TCTRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCTRCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `bancoExterno` | `BancoExterno` | `BEXTCDGO` | FK → `BancoExterno` (BEXTCDGO), ManyToOne |
| `cuentaOrigen` | `String` | `TCTRCTOR` | length=50 |
| `numeroTransferencia` | `Long` | `TCTRNMRO` |  |
| `banco` | `Banco` | `BNCOCDGO` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancaria` | `CuentaBancaria` | `CNBCCDGO` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `cuentaDestino` | `String` | `TCTRCTDS` | length=50 |
| `valor` | `Double` | `TCTRVLRR` |  |

### `TempDebitoCredito` → tabla **`TSR.TDBC`**

Secuencia PK: `TSR.SQ_TDBCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TDBCCDGO` | **PK**, SEQUENCE |
| `tipo` | `Long` | `TDBCTPOO` |  |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `descripcion` | `String` | `TDBCDSCR` | length=500 |
| `valor` | `Double` | `TDBCVLRR` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |

### `TempMotivoCobro` → tabla **`TSR.TCMT`**

Secuencia PK: `TSR.SQ_TCMTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TCMTCDGO` | **PK**, SEQUENCE |
| `tempCobro` | `TempCobro` | `TCBRCDGO` | FK → `TempCobro` (TCBRCDGO), ManyToOne |
| `descripcion` | `String` | `TCMTDSCR` | length=200 |
| `valor` | `Double` | `TCMTVLRR` |  |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |

### `TempMotivoPago` → tabla **`TSR.TPMT`**

Secuencia PK: `TSR.SQ_TPMTCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPMTCDGO` | **PK**, SEQUENCE |
| `tempPago` | `TempPago` | `TPGSCDGO` | FK → `TempPago` (TPGSCDGO), ManyToOne |
| `plantilla` | `Plantilla` | `PLNSCDGO` | FK → `Plantilla` (PLNSCDGO), ManyToOne |
| `detallePlantilla` | `DetallePlantilla` | `DTPLCDGO` | FK → `DetallePlantilla` (DTPLCDGO), ManyToOne |
| `descripcion` | `String` | `TPMTDSCR` | length=200 |
| `valor` | `Double` | `TPMTVLRR` |  |

### `TempPago` → tabla **`TSR.TPGS`**

Secuencia PK: `TSR.SQ_TPGSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TPGSCDGO` | **PK**, SEQUENCE |
| `tipoId` | `Long` | `TPGSTPID` |  |
| `numeroId` | `String` | `TPGSIDNT` | length=20 |
| `proveedor` | `String` | `TPGSPRVD` | length=200 |
| `descripcion` | `String` | `TGSSDSCR` | length=300 |
| `fechaPago` | `LocalDateTime` | `TPGSFCHA` |  |
| `nombreUsuario` | `String` | `TPGSUSRO` | length=50 |
| `valor` | `Double` | `TPGSVLRR` |  |
| `empresa` | `Empresa` | `PJRQCDGO` | FK → `Empresa` (PJRQCDGO), ManyToOne |
| `fechaInactivo` | `LocalDateTime` | `TPGSFCDS` |  |
| `rubroMotivoAnulacionP` | `Long` | `TPGSRYYB` |  |
| `rubroMotivoAnulacionH` | `Long` | `TPGSRZZB` |  |
| `rubroEstadoP` | `Long` | `TPGSRYYA` |  |
| `rubroEstadoH` | `Long` | `TPGSRZZA` |  |
| `cheque` | `Cheque` | `DTCHCDGO` | FK → `Cheque` (DTCHCDGO), ManyToOne |
| `titular` | `Titular` | `TTLRCDGO` | FK → `Titular` (TTLRCDGO), ManyToOne |
| `asiento` | `Asiento` | `ASNTCDGO` | FK → `Asiento` (ASNTCDGO), ManyToOne |
| `numeroAsiento` | `Long` | `PGSSNMAS` |  |
| `tipoPago` | `Long` | `TPGSTPPG` |  |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |

### `Titular` → tabla **`TSR.TTLR`**

Secuencia PK: `TSR.SQ_TTLRCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TTLRCDGO` | **PK**, SEQUENCE |
| `identificacion` | `String` | `TTLRIDNT` | length=20 |
| `nombre` | `String` | `TTLRNMBR` | length=2000 |
| `apellido` | `String` | `TTLRAPLL` | length=50 |
| `razonSocial` | `String` | `TTLRRZSC` | length=2000 |
| `tipoCliente` | `Long` | `TTLRCLNT` |  |
| `tipoProveedor` | `Long` | `TTLRPRVD` |  |
| `rubroTipoPersonaP` | `Long` | `TTLRRYYA` |  |
| `rubroTipoPersonaH` | `Long` | `TTLRRZZA` |  |
| `rubroTipoIdentificacionP` | `Long` | `TTLRRYYB` |  |
| `rubroTipoIdentificacionH` | `Long` | `TTLRRZZB` |  |
| `estado` | `Long` | `TTLRESTD` |  |
| `tipoBeneficiario` | `Long` | `TTLRBNFC` |  |
| `tipoEmpleado` | `Long` | `TTLREMPL` |  |
| `aplicaIVA` | `Long` | `TTLRAPIV` |  |
| `aplicaRetencion` | `Long` | `TTLRAPRT` |  |
| `tipoSocio` | `Long` | `TTLRSCOO` |  |
| `telefono` | `String` | `TTLRTLFN` | length=500 |
| `email` | `String` | `TTLRMLLL` | length=500 |
| `direccion` | `String` | `TTLRDRCC` | length=2000 |
| `extranjero` | `Long` | `TTLREXTR` |  |
| `pais` | `Pais` | `PSSSCDGO` | FK → `Pais` (PSSSCDGO), ManyToOne |

### `Transferencia` → tabla **`TSR.TRNS`**

Secuencia PK: `TSR.SQ_TRNSCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `TRNSCDGO` | **PK**, SEQUENCE |
| `fecha` | `LocalDateTime` | `TRNSFCHA` |  |
| `tipo` | `Long` | `TRNSTPOO` |  |
| `bancoOrigen` | `Banco` | `BNCOORGN` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancariaOrigen` | `CuentaBancaria` | `CNBCORGN` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `numeroCuentaOrigen` | `String` | `CNTAORGN` | length=50 |
| `bancoDestino` | `Banco` | `BNCODSTN` | FK → `Banco` (BNCOCDGO), ManyToOne |
| `cuentaBancariaDestino` | `CuentaBancaria` | `CNBCDSTN` | FK → `CuentaBancaria` (CNBCCDGO), ManyToOne |
| `numeroCuentaDestino` | `String` | `CNTADSTN` | length=50 |
| `valor` | `Double` | `TRNSVLRR` |  |
| `nombreUsuario` | `String` | `TRNSUSRO` | length=50 |
| `observacion` | `String` | `TRNSOBSR` | length=300 |
| `estado` | `Long` | `TRNSESTD` |  |

### `UsuarioPorCaja` → tabla **`TSR.USXC`**

Secuencia PK: `TSR.SQ_USXCCDGO`

| Campo Java | Tipo | Columna | Notas |
|---|---|---|---|
| `codigo` | `Long` | `USXCCDGO` | **PK**, SEQUENCE |
| `cajaFisica` | `CajaFisica` | `CJAACDGO` | FK → `CajaFisica` (CJAACDGO), ManyToOne |
| `nombre` | `String` | `USXCNMBR` | length=500 |
| `usuario` | `Usuario` | `PJRQCDUS` | FK → `Usuario` (PJRQCDGO), ManyToOne |
| `fechaIngreso` | `LocalDateTime` | `USXCFCIN` |  |
| `fechaInactivo` | `LocalDateTime` | `USXCFCDS` |  |
| `estado` | `Long` | `USXCESTD` |  |

