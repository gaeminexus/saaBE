# Excel de asientos por tipo y rango de fechas (equipo omen-saa-3, 2026-11-14)

Pedido del usuario: un reporte de asientos **por tipo**, ordenado por el **número de asiento que usa
Contabilidad** (`ASNTNMAL`, p. ej. `DI-A-2026-11-0145`), por **rango de fechas**, descargable en **Excel**,
con los datos del asiento y su **valor** (total del debe; debe = haber). Se ofrece desde la consulta de asientos.

## Dónde
Pantalla **Contabilidad > Reportes > Listado de Asientos** (`/menucontabilidad/reportes/listado-asientos`,
`cnt/forms/reporte-listado-asientos`). Ya tiene filtros de fecha desde/hasta, categoría y tipo, estado, número y
usuario, y los botones «Exportar CSV» / «Exportar PDF» (solo las filas ya cargadas, sin valor). Se agrega el botón
**«Exportar Excel»** junto a esos dos. La otra pantalla con el mismo nombre (`/menucontabilidad/listado-asientos`,
la de mantenimiento) no cambia.

## Por qué en el BE
El FE no tiene librería de `.xlsx` (`package.json` sin `xlsx`/`exceljs`) y el valor del asiento sale de sumar
`CNT.DTAS`: traer todos los detalles al navegador para un mes de asientos es pesado. El BE ya tiene Apache POI
y el precedente `DocumentoSeguroRest.respuestaExcel` (`posg/{id}/listado/excel`).

## E1 — BE
`GET /rest/asnt/excel?idEmpresa=&desde=yyyy-MM-dd&hasta=yyyy-MM-dd[&idTipo=][&estado=][&categoria=general|sistema]`
→ `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`,
`Content-Disposition: attachment; filename="asientos_<desde>_<hasta>.xlsx"`.

- `idEmpresa`, `desde`, `hasta` obligatorios (400 con mensaje si faltan o `desde > hasta`). Rango máximo: no se
  limita; el filtro de fecha es sobre `ASNTFCHA` (fecha del asiento), inclusive en los dos extremos.
- `idTipo` opcional (sin él: todos los tipos de la categoría). `categoria` replica el filtro de la pantalla:
  `general` = `PLNTSSTM` 0 o 2, `sistema` = 1 (misma regla que `reporte-listado-asientos.component.ts` ~224).
  `estado` opcional (rubro 21: 1 activo, 2 anulado, 3 reversado, 4 incompleto); sin él, todos.
- Consulta en el **DAO** de asiento, JPQL con `@NamedQuery` o `em.createQuery` en el `DaoServiceImpl`, no en el
  service: asiento + tipo + `SUM(d.debe)`, `SUM(d.haber)` con `LEFT JOIN` al detalle (un asiento sin detalle sale
  con valor 0), `GROUP BY` el asiento.
- **Orden:** nombre del tipo, `numeroAnio`, `numeroMes`, `numeroMesTipo`, `codigo` (el `numeroMesTipo` es el
  consecutivo mensual que forma `ASNTNMAL`, `AsientoServiceImpl.asignaNumeroAlterno` ~254; ordenar por el texto
  también funcionaría, pero el número es exacto). Los que no tienen `numeroAlterno` van al final de su tipo.
- Armado con POI (`XSSFWorkbook`) en el service de asiento (método nuevo `generarExcel(...)` que devuelve
  `byte[]`), una hoja «Asientos»:
  - Fila 1: título «Asientos del dd/MM/yyyy al dd/MM/yyyy» (+ « — tipo X» si vino `idTipo`).
  - Encabezados (negrita, fila congelada, autofiltro):
    `Tipo | Número | Número por módulo | Fecha asiento | Fecha registro | Estado | Módulo origen | Glosa | Usuario | Valor`
    - Número = `ASNTNMAL`; Número por módulo = `ASNTNAMD` (vacío si no tiene); Fecha registro = `ASNTFCPR`
      (fecha y hora); Estado = nombre del rubro 21; Módulo origen = `ASNTMDOR`; Glosa = `ASNTOBSR`.
    - Valor = total del **debe**, celda numérica con formato `#,##0.00` (no texto).
    - Si en algún asiento debe ≠ haber, se agrega al final la columna `Aviso` con «Descuadrado: haber <valor>»
      solo en esas filas (no debería pasar; si pasa, que se vea).
  - Fechas como celdas de fecha (`dd/MM/yyyy`, la de registro `dd/MM/yyyy HH:mm`), no texto.
  - **Subtotal por tipo** (fila «Total <tipo>: N asientos», valor sumado, negrita) y **total general** al final.
    Los subtotales y el total suman las filas listadas: si el usuario no filtró estado, incluyen anulados; por eso
    el estado va en una columna, para que Contabilidad pueda filtrarlo en Excel.
  - Anchos de columna ajustados; Glosa con ancho fijo (~60) y ajuste de texto.
- Sin resultados → 200 con el Excel con encabezados y la fila «Sin asientos en el rango» (no un error).
- REST: `catch (Throwable e)` → 500 con el mensaje, como el resto. Traza `System.out.println` al inicio.
- Solo lectura. Sin SQL, sin tabla nueva.

## E2 — FE
- `AsientoService.descargarExcel(filtros)`: `http.get(url, { params, responseType: 'blob', observe: 'response' })`,
  nombre del archivo desde `Content-Disposition` (mismo patrón que `PolizaSeguroService`, incluido leer el error
  que llega como `Blob`).
- Botón **«Exportar Excel»** (icono `table_view` o `grid_on`) junto a «Exportar CSV» / «Exportar PDF».
  - Usa los filtros de la pantalla: fecha desde/hasta, categoría, tipo, estado. Número, descripción y usuario
    **no** se mandan (el Excel es por rango); si están llenos, se ignoran sin aviso.
  - Exige desde y hasta: sin alguna, snackbar «Seleccione fecha desde y hasta para exportar a Excel».
  - No depende de haber presionado «Buscar»: va directo al BE con los filtros.
  - Mientras descarga, el botón se deshabilita con spinner; error → snackbar con el mensaje del BE.
- Fechas como `yyyy-MM-dd` (nunca `Date` crudo), `idEmpresa` de la sesión, como en la búsqueda actual.
- Contrato en `saaFE/docs/cnt/API-EXCEL-ASIENTOS.md`.

## Verificación
- BE: `mvn -q compile`; el árbitro lee la consulta (filtro de fecha inclusivo, orden, `LEFT JOIN`).
- FE: `npx ng build`.
- Humo (lectura): exportar noviembre 2026 con tipo DI-A y comparar el valor de `DI-A-2026-11-0145` con la
  pantalla, y un rango sin tipo para ver los subtotales.

## Autoría y cruces
`AsientoRest`/`AsientoServiceImpl`/`AsientoDaoServiceImpl` los tocaron hace poco omen1 (U-2, U-5), omen2 y el
cuadre (C12, C17); `reporte-listado-asientos` lo tocó D-036. El cambio solo **agrega** métodos y un botón; no toca
lo existente.
