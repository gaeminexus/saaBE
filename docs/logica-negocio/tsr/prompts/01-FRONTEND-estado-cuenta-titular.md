> **CORRECCION 2026-08-27:** los valores de `estadosAnulados` de este prompt eran INCORRECTOS (decia `[2]`; en CXC/CXP anulado es `0` = `Estado.INACTIVO`). El prompt valido es `01b-FRONTEND-estado-cuenta-correccion-estados.md`.

# PROMPT 01 — AGENTE FRONTEND — Estado de cuenta de titular: corregir filtro de anulados y agregar retenciones recibidas

**Agente:** FRONTEND (`C:\work\saaFE\v1\saaFE`, Angular 20 standalone). **No tocar el backend.**
**Orden:** primero de la fase D. No depende de ningún despliegue.

## Contexto (ya verificado, no re-investigar)
La pantalla `src/app/modules/tsr/forms/estado-cuenta-titular/estado-cuenta-titular.component.ts` no muestra las facturas de clientes emitidas en CXC. La causa es el filtro:

```ts
// estado-cuenta-titular.component.ts ~línea 239
if (this.esAnulado(d)) return false;
// ~líneas 316-318
private esAnulado(d) { return d.estado != null && Number(d.estado) !== 1; }
```

Ese filtro asume "1 = activo, otro = anulado" para todos los documentos, pero cada fuente tiene su propio ciclo de estados:

| Fuente | URL | Campo `estado` | Vivos | Anulados |
|---|---|---|---|---|
| Factura CXC (`fctr`), NC (`ntcr`), ND (`ntdb`), Retención emitida (`rtv2`) | CXC | ciclo SRI: 0/1 ingresada, 3 firmada, 4 enviada, 5 autorizada, 6 devuelta | 0,1,3,4,5,6 | **2** |
| Factura compra (`fctc`), NC compra (`ntcc`), ND compra (`ntdc`), Retención recibida (`rcv2`) | CXP | 1 activo | 1 | **2** |
| Anticipo cliente (`antc`), Anticipo proveedor (`antp`) | | 1 ingresado, 2 confirmado, 3 anulado, 4 migrado | 1,2,4 | **3** |

Además, al rol CLIENTE le falta la fuente **retenciones recibidas** (`/rest/rcv2`): las retenciones que el cliente hizo a la empresa sobre sus facturas. Regla de negocio: estado de cuenta de cliente = facturas CXC emitidas al titular + NC/ND + anticipos de cliente + **retenciones recibidas (CXP, `rcv2`)**; estado de cuenta de proveedor = documentos CXP + retenciones emitidas (`rtv2`) + anticipos de proveedor.

## Cambios a realizar

### 1. `src/app/modules/tsr/service/estado-cuenta-titular.service.ts`
1. En la interfaz/tipo `FuenteDocumento` (líneas ~61-117, donde se declaran las fuentes por rol) agregar la propiedad `estadosAnulados: number[]`.
2. Asignar por fuente: CXC (`fctr`, `ntcr`, `ntdb`, `rtv2`) → `[2]`; CXP (`fctc`, `ntcc`, `ntdc`, `rcv2`) → `[2]`; anticipos (`antc`, `antp`) → `[3]`.
3. Agregar al rol CLIENTE la fuente de retenciones recibidas, simétrica a la de `rtv2` del proveedor (líneas ~103-110):
   ```ts
   { etiqueta: 'Retenciones recibidas', url: ServiciosCxp.RS_RCV2, campoTitular: 'proveedor',
     tipo: TipoDocumentoEstadoCuenta.RETENCION, origen: 'RECIBIDO',
     campoFecha: 'fecha', campoNumero: 'numero', campoTotal: 'total', estadosAnulados: [2] }
   ```
   Verificar en `src/app/modules/cxp/service/ws-cxp.ts` el nombre exacto de la constante del endpoint `rcv2` (si no existe, agregar `RS_RCV2 = \`${API_URL}/rcv2\``). Verificar en el modelo `RetencionCompraV2` del FE los nombres reales de `fecha`/`numero`/`total` y usar esos.
4. En `normalizar()` (donde se construye el documento unificado) calcular `anulado: fuente.estadosAnulados.includes(Number(d.estado))` y exponerlo en el modelo unificado (`src/app/modules/tsr/model/estado-cuenta-titular.ts`: agregar `anulado: boolean`).

### 2. `estado-cuenta-titular.component.ts`
1. Reemplazar `esAnulado(d)` para que devuelva `d.anulado === true`. Eliminar la comparación `!== 1`.
2. Revisar que el filtro por estado de saldo (PENDIENTE/PARCIAL, ~línea 301) no descarte documentos cuando `saldoPendiente` es `null` porque `aplc/saldo/{id}` o `aplp/saldo/{id}` falló: en ese caso mostrar el documento con saldo "—" en lugar de ocultarlo.
3. Las retenciones (emitidas y recibidas) no tienen saldo propio: no llamar a `aplc/saldo` ni `aplp/saldo` para ellas (ya debería ser así para `rtv2`; replicar para `rcv2`).

### 3. Verificación manual (describir en el informe final)
- Titular con rol CLIENTE que tenga facturas en `CBR.FCTR` con `ESTADO = 5`: deben aparecer.
- Titular PROVEEDOR: deben aparecer ahora también sus retenciones emitidas autorizadas (estado 5) y sus anticipos confirmados (estado 2).
- Una factura CXC con `ESTADO = 2` no debe aparecer.

## Restricciones
- No cambiar el backend ni los endpoints. No crear endpoints nuevos.
- No cambiar el formato de fechas: seguir usando `FuncionesDatosService.convertirFechaDesdeBackend()`.
- Entregar al final: lista de archivos modificados y un resumen de 5 líneas.
