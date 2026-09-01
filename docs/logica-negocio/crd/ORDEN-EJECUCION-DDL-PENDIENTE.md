# Orden de ejecución del DDL pendiente de CRD — **antes del WAR**

**Fecha:** 2026-08-31 · Escrito y verificado por el árbitro del equipo A (`saabe-25`)
**Estado:** ⚠️ **PARCIALMENTE EJECUTADO — ver §5 antes de correr nada.** Lo corre el usuario.

---

## 0. Por qué esto cambió de prioridad

Hasta hoy estos scripts figuraban como *"no corren hasta que exista el código que los usa"*.
**El código ya existe** — se escribió el 2026-08-31, antes del colapso de las sesiones. El orden se
dio vuelta: **ahora el DDL va ANTES del WAR.**

**El fallo que esto evita.** Hibernate incluye **toda** columna `@Column` básica en el `SELECT` que
genera. Una columna mapeada que no existe en la base no rompe solo la función nueva: rompe
**cualquier lectura de esa entidad**, con `ORA-00904`, en pantallas sin relación aparente. No se ve
al compilar y aparece cuando un usuario abre la pantalla.

**Y hay una tabla ya viva en producción del lado equivocado:** `CRD.ACCN` existe desde el
2026-08-30 y la pantalla de condonación está desplegada. La entidad `AcuerdoCondonacion` mapea hoy
tres columnas que la tabla **no tiene**. Desplegar el WAR sin correr el 84 y el 86 no rompe una
función nueva: **rompe la pantalla de condonación entera.**

### Lo que cada script destraba

| Script | DDL | Ya mapeado en el código | Si no corre |
|---|---|---|---|
| **84** | `ACCN.ACCNVLAP`, `ACCN.ACCNVLDP` + tabla `CRD.DAAP` | `AcuerdoCondonacion:98,111`, entidad `DetalleAporteAcuerdoCondonacion` | toda lectura de `ACCN` falla |
| **85** | tabla `CRD.DAPR` | entidad `DetalleAportePrecancelacion` + DAO | la precancelación mixta no persiste |
| **86** | `ACCN.PJRQCDGO` | `AcuerdoCondonacion:197` | toda lectura de `ACCN` falla |
| **87** | `AVPC.TPAPCDGO` + catálogo | `AfectacionValoresParticipeCarga` | toda lectura de `AVPC` falla (carga Petro) |

---

## 1. ⚠️ El orden entre el 81 y el 87 NO es indistinto

**Verificado el 2026-08-31 leyendo los dos scripts.**

| Script | PDTR | Rubro | Alterno | Deja la secuencia en |
|---|---|---|---|---|
| 81 (`JUBILACION`) | 1178 | 235 | **7** | `RESTART START WITH 1179` |
| 83 (`COBRO_MIXTO`) — **ya corrido** | 1179 | 245 | 7 | — |
| 87 (`EXCEDENTE_PETRO`) | 1180 | 235 | **8** | `RESTART START WITH 1181` |

**No hay colisión de códigos:** el 81 y el 87 comparten el rubro 235 pero usan alternos distintos
(7 y 8), y PDTR distintos (1178 y 1180). El alterno 7 del script 83 es de **otro rubro** (245), así
que no choca. Esto ya estaba previsto en `REGISTRO-RESERVAS-EQUIPOS.md`.

**Pero el `ALTER SEQUENCE` sí colisiona si se corren al revés.** El 81 deja la secuencia en 1179 y
el 87 en 1181. Si corrés el 87 primero y el 81 después, **la secuencia queda en 1179 con 1179 y
1180 ya ocupados**, y el próximo rubro creado desde la aplicación muere por PK duplicada — en una
pantalla sin ninguna relación con lo que hiciste.

> **Regla: el 81 va antes que el 87.** Y como los dos `ALTER SEQUENCE` están **comentados** en sus
> scripts, corré **solo el del 87** (`RESTART START WITH 1181`), al final, una vez.

---

## 2. Orden de ejecución

**Antes de empezar**, los tres controles obligatorios del registro de reservas:

```sql
SELECT MAX(PRBRCDGO) AS MAX_PRBR FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'SCP'
AND    s.SEQUENCE_NAME IN ('SQ_PRBRCDGO','SQ_PDTRCDGO');
```

Se espera `MAX_PDTR = 1179` (lo dejó el script 83). **Si da otra cosa, parar y avisar** — significa
que alguien más insertó, y el rango del equipo A ya no es el que este documento supone.

| Paso | Script | Qué es | Nota |
|---|---|---|---|
| 1 | `81_RUBRO_MOVIMIENTO_JUBILACION.sql` | PDTR 1178, rubro 235 alterno 7 | **antes que el 87**, por la secuencia |
| 2 | `84_ACUERDO_PAGO_CON_APORTES.sql` | columnas de `ACCN` + tabla `DAAP` | DDL puro, sin catálogo |
| 3 | `86_ACUERDO_EMPRESA.sql` | `ACCN.PJRQCDGO` + FK a `SCP.PJRQ` | DDL puro. **No necesita `GRANT REFERENCES`** — ver abajo |
| 4 | `85_PRECANCELACION_MIXTA_APORTES.sql` | tabla `CRD.DAPR` | DDL puro |
| 5 | `87_EXCEDENTE_PETRO_A_APORTES.sql` | `AVPC.TPAPCDGO` + PDTR 1180 | **último**, y acá va el `ALTER SEQUENCE ... START WITH 1181` |

Los pasos 2, 3 y 4 son intercambiables entre sí. **El 1 antes del 5 no lo es.**

### Sobre el `GRANT REFERENCES` del paso 3 — verificado, NO hace falta

Una FK cross-schema normalmente exige `GRANT REFERENCES` corrido como owner del schema apuntado, y
el rol DBA no lo habilita solo — fue lo que trabó `DDL-COBRO-PETRO-DOS-PASOS.sql` con `TSR`.

**Acá no aplica, y la evidencia es que ya pasó dos veces:** `DDL-BANDAS-PRODUCTO.sql` (`FK_CBPR_PJRQ`)
y `DDL-CIERRE-CARTERA.sql` (`FK_CRCT_PJRQ`) crean FKs de `CRD` a `SCP.PJRQ` y **los dos corrieron en
producción**. Si el grant faltara, habrían fallado. `CRD` ya tiene `REFERENCES` sobre `SCP.PJRQ`.

### Después de correr todo, y antes del WAR

```
docs/logica-negocio/crd/sql/VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql
```

**Completo, las DOS consultas.** La B es la que encuentra columnas mapeadas que faltan; ya atrapó
una caída de producción el 2026-08-30.

Dos advertencias al leer el resultado:
1. `ALL_TAB_COLUMNS` muestra solo lo que ve el usuario conectado. Conectarse con el mismo usuario
   del datasource o con DBA, o salen faltantes falsos.
2. Si aparece algo **además** de lo que estos cinco scripts arreglan: **parar y avisar antes del
   WAR.** Eso es un mapeo que nadie previó.

---

## 3. Recién entonces, el WAR

Y el WAR sale **junto con el build del frontend**, no antes: la Fase 0 dejó `idEmpresa` obligatorio
en 7 endpoints, y las pantallas que lo alimentan están en `saaFE` sin desplegar. Un WAR solo deja
los cobros manuales fallando con *"idEmpresa es obligatorio"*.

**Secuencia completa:** DDL (§2) → verificación entidad-vs-esquema → WAR + build del frontend,
juntos → prueba funcional.

---

## 4. Lo que este documento NO cubre

- **`CRD.PRCA`** — entidad con DAO, service y endpoint REST vivo contra una tabla que no existe en
  producción, resto de una implementación superseded que nadie llama. Está registrada desde antes;
  no la arregla ninguno de estos scripts y no bloquea (nadie la invoca). Anotada para que no
  sorprenda en la salida de la verificación.
- **El flag de contabilidad (rubro 237).** Sigue en 0 y **no se enciende** hasta cerrar las fases
  del `PLAN-CIERRE-CONTABLE-TOTAL.md`.

---

## 5. Lo que YA se ejecutó (actualizado 2026-08-31)

**No volver a correr estos.** Registrado acá para que ningún equipo los repita.

| Script | Qué hizo | Estado |
|---|---|---|
| `90_DEVOLUCION_APORTES_RECLASIFICACION.sql` | `CRD.DVAP.DVAPNMRC` | ✅ ejecutado |
| `94_CUENTAS_POR_TIPO_APORTE.sql` | tabla `CRD.CTAP` + 11 filas (empresa 1236) | ✅ ejecutado |
| `97_PAGO_PENSION_COMPLEMENTARIA.sql` | tabla `CRD.PGPC` + detalle PDTR 1200 | ✅ ejecutado |
| `98_CONTRATOS_FALTANTES_404.sql` | 373 contratos + 486 vigencias | ✅ ejecutado |
| `99_CONTRATOS_CERRADOS_JUNIO2025.sql` | 31 contratos con vigencia cerrada al 2025-06-30 | ✅ ejecutado |
| `100_CBCR_ASIENTO_REPARTO.sql` | `CRD.CBCR.CBCRASRP` + FK a `CNT.ASNT` + índice | ✅ ejecutado |

### Los dos controles del 100 que fallan en silencio — ✅ VERIFICADOS (2026-08-31)

Ninguno de los dos da error visible si falla. **Se miraron y los dos pasaron** — resultado abajo:

- **La FK es cross-schema** (`CRD.CBCR` → `CNT.ASNT`). El rol DBA no habilita `REFERENCES`; sin el
  grant, el `ALTER` falla con `ORA-01031`. Control: bloque 2.3 del script, debe devolver una fila
  `ENABLED` / `VALIDATED`. **Resultado: `FK_CBCR_ASRP · R · ENABLED · VALIDATED`. El grant estaba.**
- **El índice puede haber quedado en el schema equivocado.** Un `CREATE INDEX` sin prefijo se crea
  en el schema de la sesión y la tabla queda sin índice, **sin ningún error**. Control: bloque 2.4,
  debe devolver `OWNER = 'CRD'`. **Resultado: `CRD · IDX_CBCR_ASRP · CBCR · VALID`. Quedó en el schema correcto.**

### El flag de contabilidad ya NO está en 0

§4 decía que el rubro 237 seguía apagado. **El usuario lo encendió el 2026-08-31.** Consecuencia
operativa: todo cobro y todo proceso que corra desde ese momento genera asientos — incluidos los
que corren con el WAR anterior, o sea con la lógica previa a las correcciones de esa fecha.

---

## 6. ⚠️ INCIDENTE — `CBCRASRP` desaparecio y tumbo la pantalla de cobros (2026-08-31)

**Qué pasó.** El script 100 se corrió por la mañana y sus controles 2.3 y 2.4 devolvieron
`FK_CBCR_ASRP ENABLED VALIDATED` e `IDX_CBCR_ASRP VALID`. Horas después, en la MISMA base de
producción, `ALL_TAB_COLUMNS` devolvia solo `CBCRASN1` y `CBCRASN2`: **la columna no estaba.**

**El daño.** El WAR nuevo ya estaba desplegado y `CobroCredito` mapea `CBCRASRP`. Hibernate
incluye toda columna mapeada en el `SELECT` que genera, así que **toda lectura de `CRD.CBCR`
fallaba con `ORA-00904`** — la pantalla de cobros entera, no solo el asiento nuevo. No se
detectó de inmediato porque nadie abrió esa pantalla después del despliegue.

**Los síntomas que despistaron, y por qué.** Se perdieron horas diagnosticando dos "defectos de
código" que no existían:

- **"El abono a capital genera 2 asientos y no 4."** El asiento de reparto no se puede guardar
  si no existe la columna donde va. El código estaba bien.
- **"La transitoria está en −$2.973.328,49 sobre 521 líneas."** Es el acumulado histórico: el
  asiento 1 la viene acreditando desde siempre y el asiento 2 —el que la descarga— nunca llegó
  a escribirse. Es exactamente el problema que los tres asientos vinieron a resolver.

**Lo que hay que aprender de esto, y es lo importante:**

1. **La FK y el índice NO son evidencia de que la columna existe.** Los dos controles pasaron
   con la columna ausente. **El único control válido es preguntar por la columna**
   (`ALL_TAB_COLUMNS`) y, mejor todavía, **abrir la pantalla que la usa.**
2. **Verificar el esquema, no solo el código.** Se analizó la cadena de llamadas entera —
   correctamente, y descartando bien un fallo silencioso — mientras la premisa no verificada
   era que la base tuviera lo que el código necesita.
3. **Un `GET` de la API dice qué WAR está corriendo.** El campo `familia` de
   `/rest/nvpc/getId/{id}` existe solo desde el 2026-08-31: si aparece, el WAR es el nuevo.
   Resolvió en 30 segundos una duda que llevaba dos horas.
4. **Confirmar que el cliente SQL y el WAR miran la misma base ANTES de diagnosticar.** Se pidió
   por SQL la fila exacta que había devuelto el `GET`; si coincide, es la misma base. Dos horas
   de análisis se hicieron sobre datos de origen dudoso por no haber empezado por ahí.

**Pendiente cuando baje la urgencia:** averiguar QUÉ eliminó la columna. Varios equipos trabajan
sobre esta base. Si vuelve a desaparecer, la pantalla de cobros se cae de nuevo — y esta vez con
asientos de reparto ya escritos que perderían su referencia.

---

## 7. Ejecutado el 2026-09-01

| Script | Que hizo | Estado |
|---|---|---|
| `95_PRODUCTOS_PAGO_APORTES.sql` | 5 grupos + 5 productos de pago, y `CRD.TPAP.TPAPPRDP` para los 11 tipos de `CRD.CTAP` | ✅ ejecutado |
| `100` (bloque 1, **re-ejecutado**) | `CRD.CBCR.CBCRASRP` + FK + indice — la columna habia desaparecido, ver §6 | ✅ ejecutado y verificado |
| `106_REFERENCIA_UNICA_COBROS.sql` | `CRD.UX_CBCR_REFERENCIA` — indice unico basado en funcion | ✅ ejecutado, `UNIQUE` / `VALID` / owner `CRD` |

### El tipo de aporte 1 sigue SIN configurar, y no es inocuo

`APORTE PERSONALES` (tipo 1) no tiene cuenta en `CRD.CTAP` ni producto de pago en `CRD.TPAP`.
La regla de `DevolucionAporteServiceImpl` es **todo o nada** (`contabiliza = tiposSinProducto.isEmpty()`):

- Una devolucion **solo** de ese tipo → se registra, se paga y **NO genera contabilidad**, sin
  error y sin aviso en pantalla. Solo queda una linea en el log del servidor.
- Una devolucion que lo **mezcla** con otros tipos → se rechaza con `ERR_TIPO_APORTE_SIN_PRODUCTO`.

**Pasó de verdad el 2026-09-01**: una devolucion se proceso completa sin asientos por este motivo.
Hasta que se defina esa cuenta, **no devolver aportes personales**.

### Regla que dejo el incidente de `CBCRASRP`

La FK y el indice **no son evidencia de que una columna exista** — los dos controles pasaron con la
columna ausente. El unico control valido es `ALL_TAB_COLUMNS`, y mejor todavia, abrir la pantalla
que la usa. Lo mismo al crear un indice: verificar `OWNER = 'CRD'`, porque un `CREATE INDEX` sin
prefijo se crea en el schema de la sesion y la tabla queda sin indice, sin ningun error.
