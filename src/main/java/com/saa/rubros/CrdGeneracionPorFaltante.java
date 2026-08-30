package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CrdGeneracionPorFaltante (242), bandera del camino nuevo de
 *         GeneracionArchivoPetroServiceImpl.recopilarAportes (Fase 4 del plan de devengo de
 *         aportes): cobra el faltante mes a mes contra CRD.VGCN en vez de un monto fijo o
 *         una multiplicacion de meses. Se entrega APAGADA por defecto.
 */
public interface CrdGeneracionPorFaltante {

	int GENERACION_POR_FALTANTE_ACTIVA = 1;

}
