package sampletest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class TestC {
    @Test
    public void test4() {
        Assert.assertEquals(3, 2);
    }
    @Test
    public void test5() {
        Assert.assertEquals(3, 1 + 2);
    }
    @Test
    public void test6() {
        Assert.assertEquals(2, 2);
    }
}
