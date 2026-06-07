package tcc.gamers;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.skript.SkriptMutable;

public class LiveGameDataManager {

    private final PersistenceGameDataManager persistenceGameDataManager;
    private final SkriptMutable<Integer> cloudsKarma;
    private final SkriptMutable<Integer> treeKarma;

    private final TCCPlugin plugin;

    public LiveGameDataManager(
            @NotNull PersistenceGameDataManager persistenceGameDataManager,
            @NotNull TCCPlugin plugin
    ) {
        this.persistenceGameDataManager = persistenceGameDataManager;

        this.cloudsKarma = new SkriptMutable<>(
                persistenceGameDataManager.getCloudKarma(),
                persistenceGameDataManager::setCloudKarma
        );
        this.treeKarma = new SkriptMutable<>(
                persistenceGameDataManager.getTreeKarma(),
                persistenceGameDataManager::setTreeKarma
        );


        this.plugin = plugin;

        registerMutables(plugin);
    }

    private void registerMutables(@NotNull TCCPlugin plugin) {
        plugin.getSkriptMutableRegistry().registerMutable("cloudsKarma", this.cloudsKarma);
        plugin.getSkriptMutableRegistry().registerMutable("treeKarma", this.treeKarma);
    }

    public void unregisterMutables() {
        plugin.getSkriptMutableRegistry().unregisterMutable("cloudsKarma");
        plugin.getSkriptMutableRegistry().unregisterMutable("treeKarma");
    }

    public void reload(){
        this.cloudsKarma.set(persistenceGameDataManager.getCloudKarma());
        this.treeKarma.set(persistenceGameDataManager.getTreeKarma());
    }

    public int getCloudsKarma() {
        return cloudsKarma.get();
    }

    public int getTreeKarma() {
        return treeKarma.get();
    }

    public @NotNull SkriptMutable<Integer> getCloudsKarmaMutable(){
        return cloudsKarma;
    }

    public @NotNull SkriptMutable<Integer> getTreeKarmaMutable(){
        return treeKarma;
    }
}
