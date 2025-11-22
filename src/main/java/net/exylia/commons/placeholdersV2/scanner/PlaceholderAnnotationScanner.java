package net.exylia.commons.placeholdersV2.scanner;

import net.exylia.commons.placeholdersV2.annotation.Placeholder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class PlaceholderAnnotationScanner {

    public List<Method> scanClass(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Method[] allMethods = clazz.getDeclaredMethods();

        for (Method method : allMethods) {
            if (method.isAnnotationPresent(Placeholder.class)) {
                if (isValidPlaceholderMethod(method)) {
                    methods.add(method);
                }
            }
        }

        return methods;
    }

    public List<Method> scanPackage(String packageName) {
        List<Method> methods = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            List<Class<?>> classes = findAllClassesInPackage(packageName, classLoader);

            for (Class<?> clazz : classes) {
                methods.addAll(scanClass(clazz));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning package: " + packageName, e);
        }

        return methods;
    }

    private boolean isValidPlaceholderMethod(Method method) {
        int modifiers = method.getModifiers();

        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            return false;
        }

        Class<?> returnType = method.getReturnType();
        return returnType != void.class && returnType != Void.class;
    }

    private List<Class<?>> findAllClassesInPackage(String packageName, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            var resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                classes.addAll(findClassesInDirectory(new java.io.File(url.getFile()), packageName));
            }
        } catch (Exception ignored) {
        }
        return classes;
    }

    private List<Class<?>> findClassesInDirectory(java.io.File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        return classes;
    }
}
