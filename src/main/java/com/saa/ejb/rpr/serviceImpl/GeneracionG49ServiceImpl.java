package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG49Service;
import com.saa.ejb.rpr.service.HistoricoG48Service;
import com.saa.ejb.rpr.service.SaldoOperacionG48Service;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.CancelacionG49;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.model.rpr.HistoricoG48;
import com.saa.model.rpr.SaldoOperacionG48;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG49ServiceImpl implements GeneracionG49Service {

    @EJB private DetallePrestamoService         detallePrestamoService;
    @EJB private CancelacionG49DaoService       cg49DaoService;
    @EJB private SaldoOperacionG48Service       saldoG48Service;
    @EJB private SaldoOperacionG48DaoService    cg48DaoService;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private EjecucionReporteService        ejrcService;
    @EJB private HistoricoG48Service            historicoG48Service;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G49");

        long mes  = detalle.getEjecucionReporte().getMes();
        long anio = detalle.getEjecucionReporte().getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();

        LocalDateTime fechaInicio  = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);
        LocalDateTime fechaFin     = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        LocalDate     fechaCancelacion = LocalDate.of((int) anio, (int) mes, ultimoDia);

        System.out.println("G49 - Rango: " + fechaInicio + " a " + fechaFin);

        long contador = 0L;

        // Set de numeroOperacion ya insertados para evitar duplicados entre grupos
        Set<String> operacionesInsertadas = new HashSet<>();

        // -------------------------------------------------------
        // GRUPO 1: Cuota con mayor numeroCuota del préstamo
        //          dentro del mes y en estado pagada (4).
        //          Indica que la última cuota fue pagada → cancelación normal.
        // -------------------------------------------------------
        List<DetallePrestamo> grupo1 = detallePrestamoService.selectMaxCuotaPagadaDelMesGlobal(fechaInicio, fechaFin);
        System.out.println("G49 - Grupo 1 (última cuota pagada del mes): " + grupo1.size());

        for (DetallePrestamo cuota : grupo1) {
            try {
                Prestamo prestamo = cuota.getPrestamo();
                if (prestamo == null) continue;
                String numOp = prestamo.getIdAsoprep() != null ? String.valueOf(prestamo.getIdAsoprep()) : null;
                if (numOp == null || operacionesInsertadas.contains(numOp)) continue;

                insertarG49(prestamo, numOp, fechaCancelacion, detalle);
                operacionesInsertadas.add(numOp);
                contador++;
            } catch (Throwable e) {
                System.out.println("G49 ERROR grupo1 cuota " + cuota.getCodigo() + ": " + e.getMessage());
            }
        }

        // -------------------------------------------------------
        // GRUPO 2: Préstamos en estadoPrestamo=4 (cancelado anticipado)
        //          cuya máxima cuota pagada esté dentro del mes.
        //          Excluye los que ya están en grupo 1.
        // -------------------------------------------------------
        List<DetallePrestamo> grupo2 = detallePrestamoService
                .selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(fechaInicio, fechaFin);
        System.out.println("G49 - Grupo 2 (cancelado anticipado del mes): " + grupo2.size());

        for (DetallePrestamo cuota : grupo2) {
            try {
                Prestamo prestamo = cuota.getPrestamo();
                if (prestamo == null) continue;
                String numOp = prestamo.getIdAsoprep() != null ? String.valueOf(prestamo.getIdAsoprep()) : null;
                if (numOp == null || operacionesInsertadas.contains(numOp)) continue;

                insertarG49(prestamo, numOp, fechaCancelacion, detalle);
                operacionesInsertadas.add(numOp);
                contador++;
            } catch (Throwable e) {
                System.out.println("G49 ERROR grupo2 cuota " + cuota.getCodigo() + ": " + e.getMessage());
            }
        }

        // -------------------------------------------------------
        // GRUPO 3: Operaciones que estaban en G48 del período anterior
        //          pero ya NO están en G48 del período actual.
        // -------------------------------------------------------
        contador += procesarGrupo3(detalle, mes, anio, fechaCancelacion, operacionesInsertadas);

        // -------------------------------------------------------
        // ELIMINAR del G48 actual TODAS las operaciones insertadas en G49
        // (grupos 1, 2 y 3) para que no aparezcan en ambos reportes.
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG48Actual = ejrdService.selectByEjecucionYTipo(
            detalle.getEjecucionReporte().getCodigo(), "G48"
        );
        if (ejrdG48Actual != null) {
            int totalBorrados = 0;
            for (String numOp : operacionesInsertadas) {
                try {
                    int borrados = cg48DaoService.deleteByDetalleYOperacion(ejrdG48Actual.getCodigo(), numOp);
                    if (borrados > 0) {
                        totalBorrados += borrados;
                        System.out.println("G49 - Eliminado del G48 operación: " + numOp);
                    }
                } catch (Throwable e) {
                    System.out.println("G49 ERROR al eliminar del G48 operación " + numOp + ": " + e.getMessage());
                }
            }
            System.out.println("G49 - Total operaciones eliminadas del G48: " + totalBorrados);

            // Actualizar cantidadRegistros del EJRD del G48 con COUNT real
            try {
                long countReal = cg48DaoService.countByDetalle(ejrdG48Actual.getCodigo());
                ejrdG48Actual.setCantidadRegistros(countReal);
                ejrdService.saveSingle(ejrdG48Actual);
                System.out.println("G49 - cantidadRegistros G48 actualizado a: " + countReal);
            } catch (Throwable e) {
                System.out.println("G49 ERROR al actualizar cantidadRegistros G48: " + e.getMessage());
            }
        }

        System.out.println("G49 generado con " + contador + " registros");
        return contador;
    }

    /**
     * Grupo 3: operaciones presentes en G48 del período anterior que
     * desaparecieron del G48 del período actual.
     * EXCEPCIÓN: para la ejecución de junio 2025 (mes=6, anio=2025),
     * el período anterior se obtiene desde la tabla HM48 usando NOT EXISTS
     * contra el CG48 de junio 2025.
     */
    private long procesarGrupo3(DetalleEjecucionReporte detalle, long mes, long anio,
            LocalDate fechaCancelacion, Set<String> operacionesInsertadas) {
        long contador = 0L;
        try {
            // --- G48 del período actual ---
            DetalleEjecucionReporte ejrdG48Actual = ejrdService.selectByEjecucionYTipo(
                detalle.getEjecucionReporte().getCodigo(), "G48"
            );
            Set<String> operacionesActuales = new HashSet<>();
            if (ejrdG48Actual != null) {
                List<SaldoOperacionG48> g48Actual = saldoG48Service.selectByDetalle(ejrdG48Actual.getCodigo());
                for (SaldoOperacionG48 r : g48Actual) {
                    if (r.getNumeroOperacion() != null) operacionesActuales.add(r.getNumeroOperacion());
                }
            }
            System.out.println("G49 Grupo 3 - Operaciones en G48 actual: " + operacionesActuales.size());

            // -------------------------------------------------------
            // Caso especial: ejecución de JUNIO 2025
            // Obtener operaciones del período anterior desde HM48
            // usando NOT EXISTS contra CG48 de junio 2025
            // -------------------------------------------------------
            if (mes == 6L && anio == 2025L) {
                System.out.println("G49 Grupo 3 - Caso especial Junio 2025: leyendo desde HM48 con NOT EXISTS en CG48");
                List<HistoricoG48> hm48NoEnCg48 = historicoG48Service.selectEnHm48NoEnCg48Junio2025();
                System.out.println("G49 Grupo 3 - Registros en HM48 no presentes en CG48 junio 2025: " + hm48NoEnCg48.size());

                for (HistoricoG48 anterior : hm48NoEnCg48) {
                    String numOp = anterior.getNumeroOperacion();
                    if (numOp == null) continue;
                    if (operacionesInsertadas.contains(numOp)) continue; // ya insertado en grupo 1 o 2

                    CancelacionG49 g49 = new CancelacionG49();
                    g49.setTipoIdentificacion(anterior.getTipoIdentificacion() != null
                            ? anterior.getTipoIdentificacion() : "C");
                    g49.setIdentificacion(anterior.getIdentificacion());
                    g49.setNumeroOperacion(numOp);
                    g49.setFechaCancelacion(fechaCancelacion);
                    g49.setFormaCancelacion("N");
                    g49.setDetalleEjecucion(detalle);

                    cg49DaoService.save(g49, null);
                    operacionesInsertadas.add(numOp);
                    contador++;
                    System.out.println("G49 Grupo 3 (HM48) INSERT op: " + numOp);
                }

            } else {
                // -------------------------------------------------------
                // Caso normal: leer G48 del período anterior (mes - 1)
                // -------------------------------------------------------
                long mesPrevio  = mes == 1 ? 12 : mes - 1;
                long anioPrevio = mes == 1 ? anio - 1 : anio;

                List<EjecucionReporte> listaPrev = ejrcService.selectByMesAnio(mesPrevio, anioPrevio);
                if (listaPrev == null || listaPrev.isEmpty()) {
                    System.out.println("G49 Grupo 3 - No hay EJRC anterior, sin cancelaciones por salida de G48.");
                    return 0L;
                }

                EjecucionReporte ejrcPrev = listaPrev.get(0);
                DetalleEjecucionReporte ejrdG48Prev = ejrdService.selectByEjecucionYTipo(ejrcPrev.getCodigo(), "G48");
                if (ejrdG48Prev == null) {
                    System.out.println("G49 Grupo 3 - No hay G48 del período anterior.");
                    return 0L;
                }

                List<SaldoOperacionG48> g48Anterior = saldoG48Service.selectByDetalle(ejrdG48Prev.getCodigo());
                System.out.println("G49 Grupo 3 - Operaciones en G48 anterior: " + g48Anterior.size());

                for (SaldoOperacionG48 anterior : g48Anterior) {
                    String numOp = anterior.getNumeroOperacion();
                    if (numOp == null) continue;
                    if (operacionesActuales.contains(numOp)) continue;   // sigue en G48 actual → no cancelado
                    if (operacionesInsertadas.contains(numOp)) continue; // ya insertado en grupo 1 o 2

                    CancelacionG49 g49 = new CancelacionG49();
                    g49.setTipoIdentificacion(anterior.getTipoIdentificacion() != null
                            ? anterior.getTipoIdentificacion() : "C");
                    g49.setIdentificacion(anterior.getIdentificacion());
                    g49.setNumeroOperacion(numOp);
                    g49.setFechaCancelacion(fechaCancelacion);
                    g49.setFormaCancelacion("N");
                    g49.setDetalleEjecucion(detalle);

                    cg49DaoService.save(g49, null);
                    operacionesInsertadas.add(numOp);
                    contador++;
                    System.out.println("G49 Grupo 3 INSERT op: " + numOp);
                }
            }

        } catch (Throwable e) {
            System.out.println("G49 ERROR grupo3: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("G49 Grupo 3 insertados: " + contador);
        return contador;
    }

    /** Crea y persiste un registro en CG49. */
    private void insertarG49(Prestamo prestamo, String numOp,
            LocalDate fechaCancelacion, DetalleEjecucionReporte detalle) throws Throwable {
        CancelacionG49 g49 = new CancelacionG49();
        g49.setTipoIdentificacion("C");
        if (prestamo.getEntidad() != null) {
            g49.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
        }
        g49.setNumeroOperacion(numOp);
        g49.setFechaCancelacion(fechaCancelacion);
        g49.setFormaCancelacion("N");
        g49.setDetalleEjecucion(detalle);
        cg49DaoService.save(g49, null);
        System.out.println("G49 INSERT op: " + numOp
            + " entidad: " + (prestamo.getEntidad() != null ? prestamo.getEntidad().getNumeroIdentificacion() : "?"));
    }
}
