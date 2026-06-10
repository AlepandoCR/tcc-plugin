package tcc.gamers.ai.spartan.model;

import org.jetbrains.annotations.NotNull;
import org.spartan.api.engine.SpartanModel;
import tcc.gamers.util.StorageFolder;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manager for handling Spartan model persistence operations.
 * Dedicated folder for managing horse model and other Spartan models.
 */
public class SpartanModelManager {

    private final String modelPath;

    private final SpartanModel<?> model;

    public SpartanModelManager(@NotNull SpartanModel<?> model, @NotNull String path) {
        this.model = model;
        modelPath = StorageFolder.SPARTAN_MODEL.getFolderName() + path;
    }

    /**
     * Save the model to the default horse model path.
     */
    public void saveModel() {
        saveModel(Paths.get(modelPath));
    }

    /**
     * Save the model to a specified path.
     *
     * @param path the path where the model should be saved
     */
    public void saveModel(@NotNull Path path) {
        model.saveModel(path);
    }

    /**
     * Load the model from the default horse model path.
     */
    public void loadModel() {
        Path path = Paths.get(modelPath);
        if(path.toFile().exists()) {
            loadModel(path);
        }
    }

    /**
     * Load the model from a specified path.
     *
     * @param path the path from which the model should be loaded
     */
    public void loadModel(@NotNull Path path) {
        model.loadModel(path);
    }

    /**
     * Get the default model path.
     *
     * @return the default model path
     */
    public @NotNull String getDefaultModelPath() {
        return modelPath;
    }

    /**
     * Close the model resources.
     */
    public void close() {
        model.close();
    }
}

