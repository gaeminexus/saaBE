# Contrato de intranet: el path `/movil` de SaaBE

> **Qué es esto.** El WAR de borde (`SaaMovilBE`, en el 192.168.2.3, expuesto a internet) no
> consume `/SaaBE/rest/...`. Consume un application path **aparte**, `/SaaBE/movil/...`, que es
> una **lista blanca cerrada** de consultas de un partícipe sobre sí mismo. Este documento es el
> contrato entre los dos: lo que el borde puede pedir y lo que SaaBE le contesta.
>
> **Fecha:** 2026-09-07 · **Estado:** IMPLEMENTADO COMPLETO y commiteado (`3666a6c` + `48f8c2c`).
> La §5.4 lleva la forma real de las respuestas, medida sobre el código: es lo que el borde congela.
> **Decidido por el usuario el 2026-09-07:** path `/movil` y clave compartida obligatoria.
> Equipo: app móvil ASOPREP (`omen-app-1`). Ejecutor en SaaBE: `omen-app-1-saa-be-app`.

## 1. Por qué un path aparte y no `/rest`

`/rest` tiene ~200 recursos, incluidos `DELETE /rest/prst/{id}`, `pagarCuota`, `aprobar`,
`anularOperacion`. **Nada de eso puede quedar a un error de configuración de distancia de
internet.** Con un path propio, la separación deja de depender de que el borde se porte bien y
pasa a ser estructural: lo que no está en la lista blanca no existe en esa URL.

Efecto lateral buscado: los tres endpoints de oficina de `usap` (`crear`, `resetearClave`,
`reactivar`) **se quedan solo en `/rest`**. No se replican en `/movil`. Ya no hace falta
confiar en que el borde no los enrute — desde el path que el borde ve, no existen.

```
                    /SaaBE/rest/...    ~200 recursos, incluye destructivos  <- intranet, NO lo toca el borde
192.168.2.4  SaaBE
                    /SaaBE/movil/...   lista blanca, solo lectura + 3 auth  <- unico que consume el borde
```

## 2. ⛔ Las dos trampas de JAX-RS que hay que resolver ANTES de escribir el primer recurso

Verificado el 2026-09-07: `com.saa.ws.rest.ApplicationConfig` es `@ApplicationPath("/rest")` y
**está vacío** — no sobreescribe `getClasses()`. Una `Application` vacía toma **todos** los
`@Path` y **todos** los `@Provider` del deployment. De ahí salen dos trampas:

**Trampa 1 — la Application nueva no puede quedar vacía.** Si `MovilApplicationConfig` se
escribe igual de vacía, `/SaaBE/movil/...` expondría exactamente los mismos ~200 recursos que
`/rest`, `DELETE` incluidos, y el path nuevo sería un segundo camino a todo en vez de una lista
blanca. **Es el resultado opuesto al que se busca, y compila y arranca sin un solo aviso.**

  → `MovilApplicationConfig` **debe** sobreescribir `getClasses()` devolviendo el conjunto
    explícito de sus recursos y su filtro. Una clase que no esté en ese `Set` no se sirve por
    `/movil`. Esa lista es *la* lista blanca; revisarla es la revisión de seguridad del frente.

**Trampa 2 — el filtro de la clave no puede ser un `@Provider` suelto.** Un
`ContainerRequestFilter` anotado `@Provider` y sin más se auto-descubre en **todas** las
Applications del deployment: le pediría la clave también a `/rest`, y **rompería el sistema
entero de intranet** (el SaaFE dejaría de funcionar).

  → El filtro va con **`@NameBinding`**: una anotación propia (p. ej. `@ClaveMovilRequerida`)
    que se pone en el filtro y en cada recurso de `/movil`. Así actúa **solo** sobre los
    recursos anotados, y — esto importa — sigue actuando aunque el escaneo automático de
    `/rest` también levante esos recursos: la protección viaja con el recurso, no con el path.

## 3. La clave compartida

- Header **`X-Movil-Key`** en cada request que el borde le hace a `/movil`.
- El valor vive como **propiedad de sistema `saa.movil.key`** en los dos WildFly (el .3 y el .4).
  No se commitea, no va en un `.properties` del WAR, no tiene valor por defecto en el código.
- **Falla cerrado, y eso es deliberado:** si la propiedad no está puesta en el .4, o llega vacía,
  o no coincide, el filtro responde **401** y no ejecuta el recurso. Un despliegue sin configurar
  deja el móvil caído — que es lo correcto: la alternativa (arrancar abierto) es la que no se
  puede permitir. Anotarlo en P4, porque es la causa más probable de "desplegué y no anda".
- El 401 del filtro sale **sin cuerpo descriptivo**: `{"mensaje":"No autorizado"}`. No dice si la
  clave falta, es corta o no coincide.
- La comparación se hace en **tiempo constante** (`MessageDigest.isEqual` sobre los bytes), no
  con `String.equals`.
- Esto es **defensa en profundidad, no la defensa principal**: la principal sigue siendo el
  firewall (el .4 solo acepta 8080 desde el .3 y la intranet). Son dos capas, no una en lugar
  de la otra.

## 4. Reglas transversales del path `/movil`

1. **Solo `GET`, y los 3 `POST` de `/auth`.** Ni un `PUT`, ni un `DELETE`, ni un `POST` de
   escritura de negocio. Si un frente futuro necesita escribir, se discute antes; no se agrega.
2. **Nunca se serializa una entidad JPA cruda.** Todo sale como DTO plano del paquete móvil.
   Motivo concreto y ya medido: `GET /rest/prst/porEntidad/{id}` devuelve `Prestamo` con sus
   `@ManyToOne` (`Entidad`, `Producto`, `Filial`, `MotivoPrestamo`) y **arrastra los datos
   personales completos del partícipe dentro de cada préstamo**. Al servidor expuesto no se le
   manda nada que no vaya a usar la app.
3. **Las fechas salen como texto, no como arreglo Jackson.** Los DTO de `/movil` declaran
   `@JsonFormat(shape = STRING, pattern = ...)`: `yyyy-MM-dd` para `LocalDate` y el ISO local
   sin zona para `LocalDateTime`. **Esto cambia la decisión §3.4 del plan**, que ponía la
   conversión en el borde: se hace acá, en el origen, una sola vez y sin que nadie tenga que
   adivinar qué campo era fecha. El borde igual valida a la salida, por si algún campo nuevo se
   escapa sin anotar.
4. **Pertenencia validada también acá.** Todo recurso que reciba un id que no sea el `idEntidad`
   (un `idPrestamo`, por ejemplo) verifica que ese recurso sea de esa entidad y devuelve **404**
   si no lo es — 404, no 403, para no confirmar que el id existe. El borde ya valida
   token↔recurso; esta es la segunda capa, y es la que sigue en pie si el borde tiene un bug.
5. **Una lista vacía es 200, no error.** Un partícipe sin préstamos, sin aportes o sin
   movimientos recibe `[]`. Nada de `IncomeException` en este path.
6. **Errores genéricos hacia afuera.** `{"mensaje": "..."}` sin `e.getMessage()`, sin stack, sin
   nombres de tabla. El detalle va al log del servidor, que se queda en la intranet.
7. **Sin paginación no se sirve una colección que pueda crecer sin techo.** Movimientos y kardex
   llevan rango de fechas o límite. **La regla es sobre las colecciones que crecen con la
   operación**, no sobre la palabra `getAll`: `CRD.APRT` tiene ~980.000 filas y su `getAll` ya
   provocó `OutOfMemoryError` (ver el JavaDoc de `AporteRest`), y el kardex o las cuotas de
   *todos* los partícipes no pueden viajar al servidor expuesto para devolver las de uno. Un
   **catálogo acotado** (`CRD.PRDC`, los productos) sí se puede listar entero y filtrar en el
   recurso: no crece con la operación y no tiene datos de nadie. *(Precisado el 2026-09-07: la
   redacción anterior decía "getAll no se usa para nada" y frenó de más — con razón — al
   implementar `/simulador/productos`.)*
8. **Cero lógica de negocio nueva.** Este path compone y recorta lo que ya calculan los
   `*Service`/`*DaoService` existentes. Si hace falta una regla nueva, no va acá.

## 5. Los endpoints

Base: `http://192.168.2.4:8080/SaaBE/movil`

### 5.1 Autenticación — delegan en `UsuarioAppService`, sin duplicar una línea de lógica

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/auth/validarCredencial` | `{identificacion, clave}` | `ValidarCredencialResponse` · 401 genérico |
| POST | `/auth/cambiarClave` | `{identificacion, claveActual, claveNueva}` | 200 · **401** clave actual mala · **400** clave nueva débil |
| POST | `/auth/desactivar` | `{identificacion, clave}` | 200 · 401 |

⚠️ **El 400 de `cambiarClave` se propaga tal cual, no se colapsa a 401.** La app trata cualquier
401 como sesión vencida y hace logout; si una clave nueva débil devolviera 401, todo partícipe
recién enrolado (`debeCambiarClave = true`) quedaría en un bucle de logout sin poder activar su
cuenta nunca. Es la razón por la que `ValidacionException` existe. Ver
`API-USUARIO-APP-MOVIL.md`.

⛔ **`crear`, `resetearClave` y `reactivar` NO se replican en `/movil`.** Son de oficina y viven
solo en `/rest/usap`.

### 5.2 Consulta — todos `GET`, todos anclados a un `{idEntidad}`

| Ruta | Se apoya en | Notas |
|---|---|---|
| `/participe/{idEntidad}` | `entd/getId` + `ParticipeDaoService.selectByEntidad(...)` | Perfil consolidado. El DAO ya tiene el `selectByEntidad`; **falta exponerlo**. DTO recortado: identificación, nombres, apellidos, contacto. Nada de columnas internas |
| `/prestamos/{idEntidad}` | `PrestamoDaoService.selectByEntidad(...)` | ⚠️ **El estado vigente es `PRSTIDST` (`idEstado`), NO `ESPSCDGO`** — trampa documentada en `CLAUDE.md`. El filtro por estado y el mapeo contra el rubro real se documentan acá cuando se implementen |
| `/prestamos/{idEntidad}/{idPrestamo}` | `prst` por id | Valida pertenencia → 404 |
| `/prestamos/{idEntidad}/{idPrestamo}/cuotas` | `DetallePrestamoDaoService.selectByPrestamo(...)` | El DAO ya lo tiene; **falta exponerlo**. `Prestamo` **no** trae las cuotas anidadas: no hay `@OneToMany`. Valida pertenencia |
| `/aportes/{idEntidad}/resumen` | `aprt/saldosPorEntidad` | El endpoint de `/rest` envuelve en `{exito, resultado}`; acá se devuelve el contenido plano |
| `/aportes/{idEntidad}/movimientos?desde=&hasta=` | `aprt/estadoCuenta` | ⚠️ **`desde`/`hasta` son obligatorios aguas abajo** (`yyyy-MM`, 400 si faltan). Si el borde no los manda, `/movil` aplica un **rango por defecto de los últimos 24 meses** y lo dice en la respuesta |
| `/cuenta-individual/{idEntidad}` | composición | Saldos de aportes + préstamos con saldo + kardex CXC del partícipe |
| `/simulador/productos` | `prdc` | Solo los vigentes que la app puede simular, recortados |
| `/simulador/simular` (POST) | `prst/simularCreditoNuevo` | No persiste nada. Único POST de datos, porque el cálculo necesita cuerpo |

### 5.3 Lo que faltaba en `saaBE` — RESUELTO el 2026-09-07 (commit `3666a6c`)

Tres de los apoyos de arriba **no existen hoy** (verificado contra el código el 2026-09-07):

- **`prtc` por entidad** y **`dtpr` por préstamo**: el método ya está en el DAO
  (`ParticipeDaoService.selectByEntidad`, `DetallePrestamoDaoService.selectByPrestamo`). Solo
  falta consumirlo desde el recurso móvil. **No se toca `ParticipeRest` ni `DetallePrestamoRest`**
  — son de otros equipos y `saaBE` es árbol compartido.
- **CXC del partícipe**: `CxcParticipeDaoService` y `CxcKardexParticipeDaoService` están
  **vacíos**, sin una sola consulta propia. Hay que agregar ahí las consultas por entidad
  (`CxcParticipe.entidad` es FK a `ENTD`; `CxcKardexParticipe.cxcParticipe` es FK a `CXCP`).
  Se agregan **métodos nuevos** a esas dos interfaces y sus `Impl`, sin tocar sus `Rest` ni sus
  `Service`. Es el único archivo de otro equipo que este frente modifica, y se declara en el commit.

**Lo que NO se hace, y el motivo:** ni `getAll` ni `selectByCriteria` desde el borde. `getAll`
traería el kardex y las cuotas de *todos* los partícipes al servidor expuesto para devolver las
de uno. `selectByCriteria` arma el JPQL leyendo los operadores **desde la base**
(`Rubro`/`DetalleRubro`, ver `CLAUDE.md`): es una dependencia silenciosa de datos de catálogo
para algo que tiene que funcionar siempre, y le da al borde un constructor de consultas genérico
justo donde la regla es lista blanca.



**Lo único que queda abierto: `/simulador/productos`.** `ProductoDaoService` no tiene ningún
método de listado (solo `selectByCodigoPetro`/`selectAllByCodigoPetro`, que buscan un código
Petro puntual). Se resuelve **sin tocar ese DAO**, que es de otro equipo: el recurso móvil usa
el `selectAll` genérico que `EntityDao` ya le da, y filtra en el recurso por
`Producto.estado == Estado.ACTIVO` (`PRDCESTD = 1`, el rubro `Estado` de siempre; lo confirma
`ProductoServiceImpl`, que setea ese valor al crear). `CRD.PRDC` es un catálogo acotado, no una
tabla transaccional — ver la regla 7 precisada. La respuesta va recortada a DTO: **nunca**
`Producto` crudo, que tiene `@ManyToOne` a `Filial` y a `TipoPrestamo`.

**Cerrado el 2026-09-07 (`48f8c2c`).** Y de paso quedó averiguado de dónde saldrían **tasa y
plazo por producto**, si algún día la app quisiera prellenarlos — porque la respuesta no era la
esperada:

- `BandaProducto`/`ConfiguracionBandaProducto` **no son parámetros de crédito**: son la
  clasificación de cartera vencida por antigüedad para provisión contable (`CRD.BNDP`: número de
  banda, períodos, `PlanCuenta`). El árbitro sospechaba que ahí vivían la tasa y el plazo, y
  estaba equivocado.
- `TipoPrestamo.tasa` (`TPPRTSAA`) existe en la entidad pero **nadie la lee**: todos los
  `getTasa()` del sistema son de `Prestamo` (`PRSTTSAA`). Es dato muerto.
- `ParametrosAmortizacion`, la entrada del simulador, **no tiene `idProducto`**: recibe monto,
  tasa y plazo como escalares sueltos. Hoy **hasta la pantalla de oficina simula con valores
  tecleados a mano**; el producto es informativo, no alimenta el cálculo.

Conclusión: prellenar tasa/plazo por producto **no es superficie existente que se pueda
exponer** — sería funcionalidad nueva, con DDL, y para todos los consumidores, no solo la app.

#### Catálogo de estados de préstamo (`PRSTIDST`) — para el filtro de la lista

Verificado el 2026-09-07 en `com.saa.rubros.EstadoPrestamo`. **Son constantes del código, no
hay que ir a la BD a buscarlas.** ⚠️ El estado vigente es `PRSTIDST` (`idEstado`), **nunca**
`ESPSCDGO` (`estadoPrestamo`), que es la FK al catálogo `CRD.ESPS` — trampa de `CLAUDE.md`.

| Id | Constante | ¿Se le muestra al partícipe? |
|---|---|---|
| 1 | `GENERADO` | flujo interno de oficina |
| 2 | `VIGENTE` | **sí** |
| 3 | `CANCELADO` | **sí** |
| 4 | `CANCELADO_ANTICIPADO` | **sí** |
| 5 | `CANCELADO_POR_NOVACION` | **sí** |
| 6 | `PENDIENTE_DE_APROBACION` | flujo interno de oficina |
| 7 | `RECHAZADO` | flujo interno de oficina |
| 8 | `DE_PLAZO_VENCIDO` | **sí** |
| 9 | `CANCELADO_POR_REVISAR` | **sí** (es un cancelado) |
| 10 | `VIGENTE_POR_REVISAR` | **sí** (es un vigente) |
| 11 | `EN_MORA` | **sí** |

**El endpoint devuelve el préstamo cualquiera sea su estado** — no se filtra nada del lado del
servidor, para que un partícipe nunca deje de ver un crédito suyo por una clasificación de la
que no sabe nada. La columna de arriba es para **agrupar el filtro de la app**, no para
esconder filas:

- **Vigentes** = 2, 8, 10, 11 · **Cancelados** = 3, 4, 5, 9
- Los de flujo interno (1, 6, 7) no son una opción del filtro, pero si a un partícipe le
  aparece uno, se lista igual bajo "otros". Ocultarlo sería peor: el crédito existe.
### 5.4 Forma real de las respuestas — medida sobre el código implementado (2026-09-07)

**Esta es la referencia que el borde congela.** Salió de la implementación, no de una
suposición: los campos son los que declaran los DTO de `com.saa.ws.movil.dto`. Todo lo marcado
*(texto)* lleva `@JsonFormat` y viaja como cadena, nunca como arreglo Jackson.

| DTO | Campos |
|---|---|
| `ParticipeMovilDTO` | `idEntidad:Long, identificacion:String, nombres:String, apellidos:String, correoPersonal:String, correoInstitucional:String, telefono:String, movil:String` |
| `PrestamoMovilDTO` | `codigo:Long, idAsoprep:Long, idProducto:Long, nombreProducto:String, fecha *(texto)*, fechaInicio *(texto)*, fechaFin *(texto)*, plazo:Long, montoSolicitado:Double, valorCuota:Double, tasa:Double, totalPagado:Double, saldoCapital:Double, saldoInteres:Double, saldoPorVencer:Double, saldoVencido:Double, saldoTotal:Double, moraCalculada:Double, diasVencido:Long, idEstado:Long` |
| `CuotaPrestamoMovilDTO` | `codigo:Long, numeroCuota:Double, fechaVencimiento *(texto)*, capital:Double, interes:Double, mora:Double, cuota:Double, saldoCapital:Double, saldo:Double, estado:Long, fechaPagado *(texto)*, capitalPagado:Double, interesPagado:Double, diasMora:Long` |
| `SaldoTipoAporte` *(reutilizado tal cual)* | `idTipoAporte:Long, nombre:String, saldo:Double` |
| `EstadoCuentaAportesMovilDTO` | `idEntidad:Long, identificacion:String, razonSocial:String, totalFaltante:Double, rangoPorDefectoAplicado:boolean, desdeAplicado:String (yyyy-MM), hastaAplicado:String (yyyy-MM), periodos:[...]` |
| `PeriodoEstadoCuentaMovilDTO` | `periodo:String, idTipoAporte:Long, nombreTipoAporte:String, esperado:Double, aportado:Double, faltante:Double, estado:String, movimientos:[...]` |
| `MovimientoEstadoCuentaMovilDTO` | `idAporte:Long, fechaTransaccion *(texto)*, valor:Double, tipoMovimiento:Long, tipoMovimientoTexto:String, glosa:String` |
| `CuentaIndividualMovilDTO` | `idEntidad:Long, saldosAportes:[SaldoTipoAporte], prestamosConSaldo:[PrestamoMovilDTO], kardexCxc:[CxcKardexMovilDTO]` |
| `CxcKardexMovilDTO` | `codigo:Long, totalDebito:Double, totalCredito:Double, saldoActual:Double, concepto:String, fechaCreado *(texto)*` |
| `SimulacionCreditoMovilDTO` | `totalCapital:Double, totalInteres:Double, totalDesgravamen:Double, totalSeguro:Double, totalAPagar:Double, valorCuota:Double, tablaProyectada:[...]` |
| `CuotaProyectadaMovilDTO` | `numeroCuota:Double, fechaVencimiento *(texto)*, capital:Double, interes:Double, cuota:Double, saldoCapital:Double, desgravamen:Double, seguroIncendio:Double, total:Double` |
| `ProductoMovilDTO` | `codigo:Long, nombre:String, codigoSBS:String, idTipoPrestamo:Long, nombreTipoPrestamo:String` |
| `MensajeMovilDTO` | `mensaje:String` |

Entrada del simulador: se reutiliza **`ParametrosAmortizacion`** tal cual (es un DTO plano de
entrada, no una entidad). Su `fechaInicio` es `LocalDateTime`: el borde la manda como **ISO
local sin zona**, nunca terminada en `Z` — Jackson descarta el offset en vez de convertirlo.

**Decisiones de composición tomadas al implementar, y por qué:**

- **`/aportes/{id}/movimientos` sin `desde`/`hasta`** aplica los últimos 24 meses y lo **declara
  en la respuesta** (`rangoPorDefectoAplicado`, `desdeAplicado`, `hastaAplicado`). El endpoint
  de `/rest` que hay debajo devuelve 400 si faltan, así que el rango por defecto es del path
  móvil, no de él. Que venga declarado evita que la app muestre "todos tus movimientos" cuando
  en realidad está mostrando dos años.
- **"Préstamos con saldo" = `PrestamoDaoService.selectVigentesByEntidad`**, o sea los **no
  terminales** (excluye CANCELADO, CANCELADO_ANTICIPADO y CANCELADO_POR_NOVACION, filtrando por
  `PRSTIDST`), no un `saldoTotal > 0`. Es el mismo criterio que usa `dvap/deudaVigente`, y esa
  consistencia con el resto del sistema vale más que una definición propia inventada para el
  móvil. Los dos conjuntos son casi siempre el mismo.
- **La respuesta del simulador tiene DTO propio** en vez de reutilizar
  `ResultadoSimulacionCreditoNuevo`: ese es el contrato congelado de
  `/rest/prst/simularCreditoNuevo` y anotarle `@JsonFormat` le habría cambiado el formato de
  fecha a un consumidor existente.

⚠️ **Hallazgo a tener presente, no es un defecto de este frente:**
`selectVigentesByEntidad` **absorbe los errores de BD y devuelve lista vacía** — es el patrón
deliberado de la casa para procesos por lotes (`docs/general/CORRECCION_MANEJO_EXCEPCIONES_DAO.md`),
y ahí tiene sentido. En la app significa que, si esa consulta falla, el partícipe ve **"no tenés
créditos" en vez de un error**. No se cambió, porque el método es de otro frente y el
comportamiento es intencional; queda anotado para que nadie diagnostique dos veces el mismo
"se ven bien pero salen vacíos".
## 6. Cómo se verifica que la lista blanca sigue siendo blanca

Antes de cada despliegue, tres controles — dos se leen y uno se corre:

1. `MovilApplicationConfig.getClasses()` no devuelve `null`, no está vacío, y cada clase de la
   lista es del paquete móvil. Una clase de fuera del paquete es un defecto grave.
2. Cada recurso de esa lista lleva `@ClaveMovilRequerida` y no tiene ningún método
   `@PUT`/`@DELETE`, ni un `@POST` que no sea de `/auth` o el simulador.
3. Con el WAR desplegado: `curl` sin el header → **401**; `curl` a
   `/SaaBE/movil/prst/getAll` (un recurso de `/rest` que NO está en la lista) → **404**. Si eso
   devuelve 200, la Application quedó vacía: es la trampa 1.

## 7. Registro de cambios

| Fecha | Cambio |
|---|---|
| 2026-09-07 | Creación. Path `/movil` y clave compartida decididos por el usuario; las dos trampas de JAX-RS y el cambio de la §3.4 del plan (fechas en el origen) los aporta el árbitro |
| 2026-09-07 | Implementado (`3666a6c`) y revisado por el árbitro. Se agrega la §5.4 con la forma real de cada respuesta, se precisa la regla 7 (prohibía `getAll` por su nombre y no por su motivo, y frenó de más el endpoint de productos) y se anota que `selectVigentesByEntidad` absorbe errores y devuelve lista vacía |
| 2026-09-07 | Cerrado `/simulador/productos` (`48f8c2c`): la lista blanca queda completa. Se documenta que tasa y plazo por producto no existen hoy en `saaBE` para ningún consumidor |
| 2026-09-07 | Se documenta el catálogo real de `PRSTIDST` (11 estados, de `com.saa.rubros.EstadoPrestamo`) y cómo se agrupa para el filtro de la app, que había quedado sin poder implementarse |
