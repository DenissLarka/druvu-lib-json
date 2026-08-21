package com.druvu.json;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The api module has no backend on its test classpath, so discovery must fail with a message that tells the user what
 * to do.
 *
 * @author Deniss Larka
 */
public class TestBackendDiscovery {

    @Test
    public void failsWithClearMessageWhenNoBackendPresent() {
        IllegalStateException e = Assert.expectThrows(IllegalStateException.class, JsonBuilderFactory::buildObject);
        Assert.assertTrue(e.getMessage().contains("No JSON backend found"), e.getMessage());
    }

    @Test
    public void failureIsNotCachedAndThrowsAgain() {
        Assert.expectThrows(IllegalStateException.class, JsonBuilderFactory::buildArray);
        Assert.expectThrows(IllegalStateException.class, JsonBuilderFactory::buildArray);
    }
}
