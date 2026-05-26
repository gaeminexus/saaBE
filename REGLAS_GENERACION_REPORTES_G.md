# Reglas de Generación de Reportes G40-G51

## Contexto General
- Endpoint: `POST http://{servidor}/saaBE/rest/ejrc/ejecutar`
- Body: `{ "mes": 5, "anio": 2026, "usuario": "jperez" }`
- El orquestador está en: `GeneracionReportesServiceImpl.java`
- Si un G ya está OK no se reprocesa. Si falla queda en estado 2 (Con novedades) y se puede volver a llamar.
- Cada registro insertado en las tablas G lleva el campo `detalleEjecucion` (FK a EJRD).

---

## Estados importantes

### EJRC (Cabecera ejecución)
| Valor | Significado |
|---|---|
| 1 | En proceso |
| 2 | Con novedades |
| 3 | Completo |

### EJRD (Detalle por G)
| Valor | Significado |
|---|---|
| 1 | OK |
| 2 | Con novedades / Error |
| 3 | Pendiente |

### Entidad (ENTD.ENTDIDST)
| Valor | Significado en G41 |
|---|---|
| 1 | Nuevo → incluir en G41 |
| 10 | Procesado → ya fue incluido en G41 |

---

## ✅ G40 — CreditoG40 — COMPLETADO

**Tabla destino:** `RPR.CG40`  
**Fuente:** `CRD.IGFN` (InformacionGeneralFondo) — siempre tiene UN solo registro

**Regla:**
- Si `IGFN.IGFNESTD = 2` (Modificado) → copiar todos los campos de IGFN a CG40, luego resetear `IGFNESTD = 1`
- Si `IGFN.IGFNESTD = 1` (Sin cambios) → G40 vacío, retornar 0 registros, EJRD queda OK

**Cuándo se marca IGFN como modificado:**
- El `saveSingle` de `InformacionGeneralFondoServiceImpl` setea automáticamente `estado=2` y `fechaModificacion=hoy` cuando se actualiza un registro existente (codigo != null)

**Campos fijos:** Ninguno. Todos vienen de IGFN.

**Archivos creados/modificados:**
- `docs/ALTER_TABLE_IGFN_agregar_auditoria.sql` → agrega `IGFNESTD`, `IGFNUSRM`, `IGFNFCMD`
- `InformacionGeneralFondo.java` → campos `estado`, `usuarioModificacion`, `fechaModificacion`
- `InformacionGeneralFondoDaoService/Impl` → método `selectModificados()`
- `InformacionGeneralFondoService/Impl` → método `selectModificados()`
- `GeneracionG40Service.java` + `GeneracionG40ServiceImpl.java`

---

## ✅ G41 — ParticipeActivoG41 — COMPLETADO

**Tabla destino:** `RPR.CG41`  
**Fuente principal:** `CRD.ENTD` (Entidad) con `idEstado = 1`  
**Fuente secundaria 1:** `CRD.PRTC` (Participe) — join por `ENTD.ENTDCDGO = PRTC.ENTDCDGO`  
**Fuente secundaria 2:** `CRD.EXTER` (Exter) — join por `ENTD.ENTDNMID = EXTER.cedula`

**Regla:**
- Si no hay entidades con `idEstado = 1` → G41 vacío, retornar 0, EJRD queda OK
- Por cada entidad con `idEstado = 1`:
  - Buscar su `Participe` (por `entidad.codigo`)
  - Buscar su `Exter` (por `entidad.numeroIdentificacion = exter.cedula`)
  - Mapear campos a `CG41`
  - Actualizar `entidad.idEstado = 10`

**Mapeo de campos:**
| Campo CG41 | Fuente | Valor |
|---|---|---|
| `tipoIdentificacion` | `Entidad.tipoIdentificacion.nombre` | dinámico |
| `identificacion` | `Entidad.numeroIdentificacion` | dinámico |
| `genero` | `Exter.genero` | dinámico |
| `estadoCivil` | `Exter.estadoCivil` | dinámico |
| `fechaNacimiento` | `Exter.fechaNacimiento.toLocalDate()` | dinámico |
| `fechaIngreso` | `Participe.fechaIngresoFondo.toLocalDate()` | dinámico |
| `estadoParticipe` | — | **FIJO: `"A"`** |
| `tipoSistema` | — | **FIJO: `"I"`** |
| `baseCalculoAportacion` | — | **FIJO: `"S"`** |
| `tipoRelacionLaboral` | — | **FIJO: `"N"`** |
| `estadoRegistro` | — | **FIJO: `"A"`** |

**Archivos creados/modificados:**
- `EntidadDaoService/Impl` → `selectByIdEstado(Long idEstado)`
- `EntidadService/Impl` → `selectByIdEstado(Long idEstado)`
- `ExterDaoService/Impl` → `selectByCedula(String cedula)`
- `ExterService/Impl` → `selectByCedula(String cedula)`
- `ParticipeDaoService/Impl` → `selectByEntidad(Long codigoEntidad)`
- `ParticipeService/Impl` → `selectByEntidad(Long codigoEntidad)`
- `GeneracionG41Service.java` + `GeneracionG41ServiceImpl.java`

---

## ✅ G42 — SaldoCuentaG42 — COMPLETADO

**Tabla destino:** `RPR.CG42`  
**Fuente 1:** `CRD.APRT` (Aporte) — join con `CRD.TPAP` (TipoAporte)  
**Fuente 2:** `CRD.HSTR` (HistorialSueldo) — solo registros con estado = 99

**Regla general:** Solo se consideran TipoAportes con `estado = 1`. Todas las entidades que tengan suma > 0 en cualquiera de los grupos aparecen en G42, independientemente del estado de la entidad.

**3 grupos de aportes (todos con SUM en BD, agrupados por entidad):**

| Grupo | Condición | Campo destino |
|---|---|---|
| 1 | `tipoAporte.estado=1` y `tipoAporte.codigoSBS='RE'` | `rendimiento` |
| 2 | `tipoAporte.estado=1` y `tipoAporte.codigo IN (3,13,14)` | `saldoAportePatronal` |
| 3 | `tipoAporte.estado=1` excluyendo grupos 1 y 2 | `saldoAportePersonal` |

**Aporte Personal:**
- Fuente: `CRD.HSTR` con `estado = 99`
- Valor: `SUM(montoCesantia + montoJubilacion)` agrupado por entidad
- Campo destino: `aportePersonal`

**Lógica de persistencia:**
- Los 4 SELECTs con SUM se ejecutan en BD (no en memoria)
- Se consolida un mapa `<codigoEntidad, SaldoCuentaG42>` con los 4 grupos
- Por cada entidad: buscar en `CG42` por `entidad + detalleEjecucion`
  - Si existe → UPDATE de los campos con valor
  - Si no existe → INSERT con datos de identificación de `Entidad`

**Cambios de BD:**
- `docs/ALTER_TABLE_CG42_agregar_entidad.sql` → agrega `CG42ENTD` (FK a `CRD.ENTD`) + índice `IDX_CG42_ENTD_EJRD`

**Archivos creados/modificados:**
- `SaldoCuentaG42.java` → campo `entidad` (FK a ENTD) agregado
- `SaldoCuentaG42DaoService` → método `selectByEntidadYDetalle(codigoEntidad, detalle)` agregado
- `SaldoCuentaG42DaoServiceImpl` → implementación del método anterior
- `AporteDaoService/Impl` → métodos `selectSumaRendimientoPorEntidad()`, `selectSumaPatronalPorEntidad()`, `selectSumaPersonalPorEntidad()` agregados
- `AporteService/Impl` → mismos 3 métodos expuestos
- `HistorialSueldoDaoService/Impl` → método `selectSumaAportePersonalPorEntidad()` agregado
- `HistorialSueldoService/Impl` → método expuesto
- `GeneracionG42Service.java` + `GeneracionG42ServiceImpl.java` → creados
- Orquestador → `@EJB g42Service` activo, `case "G42"` descomentado

---

## ✅ G43 — ParticipeCesanteG43 — COMPLETADO

**Tabla destino:** `RPR.CG43`  
**Fuente principal:** `RPR.CG42` del mes anterior (o `RPR.HM42` si es primera ejecución)  
**Fuente de comparación:** `RPR.CG42` del mes actual

**Regla:**
- Se compara el universo de entidades que tenían saldo en **G42 del mes anterior** contra las que tienen saldo en el **G42 del mes actual**.
- Toda entidad que **estaba en el mes anterior** pero **ya no aparece en el mes actual** → es un partícipe cesante → se inserta en CG43.
- **Si no existe EJRC del mes anterior** (primera ejecución histórica) → se usa `RPR.HM42` (HistoricoG42) como universo anterior en lugar del CG42 previo.
- Los campos de liquidación (`fechaTerminoRelacionLaboral`, `fechaLiquidacion`, `saldoCuentaIndividual`, `valoresCompensados`, `valoresPagados`, `numeroImposicionesPersonales`, `numeroImposicionesPatronales`) quedan en `null` — provienen de procesos operativos de liquidación externos a esta generación.

**Lógica paso a paso:**
1. Obtener el EJRC actual + calcular `mesPrevio/anioPrevio`.
2. Obtener los registros CG42 del EJRD G42 del EJRC actual → `Set<identificacion>` como "entidades activas".
3. Buscar EJRC del mes anterior con `selectByMesAnio(mesPrevio, anioPrevio)`:
   - Si existe → obtener EJRD G42 de ese EJRC → `selectByDetalle(codigoEJRD)` → universo anterior.
   - Si NO existe → `HistoricoG42DaoService.selectAll("HistoricoG42All")` → universo anterior.
4. `cesantes = universoPrevio - entidadesActuales`
5. Por cada cesante → INSERT en CG43 con `identificacion`, `tipoIdentificacion` y `detalleEjecucion`.

**Archivos creados/modificados:**
- `SaldoCuentaG42DaoService.java` → método `selectByDetalle(Long codigoDetalle)` agregado
- `SaldoCuentaG42DaoServiceImpl.java` → implementación del método anterior
- `SaldoCuentaG42Service.java` → método `selectByDetalle` expuesto
- `SaldoCuentaG42ServiceImpl.java` → implementación delegada
- `DetalleEjecucionReporteDaoService.java` → método `selectByEjecucionYTipo(Long, String)` agregado
- `DetalleEjecucionReporteDaoServiceImpl.java` → implementación del método anterior
- `DetalleEjecucionReporteService.java` → método `selectByEjecucionYTipo` expuesto
- `DetalleEjecucionReporteServiceImpl.java` → implementación delegada
- `GeneracionG43Service.java` → interface creada
- `GeneracionG43ServiceImpl.java` → implementación completa con la lógica descrita
- `GeneracionReportesServiceImpl.java` → `@EJB g43Service` agregado + `case "G43"` activado

---

## ✅ G44 — ParticipeJubiladoG44 — COMPLETADO

**Tabla destino:** `RPR.CG44`  
**Fuente principal:** `CRD.ENTD` con `idEstado = 30` (jubilados)  
**Fuente imposiciones:** `CRD.APRT` con `tipoAporte.codigo IN (9, 11)` — COUNT agrupado por entidad  
**Fuente valorPension / valorNetoRecibir:** `CRD.VPPC` (ValorPagoPensionComplementaria) — campo `valorPagar`, buscado por entidad  
**Fuente saldoCuenta:** `CRD.APRT` con `tipoAporte.codigo = 23` — SUM del campo `valor`, agrupado por entidad  

**Regla:**
- Se obtienen todas las entidades con `idEstado = 30`.
- Si no hay ninguna → G44 vacío, OK.
- Por cada entidad jubilada:
  - `imposicionesAcumuladas` = COUNT de aportes con `tipoAporte.codigo IN (9, 11)`
  - `valorPension` = `valorPagar` del primer registro de `VPPC` para esa entidad (null si no existe)
  - `valorNetoRecibir` = mismo valor que `valorPension` (mismo origen: VPPC)
  - `saldoCuenta` = SUM de `aportes.valor` con `tipoAporte.codigo = 23` (0 si no hay)
  - `jubilacionIess` = **FIJO: `"S"`**
  - Los campos `tipoJubilacion`, `fechaJubilacion`, `valoresCompensados` quedan en `null`

**Optimización:** Los SELECTs de COUNT e imposiciones y SUM de saldo se ejecutan una sola vez en BD agrupados por entidad (no hay queries individuales por jubilado). Solo `selectByEntidad` de VPPC se ejecuta por entidad.

**Archivos creados/modificados:**
- `AporteDaoService.java` → métodos `selectCountImposicionesJubilacionPorEntidad()` y `selectSumaSaldoCuentaJubilacionPorEntidad()` agregados
- `AporteDaoServiceImpl.java` → implementaciones con JPQL `COUNT`/`SUM GROUP BY`
- `AporteService.java` → mismos 2 métodos expuestos
- `AporteServiceImpl.java` → implementaciones delegadas
- `ValorPagoPensionComplementariaDaoService.java` → método `selectByEntidad(Long)` agregado
- `ValorPagoPensionComplementariaDaoServiceImpl.java` → implementación completa con `EntityManager`, `@PersistenceContext` y `obtieneCampos()`
- `ValorPagoPensionComplementariaService.java` → método `selectByEntidad(Long)` expuesto
- `ValorPagoPensionComplementariaServiceImpl.java` → implementación delegada
- `GeneracionG44Service.java` → interface creada
- `GeneracionG44ServiceImpl.java` → implementación completa con la lógica descrita
- `GeneracionReportesServiceImpl.java` → `@EJB g44Service` agregado + `case "G44"` activado

---

## ✅ G45 — NuevoParticipeG45 — COMPLETADO

**Tabla destino:** `RPR.CG45`  
**Fuente principal:** `RPR.CG41` del mismo EJRC (los partícipes que G41 insertó en este ciclo)  
**Nota importante:** G41 corre antes que G45 y cambia `idEstado 1→10`, por eso G45 **no puede filtrar por `idEstado=1`** — lee directamente de `CG41` del mismo EJRC.

**Enriquecimiento por entidad:**

| Campo G45 | Fuente | Campo origen |
|---|---|---|
| `tipoIdentificacion` | `CG41` (viene de Entidad.tipoIdentificacion.nombre) | directo |
| `identificacion` | `CG41` | directo |
| `cargasFamiliares` | `Entidad` | `cargasFamiliares` |
| `genero` | `Exter` | `genero` |
| `estadoCivil` | `Exter` | `estadoCivil` |
| `fechaNacimiento` | `Exter` | `fechaNacimiento.toLocalDate()` |
| `profesion` | `Exter` | `profesion` |
| `provincia` | `Exter` | `provincia` |
| `canton` | `Exter` | `canton` |
| `parroquia` | `Direccion` (primera dirección) | `parroquia.nombre` |
| `patrimonio` | `PerfilEconomico` | `patrimonioNeto` |
| `origenIngresos` | `PerfilEconomico` | `origenOtrosIngresos` |
| `tipoParticipe` | `Participe` | `tipoParticipante.nombre` |
| `actividadEconomica` | `Participe` | `ingresoAdicionalActividad` |

**Regla:**
- Si no hay registros en CG41 del EJRC actual → G45 vacío, OK.
- Por cada registro de CG41 → buscar Entidad por `identificacion` (query en BD), luego enriquecer con Exter, Direccion, PerfilEconomico y Participe.
- Si alguna fuente no existe para la entidad → el campo queda `null` (no bloquea la generación).

**Archivos creados/modificados:**
- `ParticipeActivoG41DaoService.java` → método `selectByDetalle(Long codigoDetalle)` agregado
- `ParticipeActivoG41DaoServiceImpl.java` → implementación con query JPQL
- `EntidadDaoService.java` + `Impl` → método `selectByNumeroIdentificacion(String)` agregado
- `EntidadService.java` + `Impl` → mismo método expuesto
- `PerfilEconomicoDaoService.java` + `Impl` → método `selectByEntidad(Long)` agregado + clase completada con `EntityManager` y `obtieneCampos()`
- `PerfilEconomicoService.java` + `Impl` → mismo método expuesto
- `DireccionService.java` + `Impl` → método `selectByParent(Long)` expuesto (ya existía en el DAO)
- `GeneracionG45Service.java` → interface creada
- `GeneracionG45ServiceImpl.java` → implementación completa
- `GeneracionReportesServiceImpl.java` → `@EJB g45Service` agregado + `case "G45"` activado

---

## ⏳ G46 — NuevoPrestamoG46 — PENDIENTE

**Tabla destino:** `RPR.CG46`  
**Regla:** pendiente — el usuario la proporcionará

---

## ⏳ G47 — NovacionG47 — PENDIENTE

**Tabla destino:** `RPR.CG47`  
**Regla:** pendiente — el usuario la proporcionará

---

## ⏳ G48 — SaldoOperacionG48 — PENDIENTE

**Tabla destino:** `RPR.CG48`  
**Regla:** pendiente — el usuario la proporcionará

---

## ⏳ G49 — CancelacionG49 — PENDIENTE

**Tabla destino:** `RPR.CG49`  
**Regla:** pendiente — el usuario la proporcionará

---

## ⏳ G50 — GaranteG50 — PENDIENTE

**Tabla destino:** `RPR.CG50`  
**Regla:** pendiente — el usuario la proporcionará

---

## ⏳ G51 — GarantiaRealG51 — PENDIENTE

**Tabla destino:** `RPR.CG51`  
**Regla:** pendiente — el usuario la proporcionará

---

## Patrón estándar para implementar un G nuevo

Cada G nuevo sigue este patrón:

### 1. Crear interface
```
src/main/java/com/saa/ejb/rpr/service/GeneracionGxxService.java
→ método: long generar(DetalleEjecucionReporte detalle) throws Throwable;
```

### 2. Crear implementación
```
src/main/java/com/saa/ejb/rpr/serviceImpl/GeneracionGxxServiceImpl.java
→ @EJB de los services que necesite
→ lógica de consulta + mapeo + persistencia + actualización de estados fuente
```

### 3. Conectar en el orquestador
```
GeneracionReportesServiceImpl.java
→ Agregar: @EJB private GeneracionGxxService gxxService;
→ Descomentar: case "Gxx": return gxxService.generar(ejrd);
```

### 4. Actualizar este documento con las reglas del G implementado

---

## Instrucciones para retomar en nueva conversación

1. Leer: `REGLAS_GENERACION_REPORTES_G.md` (este archivo)
2. Leer: `PLAN_GENERACION_REPORTES_G.md` (estado técnico detallado)
3. El siguiente G a implementar es el **G42**
4. El usuario dará la lógica y se seguirá el patrón estándar descrito arriba

---

## NOTA FINAL
> ⚠️ Una vez completada toda la implementación, **eliminar los siguientes archivos**:
> - `REGLAS_GENERACION_REPORTES_G.md` (este archivo)
> - `PLAN_GENERACION_REPORTES_G.md`
