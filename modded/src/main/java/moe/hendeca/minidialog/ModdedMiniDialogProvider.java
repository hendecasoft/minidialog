package moe.hendeca.minidialog;

import moe.hendeca.minidialog.server.ModdedServerMiniDialog;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@NullMarked
@ApiStatus.Internal
final class ModdedMiniDialogProvider {

    private static boolean INITIALIZED = false;
    private static @UnknownNullability ModdedMiniDialog INSTANCE;

    //

    static synchronized ModdedMiniDialog get(@Nullable MinecraftServer server) {
        if (INITIALIZED) return INSTANCE;
        ModdedMiniDialog ret = init(server);
        INSTANCE = ret;
        INITIALIZED = true;
        return ret;
    }

    private static ModdedMiniDialog init(@Nullable MinecraftServer server) {
        if (server != null) return new ModdedServerMiniDialog(server);
        ModdedMiniDialog ret = initClient();
        if (ret != null) return ret;
        throw new IllegalStateException("No client found and no server provided. " +
                "A server must be supplied when running on a server!");
    }

    @SuppressWarnings("JavaReflectionInvocation")
    private static @Nullable ModdedMiniDialog initClient() {
        Class<?> cls;
        try {
            cls = Class.forName("net.minecraft.client.Minecraft");
        } catch (ClassNotFoundException ignored) {
            return null;
        }

        Object instance;
        try {
            instance = cls.getDeclaredMethod("getInstance")
                    .invoke(null);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Client has no getInstance method");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) cause = e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error re) throw re;
            throw new IllegalStateException("Accessor raised a checked exception", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected reflection error", e);
        }

        ModdedMiniDialog ret;
        try {
            Class<?> implClass = Class.forName("moe.hendeca.minidialog.client.ModdedClientMiniDialog");
            Constructor<?> con = implClass.getDeclaredConstructor(cls);
            ret = (ModdedMiniDialog) con.newInstance(instance);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create client MiniDialog instance", e);
        }

        return ret;
    }

    //

    private ModdedMiniDialogProvider() { }

}
