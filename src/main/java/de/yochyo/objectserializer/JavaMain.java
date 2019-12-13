package de.yochyo.objectserializer;

import org.json.JSONObject;

import java.util.Arrays;

public class JavaMain {
	public static void main(String[] args){
		ObjectSerializer o = ObjectSerializer.INSTANCE;
		JSONObject json = o.toJSONObject(new JavaFoo());
		System.out.println(json);
		System.out.println(Arrays.toString(o.toObject(json, JavaFoo.class).intList.toArray()));
		int[] bb = {};
		//KotlinMain.INSTANCE.a(bb);+
	}
}
