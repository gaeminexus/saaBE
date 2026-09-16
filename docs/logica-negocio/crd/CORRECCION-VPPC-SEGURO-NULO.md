# Jubilar un partícipe revienta con ORA-01400 cuando el seguro va vacío

**Fecha:** 2026-09-16 · **Equipo:** `omen-saa-1` (crd) · **Reportado por el usuario, urgente**
**Estado:** diseño congelado, sin implementar

---

## 1. El error, tal como llega

```
LLEGA AL SERVICIO POST - VALOR_PAGO_PENSION_COMPLEMENTARIA
saveSingle - ValorPagoPensionComplementaria
ORA-01400: no se puede realizar una inserción NULL en ("CRD"."VPPC"."VPPCVLSR")
insert into CRD.VPPC (ENTDCDGO,VPPCIDST,VPPCFCRG,VPPCCFMD,VPPCNMCT,VPPCTNPR,VPPCUSRG,VPPCUSMD,VPPCVLRR,VPPCVLSR,VPPCCDGO) …
```

`VPPCVLSR` es el **valor del seguro médico** del jubilado (`ValorPagoPensionComplementaria.valorSeguro`).

## 2. La causa, verificada en el código

`jubilar-participe.component.ts:907-922` manda `valorSeguro: null` cuando el campo del formulario está
vacío:

```ts
const valorSeguroTexto = this.valorSeguroPensionTexto.trim();
const valorSeguro = valorSeguroTexto ? this.parseMoneda(valorSeguroTexto) : null;
```

y `ValorPagoPensionComplementariaServiceImpl.saveSingle:57-67` graba lo que llega, sin normalizar.
La columna en Oracle es **NOT NULL**, así que el `INSERT` muere y la transacción se revierte entera:
**el jubilado no queda registrado.**

**Qué significa un seguro vacío:** «este jubilado no paga seguro médico». En el resto del sistema eso
se representa con **0**, no con nulo — `PagoPensionComplementariaServiceImpl` lee
`vppc.getValorSeguro() != null ? vppc.getValorSeguro() : 0.0` y el proceso de seguro fija 0 cuando no
corresponde cobrarlo (§11.1 de `API-DOS-PROCESOS-MENSUALES-JUBILADOS.md`).

⚠️ **Rodeo disponible mientras no se despliegue:** escribir **0** en el campo del seguro. Con 0 el
`INSERT` pasa y el resultado es idéntico al que va a producir la corrección.

## 3. La corrección

### 3.1 Backend (`crd`) — es la que cierra el agujero

`ValorPagoPensionComplementariaServiceImpl.saveSingle`: antes de guardar, **`valorSeguro` nulo pasa a
`0.0`**, con un comentario que diga que la columna es `NOT NULL` y que «sin seguro» se representa con
cero. Vale para el alta y para la edición.

**Por qué en el service y no sólo en la pantalla:** el endpoint `POST /rest/vppc` es CRUD genérico y
lo puede llamar cualquier cliente; el frontend es sólo uno de ellos. Ese es el mismo criterio con el
que se cerró `/rest/aprt` (H61).

**Lo que NO se hace:** no se relaja la columna a nullable. Un jubilado sin valor de seguro definido y
un jubilado con seguro 0 son lo mismo para el negocio, y dejar entrar nulos obliga a tratar los dos
casos en cada lectura.

### 3.2 Frontend (`crd`) — que no dependa del backend para lo obvio

`jubilar-participe.component.ts`: el campo vacío viaja como **0**, no como `null`. Y el campo arranca
en `'$0.00'` como el de la pensión, para que se vea que el valor por defecto es cero y no «sin dato».

## 4. Qué revisar además, y reportar sin tocar

- Si `VPPCNMCT` (número de cuotas) o `VPPCTNPR` (tiene préstamo) también son `NOT NULL`, el mismo
  formulario los puede dejar en nulo: `numeroCuotas` viaja `null` cuando el campo está vacío. En el
  error de hoy no falló, pero eso sólo prueba que esa fila traía valor.
- Cualquier otro llamador de `/rest/vppc` en el frontend (hay otro en
  `proceso-pago-jubilados.component.ts`).
