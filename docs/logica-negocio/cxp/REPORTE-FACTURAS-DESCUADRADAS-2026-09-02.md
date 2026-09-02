# Facturas de compra con descuadre de centavos — reporte

**Equipo:** `lap-saa-1` · **2026-09-02** · Módulo `cxp`
**Datos medidos en producción** con `cxp/sql/lap1-04-diagnostico-descuadre-centavos-fctc.sql`,
corrido por el usuario. **Este documento describe lo ya contabilizado**, no lo que hará el cambio.

---

## 1. Resumen

| | |
|---|---|
| Facturas con algún descuadre | **7** |
| Facturas cuya cuenta por pagar quedó **mal** | **5** |
| Facturas ya correctas pese al descuadre | **2** — ver §3 |
| Diferencia máxima | **1 centavo** |
| Casos por encima de la tolerancia | **0** (bloque 5 vacío) |
| Cuenta `4.8.90.90.35` | ✅ **existe y está activa** — id 10849, nivel 5, `DIFERENCIA POR REDONDEO SRI` |

---

## 2. Las 5 facturas a anular y volver a cargar

`CxP contabilizada` es lo que el asiento registró: `Σ detalles + IVA de cabecera`.
`Debió ser` es el `importeTotal` de la factura.

| id | Número | CxP contabilizada | Debió ser | Diferencia | Al recargar, el ajuste será |
|---:|---|---:|---:|---:|---|
| **120** | 164-107-000475720 | 49.77 | **49.76** | −0.01 | **HABER** 0.01 |
| **121** | 164-107-000475719 | 35.73 | **35.72** | −0.01 | **HABER** 0.01 |
| **446** | 002-101-000574112 | 23.83 | **23.84** | +0.01 | **DEBE** 0.01 |
| **447** | 072-144-000133636 | 22.09 | **22.10** | +0.01 | **DEBE** 0.01 |
| **118** | 001-500-000078064 | 19.79 | **19.80** | +0.01 | **DEBE** 0.01 |

**Neto sobre la cuenta por pagar: +1 centavo** (tres de más, dos de menos). El impacto económico es
nulo; lo que importa es que la CxP de cada proveedor deje de diferir de su factura.

Claves de acceso completas, para buscarlas en el portal:

```
120  0907202601179001691900121641070004757200547003614
121  0907202601179001691900121641070004757190547003511
446  2008202601179001691900120021010005741120103012913
447  1708202601179001691900120721440001336360711003617
118  0907202601210000592100120015000000780641234567819
```

---

## 3. Dos facturas que NO hay que tocar, y por qué

| id | Número | Subtotal cab. | Σ detalles | IVA | CxP contabilizada | `importeTotal` |
|---:|---|---:|---:|---:|---:|---:|
| **166** | 072-128-000348916 | 13.50 | 13.51 | 1.76 | 15.27 | **15.27** ✅ |
| **138** | 072-112-000279800 | 32.57 | 32.58 | 3.84 | 36.42 | **36.42** ✅ |

Las dos aparecen en el diagnóstico porque **sí tienen los dos descuadres** —el detalle suma un
centavo más que el subtotal de cabecera, y la cabecera no cuadra consigo misma por un centavo— pero
**los dos errores se cancelan exactamente**: el centavo que sobra en el detalle es el mismo que falta
en la cabecera.

**Su cuenta por pagar ya es correcta.** Recargarlas no cambiaría un solo valor, y anularlas tiene
costo (§4). Se dejan como están.

> **Vale como advertencia de método:** el diagnóstico mide **dos** descuadres por separado, y la
> primera lectura invita a sumar «facturas del bloque 2 + facturas del bloque 3». **La que importa es
> ninguna de las dos: es el neto contra `importeTotal`.** Dos de siete casos son falsos positivos
> exactamente por eso.

---

## 4. ⚠️ Antes de anular: la anulación no es gratis

Anular una factura de compra **no** es solo marcarla. Desde el frente R
(`cxc/API-ANULACION-DOCUMENTOS.md`), `POST /fctc/anular/{id}`:

- Si la factura **tiene pagos, retenciones o anticipos cruzados**, responde **409 Conflict** y no
  hace nada. Sólo procede con `anularEnCascada: true`, que **reversa todos esos movimientos**.
- Y `fctc` es del lado compra: **«no existe» y «ya anulada» responden 200 con `exito:false`**. Mirar
  el status HTTP no alcanza; hay que leer el cuerpo.

**Consultá primero qué movimientos tiene cada una** con
`cxp/sql/lap1-05-movimientos-de-las-facturas-a-recargar.sql`. Si alguna ya está pagada, anularla
reversa el pago, y eso es una decisión distinta a corregir un centavo.

---

## 5. El orden

1. Desplegar el cambio (`DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md`). **No hay DDL**: la cuenta ya existe.
2. Correr `lap1-05` y revisar qué movimientos tienen las 5.
3. Anular y volver a cargar **sólo esas 5**.
4. Volver a correr `lap1-04`: el bloque 4 debería quedar con **2 filas**, las de §3, y el bloque 5
   seguir vacío.

---

## 6. Qué dicen los datos sobre la tolerancia elegida

El diseño frena el proceso si la diferencia supera **0,50**, por si el `importeTotal` trae ICE o
propina que no deben ir a una cuenta de redondeo.

Los datos la respaldan y de paso la calibran: **la diferencia máxima observada es de 1 centavo** en
todo el histórico, y el bloque 5 —diferencias mayores a 5 centavos— vino **vacío**. O sea que el
umbral no va a estorbar en la operación normal, y si algún día salta, va a ser por algo que
efectivamente no es redondeo.
