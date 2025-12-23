package com.saa.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/reportes")
public class ReportesResource {

    // =========================
    // 0) Datasource lookup (manual)
    // =========================
    private static final List<String> DS_CANDIDATES = List.of(
            "java:jboss/datasources/SaaDS",
            "java:/SaaDS",
            "java:/jdbc/SaaDS",
            "java:jboss/datasources/OracleDS",
            "java:/OracleDS",
            "java:/jdbc/OracleDS"
    );

    private DataSource lookupDs() throws NamingException {
        InitialContext ctx = new InitialContext();
        NamingException last = null;

        for (String jndi : DS_CANDIDATES) {
            try {
                Object obj = ctx.lookup(jndi);
                if (obj instanceof DataSource ds) {
                    return ds;
                }
            } catch (NamingException e) {
                last = e;
            }
        }
        throw (last != null) ? last : new NamingException("No DataSource found. Probados: " + DS_CANDIDATES);
    }

    // =========================
    // 1) Healthcheck
    // =========================
    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "pong";
    }

    // =========================
    // 2) Jasper test (sin BD) -> .jrxml simple / o usa tu test.jasper si quieres
    //    Aquí lo dejamos en jrxml porque ya te funcionaba, pero si quieres lo pasamos a jasper también.
    // =========================
    @GET
    @Path("/test")
    @Produces("application/pdf")
    public Response testPdf() {
        try (InputStream jrxml = getClass().getResourceAsStream("/reports/test/test.jrxml")) {

            if (jrxml == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No se encontró /reports/test.jrxml en resources")
                        .type("text/plain")
                        .build();
            }

            // Si este test te funcionaba, mantenlo. Si empieza a fallar también, lo convertimos a .jasper.
            // (Tu error actual es por compilar el JRXML real; el test era simple y no disparaba el mismo problema)
            net.sf.jasperreports.engine.JasperReport report =
                    net.sf.jasperreports.engine.JasperCompileManager.compileReport(jrxml);

            JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), new JREmptyDataSource());
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            return Response.ok(pdf)
                    .header("Content-Disposition", "inline; filename=\"test.pdf\"")
                    .build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("Error generando PDF test: " + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .type("text/plain")
                    .build();
        }
    }

    // =========================
    // 3) Probar Oracle (SELECT 1 FROM dual)
    // =========================
    @GET
    @Path("/test-db")
    @Produces("text/plain")
    public String testDb() {
        try {
            DataSource ds = lookupDs();

            try (Connection con = ds.getConnection();
                 PreparedStatement ps = con.prepareStatement("select 1 from dual");
                 ResultSet rs = ps.executeQuery()) {

                return rs.next() ? "Oracle OK: " + rs.getInt(1) : "Oracle NO DATA";
            }

        } catch (Exception e) {
            return "DB ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage()
                    + " | Probados: " + DS_CANDIDATES;
        }
    }

    // =========================
    // 4) Confirmar que el JRXML real existe (opcional)
    // =========================
    @GET
    @Path("/exists-naturaleza-jrxml")
    @Produces("text/plain")
    public String existsNaturalezaJrxml() {
        try (InputStream is = getClass().getResourceAsStream("/reports/test/naturaleza_cuenta.jrxml")) {
            return (is != null)
                    ? "OK: naturaleza_cuenta.jrxml encontrado"
                    : "NO: no está /reports/naturaleza_cuenta.jrxml";
        } catch (Exception e) {
            return "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    // =========================
    // 5) Confirmar que el JASPER compilado existe (ESTE ES EL IMPORTANTE)
    // =========================
    @GET
    @Path("/exists-naturaleza")
    @Produces("text/plain")
    public String existsNaturalezaJasper() {
        try (InputStream is = getClass().getResourceAsStream("/reports/test/naturaleza_cuenta.jasper")) {
            return (is != null)
                    ? "OK: naturaleza_cuenta.jasper encontrado"
                    : "NO: falta /reports/naturaleza_cuenta.jasper (compílalo en Studio y cópialo aquí)";
        } catch (Exception e) {
            return "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    // =========================
    // 6) Reporte REAL: Naturaleza de cuentas (usa .jasper precompilado)
    // =========================
    @GET
    @Path("/naturaleza-cuentas")
    @Produces("application/pdf")
    public Response naturalezaCuentasPdf(@QueryParam("pjrqcdgo") Long pjrqcdgo) {
        try (InputStream jasperStream = getClass().getResourceAsStream("/reports/test/naturaleza_cuenta.jasper")) {

            if (jasperStream == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No se encontró /reports/naturaleza_cuenta.jasper en resources. " +
                                "Compila el reporte en Jaspersoft Studio y copia el .jasper a src/main/resources/reports/")
                        .type("text/plain")
                        .build();
            }

            JasperReport report = (JasperReport) JRLoader.loadObject(jasperStream);

            Map<String, Object> params = new HashMap<>();
            // Por ahora tu query tiene PJRQCDGO fijo = 1236, así que este parámetro no se usa aún.
            // Cuando cambies el JRXML a $P{pjrqcdgo}, esto ya quedará listo:
            if (pjrqcdgo != null) {
                params.put("pjrqcdgo", pjrqcdgo);
            }

            DataSource ds = lookupDs();
            try (Connection con = ds.getConnection()) {
                JasperPrint print = JasperFillManager.fillReport(report, params, con);
                byte[] pdf = JasperExportManager.exportReportToPdf(print);

                return Response.ok(pdf)
                        .header("Content-Disposition", "inline; filename=\"naturaleza_cuentas.pdf\"")
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity("Error generando reporte naturaleza (jasper): "
                            + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .type("text/plain")
                    .build();
        }
    }
}
