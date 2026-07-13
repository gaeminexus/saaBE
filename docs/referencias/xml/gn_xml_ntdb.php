<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_notaDebito($dbConn, $clave, $ambiente){
    $tipoDoc = "05"; // Segun tabla 3 del SRI para NotaDebito
    // 1 -- Datos Facturador, Comprador y NotaDebito  
    $sql = $dbConn->prepare("select b.*, b.subtotal 'Sub_12', b.total 'TotalFac', c.numdoc 'RUC', c.nombre 'LOCAL', 
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail
        from ntdb b, fcdr c, cmpr d
        where b.facturador=c.id
        and b.comprador=d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idNotaDebito=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from ntdb a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idNotaDebito);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];    
    
    // 3 -- Detalle NotaDebito   
    $sql = $dbConn->prepare("select * from dtnd a 
    where a.notaDebito =:notaDebito");
    $sql->bindValue(':notaDebito', $idNotaDebito);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detNotaDebito=$sql->fetchAll();
    
    // 4 -- Formas de Pago
    $sql = $dbConn->prepare("select a.*, c.detalle from fpnd a, ntdb b, tsri c
        where a.notaDebito = b.id
        and c.lSRI = 24
        and c.codigo = a.formaPago
        and b.id=:id");
    $sql->bindValue(':id', $idNotaDebito);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $formasPago = $sql->fetchAll();
    
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
    $writer->startElement("notaDebito");
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
    
    $writer->startElement('infoNotaDebito');
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
        // *********************** Impuestos
        // ****** IVA
        $v_sw_impuestos = false;
        if($subtotal_0){
            if($subtotal_0>0){
                $writer->startElement('impuestos');
                $v_sw_impuestos = true;
                $writer->startElement('impuesto');
                $writer->writeElement('codigo', $codIVA);
                $writer->writeElement('codigoPorcentaje', $codPorIVACero);
                $writer->writeElement('tarifa', '0.00');
                $writer->writeElement('baseImponible', $subtotal_0);
                $writer->writeElement('valor', '0.00');
                $writer->endElement();
            }
        }
        if($v_iva_12){
            if($v_iva_12>0){
                if(!$v_sw_impuestos){
                    $writer->startElement('impuestos');
                }
                $v_sw_impuestos = true;
                $writer->startElement('impuesto');
                $writer->writeElement('codigo', $codIVA);
                $writer->writeElement('codigoPorcentaje', $codPorIVA);
                $writer->writeElement('tarifa', $primero['pIVA']);
                $writer->writeElement('baseImponible', $subtotal_12);
                $writer->writeElement('valor', $v_iva_12);
                $writer->endElement();
            }
        }
        /*if($v_ice){
            if($v_ice>0){
                if(!$v_sw_impuestos){
                    $writer->startElement('impuestos');
                }
                $v_sw_impuestos = true;
                $writer->startElement('impuesto');
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
                    $writer->startElement('impuestos');
                }
                $v_sw_impuestos = true;
                $writer->startElement('impuesto');
                $writer->writeElement('codigo', $codIRBPNR);
                $writer->writeElement('codigoPorcentaje', $codPorIRBPNR); // 00000000000 --> Consultar este código de porcentaje
                $writer->writeElement('baseImponible', $subtotal_12);
                $writer->writeElement('valor', $v_IRBPNR);
                $writer->endElement();
            }
        }*/
        if($v_sw_impuestos){
            $writer->endElement();
        }
        $writer->writeElement('valorTotal', $primero['total']); //Total
        $writer->startElement('pagos');
        foreach ($formasPago as $forma){
            $writer->startElement('pago');
            $writer->writeElement('formaPago', $forma['formaPago']);
            $writer->writeElement('total', $forma['valor']);
            if($forma['plazo']){
                $writer->writeElement('plazo', $forma['plazo']);
            }
            if($forma['unidadTiempo']){
                $writer->writeElement('unidadTiempo', $forma['unidadTiempo']);
            }
            $writer->endElement();
        }
        $writer->endElement();
    
    $writer->endElement();
    
    $writer->startElement('motivos');
    foreach ($detNotaDebito as $detalle){
        $writer->startElement('motivo');
        $writer->writeElement('razon', $detalle['descripcion']);
        $writer->writeElement('valor', $detalle['baseImponible']);
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
    $dbConn = null;
    $mensaje="OK";
    try{
        $pathXMLPHP="../../resources/".$idFacturador."/ntdb/g/".$clave.".xml";
        $pathXMLANG="resources/".$idFacturador."/ntdb/g/".$clave.".xml";
        file_put_contents($pathXMLPHP, $xml);
    } catch (Exception $e) {
        $mensaje = $e->getMessage();
    }
    return array($mensaje,$pathXMLANG,$pathXMLPHP);
}

function getCodigoIVA($dbConn, $porcentaje){    
    // 4 -- Código IVA
    $sql = $dbConn->prepare("SELECT codigo FROM fel.tsri
        WHERE lSRI = 17
        AND porcentaje =:porcentaje
        order by codigo");
    $sql->bindValue(':porcentaje', $porcentaje);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $result = $sql->fetch();
    return $result['codigo'];
}
?>