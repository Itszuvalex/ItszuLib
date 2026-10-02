package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.ItszuLib
import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Server to client: new values for some of a [MenuCore]'s [MenuSync]s. Port of ItszuLib 1.12.2's `MessageSync`, batched:
 * one payload carries every sync that changed in a tick.
 *
 * Each entry holds its sync's value already encoded with that sync's codec, because only the menu knows the codecs.
 * The client applies it only to the open menu with the same [containerId] (1.12.2 matched on the GUI type, so a sync
 * meant for a closed menu could land in a newly opened one of the same type).
 */
data class MenuSyncPayload(val containerId: Int, val entries: List<Entry>) : CustomPacketPayload {
    data class Entry(val index: Int, val data: ByteArray) {
        override fun equals(other: Any?): Boolean = other is Entry && other.index == index && other.data.contentEquals(data)

        override fun hashCode(): Int = index * 31 + data.contentHashCode()
    }

    override fun type(): CustomPacketPayload.Type<MenuSyncPayload> = TYPE

    companion object {
        @JvmField
        val TYPE: CustomPacketPayload.Type<MenuSyncPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(ItszuLib.ID, "menu_sync"))

        private val ENTRY_CODEC: StreamCodec<RegistryFriendlyByteBuf, Entry> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Entry::index,
            ByteBufCodecs.BYTE_ARRAY, Entry::data,
            ::Entry,
        )

        @JvmField
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MenuSyncPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuSyncPayload::containerId,
            ENTRY_CODEC.apply(ByteBufCodecs.list()), MenuSyncPayload::entries,
            ::MenuSyncPayload,
        )

        /**
         * Client side, on the main thread.
         */
        @JvmStatic
        fun handle(payload: MenuSyncPayload, context: IPayloadContext) {
            val player = context.player()
            (player.containerMenu as? MenuCore)?.applySyncPayload(payload, player.registryAccess())
        }

        /**
         * A buffer for encoding or decoding one sync's value.
         */
        @JvmStatic
        fun buffer(registries: RegistryAccess, data: ByteArray? = null): RegistryFriendlyByteBuf =
            RegistryFriendlyByteBuf(if (data == null) Unpooled.buffer() else Unpooled.wrappedBuffer(data), registries)
    }
}

/**
 * Client to server: a button or similar control in a [MenuCore]'s screen was used. [action] and [data] mean whatever
 * the menu's [MenuCore.dispatchAction] makes of them (e.g. "cycle the storage on face [data] forward").
 *
 * Replaces 1.12.2's per-control messages, which named a block by position and were applied without checking
 * that the sender had that block's menu open. This payload only reaches the sender's open menu, and only while
 * [AbstractContainerMenu.stillValid].
 */
data class MenuActionPayload(val containerId: Int, val action: Int, val data: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<MenuActionPayload> = TYPE

    companion object {
        @JvmField
        val TYPE: CustomPacketPayload.Type<MenuActionPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(ItszuLib.ID, "menu_action"))

        @JvmField
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuActionPayload::containerId,
            ByteBufCodecs.VAR_INT, MenuActionPayload::action,
            ByteBufCodecs.VAR_INT, MenuActionPayload::data,
            ::MenuActionPayload,
        )

        /**
         * Server side, on the main thread.
         */
        @JvmStatic
        fun handle(payload: MenuActionPayload, context: IPayloadContext) {
            dispatch(context.player().containerMenu, payload, context.player())
        }

        /**
         * Runs [payload] on [menu] if it is the menu the payload names and [player] may still use it.
         *
         * @return True if the menu handled it.
         */
        @JvmStatic
        fun dispatch(menu: AbstractContainerMenu?, payload: MenuActionPayload, player: Player): Boolean {
            if (menu !is MenuCore || menu.containerId != payload.containerId || !menu.stillValid(player)) return false
            return menu.dispatchAction(player, payload.action, payload.data)
        }
    }
}
