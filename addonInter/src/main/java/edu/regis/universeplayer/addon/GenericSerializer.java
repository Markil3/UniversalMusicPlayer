package edu.regis.universeplayer.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import edu.regis.universeplayer.browserCommands.BrowserError;

public class GenericSerializer implements JsonSerializer<Object>,
        JsonDeserializer<Object>
{
    private static final Logger logger =
            LoggerFactory.getLogger(GenericSerializer.class);

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc,
                                 JsonSerializationContext context)
    {
        JsonObject val = new JsonObject();
        for (Field field : src.getClass().getFields())
        {
            try
            {
                if (!Modifier.isTransient(field.getModifiers()))
                {
                    val.add(field.getName(), context.serialize(field.get(src)));
                }
            }
            catch (IllegalAccessException e)
            {
                logger.error("Could not store field " + field.getName() + " " +
                        "of class " + src.getClass().getName());
            }
        }
        val.addProperty("type", src.getClass().getName());
        return val;
    }

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        Class<?> clazz;
        Field field;
        Object ob = null;
        if (json.isJsonObject() && json.getAsJsonObject().has("type"))
        {
            JsonObject jsonOb = json.getAsJsonObject();
            try
            {
                clazz = Class.forName(jsonOb.remove("type").getAsString());
                ob = clazz.getConstructor().newInstance();
                for (Map.Entry<String, JsonElement> fields: jsonOb.entrySet())
                {
                    field = clazz.getField(fields.getKey());
                    if (field.getType().isAssignableFrom(String.class))
                    {
                        field.set(ob, fields.getValue().getAsString());
                    }
                    else if (field.getType().isAssignableFrom(int.class))
                    {
                        field.setInt(ob, fields.getValue().getAsInt());
                    }
                    else if (field.getType().isAssignableFrom(double.class))
                    {
                        field.setDouble(ob, fields.getValue().getAsDouble());
                    }
                    else if (field.getType().isAssignableFrom(byte.class))
                    {
                        field.setByte(ob, fields.getValue().getAsByte());
                    }
                    else if (field.getType().isAssignableFrom(String.class))
                    {
                        field.set(ob, fields.getValue().getAsString());
                    }
                    else if (field.getType().isAssignableFrom(boolean.class))
                    {
                        field.setBoolean(ob, fields.getValue().getAsBoolean());
                    }
                    else if (field.getType().isAssignableFrom(float.class))
                    {
                        field.setFloat(ob, fields.getValue().getAsFloat());
                    }
                    else if (field.getType().isAssignableFrom(long.class))
                    {
                        field.setLong(ob, fields.getValue().getAsLong());
                    }
                    else if (field.getType().isAssignableFrom(short.class))
                    {
                        field.setShort(ob, fields.getValue().getAsShort());
                    }
                    else if (field.getType().isAssignableFrom(URL.class))
                    {
                        try
                        {
                            field.set(ob, new URL(fields.getValue().getAsString()));
                        }
                        catch (MalformedURLException e)
                        {
                            logger.error("Could not create url {}",
                                    fields.getValue().getAsString(), e);
                        }
                    }
                    else if (fields.getValue().isJsonObject())
                    {
//                        clazz = fields.getValue().getAsJsonObject().
//                        field.set(ob, context.);
                    }
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e)
            {
                logger.error("Could not deserialize {} ", json, e);
            }
        }
        return ob;
    }
}
