package de.yochyo.objectserializer;

import java.util.*;

public class JavaFoo {
	int intValue = 1;
	Integer[] intArray = {1, 2, 3, 4, 5, 6};
	Collection<Integer> intList = new HashSet<Integer>();

	/*
	@Serializeable
	SerializeableRunnable run = new SerializeableRunnable() {
		public void run() {
			System.out.println("test");
		}
	};
	 */

	public JavaFoo() {
		intList.add(1);
		intList.add(2);
		intList.add(3);
		intList.add(4);
	}
}
