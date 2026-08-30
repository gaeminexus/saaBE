package com.saa.rubros;

/**
 * @author GaemiSoft
 * <p>Interfaz del rubro TipoPartidaTransito (239) - tipo de partida en tránsito de la
 * conciliación bancaria (<code>TSR.DTCN.DTCNTPOO</code>).</p>
 *
 * <p>Las cuatro esquinas clásicas de la conciliación bancaria - ver
 * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §2:</p>
 * <ul>
 * <li>{@link #DEPOSITO_EN_TRANSITO} y {@link #CHEQUE_GIRADO_NO_COBRADO}: están en libros
 * (<code>TSR.MVCB</code>), todavía no en el banco.</li>
 * <li>{@link #NC_BANCO_NO_REGISTRADA} y {@link #ND_BANCO_NO_REGISTRADA}: están en el extracto
 * bancario (<code>TSR.DEXB</code>), todavía no registradas en libros.</li>
 * </ul>
 */
public interface TipoPartidaTransito {

    /** 1 - Depósito en tránsito: está en libros (MVCB), no en el banco. */
    int DEPOSITO_EN_TRANSITO = 1;

    /** 2 - Cheque girado y no cobrado: está en libros (MVCB), no en el banco. */
    int CHEQUE_GIRADO_NO_COBRADO = 2;

    /** 3 - Nota de crédito del banco no registrada en libros: está en el extracto (DEXB). */
    int NC_BANCO_NO_REGISTRADA = 3;

    /** 4 - Nota de débito del banco no registrada en libros (comisiones): está en el extracto (DEXB). */
    int ND_BANCO_NO_REGISTRADA = 4;

}
