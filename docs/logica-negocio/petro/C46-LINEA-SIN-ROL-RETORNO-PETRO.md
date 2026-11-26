# C46 — Línea sin rol del retorno de Petro y control de cuadre contra los TOTAL del reporte

Equipo omen-saa-1 · 2026-11-26 · rama `omen1-c46-petro-linea-sin-rol`. Contexto del formato: `REGLAS-CARGA-PETRO.md` §1.2.

## 1. El defecto
El lector del retorno trataba como «resumen» (y saltaba) **toda** línea con el rol [0,7) vacío. Cuando un partícipe tiene dos descuentos en el mismo producto,
Petro imprime la segunda línea con rol y nombre en blanco **pero con todas las columnas numéricas** en las mismas posiciones, y debajo una línea
`TOTAL EMPLEADO` (sin rol, con `:` hacia la columna 79 y la suma de las dos en las columnas de montos). La segunda línea se perdía: el SAA cargaba menos de lo
que Petro descontó, sin ningún aviso.

## 2. Qué hace ahora
**`LectorRetornoPetro`** (`com.saa.ejb.asoprep.serviceImpl`, clase pura, sin EJB ni BD) lee el contenido y devuelve un `ResultadoLecturaRetornoPetro`: las filas
(`List<ParticipeXCargaArchivo>`, como antes) y el cuadre. `CargaArchivoPetroServiceImpl.leerRetornoPetro` delega en él (la carga y el reproceso de carga), loguea y
deja la alerta en `CargaArchivo.alertaCuadre`. Todo lo demás se conserva: encabezado `EP`, salto de 8 líneas, línea de producto, líneas de resumen, errores con
número de línea y línea cruda (el número de línea ahora es el real; antes, desde la segunda sección, se calculaba mal).

- **Continuación**: rol [0,7) vacío, nombre [7,44) vacío, la línea no contiene `TOTAL` y trae plazo, monto a descontar y total descontado numéricos → registro
  nuevo con el `codigoPetro` y el nombre del **último partícipe leído en la misma sección**, el producto de la sección y sus propios montos. El «último rol»
  sobrevive a un salto de página `EP` dentro de la misma sección y se reinicia cuando cambia el producto. Una continuación sin partícipe previo en la sección es un
  **error con número de línea** (no se adivina). Se cuentan y se loguean junto a las líneas de resumen.
- **`TOTAL EMPLEADO`**: se salta, pero se valida: debe ser igual a la suma del total descontado de las líneas del partícipe desde su línea con rol. Si no, **alerta**
  (no excepción).
- **Cuadre** (columna total descontado, [140,155)): por sección, Σ cargado = su `TOTAL <producto>` (se ubica la sección por la descripción del producto), y Σ de todo
  = `TOTAL ==>` (línea de ~105 caracteres; el número va a la derecha de `==>`). Tolerancia 0,005. Una sección sin línea TOTAL, o un archivo sin `TOTAL ==>`, también
  es alerta. `TOTAL ZONA …` se ignora. Los montos con punto de miles (`165.569,33`) los acepta `parseDouble`.
- **La última página** del reporte es un resumen por producto (`CODDSC/CODRAZ/VALOR`) con un encabezado más corto: su «línea de producto» no trae código. No abre una
  sección de cuadre; sus líneas se cuentan como resumen, igual que siempre.
- **Alerta persistente**: si hay cualquier diferencia, la carga **no falla** (marzo 2026 es un descuadre del propio Petro y hay que poder cargarlo) y queda en
  `CRD.CRAR.CRARALCD` (2000, nullable; `CargaArchivo.alertaCuadre`), con un texto como
  «Descuadre con el reporte de Petro: PE (PRESTAMO ESPECIAL) subtotal 118.133,90, cargado 118.113,90, diferencia 20,00; TOTAL ==> 370.577,20, cargado 370.557,20,
  diferencia 20,00». `NULL` si cuadra. La diferencia es «lo que imprime Petro menos lo cargado».
- **Fase 3** (`aplicarPagosArchivoPetro`): si la carga tiene alerta, el resumen que devuelve empieza con un bloque `ALERTA DE CUADRE: …` y se loguea con ⚠️.
  **No bloquea la fase 3**: si debe bloquearla es una decisión pendiente del usuario.

## 3. Revisión de la parte crd (qué asume un registro por rol y producto)
Se revisaron `CargaArchivoPetroServiceImpl` (fases 1, 2 y 3, reproceso) y `ProcesoCargaPetroServiceImpl`. No hay un mapa ni un `Set` que deduplique por (rol, producto) en
el recorrido principal: cada fila (`ParticipeXCargaArchivo`) se procesa por su cuenta —validación de partícipe, novedades `NVPC` por código de la fila, afectaciones
manuales, aplicación por el motor—, así que dos filas del mismo rol y producto siguen el mismo camino que ya seguían los roles repetidos con rol impreso (enero 2026).
Lo encontrado:

1. **Defecto latente corregido — el HS (seguro de incendio) se sumaba a las dos filas.** `selectByCodigoPetroYProductoEnCarga` devuelve una sola fila (la primera de
   la lista, sin `ORDER BY`), y el HS se suma al monto de la fila PH/PP (`montoConHsMergeado`) si `hsLeCorrespondeAFila`. Con dos filas PH del mismo rol, las dos
   cumplían la condición y las dos sumaban el mismo HS (doble cobro; lo habría frenado el invariante recibido = aplicado). Ahora la consulta ordena por código de fila y
   `hsLeCorrespondeAFila` devuelve `false` si la fila no es la primera de su producto: el HS viaja solo con la primera. En los retornos reales (abril y mayo 2026 traen
   un rol con dos filas PH) el rol afectado no tiene HS, así que no cambia ningún resultado medido; es protección.
2. **No corregido, sin caso real**: un rol con **dos filas HS**. El merge solo toma la primera; el HS de la segunda no se suma y, si el rol tiene PH/PP, se omite como
   «no huérfano». No aparece en ninguno de los 14 retornos revisados; si apareciera, el invariante recibido = aplicado frena la fase 3 antes de los asientos.
3. **Reproceso con excedentes devueltos (D-010)**: la clave es `rol|producto`; con dos filas, el excedente se aplica en la primera y la segunda ve «ya aplicado» (0). Es
   lo correcto (un excedente por rol y producto).

## 4. Archivos
- `src/main/java/com/saa/ejb/asoprep/serviceImpl/LectorRetornoPetro.java` y `ResultadoLecturaRetornoPetro.java` (nuevos).
- `src/main/java/com/saa/ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java`: delega en el lector (la carga y el reproceso de carga), alerta en el resumen de la
  fase 3, guarda de HS en `hsLeCorrespondeAFila`.
- `src/main/java/com/saa/model/crd/CargaArchivo.java`: `alertaCuadre` (`CRARALCD`).
- `src/main/java/com/saa/ejb/crd/daoImpl/ParticipeXCargaArchivoDaoServiceImpl.java`: `ORDER BY p.codigo`.
- `docs/logica-negocio/crd/sql/411_CRAR_ALERTA_CUADRE_PETRO.sql` (**va antes del EAR**; no ejecutado).
- `src/test/java/com/saa/ejb/asoprep/LectorRetornoPetroPruebas.java` y `tools/pruebas/correr-pruebas-c46.sh`.

## 5. Cómo correr la prueba
Desde la raíz del repo: `mvn -o -q compile` y `sh tools/pruebas/correr-pruebas-c46.sh` (sin JUnit; sale con 1 si algo falla). Cubre: (a) archivo normal que cuadra;
(b) segunda línea sin rol + `TOTAL EMPLEADO`; (c) subtotal de sección con +20,00; (d) continuación tras un salto de página `EP` y continuación sin partícipe previo;
(e) `TOTAL EMPLEADO` que no cuadra; (f) sin TOTAL de sección ni `TOTAL ==>` y montos con punto de miles. Resultado: 30 verificaciones, 0 fallos. La prueba de la C33
(`correr-pruebas-c33.sh`) sigue pasando.

## 6. Verificación contra los retornos reales (lector sobre cada archivo; solo totales del archivo)
| Archivo | Líneas | Continuaciones | Suma cargada | TOTAL ==> | Alerta |
|---|---|---|---|---|---|
| jun-2025 | 3.636 | 0 | 535.477,39 | 535.477,39 | no |
| jul-2025 | 3.579 | 0 | 513.962,53 | 513.962,53 | no |
| ago-2025 | 3.420 | 0 | 456.577,03 | 456.577,03 | no |
| sep-2025 | 3.001 | 0 | 394.761,00 | 394.761,00 | no |
| oct-2025 | 2.945 | 0 | 380.075,62 | 380.075,62 | no |
| dic-2025 | 2.907 | 0 | 406.361,85 | 406.361,85 | no |
| ene-2026 | 2.872 | 0 | 373.242,83 | 373.242,83 | no |
| feb-2026 | 2.832 | 0 | 371.053,34 | 371.053,34 | no |
| mar-2026 | 2.689 | 0 | 370.557,20 | 370.577,20 | **sí**: PE subtotal 118.133,90, cargado 118.113,90, dif. 20,00; TOTAL ==> dif. 20,00 |
| abr-2026 | 2.765 | 2 (866,00) | 355.498,26 | 355.498,26 | no |
| may-2026 | 2.771 | 3 (966,00) | 358.568,72 | 358.568,72 | no |
| jun-2026 | 2.732 | 2 (276,67) | 352.005,81 | 352.005,81 | no |
| jul-2026 | 2.683 | 1 (115,82) | 362.725,02 | 362.725,02 | no |
| ago-2026 | 2.643 | 0 | 354.491,37 | 354.491,37 | no |

Todas las secciones de todos los archivos cuadran con su TOTAL salvo la PE de marzo. (Los archivos no se copiaron al repo ni se imprimieron nombres ni cédulas.)

## 7. Pendiente
- Ejecutar el `sql/411` **antes** del EAR (sin la columna, el guardado de `CRD.CRAR` falla).
- Decisión del usuario: si una carga con alerta debe **bloquear** la fase 3 (hoy solo avisa).
- La pantalla (FE) puede mostrar `alertaCuadre` de la carga; el campo ya viaja en el JSON de `CargaArchivo`. No se tocó saaFE.
- Probar en la copia una carga real de abril (con las continuaciones) de punta a punta: la fase 3 no se ejecutó aquí.
