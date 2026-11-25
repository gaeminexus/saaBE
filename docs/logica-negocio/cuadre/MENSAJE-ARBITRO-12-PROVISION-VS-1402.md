# Mensaje para el árbitro omen-saa-1: provisión de intereses mayor que la 1.4.02 (oct–dic 2025)

Copiar tal cual el bloque de abajo. No lleva datos de partícipes (préstamos por idAsoprep).

```text
Del líder del proyecto de cuadre (25-nov-2026). El contador de ASOPREP vio en el balance de octubre 2025 que la provisión
1.4.99.05 (1.160.911,72) es mayor que los intereses por cobrar 1.4.02 (1.151.756,85). En noviembre la diferencia es 37.867,27.
Medimos cuatro causas. Necesitamos tres arreglos antes de seguir con diciembre 2025 y uno sobre la re-imputación.

Archivos en la rama cuadre-docs-provision-1402-1125, docs/logica-negocio/cuadre/:
  - provision-vs-1402-oct-nov-2025.sql: saldos, libro de provisión, cuotas cerradas con provisión, asiento ⑦ contra su libro.
  - def9-205-reverso-devengo-de-mas.sql: el reverso de devengo, asiento por asiento.

1) DevengoCompletoServiceImpl.limiteDevengado (BLOQUEANTE)
   - Toma el último cierre de cartera EJECUTADO de la base (CRD.CRCT), sin importar su fecha. En la base de trabajo, que es
     copia de producción, están los cierres de producción de jul–sep 2026 (CRCT 1, 2 y 4). El límite queda en el 31-oct-2026.
   - Por eso lineasReversoCuotasCanceladas y lineasReversoAbono reversan (D 5.1.02 / H 1.4.02) el interés de hasta 12 cuotas
     futuras que nunca se devengaron.
   - Medición: 68 asientos de oct–dic 2025. Con el límite 31-oct-2026, la emulación da exactamente lo reversado en los 68
     (diferencia 0,00). Reversado 67.575,01; correcto 4.821,03; de más 62.753,98 (1.4.02.05 32.861,33 · 1.4.02.15 29.892,65).
     Ejemplo: en el acuerdo 42 (préstamo 69225) reversó 5.613,98; lo correcto era solo la cuota 51 (488,10).
   - Arreglo pedido: el límite debe salir del último cierre ejecutado con corte ANTERIOR a la fecha de la operación. Con
     devengoCompletoDesde = 2025-10-31 eso da: operaciones de octubre, nada; de nov y dic, solo las cuotas del mes de la
     operación. Revisen también los demás usos de limiteDevengado, porque en producción el reemplazo va a dejar igual cierres
     posteriores en CRCT.
   - El arrastre lo corregimos con asientos manuales: oct–nov dentro del ajuste al 1-dic-2025 y diciembre al 31-dic
     (D 1.4.02 / H 5.1.02, 20.719,68). Ver el bloque nuevo del encargo (ENCARGO-ARBITRO-BLOQUE-1402-PROVISION).

2) Provisión que no se reversa en cuotas que ya no están pendientes
   - Al 30-nov quedan 5.171,00 de provisión de interés en 54 cuotas canceladas (estado 7) de 18 préstamos. Al 31-dic, antes
     del ⑦, son 12.816,69 en 173 cuotas de 41 préstamos.
   - Casi todo viene de acuerdos en los que el deudor PAGÓ el interés vencido (DACC concepto 2, DACCVLPG), pero el pago no
     reversó la provisión de esas cuotas. Ejemplo: acuerdo 42, préstamo 69225: cuotas 49 y 50 pagadas (986,88), sin tipo 2 en
     MVIC y sin PGPR en esas cuotas. La 1.4.02 sí se acreditó, así que la provisión queda sin activo.
   - Además hay provisión negativa en cuotas pagadas (reversos mayores que lo provisionado): −1.787,26 al 30-nov y −2.748,36
     al 31-dic.
   - El ⑦ no las toca porque ya no son provisionables, así que quedan para siempre.
   - Arreglo pedido: (a) que el acuerdo (y la precancelación) reverse la provisión de las cuotas vencidas cuyo interés cobra;
     (b) un ajuste por cuota como /prov/ajusteTopeMora, pero para interés: lleva a 0 el saldo provisionado de las cuotas que al
     corte ya no están pendientes, con grabar=false para previsualizar y ligado a un asiento manual, para regularizar el 31-dic.

3) Paso ⑦: el asiento registra menos que su libro por cuota
   - armaProvisionIntereses arma las líneas con calcularProvisionPorTipoPrestamo. Después generaAsiento graba el libro
     (registrarProvisionCierre), que recalcula todo.
   - Asiento contra MVIC tipo 1 del mismo asiento: oct 54.358,53 contra 55.548,29 (libro 1.189,76 mayor); nov 59.809,15 contra
     70.795,89 (10.986,74 mayor). La diferencia está en la mora (libro de mora oct 2.273,64; nov 13.218,27).
   - Sospecha: entre las dos lecturas se graba el devengo de mora del mismo cierre (⑩, MVIC 5) y sube el tope de mora. ¿Lo
     confirman?
   - Arreglo pedido: un solo cálculo para el asiento y el libro (o que el ⑦ se arme después del ⑩). Dígannos cómo igualar lo
     acumulado: asiento manual por la diferencia, o ajuste del libro.

4) Re-imputación de prelación (/prelacioncuotas/reimputarAlCorte)
   - El líder reporta que deja cuotas parciales entre cuotas pagadas. No la vamos a usar hasta que la revisen.
   - Caso que nos detiene en diciembre: préstamo 7940 (63344), cuota 129 sobreimputada 398,81. El cruce de pensión del
     31-dic se aplicó antes que el cobro del 26-dic y los dos fueron a la cuota 129. ¿Qué camino proponen sin la
     re-imputación?
```
