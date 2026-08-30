package com.saa.ejb.crd.serviceImpl;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.ejb.crd.service.CuentaBancariaParticipeService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaConCertificado;
import com.saa.ejb.tsr.dao.BancoExternoDaoService;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoAdjunto;
import com.saa.model.tsr.BancoExterno;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class CuentaBancariaParticipeServiceImpl implements CuentaBancariaParticipeService {

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private EntidadService entidadService;

    @EJB
    private BancoExternoDaoService bancoExternoDaoService;

    @EJB
    private FileService fileService;

    /** Subcarpeta bajo el directorio de uploads (ver FileService.uploadFileToPath). */
    private static final String CARPETA_CERTIFICADOS = "crd/certificados-bancarios";

    @Override
    public CuentaBancariaParticipe selectById(Long id) throws Throwable {
        System.out.println("selectById - CuentaBancariaParticipe: " + id);
        return cuentaBancariaParticipeDaoService.selectById(id, NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - CuentaBancariaParticipe");
        CuentaBancariaParticipe entidad = new CuentaBancariaParticipe();
        for (Long registro : id) {
            cuentaBancariaParticipeDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CuentaBancariaParticipe> lista) throws Throwable {
        System.out.println("save list - CuentaBancariaParticipe");
        for (CuentaBancariaParticipe registro : lista) {
            cuentaBancariaParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuentaBancariaParticipe> selectAll() throws Throwable {
        System.out.println("selectAll - CuentaBancariaParticipe");
        List<CuentaBancariaParticipe> result = cuentaBancariaParticipeDaoService.selectAll(NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros CuentaBancariaParticipe");
        }
        return result;
    }

    @Override
    public CuentaBancariaParticipe saveSingle(CuentaBancariaParticipe cuenta) throws Throwable {
        System.out.println("saveSingle - CuentaBancariaParticipe");
        if (cuenta.getCodigo() == null) {
            cuenta.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return cuentaBancariaParticipeDaoService.save(cuenta, cuenta.getCodigo());
    }

    @Override
    public List<CuentaBancariaParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - CuentaBancariaParticipe");
        List<CuentaBancariaParticipe> result = cuentaBancariaParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CuentaBancariaParticipe no devolvio registros");
        }
        return result;
    }

    @Override
    public List<CuentaBancariaParticipe> selectByParent(Long idEntidad) throws Throwable {
        System.out.println("selectByParent CuentaBancariaParticipeService idEntidad: " + idEntidad);
        return cuentaBancariaParticipeDaoService.selectByParent(idEntidad);
    }

    @Override
    public CuentaBancariaParticipe saveSingle(CuentaBancariaParticipe cuenta, String usuario) throws Throwable {
        System.out.println("saveSingle(CuentaBancariaParticipe, usuario) - usuario: " + usuario);
        cuenta = saveSingle(cuenta);
        entidadService.sellarActualizacion(cuenta.getEntidad().getCodigo(), usuario);
        return cuenta;
    }

    // ========================================================================
    // Cuenta + certificado, atómico
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoCuentaBancariaConCertificado crearConCertificado(SolicitudCuentaBancariaConCertificado solicitud)
            throws Throwable {
        System.out.println("crearConCertificado - CuentaBancariaParticipe - idEntidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null));

        if (solicitud == null
                || solicitud.getIdEntidad() == null
                || solicitud.getIdBancoExterno() == null
                || solicitud.getTipoCuenta() == null
                || solicitud.getNumeroCuenta() == null || solicitud.getNumeroCuenta().trim().isEmpty()
                || solicitud.getArchivo() == null
                || solicitud.getNombreArchivo() == null || solicitud.getNombreArchivo().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": idEntidad, idBancoExterno, tipoCuenta, numeroCuenta y el archivo son obligatorios");
        }

        // --- Archivo: SOLO PDF acá, aunque FileService permita más extensiones ---------
        String nombreArchivo = solicitud.getNombreArchivo().trim();
        if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
            throw new IncomeException(ERR_EXTENSION_NO_PERMITIDA
                + ": el certificado bancario debe ser un archivo .pdf (recibido: " + nombreArchivo + ")");
        }

        byte[] contenido = solicitud.getArchivo().readAllBytes();
        if (contenido.length == 0) {
            throw new IncomeException(ERR_ARCHIVO_VACIO + ": el certificado bancario llegó vacío");
        }
        if (!fileService.validarTamaño(contenido.length)) {
            throw new IncomeException(ERR_ARCHIVO_MUY_GRANDE
                + ": el certificado bancario pesa " + contenido.length
                + " bytes; el máximo es " + FileService.TAMAÑO_MAXIMO + " bytes (10 MB)");
        }

        // --- Referencias obligatorias ---------------------------------------------------
        Entidad entidad = entidadDaoService.findById(solicitud.getIdEntidad());
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA
                + ": no existe el partícipe " + solicitud.getIdEntidad());
        }

        BancoExterno banco = bancoExternoDaoService.find(new BancoExterno(), solicitud.getIdBancoExterno());
        if (banco == null) {
            throw new IncomeException(ERR_BANCO_NO_ENCONTRADO
                + ": no existe el banco " + solicitud.getIdBancoExterno());
        }

        TipoAdjunto tipoCertificado = resolverTipoCertificadoBancario();

        // --- Subir el archivo a disco ANTES de tocar la base. Si algo de acá para abajo
        // falla, el catch de más abajo lo borra: no queda archivo colgado. ----------------
        String nombreUnico = fileService.generarNombreUnico(nombreArchivo);
        String rutaArchivo;
        try {
            rutaArchivo = fileService.uploadFileToPath(
                new ByteArrayInputStream(contenido), nombreUnico, CARPETA_CERTIFICADOS);
        } catch (Throwable e) {
            throw new IncomeException("ERROR_ARCHIVO: no se pudo guardar el certificado bancario: " + e.getMessage());
        }

        try {
            CuentaBancariaParticipe cuenta = new CuentaBancariaParticipe();
            cuenta.setEntidad(entidad);
            cuenta.setBancoExterno(banco);
            cuenta.setTipoCuenta(solicitud.getTipoCuenta());
            cuenta.setNumeroCuenta(solicitud.getNumeroCuenta().trim());
            cuenta.setEstado(Long.valueOf(Estado.ACTIVO));
            cuenta = cuentaBancariaParticipeDaoService.save(cuenta, null);
            System.out.println("  💾 CuentaBancariaParticipe creada: " + cuenta.getCodigo());

            Adjunto certificado = new Adjunto();
            certificado.setEntidad(entidad);
            certificado.setIdReferencia(cuenta.getCodigo());
            certificado.setTipoAdjunto(tipoCertificado);
            certificado.setNombreArchivo(nombreArchivo);
            certificado.setUrlArchivo(rutaArchivo);
            certificado.setMimeType("application/pdf");
            certificado.setEstado(Long.valueOf(Estado.ACTIVO));
            certificado.setFechaRegistro(LocalDateTime.now());
            certificado.setUsuarioRegistro(solicitud.getUsuarioRegistro());
            certificado = adjuntoDaoService.save(certificado, null);
            System.out.println("  📎 Adjunto (certificado bancario) creado: " + certificado.getCodigo()
                + " -> CNBP " + cuenta.getCodigo());

            entidadService.sellarActualizacion(entidad.getCodigo(), solicitud.getUsuarioRegistro());

            return new ResultadoCuentaBancariaConCertificado(cuenta, certificado);

        } catch (Throwable e) {
            // La transacción de BD se revierte sola (IncomeException/RuntimeException con
            // @ApplicationException(rollback=true), o excepción de persistencia, ambas
            // deshacen CNBP y ADJN juntos). Lo que NO revierte solo es el archivo en disco.
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
    public Adjunto obtenerCertificado(Long idCuenta) throws Throwable {
        System.out.println("obtenerCertificado - CuentaBancariaParticipe: " + idCuenta);
        if (idCuenta == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idCuenta es obligatorio");
        }
        TipoAdjunto tipoCertificado = resolverTipoCertificadoBancario();
        List<Adjunto> encontrados = adjuntoDaoService.selectByReferenciaYTipo(idCuenta, tipoCertificado.getCodigo());
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    /**
     * Resuelve el {@code TipoAdjunto} "CERTIFICADO BANCARIO" del catálogo CRD.TPDJ. No hay un
     * código fijo en código: se busca por nombre porque el ID real solo existe después de correr
     * docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql en cada ambiente.
     */
    private TipoAdjunto resolverTipoCertificadoBancario() throws Throwable {
        List<TipoAdjunto> tipos = tipoAdjuntoDaoService.selectByNombre(CERTIFICADO_BANCARIO);
        if (tipos.isEmpty()) {
            throw new IncomeException(ERR_TIPO_ADJUNTO_NO_CONFIGURADO
                + ": falta cargar '" + CERTIFICADO_BANCARIO + "' en CRD.TPDJ"
                + " (docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql)");
        }
        return tipos.get(0);
    }
}
