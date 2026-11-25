package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CobroCreditoDaoService;
import com.saa.model.crd.CobroCredito;
import com.saa.rubros.CrdEstadoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CobroCreditoDaoServiceImpl extends EntityDaoImpl<CobroCredito>
        implements CobroCreditoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CobroCredito");
        return new String[]{
            "codigo",
            "entidad",
            "tipoOperacion",
            "estado",
            "cuentaBancaria",
            "referencia",
            "rutaRespaldo",
            "valor",
            "fecha",
            "fechaAfectacion",
            "observacion",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioAprobacion",
            "fechaAprobacion",
            "usuarioRechazo",
            "fechaRechazo",
            "motivoRechazo",
            "usuarioProceso",
            "fechaProceso",
            "asientoTransitorio",
            "asientoDefinitivo"
        };
    }

    @Override
    public List<CobroCredito> selectByEstado(Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEstado de CobroCredito"
                + " - estado: " + estado);
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  c.estado = :estado " +
                " order by c.fechaRegistro asc");
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<CobroCredito> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de CobroCredito"
                + " - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  c.entidad.codigo = :idEntidad " +
                " order by c.fechaRegistro desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public List<CobroCredito> selectByReferencia(String referenciaTrim, Long idCobroExcluido) throws Throwable {
        System.out.println("Ingresa al metodo selectByReferencia de CobroCredito"
                + " - referencia: " + referenciaTrim + " - excluido: " + idCobroExcluido);
        // TRIM(c.referencia), misma comparación que el índice único CRD.UX_CBCR_REFERENCIA —
        // no se recorta acá: referenciaTrim ya viene recortado del llamador (validar()).
        // Un cobro ANULADO libera su referencia (2026-09-01, decisión del usuario) — mismo
        // criterio que el índice (NVL(CBCRESTD,0)=5 => NULL, fuera del choque de unicidad).
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  TRIM(c.referencia) = :referenciaTrim " +
                " and    (:idCobroExcluido is null or c.codigo <> :idCobroExcluido)" +
                " and    c.estado <> :estadoAnulado");
        query.setParameter("referenciaTrim", referenciaTrim);
        query.setParameter("idCobroExcluido", idCobroExcluido);
        query.setParameter("estadoAnulado", Long.valueOf(CrdEstadoCobro.ANULADO));
        return query.getResultList();
    }

    @Override
    public List<CobroCredito> selectByRangoFechaCobro(LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectByRangoFechaCobro de CobroCredito"
                + " - desde: " + desde + " - hasta: " + hasta);
        // JOIN FETCH de entidad/cuentaBancaria/los 3 asientos: la pantalla de seguimiento
        // muestra las cinco en cada fila y un mes puede traer cientos de cobros — sin esto,
        // cada fila dispara sus propias consultas al recorrer la lista (N+1). Ninguna lleva
        // alias: la especificación JPA NO permite aliasear un join fetch
        // ("The JPA specification does not permit specifying an alias for fetch joins"),
        // Hibernate 6 lo rechaza en tiempo de ejecución (compila igual, es un string) — NO
        // vuelvas a aliasear ninguna de estas para encadenar un fetch de segundo nivel.
        // ⚠️ Residuo conocido: el servicio lee además cuentaBancaria.banco (texto "{banco} -
        // {numeroCuenta}"). Como no se puede alias-ear cuentaBancaria, ese segundo nivel NO se
        // puede traer en este mismo fetch. CuentaBancaria.banco es @ManyToOne sin fetch
        // explícito (EAGER por defecto), así que Hibernate lo va a cargar igual — con toda
        // probabilidad un SELECT aparte por cada BANCOCDGO distinto entre los cobros del rango,
        // no uno por fila. Es un N+1 acotado por bancos distintos (normalmente unos pocos), no
        // por cantidad de cobros, y es el precio de que la especificación no permita el fetch
        // anidado sin alias. No se resuelve acá: no inventar un @EntityGraph ni otro mecanismo
        // sin que el equipo lo decida.
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " left join fetch c.entidad " +
                " left join fetch c.cuentaBancaria " +
                " left join fetch c.asientoTransitorio " +
                " left join fetch c.asientoReparto " +
                " left join fetch c.asientoDefinitivo " +
                " where  c.fecha between :desde and :hasta " +
                " order by c.fecha desc, c.codigo desc");
        query.setParameter("desde", desde);
        query.setParameter("hasta", hasta);
        return query.getResultList();
    }

    @Override
    public void bloquearParaProcesar(Long idCobro) throws Throwable {
        System.out.println("CobroCreditoDaoServiceImpl.bloquearParaProcesar - cobro: " + idCobro);
        // Sin catch general: un bloqueo que traga su excepción es un bloqueo que no existe
        // (mismo criterio que AporteDaoServiceImpl.bloquearAportesEntidad). NOWAIT y no WAIT:
        // procesar un cobro puede tardar minutos, así que la segunda llamada no debe quedar
        // colgada: se le responde de inmediato que el cobro está en proceso.
        Query query = em.createNativeQuery(
                " SELECT CBCRCDGO FROM CRD.CBCR WHERE CBCRCDGO = :id FOR UPDATE NOWAIT");
        query.setParameter("id", idCobro);
        List<Object> filas;
        try {
            filas = query.getResultList();
        } catch (Exception e) {
            if (contieneCodigoOra(e, "ORA-00054")) {
                throw new IncomeException("COBRO_EN_PROCESO: el cobro " + idCobro + " ya se está procesando"
                        + " en otra sesión; espere a que termine y revise su estado antes de reintentar");
            }
            throw e;
        }
        if (filas == null || filas.isEmpty()) {
            throw new IncomeException("No existe el cobro " + idCobro);
        }
    }

    /** Busca un código ORA- en la cadena de causas (Hibernate envuelve la SQLException). */
    private static boolean contieneCodigoOra(Throwable e, String codigo) {
        Throwable actual = e;
        while (actual != null) {
            if (actual.getMessage() != null && actual.getMessage().contains(codigo)) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    @Override
    public List<CobroCredito> selectSinReferenciaCoincidentes(Long idEntidad, Long idCuentaBancaria,
            LocalDate fecha, Double valor, Long idCobroExcluido) throws Throwable {
        System.out.println("Ingresa al metodo selectSinReferenciaCoincidentes de CobroCredito - entidad: "
                + idEntidad + " - cuenta: " + idCuentaBancaria + " - fecha: " + fecha + " - valor: " + valor);
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  c.entidad.codigo = :idEntidad " +
                " and    c.cuentaBancaria.codigo = :idCuenta " +
                " and    c.fecha = :fecha " +
                " and    c.valor between :valorMin and :valorMax " +
                " and    TRIM(c.referencia) in ('9', '09') " +
                " and    (:idCobroExcluido is null or c.codigo <> :idCobroExcluido)" +
                " and    c.estado <> :estadoAnulado" +
                " order by c.codigo");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("idCuenta", idCuentaBancaria);
        query.setParameter("fecha", fecha);
        query.setParameter("valorMin", valor - 0.005);
        query.setParameter("valorMax", valor + 0.005);
        query.setParameter("idCobroExcluido", idCobroExcluido);
        query.setParameter("estadoAnulado", Long.valueOf(CrdEstadoCobro.ANULADO));
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectEventosSinCobroCoincidentes(Long idPrestamo, LocalDate fecha, Double valor)
            throws Throwable {
        System.out.println("Ingresa al metodo selectEventosSinCobroCoincidentes - prestamo: " + idPrestamo
                + " - fecha: " + fecha + " - valor: " + valor);
        // EVPRESTD = 1 vigente; 0 = anulado (ProcesoPagoPrestamoServiceImpl.anularOperacion).
        Query query = em.createNativeQuery(
                " SELECT e.EVPRCDGO, e.EVPRTPOO FROM CRD.EVPR e " +
                " WHERE  e.PRSTCDGO = :prestamo " +
                " AND    e.EVPRESTD = 1 " +
                " AND    e.EVPRFCHA >= :desde AND e.EVPRFCHA < :hasta " +
                " AND    ABS(e.EVPRVLRR - :valor) < 0.005 " +
                " AND    NOT EXISTS (SELECT 1 FROM CRD.DCBC d WHERE d.EVPRCDGO = e.EVPRCDGO) " +
                " ORDER BY e.EVPRCDGO");
        query.setParameter("prestamo", idPrestamo);
        query.setParameter("desde", java.sql.Timestamp.valueOf(fecha.atStartOfDay()));
        query.setParameter("hasta", java.sql.Timestamp.valueOf(fecha.plusDays(1).atStartOfDay()));
        query.setParameter("valor", valor);
        return query.getResultList();
    }
}
