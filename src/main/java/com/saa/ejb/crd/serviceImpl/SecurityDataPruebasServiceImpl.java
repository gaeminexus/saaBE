package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.ejb.crd.dao.DireccionDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionesFirma;
import com.saa.ejb.crd.firma.SecurityDataPruebas.Archivo;
import com.saa.ejb.crd.service.SecurityDataPruebasService;
import com.saa.ejb.crd.util.ContratoAdhesionParametros;
import com.saa.ejb.crd.util.PosicionesFirmaContrato;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Participe;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see SecurityDataPruebasService
 * Solo lectura: no hay transacción (NOT_SUPPORTED) y no se guarda nada.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SecurityDataPruebasServiceImpl implements SecurityDataPruebasService {

    private static final String MODULO_REPORTE = "crd";
    private static final String REPORTE_CONTRATO = "RPRT_CNTR_ADHS";
    private static final String RUTA_JASPER = "/rep/" + MODULO_REPORTE + "/" + REPORTE_CONTRATO + ".jasper";

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private DireccionDaoService direccionDaoService;

    @EJB
    private ReporteService reporteService;

    @Override
    public Map<String, Object> socioPorCedula(String cedula) throws Throwable {
        System.out.println("socioPorCedula - SecurityDataPruebas");
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IncomeException("PARAMETRO_INVALIDO: la cédula es obligatoria");
        }
        Entidad e = null;
        try {
            e = entidadDaoService.selectByNumeroIdentificacion(cedula.trim());
        } catch (Throwable ignorada) {
            // sin coincidencia: se responde encontrado=false
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("encontrado", e != null);
        if (e != null) {
            r.put("nombre", e.getRazonSocial());
            r.put("correo", e.getCorreoPersonal() != null && !e.getCorreoPersonal().trim().isEmpty()
                ? e.getCorreoPersonal().trim() : e.getCorreoInstitucional());
            r.put("celular", e.getMovil());
        }
        return r;
    }

    @Override
    public Archivo generarPdfContrato(Long idContrato) throws Throwable {
        System.out.println("generarPdfContrato - SecurityDataPruebas - idContrato: " + idContrato);
        Map<String, Object> parametros = parametros(idContrato);
        byte[] pdf = reporteService.generarReporteDesdeColeccion(MODULO_REPORTE, REPORTE_CONTRATO, parametros,
            Collections.singletonList(new HashMap<String, Object>()), "PDF");
        return new Archivo("contrato-adhesion-" + idContrato + "-prueba.pdf", pdf);
    }

    @Override
    public PosicionesFirma calcularPosiciones(Long idContrato) throws Throwable {
        System.out.println("calcularPosiciones - SecurityDataPruebas - idContrato: " + idContrato);
        try {
            return PosicionesFirmaContrato.calcular(RUTA_JASPER, parametros(idContrato));
        } catch (IncomeException e) {
            throw e;
        } catch (Exception e) {
            throw new IncomeException("POSICION_FIRMA_NO_ENCONTRADA: no se pudo ubicar la firma en el contrato: " + e.getMessage());
        }
    }

    private Map<String, Object> parametros(Long idContrato) throws Throwable {
        if (idContrato == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el número de contrato es obligatorio");
        }
        if (SecurityDataPruebasServiceImpl.class.getResource(RUTA_JASPER) == null) {
            throw new IncomeException("FORMATO_CONTRATO_PENDIENTE: el formato " + REPORTE_CONTRATO + " no está disponible");
        }
        Contrato contrato = contratoDaoService.find(new Contrato(), idContrato);
        if (contrato == null) {
            throw new IncomeException("CONTRATO_NO_ENCONTRADO: no existe el contrato " + idContrato);
        }
        Long idEntidad = contrato.getEntidad() != null ? contrato.getEntidad().getCodigo() : null;
        List<Participe> participes = idEntidad != null ? participeDaoService.selectByEntidad(idEntidad) : null;
        List<Direccion> direcciones = idEntidad != null ? direccionDaoService.selectByParent(idEntidad) : null;
        return ContratoAdhesionParametros.armar(contrato,
            participes != null && !participes.isEmpty() ? participes.get(0) : null, direcciones, null, LocalDate.now());
    }
}
