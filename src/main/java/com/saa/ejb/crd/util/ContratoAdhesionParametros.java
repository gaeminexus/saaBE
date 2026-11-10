package com.saa.ejb.crd.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.model.crd.Contrato;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Participe;
import com.saa.rubros.Estado;
import com.saa.rubros.ModalidadLaboral;
import com.saa.rubros.SexoParticipe;

/**
 * Parámetros del reporte RPRT_CNTR_ADHS (contrato de adhesión + Anexo I) a partir de las entidades ya cargadas. Clase pura
 * (sin EJB ni base): el servicio de firma carga contrato, partícipe y direcciones y se las pasa. Toda la lógica de «qué
 * se imprime» vive aquí; el .jrxml solo coloca textos. Dato faltante = cadena vacía (nunca «null» ni puntos suspensivos).
 *
 * <p>Reglas (DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md §9):</p>
 * <ul>
 *   <li>Opción de la cláusula CUARTA según los DATOS del contrato (CRD.TPCN no distingue jubilación/cesantía): hay
 *       jubilación si {@code CNTRPRAJ} o {@code CNTRMNAJ} son &gt; 0; cesantía si {@code CNTRPRAI} o {@code CNTRMNAC} son &gt; 0.</li>
 *   <li>Mínimos fijos del modelo del cliente: ver {@link #MIN_JUBILACION}, {@link #MIN_CESANTIA_SOLA},
 *       {@link #MIN_CESANTIA_CON_JUBILACION}.</li>
 *   <li>Sexo (ENTDSEXO) y modalidad laboral (PRTCMDLB): casilla marcada solo si el dato existe; si no, vacía.</li>
 *   <li>Barrio solo desde DRCCBRRO (sin respaldo en la referencia); título desde PRTCTTPR con respaldo en nivelEstudios.</li>
 * </ul>
 */
public final class ContratoAdhesionParametros {

    /** Mínimo de jubilación del modelo (opciones 1 y 3). */
    public static final double MIN_JUBILACION = 5.0;
    /** Mínimo de cesantía cuando es la única prestación (opción 2). */
    public static final double MIN_CESANTIA_SOLA = 5.0;
    /** Mínimo de cesantía cuando va junto a la jubilación (opción 3). */
    public static final double MIN_CESANTIA_CON_JUBILACION = 2.0;

    private static final Locale ES = new Locale("es", "EC");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MESES = { "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
        "septiembre", "octubre", "noviembre", "diciembre" };

    private ContratoAdhesionParametros() {
    }

    /**
     * @param contrato  contrato (con su entidad cargada)
     * @param participe datos laborales del partícipe, o null
     * @param direcciones direcciones de la entidad (se elige la domiciliaria), o null
     * @param ciudad    ciudad de la firma; null o vacío = «Quito»
     * @param hoy       día de generación del PDF
     */
    public static Map<String, Object> armar(Contrato contrato, Participe participe, List<Direccion> direcciones, String ciudad,
            LocalDate hoy) {
        Map<String, Object> p = new HashMap<>();
        Entidad e = contrato.getEntidad();

        // --- Comparecientes
        p.put("P_NOMBRE", texto(e != null ? e.getRazonSocial() : null));
        p.put("P_CEDULA", texto(e != null ? e.getNumeroIdentificacion() : null));
        String empresa = e != null && e.getTipoHidrocarburifica() != null && e.getTipoHidrocarburifica().getCodigoEntidad() != null
            ? texto(e.getTipoHidrocarburifica().getCodigoEntidad().getRazonSocial()) : "";
        p.put("P_EMPRESA", empresa);
        String rol = e != null && e.getRolPetroComercial() != null ? String.valueOf(e.getRolPetroComercial()) : "";
        p.put("P_ROL", rol);

        // --- Cláusula CUARTA
        boolean jub = positivo(contrato.getPorcentajeAporteJubilacion()) || positivo(contrato.getMontoAporteJubilacion());
        boolean ces = positivo(contrato.getPorcentajeAporteIndividual()) || positivo(contrato.getMontoAporteCesantia());
        int opcion = jub && !ces ? 1 : (ces && !jub ? 2 : 3);
        double minCes = opcion == 2 ? MIN_CESANTIA_SOLA : MIN_CESANTIA_CON_JUBILACION;
        p.put("P_OPCION", Integer.valueOf(opcion));
        p.put("P_JUB_MINIMO", porcentaje(MIN_JUBILACION));
        p.put("P_CES_MINIMO", porcentaje(minCes));
        p.put("P_JUB_ADICIONAL", adicional(contrato.getPorcentajeAporteJubilacion(), MIN_JUBILACION));
        p.put("P_CES_ADICIONAL", adicional(contrato.getPorcentajeAporteIndividual(), minCes));

        // --- Cláusula DÉCIMO SEXTA
        LocalDate dia = hoy != null ? hoy : LocalDate.now();
        p.put("P_CIUDAD", ciudad != null && !ciudad.trim().isEmpty() ? ciudad.trim() : "Quito");
        p.put("P_DIA", String.valueOf(dia.getDayOfMonth()));
        p.put("P_MES", MESES[dia.getMonthValue() - 1]);
        p.put("P_ANIO", String.valueOf(dia.getYear()));
        p.put("P_FECHA_FIRMA", FECHA.format(dia));

        // --- Anexo I: datos personales
        String ec = e != null && e.getEstadoCivil() != null ? normalizar(e.getEstadoCivil().getNombre()) : "";
        p.put("P_EC_SOLTERO", marca(ec.contains("SOLTER")));
        p.put("P_EC_CASADO", marca(ec.contains("CASAD")));
        p.put("P_EC_DIVORCIADO", marca(ec.contains("DIVORC")));
        p.put("P_EC_VIUDO", marca(ec.contains("VIUD")));
        p.put("P_EC_UNION", marca(ec.contains("UNION") || ec.contains("LIBRE")));
        String viv = e != null && e.getTipoVivienda() != null ? normalizar(e.getTipoVivienda().getNombre()) : "";
        p.put("P_VIV_PROPIA", marca(viv.contains("PROPI")));
        p.put("P_VIV_ALQUILADA", marca(viv.contains("ALQUIL") || viv.contains("ARRIEND")));
        p.put("P_VIV_FAMILIAR", marca(viv.contains("FAMILIAR")));
        String sexo = e != null && e.getSexo() != null ? e.getSexo().trim().toUpperCase() : "";
        p.put("P_SEXO_M", marca(SexoParticipe.MASCULINO.equals(sexo)));
        p.put("P_SEXO_F", marca(SexoParticipe.FEMENINO.equals(sexo)));
        Long modalidad = participe != null ? participe.getModalidadLaboral() : null;
        p.put("P_MOD_NOMBRAMIENTO", marca(modalidad != null && modalidad == ModalidadLaboral.NOMBRAMIENTO));
        p.put("P_MOD_CONTRATO", marca(modalidad != null && modalidad == ModalidadLaboral.CONTRATO));

        String[] nac = fechaNacimiento(e != null ? e.getFechaNacimiento() : null);
        p.put("P_NAC_DIA", nac[0]);
        p.put("P_NAC_MES", nac[1]);
        p.put("P_NAC_ANIO", nac[2]);
        p.put("P_CARGAS", e != null && e.getCargasFamiliares() != null ? String.valueOf(e.getCargasFamiliares()) : "");
        p.put("P_TEL_CASA", texto(e != null ? e.getTelefono() : null));
        p.put("P_CELULAR", texto(e != null ? e.getMovil() : null));
        p.put("P_CORREO_PERSONAL", texto(e != null ? e.getCorreoPersonal() : null));
        p.put("P_CORREO_INSTITUCIONAL", texto(e != null ? e.getCorreoInstitucional() : null));

        // --- Anexo I: dirección domiciliaria (la de trabajo no)
        Direccion d = direccionDomiciliaria(direcciones);
        String parroquia = "", canton = "", provincia = "", barrio = "", calles = "";
        if (d != null) {
            if (d.getParroquia() != null) {
                parroquia = texto(d.getParroquia().getNombre());
                if (d.getParroquia().getCiudad() != null) {
                    // En el modelo la parroquia cuelga de CRD.Ciudad (no de Cantón): la ciudad hace de cantón.
                    canton = texto(d.getParroquia().getCiudad().getNombre());
                    if (d.getParroquia().getCiudad().getProvincia() != null) {
                        provincia = texto(d.getParroquia().getCiudad().getProvincia().getNombre());
                    }
                }
            }
            barrio = texto(d.getBarrio());
            StringBuilder c = new StringBuilder(texto(d.getCallePrincipal()));
            if (!texto(d.getCalleSecundaria()).isEmpty()) {
                c.append(c.length() > 0 ? " y " : "").append(texto(d.getCalleSecundaria()));
            }
            if (!texto(d.getNumero()).isEmpty()) {
                c.append(c.length() > 0 ? " " : "").append("N° ").append(texto(d.getNumero()));
            }
            calles = c.toString();
        }
        p.put("P_PROVINCIA", provincia);
        p.put("P_CANTON", canton);
        p.put("P_PARROQUIA", parroquia);
        p.put("P_BARRIO", barrio);
        p.put("P_CALLES", calles);

        // --- Anexo I: datos profesionales
        p.put("P_REMUNERACION", participe != null && participe.getRemuneracionUnificada() != null
            ? "$" + String.format(ES, "%,.2f", participe.getRemuneracionUnificada()) : "");
        p.put("P_FECHA_INGRESO", participe != null ? fecha(participe.getFechaIngresoTrabajo()) : "");
        p.put("P_LUGAR_TRABAJO", texto(participe != null ? participe.getLugarTrabajo() : null));
        p.put("P_UNIDAD", texto(participe != null ? participe.getUnidadAdministrativa() : null));
        p.put("P_CARGO", texto(participe != null ? participe.getCargoActual() : null));
        String titulo = participe != null ? texto(participe.getTituloProfesional()) : "";
        if (titulo.isEmpty() && participe != null) {
            titulo = texto(participe.getNivelEstudios()); // respaldo: el nivel de estudios
        }
        p.put("P_TITULO", titulo);
        p.put("P_ING_MONTO", participe != null && participe.getIngresoAdicionalMensual() != null
            && participe.getIngresoAdicionalMensual() > 0 ? "$" + String.format(ES, "%,d", participe.getIngresoAdicionalMensual()) : "");
        p.put("P_ING_ACTIVIDAD", texto(participe != null ? participe.getIngresoAdicionalActividad() : null));
        return p;
    }

    /**
     * REGLA DEL ADICIONAL (confirmada por el usuario el 2026-11-10): el % registrado en el contrato es el TOTAL, así que
     * adicional = % del contrato − mínimo del modelo; en blanco si el % es null o el resultado es ≤ 0.
     * Es el ÚNICO lugar donde está la regla.
     */
    static String adicional(Double porcentajeContrato, double minimo) {
        if (porcentajeContrato == null) {
            return "";
        }
        double extra = porcentajeContrato - minimo;
        return extra > 0.0000001 ? porcentaje(extra) : "";
    }

    /** «5%», «2,5%». */
    static String porcentaje(double v) {
        String s = Math.abs(v - Math.rint(v)) < 0.0000001 ? String.valueOf((long) Math.rint(v))
            : String.format(ES, "%.2f", v).replaceAll("0+$", "").replaceAll("[,.]$", "");
        return s + "%";
    }

    private static boolean positivo(Double v) {
        return v != null && v > 0;
    }

    private static String marca(boolean cond) {
        return cond ? "X" : "";
    }

    private static String texto(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(ES).trim();
    }

    private static String fecha(LocalDateTime f) {
        return f == null ? "" : FECHA.format(f);
    }

    /** ENTD.ENTDFCNC es un texto libre: se aceptan yyyy-MM-dd (con o sin hora) y dd/MM/yyyy; si no se entiende, en blanco. */
    static String[] fechaNacimiento(String texto) {
        String[] vacio = { "", "", "" };
        if (texto == null || texto.trim().length() < 8) {
            return vacio;
        }
        String t = texto.trim();
        try {
            LocalDate d;
            if (t.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                d = LocalDate.parse(t.substring(0, 10));
            } else if (t.matches("^\\d{1,2}/\\d{1,2}/\\d{4}.*")) {
                String[] x = t.substring(0, t.indexOf(' ') > 0 ? t.indexOf(' ') : t.length()).split("/");
                d = LocalDate.of(Integer.parseInt(x[2]), Integer.parseInt(x[1]), Integer.parseInt(x[0]));
            } else {
                return vacio;
            }
            return new String[] { String.valueOf(d.getDayOfMonth()), String.valueOf(d.getMonthValue()), String.valueOf(d.getYear()) };
        } catch (RuntimeException ex) {
            return vacio;
        }
    }

    /** Domiciliaria: activa y no de trabajo; la marcada por defecto si hay, si no la primera. */
    private static Direccion direccionDomiciliaria(List<Direccion> direcciones) {
        Direccion primera = null;
        if (direcciones != null) {
            for (Direccion d : direcciones) {
                boolean trabajo = d.getTrabajo() != null && d.getTrabajo() == 1L;
                boolean activa = d.getEstado() == null || d.getEstado() == Estado.ACTIVO;
                if (trabajo || !activa) {
                    continue;
                }
                if (d.getPorDefecto() != null && d.getPorDefecto() == 1L) {
                    return d;
                }
                if (primera == null) {
                    primera = d;
                }
            }
        }
        return primera;
    }
}
