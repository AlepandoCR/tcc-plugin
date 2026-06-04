package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.area.Area;

import java.util.Optional;

@Configurable(name = "GeneratedAreaDto")
public class AreaDto extends LocationDto{

    @ConfigValue(fieldName = "radius-x")
    private final double radiusX;

    @ConfigValue(fieldName = "radius-y")
    private final double radiusY;

    @ConfigValue(fieldName = "radius-z")
    private final double radiusZ;

    @ConfigValue(fieldName = "area-name")
    private final String areaId;

    public AreaDto(
            double xCoord,
            double yCoord,
            double zCoord,
            @NotNull String worldName,
            double radiusX,
            double radiusY,
            double radiusZ,
            @NotNull String areaId
    ) {
        super(
                xCoord,
                yCoord,
                zCoord,
                worldName
        );
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.areaId = areaId;
    }

    public AreaDto(
            @NotNull GeneratedAreaDto generatedAreaDto
    ){
        this(
                generatedAreaDto.getXCoord(),
                generatedAreaDto.getYCoord(),
                generatedAreaDto.getZCoord(),
                generatedAreaDto.getWorldName(),
                generatedAreaDto.getRadiusX(),
                generatedAreaDto.getRadiusY(),
                generatedAreaDto.getRadiusZ(),
                generatedAreaDto.getAreaId()
        );
    }

    @Contract("-> new")
    public @NotNull Optional<Area> buildArea(){

        var maybeLocation = toLocation();

        if(maybeLocation.isPresent()){ // not lambda bc of return
            var location = maybeLocation.get();

            return Optional.of(
                    new Area(
                            location,
                            areaId,
                            radiusX,
                            radiusY,
                            radiusZ
                    )
            );
        }

        return Optional.empty();
    }

    public @NotNull String getAreaId() {
        return areaId;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusY() {
        return radiusY;
    }

    public double getRadiusZ() {
        return radiusZ;
    }
}
