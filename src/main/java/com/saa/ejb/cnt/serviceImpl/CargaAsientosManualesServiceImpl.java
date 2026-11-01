package com.saa.ejb.cnt.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.CargaAsientosManualesService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cnt.ResultadoCargaAsientosManuales;
import com.saa.model.cnt.ResultadoCargaAsientosManuales.AsientoCreado;
import com.saa.model.cnt.ResultadoCargaAsientosManuales.ErrorLinea;
import com.saa.model.cnt.SolicitudCargaAsientosManuales;
import com.saa.model.cnt.TipoAsiento;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoPeriodos;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Cargador de asientos manuales (C5, proyecto cuadre Superintendencia). Ver {@link CargaAsientosManualesService} y
 * docs/logica-negocio/cnt/CARGADOR-ASIENTOS-MANUALES.md.
 *
 * <p>Fases: (1) peticion y tipo de asiento; (2) parseo del CSV (cabecera por nombre de columna, separador ';',
 * comillas dobles opcionales, BOM tolerado); (3) agrupacion por {@code numero_asiento} en orden de aparicion;
 * (4) validaciones de linea y de asiento acumulando TODOS los errores; (5) si hay errores, se devuelve sin grabar;
 * (6) grabacion de todos los asientos con {@code generarAsientoManual} en la transaccion del bean. Las consultas de
 * la fase de validacion van por JPQL propio y devuelven listas (nunca lanzan {@code IncomeException}, que lleva
 * {@code rollback = true} y marcaria la transaccion durante una validacion).</p>
 */
@Stateless
public class CargaAsientosManualesServiceImpl implements CargaAsientosManualesService {

    /** U-2: todo proceso que graba valida el usuario PRIMERO (sin usuario válido no se hace nada). */
    @jakarta.ejb.EJB
    private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AsientoContableService asientoContableService;

    /** Prefijo de la observacion: "CARGA MANUAL [archivo] #numero_asiento | glosa | Respaldo: ...". */
    static final String PREFIJO_OBSERVACION = "CARGA MANUAL [";

    static final String NOMBRE_TIPO_POR_DEFECTO = "DIARIO";

    static final int LARGO_OBSERVACION = 2000;  // CNT.ASNT.ASNTOBSR

    static final int LARGO_DESCRIPCION = 200;   // CNT.DTAS.DTASDSCR

    static final long TIPO_CUENTA_MOVIMIENTO = 2L; // CNT.PLNN.PLNNTPOO

    static final char SEPARADOR = ';';

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    // Columnas de la plantilla (se comparan en minusculas, sin espacios)
    private static final String COL_FECHA = "fecha";
    private static final String COL_NUMERO = "numero_asiento";
    private static final String COL_GLOSA = "glosa";
    private static final String COL_CUENTA = "cuenta";
    private static final String COL_CUENTA_SAA = "cuenta_saa";
    private static final String COL_NOMBRE_CUENTA = "nombre_cuenta";
    private static final String COL_DEBE = "debe";
    private static final String COL_HABER = "haber";
    private static final String COL_RESPALDO = "documento_respaldo";
    private static final String COL_CENTRO = "centro_costo";

    /** Una linea del archivo ya parseada y, si valido, resuelta contra el plan. */
    private static class LineaArchivo {
        int numeroLinea;
        String fechaTexto;
        LocalDate fecha;
        String numeroAsiento;
        String glosa;
        String cuentaTexto;
        String nombreCuentaTexto;
        String debeTexto;
        String haberTexto;
        BigDecimal debe;
        BigDecimal haber;
        String documentoRespaldo;
        String centroCostoTexto;
        PlanCuenta planCuenta;
        CentroCosto centroCosto;
    }

    /** Un asiento del archivo: todas las lineas con el mismo numero_asiento. */
    private static class AsientoArchivo {
        String numeroAsiento;
        List<LineaArchivo> lineas = new ArrayList<>();
        LocalDate fecha;
        String glosa;
        long debeCentavos;
        long haberCentavos;
    }

    @Override
    public ResultadoCargaAsientosManuales cargar(SolicitudCargaAsientosManuales solicitud) throws Throwable {
        System.out.println("Ingresa al metodo cargar de CargaAsientosManualesService con archivo: "
                + (solicitud != null ? solicitud.getNombreArchivo() : null)
                + ", empresa: " + (solicitud != null ? solicitud.getIdEmpresa() : null));

        ResultadoCargaAsientosManuales resultado = new ResultadoCargaAsientosManuales();
        List<ErrorLinea> errores = resultado.getErrores();

        // ── 1. Peticion ─────────────────────────────────────────────────────────────────────────────────
        if (solicitud == null) {
            errores.add(new ErrorLinea(0, null, "No se recibio la peticion."));
            return resultado;
        }
        String nombreArchivo = solicitud.getNombreArchivo() != null ? solicitud.getNombreArchivo().trim() : "";
        resultado.setNombreArchivo(nombreArchivo);
        if (solicitud.getIdEmpresa() == null) {
            errores.add(new ErrorLinea(0, null, "El campo idEmpresa es obligatorio."));
        }
        // U-2: lo primero, el usuario existe (helper único); sin usuario válido no se hace nada.
        solicitud.setUsuario(usuarioValidacionService.validar(solicitud.getUsuario()));
        if (nombreArchivo.isEmpty()) {
            errores.add(new ErrorLinea(0, null, "El campo nombreArchivo es obligatorio: identifica el origen de los "
                    + "asientos y evita cargar dos veces el mismo archivo."));
        }
        if (solicitud.getContenido() == null || solicitud.getContenido().trim().isEmpty()) {
            errores.add(new ErrorLinea(0, null, "El archivo esta vacio."));
        }
        if (!errores.isEmpty()) {
            return resultado;
        }

        Empresa empresa = em.find(Empresa.class, solicitud.getIdEmpresa());
        if (empresa == null) {
            errores.add(new ErrorLinea(0, null, "No existe la empresa " + solicitud.getIdEmpresa() + "."));
            return resultado;
        }
        Long idEmpresa = empresa.getCodigo();

        TipoAsiento tipoAsiento = resolverTipoAsiento(idEmpresa, solicitud.getCodigoAltTipoAsiento(),
                solicitud.getNombreTipoAsiento(), errores);
        if (tipoAsiento != null) {
            resultado.setTipoAsiento(tipoAsiento.getNombre() + " (alterno " + tipoAsiento.getCodigoAlterno() + ")");
        }

        // ── 2. Parseo ───────────────────────────────────────────────────────────────────────────────────
        List<LineaArchivo> lineas = parsear(solicitud.getContenido(), errores);
        resultado.setLineasLeidas(lineas.size());
        if (lineas.isEmpty() && errores.isEmpty()) {
            errores.add(new ErrorLinea(0, null, "El archivo no tiene lineas de datos (solo cabecera)."));
        }

        // ── 3. Agrupacion por numero_asiento, en orden de aparicion ────────────────────────────────────
        Map<String, AsientoArchivo> asientos = new LinkedHashMap<>();
        for (LineaArchivo linea : lineas) {
            if (linea.numeroAsiento == null || linea.numeroAsiento.isEmpty()) {
                errores.add(new ErrorLinea(linea.numeroLinea, null, "numero_asiento vacio."));
                continue;
            }
            AsientoArchivo asiento = asientos.get(linea.numeroAsiento);
            if (asiento == null) {
                asiento = new AsientoArchivo();
                asiento.numeroAsiento = linea.numeroAsiento;
                asientos.put(linea.numeroAsiento, asiento);
            }
            asiento.lineas.add(linea);
        }
        resultado.setAsientosLeidos(asientos.size());

        // ── 4. Validaciones ─────────────────────────────────────────────────────────────────────────────
        Map<String, PlanCuenta> cuentasPorCodigo = new HashMap<>();
        Map<String, List<PlanCuenta>> cuentasPorDigitos = new HashMap<>();
        List<PlanCuenta> plan = cargarPlanCuentas(idEmpresa, cuentasPorCodigo, cuentasPorDigitos);
        Map<String, CentroCosto> centrosPorNumero = cargarCentrosCosto(idEmpresa);
        Map<YearMonth, String> periodosRevisados = new HashMap<>(); // mes -> mensaje de error (null si esta bien)
        List<String> observacionesExistentes = observacionesDelArchivo(idEmpresa, nombreArchivo);

        for (AsientoArchivo asiento : asientos.values()) {
            for (LineaArchivo linea : asiento.lineas) {
                validarLinea(linea, plan, cuentasPorCodigo, cuentasPorDigitos, centrosPorNumero, errores);
            }
            validarAsiento(asiento, idEmpresa, periodosRevisados, errores);

            // Idempotencia: misma empresa, mismo archivo, mismo numero_asiento, asiento activo
            String referencia = referencia(nombreArchivo, asiento.numeroAsiento);
            for (String obs : observacionesExistentes) {
                if (obs != null && obs.startsWith(referencia)) {
                    errores.add(new ErrorLinea(asiento.lineas.get(0).numeroLinea, asiento.numeroAsiento,
                            "El asiento " + asiento.numeroAsiento + " del archivo '" + nombreArchivo
                            + "' ya esta registrado y activo en la empresa " + idEmpresa
                            + " (observacion '" + referencia + "...'). Si hay que volver a cargarlo, anule o "
                            + "reverse el asiento existente primero, o cargue el archivo con otro nombre."));
                    break;
                }
            }
        }

        // ── 5. Rechazo total si hay errores ─────────────────────────────────────────────────────────────
        if (!errores.isEmpty() || tipoAsiento == null) {
            resultado.setExito(false);
            System.out.println("cargar: archivo '" + nombreArchivo + "' rechazado con " + errores.size()
                    + " error(es); no se registro nada.");
            return resultado;
        }

        // ── 6. Grabacion: todo o nada ───────────────────────────────────────────────────────────────────
        for (AsientoArchivo asiento : asientos.values()) {
            try {
                Asiento grabado = asientoContableService.generarAsientoManual(idEmpresa, tipoAsiento,
                        asiento.fecha, observacion(nombreArchivo, asiento), solicitud.getUsuario().trim(),
                        construirLineas(asiento));
                AsientoCreado creado = new AsientoCreado();
                creado.setIdAsiento(grabado.getCodigo());
                creado.setNumeroAlterno(grabado.getNumeroAlterno());
                creado.setNumeroAsientoCliente(asiento.numeroAsiento);
                creado.setFecha(asiento.fecha);
                creado.setLineas(asiento.lineas.size());
                creado.setDebe(asiento.debeCentavos / 100.0);
                creado.setHaber(asiento.haberCentavos / 100.0);
                resultado.getAsientos().add(creado);
            } catch (Throwable e) {
                // Una excepcion checked no marca rollback por si sola en un EJB: se marca a mano y se relanza
                // como IncomeException (rollback = true) para que el lote entero se revierta.
                sessionContext.setRollbackOnly();
                throw new IncomeException("Error al registrar el asiento " + asiento.numeroAsiento
                        + " del archivo '" + nombreArchivo + "'; no quedo grabado ningun asiento del archivo. "
                        + "Causa: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }
        resultado.setExito(true);
        System.out.println("cargar: archivo '" + nombreArchivo + "' registrado, " + resultado.getAsientos().size()
                + " asiento(s) de tipo " + tipoAsiento.getNombre());
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────────────
    // Tipo de asiento
    // ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Alterno nulo o 0: por nombre (DIARIO por defecto), porque DIARIO / INGRESO / EGRESO comparten el alterno 0.
     * Alterno distinto de 0: por alterno; si ademas viene nombre, tiene que coincidir (y desempata cuando varios
     * tipos comparten el alterno). Solo tipos activos de la empresa.
     */
    private TipoAsiento resolverTipoAsiento(Long idEmpresa, Integer alterno, String nombre, List<ErrorLinea> errores) {
        String nombreBuscado = (nombre != null && !nombre.trim().isEmpty())
                ? nombre.trim().toUpperCase(Locale.ROOT) : null;
        List<TipoAsiento> candidatos;
        if (alterno == null || alterno.intValue() == 0) {
            if (nombreBuscado == null) {
                nombreBuscado = NOMBRE_TIPO_POR_DEFECTO;
            }
            candidatos = em.createQuery(
                    "select t from TipoAsiento t where t.empresa.codigo = :empresa and t.estado = :activo "
                    + "and upper(t.nombre) = :nombre order by t.codigo", TipoAsiento.class)
                    .setParameter("empresa", idEmpresa)
                    .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                    .setParameter("nombre", nombreBuscado)
                    .getResultList();
            if (candidatos.isEmpty()) {
                errores.add(new ErrorLinea(0, null, "La empresa " + idEmpresa + " no tiene un tipo de asiento activo "
                        + "con nombre '" + nombreBuscado + "' (CNT.PLNT). Cree el tipo o indique otro nombre."));
                return null;
            }
        } else {
            candidatos = em.createQuery(
                    "select t from TipoAsiento t where t.empresa.codigo = :empresa and t.estado = :activo "
                    + "and t.codigoAlterno = :alterno order by t.codigo", TipoAsiento.class)
                    .setParameter("empresa", idEmpresa)
                    .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                    .setParameter("alterno", Long.valueOf(alterno.intValue()))
                    .getResultList();
            if (candidatos.isEmpty()) {
                errores.add(new ErrorLinea(0, null, "La empresa " + idEmpresa + " no tiene un tipo de asiento activo "
                        + "con codigo alterno " + alterno + " (CNT.PLNT)."));
                return null;
            }
            if (nombreBuscado != null) {
                List<TipoAsiento> porNombre = new ArrayList<>();
                for (TipoAsiento t : candidatos) {
                    if (t.getNombre() != null && t.getNombre().trim().toUpperCase(Locale.ROOT).equals(nombreBuscado)) {
                        porNombre.add(t);
                    }
                }
                candidatos = porNombre;
                if (candidatos.isEmpty()) {
                    errores.add(new ErrorLinea(0, null, "Ningun tipo de asiento con codigo alterno " + alterno
                            + " de la empresa " + idEmpresa + " se llama '" + nombreBuscado + "'."));
                    return null;
                }
            }
        }
        if (candidatos.size() > 1) {
            StringBuilder nombres = new StringBuilder();
            for (TipoAsiento t : candidatos) {
                nombres.append(nombres.length() > 0 ? ", " : "").append(t.getNombre())
                       .append(" (alterno ").append(t.getCodigoAlterno()).append(")");
            }
            errores.add(new ErrorLinea(0, null, "El tipo de asiento es ambiguo en la empresa " + idEmpresa + ": "
                    + nombres + ". Indique nombreTipoAsiento para desempatar."));
            return null;
        }
        return candidatos.get(0);
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────────────
    // Parseo del CSV
    // ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /** Lee cabecera y filas. Las columnas se ubican por nombre; cuenta_saa tiene prioridad sobre cuenta. */
    private List<LineaArchivo> parsear(String contenido, List<ErrorLinea> errores) {
        List<LineaArchivo> lineas = new ArrayList<>();
        String texto = contenido.startsWith("﻿") ? contenido.substring(1) : contenido;
        String[] filas = texto.split("\r\n|\n|\r", -1);

        // Cabecera
        int numeroLineaCabecera = 0;
        String[] cabecera = null;
        for (int i = 0; i < filas.length; i++) {
            if (!filas[i].trim().isEmpty()) {
                cabecera = dividir(filas[i]);
                numeroLineaCabecera = i + 1;
                break;
            }
        }
        if (cabecera == null) {
            errores.add(new ErrorLinea(0, null, "El archivo esta vacio."));
            return lineas;
        }
        Map<String, Integer> columnas = new HashMap<>();
        for (int c = 0; c < cabecera.length; c++) {
            String nombre = cabecera[c].trim().toLowerCase(Locale.ROOT);
            if (!nombre.isEmpty() && !columnas.containsKey(nombre)) {
                columnas.put(nombre, c);
            }
        }
        boolean cabeceraValida = true;
        for (String obligatoria : new String[] { COL_FECHA, COL_NUMERO, COL_GLOSA, COL_DEBE, COL_HABER }) {
            if (!columnas.containsKey(obligatoria)) {
                errores.add(new ErrorLinea(numeroLineaCabecera, null, "Falta la columna '" + obligatoria
                        + "' en la cabecera. Columnas esperadas: fecha;numero_asiento;glosa;cuenta;nombre_cuenta;"
                        + "debe;haber;documento_respaldo;centro_costo (separador ';')."));
                cabeceraValida = false;
            }
        }
        if (!columnas.containsKey(COL_CUENTA) && !columnas.containsKey(COL_CUENTA_SAA)) {
            errores.add(new ErrorLinea(numeroLineaCabecera, null,
                    "Falta la columna de cuenta: 'cuenta' (codigo del catalogo o del SAA) o 'cuenta_saa'."));
            cabeceraValida = false;
        }
        if (!cabeceraValida) {
            return lineas;
        }
        Integer colCuenta = columnas.containsKey(COL_CUENTA_SAA) ? columnas.get(COL_CUENTA_SAA) : columnas.get(COL_CUENTA);

        for (int i = numeroLineaCabecera; i < filas.length; i++) {
            if (filas[i].trim().isEmpty()) {
                continue;
            }
            String[] campos = dividir(filas[i]);
            LineaArchivo linea = new LineaArchivo();
            linea.numeroLinea = i + 1;
            linea.fechaTexto = campo(campos, columnas.get(COL_FECHA));
            linea.numeroAsiento = campo(campos, columnas.get(COL_NUMERO));
            linea.glosa = campo(campos, columnas.get(COL_GLOSA));
            linea.cuentaTexto = campo(campos, colCuenta);
            linea.nombreCuentaTexto = campo(campos, columnas.get(COL_NOMBRE_CUENTA));
            linea.debeTexto = campo(campos, columnas.get(COL_DEBE));
            linea.haberTexto = campo(campos, columnas.get(COL_HABER));
            linea.documentoRespaldo = campo(campos, columnas.get(COL_RESPALDO));
            linea.centroCostoTexto = campo(campos, columnas.get(COL_CENTRO));
            lineas.add(linea);
        }
        return lineas;
    }

    /** Divide una fila por ';' respetando comillas dobles ("" = comilla literal). */
    private static String[] dividir(String fila) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < fila.length(); i++) {
            char c = fila.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < fila.length() && fila.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == SEPARADOR) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos.toArray(new String[0]);
    }

    private static String campo(String[] campos, Integer indice) {
        if (indice == null || indice < 0 || indice >= campos.length) {
            return "";
        }
        return campos[indice].trim();
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────────────
    // Validaciones
    // ─────────────────────────────────────────────────────────────────────────────────────────────────────

    private void validarLinea(LineaArchivo linea, List<PlanCuenta> plan, Map<String, PlanCuenta> cuentasPorCodigo,
            Map<String, List<PlanCuenta>> cuentasPorDigitos, Map<String, CentroCosto> centrosPorNumero,
            List<ErrorLinea> errores) {
        int n = linea.numeroLinea;
        String na = linea.numeroAsiento;

        // Fecha
        if (linea.fechaTexto.isEmpty()) {
            errores.add(new ErrorLinea(n, na, "fecha vacia (formato AAAA-MM-DD)."));
        } else {
            try {
                linea.fecha = LocalDate.parse(linea.fechaTexto, FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                errores.add(new ErrorLinea(n, na, "fecha invalida '" + linea.fechaTexto + "' (formato AAAA-MM-DD)."));
            }
        }

        // Importes
        linea.debe = importe(linea.debeTexto, "debe", n, na, errores);
        linea.haber = importe(linea.haberTexto, "haber", n, na, errores);
        if (linea.debe != null && linea.haber != null) {
            boolean hayDebe = linea.debe.signum() > 0;
            boolean hayHaber = linea.haber.signum() > 0;
            if (hayDebe && hayHaber) {
                errores.add(new ErrorLinea(n, na, "la linea tiene valor en debe (" + linea.debe + ") y en haber ("
                        + linea.haber + "): uno de los dos debe ser cero."));
            } else if (!hayDebe && !hayHaber) {
                errores.add(new ErrorLinea(n, na, "la linea tiene debe y haber en cero."));
            }
        }

        // Cuenta
        linea.planCuenta = resolverCuenta(linea, plan, cuentasPorCodigo, cuentasPorDigitos, errores);

        // Centro de costo (opcional)
        if (!linea.centroCostoTexto.isEmpty()) {
            CentroCosto centro = centrosPorNumero.get(linea.centroCostoTexto);
            if (centro == null) {
                errores.add(new ErrorLinea(n, na, "el centro de costo '" + linea.centroCostoTexto
                        + "' no existe en la empresa (CNT.CNCS)."));
            } else if (!Long.valueOf(Estado.ACTIVO).equals(centro.getEstado())) {
                errores.add(new ErrorLinea(n, na, "el centro de costo '" + linea.centroCostoTexto + "' esta inactivo."));
            } else {
                linea.centroCosto = centro;
            }
        }
    }

    /** Importe no negativo con hasta dos decimales; acepta coma decimal si no hay punto. */
    private static BigDecimal importe(String texto, String columna, int n, String na, List<ErrorLinea> errores) {
        String t = texto.replace(" ", "");
        if (t.isEmpty()) {
            t = "0";
        }
        if (t.indexOf(',') >= 0) {
            if (t.indexOf('.') >= 0) {
                errores.add(new ErrorLinea(n, na, columna + " invalido '" + texto
                        + "': use punto decimal y sin separador de miles."));
                return null;
            }
            t = t.replace(',', '.');
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(t);
        } catch (NumberFormatException e) {
            errores.add(new ErrorLinea(n, na, columna + " invalido '" + texto + "' (numero con punto decimal)."));
            return null;
        }
        if (valor.signum() < 0) {
            errores.add(new ErrorLinea(n, na, columna + " negativo (" + texto + "): los importes van en valor "
                    + "absoluto, en el lado que corresponda."));
            return null;
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            errores.add(new ErrorLinea(n, na, columna + " con mas de dos decimales (" + texto + ")."));
            return null;
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Cuenta del SAA: (a) exacta por PLNNCNTA (con puntos, p. ej. 1.6.02.15); (b) si el codigo viene sin puntos
     * (catalogo de la Super, p. ej. 160215), la cuenta del SAA cuyos digitos coinciden, solo si es unica. En los dos
     * casos tiene que estar activa y ser de movimiento (PLNNTPOO = 2).
     */
    private PlanCuenta resolverCuenta(LineaArchivo linea, List<PlanCuenta> plan, Map<String, PlanCuenta> porCodigo,
            Map<String, List<PlanCuenta>> porDigitos, List<ErrorLinea> errores) {
        int n = linea.numeroLinea;
        String na = linea.numeroAsiento;
        String codigo = linea.cuentaTexto;
        if (codigo.isEmpty()) {
            errores.add(new ErrorLinea(n, na, "cuenta vacia."));
            return null;
        }
        PlanCuenta cuenta = porCodigo.get(codigo);
        if (cuenta == null) {
            if (codigo.indexOf('.') >= 0) {
                errores.add(new ErrorLinea(n, na, "la cuenta '" + codigo + "' no existe en el plan de la empresa."));
                return null;
            }
            List<PlanCuenta> candidatas = porDigitos.get(codigo);
            if (candidatas == null || candidatas.isEmpty()) {
                errores.add(new ErrorLinea(n, na, "la cuenta '" + codigo + "' no existe en el plan de la empresa "
                        + "ni como codigo del catalogo (ninguna cuenta del SAA tiene esos digitos)."));
                return null;
            }
            if (candidatas.size() > 1) {
                errores.add(new ErrorLinea(n, na, "el codigo '" + codigo + "' corresponde a " + candidatas.size()
                        + " cuentas del SAA (" + listaCuentas(candidatas, 5) + "): indique la cuenta con puntos."));
                return null;
            }
            cuenta = candidatas.get(0);
        }
        if (!Long.valueOf(Estado.ACTIVO).equals(cuenta.getEstado())) {
            errores.add(new ErrorLinea(n, na, "la cuenta " + cuenta.getCuentaContable() + " '" + cuenta.getNombre()
                    + "' esta inactiva."));
            return null;
        }
        if (!Long.valueOf(TIPO_CUENTA_MOVIMIENTO).equals(cuenta.getTipo())) {
            List<PlanCuenta> hijas = new ArrayList<>();
            String prefijo = cuenta.getCuentaContable() + ".";
            for (PlanCuenta p : plan) {
                if (p.getCuentaContable() != null && p.getCuentaContable().startsWith(prefijo)
                        && Long.valueOf(TIPO_CUENTA_MOVIMIENTO).equals(p.getTipo())
                        && Long.valueOf(Estado.ACTIVO).equals(p.getEstado())) {
                    hijas.add(p);
                }
            }
            errores.add(new ErrorLinea(n, na, "la cuenta " + cuenta.getCuentaContable() + " '" + cuenta.getNombre()
                    + "' es de agrupacion (nivel " + cuenta.getNivel() + "), no de movimiento. "
                    + (hijas.isEmpty() ? "No tiene cuentas de movimiento debajo."
                            : "Use una de sus " + hijas.size() + " cuentas de movimiento, p. ej. " + listaCuentas(hijas, 5) + ".")));
            return null;
        }
        return cuenta;
    }

    private static String listaCuentas(List<PlanCuenta> cuentas, int maximo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cuentas.size() && i < maximo; i++) {
            sb.append(i > 0 ? ", " : "").append(cuentas.get(i).getCuentaContable())
              .append(" '").append(cuentas.get(i).getNombre()).append("'");
        }
        if (cuentas.size() > maximo) {
            sb.append(", ...");
        }
        return sb.toString();
    }

    /** Fecha unica por asiento, periodo existente y no mayorizado ni cerrado, y cuadre al centavo. */
    private void validarAsiento(AsientoArchivo asiento, Long idEmpresa, Map<YearMonth, String> periodosRevisados,
            List<ErrorLinea> errores) {
        LineaArchivo primera = asiento.lineas.get(0);
        int n = primera.numeroLinea;
        String na = asiento.numeroAsiento;

        // Fecha y glosa del asiento: las de la primera linea; todas las lineas deben traer la misma fecha
        asiento.fecha = primera.fecha;
        for (LineaArchivo linea : asiento.lineas) {
            if (asiento.glosa == null && !linea.glosa.isEmpty()) {
                asiento.glosa = linea.glosa;
            }
            if (linea.fecha != null && asiento.fecha != null && !linea.fecha.equals(asiento.fecha)) {
                errores.add(new ErrorLinea(linea.numeroLinea, na, "la fecha " + linea.fecha + " no coincide con la "
                        + "de la primera linea del asiento (" + asiento.fecha + ", linea " + n + ")."));
            }
        }
        if (asiento.glosa == null) {
            errores.add(new ErrorLinea(n, na, "glosa vacia en todas las lineas del asiento."));
        }

        // Periodo contable de la fecha (una consulta por mes distinto)
        if (asiento.fecha != null) {
            YearMonth mes = YearMonth.from(asiento.fecha);
            if (!periodosRevisados.containsKey(mes)) {
                periodosRevisados.put(mes, revisarPeriodo(idEmpresa, mes));
            }
            String problema = periodosRevisados.get(mes);
            if (problema != null) {
                errores.add(new ErrorLinea(n, na, problema));
            }
        }

        // Cuadre al centavo (solo si todos los importes se pudieron leer)
        boolean importesCompletos = true;
        long debe = 0L;
        long haber = 0L;
        for (LineaArchivo linea : asiento.lineas) {
            if (linea.debe == null || linea.haber == null) {
                importesCompletos = false;
                break;
            }
            debe += linea.debe.movePointRight(2).longValueExact();
            haber += linea.haber.movePointRight(2).longValueExact();
        }
        if (importesCompletos) {
            asiento.debeCentavos = debe;
            asiento.haberCentavos = haber;
            if (debe != haber) {
                errores.add(new ErrorLinea(n, na, String.format(Locale.US,
                        "el asiento no cuadra: debe %.2f, haber %.2f, diferencia %.2f (%d lineas).",
                        debe / 100.0, haber / 100.0, Math.abs(debe - haber) / 100.0, asiento.lineas.size())));
            }
        }
    }

    /** Null si el periodo existe y admite asientos; si no, el mensaje de error. */
    private String revisarPeriodo(Long idEmpresa, YearMonth mes) {
        List<Periodo> periodos = em.createQuery(
                "select p from Periodo p where p.empresa.codigo = :empresa and p.anio = :anio and p.mes = :mes",
                Periodo.class)
                .setParameter("empresa", idEmpresa)
                .setParameter("anio", Long.valueOf(mes.getYear()))
                .setParameter("mes", Long.valueOf(mes.getMonthValue()))
                .getResultList();
        if (periodos.isEmpty()) {
            return "no existe el periodo contable " + mes.getMonthValue() + "/" + mes.getYear() + " en la empresa "
                    + idEmpresa + " (CNT.PRDO). Cree el periodo antes de cargar.";
        }
        Periodo periodo = periodos.get(0);
        // C17: cierre contable (PRDOCNCR) antes que mayorizado, porque un período cerrado también está mayorizado
        // y el remedio es otro (reabrir, no desmayorizar).
        if (com.saa.model.cnt.CierrePeriodoListener.contableCerrado(periodo)) {
            return com.saa.model.cnt.CierrePeriodoListener.mensaje(periodo, "asientos manuales del cargador");
        }
        if (Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(periodo.getEstado())) {
            return "el periodo " + mes.getMonthValue() + "/" + mes.getYear() + " esta MAYORIZADO: desmayorice antes de "
                    + "cargar asientos en ese mes.";
        }
        if (Long.valueOf(EstadoPeriodos.CERRADO).equals(periodo.getEstado())) {
            return "el periodo " + mes.getMonthValue() + "/" + mes.getYear() + " esta CERRADO: no admite asientos.";
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────────────
    // Catalogos de la empresa (una consulta por archivo)
    // ─────────────────────────────────────────────────────────────────────────────────────────────────────

    private List<PlanCuenta> cargarPlanCuentas(Long idEmpresa, Map<String, PlanCuenta> porCodigo,
            Map<String, List<PlanCuenta>> porDigitos) {
        List<PlanCuenta> plan = em.createQuery(
                "select p from PlanCuenta p where p.empresa.codigo = :empresa order by p.cuentaContable", PlanCuenta.class)
                .setParameter("empresa", idEmpresa)
                .getResultList();
        for (PlanCuenta p : plan) {
            if (p.getCuentaContable() == null) {
                continue;
            }
            String codigo = p.getCuentaContable().trim();
            porCodigo.putIfAbsent(codigo, p);
            String digitos = codigo.replace(".", "");
            porDigitos.computeIfAbsent(digitos, k -> new ArrayList<>()).add(p);
        }
        return plan;
    }

    private Map<String, CentroCosto> cargarCentrosCosto(Long idEmpresa) {
        List<CentroCosto> centros = em.createQuery(
                "select c from CentroCosto c where c.empresa.codigo = :empresa", CentroCosto.class)
                .setParameter("empresa", idEmpresa)
                .getResultList();
        Map<String, CentroCosto> porNumero = new HashMap<>();
        for (CentroCosto c : centros) {
            if (c.getNumero() != null) {
                porNumero.putIfAbsent(c.getNumero().trim(), c);
            }
        }
        return porNumero;
    }

    /** Observaciones de los asientos activos de la empresa que ya referencian este archivo. */
    private List<String> observacionesDelArchivo(Long idEmpresa, String nombreArchivo) {
        String patron = escaparLike(PREFIJO_OBSERVACION + nombreArchivo + "] #") + "%";
        return em.createQuery(
                "select a.observaciones from Asiento a where a.empresa.codigo = :empresa and a.estado = :activo "
                + "and a.observaciones like :patron escape '\\'", String.class)
                .setParameter("empresa", idEmpresa)
                .setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO))
                .setParameter("patron", patron)
                .getResultList();
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────────────
    // Construccion del asiento
    // ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /** Referencia unica de un asiento del archivo: prefijo comun de su observacion. */
    static String referencia(String nombreArchivo, String numeroAsiento) {
        return PREFIJO_OBSERVACION + nombreArchivo + "] #" + numeroAsiento + " |";
    }

    private static String observacion(String nombreArchivo, AsientoArchivo asiento) {
        Set<String> respaldos = new LinkedHashSet<>();
        for (LineaArchivo linea : asiento.lineas) {
            if (!linea.documentoRespaldo.isEmpty()) {
                respaldos.add(linea.documentoRespaldo);
            }
        }
        StringBuilder obs = new StringBuilder(referencia(nombreArchivo, asiento.numeroAsiento))
                .append(' ').append(asiento.glosa);
        if (!respaldos.isEmpty()) {
            obs.append(" | Respaldo: ").append(String.join("; ", respaldos));
        }
        return recortar(obs.toString(), LARGO_OBSERVACION);
    }

    private static List<DetalleAsiento> construirLineas(AsientoArchivo asiento) {
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (LineaArchivo linea : asiento.lineas) {
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(linea.planCuenta);
            detalle.setNumeroCuenta(linea.planCuenta.getCuentaContable());
            detalle.setNombreCuenta(linea.planCuenta.getNombre());
            String descripcion = !linea.documentoRespaldo.isEmpty() ? linea.documentoRespaldo : asiento.glosa;
            detalle.setDescripcion(recortar(descripcion, LARGO_DESCRIPCION));
            detalle.setValorDebe(linea.debe.doubleValue());
            detalle.setValorHaber(linea.haber.doubleValue());
            detalle.setCentroCosto(linea.centroCosto);
            lineas.add(detalle);
        }
        return lineas;
    }

    private static String recortar(String texto, int largo) {
        if (texto == null || texto.length() <= largo) {
            return texto;
        }
        return texto.substring(0, largo);
    }

}
