package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaConCertificado;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;

import jakarta.ejb.Local;

@Local
public interface CuentaBancariaParticipeService extends EntityService<CuentaBancariaParticipe> {

    /**
     * Nombre exacto del {@code TipoAdjunto} (CRD.TPDJ) que identifica un certificado bancario.
     * Tiene que coincidir con el valor cargado por
     * {@code docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql}.
     */
    String CERTIFICADO_BANCARIO = "CERTIFICADO BANCARIO";

    /** 400 - Falta un campo obligatorio (incluido el archivo) */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - El partícipe no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 404 - El banco externo no existe */
    String ERR_BANCO_NO_ENCONTRADO = "BANCO_NO_ENCONTRADO";
    /** 422 - El archivo no es un PDF */
    String ERR_EXTENSION_NO_PERMITIDA = "EXTENSION_NO_PERMITIDA";
    /** 422 - El archivo supera el tope de FileService.TAMAÑO_MAXIMO */
    String ERR_ARCHIVO_MUY_GRANDE = "ARCHIVO_MUY_GRANDE";
    /** 422 - El archivo llegó vacío */
    String ERR_ARCHIVO_VACIO = "ARCHIVO_VACIO";
    /**
     * 500 (de configuración, no del usuario) - Falta cargar CRD.TPDJ con
     * {@link #CERTIFICADO_BANCARIO}. Ver docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql.
     */
    String ERR_TIPO_ADJUNTO_NO_CONFIGURADO = "TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO";

    /**
     * Retorna las cuentas bancarias de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de cuentas bancarias
     */
    List<CuentaBancariaParticipe> selectByParent(Long idEntidad) throws Throwable;

    /**
     * Registra una cuenta bancaria de partícipe JUNTO con su certificado bancario, en una sola
     * transacción: si algo falla, no queda ni cuenta huérfana en CRD.CNBP ni archivo colgado en
     * disco. Es el ÚNICO camino soportado para crear una cuenta bancaria de partícipe — el
     * {@code POST /rest/cnbp} genérico rechaza la creación y remite acá.
     *
     * @param solicitud Datos de la cuenta + el PDF del certificado
     * @return La cuenta y el adjunto creados
     * @throws Throwable Si algún dato es inválido, el archivo no es un PDF válido, o falla el
     *                    guardado (en cuyo caso no queda rastro de ninguna de las dos partes)
     */
    ResultadoCuentaBancariaConCertificado crearConCertificado(SolicitudCuentaBancariaConCertificado solicitud)
            throws Throwable;

    /**
     * El certificado bancario ACTIVO de una cuenta, o null si no tiene (no debería pasar para
     * cuentas creadas por {@link #crearConCertificado}, pero cuentas migradas o antiguas del
     * bypass pueden no tenerlo).
     *
     * @param idCuenta Código de CuentaBancariaParticipe (CNBPCDGO)
     * @throws Throwable Si ocurre un error
     */
    Adjunto obtenerCertificado(Long idCuenta) throws Throwable;
}
