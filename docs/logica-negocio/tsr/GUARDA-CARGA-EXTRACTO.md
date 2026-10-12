# Guarda de la carga de extractos: un extracto activo por cuenta y período, y nada de extractos vacíos

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-12 por `omen-saa-2-arb`, antes de despachar.
**Origen:** los resultados de `tsr/sql/e2-95` y `e2-97` (Pacífico 7713878, cuenta 419), corridos por el usuario el 2026-10-12.

## Lo medido

- La cuenta 419 tiene **cuatro extractos activos en agosto** (período 4518): 42, 56, 57 y 59. La 418 tiene
  dos (1 y 53).
- 42, 56 y 57 tienen **0 filas** en `TSR.DEXB`, pero el saldo inicial y el final son distintos
  (41.238,45 → 41.309,44): el banco sí tuvo movimientos y el parser no los reconoció. El extracto bueno es el
  59, con 1 fila (crédito de 70,99), que es el que está conciliado.
- En el código (`ImportacionExtractoBancarioServiceImpl`, verificado el 2026-10-12):
  - `ejecutarImportacion` (que usan `confirmar` y `recargar`) **solo** rechaza un archivo con el mismo
    hash (`selectByHash`). Un archivo bajado otra vez del banco tiene otro hash y entra como un extracto más
    del mismo mes.
  - No rechaza un archivo del que el parser no sacó ninguna fila.
  - `recargar` reemplaza el de código mayor (`selectByCuentaYPeriodo`, `order by codigo desc`); los otros
    quedan.

## La regla nueva

**G-A. Un solo extracto activo por cuenta y período.**
- `confirmar` (la carga normal) rechaza la carga si `selectByCuentaYPeriodo(idCuentaBancaria, idPeriodo)`
  devuelve un extracto. Mensaje:
  *«Ya existe un extracto cargado para esta cuenta y este período (extracto #N, «archivo»). Para
  reemplazarlo use «Recargar extracto».»*
- `validar` muestra lo mismo **antes** de confirmar. El usuario se entera en la vista previa y no al final.
- `recargar` **no** cambia: borra el anterior e importa el nuevo. Se reordena para que la guarda G-A no
  la frene: la validación va en `confirmar`/`validar`, **no** dentro de `ejecutarImportacion`.

**G-B. No se carga un extracto sin movimientos cuando los saldos dicen que los hubo.**
- Si el parser devuelve 0 filas **y** el saldo inicial es distinto del saldo final (con la tolerancia que ya
  se use en ese servicio para comparar montos, o 0,005 si no hay ninguna), se rechaza en `validar`,
  `confirmar` y `recargar`:
  *«El archivo no tiene movimientos reconocibles, pero el saldo inicial (X) y el final (Y) son distintos.
  Revise que el formato del archivo sea el que entrega el banco para esta cuenta.»*
- 0 filas con saldos iguales (o nulos) **sí** se acepta: un mes sin movimientos es legítimo. Va como
  advertencia en el resumen («El extracto no tiene movimientos»).

**Respuesta HTTP.** Las dos guardas lanzan `IncomeException`, y desde `f3473cab`/`19363599` el REST
responde **400 con el motivo** y la pantalla lo muestra (FE `d85c946`). El **contrato no cambia**: mismos
endpoints, mismo cuerpo, solo nuevos motivos de 400.

## Lo que NO se hace en este cambio

- No se limpian los extractos que ya están duplicados (419: 42, 56 y 57; 418: el que corresponda). Es un
  script aparte, después.
- No se agrega un índice único en BD: con los duplicados existentes fallaría, y la regla depende del estado
  (`EXBCESTD`).
- No se investiga por qué el parser no reconoció las filas de 42, 56 y 57. Si el ejecutor ve la causa
  leyendo el código, la reporta; no la arregla en este cambio.
