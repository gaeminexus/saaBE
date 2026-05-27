package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.InformacionGeneralFondoService;
import com.saa.ejb.rpr.dao.CreditoG40DaoService;
import com.saa.ejb.rpr.service.GeneracionG40Service;
import com.saa.model.crd.InformacionGeneralFondo;
import com.saa.model.rpr.CreditoG40;
import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG40ServiceImpl implements GeneracionG40Service {

    private static final long MES_JUNIO           = 6L;
    private static final long MES_DICIEMBRE       = 12L;
    private static final Long TIPO_APORTE_JUBILACION = 9L;
    private static final Long TIPO_APORTE_CESANTIA   = 11L;

    @EJB private InformacionGeneralFondoService igfnService;
    @EJB private CreditoG40DaoService           cg40DaoService;
    @EJB private AporteService                  aporteService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G40");

        // -------------------------------------------------------
        // 1. Verificar que el mes sea junio o diciembre
        // -------------------------------------------------------
        long mes  = detalle.getEjecucionReporte().getMes();
        long anio = detalle.getEjecucionReporte().getAnio();

        if (mes != MES_JUNIO && mes != MES_DICIEMBRE) {
            System.out.println("G40 solo se genera en junio y diciembre. Mes actual: " + mes + " → G40 omitido");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Obtener el único registro de IGFN
        //    Si no existe, retornar 0 sin error (no hay datos para generar)
        // -------------------------------------------------------
        List<InformacionGeneralFondo> lista;
        try {
            lista = igfnService.selectAll();
        } catch (Throwable e) {
            System.out.println("G40 - No se encontro registro IGFN → G40 queda vacio y OK. Detalle: " + e.getMessage());
            return 0L;
        }
        if (lista == null || lista.isEmpty()) {
            System.out.println("G40 - Lista IGFN vacia → G40 queda vacio y OK");
            return 0L;
        }
        InformacionGeneralFondo igfn = lista.get(0);
        System.out.println("IGFN encontrado con id: " + igfn.getCodigo());

        // -------------------------------------------------------
        // 4. Calcular fechaCorte = último día del mes a las 23:59:59
        //    y obtener sumas de aportes tipo 9 (jubilación) y tipo 11 (cesantía)
        // -------------------------------------------------------
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaCorte = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);

        Double sumaJubilacion = aporteService.selectSumaTotalPorTipoAporte(fechaCorte, TIPO_APORTE_JUBILACION);
        Double sumaCesantia   = aporteService.selectSumaTotalPorTipoAporte(fechaCorte, TIPO_APORTE_CESANTIA);
        System.out.println("G40 - valorAportePersonalJubilacion (tipo 9): " + sumaJubilacion);
        System.out.println("G40 - valorAportePersonalCesantia   (tipo 11): " + sumaCesantia);

        // -------------------------------------------------------
        // 5. Mapear IGFN → CG40
        // -------------------------------------------------------
        CreditoG40 cg40 = new CreditoG40();
        cg40.setDetalleEjecucion(detalle);
        cg40.setTipoIdentificacionFcpc(igfn.getTipoIdentificacionFcpc());
        cg40.setIdentificacionFcpc(igfn.getIdentificacionFcpc());
        cg40.setNumeroResolucion(igfn.getNumeroResolucion());
        cg40.setFechaResolucion(igfn.getFechaResolucion());
        cg40.setProvincia(igfn.getProvincia());
        cg40.setCanton(igfn.getCanton());
        cg40.setDireccion(igfn.getDireccion());
        cg40.setTelefonos(igfn.getTelefonos());
        cg40.setCorreoElectronico(igfn.getCorreoElectronico());
        cg40.setTipoSistema(igfn.getTipoSistema());
        cg40.setTipoPrestacion(igfn.getTipoPrestacion());
        cg40.setTipoAporte(igfn.getTipoAporte());
        cg40.setTipoAdministracion(igfn.getTipoAdministracion());
        cg40.setFechaTraspaso(igfn.getFechaTraspaso());
        cg40.setTipoFcpc(igfn.getTipoFcpc());
        cg40.setNumeroResolucionCambioEstatuto(igfn.getNumeroResolucionCambioEstatuto());
        cg40.setFechaResolucionCambioEstatuto(igfn.getFechaResolucionCambioEstatuto());
        cg40.setCambioNombre(igfn.getCambioNombre());
        cg40.setPorcentajeAportePatronalCesantia(igfn.getPorcentajeAportePatronalCesantia());
        cg40.setPorcentajeAportePersonalCesantia(igfn.getPorcentajeAportePersonalCesantia());
        cg40.setPorcentajeAportePatronalJubilacion(igfn.getPorcentajeAportePatronalJubilacion());
        cg40.setPorcentajeAportePersonalJubilacion(igfn.getPorcentajeAportePersonalJubilacion());
        // Valores calculados desde los aportes del G42 (suma tipo 11 = cesantía, tipo 9 = jubilación)
        cg40.setValorAportePersonalCesantia(sumaCesantia);
        cg40.setValorAportePersonalJubilacion(sumaJubilacion);

        // -------------------------------------------------------
        // 6. Persistir el registro en CG40
        // -------------------------------------------------------
        cg40DaoService.save(cg40, null);
        System.out.println("Registro G40 insertado correctamente");

        return 1L;
    }
}
