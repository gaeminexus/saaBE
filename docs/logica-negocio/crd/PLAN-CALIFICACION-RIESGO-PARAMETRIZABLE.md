# Parametrizar la calificación de riesgo (G48)

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** decisión tomada, pendiente de implementar

> **Decisión del usuario, 2026-09-02:** *«Respecto al G48 debemos hacer esto: parametrizar la
> calificación de riesgo por separado. Es lo correcto.»*
>
> Contexto: *«las bandas se configuran en función de lo que el ente regulatorio exige»* — el mismo
> principio aplica a la escala de calificación, pero **es una escala distinta**, no la de bandas.

---

## 1. ⛔ Lo primero: NO es la misma clasificación que las bandas

Esto hay que tenerlo clarísimo antes de escribir una línea, porque confundirlas produce un **reporte
regulatorio incorrecto**, que es peor que un asiento descuadrado.

| | Bandas contables | Calificación de riesgo |
|---|---|---|
| Para qué | a qué **cuenta contable** va el saldo | cuánta **provisión** se constituye |
| Valores | 1-30, 31-90, 91-180, 181-360, +360 | A1, A2, A3, B1, B2, C1, C2, D, E |
| Dónde vive hoy | `CRD.BNDP`, parametrizado | `GeneracionG48ServiceImpl`, **cableado** |
| Varía por | producto y empresa | tipo de crédito (hipotecario vs. general) |

**Ningún corte coincide.** Reemplazar la escala de riesgo por la parametrización de bandas haría que
el G48 reporte mal al organismo de control.

---

## 2. Lo que está cableado hoy

`GeneracionG48ServiceImpl.calcularCalificacion:472` — **dos escalas**, elegidas por un literal:

```java
boolean esHipotecario = codigoProducto != null &&
    (codigoProducto == 7L || codigoProducto == 8L || codigoProducto == 21L);
```

| Calificación | Hipotecaria (7, 8, 21) | General | Provisión |
|---|---|---|---|
| A1 | 0 días | 0 días | 0,99 % |
| A2 | 1–30 | 1–15 | 1,99 % |
| A3 | 31–60 | 16–30 | 2 % |
| B1 | 61–120 | 31–60 | 5 % |
| B2 | 121–180 | 61–90 | 10 % |
| C1 | 181–210 | 91–120 | 20 % |
| C2 | 211–270 | 121–180 | 40 % |
| D | 271–450 | 181–270 | 60 % |
| E | +450 | +270 | 100 % |

Hay **tres cosas** cableadas, no una, y las tres tienen que parametrizarse juntas o el trabajo queda
a medias:

1. Los **rangos de días** por calificación.
2. Los **porcentajes de provisión** (`calcularProvision:454`).
3. **Qué productos son hipotecarios** — el literal `(7, 8, 21)`, que es lo más frágil de todo: un
   producto hipotecario nuevo se califica con la tabla general y **nadie se entera**.

---

## 3. El modelo

Se copia la estructura que ya existe para bandas (`CBPR` cabecera + `BNDP` detalle), porque es la que
el usuario ya conoce de la pantalla de configuración y porque resolvió el mismo problema.

**`CRD.CFCR` — Configuración de Calificación de Riesgo** (cabecera): a qué **producto** y **empresa**
aplica, con **vigencia**. Que sea por producto elimina de raíz el literal `(7, 8, 21)`: cada producto
apunta a su escala y un producto nuevo se configura, no se cablea.

**`CRD.ESCR` — Escala de Calificación de Riesgo** (detalle): una fila por calificación, con
`díaDesde`, `díaHasta` y `porcentajeProvision`.

⚠️ **La vigencia no es decoración.** Las escalas regulatorias cambian, y un G48 de un período
anterior **debe recalcularse con la escala que regía entonces**, no con la actual. Es la misma razón
por la que `CBPR` tiene `CBPRFCIN`/`CBPRFCFN`. Sin vigencia, el primer cambio de norma reescribe la
historia.

DDL en **`sql/177`**.

---

## 4. Cómo se conecta

Un servicio de resolución en `crd` —`CalificacionRiesgoService`, hermano de
`ClasificadorBandaService`— que dado (producto, empresa, días, fecha) devuelva calificación y
porcentaje. `GeneracionG48ServiceImpl` pasa a preguntarle en vez de decidir.

⛔ **La lógica de negocio va en `crd`, no en `rpr`.** El G48 es un **consumidor** de la clasificación,
igual que el asiento es consumidor de la banda. Si la escala vive dentro del generador del reporte,
el próximo reporte que la necesite la va a volver a cablear — que es exactamente cómo llegamos acá.

**Alcance:** las tablas y el servicio son `crd` (este equipo). El cambio dentro de
`GeneracionG48ServiceImpl` toca `rpr`; si ese módulo tiene dueño, **avisar al árbitro antes de
tocarlo**, no después.

---

## 5. ⛔ Prerrequisito y riesgo

`sql/177` **carga las dos escalas actuales tal cual están hoy**, valor por valor. **No es una
oportunidad para mejorarlas:** el objetivo de esta primera etapa es que el G48 dé **exactamente el
mismo resultado** leyendo de la base. Cualquier diferencia entre lo cableado y lo cargado es un error
del script, no una mejora.

Y hay un riesgo que hay que mirar antes de desplegar: **qué pasa con un producto sin configuración.**
Dos opciones y ninguna es obvia:

- **Fallar** — coherente con `lineaBandaCapital`, que lanza si falta la cuenta. Pero un G48 que no
  se genera es un reporte que no se entrega al organismo.
- **Caer a la escala general** — es el comportamiento de hoy, y es justamente el que esconde el
  problema del producto hipotecario nuevo.

**Recomendación:** fallar, **pero con el listado completo de productos sin configurar en el mensaje**,
para que se resuelva de una vez y no producto por producto. Y `sql/177` incluye el control que lo
detecta antes de desplegar, así el fallo se descubre acá y no frente al organismo.

---

## 6. Verificación

1. `mvn -q compile`.
2. `sql/177` y su control final: las dos escalas completas, sin huecos ni solapes de rangos.
3. **Correr el G48 antes y después y comparar fila por fila.** Calificación y provisión tienen que
   ser **idénticas**. Si cambia una sola fila, el arreglo está mal — esta etapa no cambia reglas.
4. Reconfigurar un rango en la base y volver a correr: el resultado **debe** cambiar. Es la prueba de
   que dejó de estar cableado.
5. Un producto hipotecario nuevo agregado a la configuración se califica con la escala hipotecaria,
   sin tocar código. **Ese es el defecto que este cambio viene a cerrar.**
