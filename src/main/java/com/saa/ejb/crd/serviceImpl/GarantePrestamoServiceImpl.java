package com.saa.ejb.crd.serviceImpl;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.GarantePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.ejb.crd.service.GarantePrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoGaranteConAutorizacion;
import com.saa.ejb.crd.service.dto.SolicitudGaranteConAutorizacion;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.GarantePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAdjunto;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class GarantePrestamoServiceImpl implements GarantePrestamoService {

    @EJB
    private GarantePrestamoDaoService garantePrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    @EJB
    private FileService fileService;

    @Override
    public GarantePrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - GarantePrestamo: " + id);
        return garantePrestamoDaoService.selectById(id, NombreEntidadesCredito.GARANTE_PRESTAMO);
    }

    @Override
    public GarantePrestamo selectActivoByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("selectActivoByPrestamo - GarantePrestamo - préstamo: " + idPrestamo);
        return garantePrestamoDaoService.selectActivoByPrestamo(idPrestamo);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // El garante no se borra: se anula (GRPRESTD = 0). Ver anularGarante.
        throw new IncomeException("El garante no se borra; se anula (DELETE /grpr/{id}?usuario=...).");
    }

    @Override
    public void save(List<GarantePrestamo> lista) throws Throwable {
        System.out.println("save list - GarantePrestamo");
        for (GarantePrestamo registro : lista) {
            guardarGarante(registro, registro.getUsuarioRegistro());
        }
    }

    @Override
    public List<GarantePrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - GarantePrestamo");
        List<GarantePrestamo> result = garantePrestamoDaoService.selectAll(NombreEntidadesCredito.GARANTE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros GarantePrestamo");
        }
        return result;
    }

    @Override
    public GarantePrestamo saveSingle(GarantePrestamo garante) throws Throwable {
        System.out.println("saveSingle - GarantePrestamo");
        return guardarGarante(garante, garante != null ? garante.getUsuarioRegistro() : null);
    }

    @Override
    public List<GarantePrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - GarantePrestamo");
        List<GarantePrestamo> result = garantePrestamoDaoService.selectByCriteria(datos,
            NombreEntidadesCredito.GARANTE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio GarantePrestamo no devolvio registros");
        }
        return result;
    }

    @Override
    public GarantePrestamo guardarGarante(GarantePrestamo garante, String usuario) throws Throwable {
        System.out.println("guardarGarante - GarantePrestamo - préstamo: "
            + (garante != null ? garante.getIdPrestamo() : null) + " - código: "
            + (garante != null ? garante.getCodigo() : null));
        if (garante == null || garante.getIdPrestamo() == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el préstamo del garante es obligatorio");
        }
        usuario = usuarioValidacionService.validar(usuario);

        Prestamo prestamo = cargarPrestamoEditable(garante.getIdPrestamo());
        return guardarSobrePrestamo(garante, prestamo, usuario);
    }

    /**
     * Reglas comunes del alta/edición del garante, ya con el préstamo cargado y validado por quien llama:
     * {@link #guardarGarante} (préstamo en 6 o 1) y {@link #crearConAutorizacion} (6, 1 o vivo, con autorización).
     */
    private GarantePrestamo guardarSobrePrestamo(GarantePrestamo garante, Prestamo prestamo, String usuario)
            throws Throwable {
        // El garante es SIEMPRE partícipe del fondo (decisión del usuario 2026-11-11, DISENO-PAGO-CON-APORTES-DEL-GARANTE.md G1):
        // sin ENTDCDGO no hay aportes con qué responder. Rige en alta y en edición; los históricos sin él no se tocan.
        if (garante.getIdEntidad() == null) {
            throw new IncomeException("El garante debe ser partícipe del fondo: indique el partícipe (idEntidad)");
        }
        // Nombre y cédula salen de CRD.ENTD, no de la pantalla.
        if (garante.getIdEntidad() != null) {
            Entidad entidad = entidadDaoService.find(new Entidad(), garante.getIdEntidad());
            if (entidad == null) {
                throw new IncomeException("No existe el partícipe " + garante.getIdEntidad() + " indicado como garante");
            }
            garante.setNombre(entidad.getRazonSocial());
            garante.setCedula(entidad.getNumeroIdentificacion());
        }
        String nombre = garante.getNombre() != null ? garante.getNombre().trim() : "";
        String cedula = garante.getCedula() != null ? garante.getCedula().trim() : "";
        if (nombre.isEmpty() || cedula.isEmpty()) {
            throw new IncomeException("El nombre y la cédula del garante son obligatorios");
        }
        garante.setNombre(nombre);
        garante.setCedula(cedula);

        // La cédula del garante no puede ser la del deudor.
        String cedulaDeudor = prestamo.getEntidad() != null && prestamo.getEntidad().getNumeroIdentificacion() != null
            ? prestamo.getEntidad().getNumeroIdentificacion().trim() : "";
        if (cedula.equals(cedulaDeudor)
                || (garante.getIdEntidad() != null && prestamo.getEntidad() != null
                    && garante.getIdEntidad().equals(prestamo.getEntidad().getCodigo()))) {
            throw new IncomeException("El garante no puede ser el mismo deudor del préstamo");
        }

        GarantePrestamo activo = garantePrestamoDaoService.selectActivoByPrestamo(garante.getIdPrestamo());
        if (garante.getCodigo() == null) {
            if (activo != null) {
                throw new IncomeException("El préstamo " + garante.getIdPrestamo() + " ya tiene un garante activo ("
                    + activo.getNombre() + "); edítelo o anúlelo antes de agregar otro");
            }
            garante.setEstado(Long.valueOf(Estado.ACTIVO));
            garante.setUsuarioRegistro(usuario);
            garante.setFechaRegistro(LocalDateTime.now());
            garante.setUsuarioModificacion(null);
            garante.setFechaModificacion(null);
        } else {
            GarantePrestamo existente = garantePrestamoDaoService.find(new GarantePrestamo(), garante.getCodigo());
            if (existente == null) {
                throw new IncomeException("No existe el garante " + garante.getCodigo());
            }
            if (!existente.getIdPrestamo().equals(garante.getIdPrestamo())) {
                throw new IncomeException("El garante " + garante.getCodigo() + " no es del préstamo "
                    + garante.getIdPrestamo());
            }
            if (existente.getEstado() == null || existente.getEstado().longValue() != Estado.ACTIVO) {
                throw new IncomeException("El garante " + garante.getCodigo() + " está anulado; agregue uno nuevo");
            }
            // Auditoría de registro intacta; estado no lo decide el cliente.
            garante.setEstado(existente.getEstado());
            garante.setUsuarioRegistro(existente.getUsuarioRegistro());
            garante.setFechaRegistro(existente.getFechaRegistro());
            garante.setUsuarioModificacion(usuario);
            garante.setFechaModificacion(LocalDateTime.now());
        }
        return garantePrestamoDaoService.save(garante, garante.getCodigo());
    }

    @Override
    public GarantePrestamo anularGarante(Long idGarante, String usuario) throws Throwable {
        System.out.println("anularGarante - GarantePrestamo: " + idGarante);
        if (idGarante == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el código del garante es obligatorio");
        }
        usuario = usuarioValidacionService.validar(usuario);
        GarantePrestamo garante = garantePrestamoDaoService.find(new GarantePrestamo(), idGarante);
        if (garante == null) {
            throw new IncomeException("No existe el garante " + idGarante);
        }
        cargarPrestamoConAutorizacion(garante.getIdPrestamo());
        if (garante.getEstado() != null && garante.getEstado().longValue() == Estado.INACTIVO) {
            return garante; // ya estaba anulado: idempotente
        }
        garante.setEstado(Long.valueOf(Estado.INACTIVO));
        garante.setUsuarioModificacion(usuario);
        garante.setFechaModificacion(LocalDateTime.now());
        return garantePrestamoDaoService.save(garante, garante.getCodigo());
    }

    /** El préstamo del garante, que tiene que estar pendiente de aprobación (6) o generado (1). */
    private Prestamo cargarPrestamoEditable(Long idPrestamo) throws Throwable {
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        Long estado = prestamo.getIdEstado();
        if (EstadoPrestamo.esVivo(estado)) {
            throw new IncomeException("El préstamo " + idPrestamo + " ya está otorgado (estado " + estado + "): el garante"
                + " se registra con su autorización firmada (POST /rest/grpr/conAutorizacion); para cambiarlo, anule el"
                + " actual y agregue otro");
        }
        if (estado == null || (estado != EstadoPrestamo.PENDIENTE_DE_APROBACION && estado != EstadoPrestamo.GENERADO)) {
            throw new IncomeException("El garante solo se agrega, cambia o anula mientras el préstamo está pendiente de"
                + " aprobación (6) o generado (1); el préstamo " + idPrestamo + " está en estado " + estado);
        }
        return prestamo;
    }

    /**
     * El préstamo del garante para ANULAR o AGREGAR CON AUTORIZACIÓN: 6 y 1 como siempre, más los vivos (2, 8, 10, 11)
     * (DISENO-PAGO-CON-APORTES-DEL-GARANTE.md §G4).
     */
    private Prestamo cargarPrestamoConAutorizacion(Long idPrestamo) throws Throwable {
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        Long estado = prestamo.getIdEstado();
        boolean editable = estado != null
            && (estado == EstadoPrestamo.PENDIENTE_DE_APROBACION || estado == EstadoPrestamo.GENERADO);
        if (!editable && !EstadoPrestamo.esVivo(estado)) {
            throw new IncomeException("El garante solo se agrega o anula con el préstamo pendiente de aprobación (6),"
                + " generado (1) o vigente (2, 8, 10, 11); el préstamo " + idPrestamo + " está en estado " + estado);
        }
        return prestamo;
    }

    // ========================================================================
    // Garante de un préstamo ya otorgado, con autorización firmada (G4)
    // ========================================================================

    /** Subcarpeta bajo el directorio de uploads (ver FileService.uploadFileToPath). */
    private static final String CARPETA_AUTORIZACIONES = "crd/autorizaciones-garante";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoGaranteConAutorizacion crearConAutorizacion(SolicitudGaranteConAutorizacion solicitud)
            throws Throwable {
        System.out.println("crearConAutorizacion - GarantePrestamo - préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null) + " - partícipe: "
            + (solicitud != null ? solicitud.getIdEntidad() : null));

        if (solicitud == null || solicitud.getIdPrestamo() == null || solicitud.getIdEntidad() == null
                || solicitud.getArchivo() == null
                || solicitud.getNombreArchivo() == null || solicitud.getNombreArchivo().trim().isEmpty()) {
            throw new IncomeException("PARAMETRO_INVALIDO: idPrestamo, idEntidad y el archivo de la autorización"
                + " firmada del garante son obligatorios");
        }
        String usuario = usuarioValidacionService.validar(solicitud.getUsuario());

        // --- Archivo: PDF o imagen ------------------------------------------------------
        String nombreArchivo = solicitud.getNombreArchivo().trim();
        String minuscula = nombreArchivo.toLowerCase();
        String mime;
        if (minuscula.endsWith(".pdf")) {
            mime = "application/pdf";
        } else if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) {
            mime = "image/jpeg";
        } else if (minuscula.endsWith(".png")) {
            mime = "image/png";
        } else {
            throw new IncomeException("EXTENSION_NO_PERMITIDA: la autorización del garante debe ser un PDF o una"
                + " imagen (.pdf, .jpg, .jpeg, .png); recibido: " + nombreArchivo);
        }
        byte[] contenido = solicitud.getArchivo().readAllBytes();
        if (contenido.length == 0) {
            throw new IncomeException("ARCHIVO_VACIO: la autorización del garante llegó vacía");
        }
        if (!fileService.validarTamaño(contenido.length)) {
            throw new IncomeException("ARCHIVO_MUY_GRANDE: la autorización del garante pesa " + contenido.length
                + " bytes; el máximo es " + FileService.TAMAÑO_MAXIMO + " bytes (10 MB)");
        }

        // --- Reglas del garante y del préstamo (antes de tocar el disco) ------------------
        Prestamo prestamo = cargarPrestamoConAutorizacion(solicitud.getIdPrestamo());
        TipoAdjunto tipo = resolverTipoAutorizacion();

        // Archivo a disco ANTES de la base; si algo de abajo falla, el catch lo borra.
        String nombreUnico = fileService.generarNombreUnico(nombreArchivo);
        String rutaArchivo;
        try {
            rutaArchivo = fileService.uploadFileToPath(
                new ByteArrayInputStream(contenido), nombreUnico, CARPETA_AUTORIZACIONES);
        } catch (Throwable e) {
            throw new IncomeException("ERROR_ARCHIVO: no se pudo guardar la autorización del garante: " + e.getMessage());
        }

        try {
            GarantePrestamo nuevo = new GarantePrestamo();
            nuevo.setIdPrestamo(solicitud.getIdPrestamo());
            nuevo.setIdEntidad(solicitud.getIdEntidad());
            GarantePrestamo garante = guardarSobrePrestamo(nuevo, prestamo, usuario);

            Adjunto autorizacion = new Adjunto();
            autorizacion.setEntidad(prestamo.getEntidad());
            autorizacion.setPrestamo(prestamo);
            autorizacion.setIdReferencia(garante.getCodigo());
            autorizacion.setTipoAdjunto(tipo);
            autorizacion.setNombreArchivo(nombreArchivo);
            autorizacion.setUrlArchivo(rutaArchivo);
            autorizacion.setMimeType(mime);
            autorizacion.setEstado(Long.valueOf(Estado.ACTIVO));
            autorizacion.setFechaRegistro(LocalDateTime.now());
            autorizacion.setUsuarioRegistro(usuario);
            autorizacion = adjuntoDaoService.save(autorizacion, null);
            System.out.println("  📎 Adjunto (autorización del garante) creado: " + autorizacion.getCodigo()
                + " -> GRPR " + garante.getCodigo());

            return new ResultadoGaranteConAutorizacion(garante, autorizacion);
        } catch (Throwable e) {
            // La BD se revierte sola (IncomeException / excepción de persistencia); el archivo en disco no.
            try {
                fileService.deleteFile(rutaArchivo);
                System.err.println("  🧹 Archivo huérfano borrado tras fallo: " + rutaArchivo);
            } catch (Throwable borrado) {
                System.err.println("  ⚠️ No se pudo borrar el archivo huérfano " + rutaArchivo
                    + ": " + borrado.getMessage());
            }
            throw e;
        }
    }

    @Override
    public Adjunto obtenerAutorizacion(Long idGarante) throws Throwable {
        System.out.println("obtenerAutorizacion - GarantePrestamo: " + idGarante);
        if (idGarante == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el código del garante es obligatorio");
        }
        TipoAdjunto tipo = resolverTipoAutorizacion();
        List<Adjunto> encontrados = adjuntoDaoService.selectByReferenciaYTipo(idGarante, tipo.getCodigo());
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    /** «AUTORIZACION DEL GARANTE» de CRD.TPDJ: exactamente una fila activa, como «CERTIFICADO BANCARIO». */
    private TipoAdjunto resolverTipoAutorizacion() throws Throwable {
        List<TipoAdjunto> tipos = tipoAdjuntoDaoService.selectByNombre(AUTORIZACION_DEL_GARANTE);
        if (tipos.isEmpty()) {
            throw new IncomeException("TIPO_ADJUNTO_AUTORIZACION_NO_CONFIGURADO: Falta el tipo de adjunto "
                + AUTORIZACION_DEL_GARANTE + " (correr crd/sql/e3-17)");
        }
        if (tipos.size() > 1) {
            StringBuilder ids = new StringBuilder();
            for (TipoAdjunto t : tipos) {
                ids.append(ids.length() > 0 ? ", " : "").append(t.getCodigo());
            }
            throw new IncomeException("TIPO_ADJUNTO_AUTORIZACION_NO_CONFIGURADO: hay " + tipos.size()
                + " tipos de adjunto activos llamados '" + AUTORIZACION_DEL_GARANTE + "' (ids: " + ids
                + "); debe quedar uno solo activo");
        }
        return tipos.get(0);
    }
}
