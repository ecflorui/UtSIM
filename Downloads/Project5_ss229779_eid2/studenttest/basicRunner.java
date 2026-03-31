package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class basicRunner {
    @Test
    public void Zgo() {
        Assert.assertTrue(true);
    }

    @Test
    public void Mgo() {
        Assert.assertEquals(2, 3);
    }
}