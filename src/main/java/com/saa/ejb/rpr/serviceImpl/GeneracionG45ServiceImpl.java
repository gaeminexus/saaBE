package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.ProvinciaService;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.ejb.rpr.dao.SujetoReportadoG45DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG45Service;
import com.saa.ejb.rpr.service.NuevoPrestamoG46Service;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Exter;
import com.saa.model.crd.Provincia;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoParticipeG45;
import com.saa.model.rpr.NuevoPrestamoG46;
import com.saa.model.rpr.SaldoCuentaG42;
import com.saa.model.rpr.SujetoReportadoG45;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG45ServiceImpl implements GeneracionG45Service {

    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private NuevoPrestamoG46Service        g46Service;
    @EJB private SaldoCuentaG42DaoService       g42DaoService;
    @EJB private EntidadService                 entidadService;
    @EJB private ExterService                   exterService;
    @EJB private ProvinciaService               provinciaService;
    @EJB private NuevoParticipeG45DaoService    cg45DaoService;
    @EJB private SujetoReportadoG45DaoService   sujetoReportadoDao;

    @EJB private com.saa.ejb.rpr.dao.CatalogoGeograficoDaoService geografia;

    /** D-035: tipo de partícipe de la G45, fijo (directriz de la Superintendencia, mayo 2025 presentado con P). */
    static final String TIPO_PARTICIPE = "P";

    /** Tabla 50: 50 = cabecera cantonal. Se usa mientras el catálogo de parroquias no tenga el código INEC (PRRQCDEX). */
    static final String PARROQUIA_CABECERA_CANTONAL = "50";

    /**
     * D-039 — provincia, cantón y parroquia (tablas 6, 7, 50) de un sujeto: primero provincia y cantón de EXTR (con la
     * G46 de mayo 2025 presentada coinciden 47 de 73; la dirección DRCC solo 39); si EXTR no da los dos, la dirección
     * vigente {@code ubicacionDrcc} = {provincia, cantón, parroquia}. Parroquia: la de la dirección (PRRQCDEX) cuando es
     * del mismo cantón; si no, o mientras ese código INEC no esté cargado, 50 (cabecera cantonal).
     */
    static String[] resolverUbicacion(String provinciaExter, String cantonExter, String[] ubicacionDrcc) {
        boolean drcc = ubicacionDrcc != null && ubicacionDrcc[0] != null && ubicacionDrcc[1] != null;
        String prov, canton;
        if (provinciaExter != null && cantonExter != null) {
            prov = provinciaExter; canton = cantonExter;
        } else if (drcc) {
            prov = ubicacionDrcc[0]; canton = ubicacionDrcc[1];
        } else {
            prov = provinciaExter; canton = cantonExter;
        }
        String parroquia = drcc && ubicacionDrcc[2] != null && prov != null && prov.equals(ubicacionDrcc[0])
                && canton != null && canton.equals(ubicacionDrcc[1]) ? ubicacionDrcc[2] : PARROQUIA_CABECERA_CANTONAL;
        return new String[] { prov, canton, parroquia };
    }

    private static String dosDigitos(String c) {
        if (c == null) return null;
        String v = c.trim();
        if (!v.matches("\\d{1,2}")) return null;
        v = v.length() == 1 ? "0" + v : v;
        return "00".equals(v) ? null : v;
    }

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G45");

        // D-035 (29-oct-2026) — "sujetos de riesgo" NUEVOS. La Superintendencia mantiene una base acumulada de
        // sujetos: el control de la G46 exige que el sujeto "conste en la base de datos de la estructura G45"
        // (Manual de Operaciones de Crédito FCPC v2.4, §5.2). La G45 del mes trae SOLO a los titulares de la
        // G46 del mes (y garantes/codeudores de la G50, cuando exista) que todavía no están en esa base; la
        // copia local de la base es RPR.SG45 (semilla + HM45 de mayo 2025 + lo que reporta cada ejecución).
        // Antes se copiaban todos los sujetos de la G46 (73 en mayo 2025, contra 10 presentados y aceptados).
        Long codigoEjrc = detalle.getEjecucionReporte().getCodigo();
        long periodo = detalle.getEjecucionReporte().getAnio() * 100L + detalle.getEjecucionReporte().getMes();

        // Regenerar un mes reemplaza lo que una ejecución anterior registró para ese mes (sin duplicar).
        int borrados = sujetoReportadoDao.deleteEjecucionDelPeriodo(periodo);
        System.out.println("G45 - periodo " + periodo + ": " + borrados + " sujetos de una ejecucion anterior quitados del registro SG45");
        Map<String, SujetoReportadoG45> registro = new HashMap<>();
        for (SujetoReportadoG45 s : sujetoReportadoDao.selectAll()) {
            if (s.getIdentificacion() != null) {
                registro.put(s.getIdentificacion().trim(), s);
            }
        }
        System.out.println("G45 - sujetos en el registro SG45: " + registro.size());

        // Candidatos: titulares de la G46 y garantes/codeudores de la G50 del mismo EJRC, sin repetir.
        Map<String, String> candidatos = new LinkedHashMap<>();
        DetalleEjecucionReporte ejrdG46 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G46");
        if (ejrdG46 != null) {
            List<NuevoPrestamoG46> registrosG46 = g46Service.selectByDetalle(ejrdG46.getCodigo());
            System.out.println("G45 - Registros de CG46: " + registrosG46.size());
            for (NuevoPrestamoG46 g46 : registrosG46) {
                if (g46.getIdentificacion() != null) {
                    candidatos.putIfAbsent(g46.getIdentificacion().trim(), g46.getTipoIdentificacion());
                }
            }
        }
        DetalleEjecucionReporte ejrdG50 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G50");
        if (ejrdG50 != null) {
            for (Object[] g : sujetoReportadoDao.selectGarantesG50(ejrdG50.getCodigo())) {
                candidatos.putIfAbsent(String.valueOf(g[1]).trim(), g[0] != null ? String.valueOf(g[0]) : "C");
            }
        }
        if (candidatos.isEmpty()) {
            System.out.println("G45 - G46/G50 sin sujetos. G45 vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Obtener el EJRD del G42 del mismo EJRC para calcular patrimonio
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG42 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G42");

        long contador = 0L;
        long yaReportados = 0L;

        // -------------------------------------------------------
        // 3. Por cada sujeto nuevo, un registro G45 con los datos fijos requeridos y patrimonio desde G42.
        // -------------------------------------------------------
        for (Map.Entry<String, String> candidato : candidatos.entrySet()) {
            String identificacion = candidato.getKey();
            SujetoReportadoG45 previo = registro.get(identificacion);
            if (previo != null && previo.getPeriodo() != null && previo.getPeriodo() < periodo) {
                yaReportados++;
                continue; // ya consta en la base de sujetos de la Superintendencia
            }

            // --- Entidad: necesaria para patrimonio y tipo de participe ---
            Entidad entidad = null;
            try {
                entidad = entidadService.selectByNumeroIdentificacion(identificacion);
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se pudo obtener entidad para: " + identificacion);
            }

            // --- Patrimonio: suma saldoAportePatronal + saldoAportePersonal + rendimiento desde G42 ---
            Double patrimonio = 0.0;
            if (ejrdG42 != null && entidad != null) {
                try {
                    SaldoCuentaG42 g42 = g42DaoService.selectByEntidadYDetalle(entidad.getCodigo(), ejrdG42);
                    if (g42 != null) {
                        double saldoPat = g42.getSaldoAportePatronal() != null ? g42.getSaldoAportePatronal() : 0.0;
                        double saldoPer = g42.getSaldoAportePersonal() != null ? g42.getSaldoAportePersonal() : 0.0;
                        double rendim   = g42.getRendimiento()         != null ? g42.getRendimiento()         : 0.0;
                        patrimonio = saldoPat + saldoPer + rendim;
                    }
                } catch (Throwable ex) {
                    System.out.println("G45 WARN - No se encontro G42 para entidad: " + entidad.getCodigo());
                }
            }

            // --- Tipo de partícipe: SIEMPRE "P" (D-035). El manual admite C cesante, J jubilado, P partícipe;
            // la G45 de mayo 2025 (aceptada) trae P en los 10 sujetos y el área financiera (correo del
            // 5-jun-2026) indica, por directriz de la Superintendencia, que el jubilado se reporta como P.
            // J y C no se usan. Antes: J si la entidad estaba en estado 30, C en los demás.
            String tipoParticipe = TIPO_PARTICIPE;

            // --- Exter: genero, estadoCivil, fechaNacimiento, provincia ---
            String genero      = null;
            String estadoCivil = null;
            java.time.LocalDate fechaNac = null;
            String codigoAlternoProvincia = null;
            String cantonExter = null;
            try {
                List<Exter> exters = exterService.selectByCedula(identificacion);
                if (exters != null && !exters.isEmpty()) {
                    Exter exter = exters.get(0);
                    if (exter.getGenero() != null && !exter.getGenero().isEmpty()) {
                        char primeraLetraGenero = Character.toUpperCase(exter.getGenero().charAt(0));
                        if (primeraLetraGenero == 'H') {
                            genero = "M";
                        } else if (primeraLetraGenero == 'M') {
                            genero = "F";
                        } else {
                            genero = String.valueOf(primeraLetraGenero);
                        }
                    }
                    // D-039: estado civil (tabla 211) desde EXTR, ya no "S" fijo.
                    estadoCivil = GeneracionG41ServiceImpl.codigoEstadoCivil(exter.getEstadoCivil());
                    if (exter.getFechaNacimiento() != null) {
                        fechaNac = exter.getFechaNacimiento().toLocalDate();
                    }
                    // --- Provincia: buscar en tabla PRVN por nombre ---
                    if (exter.getProvincia() != null && !exter.getProvincia().isEmpty()) {
                        try {
                            Provincia prov = provinciaService.selectByNombre(exter.getProvincia());
                            if (prov != null) {
                                codigoAlternoProvincia = prov.getCodigoAlterno();
                            }
                        } catch (Throwable ex) {
                            System.out.println("G45 WARN - No se encontro Provincia por nombre: " + exter.getProvincia());
                        }
                    }
                    cantonExter = geografia.codigoCanton(exter.getProvincia(), exter.getCanton());
                    if (patrimonio <= 0 && exter.getSalarioTotal() != null && exter.getSalarioTotal() > 0) {
                        // Manual §3.1 campo 5: "en caso de que el deudor no posea bienes, se tomará como
                        // referencia el valor de sus ingresos". Control §5.1: patrimonio > 0.
                        patrimonio = exter.getSalarioTotal();
                    }
                }
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se encontro Exter para: " + identificacion);
            }

            // D-039: residencia del sujeto (tablas 6, 7 y 50): provincia y cantón de EXTR; si EXTR no da los dos,
            // la dirección vigente DRCC → parroquia → cantón → provincia (ver resolverUbicacion).
            String[] ubicacion = entidad != null ? geografia.ubicacionEntidad(entidad.getCodigo()) : null;
            String[] residencia = resolverUbicacion(dosDigitos(codigoAlternoProvincia), cantonExter, ubicacion);
            String provinciaSujeto = residencia[0];
            String cantonSujeto    = residencia[1];
            String parroquiaSujeto = residencia[2];

            // --- Construir y guardar CG45 ---
            NuevoParticipeG45 g45 = new NuevoParticipeG45();
            g45.setIdentificacion(identificacion);
            g45.setTipoIdentificacion(candidato.getValue() != null ? candidato.getValue() : "C");
            g45.setGenero(genero);
            g45.setEstadoCivil(estadoCivil);
            g45.setFechaNacimiento(fechaNac);
            g45.setTipoParticipe(tipoParticipe);
            g45.setActividadEconomica("009");
            g45.setPatrimonio(patrimonio);
            g45.setProvincia(provinciaSujeto);
            g45.setCanton(cantonSujeto);
            g45.setParroquia(parroquiaSujeto);
            g45.setProfesion("O");
            g45.setCargasFamiliares(0L);
            g45.setOrigenIngresos("B");
            g45.setDetalleEjecucion(detalle);

            cg45DaoService.save(g45, null);
            registrar(previo, g45.getTipoIdentificacion(), identificacion, periodo, detalle.getCodigo());
            System.out.println("G45 INSERT participe: " + identificacion + " patrimonio=" + patrimonio);
            contador++;
        }

        System.out.println("G45 generado con " + contador + " registros (" + yaReportados
            + " sujetos de la G46/G50 ya constaban en el registro SG45)");
        return contador;
    }

    /**
     * Agrega el sujeto al registro SG45 con este período. Si ya estaba registrado con un período POSTERIOR
     * (se generó antes un mes siguiente), el registro pasa a este período: ese mes siguiente lo traía como nuevo
     * y hay que regenerarlo.
     */
    private void registrar(SujetoReportadoG45 previo, String tipo, String identificacion, long periodo, Long idEjrd)
            throws Throwable {
        if (previo != null) {
            if (previo.getPeriodo() != null && previo.getPeriodo() == periodo) {
                return; // mismo período (p. ej. HM45 de mayo 2025): ya registrado
            }
            System.out.println("G45 WARN - sujeto registrado en el periodo posterior " + previo.getPeriodo()
                + "; pasa a " + periodo + ". Regenerar la G45 de " + previo.getPeriodo());
            previo.setPeriodo(periodo);
            previo.setOrigen(SujetoReportadoG45.ORIGEN_EJECUCION);
            previo.setDetalleEjecucion(idEjrd);
            previo.setFechaRegistro(LocalDate.now());
            sujetoReportadoDao.actualizar(previo);
            return;
        }
        SujetoReportadoG45 nuevo = new SujetoReportadoG45();
        nuevo.setTipoIdentificacion(tipo);
        nuevo.setIdentificacion(identificacion);
        nuevo.setPeriodo(periodo);
        nuevo.setOrigen(SujetoReportadoG45.ORIGEN_EJECUCION);
        nuevo.setDetalleEjecucion(idEjrd);
        nuevo.setFechaRegistro(LocalDate.now());
        sujetoReportadoDao.insertar(nuevo);
    }
}