# Búsquedas rotas por `selectByCriteria` — halladas el 2026-08-20

**Origen:** barrido del backend al resolver el `case INTEGER` de `EntityDaoImpl.selectByCriteria`
para RRHH. **Están rotas desde antes**; no son una regresión de ningún trabajo en curso, y por
eso se registran aquí en vez de arreglarse sobre la marcha.

---

## Por qué fallan

`EntityDaoImpl.selectByCriteria` escribe la cláusula JPQL mirando el nombre del campo pero
**enlaza el parámetro con un `switch` sobre el tipo de dato** que sólo tiene `case` para `STRING`,
`LONG`, `DATE`, `DATE_TIME` y `DOUBLE`. `INTEGER` cae en `default: break;`, así que el parámetro
queda declarado en la consulta y **sin enlazar**.

Hibernate valida los bindings antes de ejecutar (`QueryParameterBindingsImpl.validate()`) y lanza
`QueryParameterException` para cualquiera con `isBound() == false`. Nadie la absorbe: el REST la
convierte en un 500. **No devuelve resultados sin filtrar — no devuelve nada.**

---

## Las cuatro

| Pantalla | Campo | Tipo en la entidad | Arreglo |
|---|---|---|---|
| `entidad-consulta` | `sectorPublico` | `Entidad.sectorPublico` → `Long` | El frontend debe mandar `LONG` |
| `entidad-consulta` | `migrado` | `Entidad.migrado` → `Long` | El frontend debe mandar `LONG` |
| `prestamo-consulta` | `plazo` | `Prestamo.plazo` → `Long` | El frontend debe mandar `LONG` |
| `aportes-dash` | `anio`, `mes` | **no existen en `Aporte`** | Necesita revisión aparte: falla al parsear el JPQL con cualquier tipo |

**Añadir el `case INTEGER` no las repara.** Al contrario: por la misma estrictez que hoy rechaza
el parámetro sin enlazar, enlazar un `Integer` contra un atributo `Long` daría el error simétrico
(«Argument of type Long did not match parameter type Integer»). Las tres primeras son un defecto
del criterio que manda el cliente, no del DAO.

> El backend marcó lo del error simétrico como **inferencia por simetría, no verificado
> ejecutando**. Antes de dar por buena la corrección de las tres pantallas, comprobar el tipo de
> la entidad y probar la búsqueda.

La cuarta es distinta y peor: `aportes-dash` filtra por `anio` y `mes`, que `Aporte` no declara.
Ahí no hay tipo que sirva; hay que decidir de dónde salen esos dos filtros —si de una columna que
falta, de un derivado de la fecha, o de una consulta propia en el DAO—.

---

---

## Un quinto defecto, en el mismo `switch`: `BETWEEN` sobre `DOUBLE`

El `case DOUBLE` enlaza **el segundo parámetro de un `BETWEEN` con `getValor()` en vez de
`getValor1()`**, así que un rango de importes compara contra el límite inferior dos veces:
`BETWEEN 100 AND 100` en lugar de `BETWEEN 100 AND 500`. Una búsqueda por rango de importe
devuelve **sólo las coincidencias exactas con el límite inferior**.

**Por qué no se arregló de inmediato.** No cae bajo la regla de «aditivo o nada» que autorizó el
`case INTEGER`: allí la conducta previa era **fallar**, así que nadie podía apoyarse en ella; aquí
hoy **salen filas**, y una corrección cambia los resultados de quien las esté leyendo.

**Cómo se decide, que es la misma pregunta empírica de la otra vez:** barrer el frontend por
criterios que combinen `DOUBLE` con `BETWEEN`.

- **Si no hay ninguno**, la corrección es aditiva de hecho y se aplica sin más.
- **Si los hay**, se listan y se mira uno por uno. Nadie puede depender legítimamente de que un
  rango devuelva sólo el extremo inferior, pero sí puede haber una pantalla o un reporte cuyo
  resultado cambie de tamaño, y eso hay que verlo antes, no después.

### Barrido hecho y corrección aplicada — 2026-08-20

**Nueve llamadas a `asignaUnCampoConBetween` en todo el frontend. Ocho son `DATE`** —listado de
asientos, contratos, entidad partícipe, préstamos y vacaciones— y no las toca este defecto. **La
novena es la única afectada:**

`modules/crd/forms/prestamo/prestamo-consulta/prestamo-consulta.component.ts:362`, el helper
`agregarRangoNumerico`, con dos campos: **`montoSolicitado` y `saldoTotal`**, ambos `Double` en
`Prestamo`. Solo entra por la rama del `BETWEEN` cuando el usuario llena **los dos extremos**;
con uno solo va por `MAYOR_IGUAL` / `MENOR_IGUAL`, que no están afectados.

Con un solo sitio, y siendo ese sitio uno que hoy devuelve casi siempre cero filas, se autorizó y
**se aplicó la corrección**: `getValor1()` en la rama `BETWEEN` del `case DOUBLE`. **Pendiente de
recompilar.**

> **Prueba de humo pendiente:** el filtro por rango de monto y de saldo de la consulta de
> préstamos **va a devolver resultados por primera vez**. No es una regresión —empieza a
> funcionar lo que no funcionaba— pero conviene mirarlo la próxima vez que alguien toque CRD:
> es la única pantalla del sistema cuyo conjunto de resultados cambia por este arreglo.

---

## Un sexto, de otra familia: JSON construido a mano en 14 sitios

`FormaPagoFacturaRest`, `FormaPagoLiquidacionRest`, `EjecucionReporteRest` y
`EjecucionReporteCarteraRest` devuelven sus errores así:

```java
.entity("{\"error\":\"" + e.getMessage() + "\"}")
```

**Concatenan el JSON.** Un `getMessage()` con una comilla doble o un salto de línea produce un
cuerpo **inválido** que ningún cliente puede parsear — y los mensajes de excepción de Hibernate
llevan las dos cosas: el `MensajeErrorJsonFilter` se probó justamente con uno que devolvía el
JPQL entero, con comillas y tres líneas.

No estalla hoy porque esos mensajes concretos suelen ser prosa simple, y el filtro los respeta
correctamente —su guarda del `{` evita envolverlos dos veces, comprobado en vivo—. El arreglo es
sustituir la concatenación por un `Map` y dejar que lo serialice el proveedor, igual que hace el
filtro. Catorce sitios, cambio mecánico.

---

## Un séptimo: `getRubros/{id}` está roto para todos los rubros

`GET /rest/pdtr/getRubros/{id}` devuelve
`StrictJpaComplianceViolation: Encountered implicit 'select' clause` para **cualquier** rubro. La
consulta de `selectByCodigoAlternoRubro` no declara el `select`, y Hibernate 6 en modo estricto ya
no lo tolera — lo que en versiones anteriores era una laxitud aceptada.

Vive en `basico`, así que afecta a **todos los módulos**, no sólo a RRHH.

**Autorizado el arreglo, y sin barrido previo**, por el mismo criterio que el `case INTEGER`: hoy
la consulta **lanza**, así que ningún consumidor puede estar apoyándose en su resultado. Añadir el
`select e` explícito es aditivo por definición. Es una línea.

> Conviene mirar de paso si hay más consultas del proyecto con `select` implícito: si ésta lleva
> tiempo rota sin que nadie lo notara, es porque nadie la llama — pero otras de la misma familia
> pueden estar en rutas que sí se usan y fallar el día que alguien las toque.

---

## El barrido del `select` implícito — 2026-08-20

**20 consultas en 8 archivos**, todas con la forma `em.createQuery(" from X ...")` sin `select`.

**El mecanismo, que explica por qué son todas y no algunas:** `em.createQuery(String)` es la
entrada **JPQL**, y JPQL no admite el `select` implícito — era HQL antiguo lo que lo permitía.
Hibernate 6 lanza `StrictJpaComplianceViolation: Encountered implicit 'select' clause` **al
construir la consulta**, así que el método no llega a ejecutarse. No es una configuración que se
pueda relajar desde `persistence.xml`: es la semántica de la entrada que se está usando.

| Archivo | Consultas | Qué se hace |
|---|---:|---|
| `basico/DetalleRubroDaoServiceImpl` | 2 | **Corregidas** |
| `basico/EmpresaDaoServiceImpl` | 2 | **Corregidas** |
| `cxc/ProductoCobroDaoServiceImpl` | 6 | Clase viva: se arregla cuando alguien toque CXC |
| `cxp/ProductoPagoDaoServiceImpl` | 6 | Clase viva: se arregla cuando alguien toque CXP |
| `cxp/AprobacionXMonto`, `MontoAprobacion` | 3 | **Deprecadas: no se arreglan, se borran con la clase** |
| `cxp/TempUsuarioXAprobacion`, `UsuarioXAprobacion` | 2 | Revisar si siguen vivas |

Reparar una consulta rota dentro de algo marcado para desaparecer es trabajo que se tira dos
veces.

### El hallazgo de fondo no son 20 consultas: son 20 métodos muertos

Si una consulta con `select` implícito **lanza en su primera ejecución**, cada uno de esos veinte
métodos **está muerto desde el día que se escribió**. Nunca devolvió una fila a nadie.

Eso no es una familia de defectos: es una familia de **código que sobra o de funcionalidad que
quedó a medias**, y saber cuál de las dos cosas es vale más que el `select`. Los nombres apuntan a
funcionalidad prevista y no terminada —`selectArbolEmpresas`, `selectEmpresaByUsuario`,
`selectModulosNoClienteConContabilidad`, seis métodos de producto de cobro y seis de producto de
pago—, no a restos de refactorizaciones.

**La pregunta para quien retome cada módulo, y va antes que el arreglo:** ¿esta consulta hace
falta? Si hace falta, el `select` la revive y hay que probar qué se esperaba de ella; si no,
sobra el método entero. Añadirle el `select` sin responder eso deja veinte métodos vivos que
nadie sabe si alguien debía llamar.

---

## Estado

**Las cuatro primeras siguen sin asignar. La quinta está corregida** el 2026-08-20, tras el
barrido, y solo espera recompilación. Nada de esto bloquea el módulo RRHH ni la calibración de
ASOPREP. Se registra para que no se pierda: el arreglo de las tres primeras es de una línea cada
una en el frontend y la de `aportes-dash` necesita una decisión de modelo, porque `anio` y `mes`
no existen en `Aporte` y no hay tipo que valga.
