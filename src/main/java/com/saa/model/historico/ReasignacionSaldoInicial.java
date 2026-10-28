package com.saa.model.historico;

/**
 * D-013: petición para mover parte del saldo inicial (titular SALDO INICIAL, documentos al 31-may-2025 sin asiento)
 * a un titular real cuando aparece el origen del saldo. {@code POST /SaaBE/rest/historico/reasignarSaldoInicial}.
 *
 * <p>No toca la contabilidad de mayo: rebaja el total del documento genérico y crea el documento del titular real
 * con fecha de emisión ≤ corte, también sin líneas ni asiento. La fecha, la decisión y el motivo de la reasignación
 * quedan en la observación de los dos documentos. POJO de transporte, campos públicos.</p>
 */
public class ReasignacionSaldoInicial {

    public Long idEmpresa;
    /** Usuario del SAA; por defecto 38. */
    public Long idUsuario;
    public String usuario;
    /** true = solo informa lo que haría. */
    public boolean soloValidar;
    /** PROVEEDOR (2.3.90.10, PGS.FCTC) o CLIENTE (1.4.90.10, CBR.FCTR). */
    public String lado;
    /** Titular genérico de origen; por defecto 9999999999001 (SALDO INICIAL). */
    public String identificacionGenerico;
    /** Número del documento genérico del que se saca el saldo; por defecto 999-999-000000001. */
    public String numeroGenerico;
    /** Titular real que recibe el saldo. Si no existe se crea con razonSocial/tipoPersona/tipoIdentificacion. */
    public String identificacionDestino;
    public String razonSocialDestino;
    /** NATURAL o JURIDICA (solo si hay que crear el titular). */
    public String tipoPersonaDestino;
    /** RUC, CEDULA o PASAPORTE (solo si hay que crear el titular). */
    public String tipoIdentificacionDestino;
    /** Monto a mover (≤ saldo del documento genérico). */
    public Double monto;
    /** Fecha en que se hace la reasignación (trazabilidad). No es fecha contable: no se genera asiento. */
    public String fecha;
    /** Decisión que la sustenta en DECISIONES-Y-PARTIDAS.md (p. ej. "D-014"). Obligatoria (regla 4 del expediente). */
    public String decision;
    /** Evidencia: de dónde sale el origen (pago de junio a la factura X, extracto, acta...). Obligatorio. */
    public String motivo;
    /** Número real del comprobante del titular destino; si falta se genera 999-998-NNNNNNNNN. */
    public String numeroDocumento;
    public String claveAcceso;
    /** Fecha de emisión real del comprobante destino (≤ fecha del documento genérico); si falta, la del genérico. */
    public String fechaEmisionDocumento;
}
