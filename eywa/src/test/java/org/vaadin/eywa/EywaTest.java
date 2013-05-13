package org.vaadin.eywa;

import junit.framework.Assert;

import org.junit.Test;

// JUnit tests here
public class EywaTest {

    @Test
    public void testGetType() {
        EywaProperty<String> p = new EywaProperty<String>("foo", String.class);

        Assert.assertEquals(p.getType(), String.class);
    }
}
