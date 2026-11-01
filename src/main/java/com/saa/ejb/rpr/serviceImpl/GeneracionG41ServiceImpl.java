package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.InformacionGeneralFondoService;
import com.saa.ejb.crd.service.ParticipeService;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.ejb.rpr.dao.SujetoReportadoG41DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG41Service;
import com.saa.ejb.rpr.service.GeneracionGException;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.model.crd.Exter;
import com.saa.model.crd.InformacionGeneralFondo;
import com.saa.model.crd.Participe;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.ParticipeActivoG41;
import com.saa.model.rpr.SaldoCuentaG42;
import com.saa.model.rpr.SujetoReportadoG41;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * G41 — Partícipes activos y voluntarios (Manual de Catastros FCPC v1.2, §2.2 y §4.2). D-038 (29-oct-2026).
 *
 * <p>Mensual: solo los partícipes que ingresan (I) o cuyo estado cambió (A) frente a lo último reportado, que
 * vive en {@code RPR.SG41}. Antes tomaba las entidades con la marca {@code ENTDIDST = 9} (sin fecha, se consumía
 * al generar): no podía regenerar un mes pasado y en junio 2025 salía vacía.</p>
 *
 * <ol>
 *   <li>Universo: los sujetos de la G42 de la misma ejecución que no están en la G43 (cesantes) ni en la G44
 *       (jubilados) del mes. Por eso la G41 se genera DESPUÉS de G42, G43 y G44.</li>
 *   <li>Estado del partícipe (tabla 195): A si aportó en el mes (aporte personal del mes de la G42 &gt; 0, que son
 *       los aportes 9/11 del período efectivo), P si no.</li>
 *   <li>Contra el último estado reportado (fila de SG41 con mayor período &lt; mes): no está → I; cambió → A;
 *       no cambió → no va. Cambios de estado civil y de relación laboral no se reportan: el SAA no guarda su
 *       historia (N11). No se envía E (decisión pendiente de ASOPREP).</li>
 *   <li>Cada registro emitido se agrega a SG41 (origen EJECUCION, período = mes). Regenerar el mes primero borra
 *       las filas EJECUCION del mes. Si el mes ya tiene filas de otro origen (mayo 2025: semilla + HM41), es un mes
 *       presentado: la G41 reproduce lo presentado (las filas con estado de registro) y no toca el registro.</li>
 * </ol>
 */
@Stateless
public class GeneracionG41ServiceImpl implements GeneracionG41Service {

    /** Tabla 196: S = sueldo. El SAA no guarda la base de cálculo por partícipe (todos aportan sobre el sueldo). */
    static final String BASE_CALCULO_SUELDO = "S";
    /** Tabla 197: N = nombramiento. El SAA no guarda la relación laboral por partícipe (N11). */
    static final String RELACION_NOMBRAMIENTO = "N";

    @EJB private DetalleEjecucionReporteService  ejrdService;
    @EJB private SaldoCuentaG42Service           g42Service;
    @EJB private ExterService                    exterService;
    @EJB private ParticipeService                participeService;
    @EJB private InformacionGeneralFondoService  igfnService;
    @EJB private ParticipeActivoG41DaoService    cg41DaoService;
    @EJB private SujetoReportadoG41DaoService    registroDao;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G41");
        Long codigoEjrc = detalle.getEjecucionReporte().getCodigo();
        int anio = detalle.getEjecucionReporte().getAnio().intValue();
        int mes  = detalle.getEjecucionReporte().getMes().intValue();
        long periodo = anio * 100L + mes;
        LocalDate corte = YearMonth.of(anio, mes).atEndOfMonth();

        // Tipo de sistema (tabla 193): el del fondo, desde la información general (CRD.IGFN).
        String tipoSistema = tipoSistemaDelFondo();

        // ---- Universo: G42 del mes sin G43 ni G44 ----
        DetalleEjecucionReporte ejrdG42 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G42");
        if (ejrdG42 == null) {
            throw new GeneracionGException("G41: la ejecución " + codigoEjrc + " no tiene G42; la G41 se calcula desde ella.");
        }
        List<SaldoCuentaG42> g42 = g42Service.selectByDetalle(ejrdG42.getCodigo());
        Set<String> excluidos = new HashSet<>();
        DetalleEjecucionReporte ejrdG43 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G43");
        if (ejrdG43 != null) for (String id : registroDao.selectIdentificacionesG43(ejrdG43.getCodigo())) excluidos.add(id.trim());
        DetalleEjecucionReporte ejrdG44 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G44");
        if (ejrdG44 != null) for (String id : registroDao.selectIdentificacionesG44(ejrdG44.getCodigo())) excluidos.add(id.trim());
        System.out.println("G41 - periodo " + periodo + ": G42 " + g42.size() + " sujetos, excluidos por G43/G44 " + excluidos.size());

        // ---- Mes presentado (semilla/HM41): reproducir lo presentado sin tocar el registro ----
        List<SujetoReportadoG41> delPeriodo = registroDao.selectDelPeriodo(periodo);
        boolean mesPresentado = delPeriodo.stream()
                .anyMatch(s -> !SujetoReportadoG41.ORIGEN_EJECUCION.equals(s.getOrigen()));
        Map<String, SaldoCuentaG42> g42PorId = new HashMap<>();
        for (SaldoCuentaG42 s : g42) if (s.getIdentificacion() != null) g42PorId.put(s.getIdentificacion().trim(), s);
        if (mesPresentado) {
            long n = 0;
            for (SujetoReportadoG41 s : delPeriodo) {
                if (s.getEstadoRegistro() == null) continue; // semilla: no se envió
                guardar(detalle, s.getTipoIdentificacion(), s.getIdentificacion().trim(), g42PorId.get(s.getIdentificacion().trim()),
                        s.getEstadoParticipe(), s.getEstadoRegistro(), corte, tipoSistema);
                n++;
            }
            System.out.println("G41 - periodo " + periodo + " ya presentado (registro SG41 de origen semilla/HM41): "
                    + n + " registros reproducidos; registro sin cambios");
            return n;
        }

        // ---- Estado anterior: la fila de mayor período < mes ----
        int borrados = registroDao.deleteEjecucionDelPeriodo(periodo);
        long posteriores = registroDao.countEjecucionPosterioresA(periodo);
        if (posteriores > 0) {
            System.out.println("G41 WARN - hay " + posteriores + " filas SG41 de meses posteriores a " + periodo
                    + ": regenerar también esos meses (su estado anterior cambia).");
        }
        Map<String, String> estadoAnterior = new HashMap<>();
        for (SujetoReportadoG41 s : registroDao.selectAnterioresA(periodo)) { // ascendente: el último gana
            if (s.getIdentificacion() != null) estadoAnterior.put(s.getIdentificacion().trim(), s.getEstadoParticipe());
        }
        System.out.println("G41 - " + borrados + " filas EJECUCION del periodo quitadas; sujetos con estado anterior: "
                + estadoAnterior.size());

        long ingresos = 0, cambios = 0, sinCambio = 0;
        for (SaldoCuentaG42 s : g42) {
            if (s.getIdentificacion() == null) continue;
            String id = s.getIdentificacion().trim();
            if (excluidos.contains(id)) continue;
            String estado = (s.getAportePersonal() != null && s.getAportePersonal() > 0) ? "A" : "P";
            String previo = estadoAnterior.get(id);
            String estadoRegistro;
            if (previo == null) {
                estadoRegistro = "I";
                ingresos++;
            } else if (!previo.equals(estado)) {
                estadoRegistro = "A";
                cambios++;
            } else {
                sinCambio++;
                continue;
            }
            String tipoId = s.getTipoIdentificacion() != null ? s.getTipoIdentificacion() : "C";
            guardar(detalle, tipoId, id, s, estado, estadoRegistro, corte, tipoSistema);

            SujetoReportadoG41 r = new SujetoReportadoG41();
            r.setTipoIdentificacion(tipoId);
            r.setIdentificacion(id);
            r.setEstadoParticipe(estado);
            r.setEstadoRegistro(estadoRegistro);
            r.setPeriodo(periodo);
            r.setOrigen(SujetoReportadoG41.ORIGEN_EJECUCION);
            r.setDetalleEjecucion(detalle.getCodigo());
            r.setFechaRegistro(LocalDate.now());
            registroDao.insertar(r);
        }
        System.out.println("G41 generado: " + ingresos + " I + " + cambios + " A (" + sinCambio + " sin cambio, no van)");
        return ingresos + cambios;
    }

    private void guardar(DetalleEjecucionReporte detalle, String tipoId, String id, SaldoCuentaG42 g42,
            String estadoParticipe, String estadoRegistro, LocalDate corte, String tipoSistema) throws Throwable {
        ParticipeActivoG41 cg41 = new ParticipeActivoG41();
        cg41.setDetalleEjecucion(detalle);
        cg41.setTipoIdentificacion(tipoId != null ? tipoId : "C");
        cg41.setIdentificacion(id);

        List<Exter> exters = exterService.selectByCedula(id);
        if (exters != null && !exters.isEmpty()) {
            Exter exter = exters.get(0);
            cg41.setGenero(codigoGenero(exter.getGenero()));
            cg41.setEstadoCivil(codigoEstadoCivil(exter.getEstadoCivil()));
            if (exter.getFechaNacimiento() != null) cg41.setFechaNacimiento(exter.getFechaNacimiento().toLocalDate());
        } else {
            System.out.println("G41 WARN - sin EXTR para el sujeto del EJRD G42 (entidad "
                    + (g42 != null && g42.getEntidad() != null ? g42.getEntidad().getCodigo() : "?") + ")");
        }
        if (g42 != null && g42.getEntidad() != null) {
            List<Participe> participes = participeService.selectByEntidad(g42.getEntidad().getCodigo());
            if (participes != null && !participes.isEmpty() && participes.get(0).getFechaIngresoFondo() != null) {
                cg41.setFechaIngreso(participes.get(0).getFechaIngresoFondo().toLocalDate());
            }
        }
        cg41.setEstadoParticipe(estadoParticipe);
        cg41.setTipoSistema(tipoSistema);
        cg41.setBaseCalculoAportacion(BASE_CALCULO_SUELDO);
        cg41.setTipoRelacionLaboral(RELACION_NOMBRAMIENTO);
        cg41.setEstadoRegistro(estadoRegistro);
        // §4.2: la fecha de actualización es obligatoria con A; "caso contrario se registrará en blanco" (I).
        cg41.setFechaActualizacionEstado("I".equals(estadoRegistro) ? null : corte);
        cg41DaoService.save(cg41, null);
    }

    private String tipoSistemaDelFondo() throws Throwable {
        List<InformacionGeneralFondo> igfn = igfnService.selectAll();
        String ts = (igfn != null && !igfn.isEmpty()) ? igfn.get(0).getTipoSistema() : null;
        ts = ts != null ? ts.trim().toUpperCase() : null;
        if (ts == null || !(ts.equals("D") || ts.equals("I") || ts.equals("M"))) {
            throw new GeneracionGException("G41: el tipo de sistema del fondo (CRD.IGFN.IGFNTPSS = '" + ts
                    + "') no es un código de la tabla 193 (D, I, M).");
        }
        return ts;
    }

    /** Tabla 8: M masculino, F femenino. EXTR guarda HOMBRE / MUJER. */
    static String codigoGenero(String genero) {
        if (genero == null || genero.isBlank()) return null;
        String g = genero.trim().toUpperCase();
        if (g.startsWith("H") || g.equals("M") || g.startsWith("MASC")) return "M";
        if (g.startsWith("MUJ") || g.equals("F") || g.startsWith("FEM")) return "F";
        return null;
    }

    /** Tabla 183: S soltero, C casado, V viudo, D divorciado, U unión de hecho. EXTR guarda el nombre completo. */
    static String codigoEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) return null;
        String e = estadoCivil.trim().toUpperCase();
        if (e.contains("UNION") || e.contains("UNIÓN")) return "U"; // "EN UNION DE HECHO" empieza con E
        char c = e.charAt(0);
        return (c == 'S' || c == 'C' || c == 'V' || c == 'D' || c == 'U') ? String.valueOf(c) : null;
    }
}
