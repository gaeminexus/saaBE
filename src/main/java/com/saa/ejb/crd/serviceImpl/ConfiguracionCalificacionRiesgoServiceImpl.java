package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ConfiguracionCalificacionRiesgoDaoService;
import com.saa.ejb.crd.dao.EscalaCalificacionRiesgoDaoService;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.ConfiguracionCalificacionRiesgoService;
import com.saa.ejb.crd.service.dto.DetalleConfiguracionCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.DetalleEscalaCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.ProductoCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigenciaCalificacion;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.SolicitudEscala;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.EscalaCalificacionRiesgo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación de la parametrización de calificación de riesgo. Ver
 * {@link ConfiguracionCalificacionRiesgoService} para el contrato — P22,
 * PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 */
@Stateless
public class ConfiguracionCalificacionRiesgoServiceImpl implements ConfiguracionCalificacionRiesgoService {

    @EJB
    private ConfiguracionCalificacionRiesgoDaoService configuracionCalificacionRiesgoDaoService;

    @EJB
    private EscalaCalificacionRiesgoDaoService escalaCalificacionRiesgoDaoService;

    @EJB
    private ProductoDaoService productoDaoService;

    // ------------------------------------------------------------------------
    // Métodos del EntityService (CRUD genérico)
    // ------------------------------------------------------------------------

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ConfiguracionCalificacionRiesgo service");
        ConfiguracionCalificacionRiesgo entidad = new ConfiguracionCalificacionRiesgo();
        for (Long registro : id) {
            configuracionCalificacionRiesgoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ConfiguracionCalificacionRiesgo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ConfiguracionCalificacionRiesgo service");
        for (ConfiguracionCalificacionRiesgo entidad : lista) {
            configuracionCalificacionRiesgoDaoService.save(entidad, entidad.getCodigo());
        }
    }

    @Override
    public ConfiguracionCalificacionRiesgo saveSingle(ConfiguracionCalificacionRiesgo entidad) throws Throwable {
        System.out.println("Ingresa al metodo (saveSingle) ConfiguracionCalificacionRiesgo Service");
        configuracionCalificacionRiesgoDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<ConfiguracionCalificacionRiesgo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ConfiguracionCalificacionRiesgoService");
        List<ConfiguracionCalificacionRiesgo> result = configuracionCalificacionRiesgoDaoService
                .selectAll(NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de ConfiguracionCalificacionRiesgo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ConfiguracionCalificacionRiesgo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al metodo (selectById) de ConfiguracionCalificacionRiesgo con id: " + id);
        return configuracionCalificacionRiesgoDaoService.selectById(id,
                NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
    }

    @Override
    public List<ConfiguracionCalificacionRiesgo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) ConfiguracionCalificacionRiesgo");
        List<ConfiguracionCalificacionRiesgo> result = configuracionCalificacionRiesgoDaoService
                .selectByCriteria(datos, NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de ConfiguracionCalificacionRiesgo no devolvio ningun registro");
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------------

    @Override
    public DetalleConfiguracionCalificacionRiesgo selectVigenteConEscala(Long idProducto, Long idEmpresa,
            LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo (selectVigenteConEscala) ConfiguracionCalificacionRiesgo"
                + " - producto: " + idProducto + " empresa: " + idEmpresa + " fecha: " + fecha);
        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        LocalDate fechaEfectiva = (fecha != null ? fecha : LocalDate.now());
        ConfiguracionCalificacionRiesgo configuracion = configuracionCalificacionRiesgoDaoService
                .selectVigentePorProducto(idProducto, idEmpresa, fechaEfectiva);
        if (configuracion == null) {
            throw new IncomeException("No hay configuracion de calificacion de riesgo vigente al " + fechaEfectiva
                    + " para el producto " + idProducto + (idEmpresa != null ? ", empresa " + idEmpresa : ""));
        }
        return armaDetalle(configuracion, fechaEfectiva);
    }

    @Override
    public List<ProductoCalificacionRiesgo> listarParametrizacion(Long idEmpresa, LocalDate fecha)
            throws Throwable {
        System.out.println("Ingresa al metodo (listarParametrizacion) ConfiguracionCalificacionRiesgo"
                + " - empresa: " + idEmpresa + " fecha: " + fecha);
        LocalDate fechaEfectiva = (fecha != null ? fecha : LocalDate.now());

        List<Producto> productos = productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
        if (productos.isEmpty()) {
            throw new IncomeException("No hay productos de credito registrados");
        }

        List<ConfiguracionCalificacionRiesgo> configuraciones = configuracionCalificacionRiesgoDaoService
                .selectVigentesPorEmpresa(idEmpresa, fechaEfectiva);

        List<Long> idsConfiguracion = new ArrayList<>();
        for (ConfiguracionCalificacionRiesgo configuracion : configuraciones) {
            idsConfiguracion.add(configuracion.getCodigo());
        }
        Map<Long, List<EscalaCalificacionRiesgo>> escalasPorConfiguracion =
                agrupaEscalas(escalaCalificacionRiesgoDaoService.selectByConfiguraciones(idsConfiguracion));

        // Clave: producto. selectVigentesPorEmpresa ordena por fechaInicio descendente, asi que
        // si hubiera dos vigentes del mismo producto (defecto de parametrizacion historica: la
        // validacion de unicidad de guardarConfiguracion evita que esto vuelva a pasar hacia
        // adelante), gana la mas reciente -- mismo criterio que el listado de bandas.
        Map<Long, ConfiguracionCalificacionRiesgo> vigentePorProducto = new LinkedHashMap<>();
        for (ConfiguracionCalificacionRiesgo configuracion : configuraciones) {
            if (configuracion.getProducto() == null) {
                continue;
            }
            Long idProducto = configuracion.getProducto().getCodigo();
            if (!vigentePorProducto.containsKey(idProducto)) {
                vigentePorProducto.put(idProducto, configuracion);
            }
        }

        List<Producto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(Producto::getCodigo, Comparator.nullsLast(Comparator.naturalOrder())));

        List<ProductoCalificacionRiesgo> resultado = new ArrayList<>();
        for (Producto producto : ordenados) {
            ProductoCalificacionRiesgo fila = new ProductoCalificacionRiesgo();
            fila.setIdProducto(producto.getCodigo());
            fila.setNombreProducto(producto.getNombre());
            fila.setEstadoProducto(producto.getEstado());

            ConfiguracionCalificacionRiesgo configuracion = vigentePorProducto.get(producto.getCodigo());
            if (configuracion != null) {
                List<EscalaCalificacionRiesgo> escalas = escalasPorConfiguracion.get(configuracion.getCodigo());
                if (escalas == null) {
                    escalas = new ArrayList<>();
                }
                fila.setConfiguracion(armaDetalle(configuracion, escalas, fechaEfectiva));
            }
            resultado.add(fila);
        }
        return resultado;
    }

    @Override
    public List<DetalleConfiguracionCalificacionRiesgo> selectHistorial(Long idProducto, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo (selectHistorial) ConfiguracionCalificacionRiesgo"
                + " - producto: " + idProducto + " empresa: " + idEmpresa);
        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        LocalDate hoy = LocalDate.now();
        List<DetalleConfiguracionCalificacionRiesgo> resultado = new ArrayList<>();
        for (ConfiguracionCalificacionRiesgo configuracion : configuracionCalificacionRiesgoDaoService
                .selectHistorial(idProducto, idEmpresa)) {
            resultado.add(armaDetalle(configuracion, hoy));
        }
        return resultado;
    }

    // ------------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DetalleConfiguracionCalificacionRiesgo guardarConfiguracion(
            SolicitudConfiguracionCalificacionRiesgo solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (guardarConfiguracion) ConfiguracionCalificacionRiesgo"
                + " - configuracion: " + (solicitud != null ? solicitud.getIdConfiguracion() : null));

        if (solicitud == null) {
            throw new IncomeException("La solicitud de configuracion es obligatoria");
        }

        LocalDate hoy = LocalDate.now();
        ConfiguracionCalificacionRiesgo configuracion;

        if (solicitud.getIdConfiguracion() == null) {
            configuracion = nuevaCabecera(solicitud);
        } else {
            configuracion = recuperaConfiguracion(solicitud.getIdConfiguracion());
            // Misma regla que bandas: una configuracion cuya vigencia ya empezo no se edita en
            // caliente -- su escala ya pudo haber calificado cuotas reales del G48.
            if (configuracion.getFechaInicio() != null && !configuracion.getFechaInicio().isAfter(hoy)) {
                throw new IncomeException("La configuracion " + configuracion.getCodigo()
                        + " ya esta vigente desde el " + configuracion.getFechaInicio()
                        + ": no se puede editar en el lugar. Use el cierre de vigencia para crear"
                        + " una configuracion nueva a partir de una fecha");
            }
            actualizaCabecera(configuracion, solicitud);
        }

        validaVigencia(configuracion.getFechaInicio(), configuracion.getFechaFin());
        validaUnicidadVigente(configuracion);

        // Validado ANTES de tocar la base -- si la escala esta mal, la cabecera tampoco se graba.
        List<EscalaValidada> tramos = validaEscalas(solicitud.getEscalas());

        configuracionCalificacionRiesgoDaoService.save(configuracion, configuracion.getCodigo());
        if (configuracion.getCodigo() == null) {
            throw new IncomeException("No se pudo obtener el codigo de la configuracion grabada");
        }

        // Reemplazo completo del juego de calificaciones: es una configuracion que aun no rige.
        escalaCalificacionRiesgoDaoService.deleteByConfiguracion(configuracion.getCodigo());
        grabaEscalas(configuracion, tramos, solicitud.getUsuario());

        return armaDetalle(configuracion, hoy);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DetalleConfiguracionCalificacionRiesgo cerrarVigencia(SolicitudCierreVigenciaCalificacion solicitud)
            throws Throwable {
        System.out.println("Ingresa al metodo (cerrarVigencia) ConfiguracionCalificacionRiesgo"
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

        ConfiguracionCalificacionRiesgo vigente = recuperaConfiguracion(solicitud.getIdConfiguracionVigente());
        if (vigente.getFechaFin() != null) {
            throw new IncomeException("La configuracion " + vigente.getCodigo() + " ya fue cerrada el "
                    + vigente.getFechaFin());
        }
        if (vigente.getFechaInicio() != null
                && !solicitud.getFechaDesdeNueva().isAfter(vigente.getFechaInicio())) {
            throw new IncomeException("La fecha desde de la configuracion nueva (" + solicitud.getFechaDesdeNueva()
                    + ") debe ser posterior a la fecha desde de la configuracion que se cierra ("
                    + vigente.getFechaInicio() + ")");
        }

        List<EscalaValidada> tramos = validaEscalas(solicitud.getEscalas());

        // Las dos vigencias quedan contiguas: sin traslape y sin hueco.
        vigente.setFechaFin(solicitud.getFechaDesdeNueva().minusDays(1));
        configuracionCalificacionRiesgoDaoService.save(vigente, vigente.getCodigo());

        ConfiguracionCalificacionRiesgo nueva = new ConfiguracionCalificacionRiesgo();
        nueva.setProducto(vigente.getProducto());
        nueva.setIdEmpresa(vigente.getIdEmpresa());
        nueva.setNombre(vigente.getNombre());
        nueva.setFechaInicio(solicitud.getFechaDesdeNueva());
        nueva.setFechaFin(null);
        nueva.setEstado(Long.valueOf(Estado.ACTIVO));
        nueva.setUsuarioRegistro(solicitud.getUsuario());
        nueva.setFechaRegistro(LocalDateTime.now());

        // La consulta de traslape es JPQL, asi que fuerza el flush del cierre de arriba -- la
        // configuracion recien cerrada ya no cuenta como solapada. Lo que si atrapa es una
        // TERCERA configuracion de la misma terna que alguien haya dejado abierta.
        validaUnicidadVigente(nueva);
        configuracionCalificacionRiesgoDaoService.save(nueva, null);
        if (nueva.getCodigo() == null) {
            throw new IncomeException("No se pudo obtener el codigo de la configuracion nueva grabada");
        }
        grabaEscalas(nueva, tramos, solicitud.getUsuario());

        return armaDetalle(nueva, solicitud.getFechaDesdeNueva());
    }

    // ------------------------------------------------------------------------
    // Auxiliares privados
    // ------------------------------------------------------------------------

    private ConfiguracionCalificacionRiesgo nuevaCabecera(SolicitudConfiguracionCalificacionRiesgo solicitud)
            throws Throwable {
        if (solicitud.getIdProducto() == null) {
            throw new IncomeException("El producto es obligatorio");
        }
        ConfiguracionCalificacionRiesgo configuracion = new ConfiguracionCalificacionRiesgo();
        configuracion.setProducto(recuperaProducto(solicitud.getIdProducto()));
        // Sin validar existencia de la empresa: PJRQCDGO es un numero de trazabilidad sin FK a
        // proposito (ver el javadoc de la entidad), null = aplica a cualquier empresa.
        configuracion.setIdEmpresa(solicitud.getIdEmpresa());
        configuracion.setNombre(solicitud.getNombre());
        configuracion.setFechaInicio(solicitud.getFechaDesde());
        configuracion.setFechaFin(solicitud.getFechaHasta());
        configuracion.setEstado(Long.valueOf(Estado.ACTIVO));
        configuracion.setUsuarioRegistro(solicitud.getUsuario());
        configuracion.setFechaRegistro(LocalDateTime.now());
        return configuracion;
    }

    /**
     * Aplica sobre una cabecera existente los campos editables. El producto y la empresa NO se
     * cambian: mover una configuración de terna es crear otra, no editar esta.
     */
    private void actualizaCabecera(ConfiguracionCalificacionRiesgo configuracion,
            SolicitudConfiguracionCalificacionRiesgo solicitud) {
        if (solicitud.getFechaDesde() != null) {
            configuracion.setFechaInicio(solicitud.getFechaDesde());
        }
        configuracion.setFechaFin(solicitud.getFechaHasta());
        if (solicitud.getNombre() != null) {
            configuracion.setNombre(solicitud.getNombre());
        }
        if (configuracion.getEstado() == null) {
            configuracion.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        // CFCR no tiene columnas de modificacion (decision del arbitro 2026-09-07: la
        // trazabilidad la da la cadena de vigencias, no hace falta duplicarla) -- usuarioRegistro/
        // fechaRegistro de esta fila NO se tocan en una edicion en el lugar.
    }

    /**
     * Valida el juego de calificaciones completo y devuelve los tramos ya resueltos --
     * calificación, {@code diaDesde} DERIVADO (nunca del cliente, ver {@link SolicitudEscala}),
     * {@code diaHasta} y provisión -- en el mismo orden en que llegaron.
     *
     * Reglas (todas fallan con {@code IncomeException}, ANTES de tocar la base): al menos una
     * línea; calificación obligatoria y sin repetidas; provisión obligatoria entre 0 y 1; SOLO
     * la última puede tener {@code diaHasta} nulo (abierta); {@code diaHasta}, si viene, no
     * puede ser menor al {@code diaDesde} derivado de esa línea.
     *
     * El criterio de "sin hueco ni solape" es el del control D.3 de {@code sql/177}
     * (ESCRDSDE = LAG(ESCRHSTA) + 1, con -1 para la primera fila -- por eso empieza en 0), pero
     * acá se aplica POR CONSTRUCCIÓN en vez de verificarse después: como {@code diaDesde} nunca
     * se recibe del cliente, un hueco o un solape no se puede ni expresar en la solicitud.
     */
    private List<EscalaValidada> validaEscalas(List<SolicitudEscala> escalas) {
        if (escalas == null || escalas.isEmpty()) {
            throw new IncomeException("La escala debe tener al menos una calificacion");
        }

        Set<String> vistas = new HashSet<>();
        List<EscalaValidada> tramos = new ArrayList<>();
        long diaDesdeEsperado = 0L;

        for (int i = 0; i < escalas.size(); i++) {
            SolicitudEscala linea = escalas.get(i);
            int posicion = i + 1;
            boolean esUltima = (i == escalas.size() - 1);

            if (linea.getCalificacion() == null || linea.getCalificacion().trim().isEmpty()) {
                throw new IncomeException("La calificacion de la linea " + posicion + " es obligatoria");
            }
            String calificacion = linea.getCalificacion().trim().toUpperCase();
            if (!vistas.add(calificacion)) {
                throw new IncomeException("La calificacion " + calificacion
                        + " esta repetida en la escala; cada calificacion debe aparecer una sola vez");
            }

            if (linea.getPorcentajeProvision() == null) {
                throw new IncomeException("El porcentaje de provision de " + calificacion + " es obligatorio");
            }
            double provision = linea.getPorcentajeProvision().doubleValue();
            if (provision < 0.0 || provision > 1.0) {
                throw new IncomeException("El porcentaje de provision de " + calificacion + " (" + provision
                        + ") debe estar entre 0 y 1 -- es tanto por uno (0.0099 = 0,99%); revise si"
                        + " se quiso decir " + (provision / 100.0));
            }

            Long diaHasta = linea.getDiaHasta();
            if (diaHasta == null && !esUltima) {
                throw new IncomeException("Solo la ULTIMA calificacion de la escala puede quedar sin tope"
                        + " superior (dia hasta nulo); la calificacion " + calificacion + " en la posicion "
                        + posicion + " de " + escalas.size() + " no lo es");
            }
            if (diaHasta != null && diaHasta.longValue() < diaDesdeEsperado) {
                throw new IncomeException("El dia hasta de " + calificacion + " (" + diaHasta
                        + ") no puede ser menor al dia desde que le corresponde a esa calificacion ("
                        + diaDesdeEsperado + ")");
            }

            tramos.add(new EscalaValidada(calificacion, Long.valueOf(diaDesdeEsperado), diaHasta,
                    Double.valueOf(provision), Long.valueOf(posicion)));

            if (diaHasta != null) {
                diaDesdeEsperado = diaHasta.longValue() + 1L;
            }
        }
        return tramos;
    }

    /** Valida que la vigencia sea coherente -- mismo criterio que bandas. */
    private void validaVigencia(LocalDate desde, LocalDate hasta) {
        if (desde == null) {
            throw new IncomeException("La fecha desde de la vigencia es obligatoria");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IncomeException("La fecha hasta (" + hasta + ") no puede ser anterior a la fecha desde ("
                    + desde + ")");
        }
    }

    /**
     * Valida que no exista otra configuración activa del mismo producto (con la semántica
     * null-tolerante de empresa, ver {@code ConfiguracionCalificacionRiesgoDaoService#selectSolapadas})
     * cuya vigencia se solape con la que se pretende grabar.
     */
    private void validaUnicidadVigente(ConfiguracionCalificacionRiesgo configuracion) throws Throwable {
        List<ConfiguracionCalificacionRiesgo> solapadas = configuracionCalificacionRiesgoDaoService
                .selectSolapadas(configuracion.getProducto().getCodigo(), configuracion.getIdEmpresa(),
                        configuracion.getFechaInicio(), configuracion.getFechaFin());
        for (ConfiguracionCalificacionRiesgo otra : solapadas) {
            if (configuracion.getCodigo() != null && configuracion.getCodigo().equals(otra.getCodigo())) {
                continue;
            }
            throw new IncomeException("Ya existe la configuracion " + otra.getCodigo() + " vigente desde el "
                    + otra.getFechaInicio()
                    + (otra.getFechaFin() != null ? " hasta el " + otra.getFechaFin() : "")
                    + (otra.getIdEmpresa() != null ? " para la empresa " + otra.getIdEmpresa()
                            : " para CUALQUIER empresa")
                    + " de ese producto. Solo puede haber una configuracion vigente a la vez: cierre la"
                    + " anterior antes de crear otra");
        }
    }

    /** Inserta las calificaciones ya validadas de una configuración ya grabada. */
    private void grabaEscalas(ConfiguracionCalificacionRiesgo configuracion, List<EscalaValidada> tramos,
            String usuario) throws Throwable {
        LocalDateTime ahora = LocalDateTime.now();
        for (EscalaValidada tramo : tramos) {
            EscalaCalificacionRiesgo escala = new EscalaCalificacionRiesgo();
            escala.setConfiguracion(configuracion);
            escala.setCalificacion(tramo.calificacion);
            escala.setDiaDesde(tramo.diaDesde);
            escala.setDiaHasta(tramo.diaHasta);
            escala.setPorcentajeProvision(tramo.porcentajeProvision);
            escala.setOrden(tramo.orden);
            escala.setEstado(Long.valueOf(Estado.ACTIVO));
            escala.setUsuarioRegistro(usuario);
            escala.setFechaRegistro(ahora);
            escalaCalificacionRiesgoDaoService.save(escala, null);
        }
    }

    private ConfiguracionCalificacionRiesgo recuperaConfiguracion(Long idConfiguracion) throws Throwable {
        ConfiguracionCalificacionRiesgo configuracion;
        try {
            configuracion = configuracionCalificacionRiesgoDaoService.selectById(idConfiguracion,
                    NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
        } catch (Throwable e) {
            throw new IncomeException("No existe la configuracion de calificacion de riesgo " + idConfiguracion);
        }
        if (configuracion == null) {
            throw new IncomeException("No existe la configuracion de calificacion de riesgo " + idConfiguracion);
        }
        return configuracion;
    }

    private Producto recuperaProducto(Long idProducto) throws Throwable {
        Producto producto;
        try {
            producto = productoDaoService.selectById(idProducto, NombreEntidadesCredito.PRODUCTO);
        } catch (Throwable e) {
            throw new IncomeException("No existe el producto " + idProducto);
        }
        if (producto == null) {
            throw new IncomeException("No existe el producto " + idProducto);
        }
        return producto;
    }

    private DetalleConfiguracionCalificacionRiesgo armaDetalle(ConfiguracionCalificacionRiesgo configuracion,
            LocalDate fecha) throws Throwable {
        List<EscalaCalificacionRiesgo> escalas = escalaCalificacionRiesgoDaoService
                .selectByConfiguracion(configuracion.getCodigo());
        return armaDetalle(configuracion, escalas, fecha);
    }

    private DetalleConfiguracionCalificacionRiesgo armaDetalle(ConfiguracionCalificacionRiesgo configuracion,
            List<EscalaCalificacionRiesgo> escalas, LocalDate fecha) {
        DetalleConfiguracionCalificacionRiesgo detalle = new DetalleConfiguracionCalificacionRiesgo();
        detalle.setIdConfiguracion(configuracion.getCodigo());
        if (configuracion.getProducto() != null) {
            detalle.setIdProducto(configuracion.getProducto().getCodigo());
            detalle.setNombreProducto(configuracion.getProducto().getNombre());
        }
        detalle.setIdEmpresa(configuracion.getIdEmpresa());
        detalle.setNombre(configuracion.getNombre());
        detalle.setFechaDesde(configuracion.getFechaInicio());
        detalle.setFechaHasta(configuracion.getFechaFin());
        detalle.setEstado(configuracion.getEstado());
        detalle.setEditable(Boolean.valueOf(configuracion.getFechaInicio() != null
                && configuracion.getFechaInicio().isAfter(fecha)));

        List<DetalleEscalaCalificacionRiesgo> lineas = new ArrayList<>();
        for (EscalaCalificacionRiesgo escala : escalas) {
            lineas.add(aDetalleEscala(escala));
        }
        detalle.setEscalas(lineas);
        return detalle;
    }

    private DetalleEscalaCalificacionRiesgo aDetalleEscala(EscalaCalificacionRiesgo escala) {
        DetalleEscalaCalificacionRiesgo detalle = new DetalleEscalaCalificacionRiesgo();
        detalle.setIdEscala(escala.getCodigo());
        detalle.setCalificacion(escala.getCalificacion());
        detalle.setDiaDesde(escala.getDiaDesde());
        detalle.setDiaHasta(escala.getDiaHasta());
        detalle.setPorcentajeProvision(escala.getPorcentajeProvision());
        detalle.setEtiqueta(etiquetaTramo(escala.getDiaDesde(), escala.getDiaHasta()));
        return detalle;
    }

    /**
     * "0", "1 - 30", "mas de 450 (resto)" -- mismo estilo que las etiquetas de banda contable.
     * Duplicado a propósito en {@code EscalaCalificacionRiesgoServiceImpl}: es un formateo de 3
     * líneas, no vale la pena una dependencia entre los dos servicios por esto.
     */
    private String etiquetaTramo(Long diaDesde, Long diaHasta) {
        if (diaHasta == null) {
            return "mas de " + (diaDesde != null ? diaDesde.longValue() - 1 : 0) + " (resto)";
        }
        if (diaDesde != null && diaDesde.equals(diaHasta)) {
            return String.valueOf(diaDesde);
        }
        return diaDesde + " - " + diaHasta;
    }

    private Map<Long, List<EscalaCalificacionRiesgo>> agrupaEscalas(List<EscalaCalificacionRiesgo> escalas) {
        Map<Long, List<EscalaCalificacionRiesgo>> mapa = new LinkedHashMap<>();
        for (EscalaCalificacionRiesgo escala : escalas) {
            if (escala.getConfiguracion() == null) {
                continue;
            }
            Long clave = escala.getConfiguracion().getCodigo();
            List<EscalaCalificacionRiesgo> lista = mapa.get(clave);
            if (lista == null) {
                lista = new ArrayList<>();
                mapa.put(clave, lista);
            }
            lista.add(escala);
        }
        return mapa;
    }

    /** Tramo de escala ya validado, con {@code diaDesde}/{@code orden} DERIVADOS -- nunca vienen del cliente. */
    private static final class EscalaValidada {
        private final String calificacion;
        private final Long diaDesde;
        private final Long diaHasta;
        private final Double porcentajeProvision;
        private final Long orden;

        private EscalaValidada(String calificacion, Long diaDesde, Long diaHasta, Double porcentajeProvision,
                Long orden) {
            this.calificacion = calificacion;
            this.diaDesde = diaDesde;
            this.diaHasta = diaHasta;
            this.porcentajeProvision = porcentajeProvision;
            this.orden = orden;
        }
    }
}
