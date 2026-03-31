package studenttest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class orderRunner {
    @Test
    @Order(1)
    public void ZgoOrd() {
        Assert.assertTrue(true);
    }

    @Test
    @Order(2)
    public void MgoOrd() {
        Assert.assertEquals(2, 3);
    }
}