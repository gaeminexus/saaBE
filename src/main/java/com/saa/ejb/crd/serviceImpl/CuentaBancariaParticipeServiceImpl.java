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
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.ejb.crd.service.CuentaBancariaParticipeService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaConCertificado;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.tsr.dao.BancoExternoDaoService;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAdjunto;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.BancoExterno;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.OrigenPagoExterno;

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

    // Sin PrestamoService a proposito: PrestamoServiceImpl ya inyecta este servicio (dependencia circular).
    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private PagoProgramadoDaoService pagoProgramadoDaoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

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
            // El idEntidad se lee ANTES de borrar: despues la fila ya no existe.
            CuentaBancariaParticipe existente = cuentaBancariaParticipeDaoService.find(new CuentaBancariaParticipe(), registro);
            Long idEntidad = existente != null && existente.getEntidad() != null
                ? existente.getEntidad().getCodigo() : null;
            cuentaBancariaParticipeDaoService.remove(entidad, registro);
            sincronizarOrdenesDesembolso(idEntidad);
        }
    }

    @Override
    public void save(List<CuentaBancariaParticipe> lista) throws Throwable {
        System.out.println("save list - CuentaBancariaParticipe");
        for (CuentaBancariaParticipe registro : lista) {
            cuentaBancariaParticipeDaoService.save(registro, registro.getCodigo());
            sincronizarOrdenesDesembolso(registro.getEntidad() != null ? registro.getEntidad().getCodigo() : null);
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
        CuentaBancariaParticipe guardada = cuentaBancariaParticipeDaoService.save(cuenta, cuenta.getCodigo());
        sincronizarOrdenesDesembolso(guardada != null && guardada.getEntidad() != null
            ? guardada.getEntidad().getCodigo() : null);
        return guardada;
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
            sincronizarOrdenesDesembolso(entidad.getCodigo());

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
     *
     * ⛔⛔ Corrección 2026-09-04: exige EXACTAMENTE una fila activa, ni cero ni más de una — mismo
     * criterio que {@code unicaCuentaActiva}/{@code unicaActiva} para cuentas bancarias y VPPC
     * ("ni cero ni más de una: no se adivina"). Antes, con dos o más, {@code tipos.get(0)}
     * elegía en silencio sobre una consulta SIN {@code ORDER BY} — Oracle no garantiza el orden
     * de un resultado sin ordenar, así que esa elección podía cambiar de un día para el otro sin
     * ningún error visible. Y desde que el certificado gobierna si sale dinero (§6/D2 del
     * contrato de pago de pensión), un {@code get(0)} equivocado no es un dato mal mostrado: es
     * plata que deja de salir para TODOS los jubilados, sin ninguna alerta.
     *
     * ⚠️ Esta validación DEPENDE de que CRD.TPDJ tenga una sola fila activa "CERTIFICADO
     * BANCARIO". Mientras existan las dos que hay hoy (ids 4 y 37, medido 2026-09-04), este
     * método va a fallar SIEMPRE con {@link #ERR_TIPO_ADJUNTO_NO_CONFIGURADO} — es el
     * comportamiento correcto (preferible a pagar según una lotería de plan de ejecución), pero
     * bloquea a todos los jubilados hasta que se corra sql/193 para dejar una sola fila activa.
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
                + " (ids: " + ids + "); no se puede saber cuál vale. Debe quedar uno solo activo"
                + " (correr sql/193).");
        }
        return tipos.get(0);
    }

    private List<CuentaBancariaParticipe> cuentasActivas(Long idEntidad) {
        List<CuentaBancariaParticipe> activas = new java.util.ArrayList<>();
        List<CuentaBancariaParticipe> cuentas = idEntidad != null
            ? cuentaBancariaParticipeDaoService.selectByParent(idEntidad) : null;
        if (cuentas != null) {
            for (CuentaBancariaParticipe cuenta : cuentas) {
                if (cuenta.getEstado() != null && cuenta.getEstado() == Estado.ACTIVO) {
                    activas.add(cuenta);
                }
            }
        }
        return activas;
    }

    @Override
    public CuentaBancariaParticipe cuentaActivaUnica(Long idEntidad) throws Throwable {
        List<CuentaBancariaParticipe> activas = cuentasActivas(idEntidad);
        return activas.size() == 1 ? activas.get(0) : null;
    }

    @Override
    public String motivoSinCuentaActivaUnica(Long idEntidad) throws Throwable {
        List<CuentaBancariaParticipe> lista = cuentasActivas(idEntidad);
        int activas = lista.size();
        if (activas == 1) {
            // D1: una sola activa no basta, tiene que estar completa (si no, la orden nace sin cuenta de destino).
            CuentaBancariaParticipe unica = lista.get(0);
            List<String> faltan = new java.util.ArrayList<>();
            if (unica.getBancoExterno() == null) {
                faltan.add("banco");
            }
            if (unica.getTipoCuenta() == null) {
                faltan.add("tipo de cuenta");
            }
            if (unica.getNumeroCuenta() == null || unica.getNumeroCuenta().trim().isEmpty()) {
                faltan.add("número");
            }
            return faltan.isEmpty() ? null
                : "La cuenta bancaria activa del socio no tiene " + String.join(" / ", faltan)
                    + "; complétela antes de aprobar";
        }
        return activas == 0
            ? "El socio no tiene una cuenta bancaria activa registrada; registre una (y deje una sola activa) antes de aprobar"
            : "El socio tiene " + activas + " cuentas bancarias activas; deje una sola activa antes de aprobar";
    }

    // ========================================================================
    // D2: mantener al dia la cuenta de destino de las ordenes de desembolso pendientes
    // ========================================================================

    /**
     * Tras escribir CRD.CNBP de un socio, copia (o vacia) banco/tipo/numero de la cuenta activa unica y completa en
     * las ordenes PGS.PGTR de desembolso de sus prestamos APROBADOS que siguen POR_APROBAR. Misma transaccion que la
     * escritura de la cuenta: si falla aca, falla el guardado. Nunca toca una orden que ya salio de POR_APROBAR.
     */
    private void sincronizarOrdenesDesembolso(Long idEntidad) throws Throwable {
        if (idEntidad == null) {
            return;
        }
        System.out.println("sincronizarOrdenesDesembolso - entidad: " + idEntidad);
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidadEstadoConOrdenPago(
            idEntidad, Long.valueOf(EstadoPrestamo.APROBADO));
        if (prestamos == null || prestamos.isEmpty()) {
            return;
        }
        Long idBanco = null;
        Long tipo = null;
        String numero = null;
        List<CuentaBancariaParticipe> activas = cuentasActivas(idEntidad);
        if (activas.size() == 1) {
            CuentaBancariaParticipe unica = activas.get(0);
            if (unica.getBancoExterno() != null && unica.getTipoCuenta() != null
                    && unica.getNumeroCuenta() != null && !unica.getNumeroCuenta().trim().isEmpty()) {
                idBanco = unica.getBancoExterno().getCodigo();
                tipo = unica.getTipoCuenta();
                numero = unica.getNumeroCuenta().trim();
            }
        }
        for (Prestamo prestamo : prestamos) {
            PagoProgramado pago = pagoProgramadoDaoService.find(new PagoProgramado(), prestamo.getIdPagoProgramado());
            if (pago == null
                    || pago.getEstado() == null || pago.getEstado() != EstadoPagoProgramado.POR_APROBAR
                    || !OrigenPagoExterno.CRD_DESEMBOLSO_PRESTAMO.equals(pago.getOrigenExterno())
                    || pago.getIdOrigen() == null || !pago.getIdOrigen().equals(prestamo.getCodigo())) {
                continue;
            }
            Long bancoAntes = pago.getBeneficiarioBanco() != null ? pago.getBeneficiarioBanco().getCodigo() : null;
            String numeroAntes = pago.getBeneficiarioCuenta() != null ? pago.getBeneficiarioCuenta().trim() : null;
            boolean igual = java.util.Objects.equals(bancoAntes, idBanco)
                && java.util.Objects.equals(pago.getBeneficiarioTipoCuenta(), tipo)
                && java.util.Objects.equals(numeroAntes, numero);
            if (igual) {
                continue;
            }
            pagoProgramadoService.actualizarBeneficiarioOcasional(pago.getId(), idBanco, tipo, numero);
            System.out.println("  Orden " + pago.getId() + " (prestamo " + prestamo.getCodigo() + "): cuenta destino "
                + ultimos4(numeroAntes) + " -> " + ultimos4(numero));
        }
    }

    /** Solo los ultimos 4 digitos: el numero completo no va al log. */
    private String ultimos4(String numero) {
        if (numero == null || numero.isEmpty()) {
            return "(vacia)";
        }
        return "****" + (numero.length() > 4 ? numero.substring(numero.length() - 4) : numero);
    }
}
