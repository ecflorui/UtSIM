package sampletest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class TestD {
    @Test
    @Order(3)
    public void testA() {
        Assert.assertEquals(1, 1);
    }
    @Test
    @Order(2)
    public void testC() {
        Assert.assertEquals(3, 1 + 2);
    }
    @Test
    @Order(1)
    public void atest() {
        Assert.assertEquals(3, 3);
    }
}
