# Aura Visuals — Minecraft Fabric Mod

Mod client-side per Minecraft **1.21.1** (Fabric) che aggiunge effetti visivi "shader-like":

- **Particelle ambientali**: motine luminose che fluttuano lentamente nell'aria intorno al giocatore.
- **Particelle aura**: particelle che orbitano strette attorno al giocatore, come un alone.
- **Menu in-game** per attivare/disattivare ciascun effetto, cambiare i colori (hex), la densità e la velocità delle particelle.
- Tutto salvato in `config/auravisuals.json`, nessuna dipendenza esterna oltre a Fabric API.

Premi **virgola `,`** in gioco per aprire il menu delle impostazioni (keybind rimappabile dal menu Opzioni → Tasti di Minecraft).

## Ottenere il .jar compilato senza installare nulla (consigliato)

Questo repo include già `.github/workflows/build.yml`: una GitHub Action che compila il jar al posto
tuo sui server di GitHub (che hanno accesso pieno a internet, a differenza del mio sandbox).

1. Estrai lo zip dentro il tuo repository `aura-visuals` (o copia i file se il repo ha già una
   struttura), poi:
   ```
   git add .
   git commit -m "Add Aura Visuals mod"
   git push
   ```
2. Vai su GitHub → tab **Actions** del repo → apri l'ultima run ("Build mod jar") → aspetta il
   segno di spunta verde.
3. Scarica l'artifact **aura-visuals-jar** dalla pagina della run: dentro c'è il `.jar` pronto da
   mettere nella cartella `mods` di Minecraft.
4. (Opzionale) Se vuoi che il jar venga anche allegato automaticamente a una Release, crea e pusha
   un tag tipo `v1.0.0`: `git tag v1.0.0 && git push origin v1.0.0`.

## Struttura del progetto

```
auravisuals-mod/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── LICENSE
└── src/
    ├── main/resources/
    │   ├── fabric.mod.json
    │   └── assets/auravisuals/lang/{en_us,it_it}.json
    └── client/java/com/auravisuals/
        ├── AuraVisualsClient.java      (entrypoint, keybind, hook agli eventi)
        ├── config/
        │   ├── ModConfig.java         (persistenza JSON)
        │   └── ConfigScreen.java      (schermata del menu)
        └── particle/
            ├── FloatingParticle.java
            ├── ParticleManager.java   (simulazione/spawning)
            └── ParticleRenderer.java  (rendering: quad "billboard" con blending additivo)
```

Il rendering non usa un vero shader pack: disegna quad colorati sempre rivolti verso la camera con
blending additivo (`SRC_ALPHA, ONE`), che dà l'effetto di bagliore/glow tipico degli "shader" di
mod come questa, senza bisogno di GLSL custom o Iris/Optifine.

## Come compilare

**Importante:** in questo ambiente sandbox non ho accesso di rete ai server Maven di Fabric/Gradle,
quindi non ho potuto lanciare `gradle build` qui per verificarlo end-to-end. Il codice segue le API
standard di Fabric 1.21.1 (Yarn mappings), ma ti conviene seguire uno di questi due percorsi per
partire con il piede giusto:

### Opzione A (consigliata) — parti dal template ufficiale
1. Vai su https://fabricmc.net/develop/template/ e genera un template per **Minecraft 1.21.1**,
   **Fabric API** incluso, **solo client** (mod id: `auravisuals`, package: `com.auravisuals`).
   Questo ti dà `gradlew`/`gradlew.bat` e le versioni esatte e aggiornate di loom/loader/yarn.
2. Sostituisci/aggiungi dentro il template generato i file che trovi in questo archivio:
   - `src/client/java/com/auravisuals/...` → tutte le classi
   - `src/main/resources/fabric.mod.json` → sovrascrivi il loro
   - `src/main/resources/assets/auravisuals/lang/...` → aggiungi
3. Apri il progetto in IntelliJ IDEA (con plugin "Minecraft Development" opzionale ma utile) oppure
   lancia da terminale:
   ```
   ./gradlew build
   ```
4. Il `.jar` compilato lo trovi in `build/libs/auravisuals-1.0.0.jar`.

### Opzione B — usa direttamente questo progetto
Se hai Gradle 8.x installato globalmente:
```
cd auravisuals-mod
gradle wrapper --gradle-version 8.8   # genera gradlew, serve una volta sola e richiede internet
./gradlew build
```
Se `gradle build` si lamenta della versione del plugin `fabric-loom` (riga 2 di `build.gradle`) o
delle versioni in `gradle.properties` (yarn_mappings, loader_version, fabric_version), controlla i
valori più recenti su https://fabricmc.net/develop e aggiornali — Fabric rilascia build nuove spesso
e i numeri esatti possono essere cambiati rispetto a quando ho scritto questo codice.

## Come installare la mod compilata

1. Installa [Fabric Loader](https://fabricmc.net/use/) per Minecraft 1.21.1.
2. Scarica [Fabric API](https://modrinth.com/mod/fabric-api) per 1.21.1 e mettila nella cartella `mods`.
3. Copia il `.jar` compilato di Aura Visuals nella stessa cartella `mods`.
4. Avvia il profilo Fabric da launcher ufficiale (o Prism/MultiMC).

## Personalizzare oltre al menu

- `ModConfig.java` ha i valori di default (colori, densità, velocità) se vuoi cambiarli via codice.
- `ParticleManager.BASE_AMBIENT_TARGET` / `BASE_AURA_TARGET` e `AMBIENT_RADIUS` controllano quante
  particelle e su che raggio, come base moltiplicata dalla densità impostata nel menu.
- `ParticleRenderer` è il punto in cui potresti sostituire i quad con texture (invece dei quad a
  tinta unita) se vuoi un effetto ancora più "shader", aggiungendo uno sprite in
  `assets/auravisuals/textures/particle/...` e usando `RenderSystem.setShaderTexture(0, ...)` con
  `GameRenderer.getPositionColorTexProgram()` al posto del program position-color.
