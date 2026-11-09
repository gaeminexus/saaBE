# DISEÑO — Preview del listado de desgravamen en una sola consulta

omen-saa-1-arb · 2026-11-08 · Pedido del usuario: «al tratar de ver la vista previa de los préstamos a incluir en el
seguro de desgravamen … el sistema se demora muchísimo». Ejecutor: omen-saa-1-be.

## Hoy (verificado, `DocumentoSeguroServiceImpl.calcularCandidatos` ~191-278)
1. `prestamoDaoService.selectByEstados([2, 11])` → entidades completas con sus @ManyToOne EAGER (Entidad, Producto,
   Filial, MotivoPrestamo; Entidad arrastra 5 catálogos) → N+1 por partícipe.
2. Desgravamen no filtra (incendio/prendario sí filtran por tipo → por eso solo desgravamen es lento).
3. `detallePrestamoDaoService.selectByPrestamos(ids)` → TODAS las cuotas (también pagadas) como entidades, bloques de 900.
4. `pagoPrestamoDaoService.selectDatosPagosVigentes(idsCuotas)` → todos los pagos, bloques de 900 cuotas (solo desgravamen).
5. Suma en Java; al cerrar la transacción Hibernate hace dirty-checking de cientos de miles de entidades.
`previewListado`, `previewListadoExcel` y `generarListado` usan el mismo `calcularCandidatos`.

## Nuevo
- Para **desgravamen**, reemplazar 1-5 por **una consulta agregada** (nativa o JPQL escalar) que devuelva por préstamo:
  código, idAsoprep, razón social y cédula del partícipe, nombre del tipo de préstamo, estado, y
  `saldoCapital = Σ por cuota no PAGADA(4)/no CANCELADA_ANTICIPADA(7) de GREATEST(0, ROUND(DTPRCPTL − Σ PGPRCPPG de
  pagos no anulados, 2))`, con la condición de plazo vigente (existe cuota no 4/7 con vencimiento > fechaCorte,
  mismo criterio «>» de S15). Estados 2 y 11 (no 8), igual que hoy.
- Resultado **idéntico** al de hoy (mismo redondeo por cuota: `redondear(nvl(capital) − capitalPagado)` y máx 0, base
  redondeada al final; `anulado IS NULL OR anulado = 0`; capitalPagado = `PGPRCPPG` — el índice 4, sin el CASE de
  saldoOtros, que hoy no se usa aquí).
- `generarListado` necesita el `Prestamo` para `PrestamoSeguro`: usar `em.getReference(Prestamo.class, id)` (o el DAO
  equivalente) en vez de cargar todo.
- Incendio/prendario: pueden quedarse como están (ya filtran por tipo) o usar la misma consulta; el ejecutor decide y
  reporta, sin cambiar sus resultados.
- `distribuir` y `registrarNota` (~506, ~754, ~1100) usan los mismos cargadores pero solo con los préstamos del documento:
  fuera de alcance.
- **Verificación obligatoria:** en la copia de desarrollo, comparar préstamo por préstamo la salida vieja contra la nueva
  para una fecha de corte (mismos préstamos, misma base al centavo) y medir el tiempo de las dos. Reportar conteos,
  diferencias (deben ser 0) y tiempos.
- Docs: `crd/API-POLIZAS-SEGURO.md` (nota de rendimiento; el contrato no cambia).
