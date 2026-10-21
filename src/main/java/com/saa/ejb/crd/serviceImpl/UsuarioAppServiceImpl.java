package com.saa.ejb.crd.serviceImpl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.UsuarioAppDaoService;
import com.saa.ejb.crd.service.UsuarioAppService;
import com.saa.ejb.crd.service.ValidacionException;
import com.saa.ejb.crd.service.dto.SolicitudCambiarClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudConfirmarRecuperacionUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudCrearUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudDesactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudReactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudResetearClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudSolicitarRecuperacionUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudValidarCredencial;
import com.saa.ejb.crd.service.dto.ValidarCredencialResponse;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.UsuarioApp;
import com.saa.rubros.EstadoUsuarioApp;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javax.naming.InitialContext;

/**
 * @see UsuarioAppService
 * @author Sistema SAA
 * @since 2026-09-03
 */
@Stateless
public class UsuarioAppServiceImpl implements UsuarioAppService {

    private static final String MSG_CREDENCIALES_INVALIDAS = "Credenciales invalidas";

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long MINUTOS_BLOQUEO = 15;

    private static final String MSG_CODIGO_INVALIDO = "Codigo invalido o vencido";
    private static final String JNDI_SESION_CORREO = "java:jboss/mail/AppMovil";
    private static final long MINUTOS_VIGENCIA_CODIGO = 15;
    private static final long SEGUNDOS_ENTRE_ENVIOS = 60;
    private static final long HORAS_VENTANA_ENVIOS = 24;
    private static final int MAX_ENVIOS_VENTANA = 5;
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final Pattern PATRON_CODIGO = Pattern.compile("^\\d{6}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int PBKDF2_ITERACIONES = 100_000;
    private static final int PBKDF2_LARGO_SALT = 16;
    private static final int PBKDF2_LARGO_CLAVE_BITS = 256;
    private static final String PBKDF2_ALGORITMO = "PBKDF2WithHmacSHA256";

    @EJB
    private UsuarioAppDaoService usuarioAppDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ValidarCredencialResponse validarCredencial(SolicitudValidarCredencial solicitud) throws Throwable {
        System.out.println("UsuarioAppService.validarCredencial - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));

        UsuarioApp usap = autenticar(
                solicitud != null ? solicitud.getIdentificacion() : null,
                solicitud != null ? solicitud.getClave() : null);

        usap.setFechaUltimoAcceso(LocalDateTime.now());
        usuarioAppDaoService.save(usap, usap.getCodigo());

        Entidad entidad = usap.getEntidad();
        ValidarCredencialResponse respuesta = new ValidarCredencialResponse();
        respuesta.setIdEntidad(entidad.getCodigo());
        respuesta.setIdentificacion(usap.getIdentificacion());
        respuesta.setDebeCambiarClave(Long.valueOf(1L).equals(usap.getDebeCambiarClave()));

        // El nombre del partícipe vive en CRD.ENTD.ENTDRZNS (razonSocial), no en CRD.PRSN:
        // PRSN no es la fuente de datos del partícipe (ver CONTRATO-INTRANET-MOVIL.md).
        // Antes se consultaba PersonaNatural acá — se sacó porque además de ser la tabla
        // equivocada, PRSN.PRSNESCV (estado civil) está mapeado como Long pero la columna
        // real es VARCHAR2(2000) (texto: "CASADO(A)"), y esa consulta reventaba con
        // "Could not extract column [4]" / T4CVarcharAccessor.getLong en cualquier
        // partícipe con estado civil cargado. Ese desajuste es de otro equipo (crd) y no
        // se toca; acá alcanza con no depender de esa tabla. razonSocial es un campo único
        // (nombre completo), por eso apellidos viaja en null — partirlo por espacios
        // inventaría dónde terminan los apellidos.
        respuesta.setNombres(entidad.getRazonSocial());
        respuesta.setApellidos(null);

        return respuesta;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void cambiarClave(SolicitudCambiarClaveUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.cambiarClave - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));
        if (solicitud == null) {
            throw new IncomeException("La solicitud es obligatoria");
        }

        UsuarioApp usap = autenticar(solicitud.getIdentificacion(), solicitud.getClaveActual());
        validarPoliticaClave(solicitud.getClaveNueva());

        usap.setClaveHash(generarHash(solicitud.getClaveNueva()));
        usap.setDebeCambiarClave(0L);
        usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UsuarioApp crear(SolicitudCrearUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.crear - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));
        if (solicitud == null || esVacio(solicitud.getIdentificacion()) || esVacio(solicitud.getClaveTemporal())
                || esVacio(solicitud.getUsuario())) {
            throw new IncomeException("identificacion, claveTemporal y usuario son obligatorios");
        }
        String identificacion = solicitud.getIdentificacion().trim();

        Entidad entidad = entidadDaoService.selectByNumeroIdentificacion(identificacion);
        if (entidad == null) {
            throw new IncomeException("No existe un participe (CRD.ENTD) con la identificacion " + identificacion);
        }
        if (usuarioAppDaoService.selectByEntidad(entidad.getCodigo()) != null) {
            throw new IncomeException("El participe " + identificacion + " ya tiene una credencial de app registrada");
        }

        UsuarioApp usap = new UsuarioApp();
        usap.setEntidad(entidad);
        usap.setIdentificacion(identificacion);
        usap.setClaveHash(generarHash(solicitud.getClaveTemporal()));
        usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
        usap.setIntentosFallidos(0L);
        usap.setDebeCambiarClave(1L);
        usap.setFechaCreacion(LocalDateTime.now());
        usap.setUsuarioRegistro(solicitud.getUsuario().trim());
        return usuarioAppDaoService.save(usap, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UsuarioApp resetearClave(SolicitudResetearClaveUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.resetearClave - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));
        if (solicitud == null || esVacio(solicitud.getIdentificacion()) || esVacio(solicitud.getClaveTemporal())
                || esVacio(solicitud.getUsuario())) {
            throw new IncomeException("identificacion, claveTemporal y usuario son obligatorios");
        }

        UsuarioApp usap = usuarioAppDaoService.selectByIdentificacion(solicitud.getIdentificacion().trim());
        if (usap == null) {
            throw new IncomeException("No existe credencial de app para la identificacion "
                    + solicitud.getIdentificacion());
        }
        // Revertir un borrado es un acto deliberado y separado (ver reactivar) - nunca un
        // efecto lateral de un reseteo de rutina.
        if (usap.getEstado() != null && usap.getEstado().intValue() == EstadoUsuarioApp.ELIMINADO) {
            throw new IncomeException("La cuenta fue eliminada por el participe; use reactivar");
        }

        usap.setClaveHash(generarHash(solicitud.getClaveTemporal()));
        usap.setDebeCambiarClave(1L);
        usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
        usap.setIntentosFallidos(0L);
        usap.setBloqueadoHasta(null);
        // SIEMPRE sobreescribe: el rastro que importa es el del ultimo cambio de
        // credencial (este reseteo), no el del enrolamiento original.
        usap.setUsuarioRegistro(solicitud.getUsuario().trim());
        return usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UsuarioApp reactivar(SolicitudReactivarUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.reactivar - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));
        if (solicitud == null || esVacio(solicitud.getIdentificacion()) || esVacio(solicitud.getClaveTemporal())
                || esVacio(solicitud.getUsuario())) {
            throw new IncomeException("identificacion, claveTemporal y usuario son obligatorios");
        }

        UsuarioApp usap = usuarioAppDaoService.selectByIdentificacion(solicitud.getIdentificacion().trim());
        if (usap == null) {
            throw new IncomeException("No existe credencial de app para la identificacion "
                    + solicitud.getIdentificacion());
        }
        if (usap.getEstado() == null || usap.getEstado().intValue() != EstadoUsuarioApp.ELIMINADO) {
            throw new IncomeException("La cuenta " + solicitud.getIdentificacion()
                    + " no esta eliminada (estado actual: " + textoEstado(usap.getEstado())
                    + "); use resetearClave");
        }

        usap.setClaveHash(generarHash(solicitud.getClaveTemporal()));
        usap.setDebeCambiarClave(1L);
        usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
        usap.setIntentosFallidos(0L);
        usap.setBloqueadoHasta(null);
        usap.setUsuarioRegistro(solicitud.getUsuario().trim());
        return usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    private String textoEstado(Long estado) {
        if (estado == null) {
            return "desconocido";
        }
        switch (estado.intValue()) {
            case EstadoUsuarioApp.ACTIVO: return "ACTIVO";
            case EstadoUsuarioApp.BLOQUEADO: return "BLOQUEADO";
            case EstadoUsuarioApp.ELIMINADO: return "ELIMINADO";
            default: return String.valueOf(estado);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void desactivar(SolicitudDesactivarUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.desactivar - identificacion: "
                + (solicitud != null ? solicitud.getIdentificacion() : null));

        UsuarioApp usap = autenticar(
                solicitud != null ? solicitud.getIdentificacion() : null,
                solicitud != null ? solicitud.getClave() : null);

        usap.setEstado(Long.valueOf(EstadoUsuarioApp.ELIMINADO));
        usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    // =====================================================================
    // Recuperación de clave con código al correo ("olvidé mi clave")
    // Diseño: DISENO-RECUPERACION-CLAVE-CORREO-APP-MOVIL.md
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void solicitarRecuperacion(SolicitudSolicitarRecuperacionUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.solicitarRecuperacion");
        // NUNCA lanza IncomeException: el endpoint responde siempre el mismo 200, haya o no
        // cuenta, correo, freno o fallo del SMTP. Por eso aquí el save es normal (no hay rollback).
        String identificacion = solicitud != null ? solicitud.getIdentificacion() : null;
        if (esVacio(identificacion)) {
            return;
        }
        UsuarioApp usap = usuarioAppDaoService.selectByIdentificacion(identificacion.trim());
        if (usap == null) {
            return;
        }
        Long estado = usap.getEstado();
        if (estado != null && estado.intValue() == EstadoUsuarioApp.ELIMINADO) {
            return;
        }

        // Una cuenta BLOQUEADA sí puede pedir código: demostrar acceso al correo es la salida.
        Entidad entidad = usap.getEntidad();
        String correo = entidad != null && entidad.getCorreoPersonal() != null
                ? entidad.getCorreoPersonal().trim() : null;
        if (esVacio(correo) || !PATRON_CORREO.matcher(correo).matches()) {
            return;
        }
        // Un correo compartido le mandaría el código a otra persona.
        if (usuarioAppDaoService.contarEntidadesPorCorreoPersonal(correo.toLowerCase(Locale.ROOT)) != 1) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (usap.getRecuperacionUltimoEnvio() != null
                && ahora.isBefore(usap.getRecuperacionUltimoEnvio().plusSeconds(SEGUNDOS_ENTRE_ENVIOS))) {
            return;
        }
        LocalDateTime ventana = usap.getRecuperacionVentanaInicio();
        long envios = nvl(usap.getRecuperacionEnvios());
        if (ventana == null || !ahora.isBefore(ventana.plusHours(HORAS_VENTANA_ENVIOS))) {
            ventana = ahora;
            envios = 0;
        }
        if (envios >= MAX_ENVIOS_VENTANA) {
            return;
        }

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usap.setCodigoRecuperacionHash(generarHash(codigo));
        usap.setCodigoRecuperacionVence(ahora.plusMinutes(MINUTOS_VIGENCIA_CODIGO));
        usap.setCodigoRecuperacionIntentos(0L);
        usap.setRecuperacionUltimoEnvio(ahora);
        usap.setRecuperacionVentanaInicio(ventana);
        usap.setRecuperacionEnvios(envios + 1);
        usuarioAppDaoService.save(usap, usap.getCodigo());

        enviarCodigoPorCorreo(correo, entidad.getRazonSocial(), codigo);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void confirmarRecuperacion(SolicitudConfirmarRecuperacionUsuarioApp solicitud) throws Throwable {
        System.out.println("UsuarioAppService.confirmarRecuperacion");
        if (solicitud == null || esVacio(solicitud.getIdentificacion())
                || solicitud.getCodigo() == null || !PATRON_CODIGO.matcher(solicitud.getCodigo()).matches()) {
            // Lo que no son 6 dígitos no cuenta como intento.
            throw new ValidacionException(MSG_CODIGO_INVALIDO);
        }

        UsuarioApp usap = usuarioAppDaoService.selectByIdentificacion(solicitud.getIdentificacion().trim());
        if (usap == null) {
            throw new ValidacionException(MSG_CODIGO_INVALIDO);
        }
        Long estado = usap.getEstado();
        if (estado != null && estado.intValue() == EstadoUsuarioApp.ELIMINADO) {
            throw new ValidacionException(MSG_CODIGO_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (usap.getCodigoRecuperacionHash() == null || usap.getCodigoRecuperacionVence() == null
                || ahora.isAfter(usap.getCodigoRecuperacionVence())) {
            throw new ValidacionException(MSG_CODIGO_INVALIDO);
        }

        // Los intentos fallidos se graban en una transacción PROPIA (REQUIRES_NEW en el DAO,
        // llamado por la referencia @EJB): la ValidacionException de abajo es rollback=true y
        // desharía un save hecho aquí, igual que pasaba con el contador del login. En este camino
        // NO se modifica la entidad ni se hace save (ver registrarIntentoFallido).
        if (nvl(usap.getCodigoRecuperacionIntentos()) >= MAX_INTENTOS_CODIGO
                || !verificarClave(solicitud.getCodigo(), usap.getCodigoRecuperacionHash())) {
            usuarioAppDaoService.registrarIntentoCodigoRecuperacion(usap.getCodigo(), MAX_INTENTOS_CODIGO);
            throw new ValidacionException(MSG_CODIGO_INVALIDO);
        }

        // Código correcto: una clave nueva floja no consume el código ni suma intento.
        validarPoliticaClave(solicitud.getClaveNueva());

        usap.setClaveHash(generarHash(solicitud.getClaveNueva()));
        usap.setDebeCambiarClave(0L); // la eligió él mismo
        usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
        usap.setIntentosFallidos(0L);
        usap.setBloqueadoHasta(null);
        usap.setCodigoRecuperacionHash(null); // de un solo uso
        usap.setCodigoRecuperacionVence(null);
        usap.setCodigoRecuperacionIntentos(null);
        // USAPUSAR no se toca: es el rastro de la oficina y esto lo hizo el partícipe.
        usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    /**
     * Manda el código por la sesión JNDI {@code java:jboss/mail/AppMovil}, buscada AQUÍ y no con
     * {@code @Resource}: con {@code @Resource}, si la sesión no existe en el servidor falla el
     * despliegue del WAR entero y se cae el SAA. Cualquier falla (sesión ausente, SMTP caído) va
     * al log SIN el código y no se propaga: el partícipe ve siempre el mismo mensaje. Sin enlaces
     * y sin la cédula en el texto.
     */
    private void enviarCodigoPorCorreo(String destino, String nombre, String codigo) {
        try {
            Session sesion = (Session) new InitialContext().lookup(JNDI_SESION_CORREO);
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom();
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destino));
            mensaje.setSubject("Tu codigo para recuperar la clave", "UTF-8");
            String saludo = esVacio(nombre) ? "Hola," : "Hola " + nombre.trim() + ",";
            mensaje.setText(saludo + "\n\n"
                    + "Tu codigo para recuperar la clave de la app ASOPREP es: " + codigo + "\n\n"
                    + "Vale " + MINUTOS_VIGENCIA_CODIGO + " minutos.\n\n"
                    + "Si no lo pediste, ignora este correo: tu clave sigue igual.\n", "UTF-8");
            Transport.send(mensaje);
        } catch (Throwable e) {
            System.err.println("UsuarioAppService.enviarCodigoPorCorreo - no se pudo enviar: "
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    // =====================================================================
    // Helpers de negocio
    // =====================================================================

    /**
     * Autenticación compartida por {@link #validarCredencial} y {@link #desactivar}:
     * identificación inexistente, clave incorrecta, y usuario BLOQUEADO/ELIMINADO
     * devuelven exactamente el mismo error genérico — nunca se revela cuál de las tres
     * causas fue. Maneja el contador de intentos fallidos y el bloqueo de 15 minutos.
     *
     * @return La credencial ya autenticada (con intentosFallidos reseteado a 0 y
     *         persistido) — el llamador decide qué hacer a continuación (leer datos para
     *         el login, o marcar ELIMINADO para la desactivación).
     */
    private UsuarioApp autenticar(String identificacion, String clave) throws Throwable {
        if (esVacio(identificacion) || esVacio(clave)) {
            throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
        }

        UsuarioApp usap = usuarioAppDaoService.selectByIdentificacion(identificacion.trim());
        if (usap == null) {
            throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
        }

        Long estado = usap.getEstado();
        if (estado != null && estado.intValue() == EstadoUsuarioApp.ELIMINADO) {
            throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
        }

        LocalDateTime ahora = LocalDateTime.now();
        boolean bloqueoVencido = false;
        if (estado != null && estado.intValue() == EstadoUsuarioApp.BLOQUEADO) {
            if (usap.getBloqueadoHasta() != null && ahora.isBefore(usap.getBloqueadoHasta())) {
                throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
            }
            bloqueoVencido = true;
        }

        if (!verificarClave(clave, usap.getClaveHash())) {
            // El intento fallido se graba en una transacción PROPIA (REQUIRES_NEW en el DAO):
            // la IncomeException de abajo es rollback=true y desharía un save hecho acá. Por eso
            // NO se toca la entidad ni se hace save en este camino, y la llamada pasa por la
            // referencia @EJB (proxy del contenedor), nunca por this. Si el bloqueo ya venció, el
            // DAO reinicia el contador antes de sumar.
            usuarioAppDaoService.registrarIntentoFallido(usap.getCodigo(), MAX_INTENTOS_FALLIDOS, ahora,
                    MINUTOS_BLOQUEO);
            throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
        }

        if (bloqueoVencido) {
            // El bloqueo temporal ya expiró y la clave es buena: se reactiva la cuenta.
            usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
            usap.setBloqueadoHasta(null);
        }
        usap.setIntentosFallidos(0L);
        return usuarioAppDaoService.save(usap, usap.getCodigo());
    }

    private long nvl(Long valor) {
        return valor != null ? valor : 0L;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Mínimo 8 caracteres, con al menos una letra y al menos un número. Lanza
     * {@link ValidacionException} (400), NUNCA {@link IncomeException} genérica (401 en
     * este endpoint) — ver el JavaDoc de {@link ValidacionException} para el porqué.
     */
    private void validarPoliticaClave(String claveNueva) {
        if (esVacio(claveNueva) || claveNueva.length() < 8
                || !claveNueva.matches(".*[A-Za-z].*") || !claveNueva.matches(".*[0-9].*")) {
            throw new ValidacionException("La clave debe tener al menos 8 caracteres e incluir letras y numeros");
        }
    }

    /**
     * PBKDF2WithHmacSHA256, JDK puro. Formato de almacenamiento:
     * {@code iteraciones$saltBase64$hashBase64}.
     */
    private String generarHash(String claveTexto) {
        try {
            byte[] salt = new byte[PBKDF2_LARGO_SALT];
            new SecureRandom().nextBytes(salt);
            byte[] hash = derivar(claveTexto.toCharArray(), salt, PBKDF2_ITERACIONES);
            return PBKDF2_ITERACIONES + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IncomeException("Error al generar el hash de la clave: " + e.getMessage());
        }
    }

    private boolean verificarClave(String claveTexto, String hashAlmacenado) {
        if (esVacio(hashAlmacenado)) {
            return false;
        }
        String[] partes = hashAlmacenado.split("\\$");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);
            byte[] hashCalculado = derivar(claveTexto.toCharArray(), salt, iteraciones);
            return java.security.MessageDigest.isEqual(hashEsperado, hashCalculado);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            System.out.println("UsuarioAppService.verificarClave - hash con formato invalido: " + e.getMessage());
            return false;
        }
    }

    private byte[] derivar(char[] claveTexto, byte[] salt, int iteraciones)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(claveTexto, salt, iteraciones, PBKDF2_LARGO_CLAVE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITMO);
        return factory.generateSecret(spec).getEncoded();
    }
}
