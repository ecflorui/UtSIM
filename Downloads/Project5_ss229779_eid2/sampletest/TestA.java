package sampletest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class TestA {
    @Test
    public void test1() {
        Assert.assertTrue(true);
    }
    @Test
    public void test2() {
        Assert.assertEquals(3, 1 + 2);
    }
}
