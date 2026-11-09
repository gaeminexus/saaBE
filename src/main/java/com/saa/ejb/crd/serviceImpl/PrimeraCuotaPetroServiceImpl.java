package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.PrimeraCuotaPetroService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación de {@link PrimeraCuotaPetroService}. Solo consultas de lectura sobre GNAP y CXPG.
 */
@Stateless
public class PrimeraCuotaPetroServiceImpl implements PrimeraCuotaPetroService {

    /** Estados de GNAP que cuentan como «archivo generado»: 1 GENERADO, 2 ENVIADO, 3 PROCESADO (0 = anulado). */
    private static final List<Long> ESTADOS_GENERADO = List.of(1L, 2L, 3L);

    private static final String[] MESES = { "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
        "septiembre", "octubre", "noviembre", "diciembre" };

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean archivoPetroGenerado(LocalDate fechaInicio, Long codigoFilial) throws Throwable {
        System.out.println("PrimeraCuotaPetroServiceImpl.archivoPetroGenerado " + fechaInicio + " filial " + codigoFilial);
        if (fechaInicio == null || codigoFilial == null) {
            return false;
        }
        Long total = em.createQuery("SELECT COUNT(g) FROM GeneracionArchivoPetro g "
                + "WHERE g.mesPeriodo = :mes AND g.anioPeriodo = :anio AND g.filial.codigo = :filial "
                + "AND g.estado IN :estados", Long.class)
            .setParameter("mes", (long) fechaInicio.getMonthValue())
            .setParameter("anio", (long) fechaInicio.getYear())
            .setParameter("filial", codigoFilial)
            .setParameter("estados", ESTADOS_GENERADO)
            .getSingleResult();
        return total != null && total > 0;
    }

    @Override
    public Boolean primeraCuotaEnMesInicio(LocalDate fechaInicio, Long idEntidad) throws Throwable {
        System.out.println("PrimeraCuotaPetroServiceImpl.primeraCuotaEnMesInicio " + fechaInicio + " entidad " + idEntidad);
        if (fechaInicio == null || idEntidad == null) {
            return Boolean.FALSE;
        }
        // Inicio el último día del mes: no hay días que cobrar en el mes de inicio, rige la regla de siempre.
        if (fechaInicio.getDayOfMonth() == fechaInicio.lengthOfMonth()) {
            return Boolean.FALSE;
        }
        Long filial = codigoFilialDe(idEntidad);
        if (filial == null) {
            System.out.println("  El partícipe " + idEntidad + " no tiene filial: la primera cuota va al mes siguiente");
            return Boolean.FALSE;
        }
        boolean generado = archivoPetroGenerado(fechaInicio, filial);
        System.out.println("  Archivo Petro " + fechaInicio.getMonthValue() + "/" + fechaInicio.getYear() + " filial "
            + filial + (generado ? " YA generado: primera cuota al mes siguiente"
                : " NO generado: primera cuota en el mes de inicio"));
        return generado ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    public void validarAlAprobar(Prestamo prestamo) throws Throwable {
        System.out.println("PrimeraCuotaPetroServiceImpl.validarAlAprobar préstamo "
            + (prestamo != null ? prestamo.getCodigo() : null));
        if (prestamo == null || prestamo.getCodigo() == null || prestamo.getFechaInicio() == null
                || prestamo.getEntidad() == null) {
            return;
        }
        DetallePrestamo primera = primeraCuota(prestamo.getCodigo());
        if (primera == null || primera.getFechaVencimiento() == null) {
            return;
        }
        LocalDate inicio = prestamo.getFechaInicio().toLocalDate();
        LocalDate vence = primera.getFechaVencimiento().toLocalDate();
        if (vence.getYear() != inicio.getYear() || vence.getMonthValue() != inicio.getMonthValue()) {
            return; // la primera cuota no cae en el mes de inicio: nada que validar
        }
        Long filial = codigoFilialDe(prestamo.getEntidad().getCodigo());
        if (filial == null || !archivoPetroGenerado(inicio, filial)) {
            return;
        }
        // Respaldo: la primera cuota (con su número real) sí está en el archivo: préstamos que el archivo tomó (estaban en estado 1 antes de que el archivo dejara de cobrarlos).
        Long enArchivo = em.createQuery("SELECT COUNT(c) FROM CuotaXParticipeGeneracion c "
                + "WHERE c.prestamo.codigo = :prestamo AND c.numeroCuota = :cuota "
                + "AND c.participeDetalleGeneracion.codigoProductoPetro <> 'HS' "
                + "AND c.participeDetalleGeneracion.detalleGeneracionArchivo.generacionArchivoPetro.mesPeriodo = :mes "
                + "AND c.participeDetalleGeneracion.detalleGeneracionArchivo.generacionArchivoPetro.anioPeriodo = :anio "
                + "AND c.participeDetalleGeneracion.detalleGeneracionArchivo.generacionArchivoPetro.filial.codigo = :filial "
                + "AND c.participeDetalleGeneracion.detalleGeneracionArchivo.generacionArchivoPetro.estado IN :estados",
                Long.class)
            .setParameter("prestamo", prestamo.getCodigo())
            .setParameter("cuota", primera.getNumeroCuota() != null ? primera.getNumeroCuota().intValue() : 0)
            .setParameter("mes", (long) inicio.getMonthValue())
            .setParameter("anio", (long) inicio.getYear())
            .setParameter("filial", filial)
            .setParameter("estados", ESTADOS_GENERADO)
            .getSingleResult();
        if (enArchivo != null && enArchivo > 0) {
            return;
        }
        LocalDate siguiente = inicio.plusMonths(1);
        throw new IncomeException("El archivo Petro de " + MESES[inicio.getMonthValue() - 1] + " " + inicio.getYear()
            + " ya se generó; regenere la tabla de amortización (la primera cuota pasará a "
            + MESES[siguiente.getMonthValue() - 1] + " " + siguiente.getYear() + ")");
    }

    /** Primera cuota de la tabla (la de menor número, INCLUIDA la 0 de gracia), o null si no tiene tabla. */
    private DetallePrestamo primeraCuota(Long idPrestamo) {
        List<DetallePrestamo> cuotas = em.createQuery("SELECT d FROM DetallePrestamo d WHERE d.prestamo.codigo = :p "
                + "AND d.numeroCuota >= 0 ORDER BY d.numeroCuota", DetallePrestamo.class)
            .setParameter("p", idPrestamo)
            .setMaxResults(1)
            .getResultList();
        return cuotas.isEmpty() ? null : cuotas.get(0);
    }

    private Long codigoFilialDe(Long idEntidad) {
        if (idEntidad == null) {
            return null;
        }
        Entidad entidad = em.find(Entidad.class, idEntidad);
        return entidad != null && entidad.getFilial() != null ? entidad.getFilial().getCodigo() : null;
    }
}
