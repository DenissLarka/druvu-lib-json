package com.druvu.json.yaml;

import com.druvu.json.YamlBackend;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers {@link SnakeYamlBackend} for discovery via druvu-lib-loader.
 *
 * <p>The declared type is {@link YamlBackend}, not {@code JsonBackend}: discovery matches a factory by the exact class
 * it names, so this module can share a class path with a JSON backend without either shadowing the other.
 *
 * @author Deniss Larka
 */
@SuppressWarnings("rawtypes")
public final class SnakeYamlBackendFactory implements ComponentFactory<YamlBackend> {

    @Override
    public Class<YamlBackend> type() {
        return YamlBackend.class;
    }

    @Override
    public YamlBackend createComponent(Dependencies dependencies) {
        return new SnakeYamlBackend();
    }
}
