package com.saa.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/reportes")
public class ReportesResource {

    // Candidatos típicos en WildFly
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
        throw (last != null) ? last : new NamingException("No DataSource found");
    }

    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "pong";
    }

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

    /**
     * REPORTE REAL: Naturaleza de cuentas
     * Usa el .jasper precompilado para evitar compilar JRXML en WildFly.
     *
     * Recurso esperado:
     * src/main/resources/reports/test/naturaleza_cuenta.jasper
     */
    @GET
    @Path("/naturaleza-cuentas")
    @Produces("application/pdf")
    public Response naturalezaCuentasPdf() {

        // OJO: aquí NO hay parámetros obligatorios (como pediste).
        Map<String, Object> params = new HashMap<>();

        // Si tu .jasper trae el query con WHERE fijo (PJRQCDGO = 1236), ok.
        // Si luego lo pasas a parámetro, aquí lo pones: params.put("PJRQCDGO", 1236);

        try (InputStream jasper = getClass().getResourceAsStream("/reports/test/naturaleza_cuenta.jasper")) {

            if (jasper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No se encontró /reports/test/naturaleza_cuenta.jasper en resources")
                        .type("text/plain")
                        .build();
            }

            DataSource ds = lookupDs();

            try (Connection con = ds.getConnection()) {
                JasperPrint print = JasperFillManager.fillReport(jasper, params, con);
                byte[] pdf = JasperExportManager.exportReportToPdf(print);

                return Response.ok(pdf)
                        .header("Content-Disposition", "inline; filename=\"naturaleza_cuentas.pdf\"")
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity("ERROR generando PDF (jasper): " + e.getClass().getSimpleName()
                            + " - " + e.getMessage())
                    .type("text/plain")
                    .build();
        }
    }

    // Endpoint de ayuda para confirmar que el recurso está dentro del WAR
    @GET
    @Path("/debug-report")
    @Produces("text/plain")
    public String debugReport() {
        try (InputStream jasper = getClass().getResourceAsStream("/reports/test/naturaleza_cuenta.jasper")) {
            return (jasper != null)
                    ? "OK: encontrado /reports/test/naturaleza_cuenta.jasper"
                    : "FAIL: no existe /reports/test/naturaleza_cuenta.jasper";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
