package cuchaz.enigma.analysis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class ClassUtils {

	public static Set<Class<?>> getAllClasses(String packageName) {
		List<ClassLoader> classLoadersList = new LinkedList<>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());

		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
				.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))));


		return reflections.getSubTypesOf(Object.class);
	}

	public static String showClassFields(String clazz, ClassLoader classloader) throws Exception {
		return showClassFields(Class.forName(clazz, true, classloader));
	}

	public static String showClassFields(Class<?> clazz) throws Exception {
		return crawlClassFields(clazz, null, null, true);
	}

	public static String showClassFields(Class<?> clazz, String pattern) throws Exception {
		return crawlClassFields(clazz, null, pattern, true);
	}

	public static String crawlClassFields(Class<?> clazz) throws Exception {
		return crawlClassFields(clazz, null, null, false);
	}

	public static String crawlClassFields(Class<?> clazz, String pattern) throws Exception {
		return crawlClassFields(clazz, null, pattern, false);
	}

	public static String crawlClassFields(Class<?> clazz, Object instance,  String pattern, boolean showClassFields) {
		StringBuilder sb = new StringBuilder();
		try {
			if(pattern == null) {
				sb.append("\n#################\n" + clazz.getName() + "\n#################\n");
			}

			if (instance == null) {
				try {
					instance=clazz.getDeclaredConstructor().newInstance();
				} catch (Throwable e) {
					sb.append("couldn't create instance of " + clazz.getName() + "\n");
				}
			}
			sb.append("\n* Fields :\n");

			Field[] fields = clazz.getDeclaredFields();
			for(Field f : fields) {
				f.setAccessible(true);

				Object value;
				if (instance != null || Modifier.isStatic(f.getModifiers())) {
					value = f.get(instance);
					String stringValue = getValue(value);
					if(pattern == null) {
						if (value.getClass().isArray()) {
							sb.append("- " + f.getName() + " = " + Arrays.toString((Object[])value) + "\n");
						} else {
							sb.append("- " + f.getName() + " = " + value + "\n");
						}
					}
					checkPattern(pattern, stringValue, clazz.getName(), f.toString());
				}

			}

			sb.append("\n* Methods :\n");
			Method[] methods = clazz.getDeclaredMethods();
			for(Method m : methods) {
				String method = m.toString();
				if(pattern == null) {
					sb.append("* " + method + "\n");
				} else {
					sb.append(checkPattern(pattern, method, clazz.getName(), method) + "\n");
				}
			}
		} catch (Exception | NoClassDefFoundError e) {
			System.err.println("error when analyzing class : ");
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static String checkPattern(String pattern, String value, String clazz, String fieldMethod) {
		StringBuilder sb = new StringBuilder();
		if(pattern != null && value != null) {
			Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE).matcher(value);

			if (m.matches()) {
				sb.append("pattern found in class " + clazz + ", field/method " + fieldMethod + " : " + value);
			}
		}
		return sb.toString();
	}

	private static String getValue(Object value) {
		String strVal = null;
		if (value instanceof char[]) {
			strVal = new String((char[])value);
		} else if (value instanceof byte[]) {
			strVal = new String((byte[])value);
		} else if(value instanceof Object[]) {
			strVal = Arrays.toString((Object[])value);
		} else {
			strVal = String.valueOf(value);
		}

		return strVal;
	}
}
