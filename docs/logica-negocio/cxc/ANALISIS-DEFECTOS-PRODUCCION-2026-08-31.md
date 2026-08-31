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

#### ✅ D2-CAUSA REAL — `tipoIdentificacionProveedor` manda el rubro interno crudo

**Confirmado con la respuesta del SRI, 2026-08-31.** El archivo de log de D2a la tenía:

```xml
<identificador>35</identificador>
<mensaje>ARCHIVO NO CUMPLE ESTRUCTURA XML</mensaje>
<informacionAdicional>cvc-pattern-valid: Value '1' is not facet-valid with respect to
   pattern '[0][4-8]' for type 'tipoIdentificacionProveedor'.</informacionAdicional>
```

> ⚠️ **La hipótesis del secuencial era incorrecta como causa del rechazo.** Se mantiene abajo
> porque el defecto es real, pero **no es lo que rompió hoy**: el SRI valida el XSD antes que nada
> y frena en el primer error de estructura. **La lección: un log que no dice el motivo no autoriza
> a inferirlo del dato más llamativo.** El motivo estaba en disco todo el tiempo (D2a) y bastaba
> con leerlo.

**La línea:**

```java
// LiquidacionCompraServiceImpl:1017
writeElement(writer, "tipoIdentificacionProveedor",
        String.valueOf(liquidacion.getTitular().getRubroTipoIdentificacionH()), 4);
```

Manda el **código del rubro interno** (`1`) donde el XSD del SRI exige el patrón `[0][4-8]`, o sea
`04` RUC · `05` cédula · `06` pasaporte · `07` consumidor final · `08` identificación del exterior.
**Los dos catálogos no coinciden ni en valores ni en orden.**

**Es exactamente la trampa #1 del frente Q**, ya encontrada y corregida *para el ATS* (`ESTADO`
§2, «Tres correcciones encontradas antes de entregar»): *«el plan era reusar
`Titular.rubroTipoIdentificacionH` tal cual… habría mandado RUC y Cédula invertidos»*. **Se corrigió
en el generador del ATS y no se buscó el mismo patrón en los generadores de comprobantes.**

##### El patrón correcto ya existe en el repositorio

`FacturaServiceImpl:1073-1092` —el generador de facturas de venta, que sí emite bien— resuelve el
código contra el catálogo y normaliza a dos dígitos:

```java
String valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
        titular.getRubroTipoIdentificacionP().intValue(),   // rubro PADRE
        titular.getRubroTipoIdentificacionH().intValue());  // rubro HIJO
tipo = (valorAlfa.length() == 1) ? "0" + valorAlfa : valorAlfa;
```

**Dos diferencias con el código roto, y las dos importan:** usa **los dos** rubros (padre e hijo),
no sólo el hijo; y **pasa por el catálogo**, que es donde vive la equivalencia con el código del SRI.

##### El mismo defecto, en un segundo lugar

| Archivo:línea | Campo | Estado |
|---|---|---|
| `LiquidacionCompraServiceImpl:1017` | `tipoIdentificacionProveedor` | 🔴 **es el que falló hoy** |
| `RetencionServiceImpl:231` | `tipoIdentificacionSujetoRetenido` | 🟠 **mismo bug, latente** |

`RetencionServiceImpl` es el generador **viejo** (el vigente es `RetencionV2ServiceImpl`, ruta
`/rtv2`, que no aparece en el barrido y por lo tanto no tiene el defecto). Corregirlo igual, o
confirmar que está muerto y retirarlo — pero **no dejarlo como está**: hoy no lo alcanza nadie, y
esa es exactamente la condición del `TSR.TSRD` del §7.4b, que se dejó latente y sigue ahí.

##### ⚠️ Decisión de diseño: qué hacer si el catálogo no resuelve

El patrón de la factura usa `"05"` (cédula) como **valor por defecto silencioso** cuando el lookup
falla o devuelve vacío. **Para la liquidación de compra eso no debe copiarse tal cual:** emitir un
comprobante fiscal con un tipo de identificación adivinado es peor que no emitirlo — el SRI lo
acepta y queda mal declarado, sin ningún error visible.

**Criterio del árbitro: abortar la emisión con un mensaje claro**, igual que ya se decidió para
`dirEstablecimiento` vacío en este mismo método (*«El XSD declara dirEstablecimiento con
minLength>=1: un comprobante con este campo vacío el SRI lo devuelve. Mejor abortar la emisión»*).
Un `IncomeException` que nombre al titular y su tipo de identificación sin mapear.

---

#### D2b — El secuencial es `000000000` — defecto real, pero NO la causa del rechazo de hoy

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
