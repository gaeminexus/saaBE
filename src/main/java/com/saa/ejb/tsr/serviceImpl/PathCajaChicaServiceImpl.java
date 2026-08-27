package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.PathCajaChicaDaoService;
import com.saa.ejb.tsr.service.PathCajaChicaService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PathCajaChica;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de PathCajaChicaService.</p>
 */
@Stateless
public class PathCajaChicaServiceImpl implements PathCajaChicaService {

	@EJB
	private PathCajaChicaDaoService pathCajaChicaDaoService;

	@EJB
	private FileService fileService;

	@Override
	public PathCajaChica selectById(Long id) throws Throwable {
		return pathCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.PATH_CAJA_CHICA);
	}

	@Override
	public List<PathCajaChica> selectAll() throws Throwable {
		List<PathCajaChica> result =
				pathCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.PATH_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PathCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<PathCajaChica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<PathCajaChica> result = pathCajaChicaDaoService.selectByCriteria(datos,
				NombreEntidadesTesoreria.PATH_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PathCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public PathCajaChica saveSingle(PathCajaChica adjunto) throws Throwable {
		if (adjunto.getCodigo() == null && adjunto.getFechaRegistro() == null) {
			adjunto.setFechaRegistro(LocalDateTime.now());
		}
		return pathCajaChicaDaoService.save(adjunto, adjunto.getCodigo());
	}

	@Override
	public void save(List<PathCajaChica> lista) throws Throwable {
		for (PathCajaChica registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		PathCajaChica entidad = new PathCajaChica();
		for (Long registro : id) {
			pathCajaChicaDaoService.remove(entidad, registro);
		}
	}

	@Override
	public List<PathCajaChica> porMovimiento(Long idMovimiento) throws Throwable {
		return pathCajaChicaDaoService.selectByMovimiento(idMovimiento);
	}

	@Override
	public void eliminar(Long idPath) throws Throwable {
		PathCajaChica adjunto = selectById(idPath);
		if (adjunto.getPath() != null) {
			try {
				fileService.deleteFile(adjunto.getPath());
			} catch (Throwable e) {
				System.err.println("⚠ No se pudo eliminar el archivo físico '" + adjunto.getPath()
						+ "': " + e.getMessage());
			}
		}
		pathCajaChicaDaoService.remove(adjunto, idPath);
	}

}
