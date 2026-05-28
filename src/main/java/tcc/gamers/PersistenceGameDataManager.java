package tcc.gamers;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.StorageFolder;
import tcc.gamers.util.store.YamlStore;

public class PersistenceGameDataManager {

    private static final String CLOUD_KARMA_KEY = "cloud-karma";
    private static final String TREE_KARMA_KEY  = "tree-karma";

    private final TCCPlugin plugin;

    private final YamlStore cloudStore;
    private final YamlStore treeStore;

    public PersistenceGameDataManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.cloudStore = new YamlStore(plugin, StorageFolder.CLOUD_DATA.getFolder(plugin));
        this.treeStore = new YamlStore(plugin, StorageFolder.TREE_DATA.getFolder(plugin));
    }

    public void reload() {
        cloudStore.reload();
        treeStore.reload();
    }

    public int getCloudKarma(){
        if(cloudStore.has(CLOUD_KARMA_KEY)){
            return cloudStore.readOrDefault(CLOUD_KARMA_KEY, 0);
        } else {
            cloudStore.write(CLOUD_KARMA_KEY, 0);
            cloudStore.saveAsync(); //save onto file
            return 0;
        }
    }
    public int getTreeKarma(){
        if(treeStore.has(TREE_KARMA_KEY)){
            return treeStore.readOrDefault(TREE_KARMA_KEY, 0);
        } else {
            treeStore.write(TREE_KARMA_KEY, 0);
            treeStore.saveAsync(); //save onto file
            return 0;
        }
    }

    public void setCloudKarma(int cloudKarma){
        cloudStore.write(CLOUD_KARMA_KEY, cloudKarma);
        cloudStore.saveAsync();
    }

    public void setTreeKarma(int treeKarma){
        treeStore.write(TREE_KARMA_KEY, treeKarma);
        treeStore.saveAsync();
    }
}
