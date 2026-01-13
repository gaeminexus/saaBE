package com.saa.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
                if (obj instanceof DataSource ds) return ds;
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
     * REPORTE: Naturaleza de cuentas (con parámetro empresa)
     *
     * Espera el recurso:
     *   src/main/resources/rep/test/naturaleza_cuenta_empresa.jasper
     *
     * URL:
     *   GET /SaaBE/api/reportes/naturaleza-cuentas-empresa?empresa=1236
     */
    @GET
    @Path("/naturaleza-cuentas-empresa")
    @Produces("application/pdf")
    public Response naturalezaCuentasEmpresaPdf(@QueryParam("empresa") Long empresa) {

        if (empresa == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Falta parámetro requerido: empresa")
                    .type("text/plain")
                    .build();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pr_empresa", empresa); // ✅ debe coincidir con JRXML

        // ✅ si el reporte usa logo por parámetro
        InputStream logo = getClass().getResourceAsStream("/rep/_common/images/logo_empresa.png");
        if (logo != null) {
            params.put("pr_logo", logo);
        }

        try (InputStream jasper = getClass().getResourceAsStream("/rep/test/naturaleza_cuenta_empresa.jasper")) {

            if (jasper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No se encontró /rep/test/naturaleza_cuenta_empresa.jasper en resources")
                        .type("text/plain")
                        .build();
            }

            DataSource ds = lookupDs();
            try (Connection con = ds.getConnection()) {

                JasperPrint print = JasperFillManager.fillReport(jasper, params, con);

                // 🔎 Debug útil: cuántas páginas generó realmente
                if (print.getPages() == null || print.getPages().isEmpty()) {
                    return Response.serverError()
                            .entity("El reporte se llenó pero generó 0 páginas. Revisa query/param/WhenNoDataType.")
                            .type("text/plain")
                            .build();
                }

                byte[] pdf = JasperExportManager.exportReportToPdf(print);

                return Response.ok(pdf)
                        .header("Content-Disposition", "inline; filename=\"naturaleza_cuentas_empresa.pdf\"")
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity("ERROR Jasper: " + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .type("text/plain")
                    .build();
        }
    }


    /**
     * Debug: confirma que el .jasper está dentro del WAR en la ruta correcta
     */
    @GET
    @Path("/debug-rep")
    @Produces("text/plain")
    public String debugRep() {
        try (InputStream jasper = getClass().getResourceAsStream("/rep/test/naturaleza_cuenta_empresa.jasper")) {
            return (jasper != null)
                    ? "OK: encontrado /rep/test/naturaleza_cuenta_empresa.jasper"
                    : "FAIL: no existe /rep/test/naturaleza_cuenta_empresa.jasper";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
