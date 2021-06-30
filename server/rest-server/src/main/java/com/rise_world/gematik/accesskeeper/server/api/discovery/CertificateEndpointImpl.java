/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.discovery;

import com.rise_world.gematik.accesskeeper.server.service.CertService;
import com.rise_world.gematik.idp.server.api.discovery.CertificateEndpoint;
import com.rise_world.gematik.idp.server.api.discovery.JsonWebKey;
import com.rise_world.gematik.idp.server.api.discovery.JsonWebKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Response;

import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_CURVE;
import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_X_COORDINATE;
import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_Y_COORDINATE;

@RestController
public class CertificateEndpointImpl implements CertificateEndpoint {

    private final CertService certService;

    @Autowired
    public CertificateEndpointImpl(CertService certService) {
        this.certService = certService;
    }

    @Override
    public Response getJsonWebKeys() {
        JsonWebKeys keys = new JsonWebKeys();
        keys.getKeys().add(transform(certService.getSignatureCert()));
        keys.getKeys().add(transform(certService.getEncryptionKey()));
        return Response.ok(keys).build();
    }

    @Override
    // @AFO: A_20687-01 - Endpunkt zum Abrufen von puk_idp_enc
    public Response getPukIdpEnc() {
        return Response.ok(transform(certService.getEncryptionKey())).build();
    }

    @Override
    // @AFO: A_20687-01 - Endpunkt zum Abrufen von puk_idp_sig
    public Response getPukIdpSig() {
        return Response.ok(transform(certService.getSignatureCert())).build();
    }

    private JsonWebKey transform(org.apache.cxf.rs.security.jose.jwk.JsonWebKey src) {
        JsonWebKey dst = new JsonWebKey();
        dst.setKid(src.getKeyId());
        if (src.getPublicKeyUse() != null) {
            dst.setUse(src.getPublicKeyUse().toString());
        }
        dst.setKty(src.getKeyType().toString());
        dst.setCrv(src.getStringProperty(EC_CURVE));
        dst.setX(src.getStringProperty(EC_X_COORDINATE));
        dst.setY(src.getStringProperty(EC_Y_COORDINATE));
        dst.setX5c(src.getX509Chain());

        return dst;
    }
}
