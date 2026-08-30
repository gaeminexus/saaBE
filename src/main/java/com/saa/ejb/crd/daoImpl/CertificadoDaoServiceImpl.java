package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CertificadoDaoService;
import com.saa.model.crd.Certificado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CertificadoDaoServiceImpl extends EntityDaoImpl<Certificado> implements CertificadoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "anio",
            "numero",
            "numeroAlterno",
            "tipoCertificado",
            "entidad",
            "prestamo",
            "calidad",
            "fechaEmision",
            "usuarioEmision",
            "datos",
            "pdf",
            "estado",
            "usuarioAnulacion",
            "fechaAnulacion",
            "motivoAnulacion",
            "fechaRegistro"
        };
    }

    @Override
    public List<Certificado> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad con entidad: " + idEntidad);
        Query query = em.createQuery(
                " select c from Certificado c " +
                " where  c.entidad.codigo = :idEntidad " +
                " order by c.anio desc, c.numero desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public List<Certificado> selectByAnio(Long anio) throws Throwable {
        System.out.println("Ingresa al metodo selectByAnio con anio: " + anio);
        Query query = em.createQuery(
                " select c from Certificado c " +
                " where  c.anio = :anio " +
                " order by c.numero");
        query.setParameter("anio", anio);
        return query.getResultList();
    }

    @Override
    public Long siguienteNumero(Long anio) throws Throwable {
        System.out.println("Ingresa al metodo siguienteNumero con anio: " + anio);
        // Serializa las emisiones concurrentes. El lock vive hasta el commit/rollback de la
        // transaccion del llamador, que es la misma en la que se inserta el certificado.
        em.createNativeQuery("LOCK TABLE CRD.CRTF IN EXCLUSIVE MODE").executeUpdate();
        Query query = em.createQuery(
                " select max(c.numero) from Certificado c " +
                " where  c.anio = :anio ");
        query.setParameter("anio", anio);
        Long maximo = (Long) query.getSingleResult();
        return (maximo == null ? 0L : maximo) + 1L;
    }

    @Override
    public boolean existeAporteDeTipos(Long idEntidad, List<Long> tipos) throws Throwable {
        System.out.println("Ingresa al metodo existeAporteDeTipos con entidad: " + idEntidad + " tipos: " + tipos);
        if (tipos == null || tipos.isEmpty()) {
            return false;
        }
        Query query = em.createQuery(
                " select count(a.codigo) from Aporte a " +
                " where  a.entidad.codigo = :idEntidad " +
                " and    a.tipoAporte.codigo in (:tipos) ");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("tipos", tipos);
        Long cantidad = (Long) query.getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    @Override
    public LocalDate primerPeriodoAporte(Long idEntidad, List<Long> tipos) throws Throwable {
        System.out.println("Ingresa al metodo primerPeriodoAporte con entidad: " + idEntidad + " tipos: " + tipos);
        if (tipos == null || tipos.isEmpty()) {
            return null;
        }
        // Nativo: el periodo se resuelve con NVL(APRTPRDV, TRUNC(APRTFCTR,'MM')), nunca la columna sola.
        StringBuilder marcadores = new StringBuilder();
        for (int i = 0; i < tipos.size(); i++) {
            marcadores.append(i == 0 ? "?" : ", ?");
        }
        Query query = em.createNativeQuery(
                " SELECT MIN(NVL(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))) " +
                " FROM   CRD.APRT a " +
                " WHERE  a.ENTDCDGO = ? " +
                " AND    a.TPAPCDGO IN (" + marcadores + ") ");
        query.setParameter(1, idEntidad);
        for (int i = 0; i < tipos.size(); i++) {
            query.setParameter(i + 2, tipos.get(i));
        }
        Object resultado = query.getSingleResult();
        if (resultado == null) {
            return null;
        }
        if (resultado instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) resultado).toLocalDateTime().toLocalDate();
        }
        if (resultado instanceof java.sql.Date) {
            return ((java.sql.Date) resultado).toLocalDate();
        }
        if (resultado instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) resultado).getTime()).toLocalDate();
        }
        return LocalDate.parse(resultado.toString().substring(0, 10));
    }

    @Override
    public boolean existePagoPension(String cedula) throws Throwable {
        System.out.println("Ingresa al metodo existePagoPension con cedula: " + cedula);
        if (cedula == null || cedula.trim().isEmpty()) {
            return false;
        }
        Query query = em.createNativeQuery(
                " SELECT COUNT(*) FROM CRD.HPPJ h WHERE h.HPPJCEDU = ? ");
        query.setParameter(1, cedula.trim());
        Object resultado = query.getSingleResult();
        return resultado != null && ((Number) resultado).longValue() > 0;
    }
}
