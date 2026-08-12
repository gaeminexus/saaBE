# Límites transaccionales de los documentos electrónicos (SRI)

## Fecha: 2026-08-12

## Regla de negocio

El envío al SRI es **irreversible**: una vez que el SRI recibe o autoriza un
comprobante, el sistema no puede "deshacerlo". De ahí la regla que gobierna todo
este módulo:

1. Si el SRI **recibe** el documento → el documento **queda grabado** en BD, pase
   lo que pase después.
2. Si el SRI **autoriza** el documento → se genera la contabilidad.
3. Si falla el **cruce/abono con la factura** → sólo ese cruce queda pendiente.
   Ni el documento ni el asiento se reversan.
4. Sólo se descarta el registro cuando el comprobante **nunca llegó a existir en
   el SRI** (error de XML/firma, o recepción `DEVUELTA`).

## El problema que se corrigió

Todo el proceso corría en **una sola transacción** (todos los `@Stateless` usan
`REQUIRED` por defecto, y la transacción la abre el primer EJB que invoca REST).
El código intentaba aislar los fallos tardíos así:

```java
try {
    ... generar asiento ...
    aplicacionPagoCxpService.aplicarRetencionEmitida(...);   // EJB anidado
} catch (Throwable e) {
    resultado.put("advertenciaAsiento", ...);   // ← NO SIRVE DE NADA
}
```

**Ese `catch` no puede salvar nada.** El EJB anidado corre con `REQUIRED`, así que
**se une a la transacción del llamador**. Cuando escapa una excepción de sistema
(cualquier `RuntimeException`), el contenedor marca *esa misma transacción* como
`rollback-only`. Atraparla después no la revive: el commit final falla y se
reversa **todo**, incluido un documento ya autorizado por el SRI.

Caso real que lo destapó (2026-08-12, retención V2 id=81):

```
✓ Retención V2 AUTORIZADA por el SRI.
✓ Asiento contable generado: CXP-2026-08-0042 | ID: 7835
=== aplicarRetencionEmitida | retencion=81 | empresa=1236 ===
ERROR ... StrictJpaComplianceViolation: Encountered non-compliant non-standard
         function call [replace] ... use FUNCTION(functionName[,...]) syntax
```

Resultado: la retención quedó autorizada en el SRI y **desaparecida** del sistema.

> Nota: los archivos en disco (`.xml` firmado / autorizado, `.pdf` RIDE) **sí
> sobreviven** al rollback — las escrituras de archivo no son transaccionales.
> Sirven para recuperar la clave de acceso de un documento perdido, bajo
> `{saa.upload.dir}/resources/{idFacturador}/`.

## La solución: etapas transaccionales independientes

El método orquestador pasa a `NOT_SUPPORTED` (sin transacción propia) y cada
etapa corre en la suya con `REQUIRES_NEW`. Con `REQUIRES_NEW` el contenedor
suspende la transacción del llamador; si la etapa falla, sólo se reversa **esa**
transacción y el llamador recibe la excepción con su trabajo ya confirmado
intacto.

**Detalle crítico:** las etapas se invocan a través de
`sessionContext.getBusinessObject(XxxService.class)` (método `self()` en cada
impl). Una llamada directa `this.metodo()` **se salta los interceptores del
contenedor** y correría en la transacción del llamador — que es exactamente el
bug que se está corrigiendo.

```java
@Resource
private SessionContext sessionContext;

private FacturaService self() {
    return sessionContext.getBusinessObject(FacturaService.class);
}
```

### Por qué el orquestador debe ser `NOT_SUPPORTED`

No basta con poner `REQUIRES_NEW` sólo en la etapa del asiento. El asiento se
genera **leyendo el documento desde la BD**, y una transacción nueva no puede ver
filas que la transacción del llamador todavía no confirmó. El documento tiene que
estar *committed* antes de que arranque la etapa contable.

## Flujo resultante

```
procesarXxxCompleta            @TransactionAttribute(NOT_SUPPORTED)   ← orquestador
 ├─ emitirXxxAnteSRI           @TransactionAttribute(REQUIRES_NEW)    ← commit
 │    prepara campos → genera y firma XML → WS1 recepción
 │    → si el SRI acepta: graba documento + detalles + paths
 │    → WS2 autorización: persiste estado, autorización y XML autorizado
 ├─ generarContabilidadXxx     @TransactionAttribute(REQUIRES_NEW)    ← commit
 ├─ aplicarPagoXxx             @TransactionAttribute(REQUIRES_NEW)    ← commit
 └─ email                      (sin transacción)
```

### Cobertura por documento

| Documento | Impl | Emisión | Asiento | Cruce con factura |
|---|---|---|---|---|
| Factura | `FacturaServiceImpl` | `emitirFacturaAnteSRI` | `generarContabilidadFactura` | — |
| Nota de Crédito | `NotaCreditoServiceImpl` | `emitirNotaCreditoAnteSRI` | `generarContabilidadNotaCredito` | `aplicarPagoNotaCredito` |
| Nota de Débito | `NotaDebitoServiceImpl` | `emitirNotaDebitoAnteSRI` | `generarContabilidadNotaDebito` | `aplicarPagoNotaDebito` |
| Retención (V1) | `RetencionServiceImpl` | `emitirRetencionAnteSRI` | `generarContabilidadRetencion` | — |
| Retención V2 | `RetencionV2ServiceImpl` | ver nota | `generarContabilidadRetencionV2` | `aplicarPagoRetencionV2` |
| Liquidación de Compra | `LiquidacionCompraServiceImpl` | `emitirLiquidacionAnteSRI` | `generarContabilidadLiquidacion` | — |

**Nota sobre Retención V2:** su flujo es distinto — graba en BD *antes* de enviar
al SRI (no "BD tras RECIBIDA"), así que su emisión está partida más fino:
`grabarRetencionV2ConDetalles` → `generarXMLRetencionV2` → `autorizarRetencionV2`
→ `generarContabilidadRetencionV2` → `aplicarPagoRetencionV2`, más
`eliminarRetencionV2NoEmitida` para descartar el registro cuando nunca llegó al
SRI.

## Idempotencia y recuperación

Todas las etapas posteriores a la emisión son **idempotentes**:

- `generarContabilidadXxx` no genera un segundo asiento si el documento ya tiene
  uno (devuelve `yaExistia=true`).
- `aplicarPagoXxx` consulta `selectActivasByDocumento(tipo, id)` y no duplica el
  cruce si ya existe.
- `marcarXxxAutorizada` no reescribe el estado si ya está en 5.

Por eso el **punto de recuperación** es el endpoint que ya existía:

```
consultarYActualizarEstadoXxx(idDocumento)
```

También pasó a `NOT_SUPPORTED` y ahora reintenta *todas* las etapas pendientes:
consulta el estado al SRI, actualiza el documento, genera el asiento si falta y
registra el cruce si falta. Ejecutarlo varias veces es seguro.

## Contrato de respuesta

El mapa que devuelven los `procesar*Completa` incorpora banderas de pendientes:

| Clave | Significado |
|---|---|
| `exito` | `true` si el SRI autorizó el documento |
| `etapa` | `COMPLETADO` o `COMPLETADO_CON_PENDIENTES` |
| `contabilidadPendiente` | `true` si falló la generación del asiento |
| `advertenciaAsiento` | Detalle del fallo contable |
| `cruceFacturaPendiente` | `true` si falló el cruce/abono con la factura |
| `advertenciaAplicacion` | Detalle del fallo del cruce |
| `asiento` | Número alterno del asiento generado |
| `aplicacionPago` | Id de la aplicación de pago registrada |

`exito=true` con pendientes es correcto y deliberado: el documento **está**
autorizado ante el SRI. El frontend debe mostrar las advertencias, no tratarlo
como error.

## Cambios de comportamiento

- **El asiento ya no se anula cuando falla el cruce.** Antes,
  `anulaAsientoPorFalloAplicacion` anulaba el asiento recién creado (y ese método
  se eliminó). Ahora el asiento se conserva: el documento está autorizado y su
  contabilidad es válida por sí sola. La contrapartida es que el saldo de la
  factura afectada no refleja el cruce hasta que se complete.
- **Retención V1 y Liquidación de Compra ahora vinculan el asiento al documento**
  (`setAsiento`). Antes lo generaban sin enlazarlo, y por eso la anulación no
  encontraba el asiento que debía anular.
- **`consultarYActualizarEstadoRetencionV2`** usaba
  `TipoAsientos.FACTURAS_VENTA` para el asiento de una retención — inconsistente
  con el flujo principal. Unificado a `TipoAsientos.RETENCIONES_EMITIDAS_V2`.
- **Retención V2, caso `DEVUELTA`:** antes se descartaba con rollback; ahora se
  elimina explícitamente (`eliminarRetencionV2NoEmitida`). Consecuencia: el
  secuencial consumido **no** se devuelve, así que puede quedar un hueco en la
  numeración.
- **Factura, caso "CLAVE ACCESO REGISTRADA":** antes terminaba sin generar
  asiento. Ahora continúa al flujo contable como cualquier documento autorizado.
- **`VALIDACION_FACTURA`** en `RetencionV2Rest` devuelve 422 en vez de 500 (es un
  error de datos del usuario, no técnico).

## JPQL: funciones no estándar

Hibernate 7 (WildFly 38) corre con *strict JPA compliance*. Las funciones de BD
que no son estándar de JPQL deben llamarse con la sintaxis `FUNCTION`:

```java
// ✗ Revienta: StrictJpaComplianceViolation
" where  replace(f.numero, '-', '') = :numero "

// ✓ Correcto: sintaxis JPQL estándar, Hibernate la traduce al REPLACE de Oracle
" where  FUNCTION('replace', f.numero, '-', '') = :numero "
```

Corregido en `AplicacionPagoCxpDaoServiceImpl.selectFacturaByNumero` y
`AplicacionPagoCxcDaoServiceImpl.selectFacturaByNumero`.

> Ojo de rendimiento: comparar sobre una expresión impide usar índice en
> `numero`, así que la consulta hace full scan. Si el volumen crece, conviene una
> columna con el número normalizado o un índice basado en función
> `REPLACE(XXXXNMRO,'-','')`.

## Al tocar este código

- Nunca dejes que una etapa posterior a la emisión corra en la transacción del
  documento.
- Invoca siempre las etapas por `self()`, nunca por `this.`.
- Marca `sessionContext.setRollbackOnly()` antes de relanzar dentro de una etapa
  `REQUIRES_NEW`: `IncomeException` es una *application exception* y por sí sola
  **no** reversa la transacción, así que sin eso podrían quedar escrituras
  parciales confirmadas.
- Mantén la idempotencia de cada etapa: el endpoint de recuperación las reejecuta.
