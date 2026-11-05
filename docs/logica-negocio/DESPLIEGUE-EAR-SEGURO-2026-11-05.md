# Despliegue del EAR «seguro médico + usuario» (2026-11-05)

**omen-saa-1-arb.** Se armó sobre el EAR C23 desplegado hoy (`c23-1105`); cambian los dos WAR.

## 1. El EAR
- `F:\ears\asoprep\seguro-1105\SaaEAR.ear` — SHA-256 `89A482D83150887C1DDBC3506AEBC8C56C926422C3BD883D5D20812B84C4F1C3`
  (copia partida en `seguro-1105\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `FE6BA676421732FDF1DD373119D3AE23AC89E7A9316678317704AF399B88182E` — saaBE `main` en `ddf1f51b`.
  - `SaaFE.war` `2DDBB6EA5D550C6340E5395789F86764E7A398A2E86352F9DEDDF8E00B779FA7` — saaFE `main` en `c1155b6`
    (`ng build --configuration production`; `WEB-INF/web.xml` y `MANIFEST.MF` del SaaFE.war desplegado).
- **Vuelta atrás:** `F:\ears\asoprep\c23-1105\SaaEAR.ear` (`C5598304…BECD4FD`).

## 2. Qué trae
- **BE `ddf1f51b`:** el seguro médico de jubilados se debita del aporte 23 al GENERAR el seguro
  (`crd/DISENO-SEGURO-MEDICO-DEBITO-AL-GENERAR.md`). Noviembre 2026 (ya generado) se debita en las pensiones de
  fin de mes, como antes. Sin SQL previo.
- **FE `c1155b6`:** las pantallas de partícipe (cambio de estado, cónyuge, dirección, referencias, perfil
  económico, cuenta bancaria), generación Petro, jubilados y cierre de período mandan siempre el usuario de la
  sesión (el cambio de estado daba 400 «falta el usuario»).

## 3. Despliegue
1. Sin cargas Petro/ARCH ni procesos de jubilados en curso.
2. Desplegar `seguro-1105\SaaEAR.ear`. Las propiedades de sistema no cambian (`saa.cartera.alCorte=nunca`,
   `saa.cartera.neteoPorPrestamo=nunca`).
3. Verificar: `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `89A482D8…84C4F1C3`; «Last enabled at» con la
   hora del despliegue; log sin `ERROR`/`ORA-` desde el despliegue.
4. Humo (solo lectura o acciones reales del día): cambiar el estado de un partícipe desde Consulta de entidades.
