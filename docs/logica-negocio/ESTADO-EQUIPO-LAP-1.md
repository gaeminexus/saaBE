# ESTADO — equipo `lap-saa-1`

**Árbitro:** `lap-saa-1-arb` (máquina **laptop**) · **Agentes:** `lap-saa-1-be`, `lap-saa-1-fe`
**Creado:** 2026-09-01 · **Este documento lo mantiene SOLO este equipo.**

---

## 0. Alcance, máquina y modo de trabajo

> ## ⚠️ EL ALCANCE CAMBIÓ EL 2026-09-03 — y es casi el inverso del anterior
>
> **Alcance vigente: `crd` · `cnt` · `tsr`.** ⛔ **NO TOCAR: `cxc` · `cxp` · `rhh`.**
>
> ~~Alcance hasta el 2026-09-02: `cxp` · `cxc` · `pagos` · `tsr` · `rhh` · `sri`, con `crd` vedado.~~
>
> **Lo de abajo (§1 a §8) se escribió bajo el alcance viejo y se conserva como registro histórico.**
> El §3, el §6 y el §7 describen frentes de `cxc`/`cxp`/`sri` que **ya no son de este equipo**.
>
> ### Qué quedó huérfano al cambiar, y hay que decirlo en voz alta
>
> | Módulo | Quién lo tiene ahora | Qué queda sin dueño |
> |---|---|---|
> | `cxc` · `cxp` · `rhh` | `omen-saa-2` (`eq2`) — tomó `cxc` el 2026-09-03 | Coordinado: el secuencial fiscal **D2b/D2c** sigue esperando una decisión del usuario |
> | **`sri`** | **NADIE** | ⛔ **El XML del ATS nunca se validó contra el XSD ni el validador oficial.** Bloqueante antes de cualquier presentación real, y hoy no lo mira ningún equipo |
>
> **`sri` es el hueco real de este cambio.** `omen-saa-3` murió, `omen-saa-2` lo excluyó de su
> alcance, y este equipo salió. No es que esté pausado: **no tiene dueño.**
>
> ### Y el alcance nuevo entra en territorio ocupado
>
> Los tres módulos que entraron tienen equipos activos encima, verificado contra `git log` el mismo
> día del cambio:
>
> | Módulo | Quién más está adentro |
> |---|---|
> | `crd` | **`omen-saa-1` (`eqB`)**, con decenas de commits el 2026-09-03. El equipo A (`saabe-25`, `eqA`) parece apagado desde el 08-31 y **`eqB` absorbió sus frentes** (jubilados, contabilidad) |
> | `tsr` | **`omen-saa-2` (`eq2`)**, frente de gasto de caja chica, commiteando el mismo día |
> | `cnt` | Compartido por diseño (§4 del registro de reservas). Sin frente activo de nadie |
>
> **Acuerdo con `omen-saa-1-arb` del 2026-09-03, sobre el frente de pago a jubilados:**
> **el `saaBE` es de ellos, el `saaFE` es de este equipo.** No se escribe Java de `crd` desde acá sin
> renegociarlo.

**Checkout: `C:\work\saaBE\v1\saaBE` y `C:\work\saaFE\v1\saaFE`** — máquina distinta de la de los
equipos OMEN, mismo `origin`.

**Marcador de commit: `lap1`** (§2d del registro de reservas). **Prefijo de scripts `.sql`:
`lap1-`** (§2b del mismo registro).

**Despacho: modo directo.** El árbitro manda los prompts por `SendMessage` y recibe los reportes sin
pasar por el usuario; se lo interrumpe sólo para una decisión de negocio o un `.sql` que haya que
correr.

### 0.1 Convivencia con `omen-saa-2` — la regla operativa de este equipo

**`omen-saa-2` (máquina OMEN) está activo sobre `rhh`, `cxp`, `pagos`, `tsr` y `cnt`.** Cuatro de
sus cinco módulos son también de este equipo. **Decisión del usuario (2026-09-01): no se reparte el
alcance.** Él conoce el sistema y asigna las tareas de modo que no se pisen.

**La salvaguarda es de los agentes, y es obligatoria:**

> **Antes de tocar un archivo, `git status` y `git log -3` sobre él. Si aparece modificado, o
> commiteado hoy por un marcador que no sea `lap1`, PARAR y reportarlo al árbitro.** No editarlo y
> avisar después.

**Por qué acá el riesgo es distinto —y menor— que el que registró `omen-saa-3` en su §9.6:** ese
equipo compartía *working tree* con otro, donde un `git clean` ajeno se lleva trabajo sin commitear.
Acá las máquinas son distintas; lo que queda es **trabajo duplicado y carreras de `push`**, que un
`git fetch` antes de empezar y commits frecuentes sí mitigan.

⚠️ **`ESTADO-EQUIPO-OMEN-2.md` §0 afirma que «los equipos paralelos se cerraron el 2026-09-01» y que
«`cxc` y `sri` quedan sin dueño, nadie los está trabajando hoy».** Era cierto al escribirse y dejó de
serlo horas después, cuando arrancó este equipo. **No es un error de quien lo escribió** — es la
misma forma que su propio documento ya tiene registrada: un estado verdadero al momento de anotarlo
envejece sin que nadie lo toque.

---

## 1. Verificación de arranque — 2026-09-01

Hechos comprobados en **esta** máquina, no leídos de la documentación.

### 1.1 ⛔ Acá NO se puede compilar el backend

`mvn -v` en bash → *command not found*. `Get-Command mvn` en PowerShell → no está en el PATH.
`C:\Program Files\maven` **no existe**. `java` sí está (`javapath`).

**Consecuencia, y va en el prompt inicial del agente de backend:** la única verificación de un
cambio de Java desde este equipo es la lectura de código del árbitro más la compilación del usuario
en Eclipse. **Un `mvn` que no existe no es un error del código** (`CLAUDE.md`); no hay nada que
arreglar cuando el comando falla.

Fila para la tabla de `CLAUDE.md`:

| Verificado | Máquina | Resultado |
|---|---|---|
| 2026-09-01 | laptop (`lap-saa-1`) | `mvn` no se reconoce; `C:\Program Files\maven` no existe |

### 1.2 ✅ El frontend SÍ se verifica acá

`node`, `npm` y `ng` están en el PATH (`C:\nvm4w\nodejs`) y `saaFE/node_modules` está poblado. **`ng
build` es verificación real** de todo lo que entregue el agente de frontend.

⚠️ **`tsc --noEmit -p tsconfig.json` no valida nada en este repositorio** — el `tsconfig.json` es
solution-style (`files: []` + `references`) y sin `-b` compila cero archivos con exit 0 siempre.
Para un chequeo rápido, `tsconfig.app.json`. *(Hallazgo heredado de `omen-saa-3`, §4 de su estado.)*

### 1.3 ✅ Los dos repositorios limpios y sincronizados

`git fetch` en ambos: `HEAD == origin/main`, `git status` vacío. `saaBE` en `3a6a2b1`, `saaFE` en
`c90cd76`. Nadie tiene trabajo a medio camino sin commitear.

### 1.4 ✅ Un 🔴 del estado heredado ya está cerrado, y seguía marcado como abierto

`ESTADO-CXP-CXC-TSR-RHH-SRI.md` §9.3 afirma que `tsr/sql/README-ORDEN-PRODUCCION.md` *«sigue llegando
hasta el 06»*, y que por eso un despliegue volvería a saltarse los scripts `07` y `08`.

**Falso hoy:** ese README lista **siete pasos**, con el `07` en la posición 6 (incluida la
advertencia de `ORA-00942` sobre `TSR.DTCN`) y el `08` en la 7. Se corrigió sin anotarlo.

**Lo que sigue abierto es otra cosa, y conviene no confundirlas:** que los scripts estén *listados*
no dice que se hayan *ejecutado*. No hay registro de que el `07` ni el `08` se hayan corrido. Sólo lo
cierra una consulta a la base — ver §3.2.

### 1.5 La §6 del registro de reservas cambia cómo se reserva un rubro

**Escrita hoy por el árbitro de `omen-saa-2`, y aplica a todo lo que este equipo escriba.** Ese
registro controlaba `PRBRCDGO`, pero **el código busca los rubros por `PRBRALTR`** (código alterno)
vía `DetalleRubroDaoService.selectValorStringByRubAltDetAlt`. Dos equipos pueden cumplir el registro
al pie de la letra, tomar `PRBRCDGO` distintos y **el mismo alterno**: no falla al insertar, falla al
**leer**, devolviendo el rubro del otro.

**Regla para este equipo:** al reservar un `PRBRCDGO`, anotar **también** el `PRBRALTR`, usar la
convención `PRBRALTR = PRBRCDGO`, y verificar los dos antes de ejecutar.

> **Cómo me enteré, y la lección vale más que el dato:** mi primera lectura de
> `REGISTRO-RESERVAS-EQUIPOS.md` salió **truncada por el límite de salida de la herramienta** y
> terminó justo antes de esa sección, en un punto donde el texto cerraba de forma perfectamente
> plausible. **Una lectura truncada no se anuncia: se ve igual que una completa.** Lo detecté por
> casualidad, al mirar el `git log` del archivo antes de editarlo y encontrar un commit que hablaba
> de algo que yo no había visto. **Contrastar `wc -l` contra lo último que se leyó** cuesta nada.

---

## 2. Deuda de contratos de API — el frente estructural de este equipo

| Módulo | `API-*.md` en `docs/logica-negocio/` |
|---|---|
| crd | 14 |
| cnt | 2 |
| cxp · rhh | 1 cada uno |
| **cxc · pagos · tsr · sri** | **0** |

Los contratos de los frentes **R** (anulación con cascada, 9 documentos), **S** (bandeja universal de
pagos) y **Q** (ATS + cuadre 103/104) viven **dentro de documentos de plan**, no como contrato.

**El frente R es el que peor tolera esa deuda:** tiene tres trampas que no se deducen leyendo el
código (§3.4 del estado heredado) — la **asimetría deliberada** entre compra `/anular/{id}` y venta
`/anular` con el id en el body; el **200 con `exito:false`** para documento inexistente o ya anulado,
que no se ve mirando el status HTTP; y la **forma heterogénea** de `movimientosRelacionados` según el
tipo de documento (las notas de crédito y débito devuelven `idFactura`, no `tipoDocPago`).

### 2.1 ⚠️ Trampa al espejar: en `saaFE` la carpeta de RRHH es `docs/rrh/`

Tres letras, no cuatro, y **no** coincide con `docs/logica-negocio/rhh/` del backend. Espejar a
`saaFE/docs/rhh/` deja al frontend sin poder leer el contrato — el fallo es silencioso, porque el
archivo se crea igual.

Carpetas reales en `saaFE/docs/`: `cnt`, `crd`, `cxc`, `cxp`, `pagos`, **`rrh`**, `tsr`,
`transversal`, `patrones`, `propuestas`, `historico`. **No existe `sri`**: hay que crearla.

Recordatorio de la ruta, que ya dejó a un frontend sin contrato: en `saaBE` es
`docs/logica-negocio/{modulo}/`, en `saaFE` es `docs/{modulo}/` — **sin** `logica-negocio`.

---

## 3. Inventario de lo abierto en el alcance — al 2026-09-01

Heredado de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` (equipo `omen-saa-3`, hasta el 31-08) y verificado.

### 3.1 `cxc` / `sri` — sin otro equipo encima

| # | Qué | Estado |
|---|---|---|
| **D2b** | **Secuencial `000000000` en liquidación de compra.** `obtenerSecuencial` (`LiquidacionCompraServiceImpl:1657-1676`) devuelve `numActual` y recién después incrementa | 🔴 **decisión del usuario**: ¿dato (`numActual=0`, se arregla con `UPDATE`) o código (debe pre-incrementar, y entonces *todos* los tipos están corridos en uno)? Elegir mal reordena numeración fiscal |
| **D2c** | **Cada intento fallido quema un secuencial.** El `UPDATE` va antes de saber si el SRI acepta, y un `DEVUELTA` no lanza excepción → la transacción commitea el incremento | 🔴 se resuelve junto con D2b |
| — | **`RetencionServiceImpl`** — generador viejo de retenciones. Se le aplicó el arreglo del tipo de identificación en vez de retirarlo, porque nadie confirmó que esté muerto | 🟠 latente. Es la misma condición del `TSR.TSRD` de §7.4b: código que no falla hasta que alguien lo conecta |
| — | **El XML del ATS nunca se validó contra el XSD ni el validador oficial del SRI** | ⛔ **bloqueante antes de cualquier presentación real** |
| — | **Casillas 103/104 con sufijo** (`303A`, `304B`) sin mapear — a propósito, esperan declaraciones reales del usuario | 🟡 |
| — | **§7.4b** `TSR.TSRD` no existe; consulta nativa en `TitularDaoServiceImpl:101`, hoy sin llamadores | 🟠 latente |
| — | **§7.4c** `CBR.TDCC` (13 col.) y `CBR.TFDC` (8 col.) mucho más angostas que su entidad; `PGS.DTDP` no existe (ojo: `TSR.DTDP` sí — mismo código en dos esquemas) | 🟠 un solo endpoint vivo alcanzado: `procesos/cobros/ingresar` → `TSR.TCBR` |

**Ya corregido, verificado contra el código y no tomado de palabra:** D1 (`ORA-01400` en
`CBR.ANTC.ANTCAPLC`) está cerrado — `model/cxc/AnticipoCliente.java:188` ya declara
`private Long aplicado = 0L;`. La causa real de D2 (`tipoIdentificacionProveedor` mandando el rubro
interno crudo donde el XSD del SRI exige el patrón `[0][4-8]`) también.

### 3.2 `cxp` / `pagos` / `tsr` / `rhh` — territorio compartido con `omen-saa-2`

**Nada de acá se toca sin la salvaguarda del §0.1.**

| # | Qué | Estado |
|---|---|---|
| — | **`tsr/sql/07` y `08` sin confirmación de ejecución.** Si `TSR.DTCN` no existe, **cualquier lectura de `DetalleTransito` revienta con ORA-00942** — y el frente N figura «✅ Cerrado BE+FE» | 🔴 **lo cierra una consulta a la base**, no una lectura de código |
| — | **10b — Novedades del período: campos.** `RHH.NVNM` sólo tiene `cantidad`/`descripcion`/`valor` y nunca tuvo columna de días | 🔴 bloqueado: falta que el usuario diga qué campo echan de menos |
| — | **Frente O · T3 — provisión de vacaciones** en el motor de nómina. Requiere tocar `ContabilizacionNominaServiceImpl`, congelado | 🟡 diferido a propósito. ⚠️ **`omen-saa-2` tiene un frente 3-B sobre exactamente esto** — coordinar antes de mirarlo |
| — | **Respuesta del banco:** el lector espera un Excel de 4 columnas armado a mano, no el formato nativo | ⚪ limitación aceptada por el usuario |
| — | `MIGRACION-CRUCES-ANTICIPO.md` (20-08), `PLAN-TECNICO-PAGOS-COBROS.md` §7.5 | ⚪ sin urgencia |

### 3.3 La regla que cruza los seis módulos: el `merge` desnudo

`EntityDaoImpl.save()` hace `em.merge()` con el objeto tal cual llegó del JSON — sin re-leer la fila
y sin saltar nulos. Como en `com.saa.model` **no hay un solo campo primitivo persistido**, una clave
ausente en el JSON deserializa a `null` y **se graba `null` en la columna real, FKs incluidas**.

- **Regla para el frontend de este equipo:** `GET` de la entidad completa → aplicar encima sólo lo
  que el formulario edita → mandar **el objeto entero**. Nunca un payload «sólo con lo que cambió».
- **Regla para el backend:** cuando una columna es **estado interno del servidor** —contadores,
  banderas de idempotencia, vínculos de reconciliación— el `saveSingle` debe **preservarla
  releyéndola**, no confiar en que el cliente la reenvíe. Ya aplicado en `AnticipoCliente`
  (`idPagoDevolucion`, `aplicado`). Candidatos pendientes: `PGS.PGTR` (estado y cuenta que asigna
  tesorería) y las entidades `Temp*`.
- ⛔ **No «arreglar» `EntityDaoImpl`.** Hereda todo el proyecto; cambiarlo alteraría todas las
  escrituras del sistema de una vez, incluidas las que hoy ponen un campo en `null` a propósito.

---

## 4. Pendientes del usuario — al 2026-09-03, alcance nuevo

### 🔴 Bloqueante
- **Correr `crd/sql/175`.** `PagoPensionComplementaria` mapea `PGPCNMDV` y `DistribucionBanda` mapea
  `CRD.DSBN`; **las dos entidades ya están en `main` y el script no corrió.** Cualquier WAR
  construido desde `main` —de cualquier equipo— rompe toda lectura de `CRD.PGPC` con `ORA-00904`.
  Es el §7 del registro de reservas en vivo. Ni este árbitro ni el de `omen-saa-1` pueden ejecutarlo.
- **Decidir quién implementa el backend de la anulación de pagos de pensión.** El usuario de
  `omen-saa-1` no respondió; su equipo está cerrando la carga Petro. El diseño y el contrato están
  escritos y verificados: es ejecutar, no diseñar. Tres caminos — esperarlos, que lo tome este
  equipo (renegociando el archivo), o dejar la pestaña de seguimiento afuera.

### 🟡 Decidible
- **¿El botón que ejecuta el pago del mes lleva restricción de usuario?** Hoy lo aprieta cualquiera
  con sesión iniciada, y **los otros dos procesos pesados de `crd`** (`bandas-de-cartera`,
  `cierre-de-cartera`) **sí** tienen `usuarioUnoGuard`.
- **Confirmar `CRD.PGCE` contra `ALL_TABLES`** antes de que exista DDL.
- **`saaFE/docs/REGISTRO-RESERVAS-EQUIPOS.md` es un espejo viejo** — 113 líneas contra 700, sin
  ninguna señal de estarlo. Está **fuera del alcance de escritura de los dos árbitros** (los dos nos
  limitamos a `docs/{modulo}/`). Hay que sincronizarlo o borrarlo; `omen-saa-1` recomienda borrarlo,
  porque el original vive en `saaBE` y todos lo leen ahí.

### ⚪ Sin prisa
- Verificar `crd/sql/174`, y la fila `'CERTIFICADO BANCARIO'` en `CRD.TPDJ` — de esa fila dependen
  los dos vistos de la pantalla de jubilados.

### ⛔ Heredado del alcance viejo, y hoy sin dueño
- **Validar el XML del ATS contra el XSD y el validador oficial del SRI. Nunca se hizo**, y `sri` no
  es de ningún equipo desde el 2026-09-03. Bloqueante antes de cualquier presentación real.
- **D2b/D2c — el secuencial fiscal.** El `.sql` de diagnóstico está escrito
  (`cxc/sql/lap1-01`). `cxc` pasó a `omen-saa-2`; la decisión sigue siendo del usuario.

---

## 5. Bitácora

| Fecha | Qué |
|---|---|
| 2026-09-01 | Arranque del equipo. Verificación de máquina (§1), inventario del alcance (§3) y reserva del bloque `PRBR` 330-349 / `PDTR` 1600-1699 en el registro |
| 2026-09-01 | `cxc/sql/lap1-01-diagnostico-secuencial-comprobantes.sql` — diagnóstico de solo lectura para D2b/D2c. Ver §6 |
| 2026-09-01 | **`cxc/API-ANULACION-DOCUMENTOS.md`** — primer contrato de este equipo, los 9 documentos. Espejado a `saaFE/docs/cxc/` + puntero en `saaFE/docs/cxp/`. Ver §7 |
| 2026-09-01 | **`BARRIDO-PAYLOADS-PARCIALES-LAP-1.md`** — 2 defectos 🔴 confirmados extremo a extremo en `rhh` (aprobación de permisos y de vacaciones), 3 probables en `tsr`, más el inventario de pantallas inalcanzables y código muerto. **Avisado al árbitro de `omen-saa-2`, que es el dueño de `rhh`; no se toca sin su ok** |
| 2026-09-01 | Hilo de coordinación con `omen-saa-2-arb` **cerrado de común acuerdo**, diagnóstico completo de los dos lados. Resultados: le corregí un contrato congelado (el `MensajeErrorJsonFilter`), retiró su propuesta de subir `extraerCodigo` devolviendo la PK (habría roto `tsr` y `cnt`), y me corrigió una lectura invertida de `bancos.component.ts`. `rhh` queda con él; los dos esperando decisión de nuestros usuarios. Ver §8 |
| **2026-09-03** | **Cambia el alcance a `crd`/`cnt`/`tsr`** (ver el recuadro del §0). Arranque del frente de **pago a jubilados**: relevamiento, diseño del rediseño de pantalla, contrato de los endpoints que faltan, y acuerdo de reparto con `omen-saa-1-arb`. Ver §9 |
| 2026-09-03 | Hilo con los otros dos árbitros sobre el registro de reservas: se le agregó un **§0 con índice**, se corrigió el remedio de la duplicación, y se renumeraron los `§` duplicados. **Cinco afirmaciones mal medidas en cadena, tres de ellas mías.** Ver §9.4 |

---

## 8. Lo que enseñó el intercambio entre árbitros — 2026-09-01

Se registra porque el mecanismo es reutilizable, no por el detalle de lo acordado.

**Los dos errores de la jornada murieron igual, y no fue releyendo.** El árbitro de `omen-saa-2`
escribió un `INSERT` con dos columnas inventadas (`PRBRNMBR`, `PRBRESTD`, que no existen en la
entidad `Rubro`); yo afirmé que `bancos.component.ts` prefería el código alterno cuando prefiere el
`codigo`. **Ninguno de los dos habría sobrevivido a que el otro fuera al archivo, y los dos
sobrevivían a que su autor releyera** — porque un autor relee con la misma hipótesis con la que
escribió.

> Es la tercera aparición en este proyecto del mismo principio —**el control y lo controlado
> compartiendo origen**— pero las dos anteriores eran entre árbitro y agente, donde se puede
> confundir con jerarquía. **Que aparezca entre pares sin jerarquía es lo que lo vuelve general.**

**Corolario propio, barato de aplicar:** un `grep` **recorta por definición — muestra lo que casa, no
lo que decide.** Mi error salió de un `grep` que devolvió la rama del alterno, que era donde casaba
el patrón, y no la rama de `codigo`, tres líneas antes. Sumado al §1.5 (lectura truncada del
registro), son **dos conclusiones invertidas en un día por lecturas parciales**. Antes de afirmar
qué hace un bloque de código, abrirlo entero.

### 8.1 Trampa registrada para el día que se unifique `extraerCodigo`

Existen **dos funciones llamadas `extraerCodigo` con criterios opuestos**:

| Archivo | Gana |
|---|---|
| `rrh/forms/parametrizacion/utiles-parametrizacion.ts:81-86` | el **alterno** |
| `tsr/forms/bancos/bancos.component.ts:161-174` (arrow local) | el **`codigo`** |

⚠️ **Borrar la local y agregar el `import` compila, no emite ningún aviso, se ve idéntico en el diff
y cambia qué fila se graba.** Si esa migración se despacha alguna vez, la sustitución va **explícita
en el prompt**, nunca dejada al criterio del agente.

---

## 6. El secuencial fiscal (D2b/D2c) — lo que acotó la pregunta

El documento heredado planteaba la disyuntiva «¿el dato o el código?» sin cerrarla. **Leyendo el
código se cierra a medias, y eso cambia qué hay que medir:**

**`obtenerSecuencial` está copiado seis veces, idéntico**, y las seis devuelven el valor **previo** al
incremento:

| Clase | Línea |
|---|---|
| `FacturaServiceImpl` | 1801 |
| `NotaCreditoServiceImpl` | 1603 |
| `NotaDebitoServiceImpl` | 1719 |
| `RetencionServiceImpl` | 1396 |
| `RetencionV2ServiceImpl` | 177 |
| `LiquidacionCompraServiceImpl` | **1806** *(el doc heredado decía 1657 — el archivo cambió desde entonces)* |

**Consecuencia:** la opción (B) «el código está mal» exigiría que **todos** los tipos de comprobante
estuvieran corridos en uno, no sólo la liquidación. **Una sola factura autorizada por el SRI con
secuencial correcto la descarta.** Eso es lo que mide el BLOQUE 3 del script.

**Dos errores del documento heredado que habrían hecho fallar el `.sql` en el cliente del usuario:**

1. Hablaba de una columna `NUMEROAUTORIZACION`. **No existe:** en las cinco tablas de comprobantes la
   columna es `AUTORIZACION` (más `FECHAAUTORIZACION`). Habría dado `ORA-00904`.
2. `SECUENCIAL` es `VARCHAR2(1000)`. Un `TO_NUMBER` pelado aborta el bloque entero con `ORA-01722` si
   **una sola** fila trae texto o vacío. El script va con `DEFAULT NULL ON CONVERSION ERROR`.

⛔ **La corrección va comentada en el script y no se descomenta hasta que el usuario decida.** El
secuencial es numeración fiscal: elegir mal el valor de arranque reordena comprobantes ya emitidos.

---

## 7. Contrato de anulación — lo que corrigió de la documentación previa

`cxc/API-ANULACION-DOCUMENTOS.md`. **El frontend habría construido contra una versión equivocada en
cuatro puntos**, todos verificados archivo:línea antes de congelar:

| La documentación previa decía | Es |
|---|---|
| «200 con `exito:false` para inexistente o ya anulado», para los nueve | **Sólo en los 4 de `cxp`.** En los 5 de `cxc` es **400** (`Response.status(exito ? OK : BAD_REQUEST)`) |
| Dos formas de `movimientosRelacionados` | **Cuatro.** `lqcs` no trae `tipoDocPagoTexto`; `rtv2` devuelve `idFacturaCompra` |
| Los nueve con contrato completo | **`lqcc` no tiene `movimientosRelacionados`, no acepta `anularEnCascada` ni `idUsuario`, y nunca devuelve 409** |
| El 409 de compra como texto plano | Llega como **`{mensaje}`** — lo envuelve `MensajeErrorJsonFilter` |

**Por qué la versión previa se equivocaba:** describía el patrón de `Factura` y `FacturaCompra` y lo
extendía por analogía. **Los dos documentos más usados son justamente los que no exhiben las
excepciones.** Un contrato se verifica en los nueve, no en los dos representativos.

**Nota de método, porque justifica el rol:** el agente de backend encontró la divergencia
`cxp`/`cxc`; el que ninguno de los dos lados podía ver es el del `MensajeErrorJsonFilter` — el
backend lee `.entity(String)` y concluye texto plano, y el frontend recibe JSON y concluye que
siempre lo fue. **El filtro vive fuera de las dos clases que cada uno estaba mirando.**

---

## 9. Frente de pago a jubilados — arrancado el 2026-09-03

**Reparto:** `saaBE` de `omen-saa-1` (`eqB`), `saaFE` de este equipo. **Documentos** (los dos
espejados a `saaFE/docs/crd/`): `crd/DISENO-PANTALLA-PAGO-JUBILADOS.md` y
`crd/API-PAGO-JUBILADOS-ANULACION-Y-PERIODO.md`.

### 9.1 Lo que se creía listo, y qué faltaba de verdad

El frente figuraba como terminado. **El backend lo estaba; el proceso no.** De ocho piezas había
**dos**: jubilar al partícipe y parametrizar el valor mensual (`VPPC`).

| 🔴 Hallazgo | Cómo se veía |
|---|---|
| **El botón «Procesar pago del mes» era una maqueta.** Un `setInterval` de 5 s que terminaba en `snackBar('Pago procesado exitosamente')`, **sin una sola llamada HTTP** | El operador apretaba y **creía que había pagado el mes** |
| **`generarPagosDelMes` no tenía ningún llamador.** `pgpc` no aparecía en un solo `.ts` de `saaFE` | Backend completo, verificado, y nadie lo invocaba |
| **No existe el timer de reconciliación.** El JavaDoc del REST dice que «normalmente lo dispara un timer»; no hay tal timer, y los `@Schedule` de los dos timers de `crd` están comentados | **Ningún pago pasaría nunca de `EN_PAGO` a `PAGADA`** |
| **Un rechazo de CXP no reversa el asiento de devengo.** `sincronizarPago` arregla el `APRT` y deja el asiento vivo | `2.3.01.10.03` acumula pasivo por pagos que no ocurrieron. Reportado a `eqB` |
| **El monto cruzado no se persiste.** `montoCruzado` es variable local: va a un `println` y al DTO, y muere ahí | Hacía **no implementable** la decisión de bloquear la anulación de pagos con cruce |

### 9.2 Dos hallazgos que abarataron el trabajo

- **Los vistos de cuenta y certificado no necesitan backend nuevo.** El certificado es un `Adjunto`
  en `CRD.ADJN` ligado por `ADJNIDRF`, y `tpdj`/`cnbp`/`adjn` ya exponen `selectByCriteria`: tres
  llamadas y un cruce en el cliente.
- **El patrón de pantalla ya existía**: `crd/forms/cierre-cartera` — período, `Previsualizar`
  separado de `Ejecutar`, spinner por acción, y un bloque de **«Desviaciones» que explica que no son
  errores**, que es exactamente la categoría del caso «cruzado íntegro, sin orden de pago».

### 9.3 Anular con cruce: el mecanismo ya existía entero

Decisión del usuario del 2026-09-03, que revirtió la anterior. Verificado contra el código:
`pagarConAportes` **ya devuelve el `idEvento`** y lo descarta; `anularOperacion` reversa el evento
**y su paso 4 `revertirAportes` devuelve el aporte consumido**; `pagoProgramadoService.anularPago`
ya lo llama CRD desde `DevolucionAporteServiceImpl:858`. **`anularPagoPension` es `anularDevolucion`
más un bucle sobre los eventos.**

Falta sólo el modelo: **`CRD.PGCE`**, tabla hija, porque `cruzarContraPrestamos` llama a
`pagarConAportes` **una vez por préstamo vigente** — N eventos por pago, no uno.

⛔ **Límite honesto:** la regla LIFO impide reversar un evento si hay operaciones posteriores
vigentes sobre el préstamo. **En la práctica se anula el último mes, no uno cualquiera del pasado**,
porque la pensión del mes siguiente vuelve a cruzar. El 409 va a ser frecuente, no excepcional.

### 9.4 La lección de método de la jornada — cinco vueltas, tres mías

Sobre los `§` duplicados del registro se encadenaron **cinco afirmaciones mal medidas**, cada una
corrigiendo a la anterior **con la misma confianza**. Ninguna fue negligente; las cinco fueron
*razonables*.

**Lo que ninguna muestra sola:**

> **El filtro es la parte invisible de una medición.** Un `grep` muestra lo que casa **dentro de lo
> que le dejaste mirar**, y el `--include` **no aparece en el resultado**: se ven los hallazgos,
> nunca lo excluido. Un hueco de filtro **no se manifiesta como error, sino como una lista más
> corta — indistinguible de una lista correcta.**

Es distinto de la nota vieja *«un `grep` recorta por definición»*: aquélla es sobre el **patrón**,
que queda escrito en el comando y se puede releer; ésta es sobre el **alcance**, que no deja rastro.

- Mi primer error: busqué `§6.[0-9]` con `--include=*.md` sobre `docs/`. La cita vivía en un `.sql`.
- Mi segundo error, **corrigiendo el primero**: anclé el `grep` a que el nombre del archivo
  apareciera **en la misma línea** que el `§`. Una cita que decía sólo «Va al registro §6» quedó
  fuera. Distinto filtro, mismo fallo.

**Práctica que sale de esto: pegar el comando junto a la afirmación.** Es la única forma de que el
alcance sea auditable por el que viene después.

Y el corolario de `omen-saa-2-arb`, que es la raíz: **un fallo suele tener varias causas plausibles,
y todas explican el resultado igual de bien.** Por eso releer no alcanza — releer confirma la
historia con la que escribiste. **Las cinco murieron porque otro fue al archivo.**
