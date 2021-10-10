package edu.regis.universeplayer.addon;

import com.google.gson.*;
import edu.regis.universeplayer.browserCommands.CommandConfirmation;
import edu.regis.universeplayer.browserCommands.CommandReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class CommandReturnSerializer implements JsonSerializer<CommandReturn<?>>, JsonDeserializer<CommandReturn<?>>
{
    private static final Logger logger = LoggerFactory.getLogger(CommandReturnSerializer.class);

    @Override
    public CommandReturn<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        logger.debug("Deserializing return value {}", json);
        CommandReturn<?> returnVal;
        CommandConfirmation confirmation;
        Object value = null;
        JsonObject jsonOb = json.getAsJsonObject();
        JsonElement jsonVal = jsonOb.get("returnValue");
        confirmation = context.deserialize(jsonOb.getAsJsonObject("confirmation"), CommandConfirmation.class);
        try
        {
            value = deserializeObject(jsonVal, context);
        }
        catch (ClassNotFoundException e)
        {
            logger.error("Could not deserialize object {}.", json, e);
        }
        finally
        {
            returnVal = new CommandReturn<>(value, confirmation);
        }
        return returnVal;
    }

    /**
     * Attempts to determine what type of object is provided and deserializes
     * it.
     *
     * @param jsonVal The element to deserialize. If it is an object, a "type"
     *                field will be used to obtain the class.
     * @param context The deserialization context.
     * @return The deserialized object.
     * @throws ClassNotFoundException If the provided "type" field did not
     *                                contain a known class.
     */
    private Object deserializeObject(JsonElement jsonVal, JsonDeserializationContext context) throws ClassNotFoundException
    {
        Object value;
        if (jsonVal.isJsonPrimitive())
        {
            JsonPrimitive primVal = jsonVal.getAsJsonPrimitive();
            if (primVal.isString())
            {
                value = primVal.getAsString();
            }
            else if (primVal.isBoolean())
            {
                value = primVal.getAsBoolean();
            }
            else if (primVal.isNumber())
            {
                value = primVal.getAsNumber();
            }
            else
            {
                value = 0;
            }
        }
        else if (jsonVal.isJsonArray())
        {
            JsonArray arrVal = jsonVal.getAsJsonArray();
            if (arrVal.size() > 0)
            {
                value = new ArrayList<>();
                for (JsonElement el : arrVal)
                {
                    ((ArrayList) value).add(deserializeObject(el, context));
                }
            }
            else
            {
                value = new ArrayList<>();
            }
        }
        else if (jsonVal.isJsonObject())
        {
            JsonObject obVal = jsonVal.getAsJsonObject();
            Class type;
            if (obVal.has("type"))
            {
                type = Class.forName(obVal.get("type").getAsString());
                value = context.deserialize(obVal, type);
                logger.debug("Deserializing object of type {} {}", obVal.get("type").getAsString(), value);
            }
            else
            {
                value = context.deserialize(obVal, Object.class);
                logger.debug("Deserializing object of type {} {}", value.getClass(), value);
            }
        }
        else
        {
            value = null;
        }
        if (!jsonVal.isJsonObject())
        {
            logger.debug("Deserializing non-object {}", jsonVal);
        }
        return value;
    }

    @Override
    public JsonElement serialize(CommandReturn<?> src, Type typeOfSrc, JsonSerializationContext context)
    {
        return serializeObject(src, context);
    }

    /**
     * Attempts to determine what type of object is provided and deserializes
     * it.
     *
     * @param value   The element to deserialize. If it is an object, a "type"
     *                field will be used to obtain the class.
     * @param context The serialization context.
     * @return The serialized object.
     */
    private JsonElement serializeObject(Object value, JsonSerializationContext context)
    {
        JsonElement jsonVal;
        if (value == null)
        {
            jsonVal = JsonNull.INSTANCE;
        }
        else
        {
            if (value.getClass().isArray())
            {
                value = Arrays.stream((Object[]) value).collect(Collectors.toList());
            }
            if (value instanceof String || value instanceof Number || value instanceof Boolean)
            {
                jsonVal = context.serialize(value);
            }
            else if (value instanceof Collection)
            {
                jsonVal = new JsonArray();
                for (Object val : (Collection) value)
                {
                    jsonVal.getAsJsonArray().add(serializeObject(val, context));
                }
            }
            else
            {
                jsonVal = context.serialize(value, value.getClass());
                jsonVal.getAsJsonObject().addProperty("type", value.getClass().getName());
            }
        }
        return jsonVal;
    }
}
