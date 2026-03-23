package ru.asteris;

import org.bukkit.plugin.java.JavaPlugin;
import ru.asteris.commands.AstInvseeCommand;
import ru.asteris.commands.InvseeCommand;
import ru.asteris.managers.InvseeManager;
import ru.asteris.managers.SaveManager;

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

        AstInvseeCommand astCmd = new AstInvseeCommand();
        getCommand("astinvsee").setExecutor(astCmd);
        getCommand("astinvsee").setTabCompleter(astCmd);

        InvseeCommand invCmd = new InvseeCommand();
        getCommand("invsee").setExecutor(invCmd);
        getCommand("invsee").setTabCompleter(invCmd);

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