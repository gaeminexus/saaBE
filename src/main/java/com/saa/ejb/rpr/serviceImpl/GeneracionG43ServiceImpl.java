package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.HistoricoG42DaoService;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG43Service;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.model.rpr.HistoricoG42;
import com.saa.model.rpr.ParticipeCesanteG43;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG43ServiceImpl implements GeneracionG43Service {

    @EJB private EjecucionReporteService        ejrcService;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private SaldoCuentaG42Service          saldoG42Service;
    @EJB private HistoricoG42DaoService         historicoG42DaoService;
    @EJB private ParticipeCesanteG43DaoService  cg43DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G43");

        // -------------------------------------------------------
        // 1. Obtener la cabecera EJRC actual y calcular mes/anio anterior
        // -------------------------------------------------------
        EjecucionReporte ejrcActual = detalle.getEjecucionReporte();
        long mesActual  = ejrcActual.getMes();
        long anioActual = ejrcActual.getAnio();

        long mesPrevio  = (mesActual == 1) ? 12 : mesActual - 1;
        long anioPrevio = (mesActual == 1) ? anioActual - 1 : anioActual;

        System.out.println("G43 - Mes actual: " + mesActual + "/" + anioActual
                + " | Mes anterior buscado: " + mesPrevio + "/" + anioPrevio);

        // -------------------------------------------------------
        // 2. Obtener el EJRD del G42 actual — necesario para el NOT EXISTS
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG42Actual =
                ejrdService.selectByEjecucionYTipo(ejrcActual.getCodigo(), "G42");

        if (ejrdG42Actual == null) {
            throw new Exception("G43: No se encontro el EJRD del G42 del mes actual. "
                    + "El G42 debe haberse generado antes que el G43.");
        }
        Long codigoDetalleActual = ejrdG42Actual.getCodigo();

        // -------------------------------------------------------
        // 3. Obtener los cesantes directamente en BD con NOT EXISTS:
        //    a) Si existe EJRC del mes anterior con EJRD G42
        //       → selectCesantesDesdeG42Previo(previo, actual)
        //    b) Si NO existe (primera ejecución)
        //       → selectCesantesDesdeHistorico(actual)
        // -------------------------------------------------------
        List<EjecucionReporte> ejecucionesPrevias = null;
        try {
            ejecucionesPrevias = ejrcService.selectByMesAnio(mesPrevio, anioPrevio);
        } catch (Throwable e) {
            ejecucionesPrevias = null;
        }

        // Lista final de cesantes — cada elemento lleva identificacion + tipoIdentificacion
        List<SaldoCuentaG42> cesantesG42   = null;
        List<HistoricoG42>   cesantesHist  = null;
        boolean usandoHistorico = false;

        if (ejecucionesPrevias != null && !ejecucionesPrevias.isEmpty()) {
            // --- Camino A: comparar CG42 mes anterior vs CG42 mes actual en BD ---
            EjecucionReporte ejrcPrevio = ejecucionesPrevias.get(0);
            DetalleEjecucionReporte ejrdG42Previo =
                    ejrdService.selectByEjecucionYTipo(ejrcPrevio.getCodigo(), "G42");

            if (ejrdG42Previo != null) {
                cesantesG42 = saldoG42Service.selectCesantesDesdeG42Previo(
                        ejrdG42Previo.getCodigo(), codigoDetalleActual);
                System.out.println("G43 - Camino A (CG42 mes previo): "
                        + cesantesG42.size() + " cesantes encontrados en BD");
            } else {
                // Existe EJRC pero sin EJRD G42 → tratar como primera vez
                usandoHistorico = true;
            }
        } else {
            // --- Camino B: primera ejecución → comparar HistoricoG42 vs CG42 actual en BD ---
            usandoHistorico = true;
        }

        if (usandoHistorico) {
            cesantesHist = historicoG42DaoService.selectCesantesDesdeHistorico(codigoDetalleActual);
            System.out.println("G43 - Camino B (HistoricoG42): "
                    + cesantesHist.size() + " cesantes encontrados en BD");
        }

        // -------------------------------------------------------
        // 4. Insertar los cesantes en CG43
        // -------------------------------------------------------
        long contador = 0L;

        if (cesantesG42 != null) {
            for (SaldoCuentaG42 g42 : cesantesG42) {
                ParticipeCesanteG43 cesante = new ParticipeCesanteG43();
                cesante.setIdentificacion(g42.getIdentificacion());
                cesante.setTipoIdentificacion(g42.getTipoIdentificacion());
                cesante.setDetalleEjecucion(detalle);
                cg43DaoService.save(cesante, null);
                System.out.println("G43 INSERT cesante (desde G42 previo): " + g42.getIdentificacion());
                contador++;
            }
        }

        if (cesantesHist != null) {
            for (HistoricoG42 h : cesantesHist) {
                ParticipeCesanteG43 cesante = new ParticipeCesanteG43();
                cesante.setIdentificacion(h.getIdentificacion());
                cesante.setTipoIdentificacion(h.getTipoIdentificacion());
                cesante.setDetalleEjecucion(detalle);
                cg43DaoService.save(cesante, null);
                System.out.println("G43 INSERT cesante (desde HistoricoG42): " + h.getIdentificacion());
                contador++;
            }
        }

        if (contador == 0) {
            System.out.println("G43 - Sin cesantes este mes. G43 vacio, OK.");
        } else {
            System.out.println("G43 generado con " + contador + " registros");
        }
        return contador;
    }
}