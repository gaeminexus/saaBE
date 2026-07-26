/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import java.io.InputStream;

import com.saa.model.tsr.CuentaBancaria;

/**
 * @author GaemiSoft
 * <p>Estrategia de parseo de un estado de cuenta bancario (Excel/legado) hacia
 * el formato normalizado de DetalleExtractoBancario. Una implementacion por
 * banco/cooperativa - la seleccion de cual usar se hace por
 * {@link BankStatementParserFactory} en base al banco de la cuenta elegida
 * por el usuario, nunca adivinando por el contenido del archivo.</p>
 */
public interface BankStatementParser {

    /**
     * Parsea el archivo del estado de cuenta.
     * @param archivo : Contenido del archivo (Excel legado o XLSX)
     * @param cuenta  : Cuenta bancaria a la que pertenece este extracto
     * @return        : Resultado normalizado, sin persistir
     * @throws Throwable : Excepcion
     */
    ParsedStatement parse(InputStream archivo, CuentaBancaria cuenta) throws Throwable;

}
