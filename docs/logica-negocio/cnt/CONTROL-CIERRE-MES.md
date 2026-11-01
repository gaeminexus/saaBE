# Controles de cierre de mes (semáforo) y pantallas del pase a producción

Rama `cuadre/pase-pantallas-controles` (saaBE y saaFE), 29-oct-2026. Cubre las brechas del expediente
`01-seguimiento/PASE-A-PRODUCCION.md`: I-1 (FE de C17), I-4 (cierre de cartera solo para el usuario 1),
I-6 (regenerar G desde pantalla), los avisos genéricos de §2 y B-1..B-3 (controles del expediente que pasan al
SAA). **No** integra C19b: los endpoints `/esfo` y `/esrp` siguen como están.

## 1. Endpoint

`GET /prdo/controlCierre/{idEmpresa}/{anio}/{mes}` → lista de controles, en orden de presentación:

```json
[{ "grupo": "PUENTES", "codigo": "PUENTE_1.4.05.05", "descripcion": "...",
   "valor": 0.0, "esperado": 0.0, "diferencia": 0.0, "estado": "OK", "detalle": "..." }]
```

- `estado`: `OK` (|valor − esperado| < 0,005), `ERROR` (hay diferencia) o `ADVERTENCIA` (no se pudo medir o
  es un conteo informativo). `valor`/`esperado`/`diferencia` pueden venir nulos en las advertencias.
- Solo lectura (`ControlCierreServiceImpl`, consultas nativas). No escribe ni bloquea.
- Errores: 400 con el mensaje si faltan empresa/año/mes; 500 si algo inesperado.

Regla de saldo (la misma de la cuadratura C14 y del cierre de cartera): asientos `ASNTESTD IN (1, 3)` de la
empresa con `ASNTFCHA <= fin de mes`; saldo **deudor** (debe − haber).

## 2. Controles

| Grupo | Código | Qué compara | Estado |
|---|---|---|---|
| (a) PUENTES | `PUENTE_1.4.05.05`, `PUENTE_2.3.02.05` | Saldo de la cuenta y sus subcuentas = 0 (D-024) | ERROR si ≠ 0 |
| (a) PUENTES | `PUENTE_1.4.05.10_2.3.02.10` | Saldo deudor 1.4.05.10 = saldo acreedor 2.3.02.10 (con D-032, tras el ⑥ del cierre de cartera las dos en 0: `crd/CIERRE-CARTERA-A-FECHA-DE-CORTE.md` §1.9) | ERROR si ≠ |
| (b) ECUACION | `ECUACION_CONTABLE` | Activo (1) = pasivo (2) + patrimonio (3) + resultado (5 − 4) | ERROR si ≠ |
| (c) CARTERA | `CARTERA_GRUPO_Q/P/H` | Contabilidad 1.3 sin 1.3.99 por grupo (Q = 1.3.01–04, P = 05–08, H = 09–12, la agrupación del ⑨) vs la distribución del módulo que dejó la corrida de cierre de cartera **vigente** del mes (`CRD.BDCC`, snapshot de `distNueva`). La diferencia es la que el ⑨ deja en la banda .25 | ERROR si ≠ |
| (c) CARTERA | `CARTERA_NO_CERRADA` | El mes no tiene corrida CRCT EJECUTADA vigente: se informa el capital contable por grupo | ADVERTENCIA |
| (c) CARTERA | `CARTERA_SIN_CUENTA` | Capital del módulo en bandas sin cuenta 1.3 | ADVERTENCIA |
| (d) BANCOS | `BANCO_<cuenta>` | Por cada cuenta bancaria real (`TSR.BNCO.BNCOREAL = 1`, `CNBCESTD = 1`): saldo contable de su cuenta vs saldo final del extracto vigente (`EXBCESTD = 1`) del período del mes con la fecha "hasta" más reciente | ERROR si ≠; ADVERTENCIA sin extracto |
| (e) ASIENTOS | `ASIENTOS_DESCUADRADOS` | Asientos 1/3 del mes con debe ≠ haber (lista hasta 20 códigos) | ERROR si hay |
| (e) ASIENTOS | `ASIENTOS_BORRADOR` | Asientos del mes en estado 4 (INCOMPLETO) | ADVERTENCIA si hay |
| (e) ASIENTOS | `ASIENTOS_SIN_LINEAS` | Asientos 1/3 del mes sin líneas | ADVERTENCIA si hay |
| (f) B17_G | `B17_OFICIAL` | Integridad del archivo B17 (líneas, suma de control, padres = hijas): **pendiente C19b** | ADVERTENCIA siempre |
| (f) B17_G | `B17_21_VS_G42` | −saldo 2.1 (cuenta 21 del B17) = Σ G42 (saldos patronal + personal + voluntario + rendimiento) | ERROR si ≠ |
| (f) B17_G | `B17_13_VS_G48` | 1.3 sin 1.3.99 (13 − 1399 del B17, cartera bruta) = Σ G48 por vencer + vencido | ERROR si ≠ |
| (f) B17_G | `B17_1399_VS_G48` | −1.3.99 = Σ provisión constituida G48 (D-015) | ERROR si ≠ |
| (f) B17_G | `G_NO_GENERADAS` / sin G42 o G48 | No hay ejecución OK de la estructura en el mes | ADVERTENCIA |

Notas:
- (f) toma la lógica de `03-trabajo/fase1-balance-arranque/scripts/10-cuadratura-b17-g.js` del expediente con
  los datos que ya están en el SAA. El plan de cuentas sigue el catálogo de la Superintendencia (21 = 2.1,
  13 = 1.3, 1399 = 1.3.99; verificado contra el B17 de mayo 2025: 21 = 52.844.299,10 = −saldo 2.1 al 31-may).
  La G vigente es el `RPR.EJRD` OK de mayor código del mes (la última regenerada o corregida).
- (c) no recalcula la cartera: usa lo que el cierre de cartera ya grabó. Si el mes no está cerrado para
  cartera, es advertencia (cerrar la cartera es requisito del cierre contable, C17).
- (d) depende de que el extracto del mes esté importado en Tesorería. Diferencia = partidas en tránsito o sin
  registrar (revisar la conciliación).

Ejemplo real (junio y mayo 2025, base de trabajo): expediente
`03-trabajo/meses/2025-06/resultados/27-control-cierre-ejemplo.txt`.

## 3. Pantalla (saaFE)

Contabilidad > Procesos > **Cierre de Períodos**:
- Botón de semáforo (`traffic`) en cada fila y tarjeta **Controles de cierre** con el estado general (peor
  estado), conteo y la tabla de controles (valor, esperado, diferencia, detalle).
- **Cerrar período** consulta los controles primero. Sin ERROR, sigue al diálogo de motivo (con el número de
  advertencias). Con algún ERROR, o si la consulta falla, pide una **confirmación explícita** ("Cerrar con
  diferencias") y el diálogo de motivo recuerda que hay que justificarlo (el motivo queda en `CNT.HCPR`).

## 4. Otras piezas del pase en la misma rama

- **I-1**: saaFE integra `cuadre/c17-pantallas-cierres` (`6a6000c`, incluye `7a44a07` de C19 con `/esfo`).
- **I-4**: la ruta `menucreditos/cierre-cartera` ya no usa `usuarioUnoGuard`; la opción de menú sale para todos
  con el permiso 1412. Reabrir cartera sigue pidiendo 1561.
- **I-6**: Reportes > Créditos > Super de Bancos: botón **Regenerar G** cuando el mes seleccionado ya tiene
  ejecución (consultada o respuesta "ya fueron generados"). Confirma y manda `regenerar: true` a
  `POST /ejrc/ejecutar`. El backend no cambia: valida el mes cerrado para cartera **antes** de mirar
  `regenerar` (`GeneracionReportesServiceImpl.java:54`), así que regenerar un mes abierto responde 400
  `REPORTE_MES_NO_CERRADO` y la pantalla muestra ese texto. Regenerar no mueve el "próximo período a generar".
- **Avisos genéricos → mensaje del backend**: tope de afectación manual (prevuelo y guardar afectaciones, en
  `detalle-consulta-carga`), Balance General (`reporte-balance.service.ts` ya no se traga el error) y confirmación
  en lote de la conciliación (el motivo por partida).

## 5. Opción: bloquear el cierre con ERROR (no implementada)

Hoy el semáforo **avisa y pide confirmación**; no bloquea. Si ASOPREP decide bloquear:
1. En `CierrePeriodoContableServiceImpl.cerrar`, llamar a `ControlCierreService.controlar` y lanzar
   `IncomeException("CONTROL_CIERRE: ...")` si algún control de una lista configurable (p. ej. `PUENTE_*`,
   `ECUACION_CONTABLE`, `ASIENTOS_DESCUADRADOS`) está en ERROR.
2. Permitir la excepción con un parámetro explícito (`forzar=true` + motivo, permiso propio) para meses con
   diferencias decididas por el responsable de ASOPREP (regla 6 del contrato: constancia escrita).
3. Decidir qué controles bloquean: los de cartera y B17 ↔ G dependen de decisiones D-011 y de C19b; los
   bancos, de que el extracto esté importado. Recomendación: bloquear solo puentes, ecuación y descuadrados.

## 6. Permisos que hay que asignar en producción

El FE protege las pantallas contables con el **árbol de permisos** (`SCP.PJRQ`, servicio `PermisosService`,
`GET /usro/verificaPermiso/{empresa}/{usuario}/{permiso}`): el menú verifica el permiso antes de navegar y las
acciones sensibles lo verifican antes de abrir el diálogo. Las rutas solo exigen sesión (`authGuard`). Si el
interruptor del rubro 7 ("MANEJA PERMISO") está en 0, no se valida nada.

| Permiso | Nodo | Dónde se usa | Rol sugerido |
|---|---|---|---|
| 1558 | CONTABILIDAD > PROCESOS > CIERRE DE PERIODOS | Menú de la pantalla de cierre de períodos | Contador |
| 1559 | … > CERRAR PERIODO | Botón Cerrar período | Contador |
| 1560 | … > REABRIR PERIODO | Botón Reabrir período | Contador jefe / Gerente (reapertura con motivo) |
| 1412 | CREDITOS > PARAMETRIZACION > CIERRE DE CARTERA | Menú de Cierre de Cartera (previsualizar, ejecutar) | Contador |
| 1561 | … > CIERRE DE CARTERA > REABRIR CARTERA | Reabrir la cartera del mes | Contador jefe / Gerente |
| 1494 | REPORTES > CREDITOS > SUPER DE BANCOS | Generar y regenerar G40–G51 | Contador |

Pasos: correr `docs/logica-negocio/seguridad/sql/C17-FE-01-permisos-cierres.sql` (crea 1558–1561), y asignar
los nodos al rol del contador desde la pantalla de seguridades (o por SQL revisado). Comprobar con un usuario
que no sea el 1: entrar a Créditos > Parametrización > Cierre de Cartera y a Contabilidad > Procesos > Cierre
de Períodos.
