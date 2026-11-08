# Despliegue del EAR «otorgamiento: solicitud V1, autorización, reglas y niveles» (2026-11-07)

**omen-saa-1-arb.** Se armó sobre el EAR desplegado el 2026-11-05 (`seguro-1105`). Cambian los dos WAR.
Diseño: `crd/DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md`. Contrato: `crd/API-CICLO-OTORGAMIENTO.md` §7, §9–§12.

## 1. El EAR
- `F:\ears\asoprep\otorga-1107\SaaEAR.ear` — SHA-256 `D71A05D51D370A0AFF7402E6F576FC0E8978EEB80C7BECAD179ED8171C787FC1`
  (copia partida en `otorga-1107\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `B8C7B7FFA91CD5FEEB1049A065274AF9EB027AC4B1E3A09C5A7791B5B066330C` — saaBE `main` en `ba069787`.
  - `SaaFE.war` `4DC798B5E300726F3F543CA6AEDB3E93A3468DAEAA400416D349A4D217E055F8` — saaFE `main` en `d096154`
    (`ng build --configuration production`; `WEB-INF/web.xml` y `MANIFEST.MF` del SaaFE.war desplegado).
- **Vuelta atrás:** `F:\ears\asoprep\seguro-1105\SaaEAR.ear` (`89A482D8…84C4F1C3`). Las tablas del 369/370 pueden
  quedarse: el EAR anterior no las lee.

## 2. ⛔ ANTES del EAR: scripts 369 y 370
- `crd/sql/369_DDL_GRPR_GARANTE_PRESTAMO.sql` y `crd/sql/370_DDL_NIVELES_APROBACION_PRESTAMO.sql`, bloque por bloque.
- **Sin ellos el EAR rompe la impresión de TODOS los préstamos**: el pagaré lee `CRD.GRPR` y la solicitud lee
  `CRD.APPR/RGAP/NVAP` (ORA-00942).
- Control del 370 bloque 5.3: `REGLAS_ACTIVAS = 0` → los préstamos se siguen aprobando en un paso hasta que crédito
  configure niveles.

## 3. Qué trae
- **Solicitud de crédito V1** (`RPRT_PRST_SLCT`): empleador = filial; frecuencia MENSUAL; sección 3 calculada
  (ingreso = remuneración unificada, capacidad 50 %, monto máximo = cuenta individual, saldo y cuotas vigentes, cuota
  del préstamo novado, valor líquido); firma en página propia; plazo de desembolso; fechas y nivel de la resolución.
- **Autorización de descuento por rol** (`RPRT_PRST_ADRL`, FOR-CRE-AUT-01), nueva. El lote al aprobar trae cuatro
  documentos: solicitud → autorización → pagaré → tabla.
- **Reglas:** tasa 9 % (solo lectura; otra tasa → error al guardar); cupo (capital pendiente + monto ≤ cuenta
  individual) bloquea al guardar y al aprobar; capacidad (cuota nueva + vigentes ≤ 50 % de la remuneración) y
  remuneración registrada bloquean al aprobar. No aplica a reestructuraciones en mora, carga Petro ni migración.
- **Garante** opcional en el préstamo; firma el pagaré en un segundo bloque.
- **Simulador de novación** (quirografarios y emergentes) en Simulador de crédito.
- **Niveles de aprobación configurables** (menú Créditos → Niveles de Aprobación; por ahora la pantalla solo la ve el
  usuario 1, con el permiso de parametrización de crédito).

## 4. Despliegue
1. Correr 369 y 370 (§2) y verificar sus controles.
2. Desplegar `otorga-1107\SaaEAR.ear`. Propiedades de sistema sin cambios.
3. Verificar: `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `D71A05D5…8C787FC1`; «Last enabled at» con la hora
   del despliegue; log sin `ERROR`/`ORA-` desde el despliegue.
4. Humo (solo lectura, sin grabar nada de prueba):
   - imprimir la **solicitud** y el **pagaré** de un préstamo ya existente (aprobado) → salen sin ORA-00942;
   - abrir un préstamo existente → paneles «Evaluación del otorgamiento» y «Aprobaciones» («Aprobación en un paso»);
   - Créditos → Niveles de Aprobación → lista vacía; «Probar» con cualquier tipo/monto/plazo → «sin reglas activas»;
   - Simulador de crédito → Novación con un partícipe que tenga un quirografario vigente → simula (no graba).
5. El primer préstamo real del día valida el resto (tasa 9, evaluación, lote de cuatro documentos).

## 5. No verificado antes del despliegue
Nada de esto corrió contra Oracle de producción ni en WildFly: solo compila y los reportes se llenaron contra la copia
de desarrollo (sin el esquema nuevo, con vistas simuladas). Riesgos a mirar en el humo: la solicitud de una novación
con `PRSTPROR/PRSTVLSS` reales; la aprobación por niveles y su transacción única (cuando crédito cargue reglas); los
índices únicos del 369/370.
