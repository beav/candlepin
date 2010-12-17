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
package org.fedoraproject.candlepin.resource.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.fedoraproject.candlepin.model.Status;
import org.fedoraproject.candlepin.resource.StatusResource;

import org.junit.Test;

import java.io.File;
import java.io.PrintStream;


/**
 * StatusResourceTest
 */
public class StatusResourceTest {

    @Test
    public void status() throws Exception {
        PrintStream ps = new PrintStream(new File(this.getClass()
            .getClassLoader().getResource("candlepin_info.properties").toURI()));
        ps.println("version=${version}");
        ps.println("release=${release}");
        StatusResource sr = new StatusResource();
        Status s = sr.status();
        assertNotNull(s);
        assertEquals("${release}", s.getRelease());
        assertEquals("${version}", s.getVersion());
        assertTrue(s.getResult().booleanValue());
    }

    @Test
    public void unknown() throws Exception {
        PrintStream ps = new PrintStream(new File(this.getClass()
            .getClassLoader().getResource("candlepin_info.properties").toURI()));
        ps.println("foo");
        StatusResource sr = new StatusResource();
        Status s = sr.status();
        assertNotNull(s);
        assertEquals("Unknown", s.getRelease());
        assertEquals("Unknown", s.getVersion());
        assertTrue(s.getResult().booleanValue());
    }
}