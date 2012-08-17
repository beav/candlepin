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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.candlepin.controller.Entitler;
import org.candlepin.controller.PoolManager;
import org.candlepin.exceptions.BadRequestException;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.Entitlement;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.ProductCurator;
import org.candlepin.model.SubscriptionCurator;

import com.google.inject.Inject;

/**
 * SpliceResource
 */
@Path("/splice")
public class SpliceResource {
    
    private static Logger log = Logger.getLogger(SpliceResource.class);
    
    private OwnerCurator ownerCurator;
    private ProductCurator productCurator;

    private SubscriptionCurator subCurator;
    private ConsumerCurator consumerCurator;
    private ConsumerTypeCurator consumerTypeCurator;
    private Entitler entitler;
    private PoolManager poolManager;


    @Inject
    public SpliceResource(OwnerCurator ownerCurator, ProductCurator productCurator, SubscriptionCurator subCurator,
        ConsumerCurator consumerCurator, ConsumerTypeCurator consumerTypeCurator, Entitler entitler, PoolManager poolManager) {
        this.ownerCurator = ownerCurator;
        this.productCurator = productCurator;
        this.subCurator = subCurator;
        this.consumerCurator = consumerCurator;
        this.consumerTypeCurator = consumerTypeCurator;
        this.entitler = entitler;
        this.poolManager = poolManager;
        
    }
    
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    @Path("test")
    public String getCertificate() {
        return "hello!!";
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON) // maybe just return a raw cert here, not sure
    @Path("cert")
    public List<Entitlement> getCertForProducts(@QueryParam("installed") String[] installed, @QueryParam("start") Date startDate,
                                                    @QueryParam("end") Date endDate, @QueryParam("rhic") String rhicId) {
        // start and end date are needed, but let's just make some up for now
        startDate = new Date();
        endDate = DateUtils.addHours(startDate, 1);
        List<String> installedIdNames = Arrays.asList(installed);
    
        Set<ConsumerInstalledProduct> consumerInstalledProducts = new HashSet<ConsumerInstalledProduct>(); 

        for (String str: installedIdNames) {
            String[] tokens = str.split("!");
            String id = tokens[0];
            String name = tokens[1];
            log.debug("installed product id: " + id + ", name: " + name);
            consumerInstalledProducts.add(new ConsumerInstalledProduct(id, name));            
        }
                
        // timestamp is used in name for debugging, in case it isn't cleaned up
        long unixTime = System.currentTimeMillis() / 1000L;
        
        Owner owner = ownerCurator.lookupByKey(rhicId);
        if (owner == null) {
            throw new BadRequestException("Owner " + rhicId + " not found");
        }
        ConsumerType type = consumerTypeCurator.lookupByLabel("system");
        Consumer consumer = consumerCurator.create(new Consumer("foo" + unixTime, "", owner, type));


        //consumer.setInstalledProducts(consumerInstalledProducts);
        for (ConsumerInstalledProduct cip : consumerInstalledProducts) {
            consumer.addInstalledProduct(cip);
        }
        
        consumerCurator.update(consumer);
        
        List<Entitlement> entitlements = entitler.bindByProducts(null, consumer, null);
        
        poolManager.removeAllEntitlements(consumer);
       
        consumerCurator.delete(consumer);
                
        return entitlements;
        
    }
}

