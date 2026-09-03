# El asiento de entrega pasa a la parametrización dinámica de bandas

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** decisión tomada, pendiente de implementar

> **Decisión del usuario, 2026-09-02:** *«Ahora entiendo el punto 1 y la decisión es plantillas por
> banda dinámica»*, sobre el hallazgo de que el asiento de entrega no lee la parametrización.

---

## 1. El defecto

`ContabilidadPrestamoServiceImpl:601`, en el asiento de entrega del préstamo:

```java
int indice = (dias <= 30) ? 0 : (dias <= 90) ? 1 : (dias <= 180) ? 2 : (dias <= 360) ? 3 : 4;
```

El comentario de al lado lo declara: *«NO es el modelo dinámico de bandas de
`ClasificadorBandaService` (CRD.BNDP)»*. La razón que se dio es que las plantillas de entrega
—**9** prendario, **13** hipotecario, **34** quirografario— traen cinco cuentas fijas en `aux1` 1–5.

**Hoy los dos criterios coinciden, así que no se nota.** El día que se reconfiguren las bandas
—seis tramos, o cortes en 45 y 120 días— la parametrización cambia para el cobro y **no** para la
entrega: el mismo préstamo queda clasificado de una forma al entrar y de otra al cobrarse, **sin un
solo error en ningún log**. Es la peor clase de defecto: sólo se dispara el día que alguien toca la
configuración, cuando ya nadie recuerda por qué estaba así.

---

## 2. ⛔ El nombre de la decisión engaña: NO hay que agregar líneas a las plantillas

Esto es lo que más trabajo ahorra, y hay que entenderlo antes de tocar nada.

**La cuenta de una banda no vive en la plantilla: vive en la propia banda.** `CRD.BNDP` tiene
`PLNNCDGO` (`BandaProducto.planCuenta`). Y el cobro **ya resuelve la cuenta desde ahí**, sin pasar
por ninguna plantilla — `ContabilizacionIndividualCreditoServiceImpl.lineaBandaCapital:196`:

```java
ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(...);
BandaProductoDetalle banda = resultado.getBanda();
if (banda.getIdPlanCuenta() == null) {
    throw new IncomeException(... "la banda " + banda.getNumero() + " del producto " + idProducto
            + " no tiene cuenta contable asignada en CRD.BNDP.");
}
PlanCuenta cuenta = planCuentaDaoService.selectById(banda.getIdPlanCuenta(), ...);
```

Ese método es exactamente lo que la entrega necesita, **ya escrito, ya en producción y ya usado por
Petro, el cobro individual y el abono a capital**. La entrega es el único proceso que se quedó afuera.

Entonces la implementación de la decisión es:

- La entrega **deja de repartir sobre cinco líneas de plantilla** y pasa a clasificar con
  `ClasificadorBandaService`, tomando la cuenta de la banda.
- Las líneas `aux1` 1–5 de las plantillas 9, 13 y 34 **quedan sin uso**.
- Las plantillas conservan sus líneas que **no** son de banda: cuenta de orden, documentos en
  garantía, socios por pagar (en la 34, los `aux1` 6, 7 y 8).

⚠️ **El único ajuste real sobre `lineaBandaCapital`: la entrega va al DEBE.** Ese método escribe
`valorHaber` porque el cobro **reduce** el activo; la entrega lo **aumenta**. Hay que parametrizar el
lado, no duplicar el método — duplicarlo sería crear la segunda escalera que este cambio viene a
eliminar.

⚠️ **Y la fecha de corte de la entrega no es la de hoy:** los días se cuentan desde
`prestamo.getFechaInicio()` hasta el vencimiento de cada cuota, que es lo que hace el código actual
y hay que preservar. Es una entrega, no un cobro: todas las cuotas están por vencer.

---

## 3. ⛔ Prerrequisito duro: toda banda usada debe tener cuenta asignada

`lineaBandaCapital` **lanza `IncomeException`** si la banda no tiene `PLNNCDGO`. Y en el otorgamiento
eso es rollback: **el préstamo no se entrega.**

Hoy ese `throw` es inalcanzable desde la entrega porque la entrega no llama a ese camino. **El
momento en que se conecte es el momento en que se vuelve alcanzable** — la misma trampa que la línea
de mora de la plantilla 21 y que el `.jasper` faltante.

**`sql/176` lo verifica, y se corre ANTES de desplegar.** Si devuelve filas, hay productos con bandas
sin cuenta y **no se sube**: se asignan primero.

---

## 4. Qué NO se toca

- **`ClasificadorBandaService` y `CRD.BNDP`.** Se leen, no se modifican.
- **Las líneas no-banda de las plantillas 9, 13 y 34.** Siguen igual.
- **Los otros procesos.** Ya clasifican bien; este cambio los alcanza para igualarlos, no para
  cambiarlos.
- **Las líneas `aux1` 1–5 de las plantillas**: se dejan **en la base**, no se borran. Si el cambio
  hubiera que revertirlo, borrarlas obligaría a recrearlas. Desactivarlas o no es decisión del
  usuario, no del implementador.

---

## 5. Verificación

1. `mvn -q compile`.
2. `sql/176` sin filas.
3. Entregar un préstamo de prueba y comparar el asiento contra el criterio del **cobro** del mismo
   préstamo: los dos tienen que nombrar la **misma banda** para la misma cuota. **Ese es el punto de
   todo el cambio**; si no coinciden, no está hecho.
4. Un producto con bandas reconfiguradas (más o menos de cinco tramos) debe generar un asiento de
   entrega con esa cantidad de líneas de capital. Con la escalera vieja siempre daban cinco.
5. El asiento sigue cuadrando: la suma de las líneas de capital debe igualar a `socios por pagar`.

---

## 6. Documentación en el mismo cambio

- Borrar el comentario de `ContabilidadPrestamoServiceImpl:590-591` que declara la desviación —
  deja de ser cierto, y un comentario que miente es peor que ninguno.
- `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §3.8: anotar que las líneas de banda de las
  plantillas 9/13/34 quedaron sin uso y por qué.
