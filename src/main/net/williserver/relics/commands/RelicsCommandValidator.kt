package net.williserver.relics.commands

import net.williserver.relics.integration.item.RelicItemStackIntegrator
import net.williserver.relics.integration.messaging.sendErrorMessage
import net.williserver.relics.model.Relic
import net.williserver.relics.model.RelicRarity
import net.williserver.relics.model.RelicSet
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Functions to validate a single invocation of a command.
 * Wrap around a sender.
 *
 * @param s Sender invoking command.
 * @param relics Set of relics for this session.
 * @param itemIntegrator Utility for checking if an item is a relic.
 */
class RelicsCommandValidator(private val s: CommandSender,
                             private val relics: RelicSet,
                             private val itemIntegrator: RelicItemStackIntegrator) {
    /**
     * Determine whether a command sender is a player.
     * If not, message them a warning.
     *
     * @return whether the sender is a player.
     */
    fun assertValidPlayer() =
        if (s !is Player) {
            sendErrorMessage(s, "This command can only be run by players.")
            false
        } else true

    /**
     * Determine whether a player is holding a single item in their hand.
     * If not, message them a warning.
     *
     * @return whether the sender is holding a single item.
     * @throws IllegalArgumentException if the sender is not a player.
     */
    fun assertSingleItemHeld() =
        if (s !is Player) {
            throw IllegalArgumentException("This command can only be run by players.")
        } else if (s.inventory.itemInMainHand.amount != 1) {
            sendErrorMessage(s, "This command can only be run when holding a single item in your hand.")
            false
        } else true

    /**
     * Validates that the item held by the sender is non-stackable.
     * If it is stackable, send them a warning.
     *
     * @return whether the sender is holding a non-stackable item.
     * @throws IllegalArgumentException if the sender is not a player.
     */
    fun assertHeldItemValidMaterial() =
        if (s !is Player) {
            throw IllegalArgumentException("This function should only be run by players -- this should have been checked earlier!")
        } else if (!Relic.validMaterial(s.inventory.itemInMainHand.type)) {
            sendErrorMessage(s, "This item cannot be registered as a relic.")
            false
        } else true

    /**
     * Asserts that the item held by the player is not already registered as a relic.
     * Uses deep equivalence, checking item metadata -- this means that items that were once relics cannot be re-registered!
     * However, it provides better protection against scamming. Lets you confirm the real relic.
     *
     * @return `true` if the held item is not already registered as a relic, otherwise `false`.
     * @throws IllegalArgumentException if the sender is not a player.
     */
    fun assertHeldItemNotAlreadyRelic() =
        if (s !is Player) {
            throw IllegalArgumentException("This function should only be run by players -- this should have been checked earlier!")
        } else if (itemIntegrator.isRelic(s.inventory.itemInMainHand)) {
            sendErrorMessage(s, "This item was already registered as a relic.")
            false
        } else true

    /**
     * Asserts that the item held by the player is already registered as a relic.
     * Uses deep equivalence, checking item metadata -- this means that items that were once relics cannot be re-registered!
     * However, it provides better protection against scamming. Lets you confirm the real relic.
     *
     * @return `true` if the held item is registered as a relic, otherwise `false`.
     * @throws IllegalArgumentException if the sender is not a player.
     */
    fun assertHeldItemIsRelic() =
        if (s !is Player) {
            throw IllegalArgumentException("This function should only be run by players -- this should have been checked earlier!")
        } else if (!itemIntegrator.isRelic(s.inventory.itemInMainHand)) {
            sendErrorMessage(s, "This item is not a relic.")
            false
        } else true

    /**
     * Assert that the name provided is valid. If not, message sender a warning.
     * @param name Name to check for validity.
     * @return whether the name is valid
     */
    fun assertValidName(name: String) =
        if (!Relic.validName(name)) {
            sendErrorMessage(s, "Invalid relic name: $name.")
            false
        } else true

    /**
     * Assert that the name provided refers to an existing relic. If not, message sender a warning.
     * @param name Name to check for corresponding relic.
     * @return whether the name refers to an existing relic.
     */
    fun assertNameRefersToRelic(name: String) =
        if (relics.relicNamed(name) == null) {
            sendErrorMessage(s, "There is no relic named \"$name\".")
            false
        } else true

    /**
     * Asserts that the provided name is unique among the existing relics in the given relic set.
     * If a relic with the same name already exists, an error message will be sent.
     *
     * @param name The name of the relic to validate for uniqueness.
     * @return whether the name is unique.
     */
    fun assertUniqueName(name: String) =
        if (relics.relicNamed(name) != null) {
            sendErrorMessage(s, "There is already a relic named \"$name\".")
            false
        } else true

    /**
     * Asserts that the provided rarity name is valid. If the rarity name is invalid,
     * it sends an error message indicating the valid options.
     *
     * @param rarityName The name of the rarity to validate.
     * @return `true` if the provided rarity name is valid, otherwise `false`.
     */
    fun assertRarityValid(rarityName: String) =
        if (RelicRarity.rarityFromName(rarityName) == null) {
            sendErrorMessage(s, "Invalid rarity: $rarityName. Choose common, rare, epic, legendary, or unique.")
            false
        } else true
}