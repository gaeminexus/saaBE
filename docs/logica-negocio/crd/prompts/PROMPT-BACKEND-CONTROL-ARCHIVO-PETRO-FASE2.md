# PROMPT — Agente BACKEND · Fase 2 (complemento): control de archivo Petro y desglose de aportes

> **Etiqueta: BACKEND** (repo `saaBE`). Complemento corto de la Fase 2, que ya está
> implementada, desplegada y con sus tablas creadas en local, pruebas y producción.
> **No rehagas nada de la Fase 2**: esto añade una validación y un desglose.

---

## De dónde viene esto

Al entregar la Fase 2 dejaste marcado como PENDIENTE DE VALIDAR que el lado aportes de ⑥
calcula `esperado − registrado` porque no existe documento de planilla de aportes emitida.
El usuario decidió el 2026-08-25 (decisión **D13** del levantamiento): **se mantiene ese
cálculo, pero con un control bloqueante**. Sus palabras: *"ese control es extremadamente
necesario"*.

**El riesgo, medido en la BD real:** el aporte mensual esperado es 121.160,97 (1.647
partícipes activos). En agosto 2026 había 5.499,75 registrados — porque **el archivo Petro
de agosto no estaba cargado**: `CRD.CRAR` tenía los meses 1 a 7 de 2026 y ningún 8. Cerrar
agosto en ese momento habría reversado 115.661,22 como "no cobrado" siendo cobrable. Los
meses ya cargados registran ~121 mil, coherentes con el esperado.

Contexto completo en `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §5.10 y decisión D13.

## 1. Control: no se cierra un mes sin su archivo Petro

`CRD.CRAR` (`com.saa.model.crd.CargaArchivo`) lleva `CRARANAF` (año de afectación) y
`CRARMSAF` (mes de afectación). Antes de cerrar el mes M del año Y hay que exigir que exista
la carga de **ese mismo mes de afectación** en estado de carga completa.

- **Determina el estado correcto contra los datos, no por suposición.** Al 2026-08-25 las
  siete cargas de 2026 estaban en `CRARESTD = 3`. Existe el rubro
  `com.saa.rubros.ASPEstadoCargaArchivoPetro` (CARGADO 1, VALIDADO 2, APROBADO_CONTABILIDAD
  3, PROCESADO 4), pero es del módulo asoprep: **verifica si `CRD.CRAR.CRARESTD` usa
  realmente ese catálogo** antes de referenciarlo, y si no, documenta qué significa el 3 en
  esta tabla y usa una constante propia. No dejes el número suelto en el código.
- **En `previsualizar`: advertencia, no bloqueo.** Previsualizar un mes sin archivo debe
  poder hacerse (sirve para ver el estado), pero tiene que salir en `advertencias` con el
  aviso explícito de que el lado aportes está incompleto y **cuánto** está en juego: el
  esperado, el registrado y la diferencia.
- **En `ejecutar`: bloqueo.** `IncomeException` con un mensaje que diga qué falta y qué
  hacer, en la línea de: *"No se puede cerrar 2026-08: no hay archivo Petro cargado con mes
  de afectación 8 de 2026. Cargue el archivo del mes antes de ejecutar el cierre."*
- Piensa si el bloqueo debe poder saltarse (por ejemplo, para un mes en que legítimamente no
  hubo archivo). **Si añades una forma de saltarlo, que sea explícita, quede registrada en
  la corrida y salga en el resultado** — nunca un silencio.

## 2. Desglose del lado aportes en la previsualización

Hoy el sub-proceso ⑥ devuelve el neto y no se ve de dónde sale. Añade al resultado de la
previsualización, para el lado aportes, los tres componentes por separado: **esperado**,
**registrado** y **diferencia** (más el número de partícipes que entran en el esperado, que
ya lo devuelve `selectAporteMensualEsperado`). Contabilidad tiene que poder ver el origen
del número antes de autorizar, no recibir un total opaco.

Aprovecha para dejar visible en ese mismo desglose el caso de **exceso de cobro**: cuando lo
registrado supera a lo esperado, la diferencia es negativa y el código la lleva a cero con
`Math.max`. Eso no es un error —lo resuelve el proceso de cobro en exceso, §3.7 del
levantamiento— pero **hoy desaparece sin dejar rastro**, y debería verse. En julio 2026 el
registrado fue 156.797 contra 121.161 esperados.

## 3. Lo que NO cambia

- **Las bandas se siguen cortando por DÍAS.** El usuario lo validó con la Superintendencia
  de Bancos y esa fue su orden expresa (decisión **D12**). Que doce cuotas mensuales se
  repartan 1, 1, 3, 6, 1 entre las cinco bandas **no es un defecto y no se toca**.
- No se crea la planilla de aportes emitida (opción C de D13): evaluada y pospuesta.
- No se cambia el cálculo `esperado − registrado` ni el piso en cero.

## 4. Entrega

Actualiza `API-CIERRE-CARTERA.md` con los campos nuevos del desglose y con el error nuevo de
`ejecutar`, en el mismo cambio. Prueba contra la BD local: la previsualización de **agosto
2026** debe advertir (no hay archivo del mes 8) y la de un mes con archivo cargado no. Pega
los resultados reales en tu informe. Y avisa si el cambio del contrato obliga a tocar la
pantalla, que se está construyendo en paralelo.
