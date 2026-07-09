package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG40DaoService;
import com.saa.model.rpr.HistoricoG40;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG40DaoServiceImpl extends EntityDaoImpl<HistoricoG40> implements HistoricoG40DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","numeroResolucion","fechaResolucion",
            "provincia","canton","direccion","telefonos","correoElectronico","tipoSistema","tipoPrestacion",
            "tipoAporte","tipoAdministracion","fechaTraspaso","tipoFcpc","numeroResolucionCambioEstatuto",
            "fechaResolucionCambioEstatuto","cambioNombre","porcentajeAportePatronalCesantia",
            "porcentajeAportePersonalCesantia","porcentajeAportePatronalJubilacion",
            "porcentajeAportePersonalJubilacion","valorAportePersonalCesantia","valorAportePersonalJubilacion" };
    }
}
