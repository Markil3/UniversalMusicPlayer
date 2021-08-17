/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ThrowableSerializer implements JsonSerializer<Throwable>, JsonDeserializer<Throwable>
{
    @Override
    public JsonElement serialize(Throwable src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonObject ob = new JsonObject();
        ob.addProperty("message", src.getMessage());
        ob.add("cause", context.serialize(src.getCause()));
        JsonArray stack = new JsonArray();
        for (StackTraceElement trace: src.getStackTrace())
        {
            stack.add(context.serialize(trace));
        }
        ob.add("trace", stack);
        JsonArray suppressed = new JsonArray();
        for (Throwable throwable: src.getSuppressed())
        {
            suppressed.add(context.serialize(throwable));
        }
        ob.add("suppressed", suppressed);
        return ob;
    }
    
    @Override
    public Throwable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject ob = json.getAsJsonObject();
        Throwable throwable = new Throwable(ob.get("message").getAsString(), context.deserialize(ob.get("cause"), typeOfT));
        throwable.initCause(context.deserialize(ob.get("cause"), Throwable.class));
        JsonArray traceJson = ob.getAsJsonArray("trace");
        StackTraceElement[] trace = new StackTraceElement[traceJson.size()];
        for (int i = 0; i < trace.length; i++)
        {
            trace[i] = context.deserialize(traceJson.get(i), StackTraceElement.class);
        }
        throwable.setStackTrace(trace);
        JsonArray suppressed = ob.getAsJsonArray("suppressed");
        for (JsonElement el: suppressed)
        {
            throwable.addSuppressed(context.deserialize(el, Throwable.class));
        }
        return throwable;
    }
}
