import moment from 'moment';
import * as forge from 'node-forge';

const BCE = 1;
const SECURITY_DATA = 2;
const UANATACA = 3;
const ANFAC = 4;
const BCE_CAMBIO_CLAVE = 5;
const LAZZATE = 6;

export function p_firmar_documento(rutaCertificado: string, pwdP12: string, rutaDoc: string, empresaFirma: number, tagDoc: string): any {
    return new Promise((resolve, reject) => {
        let docFimr: any;
        // tslint:disable-next-line: only-arrow-functions
        openXMLFile(rutaDoc, function(documento: any) {
            // tslint:disable-next-line: only-arrow-functions
            p_generar_xades_bes(documento, rutaCertificado, pwdP12, empresaFirma, tagDoc, function(docFirmado: any){
            // console.log(factura_firmada);
            docFimr = docFirmado;
            resolve(docFimr);
            });
        });
    });
}

/* export function validaCertificado(rutaCertificado: string, pwdP12: string, empresaFirma: number): any {
    return new Promise((resolve, reject) => {
        const pwdCert = pwdP12;
        let p12xxx: any;
        const oReq = new XMLHttpRequest();
        oReq.open('GET', rutaCertificado, true);
        oReq.responseType = 'arraybuffer';
        // tslint:disable-next-line: only-arrow-functions
        oReq.onload = function(oEvent) {
          const blob = new Blob([oReq.response], {type: 'application/x-pkcs12'}); // agregar comentario
          p12xxx = [oReq.response];
        };
    oReq.send();
    });
}*/


function p_generar_xades_bes(docXML: any, rutaCertificado: string, pwdP12: string, empresaFirma: number, tagDoc: string, callback: any) {
    const serializer = new XMLSerializer();
    let documento = serializer.serializeToString(docXML);
    documento = documento.replace('<?xml version="1.0" encoding="UTF-8"?>', '<?xml version="1.0" encoding="UTF-8"?>\n');
    const xmlDoc = docXML;
    // const claveAcceso = xmlDoc.getElementsByTagName('claveAcceso')[0].childNodes[0].nodeValue;
    const oReq = new XMLHttpRequest();
    let p12xxx: any;
    const pwdCert = pwdP12;
    oReq.open('GET', rutaCertificado, true);
    oReq.responseType = 'arraybuffer';
    // tslint:disable-next-line: only-arrow-functions
    oReq.onload = function(oEvent) {
        const blob = new Blob([oReq.response], {type: 'application/x-pkcs12'});
        p12xxx = [oReq.response];
        // tslint:disable-next-line: max-line-length tslint:disable-next-line: only-arrow-functions
        generarFirma(p12xxx[0], documento, pwdCert, empresaFirma, function(certificado: any, modulus: any, certificateX509DerHash: any, X509SerialNumber: any, exponent: any, issuerName: any) {

            documento = documento.replace('<?xml version="1.0" encoding="UTF-8"?>\n', '');
            documento = documento.replace('</'+tagDoc+'>\n', '</'+tagDoc+'>');

            const sha1Factura = sha1_base64_fact(documento.replace('<?xml version="1.0" encoding="UTF-8"?>\n', ''));
            const xmlns = 'xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:etsi="http://uri.etsi.org/01903/v1.3.2#"';

            // numeros involucrados en los hash:
            const CertificateNumber = p_obtener_aleatorio(); // 1562780 en el ejemplo del SRI
            const SignatureNumber = p_obtener_aleatorio(); // 620397 en el ejemplo del SRI
            const SignedPropertiesNumber = p_obtener_aleatorio(); // 24123 en el ejemplo del SRI

            // numeros fuera de los hash:
            const SignedInfoNumber = p_obtener_aleatorio(); // 814463 en el ejemplo del SRI
            const SignedPropertiesIDNumber = p_obtener_aleatorio(); // 157683 en el ejemplo del SRI
            const ReferenceIDNumber = p_obtener_aleatorio(); // 363558 en el ejemplo del SRI
            const SignatureValueNumber = p_obtener_aleatorio(); // 398963 en el ejemplo del SRI
            const ObjectNumber = p_obtener_aleatorio(); // 231987 en el ejemplo del SRI

            let SignedProperties = '';

            SignedProperties += '<etsi:SignedProperties Id="Signature' + SignatureNumber + '-SignedProperties' + SignedPropertiesNumber + '">';  // SignedProperties
            SignedProperties += '<etsi:SignedSignatureProperties>';
            // Inicia time
            SignedProperties += '<etsi:SigningTime>';
            SignedProperties += moment().format('YYYY-MM-DD\THH:mm:ssZ');
            // SignedProperties += '2022-09-16T11:49:11-05:00';
            SignedProperties += '</etsi:SigningTime>';
            // Termina time
            // Inicia certificado
            SignedProperties += '<etsi:SigningCertificate>';
            SignedProperties += '<etsi:Cert>';
            SignedProperties += '<etsi:CertDigest>';
            SignedProperties += '<ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1">';
            SignedProperties += '</ds:DigestMethod>';
            SignedProperties += '<ds:DigestValue>';
            SignedProperties += certificateX509DerHash;
            SignedProperties += '</ds:DigestValue>';
            SignedProperties += '</etsi:CertDigest>';
            SignedProperties += '<etsi:IssuerSerial>';
            SignedProperties += '<ds:X509IssuerName>';
            SignedProperties += issuerName;
            SignedProperties += '</ds:X509IssuerName>';
            SignedProperties += '<ds:X509SerialNumber>';
            SignedProperties += X509SerialNumber;
            SignedProperties += '</ds:X509SerialNumber>';
            SignedProperties += '</etsi:IssuerSerial>';
            SignedProperties += '</etsi:Cert>';
            SignedProperties += '</etsi:SigningCertificate>';
            SignedProperties += '</etsi:SignedSignatureProperties>';
            SignedProperties += '<etsi:SignedDataObjectProperties>';
            SignedProperties += '<etsi:DataObjectFormat ObjectReference="#Reference-ID-' + ReferenceIDNumber + '">';
            SignedProperties += '<etsi:Description>';
            SignedProperties += 'contenido comprobante';
            SignedProperties += '</etsi:Description>';
            SignedProperties += '<etsi:MimeType>';
            SignedProperties += 'text/xml';
            SignedProperties += '</etsi:MimeType>';
            SignedProperties += '</etsi:DataObjectFormat>';
            SignedProperties += '</etsi:SignedDataObjectProperties>';
            SignedProperties += '</etsi:SignedProperties>';
            // fin SignedProperties
            const SignedPropertiesParaHash = SignedProperties.replace('<etsi:SignedProperties', '<etsi:SignedProperties ' + xmlns);
            const sha1SignedProperties = sha1_base64(SignedPropertiesParaHash);
            let KeyInfo = '';
            KeyInfo += '<ds:KeyInfo Id="Certificate' + CertificateNumber + '">';
            KeyInfo += '\n<ds:X509Data>';
            KeyInfo += '\n<ds:X509Certificate>\n';
            // CERTIFICADO X509 CODIFICADO EN Base64
            KeyInfo += certificado;
            KeyInfo += '\n</ds:X509Certificate>';
            KeyInfo += '\n</ds:X509Data>';
            KeyInfo += '\n<ds:KeyValue>';
            KeyInfo += '\n<ds:RSAKeyValue>';
            KeyInfo += '\n<ds:Modulus>\n';
            // MODULO DEL CERTIFICADO X509}
            KeyInfo += modulus;
            KeyInfo += '\n</ds:Modulus>';
            KeyInfo += '\n<ds:Exponent>';
            // KeyInfo += 'AQAB';
            KeyInfo += exponent;
            KeyInfo += '</ds:Exponent>';
            KeyInfo += '\n</ds:RSAKeyValue>';
            KeyInfo += '\n</ds:KeyValue>';
            KeyInfo += '\n</ds:KeyInfo>';

            const KeyInfoParaHash = KeyInfo.replace('<ds:KeyInfo', '<ds:KeyInfo ' + xmlns);
            const sha1Certificado = sha1_base64(KeyInfoParaHash);
            let SignedInfo = '';
            SignedInfo += '<ds:SignedInfo Id="Signature-SignedInfo' + SignedInfoNumber + '">';
            SignedInfo += '\n<ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">';
            SignedInfo += '</ds:CanonicalizationMethod>';
            SignedInfo += '\n<ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1">';
            SignedInfo += '</ds:SignatureMethod>';
            SignedInfo += '\n<ds:Reference Id="SignedPropertiesID' + SignedPropertiesIDNumber + '" Type="http://uri.etsi.org/01903#SignedProperties" URI="#Signature' + SignatureNumber + '-SignedProperties' + SignedPropertiesNumber + '">';
            SignedInfo += '\n<ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1">';
            SignedInfo += '</ds:DigestMethod>';
            SignedInfo += '\n<ds:DigestValue>';
            // HASH O DIGEST DEL ELEMENTO <etsi:SignedProperties>';
            SignedInfo += sha1SignedProperties;
            SignedInfo += '</ds:DigestValue>';
            SignedInfo += '\n</ds:Reference>';
            SignedInfo += '\n<ds:Reference URI="#Certificate' + CertificateNumber + '">';
            SignedInfo += '\n<ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1">';
            SignedInfo += '</ds:DigestMethod>';
            SignedInfo += '\n<ds:DigestValue>';
            // HASH O DIGEST DEL CERTIFICADO X509
            SignedInfo += sha1Certificado;
            SignedInfo += '</ds:DigestValue>';
            SignedInfo += '\n</ds:Reference>';
            SignedInfo += '\n<ds:Reference Id="Reference-ID-' + ReferenceIDNumber + '" URI="#comprobante">';
            SignedInfo += '\n<ds:Transforms>';
            SignedInfo += '\n<ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature">';
            SignedInfo += '</ds:Transform>';
            SignedInfo += '\n</ds:Transforms>';
            SignedInfo += '\n<ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1">';
            SignedInfo += '</ds:DigestMethod>';
            SignedInfo += '\n<ds:DigestValue>';
            // HASH O DIGEST DE TODO EL ARCHIVO XML IDENTIFICADO POR EL id="comprobante"
            SignedInfo += sha1Factura;
            SignedInfo += '</ds:DigestValue>';
            SignedInfo += '\n</ds:Reference>';
            SignedInfo += '\n</ds:SignedInfo>';

            const SignedInfoParaFirma = SignedInfo.replace('<ds:SignedInfo', '<ds:SignedInfo ' + xmlns);
            // tslint:disable-next-line: only-arrow-functions
            p_firmar(p12xxx[0], SignedInfoParaFirma, pwdCert, empresaFirma, function(firmaSignedInfo: any) {
                let xadesBes = '';
                // INICIO DE LA FIRMA DIGITAL
                xadesBes += '<ds:Signature ' + xmlns + ' Id="Signature' + SignatureNumber + '">';
                xadesBes += '\n' + SignedInfo;
                xadesBes += '\n<ds:SignatureValue Id="SignatureValue' + SignatureValueNumber + '">\n';
                // VALOR DE LA FIRMA (ENCRIPTADO CON LA LLAVE PRIVADA DEL CERTIFICADO DIGITAL)
                xadesBes += firmaSignedInfo;
                xadesBes += '\n</ds:SignatureValue>';
                xadesBes += '\n' + KeyInfo;
                xadesBes += '\n<ds:Object Id="Signature' + SignatureNumber + '-Object' + ObjectNumber + '">';
                xadesBes += '<etsi:QualifyingProperties Target="#Signature' + SignatureNumber + '">';
                // ELEMENTO <etsi:SignedProperties>';
                xadesBes += SignedProperties;
                xadesBes += '</etsi:QualifyingProperties>';
                xadesBes += '</ds:Object>';
                xadesBes += '</ds:Signature>';
                // FIN DE LA FIRMA DIGITAL
                callback(documento.replace('</'+tagDoc+'>', xadesBes + '</'+tagDoc+'>'));
            });
        });
    };
    oReq.send();
}

function generarFirma(p12File: any, infoAFirmar: any, pwdCert: string, empresaFirma: number, callback2: any): void {
    let certificateX509 = '';
    let cert: any;
    let issuerName: string;
    let pkcs8: any;
    if (p12File === undefined || infoAFirmar === undefined) {
        return;
    }

    const arrayUint8 = new Uint8Array(p12File);
        const p12B64 = forge.util.binary.base64.encode(arrayUint8);
        const p12Der = forge.util.decode64(p12B64);
        const p12Asn1 = forge.asn1.fromDer(p12Der);

        let p12 = null;
        try {
            p12 = forge.pkcs12.pkcs12FromAsn1(p12Asn1, pwdCert);
        } catch {
            window.alert('La clave de la firma es incorrecta');
        }

        if (!p12) {
            return;
        }

        const certBags = p12.getBags({bagType: forge.pki.oids.certBag});

        //console.log(certBags);
        cert = obtieneCertificado(+empresaFirma, certBags);
        //console.log(cert);

        /*if (+empresaFirma === BCE) {
            cert = certBags[forge.oids.certBag][1].cert;
        } else {
            cert = certBags[forge.oids.certBag][0].cert;
        }*/

        const pkcs8bags = p12.getBags({bagType: forge.pki.oids.pkcs8ShroudedKeyBag});

        //console.log(pkcs8bags);
        pkcs8 = obtieneKey(+empresaFirma, pkcs8bags);

        //console.log(pkcs8);
        /*if (+empresaFirma === BCE) {
            pkcs8 = pkcs8bags[forge.oids.pkcs8ShroudedKeyBag][1];
        } else {
            pkcs8 = pkcs8bags[forge.oids.pkcs8ShroudedKeyBag][0];
        }*/
        let key = pkcs8.key;
        if (key == null ) {
            key = pkcs8.asn1;
        }

        /*const md = forge.md.sha1.create();
        md.update(infoAFirmar, 'utf8');
        const signature = btoa(key.sign(md)).match(/.{1,76}/g).join('\n');*/

        const certificateX509Pem = forge.pki.certificateToPem(cert);
        certificateX509 = certificateX509Pem;
        certificateX509 = certificateX509.substr( certificateX509.indexOf('\n') );
        certificateX509 = certificateX509.substr( 0, certificateX509.indexOf('\n-----END CERTIFICATE-----') );
        certificateX509 = certificateX509.replace(/\r?\n|\r/g, '').replace(/([^\0]{76})/g, '$1\n');

        // Pasar certificado a formato DER y sacar su hash:
        const certificateX509Asn1 = forge.pki.certificateToAsn1(cert);
        const certificateX509Der = forge.asn1.toDer(certificateX509Asn1).getBytes();
        const certificateX509DerHash = sha1_base64(certificateX509Der);

        // Serial Number
        // var X509SerialNumber = parseInt(cert.serialNumber, 16);
        const bn = BigInt('0x' + cert.serialNumber);
        const X509SerialNumber = bn.toString(10);

        const exponent = hexToBase64(key.e.data[0].toString(16));
        // let modulus: any;
        const modulus = bigint2base64(key.n);

        switch (+empresaFirma) {
            case BCE: {
                issuerName =
                    cert.issuer.attributes[4].shortName + '=' + cert.issuer.attributes[4].value + ',' +
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
            case SECURITY_DATA: {
                issuerName =
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
            case UANATACA: {
                issuerName =
                    cert.issuer.attributes[5].type + '=' + cert.issuer.attributes[5].value + ',' +
                    cert.issuer.attributes[4].shortName + '=' + cert.issuer.attributes[4].value + ',' +
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
            case ANFAC: {
                issuerName =
                    cert.issuer.attributes[4].shortName + '=' + cert.issuer.attributes[4].value + ',' +
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].type + '=' + cert.issuer.attributes[0].value;
                break;
            }
            case BCE_CAMBIO_CLAVE: {
                issuerName =
                    cert.issuer.attributes[4].shortName + '=' + cert.issuer.attributes[4].value + ',' +
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
            case LAZZATE: {
                issuerName =
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
            default: {
                issuerName =
                    cert.issuer.attributes[3].shortName + '=' + cert.issuer.attributes[3].value + ',' +
                    cert.issuer.attributes[2].shortName + '=' + cert.issuer.attributes[2].value + ',' +
                    cert.issuer.attributes[1].shortName + '=' + cert.issuer.attributes[1].value + ',' +
                    cert.issuer.attributes[0].shortName + '=' + cert.issuer.attributes[0].value;
                break;
            }
        }

        callback2(certificateX509, modulus, certificateX509DerHash, X509SerialNumber, exponent, issuerName);
}

function obtieneCertificado(entidad: number, bag: any): any {

    let certificado: any;

    switch (+entidad) {
        case BCE: {
            certificado = bag[forge.pki.oids.certBag][1].cert;
            break;
        }
        case BCE_CAMBIO_CLAVE: {
            certificado = bag[forge.pki.oids.certBag][2].cert;
            break;
        }
        default: {
            certificado = bag[forge.pki.oids.certBag][0].cert;
            break;
        }
    }

    return certificado;
}

function obtieneKey(entidad: number, bag: any): any {

    let pkcs8: any;

    switch (+entidad) {
        case BCE: {
            pkcs8 = bag[forge.pki.oids.pkcs8ShroudedKeyBag][1];
            break;
        }
        case BCE_CAMBIO_CLAVE: {
            pkcs8 = bag[forge.pki.oids.pkcs8ShroudedKeyBag][2];
            break;
        }
        default: {
            pkcs8 = bag[forge.pki.oids.pkcs8ShroudedKeyBag][0];
            break;
        }
    }

    return pkcs8;
}

function p_firmar(p12File: any, infoAFirmar: any, pwdCert: string, empresaFirma: number, callback: any){
    let signature = '';
    let pkcs8: any;
    const arrayUint8 = new Uint8Array(p12File);
    const p12B64 = forge.util.binary.base64.encode(arrayUint8);
    const p12Der = forge.util.decode64(p12B64);
    const p12Asn1 = forge.asn1.fromDer(p12Der);

    let p12 = null;

    try {
        p12 = forge.pkcs12.pkcs12FromAsn1(p12Asn1, pwdCert);
    } catch {
        window.alert('La clave de la firma es incorrecta');
    }

    if (!p12) {
        callback('');
        return;
    }

    const pkcs8bags = p12.getBags({bagType: forge.pki.oids.pkcs8ShroudedKeyBag});
    pkcs8 = obtieneKey(+empresaFirma, pkcs8bags);
    /*if (+empresaFirma === BCE) {
        pkcs8 = pkcs8bags[forge.oids.pkcs8ShroudedKeyBag][1];
    } else {
        pkcs8 = pkcs8bags[forge.oids.pkcs8ShroudedKeyBag][0];
    }*/

    let key = pkcs8.key;
    if (key == null ) {
        key = pkcs8.asn1;
    }

    const md = forge.md.sha1.create();
    md.update(infoAFirmar, 'utf8');
    signature = btoa(key.sign(md)).match(/.{1,76}/g)!.join('\n');


    callback(signature);
}

function openXMLFile(path: string, callback: any) {
    let xhttp: any;
    if (window.XMLHttpRequest) {
        xhttp = new XMLHttpRequest();
    }
    xhttp.open('GET', path, false);
    xhttp.send();
    const xmlDoc = xhttp.responseXML;
    callback(xmlDoc);
}

function sha1_base64(txt: any) {
    const md = forge.md.sha1.create();
    md.update(txt);
    // tslint:disable-next-line: deprecation
    return new Buffer(md.digest().toHex(), 'hex').toString('base64');
}

function sha1_base64_fact(txt: any) {
    const md = forge.md.sha1.create();
    md.update(txt, 'utf8');
    // tslint:disable-next-line: deprecation
    return new Buffer(md.digest().toHex(), 'hex').toString('base64');
}

function p_obtener_aleatorio() {
    return Math.floor(Math.random() * 999000) + 990;
}

function bigint2base64(bigint: any){
    let base64 = '';
    // tslint:disable-next-line: only-arrow-functions
    base64 = btoa(bigint.toString(16).match(/\w{2}/g).map(function(a: any){return String.fromCharCode(parseInt(a, 16)); } ).join(''));
    base64 = base64.match(/.{1,76}/g)!.join('\n');
    return base64;
}


function hexToBase64(str: string) {
    const hexStr = ('00' + str).slice(0 - str.length - str.length % 2); // Original
    const hexCad = hexStr.replace(/\r|\n/g, '').replace(/([\da-fA-F]{2}) ?/g, '0x$1 ').replace(/ +$/, '').split(' ');
    let hexNumArray = [] as number[];

    hexCad.forEach(number => {
        hexNumArray.push(parseInt(number,16));
    });

    return btoa(String.fromCharCode.apply(null, hexNumArray));

}


