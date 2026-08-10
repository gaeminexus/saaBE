package com.saa.ejb.cxc.serviceImpl;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.NodeList;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.RetencionV2DaoService;
import com.saa.ejb.cxc.dao.PathRetencionV2DaoService;
import com.saa.ejb.cxc.service.RetencionV2Service;
import com.saa.ejb.cxc.service.EmailFacturaService;
import com.saa.model.cxc.DetalleRetencionV2;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathRetencionV2;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoAsientos;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
@Stateless
public class RetencionV2ServiceImpl implements RetencionV2Service {
	
	// Constantes para códigos del SRI
	// private static final String COD_IVA = "2";
	private static final String COD_POR_IVA_15 = "2";
	
	@EJB
	private com.saa.basico.ejb.DetalleRubroService detalleRubroService;

	@EJB
	private RetencionV2DaoService retencionV2DaoService;
	
	@EJB
	private PathRetencionV2DaoService pathRetencionV2DaoService;

	@EJB
	private com.saa.ejb.signature.service.SignatureService signatureService;

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

	@EJB
	private com.saa.ejb.cxp.service.AplicacionPagoCxpService aplicacionPagoCxpService;

	@EJB
	private EmailFacturaService emailFacturaService;

	@EJB
	private com.saa.ejb.reporte.service.ReporteService reporteService;

	@PersistenceContext
	private EntityManager em;

	@Resource
	private SessionContext sessionContext;
	@Override
	public RetencionV2 selectById(Long id) throws Throwable {
		return retencionV2DaoService.selectById(id, NombreEntidadesCobro.RETENCION_V2);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		RetencionV2 entidad = new RetencionV2();
		for (Long registro : id) {
			retencionV2DaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<RetencionV2> lista) throws Throwable {
		for (RetencionV2 registro : lista) {
			retencionV2DaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<RetencionV2> selectAll() throws Throwable {
		List<RetencionV2> result = retencionV2DaoService.selectAll(NombreEntidadesCobro.RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total RetencionV2 no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public RetencionV2 saveSingle(RetencionV2 entidad) throws Throwable {
		System.out.println("saveSingle - RetencionV2");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));

			if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null)
				throw new IncomeException("Debe especificar un punto de emisión para la retención V2");
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null)
				throw new IncomeException("Debe especificar un facturador para la retención V2");

			String tipoComprobante = "07"; // Retención
			String tipoEmision = "1";

			try {
				com.saa.model.cxc.Facturador facturador = em.find(com.saa.model.cxc.Facturador.class, entidad.getFacturador().getId());
				if (facturador == null)
					throw new IncomeException("Facturador con ID " + entidad.getFacturador().getId() + " no encontrado");
				entidad.setFacturador(facturador);

				// Obtener ambiente desde el facturador (BD) — fuente autoritativa por seguridad
				Long ambiente;
				if (facturador.getAmbiente() != null) {
					ambiente = facturador.getAmbiente();
					System.out.println("Ambiente tomado del facturador (BD): " + ambiente
							+ (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)"));
				} else {
					ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
					System.out.println("⚠ Facturador sin ambiente configurado en BD, usando valor recibido: " + ambiente);
				}
				entidad.setAmbiente(ambiente); // sincronizar el valor correcto en la entidad

				com.saa.model.cxc.PuntoEmision ptoEmision = em.find(com.saa.model.cxc.PuntoEmision.class, entidad.getPtoEmision().getId());
				if (ptoEmision == null)
					throw new IncomeException("Punto de emisión con ID " + entidad.getPtoEmision().getId() + " no encontrado");
				entidad.setPtoEmision(ptoEmision);

				// Cargar Proveedor (Titular) completo desde BD
				if (entidad.getProveedor() != null && entidad.getProveedor().getCodigo() != null) {
					com.saa.model.tsr.Titular proveedor = em.find(com.saa.model.tsr.Titular.class, entidad.getProveedor().getCodigo());
					if (proveedor == null)
						throw new IncomeException("Proveedor con código " + entidad.getProveedor().getCodigo() + " no encontrado");
					entidad.setProveedor(proveedor);
				}

				// Derivar numEstablecimiento y numPtoEmision si no vienen del frontend
				if (entidad.getNumEstablecimiento() == null || entidad.getNumEstablecimiento().isEmpty())
					entidad.setNumEstablecimiento(ptoEmision.getEstablecimiento().getCodigo());
				if (entidad.getNumPtoEmision() == null || entidad.getNumPtoEmision().isEmpty())
					entidad.setNumPtoEmision(ptoEmision.getCodigo());

				String secuencial = obtenerSecuencial(ptoEmision.getId(), tipoComprobante);
				entidad.setSecuencial(secuencial);
				String numero = entidad.getNumEstablecimiento() + "-" + entidad.getNumPtoEmision() + "-" + secuencial;
				entidad.setNumero(numero);
				String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
				entidad.setClave(clave);
				entidad.setTipoComprobante(tipoComprobante);
				if (entidad.getEstadoEmision() == null) entidad.setEstadoEmision(1L);
				System.out.println("Número de retención V2 generado: " + numero);
				System.out.println("Clave de acceso generada: " + clave);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la retención V2: " + e.getMessage());
			}
		}
		entidad = retencionV2DaoService.save(entidad, entidad.getId());
		return entidad;
	}

	private String obtenerSecuencial(Long idPtoEmision, String tipoDoc) throws Exception {
		String sql = "SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query query = em.createQuery(sql);
		query.setParameter("ptoEmision", idPtoEmision);
		query.setParameter("tipoDoc", tipoDoc);
		@SuppressWarnings("unchecked")
		List<Object> resultados = query.getResultList();
		if (resultados.isEmpty())
			throw new IncomeException("No existe numeración para el punto de emisión " + idPtoEmision + " y tipo de documento " + tipoDoc);
		com.saa.model.cxc.NumeracionPuntoEmision numeracion = (com.saa.model.cxc.NumeracionPuntoEmision) resultados.get(0);
		Long numeroActual = numeracion.getNumActual();
		Long nuevoNumero = numeroActual + 1;
		em.createQuery("UPDATE NumeracionPuntoEmision n SET n.numActual = :nuevoNumero " +
				"WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc")
			.setParameter("nuevoNumero", nuevoNumero)
			.setParameter("ptoEmision", idPtoEmision)
			.setParameter("tipoDoc", tipoDoc)
			.executeUpdate();
		return String.format("%09d", numeroActual);
	}

	private String generarClaveAcceso(RetencionV2 retencion, String tipoComprobante, Long ambiente,
			String tipoEmision, String secuencial) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = retencion.getFecha().format(formatter);
		String ruc      = retencion.getFacturador().getNumDoc();
		String codClave = retencion.getFacturador().getCodClave();
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente +
				retencion.getNumEstablecimiento() + retencion.getNumPtoEmision() +
				secuencial + codClave + tipoEmision;
		int dv = calcularModulo11(claveSinDV);
		return claveSinDV + dv;
	}

	private int calcularModulo11(String cadena) {
		String invertida = new StringBuilder(cadena).reverse().toString();
		int suma = 0;
		int factor = 2;
		for (int i = 0; i < invertida.length(); i++) {
			suma += Character.getNumericValue(invertida.charAt(i)) * factor;
			factor = (factor == 7) ? 2 : factor + 1;
		}
		int dv = 11 - (suma % 11);
		if (dv == 10) return 1;
		if (dv == 11) return 0;
		return dv;
	}
	@Override
	public List<RetencionV2> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<RetencionV2> result = retencionV2DaoService.selectByCriteria(datos, NombreEntidadesCobro.RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio RetencionV2 no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public String[] generarXMLRetencionV2(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLRetencionV2 con clave: " + clave + " y ambiente: " + ambiente);
		try {
			// 1. Obtener la retención V2 por clave
			String sqlRetencion = "SELECT r FROM RetencionV2 r WHERE r.clave = :clave";
			Query queryRetencion = em.createQuery(sqlRetencion);
			queryRetencion.setParameter("clave", clave);
			RetencionV2 retencion = (RetencionV2) queryRetencion.getSingleResult();
			if (retencion == null) throw new IncomeException("RetencionV2 con clave " + clave + " no encontrada");

			Long idRetencion  = retencion.getId();
			Long idFacturador = retencion.getFacturador().getId();

			// 2. Obtener dirección del establecimiento
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", retencion.getPtoEmision().getId());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();

			// 3. Obtener documentos sustento agrupados (un registro por numDocReten)
			String sqlDocumentos = "SELECT DISTINCT d.tipoDocReten, d.numDocReten, d.fechaEmiDoc, " +
					"d.docResTotalSinImpuestos, d.docResTotal, d.docResIvaCero, d.docResTotalIva, " +
					"d.docResPorIva, d.docResForPago " +
					"FROM DetalleRetencionV2 d " +
					"WHERE d.retencionV2.id = :retencionId " +
					"ORDER BY d.numDocReten";
			Query queryDocumentos = em.createQuery(sqlDocumentos);
			queryDocumentos.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<Object[]> documentos = queryDocumentos.getResultList();

			// 4. Obtener todos los detalles de retención
			String sqlDetalle = "SELECT d FROM DetalleRetencionV2 d WHERE d.retencionV2.id = :retencionId ORDER BY d.id";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<DetalleRetencionV2> detalles = queryDetalle.getResultList();

			// 5. Generar XML
			String xmlContent = generarXMLContentRetencionV2(retencion, dirEstablecimiento, documentos, detalles, ambiente);

			// 6. Guardar archivo XML
			String pathRelativo = "resources/" + idFacturador + "/rtv2/g/" + clave + ".xml";
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto  = baseUploadDir + pathRelativo;

			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));

			System.out.println("✓ XML RetencionV2 generado en: " + pathAbsoluto);
			return new String[]{"OK", pathRelativo, pathAbsoluto};

		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML RetencionV2: " + e.getMessage());
		}
	}

	/**
	 * Genera el contenido XML de la retención V2 según estándares del SRI v2.0.0.
	 * Basado en gn_xml_rtv2.php.
	 */
	private String generarXMLContentRetencionV2(RetencionV2 retencion, String dirEstablecimiento,
			List<Object[]> documentos, List<DetalleRetencionV2> detalles, Long ambiente) throws Exception {

		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory  = XMLOutputFactory.newInstance();
		XMLStreamWriter writer    = factory.createXMLStreamWriter(stringWriter);

		final String TIPO_DOC    = "07";
		final String TIPO_EMISION = "1";
		final String COD_IVA     = "2";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		com.saa.model.cxc.Facturador f  = retencion.getFacturador();
		com.saa.model.tsr.Titular    pr = retencion.getProveedor();

		String obligadoContabilidad = (f.getContabilidad() != null && f.getContabilidad() == 1) ? "SI" : "NO";
		String facturadorTelefono   = nvl(f.getTelefono(), "");
		String facturadorMail       = nvl(f.getMail(), "");
		String proveedorTelefono    = pr != null ? nvl(pr.getTelefono(), "") : "";
		String proveedorMail        = pr != null ? nvl(pr.getEmail(), "")    : "";

		// ── Raíz ──────────────────────────────────────────────────────────────
		writer.writeStartElement("comprobanteRetencion");
		writer.writeAttribute("id", "comprobante");
		writer.writeAttribute("version", "2.0.0");
		writer.writeCharacters("\n");

		// ── infoTributaria ────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		writeElement(writer, "ambiente",        String.valueOf(ambiente),          4);
		writeElement(writer, "tipoEmision",     TIPO_EMISION,                      4);
		writeElement(writer, "razonSocial",     nvl(f.getRazonSocial(), ""),       4);
		writeElement(writer, "nombreComercial", nvl(f.getNombre(), ""),            4);
		writeElement(writer, "ruc",             nvl(f.getNumDoc(), ""),            4);
		writeElement(writer, "claveAcceso",     nvl(retencion.getClave(), ""),     4);
		writeElement(writer, "codDoc",          TIPO_DOC,                          4);
		writeElement(writer, "estab",           nvl(retencion.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi",          nvl(retencion.getNumPtoEmision(), ""),      4);
		writeElement(writer, "secuencial",      nvl(retencion.getSecuencial(), ""),         4);
		writeElement(writer, "dirMatriz",       nvl(f.getDireccion(), ""),         4);
		if (f.getMicroEmpresa() != null && f.getMicroEmpresa() == 1)
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		if (f.getAgenteRetencion() != null && !f.getAgenteRetencion().isEmpty())
			writeElement(writer, "agenteRetencion", f.getAgenteRetencion(), 4);
		if (f.getRimpe() != null && f.getRimpe() == 1)
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		if (f.getPopularRimpe() != null && f.getPopularRimpe() == 1)
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoTributaria
		writer.writeCharacters("\n");

		// ── infoCompRetencion ─────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoCompRetencion");
		writer.writeCharacters("\n");
		writeElement(writer, "fechaEmision", retencion.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		if (f.getContribuyenteEspecial() != null && !f.getContribuyenteEspecial().isEmpty())
			writeElement(writer, "contribuyenteEspecial", f.getContribuyenteEspecial(), 4);
		writeElement(writer, "obligadoContabilidad", obligadoContabilidad, 4);
		// Obtener tipoIdentificacionSujetoRetenido desde DetalleRubro (debe ser siempre 2 dígitos: "04"=RUC, "05"=Cédula, "06"=Pasaporte, "07"=Consumidor Final, "08"=ID Exterior)
		String tipoIdentificacionSujetoRetenido = "05"; // valor por defecto: cédula
		try {
			if (pr != null && pr.getRubroTipoIdentificacionP() != null && pr.getRubroTipoIdentificacionH() != null) {
				String valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
						pr.getRubroTipoIdentificacionP().intValue(),
						pr.getRubroTipoIdentificacionH().intValue());
				if (valorAlfa != null && !valorAlfa.isEmpty()) {
					tipoIdentificacionSujetoRetenido = valorAlfa.length() == 1 ? "0" + valorAlfa : valorAlfa;
				}
			}
		} catch (Throwable e) {
			System.err.println("⚠ Error al obtener tipoIdentificacionSujetoRetenido: " + e.getMessage());
		}
		writeElement(writer, "tipoIdentificacionSujetoRetenido", tipoIdentificacionSujetoRetenido, 4);
		writeElement(writer, "parteRel", "NO", 4); // Tabla 14 ATS: NO = no vinculado
		writeElement(writer, "razonSocialSujetoRetenido", pr != null ? nvl(pr.getNombre(), "") : "", 4);
		writeElement(writer, "identificacionSujetoRetenido", pr != null ? nvl(pr.getIdentificacion(), "") : "", 4);
		writeElement(writer, "periodoFiscal", nvl(retencion.getPeriodoFiscal(), ""), 4);
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoCompRetencion
		writer.writeCharacters("\n");

		// ── docsSustento ──────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("docsSustento");
		writer.writeCharacters("\n");

		for (Object[] doc : documentos) {
			String  tipoDocReten           = (String)  doc[0];
			String  numDocReten            = (String)  doc[1];
			java.time.LocalDate fechaEmiDoc = (java.time.LocalDate) doc[2];
			Double  docResTotalSinImpuestos = (Double)  doc[3];
			Double  docResTotal            = (Double)  doc[4];
			Double  docResIvaCero          = (Double)  doc[5];
			Double  docResTotalIva         = (Double)  doc[6];
			Double  docResPorIva           = (Double)  doc[7];
			String  docResForPago          = (String)  doc[8];

			writer.writeCharacters("    ");
			writer.writeStartElement("docSustento");
			writer.writeCharacters("\n");

			writeElement(writer, "codSustento",              "02", 6); // Tabla 5 ATS: 02=Factura
			writeElement(writer, "codDocSustento",           nvl(tipoDocReten, ""), 6);
			writeElement(writer, "numDocSustento", nvl(numDocReten, "").replace("-", ""), 6);
			writeElement(writer, "fechaEmisionDocSustento",
					fechaEmiDoc != null ? fechaEmiDoc.format(dateFormatter) : "", 6);
			writeElement(writer, "pagoLocExt",               "01", 6); // Tabla 15 ATS: 01=Residente
			writeElement(writer, "totalSinImpuestos",        fmt(docResTotalSinImpuestos), 6);
			writeElement(writer, "importeTotal",             fmt(docResTotal), 6);

					// impuestosDocSustento — el SRI exige al menos un impuestoDocSustento
					writer.writeCharacters("      ");
					writer.writeStartElement("impuestosDocSustento");
					writer.writeCharacters("\n");

					boolean tieneImpuesto = false;

					if (docResIvaCero != null && docResIvaCero > 0) {
						writeImpuestoDocSustento(writer, COD_IVA, "0", docResIvaCero, "0", 0.0);
						tieneImpuesto = true;
					}
					if (docResTotalIva != null && docResTotalIva > 0) {
						double baseIva = nvl(docResTotalSinImpuestos, 0.0) - nvl(docResIvaCero, 0.0);
						writeImpuestoDocSustento(writer, COD_IVA, COD_POR_IVA_15,
								baseIva, fmt(nvl(docResPorIva, 0.0)), docResTotalIva);
						tieneImpuesto = true;
					}

					// Si no hay IVA, escribir un registro con base = totalSinImpuestos y valor = 0
					// (obligatorio según esquema XSD del SRI para retención v2)
					if (!tieneImpuesto) {
						writeImpuestoDocSustento(writer, COD_IVA, "0",
								nvl(docResTotalSinImpuestos, 0.0), "0", 0.0);
					}

					writer.writeCharacters("      ");
					writer.writeEndElement(); // impuestosDocSustento
					writer.writeCharacters("\n");

			// retenciones — filtradas por numDocReten del documento sustento actual
			writer.writeCharacters("      ");
			writer.writeStartElement("retenciones");
			writer.writeCharacters("\n");
			for (DetalleRetencionV2 det : detalles) {
				if (numDocReten != null && numDocReten.equals(det.getNumDocReten())) {
					writer.writeCharacters("        ");
					writer.writeStartElement("retencion");
					writer.writeCharacters("\n");
					writeElement(writer, "codigo",            nvl(det.getCodImpuesto(), ""),  10);
					writeElement(writer, "codigoRetencion",   nvl(det.getCodRetencion(), ""), 10);
					writeElement(writer, "baseImponible",     fmt(det.getBaseImponible()),     10);
					writeElement(writer, "porcentajeRetener", fmt(det.getPorcentajeReten()),   10);
					writeElement(writer, "valorRetenido",     fmt(det.getValorReten()),        10);
					writer.writeCharacters("        ");
					writer.writeEndElement(); // retencion
					writer.writeCharacters("\n");
				}
			}
			writer.writeCharacters("      ");
			writer.writeEndElement(); // retenciones
			writer.writeCharacters("\n");

			// pagos
			writer.writeCharacters("      ");
			writer.writeStartElement("pagos");
			writer.writeCharacters("\n");
			writer.writeCharacters("        ");
			writer.writeStartElement("pago");
			writer.writeCharacters("\n");
			writeElement(writer, "formaPago", nvl(docResForPago, "01"), 10);
			writeElement(writer, "total",     fmt(docResTotal),         10);
			writer.writeCharacters("        ");
			writer.writeEndElement(); // pago
			writer.writeCharacters("\n");
			writer.writeCharacters("      ");
			writer.writeEndElement(); // pagos
			writer.writeCharacters("\n");

			writer.writeCharacters("    ");
			writer.writeEndElement(); // docSustento
			writer.writeCharacters("\n");
		}

		writer.writeCharacters("  ");
		writer.writeEndElement(); // docsSustento
		writer.writeCharacters("\n");

		// ── infoAdicional ─────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Soporte[" + facturadorTelefono + " - " + facturadorMail + "] " +
				"Contacto Cliente[" + proveedorTelefono + " - " + proveedorMail + "] " +
				"Observacion[" + nvl(retencion.getObservacion(), "") + "]");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoAdicional
		writer.writeCharacters("\n");

		writer.writeEndElement(); // comprobanteRetencion
		writer.writeEndDocument();
		writer.close();

		return stringWriter.toString();
	}
	
	private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
		for (int i = 0; i < indent / 2; i++) writer.writeCharacters("  ");
		writer.writeStartElement(name);
		writer.writeCharacters(value != null ? value : "");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private void writeImpuestoDocSustento(XMLStreamWriter writer, String codigo, String codigoPorcentaje,
			Double baseImponible, String tarifa, Double valorImpuesto) throws Exception {
		writer.writeCharacters("        ");
		writer.writeStartElement("impuestoDocSustento");
		writer.writeCharacters("\n");
		writeElement(writer, "codImpuestoDocSustento", codigo,               10);
		writeElement(writer, "codigoPorcentaje",        codigoPorcentaje,     10);
		writeElement(writer, "baseImponible",            fmt(baseImponible),   10);
		writeElement(writer, "tarifa",                   tarifa,               10);
		writeElement(writer, "valorImpuesto",            fmt(valorImpuesto),   10);
		writer.writeCharacters("        ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private String nvl(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}

	private double nvl(Double value, double defaultValue) {
		return value != null ? value : defaultValue;
	}

	/**
	 * Anula el asiento recién generado cuando falla el registro del pago sobre
	 * la factura de compra, para que nunca quede un asiento activo sin su
	 * aplicación correspondiente.
	 * @param asiento : Asiento a anular
	 * @param causa   : Error que impidió registrar el pago
	 */
	private void anulaAsientoPorFalloAplicacion(com.saa.model.cnt.Asiento asiento, Throwable causa) {
		if (asiento == null) {
			return;
		}
		try {
			asiento.setEstado(Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO));
			asiento.setMotivoAnulacion("Anulado automáticamente: no se pudo registrar el pago "
					+ "sobre la factura afectada. " + causa.getMessage());
			asiento.setFechaAnulacion(java.time.LocalDateTime.now());
			asiento.setUsuarioAnulacion("SISTEMA");
			em.merge(asiento);
			em.flush();
			System.err.println("⚠ Asiento " + asiento.getCodigo()
					+ " anulado porque falló el registro del pago: " + causa.getMessage());
		} catch (Exception e) {
			System.err.println("⚠ No se pudo anular el asiento tras el fallo de la aplicación: "
					+ e.getMessage());
		}
	}

	/**
	 * Construye el fragmento de observación con el número de los documentos
	 * origen (documentos sustento retenidos) para incluirlo en la observación
	 * del asiento contable.
	 * Los documentos sustento están en los detalles de la retención
	 * (NUMDOCRETEN); si hay varios se listan separados por coma.
	 * No interrumpe la generación del asiento: ante cualquier error devuelve
	 * cadena vacía.
	 * @param idRetencionV2 : Id de la retención V2
	 * @return : " | Doc. origen: XXX-XXX-XXXXXXXXX" o cadena vacía si no hay dato
	 */
	@SuppressWarnings("unchecked")
	private String observacionDocumentoOrigen(Long idRetencionV2) {
		if (idRetencionV2 == null) {
			return "";
		}
		try {
			List<String> documentos = em.createQuery(
					"SELECT DISTINCT d.numDocReten FROM DetalleRetencionV2 d "
					+ "WHERE d.retencionV2.id = :idRetencion "
					+ "AND d.numDocReten IS NOT NULL "
					+ "ORDER BY d.numDocReten")
					.setParameter("idRetencion", idRetencionV2)
					.getResultList();
			StringBuilder origen = new StringBuilder();
			for (String documento : documentos) {
				if (documento == null || documento.trim().isEmpty()) {
					continue;
				}
				if (origen.length() > 0) {
					origen.append(", ");
				}
				origen.append(documento.trim());
			}
			if (origen.length() == 0) {
				return "";
			}
			return " | Doc. origen: " + origen;
		} catch (Exception e) {
			System.err.println("⚠ No se pudo obtener el documento origen de la retención V2 "
					+ idRetencionV2 + ": " + e.getMessage());
			return "";
		}
	}

	private String fmt(Double value) {
		if (value == null) return "0.00";
		return String.format(java.util.Locale.US, "%.2f", value);
	}

	private String fmt(double value) {
		return String.format(java.util.Locale.US, "%.2f", value);
	}

	private String getBaseUploadDirectory() {
		String uploadDir = System.getProperty("saa.upload.dir");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}
		uploadDir = System.getenv("SAA_UPLOAD_DIR");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}
		String userHome = System.getProperty("user.home");
		String osName   = System.getProperty("os.name").toLowerCase();
		return osName.contains("windows") ? userHome + "/saa-uploads/" : "/opt/saa-uploads/";
	}
	
	@Override
	public java.util.Map<String, Object> autorizarRetencionV2(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarRetencionV2 con clave: " + clave);
		
		String respuesta = "";
		byte[] pdfBytesGenerado = null; // Variable para retornar los bytes del PDF
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/rtv2/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla prt2 (alterno=3)
			PathRetencionV2 pathF = new PathRetencionV2();
			RetencionV2 retencion = retencionV2DaoService.selectById(codigoRetencion, NombreEntidadesCobro.RETENCION_V2);
			pathF.setRetencionV2(retencion);
			pathF.setPath("resources/" + idFacturador + "/rtv2/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathRetencionV2DaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			retencion.setEstado(3L);
			retencionV2DaoService.save(retencion, retencion.getId());
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/rtv2/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/rtv2/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
					PathRetencionV2 pathE = new PathRetencionV2();
					pathE.setRetencionV2(retencion);
					pathE.setPath("resources/" + idFacturador + "/rtv2/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathRetencionV2DaoService.save(pathE, null);
					
					retencion.setEstado(4L);
					retencionV2DaoService.save(retencion, retencion.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						Thread.sleep(2000);
						
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								Path logWS2A = Paths.get(resourcesPath + "/rtv2/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								Path pathAutorizado = Paths.get(resourcesPath + "/rtv2/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								PathRetencionV2 pathA = new PathRetencionV2();
								pathA.setRetencionV2(retencion);
								pathA.setPath("resources/" + idFacturador + "/rtv2/a/" + clave + ".xml");
								pathA.setAlterno(5L);
								pathRetencionV2DaoService.save(pathA, null);
								
								retencion.setEstado(5L);
								retencion.setEstadoEmision(1L);
								retencion.setAutorizacion(resultado.numeroAutorizacion);
								retencion.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								retencionV2DaoService.save(retencion, retencion.getId());
								
								respuesta = resultado.estado;
								
							// Generar PDF RIDE de la retención V2 autorizada
							try {
								java.util.Map<String, Object> pdfParams = new java.util.HashMap<>();
								// CRÍTICO: P_ID_RETENCION_V2 es el parámetro que usa el JRXML
								// en su cláusula WHERE para obtener todos los datos.
								pdfParams.put("P_ID_RETENCION_V2", retencion.getId());
								byte[] pdfBytes = reporteService.generarReporte(
										"cxc", "RPRT_RIDE_RETENCION_V2", pdfParams, "PDF");
								if (pdfBytes != null && pdfBytes.length > 0) {
									Path pathPdf = Paths.get(resourcesPath + "/rtv2/a/" + clave + ".pdf");
									Files.write(pathPdf, pdfBytes);
									PathRetencionV2 pathPdfRec = new PathRetencionV2();
									pathPdfRec.setRetencionV2(retencion);
									pathPdfRec.setPath("resources/" + idFacturador + "/rtv2/a/" + clave + ".pdf");
									pathPdfRec.setAlterno(7L); // 7 = PDF RIDE
									pathRetencionV2DaoService.save(pathPdfRec, null);
									// Guardar los bytes del PDF para retornarlos
									pdfBytesGenerado = pdfBytes;
									System.out.println("✓ PDF RIDE retención V2 generado: " + pathPdf);
								}
							} catch (Exception pdfEx) {
								System.err.println("⚠ Error generando PDF retención V2 (no crítico): " + pdfEx.getMessage());
								pdfEx.printStackTrace();
							}

								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								Path logWS2N = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									Path pathNoAutorizado = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									PathRetencionV2 pathN = new PathRetencionV2();
									pathN.setRetencionV2(retencion);
									pathN.setPath("resources/" + idFacturador + "/rtv2/n/" + clave + ".xml");
									pathN.setAlterno(6L);
									pathRetencionV2DaoService.save(pathN, null);
								}
								
								retencion.setEstado(6L);
								retencion.setEstadoEmision(2L);
								retencionV2DaoService.save(retencion, retencion.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							Path logWS2Error = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/rtv2/n/" + clave + ".xml"));
							
							retencion.setEstado(6L);
							retencion.setEstadoEmision(2L);
							retencionV2DaoService.save(retencion, retencion.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						respuesta = "Estado: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							retencion.setAutorizacion(clave);
							retencion.setFechaAutorizacion(retencion.getFecha().plusMinutes(1).plusSeconds(15));
							retencion.setEstado(5L);
							retencionV2DaoService.save(retencion, retencion.getId());
						}
					}
					
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
				
			} else {
				respuesta = "Retencion Generada pero no enviada";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarRetencionV2: " + e.getMessage());
		}
		
		// Retornar mapa con mensaje y bytes del PDF
		java.util.Map<String, Object> resultadoFinal = new java.util.HashMap<>();
		resultadoFinal.put("mensaje", respuesta);
		resultadoFinal.put("pdfBytes", pdfBytesGenerado);
		return resultadoFinal;
	}
	
	private String llamarRecepcionSRI(String url, byte[] xmlBytes, PrintWriter log) throws Exception {
		try {
			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:rec=\"http://ec.gob.sri.ws.recepcion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<rec:validarComprobante><xml>" + xmlBase64 + "</xml></rec:validarComprobante>" +
				"</soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			System.out.println(">>> RESPUESTA COMPLETA WS1 SRI (RTV2):\n" + respuestaCompleta);
			log.println("Respuesta WS1 completa: " + respuestaCompleta);

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado WS1 extraído: [" + estado + "]");

				NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
				if (mensajeList.getLength() == 0) mensajeList = docEl.getElementsByTagName("mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						return "CLAVE ACCESO REGISTRADA";
					}
				}

				if ("DEVUELTA".equals(estado)) {
					StringBuilder sbErrores = new StringBuilder("DEVUELTA");
					NodeList mensajesDevuelta = docEl.getElementsByTagNameNS("*", "mensajeDevuelta");
					if (mensajesDevuelta.getLength() == 0) mensajesDevuelta = docEl.getElementsByTagName("mensajeDevuelta");
					System.err.println(">>> SRI rechazó la RTV2 (DEVUELTA). Errores: " + mensajesDevuelta.getLength());
					log.println(">>> Errores DEVUELTA: " + mensajesDevuelta.getLength());
					for (int i = 0; i < mensajesDevuelta.getLength(); i++) {
						org.w3c.dom.Node nodeMD = mensajesDevuelta.item(i);
						String identificador = extraerTextoHijo(nodeMD, "identificador");
						String msgError      = extraerTextoHijo(nodeMD, "mensaje");
						String infoAd        = extraerTextoHijo(nodeMD, "informacionAdicional");
						String tipo          = extraerTextoHijo(nodeMD, "tipo");
						String lineaError = " | [" + tipo + "] Id:" + identificador + " Msg:" + msgError + " Info:" + infoAd;
						sbErrores.append(lineaError);
						System.err.println("  ERROR SRI[" + i + "]: tipo=" + tipo + " | identificador=" + identificador + " | mensaje=" + msgError + " | informacionAdicional=" + infoAd);
						log.println("  ERROR SRI[" + i + "]: " + lineaError);
					}
					return sbErrores.toString();
				}
				return estado;
			}
			System.err.println(">>> ADVERTENCIA: No se encontró <estado> en la respuesta WS1 (RTV2)");
			return "SIN_RESPUESTA";
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarRecepcionSRI RTV2: " + e.getMessage());
			log.println("Error en llamarRecepcionSRI: " + e.getMessage());
			e.printStackTrace(log);
			throw e;
		}
	}

	private String extraerTextoHijo(org.w3c.dom.Node nodo, String nombreHijo) {
		if (nodo == null) return "";
		NodeList hijos = nodo.getChildNodes();
		for (int i = 0; i < hijos.getLength(); i++) {
			org.w3c.dom.Node hijo = hijos.item(i);
			if (nombreHijo.equals(hijo.getLocalName()) || nombreHijo.equals(hijo.getNodeName())) {
				return hijo.getTextContent() != null ? hijo.getTextContent().trim() : "";
			}
		}
		return "";
	}
	
	private ResultadoAutorizacion llamarAutorizacionSRI(String url, String claveAcceso) throws Exception {
		try {
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<aut:autorizacionComprobante><claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>" +
				"</aut:autorizacionComprobante></soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			System.out.println(">>> XML RESPUESTA WS2 (Autorización SRI - RTV2):\n" + respuestaCompleta);

			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				resultado.estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado WS2: " + resultado.estado);
			} else {
				System.err.println(">>> ADVERTENCIA: No se encontró <estado> en respuesta WS2 (RTV2)");
			}

			NodeList numAutList = docEl.getElementsByTagNameNS("*", "numeroAutorizacion");
			if (numAutList.getLength() > 0) resultado.numeroAutorizacion = numAutList.item(0).getTextContent();

			NodeList fechaAutList = docEl.getElementsByTagNameNS("*", "fechaAutorizacion");
			if (fechaAutList.getLength() > 0) resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();

			NodeList comprobanteList = docEl.getElementsByTagNameNS("*", "comprobante");
			if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();

			NodeList mensajeIdList = docEl.getElementsByTagNameNS("*", "identificador");
			if (mensajeIdList.getLength() > 0) resultado.mensajeId = mensajeIdList.item(0).getTextContent();

			NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
			if (mensajeList.getLength() > 0) resultado.mensaje = mensajeList.item(0).getTextContent();

			NodeList infoAdicionalList = docEl.getElementsByTagNameNS("*", "informacionAdicional");
			if (infoAdicionalList.getLength() > 0) resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();

			return resultado;
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarAutorizacionSRI RTV2: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	private LocalDateTime parseFechaAutorizacion(String fechaStr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			return LocalDateTime.parse(fechaStr, formatter);
		} catch (Exception e) {
			return LocalDateTime.now();
		}
	}
	
	private static class ResultadoAutorizacion {
		String estado;
		String numeroAutorizacion;
		String fechaAutorizacion;
		String comprobanteXML;
		String mensajeId;
		String mensaje;
		String informacionAdicional;
		String respuestaCompleta;
	}

	@Override
	public java.util.Map<String, Object> reenviarEmail(Long idRetencion, String destinatarios) throws Throwable {
		System.out.println("=== reenviarEmail RTV2 | id=" + idRetencion + " | destinatarios=" + destinatarios + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		if (destinatarios == null || destinatarios.trim().isEmpty()) {
			resultado.put("mensaje", "Debe especificar al menos un correo electrónico destinatario.");
			return resultado;
		}

		RetencionV2 retencion = retencionV2DaoService.selectById(idRetencion, com.saa.model.cxc.NombreEntidadesCobro.RETENCION_V2);
		if (retencion == null) {
			resultado.put("mensaje", "No se encontró la retención V2 con ID: " + idRetencion);
			return resultado;
		}
		if (!Long.valueOf(5L).equals(retencion.getEstado())) {
			resultado.put("mensaje", "Solo se puede reenviar el email de retenciones autorizadas. Estado actual: " + retencion.getEstado());
			return resultado;
		}

		String clave       = retencion.getClave();
		Long idFacturador  = retencion.getFacturador().getId();
		String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;

		// Leer XML autorizado
		String xmlAutorizado = null;
		try {
			java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/rtv2/a/" + clave + ".xml");
			if (java.nio.file.Files.exists(pXml))
				xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
		} catch (Exception e) { System.err.println("⚠ Error leyendo XML RTV2: " + e.getMessage()); }

		// Leer PDF, o regenerarlo si no existe (cubre documentos anteriores al fix)
		byte[] pdfBytes = null;
		try {
			java.nio.file.Path pPdf = java.nio.file.Paths.get(resourcesPath + "/rtv2/a/" + clave + ".pdf");
			if (java.nio.file.Files.exists(pPdf)) {
				pdfBytes = java.nio.file.Files.readAllBytes(pPdf);
				System.out.println("✓ PDF RTV2 leído desde disco.");
			} else {
				System.out.println("ℹ PDF RTV2 no encontrado en disco. Regenerando...");
				try {
					java.util.Map<String, Object> pdfParams = new java.util.HashMap<>();
					pdfParams.put("P_ID_RETENCION_V2", retencion.getId());
					pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_RETENCION_V2", pdfParams, "PDF");
					if (pdfBytes != null && pdfBytes.length > 0) {
						java.nio.file.Files.createDirectories(pPdf.getParent());
						java.nio.file.Files.write(pPdf, pdfBytes);
						System.out.println("✓ PDF RTV2 regenerado y guardado en disco.");
					}
				} catch (Exception pdfEx) {
					System.err.println("⚠ No se pudo regenerar el PDF RTV2: " + pdfEx.getMessage());
				}
			}
		} catch (Exception e) { System.err.println("⚠ Error leyendo/regenerando PDF RTV2: " + e.getMessage()); }

		String razonSocial = retencion.getFacturador() != null
				? nvl(retencion.getFacturador().getRazonSocial(), nvl(retencion.getFacturador().getNombre(), "")) : "";
		String numeroDoc = nvl(retencion.getNumero(), clave);

		String[] lista = destinatarios.split(";");
		java.util.List<String> enviados = new java.util.ArrayList<>();
		java.util.List<String> fallidos = new java.util.ArrayList<>();
		for (String mail : lista) {
			String m = mail.trim();
			if (m.isEmpty()) continue;
			try {
				emailFacturaService.enviarFacturaAutorizada(m, numeroDoc, clave, razonSocial, "Retención", xmlAutorizado, pdfBytes);
				enviados.add(m);
			} catch (Exception e) { fallidos.add(m + " (" + e.getMessage() + ")"); }
		}

		resultado.put("emailsEnviados", enviados);
		resultado.put("emailsFallidos", fallidos);
		resultado.put("clave", clave);
		if (!enviados.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado a " + enviados.size() + " destinatario(s).");
		} else {
			resultado.put("mensaje", "No se pudo enviar el email a ningún destinatario.");
		}
		return resultado;
	}

	@Override
	public java.util.Map<String, Object> procesarRetencionV2Completa(RetencionV2 retencion,
			java.util.List<DetalleRetencionV2> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {

		System.out.println("=== INICIANDO PROCESO COMPLETO DE RETENCIÓN V2 ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// ── PASO 0: Validar configuración contable ANTES de grabar ─────────────
		if (retencion.getFacturador() != null
				&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {

			if (retencion.getFacturador().getEmpresa() == null) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
						+ "pero no tiene empresa contable configurada.");
				return resultado;
			}

			Long idEmpresa = retencion.getFacturador().getEmpresa().getCodigo();
			System.out.println("PASO 0: Validando cuentas contables para empresa " + idEmpresa + "...");

			// Convertir DetalleRetencionV2 → DetalleRetencion para reutilizar validador
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalesParaValidar = new java.util.ArrayList<>();
			if (detalles != null) {
				for (DetalleRetencionV2 d : detalles) {
					com.saa.model.cxc.DetalleRetencion dr = new com.saa.model.cxc.DetalleRetencion();
					dr.setCodRetencion(d.getCodRetencion());
					dr.setValorReten(d.getValorReten());
					detalesParaValidar.add(dr);
				}
			}
			// Construir objeto Retencion "dummy" para reutilizar validarCuentasContablesRetencion
			com.saa.model.cxc.Retencion retencionDummy = new com.saa.model.cxc.Retencion();
			retencionDummy.setProveedor(retencion.getProveedor());

			java.util.List<String> erroresContables =
					asientoContableService.validarCuentasContablesRetencion(retencionDummy, detalesParaValidar, idEmpresa);

			if (!erroresContables.isEmpty()) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "No se puede emitir la retención V2: faltan cuentas contables.");
				resultado.put("erroresContables", erroresContables);
				StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
				for (int i = 0; i < erroresContables.size(); i++)
					sb.append("  ").append(i + 1).append(". ").append(erroresContables.get(i)).append("\n");
				resultado.put("error", sb.toString());
				System.err.println("✗ Validación contable fallida:\n" + sb);
				return resultado;
			}
			System.out.println("✓ Validación contable OK.");
		}

		// ── PASO 0.1: La factura de compra a la que afecta debe existir ────────
		// El pago a la factura se registra junto con el asiento, así que sin
		// factura no se puede emitir la retención: se valida ANTES de firmar y
		// enviar al SRI, cuando todavía se puede abortar sin consecuencias.
		if (retencion.getFacturador() != null
				&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())
				&& retencion.getFacturador().getEmpresa() != null) {
			try {
				Long idEmpresaValida = retencion.getFacturador().getEmpresa().getCodigo();
				Long idProveedorValida = (retencion.getProveedor() != null)
						? retencion.getProveedor().getCodigo() : null;
				java.util.Set<String> documentos = new java.util.LinkedHashSet<>();
				if (detalles != null) {
					for (DetalleRetencionV2 detalle : detalles) {
						if (detalle.getNumDocReten() != null
								&& !detalle.getNumDocReten().trim().isEmpty()) {
							documentos.add(detalle.getNumDocReten().trim());
						}
					}
				}
				if (documentos.isEmpty()) {
					resultado.put("etapa", "VALIDACION_FACTURA");
					resultado.put("mensaje", "La retención no indica el número de la factura de compra "
							+ "a la que afecta (documento sustento).");
					return resultado;
				}
				for (String numeroDocumento : documentos) {
					aplicacionPagoCxpService.resolverFacturaCompraPorNumero(
							numeroDocumento, idProveedorValida, idEmpresaValida);
				}
				System.out.println("✓ Factura(s) de compra afectada(s) verificada(s): " + documentos);
			} catch (Throwable e) {
				resultado.put("etapa", "VALIDACION_FACTURA");
				resultado.put("mensaje", e.getMessage());
				resultado.put("error", e.getMessage());
				System.err.println("✗ Validación de factura afectada fallida: " + e.getMessage());
				return resultado;
			}
		}

		try {
			if (ambiente  == null) ambiente  = 1L;
			if (conectaSRI == null) conectaSRI = 1L;
			if (pathLogo  == null) pathLogo  = "resources/logos/logo_aso.png";

			// ── PASO 1: Grabar retención V2 ────────────────────────────────────
			System.out.println("PASO 1: Grabando retención V2 en base de datos...");
			try {
				retencion = this.saveSingle(retencion);
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_RETENCION");
				resultado.put("mensaje", "Error al grabar la retención V2: " + e.getMessage());
				resultado.put("error", e.getMessage());
				sessionContext.setRollbackOnly();
				return resultado;
			}
			resultado.put("retencion",   retencion);
			resultado.put("idRetencion", retencion.getId());
			System.out.println("✓ Retención V2 grabada ID: " + retencion.getId()
					+ " | Clave: " + retencion.getClave());

			// ── PASO 1.5: Guardar detalles ─────────────────────────────────────
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 1.5: Guardando " + detalles.size() + " detalles de retención V2...");
				try {
					for (DetalleRetencionV2 detalle : detalles) {
						detalle.setRetencionV2(retencion);
						if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(Estado.ACTIVO));
						em.persist(detalle);
					}
					em.flush();
					System.out.println("✓ Detalles V2 guardados correctamente.");
				} catch (Exception e) {
					resultado.put("etapa", "GRABADO_DETALLES");
					resultado.put("mensaje", "Error al grabar los detalles de la retención V2: " + e.getMessage());
					resultado.put("error", e.getMessage());
					sessionContext.setRollbackOnly();
					return resultado;
				}
			}

			if (destinatario == null && retencion.getProveedor() != null)
				destinatario = retencion.getProveedor().getEmail();

			String clave = retencion.getClave();
			if (clave == null || clave.isEmpty())
				throw new Exception("La retención V2 no tiene clave de acceso");
			Long idFacturador = retencion.getFacturador() != null ? retencion.getFacturador().getId() : null;
			if (idFacturador == null)
				throw new Exception("La retención V2 no tiene facturador asociado");

			resultado.put("claveAcceso", clave);
			// Usar el ambiente del facturador (ya resuelto en saveSingle) — NO del frontend
			ambiente = retencion.getAmbiente();
			if (ambiente == null) ambiente = 1L;
			if (conectaSRI == null) conectaSRI = 1L;
			System.out.println(">>> AMBIENTE (desde facturador BD): " + ambiente
					+ (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)") + " | CONECTA_SRI: " + conectaSRI);

			// ── PASO 2 y 3: Generar y firmar XML ──────────────────────────────
			String xmlFirmado;
			try {
				System.out.println("PASO 2: Generando XML de retención V2...");
				String[] resultadoXML = this.generarXMLRetencionV2(clave, ambiente);
				String xmlSinFirmar = new String(
						java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(resultadoXML[2])),
						java.nio.charset.StandardCharsets.UTF_8);
				System.out.println("PASO 3: Firmando XML...");
				xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
				System.out.println("✓ XML generado y firmado.");
			} catch (Exception e) {
				resultado.put("etapa", "GENERACION_XML");
				resultado.put("mensaje", "Error al generar o firmar el XML de la retención V2: " + e.getMessage());
				resultado.put("error", e.getMessage());
				sessionContext.setRollbackOnly();
				return resultado;
			}

			// ── PASO 4: Autorizar ante el SRI ──────────────────────────────────
			System.out.println("PASO 4: Autorizando ante el SRI...");
			String resultadoAutorizacion;
			byte[] pdfBytesParaEmail = null; // Bytes del PDF generado en autorizarRetencionV2
			try {
				java.util.Map<String, Object> resultadoAuth = this.autorizarRetencionV2(
						idFacturador, ambiente, conectaSRI, clave,
						retencion.getId(), xmlFirmado, destinatario, pathLogo);
				resultadoAutorizacion = (String) resultadoAuth.get("mensaje");
				pdfBytesParaEmail = (byte[]) resultadoAuth.get("pdfBytes");
			} catch (Exception e) {
				resultado.put("etapa", "ERROR_AUTORIZACION_SRI");
				resultado.put("mensaje", "Error al comunicarse con el SRI: " + e.getMessage());
				resultado.put("error", e.getMessage());
				sessionContext.setRollbackOnly();
				return resultado;
			}

			resultado.put("autorizacion", resultadoAutorizacion);
			System.out.println(">>> Respuesta completa autorización RTV2: [" + resultadoAutorizacion + "]");

			boolean autorizada = resultadoAutorizacion != null
					&& resultadoAutorizacion.contains("AUTORIZADO");

			if (!autorizada) {
				// DEVUELTA = el SRI rechazó el XML por errores de formato/estructura.
				// El registro NO debe quedar en BD → rollback.
				if (resultadoAutorizacion != null && resultadoAutorizacion.contains("DEVUELTA")) {
					System.err.println("✗ RTV2 DEVUELTA por el SRI (error de formato XML). Haciendo rollback.");
					resultado.put("exito",   false);
					resultado.put("etapa",   "XML_DEVUELTO");
					resultado.put("estado",  "DEVUELTA");
					resultado.put("mensaje", "El SRI rechazó el XML de la retención V2 por errores de formato. "
							+ "Detalle: " + resultadoAutorizacion);
					sessionContext.setRollbackOnly();
					return resultado;
				}
				// Cualquier otro caso (NO_AUTORIZADO, etc.) → registro queda como evidencia.
				System.err.println("✗ RTV2 no autorizada por el SRI: " + resultadoAutorizacion);
				resultado.put("exito",   false);
				resultado.put("etapa",   "AUTORIZACION_SRI");
				resultado.put("estado",  "NO_AUTORIZADO");
				resultado.put("mensaje", "La retención V2 fue enviada al SRI pero no fue autorizada. "
						+ "Respuesta: " + resultadoAutorizacion);
				return resultado;
			}

			System.out.println("✓ Retención V2 AUTORIZADA por el SRI.");
			resultado.put("estado", "AUTORIZADO");

			// ── PASO 5: Generar asiento contable ──────────────────────────────
			if (retencion.getFacturador().getEmpresa() != null
					&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {
				System.out.println("PASO 5: Generando asiento contable de Retención V2...");
				try {
					Long idEmpresaConta = retencion.getFacturador().getEmpresa().getCodigo();
					// retencion ya está en memoria — sin recargar desde BD
					java.time.LocalDate fechaAsiento = retencion.getFecha() != null
							? retencion.getFecha().toLocalDate() : java.time.LocalDate.now();
					String obsAsiento = "Retención V2 N° " + nvl(retencion.getNumero(), clave)
							+ observacionDocumentoOrigen(retencion.getId())
							+ " | Proveedor: " + (retencion.getProveedor() != null
									? retencion.getProveedor().getNombre() : "")
							+ " | Aut: " + nvl(retencion.getAutorizacion(), clave);
					String usuarioAsiento = (retencion.getUsuario() != null)
							? retencion.getUsuario().getNombre() : "SISTEMA";
				com.saa.model.cnt.Asiento asientoGenerado =
						asientoContableService.generarAsientoRetencionV2(
								retencion.getId(), idEmpresaConta,
								TipoAsientos.RETENCIONES_EMITIDAS_V2,
								fechaAsiento, obsAsiento, usuarioAsiento);
				// Vincular el asiento a la retención V2 (igual que Factura, NotaDebito, NotaCredito)
				com.saa.model.cnt.Asiento asientoAttached =
						em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
				if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
				retencion.setAsiento(asientoAttached);
				retencionV2DaoService.save(retencion, retencion.getId());
				resultado.put("asiento", asientoGenerado.getNumeroAlterno());
					System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());

					// Registrar el abono a la factura de compra junto con el asiento:
					// si el pago no se puede registrar, el asiento no debe quedar activo.
					try {
						aplicacionPagoCxpService.aplicarRetencionEmitida(
								retencion, asientoAttached, idEmpresaConta, usuarioAsiento);
					} catch (Throwable aplEx) {
						anulaAsientoPorFalloAplicacion(asientoAttached, aplEx);
						throw aplEx;
					}
				} catch (Throwable e) {
					resultado.put("advertenciaAsiento",
							"Retención V2 autorizada pero ocurrió un error al generar el asiento "
							+ "o al registrar el pago de la factura: " + e.getMessage()
							+ ". Revise la factura afectada y genere el asiento manualmente desde Contabilidad.");
					System.err.println("⚠ Error en asiento contable de Retención V2: " + e.getMessage());
					e.printStackTrace();
				}
			}

		// ── PASO 6: Enviar correo electrónico ─────────────────────────────
		System.out.println("PASO 6: Enviando email al proveedor...");
		try {
			if (destinatario != null && !destinatario.trim().isEmpty()) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				try {
					java.nio.file.Path pXml = java.nio.file.Paths.get(
							resourcesPath + "/rtv2/a/" + clave + ".xml");
					if (java.nio.file.Files.exists(pXml))
						xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
				} catch (Exception ioEx) {
					System.err.println("⚠ No se pudo leer el XML para el email: " + ioEx.getMessage());
				}
				// Usar directamente los bytes del PDF que retornó autorizarRetencionV2
				// en lugar de intentar leerlos del disco, evitando problemas de sincronización
				String razonSocial = retencion.getFacturador() != null
						? nvl(retencion.getFacturador().getRazonSocial(),
							  nvl(retencion.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(
						destinatario, nvl(retencion.getNumero(), clave),
						clave, razonSocial, "Retención", xmlAutorizado, pdfBytesParaEmail);
				resultado.put("emailEnviado", true);
				System.out.println("✓ Email enviado a: " + destinatario);
			} else {
				resultado.put("emailEnviado", false);
				System.out.println("ℹ Email omitido: sin dirección de correo del proveedor.");
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail",
					"Retención V2 autorizada pero no se pudo enviar el email: "
					+ mailEx.getMessage() + ". Reenvíe manualmente.");
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

			// ── FIN ────────────────────────────────────────────────────────────
			resultado.put("exito",   true);
			resultado.put("etapa",   "COMPLETADO");
			resultado.put("mensaje", "Retención V2 procesada y autorizada exitosamente.");
			System.out.println("=== PROCESO COMPLETO DE RETENCIÓN V2 FINALIZADO ===");

		} catch (Exception e) {
			System.err.println("ERROR inesperado en procesarRetencionV2Completa: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito",   false);
			resultado.put("etapa",   "ERROR_INESPERADO");
			resultado.put("error",   e.getMessage());
			resultado.put("mensaje", "Error inesperado al procesar la retención V2: " + e.getMessage());
		sessionContext.setRollbackOnly();
		throw e;
	}
	return resultado;
}

@Override
public java.util.Map<String, Object> anularRetencionV2(Long idRetencion, String motivo, String usuario) throws Throwable {
	System.out.println("=== anularRetencionV2 | idRetencion=" + idRetencion + " | usuario=" + usuario + " ===");

	java.util.Map<String, Object> resultado = new java.util.HashMap<>();
	resultado.put("exito", false);

	// 1. Cargar la retención V2
	RetencionV2 retencion = retencionV2DaoService.selectById(idRetencion, NombreEntidadesCobro.RETENCION_V2);
	if (retencion == null) {
		resultado.put("mensaje", "Retención V2 con ID " + idRetencion + " no encontrada.");
		return resultado;
	}

	// 2. Validar que no esté ya anulada
	if (Long.valueOf(com.saa.rubros.Estado.INACTIVO).equals(retencion.getEstado())) {
		resultado.put("mensaje", "La retención V2 ya se encuentra anulada.");
		return resultado;
	}

	// 3. Datos de anulación
	String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
	String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
	java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

	// 3.5. Reversar el abono que esta retención hizo sobre la factura de compra
	try {
		int reversadas = aplicacionPagoCxpService.revertirAplicacionesDeDocumento(
				"RETENCION_V2", idRetencion, motivoFinal, null);
		if (reversadas > 0) {
			resultado.put("aplicacionesReversadas", reversadas);
			System.out.println("✓ Aplicaciones de pago reversadas: " + reversadas);
		}
	} catch (Exception e) {
		System.err.println("⚠ Error al reversar las aplicaciones de pago: " + e.getMessage());
		resultado.put("advertenciaAplicacion",
				"La retención V2 fue anulada pero ocurrió un error al reversar el pago "
				+ "aplicado a la factura: " + e.getMessage());
	}

	// 4. Anular asiento contable vinculado (si existe)
	if (retencion.getAsiento() != null && retencion.getAsiento().getCodigo() != null) {
		try {
			com.saa.model.cnt.Asiento asiento = em.find(
					com.saa.model.cnt.Asiento.class, retencion.getAsiento().getCodigo());
			if (asiento != null && !Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO).equals(asiento.getEstado())) {
				asiento.setEstado(Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO));
				asiento.setMotivoAnulacion(motivoFinal);
				asiento.setFechaAnulacion(ahora);
				asiento.setUsuarioAnulacion(usuarioAnulacion);
				em.merge(asiento);
				em.flush();
				System.out.println("✓ Asiento contable anulado: " + asiento.getCodigo());
				resultado.put("asientoAnulado", asiento.getCodigo());
			}
		} catch (Exception e) {
			System.err.println("⚠ Error al anular asiento contable: " + e.getMessage());
			resultado.put("advertenciaAsiento",
					"La retención V2 fue anulada pero ocurrió un error al anular el asiento: " + e.getMessage());
		}
	}

	// 5. Anular la retención V2 y registrar datos de anulación
	retencion.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));
	retencion.setEstadoEmision(3L); // 3 = ANULADA
	retencion.setMotivoAnulacion(motivoFinal);
	retencion.setFechaAnulacion(ahora);
	retencion.setUsuarioAnulacion(usuarioAnulacion);
	retencionV2DaoService.save(retencion, retencion.getId());
	em.flush();

	System.out.println("✓ Retención V2 anulada: " + idRetencion
			+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

	resultado.put("exito", true);
	resultado.put("mensaje", "Retención V2 N° " + nvl(retencion.getNumero(), String.valueOf(idRetencion))
			+ " anulada correctamente.");
	resultado.put("idRetencion", idRetencion);
	resultado.put("motivoAnulacion", motivoFinal);
	resultado.put("fechaAnulacion", ahora.toString());
	resultado.put("usuarioAnulacion", usuarioAnulacion);

	return resultado;
}

// =========================================================================
// consultarYActualizarEstadoRetencionV2
// =========================================================================
@Override
public java.util.Map<String, Object> consultarYActualizarEstadoRetencionV2(Long idRetencion) throws Throwable {
	System.out.println("=== consultarYActualizarEstadoRetencionV2 | idRetencion=" + idRetencion + " ===");
	java.util.Map<String, Object> resultado = new java.util.HashMap<>();
	resultado.put("exito", false);

	// 1. Cargar la retención V2
	RetencionV2 retencion = retencionV2DaoService.selectById(idRetencion, NombreEntidadesCobro.RETENCION_V2);
	if (retencion == null) {
		resultado.put("mensaje", "Retención V2 con ID " + idRetencion + " no encontrada.");
		return resultado;
	}
	if (retencion.getClave() == null || retencion.getClave().isEmpty()) {
		resultado.put("mensaje", "La retención V2 no tiene clave de acceso registrada.");
		return resultado;
	}

	Long ambiente = retencion.getAmbiente() != null ? retencion.getAmbiente() : 1L;
	String clave = retencion.getClave();
	Long idFacturador = retencion.getFacturador() != null ? retencion.getFacturador().getId() : null;
	resultado.put("clave", clave);
	resultado.put("estadoActual", retencion.getEstado());

	// 2. Consultar estado al SRI
	String urlWS2 = ambiente == 2L
			? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
			: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
	System.out.println(">>> Consultando estado al SRI: " + urlWS2);

	ResultadoAutorizacion ra;
	try {
		ra = llamarAutorizacionSRI(urlWS2, clave);
	} catch (Exception e) {
		resultado.put("mensaje", "Error al consultar el estado en el SRI: " + e.getMessage());
		resultado.put("error", e.getMessage());
		return resultado;
	}

	resultado.put("estadoSRI", ra.estado);
	resultado.put("numeroAutorizacion", ra.numeroAutorizacion);
	resultado.put("fechaAutorizacion", ra.fechaAutorizacion);
	System.out.println(">>> Estado SRI: " + ra.estado);

	if (!"AUTORIZADO".equals(ra.estado)) {
		resultado.put("mensaje", "El SRI indica que la retención V2 NO está autorizada. Estado: " + ra.estado
				+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
		return resultado;
	}

	// 3. SRI devuelve AUTORIZADO → actualizar retención
	boolean actualizada = false;
	if (!Long.valueOf(5L).equals(retencion.getEstado())) {
		retencion.setEstado(5L);
		retencion.setEstadoEmision(1L);
		if (ra.numeroAutorizacion != null && !ra.numeroAutorizacion.isEmpty()) {
			retencion.setAutorizacion(ra.numeroAutorizacion);
		}
		if (ra.fechaAutorizacion != null && !ra.fechaAutorizacion.isEmpty()) {
			retencion.setFechaAutorizacion(parseFechaAutorizacion(ra.fechaAutorizacion));
		}
		// Guardar XML autorizado en disco
		if (ra.comprobanteXML != null && !ra.comprobanteXML.isEmpty() && idFacturador != null) {
			try {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				java.nio.file.Path pathAutorizado = java.nio.file.Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
				java.nio.file.Files.createDirectories(pathAutorizado.getParent());
				java.nio.file.Files.write(pathAutorizado, ra.comprobanteXML.getBytes("UTF-8"));
				PathRetencionV2 pathA = new PathRetencionV2();
				pathA.setRetencionV2(retencion);
				pathA.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".xml");
				pathA.setAlterno(5L);
				pathRetencionV2DaoService.save(pathA, null);
				System.out.println("✓ XML autorizado guardado en disco.");
			} catch (Exception xmlEx) {
				System.err.println("⚠ Error guardando XML autorizado: " + xmlEx.getMessage());
			}
		}
		retencionV2DaoService.save(retencion, retencion.getId());
		actualizada = true;
		System.out.println("✓ Retención V2 actualizada a estado AUTORIZADA (5). Aut: " + ra.numeroAutorizacion);
	} else {
		System.out.println("ℹ Retención V2 ya estaba en estado 5 (autorizada).");
	}
	resultado.put("retencionActualizada", actualizada);

	// 4. Generar asiento contable si no tiene y el facturador lo requiere
	boolean asientoGenerado = false;
	if (retencion.getAsiento() == null
			&& retencion.getFacturador() != null
			&& retencion.getFacturador().getEmpresa() != null
			&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {
		System.out.println("PASO 4: Generando asiento contable...");
		try {
			Long idEmpresa = retencion.getFacturador().getEmpresa().getCodigo();
			String obsAsiento = "Retención V2 N° " + nvl(retencion.getNumero(), clave)
					+ observacionDocumentoOrigen(retencion.getId())
					+ " | Proveedor: " + (retencion.getProveedor() != null ? retencion.getProveedor().getNombre() : "")
					+ " | Aut: " + nvl(retencion.getAutorizacion(), clave);
			String usuarioAsiento = retencion.getUsuario() != null ? retencion.getUsuario().getNombre() : "SISTEMA";
			com.saa.model.cnt.Asiento asientoGeneradoObj =
					asientoContableService.generarAsientoRetencionV2(
							retencion.getId(), idEmpresa,
							com.saa.rubros.TipoAsientos.FACTURAS_VENTA,
							retencion.getFecha().toLocalDate(), obsAsiento, usuarioAsiento);
			com.saa.model.cnt.Asiento asientoAttached = em.merge(asientoGeneradoObj);
			retencion.setAsiento(asientoAttached);
			retencionV2DaoService.save(retencion, retencion.getId());
			em.flush();
			resultado.put("asiento", asientoGeneradoObj.getNumeroAlterno());
			asientoGenerado = true;
			System.out.println("✓ Asiento contable generado: " + asientoGeneradoObj.getNumeroAlterno());

			// Registrar el abono a la factura de compra junto con el asiento.
			try {
				aplicacionPagoCxpService.aplicarRetencionEmitida(
						retencion, asientoAttached, idEmpresa, usuarioAsiento);
			} catch (Throwable aplEx) {
				anulaAsientoPorFalloAplicacion(asientoAttached, aplEx);
				throw aplEx;
			}
		} catch (Throwable e) {
			resultado.put("advertenciaAsiento",
					"Retención V2 autorizada pero error al generar asiento o al registrar "
					+ "el pago de la factura: " + e.getMessage());
			System.err.println("⚠ Error en asiento contable: " + e.getMessage());
		}
	} else if (retencion.getAsiento() != null) {
		resultado.put("asientoExistente", retencion.getAsiento().getNumeroAlterno());
		System.out.println("ℹ La retención V2 ya tiene asiento contable: " + retencion.getAsiento().getNumeroAlterno());
	}
	resultado.put("asientoGenerado", asientoGenerado);

	// 5. Enviar email con XML autorizado (y PDF si existe en disco)
	System.out.println("PASO 5: Enviando email al proveedor...");
	String destinatario = null;
	if (retencion.getProveedor() != null) destinatario = retencion.getProveedor().getEmail();
	try {
		if (destinatario != null && !destinatario.trim().isEmpty() && idFacturador != null) {
			String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
			String xmlAutorizado = null;
			byte[] pdfBytes = null;
			try {
				java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
				if (java.nio.file.Files.exists(pXml))
					xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
				// Intentar leer PDF si existe (las retenciones V2 pueden tener PDF generado previamente)
				java.nio.file.Path pPdf = java.nio.file.Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
				if (java.nio.file.Files.exists(pPdf)) {
					pdfBytes = java.nio.file.Files.readAllBytes(pPdf);
					System.out.println("✓ PDF RIDE leído desde disco.");
				}
			} catch (Exception ioEx) {
				System.err.println("⚠ Error leyendo archivos para email: " + ioEx.getMessage());
			}
			String razonSocial = retencion.getFacturador() != null
					? nvl(retencion.getFacturador().getRazonSocial(), nvl(retencion.getFacturador().getNombre(), "")) : "";
			emailFacturaService.enviarFacturaAutorizada(
					destinatario, nvl(retencion.getNumero(), clave),
					clave, razonSocial, "Retención V2", xmlAutorizado, pdfBytes);
			resultado.put("emailEnviado", true);
			resultado.put("emailDestinatario", destinatario);
			System.out.println("✓ Email enviado a: " + destinatario);
		} else {
			resultado.put("emailEnviado", false);
			System.out.println("ℹ Email omitido: no hay dirección de correo del proveedor.");
		}
	} catch (Exception mailEx) {
		resultado.put("advertenciaEmail",
				"Retención V2 autorizada pero no se pudo enviar el email: " + mailEx.getMessage());
		resultado.put("emailEnviado", false);
		System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
	}

	resultado.put("exito", true);
	resultado.put("mensaje", "Retención V2 verificada en el SRI: AUTORIZADA."
			+ (actualizada ? " Estado actualizado." : "")
			+ (asientoGenerado ? " Asiento contable generado." : "")
			+ (Boolean.TRUE.equals(resultado.get("emailEnviado")) ? " Email enviado a " + destinatario + "." : ""));
	System.out.println("=== consultarYActualizarEstadoRetencionV2 COMPLETADO ===");
	return resultado;
}
}
