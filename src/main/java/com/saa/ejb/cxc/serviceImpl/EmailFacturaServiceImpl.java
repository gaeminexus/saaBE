package com.saa.ejb.cxc.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.saa.ejb.cxc.service.EmailFacturaService;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

/**
 * Implementación del servicio de envío de correo para facturas electrónicas autorizadas.
 * Usa la sesión de correo configurada en WildFly (standalone.xml):
 *   JNDI: java:jboss/mail/Facturacion
 *   Servidor: smtp.office365.com:587 con STARTTLS
 *   Cuenta: facturacion@asoprep.com.ec
 */
@Stateless
public class EmailFacturaServiceImpl implements EmailFacturaService {

    private static final String FROM_ADDRESS    = "facturacion@asoprep.com.ec";
    private static final String FROM_NAME       = "Facturación Electrónica";

    /** Sesión de correo configurada en standalone.xml */
    @Resource(lookup = "java:jboss/mail/Facturacion")
    private Session mailSession;

    @Override
    public void enviarFacturaAutorizada(String destinatario, String numeroFactura, String clave,
            String razonSocialEmisor, String xmlAutorizado, byte[] pdfBytes) throws Exception {

        if (destinatario == null || destinatario.trim().isEmpty()) {
            System.out.println("⚠ Email no enviado: destinatario vacío para factura " + numeroFactura);
            return;
        }

        System.out.println(">>> Enviando email de factura autorizada a: " + destinatario
                + " | Factura: " + numeroFactura);

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(FROM_ADDRESS, FROM_NAME, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        message.setSubject("Factura Electrónica Autorizada N° " + numeroFactura, "UTF-8");
        message.setSentDate(new Date());

        // Cuerpo HTML
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(construirCuerpoHTML(numeroFactura, clave, razonSocialEmisor),
                "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);

        // Adjunto XML autorizado
        if (xmlAutorizado != null && !xmlAutorizado.isEmpty()) {
            MimeBodyPart xmlPart = new MimeBodyPart();
            byte[] xmlBytes = xmlAutorizado.getBytes(StandardCharsets.UTF_8);
            DataSource xmlDs = new ByteArrayDataSource(xmlBytes, "application/xml");
            xmlPart.setDataHandler(new DataHandler(xmlDs));
            xmlPart.setFileName(clave + ".xml");
            multipart.addBodyPart(xmlPart);
        }

        // Adjunto PDF RIDE (opcional)
        if (pdfBytes != null && pdfBytes.length > 0) {
            MimeBodyPart pdfPart = new MimeBodyPart();
            DataSource pdfDs = new ByteArrayDataSource(pdfBytes, "application/pdf");
            pdfPart.setDataHandler(new DataHandler(pdfDs));
            pdfPart.setFileName(clave + ".pdf");
            multipart.addBodyPart(pdfPart);
        }

        message.setContent(multipart);

        Transport.send(message);
        System.out.println("✓ Email enviado correctamente a: " + destinatario
                + " | Factura: " + numeroFactura);
    }

    private String construirCuerpoHTML(String numeroFactura, String clave, String razonSocialEmisor) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'></head>" +
            "<body style='font-family:Arial,sans-serif;color:#333'>" +
            "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                "border:1px solid #ddd;border-radius:8px'>" +
            "<h2 style='color:#2e7d32'>Factura Electrónica Autorizada</h2>" +
            "<p>Estimado cliente,</p>" +
            "<p>Le informamos que su factura electrónica ha sido " +
                "<strong>autorizada</strong> por el SRI.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:20px 0'>" +
            "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold'>Emisor:</td>" +
            "    <td style='padding:8px'>" + razonSocialEmisor + "</td></tr>" +
            "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold'>Número:</td>" +
            "    <td style='padding:8px'>" + numeroFactura + "</td></tr>" +
            "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold'>" +
                "Clave de Acceso:</td>" +
            "    <td style='padding:8px;font-size:12px;word-break:break-all'>" +
                clave + "</td></tr>" +
            "</table>" +
            "<p>Adjunto encontrará el archivo <strong>XML autorizado</strong>" +
            " y el <strong>RIDE</strong> (Representación Impresa del Documento " +
            "Electrónico) en PDF.</p>" +
            "<hr style='border:none;border-top:1px solid #eee;margin:20px 0'/>" +
            "<p style='font-size:11px;color:#aaa'>Este es un mensaje automático " +
            "generado por el sistema de facturación electrónica de " + razonSocialEmisor +
            ". Por favor no responda a este correo.</p>" +
            "</div></body></html>";
    }
}