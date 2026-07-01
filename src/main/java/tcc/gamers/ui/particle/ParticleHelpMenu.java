package tcc.gamers.ui.particle;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.democracycraft.democracyLib.api.dialog.Dialog;
import net.democracycraft.democracyLib.api.dialog.DialogBody;
import net.democracycraft.democracyLib.api.dialog.DialogButton;
import net.democracycraft.democracyLib.api.dialog.factory.DialogFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.config.ParticleMenuConfig;

@Dialog
public class ParticleHelpMenu {

    private final @NotNull ParticleMenuConfig config;

    private final @NotNull ItemStack particleProjector;

    private final @NotNull TCCPlugin plugin;

    public ParticleHelpMenu(@NotNull ParticleMenuConfig config, @NotNull ItemStack particleProjector, @NotNull TCCPlugin plugin) {
        this.config = config;
        this.particleProjector = particleProjector;
        this.plugin = plugin;
    }

    @DialogBody(
            id = "wikiMessage",
            order = 0
    )
    public io.papermc.paper.registry.data.dialog.body.DialogBody getWikiMessage() {
        return io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(config.getWikiMessage());
    }

    @DialogButton(
            id = "wikiButton",
            order = 0
    )
    public ActionButton getWikiButton() { // fully hardcoded since it's just an acknowledge button

        return ActionButton.builder(
                Component.text("¡Wiki de Particulas!")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD)
        ).action(
                DialogAction
                        .staticAction(
                                ClickEvent.openUrl(
                                        config.getWikiUrl()
                                )
                        )
        ).width(200).build();
    }

    @DialogButton(
            id = "doneButton",
            order = 0
    )
    public ActionButton getDoneButton() { // fully hardcoded since it's just an acknowledge button

        return ActionButton.builder(
                Component.text("¡Listo!")
                        .color(NamedTextColor.GREEN)

        ).action(
                DialogAction
                        .customClick(
                                (response, audience) -> {
                                    if(audience instanceof Player player){
                                        player.closeDialog();
                                        player.showDialog(DialogFactory.create(new ParticleCustomizationMenu(config, plugin, particleProjector)));
                                    }
                                },
                                ClickCallback.Options.builder().build()
                        )
        ).width(200).build();
    }









}
