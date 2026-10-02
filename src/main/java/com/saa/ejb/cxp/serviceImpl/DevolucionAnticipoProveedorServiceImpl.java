package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetalleAsientoDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.DevolucionAnticipoProveedorDaoService;
import com.saa.ejb.cxp.service.DevolucionAnticipoProveedorService;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.ejb.tsr.dao.MovimientoBancoDaoService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.DetalleDevolucionAnticipo;
import com.saa.model.cxp.DevolucionAnticipoProveedor;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.MovimientoBanco;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoDevolucionAnticipoProveedor;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación del servicio de devolución del saldo de anticipos de un proveedor —
 * docs/logica-negocio/cxp/API-DEVOLUCION-ANTICIPO-PROVEEDOR.md.
 */
@Stateless
public class DevolucionAnticipoProveedorServiceImpl implements DevolucionAnticipoProveedorService {

    /** Tolerancia de centavos, igual que AplicacionPagoCxpServiceImpl / AnticipoProveedorServiceImpl. */
    private static final double TOLERANCIA = 0.01;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private DevolucionAnticipoProveedorDaoService devolucionDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @EJB
    private DetalleAsientoDaoService detalleAsientoDaoService;

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private GrupoConciliacionAsientoDaoService grupoConciliacionAsientoDaoService;

    // =========================================================================
    // CRUD estándar — copiando AnticipoProveedorServiceImpl
    // =========================================================================

    @Override
    public DevolucionAnticipoProveedor selectById(Long id) throws Throwable {
        return devolucionDaoService.selectById(id, NombreEntidadesPago.DEVOLUCION_ANTICIPO_PROVEEDOR);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        DevolucionAnticipoProveedor entidad = new DevolucionAnticipoProveedor();
        for (Long id : ids) {
            devolucionDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<DevolucionAnticipoProveedor> lista) throws Throwable {
        for (DevolucionAnticipoProveedor reg : lista) {
            devolucionDaoService.save(reg, reg.getCodigo());
        }
    }

    @Override
    public List<DevolucionAnticipoProveedor> selectAll() throws Throwable {
        List<DevolucionAnticipoProveedor> result =
                devolucionDaoService.selectAll(NombreEntidadesPago.DEVOLUCION_ANTICIPO_PROVEEDOR);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron devoluciones de anticipos de proveedores.");
        }
        return result;
    }

    @Override
    public List<DevolucionAnticipoProveedor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        List<DevolucionAnticipoProveedor> result = devolucionDaoService
                .selectByCriteria(datos, NombreEntidadesPago.DEVOLUCION_ANTICIPO_PROVEEDOR);
        if (result.isEmpty()) {
            throw new IncomeException("La búsqueda de devoluciones de anticipos no devolvió registros.");
        }
        return result;
    }

    @Override
    public DevolucionAnticipoProveedor saveSingle(DevolucionAnticipoProveedor entidad) throws Throwable {
        if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
            throw new IncomeException("La devolución debe tener un proveedor asignado.");
        }
        if (entidad.getFecha() == null) {
            throw new IncomeException("La devolución debe tener fecha.");
        }
        if (entidad.getValor() == null || entidad.getValor() <= 0) {
            throw new IncomeException("El valor de la devolución debe ser mayor a cero.");
        }

        boolean esNueva = (entidad.getCodigo() == null);
        if (esNueva) {
            entidad.setEstado(Long.valueOf(EstadoDevolucionAnticipoProveedor.ACTIVA));
            entidad.setFechaRegistro(LocalDateTime.now());
        }

        entidad = devolucionDaoService.save(entidad, entidad.getCodigo());
        System.out.println("✓ DevolucionAnticipoProveedor guardada ID=" + entidad.getCodigo());
        return entidad;
    }

    // =========================================================================
    // registrar — §4.2
    // =========================================================================

    @Override
    public Map<String, Object> registrar(Long idEmpresa, Long idTitular, Long idCuentaBancaria, String fecha,
            String referencia, String observacion, Long idUsuario, List<Map<String, Object>> anticipos)
            throws Throwable {

        System.out.println("=== registrar (devolucion anticipo) | empresa=" + idEmpresa + " | titular=" + idTitular
                + " | cuenta=" + idCuentaBancaria + " | anticipos="
                + ((anticipos != null) ? anticipos.size() : 0) + " ===");

        // 1. anticipos no vacío, sin idAnticipo repetido, cada valor > 0
        if (anticipos == null || anticipos.isEmpty()) {
            throw new IncomeException("Debe indicar al menos un anticipo a devolver.");
        }
        List<Long> idsAnticipo = new ArrayList<>();
        List<Double> valoresAnticipo = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        double total = 0.0;
        for (Map<String, Object> detalle : anticipos) {
            Long idAnticipo = toLong(detalle.get("idAnticipo"));
            Double valor = toDouble(detalle.get("valor"));
            if (idAnticipo == null) {
                throw new IncomeException("Cada línea de la devolución debe indicar el anticipo (idAnticipo).");
            }
            if (valor == null || valor <= 0) {
                throw new IncomeException("El valor a devolver del anticipo " + idAnticipo
                        + " debe ser mayor a cero.");
            }
            if (!vistos.add(idAnticipo)) {
                throw new IncomeException("El anticipo " + idAnticipo + " viene repetido en la devolución. "
                        + "Indique una sola línea por anticipo, con el valor total.");
            }
            idsAnticipo.add(idAnticipo);
            valoresAnticipo.add(valor);
            total += valor;
        }

        // 2. idEmpresa, idTitular, idCuentaBancaria y fecha presentes
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa.");
        }
        if (idTitular == null) {
            throw new IncomeException("Debe indicar el proveedor.");
        }
        if (idCuentaBancaria == null) {
            throw new IncomeException("Debe indicar la cuenta bancaria donde entró el depósito.");
        }
        if (fecha == null || fecha.trim().isEmpty()) {
            throw new IncomeException("Debe indicar la fecha del depósito.");
        }
        LocalDate fechaDeposito = LocalDate.parse(fecha.trim());

        Titular titular = em.find(Titular.class, idTitular);
        if (titular == null) {
            throw new IncomeException("No se encontró el proveedor con ID: " + idTitular);
        }

        // 3. La cuenta bancaria existe y tiene planCuenta (mismo mensaje que
        //    AnticipoProveedorServiceImpl:252-262)
        CuentaBancaria cuentaBancaria = em.find(CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria
                    + ". Verifique la configuración en Tesorería → Cuentas Bancarias.");
        }
        if (cuentaBancaria.getPlanCuenta() == null) {
            throw new IncomeException("La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada. "
                    + "Configure la cuenta contable en Tesorería → Cuentas Bancarias antes de continuar.");
        }

        // 4. Cada anticipo existe, es del mismo titular y empresa, CONFIRMADO, valor <= saldo+0.01
        List<AnticipoProveedor> anticiposEntidad = new ArrayList<>();
        for (int i = 0; i < idsAnticipo.size(); i++) {
            Long idAnticipo = idsAnticipo.get(i);
            Double valor = valoresAnticipo.get(i);
            AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
            if (anticipo == null) {
                throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
            }
            if (anticipo.getTitular() == null || !idTitular.equals(anticipo.getTitular().getCodigo())) {
                throw new IncomeException("El anticipo " + idAnticipo + " no pertenece al proveedor indicado: "
                        + "no se puede devolver en esta operación.");
            }
            if (anticipo.getEmpresa() == null || !idEmpresa.equals(anticipo.getEmpresa().getCodigo())) {
                throw new IncomeException("El anticipo " + idAnticipo + " es de otra empresa contable: "
                        + "no se puede devolver en esta operación.");
            }
            if (anticipo.getEstado() == null
                    || anticipo.getEstado().intValue() != EstadoAnticipoProveedor.CONFIRMADO) {
                throw new IncomeException("El anticipo " + idAnticipo + " no está confirmado: "
                        + "no tiene saldo para devolver.");
            }
            double saldo = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
            if (valor > saldo + TOLERANCIA) {
                throw new IncomeException("El anticipo " + idAnticipo + " ("
                        + nvl(anticipo.getNumeroDoc(), "sin número") + ") tiene un saldo disponible de $"
                        + String.format(Locale.US, "%.2f", saldo) + " y no alcanza para devolver $"
                        + String.format(Locale.US, "%.2f", valor) + ".");
            }
            anticiposEntidad.add(anticipo);
        }

        // 5. El proveedor tiene PRCC tipo 2, con saldoInicial >= total (mismo mensaje que
        //    AplicacionPagoCxpServiceImpl.aplicaCruces, adaptado a "devolver")
        PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idTitular, idEmpresa);
        double saldoAnticipos = (cuentaAnticipos.getSaldoInicial() != null)
                ? cuentaAnticipos.getSaldoInicial() : 0.0;
        if (saldoAnticipos + TOLERANCIA < total) {
            throw new IncomeException("El saldo de anticipos del proveedor '" + titular.getNombre()
                    + "' es de $" + String.format(Locale.US, "%.2f", saldoAnticipos)
                    + " y no alcanza para devolver $" + String.format(Locale.US, "%.2f", total)
                    + ". Revise el cuadre entre los anticipos y la cuenta contable.");
        }

        // ── Todo validado: una sola transacción ──────────────────────────────
        Usuario usuario = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;
        String nombreUsuario = (usuario != null && usuario.getNombre() != null)
                ? usuario.getNombre() : "SISTEMA";

        String observacionAsiento = "Devolución de anticipo: " + titular.getNombre()
                + " | Ref: " + nvl(referencia, "")
                + " | Valor: $" + String.format(Locale.US, "%.2f", total);
        if (observacion != null && !observacion.trim().isEmpty()) {
            observacionAsiento += " | " + observacion.trim();
        }

        // 1. Asiento: DEBE banco / HABER anticipos del proveedor
        Asiento asiento = asientoContableService.generarAsientoDevolucionAnticipoProveedor(
                idTitular, idCuentaBancaria, total, idEmpresa, fechaDeposito, observacionAsiento, nombreUsuario);

        // 2. Cabecera y detalle, estado ACTIVA
        DevolucionAnticipoProveedor devolucion = new DevolucionAnticipoProveedor();
        devolucion.setEmpresa(em.find(Empresa.class, idEmpresa));
        devolucion.setTitular(titular);
        devolucion.setCuentaBancaria(cuentaBancaria);
        devolucion.setFecha(fechaDeposito);
        devolucion.setValor(redondea(total));
        devolucion.setReferencia((referencia != null && !referencia.trim().isEmpty()) ? referencia.trim() : null);
        devolucion.setObservacion(observacion);
        devolucion.setAsiento(asiento);
        devolucion.setEstado(Long.valueOf(EstadoDevolucionAnticipoProveedor.ACTIVA));
        devolucion.setUsuario(usuario);
        devolucion.setFechaRegistro(LocalDateTime.now());
        devolucion = devolucionDaoService.save(devolucion, devolucion.getCodigo());

        for (int i = 0; i < anticiposEntidad.size(); i++) {
            AnticipoProveedor anticipo = anticiposEntidad.get(i);
            Double valor = valoresAnticipo.get(i);

            DetalleDevolucionAnticipo detalle = new DetalleDevolucionAnticipo();
            detalle.setDevolucion(devolucion);
            detalle.setAnticipo(anticipo);
            detalle.setValor(redondea(valor));
            em.persist(detalle);

            // 3. Cada anticipo: saldo -= valor. El estado NO cambia (igual que un cruce).
            double saldoAnterior = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
            anticipo.setSaldo(redondea(saldoAnterior - valor));
            em.merge(anticipo);
        }

        // 4. PRCC tipo 2 del proveedor
        cuentaAnticipos.setSaldoInicial(redondea(saldoAnticipos - total));
        em.merge(cuentaAnticipos);

        // 5. Movimiento bancario, mismo llamado que IngresoServiceImpl:181-185
        movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
                "Devolución de anticipo: " + titular.getNombre() + " | Ref: " + nvl(referencia, ""),
                asiento, cuentaBancaria, total,
                TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO,
                OrigenMovimientoConciliacion.COBROS);

        em.flush();

        System.out.println("✓ Devolución registrada: id=" + devolucion.getCodigo()
                + " | asiento=" + asiento.getNumeroAlterno());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("exito", true);
        resultado.put("mensaje", "Devolución registrada. El asiento contable y el movimiento bancario "
                + "fueron generados.");
        resultado.put("devolucion", devolucion.getCodigo());
        resultado.put("asiento", asiento.getNumeroAlterno());
        resultado.put("idAsiento", asiento.getCodigo());
        return resultado;
    }

    // =========================================================================
    // anular — §4.2
    // =========================================================================

    @Override
    public Map<String, Object> anular(Long idDevolucion, String motivo, Long idUsuario) throws Throwable {
        System.out.println("=== anular (devolucion anticipo) | devolucion=" + idDevolucion + " ===");

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }

        DevolucionAnticipoProveedor devolucion = em.find(DevolucionAnticipoProveedor.class, idDevolucion);
        if (devolucion == null) {
            throw new IncomeException("No se encontró la devolución con ID: " + idDevolucion);
        }
        if (devolucion.getEstado() == null
                || devolucion.getEstado().intValue() != EstadoDevolucionAnticipoProveedor.ACTIVA) {
            throw new IncomeException("La devolución " + idDevolucion + " ya está anulada.");
        }

        Long idAsiento = (devolucion.getAsiento() != null) ? devolucion.getAsiento().getCodigo() : null;

        // 2. ⛔ No conciliada (§4.2 punto 2). PRIMER uso de este chequeo en una anulación de todo
        // el sistema -- verificado antes de escribirlo: ni IngresoServiceImpl.anularIngreso, ni
        // PagoProgramadoServiceImpl, ni AnticipoProveedorServiceImpl.motivoBloqueo lo hacen hoy.
        // Es deuda conocida de esas tres (ninguna se toca en este frente), no un criterio ya
        // probado en otro lado: se arma con las dos piezas que ya usa el resto del sistema para
        // lo mismo -- GrupoConciliacionAsientoDaoService.selectIdsEnGrupoActivo (que hoy sólo usan
        // ConciliacionContableMatchServiceImpl e ImportacionExtractoBancarioServiceImpl) y
        // MovimientoBanco.conciliado.
        if (idAsiento != null) {
            List<DetalleAsiento> lineasAsiento = detalleAsientoDaoService.selectByIdAsiento(idAsiento);
            List<Long> idsDetalleAsiento = new ArrayList<>();
            for (DetalleAsiento linea : lineasAsiento) {
                idsDetalleAsiento.add(linea.getCodigo());
            }
            if (!idsDetalleAsiento.isEmpty()
                    && !grupoConciliacionAsientoDaoService.selectIdsEnGrupoActivo(idsDetalleAsiento).isEmpty()) {
                throw new IncomeException("La devolución ya está conciliada con el extracto: "
                        + "desconcílela antes de anularla.");
            }
            for (MovimientoBanco movimiento : movimientoBancoDaoService.selectByAsiento(idAsiento)) {
                if (movimiento.getConciliado() != null && movimiento.getConciliado().longValue() == 1L) {
                    throw new IncomeException("La devolución ya está conciliada con el extracto: "
                            + "desconcílela antes de anularla.");
                }
            }
        }

        // 3. Anular el movimiento bancario y el asiento. SIN capturar excepciones: un fallo acá
        //    tiene que abortar toda la anulación (a diferencia de IngresoServiceImpl.anularIngreso,
        //    que las captura y sólo las imprime -- deliberadamente distinto, pedido por contrato).
        if (idAsiento != null) {
            movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
                    Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
            asientoService.anulaAsiento(idAsiento);
        }

        // 4. Reponer el saldo de cada anticipo y el PRCC tipo 2 del proveedor
        @SuppressWarnings("unchecked")
        List<DetalleDevolucionAnticipo> detalles = em.createQuery(
                "select d from DetalleDevolucionAnticipo d where d.devolucion.codigo = :idDevolucion")
                .setParameter("idDevolucion", idDevolucion)
                .getResultList();

        Titular titular = devolucion.getTitular();
        Long idEmpresa = (devolucion.getEmpresa() != null) ? devolucion.getEmpresa().getCodigo() : null;
        PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(titular.getCodigo(), idEmpresa);
        double saldoAnticiposAnterior = (cuentaAnticipos.getSaldoInicial() != null)
                ? cuentaAnticipos.getSaldoInicial() : 0.0;

        for (DetalleDevolucionAnticipo detalle : detalles) {
            AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, detalle.getAnticipo().getId());
            double saldoAnterior = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
            double valor = (detalle.getValor() != null) ? detalle.getValor() : 0.0;
            anticipo.setSaldo(redondea(saldoAnterior + valor));
            em.merge(anticipo);
            System.out.println("✓ Anticipo " + anticipo.getId() + " saldo repuesto: " + saldoAnterior
                    + " → " + anticipo.getSaldo());
        }

        double total = (devolucion.getValor() != null) ? devolucion.getValor() : 0.0;
        cuentaAnticipos.setSaldoInicial(redondea(saldoAnticiposAnterior + total));
        em.merge(cuentaAnticipos);
        System.out.println("✓ Saldo global de anticipos del proveedor " + titular.getCodigo() + ": "
                + saldoAnticiposAnterior + " → " + cuentaAnticipos.getSaldoInicial());

        // 5. Estado ANULADA
        devolucion.setEstado(Long.valueOf(EstadoDevolucionAnticipoProveedor.ANULADA));
        devolucion.setMotivoAnulacion(motivo.trim());
        devolucion.setFechaAnulacion(LocalDateTime.now());
        em.merge(devolucion);
        em.flush();

        System.out.println("✓ Devolución " + idDevolucion + " anulada.");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("exito", true);
        resultado.put("mensaje", "Devolución anulada. El asiento y el movimiento bancario fueron "
                + "reversados, y el saldo de los anticipos fue repuesto.");
        resultado.put("devolucion", idDevolucion);
        return resultado;
    }

    // =========================================================================
    // listar — §4.2, proyección
    // =========================================================================

    @Override
    public List<Map<String, Object>> listar(Long idEmpresa, Long idTitular) throws Throwable {
        System.out.println("=== listar (devoluciones anticipo) | empresa=" + idEmpresa
                + " | titular=" + idTitular + " ===");
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa.");
        }

        List<Object[]> filas = devolucionDaoService.selectListadoByEmpresaTitular(idEmpresa, idTitular);
        List<Object[]> detalleFilas = devolucionDaoService.selectDetalleByEmpresaTitular(idEmpresa, idTitular);

        Map<Long, List<Map<String, Object>>> detallePorDevolucion = new HashMap<>();
        for (Object[] fila : detalleFilas) {
            Long idDevolucion = (Long) fila[0];
            Map<String, Object> linea = new HashMap<>();
            linea.put("idAnticipo", fila[1]);
            linea.put("numeroDocAnticipo", fila[2]);
            linea.put("valor", fila[3]);
            List<Map<String, Object>> lista = detallePorDevolucion.get(idDevolucion);
            if (lista == null) {
                lista = new ArrayList<>();
                detallePorDevolucion.put(idDevolucion, lista);
            }
            lista.add(linea);
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            Long id = (Long) fila[0];
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("fecha", fila[1]);
            item.put("valor", fila[2]);
            item.put("referencia", fila[3]);
            item.put("estado", fila[4]);
            item.put("cuentaBancaria", nvl((String) fila[5], "") + " — " + nvl((String) fila[6], ""));
            item.put("numeroAsiento", fila[7]);
            item.put("idAsiento", fila[8]);
            item.put("motivoAnulacion", fila[9]);
            List<Map<String, Object>> detalle = detallePorDevolucion.get(id);
            item.put("detalle", (detalle != null) ? detalle : Collections.<Map<String, Object>>emptyList());
            resultado.add(item);
        }
        return resultado;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Cuenta contable de anticipos (tipo 2, rol Proveedor) de un titular. Mismo criterio y mismo
     * mensaje que {@code AplicacionPagoCxpServiceImpl.obtenerCuentaAnticipos}.
     */
    private PersonaCuentaContable obtenerCuentaAnticipos(Long idTitular, Long idEmpresa) throws Throwable {
        List<PersonaCuentaContable> lista = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
        if (lista.isEmpty()) {
            throw new IncomeException("El proveedor no tiene configurada la cuenta contable de "
                    + "anticipos (Tipo 2, Rol: Proveedor) en Tesorería → Persona → Cuentas Contables. "
                    + "Sin ella no es posible devolver anticipos.");
        }
        return lista.get(0);
    }

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : Long.valueOf(texto);
    }

    private Double toDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).doubleValue();
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : Double.valueOf(texto);
    }

    private double redondea(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private String nvl(String valor, String porDefecto) {
        return (valor != null && !valor.trim().isEmpty()) ? valor : porDefecto;
    }
}
