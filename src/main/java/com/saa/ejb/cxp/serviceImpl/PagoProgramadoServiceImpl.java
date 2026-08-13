package com.saa.ejb.cxp.serviceImpl;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.LotePagoDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.FormateadorArchivoBanco;
import com.saa.ejb.cxp.service.LectorRespuestaBanco;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.RespuestaPagoBanco;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.Egreso;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoLotePago;
import com.saa.rubros.EstadoPagoProgramado;
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
	private AplicacionPagoCxpService aplicacionPagoCxpService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;

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

		System.out.println("=== registrarPago | factura=" + idFacturaCompra + " | valor=" + valor
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " ===");

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
		pago.setDebitoAutomatico(Long.valueOf(debitoAutomatico ? 1 : 0));
		pago.setValor(valor);
		pago.setFechaProgramada(fecha);
		pago.setObservacion(observacion);
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());

		if (!debitoAutomatico) {
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			pago = saveSingle(pago);

			System.out.println("✓ Pago registrado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje",
					"Pago registrado. Queda pendiente de incluirse en un archivo de pagos.");
			resultado.put("pago", pago.getId());
			resultado.put("debitoAutomatico", false);
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

		AplicacionPagoCxp aplicacion = aplicacionPagoCxpService.aplicarPagoTransferencia(pago, idUsuario);
		pago.setAplicacion(aplicacion);
		pago = pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		System.out.println("✓ Pago por débito automático registrado y aplicado: id=" + pago.getId()
				+ " | aplicacion=" + aplicacion.getId());

		resultado.put("exito", true);
		resultado.put("mensaje", "Pago por débito automático registrado. La factura quedó abonada "
				+ "y el asiento contable fue generado.");
		resultado.put("pago", pago.getId());
		resultado.put("debitoAutomatico", true);
		resultado.put("aplicacion", aplicacion.getId());
		if (aplicacion.getAsiento() != null) {
			resultado.put("asiento", aplicacion.getAsiento().getNumeroAlterno());
		}
		resultado.putAll(aplicacionPagoCxpService.saldoFactura(idFacturaCompra));
		return resultado;
	}

	@Override
	public Map<String, Object> registrarPagoDeEgreso(Long idEgreso, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia) throws Throwable {

		System.out.println("=== registrarPagoDeEgreso | egreso=" + idEgreso
				+ " | cuentaOrigen=" + idCuentaBancariaOrigen
				+ " | debitoAutomatico=" + debitoAutomatico + " ===");

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

		// La transferencia viaja en el archivo del banco: exige beneficiario y
		// cuenta de destino. El débito automático no transfiere nada.
		CuentaBancariaTitular cuentaDestino = null;
		if (!debitoAutomatico) {
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
		pago.setDebitoAutomatico(Long.valueOf(debitoAutomatico ? 1 : 0));
		pago.setValor(egreso.getValor());
		pago.setFechaProgramada(egreso.getFecha() != null ? egreso.getFecha() : LocalDate.now());
		pago.setObservacion(egreso.getDescripcion());
		pago.setUsuario(em.find(Usuario.class, idUsuario));
		pago.setFechaRegistro(LocalDateTime.now());

		if (!debitoAutomatico) {
			pago.setEstado(Long.valueOf(EstadoPagoProgramado.REGISTRADO));
			pago = saveSingle(pago);

			System.out.println("✓ Pago de egreso registrado: id=" + pago.getId());

			resultado.put("exito", true);
			resultado.put("mensaje",
					"Pago del egreso registrado. Queda pendiente de incluirse en un archivo de pagos.");
			resultado.put("pago", pago.getId());
			resultado.put("egreso", idEgreso);
			resultado.put("debitoAutomatico", false);
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

		Asiento asiento = contabilizarPagoEgreso(pago, idUsuario);
		em.flush();

		System.out.println("✓ Pago de egreso por débito automático registrado y contabilizado: id="
				+ pago.getId() + " | asiento=" + asiento.getNumeroAlterno());

		resultado.put("exito", true);
		resultado.put("mensaje", "Egreso pagado por débito automático. El asiento contable "
				+ "y el movimiento bancario fueron generados.");
		resultado.put("pago", pago.getId());
		resultado.put("egreso", idEgreso);
		resultado.put("debitoAutomatico", true);
		resultado.put("asiento", asiento.getNumeroAlterno());
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
					// Solo aquí se genera contabilidad y movimiento bancario.
					if (pago.getEgreso() != null) {
						// Pago de un egreso de tesorería: asiento contra la
						// cuenta del grupo del producto, sin aplicación.
						contabilizarPagoEgreso(pago, idUsuario);
					} else {
						AplicacionPagoCxp aplicacion =
								aplicacionPagoCxpService.aplicarPagoTransferencia(pago, idUsuario);
						pago.setAplicacion(aplicacion);
					}
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

				// Mismo camino contable que la respuesta del banco.
				if (pago.getEgreso() != null) {
					contabilizarPagoEgreso(pago, idUsuario);
				} else {
					AplicacionPagoCxp aplicacion =
							aplicacionPagoCxpService.aplicarPagoTransferencia(pago, idUsuario);
					pago.setAplicacion(aplicacion);
				}

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
		if (estado == EstadoPagoProgramado.CONFIRMADO) {
			throw new IncomeException("El pago " + idPago
					+ (esDebitoAutomatico(pago)
							? " es un débito automático ya ejecutado por el banco y tiene "
							: " ya fue confirmado por el banco y tiene ")
					+ "contabilidad generada. Use la reversión en lugar de la anulación.");
		}
		if (estado == EstadoPagoProgramado.ANULADO) {
			throw new IncomeException("El pago " + idPago + " ya está anulado.");
		}

		pago.setEstado(Long.valueOf(EstadoPagoProgramado.ANULADO));
		pago.setMotivo(motivo.trim());
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

		if (pago.getEgreso() != null) {
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

		// Una transferencia reversada vuelve a seguimiento como rechazada, por si
		// hay que reprogramarla. El débito automático no se reprograma: si se
		// reversa es porque se registró mal, así que queda anulado.
		boolean debitoAutomatico = esDebitoAutomatico(pago);
		pago.setEstado(Long.valueOf(debitoAutomatico
				? EstadoPagoProgramado.ANULADO : EstadoPagoProgramado.RECHAZADO));
		pago.setMotivo("REVERSADO: " + motivo.trim());
		pago.setAplicacion(null);
		pagoProgramadoDaoService.save(pago, pago.getId());
		em.flush();

		resultado.put("exito", true);
		resultado.put("mensaje", debitoAutomatico
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

		String observacionAsiento = "Pago egreso tesorería"
				+ (debitoAutomatico ? " (débito automático)" : " (transferencia)")
				+ " | Concepto: " + egreso.getDescripcion()
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), "")
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", pago.getValor());

		// 1. Asiento contable del egreso
		Asiento asiento = asientoContableService.generarAsientoEgresoTesoreria(
				egreso.getProducto().getId(), egreso.getDescripcion(), pago.getValor(),
				idCuentaBancaria, idEmpresa, TipoAsientos.EGRESO_TESORERIA, fecha,
				observacionAsiento, usuarioNombre(idUsuario));

		// 2. Movimiento bancario de egreso (mismo criterio que los pagos de facturas)
		movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Egreso tesorería: " + egreso.getDescripcion()
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), ""),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO,
				OrigenMovimientoConciliacion.PAGOS);

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
