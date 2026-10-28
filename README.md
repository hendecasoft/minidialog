# MiniDialog

MiniDialog is a markup specification and Minecraft client/server library for
representing custom [dialogs](https://minecraft.wiki/w/Dialog) as a reactive mutable 
tree, similar to a [DOM](https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model).
MiniDialog marries XML with [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/)
to format rich text. This is accomplished via context-aware parsing, for example when encountering
an opening `<label>` tag the document will be interpreted as MiniMessage until the first unescaped
closing `</label>` tag. Any element in the document may have an `id` to identify it at runtime.
Accessing an element and changing any of its state will result in the re-evaluation and re-transmission 
of the dialog.

## Getting MiniDialog
Replace `PLATFORM` with the target [platform](#Platforms)
and  replace `VERSION` with the latest version available
[from Maven Central](https://repo1.maven.org/maven2/moe/hendeca/minidialog-api/).

<table>
<tr>
<th>Build Script</th>
<th>Syntax</th>
</tr>
<tr>
<td>Gradle (Groovy DSL)<br><i>build.gradle</i></td>
<td>

```groovy
dependencies {
    implementation 'moe.hendeca:minidialog-PLATFORM:VERSION'
}
```

</td>
</tr>
<tr>
<td>Gradle (Kotlin DSL)<br><i>build.gradle.kts</i></td>
<td>

```kotlin
dependencies {
    implementation("moe.hendeca:minidialog-PLATFORM:VERSION")
}
```

</td>
</tr>
<tr>
<td>Maven<br><i>pom.xml</i></td>
<td>

```xml
<dependencies>
    <dependency>
        <groupId>moe.hendeca</groupId>
        <artifactId>minidialog-PLATFORM</artifactId>
        <version>VERSION</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

</td>
</tr>
</table>

> [!TIP]
> Most users will use [Shadow](https://github.com/GradleUp/shadow)
> to include the library in their plugin/mod release JAR. If you elect
> do so, it is best to also relocate the library into your own namespace
> as in `relocate("moe.hendeca.minidialog", "net.you.mod.shadow.minidialog")`.
> Alternatively, you may elect to use a platform library loading mechanism
> (see [Paper](https://docs.papermc.io/paper/dev/plugin-yml/#libraries)) or
> other runtime resolution mechanism (see [gremlin](https://github.com/jpenilla/gremlin))
> to share MiniDialog with other plugins/mods which use it.

## Example
### Document
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE dialog SYSTEM "https://hendeca.moe/schema/v1/minidialog.dtd">
<dialog>
    <title>Hello &lt;b&gt;world&lt;/b&gt;!</title>
    <body>
        <message>
            &lt;head:Wasabi_Thumbs/&gt; 
            Please appraise my 
            &lt;aqua&gt;diamond&lt;/aqua&gt;!
        </message>
        <item src="minecraft:diamond">
            <description>The diamond in question</description>
        </item>
    </body>
    <inputs>
        <input type="range" start="1" end="10" step="1" id="appraisal">
            <label>&lt;rainbow&gt;Appraisal&lt;/rainbow&gt;</label>
        </input>
    </inputs>
    <footer>
        <action type="custom" id="accept">
            <label>Sure!</label>
        </action>
        <action type="runCommand" value="tell @s :(">
            <label>No thanks.</label>
        </action>
    </footer>
</dialog>
```

> [!TIP]
> The XML prolog (`<?xml ... ?>`) and document type declaration
> (`<!DOCTYPE ...>`) are both optional but improve compatibility
> with XML tooling, especially IDE inspections as no language
> server for MiniDialog exists at this time. Configuring IDEA to
> interpret `.mdlg` files as XML and including the DTD should
> be enough to get usable inspections.

> [!IMPORTANT]
> Due to vanilla technical limitations, server implementations of
> MiniDialog cannot reliably detect when non-custom actions are clicked,
> and if that action would cause the dialog to close, when the dialog closes.
> It is encouraged that all of your dialog actions be of custom type
> to prevent issues.

### Code
```java
MiniDialog md = /* see Platforms */;

// Do this ONCE for each unique dialog type
DialogElement element = md.read("dialogs/sample.mdlg");

// Do this ONCE for each unique dialog instance (copies the element)
Dialog dialog = md.init(element, SampleDialogManager.class);

// Do this ONCE for each user viewing the same dialog
DialogUser user = md.user(/* see Platforms */);
dialog.show(user);
```

```java
class SampleDialogManager implements DialogManager {
    
    // Overview of annotations:
    // - @DialogBind.Context      : Inject a context field. Constant during execution of any bound method
    // - @DialogBind.Element      : Inject an element with a given ID. Constant during execution of any bound method
    // - @DialogBind.Init         : Execute annotated method after initializing this manager
    // - @DialogBind.Open         : Execute annotated method after first opening it for the given user (optional first parameter)
    // - @DialogBind.Think        : Execute annotated method at a specified interval while the dialog is open for one or more users
    // - @DialogBind.CustomAction : Execute annotated method when the action with the given ID and type "custom" is clicked
    
    @DialogBind.Context
    Audience audience;
    
    @DialogBind.Element("appraisal")
    RangeInputElement appraisal;
    
    @DialogBind.CustomAction("accept")
    void accept(DialogResponse response) {
        float value = response.value(this.appraisal);
        this.audience.sendMessage(Component.empty()
                .append(Component.text("Your appraisal: "))
                .append(Component.text(value).color(NamedTextColor.GOLD)));
    }
    
}
```

## Platforms
MiniDialog is a multi-platform library with a common API artifact
and many platform implementation artifacts with their own entrypoints.
See below for how to initialize MiniDialog on your platform of choice.

### Paper (`minidialog-paper`)
```java
public final class SamplePlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        MiniDialog md = PaperMiniDialog.miniDialog(this);
        // ...
    }
    
}
```

### Velocity (`minidialog-velocity`)
```java
@Plugin(/* ... */)
public final class SamplePlugin {
    
    private final ProxyServer server;
    
    @Inject
    SamplePlugin(ProxyServer server) {
        this.server = server;
    }
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        MiniDialog md = VelocityMiniDialog.miniDialog(this.server, this);
        // ...
    }
    
}
```

> [!IMPORTANT]
> The Velocity implementation will require [PacketEvents](https://modrinth.com/plugin/packetevents/versions?l=velocity) until
> a native dialog API is available. If your plugin uses
> `minidialog-velocity` it should also either:
> - Declare PacketEvents as a dependency in the `plugin.yml`
> - **OR** bundle PacketEvents as described [here](https://docs.packetevents.com/setup-when-bundling/a-beginners-guide-to-bundling/)
>   and create an instance as described [here](https://docs.packetevents.com/setup-when-bundling/master/), then pass that 
>   instance to the `miniDialog` factory method.

### Sponge (`minidialog-sponge`)
```java
@Plugin(/* ... */)
public final class SamplePlugin {
    
    private final PluginContainer container;
    
    @Inject
    SamplePlugin(PluginContainer container) {
        this.container = container;
    }
    
    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        MiniDialog md = SpongeMiniDialog.miniDialog(event.engine(), this.container);
        // ...
    }
    
}
```

> [!IMPORTANT]
> The Sponge implementation will require [PacketEvents](https://modrinth.com/plugin/packetevents/versions?l=sponge) until
> a native dialog API is available. If your plugin uses
> `minidialog-sponge` it should also either:
> - Declare PacketEvents as a dependency in the `sponge_plugins.json`
> - **OR** bundle PacketEvents as described [here](https://docs.packetevents.com/setup-when-bundling/a-beginners-guide-to-bundling/)
>   and create an instance as described [here](https://docs.packetevents.com/setup-when-bundling/master/), then pass that
>   instance to the `miniDialog` factory method.

### Fabric & NeoForge (`minidialog-modded`)
```java
// Client
MiniDialog md = ModdedMiniDialog.miniDialog();

// Server
MinecraftServer server = /* ... */;
MiniDialog md = ModdedMiniDialog.miniDialog(server);
```

> [!IMPORTANT]
> Fabric & NeoForge do not ship Adventure or MiniMessage,
> so you will need to add either [`adventure-platform-fabric`](https://docs.papermc.io/adventure/platform/fabric/)
> or [`adventure-platform-neoforge`](https://docs.papermc.io/adventure/platform/neoforge/)
> as well as [`adventure-text-minimessage`](https://docs.papermc.io/adventure/minimessage/api/)
> to your buildscript. MiniDialog does **not** depend on these
> directly as it would pin them to a specific version. 
> While the Adventure platform typically requires updates for
> every Minecraft version, MiniDialog is fairly version-agnostic.

## File Format
- **Extension**: `.mdlg`
- **MIME Type**: `application/vnd.minidialog+xml` *(unregistered)*

## License
```text
Copyright 2026 hendecasoft

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
