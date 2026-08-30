package com.saa.ejb.sri.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Reporte de apoyo al cuadre de los formularios 104 (IVA) y 103 (retenciones en la fuente) —
 * Fase 6 de docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md. <b>No genera los formularios
 * del SRI</b> (esos se llenan en el portal): calcula los totales que el sistema puede derivar
 * de sus propios datos, para que contabilidad los contraste contra lo que el SRI le prellena.
 *
 * <p>Cobertura parcial, a propósito. El propio levantamiento (§0, §2.1) advierte que el
 * instructivo del 103 está desactualizado y que el mapeo código de retención → casilla debe
 * confirmarse contra una declaración real antes de programarlo — así que este servicio agrupa
 * por el {@code codRetencion} tal como se registra hoy en el sistema, no lo traduce a una
 * casilla salvo en los pocos casos donde el código coincide literalmente con el número de
 * casilla del §2.1 sin ambigüedad. Las casillas del 104 que dependen de datos que el sistema no
 * rastrea hoy (609 retenciones de IVA recibidas, exterior, banano, etc.) no se calculan — se
 * listan como no disponibles en vez de asumir cero.</p>
 */
@Local
public interface ReporteCuadreSriService {

    /**
     * Casillas del formulario 104 (IVA) derivables de los datos del sistema para un período.
     *
     * @param idFacturador	: Id del facturador (define la empresa contable, igual que el ATS)
     * @param anio			: Año del período
     * @param mes			: Mes del período, 1-12
     * @return				: Mapa con {@code casillas} (lista casilla/concepto/valor),
     *						  {@code noDisponibles} (casillas que no se pudieron calcular, con el
     *						  motivo) y {@code avisos}
     * @throws Throwable	: IncomeException si el facturador no existe o el período es inválido
     */
    Map<String, Object> calcularCuadre104(Long idFacturador, int anio, int mes) throws Throwable;

    /**
     * Totales de retenciones en la fuente (renta) del período, agrupados por
     * {@code codRetencion} tal como se registran hoy — ver el javadoc de la interfaz sobre por
     * qué no se traducen a número de casilla del 103 salvo los casos sin ambigüedad.
     *
     * @param idFacturador	: Id del facturador
     * @param anio			: Año del período
     * @param mes			: Mes del período, 1-12
     * @return				: Mapa con {@code porCodigo} (lista código/casillaSugerida/base/valor)
     *						  y {@code avisos}
     * @throws Throwable	: IncomeException si el facturador no existe o el período es inválido
     */
    Map<String, Object> calcularCuadre103(Long idFacturador, int anio, int mes) throws Throwable;

}
