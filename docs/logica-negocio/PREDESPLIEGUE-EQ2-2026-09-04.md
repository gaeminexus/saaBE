# Pre-despliegue del equipo `omen-saa-2` — 2026-09-04

**Escrito por el árbitro `omen-saa-2-arb` justo antes de que el usuario suba a producción.**
Alcance de esta revisión: los mapeos JPA de `saaBE` contra los scripts `e2-*` de este equipo.
**No se consultó la base** — cada control de abajo hay que correrlo en producción.

---

## 0. Estado de los repositorios

| Repo | Estado |
|---|---|
| `saaBE` | ✅ todo commiteado y pusheado. `HEAD` = `origin/main` = `48abb9b` |
| `saaFE` | ✅ limpio, sin nada sin pushear. `HEAD` = `aa4ff93` |

⚠️ **Lo único sin commitear en `saaBE` NO es de este equipo:** son ~15 archivos del equipo de la app
móvil (`crd/UsuarioApp*`, `EstadoUsuarioApp`, y `NombreEntidadesCredito.java` modificado). Ver §3.

---

## 1. 🔴 El DDL que el WAR necesita, y no es sólo el `e2-07`

`persistence.xml` **no declara `hbm2ddl.auto`**, así que Hibernate **no valida el esquema al
arrancar**: el despliegue no va a fallar. **Falla la primera pantalla que lea la entidad**, con
`ORA-00904` (columna) o `ORA-00942` (tabla). Es lo que hace que esto no se vea hasta que un usuario
lo abre.

| Script | Qué exige el WAR | Si no corrió en producción |
|---|---|---|
| `tsr/sql/e2-07` | `PGS.APLP.APLPMVCH` ← `AplicacionPagoCxp.idMovimientoCajaChica` | ✅ **el usuario confirmó que lo corrió** |
| `cxp/sql/e2-05` | `PGS.APLP.APLPFCTC` pasa a aceptar `NULL` | No es un mapeo: **`ORA-01400` al cruzar un anticipo contra una liquidación** — el urgente que reportó el usuario. Si no corrió, ese bug sigue vivo |
| `rhh/sql/e2-03` | tabla `RHH.ODBS` + secuencia + **`RHH.LQBS.LQBSODBS`** | `ORA-00942` en la pantalla de beneficios **y `ORA-00904` en TODA lectura de `LQBS`**, porque `LiquidacionBeneficioSocial:138` mapea la columna nueva |
| `rhh/sql/e2-04` | corrige al schema `RHH` los dos índices que el `e2-03` creó sin prefijo | No rompe el WAR. Sólo hace falta si el `e2-03` se corrió **antes** de esta corrección |
| **`rhh/sql/e2-06`** | **`RHH.CBEM.BEXTCDGO`** ← `CuentaBancariaEmpleado:53` | `ORA-00904` en **toda** lectura de la cuenta bancaria del empleado |

### 1.1 ⛔ `e2-06` es el peligroso: es destructivo y NO es aditivo

Hace `DROP COLUMN RHH.CBEM.BNCOCDGO` y agrega `BEXTCDGO`. Los demás scripts de esta tanda son
aditivos y conviven con el WAR viejo; **éste no**:

- **WAR viejo + `e2-06` corrido** → el WAR en producción hoy mapea `BNCOCDGO`, que deja de existir.
- **WAR nuevo + `e2-06` sin correr** → el WAR nuevo mapea `BEXTCDGO`, que todavía no existe.

**No hay ventana segura entre los dos.** El `e2-06` y el WAR van pegados, uno detrás del otro, y en
el medio la cuenta bancaria del empleado no se puede leer. Es corto, pero hay que saberlo antes y no
descubrirlo con un usuario adentro.

### 1.2 ⚠️ El control 0.1 del `e2-06` no se saltea

El script exige que **`RHH.CBEM` esté vacía** y dice explícitamente: *«si devuelve cualquier otra
cosa, PARAR Y AVISAR: el bloque 2 borra la columna y con ella el banco de esas cuentas»*.

**En local estaba vacía. En producción puede no estarlo, y ahí el script borra datos reales.**
Correr primero, sólo esto:

```sql
SELECT COUNT(*) AS CUENTAS_EXISTENTES FROM RHH.CBEM;
```

Si no da **0**, el `e2-06` no se corre tal como está: hay que migrar `BNCOCDGO` → `BEXTCDGO` antes,
y eso es un script que todavía no existe.

### 1.3 ⚠️ El `GRANT REFERENCES` quedó comentado en `e2-06` y en `e2-03`

La trampa del `README-ORDEN-PRODUCCION.md`: **una FK hacia otro schema exige `GRANT REFERENCES`
directo**, y Oracle no acepta el privilegio heredado por rol, ni siendo DBA.

**A `e2-07` se le promovió el `GRANT` a bloque ejecutable** (`ae317c3`, «el GRANT REFERENCES pasa a
ser un bloque, no un comentario»). **A `e2-06` y `e2-03` no** — siguen con la línea comentada:

| Script | Línea | FK que va a fallar |
|---|---|---|
| `rhh/sql/e2-06:97` | `-- GRANT REFERENCES ON TSR.BEXT TO RHH;` | `FK_CBEM_BEXT` |
| `rhh/sql/e2-03:109` | `-- GRANT REFERENCES ON SCP.PJRQ TO RHH;` | `FK_ODBS_PJRQ` |

**Consecuencia si se corre de corrido sin el `GRANT`:** los bloques que borran y agregan columnas
**sí pasan** y el de la FK falla. Queda la columna sin su FK — el WAR funciona, pero el script quedó
a medias y nadie se entera salvo que se lean los errores.

> **La lección se aplicó a un script de tres.** Es la misma forma del §14: se arregla el caso que
> dolió y no la familia. Antes de correr `e2-03` o `e2-06`, descomentar el `GRANT` y correrlo con el
> usuario dueño del schema de destino (`TSR` y `SCP` respectivamente).

---

## 2. Orden de despliegue

1. Control `SELECT COUNT(*) FROM RHH.CBEM;` → si no es 0, **`e2-06` no va en esta tanda**.
2. `GRANT REFERENCES` de los schemas de destino (§1.3).
3. Los DDL que falten de la §1, con sus bloques de control.
4. **El frontend ANTES que el WAR** — regla del frente 2: WAR nuevo con FE viejo **rompe `generar()`
   de nómina**, porque no llegaría el `idUsuario`. FE nuevo con WAR viejo es inofensivo.
5. El WAR.

---

## 3. ⚠️ El WAR se va a llevar código de otro equipo

`F:\work\saaBE\v1\saaBE` es el checkout desde el que se despliega por Eclipse, y **tiene adentro,
sin commitear, el frente de la app móvil de `omen-arb-app`**: la entidad `UsuarioApp`, su REST y sus
DTOs. Compila (verificado hoy, `mvn -q compile` exit 0), así que no rompe el build.

**Pero `CRD.USAP` no existe en producción** — su propia línea del registro de reservas dice *«creada
en local, DDL de producción escrito y sin correr»*. Como Hibernate no valida al arrancar (§1), el
despliegue pasa igual y **el daño queda contenido a los endpoints de `usap`**, que van a dar
`ORA-00942` si alguien los llama.

**No es un bloqueo, es un aviso:** subir este WAR publica en producción un frente de otro equipo
antes de que ellos lo den por terminado. Si no se quiere, hay que construir el WAR desde un árbol
limpio (`git stash -u`, o un `git worktree add --detach` sobre `origin/main`).
