package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class LocationDto {

    @ConfigValue(fieldName = "x-coord")
    private final double xCoord;

    @ConfigValue(fieldName = "y-coord")
    private final double yCoord;

    @ConfigValue(fieldName = "z-coord")
    private final double zCoord;

    @ConfigValue(fieldName = "world")
    private final String worldName;

    public LocationDto(
            double xCoord,
            double yCoord,
            double zCoord,
            @NotNull String worldName
    ) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.zCoord = zCoord;
        this.worldName = worldName;
    }

    public double getX() {
        return xCoord;
    }

    public double getY(){
        return yCoord;
    }

    public double getZ(){
        return zCoord;
    }

    public @NotNull String getWorldName(){
        return worldName;
    }

    public @NotNull Optional<Location> toLocation(){

        var world = Bukkit.getWorld(worldName);

        if(world == null){
            return Optional.empty();
        }

        return Optional.of(
                new Location(
                        world,
                        xCoord,
                        yCoord,
                        zCoord
                )
        );

    }
}
