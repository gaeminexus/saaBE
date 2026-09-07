# Formato del archivo de pagos por banco — Internacional y Pacífico

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-07 · **Fuente:** los dos documentos oficiales que
entregó el usuario, no una reconstrucción.

| Banco | Documento fuente | Qué es |
|---|---|---|
| **Banco Internacional** | `ARCHIVO_PLANTILLA_PAGOS_SHORT.xlsx`, hoja `ESTRUCTURA DE ARCHIVO` + hoja `PLANTILLA ROLES` (216 filas reales) | Especificación formal de 12 campos + datos reales de una corrida |
| **Banco del Pacífico** | `MANUAL_MACRO_EXCEL_BIZBANK_LIGHT.pdf` | Manual de la macro **BizBank Light v2.1**, con capturas de la hoja |

> ⛔ **Este documento reemplaza al formateador provisional.**
> `FormateadorArchivoBancoPlanoImpl` dice de sí mismo, en su javadoc, *«NO USAR EN PRODUCCIÓN»* y
> *«el formato oficial del banco todavía no fue entregado»*. Ya fue entregado: es esto.

---

## 0. ⛔ La diferencia que hay que entender antes de escribir una línea

**No son dos variantes del mismo archivo. Son dos cosas distintas.**

| | **Internacional** | **Pacífico** |
|---|---|---|
| Qué recibe el banco | **Nuestro archivo**, tal cual | Un `.BCP` **encriptado que genera una macro de Excel**, no nosotros |
| Qué producimos | El archivo final `.txt` | **La data para pegar en la hoja de la macro** |
| Se sube a | El portal del banco | Se pega en `BIZBANK_LIGHT.xls` → botón *Generar Archivo .BCP* → `C:\Pacifico\Archivos_Generados\PACIFIC.BCP` → Bizbank Net |

Consecuencia práctica: para el Pacífico **nuestro entregable es un `.xlsx`**, y el paso siguiente lo
hace una persona con la macro. Cualquier diseño que asuma «generamos el archivo del banco» para los
dos casos está mal desde el principio.

---

## 1. Banco Internacional — «Estructura de archivo CORTO (pagos, recaudaciones)»

### 1.1 Reglas del archivo

- **Texto separado por TABULACIONES.**
- **Codificación ANSI** (no UTF-8). En Java: `windows-1252`.
- **SIN fila de encabezado.** Textual: *«el archivo está constituido por la data "Pura" sin cabecera»*.
- **El número de columnas es FIJO: 12.** Un campo opcional va vacío, **pero su tabulador tiene que
  estar igual**. Doce campos ⇒ once tabuladores en toda línea, siempre.

### 1.2 Los doce campos, en orden

| # | Campo | Tipo | Long. mín | Long. máx | Oblig. | Valor que va |
|---|---|---|---|---|---|---|
| 1 | Código de orientación | Alfabético | 2 | 2 | Sí | **`PA`** (pagos). `CO` es recaudos, no aplica |
| 2 | Contrapartida | Alfanumérico | 1 | 20 | Sí | Identificador de la transacción. **No puede ir vacío ni en blanco** |
| 3 | Moneda | Alfabético | 3 | 3 | Sí | **`USD`** |
| 4 | Valor | Numérico | 1 | 13 | Sí | **11 enteros + 2 decimales, SIN separador.** `2458,79` va como `245879`. Debe ser mayor que 0 |
| 5 | Forma de cobro/pago | Alfabético | 3 | 3 | Sí | **`CTA`** (crédito a cuenta / interbancario). También `EFE`, `CHQ` |
| 6 | Tipo de cuenta | Alfabético | 3 | 3 | Sí/Opc | **`CTE`** corriente · **`AHO`** ahorros · `VIR` virtual. Opcional si la forma de pago es `EFE`/`CHQ` |
| 7 | Número de cuenta | Numérico | 1 | 20 | Sí/Opc | Cuenta destino. Opcional si la forma es `EFE`/`CHQ` |
| 8 | Referencia | Alfanumérico | 0 | 1000 | No | Descripción del pago |
| 9 | Tipo ID beneficiario | Alfabético | 1 | 1 | Sí | **`C`** cédula · **`R`** RUC · **`P`** pasaporte |
| 10 | Número de ID | Num+Letras | 5 | 15 | Sí | **`C` valida cédula de 10 · `R` valida RUC de 13 · `P` entre 5 y 15.** Sin espacios intermedios |
| 11 | Nombre del cliente | Alfanumérico | 1 | **41** | Sí | Nombre del beneficiario |
| 12 | Código de banco | Numérico | 1 | 13 | No | **Código del BCE para cámara.** Vacío ⇒ el banco asume **32**; `0032`, `032` y `32` son lo mismo |

### 1.3 Línea real, del archivo que entregó el usuario

```
PA→1→USD→45000→CTA→AHO→2203144910→TRABAJOS REALIZADOS ACTAS FINIQUITO→C→1311981953→JOHNNY CEVALLOS MONTENEGRO→10
```

*(`→` es el tabulador. `45000` son **$450,00**.)*

### 1.4 Tres trampas de este formato

1. **El valor va en centavos corridos.** Mandar `450.00` en vez de `45000` no da error de formato:
   el banco transfiere **cuatro centavos**. Es la falla más cara posible de este archivo.
2. **El nombre se trunca en 41 caracteres.** `RYC AUDITORES Y CONSULTORES CIA LTDA` entra; uno más
   largo hay que cortarlo **nosotros**, no dejar que lo corte el banco.
3. **Campo 12 vacío significa «Banco Internacional» (32), no «no sé».** Si no tenemos el código BCE
   del banco del beneficiario y lo mandamos vacío, **le estamos diciendo al banco que la cuenta es
   del Internacional.** Para una cuenta del Pichincha eso es una transferencia que rebota, o peor,
   una que cae en otra cuenta. **Ver §3: hoy no tenemos ese dato.**

---

## 2. Banco del Pacífico — macro BizBank Light v2.1

### 2.1 Cómo funciona el circuito

1. Se abre `BIZBANK_LIGHT.xls` y se habilita la macro (*Opciones → Habilitar este contenido*).
2. Pregunta si conservar la plantilla y si conservar los valores anteriores.
3. **Se llenan las filas de detalle desde la fila 16.**
4. Botón **Ver Totales** → muestra número de registros y total en dólares.
5. Botón **Generar Archivo .BCP** → `C:\Pacifico\Archivos_Generados\PACIFIC.BCP`.
6. Ese `.BCP` es lo que se sube a **Bizbank Net**.

### 2.2 Bloque de cabecera de la hoja (filas 4 a 7)

Lo llena la persona, no nosotros, pero la pantalla debe mostrarlo para que no lo invente:

| Rótulo | Ejemplo del manual |
|---|---|
| EMPRESA | `EMPRESA` |
| SERVICIO | **`RP`** (roles de pago) |
| FECHA DE PROCESO | `09-08-12` |
| FECHA DE VENCIMIENTO | `03-09-13` |
| TIPO DE CTA(Emp.) | **`00`** |
| NUMERO DE CTA(Emp.) | `19` |
| REFERENCIA | `PAGO` |
| NOMBRE ARCHIVO | `PACIFIC` |

### 2.3 Las catorce columnas de detalle, en orden (fila 15 = encabezados, datos desde la 16)

| Col | Encabezado en la hoja | Valor que va |
|---|---|---|
| **A** | Forma Pag/Cob | **`CU`** crédito/débito a cuenta · `CH` cheque · `EF` efectivo · `RE` recaudación |
| **B** | Banco | **Código de banco.** `30` = Banco del Pacífico, `25` = Banco de Machala |
| **C** | Tip.Cta/Che | **`00`** corriente · **`10`** ahorros |
| **D** | Num.Cta/Che | Número de cuenta a acreditar |
| **E** | Valor | **En dólares, CON decimales.** `100` es cien dólares |
| **F** | Identificación | **Opcional.** Solo obligatorio en órdenes de cobro (nº de autorización) |
| **G** | Tip.Doc. | **`C`** cédula · **`R`** RUC · **`P`** pasaporte |
| **H** | NUC | Número de cédula, RUC o pasaporte |
| **I** | Beneficiario | Nombres y apellidos, o razón social |
| **J** | Teléfono | Opcional |
| **K** | Referencia | Descripción breve. **⛔ Sin `ñ` `,` `.` `;` `-` `/`** |
| **L** | Base Imponible | Solo recaudación. Valor a retener sin IVA |
| **M** | Base IVA | Valor a efectuar la retención del IVA |
| **N** | Tipo | Solo recaudación. `B` bien · `S` servicio |

Para pagos (`CU`) las columnas **L, M y N van vacías**, y **F y J** son opcionales.

### 2.4 Filas reales, de las capturas del manual

```
CU  30  00  3235416   100.00  <vacío>  C  0915291611  ANDRES MEDINA  <vacío>  1ERA QUINCENA
CU  30  10  27199762  200.00  <vacío>  C  0912890415  JAVIER PEREZ   <vacío>  1ERA QUINCENA
```

Subtotales que muestra la macro: `USD · CU · CANT. 2 · TOTAL 300.00`.

### 2.5 Dos trampas de este formato

1. **El valor NO va en centavos** — al revés que el Internacional. En la misma pantalla van a
   convivir dos formatos con la regla opuesta: `$450,00` es `45000` para uno y `450.00` para el
   otro. **Equivocarse acá multiplica o divide por cien todos los pagos del lote.**
2. **La referencia rechaza caracteres comunes**, incluidos la **coma, el punto, el guion y la `ñ`**.
   Una observación escrita a mano como `Pago factura 001-002-000123, 1ra quincena` tiene coma y
   guiones. **Hay que sanear ese texto nosotros**, no confiar en que el usuario lo escriba limpio.

---

## 3. 🔴 El dato que NO tenemos, y sin él ninguno de los dos archivos sale bien

**Los dos formatos piden el código de la institución financiera del beneficiario** — campo 12 del
Internacional, columna B del Pacífico. **Ese dato no existe en la base.**

`TSR.BEXT` (bancos externos, que es a donde apunta la cuenta del beneficiario) tiene exactamente
cinco columnas: `BEXTCDGO` (PK), `BEXTNMBR`, `BEXTTRJT`, `BEXTESTD`, `BEXTFCIN`. **No hay código de
institución.**

### 3.1 Y no es un hueco nuevo: RRHH ya choca contra él, hoy, en producción

`GeneracionOrdenPagoServiceImpl:591-594`, comentario textual de quien lo escribió:

```java
case RhhCampoArchivoBancario.CODIGO_DEL_BANCO:
    // Sale del snapshot, que guarda el NOMBRE del banco: TSR.BNCO no tiene codigo
    // de institucion. Ver la nota de la clase.
    return texto(detalle.getBanco());
```

**El archivo bancario de la nómina está mandando el NOMBRE del banco donde el banco espera un
código numérico.** Está escrito en el código y nadie lo levantó como defecto.

> **Es exactamente la lección del §29 de nuestro estado:** el defecto que tenés en la mano casi
> nunca es el único. Acá el arreglo —una columna en `TSR.BEXT`— **cierra tres agujeros a la vez**:
> los dos formatos nuevos y el archivo de nómina que ya está saliendo mal.

### 3.2 El arreglo

`docs/logica-negocio/tsr/sql/e2-14-codigo-institucion-banco-externo.sql` agrega
**`TSR.BEXT.BEXTCDBC`** (código BCE de cámara).

⛔ **La carga de valores NO se inventa.** De los dos documentos salen **dos códigos verificados**:

| Código | Banco | Fuente |
|---|---|---|
| **30** | Banco del Pacífico | Manual BizBank Light, textual |
| **25** | Banco de Machala | Manual BizBank Light, textual |

Los demás (`10`, `17`, `32`, `36`, `213` aparecen en la muestra del Internacional **sin decir a qué
banco corresponde cada uno**) los tiene que confirmar el usuario contra la tabla oficial del BCE.
**Adivinarlos es mandar plata al banco equivocado.**

---

## 4. De dónde sale cada campo, en nuestro modelo

El pago (`PGS.PGTR`, `PagoProgramado`) tiene **dos caminos** para los datos del beneficiario, y el
formateador actual ya los distingue:

| Dato | Si hay cuenta de titular (`cuentaDestino`) | Si es beneficiario ocasional |
|---|---|---|
| Identificación | `pago.titular.identificacion` | `pago.beneficiarioIdentificacion` |
| **Tipo de ID** | `pago.titular.rubroTipoIdentificacionP` → rubro `TipoIdentificacion` | 🔴 **no existe campo** — ver §4.1 |
| Nombre | `pago.titular.nombre` | `pago.beneficiarioNombre` |
| Banco | `pago.cuentaDestino.banco` (`BancoExterno`) | `pago.beneficiarioBanco` (`BancoExterno`) |
| Tipo de cuenta | `pago.cuentaDestino.tipoCuenta` → rubro `TipoCuentasBancarias` | `pago.beneficiarioTipoCuenta` |
| Número de cuenta | `pago.cuentaDestino.numeroCuenta` | `pago.beneficiarioCuenta` |
| Valor | `pago.valor` | `pago.valor` |
| Referencia | `pago.observacion` | `pago.observacion` |

### 4.1 🟠 El beneficiario ocasional no tiene tipo de identificación

Los dos formatos exigen `C`/`R`/`P` **obligatorio**. Para el titular sale del rubro; para el
beneficiario ocasional **no hay de dónde sacarlo**.

**Se deduce por longitud, y se deja dicho que es una deducción:** 10 dígitos ⇒ `C`, 13 dígitos ⇒
`R`, cualquier otra cosa ⇒ `P`. Es la misma regla con la que el propio Internacional valida el campo
10 (§1.2), así que no es una invención nuestra: es la regla del banco leída al revés.

### 4.2 Mapeos de rubro a código de banco

| Nuestro rubro | Internacional | Pacífico |
|---|---|---|
| `TipoCuentasBancarias.CORRIENTE` (1) | `CTE` | `00` |
| `TipoCuentasBancarias.AHORROS` (2) | `AHO` | `10` |
| `TipoIdentificacion.CEDULA_IDENTIDAD` (1) | `C` | `C` |
| `TipoIdentificacion.RUC` (2) | `R` | `R` |
| `TipoIdentificacion.PASAPORTE` (3) | `P` | `P` |
| `TipoIdentificacion.IDENTIFICACION_DEL_EXTERIOR` (4) | 🔴 **no tiene equivalente** — rechazar el pago con mensaje claro, no mandar `P` a la buena de Dios | ídem |

---

## 5. Cómo se elige el formateador — el patrón ya existe, no se inventa uno

⛔ **No crear un rubro ni una columna de configuración para esto.**
`com.saa.ejb.tsr.parser.BankStatementParserFactory` ya resuelve el mismo problema para los extractos
bancarios: un mapa de **palabra clave → proveedor**, resuelto contra el nombre del banco normalizado
(sin tildes, en mayúsculas), y **falla explícitamente** si el banco no tiene implementación:

```java
throw new IllegalArgumentException(
    "No hay parser implementado para el banco '" + banco.getNombre() + "'. "
        + "Bancos soportados: " + PARSERS_POR_PALABRA_CLAVE.keySet());
```

Ese fallo explícito es lo que hace que enumerar acá sea **correcto** y no el antipatrón del §24 de
nuestro estado: el banco número N+1 **no pasa de largo en silencio**, choca con un mensaje que dice
qué falta. Se copia ese archivo tal cual, cambiando el tipo que devuelve.

---

## 6. Por qué NO se reutiliza el motor de formatos de RRHH

`RHH.FMBN` + `RHH.DFMB` son un motor **parametrizable** de archivos bancarios (delimitador,
codificación, plantilla de cabecera y pie, y por campo: orden, longitud, relleno, decimales,
separador decimal). Cubre **once de los doce** campos del Internacional. Es tentador y **no se usa**,
por tres razones concretas:

1. **No puede generar el Pacífico**, que no es un archivo delimitado sino una hoja de Excel.
2. Su resolución de campos (`resuelveCampo`) está escrita **sobre `OrdenPagoNomina`**. Reusarla para
   `PagoProgramado` obliga a abstraer la fuente de datos, o sea a refactorizar el archivo que hoy
   genera la nómina en producción. **Cambiar el motor de la nómina para estrenar un formato de CxP
   es poner en riesgo un proceso vivo por una elegancia.**
3. Le falta igual el campo **tipo de identificación**, y le falta el **mismo** código de banco.

**Queda anotado como deuda, no como error:** el día que haya un tercer banco con formato de texto
plano, la conversación correcta es generalizar `FMBN` y que la nómina y los pagos usen el mismo
motor. Hoy sería duplicar un concepto; mañana puede ser unificarlo.
