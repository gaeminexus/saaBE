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

### ⏳ PENDIENTE — Continuar desde aquí

#### Paso 1 — Crear `GeneracionReportesServiceImpl.java`
**Archivo:** `src/main/java/com/saa/ejb/rpr/serviceImpl/GeneracionReportesServiceImpl.java`

Lógica:
```
@Stateless
public class GeneracionReportesServiceImpl implements GeneracionReportesService {

  @EJB EjecucionReporteService ejrcService;
  @EJB DetalleEjecucionReporteService ejrdService;
  @EJB GeneracionG40Service g40Service;
  @EJB GeneracionG41Service g41Service;
  ... (un @EJB por cada G)

  ejecutarGeneracion(mes, anio, usuario):
    1. Buscar EJRC por mes/anio → ejrcService.selectByMesAnio(mes, anio)
    2. Si existe y todos EJRD OK → throw "ya generados"
    3. Si existe → obtener lista de EJRD pendientes/con novedades
    4. Si no existe → crear EJRC + crear 12 EJRD
    5. Para cada EJRD a ejecutar → llamar gXXService.generar(ejrd)
    6. Actualizar estado EJRC final
    7. return ejrc
}
```

#### Paso 2 — Crear `GeneracionReportesRest.java`
**Archivo:** `src/main/java/com/saa/ws/rest/rpr/GeneracionReportesRest.java`

```
@Path("generacion")
POST /generacion/ejecutar
Body: { "mes": 4, "anio": 2026, "usuario": "jperez" }
Response: 200 OK + EjecucionReporte
```

#### Paso 3 — Crear services de generación por G (uno a la vez, el usuario dará la lógica)

**Lista de Gs pendientes de lógica:**
- [x] G40 — `CreditoG40` — **COMPLETADO**
  - `GeneracionG40Service.java` + `GeneracionG40ServiceImpl.java`
  - Lógica: único registro IGFN, si estado=2 → copia a CG40 y resetea a 1, si estado=1 → 0 registros OK
  - `ALTER_TABLE_IGFN_agregar_auditoria.sql` — campos `IGFNESTD`, `IGFNUSRM`, `IGFNFCMD`
  - `InformacionGeneralFondo.java` — campos `estado`, `usuarioModificacion`, `fechaModificacion` agregados
  - `InformacionGeneralFondoDaoService/Impl` — método `selectModificados()` agregado
  - `InformacionGeneralFondoService/Impl` — método `selectModificados()` agregado
  - Orquestador `GeneracionReportesServiceImpl` — `@EJB g40Service` activo, `case "G40"` descomentado
- [x] G41 — `ParticipeActivoG41` — **COMPLETADO**
  - `GeneracionG41Service.java` + `GeneracionG41ServiceImpl.java`
  - Lógica: busca Entidad con idEstado=1, si no hay → 0 registros OK. Por cada entidad busca Participe y Exter (por cedula), mapea a CG41, actualiza entidad.idEstado=10
  - `EntidadDaoService/Impl` + `EntidadService/Impl` → método `selectByIdEstado(idEstado)` agregado
  - `ExterDaoService/Impl` + `ExterService/Impl` → método `selectByCedula(cedula)` agregado
  - `ParticipeDaoService/Impl` + `ParticipeService/Impl` → método `selectByEntidad(codigoEntidad)` agregado
  - Orquestador → `@EJB g41Service` activo, `case "G41"` descomentado
- [x] G42 — `SaldoCuentaG42` — **COMPLETADO**
  - `GeneracionG42Service.java` + `GeneracionG42ServiceImpl.java`
  - Lógica: 4 SELECTs con SUM en BD agrupados por entidad. Consolida en mapa y hace INSERT o UPDATE en CG42 por entidad+detalle
  - `ALTER_TABLE_CG42_agregar_entidad.sql` — columna `CG42ENTD` (FK a ENTD) + índice `IDX_CG42_ENTD_EJRD`
  - `SaldoCuentaG42.java` — campo `entidad` agregado
  - `SaldoCuentaG42DaoService/Impl` — método `selectByEntidadYDetalle()` agregado
  - `AporteDaoService/Impl` + `AporteService/Impl` — métodos `selectSumaRendimientoPorEntidad()`, `selectSumaPatronalPorEntidad()`, `selectSumaPersonalPorEntidad()` agregados
  - `HistorialSueldoDaoService/Impl` + `HistorialSueldoService/Impl` — método `selectSumaAportePersonalPorEntidad()` agregado
  - Orquestador → `@EJB g42Service` activo, `case "G42"` descomentado
- [x] G43 — `ParticipeCesanteG43` — **COMPLETADO**
  - `GeneracionG43Service.java` + `GeneracionG43ServiceImpl.java`
  - Lógica: compara entidades del G42 mes anterior vs G42 mes actual con NOT EXISTS en BD. Si no hay EJRC del mes anterior, compara HistoricoG42 vs G42 actual.
  - `SaldoCuentaG42DaoService/Impl` → métodos `selectByDetalle()` y `selectCesantesDesdeG42Previo()` (NOT EXISTS) agregados
  - `SaldoCuentaG42Service/Impl` → mismos métodos expuestos
  - `DetalleEjecucionReporteDaoService/Impl` → método `selectByEjecucionYTipo()` agregado
  - `DetalleEjecucionReporteService/Impl` → mismo método expuesto
  - `HistoricoG42DaoService/Impl` → método `selectCesantesDesdeHistorico()` (NOT EXISTS) agregado
  - `HistoricoG40..G51.java` → `NamedQuery` por Id eliminado de todos (solo queda `All`) — fix error Hibernate
  - Orquestador → `@EJB g43Service` activo, `case "G43"` descomentado
- [x] G44 — `ParticipeJubiladoG44` — **COMPLETADO**
  - `GeneracionG44Service.java` + `GeneracionG44ServiceImpl.java`
  - Lógica: Entidades con idEstado=30. COUNT aportes tipos 9/11 → imposiciones. VPPC.valorPagar → valorPension y valorNetoRecibir. SUM aportes tipo 23 → saldoCuenta. jubilacionIess = "S" fijo.
  - `AporteDaoService/Impl` + `AporteService/Impl` → `selectCountImposicionesJubilacionPorEntidad()` y `selectSumaSaldoCuentaJubilacionPorEntidad()` agregados
  - `ValorPagoPensionComplementariaDaoService/Impl` → `selectByEntidad()` agregado + clase completada con `EntityManager` y `obtieneCampos()`
  - `ValorPagoPensionComplementariaService/Impl` → `selectByEntidad()` expuesto
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
- [ ] G47 — `NovacionG47` — lógica pendiente
- [ ] G48 — `SaldoOperacionG48` — lógica pendiente
- [ ] G49 — `CancelacionG49` — lógica pendiente
- [ ] G50 — `GaranteG50` — lógica pendiente
- [ ] G51 — `GarantiaRealG51` — lógica pendiente

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
