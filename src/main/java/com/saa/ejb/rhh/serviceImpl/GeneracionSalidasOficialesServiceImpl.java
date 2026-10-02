package com.saa.ejb.rhh.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService;
import com.saa.ejb.rhh.dao.LiquidacionExternaDaoService;
import com.saa.ejb.rhh.dao.SalidaOficialDaoService;
import com.saa.ejb.rhh.service.GeneracionSalidasOficialesService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.LiquidacionExterna;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SalidaOficial;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.RhhConceptoLiquidacionExterna;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoSalidaOficial;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de GeneracionSalidasOficialesService.</p>
 *
 * <h3>El RDEP sale de los acumulados, no de una tabla propia</h3>
 *
 * <p>Cada empleado aporta un registro con lo que ya esta en <code>RHH.ACMN</code>: el gravado de
 * impuesto a la renta, el aporte personal y la retencion del ejercicio. Es la misma fuente con
 * la que se calculo la nomina, asi que el XML no puede divergir del rol.</p>
 *
 * <h3>El hash sirve para saber si el archivo presentado sigue siendo el generado</h3>
 *
 * <p>Se calcula sobre el contenido entero. Si alguien regenera el RDEP tras corregir un
 * acumulado, el hash cambia y la salida ya presentada queda delatada.</p>
 */
@Stateless
public class GeneracionSalidasOficialesServiceImpl implements GeneracionSalidasOficialesService {

    /** Algoritmo del hash del contenido generado. */
    private static final String ALGORITMO_HASH = "SHA-256";

    /** Codificacion del XML del RDEP. */
    private static final String CODIFICACION = "UTF-8";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private SalidaOficialDaoService salidaOficialDaoService;

    @EJB
    private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

    @EJB
    private AcumuladoNominaDaoService acumuladoNominaDaoService;

    // ===== INICIO liquidaciones de ex-colaboradores (e3-05, equipo omen-saa-3, 2026-09-30) =====
    @EJB
    private LiquidacionExternaDaoService liquidacionExternaDaoService;

    @EJB
    private DetalleLiquidacionExternaDaoService detalleLiquidacionExternaDaoService;
    // ===== FIN liquidaciones de ex-colaboradores =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionSalidasOficialesService#generarRdep(java.lang.Long, java.lang.Integer, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public byte[] generarRdep(Long idEmpresa, Integer anio, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo generarRdep de generacionSalidasOficiales service,"
                + " empresa: " + idEmpresa + ", anio: " + anio);

        if (idEmpresa == null || anio == null) {
            throw new IncomeException("El RDEP exige la empresa y el ejercicio fiscal.");
        }
        // EL RDEP DECLARA A QUIEN COBRO EN EL EJERCICIO, NO A QUIEN SIGUE EN LA EMPRESA.
        //
        // Antes se partia de selectActivosEnPeriodo, que filtra por empleado.estado <>
        // CESANTE: quien entro en enero y se fue en marzo quedaba fuera del declarativo
        // aunque hubiera cobrado tres meses y se le hubiera retenido. En 2026 eso dejaba
        // fuera a dos personas de veintidos.
        //
        // La fuente correcta es el propio acumulado: si alguien tiene GRAVADO_IR o
        // RETENCION_IR en el ejercicio, cobro, y hay que declararlo. Cesante o no.
        List<Empleado> declarables = acumuladoNominaDaoService.selectEmpleadosConAcumuladoEnAnio(
                idEmpresa, anio);
        // e3-05 (2026-09-30): las liquidaciones de ex-colaboradores PAGADAS en el anio tambien
        // declaran, aunque ACMN no tenga a nadie ese anio (empresa que solo pago rezagos de la
        // administracion anterior). Por eso el guard de "nada que declarar" de abajo ahora
        // mira las dos fuentes, no solo declarables.
        List<LiquidacionExterna> liquidacionesExternasPagadas = liquidacionExternaDaoService
                .selectPagadasByAnio(anio);
        if ((declarables == null || declarables.isEmpty())
                && (liquidacionesExternasPagadas == null || liquidacionesExternasPagadas.isEmpty())) {
            throw new IncomeException("Nadie tiene acumulados de " + anio + ": no hay nada que"
                    + " declarar en el RDEP. Compruebe que los periodos del ejercicio se cerraron"
                    + " --los acumulados solo se escriben al cerrar-- y que las liquidaciones del"
                    + " anio tienen la salida ejecutada.");
        }

        // Acumulador por identificacion, para que una liquidacion externa de alguien que
        // tambien tiene ACMN (recontratado) SUME a la misma persona en vez de duplicarla
        // (§8 del contrato). LinkedHashMap para mantener el orden de ACMN primero y los
        // extras de LQEX despues, aunque el orden no lo exige el contrato.
        Map<String, EmpleadoDeclarable> porIdentificacion = new LinkedHashMap<String, EmpleadoDeclarable>();

        if (declarables != null) {
            for (Empleado empleado : declarables) {
                if (empleado == null) {
                    continue;
                }
                Double gravado = acumulado(empleado.getCodigo(), anio, RhhTipoAcumulado.GRAVADO_IR);
                Double aporte = acumulado(empleado.getCodigo(), anio, RhhTipoAcumulado.APORTE_PERSONAL);
                Double retencion = acumulado(empleado.getCodigo(), anio, RhhTipoAcumulado.RETENCION_IR);

                if (gravado.doubleValue() == 0D && retencion.doubleValue() == 0D) {
                    // Un empleado sin ingreso gravado ni retencion en el ejercicio no se declara:
                    // incluirlo con ceros ensucia el archivo y el DIMM lo rechaza.
                    continue;
                }
                String identificacion = empleado.getIdentificacion();
                EmpleadoDeclarable declarable = new EmpleadoDeclarable(identificacion,
                        empleado.getApellidos(), empleado.getNombres());
                declarable.gravado = gravado;
                declarable.aporte = aporte;
                declarable.retencion = retencion;
                porIdentificacion.put(identificacion, declarable);
            }
        }

        // e3-05 (2026-09-30, §8) + REVISION R3 (2026-10-02): LQEX PAGADA cuya fechaPago cae en
        // el anio. ingresoGravado = tipos 1, 2, 3, 4 y 10 (RhhConceptoLiquidacionExterna.
        // esGravadoIrEnRdep, caso real verificado con el contador: remuneracion + vacaciones +
        // decimo tercero + decimo cuarto = 2.316,22); aportePersonal = tipo 20; retencion =
        // tipo 21. No se toca el calculo de ACMN de arriba.
        if (liquidacionesExternasPagadas != null) {
            for (LiquidacionExterna liquidacion : liquidacionesExternasPagadas) {
                List<DetalleLiquidacionExterna> detalles = detalleLiquidacionExternaDaoService
                        .selectByLiquidacion(liquidacion.getCodigo());
                Double gravadoLqex = Double.valueOf(0D);
                Double aporteLqex = Double.valueOf(0D);
                Double retencionLqex = Double.valueOf(0D);
                if (detalles != null) {
                    for (DetalleLiquidacionExterna detalle : detalles) {
                        if (detalle.getTipoConcepto() == null || detalle.getValor() == null) {
                            continue;
                        }
                        long tipo = detalle.getTipoConcepto().longValue();
                        if (RhhConceptoLiquidacionExterna.esGravadoIrEnRdep(tipo)) {
                            gravadoLqex = RedondeoNomina.suma(gravadoLqex, detalle.getValor());
                        } else if (tipo == RhhConceptoLiquidacionExterna.APORTE_PERSONAL_IESS) {
                            aporteLqex = RedondeoNomina.suma(aporteLqex, detalle.getValor());
                        } else if (tipo == RhhConceptoLiquidacionExterna.RETENCION_IMPUESTO_RENTA) {
                            retencionLqex = RedondeoNomina.suma(retencionLqex, detalle.getValor());
                        }
                    }
                }
                if (gravadoLqex.doubleValue() == 0D && retencionLqex.doubleValue() == 0D) {
                    // Mismo criterio que ACMN arriba: sin gravado ni retencion, no se declara.
                    continue;
                }
                String identificacion = liquidacion.getIdentificacion();
                EmpleadoDeclarable existente = porIdentificacion.get(identificacion);
                if (existente != null) {
                    // Misma identificacion ya declarada por ACMN (ex-colaborador recontratado):
                    // se suma, no se duplica.
                    existente.gravado = RedondeoNomina.suma(existente.gravado, gravadoLqex);
                    existente.aporte = RedondeoNomina.suma(existente.aporte, aporteLqex);
                    existente.retencion = RedondeoNomina.suma(existente.retencion, retencionLqex);
                } else {
                    EmpleadoDeclarable nuevo = new EmpleadoDeclarable(identificacion,
                            liquidacion.getApellidos(), liquidacion.getNombres());
                    nuevo.gravado = gravadoLqex;
                    nuevo.aporte = aporteLqex;
                    nuevo.retencion = retencionLqex;
                    porIdentificacion.put(identificacion, nuevo);
                }
            }
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"").append(CODIFICACION).append("\"?>\n");
        xml.append("<rdep anio=\"").append(anio).append("\">\n");

        int declarados = 0;
        for (EmpleadoDeclarable declarable : porIdentificacion.values()) {
            xml.append("  <empleado>\n");
            xml.append("    <identificacion>").append(texto(declarable.identificacion)).append("</identificacion>\n");
            xml.append("    <apellidos>").append(texto(declarable.apellidos)).append("</apellidos>\n");
            xml.append("    <nombres>").append(texto(declarable.nombres)).append("</nombres>\n");
            xml.append("    <ingresoGravado>").append(declarable.gravado).append("</ingresoGravado>\n");
            xml.append("    <aportePersonal>").append(declarable.aporte).append("</aportePersonal>\n");
            xml.append("    <retencion>").append(declarable.retencion).append("</retencion>\n");
            xml.append("  </empleado>\n");
            declarados++;
        }
        xml.append("</rdep>\n");

        if (declarados == 0) {
            throw new IncomeException("Ningun empleado tiene ingreso gravado ni retencion en " + anio
                    + ": no hay RDEP que presentar. Compruebe que los periodos del ejercicio se"
                    + " cerraron: los acumulados solo se escriben al cerrar.");
        }

        byte[] contenido = xml.toString().getBytes(StandardCharsets.UTF_8);
        registrarGeneracion(idEmpresa, RhhTipoSalidaOficial.RDEP, anio, null, null,
                "rdep_" + anio + ".xml", contenido, usuario);

        System.out.println("RDEP de " + anio + " generado con " + declarados + " empleado(s).");
        return contenido;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionSalidasOficialesService#registrarGeneracion(java.lang.Long, int, java.lang.Integer, java.lang.Integer, java.lang.Long, java.lang.String, byte[], java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SalidaOficial registrarGeneracion(Long idEmpresa, int tipoSalida, Integer anio,
            Integer mes, Long idEmpleado, String nombreArchivo, byte[] contenido, String usuario)
            throws Throwable {

        // Idempotencia por servicio: se busca la salida del periodo y se actualiza. El indice
        // IX_SLOF_BUSQ no es unico a proposito, porque con SLOFMESS o MPLDCDGO en nulo Oracle
        // no considera duplicadas dos filas y un UNIQUE no impediria nada.
        SalidaOficial salida = salidaOficialDaoService.selectSalida(idEmpresa,
                Long.valueOf(tipoSalida), anio, mes, idEmpleado);
        if (salida == null) {
            salida = new SalidaOficial();
            salida.setEmpresa(em.find(Empresa.class, idEmpresa));
            salida.setTipoSalida(Long.valueOf(tipoSalida));
            salida.setAnio(anio);
            salida.setMes(mes);
            if (idEmpleado != null) {
                salida.setEmpleado(em.find(Empleado.class, idEmpleado));
            }
            salida.setFechaRegistro(LocalDateTime.now());
            salida.setUsuarioRegistro(usuario);
        }
        salida.setNombreArchivo(nombreArchivo);
        salida.setHash(contenido != null ? calculaHash(contenido) : null);
        salida.setFechaGeneracion(LocalDate.now());
        salida.setEstado(Long.valueOf(Estado.ACTIVO));

        // La presentacion NO se toca al regenerar: si el archivo ya se presento, el hecho
        // ocurrio. Lo que delata que el contenido cambio despues es el hash, no borrar la fecha.

        salida = salidaOficialDaoService.save(salida, salida.getCodigo());
        System.out.println("Salida oficial tipo " + tipoSalida + " de " + anio
                + (mes != null ? "/" + mes : "") + " registrada con el codigo " + salida.getCodigo()
                + ".");
        return salida;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionSalidasOficialesService#registrarPresentacion(java.lang.Long, java.time.LocalDate, java.lang.String, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SalidaOficial registrarPresentacion(Long idSalida, LocalDate fechaPresentacion,
            String numeroComprobante, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo registrarPresentacion de generacionSalidasOficiales"
                + " service, salida: " + idSalida);

        SalidaOficial salida = salidaOficialDaoService.selectById(idSalida,
                NombreEntidadesRhh.SALIDA_OFICIAL);
        if (salida == null) {
            throw new IncomeException("No existe la salida oficial " + idSalida + ".");
        }
        if (salida.getFechaGeneracion() == null) {
            throw new IncomeException("La salida " + idSalida + " no se ha generado todavia: no se"
                    + " puede registrar su presentacion.");
        }
        salida.setFechaPresentacion(fechaPresentacion != null ? fechaPresentacion : LocalDate.now());
        salida.setNumeroComprobante(numeroComprobante);
        salida.setUsuarioRegistro(usuario);
        return salidaOficialDaoService.save(salida, salida.getCodigo());
    }

    // =====================================================================
    // Piezas
    // =====================================================================

    /**
     * Suma un acumulado del ejercicio completo.
     *
     * @param idEmpleado	: Id del empleado
     * @param anio			: Ejercicio
     * @param tipo			: Detalle del rubro RHH_TIPO_ACUMULADO
     * @return				: El acumulado, o cero
     * @throws Throwable	: Excepcion
     */
    private Double acumulado(Long idEmpleado, Integer anio, int tipo) throws Throwable {
        Double valor = acumuladoNominaDaoService.sumaValor(idEmpleado, anio, Long.valueOf(tipo),
                null, null);
        return valor != null ? RedondeoNomina.redondea(valor) : Double.valueOf(0D);
    }

    /**
     * SHA-256 del contenido generado.
     *
     * @param contenido		: Bytes del archivo
     * @return				: Hash en hexadecimal minusculo
     * @throws Throwable	: Excepcion
     */
    private String calculaHash(byte[] contenido) throws Throwable {
        MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
        byte[] resumen = digest.digest(contenido);
        StringBuilder hexadecimal = new StringBuilder(resumen.length * 2);
        for (byte b : resumen) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexadecimal.append('0');
            }
            hexadecimal.append(hex);
        }
        return hexadecimal.toString();
    }

    /**
     * Escapa el texto para el XML y devuelve cadena vacia si es nulo.
     *
     * @param valor	: Texto
     * @return		: Texto seguro para el XML
     */
    private String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Acumulador de un &lt;empleado&gt; del RDEP, con lo que ya trae ACMN y lo que se le
     * suma de LQEX (e3-05, §8): la misma identificacion nunca se declara dos veces.
     */
    private static final class EmpleadoDeclarable {
        private final String identificacion;
        private final String apellidos;
        private final String nombres;
        private Double gravado = Double.valueOf(0D);
        private Double aporte = Double.valueOf(0D);
        private Double retencion = Double.valueOf(0D);

        private EmpleadoDeclarable(String identificacion, String apellidos, String nombres) {
            this.identificacion = identificacion;
            this.apellidos = apellidos;
            this.nombres = nombres;
        }
    }
}
