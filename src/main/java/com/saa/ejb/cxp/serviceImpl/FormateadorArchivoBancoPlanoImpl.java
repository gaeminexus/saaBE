package com.saa.ejb.cxp.serviceImpl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.FormateadorArchivoBanco;
import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.PagoProgramado;

/**
 * Implementación PROVISIONAL del archivo de transferencias.
 *
 * ⚠ PENDIENTE: el formato oficial del banco todavía no fue entregado. Este
 * formateador genera un texto plano con los campos que cualquier archivo de
 * transferencias necesita, separados por pipe, únicamente para poder probar el
 * flujo completo de punta a punta. NO USAR EN PRODUCCIÓN.
 *
 * Cuando llegue la especificación real basta con escribir otra implementación de
 * {@link FormateadorArchivoBanco} y devolverla desde
 * {@code PagoProgramadoServiceImpl.obtenerFormateador(...)}: el resto del flujo
 * de pagos no cambia.
 *
 * Estructura provisional (una línea por pago):
 *   idPago|rucProveedor|nombreProveedor|codigoBanco|tipoCuenta|numeroCuenta|valor|referencia
 */
public class FormateadorArchivoBancoPlanoImpl implements FormateadorArchivoBanco {

	private static final String SEPARADOR = "|";

	@Override
	public String generarContenido(LotePago lote, List<PagoProgramado> pagos) throws Throwable {

		System.out.println("=== FormateadorArchivoBancoPlanoImpl | lote=" + lote.getId()
				+ " | pagos=" + (pagos != null ? pagos.size() : 0) + " ===");

		if (pagos == null || pagos.isEmpty()) {
			throw new IncomeException("El lote no tiene pagos que incluir en el archivo.");
		}

		StringBuilder contenido = new StringBuilder();

		for (PagoProgramado pago : pagos) {

			if (pago.getCuentaDestino() == null) {
				throw new IncomeException("El pago " + pago.getId() + " (proveedor "
						+ nombreTitular(pago) + ") no tiene cuenta bancaria de destino registrada. "
						+ "Registre la cuenta del proveedor antes de generar el archivo.");
			}

			String rucProveedor = (pago.getTitular() != null)
					? nvl(pago.getTitular().getIdentificacion()) : "";
			String codigoBanco = (pago.getCuentaDestino().getBanco() != null)
					? nvl(pago.getCuentaDestino().getBanco().getNombre()) : "";

			contenido.append(pago.getId()).append(SEPARADOR)
			         .append(rucProveedor).append(SEPARADOR)
			         .append(nombreTitular(pago)).append(SEPARADOR)
			         .append(codigoBanco).append(SEPARADOR)
			         .append(nvlLong(pago.getCuentaDestino().getTipoCuenta())).append(SEPARADOR)
			         .append(nvl(pago.getCuentaDestino().getNumeroCuenta())).append(SEPARADOR)
			         .append(String.format(Locale.US, "%.2f", nvlDouble(pago.getValor()))).append(SEPARADOR)
			         .append(nvl(pago.getObservacion()))
			         .append("\r\n");
		}

		return contenido.toString();
	}

	@Override
	public String nombreArchivo(LotePago lote) {
		String fecha = (lote.getFechaGeneracion() != null)
				? lote.getFechaGeneracion().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
				: "";
		return "PAGOS_LOTE_" + lote.getId() + "_" + fecha + ".txt";
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private String nombreTitular(PagoProgramado pago) {
		return (pago.getTitular() != null) ? nvl(pago.getTitular().getNombre()) : "";
	}

	private String nvl(String valor) {
		return (valor != null) ? valor.trim() : "";
	}

	private String nvlLong(Long valor) {
		return (valor != null) ? String.valueOf(valor) : "";
	}

	private double nvlDouble(Double valor) {
		return (valor != null) ? valor : 0.0;
	}
}
