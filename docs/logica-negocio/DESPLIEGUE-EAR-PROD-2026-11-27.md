# Despliegue del EAR «prod-1127» en producción

**omen-saa-1-arb, 2026-11-27.** El usuario pidió subir «todos los últimos cambios». Respuesta del árbitro del cuadre
del 27-11 (`RESPUESTA-ARBITRO-EAR-27112026`): la C19b **no está probada** y el 8080 no la tiene, así que el EAR sale
**sin la C19b** (D-187). Los lotes VCN tampoco se validaron y tampoco van.

## 1. El EAR
- `F:\ears\asoprep\prod-1127\SaaEAR.ear`, SHA-256
  `C97C3671BE888E5FF9AD9B26EBCE3584F756549F585838C4259D23E6EB6FA151` (62.577.423 bytes).
  - 3 partes en `partes\`. Se unen con `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`.
  - `unzip -t` sin errores.
- `saaBE.war`: `440FE87F0F9AB3F775341ECA427265AFFA916AD72776FD1E9CB850FE349BC8EE`.
  - Es la rama **`ear/prod-1127`** (en GitHub), punta `b344bb02`.
  - Se armó con cherry-pick sobre `5a40b3ac`, con `mvn -o clean package`.
  - Las pruebas C46 (30/0) y C33 (109/0) salen en verde.
- `SaaFE.war`: el mismo de producción (`69B9403C…84276410`, saaFE `63a225d`). Ningún cambio de este EAR toca el FE.
- **Vuelta atrás:** `F:\ears\asoprep\cierre-1125-d187-l10-l15\SaaEAR.ear` (`C43A4E1A…180B08E0`). Los DDL 01-04 son
  ampliaciones: el EAR anterior funciona con ellos.

## 2. Código: igual al 8080 del re-proceso, más tres cambios sin validar

| Commit en `ear/prod-1127` | Equivalente en main | Qué es | ¿Validado en el re-proceso? |
|---|---|---|---|
| sobre `5a40b3ac` | — | base de producción | sí |
| lotes 7, 7b, 10, 15 y C33-15 | `37c540f3`, `c728ba3a`, `ee647164`, `41cf67e9`, `e0113ff4` | ya en producción, menos C33-15 | sí |
| lotes 16 a 22 | `b19f5e1f` … `1d70a259` | libro de devengo por cuota, provisión, control 1.4.02 = 1.4.99.05 | sí (dic-2025 a may-2026) |
| lote 23 | `ee0c441a` | el reparto del tope de mora coloca todo el tope | **no** (el 8080 no lo tiene) |
| lote 24 | `3b5ea610` | control por cuenta; semilla en modo DIFERENCIA | sí |
| G49 D-207 | `c1382981` | la G49 no cancela dos veces | sí |
| C46 | `d4894513` | segunda línea sin rol del retorno Petro y alerta de cuadre | **no**; verificado contra los 14 retornos reales |
| C47 | `aa8a2894` | Resumen Financiero: 1.4.02 = interés + mora; aportes fuera de la 2.1 | **no** |

- Los archivos de `src` que tocan estos commits son iguales a `main`, salvo 7.
- Esos 7 difieren por `5b024151` (C33: pago registrado antes del corte), que tampoco está en el 8080.
- **No van:** la C19b (`42050351`), los lotes VCN (3–9, 11, 12, 13, 14 y 14b), `5b024151` y el frente de la app móvil.

## 3. ⛔ Scripts ANTES del EAR

Carpeta: `F:\ears\asoprep\prod-1127\scripts\` (`LEEME.txt` con el orden).

| Paso | Script | Sin él |
|---|---|---|
| 00 | `00_VERIFICAR_ANTES_solo_lectura.sql` | Dice qué falta. Confirma que el 397 ya corrió (tabla `CRD.BK_PGPR_PRSTCDGO_20261125`) |
| 01 | `crd/sql/406` (`CK_MVIC_TIPO` 1..12) | ORA-02290 en el ④ del cierre y en la precancelación, el acuerdo y el abono |
| 02 | `crd/sql/409` (`CK_MVIC_TIPO` 1..14) | Falla `/prov/ajusteLibroProvision` |
| 03 | `crd/sql/410` (`CK_ANCC_TPOO` + 13) | Falla el ajuste de último recurso si se enciende |
| 04 | `crd/sql/411` (`CRD.CRAR.CRARALCD`) | ORA-00904 en toda carga Petro/ARCH |

Los cuatro son DDL, con commit implícito. No van los `rpr/sql/C19-*` ni los scripts de los lotes VCN
(`cxc/sql/omen1-02`, `rhh/sql/omen1-07`).

## 4. Propiedades
- **No definir** `saa.cartera.ajusteUltimoRecurso` ni `saa.cartera.bloqueoCierreProvision`. Son nuevas y quedan
  apagadas.
- No encender el último recurso hasta que el control del lote 24 dé 0 en producción.
- Las demás quedan como están hoy en producción (`DESPLIEGUE-EAR-CIERRE-2026-11-25.md` §4).

## 5. Después de desplegar
1. Revisar el log del arranque: sin `ERROR`, `ORA-` ni inyecciones circulares.
2. Humo de solo lectura:
   - abrir una carga Petro existente (lee `CRARALCD` nulo);
   - `GET /ejcc/resumenFinanciero/{codigo}` de una ejecución existente;
   - previsualizar el cierre de noviembre.
3. **El libro de devengo por cuota arranca vacío en producción.** El control 1.4.02 = 1.4.99.05 del lote 22 va a
   mostrar la diferencia hasta que se siembre con la semilla A2 (`/prov/semillaDevengoInteres`) y el ajuste A1
   (`/prov/ajusteLibroProvision`).
   - Esos dos se corren con `grabar=false` primero.
   - La fecha y el asiento de referencia los define el cuadre, como en el re-proceso al 30-nov.
   - El control solo avisa: `bloqueoCierreProvision` está apagada.
4. Carga Petro de noviembre:
   - con este EAR, la segunda línea sin rol entra sola;
   - si `CRARALCD` trae una alerta, se revisa antes de procesar.
   - **No reprocesar con este EAR** una carga de abril a julio de 2026 que ya se completó a mano.
5. Después del primer cierre con este EAR, correr el `90_DESPUES_407` (solo lectura).

## 6. Riesgos
- El lote 23, la C46 y la C47 entran sin la prueba del re-proceso.
  - El lote 23 cambia el ⑦ (reparto de la mora).
  - La C46 cambia la carga Petro.
  - La C47 solo cambia un informe.
- Los lotes 16 a 24 corren por primera vez contra la base de producción. Con `devengoCompletoDesde = 2026-11-30` tocan
  las operaciones desde el 30-11 y el cierre de noviembre.
