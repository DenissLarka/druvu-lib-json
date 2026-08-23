package com.druvu.json.gson;

import com.druvu.json.JsonBackend;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers {@link GsonBackend} for discovery via druvu-lib-loader.
 *
 * <p>The raw {@code JsonBackend} type is unavoidable: ServiceLoader-based discovery matches on the class object, which
 * has no type arguments.
 *
 * @author Deniss Larka
 */
@SuppressWarnings("rawtypes")
public final class GsonBackendFactory implements ComponentFactory<JsonBackend> {

    @Override
    public Class<JsonBackend> type() {
        return JsonBackend.class;
    }

    @Override
    public JsonBackend createComponent(Dependencies dependencies) {
        return new GsonBackend();
    }
}
