/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.addon;

import com.google.gson.*;

import java.lang.reflect.Type;

public class StackTraceElementSerializer implements JsonSerializer<StackTraceElement>, JsonDeserializer<StackTraceElement>
{
    @Override
    public JsonElement serialize(StackTraceElement src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonObject ob = new JsonObject();
        ob.addProperty("loader", src.getClassLoaderName());
        ob.addProperty("class", src.getClassName());
        ob.addProperty("file", src.getFileName());
        ob.addProperty("line", src.getLineNumber());
        ob.addProperty("method", src.getMethodName());
        ob.addProperty("module", src.getModuleName());
        ob.addProperty("version", src.getModuleVersion());
        return ob;
    }
    
    @Override
    public StackTraceElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject ob = json.getAsJsonObject();
        return new StackTraceElement(ob.get("loader").getAsString(), ob.get("module").getAsString(), ob.get("version").getAsString(), ob.get("class").getAsString(), ob.get("method").getAsString(), ob.get("file").getAsString(), ob.get("line").getAsInt());
    }
}
