# Dos correcciones de la jornada 2026-09-02

**Equipo:** CRD / Equipo B · **Estado:** aprobadas por el usuario, pendientes de implementar

Las dos son acotadas y no comparten archivo con la migración de la fase 3 (`b642be1`).

---

## 1. La glosa del asiento del cruce de valores no identifica a nadie

**Reporte del usuario, 2026-09-02:** *«el asiento no tiene en su observación el nombre del
partícipe, número de cédula, idasoprep, préstamo, etc.»*

### El defecto

`ContabilidadPrestamoServiceImpl.contabilizarPagoConAportes:116`:

```java
String prefijo = "Pago con aportes - evento " + ctx.getIdEvento();
...
asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, fechaCorte,
        prefijo + (ctx.getObservacion() != null ? ": " + ctx.getObservacion() : ""),
        ctx.getUsuario(), lineas, ...);
```

El asiento sale como **«Pago con aportes - evento 1234»** y nada más. Ese mismo `prefijo` se pasa
además a `lineasCruceAportesConsumidos` y a `haberDesdePagos` como descripción de **cada línea**,
así que el asiento entero es opaco: ni la cabecera ni los renglones dicen de quién es.

### Por qué acá importa más que en otros procesos

El asiento de Petro dice «Aplicación Petro/ARCH carga 449 - interés ordinario» y **está bien**: son
1.093 personas en un lote, no hay un partícipe que nombrar. El cruce de valores es lo contrario —
**un partícipe, un préstamo, una operación** — y es exactamente el caso en que contabilidad
necesita saber de quién es sin salir del asiento.

### El dato ya está en el método, no hay que ir a buscarlo lejos

`resultado.getIdPrestamo()` está disponible, y de ahí cuelga `Prestamo → Entidad`:

| Dato | De dónde sale |
|---|---|
| Nombre | `Entidad.razonSocial` (`ENTDRZNS`) |
| Cédula | `Entidad.numeroIdentificacion` (`ENTDNMID`) |
| Código asoprep | `Entidad.rolPetroComercial` (`ENTDRLPC`) |
| Préstamo | `resultado.getIdPrestamo()` |

### Qué hacer

- Construir el prefijo con los cuatro datos, p. ej.
  `"Cruce de valores - <razonSocial> (CI <numeroIdentificacion>, asoprep <rolPetroComercial>) - préstamo <id> - evento <idEvento>"`.
- Usarlo tanto en la **glosa del asiento** como en la **descripción de las líneas**.
- **Tolerar la ausencia de dato sin romper**: si la entidad o alguno de esos campos viene null, omitir
  ese fragmento — un asiento no se cae por una glosa. Pero **no** silenciar la falta del préstamo:
  si `resultado.getIdPrestamo()` es null en un cruce, eso sí es un fallo real.
- Recortar la glosa a lo que admite la columna del asiento; verificar el largo antes de concatenar.

### Fuera de alcance, pero anotado

`CobroCreditoServiceImpl.generarAsientoTransitorio` escribe **«Cobro crédito 45 - EFECTIVO»**, con el
mismo problema y también sobre operaciones individuales. **No se toca en este cambio** — es otro
proceso y merece su propia verificación. Queda registrado acá para que no se pierda.

---

## 2. La autocorrección de la carga Petro puede dejar mora sin cobrar

**Lo levantó el agente BE al implementar la fase 3, y es correcto.** No es un defecto nuevo: es un
defecto viejo que **la decisión de cobrar mora vuelve peligroso**.

### El mecanismo

`CargaArchivoPetroServiceImpl.calcularSaldosRealesCuota` (la que sobrevive, usada por
`buscarCuotaAPagar` para elegir a qué préstamo aplicar) calcula el pendiente **excluyendo la mora**,
igual que siempre. Su autocorrección marca la cuota como **PAGADA** cuando ese pendiente sin mora
llega a cero.

Pero el motor (`MotorPagoPrestamoServiceImpl`) sí tiene la mora en su prelación y vería esa misma
cuota como **PARCIAL**. Y una vez marcada PAGADA, **ni esa consulta ni la del motor la vuelven a
mirar**: la mora de esa cuota queda sin cobrar, para siempre, sin ningún aviso.

### Por qué ahora sí importa

Mientras nada cobraba mora por ese camino, marcar PAGADA una cuota con mora pendiente no cambiaba
ningún resultado. **Desde que el motor cobra mora, ese marcado le arrebata cuotas al cobro.** Muerde
justo la decisión que tomó el usuario hoy: parte de los **$1.284,42** de la carga 449 no se cobraría
igual, y —peor— sin dejar rastro de por qué.

### Qué hacer

- Que la autocorrección use el **mismo criterio de saldo que el motor** (mora e interés vencido
  incluidos) antes de declarar una cuota PAGADA. Una cuota con mora pendiente **no está pagada**.
- Verificar el efecto sobre `buscarCuotaAPagar`: al dejar de marcar PAGADA prematuramente, esas
  cuotas siguen siendo candidatas — que es lo correcto, pero hay que confirmar que no reabre cuotas
  que ya se liquidaron de verdad.
- Si el cambio hiciera que la carga deje de encontrar préstamo donde antes lo encontraba, **eso es un
  hallazgo, no un efecto a compensar**: reportarlo, no parchearlo.

### Verificación

Con la carga 449 reprocesada, `sql/167` bloque 4 debe dar `MORA_COBRADA` ≈ **1.284,42**. Si da menos,
quedaron cuotas marcadas PAGADA antes de tiempo y este defecto sigue vivo.

### Aclaración del usuario, 2026-09-02 — la cuenta de la mora

> *«como viste en el levantamiento se envía a la misma cuenta contable que el interés ordinario
> pero en la descripción se dice que es la mora»*

**No cambia nada de lo implementado, y conviene dejar escrito por qué.** `CobroPetroContableServiceImpl`
resuelve la línea por `AUX1 = 20` (`INTERES_MORA_POR_COBRAR`) y luego usa `lineaDesdePlantilla`, así
que **la cuenta sale de la plantilla**, no del código. Si esa línea apunta a la misma cuenta que la de
interés ordinario, el asiento sale con dos renglones a la misma cuenta y descripciones distintas —
exactamente lo descrito. El `sql/172` bloque 2 no listó la columna de cuenta, así que esto lo aporta
el usuario, no la medición.

⚠️ **Consecuencia para la pantalla de auditoría por bandas:** si esa pantalla agrupa **sólo por cuenta
contable**, mora e interés ordinario se van a fusionar en una sola fila y el desglose que contabilidad
quiere revisar desaparece. Debe agrupar por **concepto** (el `AUX1` de la plantilla), y mostrar la
cuenta como un dato más.
