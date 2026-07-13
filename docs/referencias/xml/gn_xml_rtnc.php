<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_retencion($dbConn, $clave, $ambiente){
    $tipoDoc = "07"; // Segun tabla 3 del SRI para Factura
    // 1 -- Datos Facturador, Proveedor y Factura
    $sql = $dbConn->prepare("select b.*, c.numdoc 'RUC', c.nombre 'LOCAL',
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail
        from rtnc b, fcdr c, prvd d
        where b.facturador=c.id
        and b.proveedor = d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idReten=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from rtnc a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idReten);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];
    
    // 3 -- Detalle Factura
    $sql = $dbConn->prepare("SELECT * FROM dtrt
        WHERE retencion=:retencion");
    $sql->bindValue(':retencion', $idReten);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detRetencion=$sql->fetchAll();
    
    $dbConn = null;
    /* ************************************************************** FIN de selects  */
    $idFacturador = $primero['facturador'];
    $RUC=$primero['RUC'];
    $idAmbiente = $ambiente;
    $idEmision = "1";
    $numEstb = $primero['numEstablecimiento'];
    $numPtoEmision = $primero['numPtoEmision'];
    $secuencial = $primero['secuencial'];
    $claveAcceso = $primero['clave'];
    $nombreFcdr = $primero['LOCAL'];
    $razonFcdr = $primero['razonSocial'];
    $direccionFcdr = $primero['DIRECCION'];
    $telefonoFcdr = $primero['FONO'];
    $mailFcdr = $primero['MAIL'];
    $microEmpresaFcdr = $primero['microEmpresa'];
    $rimpeFcdr = $primero['RIMPE'];
    $rimpePopularFcdr = $primero['popularRIMPE'];
    $contribuyenteFcdr = $primero['contribuyenteEspecial'];
    $agenteRetencionFcdr = $primero['agenteRetencion'];
    $contabilidadFcdr = 'NO';
    if($primero['contabilidad']){
        if($primero['contabilidad']==1){
            $contabilidadFcdr = 'SI';
        }
    }
    $tipoIdCmdr = $primero['tipoId'];
    $nombreCmdr=$primero['nombre'];
    $numDocCmdr=$primero['numdoc'];
    $periodoFiscal=$primero['periodoFiscal'];
    $mailCmdr=$primero['mail'];
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];
    // ************************************************************
    //Inicia XML
    $writer = new XMLWriter();
    $writer->openMemory();
    $writer->startDocument('1.0', 'UTF-8');
    $writer->setIndent(true);
    $writer->startElement("comprobanteRetencion");
    $writer->writeAttribute('id','comprobante');
    $writer->writeAttribute('version','1.0.0');
    
    $writer->startElement('infoTributaria');
    $writer->writeElement('ambiente',$idAmbiente);
    $writer->writeElement('tipoEmision',$idEmision);
    $writer->writeElement('razonSocial', $razonFcdr);
    $writer->writeElement('nombreComercial', $nombreFcdr);
    $writer->writeElement('ruc', $RUC);
    $writer->writeElement('claveAcceso', $claveAcceso);
    $writer->writeElement('codDoc', $tipoDoc);
    $writer->writeElement('estab', $numEstb);
    $writer->writeElement('ptoEmi', $numPtoEmision);
    $writer->writeElement('secuencial', $secuencial);
    $writer->writeElement('dirMatriz', $direccionFcdr);
    if($microEmpresaFcdr){
        if($microEmpresaFcdr == 1){
            $writer->writeElement('regimenMicroempresas', "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS");
        }
    }
    if($agenteRetencionFcdr){
        if($agenteRetencionFcdr != ''){
            $writer->writeElement('agenteRetencion', $agenteRetencionFcdr);
        }
    }
    if($rimpeFcdr){
        if($rimpeFcdr == 1){
            $writer->writeElement('contribuyenteRimpe', "CONTRIBUYENTE RÉGIMEN RIMPE");
        }
    }
    if($rimpePopularFcdr){
        if($rimpePopularFcdr == 1){
            $writer->writeElement('contribuyenteRimpe', "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE");
        }
    }
    $writer->endElement();
    
    $writer->startElement('infoCompRetencion');
    $writer->writeElement('fechaEmision', date("d/m/Y", strtotime( $primero['fecha'])));
    $writer->writeElement('dirEstablecimiento', $dirEstb);
    if($contribuyenteFcdr){
        if($contribuyenteFcdr != ''){
            $writer->writeElement('contribuyenteEspecial', $contribuyenteFcdr); // Verificar cómo se incluye esta información en el XML
        }
    }
    $writer->writeElement('obligadoContabilidad', $contabilidadFcdr);
    $writer->writeElement('tipoIdentificacionSujetoRetenido', $tipoIdCmdr);
    $writer->startElement('razonSocialSujetoRetenido');
    $writer->text($nombreCmdr);
    $writer->endElement();
    $writer->writeElement('identificacionSujetoRetenido', $numDocCmdr);
    $writer->writeElement('periodoFiscal', $periodoFiscal);
    $writer->endElement();

    $writer->startElement('impuestos');
    foreach ($detRetencion as $detalle){
        $writer->startElement('impuesto');
        $writer->writeElement('codigo', $detalle['codImpuesto']);
        $writer->writeElement('codigoRetencion', $detalle['codRetencion']);
        $writer->writeElement('baseImponible', $detalle['baseImponible']);
        $writer->writeElement('porcentajeRetener', $detalle['porcentajeReten']);
        $writer->writeElement('valorRetenido', $detalle['valorReten']);
        
        $writer->writeElement('codDocSustento', $detalle['tipoDocReten']);
        $writer->writeElement('numDocSustento', $detalle['numDocReten']);
        $writer->writeElement('fechaEmisionDocSustento', date("d/m/Y",strtotime($detalle['fechaEmiDoc'])));
        $writer->endElement();
    }
    $writer->endElement();
    
    $writer->startElement('infoAdicional');
    $writer->startElement('campoAdicional');
    $writer->writeAttribute('nombre','Datos Adicionales');
    $infoAdicional = "Soporte[$telefonoFcdr - $mailFcdr] ".
        "Contacto Cliente[$iaFonos - $mailCmdr] Observacion[$iaObaservacion]";
    $writer->text($infoAdicional);
    $writer->endElement();
    $writer->endElement();
    
    $writer->endElement();
    $writer->endDocument();
    $xml = $writer->outputMemory(true);    
    $mensaje="OK";
    
    try{
         $pathXMLPHP="../../resources/".$idFacturador."/rtnc/g/".$clave.".xml";
         $pathXMLANG="resources/".$idFacturador."/rtnc/g/".$clave.".xml";
         file_put_contents($pathXMLPHP, $xml);
     } catch (Exception $e) {
        $mensaje = $e->getMessage();
     }
     return array($mensaje,$pathXMLANG,$pathXMLPHP);
}
?>