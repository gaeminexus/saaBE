package com.saa.ejb.crd.util;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionFirma;
import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionesFirma;

import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * Dónde firma cada uno en el PDF del contrato de adhesión (RPRT_CNTR_ADHS), en UN solo lugar.
 *
 * <p>El bloque de firmas del contrato fluye después del texto, así que su página y su altura cambian con los datos del
 * partícipe (el largo de la cláusula CUARTA, por ejemplo). Por eso NO se fijan: se llena el reporte con los mismos
 * parámetros y se localiza el rótulo de cada firma en el resultado (la página real y su posición real). El Anexo I va en
 * una página propia con diseño fijo, pero se localiza igual.</p>
 *
 * <p>Marcadores (dependen del .jrxml): el contrato firma en el bloque cuyo texto empieza con «PARTICIPE: » (a la izquierda,
 * en la misma fila, el Gerente), con espacio libre de ~70 pt encima de los nombres; el Anexo firma sobre el rótulo
 * «Firma PARTICIPE». Si el .jrxml cambia esos rótulos, se ajustan aquí.</p>
 *
 * <p>Las posiciones son la esquina INFERIOR IZQUIERDA del recuadro de firma, en puntos PDF con origen abajo a la
 * izquierda y página desde 1 (verificar con el «Simulador de posiciones» del proveedor).</p>
 */
public final class PosicionesFirmaContrato {

    // ---- Constantes de posición (puntos PDF) ----
    /** Ancho del recuadro de firma. */
    static final double ANCHO_FIRMA = 190;
    /** Alto del recuadro sobre el nombre en el bloque del contrato (el espacio libre es de ~70). */
    static final double ALTO_FIRMA_CONTRATO = 60;
    /** Alto del recuadro sobre el rótulo «Firma PARTICIPE» del Anexo I (el espacio libre es de ~44). */
    static final double ALTO_FIRMA_ANEXO = 40;
    /** Separación entre el borde inferior del recuadro y el rótulo que tiene debajo. */
    static final double SEPARACION = 3;

    // ---- Marcadores en el .jrxml ----
    static final String MARCA_PARTICIPE_CONTRATO = "PARTICIPE: ";
    static final String MARCA_PARTICIPE_ANEXO = "Firma";

    private PosicionesFirmaContrato() {
    }

    /**
     * Llena el reporte con una COPIA de los parámetros (el reporte real se genera aparte) y ubica las firmas.
     *
     * @throws IllegalStateException si no se encuentra algún rótulo (el .jrxml cambió o el reporte no se generó completo)
     */
    public static PosicionesFirma calcular(String rutaJasper, Map<String, Object> parametros) throws Exception {
        JasperReport reporte;
        try (InputStream in = PosicionesFirmaContrato.class.getResourceAsStream(rutaJasper)) {
            if (in == null) {
                throw new IllegalStateException("no se encontró el reporte compilado " + rutaJasper);
            }
            reporte = (JasperReport) JRLoader.loadObject(in);
        }
        JasperPrint print = JasperFillManager.fillReport(reporte, new HashMap<String, Object>(parametros),
            new JRBeanCollectionDataSource(Collections.singletonList(new HashMap<String, Object>())));
        return ubicar(print);
    }

    static PosicionesFirma ubicar(JasperPrint print) {
        double altoPagina = print.getPageHeight();
        PosicionesFirma posiciones = new PosicionesFirma();

        JRPrintText participeContrato = null;
        int paginaContrato = 0;
        JRPrintText participeAnexo = null;
        int paginaAnexo = 0;

        List<JRPrintPage> paginas = print.getPages();
        for (int i = 0; i < paginas.size(); i++) {
            for (JRPrintElement e : paginas.get(i).getElements()) {
                if (!(e instanceof JRPrintText)) {
                    continue;
                }
                JRPrintText t = (JRPrintText) e;
                String texto = t.getFullText();
                if (texto == null) {
                    continue;
                }
                if (participeContrato == null && texto.startsWith(MARCA_PARTICIPE_CONTRATO)) {
                    participeContrato = t;
                    paginaContrato = i + 1;
                } else if (texto.trim().startsWith(MARCA_PARTICIPE_ANEXO) && texto.contains("PARTICIPE")) {
                    participeAnexo = t;
                    paginaAnexo = i + 1;
                }
            }
        }
        if (participeContrato == null) {
            throw new IllegalStateException("no se encontró el bloque «" + MARCA_PARTICIPE_CONTRATO + "…» de firmas del contrato");
        }
        if (participeAnexo == null) {
            throw new IllegalStateException("no se encontró el rótulo «Firma PARTICIPE» del Anexo I");
        }

        // Contrato: el recuadro va encima del nombre; el Gerente está en la misma fila, a la izquierda (mismo borde que el texto).
        double yContrato = altoPagina - (participeContrato.getY() - SEPARACION);
        double xGerente = print.getPageWidth() > 0 ? margenIzquierdo(print) : 0;
        posiciones.getParticipe().add(new PosicionFirma(paginaContrato, participeContrato.getX(), yContrato,
            ANCHO_FIRMA, ALTO_FIRMA_CONTRATO));
        posiciones.getRepresentante().add(new PosicionFirma(paginaContrato, xGerente, yContrato,
            ANCHO_FIRMA, ALTO_FIRMA_CONTRATO));

        // Anexo I: sobre el rótulo «Firma PARTICIPE».
        posiciones.getParticipe().add(new PosicionFirma(paginaAnexo, participeAnexo.getX(),
            altoPagina - (participeAnexo.getY() - SEPARACION), ANCHO_FIRMA, ALTO_FIRMA_ANEXO));
        return posiciones;
    }

    /** Borde izquierdo del contenido: la x del bloque del Gerente (el menor x de los textos del bloque, = margen izquierdo). */
    private static double margenIzquierdo(JasperPrint print) {
        double menor = Double.MAX_VALUE;
        for (JRPrintPage p : print.getPages()) {
            for (JRPrintElement e : p.getElements()) {
                menor = Math.min(menor, e.getX());
            }
        }
        return menor == Double.MAX_VALUE ? 0 : menor;
    }
}
