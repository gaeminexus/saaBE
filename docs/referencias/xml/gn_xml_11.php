<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_factura_1($dbConn, $clave, $ambiente){
    // *********************** Datos ******************************
    $tipoDoc = "01"; // Segun tabla 3 del SRI para Factura
    // 1 -- Datos Facturador, Comprador y Factura
    $sql = $dbConn->prepare("select b.*, b.subtotal 'Sub_12', b.total 'TotalFac', c.numdoc 'RUC', c.nombre 'LOCAL',
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, c.empTransporte, d.tipoId, d.numdoc, d.nombre, 
        d.direccion, d.telefono, d.mail
        from fctr b, fcdr c, cmpr d
        where b.facturador=c.id
        and b.comprador=d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idFactura=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from fctr a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idFactura);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];
    
    // 3 -- Detalle Factura
    $sql = $dbConn->prepare("select a.descripcion as 'Produc', if(b.id is null, '4', b.tipoIVA) as 'CodPorIVA',
        a.descuento as 'dto', a.*, b.* from dtfc a
        LEFT JOIN prdc b
        ON a.producto = b.id
        where a.factura=:factura");
    $sql->bindValue(':factura', $idFactura);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detFactura=$sql->fetchAll();
    
    // 4 -- Formas de Pago
    $sql = $dbConn->prepare("select a.*, c.detalle from fpfc a, fctr b, tsri c
        where a.factura = b.id
        and c.lSRI = 24
        and c.codigo = a.formaPago
        and b.id=:id");
    $sql->bindValue(':id', $idFactura);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $formasPago=$sql->fetchAll();
    
    //5 -- Transportista
    $transportista = null;
    if($primero['empTransporte']){
        if($primero['empTransporte'] == 1){
            $idPto = $primero['ptoEmision'];
            $sql = $dbConn->prepare("SELECT b.* FROM ptem a, trpr b
                WHERE a.transportista = b.id
                AND a.id=:id");
            $sql->bindValue(':id', $idPto);
            $sql->execute();
            $sql->setFetchMode(PDO::FETCH_ASSOC);
            $transportista = $sql->fetch();
        }
    }
    
    
    $dbConn = null;
    error_log("------------> F 1.1.0", 0);
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
        $codPorIVA = "2"; // Código del 12%
    }
    $codPorIVA = "4"; // Código del 15%
    $codPorIVA5 = "5"; // Código del 5%
    $codPorIVA8 = "8"; // Código del 8%
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
    $remision = "0"; // ---------> PREGUNTAR DONDE OBTENGO ESTE
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];
    //********************************
    $subsidio=$primero['subsidio'];
    $subtotal_12=$primero['Sub_12'];
    $subtotal_0=$primero['subcero'];
    $subtotal_5=$primero['subtotal5'];
    $subtotal_8=$primero['subtotal8'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos="0.00";
    $total_descuento=nvl($primero['descuento'], "0.00");
    $v_ice=$primero['vICE'];
    $v_iva_12=$primero['vIVA'];
    $v_iva_5=$primero['vIVA5'];
    $v_iva_8=$primero['vIVA8'];
    $v_IRBPNR=$primero['vIRBPNR'];
    $valor_total=$primero['TotalFac'];
    $propina=nvl($primero['propina'],"0.00");
    // ************************************************************
    //Inicia XML
    $writer = new XMLWriter();
    $writer->openMemory();
    $writer->startDocument('1.0', 'UTF-8');
    $writer->setIndent(true);
    $writer->startElement("factura");
    $writer->writeAttribute('id','comprobante');
    $writer->writeAttribute('version','1.1.0');
    
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
    $writer->endElement(); // Fin infoTributaria
    
    $writer->startElement('infoFactura');
        $writer->writeElement('fechaEmision', date("d/m/Y", strtotime( $primero['fecha'])));
        $writer->writeElement('dirEstablecimiento', $dirEstb);
        if($contribuyenteFcdr){
            if($contribuyenteFcdr != ''){
                $writer->writeElement('contribuyenteEspecial', $contribuyenteFcdr); // Verificar cómo se incluye esta información en el XML
            }
        }
        $writer->writeElement('obligadoContabilidad', $contabilidadFcdr);
        $writer->writeElement('tipoIdentificacionComprador', $tipoIdCmdr);
        if($remision){
            $writer->writeElement('guiaRemision', $remision);
        }
        $writer->startElement('razonSocialComprador');
        $writer->text($nombreCmdr);
        $writer->endElement();
        $writer->writeElement('identificacionComprador', $numDocCmdr);
        $writer->writeElement('direccionComprador', $direccionCmdr);
        $writer->writeElement('totalSinImpuestos', $subtotal_12+$subtotal_0+$subtotal_5+$subtotal_8); //Subtotal sin IVA y el de IVA 0
        if($subsidio>0){
            $writer->writeElement('totalSubsidio', $subsidio); //Subtotal sin IVA y el de IVA 0
        }
        $writer->writeElement('totalDescuento', $total_descuento);
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
        if($v_iva_5){
            if($v_iva_5>0){
                if(!$v_sw_impuestos){
                    $writer->startElement('totalConImpuestos');
                }
                $v_sw_impuestos = true;
                $writer->startElement('totalImpuesto');
                $writer->writeElement('codigo', $codIVA);
                $writer->writeElement('codigoPorcentaje', $codPorIVA5);
                $writer->writeElement('baseImponible', $subtotal_5);
                $writer->writeElement('valor', $v_iva_5);
                $writer->endElement();
            }
        }
        if($v_iva_8){
            if($v_iva_8>0){
                if(!$v_sw_impuestos){
                    $writer->startElement('totalConImpuestos');
                }
                $v_sw_impuestos = true;
                $writer->startElement('totalImpuesto');
                $writer->writeElement('codigo', $codIVA);
                $writer->writeElement('codigoPorcentaje', $codPorIVA8);
                $writer->writeElement('baseImponible', $subtotal_8);
                $writer->writeElement('valor', $v_iva_8);
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
        $moneda="DOLAR"; //********************************* VER SI CREAMOS RUBRO DE MONEDA
        $writer->writeElement('propina', $propina);
        $writer->writeElement('importeTotal', $valor_total);
        $writer->writeElement('moneda', $moneda);
        
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
        $writer->endElement(); // Fin Pagos
    $writer->endElement(); // Fin infoFactura
    
    $writer->startElement('detalles');
    foreach ($detFactura as $detalle){
        $writer->startElement('detalle');
        //Producto
        if($detalle['codigo']){
            $writer->writeElement('codigoPrincipal', $detalle['codigo']);
        }
        if($detalle['codigoAux']){
            $writer->writeElement('codigoAuxiliar', $detalle['codigoAux']);
        }
        $writer->startElement('descripcion');
        $writer->text($detalle['Produc']);
        $writer->endElement();
        $writer->writeElement('cantidad', $detalle['cantidad']);
        $writer->writeElement('precioUnitario', $detalle['valor']);
        if($detalle['precioSinSub'] > 0){
            $writer->writeElement('precioSinSubsidio', $detalle['precioSinSub']);
        }
        $writer->writeElement('descuento', $detalle['dto']);
        $writer->writeElement('precioTotalSinImpuesto', $detalle['baseImponible']);
        //Impuestos
        $writer->startElement('impuestos');
        $writer->startElement('impuesto');
        $writer->writeElement('codigo', $codIVA);
/*        if($detalle['codigoIVASRI']==0){
            $writer->writeElement('codigoPorcentaje', $codPorIVACero);
        }
        if($detalle['codigoIVASRI']==5){
            $writer->writeElement('codigoPorcentaje', $codPorIVA5);
        }
        if($detalle['codigoIVASRI']==4){
            $writer->writeElement('codigoPorcentaje', $codPorIVA);
        }
        if($detalle['codigoIVASRI']==2){
            $writer->writeElement('codigoPorcentaje', $codPorIVA);
        }
        if($detalle['codigoIVASRI']==8){
            $writer->writeElement('codigoPorcentaje', $codPorIVA8);
        }*/
        $writer->writeElement('codigoPorcentaje', $detalle['codigoIVASRI']);
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
    if($transportista){
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Identificación');
        $writer->text($transportista['numdoc']);
        $writer->endElement();
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Nombre');
        $writer->text($transportista['nombre']);
        $writer->endElement();
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Teléfono');
        $writer->text($transportista['telefono']);
        $writer->endElement();
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Email');
        $writer->text($transportista['mail']);
        $writer->endElement();
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Placa');
        $writer->text($transportista['placa']);
        $writer->endElement();
    }else{
        $writer->startElement('campoAdicional');
        $writer->writeAttribute('nombre','Datos Adicionales');
        /*$infoAdicional = "Soporte[$telefonoFcdr - $mailFcdr] ".
            "Contacto Cliente[$iaFonos - $mailCmdr] Observacion[$iaObaservacion]";*/
        $infoAdicional = "Observ.[$iaObaservacion]";
        $writer->text($infoAdicional);
        $writer->endElement();
    }    
    $writer->endElement();
    
    $writer->endElement();
    $writer->endDocument();
    $xml = $writer->outputMemory(true);
    
    $mensaje="OK";
    
    try{
        $pathXMLPHP="../../resources/".$idFacturador."/docs/g/".$clave.".xml";
        $pathXMLANG="resources/".$idFacturador."/docs/g/".$clave.".xml";
        //$xml->save($pathXMLPHP);
        file_put_contents($pathXMLPHP, $xml);
    } catch (Exception $e) {
        $mensaje = $e->getMessage();
    }
    return array($mensaje,$pathXMLANG,$pathXMLPHP);
}

function quitarAmpersand($texto){
    $resultado = str_replace("&amp;", "(y)", $texto);
    $resultado = str_replace("&", "(y)", $resultado);
    return $resultado;
}
?>