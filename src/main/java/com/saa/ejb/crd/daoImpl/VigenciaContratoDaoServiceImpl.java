package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.VigenciaContratoDaoService;
import com.saa.model.crd.VigenciaContrato;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoContrato;
import com.saa.rubros.EstadoParticipeEntidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class VigenciaContratoDaoServiceImpl extends EntityDaoImpl<VigenciaContrato>
        implements VigenciaContratoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "contrato",
            "tipoAporte",
            "fechaInicio",
            "fechaFin",
            "monto",
            "porcentaje",
            "remuneracion",
            "modo",
            "idHistorialSueldo",
            "observacion",
            "idEstado",
            "usuarioRegistro",
            "fechaRegistro"
        };
    }

    @Override
    public List<VigenciaContrato> selectByContrato(Long idContrato) throws Throwable {
        System.out.println("Ingresa al metodo selectByContrato de VigenciaContrato con idContrato: " + idContrato);
        Query query = em.createQuery(
            " select v from VigenciaContrato v " +
            " where  v.contrato.codigo = :idContrato " +
            " order by v.fechaInicio desc, v.codigo desc");
        query.setParameter("idContrato", idContrato);
        return query.getResultList();
    }

    @Override
    public VigenciaContrato selectAbierta(Long idContrato, Long idTipoAporte) throws Throwable {
        System.out.println("Ingresa al metodo selectAbierta de VigenciaContrato con idContrato: " + idContrato
            + " - idTipoAporte: " + idTipoAporte);
        try {
            Query query = em.createQuery(
                " select v from VigenciaContrato v " +
                " where  v.contrato.codigo = :idContrato " +
                "   and  v.tipoAporte.codigo = :idTipoAporte " +
                "   and  v.fechaFin is null " +
                "   and  v.idEstado = :activo");
            query.setParameter("idContrato", idContrato);
            query.setParameter("idTipoAporte", idTipoAporte);
            query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
            return (VigenciaContrato) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public VigenciaContrato selectVigenteEnFecha(Long idContrato, Long idTipoAporte, LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo selectVigenteEnFecha de VigenciaContrato con idContrato: " + idContrato
            + " - idTipoAporte: " + idTipoAporte + " - fecha: " + fecha);
        try {
            Query query = em.createQuery(
                " select v from VigenciaContrato v " +
                " where  v.contrato.codigo = :idContrato " +
                "   and  v.tipoAporte.codigo = :idTipoAporte " +
                "   and  v.idEstado = :activo " +
                "   and  v.fechaInicio <= :fecha " +
                "   and  (v.fechaFin is null or v.fechaFin >= :fecha)");
            query.setParameter("idContrato", idContrato);
            query.setParameter("idTipoAporte", idTipoAporte);
            query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
            query.setParameter("fecha", fecha);
            return (VigenciaContrato) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> selectVigentesPorFilial(Long codigoFilial) throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesPorFilial de VigenciaContrato con codigoFilial: " + codigoFilial);

        // Nativa a proposito: necesita ROW_NUMBER() para el desempate "contrato activo mas
        // reciente" en bloque, para toda la filial en una sola consulta (mismo criterio que
        // ContratoDaoServiceImpl.selectActivoPorEntidad, pero sin una llamada por entidad).
        Query query = em.createNativeQuery(
            " SELECT ca.ENTDCDGO, v.TPAPCDGO, v.VGCNFCIN, v.VGCNFCFN, v.VGCNMNTO "
            + " FROM ( SELECT c.CNTRCDGO, c.ENTDCDGO, "
            + "               ROW_NUMBER() OVER (PARTITION BY c.ENTDCDGO ORDER BY c.CNTRCDGO DESC) rn "
            + "        FROM   CRD.CNTR c "
            + "        JOIN   CRD.ENTD e ON e.ENTDCDGO = c.ENTDCDGO "
            + "        WHERE  c.CNTRESTD = " + EstadoContrato.ACTIVO
            + "        AND    e.FLLLCDGO = :codigoFilial "
            + "        AND    e.ENTDIDST IN (" + EstadoParticipeEntidad.ACTIVO + ", "
            +                                    EstadoParticipeEntidad.ACTIVO_EN_MORA + ") "
            + "      ) ca "
            + " JOIN   CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO AND v.VGCNIDST = " + Estado.ACTIVO
            + " WHERE  ca.rn = 1");
        query.setParameter("codigoFilial", codigoFilial);

        List<Object[]> crudas = query.getResultList();
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : crudas) {
            resultado.add(new Object[]{
                fila[0] != null ? ((Number) fila[0]).longValue() : null,
                fila[1] != null ? ((Number) fila[1]).longValue() : null,
                aFecha(fila[2]),
                aFecha(fila[3]),
                fila[4] != null ? ((Number) fila[4]).doubleValue() : 0.0
            });
        }
        return resultado;
    }

    private LocalDate aFecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.sql.Date) {
            return ((java.sql.Date) valor).toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) valor).toLocalDateTime().toLocalDate();
        }
        return null;
    }
}
