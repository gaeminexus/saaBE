# Reporte del Ministerio de Trabajo (SUT) — se construye sin esperar el formato

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-01 · **Estado:** diseño congelado, sin implementar.

---

## 0. El problema, y por qué deja de ser un bloqueo

El empleador debe registrar el pago de la decimotercera y decimocuarta remuneración en el
**SUT / Sistema de Salarios en Línea** (`salarios.trabajo.gob.ec`), subiendo un **CSV delimitado por
comas**, **sin modificar ni borrar el encabezado**. El sistema valida los montos contra los sueldos
de los contratos registrados ahí. Plazo escalonado por el **noveno dígito del RUC** (enero-febrero
para 1-5, febrero-marzo para 6-9 y 0). Multa de hasta **20 SBU** por incumplir (Art. 628).

**La estructura exacta de columnas no está publicada.** Se obtiene descargando el archivo de ejemplo
del propio SUT, que exige login del empleador. Se intentaron tres búsquedas y dos PDF oficiales del
MDT; ninguno resultó extraíble.

> **Decisión del usuario, 2026-09-01: «luego obtengo el formato del SUT, pero esto no debe detener
> el trabajo».**

**Y no tiene por qué detenerlo, porque el módulo ya tiene un motor de formatos de archivo
configurable.** Verificado el 2026-09-01.

---

## 1. El motor que ya existe

`RHH.FMBN` (`FormatoArchivoBancario`) + `RHH.DFMB` (`DetalleFormatoBancario`) **no son un formato:
son un motor de formatos**, parametrizable por fila. Se construyó para los archivos de la banca
electrónica, y su vocabulario cubre exactamente lo que un CSV del MDT necesita:

| Cabecera `RHH.FMBN` | Para qué sirve acá |
|---|---|
| `FMBNTPFR` tipo de formato | delimitado vs posicional |
| `FMBNDLMT` delimitador | **la coma del CSV** |
| `FMBNEXTN` extensión | `.csv` |
| `FMBNCDFC` codificación | el SUT es sensible a esto |
| `FMBNCBCR` plantilla de cabecera | **el encabezado que el SUT exige NO borrar** |
| `FMBNPIEE` plantilla de pie | por si lo pide |
| `FMBNFRFC` formato de fecha | |

| Detalle `RHH.DFMB`, una fila por columna | |
|---|---|
| `DFMBCMPO` campo lógico · `DFMBORDN` orden | qué dato y en qué posición |
| `DFMBINCO` inicio · `DFMBLNGT` longitud | para formatos posicionales |
| `DFMBRLLN` lado · `DFMBCRLL` carácter de relleno | |
| `DFMBDCML` decimales · `DFMBSPDC` separador | el SUT suele pedir punto |
| `DFMBFRFC` formato de fecha · `DFMBVLFJ` valor fijo | |

**El campo lógico sale de un rubro** (`RhhCampoArchivoBancario`, rubro 224) y el generador lo
resuelve con un `switch` (`GeneracionOrdenPagoServiceImpl:503-535`): secuencial, identificación del
beneficiario, nombre, cuenta, tipo de cuenta, banco, valor, moneda, referencia, fecha de proceso,
literal fijo.

---

## 2. Consecuencia: el formato pasa a ser una FILA, no código

```
Hoy, sin el CSV del SUT   ->  se construye el generador y el catálogo de campos
Cuando llegue el CSV      ->  se carga UNA configuración de formato. Cero código.
```

Eso es lo que saca este frente del camino crítico. **Lo único que el CSV decide es el contenido de
esas filas de configuración**, no la forma del programa.

**Y hay un beneficio que no se buscaba:** si el MDT cambia el layout —cosa que hace— se ajusta la
parametrización en pantalla, sin tocar el WAR. La pantalla de formatos ya existe
(`rrh/forms/parametrizacion/formatos-archivo-bancario`).

---

## 3. Qué hay que construir

### 3.1 Rubro nuevo de campos lógicos del MDT

`RhhCampoArchivoMdt`, con los campos que **cualquier** layout del SUT va a pedir — esto no es
adivinar el formato, es el conjunto de datos disponibles:

`SECUENCIAL` · `IDENTIFICACION` · `APELLIDOS` · `NOMBRES` · `APELLIDOS_Y_NOMBRES` ·
`TIPO_IDENTIFICACION` · `VALOR_PAGADO` · `FECHA_PAGO` · `PERIODO_DESDE` · `PERIODO_HASTA` ·
`ANIO` · `DIAS` · `RUC_EMPLEADOR` · `RAZON_SOCIAL_EMPLEADOR` · `LITERAL_FIJO`

⚠️ **Reservar `PRBR`/`PDTR` en `REGISTRO-RESERVAS-EQUIPOS.md` antes de escribir el script**, y anotar
**también el `PRBRALTR`** — ver su §6 «El registro reserva `PRBRCDGO`, pero el código busca por `PRBRALTR`». Bloque de este equipo: `PRBR` 310-329 /
`PDTR` 1500-1599, con 310 y 1500-1503 ya usados.

### 3.2 El generador

`POST /rest/odbs/reporteMdt/{idOrden}` con `{idFormato}` → devuelve el archivo en
`contenidoBase64` + `avisos`, **el mismo contrato de respuesta que usa el ATS**
(`POST /rest/ats/generar`), que ya está probado y que el frontend ya sabe consumir.

**Los datos ya están construidos.** `GET /odbs/detalle/{id}` (§1.3 del contrato de ODBS) ya devuelve
por empleado: identificación, nombre, valor, valor pagado, período y días. El generador no consulta
nada nuevo: toma esa lista y la formatea.

### 3.3 Reutilizar, no duplicar

⛔ **No escribir un segundo motor.** La lógica de relleno, decimales, fechas y delimitador vive en
`GeneracionOrdenPagoServiceImpl` (alrededor de `:503-535` y el armado de línea). **Extraerla a un
componente reutilizable y que los dos generadores la usen**, en vez de copiarla.

*Si al intentarlo resulta que está demasiado entrelazada con `RHH.DRPG` para extraerla sin
reescribir el generador bancario —que está en producción y se estrena este mes— **parar y
reportar**. En ese caso la salida es un segundo generador que comparte el modelo de datos
(`FMBN`/`DFMB`) aunque no el código, y se anota la deuda.*

---

## 4. Lo que sigue esperando al CSV, y sólo eso

1. **Qué campos pide y en qué orden** → filas de `RHH.DFMB`.
2. **El texto exacto del encabezado** → `FMBNCBCR`. El SUT exige no modificarlo.
3. **Si decimotercera y decimocuarta comparten layout** o son dos formatos.

Nada de eso es código. Cuando llegue el archivo, se carga con un `.sql` de configuración.

---

## 5. Riesgo declarado

**El XML del ATS nunca se validó contra el validador oficial del SRI** (`ESTADO-CXP-CXC-TSR-RHH-SRI.md`
§4.7), y este reporte va a nacer con la misma limitación: **nadie lo va a haber subido al SUT hasta
que un usuario lo intente**, y el primer intento real cae contra el plazo legal.

**Mitigación concreta:** probar la carga en el SUT **fuera de temporada**, con un archivo de un
empleado, apenas se tenga el formato. No esperar a enero.
