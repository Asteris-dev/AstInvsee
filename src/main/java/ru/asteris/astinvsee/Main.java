package ru.asteris.astinvsee;

import org.bukkit.plugin.java.JavaPlugin;
import ru.asteris.astinvsee.commands.AstInvseeCommand;
import ru.asteris.astinvsee.commands.InvseeCommand;
import ru.asteris.astinvsee.managers.InvseeManager;
import ru.asteris.astinvsee.managers.SaveManager;

public class Main extends JavaPlugin {

    private static Main instance;
    private InvseeManager invseeManager;
    private SaveManager saveManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        saveManager = new SaveManager();
        invseeManager = new InvseeManager();

        getServer().getPluginManager().registerEvents(invseeManager, this);
        getServer().getPluginManager().registerEvents(saveManager, this);

        new AstInvseeCommand();
        new InvseeCommand();

        invseeManager.startTask();
    }

    @Override
    public void onDisable() {
        if (invseeManager != null) {
            invseeManager.closeAll();
        }
        if (saveManager != null) {
            saveManager.closeAll();
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public InvseeManager getInvseeManager() {
        return invseeManager;
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }
}