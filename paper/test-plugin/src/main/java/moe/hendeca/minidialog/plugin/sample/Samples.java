package moe.hendeca.minidialog.plugin.sample;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@NullMarked
public final class Samples {

    private static final String IMPL_PACKAGE_PREFIX = Samples.class.getPackageName() + ".impl";
    private static @Nullable Map<String, Sample> REGISTRY;

    //

    public static @Unmodifiable Set<String> keys() {
        return resolveRegistry().keySet();
    }

    public static @Nullable Sample get(String key) {
        return resolveRegistry().get(key);
    }

    private static synchronized Map<String, Sample> resolveRegistry() {
        Map<String, Sample> ret = REGISTRY;
        if (ret != null) return ret;
        try {
            ret = loadRegistry();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load samples registry", e);
        }
        REGISTRY = ret;
        return ret;
    }

    private static Map<String, Sample> loadRegistry() throws IOException {
        Map<String, Sample> ret = new HashMap<>();
        try (FileSystem fs = FileSystems.newFileSystem(codeSource())) {
            Path dir = fs.getPath(IMPL_PACKAGE_PREFIX.replace('.', '/'));
            try (Stream<Path> stream = Files.list(dir)) {
                Iterator<Path> iter = stream.iterator();
                while (iter.hasNext()) {
                    Path next = iter.next();
                    String fileName = next.getFileName().toString();
                    if (fileName.indexOf('$') != -1 || !fileName.endsWith(".class")) continue;
                    String className = fileName.substring(0, fileName.length() - 6);
                    String fullClassName = IMPL_PACKAGE_PREFIX + "." + className;
                    Sample sample = loadSampleFromClassName(fullClassName);
                    ret.put(sample.id(), sample);
                }
            }
        }
        return Collections.unmodifiableMap(ret);
    }

    private static Sample loadSampleFromClassName(String className) {
        Class<?> cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class found in JAR not resolvable at runtime", e);
        }

        if (!Sample.class.isAssignableFrom(cls)) {
            throw new IllegalStateException("Class " + className + " does not extend Sample");
        }

        if (cls.isInterface() | Modifier.isAbstract(cls.getModifiers())) {
            throw new IllegalStateException("Class " + className + " is not directly instantiable");
        }

        Constructor<? extends Sample> con;
        try {
            con = cls.asSubclass(Sample.class)
                    .getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Class " + className + " has no primary constructor", e);
        }

        try {
            return con.newInstance();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) cause = e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error re) throw re;
            throw new IllegalStateException("Constructor for class " + className + " raised a checked exception", cause);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected reflection error", e);
        }
    }

    private static Path codeSource() {
        try {
            return new File(Samples.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Code source is not a valid URI", e);
        }
    }

    //

    private Samples() { }

}
