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

package moe.hendeca.minidialog;

import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.element.ElementParser;
import moe.hendeca.minidialog.except.*;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.token.TokenReader;
import moe.hendeca.minidialog.util.DialogResourcePath;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@NullMarked
@ApiStatus.Internal
abstract class AbstractMiniDialog<D extends Dialog> implements MiniDialog {

    private static final String PACKAGE_PREFIX = AbstractMiniDialog.class.getPackageName() + ".";

    private static ClassLoader callerClassLoader() {
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();
        if (cl == null) cl = AbstractMiniDialog.class.getClassLoader();

        StackTraceElement[] els = t.getStackTrace();
        for (int i = 1; i < els.length; i++) {
            StackTraceElement el = els[i];
            String cn = el.getClassName();
            if (cn.startsWith(PACKAGE_PREFIX) && cn.indexOf('.', PACKAGE_PREFIX.length()) == -1) continue;
            Class<?> resolved;
            try {
                resolved = Class.forName(cn);
            } catch (ClassNotFoundException ignored) {
                break;
            }
            cl = resolved.getClassLoader();
            break;
        }

        return cl;
    }

    private static @Range(from = 0, to = 4) int memberAccessibility(Member member) {
        return switch (member.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) {
            case Modifier.PRIVATE -> 1;
            case Modifier.PROTECTED -> 2;
            case 0 -> 3;
            case Modifier.PUBLIC -> 4;
            default -> 0; // not really supposed to happen
        };
    }

    private static boolean ctorShadows(Constructor<?> target, Constructor<?> referent) {
        if (memberAccessibility(target) > memberAccessibility(referent)) return true;
        Parameter[] ap = target.getParameters();
        Parameter[] bp = referent.getParameters();
        if (ap.length != bp.length) return ap.length < bp.length;
        for (int i = 0; i < ap.length; i++) {
            Class<?> at = ap[i].getType();
            Class<?> bt = bp[i].getType();
            if (bt.isAssignableFrom(at)) continue;
            if (at.isAssignableFrom(bt)) return false;
        }
        return true;
    }

    private static @Nullable Constructor<?> matchingCtor(Class<?> clazz, Object[] args) {
        final int paramCount = args.length;
        Constructor<?> ret = null;
        outer:
        for (Constructor<?> con : clazz.getDeclaredConstructors()) {
            if (con.getParameterCount() != paramCount) continue;
            Parameter[] params = con.getParameters();
            assert params.length == paramCount;
            for (int i = 0; i < paramCount; i++) {
                if (!params[i].getType().isInstance(args[i])) continue outer;
            }
            if (ret == null || ctorShadows(con, ret)) {
                ret = con;
            }
        }
        return ret;
    }

    //

    protected abstract D newDialog(
            DialogElement document,
            @Nullable DialogManager manager
    );

    private DialogElement parse(TokenReader.Source source) throws DialogParseException {
        return (new ElementParser(new TokenReader(source))).parse();
    }

    @Override
    public DialogElement parse(@Language("XML") String text) throws DialogParseException {
        return this.parse(TokenReader.Source.string(text));
    }

    @Override
    public DialogElement read(Reader src) throws DialogException {
        return this.parse(TokenReader.Source.reader(src));
    }

    @Override
    public DialogElement read(InputStream src) throws DialogException {
        return this.read(new BufferedReader(new InputStreamReader(src, StandardCharsets.UTF_8)));
    }

    @Override
    public DialogElement read(Path src) throws DialogException {
        try (BufferedReader r = Files.newBufferedReader(src, StandardCharsets.UTF_8)) {
            return read(r);
        } catch (IOException e) {
            throw new DialogIOException("Failed to open path " + src + " for reading", e);
        }
    }

    @Override
    public DialogElement read(File src) throws DialogException {
        return this.read(src.toPath());
    }

    @Override
    public DialogElement read(@DialogResourcePath String path) throws DialogException {
        ClassLoader cl = callerClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) throw new DialogNotFoundException(cl, path);
            return this.read(in);
        } catch (IOException e) {
            throw new DialogIOException(e);
        }
    }

    @Override
    public D init(DialogElement element) {
        return this.newDialog(element.copy(), null);
    }

    @Override
    public D init(DialogElement element, @Nullable Class<? extends DialogManager> managerClass, Object... managerArgs) {
        if (managerClass == null)
            return this.newDialog(element.copy(), null);

        if (managerClass.isInterface() || Modifier.isAbstract(managerClass.getModifiers())) {
            throw new IllegalArgumentException("Manager class " + managerClass.getName()
                    + " is not directly instantiable");
        }

        Constructor<?> ctor = matchingCtor(managerClass, managerArgs);
        if (ctor == null) {
            throw new IllegalArgumentException("No matching constructor on " + managerClass.getName() +
                    " which accepts the given arguments");
        }

        try {
            ctor.setAccessible(true);
        } catch (InaccessibleObjectException e) {
            throw new DialogBindException("Cannot make constructor for manager class " +
                    managerClass.getName() + " accessible", e);
        }

        DialogManager instance;
        try {
            instance = (DialogManager) ctor.newInstance(managerArgs);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) cause = e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error re) throw re;
            throw new DialogBindException("Constructor for manager class " +
                    managerClass.getName() + " raised a checked exception", cause);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Forbidden reflection error", e);
        }

        return this.newDialog(element.copy(), instance);
    }

}
