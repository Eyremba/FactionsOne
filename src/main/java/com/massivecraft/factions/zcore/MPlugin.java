package com.massivecraft.factions.zcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Patch;
import com.massivecraft.factions.zcore.persist.EM;
import com.massivecraft.factions.zcore.persist.SaveTask;
import com.massivecraft.factions.zcore.util.LibLoader;
import com.massivecraft.factions.zcore.util.PermUtil;
import com.massivecraft.factions.zcore.util.Persist;
import com.massivecraft.factions.zcore.util.TextUtil;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class MPlugin extends JavaPlugin {
    // Some utils

    public Persist persist;
    public TextUtil txt;
    public LibLoader lib;
    public PermUtil perm;

    // Persist related
    public Gson gson;
    private Integer saveTask = null;
    private boolean autoSave = true;
    protected boolean loadSuccessful = false;

    public boolean getAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean val) {
        autoSave = val;
    }

    // Listeners
    public MPluginSecretPlayerListener mPluginSecretPlayerListener;

    // -------------------------------------------- //
    // ENABLE
    // -------------------------------------------- //
    private long timeEnableStart;

    public boolean preEnable() {
        log("=== ENABLE START ===");
        timeEnableStart = System.currentTimeMillis();

        // Ensure basefolder exists!
        getDataFolder().mkdirs();

        // Create Utility Instances
        perm = new PermUtil(this);
        persist = new Persist(this);
        lib = new LibLoader(this);

        // GSON 2.1 is now embedded in CraftBukkit, used by the auto-updater:
        // https://github.com/Bukkit/CraftBukkit/commit/0ed1d1fdbb1e0bc09a70bc7bfdf40c1de8411665
        // if ( ! lib.require("gson.jar",
        // "http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar"))
        // return false;
        gson = getGsonBuilder().create();

        txt = new TextUtil();
        initTXT();

        // Create and register listeners
        mPluginSecretPlayerListener = new MPluginSecretPlayerListener(this);

        // Register recurring tasks
        if (saveTask == null && Conf.saveToFileEveryXMinutes > 0.0) {
            long saveTicks = (long) (20 * 60 * Conf.saveToFileEveryXMinutes); // Approximately
            // every
            // 30
            // min
            // by
            // default
            saveTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new SaveTask(this), saveTicks, saveTicks);
        }

        loadSuccessful = true;
        return true;
    }

    public void postEnable() {
        log("=== ENABLE DONE (Took " + (System.currentTimeMillis() - timeEnableStart) + "ms) ===");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            getServer().getScheduler().cancelTask(saveTask);
            saveTask = null;
        }
        // only save data if plugin actually loaded successfully
        if (loadSuccessful) {
            EM.saveAllToDisc();
        }
        log("Disabled");
    }

    public void suicide() {
        log("Now I suicide!");
        getServer().getPluginManager().disablePlugin(this);
    }

    // -------------------------------------------- //
    // Some inits...
    // You are supposed to override these in the plugin if you aren't satisfied
    // with the defaults
    // The goal is that you always will be satisfied though.
    // -------------------------------------------- //
    public GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE);
    }

    // -------------------------------------------- //
    // LANG AND TAGS
    // -------------------------------------------- //
    // These are not supposed to be used directly.
    // They are loaded and used through the TextUtil instance for the plugin.
    public Map<String, String> rawTags = new LinkedHashMap<>();

    public void addRawTags() {
        rawTags.put("l", "<green>"); // logo
        rawTags.put("a", "<gold>"); // art
        rawTags.put("n", "<silver>"); // notice
        rawTags.put("i", "<yellow>"); // info
        rawTags.put("g", "<lime>"); // good
        rawTags.put("b", "<rose>"); // bad
        rawTags.put("h", "<pink>"); // highligh
        rawTags.put("c", "<aqua>"); // command
        rawTags.put("p", "<teal>"); // parameter
    }

    public void initTXT() {
        addRawTags();

        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        Map<String, String> tagsFromFile = persist.load(type, "tags");
        if (tagsFromFile != null) {
            rawTags.putAll(tagsFromFile);
        }
        persist.save(rawTags, "tags");

        for (Entry<String, String> rawTag : rawTags.entrySet()) {
            txt.tags.put(rawTag.getKey(), TextUtil.parseColor(rawTag.getValue()));
        }
    }

    // -------------------------------------------- //
    // HOOKS
    // -------------------------------------------- //
    public void preAutoSave() {

    }

    public void postAutoSave() {

    }

    // -------------------------------------------- //
    // LOGGING
    // -------------------------------------------- //
    public void log(Object msg) {
        log(Level.INFO, msg);
    }

    public void log(String str, Object... args) {
        log(Level.INFO, txt.parse(str, args));
    }

    public void log(Level level, String str, Object... args) {
        log(level, txt.parse(str, args));
    }

    public void log(Level level, Object msg) {
        Bukkit.getLogger().log(level, "[" + Patch.getFullName() + "] " + msg);
    }

}
