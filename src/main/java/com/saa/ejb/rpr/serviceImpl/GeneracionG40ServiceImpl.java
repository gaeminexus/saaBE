package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

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

    private static final Long ESTADO_SIN_CAMBIOS = 1L;
    private static final Long ESTADO_MODIFICADO  = 2L;

    @EJB private InformacionGeneralFondoService igfnService;
    @EJB private CreditoG40DaoService           cg40DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G40");

        // -------------------------------------------------------
        // 1. Obtener el único registro de IGFN
        // -------------------------------------------------------
        List<InformacionGeneralFondo> lista = igfnService.selectAll();
        InformacionGeneralFondo igfn = lista.get(0);
        System.out.println("IGFN encontrado con id: " + igfn.getCodigo() + ", estado: " + igfn.getEstado());

        // -------------------------------------------------------
        // 2. Si no fue modificado → G40 vacío, retornar 0
        // -------------------------------------------------------
        if (!ESTADO_MODIFICADO.equals(igfn.getEstado())) {
            System.out.println("IGFN sin cambios → G40 queda vacio y OK");
            return 0L;
        }

        // -------------------------------------------------------
        // 3. Mapear IGFN → CG40
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
        cg40.setValorAportePersonalCesantia(igfn.getValorAportePersonalCesantia());
        cg40.setValorAportePersonalJubilacion(igfn.getValorAportePersonalJubilacion());

        // -------------------------------------------------------
        // 4. Persistir el registro en CG40
        // -------------------------------------------------------
        cg40DaoService.save(cg40, null);
        System.out.println("Registro G40 insertado correctamente");

        // -------------------------------------------------------
        // 5. Resetear estado IGFN a 1 (Sin cambios)
        // -------------------------------------------------------
        igfn.setEstado(ESTADO_SIN_CAMBIOS);
        igfnService.saveSingle(igfn);
        System.out.println("IGFN reseteado a estado Sin cambios");

        return 1L;
    }
}
