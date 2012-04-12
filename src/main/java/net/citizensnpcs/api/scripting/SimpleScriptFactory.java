package net.citizensnpcs.api.scripting;

import javax.script.CompiledScript;
import javax.script.ScriptException;

public class SimpleScriptFactory implements ScriptFactory {
    private final CompiledScript src;
    private final ContextProvider[] providers;

    SimpleScriptFactory(CompiledScript src, ContextProvider... providers) {
        if (src == null)
            throw new IllegalArgumentException("src cannot be null");
        if (providers == null)
            providers = new ContextProvider[0];
        this.src = src;
        this.providers = providers;
    }

    @Override
    public Script newInstance() {
        Script script;
        try {
            script = new SimpleScript(src);
        } catch (ScriptException e) {
            return null;
        }
        for (ContextProvider provider : providers) {
            provider.provide(script);
        }
        return script;
    }
}