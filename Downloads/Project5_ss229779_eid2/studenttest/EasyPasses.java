package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;


public class EasyPasses {
    @Test
    public void testEasy() {
        Assert.assertEquals(99, 33 * 3);
    }
    @Test
    public void testTrue() {
        Assert.assertTrue(true);
    }
    @Test
    public void testSimple() {
        Assert.assertEquals(1, 1);
    }
}