# PROMPT 02b — AGENTE BACKEND — Correcciones a la implementación de cheques

**Agente:** BACKEND (`C:\work\saaBE\v1\saaBE`). **No tocar el frontend.**
**Contexto:** tu implementación del prompt 02 está completa y compila (verificado en Eclipse, sin errores). Una revisión posterior encontró **dos defectos bloqueantes** y varios importantes, todos **verificados leyendo el código**, no sospechas. Este prompt los corrige. No rehagas nada más.

**Tu duda 1 quedó respondida:** los cuatro caminos contables generan **una sola línea HABER** (`generarAsientoPagoTransferenciaCxp:860-864`, `generarAsientoEgresoTesoreria:927-931`, `generarAsientoAnticipoProveedor:722-742`, y el asiento de origen externo `PagoProgramadoServiceImpl:1682-1709` que hace N DEBE + 1 HABER). `anexaNotaChequeEnHaber` no duplica hoy — pero acótalo igual (tarea C4).
**Tu duda 2 quedó respondida:** `TSR.DTCH.PRSNCDGO` es **nullable** en local y en producción (verificado con `ALL_TAB_COLUMNS`: la única NOT NULL de la tabla es la PK). Un cheque de origen externo sin titular no falla. No hay nada que hacer.

---

## A. BLOQUEANTES (sin esto, las dos primeras acciones del usuario fallan)

### A1. `crearChequesDeChequera` hace `merge` sobre una fila inexistente y reutiliza la instancia
`ChequeServiceImpl:169-188` (método legado que ahora invocas desde `ChequeraServiceImpl:169`).

El problema, verificado:
```java
Cheque cheque = new Cheque();          // <-- UNA sola instancia para todo el bucle
for (...) {
    cheque.setCodigo(Long.valueOf(0)); // <-- id 0
    ...
    chequeDaoService.save(cheque, cheque.getCodigo());   // id != null  ->  em.merge()
```
`EntityDaoImpl.save` (l.303-311) sólo hace `persist` cuando el id es **null**; con `0L` ejecuta `em.merge` sobre `Cheque#0`, que no existe. Y como la instancia se reutiliza, cada vuelta pisa la anterior. El repo ya documenta esta trampa en `MovimientoBancoServiceImpl:253-254`: *"El codigo lo genera la secuencia... Si se asigna 0L el DAO ejecuta merge sobre una fila inexistente"*.

**Corrección** — instancia nueva por vuelta y `persist`:
```java
for (int j = 0; j < totalCheques.intValue(); j++) {
    try {
        Cheque cheque = new Cheque();                 // dentro del bucle
        cheque.setChequera(chequera);
        cheque.setNumero(Long.valueOf(numeroCheque));
        cheque.setRubroEstadoChequeP(Long.valueOf(Rubros.ESTADO_CHEQUE));
        cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ACTIVO));
        cheque.setRubroMotivoAnulacionP(Long.valueOf(Rubros.MOTIVO_ANULACION_CHEQUE));
        chequeDaoService.save(cheque, null);          // null -> persist
        numeroCheque++;
    } catch (PersistenceException e) { ... }
}
```
No cambies la firma del método: sigue siendo el mismo contrato.

### A2. `selectListado` devuelve cero filas por joins implícitos INNER
`ChequeDaoServiceImpl:129-140`. El JPQL hace `left join PagoProgramado p on p.cheque = c` y luego navega `p.facturaCompra.numero`, `p.egreso.descripcion`, `p.anticipo.numeroDoc`. Hibernate renderiza esos joins implícitos como **INNER JOIN**, y los tres son mutuamente excluyentes en un pago (sólo uno está lleno), así que el `where` nunca se cumple: **el listado devuelve vacío siempre**.

Nota: `p.facturaCompra.id`, `p.egreso.id`, `p.anticipo.id` y `p.empresa.codigo` **sí** son seguros (Hibernate los resuelve contra la columna FK sin join); el problema son sólo los campos que no son la PK.

**Corrección** — joins explícitos:
```
 from Cheque c
 join c.chequera ch
 join ch.cuentaBancaria cb
 left join cb.banco bnc
 left join PagoProgramado p on p.cheque = c
 left join p.facturaCompra fc
 left join p.egreso eg
 left join p.anticipo an
```
y en el `select` usar `fc.id, fc.numero, eg.id, eg.descripcion, an.id, an.numeroDoc`. Ajusta los índices del `Object[]` en `ChequeServiceImpl.listar` si cambian de posición.

---

## B. IMPORTANTES

### B1. `<> :anulada` esconde las chequeras con estado NULL
`ChequeraDaoServiceImpl:52-60` (`selectMaxFinalizaByCuenta`) y `:63-75` (`existeSolape`): `c.rubroEstadoChequeraH <> :anulada` evalúa a NULL cuando el campo es NULL, así que esas chequeras **no cuentan ni para el solape ni para sugerir el número inicial** — se podría registrar un rango solapado sobre una chequera legada. Es el mismo patrón que ya se corrigió en `DetallePrestamoDaoServiceImpl`.

**Corrección** en ambas consultas:
```
 and (c.rubroEstadoChequeraH IS NULL OR c.rubroEstadoChequeraH <> :anulada)
```

### B2. Condición de carrera al tomar el siguiente cheque
`ChequeDaoServiceImpl.selectMinChequeActivoPorCuenta` (l.~100-114) es un SELECT sin lock: dos registros simultáneos desde la misma cuenta toman el mismo cheque y ambos lo pasan a GENERADO.

**Corrección:** `query.setLockMode(LockModeType.PESSIMISTIC_WRITE)` en esa consulta (import `jakarta.persistence.LockModeType`). El usuario además creará el índice único `UQ_PGTR_DTCH` sobre `PGS.PGTR(PGTRDTCH)` como red de seguridad; **captura la violación de constraint** al grabar el pago y tradúcela a `IncomeException("El cheque N° X fue tomado por otro usuario, intente nuevamente")`.

### B3. `debitoAutomatico=true` + `formaPago=2` no debe lanzar excepción
`PagoProgramadoServiceImpl:1441-1447`. Hoy lanza *"Un pago por débito automático debe registrarse con forma de pago Débito automático"*. Un formulario que mande el combo de forma de pago con default 2 junto al check de débito automático rompería un flujo que hoy funciona.

**Decisión: normalizar en vez de lanzar.** En `validarFormaPago`:
- si `debitoAutomatico == true` → forzar `formaPago = 4` (sin excepción, dejar traza);
- si `formaPago == 4` y `debitoAutomatico == false` → forzar `debitoAutomatico = true`;
- `formaPago = 3` sigue exigiendo `manejaChequera = 1` y `debitoAutomatico = false` (si viene true con 3, ahí sí lanzar: son incompatibles de verdad);
- `formaPago = 1` (efectivo) se sigue rechazando con mensaje claro *"La forma de pago Efectivo aún no está soportada"*.
Que el método devuelva la forma de pago ya normalizada y el llamador use ese valor.

### B4. El anticipo pagado con cheque queda marcado como transferencia
`AnticipoProveedorServiceImpl:284-290` tiene hardcodeado `anticipo.setFormaPago(2L)`. Con cheque, `PGS.ANTP.ANTPFPAG` miente.

**Corrección:** propagar la forma real — `anticipo.setFormaPago(formaPago != null ? formaPago : 2L)` — y cuando sea cheque, poner en `anticipo.setReferencia(...)` el `"CHQ-" + numero` en lugar del número de cuenta, y en `setBanco(...)` mantener banco + cuenta.

### B5. La línea HABER sigue diciendo "Transferencia" en un pago con cheque
`AsientoContableServiceImpl:862-864` escribe `"Transferencia a proveedor: ..."`. El prompt 02 (T4.4a) pedía que el texto pasara a "cheque". Tu post-proceso `anexaNotaChequeEnHaber` agrega la nota pero deja la palabra equivocada.

**Corrección:** en `anexaNotaChequeEnHaber`, además de anexar `" | Cheque N° X"`, reemplazar el prefijo `"Transferencia a proveedor:"` por `"Cheque a proveedor:"` cuando exista cheque. Y en `AplicacionPagoCxpServiceImpl:610` la observación queda `"Pago por cheque | Ref: "` con `Ref` vacío: usar la referencia `CHQ-n` que ya se graba en el pago.

---

## C. MENORES (aplícalos, son de una línea)

1. **C1** — `ChequeServiceImpl.anularChequeSuelto` (l.~449-471): validar que `motivo` sea un valor de `MotivoAnulacionCheque` (1..3); hoy `ChequeRest.toLong` devuelve `null` ante basura y se graba tal cual en `DTCHRZZB`. Rechazar con `IncomeException`.
2. **C2** — `ChequeraServiceImpl:180, 229, 263`: `chequeraDaoService.selectById` usa `getSingleResult()`, o sea lanza `NoResultException` y **nunca** devuelve null; los `if (chequera == null)` son código muerto y el usuario ve *"No entity found for query"*. Envolver en try/catch y lanzar el mensaje redactado.
3. **C3** — `PagoProgramadoServiceImpl:1238-1241`: mover el guard de cheque de `anularPago` **después** del check de estado ANULADO, para que un pago ya reversado responda "ya está anulado".
4. **C4** — `anexaNotaChequeEnHaber` (l.~1502-1519): acotar a la línea del banco (`and d.planCuenta.codigo = :idCuentaBanco`, la cuenta contable de la cuenta bancaria del pago) en vez de recorrer todas las líneas con `valorHaber > 0`. Hoy no duplica, pero un asiento con dos HABER lo haría.
5. **C5** — `ChequeRest:160-164`: `siguiente` devuelve 404 ante cualquier `Throwable`. Devolver 404 sólo cuando no hay cheque disponible (`IncomeException` propia) y 500 en el resto, para no disfrazar una caída de BD de "no hay cheques".
6. **C6** — `ChequeDaoServiceImpl:118-127`: el filtro `desde`/`hasta` va contra `c.fechaUso`, que es null en los cheques ACTIVO, así que al filtrar por fechas desaparecen los disponibles. Es aceptable, pero **documéntalo en `CHEQUES.md`** para que el frontend no ponga un rango por defecto.
7. **C7** — `ChequeServiceImpl.asignarAPago` (l.~433-447): llenar también `idBeneficiario` (DTCHIDBN) con el titular cuando exista, además de `titular` (PRSNCDGO), porque las pantallas legadas leen esa columna.
8. **C8** — `ChequeraServiceImpl.registrarRecepcion` (l.~129): recibe `idUsuario` y lo descarta (`Chequera` no tiene campo de usuario). Deja un comentario diciéndolo, o quita el parámetro de la firma y del REST.

---

## D. Documentación
Actualizar `docs/logica-negocio/tsr/CHEQUES.md` con: la normalización de `formaPago` de B3, la nota de C6 (filtro de fechas vs. cheques disponibles) y el comportamiento ante carrera de B2.

## Restricciones
- No compilar con mvn; el usuario valida en Eclipse.
- No cambiar nada del comportamiento de transferencia y débito automático más allá de B3.
- Entregar: lista de archivos modificados, y confirmación explícita punto por punto (A1, A2, B1..B5, C1..C8) de qué se aplicó y qué no, con la razón.
