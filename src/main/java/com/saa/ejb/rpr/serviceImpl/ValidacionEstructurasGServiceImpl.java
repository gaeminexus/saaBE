package com.saa.ejb.rpr.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.saa.ejb.rpr.service.ValidacionEstructurasGService;
import com.saa.ejb.rpr.service.dto.ObservacionValidacionG;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C39 (23-nov-2026) — ver {@link ValidacionEstructurasGService}. Cada regla es una condición SQL sobre la tabla CGxx del
 * detalle (EJRD) de la ejecución: cuenta los registros que no la cumplen y toma hasta 10 ejemplos (número de operación o
 * código de entidad, nunca la cédula).
 *
 * <p>Origen de cada regla: G41 (rechazos del 23-nov: género, estado civil, fecha de nacimiento y de ingreso nulos; manual
 * de catastros §4.2: la fecha de actualización solo puede ir en blanco en las altas «I»); G45 (manual de crédito §5.1:
 * campos obligatorios, patrimonio &gt; 0); G46 (§5.2: «Control de G45», valores positivos, vencimiento posterior a la
 * concesión, ubicación con códigos de las tablas 6, 7 y 50); G47 (§5.3: la operación anterior debe estar en la G49);
 * G48 (las operaciones deben venir de la G46 o de la G48 anterior); G49 (cancela operaciones que estaban en la G48
 * anterior); G51 (cada alta de la G46 lleva su garantía). Fechas «de relleno»: anteriores a 1901 (el SAA guarda
 * 01/01/1800 y 01/01/1900 cuando falta el dato).</p>
 */
@Stateless
public class ValidacionEstructurasGServiceImpl implements ValidacionEstructurasGService {

    @PersistenceContext
    EntityManager em;

    private static final String RELLENO = " DATE '1901-01-01' ";

    @Override
    @SuppressWarnings("unchecked")
    public List<ObservacionValidacionG> validar(Long codigoEjrc) throws Throwable {
        List<ObservacionValidacionG> obs = new ArrayList<>();
        Object[] cab = (Object[]) em.createNativeQuery(
                "select EJRCANOO, EJRCMESS from RPR.EJRC where EJRCCDGO = :e").setParameter("e", codigoEjrc).getSingleResult();
        long anio = ((Number) cab[0]).longValue(), mes = ((Number) cab[1]).longValue();
        long periodo = anio * 100 + mes;
        long periodoAnt = mes == 1 ? (anio - 1) * 100 + 12 : periodo - 1;

        Map<String, Long> d = detalles(codigoEjrc);
        Long ejrcAnt = ultimaEjecucion(periodoAnt);
        Map<String, Long> dAnt = ejrcAnt != null ? detalles(ejrcAnt) : new HashMap<>();

        // ---- G41 ----
        regla(obs, d, "G41", "CG41", ObservacionValidacionG.ERROR,
                "Género, estado civil, fecha de nacimiento o fecha de ingreso vacíos (datos del partícipe en CRD.EXTR / CRD.PRTC)",
                "(x.CG41GNPR is null or x.CG41ECDP is null or x.CG41FNDP is null or x.CG41FIDP is null)", ENTIDAD_41);
        regla(obs, d, "G41", "CG41", ObservacionValidacionG.ERROR,
                "Fecha de nacimiento o de ingreso de relleno (antes de 1901) o futura",
                "(x.CG41FNDP < " + RELLENO + " or x.CG41FIDP < " + RELLENO + " or x.CG41FNDP > sysdate or x.CG41FIDP > sysdate)", ENTIDAD_41);
        regla(obs, d, "G41", "CG41", ObservacionValidacionG.ERROR,
                "Fecha de actualización del estado vacía en un registro que no es alta (I)",
                "(nvl(x.CG41ESRG, 'I') <> 'I' and x.CG41FADE is null)", ENTIDAD_41);
        // ---- G42, G43, G44 ----
        regla(obs, d, "G42", "CG42", ObservacionValidacionG.ERROR, "Tipo de prestación vacío",
                "x.CG42TPPR is null", "(select min(e.ENTDCDGO) from CRD.ENTD e where e.ENTDNMID = x.CG42IDPR)");
        regla(obs, d, "G43", "CG43", ObservacionValidacionG.ERROR, "Fecha de término de la relación laboral o de liquidación vacía",
                "(x.CG43FTRL is null or x.CG43FCLQ is null)", "(select min(e.ENTDCDGO) from CRD.ENTD e where e.ENTDNMID = x.CG43IDPR)");
        regla(obs, d, "G44", "CG44", ObservacionValidacionG.ERROR, "Tipo o fecha de jubilación vacíos",
                "(x.CG44TPJB is null or x.CG44FCJB is null)", "(select min(e.ENTDCDGO) from CRD.ENTD e where e.ENTDNMID = x.CG44IDJB)");
        // ---- G45 ----
        regla(obs, d, "G45", "CG45", ObservacionValidacionG.ERROR,
                "Género, estado civil, fecha de nacimiento, provincia, cantón o parroquia del sujeto vacíos",
                "(x.CG45GNRO is null or x.CG45ESCV is null or x.CG45FDNC is null or x.CG45PRVN is null or x.CG45CNTN is null or x.CG45PRRQ is null)",
                ENTIDAD_45);
        regla(obs, d, "G45", "CG45", ObservacionValidacionG.ERROR, "Fecha de nacimiento de relleno (antes de 1901)",
                "x.CG45FDNC < " + RELLENO, ENTIDAD_45);
        regla(obs, d, "G45", "CG45", ObservacionValidacionG.ERROR, "Patrimonio del sujeto igual o menor a cero",
                "nvl(x.CG45PTSJ, 0) <= 0", ENTIDAD_45);
        // ---- G46 ----
        regla(obs, d, "G46", "CG46", ObservacionValidacionG.ERROR, "Provincia, cantón o parroquia de destino vacíos",
                "(x.CG46DGPR is null or x.CG46DGCN is null or x.CG46DGPS is null)", "x.CG46NMOP");
        regla(obs, d, "G46", "CG46", ObservacionValidacionG.ERROR, "Valor de la operación o tasa iguales o menores a cero",
                "(nvl(x.CG46VDLO, 0) <= 0 or nvl(x.CG46TDIN, 0) <= 0)", "x.CG46NMOP");
        regla(obs, d, "G46", "CG46", ObservacionValidacionG.ERROR, "Fecha de vencimiento vacía o no posterior a la concesión",
                "(x.CG46FCVN is null or x.CG46FCCN is null or x.CG46FCVN <= x.CG46FCCN)", "x.CG46NMOP");
        if (d.containsKey("G46")) {
            String enG45 = d.containsKey("G45")
                    ? " exists (select 1 from RPR.CG45 g where g.CG45EJRD = " + d.get("G45") + " and g.CG45IDSJ = x.CG46IDSJ) or " : "";
            regla(obs, d, "G46", "CG46", ObservacionValidacionG.ERROR,
                    "Sujeto que no consta en la G45 (ni en esta ni en una G45 presentada antes): la Super la rechaza con «Identificación no reportada en G45»",
                    "not (" + enG45 + " exists (select 1 from RPR.SG45 s where s.SG45IDSJ = x.CG46IDSJ and (s.SG45ORGN in ('HM45', 'CONFIRMADO')"
                            + " or (s.SG45ORGN in ('EJECUCION', 'PENDIENTE') and s.SG45PRDO < " + periodo + "))))",
                    "x.CG46NMOP");
        }
        // ---- G47 ----
        if (d.containsKey("G47")) {
            String enG49 = d.containsKey("G49") ? "exists (select 1 from RPR.CG49 c where c.CG49EJRD = " + d.get("G49") + " and c.CG49NMOP = x.CG47NDOA)" : "1 = 0";
            regla(obs, d, "G47", "CG47", ObservacionValidacionG.ADVERTENCIA,
                    "Operación anterior que no está en la G49 de este mes (debe constar en la base de la G49)",
                    "not " + enG49, "x.CG47NMOP");
        }
        // ---- G48 ----
        if (d.containsKey("G48")) {
            String enG46 = d.containsKey("G46") ? "exists (select 1 from RPR.CG46 n where n.CG46EJRD = " + d.get("G46") + " and n.CG46NMOP = x.CG48NMOP)" : "1 = 0";
            String enAnt = dAnt.containsKey("G48") ? "exists (select 1 from RPR.CG48 a where a.CG48EJRD = " + dAnt.get("G48") + " and a.CG48NMOP = x.CG48NMOP)" : null;
            if (enAnt != null) {
                regla(obs, d, "G48", "CG48", ObservacionValidacionG.ERROR,
                        "Operación que no viene de la G48 del mes anterior ni de la G46 de este mes",
                        "not (" + enG46 + " or " + enAnt + ")", "x.CG48NMOP");
                long actual = contar("select count(*) from RPR.CG48 x where x.CG48EJRD = " + d.get("G48"));
                long anterior = contar("select count(*) from RPR.CG48 x where x.CG48EJRD = " + dAnt.get("G48"));
                long altas = d.containsKey("G46") ? contar("select count(*) from RPR.CG46 x where x.CG46EJRD = " + d.get("G46")) : 0;
                long bajas = d.containsKey("G49") ? contar("select count(*) from RPR.CG49 x where x.CG49EJRD = " + d.get("G49")) : 0;
                if (actual != anterior + altas - bajas) {
                    obs.add(new ObservacionValidacionG("G48", ObservacionValidacionG.ADVERTENCIA,
                            "Operaciones de la G48 (" + actual + ") distintas de las del mes anterior (" + anterior + ") + altas G46 ("
                                    + altas + ") − cancelaciones G49 (" + bajas + ")", Math.abs(actual - (anterior + altas - bajas)), null));
                }
            }
        }
        // ---- G49 ----
        if (d.containsKey("G49") && dAnt.containsKey("G48")) {
            regla(obs, d, "G49", "CG49", ObservacionValidacionG.ERROR, "Cancelación de una operación que no estaba en la G48 del mes anterior",
                    "not exists (select 1 from RPR.CG48 a where a.CG48EJRD = " + dAnt.get("G48") + " and a.CG48NMOP = x.CG49NMOP)",
                    "x.CG49NMOP");
        }
        // ---- G51 ----
        if (d.containsKey("G46")) {
            String enG51 = d.containsKey("G51") ? "exists (select 1 from RPR.CG51 w where w.CG51EJRD = " + d.get("G51") + " and w.CG51NMOP = x.CG46NMOP)" : "1 = 0";
            regla(obs, d, "G46", "CG46", ObservacionValidacionG.ERROR, "Alta de la G46 sin su garantía en la G51",
                    "not " + enG51, "x.CG46NMOP");
        }
        // ---- conteo de cada detalle frente a sus filas (la cabecera del archivo sale de EJRDCNRG) ----
        for (Map.Entry<String, Long> e : d.entrySet()) {
            String tabla = "C" + e.getKey();
            Long declarado = ((Number) em.createNativeQuery("select nvl(EJRDCNRG, 0) from RPR.EJRD where EJRDCDGO = :d")
                    .setParameter("d", e.getValue()).getSingleResult()).longValue();
            long filas = contar("select count(*) from RPR." + tabla + " x where x." + tabla + "EJRD = " + e.getValue());
            if (declarado != filas) {
                obs.add(new ObservacionValidacionG(e.getKey(), ObservacionValidacionG.ERROR,
                        "Registros declarados en la ejecución (" + declarado + ") distintos de las filas generadas (" + filas + ")",
                        Math.abs(declarado - filas), null));
            }
        }
        return obs;
    }

    @Override
    public String resumen(List<ObservacionValidacionG> observaciones) {
        List<ObservacionValidacionG> errores = observaciones.stream()
                .filter(o -> ObservacionValidacionG.ERROR.equals(o.getNivel())).collect(Collectors.toList());
        if (errores.isEmpty()) {
            long adv = observaciones.size();
            return adv == 0 ? "validación previa sin errores" : "validación previa sin errores, " + adv + " advertencia(s)";
        }
        Map<String, Long> porG = new LinkedHashMap<>();
        for (ObservacionValidacionG o : errores) porG.merge(o.getEstructura(), o.getCantidad() != null ? o.getCantidad() : 1L, Long::sum);
        return "VALIDACIÓN PREVIA: errores que la Super rechazaría en " + porG.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "))
                + "; detalle en GET /ejrc/validar/{codigo}";
    }

    // ------------------------------------------------------------------------------------------------------------

    private static final String ENTIDAD_41 = "(select min(e.ENTDCDGO) from CRD.ENTD e where e.ENTDNMID = x.CG41IDPR)";
    private static final String ENTIDAD_45 = "(select min(e.ENTDCDGO) from CRD.ENTD e where e.ENTDNMID = x.CG45IDSJ)";

    @SuppressWarnings("unchecked")
    private Map<String, Long> detalles(Long codigoEjrc) {
        Map<String, Long> m = new HashMap<>();
        List<Object[]> filas = em.createNativeQuery("select EJRDTPRP, EJRDCDGO from RPR.EJRD where EJRCCDGO = :e")
                .setParameter("e", codigoEjrc).getResultList();
        for (Object[] f : filas) m.put(String.valueOf(f[0]).trim(), ((Number) f[1]).longValue());
        return m;
    }

    private Long ultimaEjecucion(long periodo) {
        Object r = em.createNativeQuery("select max(EJRCCDGO) from RPR.EJRC where EJRCANOO * 100 + EJRCMESS = :p and EJRCESTD = 3")
                .setParameter("p", periodo).getSingleResult();
        return r != null ? ((Number) r).longValue() : null;
    }

    private long contar(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private void regla(List<ObservacionValidacionG> obs, Map<String, Long> d, String estructura, String tabla, String nivel,
            String regla, String condicion, String ejemplo) {
        Long det = d.get(estructura);
        if (det == null) return;
        String desde = " from RPR." + tabla + " x where x." + tabla + "EJRD = " + det + " and " + condicion;
        long n = contar("select count(*)" + desde);
        if (n == 0) return;
        Query q = em.createNativeQuery("select distinct to_char(" + ejemplo + ")" + desde + " fetch first 10 rows only");
        List<Object> ej = q.getResultList();
        obs.add(new ObservacionValidacionG(estructura, nivel, regla, n,
                ej.stream().map(o -> o == null ? "?" : o.toString()).collect(Collectors.joining(", "))));
    }
}
