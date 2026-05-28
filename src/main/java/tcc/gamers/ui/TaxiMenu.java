
package tcc.gamers.ui;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.democracycraft.democracyLib.api.dialog.Dialog;
import net.democracycraft.democracyLib.api.dialog.DialogBody;
import net.democracycraft.democracyLib.api.dialog.DialogButton;
import net.democracycraft.democracyLib.api.dialog.factory.DialogFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.nms.horses.TaxiSpawner;

import java.util.ArrayList;
import java.util.List;

@Dialog
@SuppressWarnings({
		"unused"
})
public class TaxiMenu {

	private final TCCPlugin plugin;

	public TaxiMenu(@NotNull TCCPlugin plugin) {
		this.plugin = plugin;
	}

	@DialogBody(
			id = "taxiMenuBody",
			order = 0
	)
	public io.papermc.paper.registry.data.dialog.body.DialogBody getBody(){
		return io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(
				Component.text("Donde te gustaría ir?",
						NamedTextColor.GOLD,
						TextDecoration.BOLD)
		);
	}

	@DialogButton(
			id = "taxiDestination",
			order = 0
	)
	public List<ActionButton> getTaxiDestinations(){
		var actions = new ArrayList<ActionButton>();

		for (String path : plugin.getPathManager().listPathIds()) {
			actions.add(
					ActionButton
							.builder(Component.text(path, NamedTextColor.GREEN))
							.action(DialogAction.customClick((_response, audience) -> {
										if(audience instanceof Player player){
											var loadedPath = plugin.getPathManager().loadPath(path, player.getLocation());
											if (loadedPath != null) {
												TaxiSpawner.spawnTaxi(plugin, loadedPath, player.getLocation(), player);
											} else {
												player.sendMessage(Component.text("Path not found", NamedTextColor.RED));
											}
										}
									}, ClickCallback.Options.builder().build()))
							.build()
			);
		}

		return actions;
	}

	public static void open(@NotNull TCCPlugin plugin, @NotNull Player player){
		var menu = new TaxiMenu(plugin);
		var dialog = DialogFactory.create(menu);

		if(dialog != null){
			player.showDialog(dialog);
		}
	}

}

