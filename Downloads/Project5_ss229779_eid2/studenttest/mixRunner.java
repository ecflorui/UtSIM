package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class mixRunner {
    @Test
    public void testBgo() {
        Assert.assertTrue(true);
    }

    @Test
    public void testAgo() {
        Assert.assertEquals(9, 10); 
    }

    @Test
    public void testC() {
        Assert.assertEquals(100, 99+1); 
    }
}