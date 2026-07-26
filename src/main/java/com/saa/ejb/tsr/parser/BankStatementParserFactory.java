/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.saa.model.tsr.Banco;
import com.saa.model.tsr.CuentaBancaria;

/**
 * @author GaemiSoft
 * <p>Resuelve que {@link BankStatementParser} usar segun el banco de la
 * cuenta bancaria elegida por el usuario - nunca por el contenido del
 * archivo ni por eleccion manual del usuario (ver decision de diseño en
 * docs/PLAN-PANTALLAS-EXTRACTOS-BANCARIOS-2026-07-25.md).</p>
 * <p>La coincidencia es por palabra clave contenida en el nombre del banco
 * (normalizado a mayusculas sin tildes), no por igualdad exacta, para
 * tolerar variaciones como "Banco Internacional" vs "BANCO INTERNACIONAL
 * S.A.". Si un banco nuevo no tiene parser registrado, falla explicitamente
 * en vez de adivinar.</p>
 * <p>Cada resolucion crea una instancia NUEVA del parser (nunca se cachean
 * instancias compartidas): algunos parsers (ej. JepStatementParser) guardan
 * estado de instancia mientras procesan un archivo, y compartir una
 * instancia entre llamados concurrentes corrompería ese estado.</p>
 */
public final class BankStatementParserFactory {

    private static final Map<String, Supplier<BankStatementParser>> PARSERS_POR_PALABRA_CLAVE = crearMapa();

    private BankStatementParserFactory() {
    }

    private static Map<String, Supplier<BankStatementParser>> crearMapa() {
        Map<String, Supplier<BankStatementParser>> mapa = new LinkedHashMap<>();
        mapa.put("INTERNACIONAL", InternacionalStatementParser::new);
        mapa.put("PACIFICO", PacificoStatementParser::new);
        mapa.put("ATLANTIDA", AtlantidaStatementParser::new);
        mapa.put("AUSTRO", AustroStatementParser::new);
        mapa.put("GUAYAQUIL", GuayaquilStatementParser::new);
        mapa.put("MANABI", ManabiStatementParser::new);
        mapa.put("POLICIA", PoliciaNacionalStatementParser::new);
        mapa.put("ALIANZA", AlianzaStatementParser::new);
        mapa.put("JEP", JepStatementParser::new);
        mapa.put("PICHINCHA", PichinchaStatementParser::new);
        mapa.put("AMAZONAS", AmazonasStatementParser::new);
        return mapa;
    }

    /**
     * Resuelve el parser a usar para la cuenta bancaria indicada.
     * @param cuenta : Cuenta bancaria (con su Banco ya cargado)
     * @return        : Nueva instancia del parser correspondiente
     * @throws IllegalArgumentException : Si no hay parser registrado para el banco
     */
    public static BankStatementParser resolver(CuentaBancaria cuenta) {
        Banco banco = cuenta.getBanco();
        if (banco == null || banco.getNombre() == null || banco.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                "La cuenta bancaria " + cuenta.getCodigo() + " no tiene un banco asociado con nombre valido");
        }
        String nombreNormalizado = normalizar(banco.getNombre());
        for (Map.Entry<String, Supplier<BankStatementParser>> entrada : PARSERS_POR_PALABRA_CLAVE.entrySet()) {
            if (nombreNormalizado.contains(entrada.getKey())) {
                return entrada.getValue().get();
            }
        }
        throw new IllegalArgumentException(
            "No hay parser implementado para el banco '" + banco.getNombre() + "'. "
                + "Bancos soportados: " + PARSERS_POR_PALABRA_CLAVE.keySet());
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().trim();
    }
}
