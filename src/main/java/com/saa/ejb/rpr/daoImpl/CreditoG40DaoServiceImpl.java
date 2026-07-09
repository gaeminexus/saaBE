package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CreditoG40DaoService;
import com.saa.model.rpr.CreditoG40;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class CreditoG40DaoServiceImpl extends EntityDaoImpl<CreditoG40> implements CreditoG40DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacionFcpc", "identificacionFcpc", "numeroResolucion",
            "fechaResolucion", "provincia", "canton", "direccion", "telefonos",
            "correoElectronico", "tipoSistema", "tipoPrestacion", "tipoAporte",
            "tipoAdministracion", "fechaTraspaso", "tipoFcpc", "numeroResolucionCambioEstatuto",
            "fechaResolucionCambioEstatuto", "cambioNombre", "porcentajeAportePatronalCesantia",
            "porcentajeAportePersonalCesantia", "porcentajeAportePatronalJubilacion",
            "porcentajeAportePersonalJubilacion", "valorAportePersonalCesantia",
            "valorAportePersonalJubilacion"
        };
    }
}
