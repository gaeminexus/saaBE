package com.saa.ejb.crd.service;

import java.io.InputStream;

import com.saa.ejb.crd.service.dto.ResultadoCargaSumaAsegurada;

import jakarta.ejb.Local;

/**
 * Suma asegurada del bien hipotecado/prendado ({@code CRD.PRST.PRSTVLAS}), base del seguro de
 * incendio y prendario. {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md} §2.
 */
@Local
public interface SumaAseguradaService {

    /** 400 - El valor es ≤ 0. */
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    /** 409 - El tipo de préstamo no es hipotecario (2) ni prendario (3). */
    String ERR_NO_ES_HIPOTECARIO_NI_PRENDARIO = "NO_ES_HIPOTECARIO_NI_PRENDARIO";
    /** 404 - El préstamo no existe. */
    String ERR_PRESTAMO_NO_ENCONTRADO = "PRESTAMO_NO_ENCONTRADO";
    /** 400 - El archivo no es un .xlsx/.xls legible, o no tiene las columnas esperadas. */
    String ERR_ARCHIVO_INVALIDO = "ARCHIVO_INVALIDO";
    /** 400 - Parámetro obligatorio faltante. */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";

    /**
     * Captura manual — {@code PUT /posg/sumaAsegurada}. Graba {@code PRSTVLAS}.
     *
     * @param idPrestamo código del préstamo
     * @param valor      suma asegurada, {@code > 0}
     * @param usuario    quién lo hace
     * @return el valor anterior (puede ser null si no tenía)
     * @throws Throwable {@link #ERR_VALOR_INVALIDO}, {@link #ERR_NO_ES_HIPOTECARIO_NI_PRENDARIO},
     *                    {@link #ERR_PRESTAMO_NO_ENCONTRADO}
     */
    Double actualizar(Long idPrestamo, Double valor, String usuario) throws Throwable;

    /**
     * Carga por Excel — {@code POST /posg/sumaAsegurada/carga} (multipart, el backend lee el
     * archivo con Apache POI, {@code PrestamoServiceImpl.cargarTablaAmortizacionDesdeExcel} como
     * precedente). Columnas {@code IDAsoprep} y {@code suma asegurada}, con encabezado.
     *
     * <p>{@code confirmar = false}: solo valida, no graba nada.
     * {@code confirmar = true}: graba SOLO las filas OK, en una transacción.</p>
     *
     * @param archivo   el libro .xlsx/.xls
     * @param confirmar si graba o solo valida
     * @param usuario   quién lo hace
     * @return el resultado fila por fila
     * @throws Throwable {@link #ERR_ARCHIVO_INVALIDO} si el archivo no se puede leer
     */
    ResultadoCargaSumaAsegurada cargarDesdeExcel(InputStream archivo, boolean confirmar, String usuario)
            throws Throwable;
}
