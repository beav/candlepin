/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.resource;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.candlepin.exceptions.BadRequestException;
import org.candlepin.model.CertificateSerial;
import org.candlepin.model.CertificateSerialCurator;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCertificate;
import org.candlepin.model.Product;
import org.candlepin.model.ProductCurator;
import org.candlepin.pki.PKIUtility;
import org.candlepin.service.impl.DefaultEntitlementCertServiceAdapter;

import com.google.inject.Inject;

/**
 * SpliceResource
 */
@Path("/splice")
public class SpliceResource {
    
    private static Logger log = Logger.getLogger(SpliceResource.class);
    
    private ProductCurator productCurator;
    private PKIUtility pkiUtility;
    private CertificateSerialCurator serialCurator;

    // don't use the interface, since the interface is for hosted and standalone
    private DefaultEntitlementCertServiceAdapter entCertAdapter;


    @Inject
    public SpliceResource(ProductCurator productCurator,
        DefaultEntitlementCertServiceAdapter entCertAdapter, PKIUtility pkiUtility, CertificateSerialCurator serialCurator) {
        this.productCurator = productCurator;
        this.entCertAdapter = entCertAdapter;
        this.pkiUtility = pkiUtility;
        this.serialCurator = serialCurator;
        
    }
    
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    @Path("test")
    public String getCertificate() {
        return "hello!!";
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cert")
    public Entitlement getCertForProducts(@QueryParam("productIDs") String products, @QueryParam("start") Date startDate,
                                                    @QueryParam("end") Date endDate, @QueryParam("rhicUUID") String rhicUUID) throws IOException {
        
        log.debug("productIDs: " + products);
        Set<Product> productSet = findProducts(products);
        
        // just use right now and one hour from now, temporarily
        startDate = new Date();
        endDate = DateUtils.addHours(startDate, 1);
        KeyPair keyPair = null;
        try {
            keyPair = pkiUtility.generateNewKeyPair();
        }
        catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        X509Certificate cert = null;

        CertificateSerial serial = new CertificateSerial(endDate);
        serialCurator.create(serial);
        
        try {
            cert = entCertAdapter.createSpliceX509Cert(productSet, BigInteger.valueOf(serial.getId()),
                                                            keyPair, startDate, endDate, rhicUUID);
        }
        catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        EntitlementCertificate entitlementCert = new EntitlementCertificate();
        
        entitlementCert.setCertAsBytes(pkiUtility.getPemEncoded(cert));
        
        entitlementCert.setKeyAsBytes(pkiUtility.getPemEncoded(keyPair.getPrivate()));

        Entitlement toReturn = new Entitlement();
        
        Set<EntitlementCertificate> certs = new HashSet<EntitlementCertificate>();
        certs.add(entitlementCert);
        
        toReturn.setCertificates(certs);
        
        toReturn.setStartDate(startDate);
        toReturn.setEndDate(endDate);
        
        
        return toReturn;

   }
    
    private Set<Product> findProducts(String productString) {
        Set<Product> productSet = new HashSet<Product>();
        Product product = null;
        
        if (productString != null && !productString.equals("")) {
            for (String s : productString.split(" ")) {
                log.debug("evaluating " + s);
                product = productCurator.find(s);
                if (product == null) {
                    throw new BadRequestException("Product " + s + " does not exist in product database");
                }
                log.debug("adding product " + s + " to set");
                productSet.add(product);
            }
        }
        return productSet;
    }
}

