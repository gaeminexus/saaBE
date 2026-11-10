package com.saa.ejb.crd.firma;

import java.net.URI;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Enmascara lo que la pantalla de pruebas de Security Data NUNCA debe mostrar (diseño de la pantalla §3): contraseñas, token
 * (12 caracteres + «…»), código dactilar, enlace de firma del partícipe (solo el dominio + «…») y todo base64 largo
 * (se reemplaza por «[PDF base64, N bytes]»; el archivo va aparte). Devuelve una COPIA: el árbol original no se modifica.
 */
final class SecurityDataEnmascarador {

    static final int BASE64_MINIMO = 200;
    private static final Pattern BASE64 = Pattern.compile("[A-Za-z0-9+/=\\r\\n]+");

    private SecurityDataEnmascarador() {
    }

    static JsonNode enmascarar(JsonNode nodo) {
        return copiar(null, nodo);
    }

    private static JsonNode copiar(String clave, JsonNode nodo) {
        if (nodo == null || nodo.isNull()) {
            return nodo;
        }
        if (nodo.isObject()) {
            ObjectNode salida = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> it = nodo.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                salida.set(e.getKey(), copiar(e.getKey(), e.getValue()));
            }
            return salida;
        }
        if (nodo.isArray()) {
            ArrayNode salida = JsonNodeFactory.instance.arrayNode();
            for (JsonNode h : nodo) {
                salida.add(copiar(clave, h));
            }
            return salida;
        }
        if (!nodo.isTextual()) {
            return nodo;
        }
        String texto = nodo.asText();
        String k = clave == null ? "" : clave.toLowerCase();
        if (k.equals("password") || k.equals("passsignature") || k.equals("signaturekey") || k.equals("fingerprintcode")) {
            return JsonNodeFactory.instance.textNode("******");
        }
        if (k.equals("signertoken")) {
            return JsonNodeFactory.instance.textNode(dominio(texto));
        }
        if (k.equals("token") || esJwt(texto)) {
            return JsonNodeFactory.instance.textNode(token(texto));
        }
        if (esBase64Largo(texto)) {
            return JsonNodeFactory.instance.textNode("[PDF base64, " + bytes(texto) + " bytes]");
        }
        return nodo;
    }

    /** Primeros 12 caracteres + «…». */
    static String token(String t) {
        return t.length() <= 12 ? "…" : t.substring(0, 12) + "…";
    }

    /** Solo el dominio del enlace + «…» (el resto es la llave del partícipe). */
    static String dominio(String enlace) {
        try {
            URI u = URI.create(enlace.trim());
            if (u.getHost() != null) {
                return u.getScheme() + "://" + u.getHost() + "/…";
            }
        } catch (RuntimeException e) {
            // se cae al valor genérico
        }
        return "…";
    }

    static boolean esJwt(String t) {
        return t.startsWith("eyJ") && t.indexOf('.') > 0 && t.length() > 40;
    }

    static boolean esBase64Largo(String t) {
        return t.length() > BASE64_MINIMO && BASE64.matcher(t).matches();
    }

    /** Tamaño aproximado en bytes del contenido de un base64. */
    static long bytes(String base64) {
        try {
            return Base64.getMimeDecoder().decode(base64).length;
        } catch (IllegalArgumentException e) {
            return (long) base64.length() * 3 / 4;
        }
    }
}
