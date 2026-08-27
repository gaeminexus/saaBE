package com.saa.ejb.cxp.serviceImpl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.DetallePagoOrigenExternoDaoService;
import com.saa.ejb.cxp.dao.LotePagoDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AnticipoProveedorService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.FormateadorArchivoBanco;
import com.saa.ejb.cxp.service.LectorRespuestaBanco;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.RespuestaPagoBanco;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
import com.saa.ejb.tsr.service.ChequeService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.DetallePagoOrigenExterno;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.BancoExterno;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.Egreso;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoLotePago;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.FormaPagoProgramado;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PagoProgramadoServiceImpl implements PagoProgramadoService {

	/** Tolerancia para comparar valores monetarios. */
	private static final double TOLERANCIA = 0.01;

	/** Subdirectorio donde se guardan los archivos enviados al banco. */
	private static final String RUTA_ARCHIVOS_BANCO = "docs/pagos/banco";

	@EJB
	private PagoProgramadoDaoService pagoProgramadoDaoService;

	@EJB
	private LotePagoDaoService lotePagoDaoService;

	@EJB
	private DetallePagoOrigenExternoDaoService detallePagoOrigenExternoDaoService;

	@EJB
	private AplicacionPagoCxpService aplicacionPagoCxpService;

	@EJB
	private AnticipoProveedorService anticipoProveedorService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;

	@EJB
	private ChequeService chequeService;

	@EJB
	private FileService fileService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public PagoProgramado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PagoProgramado con id: " + id);
		return pagoProgramadoDaoService.selectById(id, NombreEntidadesCompra.PAGO_PROGRAMADO);
	}

	@Override
	public List<PagoProgramado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PagoProgramadoService");
		List<PagoProgramado> result =
				pagoProgramadoDaoService.selectAll(NombreEntidadesCompra.PAGO_PROGRAMADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PagoProgramado no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<PagoProgramado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PagoProgramadoService");
		List<PagoProgramado> result = pagoProgramadoDaoService.selectByCriteria(datos,
				NombreEntidadesCompra.PAGO_PROGRAMADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PagoProgramado no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public PagoProgramado saveSingle(PagoProgramado pago) throws Throwable {
		System.out.println("saveSingle - PagoProgramado");
		if (pago.getId() == null) {
			if (pago.getEstado() == null) {
				pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			}
			if (pago.getFechaRegistro() == null) {
				pago.setFechaRegistro(LocalDateTime.now());
			}
		}
		return pagoProgramadoDaoService.save(pago, pago.getId());
	}

	@Override
	public void save(List<PagoProgramado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PagoProgramadoService");
		for (PagoProgramado registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PagoProgramadoService");
		PagoProgramado entidad = new PagoProgramado();
		for (Long registro : id) {
			pagoProgramadoDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Registro y listado
	// =====================================================================

	@Override
	public Map<String, Object> registrarPago(Long idFacturaCompra, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Double valor, String fechaProgramada, Long idEmpresa,
			Long idUsuario, String observacion, boolean debitoAutomatico, String referencia)
			throws Throwable {
		return registrarPago(idFacturaCompra, idCuentaBancariaOrigen, idCuentaDestinoTitular, valor,
				fechaProgramada, idEmpresa, idUsuario, observacion, debitoAutomatico, referencia,
				Long.valueOf(debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO
						: FormaPagoProgramado.TRANSFERENCIA));
	}

	@Override
	public Map<String, Object> registrarPago(Long idFacturaCompra, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Double valor, String fechaProgramada, Long idEmpresa,
			Long idUsuario, String observacion, boolean debitoAutomatico, String referencia,
			Long formaPago) throws Throwable {

		System.out.println("=== registrarPago | factura=" + idFacturaCompra + " | valor=" + valor
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " | formaPago=" + formaPago + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del pago debe ser mayor a cero.");
		}

		FacturaCompra factura = em.find(FacturaCompra.class, idFacturaCompra);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de compra con ID: " + idFacturaCompra);
		}
		if (factura.getTitular() == null) {
			throw new IncomeException("La factura de compra " + idFacturaCompra
					+ " no tiene proveedor asignado.");
		}

		CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaBancariaOrigen);
		if (cuentaOrigen == null) {
			throw new IncomeException("No se encontró la cuenta bancaria de origen con ID: "
					+ idCuentaBancariaOrigen);
		}

		long fp = validarFormaPago(cuentaOrigen, formaPago, debitoAutomatico);
		boolean esDebitoAutomatico = (fp == FormaPagoProgramado.DEBITO_AUTOMATICO);

		CuentaBancariaTitular cuentaDestino = null;
		if (idCuentaDestinoTitular != null) {
			cuentaDestino = em.find(CuentaBancariaTitular.class, idCuentaDestinoTitular);
			if (cuentaDestino == null) {
				throw new IncomeException("No se encontró la cuenta bancaria del proveedor con ID: "
						+ idCuentaDestinoTitular);
			}
			if (cuentaDestino.getTitular() != null
					&& !cuentaDestino.getTitular().getCodigo().equals(factura.getTitular().getCodigo())) {
				throw new IncomeException("La cuenta bancaria de destino pertenece a otro titular, "
						+ "no al proveedor de la factura.");
			}
		}

		// El valor no puede superar el saldo pendiente menos lo ya comprometido
		// en otros pagos vigentes de la misma factura.
		validaValorContraSaldo(factura, valor, null);

		LocalDate fecha = parseFecha(fechaProgramada);

		PagoProgramado pago = new PagoProgramado();
		pago.setEmpresa(em.find(Empresa.class, idEmpresa));
		pago.setFacturaCompra(factura);
		pago.setTitular(factura.getTitular());
		pago.setCuentaBancaria(cuentaOrigen);
		pago.setCuentaDestino(cuentaDestino);
		pago.setDebitoAutomatico(Long.valueOf(esDebitoAutomatico ? 1 : 0));
		pago.setFormaPago(Long.valueOf(fp));
		pago.setValor(valor);
		pago.setFechaProgramada(fecha);
		pago.setObservacion(observacion);
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());

		if (fp == FormaPagoProgramado.CHEQUE) {
			Cheque cheque = chequeService.asignarAPago(idCuentaBancariaOrigen, valor,
					factura.getTitular(), factura.getTitular().getNombre(), idUsuario);
			pago.setCheque(cheque);
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
			pago.setReferenciaBanco("CHQ-" + cheque.getNumero());
			pago.setFechaRespuesta(fecha);
			pago = guardaPagoConCheque(pago, cheque);

			Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
			pago = pagoProgramadoDaoService.save(pago, pago.getId());
			em.flush();

			System.out.println("✓ Pago con cheque N° " + cheque.getNumero()
					+ " registrado y aplicado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje", "Pago con cheque N° " + cheque.getNumero()
					+ " registrado. La factura quedó abonada y el asiento contable fue generado.");
			resultado.put("pago", pago.getId());
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			resultado.put("numeroCheque", cheque.getNumero());
			if (pago.getAplicacion() != null) {
				resultado.put("aplicacion", pago.getAplicacion().getId());
			}
			if (asiento != null) {
				resultado.put("asiento", asiento.getNumeroAlterno());
			}
			resultado.putAll(aplicacionPagoCxpService.saldoFactura(idFacturaCompra));
			return resultado;
		}

		if (!esDebitoAutomatico) {
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			pago = saveSingle(pago);

			System.out.println("✓ Pago registrado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje",
					"Pago registrado. Queda pendiente de incluirse en un archivo de pagos.");
			resultado.put("pago", pago.getId());
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			resultado.putAll(aplicacionPagoCxpService.saldoFactura(idFacturaCompra));
			return resultado;
		}

		// Débito automático: el banco ya debitó la cuenta. El pago no se aprueba
		// ni se envía en ningún archivo, así que nace confirmado y se contabiliza
		// aquí mismo. La fecha del débito es la fecha con la que se registra.
		pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
		pago.setReferenciaBanco((referencia != null && !referencia.trim().isEmpty())
				? referencia.trim() : null);
		pago.setFechaRespuesta(fecha);
		pago = saveSingle(pago);
		em.flush();

		Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
		pago = pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		System.out.println("✓ Pago por débito automático registrado y aplicado: id=" + pago.getId());

		resultado.put("exito", true);
		resultado.put("mensaje", "Pago por débito automático registrado. La factura quedó abonada "
				+ "y el asiento contable fue generado.");
		resultado.put("pago", pago.getId());
		resultado.put("debitoAutomatico", true);
		resultado.put("formaPago", fp);
		if (pago.getAplicacion() != null) {
			resultado.put("aplicacion", pago.getAplicacion().getId());
		}
		if (asiento != null) {
			resultado.put("asiento", asiento.getNumeroAlterno());
		}
		resultado.putAll(aplicacionPagoCxpService.saldoFactura(idFacturaCompra));
		return resultado;
	}

	@Override
	public Map<String, Object> registrarPagoDeEgreso(Long idEgreso, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia) throws Throwable {
		return registrarPagoDeEgreso(idEgreso, idCuentaBancariaOrigen, idCuentaDestinoTitular,
				idUsuario, debitoAutomatico, referencia,
				Long.valueOf(debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO
						: FormaPagoProgramado.TRANSFERENCIA));
	}

	@Override
	public Map<String, Object> registrarPagoDeEgreso(Long idEgreso, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia, Long formaPago) throws Throwable {

		System.out.println("=== registrarPagoDeEgreso | egreso=" + idEgreso
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " | formaPago=" + formaPago + " ===");

		Map<String, Object> resultado = new HashMap<>();

		Egreso egreso = em.find(Egreso.class, idEgreso);
		if (egreso == null) {
			throw new IncomeException("No se encontró el egreso de tesorería con ID: " + idEgreso);
		}
		if (egreso.getEstado() == null
				|| egreso.getEstado().intValue() != EstadoEgresoTesoreria.PENDIENTE_PAGO) {
			throw new IncomeException("El egreso " + idEgreso + " no está pendiente de pago.");
		}
		if (egreso.getValor() == null || egreso.getValor() <= 0) {
			throw new IncomeException("El valor del egreso debe ser mayor a cero.");
		}
		if (!pagoProgramadoDaoService.selectVigentesByEgreso(idEgreso).isEmpty()) {
			throw new IncomeException("El egreso " + idEgreso
					+ " ya tiene un pago vigente. Anúlelo o reviértalo antes de registrar otro.");
		}

		CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaBancariaOrigen);
		if (cuentaOrigen == null) {
			throw new IncomeException("No se encontró la cuenta bancaria de origen con ID: "
					+ idCuentaBancariaOrigen);
		}

		long fp = validarFormaPago(cuentaOrigen, formaPago, debitoAutomatico);
		boolean esDebitoAutomatico = (fp == FormaPagoProgramado.DEBITO_AUTOMATICO);

		// La transferencia viaja en el archivo del banco: exige beneficiario y
		// cuenta de destino. El débito automático y el cheque no transfieren nada.
		CuentaBancariaTitular cuentaDestino = null;
		if (fp == FormaPagoProgramado.TRANSFERENCIA) {
			if (egreso.getTitular() == null) {
				throw new IncomeException("El egreso " + idEgreso + " no tiene beneficiario. "
						+ "Para pagarlo por transferencia debe indicar el titular y su cuenta bancaria.");
			}
			if (idCuentaDestinoTitular == null) {
				throw new IncomeException("Debe indicar la cuenta bancaria del beneficiario "
						+ "para incluir el pago en el archivo del banco.");
			}
			cuentaDestino = em.find(CuentaBancariaTitular.class, idCuentaDestinoTitular);
			if (cuentaDestino == null) {
				throw new IncomeException("No se encontró la cuenta bancaria del beneficiario con ID: "
						+ idCuentaDestinoTitular);
			}
			if (cuentaDestino.getTitular() != null
					&& !cuentaDestino.getTitular().getCodigo().equals(egreso.getTitular().getCodigo())) {
				throw new IncomeException("La cuenta bancaria de destino pertenece a otro titular, "
						+ "no al beneficiario del egreso.");
			}
		}

		PagoProgramado pago = new PagoProgramado();
		pago.setEmpresa(egreso.getEmpresa());
		pago.setEgreso(egreso);
		pago.setTitular(egreso.getTitular());
		pago.setCuentaBancaria(cuentaOrigen);
		pago.setCuentaDestino(cuentaDestino);
		pago.setDebitoAutomatico(Long.valueOf(esDebitoAutomatico ? 1 : 0));
		pago.setFormaPago(Long.valueOf(fp));
		pago.setValor(egreso.getValor());
		pago.setFechaProgramada(egreso.getFecha() != null ? egreso.getFecha() : LocalDate.now());
		pago.setObservacion(egreso.getDescripcion());
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());

		if (fp == FormaPagoProgramado.CHEQUE) {
			String nombreBeneficiario = (egreso.getTitular() != null)
					? egreso.getTitular().getNombre() : nvl(pago.getBeneficiarioNombre(), "");
			if (nombreBeneficiario == null || nombreBeneficiario.trim().isEmpty()) {
				throw new IncomeException("Para pagar el egreso con cheque debe indicar el beneficiario "
						+ "(titular del egreso): el cheque se gira a su nombre.");
			}
			Cheque cheque = chequeService.asignarAPago(idCuentaBancariaOrigen, pago.getValor(),
					egreso.getTitular(), nombreBeneficiario, idUsuario);
			pago.setCheque(cheque);
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
			pago.setReferenciaBanco("CHQ-" + cheque.getNumero());
			pago.setFechaRespuesta(pago.getFechaProgramada());
			pago = guardaPagoConCheque(pago, cheque);

			Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
			em.flush();

			System.out.println("✓ Pago de egreso con cheque N° " + cheque.getNumero()
					+ " registrado y contabilizado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje", "Egreso pagado con cheque N° " + cheque.getNumero()
					+ ". El asiento contable y el movimiento bancario fueron generados.");
			resultado.put("pago", pago.getId());
			resultado.put("egreso", idEgreso);
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			resultado.put("numeroCheque", cheque.getNumero());
			resultado.put("asiento", asiento.getNumeroAlterno());
			return resultado;
		}

		if (fp != FormaPagoProgramado.DEBITO_AUTOMATICO) {
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			pago = saveSingle(pago);

			System.out.println("✓ Pago de egreso registrado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje",
					"Pago del egreso registrado. Queda pendiente de incluirse en un archivo de pagos.");
			resultado.put("pago", pago.getId());
			resultado.put("egreso", idEgreso);
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			return resultado;
		}

		// Débito automático: el banco ya debitó la cuenta. Nace confirmado y se
		// contabiliza aquí mismo.
		pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
		pago.setReferenciaBanco((referencia != null && !referencia.trim().isEmpty())
				? referencia.trim() : null);
		pago.setFechaRespuesta(pago.getFechaProgramada());
		pago = saveSingle(pago);
		em.flush();

		Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
		em.flush();

		System.out.println("✓ Pago de egreso por débito automático registrado y contabilizado: id="
				+ pago.getId() + " | asiento=" + asiento.getNumeroAlterno());

		resultado.put("exito", true);
		resultado.put("mensaje", "Egreso pagado por débito automático. El asiento contable "
				+ "y el movimiento bancario fueron generados.");
		resultado.put("pago", pago.getId());
		resultado.put("egreso", idEgreso);
		resultado.put("debitoAutomatico", true);
		resultado.put("formaPago", fp);
		resultado.put("asiento", asiento.getNumeroAlterno());
		return resultado;
	}

	@Override
	public Map<String, Object> registrarPagoDeAnticipo(Long idAnticipo, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia) throws Throwable {
		return registrarPagoDeAnticipo(idAnticipo, idCuentaBancariaOrigen, idCuentaDestinoTitular,
				idUsuario, debitoAutomatico, referencia,
				Long.valueOf(debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO
						: FormaPagoProgramado.TRANSFERENCIA));
	}

	@Override
	public Map<String, Object> registrarPagoDeAnticipo(Long idAnticipo, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia, Long formaPago) throws Throwable {

		System.out.println("=== registrarPagoDeAnticipo | anticipo=" + idAnticipo
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " | formaPago=" + formaPago + " ===");

		Map<String, Object> resultado = new HashMap<>();

		AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
		if (anticipo == null) {
			throw new IncomeException("No se encontró el anticipo a proveedor con ID: " + idAnticipo);
		}
		if (anticipo.getEstado() == null
				|| anticipo.getEstado().intValue() != EstadoAnticipoProveedor.INGRESADO) {
			throw new IncomeException("El anticipo " + idAnticipo + " no está pendiente de pago.");
		}
		if (anticipo.getValor() == null || anticipo.getValor() <= 0) {
			throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
		}
		if (anticipo.getTitular() == null) {
			throw new IncomeException("El anticipo " + idAnticipo + " no tiene proveedor asignado.");
		}
		if (!pagoProgramadoDaoService.selectVigentesByAnticipo(idAnticipo).isEmpty()) {
			throw new IncomeException("El anticipo " + idAnticipo
					+ " ya tiene un pago vigente. Anúlelo o reviértalo antes de registrar otro.");
		}

		CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaBancariaOrigen);
		if (cuentaOrigen == null) {
			throw new IncomeException("No se encontró la cuenta bancaria de origen con ID: "
					+ idCuentaBancariaOrigen);
		}

		long fp = validarFormaPago(cuentaOrigen, formaPago, debitoAutomatico);
		boolean esDebitoAutomatico = (fp == FormaPagoProgramado.DEBITO_AUTOMATICO);

		// La transferencia viaja en el archivo del banco: exige la cuenta del
		// proveedor. El débito automático y el cheque no transfieren nada.
		CuentaBancariaTitular cuentaDestino = null;
		if (fp == FormaPagoProgramado.TRANSFERENCIA) {
			if (idCuentaDestinoTitular == null) {
				throw new IncomeException("Debe indicar la cuenta bancaria del proveedor "
						+ "para incluir el pago en el archivo del banco.");
			}
			cuentaDestino = em.find(CuentaBancariaTitular.class, idCuentaDestinoTitular);
			if (cuentaDestino == null) {
				throw new IncomeException("No se encontró la cuenta bancaria del proveedor con ID: "
						+ idCuentaDestinoTitular);
			}
			if (cuentaDestino.getTitular() != null
					&& !cuentaDestino.getTitular().getCodigo().equals(anticipo.getTitular().getCodigo())) {
				throw new IncomeException("La cuenta bancaria de destino pertenece a otro titular, "
						+ "no al proveedor del anticipo.");
			}
		}

		PagoProgramado pago = new PagoProgramado();
		pago.setEmpresa(anticipo.getEmpresa());
		pago.setAnticipo(anticipo);
		pago.setTitular(anticipo.getTitular());
		pago.setCuentaBancaria(cuentaOrigen);
		pago.setCuentaDestino(cuentaDestino);
		pago.setDebitoAutomatico(Long.valueOf(esDebitoAutomatico ? 1 : 0));
		pago.setFormaPago(Long.valueOf(fp));
		pago.setValor(anticipo.getValor());
		pago.setFechaProgramada(anticipo.getFechaAnticipo() != null
				? anticipo.getFechaAnticipo() : LocalDate.now());
		pago.setObservacion("Anticipo a proveedor: " + anticipo.getTitular().getNombre()
				+ (anticipo.getNumeroDoc() != null ? " | Doc: " + anticipo.getNumeroDoc() : ""));
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());

		if (fp == FormaPagoProgramado.CHEQUE) {
			Cheque cheque = chequeService.asignarAPago(idCuentaBancariaOrigen, pago.getValor(),
					anticipo.getTitular(), anticipo.getTitular().getNombre(), idUsuario);
			pago.setCheque(cheque);
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
			pago.setReferenciaBanco("CHQ-" + cheque.getNumero());
			pago.setFechaRespuesta(pago.getFechaProgramada());
			pago = guardaPagoConCheque(pago, cheque);

			Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
			em.flush();

			System.out.println("✓ Anticipo pagado con cheque N° " + cheque.getNumero()
					+ " y contabilizado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje", "Anticipo pagado con cheque N° " + cheque.getNumero()
					+ ". El asiento contable y el movimiento bancario fueron generados.");
			resultado.put("pago", pago.getId());
			resultado.put("anticipo", idAnticipo);
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			resultado.put("numeroCheque", cheque.getNumero());
			resultado.put("asiento", asiento.getNumeroAlterno());
			return resultado;
		}

		if (fp != FormaPagoProgramado.DEBITO_AUTOMATICO) {
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			pago = saveSingle(pago);

			System.out.println("✓ Pago de anticipo registrado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje",
					"Pago del anticipo registrado. Queda pendiente de incluirse en un archivo de pagos.");
			resultado.put("pago", pago.getId());
			resultado.put("anticipo", idAnticipo);
			resultado.put("debitoAutomatico", false);
			resultado.put("formaPago", fp);
			return resultado;
		}

		// Débito automático: el banco ya debitó la cuenta. Nace confirmado y se
		// contabiliza aquí mismo con el asiento de anticipo.
		pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
		pago.setReferenciaBanco((referencia != null && !referencia.trim().isEmpty())
				? referencia.trim() : null);
		pago.setFechaRespuesta(pago.getFechaProgramada());
		pago = saveSingle(pago);
		em.flush();

		Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
		em.flush();

		System.out.println("✓ Pago de anticipo por débito automático registrado y contabilizado: id="
				+ pago.getId() + " | asiento=" + asiento.getNumeroAlterno());

		resultado.put("exito", true);
		resultado.put("mensaje", "Anticipo pagado por débito automático. El asiento contable "
				+ "y el movimiento bancario fueron generados.");
		resultado.put("pago", pago.getId());
		resultado.put("anticipo", idAnticipo);
		resultado.put("debitoAutomatico", true);
		resultado.put("formaPago", fp);
		resultado.put("asiento", asiento.getNumeroAlterno());
		return resultado;
	}

	@Override
	public Map<String, Object> registrarPagoDeOrigenExterno(String origen, Long idOrigen,
			Long idEmpresa, Long idCuentaBancariaOrigen, Double valor, String fechaProgramada,
			BeneficiarioOcasional beneficiario, List<LineaContablePago> desglose,
			String observacion, Long idUsuario, boolean debitoAutomatico, String referencia)
			throws Throwable {
		return registrarPagoDeOrigenExterno(origen, idOrigen, idEmpresa, idCuentaBancariaOrigen,
				valor, fechaProgramada, beneficiario, desglose, observacion, idUsuario,
				debitoAutomatico, referencia,
				Long.valueOf(debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO
						: FormaPagoProgramado.TRANSFERENCIA));
	}

	@Override
	public Map<String, Object> registrarPagoDeOrigenExterno(String origen, Long idOrigen,
			Long idEmpresa, Long idCuentaBancariaOrigen, Double valor, String fechaProgramada,
			BeneficiarioOcasional beneficiario, List<LineaContablePago> desglose,
			String observacion, Long idUsuario, boolean debitoAutomatico, String referencia,
			Long formaPago) throws Throwable {

		System.out.println("=== registrarPagoDeOrigenExterno | origen=" + origen
				+ " | idOrigen=" + idOrigen + " | valor=" + valor
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " | formaPago=" + formaPago + " ===");

		Map<String, Object> resultado = new HashMap<>();

		// ── Origen: para CXP es una etiqueta opaca. Solo se valida que venga. ─────
		if (origen == null || origen.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el proceso que origina el pago.");
		}
		if (idOrigen == null) {
			throw new IncomeException("Debe indicar el documento que origina el pago.");
		}
		String etiquetaOrigen = origen.trim();

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del pago debe ser mayor a cero.");
		}

		// Un mismo documento origen no puede tener dos órdenes de pago vivas: se
		// duplicaría la salida de dinero.
		if (!pagoProgramadoDaoService.selectVigentesByOrigen(etiquetaOrigen, idOrigen).isEmpty()) {
			throw new IncomeException("El documento " + idOrigen + " de " + etiquetaOrigen
					+ " ya tiene un pago vigente. Anúlelo o reviértalo antes de registrar otro.");
		}

		CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaBancariaOrigen);
		if (cuentaOrigen == null) {
			throw new IncomeException("No se encontró la cuenta bancaria de origen con ID: "
					+ idCuentaBancariaOrigen);
		}

		long fp = validarFormaPago(cuentaOrigen, formaPago, debitoAutomatico);
		boolean esDebitoAutomatico = (fp == FormaPagoProgramado.DEBITO_AUTOMATICO);

		// ── Beneficiario ocasional ────────────────────────────────────────────────
		// No pasa por TSR.TTLR: el beneficiario puede no existir en el maestro de
		// titulares. La transferencia viaja en el archivo del banco, así que exige
		// banco y cuenta; el débito automático y el cheque no transfieren nada.
		if (beneficiario == null) {
			throw new IncomeException("Debe indicar el beneficiario del pago.");
		}
		if (beneficiario.getNombre() == null || beneficiario.getNombre().trim().isEmpty()) {
			throw new IncomeException("Debe indicar el nombre del beneficiario del pago.");
		}
		if (beneficiario.getIdentificacion() == null
				|| beneficiario.getIdentificacion().trim().isEmpty()) {
			throw new IncomeException("Debe indicar la identificación del beneficiario del pago.");
		}
		BancoExterno bancoBeneficiario = null;
		if (fp == FormaPagoProgramado.TRANSFERENCIA) {
			if (beneficiario.getNumeroCuenta() == null
					|| beneficiario.getNumeroCuenta().trim().isEmpty()) {
				throw new IncomeException("Debe indicar la cuenta bancaria del beneficiario "
						+ "para incluir el pago en el archivo del banco.");
			}
			if (beneficiario.getIdBancoExterno() == null) {
				throw new IncomeException("Debe indicar el banco del beneficiario "
						+ "para incluir el pago en el archivo del banco.");
			}
		}
		if (beneficiario.getIdBancoExterno() != null) {
			bancoBeneficiario = em.find(BancoExterno.class, beneficiario.getIdBancoExterno());
			if (bancoBeneficiario == null) {
				throw new IncomeException("No se encontró el banco del beneficiario con ID: "
						+ beneficiario.getIdBancoExterno());
			}
		}

		// ── Desglose contable (OPCIONAL) ──────────────────────────────────────────
		// Un pago SIN desglose es válido: al confirmarse no genera asiento ni movimiento
		// bancario. Es la decisión del 2026-08-24 para los procesos origen cuya
		// parametrización contable todavía no está definida.
		//
		// La marca de "confirmado sin contabilidad" es PGTRASNT IS NULL en un pago
		// CONFIRMADO de origen externo: no hace falta ninguna columna extra.
		//
		// Cuando SÍ viene desglose se valida entero AL REGISTRAR, no al confirmar, para que
		// el error de parametrización salga temprano — mismo criterio que
		// EgresoServiceImpl.validaProducto.
		boolean tieneDesglose = (desglose != null && !desglose.isEmpty());
		double total = redondea(valor);
		List<ProductoPago> productos = new ArrayList<>();

		if (tieneDesglose) {
			BigDecimal sumaDesglose = BigDecimal.ZERO;
			for (LineaContablePago linea : desglose) {
				if (linea == null || linea.getIdProductoPago() == null) {
					throw new IncomeException("Cada línea del desglose contable debe indicar "
							+ "el producto que la clasifica.");
				}
				double valorLinea = redondea(linea.getValor() != null ? linea.getValor() : 0.0);
				if (valorLinea <= 0) {
					throw new IncomeException("El valor de cada línea del desglose contable "
							+ "debe ser mayor a cero.");
				}
				productos.add(validaProductoPago(linea.getIdProductoPago()));
				sumaDesglose = sumaDesglose.add(BigDecimal.valueOf(valorLinea));
			}
			if (Math.abs(sumaDesglose.doubleValue() - total) > TOLERANCIA) {
				throw new IncomeException("El desglose contable suma $"
						+ String.format(Locale.US, "%.2f", sumaDesglose.doubleValue())
						+ " y el pago es de $" + String.format(Locale.US, "%.2f", total)
						+ ". Los dos valores deben coincidir.");
			}
		}

		LocalDate fecha = parseFecha(fechaProgramada);
		boolean confirmaDeInmediato = esDebitoAutomatico || fp == FormaPagoProgramado.CHEQUE;

		// ── Cabecera del pago ─────────────────────────────────────────────────────
		PagoProgramado pago = new PagoProgramado();
		pago.setEmpresa(em.find(Empresa.class, idEmpresa));
		pago.setOrigenExterno(etiquetaOrigen);
		pago.setIdOrigen(idOrigen);
		pago.setCuentaBancaria(cuentaOrigen);
		// Sin titular ni cuentaDestino: el beneficiario no está en el maestro de TSR.
		pago.setBeneficiarioNombre(beneficiario.getNombre().trim());
		pago.setBeneficiarioIdentificacion(beneficiario.getIdentificacion().trim());
		pago.setBeneficiarioBanco(bancoBeneficiario);
		pago.setBeneficiarioTipoCuenta(beneficiario.getTipoCuenta());
		pago.setBeneficiarioCuenta((beneficiario.getNumeroCuenta() != null)
				? beneficiario.getNumeroCuenta().trim() : null);
		pago.setDebitoAutomatico(Long.valueOf(esDebitoAutomatico ? 1 : 0));
		pago.setFormaPago(Long.valueOf(fp));
		pago.setValor(total);
		pago.setFechaProgramada(fecha);
		pago.setObservacion(observacion);
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());
		pago.setEstado(Long.valueOf(confirmaDeInmediato
				? EstadoPagoProgramado.CONFIRMADO : EstadoPagoProgramado.REGISTRADO));

		Cheque cheque = null;
		if (fp == FormaPagoProgramado.CHEQUE) {
			cheque = chequeService.asignarAPago(idCuentaBancariaOrigen, total, null,
					beneficiario.getNombre().trim(), idUsuario);
			pago.setCheque(cheque);
			pago.setReferenciaBanco("CHQ-" + cheque.getNumero());
			pago.setFechaRespuesta(fecha);
			pago = guardaPagoConCheque(pago, cheque);
		} else {
			if (esDebitoAutomatico) {
				pago.setReferenciaBanco((referencia != null && !referencia.trim().isEmpty())
						? referencia.trim() : null);
				pago.setFechaRespuesta(fecha);
			}
			pago = saveSingle(pago);
			em.flush();
		}

		// ── Desglose contable persistido, si vino ─────────────────────────────────
		if (tieneDesglose) {
			for (int i = 0; i < desglose.size(); i++) {
				LineaContablePago linea = desglose.get(i);
				DetallePagoOrigenExterno detalle = new DetallePagoOrigenExterno();
				detalle.setPago(pago);
				detalle.setProducto(productos.get(i));
				detalle.setValor(redondea(linea.getValor()));
				detalle.setConcepto(linea.getConcepto());
				detallePagoOrigenExternoDaoService.save(detalle, null);
			}
			em.flush();
		} else {
			System.out.println("⚠ Pago de origen externo " + pago.getId() + " registrado SIN "
					+ "desglose contable: al confirmarse no generará asiento ni movimiento "
					+ "bancario.");
		}

		resultado.put("exito", true);
		resultado.put("pago", pago.getId());
		resultado.put("origen", etiquetaOrigen);
		resultado.put("idOrigen", idOrigen);
		resultado.put("debitoAutomatico", esDebitoAutomatico);
		resultado.put("formaPago", fp);
		if (cheque != null) {
			resultado.put("numeroCheque", cheque.getNumero());
		}

		if (!confirmaDeInmediato) {
			System.out.println("✓ Pago de origen externo registrado: id=" + pago.getId()
					+ " | origen=" + etiquetaOrigen + " | idOrigen=" + idOrigen);
			resultado.put("mensaje", "Pago registrado. Queda pendiente de incluirse "
					+ "en un archivo de pagos.");
			return resultado;
		}

		// Débito automático o cheque: nace confirmado y se contabiliza aquí mismo —
		// salvo que no tenga desglose, en cuyo caso contabilizarSegunOrigen devuelve
		// null y el pago queda confirmado sin asiento.
		Asiento asiento = contabilizarSegunOrigen(pago, idUsuario);
		pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		if (asiento != null) {
			System.out.println("✓ Pago de origen externo registrado y contabilizado: id=" + pago.getId()
					+ " | asiento=" + asiento.getNumeroAlterno());
			resultado.put("mensaje", (fp == FormaPagoProgramado.CHEQUE)
					? "Pago con cheque N° " + cheque.getNumero() + " registrado. El asiento contable "
							+ "y el movimiento bancario fueron generados."
					: "Pago por débito automático registrado. El asiento contable "
							+ "y el movimiento bancario fueron generados.");
			resultado.put("asiento", asiento.getNumeroAlterno());
		} else {
			System.out.println("✓ Pago de origen externo registrado SIN contabilidad: id=" + pago.getId()
					+ " (no tiene desglose contable)");
			resultado.put("mensaje", "Pago registrado. No se generó asiento "
					+ "contable ni movimiento bancario porque el pago no tiene desglose contable.");
			resultado.put("sinContabilidad", true);
		}
		return resultado;
	}

	@Override
	public List<PagoProgramado> listar(Long idEmpresa, Long estado, Long idTitular) throws Throwable {
		System.out.println("=== listar pagos | empresa=" + idEmpresa + " | estado=" + estado + " ===");
		return pagoProgramadoDaoService.selectByEmpresaEstado(idEmpresa, estado, idTitular);
	}

	// =====================================================================
	// Generación del lote y del archivo para el banco
	// =====================================================================

	@Override
	public Map<String, Object> generarLote(List<Long> idsPagos, Long idCuentaOrigen, Long idEmpresa,
			Long idUsuario) throws Throwable {

		System.out.println("=== generarLote | pagos=" + (idsPagos != null ? idsPagos.size() : 0)
				+ " | cuentaOrigen=" + idCuentaOrigen + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (idsPagos == null || idsPagos.isEmpty()) {
			throw new IncomeException("Debe seleccionar al menos un pago para generar el archivo.");
		}

		CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaOrigen);
		if (cuentaOrigen == null) {
			throw new IncomeException("No se encontró la cuenta bancaria de origen con ID: "
					+ idCuentaOrigen);
		}

		// Se releen los pagos aquí dentro para validar su estado real y evitar
		// que un mismo pago entre en dos lotes.
		List<PagoProgramado> pagos = pagoProgramadoDaoService.selectByIds(idsPagos);
		if (pagos.size() != idsPagos.size()) {
			throw new IncomeException("Alguno de los pagos seleccionados ya no existe.");
		}

		double total = 0.0;
		for (PagoProgramado pago : pagos) {
			if (esDebitoAutomatico(pago)) {
				throw new IncomeException("El pago " + pago.getId() + " es un débito automático: "
						+ "el banco ya lo ejecutó y no debe enviarse en el archivo de pagos.");
			}
			if (pago.getCheque() != null) {
				throw new IncomeException("El pago " + pago.getId() + " se pagó con cheque: "
						+ "el cheque ya se giró y no debe enviarse en el archivo de pagos.");
			}
			if (pago.getEstado() == null
					|| pago.getEstado().intValue() != EstadoPagoProgramado.REGISTRADO) {
				throw new IncomeException("El pago " + pago.getId() + " ya no está disponible: "
						+ "su estado actual es " + descripcionEstado(pago.getEstado())
						+ ". Actualice el listado y vuelva a intentarlo.");
			}
			if (pago.getCuentaBancaria() == null
					|| !pago.getCuentaBancaria().getCodigo().equals(idCuentaOrigen)) {
				throw new IncomeException("El pago " + pago.getId() + " se registró desde otra cuenta "
						+ "bancaria. Un archivo solo puede contener pagos de la misma cuenta de origen.");
			}
			total += (pago.getValor() != null) ? pago.getValor() : 0.0;
		}

		// 1. Crear la cabecera del lote
		LotePago lote = new LotePago();
		lote.setEmpresa(em.find(Empresa.class, idEmpresa));
		lote.setCuentaBancaria(cuentaOrigen);
		lote.setFechaGeneracion(LocalDate.now());
		lote.setValorTotal(total);
		lote.setNumeroPagos(Long.valueOf(pagos.size()));
		lote.setEstado(Long.valueOf(EstadoLotePago.GENERADO));
		lote.setUsuario(em.find(Usuario.class, idUsuario));
		lote.setFechaRegistro(LocalDateTime.now());
		lote = lotePagoDaoService.save(lote, null);
		em.flush();

		// 2. Generar el contenido del archivo
		FormateadorArchivoBanco formateador = obtenerFormateador(cuentaOrigen);
		String contenido = formateador.generarContenido(lote, pagos);
		String nombreArchivo = formateador.nombreArchivo(lote);

		// 3. Guardar el archivo en disco
		String path = null;
		try {
			path = fileService.uploadFileToPath(
					new ByteArrayInputStream(contenido.getBytes("UTF-8")),
					nombreArchivo, RUTA_ARCHIVOS_BANCO);
		} catch (Exception e) {
			// No se interrumpe: el archivo igual se devuelve para descargar.
			System.err.println("⚠ No se pudo guardar el archivo del lote en disco: " + e.getMessage());
			resultado.put("advertenciaArchivo",
					"El archivo se generó pero no se pudo guardar en el servidor: " + e.getMessage());
		}
		lote.setNombreArchivo(nombreArchivo);
		lote.setPath(path);
		lote = lotePagoDaoService.save(lote, lote.getId());

		// 4. Marcar los pagos como incluidos en el archivo
		for (PagoProgramado pago : pagos) {
			pago.setLote(lote);
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
			pagoProgramadoDaoService.save(pago, pago.getId());
		}
		em.flush();

		System.out.println("✓ Lote generado: id=" + lote.getId() + " | pagos=" + pagos.size()
				+ " | total=" + total);

		resultado.put("exito", true);
		resultado.put("mensaje", "Archivo de pagos generado con " + pagos.size() + " transferencia(s).");
		resultado.put("idLote", lote.getId());
		resultado.put("nombreArchivo", nombreArchivo);
		resultado.put("contenido", contenido);
		resultado.put("valorTotal", total);
		resultado.put("numeroPagos", pagos.size());
		return resultado;
	}

	@Override
	public Map<String, Object> obtenerArchivoLote(Long idLote) throws Throwable {
		System.out.println("=== obtenerArchivoLote | lote=" + idLote + " ===");

		LotePago lote = em.find(LotePago.class, idLote);
		if (lote == null) {
			throw new IncomeException("No se encontró el lote de pagos con ID: " + idLote);
		}

		List<PagoProgramado> pagos = pagoProgramadoDaoService.selectByLote(idLote);
		FormateadorArchivoBanco formateador = obtenerFormateador(lote.getCuentaBancaria());

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idLote", idLote);
		resultado.put("nombreArchivo", (lote.getNombreArchivo() != null)
				? lote.getNombreArchivo() : formateador.nombreArchivo(lote));
		resultado.put("contenido", formateador.generarContenido(lote, pagos));
		return resultado;
	}

	// =====================================================================
	// Respuesta del banco
	// =====================================================================

	@Override
	public Map<String, Object> procesarRespuestaBanco(Long idLote, byte[] archivoRespuesta,
			Long idUsuario) throws Throwable {

		System.out.println("=== procesarRespuestaBanco | lote=" + idLote + " ===");

		Map<String, Object> resultado = new HashMap<>();

		LotePago lote = em.find(LotePago.class, idLote);
		if (lote == null) {
			throw new IncomeException("No se encontró el lote de pagos con ID: " + idLote);
		}
		if (lote.getEstado() != null && lote.getEstado().intValue() == EstadoLotePago.ANULADO) {
			throw new IncomeException("El lote " + idLote + " está anulado.");
		}

		// 1. Leer el archivo de respuesta
		LectorRespuestaBanco lector = obtenerLectorRespuesta(lote.getCuentaBancaria());
		List<RespuestaPagoBanco> respuestas = lector.leer(archivoRespuesta, lote);

		// 2. Procesar cada pago reportado
		int confirmados = 0;
		int rechazados  = 0;
		List<String> errores = new ArrayList<>();

		for (RespuestaPagoBanco respuesta : respuestas) {

			PagoProgramado pago = em.find(PagoProgramado.class, respuesta.getIdPago());
			if (pago == null) {
				errores.add("El pago " + respuesta.getIdPago() + " del archivo no existe.");
				continue;
			}
			if (pago.getLote() == null || !pago.getLote().getId().equals(idLote)) {
				errores.add("El pago " + respuesta.getIdPago() + " no pertenece a este lote.");
				continue;
			}
			if (pago.getEstado() == null
					|| pago.getEstado().intValue() != EstadoPagoProgramado.EN_ARCHIVO) {
				errores.add("El pago " + respuesta.getIdPago() + " ya fue procesado (estado "
						+ descripcionEstado(pago.getEstado()) + ").");
				continue;
			}

			pago.setFechaRespuesta(LocalDate.now());

			if (respuesta.isConfirmado()) {
				try {
					pago.setReferenciaBanco(respuesta.getReferencia());
					// Solo aquí se genera contabilidad y movimiento bancario. El
					// asiento depende del proceso que originó el pago; el retorno
					// se descarta a propósito, la confirmación no depende de él.
					contabilizarSegunOrigen(pago, idUsuario);
					pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
					pagoProgramadoDaoService.save(pago, pago.getId());
					confirmados++;
				} catch (Throwable t) {
					errores.add("Pago " + pago.getId() + ": no se pudo registrar el pago confirmado - "
							+ t.getMessage());
					System.err.println("⚠ Error aplicando el pago " + pago.getId() + ": " + t.getMessage());
				}
			} else {
				pago.setEstado(Long.valueOf(EstadoPagoProgramado.RECHAZADO));
				pago.setMotivo(respuesta.getMotivo() != null
						? respuesta.getMotivo() : "Rechazado por la entidad financiera");
				try {
					anularMovimientoCajaChicaSiAplica(pago, "PAGO RECHAZADO POR EL BANCO: "
							+ nvl(respuesta.getMotivo(), "sin motivo informado"));
				} catch (RuntimeException e) {
					// No abortar el procesamiento del resto del archivo por un
					// movimiento de caja chica que no se pudo anular (p.ej. ya
					// quedó en un cierre confirmado); el pago igual se marca
					// RECHAZADO, sólo queda el movimiento sin anular.
					System.err.println("⚠ No se pudo anular el movimiento de caja chica del pago "
							+ pago.getId() + ": " + e.getMessage());
				}
				pagoProgramadoDaoService.save(pago, pago.getId());
				rechazados++;
			}
		}

		// 3. Cerrar el lote
		lote.setEstado(Long.valueOf(EstadoLotePago.RESPUESTA_PROCESADA));
		lotePagoDaoService.save(lote, lote.getId());
		em.flush();

		System.out.println("✓ Respuesta procesada | confirmados=" + confirmados
				+ " | rechazados=" + rechazados + " | errores=" + errores.size());

		resultado.put("exito", true);
		resultado.put("mensaje", "Respuesta procesada: " + confirmados + " confirmado(s), "
				+ rechazados + " rechazado(s).");
		resultado.put("confirmados", confirmados);
		resultado.put("rechazados", rechazados);
		if (!errores.isEmpty()) {
			resultado.put("errores", errores);
		}
		return resultado;
	}

	// =====================================================================
	// Confirmación manual
	// =====================================================================

	@Override
	public Map<String, Object> confirmarPagosManual(List<Long> idsPagos, String referencia,
			String fechaPago, String observacion, Long idUsuario) throws Throwable {

		System.out.println("=== confirmarPagosManual | pagos=" + idsPagos + " ===");

		if (idsPagos == null || idsPagos.isEmpty()) {
			throw new IncomeException("Debe seleccionar al menos un pago para confirmar.");
		}

		LocalDate fecha = parseFecha(fechaPago);
		String ref  = (referencia != null && !referencia.trim().isEmpty())
				? referencia.trim() : null;
		String nota = (observacion != null && !observacion.trim().isEmpty())
				? observacion.trim() : null;

		int confirmados = 0;
		List<String> errores = new ArrayList<>();
		List<Long> lotesTocados = new ArrayList<>();

		for (Long idPago : idsPagos) {

			PagoProgramado pago = em.find(PagoProgramado.class, idPago);
			if (pago == null) {
				errores.add("No se encontró el pago " + idPago + ".");
				continue;
			}

			// Solo se confirma lo que todavía está esperando al banco. Un pago
			// ya confirmado tiene contabilidad hecha y volver a aplicarlo la
			// duplicaría; uno rechazado o anulado ya cerró su ciclo.
			int estado = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
			if (estado != EstadoPagoProgramado.REGISTRADO
					&& estado != EstadoPagoProgramado.EN_ARCHIVO) {
				errores.add("El pago " + idPago + " está " + descripcionEstado(pago.getEstado())
						+ " y no se puede confirmar.");
				continue;
			}

			try {
				pago.setFechaRespuesta(fecha);
				if (ref != null) {
					pago.setReferenciaBanco(ref);
				}
				if (nota != null) {
					pago.setObservacion((pago.getObservacion() != null
							&& !pago.getObservacion().trim().isEmpty())
									? pago.getObservacion() + " | " + nota : nota);
				}

				// Mismo camino contable que la respuesta del banco: el asiento
				// depende del proceso que originó el pago; el retorno se
				// descarta a propósito, la confirmación no depende de él.
				contabilizarSegunOrigen(pago, idUsuario);

				pago.setEstado(Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
				pagoProgramadoDaoService.save(pago, pago.getId());
				confirmados++;

				if (pago.getLote() != null && !lotesTocados.contains(pago.getLote().getId())) {
					lotesTocados.add(pago.getLote().getId());
				}
			} catch (Throwable t) {
				errores.add("Pago " + idPago + ": no se pudo confirmar - " + t.getMessage());
				System.err.println("⚠ Error confirmando manualmente el pago " + idPago + ": "
						+ t.getMessage());
			}
		}

		em.flush();

		// Un lote sin pagos pendientes ya no espera respuesta del banco.
		for (Long idLote : lotesTocados) {
			cierraLoteSiNoQuedanPendientes(idLote);
		}

		System.out.println("✓ Confirmación manual | confirmados=" + confirmados
				+ " | errores=" + errores.size());

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", confirmados > 0);
		resultado.put("mensaje", confirmados + " pago(s) confirmado(s) manualmente"
				+ (errores.isEmpty() ? "." : ", " + errores.size() + " con novedad."));
		resultado.put("confirmados", confirmados);
		if (!errores.isEmpty()) {
			resultado.put("errores", errores);
		}
		return resultado;
	}

	/**
	 * Cierra el lote cuando ninguno de sus pagos sigue esperando respuesta.
	 * @param idLote     : Id del lote a revisar
	 * @throws Throwable : Excepcion
	 */
	private void cierraLoteSiNoQuedanPendientes(Long idLote) throws Throwable {
		LotePago lote = em.find(LotePago.class, idLote);
		if (lote == null || (lote.getEstado() != null
				&& lote.getEstado().intValue() != EstadoLotePago.GENERADO)) {
			return;
		}
		for (PagoProgramado pago : pagoProgramadoDaoService.selectByLote(idLote)) {
			if (pago.getEstado() != null
					&& pago.getEstado().intValue() == EstadoPagoProgramado.EN_ARCHIVO) {
				return;
			}
		}
		lote.setEstado(Long.valueOf(EstadoLotePago.RESPUESTA_PROCESADA));
		lotePagoDaoService.save(lote, lote.getId());
		em.flush();
	}

	// =====================================================================
	// Anulación y reversión
	// =====================================================================

	@Override
	public Map<String, Object> anularPago(Long idPago, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== anularPago | pago=" + idPago + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}

		PagoProgramado pago = em.find(PagoProgramado.class, idPago);
		if (pago == null) {
			throw new IncomeException("No se encontró el pago con ID: " + idPago);
		}

		int estado = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
		if (estado == EstadoPagoProgramado.ANULADO) {
			throw new IncomeException("El pago " + idPago + " ya está anulado.");
		}
		// Defensivo: un pago con cheque nace CONFIRMADO, así que nunca debería
		// llegar aquí en estado REGISTRADO; el check de estado de abajo ya lo
		// bloquearía, pero el mensaje específico es más claro para el usuario.
		// Va después del check de ANULADO para que un pago ya reversado (cuyo
		// cheque quedó anulado, pero el pago sigue con la referencia al cheque)
		// responda "ya está anulado" y no el mensaje de cheque.
		if (pago.getCheque() != null) {
			throw new IncomeException("El pago " + idPago + " se pagó con cheque y nace confirmado: "
					+ "use la reversión (pgtr/revertirConfirmado) en lugar de la anulación.");
		}
		if (estado == EstadoPagoProgramado.CONFIRMADO) {
			throw new IncomeException("El pago " + idPago
					+ (esDebitoAutomatico(pago)
							? " es un débito automático ya ejecutado por el banco y tiene "
							: " ya fue confirmado por el banco y tiene ")
					+ "contabilidad generada. Use la reversión en lugar de la anulación.");
		}

		pago.setEstado(Long.valueOf(EstadoPagoProgramado.ANULADO));
		pago.setMotivo(motivo.trim());
		anularMovimientoCajaChicaSiAplica(pago, "PAGO ANULADO: " + motivo.trim());
		pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Pago anulado correctamente.");
		resultado.put("pago", idPago);
		return resultado;
	}

	@Override
	public Map<String, Object> revertirPagoConfirmado(Long idPago, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== revertirPagoConfirmado | pago=" + idPago + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la reversión.");
		}

		PagoProgramado pago = em.find(PagoProgramado.class, idPago);
		if (pago == null) {
			throw new IncomeException("No se encontró el pago con ID: " + idPago);
		}
		if (pago.getEstado() == null
				|| pago.getEstado().intValue() != EstadoPagoProgramado.CONFIRMADO) {
			throw new IncomeException("Solo se puede revertir un pago confirmado. Estado actual: "
					+ descripcionEstado(pago.getEstado()));
		}

		Map<String, Object> resultado = new HashMap<>();

		if (pago.getOrigenExterno() != null) {
			// Pago de un documento originado en otro módulo: se anula el movimiento
			// bancario y el asiento que cuelga del propio pago. El documento origen
			// NO se toca: CXP no lo conoce. Es el módulo origen el que consulta el
			// estado del pago y reacciona (no hay callback desde CXP).
			revertirContabilidadOrigenExterno(pago, motivo.trim());
		} else if (pago.getAnticipo() != null) {
			// Pago de un anticipo a proveedor: se anula el movimiento bancario
			// y el asiento de anticipo, se descuenta el saldo de anticipos del
			// proveedor, y el anticipo vuelve a quedar Ingresado.
			revertirContabilidadAnticipoPago(pago, motivo.trim());
		} else if (pago.getEgreso() != null) {
			// Pago de un egreso de tesorería: no hay aplicación que reversar.
			// Se anula el asiento y el movimiento bancario, y el egreso vuelve
			// a quedar pendiente de pago.
			revertirContabilidadEgreso(pago, motivo.trim());
		} else if (pago.getAplicacion() != null) {
			// Reversa la aplicación: devuelve el saldo a la factura, anula el
			// asiento y el movimiento bancario.
			Map<String, Object> reversion = aplicacionPagoCxpService.revertirAplicacion(
					pago.getAplicacion().getId(), motivo.trim(), idUsuario);
			resultado.putAll(reversion);
		}

		// El cheque girado se anula por el reverso: no se reutiliza.
		if (pago.getCheque() != null) {
			chequeService.anularPorReverso(pago.getCheque().getCodigo());
		}

		// Una transferencia reversada vuelve a seguimiento como rechazada, por si
		// hay que reprogramarla. El débito automático y el cheque no se
		// reprograman: si se reversan es porque se registraron mal, así que
		// quedan anulados.
		boolean debitoAutomatico = esDebitoAutomatico(pago);
		boolean pagoConCheque = pago.getCheque() != null;
		pago.setEstado(Long.valueOf((debitoAutomatico || pagoConCheque)
				? EstadoPagoProgramado.ANULADO : EstadoPagoProgramado.RECHAZADO));
		pago.setMotivo("REVERSADO: " + motivo.trim());
		pago.setAplicacion(null);
		pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		resultado.put("exito", true);
		resultado.put("mensaje", pagoConCheque
				? "Pago con cheque reversado. El cheque queda anulado y el pago anulado."
				: debitoAutomatico
						? "Débito automático reversado. El pago queda anulado."
						: "Pago reversado. Queda en seguimiento como rechazado.");
		resultado.put("pago", idPago);
		return resultado;
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Devuelve el formateador del archivo según la cuenta bancaria de origen.
	 * PENDIENTE: cuando existan formatos por banco, elegir aquí la implementación
	 * que corresponda a partir del banco de la cuenta.
	 * @param cuentaOrigen : Cuenta bancaria propia desde la que se paga
	 * @return             : Formateador a usar
	 */
	private FormateadorArchivoBanco obtenerFormateador(CuentaBancaria cuentaOrigen) {
		return new FormateadorArchivoBancoPlanoImpl();
	}

	/**
	 * Devuelve el lector del archivo de respuesta según la cuenta bancaria.
	 * PENDIENTE: igual que el formateador, se elegirá por banco cuando existan
	 * varios formatos.
	 * @param cuentaOrigen : Cuenta bancaria propia desde la que se pagó
	 * @return             : Lector a usar
	 */
	private LectorRespuestaBanco obtenerLectorRespuesta(CuentaBancaria cuentaOrigen) {
		return new LectorRespuestaBancoExcelImpl();
	}

	/**
	 * Valida que el valor a pagar quepa en el saldo pendiente de la factura,
	 * descontando lo ya comprometido en otros pagos vigentes.
	 * @param factura   : Factura de compra
	 * @param valor     : Valor que se pretende pagar
	 * @param idPagoEx  : Id de pago a excluir del cálculo (null si no aplica)
	 * @throws Throwable : Excepcion si el valor supera lo disponible
	 */
	private void validaValorContraSaldo(FacturaCompra factura, Double valor, Long idPagoEx)
			throws Throwable {

		Map<String, Object> saldos = aplicacionPagoCxpService.saldoFactura(factura.getId());
		double saldoPendiente = ((Number) saldos.get("saldoPendiente")).doubleValue();

		// Lo ya comprometido en pagos registrados o enviados al banco todavía no
		// figura como aplicado, pero no se puede volver a comprometer.
		double comprometido = 0.0;
		List<PagoProgramado> vigentes =
				pagoProgramadoDaoService.selectVigentesByFactura(factura.getId());
		for (PagoProgramado vigente : vigentes) {
			if (idPagoEx != null && idPagoEx.equals(vigente.getId())) {
				continue;
			}
			// Los confirmados ya están reflejados en el saldo por su aplicación.
			if (vigente.getEstado() != null
					&& vigente.getEstado().intValue() != EstadoPagoProgramado.CONFIRMADO) {
				comprometido += (vigente.getValor() != null) ? vigente.getValor() : 0.0;
			}
		}

		double disponible = saldoPendiente - comprometido;
		if (valor > disponible + TOLERANCIA) {
			throw new IncomeException("El valor a pagar ($"
					+ String.format(java.util.Locale.US, "%.2f", valor)
					+ ") supera lo disponible de la factura N° " + factura.getNumero()
					+ " ($" + String.format(java.util.Locale.US, "%.2f", disponible)
					+ "). Saldo pendiente: $"
					+ String.format(java.util.Locale.US, "%.2f", saldoPendiente)
					+ " | comprometido en otros pagos: $"
					+ String.format(java.util.Locale.US, "%.2f", comprometido) + ".");
		}
	}

	/**
	 * Indica si el pago se realizó por débito automático del banco.
	 * @param pago : Pago programado
	 * @return     : true si es débito automático
	 */
	private boolean esDebitoAutomatico(PagoProgramado pago) {
		return pago.getDebitoAutomatico() != null && pago.getDebitoAutomatico().intValue() == 1;
	}

	/**
	 * Valida la forma de pago contra la cuenta de origen y normaliza su
	 * coherencia con el flag de débito automático — son la misma información
	 * contada dos veces, así que si vienen en desacuerdo se ajusta la forma de
	 * pago en vez de rechazar el registro (un formulario que mande el combo
	 * con su valor por defecto junto al check de débito automático no debe
	 * romper un flujo que hoy funciona). El llamador debe usar SIEMPRE el
	 * valor devuelto — no el parámetro {@code debitoAutomatico} de entrada —
	 * para decidir el resto del flujo (estado del pago, campos a exigir, etc.).
	 * @param cuentaOrigen     : Cuenta bancaria de origen
	 * @param formaPago        : Forma de pago indicada (puede ser null)
	 * @param debitoAutomatico : true si el banco ya debitó la cuenta
	 * @return                 : Forma de pago efectiva y normalizada (ver FormaPagoProgramado)
	 * @throws Throwable       : Excepcion si la forma de pago es inválida o realmente incompatible
	 */
	private long validarFormaPago(CuentaBancaria cuentaOrigen, Long formaPago,
			boolean debitoAutomatico) throws Throwable {

		long fp = (formaPago != null) ? formaPago.longValue()
				: (debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO
						: FormaPagoProgramado.TRANSFERENCIA);

		if (fp != FormaPagoProgramado.EFECTIVO && fp != FormaPagoProgramado.TRANSFERENCIA
				&& fp != FormaPagoProgramado.CHEQUE && fp != FormaPagoProgramado.DEBITO_AUTOMATICO) {
			throw new IncomeException("Forma de pago inválida: " + fp
					+ ". Use 2=Transferencia, 3=Cheque o 4=Débito automático.");
		}
		if (fp == FormaPagoProgramado.EFECTIVO) {
			throw new IncomeException("La forma de pago Efectivo aún no está soportada.");
		}
		// Cheque y débito automático sí son incompatibles de verdad: un pago no
		// puede a la vez requerir chequera (formaPago=3) y no transferir nada
		// porque el banco ya lo debitó por convenio (debitoAutomatico=true).
		if (fp == FormaPagoProgramado.CHEQUE && debitoAutomatico) {
			throw new IncomeException("Un pago con cheque no puede ser además débito automático.");
		}
		if (debitoAutomatico && fp != FormaPagoProgramado.DEBITO_AUTOMATICO) {
			System.out.println("⚠ formaPago=" + fp + " normalizado a Débito automático (4): "
					+ "debitoAutomatico=true.");
			fp = FormaPagoProgramado.DEBITO_AUTOMATICO;
		} else if (!debitoAutomatico && fp == FormaPagoProgramado.DEBITO_AUTOMATICO) {
			System.out.println("⚠ formaPago=4 (Débito automático) recibido con debitoAutomatico=false: "
					+ "se trata como débito automático.");
		}
		if (fp == FormaPagoProgramado.CHEQUE
				&& (cuentaOrigen.getManejaChequera() == null
						|| cuentaOrigen.getManejaChequera().intValue() != 1)) {
			throw new IncomeException("La cuenta bancaria '" + cuentaOrigen.getNumeroCuenta()
					+ "' no maneja chequeras. Actívela en Tesorería → Cuentas bancarias "
					+ "para pagar con cheque.");
		}
		return fp;
	}

	/**
	 * Guarda el pago ya asignado a un cheque y hace flush de inmediato para
	 * forzar la validación del índice único {@code UQ_PGTR_DTCH} (PGS.PGTR.PGTRDTCH).
	 * Es la red de seguridad final contra la condición de carrera de dos
	 * usuarios tomando el mismo cheque a la vez: el lock pesimista de
	 * {@code ChequeDaoServiceImpl.selectMinChequeActivoPorCuenta} ya debería
	 * evitarlo, pero si dos transacciones lo pasan igual (por ejemplo, aislamiento
	 * READ_COMMITTED sin ver el lock a tiempo) la constraint es quien lo detiene.
	 * @param pago   : Pago con el cheque ya asignado (PGTRDTCH seteado)
	 * @param cheque : Cheque asignado, para el mensaje de error
	 * @return       : Pago guardado
	 * @throws Throwable : IncomeException con mensaje accionable si el cheque ya fue tomado
	 */
	private PagoProgramado guardaPagoConCheque(PagoProgramado pago, Cheque cheque) throws Throwable {
		try {
			pago = saveSingle(pago);
			em.flush();
		} catch (jakarta.persistence.PersistenceException e) {
			// No toda PersistenceException es la carrera del cheque: una columna
			// que falta porque no se corrió el DDL, una FK rota, cualquier otro
			// ORA, todo caía aquí con el mismo mensaje engañoso. Se imprime el
			// stack y solo se traduce cuando el texto de la causa apunta
			// específicamente al índice único o a un ORA-00001.
			e.printStackTrace();
			String txt = String.valueOf(e.getMessage())
					+ String.valueOf((e.getCause() != null) ? e.getCause().getMessage() : "");
			if (txt.toUpperCase().contains("UQ_PGTR_DTCH") || txt.contains("ORA-00001")) {
				throw new IncomeException("El cheque N° " + cheque.getNumero()
						+ " fue tomado por otro usuario, intente nuevamente.");
			}
			throw e;
		}
		return pago;
	}

	/**
	 * Genera la contabilidad y el movimiento bancario del pago según el
	 * documento que lo originó: es el único punto que decide el switch entre
	 * factura, egreso, anticipo y origen externo. Lo llaman el registro con
	 * débito automático, el registro con cheque, procesarRespuestaBanco y
	 * confirmarPagosManual. Si el pago se pagó con cheque, además anexa la
	 * nota del cheque a la línea HABER (banco) del asiento generado.
	 * @param pago      : Pago confirmado (o a punto de confirmarse)
	 * @param idUsuario : Id del usuario que registra o procesa
	 * @return          : Asiento generado, o null si el pago de origen externo no tiene desglose
	 * @throws Throwable: Excepcion
	 */
	private Asiento contabilizarSegunOrigen(PagoProgramado pago, Long idUsuario) throws Throwable {
		Asiento asiento;
		if (pago.getOrigenExterno() != null) {
			// Documento de origen en otro módulo: el asiento se arma con el
			// desglose de PGS.DPGT y cuelga del propio pago. Devuelve null si
			// el pago no tiene desglose.
			asiento = contabilizarPagoOrigenExterno(pago, idUsuario);
		} else if (pago.getAnticipo() != null) {
			asiento = contabilizarPagoAnticipo(pago, idUsuario);
		} else if (pago.getEgreso() != null) {
			// Pago de un egreso de tesorería: asiento contra la cuenta del
			// grupo del producto, sin aplicación.
			asiento = contabilizarPagoEgreso(pago, idUsuario);
		} else {
			AplicacionPagoCxp aplicacion =
					aplicacionPagoCxpService.aplicarPagoTransferencia(pago, idUsuario);
			pago.setAplicacion(aplicacion);
			asiento = aplicacion.getAsiento();
		}
		if (pago.getCheque() != null && asiento != null) {
			anexaNotaChequeEnHaber(asiento, pago);
		}
		return asiento;
	}

	/**
	 * Anexa " | Cheque N° {numero}" a la descripción de la línea HABER del
	 * banco (no a cualquier línea con valorHaber &gt; 0: los cuatro caminos
	 * contables de cheque generan una sola línea HABER hoy, pero acotar por la
	 * cuenta contable de la cuenta bancaria del pago evita que un asiento con
	 * más de un HABER termine anotando una línea que no es la del banco). No
	 * interrumpe el flujo si algo falla: es una mejora de la glosa, no una
	 * condición del asiento.
	 * @param asiento : Asiento ya generado
	 * @param pago    : Pago con el cheque y la cuenta bancaria de origen
	 */
	private void anexaNotaChequeEnHaber(Asiento asiento, PagoProgramado pago) {
		try {
			PlanCuenta planCuentaBanco = (pago.getCuentaBancaria() != null)
					? pago.getCuentaBancaria().getPlanCuenta() : null;
			if (planCuentaBanco == null) {
				return;
			}
			@SuppressWarnings("unchecked")
			List<DetalleAsiento> lineas = em.createQuery(
					"select d from DetalleAsiento d where d.asiento.codigo = :idAsiento "
					+ "and d.valorHaber > 0 and d.planCuenta.codigo = :idCuentaBanco")
					.setParameter("idAsiento", asiento.getCodigo())
					.setParameter("idCuentaBanco", planCuentaBanco.getCodigo())
					.getResultList();
			String nota = " | Cheque N° " + pago.getCheque().getNumero();
			for (DetalleAsiento linea : lineas) {
				// AsientoContableServiceImpl.generarAsientoPagoTransferenciaCxp arma la
				// línea HABER como "Transferencia a proveedor: ..." sin distinguir cheque;
				// se corrige aquí en vez de tocar ese método (lo usan otros orígenes).
				String descripcion = nvl(linea.getDescripcion(), "")
						.replace("Transferencia a proveedor:", "Cheque a proveedor:")
						+ nota;
				// CNT.DTAS.DTASDSCR es VARCHAR2(200): sin truncar, una razón social
				// larga produce ORA-12899 recién en el flush posterior, donde este
				// catch ya no está para atraparlo y tumba la transacción del pago.
				if (descripcion.length() > 200) {
					descripcion = descripcion.substring(0, 200);
				}
				linea.setDescripcion(descripcion);
				em.merge(linea);
			}
		} catch (Exception e) {
			System.err.println("⚠ No se pudo anexar la nota de cheque a la línea HABER del asiento "
					+ asiento.getCodigo() + ": " + e.getMessage());
		}
	}

	/**
	 * Contabiliza el pago de un egreso de tesorería: genera el asiento
	 * (DEBE cuenta del grupo del producto / HABER banco), el movimiento
	 * bancario de egreso, y deja el egreso como Pagado con su asiento.
	 * @param pago      : Pago del egreso ya ejecutado por el banco
	 * @param idUsuario : Id del usuario que registra o procesa
	 * @return          : Asiento generado
	 * @throws Throwable : Excepcion
	 */
	private Asiento contabilizarPagoEgreso(PagoProgramado pago, Long idUsuario) throws Throwable {

		Egreso egreso = pago.getEgreso();
		Long idEmpresa = (pago.getEmpresa() != null) ? pago.getEmpresa().getCodigo() : null;
		Long idCuentaBancaria = (pago.getCuentaBancaria() != null)
				? pago.getCuentaBancaria().getCodigo() : null;
		LocalDate fecha = (pago.getFechaRespuesta() != null)
				? pago.getFechaRespuesta() : LocalDate.now();
		boolean debitoAutomatico = esDebitoAutomatico(pago);
		Cheque cheque = pago.getCheque();
		String notaCheque = (cheque != null)
				? " | Cheque N° " + cheque.getNumero() + " Cta " + pago.getCuentaBancaria().getNumeroCuenta()
				: "";

		String observacionAsiento = "Pago egreso tesorería"
				+ (cheque != null ? " (cheque)" : debitoAutomatico ? " (débito automático)" : " (transferencia)")
				+ " | Concepto: " + egreso.getDescripcion()
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), "")
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", pago.getValor())
				+ notaCheque;

		// 1. Asiento contable del egreso
		Asiento asiento = asientoContableService.generarAsientoEgresoTesoreria(
				egreso.getProducto().getId(), egreso.getDescripcion(), pago.getValor(),
				idCuentaBancaria, idEmpresa, TipoAsientos.EGRESO_TESORERIA, fecha,
				observacionAsiento, usuarioNombre(idUsuario));

		// 2. Movimiento bancario de egreso (mismo criterio que los pagos de facturas)
		int tipoMovimiento = (cheque != null)
				? TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS
				: TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO;
		com.saa.model.tsr.MovimientoBanco mov = movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Egreso tesorería: " + egreso.getDescripcion()
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ (cheque != null ? " | Cheque N° " + cheque.getNumero() : " | Ref: " + nvl(pago.getReferenciaBanco(), "")),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				tipoMovimiento, OrigenMovimientoConciliacion.PAGOS);
		if (cheque != null) {
			mov.setCheque(cheque);
			mov.setNumeroCheque(cheque.getNumero());
			movimientoBancoService.saveSingle(mov);
		}

		// 3. El egreso queda pagado, con su asiento vinculado
		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PAGADO));
		egreso.setAsiento(asiento);
		em.merge(egreso);

		System.out.println("✓ Egreso " + egreso.getId() + " pagado y contabilizado"
				+ " | asiento=" + asiento.getNumeroAlterno());
		return asiento;
	}

	/**
	 * Reversa la contabilidad del pago de un egreso: anula el movimiento
	 * bancario y el asiento, y el egreso vuelve a Pendiente de pago.
	 * @param pago   : Pago confirmado del egreso
	 * @param motivo : Motivo de la reversión
	 * @throws Throwable : Excepcion
	 */
	private void revertirContabilidadEgreso(PagoProgramado pago, String motivo) throws Throwable {

		Egreso egreso = pago.getEgreso();
		Long idAsiento = (egreso.getAsiento() != null) ? egreso.getAsiento().getCodigo() : null;

		if (idAsiento != null) {
			try {
				movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
						Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
			} catch (Exception e) {
				System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
						+ idAsiento + ": " + e.getMessage());
			}
			try {
				asientoService.anulaAsiento(idAsiento);
				System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
			} catch (Throwable e) {
				System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": " + e.getMessage());
			}
		}

		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PENDIENTE_PAGO));
		egreso.setAsiento(null);
		egreso.setObservacion(nvl(egreso.getObservacion(), "") + " | PAGO REVERSADO: " + motivo);
		em.merge(egreso);

		System.out.println("✓ Egreso " + egreso.getId() + " vuelve a Pendiente de pago.");
	}

	/**
	 * Contabiliza el pago de un documento cuyo origen vive en otro módulo del
	 * sistema: genera el asiento a partir del desglose de PGS.DPGT (una línea
	 * DEBE por producto, contra una sola línea HABER al banco por el total), el
	 * movimiento bancario de egreso, y cuelga el asiento del propio pago.
	 * <p>
	 * A diferencia de los otros orígenes, aquí el asiento se guarda en PGTRASNT:
	 * no hay documento de CXP donde colgarlo, y CXP no puede escribir en el
	 * documento del módulo que originó el pago porque no lo conoce.
	 * <p>
	 * El asiento se clasifica en {@code ModuloSistema.CUENTAS_POR_PAGAR}: lo
	 * genera CXP. No existe un ModuloSistema por cada módulo que pueda originar
	 * un pago, y crearlo sería exactamente la dependencia que hay que evitar.
	 * <p>
	 * <b>La contabilidad es OPCIONAL.</b> Si el pago no tiene desglose en PGS.DPGT, este
	 * método NO genera asiento ni movimiento bancario, deja {@code PGTRASNT} en null y
	 * devuelve {@code null}. El pago igual pasa a CONFIRMADO: la confirmación es del banco y
	 * no depende del asiento.
	 * <p>
	 * Consecuencia, dicha de frente: sin asiento tampoco hay movimiento bancario —
	 * {@code creaMovimientoPorTransferencia} recibe el Asiento como parámetro, así que los dos
	 * caen juntos—. Ese pago salió del banco y no queda registrado en ningún lado más que en
	 * PGS.PGTR: es invisible para la conciliación bancaria hasta que se regularice.
	 * {@code PGTRASNT IS NULL} en un pago CONFIRMADO de origen externo es la marca para
	 * encontrarlos; el control está en
	 * {@code docs/logica-negocio/crd/sql/DDL-DEVOLUCION-APORTES.sql}.
	 *
	 * @param pago      : Pago de origen externo ya ejecutado por el banco
	 * @param idUsuario : Id del usuario que registra o procesa
	 * @return          : Asiento generado, o <b>null</b> si el pago no tiene desglose contable
	 * @throws Throwable : Excepcion
	 */
	private Asiento contabilizarPagoOrigenExterno(PagoProgramado pago, Long idUsuario)
			throws Throwable {

		// La caja chica no usa el desglose de PGS.DPGT: el DEBE es siempre la
		// cuenta contable de la caja, no un producto por línea. Se resuelve
		// aparte con su propia plantilla de asiento.
		if (com.saa.rubros.OrigenPagoExterno.TSR_CAJA_CHICA.equals(pago.getOrigenExterno())) {
			return contabilizarPagoCajaChica(pago, idUsuario);
		}

		Long idEmpresa = (pago.getEmpresa() != null) ? pago.getEmpresa().getCodigo() : null;
		LocalDate fecha = (pago.getFechaRespuesta() != null)
				? pago.getFechaRespuesta() : LocalDate.now();
		boolean debitoAutomatico = esDebitoAutomatico(pago);

		// Sin desglose no hay contabilidad, y no es un error: es el caso previsto en el que la
		// parametrización contable del proceso origen todavía no está definida.
		List<DetallePagoOrigenExterno> detalles =
				detallePagoOrigenExternoDaoService.selectByPago(pago.getId());
		if (detalles == null || detalles.isEmpty()) {
			System.out.println("⚠ Pago " + pago.getId() + " confirmado SIN contabilidad: "
					+ "no tiene desglose contable. No se generó asiento ni movimiento bancario"
					+ " | origen=" + nvl(pago.getOrigenExterno(), "")
					+ " | idOrigen=" + pago.getIdOrigen()
					+ " | valor=$"
					+ String.format(Locale.US, "%.2f", (pago.getValor() != null ? pago.getValor() : 0.0)));
			return null;
		}

		if (pago.getCuentaBancaria() == null || pago.getCuentaBancaria().getPlanCuenta() == null) {
			throw new IncomeException("La cuenta bancaria del pago " + pago.getId()
					+ " no tiene cuenta contable configurada (Tesorería → Cuentas bancarias).");
		}

		String descripcionBase = "Pago " + nvl(pago.getOrigenExterno(), "")
				+ " N° " + pago.getIdOrigen();

		// ── Una línea DEBE por producto del desglose ─────────────────────────────
		List<DetalleAsiento> lineas = new ArrayList<>();
		BigDecimal suma = BigDecimal.ZERO;
		for (DetallePagoOrigenExterno detalle : detalles) {
			PlanCuenta cuenta = cuentaDelProducto(detalle.getProducto());
			double valorLinea = (detalle.getValor() != null) ? detalle.getValor() : 0.0;
			lineas.add(creaLineaAsiento(cuenta,
					(detalle.getConcepto() != null && !detalle.getConcepto().trim().isEmpty())
							? detalle.getConcepto().trim() : descripcionBase,
					valorLinea, true));
			suma = suma.add(BigDecimal.valueOf(valorLinea));
		}

		// ── Una sola línea HABER al banco, por el total ──────────────────────────
		double totalPago = (pago.getValor() != null) ? pago.getValor() : 0.0;
		if (Math.abs(suma.doubleValue() - totalPago) > TOLERANCIA) {
			throw new IncomeException("El desglose contable del pago " + pago.getId() + " suma $"
					+ String.format(Locale.US, "%.2f", suma.doubleValue())
					+ " y el pago es de $" + String.format(Locale.US, "%.2f", totalPago)
					+ ". No se genera un asiento descuadrado.");
		}
		Cheque cheque = pago.getCheque();
		String notaCheque = (cheque != null)
				? " | Cheque N° " + cheque.getNumero() + " Cta " + pago.getCuentaBancaria().getNumeroCuenta()
				: "";

		lineas.add(creaLineaAsiento(pago.getCuentaBancaria().getPlanCuenta(),
				descripcionBase + " | Cta Banco: " + pago.getCuentaBancaria().getNumeroCuenta(),
				totalPago, false));

		String observacionAsiento = descripcionBase
				+ (cheque != null ? " (cheque)" : debitoAutomatico ? " (débito automático)" : " (transferencia)")
				+ " | Beneficiario: " + nvl(pago.getBeneficiarioNombre(), "")
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), "")
				+ " | Valor: $" + String.format(Locale.US, "%.2f", totalPago)
				+ notaCheque;

		Asiento asiento = asientoContableService.generarAsiento(idEmpresa,
				TipoAsientos.PAGO_ORIGEN_EXTERNO, fecha, observacionAsiento,
				usuarioNombre(idUsuario), lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_PAGAR));

		// Movimiento bancario de egreso (mismo criterio que los demás pagos)
		int tipoMovimiento = (cheque != null)
				? TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS
				: TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO;
		com.saa.model.tsr.MovimientoBanco mov = movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				descripcionBase + " | Beneficiario: " + nvl(pago.getBeneficiarioNombre(), "")
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ (cheque != null ? " | Cheque N° " + cheque.getNumero() : " | Ref: " + nvl(pago.getReferenciaBanco(), "")),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				tipoMovimiento, OrigenMovimientoConciliacion.PAGOS);
		if (cheque != null) {
			mov.setCheque(cheque);
			mov.setNumeroCheque(cheque.getNumero());
			movimientoBancoService.saveSingle(mov);
		}

		// El asiento cuelga del pago: no hay documento de CXP donde colgarlo.
		pago.setAsiento(asiento);

		System.out.println("✓ Pago de origen externo " + pago.getId() + " contabilizado"
				+ " | lineas=" + lineas.size() + " | asiento=" + asiento.getNumeroAlterno());
		return asiento;
	}

	/**
	 * Contabiliza la apertura o reposición de una caja chica pagada desde una
	 * cuenta bancaria: DEBE cuenta contable de la caja / HABER cuenta contable
	 * del banco (no el desglose de PGS.DPGT — la caja no tiene productos por
	 * línea, es una sola cuenta). El movimiento de caja chica se ubica por
	 * {@code pago.getIdOrigen()} (es su MVCHCDGO) y recibe el mismo asiento que
	 * el pago.
	 * @param pago      : Pago de origen externo TSR_CAJA_CHICA
	 * @param idUsuario : Id del usuario que registra o procesa
	 * @return          : Asiento generado
	 * @throws Throwable : Excepcion
	 */
	private Asiento contabilizarPagoCajaChica(PagoProgramado pago, Long idUsuario) throws Throwable {

		Long idEmpresa = (pago.getEmpresa() != null) ? pago.getEmpresa().getCodigo() : null;
		LocalDate fecha = (pago.getFechaRespuesta() != null) ? pago.getFechaRespuesta() : LocalDate.now();
		boolean debitoAutomatico = esDebitoAutomatico(pago);

		com.saa.model.tsr.MovimientoCajaChica movimiento =
				em.find(com.saa.model.tsr.MovimientoCajaChica.class, pago.getIdOrigen());
		if (movimiento == null) {
			throw new IncomeException("No se encontró el movimiento de caja chica con ID: "
					+ pago.getIdOrigen());
		}
		com.saa.model.tsr.CajaChica caja = movimiento.getCajaChica();
		if (caja == null || caja.getPlanCuenta() == null) {
			throw new IncomeException("La caja chica del movimiento " + movimiento.getCodigo()
					+ " no tiene cuenta contable configurada.");
		}
		if (pago.getCuentaBancaria() == null || pago.getCuentaBancaria().getPlanCuenta() == null) {
			throw new IncomeException("La cuenta bancaria del pago " + pago.getId()
					+ " no tiene cuenta contable configurada (Tesorería → Cuentas bancarias).");
		}

		boolean esApertura = movimiento.getTipo() != null
				&& movimiento.getTipo().intValue() == com.saa.rubros.TipoMovimientoCajaChica.APERTURA;
		String etiqueta = esApertura ? "Apertura" : "Reposición";
		Cheque cheque = pago.getCheque();
		String notaCheque = (cheque != null)
				? " | Cheque N° " + cheque.getNumero() + " Cta " + pago.getCuentaBancaria().getNumeroCuenta()
				: "";

		String observacionAsiento = etiqueta + " caja chica " + caja.getNombre()
				+ " | " + nvl(movimiento.getDescripcion(), "")
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), "")
				+ " | Valor: $" + String.format(Locale.US, "%.2f", pago.getValor())
				+ notaCheque;

		Asiento asiento = asientoContableService.generarAsientoReposicionCajaChica(
				caja.getPlanCuenta().getCodigo(), pago.getCuentaBancaria().getCodigo(), pago.getValor(),
				idEmpresa, fecha, observacionAsiento, usuarioNombre(idUsuario));

		int tipoMovimiento = (cheque != null)
				? TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS
				: TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO;
		com.saa.model.tsr.MovimientoBanco mov = movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				etiqueta + " caja chica " + caja.getNombre()
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ (cheque != null ? " | Cheque N° " + cheque.getNumero() : " | Ref: " + nvl(pago.getReferenciaBanco(), "")),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				tipoMovimiento, OrigenMovimientoConciliacion.PAGOS);
		if (cheque != null) {
			mov.setCheque(cheque);
			mov.setNumeroCheque(cheque.getNumero());
			movimientoBancoService.saveSingle(mov);
		}

		pago.setAsiento(asiento);
		movimiento.setAsiento(asiento);
		em.merge(movimiento);

		System.out.println("✓ " + etiqueta + " de caja chica " + caja.getNombre() + " contabilizada"
				+ " | movimiento=" + movimiento.getCodigo() + " | asiento=" + asiento.getNumeroAlterno());
		return asiento;
	}

	/**
	 * Reversa la contabilidad de un pago de origen externo: anula el movimiento
	 * bancario y el asiento, y desvincula el asiento del pago.
	 * <p>
	 * No toca el documento de origen: CXP no lo conoce. Es el módulo que lo
	 * generó el que consulta el estado del pago y genera sus propios
	 * contra-movimientos.
	 * <p>
	 * <b>Soporta un pago con {@code PGTRASNT} nulo</b>: si se confirmó sin contabilidad
	 * (porque no tenía desglose) no hay asiento que anular ni movimiento que marcar, así que
	 * el método sale limpio sin lanzar. El pago igual queda RECHAZADO o ANULADO según
	 * corresponda: quien decide eso es {@code revertirPagoConfirmado}, no este método.
	 *
	 * @param pago   : Pago confirmado de origen externo
	 * @param motivo : Motivo de la reversión
	 * @throws Throwable : Excepcion
	 */
	private void revertirContabilidadOrigenExterno(PagoProgramado pago, String motivo)
			throws Throwable {

		Long idAsiento = (pago.getAsiento() != null) ? pago.getAsiento().getCodigo() : null;

		if (idAsiento == null) {
			System.out.println("✓ Pago de origen externo " + pago.getId() + " sin asiento: "
					+ "se confirmó sin contabilidad, no hay nada que reversar. Motivo: " + motivo);
			return;
		}

		try {
			movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
					Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
		} catch (Exception e) {
			System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
					+ idAsiento + ": " + e.getMessage());
		}
		try {
			asientoService.anulaAsiento(idAsiento);
			System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
		} catch (Throwable e) {
			System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": "
					+ e.getMessage());
		}

		pago.setAsiento(null);

		// La caja chica además anula su propio movimiento: no queda como "activo
		// sin asiento" (no existe ese estado para ella, a diferencia del origen
		// externo genérico donde un pago sin desglose es un caso previsto).
		anularMovimientoCajaChicaSiAplica(pago, "PAGO REVERSADO: " + motivo);

		System.out.println("✓ Contabilidad del pago de origen externo " + pago.getId()
				+ " reversada. Motivo: " + motivo);
	}

	/**
	 * Anula el {@code MovimientoCajaChica} asociado a un pago, si el pago es
	 * de origen {@code TSR_CAJA_CHICA} y el movimiento no está ya anulado.
	 * Único punto que lo hace: lo llaman la reversión de un pago confirmado
	 * ({@code revertirContabilidadOrigenExterno}), la anulación de un pago
	 * todavía no confirmado ({@code anularPago}) y el rechazo del banco
	 * ({@code procesarRespuestaBanco}). Defensivo: hoy inalcanzable en la
	 * práctica porque el pago de caja chica nace CONFIRMADO (sólo admite
	 * cheque o débito automático, nunca transferencia), así que nunca llega
	 * a pasar por {@code anularPago} ni por {@code EN_ARCHIVO}/rechazo de
	 * banco — se deja por si esa restricción cambia a futuro.
	 * <p>Rechaza si el movimiento ya quedó incluido en un cierre confirmado
	 * ({@code movimiento.getCierre() != null}): anularlo alteraría en
	 * silencio un periodo ya cerrado. El llamador debe propagar esta
	 * excepción salvo en el rechazo por lote, donde se captura para no
	 * abortar el procesamiento del archivo completo.
	 * @param pago   : Pago que se está anulando/reversando/rechazando
	 * @param motivo : Motivo a grabar en el movimiento
	 * @throws IncomeException : Si el movimiento pertenece a un cierre confirmado
	 */
	private void anularMovimientoCajaChicaSiAplica(PagoProgramado pago, String motivo) {
		if (!com.saa.rubros.OrigenPagoExterno.TSR_CAJA_CHICA.equals(pago.getOrigenExterno())) {
			return;
		}
		com.saa.model.tsr.MovimientoCajaChica movimiento =
				em.find(com.saa.model.tsr.MovimientoCajaChica.class, pago.getIdOrigen());
		if (movimiento == null) {
			return;
		}
		if (movimiento.getEstado() != null
				&& movimiento.getEstado().intValue() == com.saa.rubros.EstadoMovimientoCajaChica.ANULADO) {
			return;
		}
		if (movimiento.getCierre() != null) {
			throw new IncomeException("El movimiento de caja chica " + movimiento.getCodigo()
					+ " pertenece a un cierre confirmado (N° " + movimiento.getCierre().getCodigo()
					+ "): primero hay que anular ese cierre.");
		}
		movimiento.setEstado(Long.valueOf(com.saa.rubros.EstadoMovimientoCajaChica.ANULADO));
		movimiento.setMotivoAnulacion(motivo);
		movimiento.setAsiento(null);
		em.merge(movimiento);
		System.out.println("✓ Movimiento de caja chica " + movimiento.getCodigo()
				+ " anulado. Motivo: " + motivo);
	}

	/**
	 * Valida que el producto exista y que su grupo tenga cuenta contable: sin eso
	 * el asiento del pago no se puede generar. Se llama AL REGISTRAR para que el
	 * error de parametrización salga temprano — mismo criterio que
	 * {@code EgresoServiceImpl.validaProducto}.
	 * @param idProductoPago : Id del producto CXP (PGS.PRDP)
	 * @return               : Producto validado
	 * @throws Throwable     : Excepcion con mensaje accionable
	 */
	private ProductoPago validaProductoPago(Long idProductoPago) throws Throwable {
		ProductoPago producto = em.find(ProductoPago.class, idProductoPago);
		if (producto == null) {
			throw new IncomeException("No se encontró el producto CXP con ID: " + idProductoPago);
		}
		cuentaDelProducto(producto);
		return producto;
	}

	/**
	 * Cuenta contable del grupo de un producto CXP, con los mensajes accionables
	 * de la cadena producto → grupo → planCuenta.
	 * @param producto   : Producto CXP
	 * @return           : Cuenta contable del grupo
	 * @throws Throwable : Excepcion si falta el grupo o la cuenta
	 */
	private PlanCuenta cuentaDelProducto(ProductoPago producto) throws Throwable {
		if (producto == null) {
			throw new IncomeException("Debe indicar el producto que clasifica el pago.");
		}
		if (producto.getGrupoProducto() == null) {
			throw new IncomeException("El producto '" + producto.getNombre()
					+ "' no tiene grupo asignado. Clasifíquelo en CXP → Productos antes de usarlo.");
		}
		if (producto.getGrupoProducto().getPlanCuenta() == null) {
			throw new IncomeException("El grupo '" + producto.getGrupoProducto().getNombre()
					+ "' del producto '" + producto.getNombre()
					+ "' no tiene cuenta contable configurada (Contabilidad → Grupos de Producto).");
		}
		return producto.getGrupoProducto().getPlanCuenta();
	}

	/**
	 * Arma una línea de detalle del asiento.
	 * @param cuenta      : Cuenta contable
	 * @param descripcion : Descripción de la línea
	 * @param valor       : Valor de la línea
	 * @param esDebe      : true para DEBE, false para HABER
	 * @return            : Línea de detalle
	 */
	private DetalleAsiento creaLineaAsiento(PlanCuenta cuenta, String descripcion,
			Double valor, boolean esDebe) {
		DetalleAsiento linea = new DetalleAsiento();
		linea.setPlanCuenta(cuenta);
		linea.setNumeroCuenta(cuenta.getCuentaContable());
		linea.setNombreCuenta(cuenta.getNombre());
		linea.setDescripcion(descripcion);
		linea.setValorDebe(esDebe  ? valor : 0.0);
		linea.setValorHaber(esDebe ? 0.0   : valor);
		return linea;
	}

	/**
	 * Redondea un valor monetario a 2 decimales (HALF_UP).
	 * @param valor : Valor a redondear
	 * @return      : Valor redondeado; 0.0 si viene nulo
	 */
	private double redondea(Double valor) {
		if (valor == null) {
			return 0.0;
		}
		return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Contabiliza el pago de un anticipo a proveedor: genera el asiento de
	 * ANTICIPO (DEBE cuenta de anticipos del proveedor / HABER banco — no el
	 * asiento de egreso) a través del servicio de anticipos, que además
	 * acredita el saldo de anticipos y deja el anticipo Confirmado; aquí se
	 * genera el movimiento bancario de egreso, igual que en los demás pagos.
	 * @param pago      : Pago del anticipo ya ejecutado por el banco
	 * @param idUsuario : Id del usuario que registra o procesa
	 * @return          : Asiento generado
	 * @throws Throwable : Excepcion
	 */
	private Asiento contabilizarPagoAnticipo(PagoProgramado pago, Long idUsuario) throws Throwable {

		AnticipoProveedor anticipo = pago.getAnticipo();
		Long idEmpresa = (pago.getEmpresa() != null) ? pago.getEmpresa().getCodigo() : null;
		Long idCuentaBancaria = (pago.getCuentaBancaria() != null)
				? pago.getCuentaBancaria().getCodigo() : null;
		LocalDate fecha = (pago.getFechaRespuesta() != null)
				? pago.getFechaRespuesta() : LocalDate.now();
		boolean debitoAutomatico = esDebitoAutomatico(pago);
		Cheque cheque = pago.getCheque();
		String notaCheque = (cheque != null)
				? "Cheque N° " + cheque.getNumero() + " Cta " + pago.getCuentaBancaria().getNumeroCuenta()
				: null;

		// 1. Asiento de anticipo + saldo de anticipos + anticipo Confirmado
		Asiento asiento = anticipoProveedorService.contabilizarAnticipoConfirmado(
				anticipo.getId(), idCuentaBancaria, fecha, idUsuario, notaCheque);

		// 2. Movimiento bancario de egreso (mismo criterio que los demás pagos)
		int tipoMovimiento = (cheque != null)
				? TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS
				: TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO;
		com.saa.model.tsr.MovimientoBanco mov = movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Anticipo a proveedor: " + anticipo.getTitular().getNombre()
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ (cheque != null ? " | Cheque N° " + cheque.getNumero() : " | Ref: " + nvl(pago.getReferenciaBanco(), "")),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				tipoMovimiento, OrigenMovimientoConciliacion.PAGOS);
		if (cheque != null) {
			mov.setCheque(cheque);
			mov.setNumeroCheque(cheque.getNumero());
			movimientoBancoService.saveSingle(mov);
		}

		System.out.println("✓ Anticipo " + anticipo.getId() + " pagado y contabilizado"
				+ " | asiento=" + asiento.getNumeroAlterno());
		return asiento;
	}

	/**
	 * Reversa la contabilidad del pago de un anticipo: anula el movimiento
	 * bancario y delega en el servicio de anticipos la anulación del asiento,
	 * el descuento del saldo de anticipos y el retorno del anticipo a
	 * Ingresado.
	 * @param pago   : Pago confirmado del anticipo
	 * @param motivo : Motivo de la reversión
	 * @throws Throwable : Excepcion
	 */
	private void revertirContabilidadAnticipoPago(PagoProgramado pago, String motivo)
			throws Throwable {

		AnticipoProveedor anticipo = pago.getAnticipo();
		Long idAsiento = (anticipo.getAsiento() != null)
				? anticipo.getAsiento().getCodigo() : null;

		if (idAsiento != null) {
			try {
				movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
						Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
			} catch (Exception e) {
				System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
						+ idAsiento + ": " + e.getMessage());
			}
		}

		anticipoProveedorService.revertirContabilidadAnticipo(anticipo.getId(), motivo);
	}

	/**
	 * Recupera el nombre de un usuario para las trazas y observaciones.
	 * @param idUsuario : Id del usuario
	 * @return          : Nombre del usuario o SISTEMA
	 */
	private String usuarioNombre(Long idUsuario) {
		if (idUsuario == null) {
			return "SISTEMA";
		}
		Usuario usuario = em.find(Usuario.class, idUsuario);
		return (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "SISTEMA";
	}

	private String nvl(String valor, String porDefecto) {
		return (valor != null) ? valor : porDefecto;
	}

	/**
	 * Descripción legible del estado de un pago, para los mensajes de error.
	 * @param estado : Estado del pago
	 * @return       : Nombre del estado
	 */
	private String descripcionEstado(Long estado) {
		if (estado == null) {
			return "sin estado";
		}
		switch (estado.intValue()) {
			case EstadoPagoProgramado.REGISTRADO: return "Registrado";
			case EstadoPagoProgramado.EN_ARCHIVO: return "En archivo";
			case EstadoPagoProgramado.CONFIRMADO: return "Confirmado";
			case EstadoPagoProgramado.RECHAZADO:  return "Rechazado";
			case EstadoPagoProgramado.ANULADO:    return "Anulado";
			default: return String.valueOf(estado);
		}
	}

	/**
	 * Interpreta una fecha en formato yyyy-MM-dd.
	 * @param fecha : Fecha en texto
	 * @return      : Fecha, o la de hoy si viene vacía o mal formada
	 */
	private LocalDate parseFecha(String fecha) {
		if (fecha == null || fecha.trim().isEmpty()) {
			return LocalDate.now();
		}
		try {
			return LocalDate.parse(fecha.trim());
		} catch (Exception e) {
			System.err.println("⚠ Fecha inválida '" + fecha + "', se usa la fecha actual.");
			return LocalDate.now();
		}
	}
}
