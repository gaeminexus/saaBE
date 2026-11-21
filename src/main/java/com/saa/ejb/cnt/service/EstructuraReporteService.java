package com.saa.ejb.cnt.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;
import com.saa.ejb.cnt.estructura.SolicitudEstructura;

import jakarta.ejb.Local;

/**
 * Estructuras de renglones (C19): reportes cuyo renglon es un codigo propio (B17, flujo de caja...) alimentado
 * por cuentas del plan segun reglas parametrizadas en CNT.RNRP / CNT.CRRP. Ver
 * docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md.
 */
@Local
public interface EstructuraReporteService {

	/** Calcula la estructura y, si la definicion tiene formato de archivo, arma el contenido y el nombre. */
	ResultadoEstructura generar(SolicitudEstructura solicitud) throws Throwable;

	/** true si el reporte (empresa, alterno) es una estructura de renglones (RPRTTPRP = 2). Nunca lanza. */
	boolean esEstructura(Long empresa, Long codigoAlterno);

	/**
	 * Debe y haber por cuenta del plan, asientos activos y reversados (los mismos estados que el balance), con
	 * fecha en [desde, hasta]. desde null = desde el inicio de la contabilidad.
	 */
	List<SaldoCuenta> saldosPorCuenta(Long empresa, LocalDate desde, LocalDate hasta) throws Throwable;

	/** Cabecera activa de la estructura por su codigo (RPRTCDES: B17, FLE, FCD, CMP); lanza si no existe (C38). */
	DefinicionEstructura definicionEstructura(Long empresa, String codigoEstructura) throws Throwable;

	/** Renglones vigentes a la fecha de corte (CNT.RNRP), en su orden (C38). */
	List<RenglonEstructura> renglonesVigentes(Long codigoReporte, LocalDate corte);

	/** Reglas cuenta -> renglon vigentes a la fecha de corte (CNT.CRRP) (C38). */
	List<ReglaCuentaRenglon> reglasVigentes(Long codigoReporte, LocalDate corte);
}
