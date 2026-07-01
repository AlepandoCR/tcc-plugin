package tcc.gamers.config;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import net.democracycraft.democracyLib.api.dialog.factory.DialogConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.MiniMessageUtil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Configurable(name = "GeneratedParticleMenuConfig")
public class ParticleMenuConfig implements DialogConfig, TCCConfig {

    private final MiniMessageUtil miniMessageUtil;

    @ConfigValue(fieldName = "menu-main-body", comment = "MiniMessage string used as the main body of the particle customization menu.")
    private final String menuMainBody;

    @ConfigValue(fieldName = "wiki-url", comment = "URL for players to lookup particles")
    private final String wikiUrl;

    @ConfigValue(fieldName = "message-to-send-players-to-wiki", comment = "MiniMessage string used as the body to guide players to the particle wiki")
    private final String wikiMessage;

    @ConfigValue(fieldName = "particle-max-range", comment = "max range for players to set their particles")
    private final double particleMaxRange;

    @ConfigValue(fieldName = "particle-min-range", comment = "min range for players to set their particles")
    private final double particleMinRange;

    @ConfigValue(fieldName = "particle-max", comment = "max particles a player can set")
    private final double particleMax;

    @ConfigValue(fieldName = "particle-min", comment = "min particles a player can set")
    private final double particleMin;


    public ParticleMenuConfig(
            @NotNull String menuMainBody,
            @NotNull String wikiUrl,
            @NotNull String wikiMessage,
            double particleMaxRange,
            double particleMinRange,
            double particleMax,
            double particleMin
    ) {
        this.menuMainBody = menuMainBody;
        this.wikiUrl = wikiUrl;
        this.wikiMessage = wikiMessage;
        this.particleMaxRange = particleMaxRange;
        this.particleMinRange = particleMinRange;
        this.particleMax = particleMax;
        this.particleMin = particleMin;
        this.miniMessageUtil = MiniMessageUtil.INSTANCE;
    }

    public ParticleMenuConfig(@NotNull GeneratedParticleMenuConfig config){
        this(
                config.getMenuMainBody(),
                config.getWikiUrl(),
                config.getWikiMessage(),
                config.getParticleMaxRange(),
                config.getParticleMinRange(),
                config.getParticleMax(),
                config.getParticleMin()
        );
    }

    public double getParticleMax() {
        return particleMax;
    }

    public double getParticleMaxRange() {
        return particleMaxRange;
    }

    public double getParticleMin() {
        return particleMin;
    }

    public double getParticleMinRange() {
        return particleMinRange;
    }

    public @NotNull Component getMenuMainBody() {
        return miniMessageUtil.parseOrPlain(menuMainBody);
    }

    public @NotNull Component getWikiMessage() {
        return miniMessageUtil.parseOrPlain(wikiMessage);
    }

    public @NotNull URL getWikiUrl() {
        try {
            var uri = new URI(wikiUrl);
            try {
                return uri.toURL();
            }catch (MalformedURLException e1){
                throw  new RuntimeException("Error parsing URI to URL in particle config menu: " + wikiUrl, e1);
            }
        }catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI syntax for wikiUrl in particle config menu: " + wikiUrl, e);
        }

    }

    @Override
    public @NotNull Component title() {
        return Component
                .text("Proyector de Particulas")
                .color(NamedTextColor.LIGHT_PURPLE)
                .shadowColor(
                        ShadowColor.shadowColor(
                                212,
                                157,
                                202,
                                0
                        )
                );
    }
}
