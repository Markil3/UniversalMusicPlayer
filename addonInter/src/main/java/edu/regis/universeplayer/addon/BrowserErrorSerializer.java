package edu.regis.universeplayer.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;

import edu.regis.universeplayer.browserCommands.BrowserError;

public class BrowserErrorSerializer implements JsonSerializer<BrowserError>, JsonDeserializer<BrowserError>
{
    @Override
    public JsonElement serialize(BrowserError src, Type typeOfSrc,
                                 JsonSerializationContext context)
    {
        JsonObject ob = new JsonObject();
        ob.addProperty("message", src.getMessage());
        String stack =
                Arrays.stream(src.getStackTrace())
                      .map(trace -> trace.getMethodName() + "@" + trace
                              .getFileName() + ":" + trace
                              .getLineNumber() + ":0").reduce("",
                        (s1, s2) -> s1.isEmpty() ? s2 : s1 + "\n" + s2);
        ob.addProperty("stack", stack);
        JsonArray suppressed = new JsonArray();
        for (Throwable throwable : src.getSuppressed())
        {
            suppressed.add(context.serialize(throwable));
        }
        return ob;
    }

    @Override
    public BrowserError deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject ob = json.getAsJsonObject();
        return new BrowserError(ob.get("name").getAsString(), ob.get(
                "message").getAsString(), ob.get("stack").getAsString());
    }
}
