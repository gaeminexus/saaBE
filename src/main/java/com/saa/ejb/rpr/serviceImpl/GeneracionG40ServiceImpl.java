package com.saa.ejb.rpr.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.saa.ejb.crd.service.InformacionGeneralFondoService;
import com.saa.ejb.rpr.dao.CatalogoGeograficoDaoService;
import com.saa.ejb.rpr.dao.CreditoG40DaoService;
import com.saa.ejb.rpr.service.GeneracionG40Service;
import com.saa.ejb.rpr.service.GeneracionGException;
import com.saa.model.crd.InformacionGeneralFondo;
import com.saa.model.rpr.CreditoG40;
import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * G40 — Datos generales del FCPC (Manual de Catastros FCPC v1.2, §2.1 y §4.1). D-038/D-039 (29-oct-2026).
 *
 * <p>TODOS los valores salen de la información general del fondo ({@code CRD.IGFN}, pantalla de Información
 * General del Fondo); el código no fija ningún valor. Lo único que hace el generador es llevar cada valor al
 * tipo, la longitud y la codificación del manual:</p>
 * <ul>
 *   <li>provincia (tabla 6) y cantón (tabla 7): IGFN guarda el nombre ("Pichincha", "Quito"); se codifica con los
 *       catálogos del SAA (PRVN.PRVNCDAL, CDDD.CDDDCDAL). Si IGFN ya trae el código, se usa tal cual;</li>
 *   <li>tipo FCPC (tabla 320: 1, 2, 3): IGFN puede traer "III" o "Tipo III" → "3";</li>
 *   <li>teléfono: texto, tal como está en IGFN (Caracter 30);</li>
 *   <li>porcentajes y valores de aporte: Numérico(5,2), entre 0 y 999,99. Los valores 23/24 son el aporte MÍNIMO
 *       del partícipe (no saldos: se retiró la regla "Σ saldoAportePersonal de la G42").</li>
 * </ul>
 * <p>Si un valor de IGFN no cumple el manual (longitud, tabla, obligatoriedad, SI/NO del cambio de nombre), la G40
 * no se genera y la novedad dice qué campo corregir en la pantalla: una estructura con errores se considera
 * información no recibida (manual, §1).</p>
 */
@Stateless
public class GeneracionG40ServiceImpl implements GeneracionG40Service {

    private static final long MES_JUNIO     = 6L;
    private static final long MES_DICIEMBRE = 12L;

    /** Tablas de referencia de la Superintendencia (TABLAS_ESTRUCTURAS_EXCEL). */
    static final Set<String> TABLA_4_TIPO_ID_FCPC   = Set.of("R");
    static final Set<String> TABLA_193_TIPO_SISTEMA = Set.of("D", "I", "M");
    static final Set<String> TABLA_194_PRESTACION   = Set.of("C", "J", "M");
    static final Set<String> TABLA_318_TIPO_APORTE  = Set.of("P", "V");
    static final Set<String> TABLA_319_ADMIN        = Set.of("B", "P");
    static final Set<String> TABLA_320_TIPO_FCPC    = Set.of("1", "2", "3");

    @EJB private InformacionGeneralFondoService igfnService;
    @EJB private CreditoG40DaoService           cg40DaoService;
    @EJB private CatalogoGeograficoDaoService   geografia;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G40");
        long mes = detalle.getEjecucionReporte().getMes();
        if (mes != MES_JUNIO && mes != MES_DICIEMBRE) {
            System.out.println("G40 solo se genera en junio y diciembre (semestral). Mes actual: " + mes + " → G40 omitido");
            return 0L;
        }

        List<InformacionGeneralFondo> lista = igfnService.selectAll();
        if (lista == null || lista.isEmpty()) {
            throw new GeneracionGException("G40: no hay registro de información general del fondo (CRD.IGFN).");
        }
        InformacionGeneralFondo igfn = lista.get(0);
        List<String> errores = new ArrayList<>();

        CreditoG40 cg40 = new CreditoG40();
        cg40.setDetalleEjecucion(detalle);
        cg40.setTipoIdentificacionFcpc(codigo(igfn.getTipoIdentificacionFcpc(), TABLA_4_TIPO_ID_FCPC, "1 tipo de identificación (tabla 4)", errores));
        cg40.setIdentificacionFcpc(texto(igfn.getIdentificacionFcpc(), 13, true, "2 identificación del FCPC", errores));
        if (cg40.getIdentificacionFcpc() != null && !cg40.getIdentificacionFcpc().matches("\\d{13}")) {
            errores.add("2 identificación del FCPC: el RUC debe tener 13 dígitos");
        }
        cg40.setNumeroResolucion(texto(igfn.getNumeroResolucion(), 15, true, "3 número de resolución", errores));
        cg40.setFechaResolucion(igfn.getFechaResolucion());
        if (igfn.getFechaResolucion() == null) errores.add("4 fecha de resolución: obligatoria");

        String provincia = geografia.codigoProvincia(igfn.getProvincia());
        if (provincia == null) errores.add("5 provincia: '" + igfn.getProvincia() + "' no está en el catálogo de provincias (tabla 6)");
        cg40.setProvincia(provincia);
        String canton = geografia.codigoCanton(igfn.getProvincia(), igfn.getCanton());
        if (canton == null) errores.add("6 cantón: '" + igfn.getCanton() + "' no está en el catálogo de cantones de la provincia (tabla 7)");
        cg40.setCanton(canton);

        cg40.setDireccion(texto(igfn.getDireccion(), 60, true, "7 dirección", errores));
        cg40.setTelefonos(texto(igfn.getTelefonos(), 30, true, "8 teléfonos", errores));
        cg40.setCorreoElectronico(texto(igfn.getCorreoElectronico(), 60, true, "9 correo electrónico", errores));
        cg40.setTipoSistema(codigo(igfn.getTipoSistema(), TABLA_193_TIPO_SISTEMA, "10 tipo de sistema (tabla 193)", errores));
        cg40.setTipoPrestacion(codigo(igfn.getTipoPrestacion(), TABLA_194_PRESTACION, "11 tipo de prestación (tabla 194)", errores));
        cg40.setTipoAporte(codigo(igfn.getTipoAporte(), TABLA_318_TIPO_APORTE, "12 tipo de aporte (tabla 318)", errores));
        cg40.setTipoAdministracion(codigo(igfn.getTipoAdministracion(), TABLA_319_ADMIN, "13 tipo de administración (tabla 319)", errores));
        cg40.setFechaTraspaso(igfn.getFechaTraspaso());
        if ("B".equals(cg40.getTipoAdministracion()) && igfn.getFechaTraspaso() == null) {
            errores.add("14 fecha de traspaso: obligatoria cuando la administración es B (BIESS)");
        }
        cg40.setTipoFcpc(codigo(codigoTipoFcpc(igfn.getTipoFcpc()), TABLA_320_TIPO_FCPC, "15 tipo FCPC (tabla 320: 1, 2, 3)", errores));
        cg40.setNumeroResolucionCambioEstatuto(texto(igfn.getNumeroResolucionCambioEstatuto(), 16, false, "16 resolución de cambio de estatuto", errores));
        cg40.setFechaResolucionCambioEstatuto(igfn.getFechaResolucionCambioEstatuto());
        if (cg40.getNumeroResolucionCambioEstatuto() != null && igfn.getFechaResolucionCambioEstatuto() == null) {
            errores.add("17 fecha de resolución de cambio de estatuto: obligatoria si hay resolución (campo 16)");
        }
        String cambioNombre = igfn.getCambioNombre() != null ? igfn.getCambioNombre().trim().toUpperCase() : null;
        if (cambioNombre != null && !cambioNombre.isEmpty() && !cambioNombre.equals("SI") && !cambioNombre.equals("NO")) {
            errores.add("18 cambio de nombre: el manual (§4.1) solo admite SI o NO; IGFN trae otro texto (" + cambioNombre.length() + " caracteres)");
        }
        cg40.setCambioNombre(cambioNombre == null || cambioNombre.isEmpty() ? null : cambioNombre);

        cg40.setPorcentajeAportePatronalCesantia(numero52(igfn.getPorcentajeAportePatronalCesantia(), "19 % aporte patronal cesantía", errores));
        cg40.setPorcentajeAportePersonalCesantia(numero52(igfn.getPorcentajeAportePersonalCesantia(), "20 % aporte personal cesantía", errores));
        cg40.setPorcentajeAportePatronalJubilacion(numero52(igfn.getPorcentajeAportePatronalJubilacion(), "21 % aporte patronal jubilación", errores));
        cg40.setPorcentajeAportePersonalJubilacion(numero52(igfn.getPorcentajeAportePersonalJubilacion(), "22 % aporte personal jubilación", errores));
        cg40.setValorAportePersonalCesantia(numero52(igfn.getValorAportePersonalCesantia(), "23 valor aporte personal cesantía (aporte mínimo)", errores));
        cg40.setValorAportePersonalJubilacion(numero52(igfn.getValorAportePersonalJubilacion(), "24 valor aporte personal jubilación (aporte mínimo)", errores));

        if (!errores.isEmpty()) {
            throw new GeneracionGException("G40 no cumple el manual de catastros; corregir en Información General del Fondo (CRD.IGFN): "
                    + String.join("; ", errores));
        }
        cg40DaoService.save(cg40, null);
        System.out.println("Registro G40 insertado desde CRD.IGFN " + igfn.getCodigo());
        return 1L;
    }

    /** Tabla 320 — IGFN puede guardar el tipo como "III", "TIPO III" o "3". */
    static String codigoTipoFcpc(String valor) {
        if (valor == null) return null;
        String v = valor.trim().toUpperCase().replace("TIPO", "").trim();
        switch (v) {
            case "I":   return "1";
            case "II":  return "2";
            case "III": return "3";
            default:    return v;
        }
    }

    private static String codigo(String valor, Set<String> tabla, String campo, List<String> errores) {
        String v = valor != null ? valor.trim().toUpperCase() : null;
        if (v == null || v.isEmpty()) { errores.add(campo + ": obligatorio"); return null; }
        if (!tabla.contains(v)) errores.add(campo + ": '" + v + "' no está en la tabla " + tabla);
        return v;
    }

    private static String texto(String valor, int largo, boolean obligatorio, String campo, List<String> errores) {
        String v = valor != null ? valor.trim() : null;
        if (v == null || v.isEmpty()) {
            if (obligatorio) errores.add(campo + ": obligatorio");
            return null;
        }
        if (v.length() > largo) errores.add(campo + ": " + v.length() + " caracteres, el manual admite " + largo);
        return v;
    }

    private static Double numero52(Double valor, String campo, List<String> errores) {
        if (valor == null) { errores.add(campo + ": obligatorio"); return null; }
        if (valor < 0 || valor > 999.99) errores.add(campo + ": " + valor + " fuera de Numérico(5,2) (0 a 999,99)");
        return Math.round(valor * 100.0) / 100.0;
    }
}
