# Hibernate: `hibernate.max_fetch_depth = 2`

**Fecha:** 2026-10-19 · **Archivo:** `src/main/resources/META-INF/persistence.xml` (unidad `SaaPU`)

## El problema
Producción falló con `ORA-04036` (PGA por encima del límite de 4 GB) al confirmar el pago 769.
Al cargar un `AnticipoEmpleado` (`RHH.ANTE`) por id, Hibernate arma **un solo SELECT con 431 LEFT JOIN** (contados sobre el SQL del log del 2026-10-19). De esos, 254 son `SCP.PJRQ` (empresa/usuario, 127) y `SCP.PGSP` (su jerarquía, 127): cada entidad trae su `Empresa`, y cada `Empresa`/`Usuario` trae su `Jerarquia`, ambas EAGER. Le siguen la cadena del asiento (`CNT.ASNT` 16, `MYRZ` 16, `PRDO` 32, `PLNT` 16) y titular/persona (`TSR.TTLR` 17, `CRD.PSSS` 17).

## La consulta de 431 JOIN
Casi toda relación `@ManyToOne` del proyecto es `EAGER` (el valor por defecto de JPA). La cadena es:
`ANTE → PagoProgramado (15 relaciones) → Asiento → Periodo → Empresa → Jerarquia → …`
y cada rama se vuelve a expandir. La `Empresa` 1236 llega a repetirse unas 116 veces en el mismo JOIN.
Oracle no aguanta la memoria que pide ese plan.

## La configuración
```xml
<property name="hibernate.max_fetch_depth" value="2"/>
```
Las relaciones siguen siendo `EAGER`. Lo que pasa de la profundidad 2 se carga con un `SELECT` aparte
por id, y la caché de la sesión hace que la `Empresa` se lea una sola vez. El objeto resultante y el
JSON son idénticos. Los `JOIN FETCH` escritos a mano en JPQL no dependen de esta propiedad.

## Por qué no LAZY
Las entidades se serializan directo a JSON (Jackson, sin módulo Hibernate ni capa DTO). Un proxy
`LAZY` sin inicializar rompe la serialización o dispara `LazyInitializationException` fuera de la
transacción. Pasarlo a `LAZY` obligaría a revisar cientos de entidades y todos los endpoints.

## Cómo revertir
Quitar la línea `hibernate.max_fetch_depth` (y su comentario) de `persistence.xml` y redesplegar.

## Qué vigilar después del despliegue
- Más consultas pequeñas por pantalla (SELECT por id): es lo esperado.
- Ninguna consulta gigante (decenas de JOIN) ni `ORA-04036`.
- Si alguna pantalla se vuelve lenta por exceso de SELECT, subir el valor a 3 y medir antes de dejarlo.

## Jerarquia de Empresa/Usuario en SELECT aparte
**Cambio:** en `model/scp/Empresa.java` y `model/scp/Usuario.java`, el campo `jerarquia` lleva
`@Fetch(FetchMode.SELECT)` (`org.hibernate.annotations`), sin tocar `@ManyToOne`, `@JoinColumn`, getters ni NamedQueries.
**Por qué:** de los 431 LEFT JOIN al cargar un `AnticipoEmpleado`, 254 eran `SCP.PJRQ` y `SCP.PGSP`
(cada Empresa/Usuario traía su Jerarquia por JOIN). Nadie lee la jerarquia de la empresa ni del usuario.
**Efecto:** la relación sigue EAGER y el JSON y la grabación no cambian; solo se lee con un SELECT por id.
**Revertir:** quitar la anotación `@Fetch(FetchMode.SELECT)` y sus dos imports de ambas clases.
**Dependencia:** `hibernate-core` se agregó al `pom.xml` como `provided` 6.6.31.Final (solo para compilar la
anotación; WildFly la provee y no entra al WAR). Hay que subir la versión si se actualiza el WildFly.
