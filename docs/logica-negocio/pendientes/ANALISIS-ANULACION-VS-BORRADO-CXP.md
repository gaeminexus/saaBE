# Análisis — anulación vs. borrado físico en cuatro pantallas de `cxp`

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-08 · **Encargo:** `omen-saa-2-arb`
**Estado:** análisis puro, nada implementado. Necesita decisión del usuario antes de escribir código
o DDL.

Alcance: las cuatro pantallas que el inventario general marcó con el mismo patrón —un botón
"anular" que en realidad es un `DELETE` físico con `confirm()` del navegador, sin motivo, sin
auditoría—: **Grupos de Productos**, **Datos SRI**, **Documentos de reembolso**, **Negociaciones**.

Verificado leyendo cada `*Rest.java` y cada entidad, no asumido por el nombre de la pantalla.

---

## 1. Tabla de respuestas

| Pregunta | Grupos de Productos (`GRPP`) | Datos SRI (`LSRI` + `TSRI`) | Documentos de reembolso (`RMBF`) | Negociaciones (`NGCP` + 4 hijas) |
|---|---|---|---|---|
| **Endpoint hoy y qué hace** | `DELETE /grpp/{id}` → `GrupoProductoPagoDaoService.remove(...)`. Único de los cuatro con **alguna** validación: rechaza (403) si el grupo es el `POR_CLASIFICAR` de uso del sistema | `DELETE /lsriCompra/{id}` y `DELETE /tsriCompra/{id}` → `remove(...)` puro, sin ninguna validación | `DELETE /rmbf/{id}` → `reembolsoFacturaCompraService.remove(List.of(id))`, sin validación | `DELETE /ngcp/{id}` (y un alias `/ngcp/delete/{id}`, **duplicado**, mismo código) → `remove(...)` puro |
| **¿Tiene columna de estado?** | **Sí** — `GRPPESTD` | **Sí, las dos** — `LSRI.ESTADO`, `TSRI.ESTADO` | **Sí** — `RMBFESTD` | **Sí** — `NGCP.ESTADO` |
| **¿Motivo / usuario / fecha de anulación?** | **Ninguno de los tres** | **Ninguno de los tres**, en ninguna de las dos | **Ninguno de los tres** (`RMBFOBSR` es una observación genérica, no auditada) | **Parcial**: tiene `USUARIO`/`USUARIOMODIF` (FK a `SCP.PJRQ`) y `FECHAREGISTRO`/`FECHAMODIF`, pero **no** un campo específico de motivo ni de fecha de anulación — `FECHAMODIF` es genérico, no distingue "se anuló" de "se editó" |
| **🔴 ¿Qué otras tablas la referencian?** | **`PGS.PRDP.GRUPOPRODUCTO`** (Producto de Pago) y **`PGS.IXGP.GRPPCDGO`** (Impuesto x Grupo) — dos tablas dependen de esta | **`PGS.TSRI.LSRI`** depende de `LSRI` — y por un camino **no convencional**: la FK no apunta a la PK (`LSRI.ID`), apunta a **`LSRI.TABLA`** (una columna de negocio, no el id). `TSRI` en sí no tiene ninguna tabla que dependa de ella | **Ninguna** — no encontré ninguna entidad del modelo que la referencie. Es una hoja | **Tres tablas de primer nivel**: `PGS.PTNG.NEGOCIACION`, `PGS.FPNG.NEGOCIACION`, `PGS.ADNG.NEGOCIACION`. Y **dos niveles**: `PGS.PGNG.FORMAPAGO` depende de `FPNG`, que a su vez depende de `NGCP` — borrar una negociación con formas de pago que ya tienen pagos registrados afecta a una tabla que ni siquiera la referencia directamente. Además `PGS.PTNG.ADENDUM` depende de `ADNG`, así que un `path` puede quedar huérfano por dos lados distintos |
| **¿Ya existe un `anular` en ese servicio o uno hermano?** | **No** | **No**, en ninguna de las dos | **No** | **No** |

---

## 2. Lo que esto significa para el `DELETE` de hoy

- **Grupos de Productos**: si un grupo tiene productos (`PRDP`) o impuestos (`IXGP`) asociados, el
  `DELETE` de hoy **depende de si esas FK tienen `ON DELETE` configurado** — o falla con un error de
  integridad referencial (el caso menos malo: al menos avisa) o, si no hay FK real a nivel de base y
  sólo existe a nivel de mapeo JPA, **no hay ninguna protección** y quedaría un producto o un
  impuesto apuntando a un grupo que ya no existe.
- **Datos SRI**: mismo riesgo para `LSRI` con `TSRI` hijos, agravado por el join no convencional
  (por `TABLA`, no por `ID`) — si `TABLA` no es única, el problema de integridad es peor que un
  huérfano simple, es una relación ambigua desde el origen.
- **Documentos de reembolso**: sin tablas dependientes conocidas, el riesgo de huérfanos **no
  aplica** — el único riesgo real acá es perder el registro en sí, sin poder saber después qué pasó
  con un reembolso que se borró por error.
- **Negociaciones**: el caso más grave de los cuatro. Una negociación con adendums, formas de pago
  o pagos ya registrados que se borra hoy puede dejar huérfanas hasta **cuatro tablas**, dos de
  ellas (`PTNG`, `PGNG`) por una dependencia indirecta que no salta a la vista mirando sólo `NGCP`.

**El script de la sección 3 mide si esto ya pasó**, no sólo si podría pasar.

---

## 3. Script de medición — solo lectura

Cuenta filas por tabla y busca huérfanos reales (una fila hija cuya FK apunta a un padre que ya no
existe). Sin `PROMPT`/`SET`/`COLUMN`; la etiqueta va como columna literal del propio `SELECT`.

```sql
-- ============================================================
-- Medicion de filas y de huerfanos reales en las cuatro tablas
-- del patron "DELETE sin validar" de cxp. Solo lectura.
-- ============================================================

-- 1) Conteo de filas por tabla
SELECT 'CONTEO DE FILAS' AS reporte, 'PGS.GRPP' AS tabla, COUNT(*) AS total FROM PGS.GRPP
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.PRDP', COUNT(*) FROM PGS.PRDP
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.IXGP', COUNT(*) FROM PGS.IXGP
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.LSRI', COUNT(*) FROM PGS.LSRI
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.TSRI', COUNT(*) FROM PGS.TSRI
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.RMBF', COUNT(*) FROM PGS.RMBF
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.NGCP', COUNT(*) FROM PGS.NGCP
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.PTNG', COUNT(*) FROM PGS.PTNG
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.FPNG', COUNT(*) FROM PGS.FPNG
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.ADNG', COUNT(*) FROM PGS.ADNG
UNION ALL
SELECT 'CONTEO DE FILAS', 'PGS.PGNG', COUNT(*) FROM PGS.PGNG;

-- 2) Huerfanos de Grupos de Productos
SELECT 'HUERFANOS - PRDP sin GRPP' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.PRDP p
WHERE  p.GRUPOPRODUCTO IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.GRPP g WHERE g.GRPPCDGO = p.GRUPOPRODUCTO);

SELECT 'HUERFANOS - IXGP sin GRPP' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.IXGP i
WHERE  i.GRPPCDGO IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.GRPP g WHERE g.GRPPCDGO = i.GRPPCDGO);

-- 3) Datos SRI: huerfanos de TSRI, y de paso si TABLA es realmente unica en LSRI
--    (si no lo es, el join TSRI->LSRI por TABLA es ambiguo desde el origen)
SELECT 'HUERFANOS - TSRI sin LSRI (join por TABLA)' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.TSRI t
WHERE  t.LSRI IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.LSRI l WHERE l.TABLA = t.LSRI);

SELECT 'VERIFICACION - LSRI.TABLA duplicado' AS reporte, l.TABLA, COUNT(*) AS repeticiones
FROM   PGS.LSRI l
GROUP BY l.TABLA
HAVING COUNT(*) > 1;

-- 4) Documentos de reembolso: sin tablas dependientes conocidas, solo el conteo de arriba aplica

-- 5) Negociaciones: huerfanos en los cuatro niveles
SELECT 'HUERFANOS - PTNG sin NGCP' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.PTNG p
WHERE  p.NEGOCIACION IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.NGCP n WHERE n.ID = p.NEGOCIACION);

SELECT 'HUERFANOS - PTNG sin ADNG (adendum)' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.PTNG p
WHERE  p.ADENDUM IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.ADNG a WHERE a.ID = p.ADENDUM);

SELECT 'HUERFANOS - FPNG sin NGCP' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.FPNG f
WHERE  f.NEGOCIACION IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.NGCP n WHERE n.ID = f.NEGOCIACION);

SELECT 'HUERFANOS - ADNG sin NGCP' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.ADNG a
WHERE  a.NEGOCIACION IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.NGCP n WHERE n.ID = a.NEGOCIACION);

SELECT 'HUERFANOS - PGNG sin FPNG (dependencia indirecta)' AS reporte, COUNT(*) AS filas_huerfanas
FROM   PGS.PGNG p
WHERE  p.FORMAPAGO IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM PGS.FPNG f WHERE f.ID = p.FORMAPAGO);
```

No lo corrí — no ejecuto SQL. El resultado de este script responde la pregunta que decide todo: si
hoy da cero huérfanos en todos los casos, esto es una mejora de diseño hacia adelante; si da algo
distinto de cero, hay datos rotos **ahora mismo** y hace falta una conversación aparte sobre qué
hacer con ellos (no forma parte de este análisis, que es sólo diagnóstico).

---

## 4. Mi recomendación por pantalla

**Ninguna de las cuatro es idéntica a las otras tres — no propongo la misma solución para todas.**

### Grupos de Productos — **marcar estado**
Tiene `ESTADO`, tiene dos tablas que dependen de ella, y **es un catálogo con valor de negocio** (se
usa para clasificar productos de pago, y el propio código ya protege el grupo "POR CLASIFICAR" del
sistema). Encaja exactamente en el patrón "barato: marcar la columna que ya existe". Necesita además
motivo/usuario/fecha — eso sí es DDL, columnas nuevas.

### Datos SRI — **marcar estado, y aparte revisar el join por `TABLA`**
Mismo caso que Grupos de Productos (tiene `ESTADO` en las dos tablas, tiene una dependencia real). Pero
antes de tocar la anulación, **el join `TSRI.LSRI → LSRI.TABLA` en vez de por `ID` merece su propia
revisión** — no es parte de este pedido (anular vs. borrar), pero lo encontré mirando las FK y no
quiero que se pierda: una relación por columna de negocio en vez de por PK es más frágil de lo que
parece, y el punto 3 de arriba mide si ya produjo un problema real (`TABLA` duplicado).

### Documentos de reembolso — **marcar estado, sin ceremonia extra**
Tiene `ESTADO` y **no tiene ninguna tabla que dependa de ella** — es la única de las cuatro sin
riesgo de huérfanos. Acá **sí es válido preguntarse si de verdad hace falta motivo/usuario/fecha de
anulación**, o si con marcar el estado alcanza: sin nada que pueda quedar huérfano ni ningún proceso
corriente que dependa de su ciclo de vida, exigirle el mismo nivel de auditoría que a una
negociación con plata comprometida podría ser ceremonia de más. Es una decisión del usuario, no
técnica — lo marco como la pregunta abierta más chica del lote, no como una recomendación cerrada.

### Negociaciones — **la más urgente, y la que necesita más que "marcar estado"**
Es la única con dependencias de **dos niveles** y **tres tablas de primer nivel**. Tiene `USUARIO`
pero no un motivo de anulación explícito ni una fecha de anulación distinguible de una edición
cualquiera. Acá "marcar estado" no alcanza por sí solo: si se anula una negociación que ya tiene
pagos registrados (`PGNG`) contra una de sus formas de pago, la anulación tiene que decidir **qué
pasa con esos pagos** — la misma familia de pregunta que ya resolvimos para anticipos y egresos
(revertir primero, o bloquear la anulación si hay algo "vigente" colgando). Ésta es, de las cuatro,
la única que probablemente necesite diseño de verdad y no sólo una columna nueva.

---

## 5. Lo que no se resolvió en esta pasada

- No corrí el script — los números reales de huérfanos los tiene que traer el usuario.
- No propuse `DDL` para ninguna de las cuatro — es la conversación que sigue, una vez que el usuario
  decida el alcance por pantalla (¿todas necesitan motivo/usuario/fecha, o sólo Negociaciones?).
- El hallazgo del join `TSRI.LSRI` por `TABLA` en vez de por `ID` queda anotado para revisión aparte,
  no lo desarrollé más porque no es parte de "anulación vs. borrado".
