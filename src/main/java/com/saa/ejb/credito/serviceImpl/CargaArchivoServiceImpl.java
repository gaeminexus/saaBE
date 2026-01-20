package com.saa.ejb.credito.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.AporteDaoService;
import com.saa.ejb.credito.dao.CargaArchivoDaoService;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.ejb.credito.service.CargaArchivoService;
import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.Entidad;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CargaArchivoServiceImpl implements CargaArchivoService {

    @EJB
    private CargaArchivoDaoService CargaArchivoDaoService;
    
    @EJB
    private EntidadDaoService entidadDaoService;
    
    @EJB
	private AporteDaoService aporteDaoService;

    /**
     * Recupera un registro de CargaArchivo por su ID.
     */
    @Override
    public CargaArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return CargaArchivoDaoService.selectById(id, NombreEntidadesCredito.CARGA_ARCHIVO);
    }

    /**
     * Elimina uno o varios registros de CargaArchivo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CargaArchivoService ... depurado");
        CargaArchivo cargaArchivo = new CargaArchivo();
        for (Long registro : id) {
            CargaArchivoDaoService.remove(cargaArchivo, registro);
        }
    }

    /**
     * Guarda una lista de registros de CargaArchivo.
     */
    @Override
    public void save(List<CargaArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CargaArchivoService");
        for (CargaArchivo registro : lista) {
            CargaArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de CargaArchivo.
     */
    @Override
    public List<CargaArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CargaArchivoService");
        List<CargaArchivo> result = CargaArchivoDaoService.selectAll(NombreEntidadesCredito.CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CargaArchivo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de CargaArchivo.
     */
    @Override
    public CargaArchivo saveSingle(CargaArchivo cargaArchivo) throws Throwable {
        System.out.println("saveSingle - CargaArchivo");
        if (cargaArchivo.getCodigo() == null) {
        	cargaArchivo.setFechaCarga(LocalDateTime.now());
            cargaArchivo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
        }
        cargaArchivo = CargaArchivoDaoService.save(cargaArchivo, cargaArchivo.getCodigo());
        return cargaArchivo;
    }

    /**
     * Recupera registros de CargaArchivo segun criterios de búsqueda.
     */
    @Override
    public List<CargaArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CargaArchivoService");
        List<CargaArchivo> result = CargaArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CargaArchivo no devolvio ningun registro");
        }
        return result;
    }
    
    /*
    @Override
    public String melyTest(Long idEntidad) throws Throwable {
    	System.out.println("Id Entidad: " + idEntidad);
    	Long conteo = 0L; 
    	List<Aporte> listaAportes;
    	/*queda obtener el nombre de la entidad
        Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        System.out.println(" el nombre de la Entidad: " + entidad.getRazonSocial());
        listaAportes = aporteDaoService.selectByEntidad(idEntidad);
        conteo = Long.valueOf(listaAportes.size());
        // conteo = aporteDaoService.selectByEntidad(idEntidad);
        System.out.println(" El conteo de aportes por entidad es: " + conteo);
        return entidad.getRazonSocial();
    }*/
    
    
    @Override
    public String melyTest(Long idEntidad) throws Throwable {
    	System.out.println("Id Entidad: " + idEntidad);
    	Long conteo = 0L; 
    	/*queda obtener el nombre de la entidad*/
        Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        System.out.println(" el nombre de la Entidad: " + entidad.getRazonSocial());
        /*contar los aportes directamente desde la base de datos*/
        conteo = aporteDaoService.selectCountByEntidad(idEntidad);
        System.out.println(" El conteo de aportes por entidad es: " + conteo);
        return entidad.getRazonSocial();
    }
}    
 /*   
    @Override
    public String testDos(Long cedula) throws Throwable {
    	System.out.println("cedula: " + cedula);
    	Long conteo = 0L; 
    	/*queda obtener el nombre de la entidad
        Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        System.out.println(" el nombre de la Entidad: " + entidad.getRazonSocial());
        /*contar los aportes directamente desde la base de datos
        conteo = aporteDaoService.selectCountByEntidad(idEntidad);
        System.out.println(" El conteo de aportes por entidad es: " + conteo);
        return entidad.getRazonSocial();
    }
}
*/


/* 10000 a 2años 10% --- calcular debo calcular el interes de los dos años y sumarle al monto y eso dividir a las cuotas   
 * 
 *     public Double melyTest(Long plazo, Double tasaInteres, Double monto, Long idEntidad, Long tipoTabla) throws Throwable {
    	
        System.out.println("Plazo: " + plazo);
        System.out.println("Interés: " + tasaInteres);
        System.out.println("Monto: " + monto);
        System.out.println("Id Entidad: " + idEntidad);
        System.out.println("Tipo Tabla: " + tipoTabla);
        
        Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        System.out.println("Entidad: " + entidad.getRazonSocial());
        
        Double aniosPrestamo = plazo / 12.0;
        System.out.println("Años del préstamo: " + aniosPrestamo);
        
        
        /*interes es igual a cap * tasa / 100
        Double interesPrestamo = 0.0;
		interesPrestamo = (monto * tasaInteres) / 100;
		
		Double interesTotal = interesPrestamo * aniosPrestamo;
		System.out.println("Interes total del prestamo por " + aniosPrestamo + " años: " + interesTotal);
		System.out.println("Capital del prestamo: " + monto);
		System.out.println("Interes del prestamo: " + interesPrestamo);
      
		
		/*interes total + monto dividido entre numero de pagos*/
		/*(interesTotal + monto)/plazo;*/
		
		
        /*capital + interes dividido entre numero de pagos
		Double valorCuota = (monto + interesPrestamo)/plazo;
		Double saldoPrestamo = (monto + interesPrestamo);
		for(int i=1; i<=plazo; i++) {
			saldoPrestamo = saldoPrestamo - valorCuota;
			System.out.println("Cuota " + i + ": " + valorCuota + " Saldo despues de pagar la cuota " + i + ": " + saldoPrestamo);
		}
	
		return valorCuota;
    }

}*/

