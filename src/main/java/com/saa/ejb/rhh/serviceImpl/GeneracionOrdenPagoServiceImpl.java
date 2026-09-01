package com.saa.ejb.rhh.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService;
import com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService;
import com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.tsr.dao.EgresoDaoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.GeneracionOrdenPagoService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.rhh.CuentaBancariaEmpleado;
import com.saa.model.rhh.DetalleFormatoBancario;
import com.saa.model.rhh.DetalleOrdenPagoNomina;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.FormatoArchivoBancario;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.Egreso;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhCampoArchivoBancario;
import com.saa.rubros.RhhEstadoOrdenPago;
import com.saa.rubros.RhhFormatoArchivoMarcacion;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhModoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de GeneracionOrdenPagoService.</p>
 *
 * <h3>El reparto entre cuentas</h3>
 *
 * <p>Un empleado puede dividir su sueldo entre varias cuentas con <code>CBEMPRCN</code>. El
 * reparto se calcula sobre el neto y <b>el residuo se acumula en la cuenta principal</b>: si
 * los porcentajes dan 333,33 + 333,33 + 333,33 sobre un neto de 1.000,00, el centavo que falta
 * va a la principal. Sin ese ajuste la suma del detalle no seria el neto y el asiento de pago
 * no cuadraria contra el rol.</p>
 *
 * <h3>El archivo bancario</h3>
 *
 * <p>El formato es dato: sale de <code>RHH.FMBN</code> y sus campos de <code>RHH.DFMB</code>,
 * espejo de salida de <code>FMRC</code>/<code>DFMR</code>. Si la empresa no tiene formato activo,
 * <code>generarArchivoBancario</code> dice que falta <b>crearlo</b>, no que falte codigo.</p>
 *
 * <h3>El egreso de tesoreria</h3>
 *
 * <p><code>confirmar</code> crea el <code>TSR.EGRS</code> consolidado y lo enlaza en
 * <code>RDPG.EGRSCDGO</code>, que es lo que permite a la conciliacion bancaria casar el pago
 * con el extracto. Va <b>con titular en nulo</b> --legitimo: el archivo bancario sale de
 * <code>DRPG</code>, no del titular, y la base lo admite-- y con el producto de pago tecnico
 * "PAGO DE NOMINA", localizado <b>por su codigo</b> y nunca por id.</p>
 */
@Stateless
public class GeneracionOrdenPagoServiceImpl implements GeneracionOrdenPagoService {

    /** Bandera de cuenta principal del empleado. */
    private static final String SI = "S";

    /** Bandera de detalle no rechazado. */
    private static final String NO = "N";

    /**
     * Codigo del producto de pago con el que se clasifica el egreso de nomina.
     *
     * <p>Es una <b>clave de busqueda</b>, no un valor normativo: no describe ninguna regla
     * de negocio ni cambia con la ley. Lo crea sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql.</p>
     */
    private static final String CODIGO_PRODUCTO_NOMINA = "NOMINA";

    /** Fin de linea del archivo bancario. */
    private static final String SALTO_LINEA = "\r\n";

    /** Lado de relleno por la izquierda. */
    private static final String LADO_IZQUIERDO = "I";

    /** Patron de fecha cuando ni el campo ni el formato lo declaran. */
    private static final String FORMATO_FECHA_POR_DEFECTO = "ddMMyyyy";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private OrdenPagoNominaDaoService ordenPagoNominaDaoService;

    @EJB
    private DetalleOrdenPagoNominaDaoService detalleOrdenPagoNominaDaoService;

    @EJB
    private PeriodoNominaDaoService periodoNominaDaoService;

    @EJB
    private NominaDaoService nominaDaoService;

    @EJB
    private CuentaBancariaEmpleadoDaoService cuentaBancariaEmpleadoDaoService;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @EJB
    private FormatoArchivoBancarioDaoService formatoArchivoBancarioDaoService;

    @EJB
    private DetalleFormatoBancarioDaoService detalleFormatoBancarioDaoService;

    @EJB
    private EgresoDaoService egresoDaoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    @EJB
    private PagoProgramadoDaoService pagoProgramadoDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generar(java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenPagoNomina generar(Long idPeriodoNomina, Long idCuentaBancaria, String usuario,
            Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo generar de generacionOrdenPago service, periodo: "
                + idPeriodoNomina + ", cuenta: " + idCuentaBancaria);

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        exigeEstadoPagable(periodo);

        List<Nomina> nominas = nominaDaoService.selectByPeriodo(idPeriodoNomina);
        if (nominas == null || nominas.isEmpty()) {
            throw new IncomeException("El periodo " + idPeriodoNomina
                    + " no tiene nominas calculadas: no hay nada que pagar.");
        }

        OrdenPagoNomina orden = localizaOrdenReutilizable(idPeriodoNomina);
        if (orden == null) {
            orden = new OrdenPagoNomina();
            orden.setPeriodoNomina(periodo);
            orden.setEmpresa(periodo.getEmpresa());
            orden.setFechaRegistro(LocalDateTime.now());
            orden.setUsuarioRegistro(usuario);
        } else {
            // Regeneracion de una orden todavia no acreditada: se rehace el detalle para que
            // un cambio de cuenta bancaria del empleado quede reflejado.
            detalleOrdenPagoNominaDaoService.eliminaByOrdenPago(orden.getCodigo());
        }

        if (idCuentaBancaria != null) {
            CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuentaBancaria);
            if (cuenta == null) {
                throw new IncomeException("No existe la cuenta bancaria " + idCuentaBancaria + ".");
            }
            orden.setCuentaBancaria(cuenta);
        }
        orden.setNumero(armaNumero(periodo));
        orden.setFechaEmision(LocalDate.now());
        orden.setEstado(Long.valueOf(RhhEstadoOrdenPago.GENERADA));
        // Se siembran para poder grabar la cabecera antes del detalle, igual que hace el motor
        // con NMNA; al final se sobrescriben con los valores reales.
        orden.setTotal(Double.valueOf(0D));
        orden.setNumeroEmpleados(Integer.valueOf(0));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());
        em.flush();

        Double total = Double.valueOf(0D);
        int empleados = 0;
        for (Nomina nomina : nominas) {
            Double neto = nomina.getNetoPagar();
            if (neto == null || neto.doubleValue() <= 0D) {
                // Un neto en cero o negativo no se acredita. El negativo ya lo bloquea el
                // motor; el cero es real --una licencia sin sueldo el mes entero-- y
                // simplemente no genera linea.
                System.out.println("Empleado " + (nomina.getEmpleado() != null
                        ? nomina.getEmpleado().getIdentificacion() : "?")
                        + " con neto " + neto + ": no entra en la orden de pago.");
                continue;
            }
            List<DetalleOrdenPagoNomina> detalles = armaDetalle(orden, nomina, neto, usuario);
            for (DetalleOrdenPagoNomina detalle : detalles) {
                detalleOrdenPagoNominaDaoService.save(detalle, detalle.getCodigo());
                total = RedondeoNomina.suma(total, detalle.getValor());
            }
            empleados++;
        }

        if (empleados == 0) {
            throw new IncomeException("Ningun empleado del periodo " + idPeriodoNomina
                    + " tiene neto por acreditar: no se emite orden de pago.");
        }

        orden.setTotal(total);
        orden.setNumeroEmpleados(Integer.valueOf(empleados));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        if (esHistorico(periodo)) {
            // Mismo interruptor que ContabilizacionNominaServiceImpl: un periodo historico
            // carga datos ya pagados fuera del sistema, no genera contabilidad nueva
            // (contabilizarPago no-opera para el), y por lo mismo no le corresponde pasar
            // por la bandeja de aprobacion de tesoreria: no hay ningun pago real que aprobar.
            System.out.println("Periodo " + idPeriodoNomina + " en modo HISTORICO: la orden de pago"
                    + " no se registra en la bandeja de tesoreria.");
        } else {
            registraPagoEnBandeja(orden, idUsuario);
        }

        System.out.println("Orden de pago " + orden.getCodigo() + " generada por " + total
                + " para " + empleados + " empleado(s).");
        return orden;
    }

    /**
     * Registra el pago consolidado de la orden en la bandeja de aprobacion de tesoreria
     * (frente 2, decision D1 del usuario, 2026-09-01).
     *
     * <p>Sin desglose contable y sin cuenta bancaria de origen: RRHH sigue contabilizando el
     * pago con <code>ContabilizacionNominaService.contabilizarPago</code> y la plantilla
     * <code>CFNMPLPG</code> (eso no se toca), y la bandeja actua solo como control y
     * aprobacion. El pago nace <code>POR_APROBAR</code> porque
     * <code>idCuentaBancariaOrigen</code> viaja en null -es el unico mecanismo,
     * <code>PagoProgramadoServiceImpl</code> decide el estado inicial exclusivamente por eso.</p>
     *
     * <p>Idempotente por (origen, idOrigen): si <code>generar</code> se vuelve a correr sobre
     * una orden que ya tiene un pago vivo en la bandeja (POR_APROBAR, REGISTRADO, EN_ARCHIVO o
     * CONFIRMADO), no se registra un segundo pago para el mismo total.</p>
     *
     * @param orden			: Orden de pago ya guardada, con el total definitivo
     * @param idUsuario		: Id de SCP.PJRQ del usuario que ejecuta, FK real que exige
     *						  registrarPagoDeOrigenExterno — nunca se resuelve por nombre, ver
     *						  el Javadoc de {@link com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generar}
     * @throws Throwable	: IncomeException si la orden no tiene empresa o falta idUsuario
     */
    private void registraPagoEnBandeja(OrdenPagoNomina orden, Long idUsuario) throws Throwable {
        if (tienePagoVivoEnBandeja(orden.getCodigo())) {
            System.out.println("La orden de pago " + orden.getCodigo()
                    + " ya tiene un pago vivo en la bandeja de tesoreria: no se registra otro.");
            return;
        }

        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        if (idEmpresa == null) {
            throw new IncomeException("La orden de pago " + orden.getCodigo() + " no tiene empresa:"
                    + " sin ella no se puede registrar el pago en la bandeja de tesoreria.");
        }
        exigeIdUsuario(idUsuario, "generar");

        // Beneficiario informativo: la orden es un pago consolidado a muchos empleados, no a
        // una sola persona, y sin desglose este registro no genera archivo de transferencias
        // propio (el archivo bancario real ya lo arma generarArchivoBancario() desde
        // RHH.DRPG). El nombre y la identificacion aqui son solo lo que la bandeja muestra.
        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre("Nomina " + orden.getPeriodoNomina().getMes() + "/"
                + orden.getPeriodoNomina().getAnio() + " - " + orden.getNumeroEmpleados() + " empleado(s)");
        beneficiario.setIdentificacion(orden.getNumero());

        Map<String, Object> resultado = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.RHH_NOMINA, orden.getCodigo(), idEmpresa,
                null, orden.getTotal(),
                orden.getFechaEmision() != null ? orden.getFechaEmision().toString() : null,
                beneficiario, null,
                "Pago de nomina " + orden.getNumero(),
                idUsuario, false, orden.getNumero());

        System.out.println("Pago de la orden " + orden.getCodigo() + " registrado en la bandeja"
                + " de tesoreria: idPago=" + resultado.get("pago") + ", estado=" + resultado.get("estado"));
    }

    /**
     * Indica si la orden ya tiene un pago vivo (cualquier estado salvo RECHAZADO o ANULADO) en
     * <code>PGS.PGTR</code> para el origen <code>RHH_NOMINA</code>.
     *
     * <p>No se usa <code>PagoProgramadoDaoService.selectVigentesByOrigen</code> porque esa
     * consulta excluye a proposito <code>POR_APROBAR</code> -no es "vigente" para el resto de
     * los modulos que la usan-, y aqui hace falta detectar tambien el pago recien nacido
     * POR_APROBAR para no duplicarlo en una regeneracion de la orden.</p>
     *
     * @param idOrdenPago	: Codigo de la orden de pago (RHH.RDPG.RDPGCDGO)
     * @return				: true si ya existe un pago que no esta RECHAZADO ni ANULADO
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private boolean tienePagoVivoEnBandeja(Long idOrdenPago) throws Throwable {
        List<PagoProgramado> vivos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen = :idOrigen "
                + "          and p.estado <> :rechazado "
                + "          and p.estado <> :anulado ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA)
                .setParameter("idOrigen", idOrdenPago)
                .setParameter("rechazado", Long.valueOf(EstadoPagoProgramado.RECHAZADO))
                .setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
                .getResultList();
        return !vivos.isEmpty();
    }

    /**
     * Exige que la orden tenga un pago CONFIRMADO en la bandeja de tesoreria antes de dejar
     * contabilizar. Sin esto la bandeja es decorativa: se podria contabilizar un pago que
     * tesoreria nunca aprobo.
     *
     * <p>Un documento origen admite un unico pago vigente a la vez (regla de
     * <code>PagoProgramadoDaoService</code>), asi que basta con el primero de la lista.</p>
     *
     * @param idOrdenPago	: Codigo de la orden de pago
     * @throws Throwable	: IncomeException si no hay pago vigente o no esta CONFIRMADO
     */
    private void exigePagoConfirmadoEnTesoreria(Long idOrdenPago) throws Throwable {
        List<PagoProgramado> vigentes = pagoProgramadoDaoService
                .selectVigentesByOrigen(OrigenPagoExterno.RHH_NOMINA, idOrdenPago);
        if (vigentes == null || vigentes.isEmpty()) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene ningun pago"
                    + " vigente en la bandeja de tesoreria (PGS.PGTR): no se puede contabilizar sin"
                    + " que tesoreria lo apruebe primero.");
        }
        PagoProgramado pago = vigentes.get(0);
        if (pago.getEstado() == null
                || pago.getEstado().intValue() != EstadoPagoProgramado.CONFIRMADO) {
            throw new IncomeException("El pago " + pago.getId() + " de la orden " + idOrdenPago
                    + " esta en estado " + pago.getEstado() + ", no CONFIRMADO ("
                    + EstadoPagoProgramado.CONFIRMADO + "): tesoreria debe aprobarlo y confirmarlo"
                    + " antes de contabilizar el pago de nomina.");
        }
    }

    /**
     * Indica si el periodo esta en modo historico. Mismo interruptor y misma convencion de
     * null que <code>ContabilizacionNominaServiceImpl.esHistorico</code>: un modo nulo se
     * trata como historico, que es el valor que tienen los periodos creados antes de que
     * existiera la columna.
     *
     * @param periodo	: Periodo de nomina
     * @return			: true si el periodo no contabiliza
     */
    private boolean esHistorico(PeriodoNomina periodo) {
        return periodo.getModo() == null
                || Long.valueOf(RhhModoPeriodoNomina.HISTORICO_SIN_CONTABILIZAR).equals(periodo.getModo());
    }

    /**
     * Exige que <code>idUsuario</code> venga informado. NO se resuelve por nombre como
     * respaldo: es un error de integracion del cliente REST y tiene que verse como tal (ver
     * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.2 «El
     * idUsuario» — un <code>selectByNombre</code> sobre el texto de auditoria de RRHH rompia
     * <code>generar()</code> segun por donde se hubiera inicializado la sesion en el frontend).
     *
     * @param idUsuario		: Id de SCP.PJRQ recibido en el payload
     * @param operacion		: Nombre de la operacion, para el mensaje
     * @throws Throwable	: IncomeException si es null
     */
    private void exigeIdUsuario(Long idUsuario, String operacion) throws Throwable {
        if (idUsuario == null) {
            throw new IncomeException("Falta idUsuario para registrar el pago en tesoreria"
                    + " (operacion: " + operacion + ").");
        }
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generarArchivoBancario(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public byte[] generarArchivoBancario(Long idOrdenPago) throws Throwable {
        System.out.println("Ingresa al metodo generarArchivoBancario de generacionOrdenPago service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = recuperaOrden(idOrdenPago);
        if (orden.getEmpresa() == null || orden.getEmpresa().getCodigo() == null) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene empresa:"
                    + " sin ella no se puede resolver el formato del archivo bancario.");
        }

        FormatoArchivoBancario formato = formatoArchivoBancarioDaoService
                .selectActivoByEmpresa(orden.getEmpresa().getCodigo());
        if (formato == null) {
            // Ya no falta codigo: falta el dato. El mensaje lo dice asi a proposito, para que
            // nadie vuelva a buscar el formato dentro del programa.
            throw new IncomeException("La empresa no tiene un formato de archivo bancario activo"
                    + " (RHH.FMBN). Cree el formato del banco con sus campos (RHH.DFMB) y vuelva a"
                    + " intentarlo: el formato es dato, no codigo.");
        }

        List<DetalleFormatoBancario> campos = detalleFormatoBancarioDaoService
                .selectByFormato(formato.getCodigo());
        if (campos == null || campos.isEmpty()) {
            throw new IncomeException("El formato de archivo bancario '" + formato.getNombre()
                    + "' no tiene ningun campo definido (RHH.DFMB): no se sabe que escribir en cada"
                    + " linea.");
        }

        List<DetalleOrdenPagoNomina> detalles = detalleOrdenPagoNominaDaoService
                .selectByOrdenPago(idOrdenPago);
        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene detalle:"
                    + " no hay nada que escribir en el archivo.");
        }

        boolean anchoFijo = Long.valueOf(RhhFormatoArchivoMarcacion.ANCHO_FIJO)
                .equals(formato.getTipoFormato());
        String delimitador = formato.getDelimitador() != null ? formato.getDelimitador() : "";
        Map<Long, String> mapaTipoCuenta = leeMapaTipoCuenta(formato.getMapaTipoCuenta());
        LocalDate hoy = LocalDate.now();

        StringBuilder archivo = new StringBuilder();

        // Cabecera. Nula significa que este banco no la pide, no que falte configurarla.
        if (formato.getPlantillaCabecera() != null && !formato.getPlantillaCabecera().trim().isEmpty()) {
            archivo.append(resuelveMarcadores(formato.getPlantillaCabecera(), orden, formato,
                    detalles.size(), hoy)).append(SALTO_LINEA);
        }

        int secuencial = 0;
        for (DetalleOrdenPagoNomina detalle : detalles) {
            secuencial++;
            StringBuilder linea = new StringBuilder();
            for (int i = 0; i < campos.size(); i++) {
                DetalleFormatoBancario campo = campos.get(i);
                String valor = valorDelCampo(campo, detalle, orden, formato, secuencial,
                        mapaTipoCuenta, hoy);
                if (anchoFijo) {
                    linea.append(rellena(valor, campo));
                } else {
                    if (i > 0) {
                        linea.append(delimitador);
                    }
                    linea.append(valor);
                }
            }
            archivo.append(linea).append(SALTO_LINEA);
        }

        // Pie.
        if (formato.getPlantillaPie() != null && !formato.getPlantillaPie().trim().isEmpty()) {
            archivo.append(resuelveMarcadores(formato.getPlantillaPie(), orden, formato,
                    detalles.size(), hoy)).append(SALTO_LINEA);
        }

        String codificacion = formato.getCodificacion() != null && !formato.getCodificacion().trim().isEmpty()
                ? formato.getCodificacion().trim() : StandardCharsets.UTF_8.name();
        Charset juego;
        try {
            juego = Charset.forName(codificacion);
        } catch (Throwable e) {
            throw new IncomeException("La codificacion '" + codificacion + "' del formato '"
                    + formato.getNombre() + "' no la reconoce esta maquina virtual. Use UTF-8,"
                    + " ISO-8859-1 o windows-1252.");
        }

        System.out.println("Archivo bancario de la orden " + idOrdenPago + " generado con el formato '"
                + formato.getNombre() + "': " + secuencial + " linea(s) de detalle, codificacion "
                + juego.name() + ".");
        return archivo.toString().getBytes(juego);
    }

    /**
     * Resuelve el valor de un campo del detalle segun lo que declara el rubro 224.
     *
     * @param campo				: Definicion del campo
     * @param detalle			: Linea de la orden de pago
     * @param orden				: Orden de pago
     * @param formato			: Formato del archivo
     * @param secuencial		: Numero de linea, base 1
     * @param mapaTipoCuenta	: Mapa del tipo de cuenta al codigo del banco
     * @param hoy				: Fecha de proceso
     * @return					: El valor ya formateado, sin relleno
     * @throws Throwable		: IncomeException si el campo no se reconoce
     */
    private String valorDelCampo(DetalleFormatoBancario campo, DetalleOrdenPagoNomina detalle,
            OrdenPagoNomina orden, FormatoArchivoBancario formato, int secuencial,
            Map<Long, String> mapaTipoCuenta, LocalDate hoy) throws Throwable {

        Long cual = campo.getCampo();
        if (cual == null) {
            throw new IncomeException("El campo de orden " + campo.getOrden() + " del formato '"
                    + formato.getNombre() + "' no dice que dato lleva (DFMBCMPO).");
        }
        int codigo = cual.intValue();

        switch (codigo) {
            case RhhCampoArchivoBancario.SECUENCIAL:
                return String.valueOf(secuencial);
            case RhhCampoArchivoBancario.IDENTIFICACION_DEL_BENEFICIARIO:
                return texto(detalle.getIdentificacion());
            case RhhCampoArchivoBancario.NOMBRE_DEL_BENEFICIARIO:
                return texto(detalle.getNombreBeneficiario());
            case RhhCampoArchivoBancario.NUMERO_DE_CUENTA:
                return texto(detalle.getNumeroCuenta());
            case RhhCampoArchivoBancario.TIPO_DE_CUENTA:
                return codigoTipoCuenta(detalle.getTipoCuenta(), mapaTipoCuenta);
            case RhhCampoArchivoBancario.CODIGO_DEL_BANCO:
                // Sale del snapshot, que guarda el NOMBRE del banco: TSR.BNCO no tiene codigo
                // de institucion. Ver la nota de la clase.
                return texto(detalle.getBanco());
            case RhhCampoArchivoBancario.VALOR:
                return importe(detalle.getValor(), campo);
            case RhhCampoArchivoBancario.MONEDA:
                // El sistema opera en una sola moneda; el literal lo pone el formato.
                return texto(campo.getValorFijo());
            case RhhCampoArchivoBancario.REFERENCIA:
                return texto(orden.getNumero());
            case RhhCampoArchivoBancario.FECHA_DE_PROCESO:
                return fecha(hoy, campo, formato);
            case RhhCampoArchivoBancario.LITERAL_FIJO:
                return texto(campo.getValorFijo());
            default:
                throw new IncomeException("El campo " + codigo + " del formato '" + formato.getNombre()
                        + "' no corresponde a ningun detalle del rubro 224.");
        }
    }

    /**
     * Sustituye los marcadores de la plantilla de cabecera o de pie.
     *
     * @param plantilla	: Plantilla con marcadores
     * @param orden		: Orden de pago
     * @param formato	: Formato del archivo
     * @param contador	: Numero de lineas de detalle
     * @param hoy		: Fecha de proceso
     * @return			: La linea resuelta
     */
    private String resuelveMarcadores(String plantilla, OrdenPagoNomina orden,
            FormatoArchivoBancario formato, int contador, LocalDate hoy) {
        String patron = formato.getFormatoFecha() != null && !formato.getFormatoFecha().trim().isEmpty()
                ? formato.getFormatoFecha().trim() : FORMATO_FECHA_POR_DEFECTO;
        String resultado = plantilla;
        resultado = resultado.replace("{FECHA}", hoy.format(DateTimeFormatter.ofPattern(patron)));
        resultado = resultado.replace("{CONTADOR}", String.valueOf(contador));
        resultado = resultado.replace("{TOTAL}", orden.getTotal() != null
                ? RedondeoNomina.redondea(orden.getTotal()).toString() : "0");
        resultado = resultado.replace("{EMPRESA}", orden.getEmpresa() != null
                && orden.getEmpresa().getNombre() != null ? orden.getEmpresa().getNombre() : "");
        resultado = resultado.replace("{SECUENCIAL}", texto(orden.getNumero()));
        return resultado;
    }

    /**
     * Lee el mapa <code>alternoRubro199=codigoBanco</code> separado por punto y coma.
     *
     * @param crudo	: Contenido de FMBNMPTC
     * @return		: Mapa del tipo de cuenta al codigo del banco
     */
    private Map<Long, String> leeMapaTipoCuenta(String crudo) {
        Map<Long, String> mapa = new LinkedHashMap<Long, String>();
        if (crudo == null || crudo.trim().isEmpty()) {
            return mapa;
        }
        for (String pareja : crudo.split(";")) {
            String[] partes = pareja.split("=", 2);
            if (partes.length != 2 || partes[0].trim().isEmpty()) {
                continue;
            }
            try {
                mapa.put(Long.valueOf(partes[0].trim()), partes[1].trim());
            } catch (NumberFormatException e) {
                // Una pareja mal escrita se ignora en vez de abortar el archivo: el efecto
                // visible es que ese tipo de cuenta sale con su codigo alterno, que es
                // diagnosticable, y no que la orden entera deje de generarse.
                System.out.println("Pareja invalida en FMBNMPTC: '" + pareja + "', se ignora.");
            }
        }
        return mapa;
    }

    /**
     * Traduce el tipo de cuenta al codigo que espera el banco.
     *
     * @param tipoCuenta	: Detalle del rubro 199 grabado en el snapshot
     * @param mapa			: Mapa del formato
     * @return				: El codigo del banco, o el alterno si el mapa no lo cubre
     */
    private String codigoTipoCuenta(Long tipoCuenta, Map<Long, String> mapa) {
        if (tipoCuenta == null) {
            return "";
        }
        String codigo = mapa.get(tipoCuenta);
        return codigo != null ? codigo : tipoCuenta.toString();
    }

    /**
     * Formatea un importe segun los decimales y el separador que pide el campo.
     *
     * @param valor	: Importe
     * @param campo	: Definicion del campo
     * @return		: El importe como texto
     */
    private String importe(Double valor, DetalleFormatoBancario campo) {
        double v = valor != null ? valor.doubleValue() : 0D;
        int decimales = campo.getDecimales() != null ? campo.getDecimales().intValue() : 2;
        boolean conSeparador = SI.equals(campo.getIncluyeSeparadorDecimal());

        BigDecimal redondeado = BigDecimal.valueOf(v).setScale(decimales, RoundingMode.HALF_UP);
        if (conSeparador) {
            return redondeado.toPlainString();
        }
        // Sin separador: el importe va en unidades minimas --centavos corridos--, que es como
        // lo piden casi todos los formatos de acreditacion masiva.
        return redondeado.movePointRight(decimales).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Formatea una fecha con el patron del campo, o el del formato si el campo no trae uno.
     *
     * @param valor		: Fecha
     * @param campo		: Definicion del campo
     * @param formato	: Formato del archivo
     * @return			: La fecha como texto
     */
    private String fecha(LocalDate valor, DetalleFormatoBancario campo, FormatoArchivoBancario formato) {
        String patron = campo.getFormatoFecha() != null && !campo.getFormatoFecha().trim().isEmpty()
                ? campo.getFormatoFecha().trim()
                : (formato.getFormatoFecha() != null && !formato.getFormatoFecha().trim().isEmpty()
                        ? formato.getFormatoFecha().trim() : FORMATO_FECHA_POR_DEFECTO);
        return valor.format(DateTimeFormatter.ofPattern(patron));
    }

    /**
     * Rellena o recorta el valor a la longitud del campo, en formato de ancho fijo.
     *
     * <p>Un valor mas largo que la longitud se <b>recorta</b>, no se deja pasar: una linea mas
     * larga de lo debido descuadra todas las columnas siguientes y el banco rechaza el archivo
     * entero.</p>
     *
     * @param valor	: Valor ya formateado
     * @param campo	: Definicion del campo
     * @return		: El valor ajustado a la longitud
     */
    private String rellena(String valor, DetalleFormatoBancario campo) {
        if (campo.getLongitud() == null || campo.getLongitud().intValue() <= 0) {
            return valor;
        }
        int longitud = campo.getLongitud().intValue();
        String texto = valor != null ? valor : "";
        if (texto.length() >= longitud) {
            return texto.substring(0, longitud);
        }
        char relleno = campo.getCaracterRelleno() != null && !campo.getCaracterRelleno().isEmpty()
                ? campo.getCaracterRelleno().charAt(0) : ' ';
        StringBuilder paja = new StringBuilder();
        for (int i = texto.length(); i < longitud; i++) {
            paja.append(relleno);
        }
        // I rellena por la izquierda --lo habitual en importes y numeros de cuenta--, D por la
        // derecha, que es lo habitual en nombres.
        return LADO_IZQUIERDO.equals(campo.getLadoRelleno())
                ? paja.toString() + texto : texto + paja.toString();
    }

    /**
     * Devuelve el texto o cadena vacia si es nulo.
     *
     * @param valor	: Texto
     * @return		: El texto o cadena vacia
     */
    private String texto(String valor) {
        return valor != null ? valor : "";
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#confirmar(java.lang.Long, java.time.LocalDate, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenPagoNomina confirmar(Long idOrdenPago, LocalDate fechaAcreditacion, String usuario,
            Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo confirmar de generacionOrdenPago service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = recuperaOrden(idOrdenPago);
        if (orden.getFechaAcreditacion() != null) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " ya se acredito el "
                    + orden.getFechaAcreditacion() + ".");
        }

        if (!esHistorico(orden.getPeriodoNomina())) {
            // Mismo criterio que en generar(): un periodo historico nunca paso por la bandeja,
            // asi que no hay pago que exigir confirmado ni idUsuario que pedir. contabilizarPago
            // tampoco emite asiento para el, mas abajo.
            exigeIdUsuario(idUsuario, "confirmar");
            exigePagoConfirmadoEnTesoreria(idOrdenPago);
        }

        // El asiento y la fecha los graba contabilizarPago, que respeta el interruptor del
        // modo historico. Aqui solo se marca el estado.
        contabilizacionNominaService.contabilizarPago(idOrdenPago, fechaAcreditacion, usuario);

        orden = recuperaOrden(idOrdenPago);
        orden.setEstado(Long.valueOf(RhhEstadoOrdenPago.CONFIRMADA));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        // El egreso va despues del asiento y con la orden ya confirmada: es el enlace que
        // la conciliacion bancaria necesita para casar el pago con el extracto.
        creaEgresoConsolidado(orden, usuario);

        System.out.println("Orden de pago " + idOrdenPago + " confirmada.");
        return orden;
    }

    // =====================================================================
    // Piezas
    // =====================================================================

    /**
     * Arma el detalle de un empleado, repartiendo el neto entre sus cuentas activas.
     *
     * @param orden			: Orden de pago
     * @param nomina		: Nomina del empleado
     * @param neto			: Neto a acreditar
     * @param usuario		: Usuario que ejecuta
     * @return				: Lineas de detalle del empleado
     * @throws Throwable	: IncomeException si el empleado no tiene cuenta
     */
    private List<DetalleOrdenPagoNomina> armaDetalle(OrdenPagoNomina orden, Nomina nomina,
            Double neto, String usuario) throws Throwable {

        Empleado empleado = nomina.getEmpleado();
        List<DetalleOrdenPagoNomina> detalles = new ArrayList<DetalleOrdenPagoNomina>();

        List<CuentaBancariaEmpleado> cuentas = cuentaBancariaEmpleadoDaoService
                .selectActivasByEmpleado(empleado.getCodigo());
        if (cuentas == null || cuentas.isEmpty()) {
            throw new IncomeException("El empleado " + empleado.getIdentificacion() + " ("
                    + empleado.getApellidos() + " " + empleado.getNombres() + ") no tiene ninguna"
                    + " cuenta bancaria activa registrada: no se le puede acreditar el neto."
                    + " Registrela en la ficha del empleado y vuelva a generar la orden.");
        }

        // Con una sola cuenta se acredita todo alli, sin mirar el porcentaje: es el caso
        // normal y evita que un CBEMPRCN mal cargado parta un pago que no se reparte.
        if (cuentas.size() == 1) {
            detalles.add(nuevoDetalle(orden, nomina, empleado, cuentas.get(0), neto, usuario));
            return detalles;
        }

        Double acumulado = Double.valueOf(0D);
        int indicePrincipal = 0;
        for (int i = 0; i < cuentas.size(); i++) {
            CuentaBancariaEmpleado cuenta = cuentas.get(i);
            if (SI.equals(cuenta.getPrincipal())) {
                indicePrincipal = i;
            }
            Double porcentaje = cuenta.getPorcentaje();
            Double valor = porcentaje != null
                    ? RedondeoNomina.porcentaje(neto, porcentaje) : Double.valueOf(0D);
            acumulado = RedondeoNomina.suma(acumulado, valor);
            detalles.add(nuevoDetalle(orden, nomina, empleado, cuenta, valor, usuario));
        }

        // El residuo va a la principal. La lista viene ordenada con la principal primero, asi
        // que el indice 0 es el respaldo cuando ninguna esta marcada.
        double residuo = neto.doubleValue() - acumulado.doubleValue();
        if (Math.abs(residuo) > 0D) {
            DetalleOrdenPagoNomina principal = detalles.get(indicePrincipal);
            principal.setValor(RedondeoNomina.redondea(Double.valueOf(
                    principal.getValor().doubleValue() + residuo)));
            System.out.println("Residuo de reparto de " + residuo + " asignado a la cuenta"
                    + " principal de " + empleado.getIdentificacion() + ".");
        }

        return detalles;
    }

    /**
     * Crea una linea de detalle con el snapshot de los datos bancarios.
     *
     * @param orden		: Orden de pago
     * @param nomina	: Nomina del empleado
     * @param empleado	: Empleado beneficiario
     * @param cuenta	: Cuenta a la que se acredita
     * @param valor		: Valor a acreditar
     * @param usuario	: Usuario que ejecuta
     * @return			: La linea de detalle
     */
    private DetalleOrdenPagoNomina nuevoDetalle(OrdenPagoNomina orden, Nomina nomina,
            Empleado empleado, CuentaBancariaEmpleado cuenta, Double valor, String usuario) {

        DetalleOrdenPagoNomina detalle = new DetalleOrdenPagoNomina();
        detalle.setOrdenPagoNomina(orden);
        detalle.setEmpleado(empleado);
        detalle.setNomina(nomina);
        detalle.setCuentaBancariaEmpleado(cuenta);
        detalle.setValor(RedondeoNomina.redondea(valor));

        // Snapshot: se copia ahora y no se relee nunca. Si el empleado cambia de banco el mes
        // que viene, esta orden sigue mostrando a que cuenta se ordeno pagar.
        detalle.setNumeroCuenta(cuenta.getNumeroCuenta());
        detalle.setTipoCuenta(cuenta.getTipoCuenta());
        detalle.setBanco(cuenta.getBanco() != null ? cuenta.getBanco().getNombre() : null);
        detalle.setIdentificacion(cuenta.getIdentificacionTitular() != null
                ? cuenta.getIdentificacionTitular() : empleado.getIdentificacion());
        detalle.setNombreBeneficiario(cuenta.getTitular() != null
                ? cuenta.getTitular()
                : (empleado.getApellidos() + " " + empleado.getNombres()));

        detalle.setRechazado(NO);
        detalle.setEstado(Long.valueOf(Estado.ACTIVO));
        detalle.setFechaRegistro(LocalDateTime.now());
        detalle.setUsuarioRegistro(usuario);
        return detalle;
    }

    /**
     * Localiza una orden del periodo que todavia se pueda rehacer.
     *
     * @param idPeriodoNomina	: Id del periodo
     * @return					: La orden reutilizable, o null si hay que crear una nueva
     * @throws Throwable		: Excepcion
     */
    private OrdenPagoNomina localizaOrdenReutilizable(Long idPeriodoNomina) throws Throwable {
        List<OrdenPagoNomina> pendientes = ordenPagoNominaDaoService
                .selectPendientesByPeriodo(idPeriodoNomina);
        if (pendientes == null || pendientes.isEmpty()) {
            return null;
        }
        return pendientes.get(0);
    }

    /**
     * Numero de la orden: <code>OP-AAAAMM</code>, con el ano y el mes del periodo.
     *
     * @param periodo	: Periodo de nomina
     * @return			: El numero de la orden
     */
    private String armaNumero(PeriodoNomina periodo) {
        return String.format("OP-%04d%02d",
                Integer.valueOf(periodo.getAnio() != null ? periodo.getAnio().intValue() : 0),
                Integer.valueOf(periodo.getMes() != null ? periodo.getMes().intValue() : 0));
    }

    /**
     * Exige que el periodo este en un estado que admita emitir la orden de pago.
     *
     * @param periodo		: Periodo de nomina
     * @throws Throwable	: IncomeException si todavia no se aprobo
     */
    private void exigeEstadoPagable(PeriodoNomina periodo) throws Throwable {
        Long estado = periodo.getEstado();
        boolean pagable = Long.valueOf(RhhEstadoPeriodoNomina.APROBADO).equals(estado)
                || Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(estado)
                || Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(estado);
        if (!pagable) {
            throw new IncomeException("La orden de pago se emite sobre un periodo APROBADO ("
                    + RhhEstadoPeriodoNomina.APROBADO + "), CONTABILIZADO ("
                    + RhhEstadoPeriodoNomina.CONTABILIZADO + ") o PAGADO ("
                    + RhhEstadoPeriodoNomina.PAGADO + "). El periodo esta en estado " + estado
                    + " y no la admite.");
        }
    }

    /**
     * Recupera la orden y falla con mensaje explicito si no existe.
     *
     * @param idOrdenPago	: Id de la orden
     * @return				: La orden
     * @throws Throwable	: IncomeException si no existe
     */
    private OrdenPagoNomina recuperaOrden(Long idOrdenPago) throws Throwable {
        OrdenPagoNomina orden = ordenPagoNominaDaoService.selectById(idOrdenPago,
                NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
        if (orden == null) {
            throw new IncomeException("No existe la orden de pago " + idOrdenPago + ".");
        }
        return orden;
    }

    /**
     * Recupera el periodo y falla con mensaje explicito si no existe.
     *
     * @param idPeriodoNomina	: Id del periodo
     * @return					: El periodo
     * @throws Throwable		: IncomeException si no existe
     */
    private PeriodoNomina recuperaPeriodo(Long idPeriodoNomina) throws Throwable {
        PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
                NombreEntidadesRhh.PERIODO_NOMINA);
        if (periodo == null) {
            throw new IncomeException("No existe el periodo de nomina " + idPeriodoNomina + ".");
        }
        return periodo;
    }
    /**
     * Crea el egreso de tesoreria consolidado de la orden y lo enlaza en RDPGEGRSCDGO.
     *
     * <p>Es lo que permite que la conciliacion bancaria case el pago de la nomina con el
     * extracto. Se crea despues del asiento, con el asiento ya enlazado.</p>
     *
     * <p><b>Titular en nulo, a proposito.</b> El Javadoc de <code>Egreso</code> lo declara
     * opcional --obligatorio solo cuando el archivo bancario sale del titular, y el de la
     * nomina sale de <code>DRPG</code>-- y la base lo confirma: <code>EGRSTTLR</code> admite
     * nulo. Los empleados no son titulares de tesoreria y <code>RHH.MPLD</code> y
     * <code>CRD.ENTD</code> siguen separados por decision del maestro.</p>
     *
     * <p><b>El producto se localiza por su codigo, nunca por id.</b> <code>PGS.PRDP.ID</code>
     * es IDENTITY y cambia entre instalaciones: fijar un numero aqui funcionaria en esta base
     * y clasificaria el gasto en el producto equivocado en la siguiente. Si no existe, se lanza
     * indicando que falta ejecutar el script 15 en vez de crear la fila al vuelo: crear
     * catalogos desde un proceso de negocio es como aparecen los duplicados.</p>
     *
     * @param orden			: Orden de pago ya contabilizada
     * @param usuario		: Usuario que ejecuta
     * @throws Throwable	: IncomeException si falta el producto del script 15
     */
    private void creaEgresoConsolidado(OrdenPagoNomina orden, String usuario) throws Throwable {
        if (orden.getEgreso() != null) {
            // Ya se creo en un intento anterior: no se duplica.
            return;
        }
        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        ProductoPago producto = localizaProductoNomina(idEmpresa);

        Egreso egreso = new Egreso();
        egreso.setEmpresa(orden.getEmpresa());
        egreso.setTitular(null);
        egreso.setProducto(producto);
        egreso.setDescripcion("Pago de nomina " + orden.getNumero());
        egreso.setValor(orden.getTotal());
        egreso.setFecha(orden.getFechaAcreditacion());
        egreso.setDebitoAutomatico(Long.valueOf(0L));
        egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PAGADO));
        egreso.setObservacion("Egreso consolidado de la orden de pago de nomina "
                + orden.getCodigo() + ", generado desde RRHH.");
        egreso.setFechaRegistro(LocalDateTime.now());
        egreso = egresoDaoService.save(egreso, egreso.getId());
        em.flush();

        orden.setEgreso(egreso.getId());
        ordenPagoNominaDaoService.save(orden, orden.getCodigo());
        System.out.println("Egreso de tesoreria " + egreso.getId() + " creado para la orden "
                + orden.getCodigo() + ".");
    }

    /**
     * Localiza el producto de pago de nomina por su codigo dentro de la empresa.
     *
     * @param idEmpresa		: Id de la empresa
     * @return				: El producto
     * @throws Throwable	: IncomeException si no existe
     */
    @SuppressWarnings("unchecked")
    private ProductoPago localizaProductoNomina(Long idEmpresa) throws Throwable {
        List<ProductoPago> lista = em.createQuery(" select   t "
                + " from     ProductoPago t "
                + " where    t.codigo = :codigo "
                + "          and t.empresa.codigo = :idEmpresa "
                + " order by t.id ")
                .setParameter("codigo", CODIGO_PRODUCTO_NOMINA)
                .setParameter("idEmpresa", idEmpresa)
                .getResultList();
        if (lista.isEmpty()) {
            throw new IncomeException("No existe el producto de pago con codigo '"
                    + CODIGO_PRODUCTO_NOMINA + "' para la empresa " + idEmpresa
                    + ". Ejecute sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql: TSR.EGRS.EGRSPRDP es"
                    + " obligatorio y el egreso consolidado de la nomina no se puede crear sin el.");
        }
        return lista.get(0);
    }

}
