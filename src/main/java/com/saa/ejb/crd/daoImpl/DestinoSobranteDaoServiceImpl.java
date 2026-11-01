package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DestinoSobranteDaoService;
import com.saa.model.crd.DestinoSobrante;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DestinoSobranteDaoServiceImpl extends EntityDaoImpl<DestinoSobrante>
        implements DestinoSobranteDaoService {

    /** DSSBESTD: 1 = activo. */
    private static final long ACTIVO = 1L;

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DestinoSobrante");
        return new String[]{
            "codigo",
            "cargaArchivo",
            "entidad",
            "tipoDestino",
            "prestamo",
            "tipoAporte",
            "valorAplicadoAportes",
            "valorAplicadoPrestamos",
            "idPagoProgramado",
            "idAsiento",
            "estado",
            "observacion",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioModificacion",
            "fechaModificacion"
        };
    }

    @Override
    public List<DestinoSobrante> selectActivosByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectActivosByCarga de DestinoSobrante - carga: " + idCarga);
        Query query = em.createQuery(
                " select d from DestinoSobrante d " +
                " where  d.cargaArchivo.codigo = :idCarga " +
                " and    d.estado              = :activo " +
                " order by d.codigo");
        query.setParameter("idCarga", idCarga);
        query.setParameter("activo", ACTIVO);
        return query.getResultList();
    }

    @Override
    public List<DestinoSobrante> selectByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectByCarga de DestinoSobrante - carga: " + idCarga);
        Query query = em.createQuery(
                " select d from DestinoSobrante d " +
                " where  d.cargaArchivo.codigo = :idCarga " +
                " order by d.codigo");
        query.setParameter("idCarga", idCarga);
        return query.getResultList();
    }

    @Override
    public void flush() throws Throwable {
        em.flush();
    }

    @Override
    public DestinoSobrante selectActivoByCargaYEntidad(Long idCarga, Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectActivoByCargaYEntidad de DestinoSobrante - carga: " + idCarga
                + " - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select d from DestinoSobrante d " +
                " where  d.cargaArchivo.codigo = :idCarga " +
                " and    d.entidad.codigo      = :idEntidad " +
                " and    d.estado              = :activo");
        query.setParameter("idCarga", idCarga);
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("activo", ACTIVO);
        query.setMaxResults(1);
        List<DestinoSobrante> lista = query.getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }
}
