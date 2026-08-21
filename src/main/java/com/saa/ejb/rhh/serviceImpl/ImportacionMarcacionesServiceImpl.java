package com.saa.ejb.rhh.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CargaMarcacionesDaoService;
import com.saa.ejb.rhh.dao.DetalleFormatoMarcacionDaoService;
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.ejb.rhh.dao.FormatoArchivoMarcacionDaoService;
import com.saa.ejb.rhh.dao.MarcacionesDaoService;
import com.saa.ejb.rhh.service.ImportacionMarcacionesService;
import com.saa.model.rhh.CargaMarcaciones;
import com.saa.model.rhh.DetalleFormatoMarcacion;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.FormatoArchivoMarcacion;
import com.saa.model.rhh.Marcaciones;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ResultadoImportacionMarcaciones;
import com.saa.model.scp.Empresa;
import com.saa.rubros.RhhCampoArchivoMarcacion;
import com.saa.rubros.RhhEstadoCargaMarcaciones;
import com.saa.rubros.RhhFormatoArchivoMarcacion;
import com.saa.rubros.RhhOrigenMarcacion;
import com.saa.rubros.RhhTipoMarcacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de ImportacionMarcacionesService.</p>
 *
 * <p>Las siete reglas del §7 del plan de backend, en el orden en que se aplican:</p>
 *
 * <ol>
 *   <li>Leer el <code>FMRC</code> y sus <code>DFMR</code> ordenados; calcular el SHA-256 del
 *       archivo y rechazarlo si ya existe una carga no anulada con ese hash.</li>
 *   <li>Saltar <code>FMRCLNCB</code> lineas de cabecera y <code>FMRCLNPI</code> de pie.</li>
 *   <li>Extraer cada campo por posicion (delimitado) o por inicio y longitud (ancho fijo), y
 *       parsear la fecha y la hora con los patrones del formato.</li>
 *   <li>Traducir el tipo de marcacion con <code>DFMRMPEO</code>.</li>
 *   <li>Emparejar el empleado por <code>MPLDCDBM</code>, con respaldo en
 *       <code>MPLDIDNT</code>.</li>
 *   <li>Deduplicar por <code>(empleado, fecha-hora)</code>.</li>
 *   <li>Una linea mala <b>no aborta el archivo</b>: va al log.</li>
 * </ol>
 *
 * <h3>Por que el archivo se lee entero en memoria</h3>
 *
 * <p>El hash se calcula sobre el contenido completo y el parseo lo recorre otra vez, asi que
 * el <code>InputStream</code> haria falta dos veces. Un archivo mensual de veinticinco
 * empleados son unas mil quinientas lineas: cabe de sobra, y evita depender de que el stream
 * admita <code>reset</code>, que con un multipart no esta garantizado.</p>
 *
 * <h3>Previsualizar y confirmar leen lo mismo</h3>
 *
 * <p>Comparten todo el recorrido; lo unico que cambia es si al final se persiste. Es el patron
 * de <code>ImportacionExtractoBancarioServiceImpl</code>, y garantiza que lo que el usuario ve
 * en la previsualizacion es exactamente lo que va a entrar.</p>
 */
@Stateless
public class ImportacionMarcacionesServiceImpl implements ImportacionMarcacionesService {

    /** Bandera afirmativa. */
    private static final String SI = "S";

    /** Bandera negativa. */
    private static final String NO = "N";

    /** Algoritmo del hash antiduplicado. */
    private static final String ALGORITMO_HASH = "SHA-256";

    /** Separador de las parejas del mapeo DFMRMPEO. */
    private static final String SEPARADOR_PAREJAS = ";";

    /** Separador de origen y destino dentro de una pareja del mapeo. */
    private static final String SEPARADOR_MAPEO = "=";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private FormatoArchivoMarcacionDaoService formatoArchivoMarcacionDaoService;

    @EJB
    private DetalleFormatoMarcacionDaoService detalleFormatoMarcacionDaoService;

    @EJB
    private CargaMarcacionesDaoService cargaMarcacionesDaoService;

    @EJB
    private MarcacionesDaoService marcacionesDaoService;

    @EJB
    private EmpleadoDaoService empleadoDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ImportacionMarcacionesService#previsualizar(java.io.InputStream, java.lang.String, java.lang.Long, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ResultadoImportacionMarcaciones previsualizar(InputStream archivo, String nombreArchivo,
            Long idFormato, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo previsualizar de importacionMarcaciones service, archivo: "
                + nombreArchivo);
        return procesa(archivo, nombreArchivo, idFormato, idEmpresa, null, false);
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ImportacionMarcacionesService#confirmar(java.io.InputStream, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoImportacionMarcaciones confirmar(InputStream archivo, String nombreArchivo,
            Long idFormato, Long idEmpresa, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo confirmar de importacionMarcaciones service, archivo: "
                + nombreArchivo);
        return procesa(archivo, nombreArchivo, idFormato, idEmpresa, usuario, true);
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ImportacionMarcacionesService#anular(java.lang.Long, java.lang.String, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void anular(Long idCarga, String motivo, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo anular de importacionMarcaciones service, carga: " + idCarga);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("La anulacion de una carga de marcaciones exige un motivo.");
        }
        CargaMarcaciones carga = cargaMarcacionesDaoService.selectById(idCarga,
                NombreEntidadesRhh.CARGA_MARCACIONES);
        if (carga == null) {
            throw new IncomeException("No existe la carga de marcaciones " + idCarga + ".");
        }
        if (Long.valueOf(RhhEstadoCargaMarcaciones.ANULADO).equals(carga.getEstado())) {
            throw new IncomeException("La carga " + idCarga + " ya estaba anulada.");
        }

        // Si alguna marcacion del lote ya se consolido, retirarla dejaria el resumen diario
        // apoyado en datos que ya no existen. Primero hay que rehacer la consolidacion.
        long consolidadas = cuentaConsolidadas(idCarga);
        if (consolidadas > 0L) {
            throw new IncomeException("La carga " + idCarga + " no se puede anular: " + consolidadas
                    + " de sus marcaciones ya se consolidaron en un resumen diario. Rehaga la"
                    + " consolidacion de esos dias antes de anular el lote.");
        }

        int borradas = marcacionesDaoService.eliminaByCarga(idCarga);
        carga.setEstado(Long.valueOf(RhhEstadoCargaMarcaciones.ANULADO));
        carga.setLog(concatena(carga.getLog(), "ANULADA por " + usuario + ": " + motivo.trim()
                + " (" + borradas + " marcacion(es) retiradas)"));
        cargaMarcacionesDaoService.save(carga, carga.getCodigo());

        System.out.println("Carga " + idCarga + " anulada, " + borradas + " marcacion(es) retiradas.");
    }

    // =====================================================================
    // El recorrido, comun a previsualizar y confirmar
    // =====================================================================

    /**
     * Lee el archivo completo y, si se pide, persiste la carga con sus marcaciones.
     *
     * @param archivo		: Contenido del archivo
     * @param nombreArchivo	: Nombre del archivo
     * @param idFormato		: Id del formato
     * @param idEmpresa		: Id de la empresa
     * @param usuario		: Usuario que ejecuta
     * @param persistir		: true para grabar
     * @return				: Resumen de la importacion
     * @throws Throwable	: Excepcion
     */
    private ResultadoImportacionMarcaciones procesa(InputStream archivo, String nombreArchivo,
            Long idFormato, Long idEmpresa, String usuario, boolean persistir) throws Throwable {

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa de la carga de marcaciones.");
        }

        // --- Regla 1: formato, campos y hash -----------------------------------------
        FormatoArchivoMarcacion formato = formatoArchivoMarcacionDaoService.selectById(idFormato,
                NombreEntidadesRhh.FORMATO_ARCHIVO_MARCACION);
        if (formato == null) {
            throw new IncomeException("No existe el formato de archivo de marcaciones " + idFormato
                    + ". El formato del reloj es dato: crearlo es parte de la puesta en marcha.");
        }
        List<DetalleFormatoMarcacion> campos = detalleFormatoMarcacionDaoService
                .selectByFormato(idFormato);
        if (campos == null || campos.isEmpty()) {
            throw new IncomeException("El formato '" + formato.getNombre() + "' no tiene ningun campo"
                    + " definido (RHH.DFMR): no se sabe que leer de cada linea.");
        }

        byte[] contenido = leeTodo(archivo);
        if (contenido.length == 0) {
            throw new IncomeException("El archivo " + nombreArchivo + " esta vacio.");
        }
        String hash = calculaHash(contenido);

        CargaMarcaciones repetida = cargaMarcacionesDaoService.selectVigenteByHash(hash, idEmpresa);
        if (repetida != null) {
            throw new IncomeException("Este archivo ya se cargo: es la carga " + repetida.getCodigo()
                    + " del " + repetida.getFechaCarga() + " ('" + repetida.getNombreArchivo()
                    + "'). Si aquella carga era incorrecta, anulela primero.");
        }

        // --- Regla 2: cabecera y pie ---------------------------------------------------
        List<String> lineas = separaLineas(contenido, formato);
        int cabecera = formato.getLineasCabecera() != null ? formato.getLineasCabecera().intValue() : 0;
        int pie = formato.getLineasPie() != null ? formato.getLineasPie().intValue() : 0;
        int desde = Math.min(cabecera, lineas.size());
        int hasta = Math.max(desde, lineas.size() - pie);

        boolean anchoFijo = Long.valueOf(RhhFormatoArchivoMarcacion.ANCHO_FIJO)
                .equals(formato.getTipoFormato());
        String delimitador = formato.getDelimitador();
        if (!anchoFijo && (delimitador == null || delimitador.isEmpty())) {
            throw new IncomeException("El formato '" + formato.getNombre() + "' es delimitado pero no"
                    + " declara el delimitador (FMRCDLMT).");
        }

        ResultadoImportacionMarcaciones resultado = new ResultadoImportacionMarcaciones();
        resultado.setNombreArchivo(nombreArchivo);

        CargaMarcaciones carga = null;
        if (persistir) {
            carga = new CargaMarcaciones();
            carga.setEmpresa(em.find(Empresa.class, idEmpresa));
            carga.setFormato(formato);
            carga.setNombreArchivo(nombreArchivo);
            carga.setHash(hash);
            carga.setFechaCarga(LocalDate.now());
            carga.setEstado(Long.valueOf(RhhEstadoCargaMarcaciones.CARGADO));
            // Se siembran para grabar la cabecera antes del detalle; al final se sobrescriben.
            carga.setLineasTotales(Integer.valueOf(0));
            carga.setLineasOk(Integer.valueOf(0));
            carga.setLineasError(Integer.valueOf(0));
            carga.setLineasDuplicadas(Integer.valueOf(0));
            carga.setFechaRegistro(LocalDateTime.now());
            carga.setUsuarioRegistro(usuario);
            carga = cargaMarcacionesDaoService.save(carga, carga.getCodigo());
            em.flush();
        }

        int totales = 0;
        int ok = 0;
        int error = 0;
        int duplicadas = 0;
        LocalDate menor = null;
        LocalDate mayor = null;
        // Deduplicacion dentro del propio archivo, ademas de contra la base: un reloj puede
        // repetir la misma marcacion en dos lineas del mismo fichero.
        List<String> vistas = new ArrayList<String>();

        for (int i = desde; i < hasta; i++) {
            String linea = lineas.get(i);
            int numeroLinea = i + 1;
            if (linea == null || linea.trim().isEmpty()) {
                continue;
            }
            totales++;
            try {
                // --- Regla 3: extraer los campos ---------------------------------------
                Map<Integer, String> valores = extraeCampos(linea, campos, anchoFijo, delimitador);
                LocalDateTime fechaHora = armaFechaHora(valores, formato, numeroLinea);

                // --- Regla 5: emparejar el empleado ------------------------------------
                Empleado empleado = localizaEmpleado(valores, idEmpresa);
                if (empleado == null) {
                    error++;
                    resultado.getErrores().add("Linea " + numeroLinea + ": no se encontro ningun"
                            + " empleado con codigo biometrico '"
                            + texto(valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.ID_EMPLEADO)))
                            + "' ni identificacion '"
                            + texto(valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.IDENTIFICACION)))
                            + "'.");
                    continue;
                }

                // --- Regla 6: deduplicar -----------------------------------------------
                String clave = empleado.getCodigo() + "|" + fechaHora;
                if (vistas.contains(clave)
                        || marcacionesDaoService.existeMarcacion(empleado.getCodigo(), fechaHora)) {
                    duplicadas++;
                    continue;
                }
                vistas.add(clave);

                // --- Regla 4: traducir el tipo -----------------------------------------
                Long tipo = tipoMarcacion(valores, campos);

                if (persistir) {
                    Marcaciones marcacion = new Marcaciones();
                    marcacion.setEmpleado(empleado);
                    marcacion.setFechaHora(fechaHora);
                    marcacion.setTipo(tipo);
                    marcacion.setOrigen(Long.valueOf(RhhOrigenMarcacion.IMPORTACION_ARCHIVO));
                    marcacion.setCargaMarcaciones(carga);
                    marcacion.setDispositivo(valores.get(
                            Integer.valueOf(RhhCampoArchivoMarcacion.ID_DISPOSITIVO)));
                    marcacion.setLineaArchivo(Integer.valueOf(numeroLinea));
                    marcacion.setProcesado(NO);
                    marcacion.setFechaRegistro(LocalDate.now());
                    marcacion.setUsuarioRegistro(usuario);
                    marcacionesDaoService.save(marcacion, marcacion.getCodigo());
                }

                LocalDate dia = fechaHora.toLocalDate();
                if (menor == null || dia.isBefore(menor)) {
                    menor = dia;
                }
                if (mayor == null || dia.isAfter(mayor)) {
                    mayor = dia;
                }
                ok++;

            } catch (Throwable e) {
                // Regla 7: una linea mala no aborta el archivo. Un archivo de reloj trae casi
                // siempre alguna linea con el dedo de un visitante o de alguien dado de baja,
                // y rechazar el mes entero por eso dejaria la nomina sin asistencia.
                error++;
                resultado.getErrores().add("Linea " + numeroLinea + ": " + e.getMessage());
            }
        }

        resultado.setLineasTotales(Integer.valueOf(totales));
        resultado.setLineasOk(Integer.valueOf(ok));
        resultado.setLineasError(Integer.valueOf(error));
        resultado.setLineasDuplicadas(Integer.valueOf(duplicadas));
        resultado.setFechaDesde(menor);
        resultado.setFechaHasta(mayor);

        if (persistir) {
            carga.setLineasTotales(Integer.valueOf(totales));
            carga.setLineasOk(Integer.valueOf(ok));
            carga.setLineasError(Integer.valueOf(error));
            carga.setLineasDuplicadas(Integer.valueOf(duplicadas));
            carga.setFechaDesde(menor);
            carga.setFechaHasta(mayor);
            carga.setLog(armaLog(resultado.getErrores()));
            carga.setEstado(Long.valueOf(error > 0
                    ? RhhEstadoCargaMarcaciones.CON_ERRORES : RhhEstadoCargaMarcaciones.VALIDADO));
            cargaMarcacionesDaoService.save(carga, carga.getCodigo());
            resultado.setIdCarga(carga.getCodigo());
        }

        System.out.println("Importacion de '" + nombreArchivo + "': " + totales + " linea(s), "
                + ok + " ok, " + error + " con error, " + duplicadas + " duplicada(s)."
                + (persistir ? " Carga " + carga.getCodigo() + "." : " (previsualizacion)"));
        return resultado;
    }

    // =====================================================================
    // Piezas del parser
    // =====================================================================

    /**
     * Extrae los campos de una linea segun la definicion del formato.
     *
     * @param linea			: Linea del archivo
     * @param campos		: Campos del formato
     * @param anchoFijo		: true si el formato es de ancho fijo
     * @param delimitador	: Delimitador, cuando no es de ancho fijo
     * @return				: Mapa codigo de campo del rubro 215 → valor
     * @throws Throwable	: IncomeException si falta un campo obligatorio
     */
    private Map<Integer, String> extraeCampos(String linea, List<DetalleFormatoMarcacion> campos,
            boolean anchoFijo, String delimitador) throws Throwable {

        Map<Integer, String> valores = new LinkedHashMap<Integer, String>();
        String[] partes = anchoFijo ? null : linea.split(java.util.regex.Pattern.quote(delimitador), -1);

        for (DetalleFormatoMarcacion campo : campos) {
            String valor = null;
            if (anchoFijo) {
                Integer inicio = campo.getIndiceInicio();
                Integer longitud = campo.getLongitud();
                if (inicio != null && longitud != null) {
                    int desde = inicio.intValue() - 1;
                    int hasta = desde + longitud.intValue();
                    if (desde >= 0 && desde < linea.length()) {
                        valor = linea.substring(desde, Math.min(hasta, linea.length()));
                    }
                }
            } else {
                Integer posicion = campo.getPosicion();
                if (posicion != null && posicion.intValue() >= 1
                        && posicion.intValue() <= partes.length) {
                    valor = partes[posicion.intValue() - 1];
                }
            }
            valor = valor != null ? valor.trim() : null;

            if (SI.equals(campo.getObligatorio()) && (valor == null || valor.isEmpty())) {
                throw new IncomeException("falta el campo obligatorio " + campo.getCampo()
                        + " del formato");
            }
            if (campo.getCampo() != null) {
                valores.put(Integer.valueOf(campo.getCampo().intValue()), valor);
            }
        }
        return valores;
    }

    /**
     * Arma el instante de la marcacion con los patrones del formato.
     *
     * <p>Admite las dos formas que usan los relojes: una columna con fecha y hora juntas, o dos
     * columnas separadas.</p>
     *
     * @param valores		: Campos ya extraidos
     * @param formato		: Formato del archivo
     * @param numeroLinea	: Numero de linea, para el mensaje
     * @return				: El instante de la marcacion
     * @throws Throwable	: IncomeException si no se puede parsear
     */
    private LocalDateTime armaFechaHora(Map<Integer, String> valores, FormatoArchivoMarcacion formato,
            int numeroLinea) throws Throwable {

        String fechaHora = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.FECHA_Y_HORA));
        if (fechaHora != null && !fechaHora.isEmpty()) {
            String patron = formato.getFormatoFechaHora();
            if (patron == null || patron.trim().isEmpty()) {
                throw new IncomeException("el formato no declara el patron de fecha y hora"
                        + " (FMRCFRFH) y la linea trae la columna combinada");
            }
            try {
                return LocalDateTime.parse(fechaHora, DateTimeFormatter.ofPattern(patron.trim()));
            } catch (Throwable e) {
                throw new IncomeException("no se pudo leer la fecha y hora '" + fechaHora
                        + "' con el patron '" + patron.trim() + "'");
            }
        }

        String fecha = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.FECHA));
        String hora = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.HORA));
        if (fecha == null || fecha.isEmpty()) {
            throw new IncomeException("la linea no trae fecha");
        }
        String patronFecha = formato.getFormatoFecha();
        if (patronFecha == null || patronFecha.trim().isEmpty()) {
            throw new IncomeException("el formato no declara el patron de fecha (FMRCFRFC)");
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha, DateTimeFormatter.ofPattern(patronFecha.trim()));
        } catch (Throwable e) {
            throw new IncomeException("no se pudo leer la fecha '" + fecha + "' con el patron '"
                    + patronFecha.trim() + "'");
        }
        if (hora == null || hora.isEmpty()) {
            // Una marcacion sin hora es media marcacion: se rechaza la linea en vez de
            // suponer medianoche, que produciria una jornada de cero horas sin avisar.
            throw new IncomeException("la linea trae fecha pero no hora");
        }
        String patronHora = formato.getFormatoHora();
        if (patronHora == null || patronHora.trim().isEmpty()) {
            throw new IncomeException("el formato no declara el patron de hora (FMRCFRHR)");
        }
        try {
            LocalTime momento = LocalTime.parse(hora, DateTimeFormatter.ofPattern(patronHora.trim()));
            return LocalDateTime.of(dia, momento);
        } catch (Throwable e) {
            throw new IncomeException("no se pudo leer la hora '" + hora + "' con el patron '"
                    + patronHora.trim() + "'");
        }
    }

    /**
     * Localiza al empleado por codigo biometrico, con respaldo en la identificacion.
     *
     * @param valores		: Campos ya extraidos
     * @param idEmpresa		: Id de la empresa
     * @return				: El empleado, o null si no hay coincidencia
     * @throws Throwable	: Excepcion
     */
    private Empleado localizaEmpleado(Map<Integer, String> valores, Long idEmpresa) throws Throwable {
        String codigo = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.ID_EMPLEADO));
        Empleado empleado = empleadoDaoService.selectByCodigoBiometrico(codigo, idEmpresa);
        if (empleado != null) {
            return empleado;
        }
        // Respaldo: hay relojes que se configuran con la cedula como identificador.
        String identificacion = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.IDENTIFICACION));
        if (identificacion == null || identificacion.isEmpty()) {
            identificacion = codigo;
        }
        if (identificacion == null || identificacion.isEmpty()) {
            return null;
        }
        return empleadoDaoService.selectByIdentificacion(identificacion, idEmpresa);
    }

    /**
     * Traduce el tipo de marcacion del reloj al detalle del rubro 192.
     *
     * <p>El mapeo vive en <code>DFMRMPEO</code>, con el formato
     * <code>origen=destino;origen=destino</code>. Si el archivo no trae tipo, o el valor no
     * esta mapeado, la marcacion entra como ENTRADA: la consolidacion reordena el dia por hora
     * de todas formas, asi que el tipo es informativo y no vale la pena perder la linea.</p>
     *
     * @param valores	: Campos ya extraidos
     * @param campos	: Campos del formato, de donde sale el mapeo
     * @return			: Detalle del rubro RHH_TIPO_MARCACION
     */
    private Long tipoMarcacion(Map<Integer, String> valores, List<DetalleFormatoMarcacion> campos) {
        String crudo = valores.get(Integer.valueOf(RhhCampoArchivoMarcacion.TIPO_MARCACION));
        if (crudo == null || crudo.isEmpty()) {
            return Long.valueOf(RhhTipoMarcacion.ENTRADA);
        }
        for (DetalleFormatoMarcacion campo : campos) {
            if (campo.getCampo() == null
                    || campo.getCampo().intValue() != RhhCampoArchivoMarcacion.TIPO_MARCACION) {
                continue;
            }
            String mapeo = campo.getMapeo();
            if (mapeo == null || mapeo.trim().isEmpty()) {
                break;
            }
            for (String pareja : mapeo.split(SEPARADOR_PAREJAS)) {
                String[] partes = pareja.split(SEPARADOR_MAPEO, 2);
                if (partes.length == 2 && partes[0].trim().equalsIgnoreCase(crudo)) {
                    try {
                        return Long.valueOf(partes[1].trim());
                    } catch (NumberFormatException e) {
                        System.out.println("Mapeo invalido en DFMRMPEO: '" + pareja + "'.");
                    }
                }
            }
            break;
        }
        System.out.println("Tipo de marcacion '" + crudo + "' sin mapeo: entra como ENTRADA.");
        return Long.valueOf(RhhTipoMarcacion.ENTRADA);
    }

    /**
     * Separa el contenido en lineas con la codificacion del formato.
     *
     * @param contenido		: Bytes del archivo
     * @param formato		: Formato del archivo
     * @return				: Lineas del archivo
     * @throws Throwable	: Excepcion
     */
    private List<String> separaLineas(byte[] contenido, FormatoArchivoMarcacion formato) throws Throwable {
        String nombre = formato.getCodificacion() != null && !formato.getCodificacion().trim().isEmpty()
                ? formato.getCodificacion().trim() : StandardCharsets.UTF_8.name();
        Charset juego;
        try {
            juego = Charset.forName(nombre);
        } catch (Throwable e) {
            throw new IncomeException("La codificacion '" + nombre + "' del formato '"
                    + formato.getNombre() + "' no la reconoce esta maquina virtual.");
        }
        List<String> lineas = new ArrayList<String>();
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), juego));
        String linea;
        while ((linea = lector.readLine()) != null) {
            lineas.add(linea);
        }
        lector.close();
        return lineas;
    }

    /**
     * Lee el stream completo a memoria.
     *
     * @param entrada		: Stream de entrada
     * @return				: Contenido del archivo
     * @throws Throwable	: Excepcion
     */
    private byte[] leeTodo(InputStream entrada) throws Throwable {
        if (entrada == null) {
            throw new IncomeException("No se recibio ningun archivo.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
        }
        return salida.toByteArray();
    }

    /**
     * SHA-256 del contenido del archivo.
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
     * Cuenta las marcaciones del lote que ya se consolidaron.
     *
     * @param idCarga	: Id de la carga
     * @return			: Numero de marcaciones consolidadas
     */
    private long cuentaConsolidadas(Long idCarga) {
        Object cuantas = em.createQuery(" select count(t) "
                + " from   Marcaciones t "
                + " where  t.cargaMarcaciones.codigo = :idCarga "
                + "        and t.procesado = 'S' ")
                .setParameter("idCarga", idCarga)
                .getSingleResult();
        return cuantas != null ? ((Long) cuantas).longValue() : 0L;
    }

    /**
     * Arma el log de la carga a partir de los errores de linea.
     *
     * @param errores	: Errores acumulados
     * @return			: El log, o null si no hubo errores
     */
    private String armaLog(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            return null;
        }
        StringBuilder log = new StringBuilder();
        for (String error : errores) {
            log.append(error).append("\n");
        }
        return log.toString();
    }

    /**
     * Concatena una entrada al log existente.
     *
     * @param log		: Log actual
     * @param entrada	: Entrada nueva
     * @return			: El log con la entrada al final
     */
    private String concatena(String log, String entrada) {
        if (log == null || log.trim().isEmpty()) {
            return entrada;
        }
        return log + "\n" + entrada;
    }

    /**
     * Devuelve el texto o un guion si es nulo, para los mensajes.
     *
     * @param valor	: Texto
     * @return		: El texto o un guion
     */
    private String texto(String valor) {
        return valor != null && !valor.isEmpty() ? valor : "-";
    }
}
