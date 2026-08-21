package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.DetalleUtilidadDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.UtilidadDaoService;
import com.saa.ejb.rhh.service.CalculoUtilidadesService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ConfiguracionNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.DetalleUtilidad;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.Utilidad;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.RhhTipoAcumulado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de CalculoUtilidadesService.</p>
 *
 * <h3>El reparto, paso a paso</h3>
 *
 * <ol>
 *   <li><code>baseTotal = utilidadContable x PRNMUTPR / 100</code>.</li>
 *   <li>De esa base, <code>PRNMUTDI</code> por ciento se reparte <b>por dias trabajados</b> y
 *       <code>PRNMUTCG</code> por ciento <b>por cargas familiares</b>.</li>
 *   <li>Los coeficientes son <code>basePorDias / totalDias</code> y
 *       <code>basePorCargas / totalCargas</code>.</li>
 *   <li>Cada empleado recibe <code>dias x coeficienteDia + cargas x coeficienteCarga</code>.</li>
 *   <li>Lo que pase de <code>PRNMUTSB x SBU</code> es excedente y <b>se transfiere al IESS</b>,
 *       no se reparte entre los demas.</li>
 * </ol>
 *
 * <h3>Dos divisiones que hay que proteger</h3>
 *
 * <p>Si no hay dias trabajados o no hay cargas familiares en toda la empresa, el coeficiente
 * correspondiente queda en cero en vez de dividir por cero. El caso de las cargas es real y
 * frecuente: una empresa donde nadie declara cargas reparte el cien por cien por dias, y la
 * parte por cargas simplemente no tiene a quien ir.</p>
 *
 * <h3>Es ingreso gravado de IR pero no materia gravada del IESS</h3>
 *
 * <p>Por eso <code>DTUTRTIR</code> existe y no hay ninguna columna de aporte: las utilidades no
 * entran en la planilla. La retencion se deja en cero aqui y la calcula la reproyeccion de IR
 * del ejercicio, que es donde vive esa logica.</p>
 */
@Stateless
public class CalculoUtilidadesServiceImpl implements CalculoUtilidadesService {

    /** Bandera afirmativa. */
    private static final String SI = "S";

    /** Base porcentual. */
    private static final double CIEN = 100D;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private UtilidadDaoService utilidadDaoService;

    @EJB
    private DetalleUtilidadDaoService detalleUtilidadDaoService;

    @EJB
    private ConfiguracionNominaDaoService configuracionNominaDaoService;

    @EJB
    private ParametroNominaDaoService parametroNominaDaoService;

    @EJB
    private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

    @EJB
    private AcumuladoNominaDaoService acumuladoNominaDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.CalculoUtilidadesService#calcular(java.lang.Long, java.lang.Integer, java.lang.Double, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Utilidad calcular(Long idEmpresa, Integer anio, Double utilidadContable, String usuario)
            throws Throwable {
        System.out.println("Ingresa al metodo calcular de calculoUtilidades service, empresa: "
                + idEmpresa + ", anio: " + anio);

        if (idEmpresa == null || anio == null) {
            throw new IncomeException("El reparto de utilidades exige la empresa y el ejercicio.");
        }
        if (utilidadContable == null || utilidadContable.doubleValue() <= 0D) {
            throw new IncomeException("La utilidad contable del ejercicio debe ser mayor que cero.");
        }

        ConfiguracionNomina configuracion = configuracionNominaDaoService.selectByEmpresa(idEmpresa);
        if (configuracion == null) {
            throw new IncomeException("No existe configuracion de nomina (RHH.CFNM) para la empresa "
                    + idEmpresa + ".");
        }
        if (!SI.equals(configuracion.getAplicaUtilidades())) {
            // El servicio existe completo; lo que decide si se usa es un dato. Mismo patron
            // que ProvisionActuarialService con CFNMAPJP y CFNMAPDS.
            throw new IncomeException("La empresa no reparte utilidades: RHH.CFNM.CFNMAPUT esta en"
                    + " 'N'. El calculo esta implementado y se activa poniendo la bandera en 'S'.");
        }

        ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
        if (prnm == null) {
            throw new IncomeException("No existen parametros de nomina (RHH.PRNM) para el anio "
                    + anio + ": sin ellos no se puede repartir.");
        }
        exigeParametros(prnm, anio);

        // --- Las tres bases -----------------------------------------------------------
        Double baseTotal = RedondeoNomina.porcentaje(utilidadContable, prnm.getUtilidadPorcentaje());
        Double basePorDias = RedondeoNomina.redondea(Double.valueOf(
                utilidadContable.doubleValue() * prnm.getUtilidadDias().doubleValue() / CIEN));
        Double basePorCargas = RedondeoNomina.redondea(Double.valueOf(
                utilidadContable.doubleValue() * prnm.getUtilidadCargas().doubleValue() / CIEN));

        // --- Los participes -----------------------------------------------------------
        List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
                idEmpresa, java.time.LocalDate.of(anio.intValue(), 1, 1),
                java.time.LocalDate.of(anio.intValue(), 12, 31));
        if (contratos == null || contratos.isEmpty()) {
            throw new IncomeException("No hay contratos activos en " + anio
                    + ": no hay entre quienes repartir.");
        }

        List<Participe> participes = new ArrayList<Participe>();
        Double totalDias = Double.valueOf(0D);
        int totalCargas = 0;
        for (ContratoEmpleado contrato : contratos) {
            Empleado empleado = contrato.getEmpleado();
            if (empleado == null) {
                continue;
            }
            Double dias = acumuladoNominaDaoService.sumaValor(empleado.getCodigo(), anio,
                    Long.valueOf(RhhTipoAcumulado.DIAS_TRABAJADOS), null, null);
            if (dias == null) {
                dias = Double.valueOf(0D);
            }
            int cargas = cuentaCargas(empleado.getCodigo(), anio);

            Participe participe = new Participe();
            participe.empleado = empleado;
            participe.dias = dias;
            participe.cargas = cargas;
            participes.add(participe);

            totalDias = RedondeoNomina.suma(totalDias, dias);
            totalCargas += cargas;
        }

        // --- Los coeficientes ---------------------------------------------------------
        // Protegidas las dos divisiones: sin dias o sin cargas el coeficiente queda en cero.
        // Lo de las cargas es un caso real y frecuente --una empresa donde nadie declara--,
        // y esa parte de la base simplemente no tiene a quien ir.
        Double valorPorDia = totalDias.doubleValue() > 0D
                ? RedondeoNomina.divide(basePorDias, totalDias) : Double.valueOf(0D);
        Double valorPorCarga = totalCargas > 0
                ? RedondeoNomina.divide(basePorCargas, Double.valueOf(totalCargas))
                : Double.valueOf(0D);
        if (totalCargas == 0) {
            System.out.println("Ningun empleado declara cargas familiares en " + anio + ": la parte"
                    + " por cargas (" + basePorCargas + ") queda sin repartir.");
        }

        Double tope = prnm.getUtilidadTopeSbu() != null && prnm.getSbu() != null
                ? RedondeoNomina.redondea(Double.valueOf(
                        prnm.getUtilidadTopeSbu().doubleValue() * prnm.getSbu().doubleValue()))
                : null;

        // --- La cabecera --------------------------------------------------------------
        Utilidad utilidad = utilidadDaoService.selectByEmpresaYAnio(idEmpresa, anio);
        if (utilidad == null) {
            utilidad = new Utilidad();
            utilidad.setEmpresa(em.find(Empresa.class, idEmpresa));
            utilidad.setAnio(anio);
            utilidad.setFechaRegistro(LocalDateTime.now());
            utilidad.setUsuarioRegistro(usuario);
        }
        utilidad.setUtilidadContable(utilidadContable);
        utilidad.setBaseTotal(baseTotal);
        utilidad.setBasePorDias(basePorDias);
        utilidad.setBasePorCargas(basePorCargas);
        utilidad.setTotalDias(totalDias);
        utilidad.setTotalCargas(Integer.valueOf(totalCargas));
        utilidad.setValorPorDia(valorPorDia);
        utilidad.setValorPorCarga(valorPorCarga);
        utilidad.setTopePorTrabajador(tope);
        utilidad.setExcedente(Double.valueOf(0D));
        utilidad.setEstado(Long.valueOf(Estado.ACTIVO));
        utilidad = utilidadDaoService.save(utilidad, utilidad.getCodigo());
        em.flush();

        // --- El detalle ---------------------------------------------------------------
        detalleUtilidadDaoService.eliminaByUtilidad(utilidad.getCodigo());
        Double excedenteTotal = Double.valueOf(0D);
        for (Participe participe : participes) {
            Double porDias = RedondeoNomina.redondea(Double.valueOf(
                    participe.dias.doubleValue() * valorPorDia.doubleValue()));
            Double porCargas = RedondeoNomina.redondea(Double.valueOf(
                    participe.cargas * valorPorCarga.doubleValue()));
            Double total = RedondeoNomina.suma(porDias, porCargas);

            Double excedente = Double.valueOf(0D);
            Double aPagar = total;
            if (tope != null && total.doubleValue() > tope.doubleValue()) {
                excedente = RedondeoNomina.redondea(Double.valueOf(
                        total.doubleValue() - tope.doubleValue()));
                aPagar = tope;
                excedenteTotal = RedondeoNomina.suma(excedenteTotal, excedente);
            }

            DetalleUtilidad detalle = new DetalleUtilidad();
            detalle.setUtilidad(utilidad);
            detalle.setEmpleado(participe.empleado);
            detalle.setDias(participe.dias);
            detalle.setNumeroCargas(Integer.valueOf(participe.cargas));
            detalle.setValorPorDias(porDias);
            detalle.setValorPorCargas(porCargas);
            detalle.setTotal(total);
            detalle.setExcedente(excedente);
            detalle.setValorPagar(aPagar);
            // La retencion la calcula la reproyeccion de IR del ejercicio, que es donde vive
            // esa logica: las utilidades son ingreso gravado de renta, pero NO materia gravada
            // del IESS, y por eso no hay ninguna columna de aporte en esta tabla.
            detalle.setRetencionIr(Double.valueOf(0D));
            detalle.setEstado(Long.valueOf(Estado.ACTIVO));
            detalle.setFechaRegistro(LocalDateTime.now());
            detalle.setUsuarioRegistro(usuario);
            detalleUtilidadDaoService.save(detalle, detalle.getCodigo());
        }

        utilidad.setExcedente(excedenteTotal);
        utilidad = utilidadDaoService.save(utilidad, utilidad.getCodigo());

        System.out.println("Reparto de utilidades de " + anio + ": base " + baseTotal + " entre "
                + participes.size() + " trabajador(es), " + totalDias + " dia(s) y " + totalCargas
                + " carga(s). Excedente al IESS: " + excedenteTotal + ".");
        return utilidad;
    }

    // =====================================================================
    // Piezas
    // =====================================================================

    /**
     * Exige que la parametria del reparto este completa.
     *
     * @param prnm			: Parametros del anio
     * @param anio			: Ejercicio, para el mensaje
     * @throws Throwable	: IncomeException si falta algun porcentaje
     */
    private void exigeParametros(ParametroNomina prnm, Integer anio) throws Throwable {
        if (prnm.getUtilidadPorcentaje() == null || prnm.getUtilidadDias() == null
                || prnm.getUtilidadCargas() == null) {
            throw new IncomeException("La parametria de " + anio + " no tiene completos los"
                    + " porcentajes de utilidades (PRNMUTPR, PRNMUTDI, PRNMUTCG). No se suponen"
                    + " valores: son normativos y viven en RHH.PRNM.");
        }
    }

    /**
     * Cuenta las cargas familiares que califican para utilidades a fin del ejercicio.
     *
     * @param idEmpleado	: Id del empleado
     * @param anio			: Ejercicio
     * @return				: Numero de cargas
     */
    private int cuentaCargas(Long idEmpleado, Integer anio) {
        java.time.LocalDate corte = java.time.LocalDate.of(anio.intValue(), 12, 31);
        Object cuantas = em.createQuery(" select count(t) "
                + " from   CargaFamiliar t "
                + " where  t.empleado.codigo = :idEmpleado "
                + "        and t.calificaUtilidades = 'S' "
                + "        and (t.fechaInicio is null or t.fechaInicio <= :corte) "
                + "        and (t.fechaFin is null or t.fechaFin >= :corte) ")
                .setParameter("idEmpleado", idEmpleado)
                .setParameter("corte", corte)
                .getSingleResult();
        return cuantas != null ? ((Long) cuantas).intValue() : 0;
    }

    /**
     * Datos de un participe mientras se calcula el reparto.
     */
    private static class Participe {
        private Empleado empleado;
        private Double dias;
        private int cargas;
    }
}
