package com.saa.ejb.asoprep.serviceImpl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.saa.basico.ejb.FileService;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.credito.service.CargaArchivoService;
import com.saa.ejb.credito.service.DetalleCargaArchivoService;
import com.saa.ejb.credito.service.ParticipeXCargaArchivoService;
import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.DetalleCargaArchivo;
import com.saa.model.credito.ParticipeXCargaArchivo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación Stateful para procesar archivos Petro con manejo de transacciones
 */
@Stateful
public class CargaArchivoPetroServiceImpl implements CargaArchivoPetroService {

    @EJB
    private FileService fileService;
    
    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @EJB
    private DetalleCargaArchivoService detalleCargaArchivoService;
    
    @EJB
    private ParticipeXCargaArchivoService participeXCargaArchivoService;
    
    /**
     * Método principal que procesa archivos Petro
     * OPCIÓN 1 APLICADA: Primero operaciones de BD, luego subir archivo
     * Utiliza transacción REQUIRED para rollback automático si falla algún paso
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String procesarArchivoPetro(InputStream archivoInputStream, String fileName, 
                                     CargaArchivo cargaArchivo, 
                                     List<DetalleCargaArchivo> detallesCargaArchivos,
                                     List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable {
        
        System.out.println("Iniciando procesamiento de archivo Petro: " + fileName);
        
        try {
        	
        	for (ParticipeXCargaArchivo participe : participesXCargaArchivo) {
            	if (participe.getCodigoPetro().equals(55145L)) {
            		System.out.println("TAL COMO LLEGA EL NOMBRE DE 55145: " + participe.getNombre());
            		System.out.println("CONVERTIDO A UTF 55145: " + convertirAUTF8(participe.getNombre()));
            	}
			}
        	
            // 1. PRIMERO: Almacenar registros en BD (TRANSACCIONAL)
            CargaArchivo cargaArchivoGuardado = almacenaRegistros(cargaArchivo, detallesCargaArchivos, participesXCargaArchivo);
        	
            System.out.println(cargaArchivoGuardado.getCodigo() + " - CargaArchivo almacenado con éxito");
            
            // 2. AL FINAL: Cargar el archivo físico (NO TRANSACCIONAL)
            // Solo se ejecuta si todas las operaciones de BD fueron exitosas
            String rutaArchivo = cargarArchivo(archivoInputStream, fileName, cargaArchivo);
            System.out.println("Archivo cargado en: " + rutaArchivo);
            
            cargaArchivoGuardado.setRutaArchivo(rutaArchivo);
            cargaArchivoGuardado = cargaArchivoService.saveSingle(cargaArchivoGuardado);
            
            System.out.println("Procesamiento completado exitosamente");
            return rutaArchivo;
            
        } catch (Throwable e) {
            System.err.println("Error en procesamiento: " + e.getMessage());
            // BENEFICIO: Si falla antes de cargar el archivo, no se sube archivo innecesario
            // Si falla en operaciones de BD, se hace rollback automático
            throw e;
        }
    }
    
    /**
     * Convierte un string de ISO-8859-1 a UTF-8
     * Útil cuando FormData envía UTF-8 pero el servidor lo interpreta como ISO-8859-1
     */
    private String convertirAUTF8(String textoOriginal) {
        if (textoOriginal == null) {
            return null;
        }
        
        // Re-interpretar: asume que los bytes son ISO-8859-1, conviértelos a UTF-8
        byte[] bytesIso = textoOriginal.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytesIso, StandardCharsets.UTF_8);
    }

    /**
     * Carga el archivo en la carpeta aportes/año/mes usando FileService
     */
    private String cargarArchivo(InputStream archivoInputStream, String fileName, CargaArchivo cargaArchivo) throws Throwable {
        // Construir la ruta: aportes/cargaArchivo.anioAfectacion/cargaArchivo.mesAfectacion
        StringBuilder uploadPath = new StringBuilder("aportes");
        
        if (cargaArchivo.getAnioAfectacion() != null) {
            uploadPath.append("/").append(cargaArchivo.getAnioAfectacion());
        }
        
        if (cargaArchivo.getMesAfectacion() != null) {
            uploadPath.append("/").append(cargaArchivo.getMesAfectacion());
        }
        
        System.out.println("Cargando archivo en ruta: " + uploadPath.toString());
        
        return fileService.uploadFileToPath(archivoInputStream, fileName, uploadPath.toString());
    }

    /**
     * Almacena el registro de CargaArchivo
     */
    private CargaArchivo almacenarCargaArchivo(CargaArchivo cargaArchivo) throws Throwable {
        System.out.println("Guardando CargaArchivo: " + cargaArchivo.getNombre());
        
        // Asignar fecha de carga si no viene del frontend
        if (cargaArchivo.getFechaCarga() == null) {
            cargaArchivo.setFechaCarga(java.time.LocalDateTime.now());
        }
        
        // Validar que los campos obligatorios vengan del frontend
        if (cargaArchivo.getFilial() == null) {
            throw new RuntimeException("El campo 'filial' es obligatorio y debe ser enviado desde el frontend");
        }
        
        if (cargaArchivo.getUsuarioCarga() == null) {
            throw new RuntimeException("El campo 'usuarioCarga' es obligatorio y debe ser enviado desde el frontend");
        }
        
        // Log de los datos recibidos
        System.out.println("Filial recibida: " + cargaArchivo.getFilial().getCodigo() + " - " + cargaArchivo.getFilial().getNombre());
        System.out.println("Usuario recibido: " + cargaArchivo.getUsuarioCarga().getCodigo());
        
        return cargaArchivoService.saveSingle(cargaArchivo);
    }
    
    private CargaArchivo almacenaRegistros(CargaArchivo cargaArchivo,
										List<DetalleCargaArchivo> detallesCargaArchivos,
										List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable {
		// Almacenar CargaArchivo
		CargaArchivo cargaArchivoGuardado = almacenarCargaArchivo(cargaArchivo);
		
		// Asignar la referencia al CargaArchivo guardado
        for (DetalleCargaArchivo detalle : detallesCargaArchivos) {
        	Long codigoDetalleOriginal = detalle.getCodigo();
        	detalle.setCodigo(null); // Para que se genere uno nuevo
            detalle.setCargaArchivo(cargaArchivoGuardado);
            detalle = detalleCargaArchivoService.saveSingle(detalle);
            for (ParticipeXCargaArchivo participe : filtrarPorCodigoDetalle(participesXCargaArchivo, codigoDetalleOriginal)) {
            	if (participe.getCodigoPetro().equals(55145L)) {
            		System.out.println("Participe con código Petro 55145 encontrado para detalle código: " + participe.getNombre());
            	}
				participe.setCodigo(null); // Limpiar código para que se genere uno nuevo
				participe.setDetalleCargaArchivo(detalle);
				participe = participeXCargaArchivoService.saveSingle(participe);
			}
        }
		return cargaArchivoGuardado;
	}
    
    // Filtrar partícipes por código específico de DetalleCargaArchivo
    public List<ParticipeXCargaArchivo> filtrarPorCodigoDetalle(List<ParticipeXCargaArchivo> participesXCargaArchivo, 
                                                              Long codigoDetalle) {
        return participesXCargaArchivo.stream()
            .filter(participe -> participe.getDetalleCargaArchivo() != null && 
                               participe.getDetalleCargaArchivo().getCodigo().equals(codigoDetalle))
            .collect(Collectors.toList());
    }
   
}