# REGLAS GENERALES — MÓDULO PETROCOMERCIAL / ASOPREP

**Documento consolidado y verificado contra el código fuente al 2026-08-13.**

Este archivo, junto con [REGLAS-CARGA-PETRO.md](REGLAS-CARGA-PETRO.md) y
[REGLAS-GENERACION-PETRO.md](REGLAS-GENERACION-PETRO.md), es la **referencia vigente** del módulo.
Consolida y reemplaza a los ~29 `.md` históricos (`PROCESO-*`, `CORRECCION-*`, `REVISION_*`,
`CERTIFICACION_*`, etc.) que vivían en esta carpeta y fueron eliminados el 2026-08-13: documentaban
correcciones puntuales, se contradecían entre sí y usaban códigos de estado obsoletos. Si hace
falta consultarlos, están en el historial de git.
Ante cualquier duda, la autoridad es el código; estos tres documentos lo resumen.

> **Regla de mantenimiento:** cualquier cambio en `CargaArchivoPetroServiceImpl`,
> `ProcesoCargaPetroServiceImpl`, `GeneracionArchivoPetroServiceImpl` o sus DAOs/REST asociados
> debe reflejarse en estos tres documentos en el mismo commit.

---

## 1. Qué es el módulo

ASOPREP (fondo de cesantía/jubilación de los empleados de Petrocomercial) cobra a sus partícipes
por **descuento de nómina**. El ciclo mensual es:

```
1. GENERACIÓN  — saaBE arma el archivo de descuentos del periodo (qué cobrar a cada partícipe)
                 y se lo envía a la empresa (Petrocomercial o ARCH según la filial).
2. DESCUENTO   — la empresa aplica los descuentos en nómina.
3. RESPUESTA   — la empresa devuelve un archivo TXT con lo efectivamente descontado.
4. CARGA       — saaBE carga ese TXT, lo valida (fases 1 y 2) y el usuario revisa novedades.
5. APLICACIÓN  — saaBE aplica los pagos a préstamos y genera los aportes (fase 3).
```

La **generación** vive en `crd` (`GeneracionArchivoPetroServiceImpl`); la **carga/aplicación** en
`asoprep` (`CargaArchivoPetroServiceImpl`).

## 2. Código fuente autoritativo

| Área | Clase | Notas |
|---|---|---|
| Carga + aplicación de pagos (vigente) | `com.saa.ejb.asoprep.serviceImpl.CargaArchivoPetroServiceImpl` (`@Stateful`) | ~3.750 líneas. Fases 1, 2 y 3 |
| Procesamiento alterno "FASE 2" | `com.saa.ejb.crd.serviceImpl.ProcesoCargaPetroServiceImpl` (`@Stateless`) | Vía paralela **incompleta** (aportes con `TODO`, `esAporte=false` fijo). Registra en `CRD.PRCA`. Endpoint `/asgn/procesarCargaPetro/{id}`. El flujo productivo es `aplicarPagosArchivoPetro` |
| Generación del archivo | `com.saa.ejb.crd.serviceImpl.GeneracionArchivoPetroServiceImpl` (`@Stateless`) | Incluye formatos por filial y ciclo de vida GNAP |
| REST carga | `com.saa.ws.rest.asoprep.AsoprepGenerales` (`@Path("asgn")`), `com.saa.ws.rest.crd.CargaArchivoRest` (`@Path("crar")`) | |
| REST generación | `com.saa.ws.rest.crd.GeneracionArchivoPetroRest` (`@Path("gnap")`) | |

## 3. Productos (códigos de 2 letras del archivo)

| Código | Significado | Tratamiento |
|---|---|---|
| `AH` | Aportes (jubilación + cesantía) | No es préstamo. Genera/paga registros `CRD.APRT`. Maneja la mora del partícipe |
| `HS` | **Seguro de incendio** de PH/PP | Nunca se procesa solo: su monto se suma al del PH/PP del mismo partícipe |
| `PE` | Préstamo Emergente | Préstamo normal (NO suma HS) |
| `PH` | Préstamo Hipotecario | Suma HS |
| `PQ` | Préstamo Quirografario | Préstamo normal |
| `PP` | Préstamo Prendario/Personal (los comentarios del código usan ambos nombres) | Suma HS |

Mapeo BD: `DetalleCargaArchivo.codigoPetroProducto` ↔ `Producto.codigoPetro` (`CRD.PRDC.PRDCCDPT`).

## 4. Filiales (`com.saa.rubros.Filiales`, tabla `CRD.FLLL`)

| Código | Filial | Identificador del partícipe | Formato del archivo generado |
|---|---|---|---|
| 1 | PETROCOMERCIAL | `ENTD.rolPetroComercial` (> 0) | Posicional 55 caracteres |
| 2 | ARCH | `ENTD.numeroIdentificacion` | Plano separado por `;`, una columna por producto |

`esFilialPetrocomercial()` trata `codigoFilial == null` como Petrocomercial.

## 5. Tablas del módulo (todas en schema CRD)

### Carga (respuesta de la empresa)
```
CRAR CargaArchivo                 cabecera del archivo cargado (mes/año afectación, totales, ruta, estado)
 └── DTCA DetalleCargaArchivo     un registro por producto, con totales
      └── PXCA ParticipeXCargaArchivo   una fila por línea del TXT (valores descontados / no descontados)
           └── NVPC NovedadParticipeCarga     N novedades por partícipe (tipo, montos esperado/recibido/diferencia)
                └── AVPC AfectacionValoresParticipeCarga   afectación MANUAL del usuario (prestamo, cuota, valores)
PRCA ProcesamientoCargaArchivo    tracking de la vía alterna ProcesoCargaPetroServiceImpl (no la usa el flujo vigente)
```

### Generación (archivo hacia la empresa)
```
GNAP GeneracionArchivoPetro       cabecera (periodo + filial únicos, estado, totales, archivo, marca de descarga)
 └── DTGA DetalleGeneracionArchivo      un registro por producto
      └── PDGA ParticipeDetalleGeneracionArchivo   una fila por línea (entidad, préstamo, monto, nº línea)
           └── CXPG CuotaXParticipeGeneracion      desglose: cuotas sumadas (préstamos/HS) o tipoAporte 9/11 (AH)
```
Nota: los SQL históricos llaman `PXDG` a la tabla de partícipes; la entidad JPA real es `PDGA`.

### Negocio afectado
`ENTD` (partícipe), `PRDC` (producto), `PRST` (préstamo), `DTPR` (cuota), `PGPR` (PagoPrestamo —
**fuente de verdad de lo pagado**), `APRT` (aporte), `PGAP` (PagoAporte), `HSTR` (HistorialSueldo —
montos esperados de jubilación/cesantía; **el registro vigente es `estado = 99`**).

## 6. Catálogos vigentes (verificados en `com.saa.rubros`)

⚠️ Los documentos históricos usan numeraciones obsoletas (p.ej. "OK=1", "PAGADA=2", "entidad
ACTIVA=10"). **Los valores vigentes son los de las interfaces Java:**

### `ASPNovedadesCargaArchivo` (rubro 169)
```
0 OK                                    12 CUOTA_NO_ENCONTRADA
1 PARTICIPE_NO_ENCONTRADO               13 MONTO_INCONSISTENTE
2 CODIGO_ROL_DUPLICADO                  14 PRESTAMO_PROCESADO_OK
3 NOMBRE_ENTIDAD_DUPLICADO              15 APORTE_GENERADO_OK
4 CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE   16 CUOTA_FECHA_DIFERENTE
5 SIN_DESCUENTOS                        17 DIFERENCIA_MENOR_UN_DOLAR
6 DESCUENTOS_INCOMPLETOS                18 HISTORIAL_SUELDO_NO_ENCONTRADO
7 DESCUENTOS_ADICIONALES                19 MULTIPLES_REGISTROS_HISTORIAL_SUELDO
8 VALORES_CERO                          20 VALORES_HISTORIAL_NULOS
9 PRODUCTO_NO_MAPEADO                   21 APORTE_VALORES_CERO
10 PRESTAMO_NO_ENCONTRADO               22 APORTE_MONTO_INCONSISTENTE
11 MULTIPLES_PRESTAMOS_ACTIVOS          23 APORTE_DIFERENCIA_MENOR_UN_DOLAR
```

### `EstadoCuotaPrestamo` (DTPR.DTPRESTD; también se reutiliza para el estado de APRT)
```
1 PENDIENTE   2 ACTIVA   3 EMITIDA   4 PAGADA   5 EN_MORA   6 PARCIAL   7 CANCELADA_ANTICIPADA   8 VENCIDA
```

### `EstadoPrestamo` (PRST.PRSTIDST — ¡no ESPSCDGO! ver CLAUDE.md "Trampa: qué columna lleva realmente el estado")
```
1 GENERADO   2 VIGENTE   3 CANCELADO   4 CANCELADO_ANTICIPADO   5 CANCELADO_POR_NOVACION
6 PENDIENTE_DE_APROBACION   7 RECHAZADO   8 DE_PLAZO_VENCIDO   9 CANCELADO_POR_REVISAR
10 VIGENTE_POR_REVISAR   11 EN_MORA
```
Estados **terminales** (nunca se cambian automáticamente): 3, 4, 5.

### `EstadoParticipeEntidad` (ENTD.ENTDIDST — códigos alternos desde la migración 2026-08-11)
```
1 ACTIVO   2 CESANTE   3 JUBILADO_COMPLEMENTARIO   4 CESANTE_DESAFILIADO   5 CESANTE_FALLECIDO
6 JUBILADO_APORTANTE   7 JUBILADO_PASIVO   8 ACTIVO_EN_MORA   9 NUEVO
```
Los docs antiguos que dicen "entidad `idEstado = 10` = ACTIVO" usan la PK vieja de `ESPR`; hoy ACTIVO = 1.

### `TipoAporte`
`9` = Jubilación, `11` = Cesantía (constantes en ambos servicios).

### Estados de `CargaArchivo` (CRAR)
El código usa: `1` al crear (Estado.ACTIVO) y **`3` = PROCESADO** al terminar la fase 3
(`validarOrdenProcesamiento` busca la última carga con estado 3).
⚠️ Discrepancia conocida: el rubro 166 (`ASPEstadoCargaArchivoPetro`) define
`1 CARGADO, 2 VALIDADO, 3 APROBADO_CONTABILIDAD, 4 PROCESADO`, pero **el código nunca usa el 4**;
"procesado" en la práctica es 3.

### Estados de `GeneracionArchivoPetro` (GNAP)
```
0 PENDIENTE (recién creada; también el estado al que vuelve una generación ANULADA)
1 GENERADO   2 ENVIADO   3 PROCESADO
```
Transiciones: 0→1 (`generarArchivo`), 1→2 (`marcarEnviado`), 2→3 (`marcarProcesado`), 1→0 (`anular`,
motivo en observaciones — al volver a 0 puede regenerarse). La **marca de descarga**
(`fechaDescarga`/`usuarioDescarga`) se estampa en la primera descarga y no se sobreescribe.

## 7. Endpoints REST vigentes (application path `/SaaBE/rest`)

### Carga — `@Path("asgn")` (`AsoprepGenerales`)
```
POST /rest/asgn/validarArchivoPetro                     multipart: archivo + CargaArchivo JSON. Fases 1+2
POST /rest/asgn/procesarArchivoPetro                    variante multipart equivalente (mismo pipeline)
POST /rest/asgn/aplicarPagosArchivoPetro/{idCarga}      Fase 3 (aplicación de pagos y aportes)
GET  /rest/asgn/valoresSinDestino/{idCarga}             preview de los registros que bloquearían la fase 3
POST /rest/asgn/procesarCargaPetro/{idCarga}            vía alterna ProcesoCargaPetroServiceImpl (PRCA)
GET  /rest/asgn/reporteProcesamientoPetro/{idCarga}
GET  /rest/asgn/actualizaCodigoPetroEntidad/{codigoPetro}/{idParticipeXCarga}/{idEntidad}
POST /rest/asgn/upload/custom
```

### Cabecera de carga — `@Path("crar")` (CRUD estándar + `GET /rest/crar/getByAnio/{anio}`)

### Generación — `@Path("gnap")` (`GeneracionArchivoPetroRest`)
```
GET    /rest/gnap/getAll                                 GET  /rest/gnap/getId/{id}
GET    /rest/gnap/porFilial/{codigoFilial}
POST   /rest/gnap/crearCabecera?mes&anio&codigoFilial&usuario
POST   /rest/gnap/generarArchivo/{codigoGeneracion}
GET    /rest/gnap/descargarArchivo/{codigo}?usuario=     (estampa la marca de descarga ANTES de enviar)
DELETE /rest/gnap/eliminar/{codigo}?usuario=             (cascada CXPG→PDGA→DTGA→GNAP + TXT)
DELETE /rest/gnap/{id}?usuario=                          (alias de eliminar)
POST   /rest/gnap/selectByCriteria                       PUT/POST genéricos (sin validación de duplicado — no usar para crear)
```
Los docs antiguos con rutas `/api/...`, `/api/generacion-petro`, `/api/petrocomercial` están obsoletos.

## 8. Tolerancias (importante: son DOS regímenes distintos)

| Contexto | Umbral | Uso |
|---|---|---|
| **Validación (fases 1-2)** | `TOLERANCIA = 1.0` ($1) | Comparar monto del archivo vs cuota/aporte esperado. Diferencias ≤ $1 generan `DIFERENCIA_MENOR_UN_DOLAR` (informativa); > $1 generan `MONTO_INCONSISTENTE` / `APORTE_MONTO_INCONSISTENTE` |
| **Aplicación (fase 3)** | `0.01` (1 centavo) | Decidir PAGADA vs PARCIAL, saldos "insignificantes", montos agotados. Los montos se aplican EXACTOS, sin ajuste |
| Valores sin destino | `TOLERANCIA = 1.0` | Un faltante de cobertura manual ≤ $1 no bloquea la carga |

## 9. Trampas conocidas (verificadas en código)

1. **`seguroDescontado` del archivo = DESGRAVAMEN**, no seguro de incendio. El seguro de incendio
   viaja en un registro separado con producto `HS` y se valida contra `DTPR.valorSeguroIncendio`
   (`DTPRVLSI`), no contra el desgravamen.
2. **`PGPR` (PagoPrestamo) es la fuente de verdad de lo pagado.** Los campos `*Pagado` de la cuota
   pueden estar desactualizados; `calcularSaldosRealesCuota()` (carga) y `obtenerPagosPorCuota()`
   (generación) siempre recalculan desde PGPR.
3. **`saldoCapital = max(0, saldoInicialCapital − capitalPagado)`** — usa `saldoInicialCapital`
   (saldo del préstamo al inicio de la cuota), NO `capital` (capital de la cuota).
4. **Estado del préstamo va en `PRSTIDST` (idEstado)**, no en `ESPSCDGO`; estado de la cuota va en
   `DTPRESTD` (estado), no en `DTPRIDST` — ver CLAUDE.md. El código escribe ambos campos de la
   cuota (`setEstado` + `setIdEstado`) por compatibilidad.
5. **`PGPR.PGPRIDST` es NOT NULL**: todo `PagoPrestamo` nuevo debe llevar `setIdEstado(1L)` o
   revienta con ORA-01400.
6. En JPQL, `estado NOT IN (...)` **descarta las filas con estado NULL**; los filtros de cuotas
   pendientes deben escribirse `(d.estado IS NULL OR d.estado NOT IN (:pagada, :cancelada))`.
7. Oracle no admite más de 1.000 elementos en un `IN`: las consultas agregadas sobre PGPR se hacen
   en bloques de 500.
8. El archivo de respuesta se lee en **ISO-8859-1** (ñ/tildes); prohibido cambiar a UTF-8.
9. `parseDouble` del parser acepta formato europeo (`1.234,56`) y devuelve `0.0` ante error, nunca
   lanza excepción.
10. **⛔ `PRST.PRSTSLTT` (`saldoTotal`) y `PRST.PRSTSLCP` (`saldoCapital`) son campos MUERTOS: nadie
    los actualiza.** En todo el backend existe `Prestamo.setSaldoTotal()` y **cero llamadas**; el
    valor es el que dejó la migración y nunca se movió. Medido el 2026-09-01: los préstamos
    **cancelados** conservan **28,5 millones** de saldo de capital entre los estados 3 y 4.

    **No filtrar ni sumar por esas dos columnas.** El saldo real se reconstruye desde las cuotas y
    los pagos (`SaldoPrestamoService` en el frontend, `calcularTotalPendientePrestamo` en el
    backend), que es lo que ya hacía la pantalla de cobros personales
    (`cobros-personales.component.ts:292`).

    **Costó un defecto en producción.** La pestaña de descuentos de `archivo-petro/carga/detalle`
    filtraba la lista de préstamos afectables por `saldoTotal > 0`, así que **los préstamos EN MORA
    con `PRSTSLTT` en 0 o NULL no aparecían** al afectar una novedad bloqueante — y, por el otro
    lado, **sí aparecían préstamos ya cancelados**. Corregido el 2026-09-01 filtrando por
    `idEstado` (VIGENTE 2 y EN_MORA 11; `DE_PLAZO_VENCIDO` 8 queda fuera por decisión del usuario).
    Diagnóstico en `crd/sql/154_DIAGNOSTICO_PRSTSLTT_AFECTABLES.sql`.

    Lo que hace difícil de ver este defecto: **el filtro no menciona el estado en ninguna parte**,
    así que buscar "por qué no salen los que están en mora" lleva derecho a los filtros de estado
    de la carga —que están todos bien e incluyen la mora— y no al filtro de saldo, que es el que
    los estaba descartando.
