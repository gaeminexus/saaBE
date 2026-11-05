# Despliegue del EAR C23 (2026-11-05)

**omen-saa-1-arb.** Pedido por el cuadre (C23 / D-052, aprobado por el usuario) tras el cierre de octubre que revivió
préstamos cancelados (`crd/DISENO-CANCELADOS-EN-CARTERA-AL-CORTE.md`, diagnóstico `crd/sql/364`).

## 1. El EAR
- `F:\ears\asoprep\c23-1105\SaaEAR.ear` — SHA-256 `C5598304412D0F7C597E5FB7DA125D36261D5E10FC3BA1C7D8460C345BECD4FD`
  (partes de 25M en `c23-1105\partes`).
  - `saaBE.war` `F2A3F5DD80D0C2683871CD9BF6EA9CEBCD4C05D209049D122BEFEE82EC2E4FEA` — saaBE `main` en `c6ce0930`;
    único cambio de código desde el EAR único: `a560cdfe` (C23).
  - `SaaFE.war` `3A7E0665…7CE8DBF` — sin cambio (el del EAR único).
  - Armado sobre `unico-1101\SaaEAR.ear` (hashes de los dos WAR verificados al extraer); solo cambia `saaBE.war`.
- Sin DDL ni semillas.
- **Vuelta atrás:** `F:\ears\asoprep\unico-1101\SaaEAR.ear` (`B16F4A08…ABB86A187`).

## 2. Propiedades (antes de desplegar)
```
/system-property=saa.cartera.alCorte:read-resource
/system-property=saa.cartera.neteoPorPrestamo:read-resource
/system-property=saa.cartera.alCorte:write-attribute(name=value,value=nunca)
/system-property=saa.cartera.neteoPorPrestamo:add(value=nunca)     # o write-attribute si ya existe
```
Quedan en `nunca` hasta la base re-procesada (`auto` vuelve al corte si el cierre se corre pasado el mes
siguiente). Con `nunca` el cierre mensual no corre ⑧, ⑨ ni ⑩ y el código C23 no actúa en producción.

## 3. Despliegue
1. Sin carga Petro/ARCH en proceso, fuera de horario de pagos.
2. Desplegar `c23-1105\SaaEAR.ear`.
3. `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `C5598304…BECD4FD`.
4. Log sin `ERROR`/`ORA-`/`ClassCastException` desde el arranque.

## 4. Cierre de octubre 2026 (después del despliegue)
1. Confirmar que la corrida de octubre quedó reversada o la cartera reabierta, no solo los asientos 12343, 12344,
   12345 y 12352 anulados. Si falta, reabrir con motivo desde la pantalla.
2. Confirmar que no quedó activo ningún asiento de provisión ⑧ ni devengo ⑩ de octubre (`DI-A-2026-10-0036…0050`).
3. Previsualizar: la primera línea **no** dice «Cierre A FECHA DE CORTE» y no aparecen el neteo por préstamo
   (D-032), ni ⑧, ni ⑨. Si aparece cualquiera: parar.
4. Revisar con contabilidad y ejecutar. Recién entonces mayorizar, G de octubre y estados.

## 5. Al cuadre
Hash desplegado, salida de las dos propiedades, avisos de la previsualización y número de la corrida de octubre.

## 6. Resultado
- **Propiedades (2026-11-05, consola web, Configuration → System Properties):** `saa.cartera.alCorte` = `nunca`
  (antes `siempre`); `saa.cartera.neteoPorPrestamo` = `nunca` (no existía, agregada). `saa.cartera.inicioDevengoMora`
  sin cambio (`2025-05-31`). Rigen sin reiniciar (`System.getProperty` en cada corrida).
- Despliegue del EAR: _(pendiente)_
