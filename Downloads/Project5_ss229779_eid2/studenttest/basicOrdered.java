package studenttest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class basicOrdered {

    @Test
    @Order(2)
    public void go2nd() {
        Assert.assertEquals(2,2);
    }

    @Test
    @Order(3)
    public void go3rd() {
        Assert.assertEquals(3,2);
    }

    @Test
    @Order(1)
    public void go1st() {
        Assert.assertTrue(false);
    }


}