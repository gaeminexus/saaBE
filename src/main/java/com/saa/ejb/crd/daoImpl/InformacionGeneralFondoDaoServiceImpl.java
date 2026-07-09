package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.InformacionGeneralFondoDaoService;
import com.saa.model.crd.InformacionGeneralFondo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class InformacionGeneralFondoDaoServiceImpl extends EntityDaoImpl<InformacionGeneralFondo>
        implements InformacionGeneralFondoDaoService {

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
            "valorAportePersonalJubilacion", "estado", "usuarioModificacion", "fechaModificacion"
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<InformacionGeneralFondo> selectModificados() throws Throwable {
        System.out.println("Ingresa al metodo selectModificados InformacionGeneralFondo");
        Query query = em.createQuery(
            "select e from InformacionGeneralFondo e where e.estado = 2");
        return query.getResultList();
    }
}
