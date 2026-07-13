<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_retencion($dbConn, $clave, $ambiente){
    $tipoDoc = "07"; // Segun tabla 3 del SRI para Factura
    // 1 -- Datos Facturador, Proveedor y Factura
    $sql = $dbConn->prepare("select b.*, c.numdoc 'RUC', c.nombre 'LOCAL',
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail
        from rtv2 b, fcdr c, prvd d
        where b.facturador=c.id
        and b.proveedor = d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idReten=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from rtv2 a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idReten);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];
    
    // 3 -- Detalle Retención - Documento Sustento
    $sql = $dbConn->prepare("select *
        from drv2
        where retencionv2=:retencion
        group by tipoDocReten, numDocReten;");
    $sql->bindValue(':retencion', $idReten);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detDoc=$sql->fetchAll();
    
    // 4 -- Detalle Retención - Retencion
    $sql = $dbConn->prepare("SELECT * FROM drv2
        WHERE retencionv2=:retencion");
    $sql->bindValue(':retencion', $idReten);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detRet=$sql->fetchAll();
    
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
    $codIVA = 2;
    if($primero['contabilidad']){
        if($primero['contabilidad']==1){
            $contabilidadFcdr = 'SI';
        }
    }
    $tipoIdCmdr = $primero['tipoId'];
    $v_parteRel = "NO"; // Valores solo SI o NO
    $v_pagoLocExt = "01"; // Tabla 15 Catálogo ATS - 01 residente
    $nombreCmdr=$primero['nombre'];
    $numDocCmdr=$primero['numdoc'];
    $periodoFiscal=$primero['periodoFiscal'];
    $mailCmdr=$primero['mail'];
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];
    
    $v_codSustento = "02"; // Tabla 5 Catálogo ATS - 02 en el ejemplo de factura
    $v_fecRegCon = ''; //Opcional Registro Contable
    $v_numAutoDoc = ''; //Opcional Número Autorización Documento Sustento
    // ************************************************************
    //Inicia XML
    try{
        $writer = new XMLWriter();
        $writer->openMemory();
        $writer->startDocument('1.0', 'UTF-8');
        $writer->setIndent(true);
        $writer->startElement("comprobanteRetencion");
        $writer->writeAttribute('id','comprobante');
        $writer->writeAttribute('version','2.0.0');
        
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
        $writer->writeElement('parteRel', $v_parteRel);
        $writer->startElement('razonSocialSujetoRetenido');
        $writer->text($nombreCmdr);
        $writer->endElement();
        $writer->writeElement('identificacionSujetoRetenido', $numDocCmdr);
        $writer->writeElement('periodoFiscal', $periodoFiscal);
        $writer->endElement();
    
        $writer->startElement('docsSustento');
        foreach ($detDoc as $doc){
            $writer->startElement('docSustento');
                $writer->writeElement('codSustento', $v_codSustento);
                $writer->writeElement('codDocSustento', $doc['tipoDocReten']);
                $writer->writeElement('numDocSustento', $doc['numDocReten']);
                $writer->writeElement('fechaEmisionDocSustento', date("d/m/Y",strtotime($doc['fechaEmiDoc'])));
                if($v_fecRegCon!=''){
                    $writer->writeElement('fechaRegistroContable', $v_fecRegCon);
                }
                if($v_numAutoDoc!=''){
                    $writer->writeElement('numAutDocSustento', $v_numAutoDoc);
                }  
                $writer->writeElement('pagoLocExt', $v_pagoLocExt);
                $writer->writeElement('totalSinImpuestos', $doc['docResTSinImpuestos']);
                $writer->writeElement('importeTotal', $doc['docResTotal']);
 
                $writer->startElement('impuestosDocSustento');
                    if($doc['docResIVACero']>0){
                        $writer->startElement('impuestoDocSustento');
                        $writer->writeElement('codImpuestoDocSustento', $codIVA);
                        $writer->writeElement('codigoPorcentaje', 0);
                        $writer->writeElement('baseImponible', $doc['docResIVACero']);
                        $writer->writeElement('tarifa', '0');
                        $writer->writeElement('valorImpuesto', '0.00');
                        $writer->endElement();
                    }
                    if($doc['docResTotalIVA']>0){
                        $writer->startElement('impuestoDocSustento');
                        $writer->writeElement('codImpuestoDocSustento', $codIVA);
                        $writer->writeElement('codigoPorcentaje', 2); // Cambiar según tabla del SRI o determinar según 'docResPorIVA'
                        $writer->writeElement('baseImponible', $doc['docResTSinImpuestos'] - $doc['docResIVACero']);
                        $writer->writeElement('tarifa', $doc['docResPorIVA']);
                        $writer->writeElement('valorImpuesto', $doc['docResTotalIVA']);
                        $writer->endElement();
                    }
                $writer->endElement();               
                $writer->startElement('retenciones');
                foreach ($detRet as $ret){
                    if($doc['numDocReten'] == $ret['numDocReten']){
                        $writer->startElement('retencion');
                        $writer->writeElement('codigo', $ret['codImpuesto']);
                        $writer->writeElement('codigoRetencion', $ret['codRetencion']);
                        $writer->writeElement('baseImponible', $ret['baseImponible']);
                        $writer->writeElement('porcentajeRetener', $ret['porcentajeReten']);
                        $writer->writeElement('valorRetenido', $ret['valorReten']);
                        $writer->endElement();
                    }
                }
                $writer->endElement();
                $writer->startElement('pagos');
                    $writer->startElement('pago');
                        $writer->writeElement('formaPago', $doc['docResForPago']);
                        $writer->writeElement('total', $doc['docResTotal']);
                    $writer->endElement();
                $writer->endElement();
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
    } catch (Throwable $e){
        $mensaje = "Error: ".$e->getMessage();
    }    
    try{
         $pathXMLPHP="../../resources/".$idFacturador."/rtv2/g/".$clave.".xml";
         $pathXMLANG="resources/".$idFacturador."/rtv2/g/".$clave.".xml";
         file_put_contents($pathXMLPHP, $xml);
     } catch (Exception $e) {
        $mensaje = $e->getMessage();
     }
     return array($mensaje,$pathXMLANG,$pathXMLPHP);
}
?>