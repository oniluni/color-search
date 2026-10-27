package dev.oni.colorsearch.mixin;

import dev.oni.colorsearch.BlockColorProfiles;
import dev.oni.colorsearch.ColorFamily;
import dev.oni.colorsearch.Release1211Items;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative search tab: appends blocks of the queried color after the vanilla name matches, and
 * keeps only items that existed in Minecraft 1.21.11.
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
	@Shadow
	private EditBox searchBox;

	@Inject(method = "refreshSearchResults", at = @At("TAIL"))
	private void colorsearch$refineSearchResults(CallbackInfo ci) {
		CreativeModeInventoryScreen.ItemPickerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
		ColorFamily.parse(searchBox.getValue()).ifPresent(query -> {
			Set<Item> shown = new HashSet<>();
			for (ItemStack stack : menu.items) {
				shown.add(stack.getItem());
			}
			for (ItemStack stack : BlockColorProfiles.matching(CreativeModeTabs.searchTab().getDisplayItems(), query)) {
				if (shown.add(stack.getItem())) {
					menu.items.add(stack);
				}
			}
		});
		menu.items.removeIf(stack -> !Release1211Items.contains(stack));
		// Vanilla already scrolled to the top before our changes; redo it so the slots show them.
		menu.scrollTo(0);
	}
}
