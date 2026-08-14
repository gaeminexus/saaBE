# Corrección: cruces de anticipo sin movimiento en el listado de anticipos

**Fecha:** 2026-08-14
**Estado:** PENDIENTE DE EJECUTAR (revisar los SELECT de control antes de cada INSERT/UPDATE)

## Contexto

Hasta el 2026-08-14, `aplicarAnticipo` (CXP y CXC) descontaba el saldo global de
anticipos del titular (`TSR.PRCC.PRCCSLIN`) pero **no** dejaba ningún registro en
la tabla de anticipos (`PGS.ANTP` / anticipos de cliente): la factura quedaba
pagada y el asiento era correcto, pero el listado de movimientos de anticipos no
mostraba la resta y el saldo acumulado de la última fila quedaba desactualizado.

El código ya está corregido: cada cruce inserta ahora un **movimiento negativo**
(valor = -cruce, saldo = acumulado tras el cruce, estado Confirmado, con el
asiento del cruce) enlazado a la aplicación por `APLPANTP`, y la reversión del
cruce anula ese movimiento y devuelve el saldo.

Este documento corrige los cruces ejecutados **antes** del fix, que no tienen
movimiento. Aplica a CXP (proveedores); si hubo cruces CXC (clientes) antes del
fix, repetir el mismo procedimiento sobre las tablas de anticipos de cliente.

## 1. Control: cruces activos sin movimiento

```sql
-- Aplicaciones de cruce de anticipo (tipo 4) activas (estado 1) sin FK al movimiento
SELECT a.APLPCDGO      AS aplicacion,
       a.APLPPJRQ      AS empresa,
       f.NUMERO        AS factura,
       f.TITULAR       AS proveedor,
       a.APLPMAPL      AS valor_cruzado,
       a.APLPFAPL      AS fecha_cruce,
       a.APLPASNT      AS asiento,
       a.APLPUSAR      AS usuario
  FROM PGS.APLP a
  JOIN PGS.FCTC f ON f.ID = a.APLPFCTC
 WHERE a.APLPTDPG = 4          -- TipoDocPagoAplicacion.ANTICIPO
   AND a.APLPESTD = 1          -- EstadoAplicacionPago.ACTIVO
   AND a.APLPANTP IS NULL
 ORDER BY a.APLPCDGO;
```

Anotar por cada fila: `aplicacion`, `empresa`, `proveedor`, `valor_cruzado`,
`fecha_cruce`, `asiento`, `usuario` — se usan en el paso 3.

## 2. Control: saldo global actual del proveedor

El `ANTPSALD` del movimiento debe quedar con el **saldo acumulado tras el
cruce**. Si el proveedor tiene UN solo cruce sin movimiento, ese acumulado es el
saldo global actual (el PRCC ya fue descontado en su momento):

```sql
-- Saldo de anticipos (tipoCuenta=2) del rol proveedor del titular
SELECT pc.PRCCCDGO, pc.PRCCSLIN AS saldo_actual
  FROM TSR.PRCC pc
  JOIN TSR.PRRL pr ON pr.PRRLCDGO = pc.PRRLCDGO
 WHERE pc.PRCCTPOO = 2
   AND pc.PJRQCDGO = :empresa
   AND pr.PRSNCDGO = :proveedor
   AND pr.PRRLRZZA = 2;               -- rol Proveedor (PersonaRol.rubroRolPersonaH)
```

Si el proveedor tiene VARIOS cruces sin movimiento, calcular el acumulado de
cada uno hacia atrás (saldo_actual + cruces posteriores) y revisarlo a mano.

## 3. Insertar el movimiento negativo y enlazarlo (una vez por aplicación)

```sql
INSERT INTO PGS.ANTP (ANTPCDGO, ANTPTTLR, ANTPPJRQ, ANTPFANT, ANTPFRCP,
                      ANTPNDOC, ANTPVLOR, ANTPSALD, ANTPOBSR, ANTPESTD,
                      ANTPUSAR, ANTPASNT, ANTPFCRG)
VALUES (PGS.SQ_ANTPCDGO.NEXTVAL,
        :proveedor,
        :empresa,
        :fecha_cruce,
        :fecha_cruce,
        'Factura N° ' || :numero_factura,
        -:valor_cruzado,
        :saldo_acumulado_tras_el_cruce,   -- paso 2
        'Cruce con factura N° ' || :numero_factura || ' | Corrección 2026-08-14',
        2,                                -- Confirmado
        :usuario,
        :asiento,
        SYSTIMESTAMP);

-- Enlazar la aplicación con el movimiento recién creado
UPDATE PGS.APLP
   SET APLPANTP = PGS.SQ_ANTPCDGO.CURRVAL
 WHERE APLPCDGO = :aplicacion
   AND APLPANTP IS NULL;

COMMIT;
```

## 4. Verificación

```sql
-- Ya no deben quedar cruces activos sin movimiento
SELECT COUNT(*) FROM PGS.APLP
 WHERE APLPTDPG = 4 AND APLPESTD = 1 AND APLPANTP IS NULL;

-- El listado de anticipos del proveedor debe mostrar el movimiento negativo
SELECT ANTPCDGO, ANTPFANT, ANTPVLOR, ANTPSALD, ANTPESTD, ANTPNDOC
  FROM PGS.ANTP
 WHERE ANTPTTLR = :proveedor AND ANTPPJRQ = :empresa
 ORDER BY ANTPFANT DESC, ANTPCDGO DESC;
```

**Importante:** NO tocar `TSR.PRCC.PRCCSLIN` en esta corrección — el saldo
global ya fue descontado correctamente cuando se hizo el cruce; aquí solo se
agrega el movimiento que faltaba en el listado.
