package studenttest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class mixOrder {

    @Test
    public void testLAST() {
        Assert.assertEquals(5, 5);
        Assert.assertEquals(1, 2);
    }

    @Test
    @Order(2)
    public void test2nd() {
        Assert.assertTrue(false); 
    }

    @Test
    @Order(1)
    public void test1st() {
        Assert.assertEquals(99, 99);
    }
}