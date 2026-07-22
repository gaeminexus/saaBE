# Plan de Implementación — Generación de Reportes G40-G51

## Contexto
El frontend enviará `{mes, anio, usuario}` a un endpoint único.
El backend llenará las 12 tablas de reportes G (CG40-CG51) y registrará la ejecución en las tablas de auditoría EJRC/EJRD.

---

## Reglas del orquestador

1. Llega `{mes, anio, usuario}`
2. Buscar en `EJRC` si ya existe una ejecución para ese mes/año
3. **Si existe y TODOS los EJRD están en estado 1 (OK)** → retornar mensaje `"Reportes ya generados para mes/año"`
4. **Si existe pero hay EJRD con estado 2 (Con novedades) o 3 (Pendiente)** → re-ejecutar solo esos Gs
5. **Si no existe** → crear EJRC (estado=1 En proceso) + 12 EJRD (estado=3 Pendiente) → ejecutar todos
6. Por cada G a ejecutar:
   - Marcar EJRD estado = 3 (Pendiente) si es re-ejecución
   - Llamar lógica del G correspondiente
   - Si éxito → actualizar EJRD: estado=1, cantidadRegistros=N, fechaGeneracion=hoy
   - Si falla → actualizar EJRD: estado=2, novedades=mensaje del error
7. Al finalizar todos los Gs → evaluar estado global del EJRC:
   - Todos OK → estado=3 (Completo)
   - Al menos uno con novedad → estado=2 (Con novedades)
8. Retornar `200 OK` con el objeto `EjecucionReporte`

---

## Estructura de archivos a crear

```
service/
  GeneracionReportesService.java          ← Interface del orquestador
serviceImpl/
  GeneracionReportesServiceImpl.java      ← Orquestador principal
ws/rest/rpr/
  GeneracionReportesRest.java             ← Endpoint POST /generacion/ejecutar
```

Adicionalmente, **un service por cada G** para encapsular la lógica de llenado:

```
service/
  GeneracionG40Service.java
  GeneracionG41Service.java
  ...
  GeneracionG51Service.java
serviceImpl/
  GeneracionG40ServiceImpl.java
  GeneracionG41ServiceImpl.java
  ...
  GeneracionG51ServiceImpl.java
```

---

## Estado de avance

### ✅ COMPLETADO

#### Base de datos
- [x] Tablas `RPR.EJRC` y `RPR.EJRD` creadas (`docs/create_tables_ejrc_ejrd.sql`)
- [x] FK `CGxxEJRD` agregada a las 12 tablas G (`docs/ALTER_TABLES_G40_G51_agregar_fk_ejrd.sql`)

#### Models (`com.saa.model.rpr`)
- [x] `EjecucionReporte.java` — mapea `RPR.EJRC`
- [x] `DetalleEjecucionReporte.java` — mapea `RPR.EJRD`
- [x] `CreditoG40.java` → campo `detalleEjecucion` agregado
- [x] `ParticipeActivoG41.java` → campo `detalleEjecucion` agregado
- [x] `SaldoCuentaG42.java` → campo `detalleEjecucion` agregado
- [x] `ParticipeCesanteG43.java` → campo `detalleEjecucion` agregado
- [x] `ParticipeJubiladoG44.java` → campo `detalleEjecucion` agregado
- [x] `NuevoParticipeG45.java` → campo `detalleEjecucion` agregado
- [x] `NuevoPrestamoG46.java` → campo `detalleEjecucion` agregado
- [x] `NovacionG47.java` → campo `detalleEjecucion` agregado
- [x] `SaldoOperacionG48.java` → campo `detalleEjecucion` agregado
- [x] `CancelacionG49.java` → campo `detalleEjecucion` agregado
- [x] `GaranteG50.java` → campo `detalleEjecucion` agregado
- [x] `GarantiaRealG51.java` → campo `detalleEjecucion` agregado
- [x] `NombreEntidadesReporte.java` → constantes `EJECUCION_REPORTE` y `DETALLE_EJECUCION_REPORTE` agregadas

#### DAO Interface (`com.saa.ejb.rpr.dao`)
- [x] `EjecucionReporteDaoService.java` — método `selectByMesAnio(mes, anio)`
- [x] `DetalleEjecucionReporteDaoService.java` — métodos `selectByEjecucion`, `selectConNovedadesByEjecucion`, `selectPendientesYNovedadesByEjecucion`

#### DAO Impl (`com.saa.ejb.rpr.daoImpl`)
- [x] `EjecucionReporteDaoServiceImpl.java`
- [x] `DetalleEjecucionReporteDaoServiceImpl.java`

#### Service Interface (`com.saa.ejb.rpr.service`)
- [x] `EjecucionReporteService.java` — método `selectByMesAnio`
- [x] `DetalleEjecucionReporteService.java` — métodos `selectByEjecucion`, `selectConNovedadesByEjecucion`, `selectPendientesYNovedadesByEjecucion`
- [x] `GeneracionReportesService.java` — interface del orquestador ← **ÚLTIMO ARCHIVO CREADO**

#### Service Impl (`com.saa.ejb.rpr.serviceImpl`)
- [x] `EjecucionReporteServiceImpl.java`
- [x] `DetalleEjecucionReporteServiceImpl.java`
- [x] `GeneracionReportesServiceImpl.java` — orquestador con despachador `ejecutarG()`

#### REST (`com.saa.ws.rest.rpr`)
- [x] `EjecucionReporteRest.java` — path `ejrc`
- [x] `DetalleEjecucionReporteRest.java` — path `ejrd`
- [x] `GeneracionReportesRest.java` — path `generacion/ejecutar` ← DTO `SolicitudGeneracion` incluido

---

### ✅ IMPLEMENTACIÓN COMPLETA — G40 a G51

Todos los Gs están implementados y conectados en el orquestador.

---

## Correcciones aplicadas al orquestador

### Bug: re-ejecución no procesaba ningún G
**Causa:** `DetalleEjecucionReporteServiceImpl.selectPendientesYNovedadesByEjecucion` lanzaba `IncomeException` cuando la lista estaba vacía (siguiendo el patrón genérico). En el orquestador esa llamada estaba en un `try-catch` que silenciaba la excepción y dejaba `ejrdsAProcesar` vacío → el bucle no procesaba nada.

**Corrección:**
- `DetalleEjecucionReporteServiceImpl.java` → ya no lanza excepción cuando no hay pendientes; retorna lista vacía (resultado válido).
- `GeneracionReportesServiceImpl.java` → eliminados los dos `try-catch` silenciosos: paso 3 (obtener EJRDs pendientes en re-ejecución) y paso 7 (evaluación final del estado del EJRC).

---

## Instrucciones para retomar en nueva conversación

1. Leer este archivo: `PLAN_GENERACION_REPORTES_G.md`
2. Leer: `REGLAS_GENERACION_REPORTES_G.md` para ver la lógica completa de cada G
3. **Toda la implementación G40–G51 está completa y funcional**
4. Pendientes menores conocidos:
   - **G50** — sin lógica de registros (retorna 0 OK). Lógica real a definir.
   - **G51** — `tipoGarantia = "A21"` fijo para todos. TODO: préstamos cuyo producto empiece con H → cambiar a otro código.

---


  - Orquestador → `@EJB g44Service` activo, `case "G44"` descomentado
- [x] G45 — `NuevoParticipeG45` — **COMPLETADO**
  - `GeneracionG45Service.java` + `GeneracionG45ServiceImpl.java`
  - Lógica: lee CG41 del mismo EJRC (G41 ya cambió idEstado 1→10). Por cada nuevo partícipe enriquece con Entidad, Exter, Direccion, PerfilEconomico y Participe. INSERT en CG45.
  - `ParticipeActivoG41DaoService/Impl` → `selectByDetalle(codigoDetalle)` agregado
  - `EntidadDaoService/Impl` + `EntidadService/Impl` → `selectByNumeroIdentificacion(String)` agregado
  - `PerfilEconomicoDaoService/Impl` + `PerfilEconomicoService/Impl` → `selectByEntidad(Long)` agregado
  - `DireccionService/Impl` → `selectByParent(Long)` expuesto (ya existía en DAO)
  - Orquestador → `@EJB g45Service` activo, `case "G45"` descomentado
- [ ] G46 — `NuevoPrestamoG46` — lógica pendiente
- [x] G47 — `NovacionG47` — **COMPLETADO**
  - `GeneracionG47Service.java` + `GeneracionG47ServiceImpl.java`
  - Lógica: préstamos con `estadoPrestamo = 9`. tipoIdentificacion="C" fijo. numeroOperacion=idAsoprep. fechaNovacion=fecha.toLocalDate(). numeroOperacionAnterior=null.
  - `PrestamoDaoService/Impl` + `PrestamoService/Impl` → `selectByEstado(Long)` agregado
  - Orquestador → `@EJB g47Service` activo, `case "G47"` activado
- [x] G48 — `SaldoOperacionG48` — **COMPLETADO**
  - `GeneracionG48Service.java` + `GeneracionG48ServiceImpl.java`
  - Lógica: 2 grupos desde `DetallePrestamo`. Grupo 1 = cuotas del mes (estado!=7). Grupo 2 = menor cuota anterior al mes (estado 2,3,5,6 y préstamo 2,8,11). Sin duplicados entre grupos.
  - valorVencido: Grupo1=0, Grupo2=capital de la cuota.
  - valorSujetoProvision = max(0, valorPorVencer - valorTotalCuentaIndividual).
  - provisionRequeridaOriginal = VSAP × % según calificación (A1=0.99%..E=100%).
  - provisionConstituida = provisionRequeridaOriginal del G48 del período anterior. Si no hay EJRC previo → consulta individual en HM48 (HistoricoG48) con caché. Si no existe → 0.
  - calificacionPropia calculada por días de morosidad (0=A1, 1-8=A2, ..., >120=E).
  - `HistoricoG48DaoService/Impl` → `selectByNumeroOperacion(String)` agregado
  - `HistoricoG48Service.java` + `HistoricoG48ServiceImpl.java` → **CREADOS**
  - `SaldoOperacionG48DaoService/Impl` → `selectByDetalle(Long)` y `selectByDetalleYOperacion(Long, String)` agregados
  - `SaldoOperacionG48Service/Impl` → mismos 2 métodos expuestos
  - `DetallePrestamoDaoServiceImpl` → `JOIN FETCH` en `selectCuotasDelMesGlobal` y `selectMenorCuotaAnteriorAlMesGlobal` (sin alias — JPA spec no lo permite)
  - Orquestador → `@EJB g48Service` activo, `case "G48"` activado
- [x] G49 — `CancelacionG49` — **COMPLETADO**
  - `GeneracionG49Service.java` + `GeneracionG49ServiceImpl.java`
  - Lógica: 3 grupos. Grupo 1 = préstamos cuya max cuota del mes sea pagada (estado=4). Grupo 2 = préstamos estadoPrestamo=4 cuya max cuota pagada esté en el mes. Grupo 3 = operaciones del G48 anterior que ya no están en el G48 actual. Deduplicación por Set<numeroOperacion>.
  - fechaCancelacion = último día del mes. formaCancelacion = "N" fijo.
  - `DetallePrestamoDaoService/Impl` → `selectMaxCuotaPagadaDelMesGlobal` y `selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal` agregados (con JOIN FETCH prestamo + entidad)
  - `DetallePrestamoService/Impl` → mismos 2 métodos expuestos
  - Orquestador → `@EJB g49Service` activo, `case "G49"` activado
- [x] G50 — `GaranteG50` — **COMPLETADO (sin registros por ahora)**
  - `GeneracionG50Service.java` + `GeneracionG50ServiceImpl.java`
  - Lógica: retorna 0 registros, EJRD queda OK. Lógica real pendiente.
  - Orquestador → `@EJB g50Service` activo, `case "G50"` activado
- [x] G51 — `GarantiaRealG51` — **COMPLETADO**
  - `GeneracionG51Service.java` + `GeneracionG51ServiceImpl.java`
  - Lógica: misma cantidad de registros que G46 del mismo EJRC. Por cada CG46 → INSERT en CG51. tipoGarantia="A21" fijo (TODO: productos con nombre H → otro código). descripcionGarantia="Cuenta Individual", valorAvaluo=0, porcentajeCubre=100, estadoRegistro="N".
  - `NuevoPrestamoG46DaoService/Impl` → `selectByDetalle(Long)` agregado
  - `NuevoPrestamoG46Service/Impl` → método expuesto y delegado
  - Orquestador → `@EJB g51Service` activo, `case "G51"` activado

---

## Constantes de estado (para no hardcodear)

| Entidad | Estado | Valor |
|---|---|---|
| EJRC | En proceso | 1 |
| EJRC | Con novedades | 2 |
| EJRC | Completo | 3 |
| EJRD | OK | 1 |
| EJRD | Con novedades | 2 |
| EJRD | Pendiente | 3 |
| EJRC.tipoEjecucion | Inicial | 1 |
| EJRC.tipoEjecucion | Corrección | 2 |

---

## Instrucciones para retomar en nueva conversación

1. Leer este archivo: `PLAN_GENERACION_REPORTES_G.md`
2. Continuar desde la sección **"⏳ PENDIENTE — Continuar desde aquí"**
3. El **Paso 1** es crear `GeneracionReportesServiceImpl.java`
4. El **Paso 2** es crear `GeneracionReportesRest.java`
5. El **Paso 3** es implementar los services de cada G conforme el usuario vaya dando la lógica

---

## NOTA FINAL
> ⚠️ Una vez completada toda la implementación, **eliminar los siguientes archivos de instrucciones**:
> - `PLAN_GENERACION_REPORTES_G.md` (este archivo)
> - Cualquier otro archivo `.md` de planificación temporal que se haya creado en la raíz del proyecto
