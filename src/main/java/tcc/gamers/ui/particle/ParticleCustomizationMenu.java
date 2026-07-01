package tcc.gamers.ui.particle;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.democracycraft.democracyLib.api.dialog.*;
import net.democracycraft.democracyLib.api.dialog.factory.DialogFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.config.ParticleMenuConfig;
import tcc.gamers.item.particle.ParticleProjectorDTO;
import tcc.gamers.item.particle.ParticleProjectorHelper;

@Dialog
public class ParticleCustomizationMenu {

    @DialogConfigProvider
    private final @NotNull ParticleMenuConfig config;

    private final @NotNull TCCPlugin plugin;

    private final @NotNull ItemStack particleProjector;

    public ParticleCustomizationMenu(@NotNull ParticleMenuConfig config, @NotNull TCCPlugin plugin, @NotNull ItemStack particleProjector) {
        this.config = config;
        this.plugin = plugin;
        this.particleProjector = particleProjector;
    }

    @DialogBody(
            id = "particleCustomizationMenuBody",
            order = 0
    )
    public io.papermc.paper.registry.data.dialog.body.DialogBody getBody() {
        return io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(config.getMenuMainBody());
    }

    @DialogBody(
            id = "particleProjectorItem",
            order = 1
    )
    public io.papermc.paper.registry.data.dialog.body.DialogBody getParticleProjectorItem() {
        return io.papermc.paper.registry.data.dialog.body.DialogBody
                .item(particleProjector)
                .height(18) // hardcoded bc tbh low values bellow 20 are the sweet spot for vanilla items
                .width(18) // since items do not scale with this on the dialog .-.
                .showTooltip(true)
                .build();
    }

    @DialogInput(
            id = "particleTypeInput",
            order = 0
    )
    public io.papermc.paper.registry.data.dialog.input.DialogInput getParticleTypeInput() {
        var particleType = ParticleProjectorHelper.getParticleName(this.particleProjector);
        return io.papermc.paper.registry.data.dialog.input.DialogInput.text(
                "particleTypeInput",
                300,
                Component.text("Introduce el nombre de la particula que quieres usar", NamedTextColor.GRAY),
                true,
                particleType,
                100,
                TextDialogInput.MultilineOptions.create(null ,null)
        );
    }

    @DialogInput(
            id = "particleAmount",
            order = 1
    )
    public io.papermc.paper.registry.data.dialog.input.DialogInput getParticleAmount() {
        float currentAmount = (float) ParticleProjectorHelper.getAmount(this.particleProjector);

        float initialValue = currentAmount > 0 ? currentAmount : 10.0f;

        return io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange(
                        "particleAmount",
                        Component.text("¿Cuántas partículas quieres?", NamedTextColor.GRAY),
                        (float)config.getParticleMin(),
                        (float)config.getParticleMax()
                ).width(300)
                .labelFormat("%s: %s")
                .initial(initialValue)
                .step(1.0f)
                .build();
    }

    @DialogInput(
            id = "particleRange",
            order = 2
    )
    public io.papermc.paper.registry.data.dialog.input.DialogInput getParticleRange() {
        float currentRange = (float) ParticleProjectorHelper.getRadius(this.particleProjector);

        currentRange = Math.round(currentRange * 10.0f) / 10.0f;

        float initialValue = currentRange > 0 ? currentRange : 2.0f;

        return io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange(
                        "particleRange",
                        Component.text("Dispersión de tus particulas", NamedTextColor.GRAY),
                        (float)config.getParticleMinRange(),
                        (float)config.getParticleMaxRange()
                )
                .width(300)
                .labelFormat("%s: %s")
                .initial(initialValue)
                .step(0.25f)
                .build();
    }

    @DialogButton(
            id = "wikiButton",
            order = 0
    )
    public ActionButton getWikiButton() { // fully hardcoded since it's just an acknowledge button

        return ActionButton.builder(
                Component.text("¡Ayuda!")
                        .color(NamedTextColor.LIGHT_PURPLE)

        ).action(
                DialogAction
                        .customClick(
                                (_, audience) -> {
                                    if(audience instanceof Player player){
                                        player.closeDialog();
                                        player.showDialog(DialogFactory.create(new ParticleHelpMenu(config, particleProjector, plugin)));
                                    }
                                },
                                ClickCallback.Options.builder().build()
                        )
        ).width(200).build();
    }

    @DialogButton(
            id = "doneButton",
            order = 0
    )
    public ActionButton getDoneButton() { // fully hardcoded since it's just an acknowledge button

        return ActionButton.builder(
                Component.text("Aplicar")
                        .color(NamedTextColor.GREEN)

        ).action(
                DialogAction
                        .customClick(
                                (response, audience) -> {
                                    if(audience instanceof Player player){
                                        player.closeDialog();
                                        String particleString = response.getText("particleTypeInput");
                                        var radius = response.getFloat("particleRange");
                                        var amount = response.getFloat("particleAmount");

                                        if (particleString == null && amount == null && radius == null) {
                                            player.sendMessage(Component.text("Entrada de particula incompleta, usando valores por defecto", NamedTextColor.YELLOW));
                                        }else if(particleString != null && amount != null && radius != null){
                                            onMenuSubmit(player, particleProjector, radius, amount.intValue(), particleString);
                                        }
                                    }
                                },
                                ClickCallback.Options.builder().build()
                        )
        ).width(200).build();
    }

    public void onMenuSubmit(@NotNull Player player, @NotNull ItemStack projectorItem, double newRange, int newAmount, @NotNull String newParticleName) {

        double cleanRange = Math.round(newRange * 10.0) / 10.0;

        ParticleProjectorHelper.updateSettings(
                projectorItem,
                cleanRange,
                newAmount,
                newParticleName
        );


        plugin.getParticleProjectorManager().checkPlayerEquipped(player);

        player.sendMessage(Component.text("¡Proyector actualizado con éxito!", NamedTextColor.GREEN));
    }
}
