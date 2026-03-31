package studenttest;

import unittest.annotations.Test;
import unittest.assertions.Assert;

public class mixFilter {
    @Test
    public void takethis() {
        Assert.assertTrue(true);
    }

    @Test
    public void dontwantthis() {
        Assert.assertTrue(false); 
    }

    @Test
    public void dontwantthiseither() {
        Assert.assertTrue(false);
    }

    @Test
    public void andthis() {
        Assert.assertTrue(true);
    }
}
