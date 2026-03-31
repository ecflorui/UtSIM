package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class FilterTest {
    @Test
    public void testA() {
        Assert.assertTrue(true);
    }

    @Test
    public void testB() {
        Assert.assertEquals(100000, 999);
    }

    @Test
    public void testC() {
        Assert.assertEquals(12, 12);
    }

    @Test
    public void testD() {
        Assert.assertTrue(false); 
}
}
