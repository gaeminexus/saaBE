package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ConfiguracionBandaProductoService;
import com.saa.ejb.crd.service.dto.ConfiguracionBandaDetalle;
import com.saa.ejb.crd.service.dto.CuentaBandaDisponible;
import com.saa.ejb.crd.service.dto.ProductoBandas;
import com.saa.ejb.crd.service.dto.SolicitudBanda;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigencia;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionBanda;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.NombreEntidadesSistema;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación de la parametrización de bandas de cartera por producto.
 * Ver {@link ConfiguracionBandaProductoService} para el contrato.
 */
@Stateless
public class ConfiguracionBandaProductoServiceImpl implements ConfiguracionBandaProductoService {

    @EJB
    private ConfiguracionBandaProductoDaoService configuracionBandaProductoDaoService;

    @EJB
    private BandaProductoDaoService bandaProductoDaoService;

    @EJB
    private ProductoDaoService productoDaoService;

    @EJB
    private EmpresaDaoService empresaDaoService;

    /** Solo lectura: valida que la cuenta exista, esté activa y sea de la misma empresa. */
    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    /** Deriva los rangos en días. La regla vive ahí y no se duplica aquí. */
    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    // ------------------------------------------------------------------------
    // Métodos del EntityService (CRUD genérico)
    // ------------------------------------------------------------------------

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ConfiguracionBandaProducto service");
        ConfiguracionBandaProducto entidad = new ConfiguracionBandaProducto();
        for (Long registro : id) {
            configuracionBandaProductoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ConfiguracionBandaProducto> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ConfiguracionBandaProducto service");
        for (ConfiguracionBandaProducto entidad : lista) {
            configuracionBandaProductoDaoService.save(entidad, entidad.getCodigo());
        }
    }

    @Override
    public ConfiguracionBandaProducto saveSingle(ConfiguracionBandaProducto entidad)
            throws Throwable {
        System.out.println("Ingresa al metodo (saveSingle) ConfiguracionBandaProducto Service");
        configuracionBandaProductoDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<ConfiguracionBandaProducto> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ConfiguracionBandaProductoService");
        List<ConfiguracionBandaProducto> result = configuracionBandaProductoDaoService
                .selectAll(NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException(
                    "Busqueda de ConfiguracionBandaProducto no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ConfiguracionBandaProducto selectById(Long id) throws Throwable {
        System.out.println("Ingresa al metodo (selectById) de ConfiguracionBandaProducto con id: "
                + id);
        return configuracionBandaProductoDaoService.selectById(id,
                NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
    }

    @Override
    public List<ConfiguracionBandaProducto> selectByCriteria(List<DatosBusqueda> datos)
            throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) ConfiguracionBandaProducto");
        List<ConfiguracionBandaProducto> result = configuracionBandaProductoDaoService
                .selectByCriteria(datos, NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException(
                    "Busqueda de ConfiguracionBandaProducto no devolvio ningun registro");
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------------

    @Override
    public ConfiguracionBandaDetalle selectVigenteConBandas(Long idProducto, Long idEmpresa,
            Long tipoCartera, LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo (selectVigenteConBandas) ConfiguracionBandaProducto"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " fecha: " + fecha);

        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        validaTipoCartera(tipoCartera);

        LocalDate fechaEfectiva = (fecha != null ? fecha : LocalDate.now());
        ConfiguracionBandaProducto configuracion = configuracionBandaProductoDaoService
                .selectVigente(idProducto, idEmpresa, tipoCartera, fechaEfectiva);
        if (configuracion == null) {
            throw new IncomeException("No hay configuracion de bandas vigente al " + fechaEfectiva
                    + " para el producto " + idProducto + ", empresa " + idEmpresa
                    + ", tipo de cartera " + nombreTipoCartera(tipoCartera));
        }
        return armaDetalle(configuracion, fechaEfectiva);
    }

    @Override
    public List<ProductoBandas> listarParametrizacion(Long idEmpresa, LocalDate fecha)
            throws Throwable {
        System.out.println("Ingresa al metodo (listarParametrizacion) ConfiguracionBandaProducto"
                + " - empresa: " + idEmpresa + " fecha: " + fecha);

        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        LocalDate fechaEfectiva = (fecha != null ? fecha : LocalDate.now());

        // TODOS los productos, activos e inactivos: los inactivos tienen cartera historica
        // que la reclasificacion puede tocar, y el usuario debe verlos marcados.
        List<Producto> productos = productoDaoService
                .selectAll(NombreEntidadesCredito.PRODUCTO);
        if (productos.isEmpty()) {
            throw new IncomeException("No hay productos de credito registrados");
        }

        // Una sola pasada por configuraciones y otra por bandas: el listado no debe
        // disparar dos consultas por producto.
        List<ConfiguracionBandaProducto> configuraciones = configuracionBandaProductoDaoService
                .selectVigentesByEmpresa(idEmpresa, fechaEfectiva);

        List<Long> idsConfiguracion = new ArrayList<Long>();
        for (ConfiguracionBandaProducto configuracion : configuraciones) {
            idsConfiguracion.add(configuracion.getCodigo());
        }
        Map<Long, List<BandaProducto>> bandasPorConfiguracion =
                agrupaBandas(bandaProductoDaoService.selectByConfiguraciones(idsConfiguracion));

        // Clave: producto + tipo de cartera. selectVigentesByEmpresa ordena por fechaDesde
        // descendente, asi que si por un defecto de parametrizacion hubiera dos vigentes,
        // gana la mas reciente y la otra se ignora.
        Map<String, ConfiguracionBandaProducto> vigentePorTerna =
                new LinkedHashMap<String, ConfiguracionBandaProducto>();
        for (ConfiguracionBandaProducto configuracion : configuraciones) {
            if (configuracion.getProducto() == null) {
                continue;
            }
            String clave = configuracion.getProducto().getCodigo() + "-"
                    + configuracion.getTipoCartera();
            if (!vigentePorTerna.containsKey(clave)) {
                vigentePorTerna.put(clave, configuracion);
            }
        }

        List<Producto> ordenados = new ArrayList<Producto>(productos);
        ordenados.sort(Comparator.comparing(Producto::getCodigo,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<ProductoBandas> resultado = new ArrayList<ProductoBandas>();
        for (Producto producto : ordenados) {
            ProductoBandas fila = new ProductoBandas();
            fila.setIdProducto(producto.getCodigo());
            fila.setNombreProducto(producto.getNombre());
            fila.setCodigoSBS(producto.getCodigoSBS());
            fila.setEstadoProducto(producto.getEstado());
            if (producto.getTipoPrestamo() != null) {
                fila.setNombreTipoPrestamo(producto.getTipoPrestamo().getNombre());
            }
            fila.setPorVencer(armaDetalleDesdeMapa(vigentePorTerna, bandasPorConfiguracion,
                    producto.getCodigo(), Long.valueOf(TipoCarteraBanda.POR_VENCER),
                    fechaEfectiva));
            fila.setVencido(armaDetalleDesdeMapa(vigentePorTerna, bandasPorConfiguracion,
                    producto.getCodigo(), Long.valueOf(TipoCarteraBanda.VENCIDO),
                    fechaEfectiva));
            resultado.add(fila);
        }
        return resultado;
    }

    @Override
    public List<ConfiguracionBandaDetalle> selectHistorial(Long idProducto, Long idEmpresa,
            Long tipoCartera) throws Throwable {
        System.out.println("Ingresa al metodo (selectHistorial) ConfiguracionBandaProducto"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera);

        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        validaTipoCartera(tipoCartera);

        LocalDate hoy = LocalDate.now();
        List<ConfiguracionBandaDetalle> resultado = new ArrayList<ConfiguracionBandaDetalle>();
        for (ConfiguracionBandaProducto configuracion : configuracionBandaProductoDaoService
                .selectHistorial(idProducto, idEmpresa, tipoCartera)) {
            resultado.add(armaDetalle(configuracion, hoy));
        }
        return resultado;
    }

    @Override
    public List<CuentaBandaDisponible> buscarCuentas(Long idEmpresa, String filtro)
            throws Throwable {
        System.out.println("Ingresa al metodo (buscarCuentas) ConfiguracionBandaProducto"
                + " - empresa: " + idEmpresa + " filtro: " + filtro);
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        List<CuentaBandaDisponible> resultado = new ArrayList<CuentaBandaDisponible>();
        for (PlanCuenta cuenta : planCuentaDaoService
                .selectMovimientoActivasByEmpresaFiltro(idEmpresa, filtro)) {
            resultado.add(new CuentaBandaDisponible(cuenta.getCodigo(),
                    cuenta.getCuentaContable(), cuenta.getNombre()));
        }
        return resultado;
    }

    // ------------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ConfiguracionBandaDetalle guardarConfiguracion(SolicitudConfiguracionBanda solicitud)
            throws Throwable {
        System.out.println("Ingresa al metodo (guardarConfiguracion) ConfiguracionBandaProducto"
                + " - configuracion: "
                + (solicitud != null ? solicitud.getIdConfiguracion() : null));

        if (solicitud == null) {
            throw new IncomeException("La solicitud de configuracion es obligatoria");
        }

        LocalDate hoy = LocalDate.now();
        ConfiguracionBandaProducto configuracion;

        if (solicitud.getIdConfiguracion() == null) {
            configuracion = nuevaCabecera(solicitud);
        } else {
            configuracion = recuperaConfiguracion(solicitud.getIdConfiguracion());
            // Una configuracion cuya vigencia ya empezo NO se edita en caliente: sus
            // bandas ya tienen saldo contabilizado y cambiarlas dejaria esos saldos sin
            // explicacion. Para eso esta cerrarVigencia.
            if (configuracion.getFechaDesde() != null
                    && !configuracion.getFechaDesde().isAfter(hoy)) {
                throw new IncomeException("La configuracion " + configuracion.getCodigo()
                        + " ya esta vigente desde el " + configuracion.getFechaDesde()
                        + ": no se puede editar en el lugar. Use el cierre de vigencia para"
                        + " crear una configuracion nueva a partir de una fecha");
            }
            actualizaCabecera(configuracion, solicitud);
        }

        validaVigencia(configuracion.getFechaDesde(), configuracion.getFechaHasta());
        validaUnicidadVigente(configuracion);

        List<PlanCuenta> cuentas = validaBandas(solicitud.getBandas(),
                configuracion.getEmpresa().getCodigo());

        configuracionBandaProductoDaoService.save(configuracion, configuracion.getCodigo());
        // El codigo de una fila nueva lo asigna la IDENTITY en el persist: sin este flush
        // seguiria nulo y las bandas no tendrian a que apuntar.
        if (configuracion.getCodigo() == null) {
            throw new IncomeException("No se pudo obtener el codigo de la configuracion grabada");
        }

        // Reemplazo completo del juego de bandas: es una configuracion que aun no rige,
        // asi que borrar y volver a insertar no destruye nada contabilizado.
        bandaProductoDaoService.deleteByConfiguracion(configuracion.getCodigo());
        grabaBandas(configuracion, solicitud.getBandas(), cuentas, solicitud.getUsuario(),
                solicitud.getIp());

        return armaDetalle(configuracion, hoy);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ConfiguracionBandaDetalle cerrarVigencia(SolicitudCierreVigencia solicitud)
            throws Throwable {
        System.out.println("Ingresa al metodo (cerrarVigencia) ConfiguracionBandaProducto"
                + " - configuracion vigente: "
                + (solicitud != null ? solicitud.getIdConfiguracionVigente() : null));

        if (solicitud == null) {
            throw new IncomeException("La solicitud de cierre de vigencia es obligatoria");
        }
        if (solicitud.getIdConfiguracionVigente() == null) {
            throw new IncomeException("La configuracion vigente a cerrar es obligatoria");
        }
        if (solicitud.getFechaDesdeNueva() == null) {
            throw new IncomeException("La fecha desde de la configuracion nueva es obligatoria");
        }

        ConfiguracionBandaProducto vigente =
                recuperaConfiguracion(solicitud.getIdConfiguracionVigente());
        if (vigente.getFechaHasta() != null) {
            throw new IncomeException("La configuracion " + vigente.getCodigo()
                    + " ya fue cerrada el " + vigente.getFechaHasta());
        }
        if (vigente.getFechaDesde() != null
                && !solicitud.getFechaDesdeNueva().isAfter(vigente.getFechaDesde())) {
            throw new IncomeException("La fecha desde de la configuracion nueva ("
                    + solicitud.getFechaDesdeNueva() + ") debe ser posterior a la fecha desde"
                    + " de la configuracion que se cierra (" + vigente.getFechaDesde() + ")");
        }

        List<PlanCuenta> cuentas = validaBandas(solicitud.getBandas(),
                vigente.getEmpresa().getCodigo());

        // Las dos vigencias quedan contiguas: sin traslape y sin hueco.
        vigente.setFechaHasta(solicitud.getFechaDesdeNueva().minusDays(1));
        vigente.setUsuarioModificacion(solicitud.getUsuario());
        vigente.setIpModificacion(solicitud.getIp());
        vigente.setFechaModificacion(LocalDateTime.now());
        configuracionBandaProductoDaoService.save(vigente, vigente.getCodigo());

        ConfiguracionBandaProducto nueva = new ConfiguracionBandaProducto();
        nueva.setProducto(vigente.getProducto());
        nueva.setEmpresa(vigente.getEmpresa());
        nueva.setTipoCartera(vigente.getTipoCartera());
        nueva.setFechaDesde(solicitud.getFechaDesdeNueva());
        nueva.setFechaHasta(null);
        nueva.setEstado(Long.valueOf(Estado.ACTIVO));
        nueva.setUsuarioRegistro(solicitud.getUsuario());
        nueva.setIpRegistro(solicitud.getIp());
        nueva.setFechaRegistro(LocalDateTime.now());

        // La consulta de traslape es JPQL, asi que fuerza el flush del cierre de arriba: la
        // configuracion que se acaba de cerrar ya no cuenta como solapada. Lo que si atrapa
        // es una TERCERA configuracion de la misma terna que alguien haya dejado abierta.
        validaUnicidadVigente(nueva);
        configuracionBandaProductoDaoService.save(nueva, null);

        if (nueva.getCodigo() == null) {
            throw new IncomeException(
                    "No se pudo obtener el codigo de la configuracion nueva grabada");
        }
        grabaBandas(nueva, solicitud.getBandas(), cuentas, solicitud.getUsuario(),
                solicitud.getIp());

        return armaDetalle(nueva, solicitud.getFechaDesdeNueva());
    }

    // ------------------------------------------------------------------------
    // Auxiliares privados
    // ------------------------------------------------------------------------

    /**
     * Construye la cabecera de una configuración nueva a partir de la solicitud.
     *
     * @param solicitud : Solicitud de guardado
     * @return          : Cabecera sin grabar
     */
    private ConfiguracionBandaProducto nuevaCabecera(SolicitudConfiguracionBanda solicitud)
            throws Throwable {
        if (solicitud.getIdProducto() == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        validaTipoCartera(solicitud.getTipoCartera());

        ConfiguracionBandaProducto configuracion = new ConfiguracionBandaProducto();
        configuracion.setProducto(recuperaProducto(solicitud.getIdProducto()));
        configuracion.setEmpresa(recuperaEmpresa(solicitud.getIdEmpresa()));
        configuracion.setTipoCartera(solicitud.getTipoCartera());
        configuracion.setFechaDesde(solicitud.getFechaDesde());
        configuracion.setFechaHasta(solicitud.getFechaHasta());
        configuracion.setEstado(Long.valueOf(Estado.ACTIVO));
        configuracion.setUsuarioRegistro(solicitud.getUsuario());
        configuracion.setIpRegistro(solicitud.getIp());
        configuracion.setFechaRegistro(LocalDateTime.now());
        return configuracion;
    }

    /**
     * Aplica sobre una cabecera existente los campos editables de la solicitud.
     * El producto, la empresa y el tipo de cartera NO se cambian: mover una configuración
     * de terna es crear otra, no editar esta.
     *
     * @param configuracion : Cabecera recuperada de la base
     * @param solicitud     : Solicitud de guardado
     */
    private void actualizaCabecera(ConfiguracionBandaProducto configuracion,
            SolicitudConfiguracionBanda solicitud) {
        if (solicitud.getFechaDesde() != null) {
            configuracion.setFechaDesde(solicitud.getFechaDesde());
        }
        configuracion.setFechaHasta(solicitud.getFechaHasta());
        if (configuracion.getEstado() == null) {
            configuracion.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        configuracion.setUsuarioModificacion(solicitud.getUsuario());
        configuracion.setIpModificacion(solicitud.getIp());
        configuracion.setFechaModificacion(LocalDateTime.now());
    }

    /**
     * Valida el juego de bandas completo y devuelve las cuentas contables resueltas, en el
     * mismo orden en que llegaron las bandas.
     *
     * Reglas: lista no vacía; números consecutivos desde 1; exactamente UNA banda con
     * períodos nulos y debe ser la última; las demás con períodos >= 1; cuenta obligatoria,
     * existente, activa y de la misma empresa.
     *
     * @param bandas     : Bandas de la solicitud
     * @param idEmpresa  : Empresa de la configuración
     * @return           : Cuentas contables resueltas, alineadas con {@code bandas}
     */
    private List<PlanCuenta> validaBandas(List<SolicitudBanda> bandas, Long idEmpresa)
            throws Throwable {
        if (bandas == null || bandas.isEmpty()) {
            throw new IncomeException("La configuracion debe tener al menos una banda");
        }

        List<PlanCuenta> cuentas = new ArrayList<PlanCuenta>();
        int abiertas = 0;
        for (int i = 0; i < bandas.size(); i++) {
            SolicitudBanda banda = bandas.get(i);
            long esperado = i + 1L;

            if (banda.getNumero() == null || banda.getNumero().longValue() != esperado) {
                throw new IncomeException("Los numeros de banda deben ser consecutivos desde 1;"
                        + " en la posicion " + esperado + " se recibio "
                        + (banda.getNumero() != null ? banda.getNumero() : "nulo"));
            }

            if (banda.getPeriodos() == null) {
                abiertas++;
                if (i != bandas.size() - 1) {
                    throw new IncomeException("Solo la ULTIMA banda puede tener periodos nulos"
                            + " (banda abierta); la banda " + esperado + " de "
                            + bandas.size() + " los tiene");
                }
            } else if (banda.getPeriodos().longValue() < 1L) {
                throw new IncomeException("Los periodos de la banda " + esperado
                        + " deben ser mayores o iguales a 1; se recibio "
                        + banda.getPeriodos());
            }

            cuentas.add(recuperaCuentaValida(banda.getIdPlanCuenta(), esperado, idEmpresa));
        }

        if (abiertas != 1) {
            throw new IncomeException("La configuracion debe tener EXACTAMENTE una banda abierta"
                    + " (periodos nulos) y debe ser la ultima; se encontraron " + abiertas);
        }
        return cuentas;
    }

    /**
     * Recupera la cuenta contable de una banda y valida que exista, esté activa y sea de la
     * misma empresa de la configuración.
     *
     * @param idPlanCuenta : Código de la cuenta (CNT.PLNN)
     * @param numeroBanda  : Número de banda, para el mensaje de error
     * @param idEmpresa    : Empresa de la configuración
     * @return             : Cuenta contable
     */
    private PlanCuenta recuperaCuentaValida(Long idPlanCuenta, long numeroBanda, Long idEmpresa)
            throws Throwable {
        if (idPlanCuenta == null) {
            throw new IncomeException("La cuenta contable de la banda " + numeroBanda
                    + " es obligatoria");
        }
        PlanCuenta cuenta;
        try {
            cuenta = planCuentaDaoService.selectById(idPlanCuenta,
                    NombreEntidadesContabilidad.PLAN_CUENTA);
        } catch (Throwable e) {
            throw new IncomeException("La cuenta contable " + idPlanCuenta + " de la banda "
                    + numeroBanda + " no existe");
        }
        if (cuenta == null) {
            throw new IncomeException("La cuenta contable " + idPlanCuenta + " de la banda "
                    + numeroBanda + " no existe");
        }
        if (cuenta.getEstado() == null || cuenta.getEstado().longValue() != Estado.ACTIVO) {
            throw new IncomeException("La cuenta contable " + cuenta.getCuentaContable()
                    + " de la banda " + numeroBanda + " no esta activa");
        }
        if (cuenta.getEmpresa() == null || cuenta.getEmpresa().getCodigo() == null
                || !cuenta.getEmpresa().getCodigo().equals(idEmpresa)) {
            throw new IncomeException("La cuenta contable " + cuenta.getCuentaContable()
                    + " de la banda " + numeroBanda + " pertenece a otra empresa");
        }
        return cuenta;
    }

    /**
     * Valida que la vigencia sea coherente.
     *
     * @param desde : Inicio de vigencia
     * @param hasta : Fin de vigencia; nulo = abierta
     */
    private void validaVigencia(LocalDate desde, LocalDate hasta) {
        if (desde == null) {
            throw new IncomeException("La fecha desde de la vigencia es obligatoria");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IncomeException("La fecha hasta (" + hasta + ") no puede ser anterior a la"
                    + " fecha desde (" + desde + ")");
        }
    }

    /**
     * Valida que no exista otra configuración activa de la misma terna cuya vigencia se
     * solape con la que se pretende grabar.
     *
     * @param configuracion : Configuración a grabar (con o sin código)
     */
    private void validaUnicidadVigente(ConfiguracionBandaProducto configuracion) throws Throwable {
        List<ConfiguracionBandaProducto> solapadas = configuracionBandaProductoDaoService
                .selectSolapadas(configuracion.getProducto().getCodigo(),
                        configuracion.getEmpresa().getCodigo(),
                        configuracion.getTipoCartera(),
                        configuracion.getFechaDesde(),
                        configuracion.getFechaHasta());
        for (ConfiguracionBandaProducto otra : solapadas) {
            if (configuracion.getCodigo() != null
                    && configuracion.getCodigo().equals(otra.getCodigo())) {
                continue;
            }
            throw new IncomeException("Ya existe la configuracion " + otra.getCodigo()
                    + " vigente desde el " + otra.getFechaDesde()
                    + (otra.getFechaHasta() != null ? " hasta el " + otra.getFechaHasta() : "")
                    + " para ese producto, empresa y tipo de cartera. Solo puede haber una"
                    + " configuracion vigente a la vez: cierre la anterior antes de crear otra");
        }
    }

    /**
     * Inserta las bandas de una configuración ya grabada.
     *
     * @param configuracion : Configuración padre, con código asignado
     * @param bandas        : Bandas de la solicitud
     * @param cuentas       : Cuentas ya validadas, alineadas con {@code bandas}
     * @param usuario       : Usuario de auditoría
     * @param ip            : IP de auditoría
     */
    private void grabaBandas(ConfiguracionBandaProducto configuracion, List<SolicitudBanda> bandas,
            List<PlanCuenta> cuentas, String usuario, String ip) throws Throwable {
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 0; i < bandas.size(); i++) {
            SolicitudBanda solicitada = bandas.get(i);
            BandaProducto banda = new BandaProducto();
            banda.setConfiguracion(configuracion);
            banda.setNumero(solicitada.getNumero());
            banda.setPeriodos(solicitada.getPeriodos());
            banda.setPlanCuenta(cuentas.get(i));
            banda.setEstado(Long.valueOf(Estado.ACTIVO));
            banda.setUsuarioRegistro(usuario);
            banda.setIpRegistro(ip);
            banda.setFechaRegistro(ahora);
            bandaProductoDaoService.save(banda, null);
        }
    }

    /**
     * Recupera una configuración por código, con mensaje de negocio si no existe.
     *
     * @param idConfiguracion : Código de la configuración
     * @return                : Configuración
     */
    private ConfiguracionBandaProducto recuperaConfiguracion(Long idConfiguracion)
            throws Throwable {
        ConfiguracionBandaProducto configuracion;
        try {
            configuracion = configuracionBandaProductoDaoService.selectById(idConfiguracion,
                    NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
        } catch (Throwable e) {
            throw new IncomeException("No existe la configuracion de bandas " + idConfiguracion);
        }
        if (configuracion == null) {
            throw new IncomeException("No existe la configuracion de bandas " + idConfiguracion);
        }
        return configuracion;
    }

    /**
     * Recupera un producto por código, con mensaje de negocio si no existe.
     *
     * @param idProducto : Código del producto
     * @return           : Producto
     */
    private Producto recuperaProducto(Long idProducto) throws Throwable {
        Producto producto;
        try {
            producto = productoDaoService.selectById(idProducto,
                    NombreEntidadesCredito.PRODUCTO);
        } catch (Throwable e) {
            throw new IncomeException("No existe el producto " + idProducto);
        }
        if (producto == null) {
            throw new IncomeException("No existe el producto " + idProducto);
        }
        return producto;
    }

    /**
     * Recupera una empresa por código, con mensaje de negocio si no existe.
     *
     * @param idEmpresa : Código de la empresa
     * @return          : Empresa
     */
    private Empresa recuperaEmpresa(Long idEmpresa) throws Throwable {
        Empresa empresa;
        try {
            empresa = empresaDaoService.selectById(idEmpresa, NombreEntidadesSistema.EMPRESA);
        } catch (Throwable e) {
            throw new IncomeException("No existe la empresa " + idEmpresa);
        }
        if (empresa == null) {
            throw new IncomeException("No existe la empresa " + idEmpresa);
        }
        return empresa;
    }

    /**
     * Arma el DTO de una configuración leyendo sus bandas de la base.
     *
     * @param configuracion : Configuración
     * @param fecha         : Fecha con la que se calcula el indicador {@code editable}
     * @return              : Detalle con los rangos derivados
     */
    private ConfiguracionBandaDetalle armaDetalle(ConfiguracionBandaProducto configuracion,
            LocalDate fecha) throws Throwable {
        List<BandaProducto> bandas = bandaProductoDaoService
                .selectByConfiguracion(configuracion.getCodigo());
        return armaDetalle(configuracion, bandas, fecha);
    }

    /**
     * Arma el DTO de una configuración con las bandas ya leídas.
     *
     * @param configuracion : Configuración
     * @param bandas        : Bandas ordenadas por número
     * @param fecha         : Fecha con la que se calcula el indicador {@code editable}
     * @return              : Detalle con los rangos derivados
     */
    private ConfiguracionBandaDetalle armaDetalle(ConfiguracionBandaProducto configuracion,
            List<BandaProducto> bandas, LocalDate fecha) throws Throwable {
        ConfiguracionBandaDetalle detalle = new ConfiguracionBandaDetalle();
        detalle.setIdConfiguracion(configuracion.getCodigo());
        if (configuracion.getProducto() != null) {
            detalle.setIdProducto(configuracion.getProducto().getCodigo());
            detalle.setNombreProducto(configuracion.getProducto().getNombre());
        }
        if (configuracion.getEmpresa() != null) {
            detalle.setIdEmpresa(configuracion.getEmpresa().getCodigo());
        }
        detalle.setTipoCartera(configuracion.getTipoCartera());
        detalle.setNombreTipoCartera(nombreTipoCartera(configuracion.getTipoCartera()));
        detalle.setFechaDesde(configuracion.getFechaDesde());
        detalle.setFechaHasta(configuracion.getFechaHasta());
        detalle.setEstado(configuracion.getEstado());
        detalle.setEditable(Boolean.valueOf(configuracion.getFechaDesde() != null
                && configuracion.getFechaDesde().isAfter(fecha)));
        detalle.setBandas(clasificadorBandaService.derivarRangos(bandas));
        return detalle;
    }

    /**
     * Arma el DTO de la configuración vigente de una terna a partir de los mapas del
     * listado; devuelve nulo si el producto no tiene configuración de ese tipo de cartera.
     *
     * @param vigentePorTerna        : Configuraciones vigentes indexadas por producto-tipo
     * @param bandasPorConfiguracion : Bandas indexadas por configuración
     * @param idProducto             : Código del producto
     * @param tipoCartera            : 1 = por vencer, 2 = vencido
     * @param fecha                  : Fecha de evaluación
     * @return                       : Detalle, o {@code null} si no hay configuración
     */
    private ConfiguracionBandaDetalle armaDetalleDesdeMapa(
            Map<String, ConfiguracionBandaProducto> vigentePorTerna,
            Map<Long, List<BandaProducto>> bandasPorConfiguracion,
            Long idProducto, Long tipoCartera, LocalDate fecha) throws Throwable {
        ConfiguracionBandaProducto configuracion =
                vigentePorTerna.get(idProducto + "-" + tipoCartera);
        if (configuracion == null) {
            return null;
        }
        List<BandaProducto> bandas = bandasPorConfiguracion.get(configuracion.getCodigo());
        if (bandas == null) {
            bandas = new ArrayList<BandaProducto>();
        }
        return armaDetalle(configuracion, bandas, fecha);
    }

    /**
     * Agrupa por configuración una lista plana de bandas, preservando el orden por número
     * en el que llegó.
     *
     * @param bandas : Bandas de varias configuraciones
     * @return       : Bandas indexadas por código de configuración
     */
    private Map<Long, List<BandaProducto>> agrupaBandas(List<BandaProducto> bandas) {
        Map<Long, List<BandaProducto>> mapa = new LinkedHashMap<Long, List<BandaProducto>>();
        for (BandaProducto banda : bandas) {
            if (banda.getConfiguracion() == null) {
                continue;
            }
            Long clave = banda.getConfiguracion().getCodigo();
            List<BandaProducto> lista = mapa.get(clave);
            if (lista == null) {
                lista = new ArrayList<BandaProducto>();
                mapa.put(clave, lista);
            }
            lista.add(banda);
        }
        return mapa;
    }

    /**
     * Valida que el tipo de cartera sea uno de los dos del catálogo.
     *
     * @param tipoCartera : Tipo de cartera recibido
     */
    private void validaTipoCartera(Long tipoCartera) {
        if (tipoCartera == null) {
            throw new IncomeException("El tipo de cartera es obligatorio");
        }
        if (tipoCartera.longValue() != TipoCarteraBanda.POR_VENCER
                && tipoCartera.longValue() != TipoCarteraBanda.VENCIDO) {
            throw new IncomeException("Tipo de cartera invalido: " + tipoCartera
                    + ". Valores permitidos: " + TipoCarteraBanda.POR_VENCER
                    + " = por vencer, " + TipoCarteraBanda.VENCIDO + " = vencido");
        }
    }

    /**
     * Etiqueta legible del tipo de cartera.
     *
     * @param tipoCartera : Tipo de cartera
     * @return            : "POR VENCER" / "VENCIDO"
     */
    private String nombreTipoCartera(Long tipoCartera) {
        if (tipoCartera != null && tipoCartera.longValue() == TipoCarteraBanda.VENCIDO) {
            return "VENCIDO";
        }
        return "POR VENCER";
    }
}
