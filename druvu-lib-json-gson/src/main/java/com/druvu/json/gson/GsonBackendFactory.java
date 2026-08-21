package com.druvu.json.gson;

import com.druvu.json.JsonBuilderFactory;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers {@link GsonBackend} for discovery via druvu-lib-loader.
 *
 * <p>The raw {@code Backend} type is unavoidable: ServiceLoader-based discovery matches on the class object, which has
 * no type arguments.
 *
 * @author Deniss Larka
 */
@SuppressWarnings("rawtypes")
public final class GsonBackendFactory implements ComponentFactory<JsonBuilderFactory.Backend> {

    @Override
    public Class<JsonBuilderFactory.Backend> type() {
        return JsonBuilderFactory.Backend.class;
    }

    @Override
    public JsonBuilderFactory.Backend createComponent(Dependencies dependencies) {
        return new GsonBackend();
    }
}
