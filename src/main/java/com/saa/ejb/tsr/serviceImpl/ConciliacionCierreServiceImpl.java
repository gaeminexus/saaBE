package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.cnt.service.PlanCuentaService;
import com.saa.ejb.tsr.dao.ConciliacionContableDaoService;
import com.saa.ejb.tsr.dao.ConciliacionDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.ejb.tsr.dao.DetalleTransitoDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.ejb.tsr.dao.MovimientoBancoDaoService;
import com.saa.ejb.tsr.service.ConciliacionCierreService;
import com.saa.ejb.tsr.service.ConciliacionContableService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.ejb.tsr.service.GrupoConciliacionContableService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.ConciliacionContable;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.DetalleTransito;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.model.tsr.GrupoConciliadoResumen;
import com.saa.model.tsr.MovimientoBanco;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PartidaTransitoAntigua;
import com.saa.model.tsr.PartidaTransitoSolicitud;
import com.saa.model.tsr.PendienteAsientoTransito;
import com.saa.model.tsr.PendienteExtractoTransito;
import com.saa.model.tsr.PreparacionCierreTransito;
import com.saa.model.tsr.ResultadoCierreTransito;
import com.saa.model.scp.Usuario;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCierreConciliacion;
import com.saa.rubros.EstadoPartidaTransito;
import com.saa.rubros.TipoPartidaTransito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * Implementación de ConciliacionCierreService. Ver el javadoc de la interfaz y
 * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md.
 */
@Stateless
public class ConciliacionCierreServiceImpl implements ConciliacionCierreService {

    /** Misma tolerancia que conciliarGrupo. NO configurable - ver el riesgo #2 del diseño. */
    private static final double TOLERANCIA = 0.01;

    private static final int DIAS_ANTIGUEDAD_DEFECTO = 60;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private PeriodoService periodoService;

    @EJB
    private PlanCuentaService planCuentaService;

    @EJB
    private ConciliacionContableDaoService conciliacionContableDaoService;

    @EJB
    private ConciliacionContableService conciliacionContableService;

    @EJB
    private ConciliacionDaoService conciliacionDaoService;

    @EJB
    private GrupoConciliacionContableService grupoConciliacionContableService;

    @EJB
    private GrupoConciliacionExtractoDaoService grupoConciliacionExtractoDaoService;

    @EJB
    private GrupoConciliacionAsientoDaoService grupoConciliacionAsientoDaoService;

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private DetalleTransitoDaoService detalleTransitoDaoService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    // =====================================================================
    // prepararCierre
    // =====================================================================

    @Override
    public PreparacionCierreTransito prepararCierre(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        Periodo periodo = obtenerPeriodo(idPeriodo);

        PreparacionCierreTransito resultado = new PreparacionCierreTransito();
        resultado.setIdCuentaBancaria(idCuentaBancaria);
        resultado.setIdPeriodo(idPeriodo);

        resultado.setConciliadosDelMes(obtenerConciliadosDelMes(idCuentaBancaria, idPeriodo));

        List<DetalleExtractoBancario> pendientesExtracto = grupoConciliacionExtractoDaoService
                .selectPendientes(idCuentaBancaria, idPeriodo);
        List<PendienteExtractoTransito> dtoExtracto = new ArrayList<>();
        double sumaTipo3 = 0.0;
        double sumaTipo4 = 0.0;
        for (DetalleExtractoBancario detalle : pendientesExtracto) {
            double neto = valorNeto(detalle);
            int tipoSugerido = neto >= 0 ? TipoPartidaTransito.NC_BANCO_NO_REGISTRADA
                    : TipoPartidaTransito.ND_BANCO_NO_REGISTRADA;
            if (tipoSugerido == TipoPartidaTransito.NC_BANCO_NO_REGISTRADA) {
                sumaTipo3 += Math.abs(neto);
            } else {
                sumaTipo4 += Math.abs(neto);
            }
            PendienteExtractoTransito dto = new PendienteExtractoTransito();
            dto.setIdDetalleExtracto(detalle.getCodigo());
            dto.setFecha(detalle.getFechaTransaccion());
            dto.setDescripcion(detalle.getDescripcion());
            dto.setValor(Math.abs(neto));
            dto.setEsArrastrada(detalle.isEsArrastrada());
            dto.setTipoSugerido(tipoSugerido);
            dtoExtracto.add(dto);
        }
        resultado.setPendientesExtracto(dtoExtracto);

        List<DetalleAsiento> pendientesAsiento = grupoConciliacionAsientoDaoService.selectPendientes(
                cuenta.getPlanCuenta().getCodigo(), periodo.getEmpresa().getCodigo(),
                periodo.getPrimerDia(), periodo.getUltimoDia());
        List<PendienteAsientoTransito> dtoAsiento = new ArrayList<>();
        double sumaTipo1 = 0.0;
        double sumaTipo2 = 0.0;
        for (DetalleAsiento detalle : pendientesAsiento) {
            PendienteAsientoTransito dto = new PendienteAsientoTransito();
            dto.setIdDetalleAsiento(detalle.getCodigo());
            dto.setIdAsiento(detalle.getAsiento().getCodigo());
            dto.setFecha(detalle.getAsiento().getFechaAsiento());
            dto.setDescripcion(detalle.getDescripcion());
            dto.setEsArrastrada(detalle.isEsArrastrada());

            // El ancla de tipo 1/2 es el propio DetalleAsiento desde el 2026-08-27 (§7bis): toda
            // linea pendiente es declarable, tipoSugerido nunca viene null. MovimientoBanco, si
            // existe, es solo informacion adicional.
            MovimientoBanco movimiento = movimientoDeLaCuenta(detalle.getAsiento().getCodigo(), idCuentaBancaria);
            if (movimiento != null) {
                dto.setIdMovimientoBanco(movimiento.getCodigo());
            }
            double neto = valorNeto(detalle);
            dto.setValor(Math.abs(neto));
            int tipoSugerido = neto >= 0 ? TipoPartidaTransito.DEPOSITO_EN_TRANSITO
                    : TipoPartidaTransito.CHEQUE_GIRADO_NO_COBRADO;
            dto.setTipoSugerido(tipoSugerido);
            if (tipoSugerido == TipoPartidaTransito.DEPOSITO_EN_TRANSITO) {
                sumaTipo1 += Math.abs(neto);
            } else {
                sumaTipo2 += Math.abs(neto);
            }
            dtoAsiento.add(dto);
        }
        resultado.setPendientesAsiento(dtoAsiento);

        // "Libros" es contabilidad, no TSR.MVCB (ver §7bis del diseño: MovimientoBanco cubre
        // 1-5% del movimiento real). saldoCuentaFechaEmpresa ya existe y ya se usa para el
        // mayor auxiliar (MayorAnaliticoServiceImpl) - es la misma fuente para las dos cosas.
        Double saldoLibros = planCuentaService.saldoCuentaFechaEmpresa(
                periodo.getEmpresa().getCodigo(), cuenta.getPlanCuenta().getCodigo(), periodo.getUltimoDia());
        resultado.setSaldoLibros(saldoLibros);

        Double saldoExtractoSugerido = ultimoSaldoExtracto(idCuentaBancaria, idPeriodo);
        resultado.setSaldoExtractoSugerido(saldoExtractoSugerido);

        if (saldoLibros != null && saldoExtractoSugerido != null) {
            double esperado = saldoExtractoEsperado(saldoLibros, sumaTipo1, sumaTipo2, sumaTipo3, sumaTipo4);
            resultado.setDiferenciaSugerida(redondea(esperado - saldoExtractoSugerido));
        }
        return resultado;
    }

    private List<GrupoConciliadoResumen> obtenerConciliadosDelMes(Long idCuentaBancaria, Long idPeriodo)
            throws Throwable {
        ConciliacionContable cabecera = conciliacionContableDaoService.selectByCuentaYPeriodo(idCuentaBancaria, idPeriodo);
        List<GrupoConciliadoResumen> resultado = new ArrayList<>();
        if (cabecera == null) {
            return resultado;
        }
        List<GrupoConciliacionContable> grupos = grupoConciliacionContableService
                .selectActivosByConciliacion(cabecera.getCodigo());
        for (GrupoConciliacionContable grupo : grupos) {
            GrupoConciliadoResumen dto = new GrupoConciliadoResumen();
            dto.setIdGrupo(grupo.getCodigo());
            dto.setValorExtracto(grupo.getValorExtracto());
            dto.setValorAsiento(grupo.getValorAsiento());
            dto.setFechaConciliacion(grupo.getFechaConciliacion());
            dto.setUsuarioConcilia(grupo.getUsuarioConcilia());
            resultado.add(dto);
        }
        return resultado;
    }

    // =====================================================================
    // cerrar
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoCierreTransito cerrar(Long idCuentaBancaria, Long idPeriodo,
            List<PartidaTransitoSolicitud> partidas, Double saldoExtracto, Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo cerrar (transito) con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo + ", partidas: " + (partidas != null ? partidas.size() : 0));

        if (saldoExtracto == null) {
            throw new IncomeException("Debe indicar el saldo segun el estado de cuenta bancario.");
        }
        String usuario = usuarioNombre(idUsuario);
        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        Periodo periodo = obtenerPeriodo(idPeriodo);
        if (controlExtractoBancarioService.estaCerrado(periodo.getEmpresa().getCodigo(), idPeriodo)) {
            throw new IncomeException("El periodo '" + periodo.getNombre()
                    + "' ya esta cerrado para conciliacion bancaria.");
        }
        List<PartidaTransitoSolicitud> lista = partidas != null ? partidas : new ArrayList<>();

        // 1) Validar y resolver cada partida nueva (sin persistir todavia).
        List<DetalleTransito> nuevas = new ArrayList<>();
        Map<Integer, Double> sumasNuevas = new HashMap<>();
        for (PartidaTransitoSolicitud solicitud : lista) {
            DetalleTransito partida = resolverPartidaNueva(solicitud, idCuentaBancaria);
            nuevas.add(partida);
            sumasNuevas.merge(partida.getTipo().intValue(), partida.getValor(), Double::sum);
        }

        // 2) Sumar lo que YA estaba declarado y sigue Pendiente (arrastradas de cierres
        // anteriores que este cierre no toca): tambien cuenta en la ecuacion de este mes.
        Map<Integer, Double> sumasExistentes = sumarPendientesExistentes(idCuentaBancaria);

        double sumaTipo1 = total(sumasNuevas, sumasExistentes, TipoPartidaTransito.DEPOSITO_EN_TRANSITO);
        double sumaTipo2 = total(sumasNuevas, sumasExistentes, TipoPartidaTransito.CHEQUE_GIRADO_NO_COBRADO);
        double sumaTipo3 = total(sumasNuevas, sumasExistentes, TipoPartidaTransito.NC_BANCO_NO_REGISTRADA);
        double sumaTipo4 = total(sumasNuevas, sumasExistentes, TipoPartidaTransito.ND_BANCO_NO_REGISTRADA);

        // "Libros" es contabilidad, no TSR.MVCB - mismo criterio que prepararCierre, ver la
        // nota de arriba y §7bis del diseño.
        Double saldoLibros = planCuentaService.saldoCuentaFechaEmpresa(
                periodo.getEmpresa().getCodigo(), cuenta.getPlanCuenta().getCodigo(), periodo.getUltimoDia());
        if (saldoLibros == null) {
            saldoLibros = 0.0;
        }
        double esperado = saldoExtractoEsperado(saldoLibros, sumaTipo1, sumaTipo2, sumaTipo3, sumaTipo4);
        double diferencia = redondea(esperado - saldoExtracto);
        if (Math.abs(diferencia) > TOLERANCIA) {
            throw new IncomeException(String.format(java.util.Locale.US,
                    "La ecuacion no cuadra: saldo segun libros %.2f, saldo segun extracto %.2f, "
                            + "diferencia %.2f (tolerancia %.2f). Revise las partidas declaradas.",
                    saldoLibros, saldoExtracto, diferencia, TOLERANCIA));
        }

        // 3) Que no quede ningun pendiente sin cubrir (ni conciliado, ni declarado aqui, ni ya
        // declarado por un cierre anterior).
        List<String> sinCubrir = pendientesSinCubrir(idCuentaBancaria, idPeriodo, cuenta, periodo, nuevas);
        if (!sinCubrir.isEmpty()) {
            throw new IncomeException("No se puede cerrar: quedan " + sinCubrir.size()
                    + " movimiento(s) sin conciliar ni declarar en transito: " + sinCubrir);
        }

        // 4) Todo valido: crear el cierre (TSR.CNCL) y las filas nuevas de TSR.DTCN.
        Conciliacion cierre = new Conciliacion();
        cierre.setIdPeriodo(idPeriodo);
        cierre.setCuentaBancaria(cuenta);
        cierre.setEmpresa(periodo.getEmpresa());
        cierre.setFecha(LocalDateTime.now());
        cierre.setFinalSistema(saldoLibros);
        cierre.setSaldoEstadoCuenta(saldoExtracto);
        cierre.setSaldoBanco(saldoExtracto);
        cierre.setDepositoTransito(sumaTipo1);
        cierre.setChequeTransito(sumaTipo2);
        cierre.setCreditoTransito(sumaTipo3);
        cierre.setDebitoTransito(sumaTipo4);
        cierre.setEstadoCierre((long) EstadoCierreConciliacion.CERRADO);
        cierre.setFechaCierre(LocalDateTime.now());
        cierre.setUsuarioCierre(usuario);
        cierre = conciliacionDaoService.save(cierre, cierre.getCodigo());

        for (DetalleTransito partida : nuevas) {
            partida.setCierre(cierre);
            partida.setFechaRegistro(LocalDateTime.now());
            detalleTransitoDaoService.save(partida, partida.getCodigo());
        }

        em.flush();

        ConciliacionContable cabecera = conciliacionContableService.obtenerOCrear(idCuentaBancaria, idPeriodo);
        conciliacionContableService.verificar(cabecera.getCodigo(), usuario);

        ResultadoCierreTransito resultado = new ResultadoCierreTransito();
        resultado.setIdCierre(cierre.getCodigo());
        resultado.setIdCuentaBancaria(idCuentaBancaria);
        resultado.setIdPeriodo(idPeriodo);
        resultado.setSaldoLibros(saldoLibros);
        resultado.setSaldoExtracto(saldoExtracto);
        resultado.setDiferencia(diferencia);
        resultado.setEstado(cierre.getEstadoCierre());
        resultado.setFechaCierre(cierre.getFechaCierre());
        resultado.setUsuarioCierre(usuario);
        resultado.setPartidasDeclaradas(nuevas.size());
        System.out.println("Cierre de transito creado: id=" + cierre.getCodigo() + ", diferencia=" + diferencia);
        return resultado;
    }

    private DetalleTransito resolverPartidaNueva(PartidaTransitoSolicitud solicitud, Long idCuentaBancaria)
            throws Throwable {
        if (solicitud == null || solicitud.getTipo() == null) {
            throw new IncomeException("Cada partida debe indicar su tipo (1 a 4).");
        }
        int tipo = solicitud.getTipo().intValue();
        boolean esLibros = tipo == TipoPartidaTransito.DEPOSITO_EN_TRANSITO
                || tipo == TipoPartidaTransito.CHEQUE_GIRADO_NO_COBRADO;
        boolean esBanco = tipo == TipoPartidaTransito.NC_BANCO_NO_REGISTRADA
                || tipo == TipoPartidaTransito.ND_BANCO_NO_REGISTRADA;
        if (!esLibros && !esBanco) {
            throw new IncomeException("Tipo de partida en transito invalido: " + tipo + ". Debe ser 1, 2, 3 o 4.");
        }

        DetalleTransito partida = new DetalleTransito();
        partida.setTipo(Long.valueOf(tipo));
        partida.setEstado(Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        partida.setObservacion(solicitud.getObservacion());

        // Ancla desde el 2026-08-27 (§7bis): tipo 1/2 cuelga de DetalleAsiento, no de
        // MovimientoBanco - MVCBCDGO pasa a ser informacion adicional opcional, se llena si
        // existe pero nunca se exige.
        if (esLibros) {
            if (solicitud.getIdDetalleAsiento() == null || solicitud.getIdDetalleExtracto() != null) {
                throw new IncomeException("Tipo " + tipo + " requiere idDetalleAsiento (y no idDetalleExtracto).");
            }
            if (detalleTransitoDaoService.selectPendientePorDetalleAsiento(solicitud.getIdDetalleAsiento()) != null) {
                throw new IncomeException("La linea de asiento " + solicitud.getIdDetalleAsiento()
                        + " ya esta declarada en transito por otro cierre.");
            }
            DetalleAsiento detalleAsiento = em.find(DetalleAsiento.class, solicitud.getIdDetalleAsiento());
            if (detalleAsiento == null) {
                throw new IncomeException("No existe la linea de asiento " + solicitud.getIdDetalleAsiento());
            }
            partida.setDetalleAsiento(detalleAsiento);
            partida.setValor(Math.abs(valorNeto(detalleAsiento)));

            MovimientoBanco movimiento = movimientoDeLaCuenta(detalleAsiento.getAsiento().getCodigo(), idCuentaBancaria);
            if (movimiento != null) {
                partida.setMovimientoBanco(movimiento);
            }
        } else {
            if (solicitud.getIdDetalleExtracto() == null || solicitud.getIdDetalleAsiento() != null) {
                throw new IncomeException("Tipo " + tipo + " requiere idDetalleExtracto (y no idDetalleAsiento).");
            }
            if (detalleTransitoDaoService.selectPendientePorDetalleExtracto(solicitud.getIdDetalleExtracto()) != null) {
                throw new IncomeException("La linea de extracto " + solicitud.getIdDetalleExtracto()
                        + " ya esta declarada en transito por otro cierre.");
            }
            DetalleExtractoBancario detalle = em.find(DetalleExtractoBancario.class, solicitud.getIdDetalleExtracto());
            if (detalle == null) {
                throw new IncomeException("No existe la linea de extracto " + solicitud.getIdDetalleExtracto());
            }
            partida.setDetalleExtracto(detalle);
            partida.setValor(Math.abs(valorNeto(detalle)));
        }
        if (partida.getValor() == null || partida.getValor() <= 0) {
            throw new IncomeException("La partida tipo " + tipo + " tiene valor cero: revise el movimiento/linea referenciado.");
        }
        return partida;
    }

    private Map<Integer, Double> sumarPendientesExistentes(Long idCuentaBancaria) throws Throwable {
        Map<Integer, Double> sumas = new HashMap<>();
        for (DetalleTransito partida : detalleTransitoDaoService.selectPendientesPorCuenta(idCuentaBancaria)) {
            sumas.merge(partida.getTipo().intValue(), partida.getValor(), Double::sum);
        }
        return sumas;
    }

    private double total(Map<Integer, Double> nuevas, Map<Integer, Double> existentes, int tipo) {
        double a = nuevas.getOrDefault(tipo, 0.0);
        double b = existentes.getOrDefault(tipo, 0.0);
        return a + b;
    }

    /**
     * Pendientes (extracto + asiento, incluidas arrastradas) que NO están cubiertos ni por una
     * partida ya declarada (Pendiente, de un cierre anterior) ni por una de las partidas nuevas
     * de este cierre.
     */
    private List<String> pendientesSinCubrir(Long idCuentaBancaria, Long idPeriodo, CuentaBancaria cuenta,
            Periodo periodo, List<DetalleTransito> nuevas) throws Throwable {
        java.util.Set<Long> idsAsientoDeclaradosAhora = new java.util.HashSet<>();
        java.util.Set<Long> idsExtractoDeclaradosAhora = new java.util.HashSet<>();
        for (DetalleTransito partida : nuevas) {
            if (partida.getDetalleAsiento() != null) {
                idsAsientoDeclaradosAhora.add(partida.getDetalleAsiento().getCodigo());
            }
            if (partida.getDetalleExtracto() != null) {
                idsExtractoDeclaradosAhora.add(partida.getDetalleExtracto().getCodigo());
            }
        }

        List<String> sinCubrir = new ArrayList<>();
        for (DetalleExtractoBancario detalle : grupoConciliacionExtractoDaoService
                .selectPendientes(idCuentaBancaria, idPeriodo)) {
            if (idsExtractoDeclaradosAhora.contains(detalle.getCodigo())) {
                continue;
            }
            if (detalleTransitoDaoService.selectPendientePorDetalleExtracto(detalle.getCodigo()) == null) {
                sinCubrir.add("Extracto #" + detalle.getCodigo() + " (" + detalle.getDescripcion() + ")");
            }
        }
        // Ancla desde el 2026-08-27 (§7bis): toda linea de asiento pendiente es declarable via
        // DetalleAsiento, ya no hace falta MovimientoBanco para cubrirla.
        for (DetalleAsiento detalle : grupoConciliacionAsientoDaoService.selectPendientes(
                cuenta.getPlanCuenta().getCodigo(), periodo.getEmpresa().getCodigo(),
                periodo.getPrimerDia(), periodo.getUltimoDia())) {
            if (idsAsientoDeclaradosAhora.contains(detalle.getCodigo())) {
                continue;
            }
            if (detalleTransitoDaoService.selectPendientePorDetalleAsiento(detalle.getCodigo()) == null) {
                sinCubrir.add("Asiento #" + detalle.getAsiento().getCodigo() + ", linea " + detalle.getCodigo());
            }
        }
        return sinCubrir;
    }

    // =====================================================================
    // anularCierre
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Conciliacion anularCierre(Long idCierre, String motivo, Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo anularCierre con idCierre: " + idCierre);
        // TSR.CNCL no tiene una columna dedicada a "quien anulo" (no se pidio en el diseño, §5
        // solo agrego motivo/estado/fecha/usuario de CIERRE) - se registra igual que ANTEMTAN
        // en AnticipoEmpleado: el motivo se guarda con el usuario que lo escribio, para no dejar
        // la anulacion sin autor por falta de una columna.
        String usuarioAnula = usuarioNombre(idUsuario);
        String motivoConAutor = (motivo != null ? motivo : "") + " (anulado por " + usuarioAnula + ")";
        Conciliacion cierre = conciliacionDaoService.selectById(idCierre, NombreEntidadesTesoreria.CONCILIACION);
        if (cierre == null) {
            throw new IncomeException("No existe el cierre " + idCierre);
        }
        if (!Long.valueOf(EstadoCierreConciliacion.CERRADO).equals(cierre.getEstadoCierre())) {
            throw new IncomeException("El cierre " + idCierre + " no esta en estado Cerrado (esta "
                    + cierre.getEstadoCierre() + "): no se puede anular.");
        }
        Conciliacion vigente = conciliacionDaoService.selectCierreVigente(
                cierre.getCuentaBancaria().getCodigo(), cierre.getIdPeriodo());
        if (vigente == null || !vigente.getCodigo().equals(idCierre)) {
            throw new IncomeException("Solo se puede anular el ultimo cierre de la cuenta/periodo. El vigente es "
                    + (vigente != null ? "#" + vigente.getCodigo() : "ninguno") + ".");
        }

        List<DetalleTransito> partidas = detalleTransitoDaoService.selectByCierre(idCierre);
        for (DetalleTransito partida : partidas) {
            if (Long.valueOf(EstadoPartidaTransito.SALDADA).equals(partida.getEstado())) {
                throw new IncomeException("No se puede anular: la partida " + partida.getCodigo()
                        + " que declaro este cierre ya fue saldada (se concilio). Deshaga esa conciliacion primero.");
            }
        }
        for (DetalleTransito partida : partidas) {
            detalleTransitoDaoService.remove(new DetalleTransito(), partida.getCodigo());
        }

        cierre.setEstadoCierre((long) EstadoCierreConciliacion.ANULADO);
        cierre.setMotivoAnulacion(motivoConAutor);
        cierre = conciliacionDaoService.save(cierre, cierre.getCodigo());
        System.out.println("Cierre " + idCierre + " anulado. Partidas liberadas: " + partidas.size());
        return cierre;
    }

    // =====================================================================
    // partidasEnTransitoAntiguas
    // =====================================================================

    @Override
    public List<PartidaTransitoAntigua> partidasEnTransitoAntiguas(Long idEmpresa, Integer dias) throws Throwable {
        int umbral = dias != null ? dias.intValue() : DIAS_ANTIGUEDAD_DEFECTO;
        LocalDateTime diasCorte = LocalDateTime.now().minusDays(umbral);
        List<DetalleTransito> partidas = detalleTransitoDaoService.selectPendientesAntiguas(idEmpresa, diasCorte);

        List<PartidaTransitoAntigua> resultado = new ArrayList<>();
        for (DetalleTransito partida : partidas) {
            PartidaTransitoAntigua dto = new PartidaTransitoAntigua();
            dto.setIdPartida(partida.getCodigo());
            dto.setTipo(partida.getTipo() != null ? partida.getTipo().intValue() : null);
            dto.setValor(partida.getValor());
            dto.setDeclaradaEn(partida.getFechaRegistro());
            dto.setObservacion(partida.getObservacion());
            if (partida.getFechaRegistro() != null) {
                dto.setDiasEnTransito(ChronoUnit.DAYS.between(partida.getFechaRegistro(), LocalDateTime.now()));
            }
            CuentaBancaria cuenta = partida.getMovimientoBanco() != null
                    ? partida.getMovimientoBanco().getCuentaBancaria()
                    : (partida.getDetalleExtracto() != null ? partida.getDetalleExtracto().getCuentaBancaria() : null);
            if (cuenta != null) {
                dto.setCuentaBancaria(nombreCuenta(cuenta));
            }
            resultado.add(dto);
        }
        return resultado;
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    /**
     * Confirmada por el usuario el 2026-08-27 (verificó partida por partida y corrigió §3 del
     * diseño con el mismo ejemplo numérico usado aquí durante la implementación):
     *
     * <pre>
     * saldoExtracto = saldoLibros - sumaTipo1(deposito en transito) + sumaTipo2(cheque girado no cobrado)
     *                             + sumaTipo3(NC banco no registrada) - sumaTipo4(ND banco no registrada)
     * </pre>
     *
     * Ejemplo: deposito en transito $100 (tipo1) + cheque girado no cobrado $50 (tipo2), sin
     * NC/ND: libros=1000 -&gt; banco = 1000 - 100 + 50 = 950 (el banco todavia no tiene el
     * deposito, y todavia tiene el dinero del cheque no cobrado -no lo ha debitado-).
     */
    private double saldoExtractoEsperado(double saldoLibros, double sumaTipo1, double sumaTipo2,
            double sumaTipo3, double sumaTipo4) {
        return saldoLibros - sumaTipo1 + sumaTipo2 + sumaTipo3 - sumaTipo4;
    }

    private MovimientoBanco movimientoDeLaCuenta(Long idAsiento, Long idCuentaBancaria) throws Throwable {
        for (MovimientoBanco movimiento : movimientoBancoDaoService.selectByAsiento(idAsiento)) {
            if (movimiento.getCuentaBancaria() != null && idCuentaBancaria.equals(movimiento.getCuentaBancaria().getCodigo())) {
                return movimiento;
            }
        }
        return null;
    }

    private Double ultimoSaldoExtracto(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        Query query = em.createQuery(
                " select d.saldo from DetalleExtractoBancario d "
                        + " where d.cuentaBancaria.codigo = :idCuentaBancaria "
                        + " and d.periodo.codigo = :idPeriodo "
                        + " and d.estado = :estadoActivo "
                        + " order by d.fechaTransaccion desc, d.numeroFila desc ");
        query.setParameter("idCuentaBancaria", idCuentaBancaria);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        query.setMaxResults(1);
        List<?> resultado = query.getResultList();
        return resultado.isEmpty() ? null : (Double) resultado.get(0);
    }

    private double valorNeto(DetalleExtractoBancario detalle) {
        double credito = detalle.getCredito() != null ? detalle.getCredito() : 0.0;
        double debito = detalle.getDebito() != null ? detalle.getDebito() : 0.0;
        return credito - debito;
    }

    private double valorNeto(DetalleAsiento detalle) {
        double debe = detalle.getValorDebe() != null ? detalle.getValorDebe() : 0.0;
        double haber = detalle.getValorHaber() != null ? detalle.getValorHaber() : 0.0;
        return debe - haber;
    }

    private String nombreCuenta(CuentaBancaria cuenta) {
        String banco = cuenta.getBanco() != null ? cuenta.getBanco().getNombre() : "";
        return banco + " - " + cuenta.getNumeroCuenta();
    }

    private double redondea(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Resuelve idUsuario (mismo tipo que usa el resto del sistema) a su nombre, que es lo que
     * se guarda en los campos de texto de este módulo (TSR.CNCL.CNCLUSCR, motivoAnulacion) -
     * mismo criterio que {@code ControlExtractoBancario.usuarioCierre} y el resto de
     * "usuarioX" de este módulo, todos VARCHAR2.
     *
     * @param idUsuario	: Id del usuario (SCP.PJRQ)
     * @return			: Su nombre
     * @throws Throwable	: IncomeException si no existe
     */
    private String usuarioNombre(Long idUsuario) throws Throwable {
        if (idUsuario == null) {
            throw new IncomeException("Debe indicar el usuario.");
        }
        Usuario usuario = em.find(Usuario.class, idUsuario);
        if (usuario == null) {
            throw new IncomeException("No existe el usuario con id " + idUsuario);
        }
        return usuario.getNombre();
    }

    private CuentaBancaria obtenerCuenta(Long idCuentaBancaria) throws Throwable {
        CuentaBancaria cuenta = cuentaBancariaDaoService.recuperaBancoCuenta(idCuentaBancaria);
        if (cuenta == null) {
            throw new IncomeException("No se encontro la cuenta bancaria con id " + idCuentaBancaria);
        }
        return cuenta;
    }

    private Periodo obtenerPeriodo(Long idPeriodo) throws Throwable {
        Periodo periodo = periodoService.selectById(idPeriodo);
        if (periodo == null) {
            throw new IncomeException("No se encontro el periodo contable con id " + idPeriodo);
        }
        return periodo;
    }
}
