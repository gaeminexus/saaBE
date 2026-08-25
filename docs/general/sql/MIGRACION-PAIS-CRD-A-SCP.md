# Migración del catálogo de países: `CRD.PSSS` → `SCP.PSSS`

> # 🛑 NO APLICADA — NO EJECUTAR
>
> **Se intentó en producción el 2026-08-24 y falló.**
>
> **Decisión del usuario: la tabla de países SE QUEDA en `CRD.PSSS`.** No se migra, y no se
> reintenta por ahora.
>
> ### Lo que SÍ quedó aplicado, y se conserva
>
> El **movimiento de paquetes Java**, que es lo que arreglaba el problema real:
>
> | Antes | Ahora (vigente) |
> |---|---|
> | `com.saa.model.crd.Pais` | **`com.saa.model.scp.Pais`** |
> | `com.saa.ejb.crd.dao/daoImpl.PaisDao*` | **`com.saa.basico.ejb/ejbImpl.PaisDao*`** |
> | `com.saa.ejb.crd.service/serviceImpl.Pais*` | **`com.saa.basico.ejb/ejbImpl.Pais*`** |
> | `com.saa.ws.rest.crd.PaisRest` | **`com.saa.ws.rest.basico.PaisRest`** |
> | `NombreEntidadesCredito.PAIS` | **`NombreEntidadesSistema.PAIS`** |
>
> Ese movimiento elimina la única dependencia `tsr → crd` del backend
> (`model/tsr/Titular.java` importaba `com.saa.model.crd.Pais`) y **no depende del esquema en
> el que viva la tabla**. Revertirlo reintroduciría la fuga de compilación, así que se
> conserva.
>
> ### Lo que quedó revertido
>
> **Solo el `@Table`**: `com.saa.model.scp.Pais` vuelve a declarar
> `@Table(name = "PSSS", schema = "CRD")`.
>
> ⚠️ **Consecuencia deliberada: el paquete Java y el esquema de base NO coinciden.** La clase
> vive en `scp` y la tabla en `CRD`. No es un error y no hay que "arreglarlo": está explicado
> en el JavaDoc de la entidad y en `docs/MODELO-DATOS.md`.
>
> ### Qué queda como deuda
>
> La FK **`TSR.TTLR.PSSSCDGO → CRD.PSSS`** sigue vigente. Es lo único que falta para poder
> extraer el módulo `crd`: mientras exista, arrancar `crd` deja una FK huérfana en `tsr`.
> **La fuga de compilación ya no existe**; la de integridad referencial sí.
>
> ### Por qué no se borra este documento
>
> Sirve como punto de partida el día que se retome. Los controles previos (bloque 2), la vista
> previa (bloque 3) y el respaldo (bloque 4) siguen siendo válidos, y el bloque 7 (rollback)
> es lo que hay que consultar si algo quedó a medias en producción.
>
> ⚠️ **Antes de reintentar**: el estado real de producción tras el intento fallido lo está
> diagnosticando el árbitro. **No correr nada de este documento hasta que ese diagnóstico
> esté cerrado** — puede haber objetos a medio crear (`SCP.PSSS` existente y vacía, FK
> dropeadas y no recreadas, respaldos ya tomados). El control **2.9** y el bloque **7** son
> los primeros que hay que mirar.

---

> Preparado el **2026-08-24**. Documento para **revisar antes de ejecutar**.
> Primero se corren los controles del bloque 2 y se lee la vista previa del bloque 3; se
> decide; y solo después se ejecutan el respaldo (bloque 4) y la migración (bloque 5).
> **Nada de los bloques 2 y 3 modifica datos.**

---

## 0. Por qué

El sistema se comercializa como sistema contable **sin el módulo `crd`**. Hoy hay una única
fuga que lo impide, y es preexistente:

```
model/tsr/Titular.java:13   ->  import com.saa.model.crd.Pais;
TSR.TTLR.PSSSCDGO           ->  FK a CRD.PSSS
```

Mientras esa FK exista, arrancar `crd` deja a `tsr` sin compilar y con una FK huérfana. El
catálogo de países no es un concepto de créditos: es un catálogo de núcleo, como `Empresa`,
`Usuario`, `Rubro` y `DetalleRubro`, que ya viven en `SCP`.

Verificado por el árbitro: **`Provincia`, `Canton`, `Ciudad` y `Parroquia` NO se fugan**. Solo
`Pais`.

### Cambio de código que acompaña a esta migración

Ya está aplicado en el WAR (no compilado todavía; el usuario compila en Eclipse):

| Antes | Ahora |
|---|---|
| `com.saa.model.crd.Pais` | `com.saa.model.scp.Pais` |
| `com.saa.ejb.crd.dao.PaisDaoService` | `com.saa.basico.ejb.PaisDaoService` |
| `com.saa.ejb.crd.daoImpl.PaisDaoServiceImpl` | `com.saa.basico.ejbImpl.PaisDaoServiceImpl` |
| `com.saa.ejb.crd.service.PaisService` | `com.saa.basico.ejb.PaisService` |
| `com.saa.ejb.crd.serviceImpl.PaisServiceImpl` | `com.saa.basico.ejbImpl.PaisServiceImpl` |
| `com.saa.ws.rest.crd.PaisRest` | `com.saa.ws.rest.basico.PaisRest` |
| `NombreEntidadesCredito.PAIS` | `NombreEntidadesSistema.PAIS` |
| ~~`@Table(name = "PSSS", schema = "CRD")`~~ | ~~`@Table(name = "PSSS", schema = "SCP")`~~ |

> 🛑 **La última fila quedó REVERTIDA.** Tras el intento fallido del 2026-08-24, la entidad
> `com.saa.model.scp.Pais` volvió a declarar `@Table(name = "PSSS", schema = "CRD")`. **Todas
> las demás filas de esta tabla siguen vigentes**: el movimiento de paquetes se conservó.

**`@Path("psss")` NO cambió**, así que la URL sigue siendo `/SaaBE/rest/psss/...` y las dos
pantallas de titulares del frontend no se tocan.

**El valor de la constante tampoco cambió** (`"Pais"`), así que los `@NamedQuery` `PaisAll` /
`PaisId` y las llamadas `selectAll(...)` / `selectById(...)` siguen resolviendo igual. Esa
terna —`@NamedQuery`, constante y valor que pasa el service— sigue coincidiendo exactamente.

### ⚠️ Orden de despliegue — aplica solo si se RETOMA la migración

> Hoy **no aplica**: el WAR vigente declara `schema = "CRD"` y no hay migración que coordinar.
> Lo que sigue es la nota original, válida para el día que se retome.

**La migración iría ANTES del WAR nuevo**, y el WAR nuevo tendría que volver a declarar
`schema = "SCP"`: si sube antes de que exista `SCP.PSSS`, toda consulta de países falla en
runtime.

El orden inverso también rompe: un WAR que apunte a `CRD.PSSS` deja de funcionar cuando este
script borra esa tabla al final. **No hay ventana sin corte**: programar migración y despliegue
en la misma parada.

**Este desacople fue justamente lo que salvó el intento fallido**: como el movimiento de
paquetes no toca el esquema, revertir el `@Table` alcanzó para dejar el sistema consistente sin
deshacer nada del refactor de Java.

---

## 1. Alcance exacto

### Tabla que se mueve

| Origen | Destino |
|---|---|
| `CRD.PSSS` | `SCP.PSSS` |

Estructura, según la entidad `com.saa.model.scp.Pais` (la entidad es la autoridad):

| Columna | Tipo | Campo Java | Notas |
|---|---|---|---|
| `PSSSCDGO` | `NUMBER` | `codigo` | **PK**, IDENTITY |
| `PSSSCDAL` | `VARCHAR2(10)` | `codigoAlterno` | código INEC |
| `PSSSNMBR` | `VARCHAR2(2000)` | `nombre` | |
| `PSSSNCNL` | `VARCHAR2(2000)` | `nacionalidad` | |
| `PSSSCDNC` | `VARCHAR2(10)` | `codigoNacionalidad` | |
| `PSSSCDEX` | `VARCHAR2(50)` | `codigoExterno` | |
| `PSSSIDST` | `NUMBER` | `estado` | |

> El bloque 5.1 **no** copia esta tabla de tipos a mano: la crea con
> `CREATE TABLE ... AS SELECT * ... WHERE 1 = 0`, que reproduce los tipos reales de la
> instancia. Esta tabla es para revisar, no para tipear.

### FK que se repuntan

Son **dos**, y las dos conservan el mismo nombre de columna (`PSSSCDGO`):

| Tabla | Columna | FK actual | FK nueva |
|---|---|---|---|
| `TSR.TTLR` (Titular) | `PSSSCDGO` | → `CRD.PSSS` | → `SCP.PSSS` |
| `CRD.PRVN` (Provincia) | `PSSSCDGO` | → `CRD.PSSS` | → `SCP.PSSS` |

`CRD.PRVN` → `SCP.PSSS` es dirección **permitida** (`crd → scp`): al retirar `crd` se va el
que depende, no el dependido.

**Ninguna anotación JPA cambia por esto**: `@JoinColumn(name = "PSSSCDGO", referencedColumnName
= "PSSSCDGO")` no nombra el esquema, lo toma de la entidad referenciada.

### Fuera de alcance

- Los datos de `PSSSCDGO` **no se renumeran**. Se conservan idénticos, que es lo que permite
  repuntar las FK sin tocar una sola fila de `TSR.TTLR` ni de `CRD.PRVN`.
- `CRD.PRVN`, `CRD.CNTN`, `CRD.CDDD` y `CRD.PRRQ` se quedan donde están.
- El frontend no se toca (la URL no cambió).

---

## 2. Bloque 1 — Controles previos (SOLO LECTURA)

Correr todo este bloque y **guardar los resultados**: son la línea base contra la que se
verifica el bloque 6.

### 2.1 — Cuántos países hay

```sql
SELECT COUNT(*) AS TOTAL_PAISES FROM CRD.PSSS;
```

### 2.2 — Distribución por estado

```sql
SELECT PSSSIDST AS ESTADO, COUNT(*) AS PAISES
FROM   CRD.PSSS
GROUP  BY PSSSIDST
ORDER  BY 1;
```

### 2.3 — Cuántos TTLR y PRVN referencian cada país

Las filas con 0 referencias también salen: es el catálogo completo con su uso real.

```sql
SELECT p.PSSSCDGO,
       p.PSSSNMBR,
       (SELECT COUNT(*) FROM TSR.TTLR t WHERE t.PSSSCDGO = p.PSSSCDGO) AS TITULARES,
       (SELECT COUNT(*) FROM CRD.PRVN v WHERE v.PSSSCDGO = p.PSSSCDGO) AS PROVINCIAS
FROM   CRD.PSSS p
ORDER  BY 3 DESC, 4 DESC, 1;
```

### 2.4 — Totales de referencias

```sql
SELECT 'TSR.TTLR con pais'    AS ORIGEN, COUNT(*) AS FILAS
FROM   TSR.TTLR WHERE PSSSCDGO IS NOT NULL
UNION ALL
SELECT 'CRD.PRVN con pais',          COUNT(*)
FROM   CRD.PRVN WHERE PSSSCDGO IS NOT NULL;
```

### 2.5 — Huérfanos: referencias a códigos que NO existen

**Debe devolver 0 filas en las dos consultas.** Si devuelve algo, las FK actuales están
deshabilitadas o fueron creadas con `NOVALIDATE`, y el bloque 5.4 va a fallar al crear las FK
nuevas. Hay que limpiar esas filas ANTES de seguir.

```sql
SELECT t.TTLRCDGO, t.PSSSCDGO AS PAIS_INEXISTENTE
FROM   TSR.TTLR t
WHERE  t.PSSSCDGO IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.PSSS p WHERE p.PSSSCDGO = t.PSSSCDGO);
```

```sql
SELECT v.PRVNCDGO, v.PSSSCDGO AS PAIS_INEXISTENTE
FROM   CRD.PRVN v
WHERE  v.PSSSCDGO IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.PSSS p WHERE p.PSSSCDGO = v.PSSSCDGO);
```

### 2.6 — Nulos

Informativo, no bloquea: una FK admite `NULL`. Solo hay que saber cuántas filas quedan sin
país para no confundirlas con datos perdidos en el bloque 6.

```sql
SELECT 'TSR.TTLR sin pais'  AS ORIGEN, COUNT(*) AS FILAS
FROM   TSR.TTLR WHERE PSSSCDGO IS NULL
UNION ALL
SELECT 'CRD.PRVN sin pais',        COUNT(*)
FROM   CRD.PRVN WHERE PSSSCDGO IS NULL;
```

### 2.7 — Nombre real de las FK actuales

**Necesario antes del bloque 5.3**: los nombres que este documento usa
(`FK_TTLR_PSSS`, `FK_PRVN_PSSS`) son los esperables por convención, pero **la instancia manda**.
Anotar lo que devuelva esta consulta y usar esos nombres en los `DROP`.

```sql
SELECT c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME, c.STATUS, c.VALIDATED,
       cc.COLUMN_NAME, r.OWNER AS REF_OWNER, r.TABLE_NAME AS REF_TABLE
FROM   ALL_CONSTRAINTS c
       JOIN ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER
                               AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
       JOIN ALL_CONSTRAINTS r   ON r.OWNER = c.R_OWNER
                               AND r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'R'
AND    r.OWNER = 'CRD'
AND    r.TABLE_NAME = 'PSSS'
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_NAME;
```

### 2.8 — Cualquier OTRA tabla que apunte a CRD.PSSS

El alcance de este documento dice **dos** FK. Si esta consulta devuelve una tercera, **parar**:
el alcance está mal y hay que rehacerlo antes de tocar nada.

```sql
SELECT c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME
FROM   ALL_CONSTRAINTS c
       JOIN ALL_CONSTRAINTS r ON r.OWNER = c.R_OWNER
                             AND r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'R'
AND    r.OWNER = 'CRD'
AND    r.TABLE_NAME = 'PSSS'
AND    NOT (c.OWNER = 'TSR' AND c.TABLE_NAME = 'TTLR')
AND    NOT (c.OWNER = 'CRD' AND c.TABLE_NAME = 'PRVN');
```

### 2.9 — SCP.PSSS no debe existir todavía

**Debe devolver 0.** Si devuelve 1, alguien ya corrió parte de este script: revisar antes de
seguir, no reejecutar a ciegas.

```sql
SELECT COUNT(*) AS YA_EXISTE_SCP_PSSS
FROM   ALL_TABLES
WHERE  OWNER = 'SCP' AND TABLE_NAME = 'PSSS';
```

---

## 3. Bloque 2 — Vista previa fila a fila de lo que se va a mover (SOLO LECTURA)

Es el catálogo completo, tal como quedará en `SCP.PSSS`. Guardar la salida: el bloque 6.2 la
compara código por código.

```sql
SELECT PSSSCDGO,
       PSSSCDAL,
       PSSSNMBR,
       PSSSNCNL,
       PSSSCDNC,
       PSSSCDEX,
       PSSSIDST
FROM   CRD.PSSS
ORDER  BY PSSSCDGO;
```

Huella de control, para comparar de un vistazo en el bloque 6:

```sql
SELECT COUNT(*)          AS FILAS,
       MIN(PSSSCDGO)     AS COD_MIN,
       MAX(PSSSCDGO)     AS COD_MAX,
       SUM(PSSSCDGO)     AS SUMA_CODIGOS,
       COUNT(PSSSNMBR)   AS NOMBRES_NO_NULOS
FROM   CRD.PSSS;
```

---

## 4. Bloque 3 — Respaldo (ejecutar ANTES de tocar nada)

Tres respaldos: el catálogo y las dos columnas de FK que se repuntan. Los dos últimos son
baratos y permiten probar que **ninguna fila referenciante cambió**.

```sql
CREATE TABLE CRD.PSSS_BKP_20260824 AS SELECT * FROM CRD.PSSS;

CREATE TABLE TSR.TTLR_PAIS_BKP_20260824 AS
SELECT TTLRCDGO, PSSSCDGO FROM TSR.TTLR;

CREATE TABLE CRD.PRVN_PAIS_BKP_20260824 AS
SELECT PRVNCDGO, PSSSCDGO FROM CRD.PRVN;
```

Verificar que los tres respaldos quedaron con el mismo conteo que el original:

```sql
SELECT 'PSSS' AS TABLA, (SELECT COUNT(*) FROM CRD.PSSS) AS ORIGEN,
                        (SELECT COUNT(*) FROM CRD.PSSS_BKP_20260824) AS RESPALDO
FROM DUAL
UNION ALL
SELECT 'TTLR', (SELECT COUNT(*) FROM TSR.TTLR),
               (SELECT COUNT(*) FROM TSR.TTLR_PAIS_BKP_20260824) FROM DUAL
UNION ALL
SELECT 'PRVN', (SELECT COUNT(*) FROM CRD.PRVN),
               (SELECT COUNT(*) FROM CRD.PRVN_PAIS_BKP_20260824) FROM DUAL;
```

> Los respaldos se borran recién cuando la migración esté verificada en producción y el WAR
> nuevo lleve unos días arriba. Ver bloque 8.

---

## 5. Bloque 4 — La migración

> ⚠️ **`CREATE TABLE`, `ALTER TABLE` y `DROP` son DDL: Oracle hace COMMIT implícito.**
> No se puede envolver todo esto en una transacción ni deshacerlo con `ROLLBACK`. La red de
> seguridad son los respaldos del bloque 4 y el bloque 7. Correr paso por paso, verificando.

### 5.1 — Crear `SCP.PSSS` con la misma estructura

`AS SELECT ... WHERE 1 = 0` copia los tipos y las longitudes reales de la instancia; no copia
PK, constraints, índices ni el `IDENTITY`, que se agregan después.

```sql
CREATE TABLE SCP.PSSS AS SELECT * FROM CRD.PSSS WHERE 1 = 0;
```

Comparar las dos estructuras antes de copiar datos. **Debe devolver 0 filas**:

```sql
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, 'SOLO EN CRD' AS LADO
FROM   ALL_TAB_COLUMNS WHERE OWNER = 'CRD' AND TABLE_NAME = 'PSSS'
MINUS
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, 'SOLO EN CRD'
FROM   ALL_TAB_COLUMNS WHERE OWNER = 'SCP' AND TABLE_NAME = 'PSSS';
```

### 5.2 — Copiar los datos CONSERVANDO `PSSSCDGO`

El `INSERT` nombra las columnas explícitamente: así, si el orden físico difiere, falla en vez
de mezclar valores en silencio.

```sql
INSERT INTO SCP.PSSS (PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST)
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST
FROM   CRD.PSSS;

COMMIT;
```

Control inmediato, antes de seguir. **Las dos consultas deben devolver 0 filas**:

```sql
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST FROM CRD.PSSS
MINUS
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST FROM SCP.PSSS;
```

```sql
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST FROM SCP.PSSS
MINUS
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST FROM CRD.PSSS;
```

### 5.3 — PK, `IDENTITY` y `NOT NULL` en la tabla nueva

La entidad declara `@GeneratedValue(strategy = GenerationType.IDENTITY)`, así que la columna
tiene que ser `IDENTITY` también en `SCP.PSSS`, o los `POST /rest/psss` fallan.

`START WITH` arranca después del máximo existente para no chocar con los códigos copiados.

```sql
ALTER TABLE SCP.PSSS MODIFY (PSSSCDGO NUMBER NOT NULL);

ALTER TABLE SCP.PSSS ADD CONSTRAINT PK_PSSS PRIMARY KEY (PSSSCDGO);
```

Calcular el arranque de la secuencia:

```sql
SELECT NVL(MAX(PSSSCDGO), 0) + 1 AS ARRANQUE FROM SCP.PSSS;
```

Y usar ese número en el `MODIFY` siguiente, reemplazando `&ARRANQUE`:

```sql
ALTER TABLE SCP.PSSS MODIFY
    (PSSSCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH &ARRANQUE INCREMENT BY 1));
```

> `GENERATED BY DEFAULT`, no `GENERATED ALWAYS`: es el estándar del proyecto
> (`docs/estandar/ESTANDARES-CREACION-TABLAS-ORACLE.md`) y además permite seguir insertando
> códigos explícitos si hiciera falta reponer una fila.

### 5.4 — Dropear las FK viejas y crear las nuevas contra `SCP.PSSS`

> Usar los nombres REALES que devolvió el control 2.7. Los de abajo son los esperables por
> convención.

```sql
ALTER TABLE TSR.TTLR DROP CONSTRAINT FK_TTLR_PSSS;

ALTER TABLE CRD.PRVN DROP CONSTRAINT FK_PRVN_PSSS;
```

```sql
ALTER TABLE TSR.TTLR ADD CONSTRAINT FK_TTLR_PSSS
    FOREIGN KEY (PSSSCDGO) REFERENCES SCP.PSSS(PSSSCDGO);

ALTER TABLE CRD.PRVN ADD CONSTRAINT FK_PRVN_PSSS
    FOREIGN KEY (PSSSCDGO) REFERENCES SCP.PSSS(PSSSCDGO);
```

> Sin `NOVALIDATE`: se quiere que Oracle valide las filas existentes acá y ahora. Si alguna FK
> falla al crearse, hay huérfanos que el control 2.5 no detectó — **parar y volver al bloque 7**.

Índices sobre las columnas de FK, si no existen ya (una FK sin índice hace lock de tabla al
borrar el padre):

```sql
SELECT INDEX_NAME, TABLE_NAME, COLUMN_NAME
FROM   ALL_IND_COLUMNS
WHERE  (TABLE_OWNER = 'TSR' AND TABLE_NAME = 'TTLR' AND COLUMN_NAME = 'PSSSCDGO')
   OR  (TABLE_OWNER = 'CRD' AND TABLE_NAME = 'PRVN' AND COLUMN_NAME = 'PSSSCDGO');
```

```sql
CREATE INDEX TSR.IDX_TTLR_PSSS ON TSR.TTLR(PSSSCDGO);
CREATE INDEX CRD.IDX_PRVN_PSSS ON CRD.PRVN(PSSSCDGO);
```

### 5.5 — Comentarios

```sql
COMMENT ON TABLE  SCP.PSSS IS
  'Catalogo de paises. Vivio en CRD.PSSS hasta el 2026-08-24; se movio a SCP porque TSR.TTLR lo referencia y el sistema se comercializa sin el modulo crd.';
COMMENT ON COLUMN SCP.PSSS.PSSSCDGO IS 'Codigo del pais (PK, IDENTITY). Se conservo identico al de CRD.PSSS.';
COMMENT ON COLUMN SCP.PSSS.PSSSCDAL IS 'Codigo alterno INEC.';
COMMENT ON COLUMN SCP.PSSS.PSSSNMBR IS 'Nombre del pais.';
COMMENT ON COLUMN SCP.PSSS.PSSSNCNL IS 'Nacionalidad.';
COMMENT ON COLUMN SCP.PSSS.PSSSCDNC IS 'Codigo de nacionalidad.';
COMMENT ON COLUMN SCP.PSSS.PSSSCDEX IS 'Codigo externo.';
COMMENT ON COLUMN SCP.PSSS.PSSSIDST IS 'Estado del registro.';
```

### 5.6 — Grants

> Verificar los nombres reales de los roles antes de ejecutar:
> `SELECT * FROM DBA_ROLES WHERE ROLE LIKE '%SCP%' OR ROLE LIKE '%CRD%' OR ROLE LIKE '%TSR%';`
> Si el esquema no usa roles, omitir este bloque y otorgar al usuario de la aplicación.

`SCP.PSSS` es un catálogo de núcleo: lo lee cualquier módulo, lo escribe el mantenimiento del
catálogo.

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON SCP.PSSS TO ROLE_SCP;
GRANT SELECT ON SCP.PSSS TO ROLE_CRD;
GRANT SELECT ON SCP.PSSS TO ROLE_TSR;
```

`CRD.PRVN` necesita el `REFERENCES` para sostener su FK nueva contra otro esquema:

```sql
GRANT REFERENCES ON SCP.PSSS TO CRD;
GRANT REFERENCES ON SCP.PSSS TO TSR;
```

### 5.7 — Dropear `CRD.PSSS`

**Último paso, y solo después de que todo el bloque 6 haya pasado.** Con el respaldo del
bloque 4 en su lugar.

```sql
DROP TABLE CRD.PSSS;
```

> Si Oracle se queja de dependencias, es que quedó una FK que el control 2.8 no vio: **no usar
> `CASCADE CONSTRAINTS`**. Averiguar cuál es y decidir a conciencia.

---

## 6. Bloque 5 — Verificaciones posteriores, contra el respaldo

Correr **todas** antes del `DROP TABLE` del paso 5.7 y antes de dar la migración por buena.

### 6.1 — Mismo conteo que el respaldo (debe devolver `IGUAL`)

```sql
SELECT CASE WHEN (SELECT COUNT(*) FROM SCP.PSSS)
               = (SELECT COUNT(*) FROM CRD.PSSS_BKP_20260824)
            THEN 'IGUAL' ELSE 'DIFERENTE - PARAR' END AS CONTEO
FROM   DUAL;
```

### 6.2 — Mismos códigos y mismos datos que el respaldo (0 filas en las dos)

```sql
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST
FROM   CRD.PSSS_BKP_20260824
MINUS
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST
FROM   SCP.PSSS;
```

```sql
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST
FROM   SCP.PSSS
MINUS
SELECT PSSSCDGO, PSSSCDAL, PSSSNMBR, PSSSNCNL, PSSSCDNC, PSSSCDEX, PSSSIDST
FROM   CRD.PSSS_BKP_20260824;
```

### 6.3 — La huella de control coincide con la del bloque 3

```sql
SELECT COUNT(*)        AS FILAS,
       MIN(PSSSCDGO)   AS COD_MIN,
       MAX(PSSSCDGO)   AS COD_MAX,
       SUM(PSSSCDGO)   AS SUMA_CODIGOS,
       COUNT(PSSSNMBR) AS NOMBRES_NO_NULOS
FROM   SCP.PSSS;
```

### 6.4 — Ninguna fila referenciante cambió (0 filas en las dos)

Es la prueba de que la migración movió el catálogo sin tocar a quien lo usa.

```sql
SELECT t.TTLRCDGO, t.PSSSCDGO AS AHORA, b.PSSSCDGO AS ANTES
FROM   TSR.TTLR t
       JOIN TSR.TTLR_PAIS_BKP_20260824 b ON b.TTLRCDGO = t.TTLRCDGO
WHERE  NVL(t.PSSSCDGO, -1) <> NVL(b.PSSSCDGO, -1);
```

```sql
SELECT v.PRVNCDGO, v.PSSSCDGO AS AHORA, b.PSSSCDGO AS ANTES
FROM   CRD.PRVN v
       JOIN CRD.PRVN_PAIS_BKP_20260824 b ON b.PRVNCDGO = v.PRVNCDGO
WHERE  NVL(v.PSSSCDGO, -1) <> NVL(b.PSSSCDGO, -1);
```

### 6.5 — FK habilitadas y VALIDADAS

**Las dos filas deben salir con `STATUS = ENABLED` y `VALIDATED = VALIDATED`**, apuntando a
`SCP.PSSS`. Una FK `ENABLED NOVALIDATE` deja pasar huérfanos históricos.

```sql
SELECT c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME, c.STATUS, c.VALIDATED,
       r.OWNER AS REF_OWNER, r.TABLE_NAME AS REF_TABLE
FROM   USER_CONSTRAINTS c
       JOIN ALL_CONSTRAINTS r ON r.OWNER = c.R_OWNER
                             AND r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'R'
AND    c.CONSTRAINT_NAME IN ('FK_TTLR_PSSS', 'FK_PRVN_PSSS')
ORDER  BY c.TABLE_NAME;
```

> `USER_CONSTRAINTS` solo muestra las del usuario conectado. Si se corre con un usuario
> administrador, o si `TSR` y `CRD` son esquemas distintos del que ejecuta, usar la misma
> consulta sobre `ALL_CONSTRAINTS` filtrando `c.OWNER IN ('TSR','CRD')`.

### 6.6 — Ya no queda ninguna FK apuntando a `CRD.PSSS` (debe devolver 0 filas)

```sql
SELECT c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME
FROM   ALL_CONSTRAINTS c
       JOIN ALL_CONSTRAINTS r ON r.OWNER = c.R_OWNER
                             AND r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
WHERE  c.CONSTRAINT_TYPE = 'R'
AND    r.OWNER = 'CRD'
AND    r.TABLE_NAME = 'PSSS';
```

### 6.7 — El `IDENTITY` quedó armado

```sql
SELECT COLUMN_NAME, IDENTITY_COLUMN, DEFAULT_ON_NULL
FROM   ALL_TAB_COLUMNS
WHERE  OWNER = 'SCP' AND TABLE_NAME = 'PSSS' AND COLUMN_NAME = 'PSSSCDGO';
```

```sql
SELECT TABLE_NAME, COLUMN_NAME, GENERATION_TYPE, SEQUENCE_NAME
FROM   ALL_TAB_IDENTITY_COLS
WHERE  OWNER = 'SCP' AND TABLE_NAME = 'PSSS';
```

### 6.8 — Prueba funcional, después de desplegar el WAR

```
GET /SaaBE/rest/psss/getAll        -> 200 con el catálogo completo
GET /SaaBE/rest/psss/getId/{id}    -> 200 con un país conocido
```

Y en el frontend, las dos pantallas de titulares (`titulares` y `titulares-v2`): el combo de
país tiene que cargar y guardar igual que antes. **La URL no cambió**, así que no debería
hacer falta tocar nada; si algo falla ahí, es señal de que el `@Path` se movió sin querer.

---

## 7. Bloque 6 — Rollback

Aplica **solo mientras `CRD.PSSS` siga existiendo**, es decir antes del paso 5.7. Después del
`DROP`, la reversa es el paso 7.2, que restaura desde el respaldo.

### 7.1 — Reversa antes del `DROP TABLE` (paso 5.7 no ejecutado)

```sql
ALTER TABLE TSR.TTLR DROP CONSTRAINT FK_TTLR_PSSS;
ALTER TABLE CRD.PRVN DROP CONSTRAINT FK_PRVN_PSSS;

ALTER TABLE TSR.TTLR ADD CONSTRAINT FK_TTLR_PSSS
    FOREIGN KEY (PSSSCDGO) REFERENCES CRD.PSSS(PSSSCDGO);
ALTER TABLE CRD.PRVN ADD CONSTRAINT FK_PRVN_PSSS
    FOREIGN KEY (PSSSCDGO) REFERENCES CRD.PSSS(PSSSCDGO);

DROP TABLE SCP.PSSS;
```

Y **volver al WAR anterior**, el que declara `schema = "CRD"`.

### 7.2 — Reversa después del `DROP TABLE` (paso 5.7 ya ejecutado)

```sql
CREATE TABLE CRD.PSSS AS SELECT * FROM CRD.PSSS_BKP_20260824;

ALTER TABLE CRD.PSSS MODIFY (PSSSCDGO NUMBER NOT NULL);
ALTER TABLE CRD.PSSS ADD CONSTRAINT PK_PSSS_CRD PRIMARY KEY (PSSSCDGO);
```

Arranque del IDENTITY, igual que en 5.3:

```sql
SELECT NVL(MAX(PSSSCDGO), 0) + 1 AS ARRANQUE FROM CRD.PSSS;
```

```sql
ALTER TABLE CRD.PSSS MODIFY
    (PSSSCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH &ARRANQUE INCREMENT BY 1));
```

Y después, el bloque 7.1 completo.

> ⚠️ Si entre la migración y el rollback se dieron de alta países nuevos en `SCP.PSSS`, el
> respaldo NO los tiene. Contrastar antes de restaurar:
>
> ```sql
> SELECT PSSSCDGO, PSSSNMBR FROM SCP.PSSS
> MINUS
> SELECT PSSSCDGO, PSSSNMBR FROM CRD.PSSS_BKP_20260824;
> ```

---

## 8. Después de esta migración

> 🛑 **La migración NO se aplicó.** Nada de este bloque se ejecutó, y en particular
> **los respaldos NO se deben borrar**: si el intento del 2026-08-24 alcanzó a crearlos, son
> justamente lo que necesita el árbitro para diagnosticar qué quedó a medias en producción.

- **Limpieza de respaldos**, cuando el WAR nuevo lleve varios días estable en producción.
  **NO ejecutar hoy**: solo aplica si algún día la migración se completa y se verifica.

```sql
DROP TABLE CRD.PSSS_BKP_20260824;
DROP TABLE TSR.TTLR_PAIS_BKP_20260824;
DROP TABLE CRD.PRVN_PAIS_BKP_20260824;
```

- **Frontend**: `tsr/model/titular.ts` sigue importando `Pais` desde `crd/model/pais`. La URL
  no cambió, así que **nada se rompe hoy**, pero la fuga equivalente sigue viva del lado del
  cliente. Es trabajo aparte, y no se toca desde este repositorio.

- **Verificación de la restricción de la §1** del plan de comercialización, ya corrida sobre
  el backend con resultado **VACÍO**:

```
grep -rn "com\.saa\.\(ejb\|model\|ws\)\.crd" \
  src/main/java/com/saa/model/tsr src/main/java/com/saa/model/cxp \
  src/main/java/com/saa/model/cnt src/main/java/com/saa/model/cxc \
  src/main/java/com/saa/ejb/tsr src/main/java/com/saa/ejb/cxp \
  src/main/java/com/saa/ejb/cnt src/main/java/com/saa/ejb/cxc \
  src/main/java/com/saa/basico
```

---

## 9. Nota sobre SQL*Plus

Ningún separador de comentario de este documento termina en guion. En SQL*Plus, una línea de
comentario acabada en `-` se interpreta como **continuación de línea** y **se traga la consulta
siguiente en silencio**: no da error, simplemente no ejecuta lo que sigue. Si se agregan
separadores al copiar los bloques, cerrarlos con `=` o con cualquier carácter que no sea `-`.
