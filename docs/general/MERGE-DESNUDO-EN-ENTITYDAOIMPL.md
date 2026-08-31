# Un `PUT` con payload parcial BORRA las columnas que no vengan

**Fecha:** 2026-08-31 · **Encontrado por:** el agente de frontend del equipo B de CRD, al investigar
antes de escribir en vez de asumir. Verificado por el árbitro contra el código.
**Alcance: TODO el sistema.** No es de `crd`.

---

## El hecho

`com.saa.basico.utilImpl.EntityDaoImpl.save(Tipo, Long)` — el DAO genérico del que hereda **cada**
entidad del proyecto — hace esto y nada más:

```java
public Tipo save(Tipo tipo, Long id) throws Throwable {
    if (id == null) {
        selloAuditoria(tipo);
        em.persist(tipo);
    } else {
        em.merge(tipo);          // <-- acá
    }
    return tipo;
}
```

`em.merge(tipo)` con el objeto **tal cual llegó del JSON**: sin re-leer la fila existente, sin
comparar, sin saltar nulos.

Y las entidades del proyecto **no tienen ni un campo primitivo persistido**: todo es `Long`,
`Double`, `String`, `LocalDateTime`. Una clave ausente en el JSON no queda "sin valor": deserializa
a `null`.

**Conclusión:** en una actualización, un campo que el cliente no manda **no queda como estaba — se
graba `NULL`**. Incluidas las FKs.

### Que no haya primitivos persistidos no es un detalle: es lo que hace visible el daño

Verificado el 2026-08-31 por dos árbitros por separado, y vale la pena decir **por qué se verificó**.

Un `boolean` primitivo persistido sería **peor** que un nulo: una clave ausente deserializa a
`false`, escribe `0` en la columna, y **no hay forma de distinguirlo de un "no" explícito del
usuario**. Con un tipo boxed el daño al menos queda como `NULL`, que se ve raro y se puede detectar.

En todo `com.saa.model` hay **nueve** campos primitivos:

| Dónde | Situación |
|---|---|
| `DetalleAsiento:240`, `DetalleExtractoBancario:377` (`esArrastrada`) | están en clases `@Entity` pero son **`@Transient`** — no se persisten |
| `ReporteResponse`, `DetalleCumplimientoCuenta`, `PendienteAsientoTransito`, `PendienteExtractoTransito`, `ResumenImportacionExtracto`, `ArchivoBatchIess` | POJOs de transporte, **sin `@Entity`** |

**Cero primitivos persistidos.** La regla no tiene excepción — y si alguien agrega un primitivo a una
entidad, introduce una clase de daño peor que la que este documento describe.

> ⚠️ **Detalle de método, por si alguien repite el grep:** `ArchivoBatchIess` da falso positivo si se
> cuenta `@Entity` con `grep -c`, porque su javadoc dice literalmente *"POJO de transporte, sin
> @Entity"*. Contar la anotación por texto engaña; hay que mirar el archivo.

## Por qué nadie lo había notado

Porque la mayoría de las pantallas mandan el objeto entero de vuelta. El defecto solo aparece cuando
alguien construye un payload "solo con lo que cambió", que es exactamente lo que parece razonable
hacer, y lo que este documento existe para desaconsejar.

## Cómo se descubrió

En `prestamo-edit` (CRD) el `guardar()` creaba siempre un préstamo nuevo, incluso editando uno
existente. Al arreglarlo hubo que decidir qué mandar en el `PUT`, y la respuesta obvia —"mandá solo
los campos del formulario"— habría puesto en `NULL` los saldos, los totales, el estado y las
relaciones de un préstamo vivo. Peor que el duplicado que se estaba arreglando.

El camino verificado, línea por línea: `PrestamoRest.put()` deserializa el body entero a la entidad
y se lo pasa a `PrestamoServiceImpl.saveSingle()`, que llama a `prestamoDaoService.save(prestamo,
prestamo.getCodigo())`, que no está sobreescrito y cae en el genérico de arriba. **No hay ninguna
protección en todo el trayecto.**

## Qué hacer

**Regla para cualquier pantalla que actualice una entidad:**

> **Leer primero, sobrescribir después.** `GET` de la entidad completa, aplicar encima solo los
> campos que el formulario edita, y mandar el objeto entero. Nunca armar un payload parcial.

Es lo que hace hoy `prestamo-edit.guardarEdicion()` y sirve de patrón de referencia.

**Antes de confiar en el `GET`, verificá que traiga el grafo completo.** Si una relación se
serializa vacía, el "leer primero" la va a mandar en `null` igual. En `Prestamo` las cuatro
relaciones son `@ManyToOne` sin `fetch` declarado, o sea **EAGER** por defecto en JPA, y vienen
pobladas — pero eso hay que confirmarlo entidad por entidad, no darlo por sentado.

## Lo que NO hay que hacer

**No "arreglar" `EntityDaoImpl.save()`.** Es el DAO del que hereda todo el sistema, y hacerlo
copiar-solo-lo-no-nulo cambiaría el comportamiento de **todas** las escrituras del proyecto de una
sola vez — incluidas las que hoy dependen de poder poner un campo en `null` a propósito. Sería un
cambio de plataforma, no una corrección, y no hay tests para respaldarlo.

Si algún día se hace, va como proyecto propio, con inventario previo de quién escribe `null`
intencionalmente.

## Cómo saber si te afecta

Buscá pantallas que construyan un objeto de payload literal para un `PUT`/`update` en vez de
partir de la entidad leída:

```bash
grep -rn "update(" src/app/modules/*/service/*.ts
```

Y del lado del backend, cualquier `*ServiceImpl.saveSingle` que reciba la entidad del REST y la pase
directo al DAO — que es el patrón estándar de la casa, así que son casi todos.

---

**Dueño: nadie todavía.** Se documenta acá para que no se pierda. Avisado a los árbitros de los
equipos activos el 2026-08-31.

**No es teórico fuera de `crd`:** el árbitro del equipo 2 lo trazó hasta un `@PUT` real de su
alcance, `FacturaCompraRest:52-57` → `saveSingle` → el mismo DAO genérico. El camino es idéntico en
`cxp`, y por construcción en cualquier módulo que siga el patrón de la casa — que son casi todos.
