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
 *   idPago|identificacion|nombre|codigoBanco|tipoCuenta|numeroCuenta|valor|referencia
 *
 * Los datos del destino salen de la cuenta bancaria del titular (TSR.CTBN). Cuando el pago
 * no la tiene —porque el beneficiario no existe en el maestro de titulares de tesorería—
 * se usan los campos del BENEFICIARIO OCASIONAL denormalizados en el propio pago
 * (PGTRBF*). Solo si no hay ninguno de los dos se rechaza la generación del archivo.
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

			// El destino sale de la cuenta del titular; si el pago no la tiene (porque el
			// beneficiario no está en el maestro de titulares de tesorería) se cae a los
			// datos del BENEFICIARIO OCASIONAL denormalizados en el propio pago.
			boolean tieneCuentaTitular = (pago.getCuentaDestino() != null);
			boolean tieneBeneficiarioOcasional = (pago.getBeneficiarioCuenta() != null
					&& !pago.getBeneficiarioCuenta().trim().isEmpty());

			if (!tieneCuentaTitular && !tieneBeneficiarioOcasional) {
				throw new IncomeException("El pago " + pago.getId() + " (proveedor "
						+ nombreBeneficiario(pago) + ") no tiene cuenta bancaria de destino registrada. "
						+ "Registre la cuenta del proveedor antes de generar el archivo.");
			}

			String identificacion;
			String nombreBanco;
			String tipoCuenta;
			String numeroCuenta;

			if (tieneCuentaTitular) {
				identificacion = (pago.getTitular() != null)
						? nvl(pago.getTitular().getIdentificacion()) : "";
				nombreBanco = (pago.getCuentaDestino().getBanco() != null)
						? nvl(pago.getCuentaDestino().getBanco().getNombre()) : "";
				tipoCuenta   = nvlLong(pago.getCuentaDestino().getTipoCuenta());
				numeroCuenta = nvl(pago.getCuentaDestino().getNumeroCuenta());
			} else {
				identificacion = nvl(pago.getBeneficiarioIdentificacion());
				nombreBanco = (pago.getBeneficiarioBanco() != null)
						? nvl(pago.getBeneficiarioBanco().getNombre()) : "";
				tipoCuenta   = nvlLong(pago.getBeneficiarioTipoCuenta());
				numeroCuenta = nvl(pago.getBeneficiarioCuenta());
			}

			contenido.append(pago.getId()).append(SEPARADOR)
			         .append(identificacion).append(SEPARADOR)
			         .append(nombreBeneficiario(pago)).append(SEPARADOR)
			         .append(nombreBanco).append(SEPARADOR)
			         .append(tipoCuenta).append(SEPARADOR)
			         .append(numeroCuenta).append(SEPARADOR)
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

	/**
	 * Nombre a imprimir en el archivo: el del titular si el pago lo tiene, y si no
	 * el del beneficiario ocasional denormalizado en el propio pago.
	 * @param pago : Pago programado
	 * @return     : Nombre del beneficiario, o cadena vacía
	 */
	private String nombreBeneficiario(PagoProgramado pago) {
		if (pago.getTitular() != null) {
			return nvl(pago.getTitular().getNombre());
		}
		return nvl(pago.getBeneficiarioNombre());
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
