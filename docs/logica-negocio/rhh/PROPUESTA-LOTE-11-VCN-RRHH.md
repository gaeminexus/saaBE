# Lote 11 — RRHH dinero (VCN #3, #4, #5, #13): medición y propuesta

*2026-11-25 · omen-saa-1 · SIN programar: espera el OK del árbitro. Referencia leída en `F:\work\vcnBE` (b65a3d8, daaee99, f7e3f27, ce7b52b); no se copia a ciegas: SAA es de una sola empresa y su motor tiene diferencias que se anotan abajo.*
*SQL de medición (solo lectura): `rhh/sql/omen1-06-medicion-lote11-vcn-rrhh.sql`.*

## Principio común: lo ya calculado o pagado no cambia
Ningún cambio toca filas existentes de nóminas, renglones, acumulados ni liquidaciones: actúan solo al calcular o generar de nuevo (y los períodos PAGADO no se reabren;
`calcularPeriodo` exige un estado calculable). Los efectos hacia atrás son de **medición** (el SQL), no de corrección automática; si el SQL encuentra casos, se corrigen aparte con script autorizado.

## #3 Renta (RetencionRentaServiceImpl, GastoPersonalProyectadoServiceImpl, CargaFamiliarServiceImpl)
**Hallazgo en SAA (los tres confirmados).** (a) `obtenerRetencionMensual` devuelve la vigente de cualquier mes; `proyectarTodos` proyecta una vez desde el mes 1 y de ahí sale la
retención de todos los meses. (b) Guardar gastos o cargas no reproyecta. (c) `mesesRestantes = 12 − mesInicio + 1`, sin mirar el contrato.
**Propuesta.**
- (a) Si la vigente es de un mes distinto al del rol, proyectar en línea con el mes del rol (igual que VCN). Dependencia a respetar: la proyección lee los acumulados cerrados (ACMN) hasta el mes anterior; si el mes anterior no está cerrado, falta ese mes. Es la misma limitación de hoy y se documenta.
- (b) `RetencionRentaService.reproyectar(idEmpleado, anio, usuario)`: vuelve a proyectar con el mismo `mesDesde` las vigentes del empleado; si no se puede (p. ej. sin tabla del año) la desmarca para que el rol la genere. Se llama al grabar o borrar gastos (`saveSingle`, `save`, `remove`, el registro del resumen) y cargas familiares.
- (c) Portar de VCN solo lo que SAA necesita: `factorMes(contrato, año, mes)` (días del contrato en base 30; el mes de ingreso y de salida se prorratean), `mesesRestantes` = meses con factor > 0, ingresos y aporte futuros × la suma de factores. NO se porta la parte de «meses completos desde el ingreso» (R5-03/R5-04: es del cliente que llega a mitad de año a VCN; SAA tiene su carga histórica en ACMN).
**Efecto sobre lo ya hecho.** Nada se reescribe. Las PYIR existentes quedan como historia; cada cálculo nuevo desmarca la vigente e inserta otra (una fila por empleado por mes, ~24/mes). **Dinero:** en producción `RETENCION_IR = 0` en las 24 personas hasta agosto (`ESTADO-RRHH.md`), así que el cambio previsiblemente no mueve centavos hoy; el bloque 3a del SQL lo confirma con el acumulado real.
**Tamaño: media.** Archivos: `RetencionRentaService.java`, `RetencionRentaServiceImpl.java`, `GastoPersonalProyectadoServiceImpl.java`, `CargaFamiliarServiceImpl.java` (+ doc `ESTADO-RRHH.md`). **Riesgo:** medio (motor de renta sin pruebas automáticas).

## #4 Finiquito (LiquidacionHaberesServiceImpl)
**Hallazgo.** La remuneración pendiente es siempre `salario × min(día de salida, 30) / 30`, sin mirar si el mes de salida ya tiene rol; y ejecutar la salida no saca al colaborador de los períodos posteriores que ya lo tenían.
**Propuesta.**
- Remuneración pendiente: contar solo los días **posteriores al fin del último período con nómina vigente** del colaborador (no EXCLUIDA ni ANULADA) dentro del mes de salida; sin nómina en el mes, desde el día 1 (como hoy).
- Como esos días ya no van en la remuneración pendiente, el finiquito suma a los décimos proporcionales la **base del décimo tercero y los días trabajados de la nómina del mes de salida** (el rango de ACMN llega solo al mes anterior, así que esa nómina no estaba contada en ninguna otra parte).
- Al ejecutar la salida: `excluirEmpleado` de los períodos que **empiezan después** de la fecha de salida y donde ya tiene nómina, solo si el período es ABIERTO, EN_CALCULO o CALCULADO; si está más avanzado se deja constancia en el log para que se reabra.
**Efecto sobre lo ya hecho.** Liquidaciones ya calculadas/ejecutadas/pagadas no se recalculan ni se tocan. Solo cambian las que se calculen o ejecuten desde el despliegue. Los casos pasados de remuneración pagada dos veces o nóminas posteriores a la salida los miden los bloques 4a y 4b del SQL; si salen casos, la corrección es manual (script con respaldo) y la decide el usuario.
**Tamaño: media.** Archivos: `LiquidacionHaberesServiceImpl.java` (inyecta `ProcesoNominaService`; verificado: ninguno de los dos inyecta hoy al otro, no hay ciclo) + `rhh/API-LIQUIDACION-EXCOLABORADORES.md`. **Riesgo:** alto en el cálculo del finiquito (ya validado con ASOPREP en enero y agosto); se prueba con los casos de `ESPERADO-CONTRASTE-*` y sobre una copia.

## #5 Fondo de reserva (BeneficioSocialServiceImpl, LiquidacionBeneficioSocialRest)
**Hallazgo.** `generarFondosReserva` no llama `yaPagadaOEnOrden`, que sí usan D13 y D14 desde A0: un recálculo pisa una fila pagada o en una orden.
**Propuesta.** La misma guarda; el método devuelve `ResultadoGeneracionBeneficio` (generados, omitidosPagados) como los otros dos; el REST responde, como D13/D14 en SAA, el objeto `{"generados": n, "omitidosPagados": m}` con 200 (en SAA D13/D14 NO usan 409, a diferencia de VCN). Cambia la firma `int → ResultadoGeneracionBeneficio` (el único llamador es el REST) y la respuesta HTTP pasa de un número a ese objeto.
**Efecto sobre lo ya hecho.** Ninguno: solo evita pisar de aquí en adelante. El SQL 5a/5b mide si alguna fila ya se pisó (pagada con valor ≠ pagado) y cuántas están en una orden.
**Tamaño: chica.** Archivos: `BeneficioSocialService.java`, `BeneficioSocialServiceImpl.java`, `LiquidacionBeneficioSocialRest.java`. **Riesgo:** bajo.

## #13 Retiro de filas obsoletas (BeneficioSocialServiceImpl)
**Hallazgo.** El recálculo del D13, D14 y FR nunca retira la fila de un colaborador que ya no corresponde (p. ej. cesante, cuyo décimo ya fue en el finiquito): sigue en los listados y puede entrar a una orden de pago → doble pago.
**Propuesta (más estricta que VCN, a propósito).** `retiraObsoletas` borra solo las filas calculadas (estado ≠ 2), sin orden, sin valor pagado (`LQBSVLPG = 0`) y **no anticipadas** (`LQBSANTC ≠ 'S'`), del tipo y año generados, de colaboradores **sin contrato activo en el año** (CESANTE o sin contrato). VCN retira a todo el que no entra al bucle de generación, y eso en SAA retiraría también a quien pasó a MENSUALIZADO a mitad de año (su fila lleva el valor `LQBSVLMN` ya cobrado en el rol) y a las anticipadas pendientes que el finiquito gestiona (`liquidacionAnticipadaPendiente`). El D14 acota por región. El log informa cuántas retiró.
**Efecto sobre lo ya hecho.** Borra filas que hoy existen y están sin pagar (irreversible): por eso el SQL 13a las lista ANTES, para que el usuario las vea; y se propone una tabla de respaldo (`CREATE TABLE … AS SELECT` en el script de despliegue) o un modo `soloContar`. 13b busca el doble pago ya ocurrido (finiquito más beneficio pagado u ordenado).
**Tamaño: media.** Archivos: `BeneficioSocialServiceImpl.java` (+ doc). **Riesgo:** medio por ser un borrado.

## Pruebas (no hay tests automáticos en el repo)
Sobre una copia de producción: (1) renta: recalcular un mes con la vigente de enero y comparar `RETENCION_IR` antes y después (esperado 0 = 0); (2) finiquito: un caso con rol del mes ya calculado (esperado: remuneración pendiente de los días no cubiertos; sin pago doble) y un caso sin rol; (3) FR: generar con una fila pagada (esperado: omitidosPagados = 1 y la fila intacta); (4) retiro: generar D13 con un cesante sin pagar (esperado: fila retirada) y con una pagada (esperado: intacta).

## Qué necesito del árbitro
1. OK a las cuatro propuestas, en particular la regla **más estricta** del #13 y el respaldo antes del borrado.
2. Quién corre el SQL `omen1-06` en producción antes del despliegue (los bloques 4a, 4b, 5a y 13b pueden mostrar dinero ya pagado de más).
3. Confirmar con el FE que la respuesta de `generarFondosReserva` pase de un número a `{generados, omitidosPagados}` (igual que D13/D14); si no, se deja el número y las omitidas solo salen en el log.

---

## ESTADO: PROGRAMADO (2026-11-25, tras la aprobación del árbitro)
Compila (`mvn -q compile`); **sin probar** en WildFly ni contra Oracle. Decisiones del árbitro: #3, #4 y #5 como aquí; el FE ya espera `{generados, omitidosPagados}` en los tres generadores (el FR cambia de número a objeto, lo que arregla el «undefined» de la pantalla);
**#13: el código NO borra**. `LQBS` solo tiene estado 1 (calculada) y 2 (pagada), sin anulación ni columnas de usuario/fecha/motivo de retiro, así que los tres generadores solo **cuentan** las filas obsoletas y devuelven `retirables` e `idsRetirables` (con la regla estricta de arriba); el retiro físico va como SQL autorizado con respaldo: `rhh/sql/omen1-07-retira-beneficios-obsoletos-vcn13.sql` (respaldo CTAS, DELETE solo de lo respaldado, controles, sin COMMIT, reverso comentado).
Detalles de lo programado: `ResultadoGeneracionBeneficio` gana `retirables` e `idsRetirables`; «contrato activo en el año» es el de `selectActivosEnPeriodo` (quien se liquidó tiene fecha de terminación dentro del año y no cuenta). En #3 los gastos y cargas llaman a `reproyectar` sin hacer fallar el grabado si no se puede. En #4 el cambio de `diasTrabajados` alcanza también a la provisión de vacaciones del mes de salida (misma variable, como en VCN: los días que ya cubrió el rol ya acreditaron las suyas).

## Casos a contrastar del #4 antes de producción (copia de producción + ESPERADO-CONTRASTE-*)
Referencias: `ContratoEmpleadoDaoServiceImpl.selectActivosEnPeriodo` (dos colaboradores con salida a mitad de enero no están en la nómina de enero; otros dos con salida a inicios de marzo están en enero y febrero y no en marzo) y `ESTADO-RRHH.md` (verificación de producción del 2026-08-22). Los casos se identifican por la fecha de salida y por el código de empleado que el usuario tome de `RHH.LQDC` (por ejemplo «empleado A»); aquí no van nombres ni montos individuales.
1. **No debe cambiar (regresión):** los finiquitos de los **empleados A y B** (salida el 15-01 y el 16-01, sin nómina de enero): `cubiertoHasta` es nulo, `diasTrabajados` = 15 y 16 como hoy; remuneración pendiente, décimos proporcionales, vacaciones y neto IGUALES a los actuales (comparar renglón por renglón contra lo ya grabado en `RHH.TMLQ`).
2. **No debe cambiar:** los finiquitos de los **empleados C y D** (salida el 06-03, con nómina de enero y febrero). El último período con nómina es febrero (termina 28-feb, antes del 1-mar): `desdePendiente` = 1-mar, 6 días como hoy; el gravado acumulado de enero + febrero + finiquito de los dos debe coincidir con el que ya verificó producción (ver ESTADO-RRHH, 2026-08-22).
3. **Cambia (el caso del defecto):** salida el **31-01** de alguien que SÍ tiene nómina de enero calculada. Esperado: remuneración pendiente = 0 (antes 30 días = pago doble); `decimoTercero` = (acumulados + nómina de enero base) / 12 con la `baseDecimoTercero` de la nómina de enero; `dias14` suma `diasTrabajados` de esa nómina.
4. **Cambia:** salida a mitad de mes (p. ej. 15-ene) con nómina del mes ya calculada para el mes completo (el período cubre hasta el 31): remuneración pendiente = 0, no 15 días; contrastar contra lo que el rol ya pagó.
5. **Sin nómina del mes:** salida el 20-ene de alguien sin nómina de enero: igual que hoy (20 días).
6. **Nómina excluida o anulada no cuenta:** repetir el caso 3 con la nómina de enero en estado EXCLUIDA (6) o ANULADA (5): debe comportarse como el caso 5.
7. **Exclusión de períodos posteriores:** ejecutar la salida (APROBADA) de alguien con nómina en febrero (período CALCULADO) y salida en enero: su nómina de febrero queda EXCLUIDA (estado 6, totales 0); con febrero APROBADO el log dice «ya no admite exclusión: reabralo». Revisar que el período de enero (el mes de salida) NO se excluya.
8. **Diciembre con décimo anticipado:** una salida en diciembre con ajuste anticipado (e3-21): la lógica de `liquidacionAnticipadaPendiente` no debe cambiar su resultado.
