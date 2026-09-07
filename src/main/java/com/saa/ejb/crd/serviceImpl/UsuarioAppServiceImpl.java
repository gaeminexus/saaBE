package com.saa.ejb.crd.serviceImpl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PersonaNaturalDaoService;
import com.saa.ejb.crd.dao.UsuarioAppDaoService;
import com.saa.ejb.crd.service.UsuarioAppService;
import com.saa.ejb.crd.service.ValidacionException;
import com.saa.ejb.crd.service.dto.SolicitudCambiarClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudCrearUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudDesactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudReactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudResetearClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudValidarCredencial;
import com.saa.ejb.crd.service.dto.ValidarCredencialResponse;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PersonaNatural;
import com.saa.model.crd.UsuarioApp;
import com.saa.rubros.EstadoUsuarioApp;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.NoResultException;

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

    private static final int PBKDF2_ITERACIONES = 100_000;
    private static final int PBKDF2_LARGO_SALT = 16;
    private static final int PBKDF2_LARGO_CLAVE_BITS = 256;
    private static final String PBKDF2_ALGORITMO = "PBKDF2WithHmacSHA256";

    @EJB
    private UsuarioAppDaoService usuarioAppDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private PersonaNaturalDaoService personaNaturalDaoService;

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

        // Nombres/apellidos viven en CRD.PRSN, que comparte PK con CRD.ENTD — no toda
        // Entidad tiene fila PersonaNatural (podría ser persona jurídica); ausencia no es
        // un error para este endpoint, solo viajan nombres/apellidos en null.
        try {
            PersonaNatural persona = personaNaturalDaoService.selectById(entidad.getCodigo(),
                    NombreEntidadesCredito.PERSONA_NATURAL);
            respuesta.setNombres(persona.getNombres());
            respuesta.setApellidos(persona.getApellidos());
        } catch (NoResultException e) {
            System.out.println("UsuarioAppService.validarCredencial - entidad " + entidad.getCodigo()
                    + " sin PersonaNatural asociada");
        }

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
        if (estado != null && estado.intValue() == EstadoUsuarioApp.BLOQUEADO) {
            if (usap.getBloqueadoHasta() != null && ahora.isBefore(usap.getBloqueadoHasta())) {
                throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
            }
            // El bloqueo temporal ya expiró: se reactiva para permitir un nuevo intento.
            usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
            usap.setIntentosFallidos(0L);
            usap.setBloqueadoHasta(null);
        }

        if (!verificarClave(clave, usap.getClaveHash())) {
            long intentos = nvl(usap.getIntentosFallidos()) + 1;
            usap.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS_FALLIDOS) {
                usap.setEstado(Long.valueOf(EstadoUsuarioApp.BLOQUEADO));
                usap.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
            }
            usuarioAppDaoService.save(usap, usap.getCodigo());
            throw new IncomeException(MSG_CREDENCIALES_INVALIDAS);
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
