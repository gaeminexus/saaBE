package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Forma de pago explícita de PGS.PGTR (PGTRFPAG). Mismo catálogo que
 *         PGS.APLP.APLPFPAG y el enum FormaPagoAplicacion del frontend.
 */
public interface FormaPagoProgramado {

	long EFECTIVO = 1L;
	long TRANSFERENCIA = 2L;
	long CHEQUE = 3L;
	long DEBITO_AUTOMATICO = 4L;

}
