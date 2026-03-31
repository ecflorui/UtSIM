package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class ChildInheritTest extends EasyPasses {
    @Test
    public void testChild() {
        Assert.assertTrue(true);
    }
}