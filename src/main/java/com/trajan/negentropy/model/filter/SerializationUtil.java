package com.trajan.negentropy.model.filter;

import java.io.*;
import java.util.Base64;

public class SerializationUtil {

    public static String serialize(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static Object deserialize(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o  = ois.readObject();
        ois.close();
        return o;
    }
}
