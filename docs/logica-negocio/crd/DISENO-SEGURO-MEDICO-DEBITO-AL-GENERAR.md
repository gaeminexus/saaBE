# DISEÑO — El seguro médico se debita del aporte 23 al GENERAR el seguro, no al pagar la pensión

omen-saa-1-arb · 2026-11-05 · Pedido del usuario (crédito). Ejecutor: omen-saa-1-be.

## 1. Pedido (palabras del usuario)

> «No se le está debitando de su saldo de pensión cuando se paga el seguro, sino cuando se paga la pensión,
> es decir, no se genera el aporte en negativo. El proceso debe debitar de la cuenta el momento que se ejecuta
> el proceso. Si ya se paga seguro médico, entonces ese valor debería debitarse en ese momento de las cuentas
> de los jubilados.»

## 2. Cómo está hoy (verificado en `PagoPensionComplementariaServiceImpl`, main 81332c7a)

| Momento | Qué hace | Dónde |
|---|---|---|
| Inicio de mes — `generarSeguroDelMes` → `generarSeguroIndividual` (REQUIRES_NEW, por jubilado) | Fija `PGPC.PGPCVLSG` (topado por saldo libre, §11.1), estado 6 SEGURO_GENERADO, `PGPCVLPN` null (= «stub»). **No crea ningún `CRD.APRT` negativo.** | ~991-1110 |
| Inicio de mes — mismo proceso, una vez | Orden de pago agregada al proveedor (`generarOrdenPagoProveedorSeguro`) **y asiento propio** D cuentas individuales / H seguros médicos por pagar (`generarAsientoSeguroMedico`, `CRJB.CRJBASSG`) | ~1252-1283 |
| Fin de mes — `generarPensionesDelMes` → `generarMesesRetroactivos` → `registrarPgpcDelMes` | Recién acá crea el APRT negativo del seguro: `crearMovimientoNegativo(..., glosa + " - SEGURO MEDICO")` con `traspasoSeguro` | ~2111-2115 |

Consecuencia: entre el inicio y el fin de mes **la contabilidad ya debitó cuentas individuales y el
proveedor ya tiene su orden, pero el saldo del aporte 23 del jubilado sigue intacto**. Módulo y mayor
descuadran todo el mes, y el saldo que ve el partícipe está inflado por el seguro.

## 3. Cómo debe quedar

1. `generarSeguroIndividual` crea, **en su misma transacción**, el APRT negativo tipo 23 por `seguroFijado`
   (si > tolerancia), con la misma glosa/tipo de movimiento que hoy usa `registrarPgpcDelMes` para el seguro,
   fecha del hecho = la que hoy usaría el proceso (`fechaDelPeriodo(anio, mes).atStartOfDay()`), y lo
   **enlaza al stub** de PGPC (ver §5, marca).
2. La corrida de pensiones, al completar un stub cuyo seguro **ya fue debitado**, **no crea el negativo del
   seguro otra vez** y **no lo vuelve a reservar** del saldo (el saldo ya lo refleja).
3. Stubs viejos (creados antes del despliegue, p. ej. **noviembre 2026, ya generado sin débito**) siguen por
   el camino de hoy: la corrida de pensiones les crea el negativo del seguro. Nada se duplica ni se pierde.
4. El tope del seguro (§11.1, H60) sigue igual en espíritu: no se fija ni se debita más de lo que el jubilado
   tiene libre. Con el débito inmediato, `seguroPendientePrevio` solo debe contar stubs **sin débito**
   (los viejos); los nuevos ya bajaron el saldo.

## 4. ⛔ Trampa principal: el ANCLA del retroactivo

`resolverAnclaRetroactivo` toma la fecha del **último APRT negativo tipo 23, cualquiera**. Si el seguro de
noviembre se debita el 05-11, la corrida de pensiones de fin de noviembre verá ancla = noviembre →
`AL_DIA` → **no paga la pensión de noviembre**. Y el propio `generarSeguroIndividual` del mes siguiente
también usa el ancla para decidir SIN_ANCLA/AL_DIA.

**Requisito:** el ancla del retroactivo NO puede moverse por un débito de seguro hecho por el proceso de
seguro. Hoy el comentario de `registrarPgpcDelMes` (~2100) documenta como «consecuencia querida» que el
traspaso del seguro sin certificado salda el mes. Ese criterio hay que conservarlo **para la corrida de
pensiones** (un mes procesado por pensiones queda saldado aunque solo haya salido el seguro), pero no para el
débito anticipado del proceso de seguro. El ejecutor propone el mecanismo verificando el código (opciones a
evaluar, no impuestas):
- (a) el negativo del seguro anticipado lleva un tipo de movimiento / marca que el DAO del ancla excluye, y la
  corrida de pensiones, al completar el mes, deja su propia marca de mes saldado (p. ej. el ancla pasa a
  considerar también PGPC completos, `PGPCVLPN` no nulo);
- (b) el ancla deja de leerse de APRT y se lee de PGPC (último período con `PGPCVLPN` no nulo), con el APRT
  como respaldo para jubilados sin PGPC.
Elegir la que cambie menos comportamiento y documentar por qué. **Probar mentalmente** los casos de §6.

## 5. Marca de «seguro ya debitado» en el stub

Hace falta distinguir stub viejo (sin débito) de stub nuevo (con débito). Preferir algo que **no requiera DDL**
(p. ej. `PGPC.PGPCIDAP` = código del APRT del seguro al crearlo; la corrida de pensiones hoy lo sobrescribe —
ver la limitación ya documentada de «un solo idAporte» en ~2130). Si la única solución limpia es una columna
nueva: **PARAR y reportar al árbitro** con el DDL propuesto; no se crea ninguna columna sin autorización del
usuario.

## 6. Casos que deben quedar bien (revisar uno por uno)

1. Jubilado normal, saldo suficiente: seguro 05-11 → APRT −seguro ese día; pensiones 30-11 → APRT −pensión,
   ningún segundo negativo de seguro; PGPC completo con VLSG y VLPN; ancla avanza a noviembre recién ahí.
2. Jubilado con meses retroactivos adeudados (ancla en meses anteriores): el débito del seguro de este mes no
   le cambia el ancla; la corrida de pensiones genera los meses adeudados como hoy.
3. Seguro topado por saldo (§11.1): se debita exactamente el valor fijado, nunca el nominal.
4. SIN_ANCLA / AL_DIA: seguro fijado 0 → no se crea APRT.
5. Stub viejo (noviembre 2026, ya generado sin débito): la corrida de pensiones lo debita como hoy.
6. Jubilado sin certificado bancario: el seguro ya salió al generar; la pensión queda retenida; ¿qué ancla
   queda? (hoy el traspaso del seguro salda el mes — mantener ese resultado al cerrar la corrida de pensiones).
7. Error a mitad del proceso de seguro: la transacción por jubilado (REQUIRES_NEW) revierte stub + APRT juntos.
8. **Anulación / reproceso**: `reprocesarPeriodo`, `reprocesarPagoJubilado`, `reprocesarDevengoJubilado`,
   `generarContraMovimiento` y cualquier anulación del seguro del período o de la corrida: si revierten un
   stub con débito, deben revertir también el APRT del seguro (contramovimiento), y no dejar un débito huérfano.
9. Bloqueo por partícipe H61 (`bloquearAportesEntidad`) antes de leer saldo y crear el negativo
   (INVARIANTE-SALDO-APORTES.md).
10. Previsualización de la corrida (`previsualizarCorrida`/`previsualizarJubilado`): que no reste el seguro dos
    veces para stubs ya debitados.

## 7. Fuera de alcance

- El asiento contable del seguro (ya se genera al generar el seguro: no se toca).
- La orden al proveedor (no se toca).
- Backfill de noviembre 2026: **no**. Noviembre se debita al pagar las pensiones (caso 5). Si el usuario
  quiere adelantarlo, será un script aparte.

## 8. Documentación

Actualizar en el mismo cambio `crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md` (qué hace cada proceso) e
`INVARIANTE-SALDO-APORTES.md` si cambia el criterio del ancla o de la reserva.
