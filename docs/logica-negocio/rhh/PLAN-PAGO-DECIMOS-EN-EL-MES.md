# Plan — pago del décimo acumulado durante el mes, y su novedad en el rol

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-07 · **Módulo:** `rhh`
**Estado:** 🔵 **decisiones cerradas el 2026-09-07 (§7). Backend despachado.**
Las preguntas del §5 quedan como registro de qué se preguntó y por qué; **las respuestas están en el §7.**

**Pedido del usuario, textual:**

> *«Así como se programó el pago de anticipos, se requiere programar el pago de décimos en los meses
> correspondientes. Recuerda que el procesar rol cierra las cuentas de pago de décimos pero
> necesitamos realizar el pago del décimo durante el mes. Es decir, hasta cierto día del mes más o
> menos se procesa el pago, se tiene que procesar la acreditación de los décimos a las cuentas de los
> empleados y después al final del mes cerrar roles con el pago de esos décimos registrados ya como
> novedad. Todo generando la contabilidad correspondiente.»*

**Decisiones del usuario, 2026-09-07:**

| # | Decisión |
|---|---|
| **E1** | Aplica **sólo a modalidad ACUMULADO**. El mensualizado sigue como está: renglón dentro del rol, dentro del neto |
| **E2** | El décimo ya pagado vuelve al rol como **novedad en `RHH.NVNM`**, no como cuota de descuento |

---

## 1. 🟢 Lo que YA existe — y es casi todo el ciclo

**El contrato `API-PAGO-BENEFICIOS-SOCIALES.md` §0 dice «estos endpoints todavía no existen».
Eso está desactualizado.** Verificado contra el código el 2026-09-07: el ciclo está implementado.

| Pieza | Dónde | Estado |
|---|---|---|
| Cabecera consolidada `RHH.ODBS` | tabla creada por el `e2-03` | ✅ |
| `POST /rest/odbs/generar` | `OrdenBeneficioSocialRest:165` | ✅ |
| `GET /rest/odbs/detalle/{id}` · `/listar` | `:200` · `:219` | ✅ |
| `POST /rest/odbs/enviarATesoreria/{id}` | `:248` — crea el `PagoProgramado` y lo manda a la bandeja | ✅ |
| `confirmarPago` · `anular` | `OrdenBeneficioSocialService:100` · `:112` | ✅ |
| Asiento de baja de provisión | dentro de `confirmarPago` | ✅ |

`OrdenBeneficioSocialServiceImpl` son 550 líneas implementadas.

> **O sea que «programar el pago del décimo como el anticipo» YA ESTÁ HECHO.** `enviarATesoreria`
> hace exactamente lo mismo que `AnticipoEmpleadoServiceImpl:210`: llama a
> `registrarPagoDeOrigenExterno` y el pago cae en la bandeja de tesorería, donde se aprueba, entra a
> un lote, sale en el archivo bancario y se acredita a la cuenta del empleado.

**Antes de escribir una línea nueva hay que probar ese ciclo**, porque el frontend no lo consume:
el diagnóstico del plan anterior midió **cero apariciones** de `odbs`/`lqbs` en `saaFE/src`. Puede
estar entero y sin usar, que es distinto de no existir.

---

## 2. 🔴 Lo que falta de verdad — y es la mitad del pedido

### 2.1 Nadie crea la novedad. El rol no se entera del pago.

Medido: `OrdenBeneficioSocialServiceImpl` y `BeneficioSocialServiceImpl` **no mencionan
`NovedadNomina` ni una vez**. Los únicos que crean novedades en todo el backend son
`NovedadNominaServiceImpl` (el ABM) y `SolicitudVacacionesServiceImpl`.

**Consecuencia:** hoy se puede pagar el décimo acumulado a mitad de mes y el rol de fin de mes no
tiene forma de saberlo. No hay doble pago —el acumulado no va al neto— pero **el rol no refleja el
hecho**, que es justamente lo que el usuario pide.

**El precedente a copiar es `SolicitudVacacionesServiceImpl`**, que ya resuelve el mismo problema:
crear una novedad desde otro proceso y poder encontrarla después. Ojo con cómo lo hace, porque es
una decisión de diseño heredada: **no hay FK de la solicitud a la novedad**; la novedad se marca con
`"Solicitud de vacaciones #{codigo}"` en la descripción y se la recupera por texto
(`NovedadNominaDaoService.selectPorDescripcion`). ⚠️ Eso es frágil —una descripción es texto libre—
y **no conviene repetirlo**: para el décimo hay que decidir si se agrega una FK real (columna nueva
en `NVNM`) o se acepta la misma convención. Va en §5.

### 2.2 🔴 `DECIMOS_POR_PAGAR` está declarado y MUERTO

`RhhLineaAsiento.DECIMOS_POR_PAGAR = 17`. Buscado en todo el backend: **la única aparición es su
propia declaración.** Ningún asiento la usa.

Esto importa porque el usuario dice *«el procesar rol cierra las cuentas de pago de décimos»*. Hoy,
para el acumulado, el rol **no cierra ninguna cuenta de décimos**: genera la provisión
(`PROVISION_DECIMO_*_POR_PAGAR`, líneas 40 y 41) y la baja la hace `confirmarPago` del ODBS, contra
banco. La línea 17 no participa.

**Hay que aclarar qué cuenta se supone que cierra el rol**, porque de eso depende si el asiento
actual de `confirmarPago` está bien o le falta un paso. Va en §5.

### 2.3 «Los meses correspondientes» no está modelado

`generar` recibe `tipoBeneficio`, `anio` y `region`, pero nada valida **en qué mes** corresponde
pagar. El décimo tercero vence en diciembre; el cuarto en marzo (Costa) o agosto (Sierra), y la
región ya está modelada (`RhhRegionDecimoCuarto`, rubro 187). El *«hasta cierto día del mes»* del
pedido tampoco existe como parámetro.

---

## 3. El ciclo propuesto

```
  Día N del mes que corresponde
    POST /rest/odbs/generar            -> agrupa las LQBS del tipo y año        [YA EXISTE]
    POST /rest/odbs/enviarATesoreria   -> PagoProgramado en la bandeja          [YA EXISTE]
         tesorería aprueba, lote, archivo bancario, acreditación                [YA EXISTE]
    POST /rest/odbs/confirmarPago      -> LQBS pagadas + asiento de baja        [YA EXISTE]
                                       -> ⭐ CREA LA NOVEDAD POR EMPLEADO       [FALTA]

  Fin de mes
    procesar rol                       -> lee las novedades aprobadas del período
                                          y refleja el décimo ya pagado         [FALTA definir §5]
```

**Lo nuevo es un solo punto de enganche:** al final de `confirmarPago`, después de marcar las
`LQBS` y generar el asiento, crear una `NovedadNomina` por empleado del período abierto.

---

## 4. Por qué el enganche va en `confirmarPago` y no antes

Podría engancharse en `enviarATesoreria` —cuando el pago se programa— y sería más temprano. **No
conviene:** en ese momento el pago todavía no ocurrió; puede quedar en la bandeja sin aprobar, o
anularse. Una novedad creada ahí afirma un hecho que aún no pasó, y si el pago se cae hay que
borrarla, con el rol posiblemente ya procesado.

`confirmarPago` ya exige que el `PagoProgramado` esté `CONFIRMADO` (409 si no). **Es el único
momento del ciclo en que el dinero salió de verdad.**

⚠️ Y por eso mismo hay que resolver el reverso: `anular` ya contempla revertir en tesorería. **Si se
anula una orden pagada, la novedad tiene que morir con ella**, y hay que decidir qué pasa si el rol
de ese período ya se procesó.

---

## 5. ⛔ Decisiones abiertas — no se despacha sin esto

| # | Pregunta | Por qué no la decido yo |
|---|---|---|
| **A** | **¿Con qué `ConceptoNomina` se crea la novedad?** ¿Se reusa el concepto del motor `DECIMO_TERCERO`/`DECIMO_CUARTO` (que hoy es el del **mensualizado**, y suma al neto), o hace falta un concepto nuevo «décimo acumulado pagado» que sea **informativo y no afecte el neto**? | Reusar el del mensualizado haría que el acumulado **entre al neto y se pague dos veces**. Es la decisión más peligrosa de este frente |
| **B** | **¿Qué cuenta cierra el rol?** `DECIMOS_POR_PAGAR` (17) está declarada y no la usa nadie. ¿El asiento de `confirmarPago` (DEBE provisión / HABER banco) es el correcto y completo, o falta un paso que pase por la 17? | Es contabilidad, y el asiento actual ya está escrito y andando |
| **C** | ¿La novedad se enlaza con **FK real** (columna nueva en `RHH.NVNM`) o con la convención de descripción que usa vacaciones? | La FK es más sólida pero es DDL sobre una tabla del motor |
| **D** | *«Hasta cierto día del mes»* — ¿es un parámetro configurable, o práctica operativa sin control en el sistema? | Si es control, hay que decidir dónde vive y qué hace al pasarse |

---

## 6. Riesgos

| # | Riesgo | Mitigación |
|---|---|---|
| 1 | 🔴 **Doble pago** si la novedad usa un concepto que suma al neto | Decisión A, antes de codear |
| 2 | 🔴 El ciclo ODBS **nunca se probó** — no tiene consumidor en el frontend | Probarlo de punta a punta **antes** de agregarle nada |
| 3 | 🟠 Anular una orden pagada con el rol ya procesado | Definir el reverso junto con el alta, no después |
| 4 | 🟠 `rhh` toca `PagoProgramado`, que es de `cxp` y lo comparte `lap-saa-1` | El enganche es aditivo y dentro de `rhh`; no se toca `cxp` |

---

## 7. ✅ Las cuatro decisiones, cerradas el 2026-09-07

| # | Decisión del usuario |
|---|---|
| **A** | **Concepto NUEVO**, no se reusa el del mensualizado |
| **B** | **El asiento de `confirmarPago` está bien** — no se toca, y `DECIMOS_POR_PAGAR` (17) sigue sin uso |
| **C** | La novedad se enlaza **como vacaciones**: por convención de descripción, sin FK nueva |
| **D** | *«Hasta cierto día del mes»* es **práctica operativa, sin control en el sistema** |

---

### 7.A 🟢 El concepto informativo ya tiene mecanismo — el motor NO se toca

Verificado: `RhhTipoConceptoNomina` ya define **`INFORMATIVO = 5`**, y el motor arma el neto sumando
**sólo** `INGRESO` y `EGRESO`:

```java
// ProcesoNominaServiceImpl:1078-1080 y :1493-1494
Double ingresos   = sumaPorTipo(renglones, RhhTipoConceptoNomina.INGRESO);
Double descuentos = sumaPorTipo(renglones, RhhTipoConceptoNomina.EGRESO);
```

**Un concepto `INFORMATIVO` no entra al neto por construcción.** No hace falta tocar
`ProcesoNominaServiceImpl` ni agregar ninguna guarda: el riesgo #1 del §6 —el doble pago— queda
cerrado por el tipo del concepto, no por una validación que alguien pueda olvidar.

> Es el criterio del §24 del estado del equipo, en su versión buena: **la condición se expresa en el
> dato, no en una lista de casos que hay que mantener.**

### 7.A.1 Cómo lo encuentra el motor: `CPNMROLM`, rubro 221

`conceptoPorRol` (`ProcesoNominaServiceImpl:1803-1810`) busca por `CPNMROLM` contra
`RhhRolConceptoMotor`, que es el **rubro 221**, con los valores en `SCP.PDTR.PDTRALTR`.
**El último valor usado es el 31** (`FINIQUITO_APORTE_PERSONAL`).

Así que el concepto nuevo necesita **tres cosas, no una**:

| # | Qué | Dónde |
|---|---|---|
| 1 | Dos constantes nuevas en `RhhRolConceptoMotor` (32 y 33) | Java |
| 2 | Dos filas en `SCP.PDTR` del rubro **221**, alternos 32 y 33 | ⚠️ **`.sql`** |
| 3 | Dos filas en `RHH.CPNM` con `CPNMTPCN = 5` y `CPNMROLM = 32/33` | ⚠️ **`.sql`** |

Son **dos** conceptos y no uno, porque el ODBS ya separa el tipo de beneficio (`1` décimo tercero,
`2` décimo cuarto) y el rol tiene que poder distinguirlos en el detalle.

⛔ **Sin la fila de `SCP.PDTR` el catálogo queda incompleto** y la pantalla de conceptos no puede
mostrar el rol. Es exactamente lo que costó el `e2-08`/`e2-13`: una constante en Java cuyo detalle
de rubro no existía en la base.

### 7.B El asiento no se toca

`confirmarPago` genera DEBE provisión por pagar (40/41) / HABER banco (51), y el usuario confirma
que está bien. **`DECIMOS_POR_PAGAR` (17) queda declarada y sin uso**, como está hoy. Se deja
anotado acá para que el próximo que la encuentre no crea que es un cabo suelto de este frente.

### 7.C El enlace, por descripción — con los ojos abiertos

Se repite la convención de `SolicitudVacacionesServiceImpl`: la novedad se marca en su descripción
y se la recupera con `NovedadNominaDaoService.selectPorDescripcion`. **Formato acordado:**

```
Décimo <tercero|cuarto> acumulado — orden #{idOrden}
```

⚠️ **Lo que esta decisión acepta a conciencia:** la descripción es texto libre de 300 caracteres y
nada impide que alguien la edite desde el ABM de novedades y rompa el vínculo. Se elige igual por
coherencia con el precedente y para no meter DDL en una tabla del motor. **Si algún día hay que
darle FK, el cambio es aditivo** y este párrafo explica por qué no se hizo ahora.

### 7.D El plazo legal SÍ existe, y ya estaba en el repositorio

El usuario pidió verificar si la normativa fija una fecha límite. **La hay, y no hizo falta buscarla
afuera** — está en `ANALISIS-MODULO-RRHH.md:107-112`:

| Beneficio | Período de cálculo | **Pago hasta** |
|---|---|---|
| Décimo tercero | 1-dic al 30-nov | **24 de diciembre** |
| Décimo cuarto — Sierra y Amazonía | 1-ago al 31-jul | **15 de agosto** |
| Décimo cuarto — Costa e Insular | 1-mar al 28-feb | **15 de marzo** |

**Decisión D: no se implementa control.** Queda como práctica operativa. Pero las fechas quedan
escritas acá, porque son el insumo del día que se quiera un aviso.

---

## 8. Lo que se despacha, y lo que NO

### Se despacha

1. **`.sql`** — las dos filas de `SCP.PDTR` (rubro 221) y los dos conceptos de `RHH.CPNM`.
   Lo escribe el árbitro. Va **antes** del WAR.
2. **Backend** — las dos constantes en `RhhRolConceptoMotor`, y el enganche al final de
   `confirmarPago`: una `NovedadNomina` por empleado del período abierto, aprobada, con el concepto
   informativo que corresponda al tipo de beneficio, valor = lo pagado a ese empleado, y la
   descripción del §7.C. Y el reverso en `anular`.

### NO se despacha todavía

⛔ **Nada del frontend.** El ciclo ODBS **no tiene consumidor** en `saaFE` y **nunca se probó**.
Construir pantalla sobre un ciclo sin estrenar es apilar dos incógnitas. Primero se prueba el ciclo
que ya existe (riesgo #2 del §6), y con eso medido se decide la pantalla.

### El caso que el agente debe reportar, no resolver

**¿Y si no hay período abierto cuando se confirma el pago?** La novedad necesita `periodoNomina`.
Si el pago se confirma después de cerrado el período del mes, o antes de que se abra, no hay dónde
colgarla. **El agente para y reporta**: elegir período por su cuenta es escribir en el rol
equivocado.
