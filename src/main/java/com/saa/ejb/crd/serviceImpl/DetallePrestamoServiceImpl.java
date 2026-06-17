package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetallePrestamoServiceImpl implements DetallePrestamoService {

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;
    
    @EJB
    private FechaService fechaService;

    /**
     * Recupera un registro de DetallePrestamo por su ID.
     */
    @Override
    public DetallePrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return detallePrestamoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de DetallePrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetallePrestamoService ... depurado");
        DetallePrestamo detalle = new DetallePrestamo();
        for (Long registro : id) {
            detallePrestamoDaoService.remove(detalle, registro);
        }
    }

    /**
     * Guarda una lista de registros de DetallePrestamo.
     */
    @Override
    public void save(List<DetallePrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetallePrestamoService");
        for (DetallePrestamo registro : lista) {
            detallePrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de DetallePrestamo.
     */
    @Override
    public List<DetallePrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetallePrestamoService");
        List<DetallePrestamo> result = detallePrestamoDaoService.selectAll(NombreEntidadesCredito.DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetallePrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de DetallePrestamo.
     */
    @Override
    public DetallePrestamo saveSingle(DetallePrestamo detalle) throws Throwable {
        System.out.println("saveSingle - DetallePrestamo");
        if(detalle.getCodigo() == null){
        	detalle.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        detalle = detallePrestamoDaoService.save(detalle, detalle.getCodigo());
        return detalle;
    }

    /**
     * Recupera registros de DetallePrestamo segun criterios de búsqueda.
     */
    @Override
    public List<DetallePrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetallePrestamoService");
        List<DetallePrestamo> result = detallePrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DetallePrestamo no devolvio ningun registro");
        }
        return result;
    }

	@Override
	public List<DetallePrestamo> selectByMesAnio(Long mes, Long anio) throws Throwable {
		System.out.println("selectByMesAnio con mes: " + mes + " anio: " + anio);
		LocalDate fechaInicio = fechaService.primerDiaMesAnioLocal(mes, anio);
		LocalDate fechaHasta = fechaService.ultimoDiaMesAnioLocal(mes, anio);
        List<DetallePrestamo> result = detallePrestamoDaoService.selectByRangoFechas(fechaInicio.atStartOfDay(), fechaHasta.atStartOfDay());
        if (result.isEmpty()) {
            throw new IncomeException("selectByMesAnio DetallePrestamo no devolvio ningun registro");
        }
        return result;
	}

	@Override
	public List<Object[]> selectSumaCuotasPagadasPorEntidad(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectSumaCuotasPagadasPorEntidad(fechaInicio, fechaFin);
	}

	@Override
	public DetallePrestamo selectMenorCuotaActiva(Long codigoPrestamo, java.time.LocalDateTime fechaCorte) throws Throwable {
		return detallePrestamoDaoService.selectMenorCuotaActiva(codigoPrestamo, fechaCorte);
	}

	@Override
	public List<DetallePrestamo> selectCuotasDelMes(Long codigoPrestamo, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectCuotasDelMes(codigoPrestamo, fechaInicio, fechaFin);
	}

	@Override
	public DetallePrestamo selectMenorCuotaAnteriorAlMes(Long codigoPrestamo, java.time.LocalDateTime fechaInicio) throws Throwable {
		return detallePrestamoDaoService.selectMenorCuotaAnteriorAlMes(codigoPrestamo, fechaInicio);
	}

	@Override
	public List<DetallePrestamo> selectCuotasDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectCuotasDelMesGlobal(fechaInicio, fechaFin);
	}

	@Override
	public List<DetallePrestamo> selectMenorCuotaAnteriorAlMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectMenorCuotaAnteriorAlMesGlobal(fechaInicio, fechaFin);
	}

	@Override
	public List<DetallePrestamo> selectMaxCuotaPagadaDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectMaxCuotaPagadaDelMesGlobal(fechaInicio, fechaFin);
	}

	@Override
	public List<DetallePrestamo> selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(fechaInicio, fechaFin);
	}

	@Override
	public Object[] selectSumaCapitalInteresGrupo2(Long codigoPrestamo, Double numeroCuotaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectSumaCapitalInteresGrupo2(codigoPrestamo, numeroCuotaInicio, fechaFin);
	}

	@Override
	public Double calcularInteresMora(Long codigoCuotaOrigen, java.time.LocalDateTime fechaHasta) throws Throwable {
		System.out.println("calcularInteresMora - cuotaOrigen: " + codigoCuotaOrigen + " hasta: " + fechaHasta);

		List<Object[]> cuotas = detallePrestamoDaoService.selectCuotasParaMora(codigoCuotaOrigen, fechaHasta);

		java.time.LocalDate fechaHastaDate = fechaHasta.toLocalDate();
		double totalMora = 0.0;

		for (Object[] fila : cuotas) {
			Double capital        = fila[0] != null ? ((Number) fila[0]).doubleValue() : 0.0;
			java.time.LocalDateTime fechaVenc = (java.time.LocalDateTime) fila[1];
			Double interesNominal = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;

			if (capital <= 0.0 || fechaVenc == null) continue;

			// Si la tasa nominal es 0 o nula, se usa 9 como valor por defecto para el G48
			if (interesNominal <= 0.0) interesNominal = 9.0;

			// Tasa de mora = tasa nominal del préstamo (almacenada como porcentaje, ej: 9 = 9%)
			double tasaMora = interesNominal / 100.0;

			// Días de mora: desde el día siguiente al vencimiento hasta fechaHasta
			long diasMora = java.time.temporal.ChronoUnit.DAYS.between(fechaVenc.toLocalDate(), fechaHastaDate);
			if (diasMora <= 0) continue;

			// Interés mora cuota = capital × (tasaMora / 360) × diasMora
			double interesMoraCuota = capital * (tasaMora / 360.0) * diasMora;
			totalMora += interesMoraCuota;

			System.out.println("  cuota capital=" + capital + " tasaMora=" + tasaMora
				+ " dias=" + diasMora + " mora=" + interesMoraCuota);
		}

		System.out.println("calcularInteresMora TOTAL=" + totalMora);
		return totalMora;
	}

	@Override
	public List<Object[]> selectSumaCapitalInteresGrupo2Batch(List<Long> codigosCuotasOrigen, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectSumaCapitalInteresGrupo2Batch(codigosCuotasOrigen, fechaFin);
	}

	@Override
	public List<Object[]> calcularInteresMoraBatch(List<Long> codigosCuotas, java.time.LocalDateTime fechaHasta) throws Throwable {
		return detallePrestamoDaoService.calcularInteresMoraBatch(codigosCuotas, fechaHasta);
	}

	@Override
	public List<Object[]> selectCapitalCuotasFuturasBatch(List<Long> codigosPrestamos, java.time.LocalDateTime fechaEjecucion) throws Throwable {
		return detallePrestamoDaoService.selectCapitalCuotasFuturasBatch(codigosPrestamos, fechaEjecucion);
	}

	@Override
	public List<Object[]> selectCapitalCuotasDesdeInicioMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio) throws Throwable {
		return detallePrestamoDaoService.selectCapitalCuotasDesdeInicioMesBatch(codigosPrestamos, fechaInicio);
	}

	@Override
	public List<Object[]> selectSaldoInicialCapitalDelMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.selectSaldoInicialCapitalDelMesBatch(codigosPrestamos, fechaInicio, fechaFin);
	}

	@Override
	public List<Object[]> calcularInteresMoraDelMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return detallePrestamoDaoService.calcularInteresMoraDelMesBatch(codigosPrestamos, fechaInicio, fechaFin);
	}
}
