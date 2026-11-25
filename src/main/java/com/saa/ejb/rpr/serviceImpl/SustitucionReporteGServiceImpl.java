package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.ejb.rpr.service.GeneracionG46Service;
import com.saa.ejb.rpr.service.SustitucionReporteGService;
import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.CancelacionG49;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * C40 (23-nov-2026) — ver {@link SustitucionReporteGService} y docs/logica-negocio/reportes/sql/C40-01-ddl-sustituciones-g.sql.
 *
 * <p>Corre dentro de la transaccion de la estructura (GeneracionUnReporteServiceImpl#generarUno), justo despues de su
 * generador: la G51 y la G45 se arman desde la G46 de la misma ejecucion, asi que heredan lo que se sustituyo en la G46,
 * y la validacion previa (C39) ve lo mismo que se presenta.</p>
 */
@Stateless
public class SustitucionReporteGServiceImpl implements SustitucionReporteGService {

    @PersistenceContext
    EntityManager em;

    @EJB private PrestamoDaoService         prestamoDao;
    @EJB private GeneracionG46Service       g46Generacion;
    @EJB private NuevoPrestamoG46DaoService cg46DaoService;
    @EJB private CancelacionG49DaoService   cg49DaoService;

    @Override
    public long aplicar(DetalleEjecucionReporte detalle) throws Throwable {
        String estructura = detalle.getTipoReporte();
        if (!"G46".equals(estructura) && !"G48".equals(estructura) && !"G49".equals(estructura)) {
            return 0L;
        }
        long periodo = detalle.getEjecucionReporte().getAnio() * 100 + detalle.getEjecucionReporte().getMes();
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
                "SELECT SSTGACCN, SSTGNMOP, SSTGOPRP, SSTGOPSJ, SSTGFCHA, SSTGFRMA, SSTGDCSN FROM RPR.SSTG"
              + " WHERE SSTGPRDO = ?1 AND SSTGESTR = ?2 AND SSTGESTD = 1 ORDER BY SSTGCDGO")
                .setParameter(1, periodo).setParameter(2, estructura).getResultList();
        if (filas.isEmpty()) {
            return 0L;
        }
        em.flush();   // lo que el generador acaba de grabar, visible para los UPDATE nativos

        long agregadas = 0L;
        for (Object[] f : filas) {
            String accion = (String) f[0], nmop = (String) f[1], oprp = (String) f[2], opsj = (String) f[3];
            LocalDate fecha = f[4] != null ? toLocalDate(f[4]) : null;
            String forma = (String) f[5], decision = (String) f[6];
            String etiqueta = estructura + " " + periodo + " " + accion + " " + nmop + " (" + decision + ")";

            if ("REEMPLAZAR".equals(accion) && ("G46".equals(estructura) || "G48".equals(estructura))) {
                String p = "G46".equals(estructura) ? "CG46" : "CG48";
                String sujeto = opsj != null ? identificacionDe(opsj, etiqueta) : null;
                StringBuilder sql = new StringBuilder("UPDATE RPR.").append(p).append(" SET ").append(p).append("NMOP = :oprp");
                if (sujeto != null) sql.append(", ").append(p).append("TIDS = 'C', ").append(p).append("IDSJ = :sujeto");
                if (fecha != null && "CG46".equals(p)) sql.append(", CG46FCCN = :fecha");
                sql.append(" WHERE ").append(p).append("EJRD = :ejrd AND ").append(p).append("NMOP = :nmop");
                var q = em.createNativeQuery(sql.toString()).setParameter("oprp", oprp)
                        .setParameter("ejrd", detalle.getCodigo()).setParameter("nmop", nmop);
                if (sujeto != null) q.setParameter("sujeto", sujeto);
                if (fecha != null && "CG46".equals(p)) q.setParameter("fecha", java.sql.Date.valueOf(fecha));
                int n = q.executeUpdate();
                if (n != 1) {
                    throw new Exception("Sustitución " + etiqueta + ": se esperaba 1 fila de la operación " + nmop + " y hay " + n);
                }
                System.out.println("SSTG aplicada: " + etiqueta + " -> " + oprp);

            } else if ("AGREGAR".equals(accion) && "G46".equals(estructura)) {
                Prestamo prestamo = prestamoDao.selectByIdAsoprep(Long.valueOf(nmop));
                if (prestamo == null) throw new Exception("Sustitución " + etiqueta + ": no existe el préstamo " + nmop);
                existeYa("CG46", oprp, detalle, etiqueta);
                NuevoPrestamoG46 g46 = g46Generacion.mapear(prestamo, detalle);
                g46.setNumeroOperacion(oprp);
                if (opsj != null) { g46.setTipoIdentificacion("C"); g46.setIdentificacion(identificacionDe(opsj, etiqueta)); }
                if (fecha != null) g46.setFechaConcesion(fecha);
                cg46DaoService.save(g46, null);
                agregadas++;
                System.out.println("SSTG aplicada: " + etiqueta + " -> alta " + oprp + " " + fecha);

            } else if ("AGREGAR".equals(accion) && "G49".equals(estructura)) {
                if (opsj == null || fecha == null || forma == null) {
                    throw new Exception("Sustitución " + etiqueta + ": la G49 necesita sujeto (SSTGOPSJ), fecha y forma");
                }
                String sujeto = identificacionDe(opsj, etiqueta);
                // El grupo 3 de la G49 cancela solo lo que estaba en la G48 anterior y ya no está: si la G48 del mes
                // anterior ya trae la operación sustituida (71148), la G49 la canceló con la fecha de fin de mes y forma
                // N. Se corrige a lo presentado en lugar de duplicarla.
                int corregidas = em.createNativeQuery("UPDATE RPR.CG49 SET CG49TIDS = 'C', CG49IDSJ = :sujeto, CG49FCCN = :fecha, CG49FMCN = :forma"
                        + " WHERE CG49EJRD = :ejrd AND CG49NMOP = :op")
                        .setParameter("sujeto", sujeto).setParameter("fecha", java.sql.Date.valueOf(fecha)).setParameter("forma", forma)
                        .setParameter("ejrd", detalle.getCodigo()).setParameter("op", oprp).executeUpdate();
                if (corregidas > 1) {
                    throw new Exception("Sustitución " + etiqueta + ": la operación " + oprp + " está " + corregidas + " veces en la G49");
                }
                if (corregidas == 0) {
                    CancelacionG49 g49 = new CancelacionG49();
                    g49.setTipoIdentificacion("C");
                    g49.setIdentificacion(sujeto);
                    g49.setNumeroOperacion(oprp);
                    g49.setFechaCancelacion(fecha);
                    g49.setFormaCancelacion(forma);
                    g49.setDetalleEjecucion(detalle);
                    cg49DaoService.save(g49, null);
                    agregadas++;
                }
                System.out.println("SSTG aplicada: " + etiqueta + " -> cancelación " + oprp + " " + fecha + " " + forma
                        + (corregidas == 1 ? " (corrige la del grupo 3)" : " (agregada)"));

            } else {
                throw new Exception("Sustitución " + etiqueta + ": acción no soportada para la estructura");
            }
        }
        return agregadas;
    }

    /** Identificación del sujeto del préstamo con idAsoprep {@code idAsoprep}. */
    private String identificacionDe(String idAsoprep, String etiqueta) throws Exception {
        @SuppressWarnings("unchecked")
        List<Object> r = em.createNativeQuery("SELECT e.ENTDNMID FROM CRD.PRST p JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO WHERE p.PRSTIDAS = ?1")
                .setParameter(1, Long.valueOf(idAsoprep)).getResultList();
        if (r.size() != 1 || r.get(0) == null) {
            throw new Exception("Sustitución " + etiqueta + ": no se encontró el sujeto del préstamo " + idAsoprep);
        }
        return String.valueOf(r.get(0)).trim();
    }

    private void existeYa(String tabla, String op, DetalleEjecucionReporte detalle, String etiqueta) throws Exception {
        Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM RPR." + tabla + " WHERE " + tabla + "EJRD = ?1 AND " + tabla + "NMOP = ?2")
                .setParameter(1, detalle.getCodigo()).setParameter(2, op).getSingleResult();
        if (n.longValue() != 0) {
            throw new Exception("Sustitución " + etiqueta + ": la operación " + op + " ya está en la estructura");
        }
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
        if (o instanceof java.sql.Date) return ((java.sql.Date) o).toLocalDate();
        if (o instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) o).toLocalDate();
        if (o instanceof LocalDate) return (LocalDate) o;
        if (o instanceof java.util.Date) return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
        throw new IllegalArgumentException("Fecha no reconocida: " + o.getClass());
    }
}
