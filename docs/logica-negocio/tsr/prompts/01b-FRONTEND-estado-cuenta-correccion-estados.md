# PROMPT 01b — AGENTE FRONTEND — Estado de cuenta: corregir los valores de `estadosAnulados` (corrección del prompt 01)

**Agente:** FRONTEND (`C:\work\saaFE\v1\saaFE`). **No tocar el backend.**
**Contexto:** implementaste el prompt 01 correctamente, pero **los valores que te di eran incorrectos**. El estado de cuenta está mostrando documentos anulados. Este prompt corrige solo eso; el resto de tu trabajo (la fuente `rcv2`, `saldoDesconocido`, `anulado` por fuente) queda como está.

## El dato correcto (verificado en el backend y contra la base de datos)

En CXC un documento anulado **NO** queda con `estado = 2`. Queda con `estado = 0`:

```java
// com/saa/rubros/Estado.java
public static final int ACTIVO   = 1;
public static final int INACTIVO = 0;   // <-- anulado

// FacturaServiceImpl.java:2640 (anularFactura), y equivalente en
// NotaCreditoServiceImpl:1530, NotaDebitoServiceImpl:1646,
// RetencionV2ServiceImpl:1721, RetencionServiceImpl:1292
factura.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));  // 0
factura.setEstadoEmision(3L);                                      // 3 = ANULADA
```

Comprobado con datos reales: en la BD las 11 facturas `CBR.FCTR` con `ESTADO = 0` tienen todas `MOTIVOANULACION` no nulo y `ESTADOEMISION = 3`. Igual las 4 `CBR.NTCR` y las 3 `CBR.RTV2` en estado 0. **El valor 2 no aparece en ninguna tabla.**

En CXP los documentos nacen con `Estado.ACTIVO = 1` y **no existe ningún flujo que los anule** (verificado por búsqueda en todo el backend); si algún día se anulan, será con `Estado.INACTIVO = 0` por la misma convención.

Los anticipos sí usan un catálogo propio y ahí tu valor era correcto: `EstadoAnticipoCliente`/`EstadoAnticipoProveedor` = `INGRESADO=1, CONFIRMADO=2, ANULADO=3, MIGRADO=4`.

## Tabla correcta

| Fuente | `estadosAnulados` correcto | Antes (incorrecto) |
|---|---|---|
| CXC: `fctr`, `ntcr`, `ntdb`, `rtv2` | **`[0, 6]`** | `[2]` |
| CXP: `fctc`, `ntcc`, `ntdc`, `rcv2` | **`[0]`** | `[2]` |
| Anticipos: `antc`, `antp` | `[3]` (sin cambio) | `[3]` |

El `6` en CXC es "devuelta / no autorizada por el SRI" (`FacturaServiceImpl:710` la deja en 6 con `estadoEmision = 2`): es un documento que el SRI rechazó, nunca fue válido y no representa deuda, así que no debe sumar al estado de cuenta. Los estados 1 (ingresada), 3 (firmada) y 4 (enviada) sí se siguen mostrando: son documentos en trámite que normalmente terminan autorizados.

## Tareas

1. En `src/app/modules/tsr/service/estado-cuenta-titular.service.ts`, actualizar `estadosAnulados` de cada `FuenteDocumento` según la tabla de arriba (afecta a los dos roles, CLIENTE y PROVEEDOR).

2. **Corregir el manejo de `estado` nulo.** Ahora que `0` significa anulado, la conversión actual es peligrosa:
   ```ts
   anulado: fuente.estadosAnulados.includes(Number(fila?.estado)),   // Number(null) === 0  ->  ¡falso positivo!
   ```
   Cambiar por algo que trate "sin estado" como no anulado:
   ```ts
   const estadoCrudo = fila?.estado;
   const estadoNum = (estadoCrudo === null || estadoCrudo === undefined || estadoCrudo === '')
       ? null : Number(estadoCrudo);
   ...
   anulado: estadoNum !== null && !Number.isNaN(estadoNum) && fuente.estadosAnulados.includes(estadoNum),
   ```

3. **Mostrar el estado del documento en la fila** (columna o chip) usando una etiqueta legible, para que se vea por qué un documento está o no en la lista. Mapeo por origen de la fuente:
   - CXC: `0 Anulada · 1 Ingresada · 3 Firmada · 4 Enviada · 5 Autorizada · 6 No autorizada`
   - CXP: `0 Anulada · 1 Activa`
   - Anticipos: `1 Ingresado · 2 Confirmado · 3 Anulado · 4 Migrado`
   Ponerlo en el modelo unificado (`etiquetaEstado: string`) resuelto en `normalizar()`, igual que `anulado`.

4. Dejar un comentario corto sobre el campo `estadosAnulados` indicando que en CXC/CXP anulado es `0` (`Estado.INACTIVO`) y que **no es 2**, para que nadie lo "corrija" de vuelta.

## Verificación
- Un cliente con facturas anuladas: esas facturas **no** deben aparecer; las autorizadas (5) sí.
- Un proveedor con retenciones emitidas anuladas (RTV2 estado 0): no deben aparecer.
- Ningún documento debe desaparecer por tener `estado` nulo.

## Restricciones
- No tocar el backend. No revertir nada del prompt 01 fuera de lo indicado aquí.
- Entregar: archivos modificados y confirmación de los valores finales por fuente.
