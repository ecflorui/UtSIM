package studenttest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class alphaOrdered {

    @Test
    @Order(1)
    public void Zgo() {
        Assert.assertEquals(2,2);
    }

    @Test
    @Order(5)
    public void goLast() {
        Assert.assertEquals(3,2);
    }

    @Test
    @Order(1)
    public void Ago() {
        Assert.assertTrue(false);
    }

    @Test
    @Order(1)
    public void Mgo() {
        Assert.assertTrue(true);
    }
}