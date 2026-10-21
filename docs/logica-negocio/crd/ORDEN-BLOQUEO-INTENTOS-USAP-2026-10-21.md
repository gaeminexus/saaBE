# Orden — el bloqueo por 5 claves malas de USAP no se graba (2026-10-21)

> Equipo de la app móvil (`app1`). Escrita por el árbitro `omen-app-1-arb`.
> Ejecutor: `omen-app-1-saa-be-app`.
>
> **Defecto confirmado dos veces:**
> - leyendo el código (lo avisó `omen-saa-1-arb` y lo verificó el árbitro);
> - **en vivo por el usuario**: en la pantalla «Acceso App Móvil», después de varias claves malas
>   en la app, la cuenta sigue en *Intentos fallidos: 0*, *Activa*.

## El defecto

En `UsuarioAppServiceImpl.autenticar` (línea ~268), cuando la clave está mala:

```java
usap.setIntentosFallidos(intentos);
if (intentos >= MAX_INTENTOS_FALLIDOS) { ...BLOQUEADO, bloqueadoHasta = ahora + 15 min }
usuarioAppDaoService.save(usap, usap.getCodigo());
throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
```

`IncomeException` está anotada con `@ApplicationException(rollback = true)`
(`com.saa.basico.util.IncomeException`). El `save` del DAO genérico corre en **la misma
transacción** del método público (`REQUIRED`), así que **el rollback deshace el `save`**:
- `intentosFallidos` no sube nunca;
- ninguna cuenta se bloquea;
- se pueden probar claves sin límite contra cualquier cédula, que es un dato público.

Afecta a los tres llamadores de `autenticar`: `validarCredencial` (login), `cambiarClave` y
`desactivar`.

## La corrección

El registro del intento fallido tiene que quedar **en una transacción propia** que se confirme
aunque la de afuera haga rollback.

1. **Método nuevo en el DAO de USAP**, `UsuarioAppDaoService` / `UsuarioAppDaoServiceImpl`, con
   JavaDoc en la interfaz como el resto:

   ```java
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   UsuarioApp registrarIntentoFallido(Long codigo, int maxIntentos, LocalDateTime ahora, long minutosBloqueo)
   ```

   - Relee la fila con `em.find(UsuarioApp.class, codigo, LockModeType.PESSIMISTIC_WRITE)`. El
     lock es para que dos intentos en paralelo no lean el mismo contador y se pisen; sin él, un
     ataque con requests concurrentes se salta el límite.
   - **Si el bloqueo anterior ya venció** (estado BLOQUEADO y `bloqueadoHasta` anterior a `ahora`),
     la cuenta vuelve a ACTIVO y el contador arranca de 0 antes de sumar este intento. Es la misma
     regla que hoy está en `autenticar`.
   - Suma 1. Si llega a `maxIntentos`, la cuenta pasa a BLOQUEADO con
     `bloqueadoHasta = ahora + minutosBloqueo`.
   - Devuelve la entidad, o `null` si la fila no existe.
   - **No absorbe errores.** Si falla, que falle: devolver `null` por un error escondería otra vez
     el bloqueo.

2. **En `autenticar`, el camino de clave mala queda así:**
   - **No modifica la entidad `usap` en memoria y no llama a `save`.** Si la transacción de afuera
     tuviera un cambio pendiente sobre esa fila, el `PESSIMISTIC_WRITE` de adentro podría quedarse
     esperando un lock que tiene la propia transacción de afuera.
   - Llama a `usuarioAppDaoService.registrarIntentoFallido(usap.getCodigo(), MAX_INTENTOS_FALLIDOS, ahora, MINUTOS_BLOQUEO)`.
   - Lanza la misma `IncomeException(MSG_CREDENCIALES_INVALIDAS)` de siempre. **La respuesta al
     cliente no cambia**: mismo 401, mismo mensaje genérico.

3. **Para que el `REQUIRES_NEW` funcione, la llamada tiene que pasar por el proxy del contenedor.**
   - Por eso va en el DAO, que es otro EJB, y se llama a través de la referencia inyectada con `@EJB`.
   - Un método `REQUIRES_NEW` en el mismo `UsuarioAppServiceImpl` llamado como `this.metodo()` **no
     abre transacción nueva y el defecto queda igual**, sin ningún aviso.

4. **El resto de `autenticar` queda igual:**
   - Cuenta ELIMINADA, o BLOQUEADA y todavía vigente → 401 sin tocar nada.
   - Clave buena → contador a 0 y `save` normal. Ese camino no lanza excepción, así que se graba
     bien.
   - El desbloqueo por vencimiento que hoy se hace en memoria antes de verificar la clave puede
     quedarse para el camino de clave buena. Para el camino de clave mala lo resuelve el método
     nuevo (punto 1).

5. **`MINUTOS_BLOQUEO` y `MAX_INTENTOS_FALLIDOS`** siguen siendo las constantes del Service: se
   pasan como parámetros, no se duplican en el DAO.

## Lo que NO se toca

- `IncomeException` ni su `rollback = true`: la usa todo el sistema.
- Los mensajes y los códigos HTTP de `UsuarioAppRest`.
- `/movil`, `MovilApplicationConfig`, el borde y la app.

## Documentación

En `API-USUARIO-APP-MOVIL.md`, sección "Bloqueo por intentos fallidos": anotar que el intento
fallido se graba en transacción propia (`REQUIRES_NEW` en el DAO) y **por qué**: el rollback de
`IncomeException`. Así nadie lo "simplifica" de vuelta a un `save` en el mismo método.

## Criterio de entrega

- `mvn -q compile` en exit 0.
- La lista exacta de archivos, por `SendMessage` a `omen-app-1-arb`.
- **Prueba (la hace el usuario, con la pantalla «Acceso App Móvil» abierta):**
  1. Una clave mala en la app → la pantalla muestra *Intentos fallidos: 1*.
  2. Cuatro malas más → *Bloqueada*, con *Bloqueada hasta* = hora + 15 min.
  3. Con la cuenta bloqueada, **la clave buena también se rechaza**.
  4. *Resetear clave* desde la pantalla → la cuenta vuelve a *Activa*, con 0 intentos.
  5. Pasados los 15 minutos sin resetear, una clave mala → *Intentos fallidos: 1*, no un nuevo bloqueo inmediato.
