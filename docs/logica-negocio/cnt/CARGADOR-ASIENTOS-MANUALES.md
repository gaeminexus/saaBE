# Cargador de asientos manuales (C5 — cuadre Superintendencia)

**Escrito:** 2026-10-28 · **Rama:** `cuadre/c5-cargador-asientos` · **Expediente:** `cuadreSuper/01-seguimiento/CAMBIOS-SAA.md` §C5.

Registra en `CNT.ASNT` / `CNT.DTAS`, **un asiento por `numero_asiento`**, el contenido de un archivo con el
formato de la plantilla `cuadreSuper/03-trabajo/plantillas/PLANTILLA-ASIENTOS-MANUALES.csv`. Sirve para la
contabilidad que el SAA no genera (activos fijos, inversiones no privativas, otros grupos que el cliente lleva
fuera) y para cargar aperturas de saldos. **Rechazo total del archivo si falla una sola validación**, con la
lista completa de errores y su número de línea; si todo pasa, los asientos se graban en **una sola transacción**.

## 1. Piezas

| Capa | Archivo | Qué hace |
|---|---|---|
| REST | `ws/rest/cnt/AsientoRest.java` | `POST /SaaBE/rest/asnt/cargarManuales` (JSON) y `POST /SaaBE/rest/asnt/cargarManualesArchivo` (multipart) |
| Service | `ejb/cnt/service/CargaAsientosManualesService.java` + `serviceImpl/CargaAsientosManualesServiceImpl.java` | Parseo, validaciones, idempotencia, grabación |
| Service | `ejb/cnt/service/AsientoContableService.generarAsientoManual(...)` | Nuevo: misma grabación que `generarAsiento` pero con el `TipoAsiento` ya resuelto y **sin** la regla IN-A / EG-A / DI-A (los manuales no cambian, `DISENO-TIPOS-ASIENTO-AUTOMATICOS.md` §2.6). La grabación común quedó en el privado `grabarAsiento`, que `generarAsiento` también usa |
| DTO | `model/cnt/SolicitudCargaAsientosManuales.java`, `model/cnt/ResultadoCargaAsientosManuales.java` | Petición y respuesta (POJOs sin `@Entity`) |
| Rubro | `rubros/TipoAsientos.APERTURA = 10` | Tipo APERTURA de la empresa 1236 (ver §7) |

No hay tabla nueva ni DDL. La trazabilidad va en la observación del asiento.

## 2. Formato del archivo

CSV, separador `;`, UTF-8 (BOM tolerado), comillas dobles opcionales (`""` = comilla literal), primera línea de
cabecera. Las columnas se ubican **por nombre** (sin distinguir mayúsculas), así que el orden no importa.

| Columna | Oblig. | Regla |
|---|---|---|
| `fecha` | sí | `AAAA-MM-DD`. Fecha contable; define el período. Todas las líneas de un asiento llevan la misma |
| `numero_asiento` | sí | Agrupa las líneas de un asiento. Los asientos se crean en orden de aparición |
| `glosa` | sí | Observación del asiento (se toma la primera no vacía del grupo) |
| `cuenta` **o** `cuenta_saa` | sí | Cuenta contable (§3). Si vienen las dos, manda `cuenta_saa` |
| `cuenta_b17` | no | Se ignora (es la hoja del B17 donde se reporta; sirve para verificar fuera del SAA) |
| `nombre_cuenta` | no | Solo referencia; el cargador valida contra el código y graba el nombre del plan del SAA |
| `debe` / `haber` | sí | Número con punto decimal, sin separador de miles, ≥ 0, máximo dos decimales. **Uno de los dos en cero** por línea. Se acepta coma decimal si no hay punto |
| `documento_respaldo` | no | Va en la descripción de la línea (`DTASDSCR`) y, sin repetir, en la observación del asiento |
| `centro_costo` | no | Número del centro de costo (`CNT.CNCS.CNCSNMRO`); si viene, debe existir y estar activo |

Las líneas vacías se saltan. La variante del archivo de apertura
(`fecha;numero_asiento;glosa;cuenta_saa;cuenta_b17;nombre_cuenta;debe;haber;documento_respaldo;centro_costo`)
se acepta tal cual.

## 3. Cuentas: cómo se resuelve el código

1. **Con puntos** (`1.6.02.15`): búsqueda exacta por `CNT.PLNN.PLNNCNTA` en la empresa.
2. **Sin puntos** (`160215`, código del catálogo de la Superintendencia): la cuenta del SAA cuyos dígitos sin
   puntos coinciden. Solo vale si es **única** (en el plan de 1236 hoy no hay dos cuentas con los mismos dígitos).
3. En los dos casos la cuenta debe estar **activa** y ser **de movimiento** (`PLNNTPOO = 2`). Si es de agrupación,
   el error lista hasta cinco cuentas de movimiento hijas para elegir.

⚠ En el plan del SAA muchas hojas del catálogo son de **agrupación**: `110205` es `1.1.02.05 BANCOS E
INSTITUCIONES FINANCIERAS LOCALES` (nivel 4, agrupación) y sus cuentas de movimiento son una por banco
(`1.1.02.05.40`, …); igual `120105` (certificados de depósito por banda y banco). Por eso **las filas de ejemplo de la
plantilla del expediente (`110205`, `120105`) se rechazan**: el cliente tiene que entregar la cuenta del SAA (con
puntos) cuando la hoja del catálogo no es de movimiento. `ejemplos/asientos-manuales-ejemplo-2025-06.csv` es la
misma plantilla con cuentas válidas.

## 4. Validaciones (todas se acumulan; una sola falla rechaza el archivo)

| # | Regla | Mensaje (resumen) |
|---|---|---|
| P1 | `idEmpresa`, `usuario`, `nombreArchivo`, `contenido` obligatorios; la empresa existe | línea 0 |
| P2 | Tipo de asiento resuelto (§5) | línea 0 |
| A1 | Cabecera con `fecha`, `numero_asiento`, `glosa`, `debe`, `haber` y `cuenta` o `cuenta_saa` | `Falta la columna '…'` |
| L1 | `numero_asiento` no vacío | `numero_asiento vacio` |
| L2 | Fecha válida `AAAA-MM-DD` | `fecha invalida '…'` |
| L3 | `debe`/`haber` numéricos, ≥ 0, ≤ 2 decimales | `debe negativo`, `con mas de dos decimales`, … |
| L4 | Exactamente un lado con valor > 0 | `valor en debe y en haber`, `debe y haber en cero` |
| L5 | Cuenta existe, única (si va sin puntos), activa, de movimiento | §3 |
| L6 | Centro de costo (si viene) existe y activo | `el centro de costo '…' no existe` |
| S1 | Misma fecha en todas las líneas del asiento | `la fecha … no coincide con la de la primera linea` |
| S2 | Glosa no vacía en alguna línea | `glosa vacia` |
| S3 | Período de la fecha existe y no está MAYORIZADO ni CERRADO (`CNT.PRDO`) | `no existe el periodo …`, `esta MAYORIZADO` |
| S4 | **Cuadre al centavo**: Σ debe = Σ haber en centavos enteros | `el asiento no cuadra: debe …, haber …, diferencia …` |
| S5 | **Idempotencia**: no existe un asiento activo de la empresa cuya observación empiece con `CARGA MANUAL [archivo] #numero_asiento \|` | `ya esta registrado y activo …` |

S3 se valida aquí además de en `AsientoService.saveSingle`, porque `saveSingle` solo rechaza MAYORIZADO cuando
el período llega nulo, y en el camino normal el período ya viene asignado (solo bloquea CERRADO).

## 5. Tipo de asiento

- `codigoAltTipoAsiento` nulo o `0` → **por nombre** (`nombreTipoAsiento`, por defecto `DIARIO`). DIARIO, INGRESO
  y EGRESO comparten el alterno 0 en 1236 y `TipoAsientoService.codigoByAlterno(0)` devuelve el último
  (EGRESO): por eso el cargador no usa esa búsqueda para el 0.
- `codigoAltTipoAsiento` ≠ 0 → por alterno; si además viene `nombreTipoAsiento`, tiene que coincidir y desempata
  cuando varios tipos comparten el alterno. Si quedan varios candidatos, error "tipo ambiguo".
- Solo tipos activos (`PLNTESTD = 1`) de la empresa.
- El módulo del asiento es `ModuloSistema.CONTABILIDAD` (`ASNTRZZA/RZZB = 1`); sin módulo origen ni regla
  automática, con cualquier fecha.

Tipos de 1236 en la base del proyecto (28-oct-2026): DIARIO / INGRESO / EGRESO (0), CREDITOS 1, CXC 2, CXP 3,
T-INGRESOS 4, T-EGRESOS 5, RECURSOS HUMANOS 6, **APERTURA 10** (creado con 8 el 28-oct; lo mueve
`sql/c5-01-apertura-alterno-10.sql`, ver §7).

## 6. Trazabilidad y numeración

- **Observación** (`ASNTOBSR`, 2000): `CARGA MANUAL [<nombreArchivo>] #<numero_asiento> | <glosa> | Respaldo: <doc1>; <doc2>`.
  El prefijo hasta el `|` es la referencia que usa la idempotencia (S5).
- **Descripción de la línea** (`DTASDSCR`, 200): `documento_respaldo`; si viene vacío, la glosa.
- Número y número alterno los asigna `AsientoService.saveSingle` como a cualquier asiento: `DIA-2025-06-0001`
  (tres primeras letras del tipo, año, mes, consecutivo mensual por tipo).
- Usuario (`ASNTUSRO`): el de la petición.

## 7. APERTURA es el alterno 10 (7, 8 y 9 son IN-A / EG-A / DI-A)

El tipo APERTURA se creó el 28-oct-2026 en la base del proyecto con el alterno 8, el mismo que `EG-A (EGRESO
AUTOMATICO)` usa desde 2026-11-01 con `PLNTSSTM = 1` (`TipoAsientoAutomatico.EG_A`): con los dos en una empresa,
`selectByAlternoSistema(8)` devuelve dos filas y `tipoAutomatico()` falla en todos los egresos automáticos.
**Decisión del coordinador (28-oct-2026): APERTURA pasa al alterno 10**; `TipoAsientos.APERTURA = 10`.

- Base del proyecto (tiene APERTURA con 8): correr `sql/c5-01-apertura-alterno-10.sql` (SQL puro, controles antes
  y después, sin `COMMIT`). Es idempotente: si APERTURA no existe no hace nada; si está en 8 lo mueve a 10 (solo si
  el 10 está libre); si ya está en 10 no hace nada. Los asientos ya grabados no cambian (`ASNT` referencia
  `PLNTCDGO`).
- Base nueva / producción: el coordinador crea APERTURA directamente con 10 (`PLNTSSTM = 1`, activo); el script
  queda sin efecto.

Para cargar aperturas: `codigoAltTipoAsiento = 10` (opcionalmente `nombreTipoAsiento = "APERTURA"`). Si la base
todavía tiene APERTURA en 8, el cargador responde `no tiene un tipo de asiento activo con codigo alterno 10`:
correr el script primero.

## 8. Peticiones y respuestas

### 8.1 JSON — `POST /SaaBE/rest/asnt/cargarManuales`

```json
{
  "idEmpresa": 1236,
  "usuario": "CUADRE-SB",
  "nombreArchivo": "asientos-manuales-ejemplo-2025-06.csv",
  "codigoAltTipoAsiento": 0,
  "nombreTipoAsiento": "DIARIO",
  "contenido": "fecha;numero_asiento;glosa;cuenta;nombre_cuenta;debe;haber;documento_respaldo;centro_costo\n2025-06-20;AF-2025-06-001;Compra de equipo de computacion;160215;Equipo de computacion;1250.00;0.00;Factura 001-001-000012345;\n2025-06-20;AF-2025-06-001;Compra de equipo de computacion;1.1.02.05.40;Banco Amazonas cta. aho.;0.00;1250.00;Factura 001-001-000012345;\n2025-06-15;INV-2025-06-001;Colocacion de inversion renta fija sector financiero privado;1.2.01.05.02.17;Certificado de deposito 31 a 90 dias - Banco Pichincha;50000.00;0.00;Certificado No. 123456;\n2025-06-15;INV-2025-06-001;Colocacion de inversion renta fija sector financiero privado;1.1.02.05.40;Banco Amazonas cta. aho.;0.00;50000.00;Certificado No. 123456;\n"
}
```

`codigoAltTipoAsiento` y `nombreTipoAsiento` pueden omitirse (= DIARIO). Con PowerShell:

```powershell
$csv = Get-Content -Raw -Encoding UTF8 docs/logica-negocio/cnt/ejemplos/asientos-manuales-ejemplo-2025-06.csv
$body = @{ idEmpresa = 1236; usuario = 'CUADRE-SB'; nombreArchivo = 'asientos-manuales-ejemplo-2025-06.csv'; contenido = $csv } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/SaaBE/rest/asnt/cargarManuales -ContentType 'application/json; charset=utf-8' -Body $body
```

**Respuesta 200** (todo grabado):

```json
{
  "exito": true,
  "nombreArchivo": "asientos-manuales-ejemplo-2025-06.csv",
  "lineasLeidas": 4,
  "asientosLeidos": 2,
  "tipoAsiento": "DIARIO (alterno 0)",
  "errores": [],
  "asientos": [
    { "idAsiento": 10888, "numeroAlterno": "DIA-2025-06-0001", "numeroAsientoCliente": "AF-2025-06-001",
      "fecha": [2025, 6, 20], "lineas": 2, "debe": 1250.0, "haber": 1250.0 },
    { "idAsiento": 10889, "numeroAlterno": "DIA-2025-06-0002", "numeroAsientoCliente": "INV-2025-06-001",
      "fecha": [2025, 6, 15], "lineas": 2, "debe": 50000.0, "haber": 50000.0 }
  ]
}
```

(`fecha` sale como arreglo porque serializa Jackson, ver `CLAUDE.md` §Serialización; los ids y números son
ilustrativos.)

**Respuesta 400** (rechazo total, nada grabado), con `ejemplos/asientos-manuales-descuadrado.csv`:

```json
{
  "exito": false,
  "nombreArchivo": "asientos-manuales-descuadrado.csv",
  "lineasLeidas": 4,
  "asientosLeidos": 2,
  "tipoAsiento": "DIARIO (alterno 0)",
  "errores": [
    { "linea": 2, "numeroAsiento": "AF-2025-06-002",
      "mensaje": "el asiento no cuadra: debe 1250.00, haber 1250.01, diferencia 0.01 (2 lineas)." }
  ],
  "asientos": []
}
```

El asiento `INV-2025-06-002` del mismo archivo está bien y **tampoco** se registra. `linea` es la línea física
del archivo (la cabecera es la 1); `linea = 0` son errores de la petición o del archivo completo.

**Respuesta 500**: error de grabación después de validar (p. ej. la base no tiene las columnas `ASNTMDOR/NMMD/NAMD`
del script `e2-111`); la transacción se revirtió, no quedó ningún asiento del archivo.

### 8.2 Multipart — `POST /SaaBE/rest/asnt/cargarManualesArchivo`

Campos de formulario: `archivo` (el CSV), `archivoNombre` (con `encodeURIComponent`, como `CargaMarcacionesRest`),
`idEmpresa`, `usuario`, `codigoAltTipoAsiento` y `nombreTipoAsiento` (opcionales). Misma respuesta.

```bash
curl -F "archivo=@asientos-manuales-ejemplo-2025-06.csv" -F "archivoNombre=asientos-manuales-ejemplo-2025-06.csv" \
     -F "idEmpresa=1236" -F "usuario=CUADRE-SB" http://localhost:8080/SaaBE/rest/asnt/cargarManualesArchivo
```

## 9. Errores posibles (catálogo)

| Origen | Texto | Qué hacer |
|---|---|---|
| Petición | `El campo idEmpresa / usuario / nombreArchivo es obligatorio`, `El archivo esta vacio`, `No existe la empresa N` | Completar la petición |
| Tipo | `no tiene un tipo de asiento activo con nombre '…'` / `con codigo alterno N` / `El tipo de asiento es ambiguo` / `Ningun tipo … se llama '…'` | Crear el tipo en Contabilidad o indicar `nombreTipoAsiento` |
| Cabecera | `Falta la columna '…'` | Corregir la cabecera (separador `;`) |
| Línea | `numero_asiento vacio`, `fecha invalida`, `debe/haber invalido / negativo / con mas de dos decimales`, `valor en debe y en haber`, `debe y haber en cero` | Corregir la línea |
| Cuenta | `no existe en el plan`, `corresponde a N cuentas del SAA`, `esta inactiva`, `es de agrupacion … use una de sus cuentas de movimiento` | Usar la cuenta de movimiento del SAA (con puntos) |
| Centro | `el centro de costo '…' no existe / esta inactivo` | Corregir o dejar vacío |
| Asiento | `la fecha … no coincide`, `glosa vacia`, `no existe el periodo contable M/AAAA`, `esta MAYORIZADO`, `esta CERRADO`, `el asiento no cuadra` | Corregir el archivo o el período (C1: crear; mayorizado: desmayorizar) |
| Idempotencia | `El asiento X del archivo '…' ya esta registrado y activo` | Anular/reversar el existente, o cargar con otro nombre de archivo si de verdad es otro lote |
| Grabación (500) | `Error al registrar el asiento X … no quedo grabado ningun asiento` | Ver la causa (base, columnas, secuencias); reintentar |

## 10. Cómo cargar con esto la apertura de 2025-05 y los archivos mensuales del cliente

### Apertura al 31-may-2025 (C2)

El archivo es `cuadreSuper/04-entregables/balance-arranque/ASIENTO-ARRANQUE-31MAY2025.csv` (175 líneas, dos
`numero_asiento`: `APERTURA-2025-05` grupos 1–5 y `APERTURA-2025-05-ORDEN` cuentas de orden; columnas con
`cuenta_saa` y `cuenta_b17`). Requisitos: período mayo 2025 abierto y tipo APERTURA con **alterno 10** (el script 05
de cuadreSuper lo creó con 8 en la base del proyecto; `sql/c5-01-apertura-alterno-10.sql` lo mueve, §7).

```json
{ "idEmpresa": 1236, "usuario": "CUADRE-SB", "nombreArchivo": "ASIENTO-ARRANQUE-31MAY2025.csv",
  "codigoAltTipoAsiento": 10, "nombreTipoAsiento": "APERTURA", "contenido": "<el CSV completo>" }
```

Resultado esperado: dos asientos `APE-2025-05-0001` (169 líneas, 73.689.146,31) y `APE-2025-05-0002` (6 líneas,
79.061.904,25). **En la base del proyecto la apertura ya está cargada por SQL** (`CARGA-APERTURA-SAA.md`,
asientos 10886 y 10887 con número alterno `APE-2025-05-000x`): sus observaciones no llevan el prefijo
`CARGA MANUAL [...]`, así que la idempotencia **no** los detecta. Antes de cargar el CSV por aquí hay que borrar
los dos asientos del SQL (el `DELETE` de reversa documentado) o no cargar. En **producción** se usa uno de los
dos caminos, no los dos. Verificación: `cuadreSuper/03-trabajo/fase1-balance-arranque/scripts/08-verifica-apertura-saa.js`
(lee los asientos por `ASNTNMAL LIKE 'APE-2025-05-%'`, que el cargador también produce).

### Archivos mensuales de activos fijos / inversiones del cliente

1. El cliente entrega un CSV por mes (o por período) con la plantilla; `numero_asiento` con prefijo del tema
   (`AF-2025-06-001`, `INV-2025-06-001`) y `documento_respaldo` con el cuadro de depreciación, certificado o factura.
2. Cuentas: de movimiento del SAA (con puntos); el código del catálogo solo si es hoja de movimiento en el SAA.
3. Carga con tipo DIARIO (sin `codigoAltTipoAsiento` o con 0), `nombreArchivo` = nombre real del archivo
   (`AF-2025-06.csv`): es la clave de idempotencia y queda en cada asiento.
4. Si responde 400, se devuelve el archivo al cliente con la lista de errores (línea y mensaje); se corrige y se
   vuelve a cargar entero. Si responde 200, los asientos quedan en el período de su fecha, tipo DIARIO
   (`DIA-2025-06-nnnn`), y el mes sigue el flujo normal (mayorización, B17, cuadre).
5. Un archivo ya cargado no se puede cargar dos veces con el mismo nombre; para corregir un asiento grabado se
   anula o reversa en el SAA y se carga un archivo nuevo (`AF-2025-06-correccion.csv`).

## 11. Pendiente de probar en WildFly (criterio de aceptación de C5)

- `ejemplos/asientos-manuales-ejemplo-2025-06.csv` → 200 con 2 asientos DIARIO en junio 2025, cuadrados;
  `SELECT ASNTCDGO, ASNTNMAL, ASNTFCHA, ASNTOBSR FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-ejemplo-2025-06.csv]%'`.
- `ejemplos/asientos-manuales-descuadrado.csv` → 400 con el error de cuadre de `AF-2025-06-002` y **cero** filas nuevas.
- Repetir el primero → 400 por idempotencia.
- ⚠ La base del proyecto (dump del 1-oct) **no tiene** `ASNTMDOR/ASNTNMMD/ASNTNAMD` que la entidad `Asiento` ya
  mapea (script `e2-111`): sin ese DDL, cualquier `INSERT`/`SELECT` de `Asiento` por JPA falla con `ORA-00904`.
  Aplicar el DDL en la base del proyecto antes de probar.
