package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;

import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG40Service;
import com.saa.ejb.rpr.service.GeneracionG41Service;
import com.saa.ejb.rpr.service.GeneracionG42Service;
import com.saa.ejb.rpr.service.GeneracionG43Service;
import com.saa.ejb.rpr.service.GeneracionG44Service;
import com.saa.ejb.rpr.service.GeneracionG45Service;
import com.saa.ejb.rpr.service.GeneracionG46Service;
import com.saa.ejb.rpr.service.GeneracionG47Service;
import com.saa.ejb.rpr.service.GeneracionG48Service;
import com.saa.ejb.rpr.service.GeneracionG49Service;
import com.saa.ejb.rpr.service.GeneracionG50Service;
import com.saa.ejb.rpr.service.GeneracionG51Service;
import com.saa.ejb.rpr.service.GeneracionUnReporteService;
import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see GeneracionUnReporteService
 * @author Sistema SAA
 * @since 2026-09-08
 */
@Stateless
public class GeneracionUnReporteServiceImpl implements GeneracionUnReporteService {

    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private GeneracionG40Service           g40Service;
    @EJB private GeneracionG41Service           g41Service;
    @EJB private GeneracionG42Service           g42Service;
    @EJB private GeneracionG43Service           g43Service;
    @EJB private GeneracionG44Service           g44Service;
    @EJB private GeneracionG45Service           g45Service;
    @EJB private GeneracionG46Service           g46Service;
    @EJB private GeneracionG47Service           g47Service;
    @EJB private GeneracionG48Service           g48Service;
    @EJB private GeneracionG49Service           g49Service;
    @EJB private GeneracionG50Service           g50Service;
    @EJB private GeneracionG51Service           g51Service;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long generarUno(DetalleEjecucionReporte ejrd) throws Throwable {
        System.out.println("GeneracionUnReporteService.generarUno - tipoReporte: " + ejrd.getTipoReporte()
                + " - EJRD: " + ejrd.getCodigo());
        // Mismo switch de los doce casos que antes vivía en
        // GeneracionReportesServiceImpl#ejecutarG — sin cambios de lógica, solo de ubicación
        // (para que corra en su propia transacción vía @TransactionAttribute REQUIRES_NEW).
        switch (ejrd.getTipoReporte()) {
            case "G40": return g40Service.generar(ejrd);
            case "G41": return g41Service.generar(ejrd);
            case "G42": return g42Service.generar(ejrd);
            case "G43": return g43Service.generar(ejrd);
            case "G44": return g44Service.generar(ejrd);
            case "G45": return g45Service.generar(ejrd);
            case "G46": return g46Service.generar(ejrd);
            case "G47": return g47Service.generar(ejrd);
            case "G48": return g48Service.generar(ejrd);
            case "G49": return g49Service.generar(ejrd);
            case "G50": return g50Service.generar(ejrd);
            case "G51": return g51Service.generar(ejrd);
            default:
                throw new Exception("Logica de generacion no implementada para: " + ejrd.getTipoReporte());
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void marcarResultado(Long idEjrd, Long estado, Long cantidadRegistros, String novedades) throws Throwable {
        System.out.println("GeneracionUnReporteService.marcarResultado - EJRD: " + idEjrd
                + " - estado: " + estado + " - novedades: " + novedades);
        // Recupera el EJRD de nuevo por id, NUNCA la entidad que trae el llamador: esa puede
        // venir de una transacción que generarUno acaba de marcar rollback-only, y guardar esa
        // instancia intentaría escribir dentro de una transacción muerta — es exactamente el
        // STATUS_MARKED_ROLLBACK que este cambio existe para eliminar.
        DetalleEjecucionReporte ejrd = ejrdService.selectById(idEjrd);
        ejrd.setEstado(estado);
        ejrd.setFechaGeneracion(LocalDate.now());
        ejrd.setCantidadRegistros(cantidadRegistros);
        // Truncado defensivo (2026-09-08): el mensaje del G48 solo (fecha + lista de
        // productos) ya pasa fácil los 200 caracteres; sin esto, saveSingle revienta con
        // ORA-12899 y el usuario vuelve a ver un error que no es el error.
        ejrd.setNovedades(truncar(novedades, LIMITE_NOVEDADES_EJRD));
        ejrdService.saveSingle(ejrd);
    }

    /**
     * Límite de {@code DetalleEjecucionReporte.novedades} — sale de
     * {@code @Column(length = 1000)} en la entidad (columna {@code EJRDNVDD}). Si esa columna
     * se agranda algún día, este número también hay que tocarlo.
     */
    private static final int LIMITE_NOVEDADES_EJRD = 1000;

    /**
     * Trunca {@code texto} a {@code limite} caracteres dejando el corte VISIBLE (termina en
     * "...", nunca a la mitad de una palabra sin avisar). {@code null} o ya corto se devuelve
     * tal cual.
     */
    private String truncar(String texto, int limite) {
        if (texto == null || texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite - 3) + "...";
    }
}
