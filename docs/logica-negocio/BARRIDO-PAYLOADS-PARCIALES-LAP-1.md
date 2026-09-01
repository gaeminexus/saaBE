# Barrido de payloads parciales — pérdida silenciosa de datos

**Equipo:** `lap-saa-1` · **2026-09-01** · Módulos `cxp` · `cxc` · `pagos` · `tsr` · `rhh`
**Nada de esto está corregido todavía.** El barrido lo hizo el agente de frontend; **los hallazgos
🔴 los verifiqué yo extremo a extremo, cruzando los dos repositorios.**

---

## 0. El mecanismo, en tres líneas

`EntityDaoImpl.save()` hace `em.merge()` con el objeto tal cual llegó del JSON: **sin re-leer la fila
y sin saltar nulos**. En `com.saa.model` no hay un solo campo primitivo persistido, así que **una
clave ausente en el JSON deserializa a `null` y se graba en la columna real.**

No hay error, no hay log, no hay aviso. El dato simplemente ya no está.

⛔ **No se arregla en `EntityDaoImpl`** — hereda todo el proyecto y hay escrituras que ponen `null` a
propósito. Se arregla en cada pantalla, o preservando el campo del lado servidor cuando es estado
interno.

---

## 1. 🔴 CONFIRMADO — Editar un permiso aprobado revierte la aprobación y borra su rastro

**`RHH.PTCN` · pantalla `modules/rrh/forms/gestion/permisos-licencias`.**

Es **un solo defecto con tres efectos**, no tres defectos sueltos. La cadena, verificada eslabón por
eslabón:

| # | Eslabón | Verificado |
|---|---|---|
| 1 | `permisos-licencias-form.component.ts:418-450` (`buildRequestData`) arma el payload desde cero. **En modo edición sólo arrastra `codigo`** del original (`:449`) | ✅ |
| 2 | Hardcodea **`estado: 'SOLICITADA'`** (`:437`) sin condicionar al modo | ✅ |
| 3 | Nunca setea `usuarioAprobacion` ni `motivo` | ✅ |
| 4 | `permiso-licencia.service.ts:128-146` (`mapToBackendFormat`) copia cada clave **sólo si `!== undefined`** — las tres ausentes **no viajan** | ✅ |
| 5 | `PUT /rest/ptcn` → `PeticionesServiceImpl.saveSingle:96-100` llama al DAO genérico **sin preservar nada** | ✅ |
| 6 | La entidad `Peticiones` mapea `PTCNAPRB` → `usuarioAprobador`, `PTCNMTVO` → `motivo`, `PTCNESTD` → `estado` | ✅ |

**Efecto para el usuario final:** editar la observación de un permiso ya aprobado

1. **lo devuelve al estado `SOLICITADA`** — o sea, lo desaprueba;
2. **borra `PTCNAPRB`**, quién lo había aprobado;
3. **borra `PTCNMTVO`**, el motivo — que la pantalla **sí lee y muestra**
   (`mapFromBackendFormat:162`) pero **nunca reenvía**.

Los tres a la vez, sin error visible. La aprobación no queda "a medias": queda **deshecha y sin
rastro de que existió**.

> **Por qué costaba verlo, y es lo que vale registrar:** el servicio tiene una capa de traducción
> (`mapToBackendFormat`) que hace ver el payload más completo de lo que es, y su guarda
> `!== undefined` es **exactamente correcta** para un `PATCH` parcial. El defecto no está en esa
> función: está en que el backend no hace `PATCH`, hace `merge` sobre la entidad entera. **Cada
> mitad es razonable por separado.**
>
> Y `motivo` es el caso más traicionero de los tres: **se lee y se muestra en pantalla**, así que el
> usuario lo ve antes de editar y no lo ve después. Los otros dos campos ni siquiera están en el
> formulario.

**Corrección recomendada — del lado servidor, no del formulario.** `estado` y `usuarioAprobador` son
**estado del flujo de aprobación**, no datos que el formulario de solicitud tenga por qué conocer:
`PeticionesServiceImpl.saveSingle` debe releer la fila y preservarlos cuando el `codigo` ya existe.
Es el mismo criterio que ya se aplicó en `AnticipoClienteServiceImpl` con `idPagoDevolucion` y
`aplicado`. Pedirle al formulario que los reenvíe es confiar en que el cliente devuelva estado
interno, que es justo lo que falla en silencio.
`motivo` sí es del formulario: ahí la corrección es del lado frontend.

⚠️ **`rhh` es territorio compartido con `omen-saa-2`.** Avisado a su árbitro el 2026-09-01. Los
archivos concretos están fríos (su commit de hoy tocó `ordenes-pago` y `pago-beneficios-sociales`),
pero la corrección de `PeticionesServiceImpl` se coordina antes de escribirla.

---

## 2. 🔴 CONFIRMADO — Editar una solicitud de vacaciones borra la fecha de aprobación

**`RHH.SLCT` · `modules/rrh/forms/gestion/vacaciones/vacaciones-form.component.ts:322-338`.**

Mismo patrón: el payload se arma desde cero y `fechaAprobacion` no se copia ni se recalcula.

**Se corrige junto con §1 y con el mismo criterio** — la fecha de aprobación es estado del flujo,
no del formulario.

---

## 3. 🟠 PROBABLES — `tsr`, verificados por el frontend, sin cruzar contra la entidad

Los tres están **en el menú**, o sea que son alcanzables por un usuario cualquiera. Ordenados por
daño.

| Archivo | Qué pierde |
|---|---|
| `tsr/forms/chequeras/solicitud-chequera.component.ts:178-221` | `rubroEstadoChequeraP` / `rubroEstadoChequeraH` sólo se setean en la rama de creación → editar una solicitud existente le resetea el estado |
| `tsr/forms/cuentas-bancarias/cuentas-bancarias.component.ts:390-447` | Payload armado enteramente desde los controles del formulario, sin `GET` previo de la entidad completa |
| `tsr/forms/bancos/bancos-nacionales-extranjeros.component.ts:200-210` | **Mitigación parcial y frágil:** preserva `fechaIngreso` a mano, campo por campo. Cualquier otro que no esté en el formulario se pierde — y el próximo `ALTER TABLE` agrega uno más sin que nadie lo note |

`tsr/forms/caja-chica/parametrizacion/cajas-chicas.component.ts:227-242` **no cuenta como hallazgo
nuevo**: su propio comentario ya advierte sobre `em.merge()` y el campo `custodio`.

> El caso de `bancos-nacionales-extranjeros` es el que más conviene entender: **preservar campos a
> mano no escala.** Funciona el día que se escribe y se rompe callado cada vez que la tabla crece.
> La forma robusta es siempre la misma — `GET` de la entidad completa, aplicar encima lo que el
> formulario edita, mandar el objeto entero — o preservar del lado servidor.

---

### 3bis. El helper que ya existe, y por qué no se puede subir tal cual

`modules/rrh/forms/comunes/cuerpo-entidad.ts` ya resuelve este problema: `armarCuerpo` parte del
registro **tal como llegó del backend** (`...(base ?? {})`) y le superpone lo editado. Es la forma
correcta, y trae dos piezas que evitan defectos que no estaban en el radar de este barrido:

- **`sinAdornos`** — las etiquetas calculadas `*Label` hacen que el backend rechace el `PUT` entero
  con *«Not able to deserialize data provided»*.
- **`referencia`** — verificado en `rrh`: un préstamo hipotecario de alterna 24 quedó grabado como el
  concepto 24, «Seguro privado». Sin error.

**Vive dentro de `rrh`**, así que usarlo desde `tsr` exige subirlo a `shared/` con sus dependencias
— y eso toca archivos de `omen-saa-2`. Coordinado con su árbitro; **ninguno de los dos lo despacha
sin que lo apruebe su usuario.**

#### ⛔ El paso previo: `extraerCodigo` adivina, y no hay default correcto

`utiles-parametrizacion.ts:83-84` prefiere **`codigoAlterno` sobre `codigo` siempre que exista**.
Subirlo a `shared/` tal cual generalizaría esa preferencia; **pero invertirla rompería más.**

| Módulo | Usos de `codigoAlterno` |
|---|---|
| **tsr** | **58** |
| **cnt** | **35** |
| rhh | 17 |
| cxp · cxc | 7 cada uno |

**En `tsr` el alterno es lo correcto, y el código ya lo escribe explícito** — sus columnas de rubro
son pares padre/hijo que guardan el alterno:

```ts
// cuentas-bancarias.component.ts:393-394
payload.rubroTipoCuentaP = this.tipoCuentaSel.rubro?.codigoAlterno ?? 23;
payload.rubroTipoCuentaH = this.tipoCuentaSel.codigoAlterno;
```

Es coherente con el backend, que busca por alterno (`selectValorStringByRubAltDetAlt`).

**Conclusión: no hay un valor por defecto correcto — depende de la columna, no del módulo.** El
arreglo es que `extraerCodigo` **deje de adivinar y el llamador declare cuál de los dos quiere**.

**Y el riesgo es más chico de lo que parece:** `armarCuerpo` sólo pasa por `extraerCodigo` los
`camposEscalares`; los `camposReferencia` van por `referencia()`, que prueba `.codigo` primero. **Una
FK nunca se lleva el alterno por ese camino.**

> **El patrón general, que vale para los seis módulos:** en este sistema conviven **dos
> identificadores por fila** —PK y código alterno— y no son intercambiables. Elegir el equivocado
> **nunca falla**: lee o graba la fila de otro. Es la misma confusión que el `PRBRALTR` del registro
> de reservas, en el otro repositorio y sin relación entre sí.
>
> **Criterio de búsqueda:** sospechar menos del objeto de catálogo que viaja entero —en `tsr` el
> código ya lo desarma bien— y más de **cualquier helper que elija identificador sin que el llamador
> se lo diga**.

#### ⛔ Y no hay consenso que subir: los dos criterios ya conviven, invertidos

`tsr/forms/bancos/bancos.component.ts:161-174` define **su propia función local, también llamada
`extraerCodigo`**, y resuelve al revés que la de `rrh`:

| Función | Archivo | Gana |
|---|---|---|
| `extraerCodigo` | `rrh/forms/parametrizacion/utiles-parametrizacion.ts:81-86` | **el alterno** |
| `extraerCodigo` (local) | `tsr/forms/bancos/bancos.component.ts:161-174` | **el `codigo`** |

**Mismo nombre, criterios incompatibles.** Un objeto que traiga los dos campos da resultados
distintos según por dónde pase, y ninguna de las dos está mal: cada una acierta para las columnas
que su pantalla escribe.

**Esto decide el diseño, y es más fuerte que cualquier argumento de estilo:** no existe un criterio
mayoritario que trasladar a `shared/`. Elegir un default sería **inventarlo**, y rompería a la mitad
de los llamadores en la dirección que se elija. La única salida que no rompe a nadie es que
**`extraerCodigo` deje de adivinar y el llamador declare cuál de los dos identificadores quiere.**

⚠️ **Trampa concreta para quien "unifique" esto:** el nombre coincide exactamente. Reemplazar la
función local de `bancos.component.ts` por un `import` de la de `rrh` **compila, no da ningún aviso,
e invierte el comportamiento.**

> **Cómo se encontró, y es una corrección a este mismo documento.** La versión anterior de esta
> sección afirmaba que `bancos.component.ts` tenía «su propia copia inline de la preferencia por el
> alterno» — o sea, lo contrario de lo que hace. El error vino de leerlo por un `grep`, que devolvió
> sólo la rama del alterno (es donde casaba el patrón) y no la rama de `codigo`, que está tres
> líneas antes. **Un `grep` recorta por definición: muestra lo que casa, no lo que decide.** Lo
> detectó el árbitro de `omen-saa-2` yendo al archivo. Es la segunda vez en la jornada que una
> lectura parcial produce una conclusión invertida — la primera fue el registro de reservas truncado
> (§1.5 del estado del equipo).

---

## 4. ⚪ DUDOSOS — hace falta cruzar contra el modelo antes de tocarlos

No están verificados. **No corregir a ciegas.**

- **cxp/cxc:** `documento-cxp.service.ts`, `factura-compra.service.ts`, `nota-credito-compra.service.ts`,
  `nota-debito-compra.service.ts` y sus análogos de `cxc`. Se consumen desde
  `ConsultaDocumentosComponent`, **la pantalla de más tráfico de `cxp`** — son las de mayor
  prioridad de este grupo.
- **tsr:** `cajas-fisicas`, `cajas-por-grupo`, `grupos-cajas` (parten de una fila de grilla, así que
  el riesgo depende de si `selectByCriteria` devuelve la entidad completa o una proyección — hay que
  mirarlo), `titulares-v2` (updates en 509 / 816 / 969 / 1220), `deposito.service.ts`, familia
  `cobro*.service.ts`.
- **rrh:** `resumen-diario:199`, `configuracion-nomina:139-140`, `parametros-anuales:157-158`,
  `novedades-nomina:466/574/639`, `descuentos-recurrentes:324/342/422`.

**Verificado y descartado:** `GruposProductosPagoComponent` y `GruposProductosCobroComponent` arman
el objeto a mano pero **cubren los 7 campos del modelo**. Sin pérdida.

---

## 5. Hallazgo aparte — un fallo silencioso de otra familia

`cxp/forms/negociaciones/detalle-negociacion/detalle-negociacion.component.ts:104` manda un objeto
plano `{negociacion:{id}}` a un `selectByCriteria` que espera `List<DatosBusqueda>`. El backend
responde 400 y el `catchError(() => of([]))` se lo traga: **la pestaña "documentos" queda vacía y el
usuario no se entera.**

⚠️ **No es nuestro.** Ya lo señaló el equipo `eq3` en el commit `5ab3b8a` de hoy. Queda anotado para
no volver a "descubrirlo" y para no pisarlo.

---

## 6. Lo que el barrido encontró de paso: pantallas inalcanzables

No es pérdida de datos, pero sale del mismo relevamiento y explica reportes de "eso no funciona".

**Links de menú rotos — el usuario hace clic y no pasa nada:**
- `cxp` → "Proposición de Pago" apunta a `/menucuentaxpagar/procesos/proposicion-pago`, ruta **nunca
  registrada** en `app.routes.ts`.
- `cxp` → "Consulta de CxP" (`consultas/cxp`), misma situación.

**Pantallas que existen y no están en el menú** (sólo por URL directa): `cxp/parametrizacion/proveedores`
(entrada comentada), `cxc` gestionar/facturas · gestionar/anticipos · cobros/abonos-factura, y varias
de `tsr` (`procesos/pagos/ingreso`, `procesos/pagos/consulta/pagos`, dos placeholders de
parametrización, un duplicado de chequeras).

**Ni siquiera ruteadas:** toda `tsr/forms/movimientos-bancarios/` (`CuentasBancariasListado`, `Ried`,
`Creditos`, `Debitos`, `Transferencias`), más `ChequesEntrega` y `ChequesImpresion`.

*(`rrh/procesos/aportes` está fuera del menú **a propósito**, con nota fechada el 2026-08-26: no
existe la entidad de backend. No es un huérfano accidental.)*

---

## 7. Código muerto — inventario, para decidir de una vez

**31 servicios `Temp*` con cero uso** fuera de su propio archivo y su `.spec`: 12 en `cxp`, 9 en
`cxc`, 10 en `tsr`. Es un patrón sistémico, no casos sueltos: parecen servicios de staging que nunca
se conectaron a una pantalla, o que quedaron reemplazados por los homónimos sin sufijo.

⚠️ **Los endpoints del backend sí existen.** Borrar los servicios del frontend no borra las tablas
`Temp*` ni sus REST — y esas tablas son parte del diseño (documentos en progreso de CXC/CXP/TSR).
**Son dos decisiones distintas y conviene no mezclarlas.**

**13 servicios más en `tsr`** sin uso (`AuxDeposito*`, `DetalleCierre`, `HistConciliacion`,
`MovimientoBanco`, `SaldoBanco`, …). De estos, `DireccionTitularService` y `TelefonoDireccionService`
**podrían ser "todavía no conectados" en vez de muertos** — no se puede distinguir desde el código.

**Dos componentes viejos ya reemplazados, código muerto completo:**
`cxc/forms/emitir/retenciones/RetencionesComponent` (lo reemplazó `Retencionesv2Component`, con la
ruta ya reasignada) y `tsr/forms/titulares/TitularesComponent` (~1.100 líneas, lo reemplazó
`TitularesV2Component`).

**8 imports muertos en `header.component.ts:22-30`**, resto de un `testServicios()` recortado. Cinco
de esos servicios no se usan en ningún lado del repositorio; dos sí (`CuotaXFinanciacionPago`,
`FinanciacionXDocumentoPago`, desde `proposicion-pago`) y sólo sobra el import.
