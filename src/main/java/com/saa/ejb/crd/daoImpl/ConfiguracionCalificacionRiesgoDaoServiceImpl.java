package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ConfiguracionCalificacionRiesgoDaoService;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.Producto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class ConfiguracionCalificacionRiesgoDaoServiceImpl extends EntityDaoImpl<ConfiguracionCalificacionRiesgo>
        implements ConfiguracionCalificacionRiesgoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "producto", "idEmpresa", "nombre", "fechaInicio", "fechaFin",
            "fechaRegistro", "usuarioRegistro", "estado"
        };
    }

    @Override
    public ConfiguracionCalificacionRiesgo selectVigentePorProducto(Long idProducto, Long idEmpresa,
            LocalDate fecha) throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectVigentePorProducto - producto: "
            + idProducto + " - empresa: " + idEmpresa + " - fecha: " + fecha);
        Query query = em.createQuery(
            "select c from ConfiguracionCalificacionRiesgo c "
                + "where c.producto.codigo = :idProducto "
                + "and (c.idEmpresa is null or c.idEmpresa = :idEmpresa) "
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :fecha "
                + "and (c.fechaFin is null or c.fechaFin >= :fecha) "
                // Sin NULLS LAST (portabilidad JPQL): con la carga inicial (sql/177) hay UNA sola
                // fila por producto, con idEmpresa null — no hay ambigüedad que desempatar todavía.
                // El día que se agregue una configuración específica por empresa, revisar este
                // orden para que la más específica gane sobre la genérica.
                + "order by c.fechaInicio desc, c.codigo desc");
        query.setParameter("idProducto", idProducto);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha", fecha);
        query.setMaxResults(1);
        List<ConfiguracionCalificacionRiesgo> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Producto> selectProductosSinConfiguracionVigente(LocalDate fecha) throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectProductosSinConfiguracionVigente - fecha: "
            + fecha);
        Query query = em.createQuery(
            "select p from Producto p where not exists ("
                + "select 1 from ConfiguracionCalificacionRiesgo c "
                + "where c.producto = p "
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :fecha "
                + "and (c.fechaFin is null or c.fechaFin >= :fecha)) "
                + "order by p.codigo");
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha", fecha);
        return query.getResultList();
    }
}
