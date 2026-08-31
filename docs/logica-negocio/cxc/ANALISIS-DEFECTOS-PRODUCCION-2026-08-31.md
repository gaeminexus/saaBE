# Dos defectos de producción — 2026-08-31

**Equipo:** cxp/cxc/pagos/tsr/rhh/sri (`omen-saa-3`) · **Reportados por el usuario con logs de
producción.** Los dos verificados contra el código, archivo:línea. Ninguno está corregido todavía.

| # | Síntoma | Módulo | Causa |
|---|---|---|---|
| **D1** | `ORA-01400` al crear un anticipo de cliente | `cxc` | Columna `NOT NULL` con `DEFAULT`, campo Java sin inicializar |
| **D2** | El SRI devuelve `DEVUELTA` al emitir una liquidación, sin decir por qué | `cxc` | Secuencial `000000000` + los mensajes del SRI se descartan |

---

## D1 — `ORA-01400` en `CBR.ANTC.ANTCAPLC` al crear un anticipo de cliente

### El log

```
ORA-01400: no se puede realizar una inserción NULL en ("CBR"."ANTC"."ANTCAPLC")
insert into CBR.ANTC (ANTCAPLC,ASIENTO,ANTCBANC,EMPRESA,ESTADO,...) values (?,?,?,...)
```

### La causa, verificada

Tres hechos que por separado son correctos y juntos rompen:

| # | Hecho | Dónde |
|---|---|---|
| 1 | La columna es `ANTCAPLC NUMBER(1) DEFAULT 0 NOT NULL` | `cxc/sql/add-anticipo-cliente-devolucion.sql:73` |
| 2 | El campo Java es `private Long aplicado;` — **sin inicializar**, así que vale `null` | `model/cxc/AnticipoCliente.java:177` |
| 3 | **Nadie llama a `setAplicado()` en el alta.** El único que lo hace es `solicitarDevolucion` | `AnticipoClienteServiceImpl:1023` |

**Y el eslabón que lo vuelve inevitable:** Hibernate incluye **toda** columna `@Column` básica en el
`INSERT` que genera — se ve en el propio log, `ANTCAPLC` es la primera de la lista.

> ⛔ **`DEFAULT 0` en Oracle sólo se aplica si el `INSERT` OMITE la columna.** Cuando el `INSERT` la
> nombra y le pasa `NULL` explícito, el default **no interviene** y el `NOT NULL` dispara. Como
> Hibernate siempre la nombra, el `DEFAULT 0` de esa columna **nunca puede actuar** en un alta
> hecha por la aplicación.

### Por qué no se vio venir

El encabezado del script dice, textualmente:

> «**POR QUE NO ROMPE NADA** — Columnas nuevas, nullable/con default, sobre una tabla existente.
> Ningún código hoy lee `ANTCIDPG`/`ANTCAPLC`.»

Era cierto **el 28-08, cuando la entidad todavía no las mapeaba.** Dejó de serlo cuando se agregaron
al `AnticipoCliente.java`. Y el razonamiento tiene un hueco propio: **el problema nunca fue leer,
fue escribir.** Un `SELECT` que no menciona la columna no falla; un `INSERT` que sí la menciona, sí.

Es la misma familia que el §7.2a del `ESTADO`: allí una columna mapeada y ausente en la base rompía
todo `SELECT` de la entidad; acá una columna mapeada, presente y `NOT NULL` rompe todo `INSERT`.
**El denominador común es que mapear una columna en la entidad tiene efectos en sentencias que
nadie escribió a mano.**

### La corrección — inicializar en Java, no aflojar la base

```java
// model/cxc/AnticipoCliente.java:177
private Long aplicado = 0L;
```

**Por qué así y no quitando el `NOT NULL`:** el campo es el guardián de idempotencia de la
devolución, y `AnticipoClienteServiceImpl:991` compara con `Long.valueOf(0L).equals(...)`. Con
`null` esa comparación da `false`, o sea que **`null` y `0` se comportan distinto**: dejar entrar
`null` reintroduce en silencio el problema que el campo existe para evitar. La columna tiene que
seguir `NOT NULL`.

**Verificar además** que ningún otro alta de `AnticipoCliente` quede sin el valor, y que las filas
históricas tengan `0` (deberían: la columna nació `DEFAULT 0 NOT NULL`, así que el `ALTER` las
rellenó).

### Detalle del log que despista

El error aparece reportado sobre `AsientoDaoServiceImpl.selectMaxNumeroMesTipo`, que es de
**contabilidad** y no tiene nada que ver. Es el `flush` de Hibernate disparándose antes de esa
consulta: el `INSERT` pendiente se ejecuta ahí y revienta ahí. **La traza apunta al lugar donde se
descubrió el problema, no donde se originó.**

---

## D2 — Liquidación de compra `DEVUELTA` por el SRI, sin motivo visible

### Dos defectos distintos, no uno

#### D2a — El motivo existe, pero el código lo tira

`LiquidacionCompraServiceImpl:1559-1567` **sí lee** los `<mensaje>` de la respuesta del SRI, pero
**sólo para detectar el caso `CLAVE ACCESO REGISTRADA`**. En cualquier otro caso devuelve el
`estado` pelado (`"DEVUELTA"`) y **descarta identificador, mensaje, `informacionAdicional` y
tipo** — que es exactamente donde el SRI explica qué está mal.

Por eso el log dice `>>> Estado WS1 Recepción: [DEVUELTA]` y nada más.

> ✅ **La respuesta completa SÍ se guarda en disco.** `LiquidacionCompraServiceImpl:1547`
> (`log.println("Respuesta WS1: " + respuestaCompleta)`) la escribe en:
>
> ```
> {saa.upload.dir}/resources/{idFacturador}/lqcs/e/{clave}.txt
> ```
>
> Para el caso reportado, según el propio log (certificado bajo `C:\Users\Administrator\saa-uploads\`,
> facturador `1`):
>
> ```
> C:\Users\Administrator\saa-uploads\resources\1\lqcs\e\3108202603179136759600120010010000000001234567817.txt
> ```

**Corrección:** propagar los mensajes al resultado y al log de consola, no sólo al archivo.

#### D2b — El secuencial es `000000000`, y es casi con certeza la causa del rechazo

Del log: `Número: 001-001-000000000`. Y la clave de acceso lo confirma al desglosarla:

```
31082026 03 1791367596001 2 001001 000000000 12345678 1 | 7
fecha    tp RUC           am serie  SECUENCIAL cod      te  dv
```

**Un comprobante electrónico con secuencial `000000000` es inválido para el SRI: la numeración
arranca en 1.**

El origen está en `obtenerSecuencial` (`:1657-1676`): lee `NumeracionPuntoEmision.numActual`,
**devuelve ese valor** y recién después incrementa. Si la fila arranca en `0`, el primer
comprobante sale `000000000`.

```java
Long numeroActual = numeracion.getNumActual();
// ... UPDATE numActual = numeroActual + 1 ...
return String.format("%09d", numeroActual);   // <-- devuelve el valor PREVIO
```

**Hay que decidir cuál de las dos cosas es la correcta** —y es decisión del usuario, porque cambia
qué número sale impreso:

- **(A) El dato está mal:** la fila de `NumeracionPuntoEmision` para ese punto de emisión y
  `tipoDoc='03'` tiene `numActual = 0` y debería tener `1`. Se corrige con un `UPDATE`, sin tocar
  código. **Es la más probable** si otros tipos de comprobante emiten bien con este mismo código.
- **(B) El código está mal:** debería pre-incrementar (`numActual + 1` y devolver ese). Pero
  entonces **todos** los tipos de comprobante estarían corridos en uno, y las facturas ya emitidas
  lo desmentirían.

⚠️ **Contrastar contra `CBR.FCTR`/`CBR.NXPE` qué secuencial tiene el último comprobante emitido de
otro tipo antes de tocar nada.** Elegir mal reordena la numeración fiscal, que es lo peor que se
puede hacer acá.

#### D2c — Efecto colateral: cada intento fallido quema un secuencial

`obtenerSecuencial` hace el `UPDATE` de `numActual` **antes** de saber si el SRI acepta. Cuando el
WS1 responde `DEVUELTA`, el método hace `return resultado` **normal** (`:477-483`) — no lanza
excepción — así que **la transacción commitea y el incremento queda**. Cada reintento consume un
número que nunca se usó.

No es lo que rompe hoy, pero explica huecos en la numeración y conviene resolverlo junto con D2b.

---

## Lo que hace falta del usuario antes de corregir D2

1. **El contenido de `…\lqcs\e\3108202603179136759600120010010000000001234567817.txt`** — ahí está
   el motivo textual del SRI. Confirma o refuta el diagnóstico del secuencial en un minuto.
2. **Decidir entre D2b (A) y (B)**, con el contraste de numeración hecho.

D1 no necesita nada: la corrección es una línea y el razonamiento está cerrado.
