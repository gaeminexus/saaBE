# Despliegue del EAR «niveles de aprobación para todos» (2026-11-08)

**omen-saa-1-arb.** Se armó sobre `otorga-1107` (el desplegado). Cambia solo el SaaFE.war.

> **⛔ NO SE DESPLIEGA (decisión del usuario, 2026-11-08).** Su cambio (saaFE `8951965`) va en el EAR único siguiente,
> junto con la primera cuota según el archivo Petro, la autorización sin el punto 4 y el PDF del simulador de novación.

## 1. El EAR
- `F:\ears\asoprep\niveles-1108\SaaEAR.ear` — SHA-256 `5CCEA462BEB7CBF9CE9035DE051EA88EE4F8E39D03CFE4822DE57EAF5B9AF1BF`
  (partido en `niveles-1108\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `B8C7B7FFA91CD5FEEB1049A065274AF9EB027AC4B1E3A09C5A7791B5B066330C` — **el mismo de `otorga-1107`**
    (saaBE sin cambios de `src`/`pom.xml` desde `ba069787`).
  - `SaaFE.war` `EA358F5CE6623B38B6A9BCF40B44D94C68D0A768ACBFEADD51250980379B3DC2` — saaFE `main` en `8951965`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war anterior).
- **Vuelta atrás:** `F:\ears\asoprep\otorga-1107\SaaEAR.ear` (`D71A05D5…8C787FC1`).

## 2. Qué trae
- Menú Créditos → Parametrización → **Niveles de Aprobación** visible para todo usuario con el permiso de
  Parametrización de crédito (antes solo el usuario 1). Las otras pantallas de configuración (bandas, calificación
  de riesgo, parámetros de CRD, cuentas por aporte) siguen solo para el usuario 1, por decisión del usuario.
- Sin scripts SQL, sin cambios de backend.

## 3. Despliegue
1. Desplegar `niveles-1108\SaaEAR.ear`.
2. Verificar hash (`Get-FileHash … -Algorithm SHA256` = `5CCEA462…5B9AF1BF`), «Last enabled at» y log sin `ERROR`.
3. Humo (solo lectura): con un usuario de crédito distinto del 1 que tenga Parametrización, entrar a
   Créditos → Parametrización → Niveles de Aprobación → la pantalla abre y lista las reglas.
