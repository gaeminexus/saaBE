/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.CuentaBancariaTitularDaoService;
import com.saa.ejb.tsr.service.CuentaBancariaTitularService;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.TipoIdentificacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CuentaBancariaTitularService.
 * Contiene los servicios relacionados con la entidad CuentaBancariaTitular.</p>
 */
@Stateless
public class CuentaBancariaTitularServiceImpl implements CuentaBancariaTitularService {

    @EJB
    private CuentaBancariaTitularDaoService cuentaBancariaTitularDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CuentaBancariaTitular service");
        CuentaBancariaTitular cuentaBancariaTitular = new CuentaBancariaTitular();
        for (Long registro : id) {
            cuentaBancariaTitularDaoService.remove(cuentaBancariaTitular, registro);
        }
    }

    @Override
    public void save(List<CuentaBancariaTitular> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CuentaBancariaTitular service");
        for (CuentaBancariaTitular registro : lista) {
            cuentaBancariaTitularDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuentaBancariaTitular> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) CuentaBancariaTitularService");
        List<CuentaBancariaTitular> result = cuentaBancariaTitularDaoService
                .selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CuentaBancariaTitular no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CuentaBancariaTitular> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) CuentaBancariaTitular");
        List<CuentaBancariaTitular> result = cuentaBancariaTitularDaoService
                .selectByCriteria(datos, NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda selectByCriteria CuentaBancariaTitular no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CuentaBancariaTitular selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CuentaBancariaTitular con id: " + id);
        return cuentaBancariaTitularDaoService.selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
    }

    @Override
    public CuentaBancariaTitular saveSingle(CuentaBancariaTitular cuentaBancariaTitular) throws Throwable {
        System.out.println("saveSingle - CuentaBancariaTitular");
        // Si codigo llega como 0 desde el cliente, se trata como nuevo registro (INSERT)
        if (cuentaBancariaTitular.getCodigo() != null && cuentaBancariaTitular.getCodigo() == 0L) {
            cuentaBancariaTitular.setCodigo(null);
        }
        validaIdentificacion(cuentaBancariaTitular);
        return cuentaBancariaTitularDaoService.save(cuentaBancariaTitular, cuentaBancariaTitular.getCodigo());
    }

    /**
     * Valida el tipo y la identificación con la que se abrió la cuenta (docs/
     * logica-negocio/tsr/API-IDENTIFICACION-CUENTA-BANCARIA.md §2.3/§3). Los dos
     * vacíos es válido -- la cuenta usa la identificación del titular, como hoy.
     * Deja {@code identificacion} ya recortada y sin espacios antes de grabar.
     * <p>
     * Valida SOLO longitud (no que el contenido sea numérico/alfanumérico): es la
     * misma regla con la que {@code InternacionalArchivoPagoFormateador.
     * validarLongitudIdentificacion} valida el archivo del banco, y esa tampoco
     * filtra el contenido -- sólo compara largo. Si el usuario quiere una
     * validación de contenido más estricta, es una regla nueva a decidir, no lo
     * que pide este ítem.
     * @param cuentaBancariaTitular : Cuenta a validar antes de grabar
     * @throws Throwable            : IncomeException si la regla no se cumple
     */
    private void validaIdentificacion(CuentaBancariaTitular cuentaBancariaTitular) throws Throwable {
        Long tipo = cuentaBancariaTitular.getTipoIdentificacion();
        String identificacion = cuentaBancariaTitular.getIdentificacion();
        if (identificacion != null) {
            identificacion = identificacion.trim();
            if (identificacion.isEmpty()) {
                identificacion = null;
            }
        }
        cuentaBancariaTitular.setIdentificacion(identificacion);

        if (tipo == null && identificacion == null) {
            return;
        }
        if (tipo == null || identificacion == null) {
            throw new IncomeException("Debe indicar el tipo de identificación y la identificación "
                    + "de la cuenta, o dejar los dos vacíos.");
        }

        int tipoInt = tipo.intValue();
        if (tipoInt != TipoIdentificacion.CEDULA_IDENTIDAD && tipoInt != TipoIdentificacion.RUC
                && tipoInt != TipoIdentificacion.PASAPORTE) {
            throw new IncomeException("Tipo de identificación de la cuenta no válido: " + tipo
                    + ". Use 1 (cédula), 2 (RUC) o 3 (pasaporte).");
        }

        // "sin espacios" -- se limpia y se graba ya limpia (§2.3).
        String limpia = identificacion.replaceAll("\\s+", "");
        cuentaBancariaTitular.setIdentificacion(limpia);

        if (tipoInt == TipoIdentificacion.CEDULA_IDENTIDAD && limpia.length() != 10) {
            throw new IncomeException("La cédula de la cuenta debe tener 10 dígitos: '" + limpia + "'.");
        }
        if (tipoInt == TipoIdentificacion.RUC && limpia.length() != 13) {
            throw new IncomeException("El RUC de la cuenta debe tener 13 dígitos: '" + limpia + "'.");
        }
        if (tipoInt == TipoIdentificacion.PASAPORTE && (limpia.length() < 5 || limpia.length() > 15)) {
            throw new IncomeException("El pasaporte de la cuenta debe tener entre 5 y 15 caracteres: '"
                    + limpia + "'.");
        }
    }
}
