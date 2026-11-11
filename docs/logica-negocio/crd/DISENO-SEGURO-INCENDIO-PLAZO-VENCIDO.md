# DISEÑO — Seguro de incendio: incluir los préstamos hipotecarios DE PLAZO VENCIDO

omen-saa-1-arb · 2026-11-11 · Pedido del usuario: «en la pantalla de seguros, para los seguros de incendio, que
también se tomen los préstamos hipotecarios en estado DE PLAZO VENCIDO». Ejecutor: omen-saa-1-be (solo backend; la
pantalla `polizas-seguro` no filtra estados ni rotula el estado del préstamo).

## 1. Lo que hay hoy (`DocumentoSeguroServiceImpl`)
- `calcularCandidatos` (línea ~207): para incendio y prendario toma `PRSTIDST IN (2 VIGENTE, 11 EN_MORA)`. El 8
  (DE_PLAZO_VENCIDO) queda fuera por D13 de `DISENO-PASE-A-PLAZO-VENCIDO.md` («un préstamo de plazo vencido ya no debe
  tener seguro»). Lo usan `previewListado`, `generarListado` y las inclusiones de las novedades.
- Novedades (línea ~1015): cada declaración de plazo vencido viva del rango sale como exclusión `PLAZO_VENCIDO`, para
  los tres tipos de seguro.
- Desgravamen va por `calcularCandidatosDesgravamen` (consulta propia): **no se toca**.

## 2. Cambio (decisión del usuario: D13 deja de aplicar al seguro de INCENDIO)
1. `calcularCandidatos`: si `tipoSeguro == INCENDIO`, los estados son `2, 11, 8`. Prendario sigue con `2, 11`.
   Se mantienen el filtro de tipo de préstamo hipotecario, S15 (al menos una cuota no pagada que venza después del
   corte; la declaración no cambia vencimientos, D18) y la base = `valorAsegurado`.
2. Novedades: para una factura de INCENDIO, las declaraciones de plazo vencido **no** generan exclusión
   `PLAZO_VENCIDO`. Desgravamen y prendario siguen igual.
3. Actualizar el comentario de la línea ~207 y dejar referencia a este documento; nota en D13 del diseño de plazo
   vencido de que para incendio quedó revocado el 2026-11-11.

## 3. Fuera de este cambio — pendiente de decisión del usuario
`DeclaracionPlazoVencidoServiceImpl.anularSeguroCuotasFuturas` pone en cero el seguro de incendio de las cuotas
futuras al declarar (y lo guarda en DPLV). Si el préstamo sigue asegurado, la pregunta es si al socio se le sigue
cargando el seguro de incendio en las cuotas futuras (y en la liquidación) o si lo asume ASOPREP. Hasta que se decida,
la declaración sigue anulándolo.

## 4. Verificación
`mvn -q compile`. Lectura del preview de incendio en un ambiente con algún hipotecario en estado 8: debe aparecer;
prendario y desgravamen sin cambios. Sin SQL ni DDL.
