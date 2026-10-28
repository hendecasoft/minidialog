/*
 * Copyright 2026 hendecasoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package moe.hendeca.minidialog.util;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@NullMarked
@ApiStatus.Internal
public final class VelocityProtocolVersions {

    public static final int MINECRAFT_26_1 = 775;
    private static final int SNAPSHOT_MASK = 1 << 30;

    //

    private final Class<?> cProtocolVersion;
    private final MethodHandle mProtocolVersionGetProtocol;
    private final int maxVersion;

    public VelocityProtocolVersions(ClassLoader classLoader) {
        Class<?> cProtocolVersion;
        MethodHandle mProtocolVersionGetProtocol;
        MethodHandle fProtocolVersionMaximumVersion;
        Object maxVersionObject;
        int maxVersion;

        try {
            cProtocolVersion = Class.forName(
                    "com.velocitypowered.api.network.ProtocolVersion",
                    true,
                    classLoader
            );
        } catch (ClassNotFoundException e1) {
            try {
                cProtocolVersion = Class.forName("com.velocitypowered.api.network.ProtocolVersion");
            } catch (ClassNotFoundException e2) {
                e2.addSuppressed(e1);
                throw new IllegalStateException("Failed to locate ProtocolVersion class", e2);
            }
        }

        try {
            mProtocolVersionGetProtocol = MethodHandles.publicLookup()
                    .findVirtual(cProtocolVersion, "getProtocol", MethodType.methodType(Integer.TYPE));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to locate getProtocol method", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access getProtocol method", e);
        }

        try {
            fProtocolVersionMaximumVersion = MethodHandles.publicLookup()
                    .findStaticGetter(cProtocolVersion, "MAXIMUM_VERSION", cProtocolVersion);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to locate MAXIMUM_VERSION field", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access MAXIMUM_VERSION field", e);
        }

        try {
            maxVersionObject = fProtocolVersionMaximumVersion.invoke();
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error re) throw re;
            throw new IllegalStateException("Field accessor generated a checked exception", t);
        }

        try {
            maxVersion = (int) mProtocolVersionGetProtocol.invoke(maxVersionObject);
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error re) throw re;
            throw new IllegalStateException("getProtocol raised a checked exception", t);
        }

        if ((maxVersion & SNAPSHOT_MASK) != 0) {
            maxVersion = (maxVersion ^ SNAPSHOT_MASK) - 1;
        }

        this.cProtocolVersion = cProtocolVersion;
        this.mProtocolVersionGetProtocol = mProtocolVersionGetProtocol;
        this.maxVersion = maxVersion;
    }

    //

    public boolean supports(
            @MagicConstant(valuesFromClass = VelocityProtocolVersions.class) int version
    ) {
        return this.maxVersion >= version;
    }

    public boolean playerSupports(
            @MagicConstant(valuesFromClass = VelocityProtocolVersions.class) int version,
            Player player
    ) {
        ProtocolVersion pv = player.getProtocolVersion();
        if (!this.cProtocolVersion.isInstance(pv))
            throw new IllegalStateException("Cannot compare ProtocolVersion instances from differing class loaders");

        int n;
        try {
            n = (int) this.mProtocolVersionGetProtocol.invokeExact(pv);
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error re) throw re;
            throw new IllegalStateException("getProtocol raised a checked exception", t);
        }

        if ((n & SNAPSHOT_MASK) != 0) return (n ^ SNAPSHOT_MASK) > version;
        return n >= version;
    }

}
