# La tormenta de consultas de vigencia en el devengo de aportes

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** ⛔ URGENTE — carga en producción tardando >20 min

> **Reporte del usuario, 2026-09-02:** *«Al procesar el archivo petro está haciendo el select de
> vigencia muchísimas veces y ya va 20 mins y no termina de procesar.»*

---

## 1. Qué muestra el log

Para **UN** partícipe (entidad 5079, $31,00 de cesantía), quince pares de consultas idénticas:

```
Ingresa al metodo selectActivoPorEntidad de Contrato con idEntidad: 5079
Ingresa al metodo selectVigenteEnFecha de VigenciaContrato con idContrato: 4088 - idTipoAporte: 9 - fecha: 2025-06-30
Ingresa al metodo selectActivoPorEntidad de Contrato con idEntidad: 5079
Ingresa al metodo selectVigenteEnFecha de VigenciaContrato con idContrato: 4088 - idTipoAporte: 9 - fecha: 2025-07-31
...                                                                              ... hasta 2026-08-31
```

**`selectActivoPorEntidad` devuelve siempre el mismo contrato (4088)** y se vuelve a preguntar en
cada iteración. Son ~1,1 segundos por partícipe sólo en esto; con miles de partícipes, los 20 minutos.

---

## 2. La causa exacta

`CargaArchivoPetroServiceImpl.esperadoMensual:3862`:

```java
private double esperadoMensual(Entidad entidad, Long idTipoAporte, LocalDate mes,
        Map<String, Double> esperadoEnLote) throws Throwable {
    if (esperadoEnLote != null) {
        Double valorEnLote = esperadoEnLote.get(entidad.getCodigo() + "|" + idTipoAporte + "|" + mes);
        if (valorEnLote != null) {
            return valorEnLote;
        }
    }
    return vigenciaContratoService.esperadoPorEntidad(entidad.getCodigo(), idTipoAporte, mes);
}
```

Y ese método se llama **dentro de dos bucles de meses**, por cada tipo de aporte (`:3744` y `:3772`),
hasta `TOPE_MESES_DEVENGO` meses. Cada fallo de caché son **dos consultas**, porque
`VigenciaContratoServiceImpl.esperadoPorEntidad:225` resuelve el contrato de nuevo cada vez.

**⛔ El defecto de fondo: un `null` del mapa es ambiguo.** El lote (`esperadoEnLotePorFilial`) sólo
trae filas **donde existe una vigencia**. Entonces un mes sin vigencia **no está en el mapa** — y el
código no puede distinguir *«esta entidad no entró en el lote»* de *«esta entidad sí entró y ese mes
no tiene vigencia»*. Ante la duda pregunta a la base, **mes por mes, tipo por tipo, partícipe por
partícipe**.

Para un partícipe con 15 meses y 2 tipos son hasta **60 consultas**. Para 2.000 partícipes, decenas
de miles.

> **Esto es mío, y conviene decirlo con precisión: no es una regresión del revert.** La optimización
> del lote sigue en el código. En la auditoría de rendimiento del 2026-09-02 optimicé el camino
> rápido y **dejé el fallback sin acotar**, midiendo el caso en que el lote acierta. El caso en que
> falla —que es el común, porque los meses sin vigencia son mayoría— nunca se midió.

---

## 3. El arreglo

**Ninguna de las dos consultas debe ocurrir más de una vez por partícipe.**

### 3.1 Desambiguar el `null` del lote

El lote tiene que informar **qué entidades cubrió**, no sólo los valores. Con eso:

- Entidad **cubierta** por el lote + clave ausente → **`0.0` sin consultar**. Es un mes sin vigencia,
  que es un dato, no una duda.
- Entidad **no cubierta** → recién ahí se resuelve por entidad, y una sola vez (§3.2).

### 3.2 Resolver por entidad UNA vez, no por mes

Para las entidades no cubiertas: obtener el contrato activo **una vez** y sus vigencias **una vez**,
y responder todos los meses y tipos desde memoria. Un caché por partícipe dentro de
`distribuirAportePorDevengo` alcanza; no hace falta caché global ni estado entre partícipes.

⚠️ **No cambiar el criterio de selección de vigencia.** El javadoc de `esperadoMensual` documenta que
fue verificado línea por línea contra `VigenciaContratoServiceImpl` y
`VigenciaContratoDaoServiceImpl.selectVigentesPorFilial`: contrato activo con desempate por mayor
código, vigencia ACTIVA que cubre el último día del mes. **Esto es una corrección de rendimiento, no
de reglas: el resultado debe ser idéntico, consulta por consulta menos.**

### 3.3 ⛔ El fallback NO se elimina

Su javadoc explica por qué existe y sigue siendo válido: `selectVigentesPorFilial` filtra además
`ENTDIDST IN (ACTIVO, ACTIVO_EN_MORA)` sobre la entidad, y el camino por entidad no. Un partícipe en
otro estado quedaría con «esperado 0» y **su plata se anticiparía a meses futuros** en vez de cubrir
lo que debía. Se acota, no se borra.

---

## 4. Verificación

1. `mvn -q compile`.
2. En el log de una carga, para un partícipe con aportes: **una** línea
   `selectActivoPorEntidad` como máximo, no quince. Idealmente ninguna, si el lote lo cubrió.
3. **El resultado tiene que ser el mismo.** Comparar los `CRD.APRT` generados para un partícipe
   antes y después: mismos meses, mismos tipos, mismos montos. Si cambia un solo aporte, el arreglo
   está mal — esto no toca reglas.
4. Tiempo total de la carga: el objetivo es que esta línea deje de ser la dominante.
