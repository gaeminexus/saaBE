# Despliegue del EAR «dirección única del socio, ciudad por combo e Ingreso neto (rol)» (2026-11-16)

**omen-saa-1-arb.** Se armó sobre el EAR desplegado `excel-asientos-1114` (omen3, «todo main» al 2026-11-14). Cambian los
dos WAR; es **todo `main`** de los dos repos al 2026-11-16.

## 1. El EAR
- `F:\ears\asoprep\direccion-1116\SaaEAR.ear` — SHA-256 `7E9881F607B4F84320724CF1DA2E192167B914FC71FC852E0B7F38664A35F32A`
  (3 partes en `direccion-1116\partes`; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `14F4DDD921E6292FEF656A8F8F462082864E054B69E4FDB71C13D4690333A533` — saaBE `main` en `3c68ccbf`.
  - `SaaFE.war` `53F73DF0B4AC66B272F1ACA1C87E3F5190CC4AA653F93B6AF75D5C8BAE9AD365` — saaFE `main` en `251046b`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war de `excel-asientos-1114`).
- **Vuelta atrás:** `F:\ears\asoprep\excel-asientos-1114\SaaEAR.ear` (`A49B7F5D…1D1B60`). La columna del 379 puede
  quedarse (el EAR viejo no la lee).

## 2. ⛔ ANTES del EAR: `crd/sql/379` (YA CORRIDO el 2026-11-16)
`CRD.DRCC.CDDDCDGO` (ciudad). **Sin el 379 todo crédito da ORA-00904** (Dirección se lee en todas partes).

## 3. Qué trae, respecto de `excel-asientos-1114`
| Cambio | Commits |
|---|---|
| Una sola dirección de domicilio para la pantalla del socio, la solicitud, el pagaré y el contrato de adhesión (la que muestra la pantalla); al guardar queda una sola por defecto | BE `3c68ccbf`; FE `251046b` |
| Ciudad por combo, obligatoria al guardar la dirección; ciudad en la solicitud y el pagaré | BE `3c68ccbf`; FE `251046b` |
| Solicitud: dirección completa (calle, número, secundaria, barrio, descripción, ciudad) y «Ingreso neto (rol)» | BE `3c68ccbf` |
| C30 del cuadre (fusionado en main): en la novación, la garantía del préstamo anterior sale por el monto entregado original | BE `dbf7195e` / `6b80b53f` (otro equipo, sin SQL) |

## 4. Humo (solo lectura, salvo la ficha)
1. Hash, desplegar, «Last enabled at», log sin `ERROR`/`ORA-`.
2. Ficha del socio del reclamo → Dirección: ver la dirección de siempre; elegir la ciudad en el combo; guardar y reabrir
   (corrección de ficha real, no prueba contable).
3. Imprimir su solicitud de crédito: «Dirección domicilio» = la de la pantalla con la ciudad; «Ingreso neto (rol)».
4. Pagaré del mismo socio: dirección y ciudad.

## 5. Riesgos
- La consulta nueva de la solicitud y el pagaré no corrió contra Oracle antes del despliegue (sí el layout con datos de
  prueba): el punto 3 del humo es la prueba.
- Desde este EAR, **editar un socio con dirección obliga a elegir la ciudad** para poder guardar.
- Si el catálogo `CRD.CDDD` está incompleto, el combo no trae la ciudad: avisar y se carga.
