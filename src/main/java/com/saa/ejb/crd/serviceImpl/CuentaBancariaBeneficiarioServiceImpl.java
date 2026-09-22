package com.saa.ejb.crd.serviceImpl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.ejb.crd.dao.CuentaBancariaBeneficiarioDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.ejb.crd.service.CuentaBancariaBeneficiarioService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaBeneficiarioConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaBeneficiarioConCertificado;
import com.saa.ejb.tsr.dao.BancoExternoDaoService;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaBeneficiario;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoAdjunto;
import com.saa.model.tsr.BancoExterno;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuentasBancarias;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class CuentaBancariaBeneficiarioServiceImpl implements CuentaBancariaBeneficiarioService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @EJB
    private CuentaBancariaBeneficiarioDaoService cuentaBancariaBeneficiarioDaoService;

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private BancoExternoDaoService bancoExternoDaoService;

    @EJB
    private FileService fileService;

    /** Subcarpeta bajo el directorio de uploads (ver FileService.uploadFileToPath). */
    private static final String CARPETA_CERTIFICADOS = "crd/certificados-beneficiarios";

    @Override
    public CuentaBancariaBeneficiario selectById(Long id) throws Throwable {
        System.out.println("selectById - CuentaBancariaBeneficiario: " + id);
        return cuentaBancariaBeneficiarioDaoService.selectById(id, NombreEntidadesCredito.CUENTA_BANCARIA_BENEFICIARIO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - CuentaBancariaBeneficiario");
        CuentaBancariaBeneficiario entidad = new CuentaBancariaBeneficiario();
        for (Long registro : id) {
            cuentaBancariaBeneficiarioDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CuentaBancariaBeneficiario> lista) throws Throwable {
        System.out.println("save list - CuentaBancariaBeneficiario");
        for (CuentaBancariaBeneficiario registro : lista) {
            cuentaBancariaBeneficiarioDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuentaBancariaBeneficiario> selectAll() throws Throwable {
        System.out.println("selectAll - CuentaBancariaBeneficiario");
        List<CuentaBancariaBeneficiario> result = cuentaBancariaBeneficiarioDaoService.selectAll(NombreEntidadesCredito.CUENTA_BANCARIA_BENEFICIARIO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros CuentaBancariaBeneficiario");
        }
        return result;
    }

    @Override
    public CuentaBancariaBeneficiario saveSingle(CuentaBancariaBeneficiario cuenta) throws Throwable {
        System.out.println("saveSingle - CuentaBancariaBeneficiario");
        if (cuenta.getCodigo() == null) {
            cuenta.setEstado(Long.valueOf(EstadoCuentasBancarias.ACTIVO));
        }
        return cuentaBancariaBeneficiarioDaoService.save(cuenta, cuenta.getCodigo());
    }

    @Override
    public List<CuentaBancariaBeneficiario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - CuentaBancariaBeneficiario");
        List<CuentaBancariaBeneficiario> result = cuentaBancariaBeneficiarioDaoService.selectByCriteria(datos, NombreEntidadesCredito.CUENTA_BANCARIA_BENEFICIARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CuentaBancariaBeneficiario no devolvio registros");
        }
        return result;
    }

    @Override
    public List<CuentaBancariaBeneficiario> selectPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("selectPorEntidad - CuentaBancariaBeneficiarioService - idEntidad: " + idEntidad);
        return cuentaBancariaBeneficiarioDaoService.selectPorEntidad(idEntidad);
    }

    // ========================================================================
    // Beneficiario + certificado, atómico
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoCuentaBancariaBeneficiarioConCertificado crearConCertificado(
            SolicitudCuentaBancariaBeneficiarioConCertificado solicitud) throws Throwable {
        System.out.println("crearConCertificado - CuentaBancariaBeneficiario - idEntidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null));

        if (solicitud == null
                || solicitud.getIdEntidad() == null
                || solicitud.getNombre() == null || solicitud.getNombre().trim().isEmpty()
                || solicitud.getNumeroIdentificacion() == null || solicitud.getNumeroIdentificacion().trim().isEmpty()
                || solicitud.getIdBancoExterno() == null
                || solicitud.getTipoCuenta() == null
                || solicitud.getNumeroCuenta() == null || solicitud.getNumeroCuenta().trim().isEmpty()
                || solicitud.getPorcentaje() == null
                || solicitud.getArchivo() == null
                || solicitud.getNombreArchivo() == null || solicitud.getNombreArchivo().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": idEntidad, nombre, numeroIdentificacion, idBancoExterno, tipoCuenta, numeroCuenta,"
                + " porcentaje y el archivo son obligatorios");
        }

        // --- El porcentaje: (0, 100], NO se valida contra los hermanos (eso es la fase de pago) ---
        if (solicitud.getPorcentaje().compareTo(BigDecimal.ZERO) <= 0
                || solicitud.getPorcentaje().compareTo(CIEN) > 0) {
            throw new IncomeException(ERR_PORCENTAJE_INVALIDO
                + ": el porcentaje debe estar entre 0 (exclusivo) y 100 (inclusive), recibido: "
                + solicitud.getPorcentaje());
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

        // --- 409: mismo precedente que RVSG/CBCR — chequear antes, no dejar salir el ORA-00001 ---
        String identificacion = solicitud.getNumeroIdentificacion().trim();
        List<CuentaBancariaBeneficiario> existentes =
            cuentaBancariaBeneficiarioDaoService.selectPorEntidadEIdentificacion(solicitud.getIdEntidad(), identificacion);
        if (existentes != null && !existentes.isEmpty()) {
            throw new IncomeException(ERR_BENEFICIARIO_DUPLICADO
                + ": ya existe el beneficiario " + existentes.get(0).getCodigo()
                + " con identificación " + identificacion + " para el partícipe " + solicitud.getIdEntidad());
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
            CuentaBancariaBeneficiario beneficiario = new CuentaBancariaBeneficiario();
            beneficiario.setEntidad(entidad);
            beneficiario.setNombre(solicitud.getNombre().trim());
            beneficiario.setNumeroIdentificacion(identificacion);
            beneficiario.setBancoExterno(banco);
            beneficiario.setTipoCuenta(solicitud.getTipoCuenta());
            beneficiario.setNumeroCuenta(solicitud.getNumeroCuenta().trim());
            beneficiario.setPorcentaje(solicitud.getPorcentaje());
            beneficiario.setEstado(Long.valueOf(EstadoCuentasBancarias.ACTIVO));
            beneficiario.setUsuarioRegistro(solicitud.getUsuarioRegistro());
            beneficiario.setFechaRegistro(LocalDate.now());
            beneficiario = cuentaBancariaBeneficiarioDaoService.save(beneficiario, null);
            System.out.println("  💾 CuentaBancariaBeneficiario creado: " + beneficiario.getCodigo());

            Adjunto certificado = new Adjunto();
            certificado.setEntidad(entidad);
            certificado.setIdReferencia(beneficiario.getCodigo());
            certificado.setTipoAdjunto(tipoCertificado);
            certificado.setNombreArchivo(nombreArchivo);
            certificado.setUrlArchivo(rutaArchivo);
            certificado.setMimeType("application/pdf");
            certificado.setEstado(Long.valueOf(Estado.ACTIVO));
            certificado.setFechaRegistro(java.time.LocalDateTime.now());
            certificado.setUsuarioRegistro(solicitud.getUsuarioRegistro());
            certificado = adjuntoDaoService.save(certificado, null);
            System.out.println("  📎 Adjunto (certificado bancario) creado: " + certificado.getCodigo()
                + " -> CBBP " + beneficiario.getCodigo());

            return new ResultadoCuentaBancariaBeneficiarioConCertificado(beneficiario, certificado);

        } catch (Throwable e) {
            // La transacción de BD se revierte sola (IncomeException/RuntimeException con
            // @ApplicationException(rollback=true), o excepción de persistencia, ambas
            // deshacen CBBP y ADJN juntos). Lo que NO revierte solo es el archivo en disco.
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
    public CuentaBancariaBeneficiario actualizar(CuentaBancariaBeneficiario cambio) throws Throwable {
        System.out.println("actualizar - CuentaBancariaBeneficiario: "
            + (cambio != null ? cambio.getCodigo() : null));
        if (cambio == null || cambio.getCodigo() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el código del beneficiario es obligatorio");
        }
        if (cambio.getPorcentaje() == null
                || cambio.getPorcentaje().compareTo(BigDecimal.ZERO) <= 0
                || cambio.getPorcentaje().compareTo(CIEN) > 0) {
            throw new IncomeException(ERR_PORCENTAJE_INVALIDO
                + ": el porcentaje debe estar entre 0 (exclusivo) y 100 (inclusive), recibido: "
                + (cambio.getPorcentaje() != null ? cambio.getPorcentaje() : "null"));
        }

        CuentaBancariaBeneficiario existente = cuentaBancariaBeneficiarioDaoService.find(
            new CuentaBancariaBeneficiario(), cambio.getCodigo());
        if (existente == null) {
            throw new IncomeException(ERR_BENEFICIARIO_NO_ENCONTRADO
                + ": no existe el beneficiario " + cambio.getCodigo());
        }

        // NO se toca entidad ni numeroIdentificacion (§3.4 del contrato): otro beneficiario
        // se registra aparte, no se "convierte" éste.
        existente.setPorcentaje(cambio.getPorcentaje());
        existente.setEstado(cambio.getEstado());
        existente.setTipoCuenta(cambio.getTipoCuenta());
        existente.setNumeroCuenta(cambio.getNumeroCuenta());
        existente.setBancoExterno(cambio.getBancoExterno());
        existente.setNombre(cambio.getNombre());

        return cuentaBancariaBeneficiarioDaoService.save(existente, existente.getCodigo());
    }

    /**
     * Resuelve el {@code TipoAdjunto} "CERTIFICADO BANCARIO" del catálogo CRD.TPDJ. Mismo
     * criterio que {@code CuentaBancariaParticipeServiceImpl.resolverTipoCertificadoBancario}:
     * exige EXACTAMENTE una fila activa, ni cero ni más de una — no se adivina sobre una consulta
     * sin ORDER BY.
     */
    private TipoAdjunto resolverTipoCertificadoBancario() throws Throwable {
        List<TipoAdjunto> tipos = tipoAdjuntoDaoService.selectByNombre(CERTIFICADO_BANCARIO);
        if (tipos.isEmpty()) {
            throw new IncomeException(ERR_TIPO_ADJUNTO_NO_CONFIGURADO
                + ": falta cargar '" + CERTIFICADO_BANCARIO + "' en CRD.TPDJ"
                + " (docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql)");
        }
        if (tipos.size() > 1) {
            StringBuilder ids = new StringBuilder();
            for (TipoAdjunto tipo : tipos) {
                if (ids.length() > 0) {
                    ids.append(", ");
                }
                ids.append(tipo.getCodigo());
            }
            throw new IncomeException(ERR_TIPO_ADJUNTO_NO_CONFIGURADO + ": hay " + tipos.size()
                + " tipos de adjunto activos llamados '" + CERTIFICADO_BANCARIO + "' en CRD.TPDJ"
                + " (ids: " + ids + "); no se puede saber cuál vale. Debe quedar uno solo activo.");
        }
        return tipos.get(0);
    }
}
