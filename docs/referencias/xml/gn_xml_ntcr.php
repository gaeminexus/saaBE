<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_notaCredito($dbConn, $clave, $ambiente){
    $tipoDoc = "04"; // Segun tabla 3 del SRI para NotaCredito
    // 1 -- Datos Facturador, Comprador y NotaCredito  
    $sql = $dbConn->prepare("select b.*, b.subtotal 'Sub_12', b.total 'TotalFac', c.numdoc 'RUC', c.nombre 'LOCAL', 
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail
        from ntcr b, fcdr c, cmpr d
        where b.facturador=c.id
        and b.comprador=d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idNotaCredito=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from ntcr a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idNotaCredito);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];    
    
    // 3 -- Detalle NotaCredito   
    $sql = $dbConn->prepare("select a.descripcion as 'Produc', if(b.id is null, '4', b.tipoIVA) as 'CodPorIVA',
        if(b.id is null, a.descuento, b.descuento) as 'dto', a.*, b.*
        from dtnc a
        LEFT JOIN prdc b
        ON a.producto = b.id
        where a.notaCredito=:notaCredito");
    $sql->bindValue(':notaCredito', $idNotaCredito);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detNotaCredito=$sql->fetchAll();
    
    $dbConn = null;
    /* ************************************************************** FIN de selects  */
    $idFacturador = $primero['facturador'];
    $RUC=$primero['RUC'];
    $idAmbiente = $ambiente;
    $idEmision = "1";
    // ***************** CODIGOS IMPUESTOS
    $codIVA = "2";
    $codICE = "3";
    $codIRBPNR = "5";
    $codPorIVACero = "0"; // Código del 0%
    $codPorIVA = "4"; // Código del 12%
    if($primero['pIVA']==12){
        $codPorIVA = "2"; // Código del 12% se debe hacer commit
    }
    $codPorICE = "xxx"; // Determinar cómo saco todos los ICE de una factura según todos los items de una factura
    $codPorIRBPNR = "xxx"; // Determinar cómo saco todos los ICE de una factura según todos los items de una factura
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
    $direccionCmdr=$primero['direccion'];
    $mailCmdr=$primero['mail'];
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];
    //********************************
    $subsidio=$primero['subsidio'];
    $subtotal_12=$primero['Sub_12'];
    $subtotal_0=$primero['subcero'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos="0.00";
    $total_descuento=nvl($primero['descuento'], "0.00");
    $v_ice=$primero['vICE'];
    $v_iva_12=$primero['vIVA'];
    $v_IRBPNR=$primero['vIRBPNR'];
    $valor_total=$primero['TotalFac'];
    $propina=nvl($primero['propina'],"0.00");
    // ************************************************************
    //Inicia XML
    $writer = new XMLWriter();
    $writer->openMemory();
    $writer->startDocument('1.0', 'UTF-8');
    $writer->setIndent(true);
    $writer->startElement("notaCredito");
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
    
    $writer->startElement('infoNotaCredito');
    $writer->writeElement('fechaEmision', date("d/m/Y", strtotime( $primero['fecha'])));
    $writer->writeElement('dirEstablecimiento', $dirEstb);
    $writer->writeElement('tipoIdentificacionComprador', $tipoIdCmdr);
    $writer->startElement('razonSocialComprador');
    $writer->text($nombreCmdr);
    $writer->endElement();
    $writer->writeElement('identificacionComprador', $numDocCmdr);
    if($contribuyenteFcdr){
        if($contribuyenteFcdr != ''){
            $writer->writeElement('contribuyenteEspecial', $contribuyenteFcdr); // Verificar cómo se incluye esta información en el XML
        }
    }
    $writer->writeElement('obligadoContabilidad', $contabilidadFcdr);
    /* <rise>Contribuyente Régimen Simplificado RISE</rise> */
    $writer->writeElement('codDocModificado', $primero['tipoDocModificado']);
    $writer->writeElement('numDocModificado', $primero['numDocModificado']);
    $writer->writeElement('fechaEmisionDocSustento', date("d/m/Y", strtotime( $primero['fechaEmisionDM'])));
    $writer->writeElement('totalSinImpuestos', $subtotal_12+$subtotal_0); //Subtotal sin IVA y el de IVA 0
    $writer->writeElement('valorModificacion', $primero['total']); //Total
    $moneda="DOLAR"; //********************************* VER SI CREAMOS RUBRO DE MONEDA
    $writer->writeElement('moneda', $moneda);
    // *********************** Impuestos
    // ****** IVA
    $v_sw_impuestos = false;
    if($subtotal_0){
        if($subtotal_0>0){
            $writer->startElement('totalConImpuestos');
            $v_sw_impuestos = true;
            $writer->startElement('totalImpuesto');
            $writer->writeElement('codigo', $codIVA);
            $writer->writeElement('codigoPorcentaje', $codPorIVACero);
            $writer->writeElement('baseImponible', $subtotal_0);
            $writer->writeElement('valor', '0.00');
            $writer->endElement();
        }
    }
    if($v_iva_12){
        if($v_iva_12>0){
            if(!$v_sw_impuestos){
                $writer->startElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $writer->startElement('totalImpuesto');
            $writer->writeElement('codigo', $codIVA);
            $writer->writeElement('codigoPorcentaje', $codPorIVA);
            $writer->writeElement('baseImponible', $subtotal_12);
            $writer->writeElement('valor', $v_iva_12);
            $writer->endElement();
        }
    }
    if($v_ice){
        if($v_ice>0){
            if(!$v_sw_impuestos){
                $writer->startElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $writer->startElement('totalImpuesto');
            $writer->writeElement('codigo', $codICE); // 00000000000 --> Crear algoritmo para poner todos los ICE de una factura
            $writer->writeElement('codigoPorcentaje', $codPorICE);
            $writer->writeElement('baseImponible', $subtotal_12);
            $writer->writeElement('valor', $v_ice);
            $writer->endElement();
        }
    }
    if($v_IRBPNR){
        if($v_IRBPNR>0){
            if(!$v_sw_impuestos){
                $writer->startElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $writer->startElement('totalImpuesto');
            $writer->writeElement('codigo', $codIRBPNR);
            $writer->writeElement('codigoPorcentaje', $codPorIRBPNR); // 00000000000 --> Consultar este código de porcentaje
            $writer->writeElement('baseImponible', $subtotal_12);
            $writer->writeElement('valor', $v_IRBPNR);
            $writer->endElement();
        }
    }
    if($v_sw_impuestos){
        $writer->endElement();
    }    
    $writer->writeElement('motivo', $iaObaservacion);    
    $writer->endElement();
    
    $writer->startElement('detalles');
    foreach ($detNotaCredito as $detalle){
        $writer->startElement('detalle');
        //Producto
        if($detalle['codigo']){
            $writer->writeElement('codigoInterno', $detalle['codigo']);
        }
        if($detalle['codigoAux']){
            $writer->writeElement('codigoAdicional', $detalle['codigoAux']);
        }
        $writer->writeElement('descripcion', $detalle['Produc']);
        $writer->writeElement('cantidad', $detalle['cantidad']);
        $writer->writeElement('precioUnitario', $detalle['valor']);
        $writer->writeElement('descuento', nvl($detalle['dto'],'0.00'));
        $writer->writeElement('precioTotalSinImpuesto', $detalle['baseImponible']);
        /*
         <detallesAdicionales> 
             <detAdicional nombre="Marca" valor="Chevrolet"/> 
             <detAdicional nombre="Modelo" valor="2012"/>
             <detAdicional nombre="Chasis" valor="8LDETA03V20003289"/>
         </detallesAdicionales>
         */
        //Impuestos
        $writer->startElement('impuestos');
        $writer->startElement('impuesto');
        $writer->writeElement('codigo', $codIVA);
        if($detalle['porcentajeIVA']>0){
            $writer->writeElement('codigoPorcentaje', $codPorIVA);
        }else{
            $writer->writeElement('codigoPorcentaje', $codPorIVACero);
        }
        $writer->writeElement('tarifa', $detalle['porcentajeIVA']);
        $writer->writeElement('baseImponible', $detalle['baseImponible']);
        $writer->writeElement('valor', $detalle['valorIVA']);
        $writer->endElement();
        $writer->endElement();
        // Debo hacer lo mismo para el ICE
        $writer->endElement();
    }
    $writer->endElement();
    
    $writer->startElement('infoAdicional');
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Datos Adicionales');
        $infoAdicional = "Soporte[$telefonoFcdr - $mailFcdr] ".
            "Contacto Cliente[$iaFonos - $mailCmdr]";
        $writer->text($infoAdicional);
        $writer->endElement();
    $writer->endElement();
    
    $writer->endElement();
    $writer->endDocument();
    $xml = $writer->outputMemory(true);
    //echo $xml;
    
    $mensaje="OK";
    try{
        $pathXMLPHP="../../resources/".$idFacturador."/ntcr/g/".$clave.".xml";
        $pathXMLANG="resources/".$idFacturador."/ntcr/g/".$clave.".xml";
        file_put_contents($pathXMLPHP, $xml);
    } catch (Exception $e) {
        $mensaje = $e->getMessage();
    }
    return array($mensaje,$pathXMLANG,$pathXMLPHP);
}
?>