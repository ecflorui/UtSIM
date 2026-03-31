package studenttest;

import unittest.annotations.Order;
import unittest.annotations.Ordered;
import unittest.annotations.Test;
import unittest.assertions.Assert;

@Ordered
public class maxOrder {

    @Test
    public void noRankGivenA() {
        Assert.assertEquals(2,2);
    }

    @Test
    @Order(3)
    public void go1st() {
        Assert.assertEquals(5, 2 + 2);
    }

    @Test
    public void noRankGivenB() {
        Assert.assertEquals(2, 3);
    }


}