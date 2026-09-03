package com.saa.ejb.asoprep.serviceImpl;

import java.util.List;

import com.saa.ejb.asoprep.service.RegistradorNovedadCargaPetroService;
import com.saa.model.crd.NovedadParticipeCarga;
import com.saa.model.crd.ParticipeXCargaArchivo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Ver el javadoc de {@link RegistradorNovedadCargaPetroService} — el porqué de este bean
 * aparte, y por qué es idempotente, están ahí.
 *
 * {@code @Stateless} a propósito (no {@code @Stateful}): no necesita conversación, y evitar
 * cualquier duda sobre reentrancia/loopback con el {@code @Stateful} que lo invoca.
 */
@Stateless
public class RegistradorNovedadCargaPetroServiceImpl implements RegistradorNovedadCargaPetroService {

	@EJB
	private com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService novedadParticipeCargaDaoService;

	@EJB
	private com.saa.ejb.crd.dao.PrestamoDaoService prestamoDaoService;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void registrarEnTransaccionPropia(ParticipeXCargaArchivo participe, int tipoNovedad, String descripcion,
			Long codigoProducto, Long codigoPrestamo, Double montoEsperado, Double montoRecibido) {
		if (participe == null || participe.getCodigo() == null) {
			return;
		}

		try {
			NovedadParticipeCarga novedad = buscarExistente(participe.getCodigo(), tipoNovedad);
			boolean esNueva = novedad == null;
			if (esNueva) {
				novedad = new NovedadParticipeCarga();
				ParticipeXCargaArchivo participeRef = new ParticipeXCargaArchivo();
				participeRef.setCodigo(participe.getCodigo());
				novedad.setParticipeXCargaArchivo(participeRef);
				novedad.setEstado(1L);
			}

			novedad.setTipoNovedad(Long.valueOf(tipoNovedad));
			novedad.setDescripcion(descripcion);
			novedad.setCodigoProducto(codigoProducto);
			novedad.setCodigoPrestamo(codigoPrestamo);
			novedad.setMontoEsperado(montoEsperado);
			novedad.setMontoRecibido(montoRecibido);
			// Diferencia CON SIGNO, igual que el registrarNovedad original: negativa = falta
			// dinero, positiva = sobra dinero.
			novedad.setMontoDiferencia(montoEsperado != null && montoRecibido != null
				? montoRecibido - montoEsperado : null);

			if (participe.getDetalleCargaArchivo() != null
					&& participe.getDetalleCargaArchivo().getCargaArchivo() != null) {
				novedad.setCodigoCargaArchivo(participe.getDetalleCargaArchivo().getCargaArchivo().getCodigo());
			}

			if (codigoPrestamo != null) {
				try {
					Prestamo prestamo = prestamoDaoService.selectById(codigoPrestamo, "Prestamo");
					if (prestamo != null && prestamo.getIdAsoprep() != null) {
						novedad.setIdAsoprepPrestamo(prestamo.getIdAsoprep());
					}
				} catch (Throwable e) {
					// Si falla, continuar sin el idAsoprep — igual que el original.
				}
			}

			novedadParticipeCargaDaoService.save(novedad, novedad.getCodigo());
			System.out.println((esNueva ? "✅ Novedad creada" : "✅ Novedad actualizada (idempotente, ya existía)")
				+ " en TRANSACCIÓN PROPIA — participe " + participe.getCodigo() + ", tipo " + tipoNovedad
				+ ": " + descripcion);
		} catch (Throwable e) {
			// Mismo criterio que el registrarNovedad original: absorbe el error para no abortar
			// la aplicación de pagos por un fallo al dejar constancia de la novedad.
			System.err.println("Error al registrar novedad (transacción propia): " + descripcion);
			e.printStackTrace();
		}
	}

	/**
	 * Clave de idempotencia: (fila PXCA, tipoNovedad). Una fila PXCA ya identifica la carga (a
	 * través de su {@code DetalleCargaArchivo}), así que no hace falta agregar
	 * {@code codigoCargaArchivo} a la clave.
	 */
	private NovedadParticipeCarga buscarExistente(Long codigoParticipe, int tipoNovedad) throws Throwable {
		List<NovedadParticipeCarga> existentes = novedadParticipeCargaDaoService.selectByParticipe(codigoParticipe);
		if (existentes == null) {
			return null;
		}
		for (NovedadParticipeCarga existente : existentes) {
			if (existente.getTipoNovedad() != null && existente.getTipoNovedad().intValue() == tipoNovedad) {
				return existente;
			}
		}
		return null;
	}

}
