/**
 * Copyright (c) 2009 Red Hat, Inc.
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
package org.fedoraproject.candlepin.model.test;

import static org.junit.Assert.*;

import org.fedoraproject.candlepin.auth.Verb;
import org.fedoraproject.candlepin.exceptions.ForbiddenException;
import org.fedoraproject.candlepin.model.Owner;
import org.fedoraproject.candlepin.test.DatabaseTestFixture;
import org.junit.Test;

/**
 * OwnerCuratorTest
 */
public class OwnerCuratorAccessControlTest extends DatabaseTestFixture {
    
    @Test
    public void superAdminCanCreateAnOwner() {
        setupPrincipal(null, Verb.SUPER_ADMIN);
        securityInterceptor.enable();
        
        Owner owner = createOwner();
        assertNotNull(ownerCurator.find(owner.getId()));
    }
    
    @Test(expected = ForbiddenException.class)
    public void ownerAdminCannotCreateAnOwner() {
        Owner owner = createOwner();
        setupPrincipal(owner, Verb.OWNER_ADMIN);
        securityInterceptor.enable();
        
        createOwner();
    }
    
    @Test(expected = ForbiddenException.class)
    public void consumerCannotCreateAnOwner() {
        Owner owner = createOwner();
        setupPrincipal(owner, Verb.CONSUMER);
        securityInterceptor.enable();
        createOwner();
    }
}
