# Estructuras de renglones (C19) — B17 y flujo de caja como configuración

**2026-10-28 · tarea C19 del cuadre con la Superintendencia.** Inventario de reportes en
[REPORTES-SUPERINTENDENCIA.md](REPORTES-SUPERINTENDENCIA.md).

## 1. Problema

El motor de balances (CNT.RPRT + CNT.DTRP → CNT.DTMT, `TempReportesServiceImpl.reporteRangoFecha`) copia rangos
del plan de cuentas: cada renglón ES una cuenta del plan. El B17 no: sus renglones son el catálogo de la
Superintendencia (534 códigos sin puntos, `110105`), cada uno alimentado por varias cuentas del SAA con reglas
propias (el "7" es neto deudor, las 7.x van en valor absoluto, 32 cuentas "REQUIERE ASIGNACIÓN" van a un renglón
decidido a mano) y sale en un `.txt` de formato exacto. El flujo de caja tiene el mismo problema (renglones del
formato BIESS, totales por fórmula, saldo inicial a otra fecha).

## 2. Extensión genérica (C19-01)

| Tabla | Qué es |
|---|---|
| `CNT.RPRT` (+10 columnas opcionales) | `RPRTTPRP` 2 = estructura de renglones (NULL/1 = motor de siempre) · `RPRTCDES` código de estructura (B17) · `RPRTPRDC` periodicidad (M) · `RPRTCDEN` entidad (3968) · `RPRTBSVL` base del valor · `RPRTMPAU` mapeo automático · `RPRTACPF` acumulación por prefijo · `RPRTFRAR` formato de archivo (1 = Superintendencia) · `RPRTSMCT` suma de control en la cabecera · `RPRTTTLO` título oficial |
| `CNT.RNRP` renglón | código, nombre, nivel, orden, tipo de cálculo (1 suma · 2 neto deudor · 3 fórmula `+A -B`), base propia, ¿va al archivo?, vigencia desde/hasta (fecha de corte), estado |
| `CNT.CRRP` regla cuenta → renglón | cuenta del plan (con puntos), alcance (1 con subcuentas · 2 exacta), renglón destino (NULL = mapeo automático), signo (1 naturaleza · 2 deudor D−H · 3 acreedor H−D · 4 valor absoluto por cuenta), ¿se reporta?, vigencia, sustento |

Con todas las columnas nuevas en NULL los reportes existentes no cambian. `/dtmt/generarBalance` rechaza un tipo de
reporte que sea estructura (mensaje claro).

## 3. Cálculo (`com.saa.ejb.cnt.estructura.CalculadorEstructura`, clase pura)

1. Debe/haber por cuenta del plan (asientos ACTIVO y REVERSADO, como el balance), por cada base necesaria:
   1 saldo al corte (desde el inicio) · 2 movimiento del período (inicio..corte, por defecto el mes) ·
   3 saldo al día anterior al inicio.
2. Cada cuenta con movimiento se resuelve: gana la regla de cuenta **más larga** que la alcance; si varias reglas
   tienen exactamente esa cuenta, alimenta a todos sus destinos. Sin regla (o regla sin destino) y con
   `RPRTMPAU = 1`: renglón = prefijo más largo de la cuenta sin separadores (`1.1.01.05.01` → `110105`).
   `CRRPRPRT = 0` excluye a propósito (sale en advertencias si tiene saldo).
3. Aporte según el signo de la regla; además se acumula el neto deudor (D−H).
4. Renglón tipo 1: suma de aportes de los destinos que empiezan por su código (si `RPRTACPF = 1`) o del suyo;
   tipo 2: lo mismo con el neto deudor; tipo 3: fórmula sobre otros renglones (detección de ciclos).
5. Importes BigDecimal exactos, redondeo a 2 decimales por renglón; suma de control = Σ renglones del archivo.
6. Comparativo opcional (`fechaComparacion`): valor comparativo y variación por renglón.

Advertencias: cuentas con saldo sin renglón (solo en estructuras con mapeo automático, que cubren todo el plan) y
cuentas excluidas por regla.

## 4. Archivo (`ArchivoEstructura`)

Cabecera `ESTRUCTURA TAB ENTIDAD TAB dd/MM/aaaa TAB líneas(incl. cabecera) [TAB suma de control]`; una línea
`código TAB valor` por renglón con `RNRPRPAR = 1`; valor con 2 decimales y punto, `0` si es cero; CRLF sin salto
final; ISO-8859-1. Nombre `B17M3968ddMMaaaa.txt`. Es exactamente la regla de `09-genera-b17-desde-saa.js`.

## 5. Mes cerrado (C17) y modo preliminar

Las estructuras con archivo para la Superintendencia (B17) exigen el mes **cerrado para cartera y contabilidad**:
`EstadoCierreService.validarReporteFinanciero` (mismo servicio y mensaje `REPORTE_MES_NO_CERRADO` que las G).
Para el re-proceso, la solicitud admite `"preliminar": true`: genera sin cierre, la respuesta trae
`preliminar: true` y el archivo se llama `B17M3968ddMMaaaa_PRELIMINAR.txt` (no presentable, no tendrá `.ACU`).
El flujo de caja (sin archivo) no exige cierre.

## 6. Endpoints

- `POST /esrp/generar` → JSON: líneas (código, nombre, nivel, valor, comparativo, variación), suma de control,
  advertencias, nombre y contenido del archivo, `preliminar`.
- `POST /esrp/archivo` → el `.txt` (latin1, `Content-Disposition` con el nombre).
- Cuerpo: `{empresa, codigoEstructura | codigoAlterno, fechaCorte, fechaInicio?, fechaComparacion?,
  fechaInicioComparacion?, preliminar?}`.

## 7. Definiciones sembradas

- **B17** (C19-02, alterno 6): 534 renglones (catálogo de mayo 2025 en su orden; el "7" tipo 2), regla "7"
  valor absoluto, 31 asignaciones explícitas "REQUIERE ASIGNACIÓN" (propuestas de la fase 1, a decidir por
  ASOPREP) y 3.3.90.01 excluida. Generada por `C19-generar-semilla-b17.js` desde `MAPEO-PLAN-CUENTAS.csv` y
  `b17-mayo2025.csv`; el mapeo automático reproduce las otras 963 cuentas del mapeo (verificado). Cambio de
  catálogo de ene-2026 (+160225 +230501 +440525, −160290 −440520) en bloque comentado hasta confirmar la vigencia.
- **Flujo de caja** (C19-03, alterno 7, provisional): 26 renglones FCnn = filas del formato BIESS; saldo inicial
  con base 3, totales y saldo final por fórmula, control FC90/FC91 contra 1.1 al corte; solo las 9 reglas de la
  columna O del formato. Falta el mapeo de las filas de caja (archivo "FLUJO DE EFECTIVO_ENE_2026 FORMATO.xlsx").

## 8. Validación (contra la base de trabajo `saa-oracle-23ai`, 2026-10-28)

Harness fuera del servidor: lee la definición **de los INSERT de la semilla**, los saldos por cuenta con la
misma consulta del servicio, corre `CalculadorEstructura` + `ArchivoEstructura` y compara bytes.

| Corte | Resultado |
|---|---|
| 31/05/2025 | **Idéntico byte a byte** a `B17M396831052025.txt` presentado (6.534 bytes, suma 900.189.987,57, sin advertencias) |
| 30/06/2025 | Idéntico al producido por la lógica del script 09 (suma 928.925.293,90) |
| 30/09/2026 | Difiere del script 09 solo en `4`, `48` y la suma: la cuenta nueva 4.8.02.1 (12.751,99) entra por prefijo a `48`; el script la descartaba. Decidir su renglón |
| Flujo sep-2026 | Calcula; control FC91 = −900.998,65 porque faltan las reglas de caja (esperado) |
