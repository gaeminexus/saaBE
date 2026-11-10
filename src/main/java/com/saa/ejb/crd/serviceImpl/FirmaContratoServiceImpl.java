package com.saa.ejb.crd.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FileService;
import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.ejb.crd.dao.DireccionDaoService;
import com.saa.ejb.crd.dao.FirmaContratoDaoService;
import com.saa.ejb.crd.dao.FirmanteContratoDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.ejb.crd.firma.ProveedorFirmaElectronica;
import com.saa.ejb.crd.firma.ProveedorFirmaException;
import com.saa.ejb.crd.firma.SecurityDataProveedor;
import com.saa.ejb.crd.service.FirmaContratoService;
import com.saa.ejb.crd.service.dto.ArchivoFirma;
import com.saa.ejb.crd.service.dto.ResultadoOperacionFirma;
import com.saa.ejb.crd.service.dto.TramiteFirma;
import com.saa.ejb.crd.util.ContratoAdhesionParametros;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.FirmaContrato;
import com.saa.model.crd.FirmanteContrato;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Participe;
import com.saa.model.crd.TipoAdjunto;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoContrato;
import com.saa.rubros.EstadoFirmaContrato;
import com.saa.rubros.EstadoFirmanteContrato;
import com.saa.rubros.RolFirmanteContrato;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see FirmaContratoService
 *
 * Un fallo del PROVEEDOR no lanza: deja el error en el trámite y confirma la transacción (un IncomeException que cruce un
 * EJB marca la transacción para deshacer y el trámite se perdería). Las validaciones sí lanzan, antes de grabar nada.
 */
@Stateless
public class FirmaContratoServiceImpl implements FirmaContratoService {

    // Códigos de error de validación (van al inicio del mensaje de IncomeException).
    static final String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    static final String ERR_CONTRATO_NO_ENCONTRADO = "CONTRATO_NO_ENCONTRADO";
    static final String ERR_CONTRATO_NO_VIGENTE = "CONTRATO_NO_VIGENTE";
    static final String ERR_TRAMITE_NO_ENCONTRADO = "TRAMITE_NO_ENCONTRADO";
    static final String ERR_TRAMITE_VIVO_EXISTENTE = "TRAMITE_VIVO_EXISTENTE";
    static final String ERR_FIRMANTE_SIN_CONTACTO = "FIRMANTE_SIN_CONTACTO";
    static final String ERR_FORMATO_CONTRATO_PENDIENTE = "FORMATO_CONTRATO_PENDIENTE";
    static final String ERR_ESTADO_INVALIDO = "ESTADO_INVALIDO";
    static final String ERR_TIPO_ADJUNTO_NO_CONFIGURADO = "TIPO_ADJUNTO_NO_CONFIGURADO";
    static final String ERR_ARCHIVO_NO_DISPONIBLE = "ARCHIVO_NO_DISPONIBLE";
    /** Error de proceso (no del proveedor): la respuesta del proveedor no trae lo mínimo. */
    static final String ERR_RESPUESTA_PROVEEDOR_INVALIDA = "RESPUESTA_PROVEEDOR_INVALIDA";

    static final String TIPO_ADJUNTO_ORIGINAL = "CONTRATO DE ADHESION";
    static final String TIPO_ADJUNTO_FIRMADO = "CONTRATO DE ADHESION FIRMADO";

    /** Subcarpeta bajo el directorio de uploads (ver FileService.uploadFileToPath). */
    private static final String CARPETA_FIRMAS = "crd/firma-contrato";

    /** Reporte del contrato de adhesión (fase 1b). Mientras no exista su .jasper, el envío responde FORMATO_CONTRATO_PENDIENTE. */
    private static final String MODULO_REPORTE = "crd";
    private static final String REPORTE_CONTRATO = "RPRT_CNTR_ADHS";

    private static final int MAX_ERROR = 2000;

    @EJB
    private FirmaContratoDaoService firmaContratoDaoService;

    @EJB
    private FirmanteContratoDaoService firmanteContratoDaoService;

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    @EJB
    private FileService fileService;

    @EJB
    private ReporteService reporteService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private DireccionDaoService direccionDaoService;

    /** U-2: todo proceso que graba valida el usuario PRIMERO (sin usuario válido no se hace nada). */
    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    // ========================================================================
    // CRUD estándar
    // ========================================================================

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - FirmaContrato");
        FirmaContrato entidad = new FirmaContrato();
        for (Long registro : id) {
            firmaContratoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<FirmaContrato> lista) throws Throwable {
        System.out.println("save list - FirmaContrato");
        for (FirmaContrato registro : lista) {
            firmaContratoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public FirmaContrato saveSingle(FirmaContrato firma) throws Throwable {
        System.out.println("saveSingle - FirmaContrato");
        return firmaContratoDaoService.save(firma, firma.getCodigo());
    }

    @Override
    public List<FirmaContrato> selectAll() throws Throwable {
        System.out.println("selectAll - FirmaContrato");
        List<FirmaContrato> result = firmaContratoDaoService.selectAll(NombreEntidadesCredito.FIRMA_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros FirmaContrato");
        }
        return result;
    }

    @Override
    public FirmaContrato selectById(Long id) throws Throwable {
        System.out.println("selectById - FirmaContrato: " + id);
        return firmaContratoDaoService.selectById(id, NombreEntidadesCredito.FIRMA_CONTRATO);
    }

    @Override
    public List<FirmaContrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - FirmaContrato");
        List<FirmaContrato> result = firmaContratoDaoService.selectByCriteria(datos, NombreEntidadesCredito.FIRMA_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio FirmaContrato no devolvio registros");
        }
        return result;
    }

    // ========================================================================
    // Operaciones
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoOperacionFirma enviarAFirmar(Long idContrato, String usuario) throws Throwable {
        System.out.println("enviarAFirmar - FirmaContrato - idContrato: " + idContrato);
        usuario = usuarioValidacionService.validar(usuario);
        if (idContrato == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idContrato es obligatorio");
        }

        Contrato contrato = contratoDaoService.find(new Contrato(), idContrato);
        if (contrato == null) {
            throw new IncomeException(ERR_CONTRATO_NO_ENCONTRADO + ": no existe el contrato " + idContrato);
        }
        if (contrato.getEstado() == null || contrato.getEstado() != EstadoContrato.ACTIVO
                || contrato.getFechaTerminacion() != null) {
            throw new IncomeException(ERR_CONTRATO_NO_VIGENTE + ": el contrato " + idContrato
                + " no está vigente (inactivo o terminado); solo se firma un contrato vigente");
        }
        // Un solo trámite vivo por contrato (además del índice único UX_FRCN_VIVO).
        List<FirmaContrato> vivos = firmaContratoDaoService.selectVivosByContrato(idContrato);
        if (!vivos.isEmpty()) {
            throw new IncomeException(ERR_TRAMITE_VIVO_EXISTENTE + ": el contrato " + idContrato
                + " ya tiene un trámite de firma vivo (" + vivos.get(0).getCodigo()
                + "); anúlelo o espere su resultado antes de enviar otro");
        }

        // El firmante: solo el partícipe (D1). Correo personal o, si no hay, institucional; celular = móvil.
        Entidad participe = contrato.getEntidad();
        FirmanteContrato firmante = armarFirmantePartícipe(participe);

        // El PDF se genera ANTES de grabar nada: si el formato no existe no queda ningún trámite a medias.
        byte[] pdf = generarPdfContrato(contrato);
        TipoAdjunto tipoOriginal = resolverTipoAdjunto(TIPO_ADJUNTO_ORIGINAL);

        Adjunto subido = null;
        try {
            LocalDateTime ahora = LocalDateTime.now();
            FirmaContrato firma = new FirmaContrato();
            firma.setIdContrato(idContrato);
            firma.setProveedor(proveedor().nombre());
            firma.setEstado(Long.valueOf(EstadoFirmaContrato.PENDIENTE_ENVIO));
            firma.setHash(sha256(pdf));
            firma.setFechaRegistro(ahora);
            firma.setUsuarioRegistro(usuario);
            firma = firmaContratoDaoService.save(firma, null);

            subido = guardarAdjunto(pdf, nombreArchivo(idContrato, firma.getCodigo(), "original"), tipoOriginal,
                participe, firma.getCodigo(), usuario);
            firma.setIdAdjuntoOriginal(subido.getCodigo());
            firmaContratoDaoService.save(firma, firma.getCodigo());

            firmante.setIdFirma(firma.getCodigo());
            firmante = firmanteContratoDaoService.save(firmante, null);
            List<FirmanteContrato> firmantes = new ArrayList<>();
            firmantes.add(firmante);

            return enviarAlProveedor(firma, firmantes, pdf, nombreArchivo(idContrato, firma.getCodigo(), "original"), participe, usuario);
        } catch (Throwable e) {
            borrarArchivoSilencioso(subido);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoOperacionFirma reintentarEnvio(Long idFirma, String usuario) throws Throwable {
        System.out.println("reintentarEnvio - FirmaContrato - idFirma: " + idFirma);
        usuario = usuarioValidacionService.validar(usuario);
        FirmaContrato firma = cargar(idFirma);
        if (firma.getEstado() == null || firma.getEstado() != EstadoFirmaContrato.PENDIENTE_ENVIO) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": solo se reintenta un trámite pendiente de envío (estado "
                + firma.getEstado() + ")");
        }
        Adjunto original = firma.getIdAdjuntoOriginal() != null
            ? adjuntoDaoService.find(new Adjunto(), firma.getIdAdjuntoOriginal()) : null;
        if (original == null) {
            throw new IncomeException(ERR_ARCHIVO_NO_DISPONIBLE + ": el trámite " + idFirma + " no tiene el PDF original guardado");
        }
        byte[] pdf = leerArchivo(original);
        if (firma.getHash() != null && !firma.getHash().equalsIgnoreCase(sha256(pdf))) {
            throw new IncomeException(ERR_ARCHIVO_NO_DISPONIBLE + ": el PDF guardado del trámite " + idFirma
                + " no coincide con su SHA-256 original; anule el trámite y envíe uno nuevo");
        }
        List<FirmanteContrato> firmantes = firmanteContratoDaoService.selectByFirma(idFirma);
        Contrato contrato = contratoDaoService.find(new Contrato(), firma.getIdContrato());
        return enviarAlProveedor(firma, firmantes, pdf, original.getNombreArchivo(),
            contrato != null ? contrato.getEntidad() : null, usuario);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoOperacionFirma actualizarEstado(Long idFirma, String usuario) throws Throwable {
        System.out.println("actualizarEstado - FirmaContrato - idFirma: " + idFirma);
        usuario = usuarioValidacionService.validar(usuario);
        FirmaContrato firma = cargar(idFirma);
        List<FirmanteContrato> firmantes = firmanteContratoDaoService.selectByFirma(idFirma);

        if (firma.getEstado() != null && firma.getEstado() == EstadoFirmaContrato.FIRMADO) {
            // Idempotente: ya firmado, no se consulta ni se descarga otra vez.
            return resultado(true, null, "El trámite ya está firmado", firma, firmantes);
        }
        if (firma.getEstado() == null || firma.getEstado() != EstadoFirmaContrato.ENVIADO) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": solo se actualiza un trámite enviado (estado "
                + firma.getEstado() + ")");
        }

        Adjunto firmado = null;
        try {
            ResultadoOperacionFirma fallo = null;
            try {
                ProveedorFirmaElectronica proveedor = proveedor();
                ProveedorFirmaElectronica.EstadoTramite estadoProveedor = proveedor.consultar(firma.getIdExterno());
                firma.setFechaUltimaConsulta(LocalDateTime.now());
                firma.setError(null);
                aplicarEstadoFirmantes(firmantes, estadoProveedor.getFirmantes());

                int nuevo = estadoProveedor.getEstado();
                if (nuevo == EstadoFirmaContrato.FIRMADO) {
                    byte[] pdfFirmado = proveedor.descargarFirmado(firma.getIdExterno());
                    if (pdfFirmado == null || pdfFirmado.length == 0) {
                        throw new ProveedorFirmaException(ERR_RESPUESTA_PROVEEDOR_INVALIDA,
                            "el proveedor devolvió el PDF firmado vacío");
                    }
                    Contrato contrato = contratoDaoService.find(new Contrato(), firma.getIdContrato());
                    firmado = registrarFirmado(firma, firmantes, pdfFirmado, contrato != null ? contrato.getEntidad() : null, usuario);
                } else if (nuevo == EstadoFirmaContrato.RECHAZADO || nuevo == EstadoFirmaContrato.VENCIDO
                        || nuevo == EstadoFirmaContrato.ANULADO) {
                    firma.setEstado(Long.valueOf(nuevo));
                }
            } catch (ProveedorFirmaException e) {
                firma.setFechaUltimaConsulta(LocalDateTime.now());
                firma.setError(truncar(e.getMessage()));
                fallo = resultado(false, e.getCodigo(), e.getMessage(), firma, firmantes);
            }

            for (FirmanteContrato f : firmantes) {
                firmanteContratoDaoService.save(f, f.getCodigo());
            }
            firmaContratoDaoService.save(firma, firma.getCodigo());
            return fallo != null ? fallo : resultado(true, null, "Estado actualizado", firma, firmantes);
        } catch (Throwable e) {
            borrarArchivoSilencioso(firmado);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoOperacionFirma anular(Long idFirma, String motivo, String usuario) throws Throwable {
        System.out.println("anular - FirmaContrato - idFirma: " + idFirma);
        usuario = usuarioValidacionService.validar(usuario);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el motivo de la anulación es obligatorio");
        }
        FirmaContrato firma = cargar(idFirma);
        boolean vivo = firma.getEstado() != null
            && (firma.getEstado() == EstadoFirmaContrato.PENDIENTE_ENVIO || firma.getEstado() == EstadoFirmaContrato.ENVIADO);
        if (!vivo) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": solo se anula un trámite vivo (estado " + firma.getEstado() + ")");
        }
        List<FirmanteContrato> firmantes = firmanteContratoDaoService.selectByFirma(idFirma);
        motivo = motivo.trim();
        if (motivo.length() > 500) {
            motivo = motivo.substring(0, 500);
        }

        if (firma.getEstado() == EstadoFirmaContrato.ENVIADO) {
            // Ya está en el proveedor: se cancela allá primero. Si falla, el trámite sigue vivo (no se cierra a ciegas).
            try {
                proveedor().anular(firma.getIdExterno(), motivo);
            } catch (ProveedorFirmaException e) {
                firma.setError(truncar(e.getMessage()));
                firmaContratoDaoService.save(firma, firma.getCodigo());
                return resultado(false, e.getCodigo(), e.getMessage(), firma, firmantes);
            }
        }
        firma.setEstado(Long.valueOf(EstadoFirmaContrato.ANULADO));
        firma.setFechaAnulacion(LocalDateTime.now());
        firma.setUsuarioAnulacion(usuario);
        firma.setMotivoAnulacion(motivo);
        firma.setError(null);
        firmaContratoDaoService.save(firma, firma.getCodigo());
        return resultado(true, null, "Trámite anulado", firma, firmantes);
    }

    @Override
    public ArchivoFirma descargar(Long idFirma, String tipo) throws Throwable {
        System.out.println("descargar - FirmaContrato - idFirma: " + idFirma + " - tipo: " + tipo);
        String t = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!t.equals("ORIGINAL") && !t.equals("FIRMADO")) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": tipo debe ser ORIGINAL o FIRMADO");
        }
        FirmaContrato firma = cargar(idFirma);
        Long idAdjunto = t.equals("ORIGINAL") ? firma.getIdAdjuntoOriginal() : firma.getIdAdjuntoFirmado();
        Adjunto adjunto = idAdjunto != null ? adjuntoDaoService.find(new Adjunto(), idAdjunto) : null;
        if (adjunto == null) {
            throw new IncomeException(ERR_ARCHIVO_NO_DISPONIBLE + ": el trámite " + idFirma + " no tiene PDF "
                + (t.equals("ORIGINAL") ? "original" : "firmado") + " todavía");
        }
        return new ArchivoFirma(adjunto.getNombreArchivo(),
            adjunto.getMimeType() != null ? adjunto.getMimeType() : "application/pdf", leerArchivo(adjunto));
    }

    @Override
    public List<TramiteFirma> consultarPorContrato(Long idContrato) throws Throwable {
        System.out.println("consultarPorContrato - FirmaContrato - idContrato: " + idContrato);
        if (idContrato == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idContrato es obligatorio");
        }
        List<TramiteFirma> historial = new ArrayList<>();
        for (FirmaContrato firma : firmaContratoDaoService.selectByContrato(idContrato)) {
            historial.add(new TramiteFirma(firma, firmanteContratoDaoService.selectByFirma(firma.getCodigo())));
        }
        return historial;
    }

    // ========================================================================
    // Piezas
    // ========================================================================

    /** El proveedor de firma. Un solo punto: la fase 2 solo completa {@link SecurityDataProveedor}. */
    private ProveedorFirmaElectronica proveedor() {
        return new SecurityDataProveedor();
    }

    /**
     * PUNTO ÚNICO para generar el PDF del contrato de adhesión (RPRT_CNTR_ADHS: contrato + Anexo I). Todo va por parámetros
     * ({@link ContratoAdhesionParametros}), sin consulta: una sola fila por generarReporteDesdeColeccion (ver CLAUDE.md,
     * reportes sin query). FORMATO_CONTRATO_PENDIENTE solo si falta el .jasper.
     */
    protected byte[] generarPdfContrato(Contrato contrato) throws Throwable {
        String jasper = "/rep/" + MODULO_REPORTE + "/" + REPORTE_CONTRATO + ".jasper";
        if (FirmaContratoServiceImpl.class.getResource(jasper) == null) {
            throw new IncomeException(ERR_FORMATO_CONTRATO_PENDIENTE
                + ": el formato del contrato de adhesión (" + REPORTE_CONTRATO + ") todavía no está disponible");
        }
        Long idEntidad = contrato.getEntidad() != null ? contrato.getEntidad().getCodigo() : null;
        List<Participe> participes = idEntidad != null ? participeDaoService.selectByEntidad(idEntidad) : null;
        List<Direccion> direcciones = idEntidad != null ? direccionDaoService.selectByParent(idEntidad) : null;
        Map<String, Object> parametros = ContratoAdhesionParametros.armar(contrato,
            participes != null && !participes.isEmpty() ? participes.get(0) : null, direcciones, null, LocalDate.now());
        return reporteService.generarReporteDesdeColeccion(MODULO_REPORTE, REPORTE_CONTRATO, parametros,
            Collections.singletonList(new HashMap<String, Object>()), "PDF");
    }

    /** Firmante por defecto: el partícipe. Valida cédula, nombre y al menos un medio de contacto, diciendo qué falta. */
    private FirmanteContrato armarFirmantePartícipe(Entidad participe) {
        List<String> faltan = new ArrayList<>();
        if (participe == null) {
            faltan.add("partícipe del contrato");
        } else {
            if (vacio(participe.getNumeroIdentificacion())) {
                faltan.add("cédula");
            }
            if (vacio(participe.getRazonSocial())) {
                faltan.add("nombre");
            }
        }
        String correo = null;
        String celular = null;
        if (participe != null) {
            correo = !vacio(participe.getCorreoPersonal()) ? participe.getCorreoPersonal().trim()
                : (!vacio(participe.getCorreoInstitucional()) ? participe.getCorreoInstitucional().trim() : null);
            celular = !vacio(participe.getMovil()) ? participe.getMovil().trim() : null;
            if (correo == null && celular == null) {
                faltan.add("correo (personal o institucional) o celular");
            }
        }
        if (!faltan.isEmpty()) {
            throw new IncomeException(ERR_FIRMANTE_SIN_CONTACTO + ": al partícipe le falta: " + String.join(", ", faltan));
        }
        FirmanteContrato f = new FirmanteContrato();
        f.setOrden(1L);
        f.setRol(RolFirmanteContrato.PARTICIPE);
        f.setIdentificacion(participe.getNumeroIdentificacion().trim());
        f.setNombre(participe.getRazonSocial().trim());
        f.setCorreo(correo);
        f.setCelular(celular);
        f.setIdEntidad(participe.getCodigo());
        f.setEstado(Long.valueOf(EstadoFirmanteContrato.PENDIENTE));
        return f;
    }

    /**
     * Llama al proveedor. Éxito: trámite ENVIADO con su id externo. Falla del proveedor: queda en PENDIENTE_ENVIO con el
     * error (no lanza; ver la nota de la clase).
     */
    private ResultadoOperacionFirma enviarAlProveedor(FirmaContrato firma, List<FirmanteContrato> firmantes, byte[] pdf,
            String nombreDocumento, Entidad entidad, String usuario) throws Throwable {
        Adjunto firmado = null;
        try {
            // Datos del partícipe: la foto guardada en FRFR (una fila por trámite).
            ProveedorFirmaElectronica.DatosParticipe datos = new ProveedorFirmaElectronica.DatosParticipe();
            FirmanteContrato f = firmantes.get(0);
            datos.setIdentificacion(f.getIdentificacion());
            datos.setNombre(f.getNombre());
            datos.setCorreo(f.getCorreo());
            datos.setCelular(f.getCelular());

            ProveedorFirmaElectronica.ResultadoEnvio r = proveedor().enviar(pdf, nombreDocumento, datos);
            if (r == null || vacio(r.getIdExterno())) {
                throw new ProveedorFirmaException(ERR_RESPUESTA_PROVEEDOR_INVALIDA,
                    "el proveedor no devolvió el identificador del trámite");
            }
            firma.setIdExterno(r.getIdExterno());
            firma.setEstado(Long.valueOf(EstadoFirmaContrato.ENVIADO));
            firma.setFechaEnvio(LocalDateTime.now());
            firma.setError(null);
            if (r.getIdsFirmantes() != null && !r.getIdsFirmantes().isEmpty()) {
                f.setIdExterno(r.getIdsFirmantes().get(0));
            }
            String mensaje = "Enviado a firmar";
            if (r.getPdfFirmado() != null && r.getPdfFirmado().length > 0) {
                // Respuesta inmediata: el proveedor ya devolvió el contrato firmado por los dos.
                firmado = registrarFirmado(firma, firmantes, r.getPdfFirmado(), entidad, usuario);
                mensaje = "Enviado y firmado";
            }
            for (FirmanteContrato fc : firmantes) {
                firmanteContratoDaoService.save(fc, fc.getCodigo());
            }
            firmaContratoDaoService.save(firma, firma.getCodigo());
            return resultado(true, null, mensaje, firma, firmantes);
        } catch (ProveedorFirmaException e) {
            firma.setError(truncar(e.getMessage()));
            firmaContratoDaoService.save(firma, firma.getCodigo());
            return resultado(false, e.getCodigo(), e.getMessage(), firma, firmantes);
        } catch (Throwable e) {
            borrarArchivoSilencioso(firmado);
            throw e;
        }
    }

    /**
     * Guarda el PDF firmado como adjunto («CONTRATO DE ADHESION FIRMADO») y cierra el trámite: FIRMADO con su fecha, y el
     * firmante FIRMADO (con la fecha que ya trajera del proveedor, o la de ahora). Sirve para la respuesta inmediata del
     * envío y para «Actualizar estado». No graba firma ni firmantes: lo hace el llamador.
     */
    private Adjunto registrarFirmado(FirmaContrato firma, List<FirmanteContrato> firmantes, byte[] pdfFirmado, Entidad entidad,
            String usuario) throws Throwable {
        Adjunto firmado = guardarAdjunto(pdfFirmado, nombreArchivo(firma.getIdContrato(), firma.getCodigo(), "firmado"),
            resolverTipoAdjunto(TIPO_ADJUNTO_FIRMADO), entidad, firma.getCodigo(), usuario);
        LocalDateTime ahora = LocalDateTime.now();
        firma.setIdAdjuntoFirmado(firmado.getCodigo());
        firma.setFechaFirma(ahora);
        firma.setEstado(Long.valueOf(EstadoFirmaContrato.FIRMADO));
        for (FirmanteContrato f : firmantes) {
            f.setEstado(Long.valueOf(EstadoFirmanteContrato.FIRMADO));
            if (f.getFechaFirma() == null) {
                f.setFechaFirma(ahora);
            }
        }
        return firmado;
    }

    private void aplicarEstadoFirmantes(List<FirmanteContrato> firmantes, List<ProveedorFirmaElectronica.EstadoFirmante> estados) {
        if (estados == null) {
            return;
        }
        for (ProveedorFirmaElectronica.EstadoFirmante e : estados) {
            for (FirmanteContrato f : firmantes) {
                if (f.getOrden() != null && f.getOrden().intValue() == e.getOrden()) {
                    f.setEstado(Long.valueOf(e.getEstado()));
                    f.setFechaFirma(e.getFechaFirma());
                }
            }
        }
    }

    private ResultadoOperacionFirma resultado(boolean exito, String codigo, String mensaje, FirmaContrato firma,
            List<FirmanteContrato> firmantes) {
        ResultadoOperacionFirma r = new ResultadoOperacionFirma();
        r.setExito(exito);
        r.setCodigoError(codigo);
        r.setMensaje(mensaje);
        r.setTramite(new TramiteFirma(firma, firmantes));
        return r;
    }

    private FirmaContrato cargar(Long idFirma) throws Throwable {
        if (idFirma == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idFirma es obligatorio");
        }
        FirmaContrato firma = firmaContratoDaoService.find(new FirmaContrato(), idFirma);
        if (firma == null) {
            throw new IncomeException(ERR_TRAMITE_NO_ENCONTRADO + ": no existe el trámite de firma " + idFirma);
        }
        return firma;
    }

    /** Tipo de adjunto por nombre exacto y activo (igual que el certificado bancario): ni cero ni más de uno. */
    private TipoAdjunto resolverTipoAdjunto(String nombre) throws Throwable {
        List<TipoAdjunto> tipos = tipoAdjuntoDaoService.selectByNombre(nombre);
        if (tipos.size() != 1) {
            throw new IncomeException(ERR_TIPO_ADJUNTO_NO_CONFIGURADO + ": hay " + tipos.size()
                + " tipos de adjunto activos llamados '" + nombre + "' en CRD.TPDJ; debe haber exactamente uno"
                + " (docs/logica-negocio/crd/sql/376_DDL_FIRMA_ELECTRONICA_CONTRATO.sql)");
        }
        return tipos.get(0);
    }

    /** Sube el PDF a disco y crea su adjunto (ADJNIDRF = el trámite). Si el adjunto falla, borra el archivo. */
    private Adjunto guardarAdjunto(byte[] pdf, String nombre, TipoAdjunto tipo, Entidad entidad, Long idFirma, String usuario)
            throws Throwable {
        String ruta;
        try {
            ruta = fileService.uploadFileToPath(new ByteArrayInputStream(pdf), fileService.generarNombreUnico(nombre), CARPETA_FIRMAS);
        } catch (Throwable e) {
            throw new IncomeException("ERROR_ARCHIVO: no se pudo guardar el PDF del contrato: " + e.getMessage());
        }
        try {
            Adjunto adjunto = new Adjunto();
            adjunto.setEntidad(entidad);
            adjunto.setIdReferencia(idFirma);
            adjunto.setTipoAdjunto(tipo);
            adjunto.setNombreArchivo(nombre);
            adjunto.setUrlArchivo(ruta);
            adjunto.setMimeType("application/pdf");
            adjunto.setEstado(Long.valueOf(Estado.ACTIVO));
            adjunto.setFechaRegistro(LocalDateTime.now());
            adjunto.setUsuarioRegistro(usuario);
            return adjuntoDaoService.save(adjunto, null);
        } catch (Throwable e) {
            try {
                fileService.deleteFile(ruta);
            } catch (Throwable borrado) {
                System.err.println("No se pudo borrar el archivo huérfano " + ruta + ": " + borrado.getMessage());
            }
            throw e;
        }
    }

    private byte[] leerArchivo(Adjunto adjunto) throws Throwable {
        try (InputStream in = fileService.downloadFile(adjunto.getUrlArchivo())) {
            return in.readAllBytes();
        } catch (Throwable e) {
            throw new IncomeException(ERR_ARCHIVO_NO_DISPONIBLE + ": no se pudo leer el archivo " + adjunto.getNombreArchivo()
                + ": " + e.getMessage());
        }
    }

    private void borrarArchivoSilencioso(Adjunto adjunto) {
        if (adjunto == null || adjunto.getUrlArchivo() == null) {
            return;
        }
        try {
            fileService.deleteFile(adjunto.getUrlArchivo());
        } catch (Throwable borrado) {
            System.err.println("No se pudo borrar el archivo huérfano " + adjunto.getUrlArchivo() + ": " + borrado.getMessage());
        }
    }

    private static String nombreArchivo(Long idContrato, Long idFirma, String sufijo) {
        return "contrato-adhesion-" + idContrato + "-tramite-" + idFirma + "-" + sufijo + ".pdf";
    }

    private static String sha256(byte[] contenido) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(contenido);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > MAX_ERROR ? texto.substring(0, MAX_ERROR) : texto;
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
