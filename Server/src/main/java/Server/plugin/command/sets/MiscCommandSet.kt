package plugin.command.sets

import core.cache.def.impl.ItemDefinition
import core.game.component.Component
import core.game.node.entity.player.info.Rights
import core.game.system.SystemLogger
import core.game.world.GameWorld
import core.game.world.map.RegionManager
import core.game.world.map.build.DynamicRegion
import core.game.world.repository.Repository
import core.plugin.InitializablePlugin
import plugin.command.Command
import plugin.command.CommandMapping
import plugin.command.CommandSet
import plugin.creditshop.CreditShop
import plugin.ge.GEOfferDispatch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@InitializablePlugin
class MiscCommandSet : CommandSet(Command.Privilege.ADMIN){
    override fun defineCommands() {

        /**
         * Toggles debug mode
         */
        define("debug",Command.Privilege.STANDARD){ player, _ ->
            player.toggleDebug()
        }

        /**
         * Prints player's current location
         */
        define("loc",Command.Privilege.STANDARD){player,_->
            val l = player.location
            val r = player.viewport.region
            player.packetDispatch.sendMessage("Absolute: " + l + ", regional: [" + l.localX + ", " + l.localY + "], chunk: [" + l.chunkOffsetX + ", " + l.chunkOffsetY + "], flag: [" + RegionManager.isTeleportPermitted(l) + ", " + RegionManager.getClippingFlag(l) + ", " + RegionManager.isLandscape(l) + "].")
            player.packetDispatch.sendMessage("Region: [id=" + l.regionId + ", active=" + r.isActive + ", instanced=" + (r is DynamicRegion) + "], obj=" + RegionManager.getObject(l) + ".")
            player.packetDispatch.sendMessage("Object: " + RegionManager.getObject(l) + ".")
            SystemLogger.log("Viewport: " + l.getSceneX(player.playerFlags.lastSceneGraph) + "," + l.getSceneY(player.playerFlags.lastSceneGraph))
            val loc = "Location.create(" + l.x + ", " + l.y + ", " + l.z + ")"
            SystemLogger.log(loc + "; " + player.playerFlags.lastSceneGraph + ", " + l.localX + ", " + l.localY)
            val stringSelection = StringSelection(loc)
            val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
            clpbrd.setContents(stringSelection, null)
        }

        /**
         * Empty a player's inventory
         * ADMIN only (for obvious reasons)
         */
        define("empty"){player,_->
            player.inventory.clear()
            player.inventory.refresh()
        }

        /**
         * Announces a message in chat (NEWS)
         */
        define("announce"){_,args ->
            val message = args.slice(1 until args.size).joinToString(" ")
            Repository.sendNews(message)
        }

        /**
         * Lists the players currently online
         */
        define("players",Command.Privilege.STANDARD){ player, _ ->
            val rights = player.rights.ordinal
            if (player!!.interfaceManager.isOpened && player.interfaceManager.opened.id != 275 || player.locks.isMovementLocked || player.locks.isTeleportLocked) {
                player.sendMessage("Please finish what you're doing first.")
                return@define
            }
            player.interfaceManager.open(Component(275))
            var i = 0
            while (i < 257) {
                player.packetDispatch.sendString("", 275, i)
                i++
            }
            val red = "<col=8A0808>"
            player.packetDispatch.sendString("<col=8A0808>" + "Players" + "</col>", 275, 2)
            val builder = StringBuilder("<br>")
            var count = 0
            for (p in Repository.getPlayers()) {
                if (count > 45) {
                    builder.append("<br>Max amount we can show on this interface.")
                    break
                }
                if (p == null || p.isAdmin && GameWorld.settings?.isDevMode != true && !player.isAdmin || p.isArtificial) {
                    continue
                }
                builder.append(red + "<img=" + (Rights.getChatIcon(p) - 1) + ">" + p.username + if(rights > 0) " [ip=" + p.details.ipAddress + ", name=" + p.details.compName + "]<br><br>" else "<br><br>")
                count++
            }
            player.packetDispatch.sendString(builder.toString(), 275, 11)
        }
        /**
         * ===================================================================================
         */


        /**
         * Opens the credit/voting shop
         */
        define("shop",Command.Privilege.STANDARD){ player, _ ->
            CreditShop().open(player)
        }

        /**
         * Shows the player a list of currently active GE sell offers
         */
        define("ge",Command.Privilege.STANDARD) { player, _ ->
            val offers = HashMap<Int,Int>()
            for(offer in GEOfferDispatch.offerMapping.values){
                val item = offer.itemId
                val amount = offer.amount - offer.completedAmount
                if(offers[item] == null){
                    offers[item] = amount
                } else {
                    offers[item] = offers[item]!!.plus(amount)
                }
            }
            for (i in 0..310) {
                player!!.packetDispatch.sendString("", 275, i)
            }
            var lineId = 11
            player!!.packetDispatch.sendString("Active Sell Offers", 275, 2)
            var counter = 0
            for(i in 0..299) {
                val offer = offers.entries.elementAtOrNull(i)
                if (offer != null)
                    player.packetDispatch.sendString("${ItemDefinition.forId(offer.key).name} x${offer.value}", 275, lineId++)
                else
                    player.packetDispatch.sendString("", 275, lineId++)
            }
            player.interfaceManager.open(Component(275))
        }
        /**
         * ==================================================================================
         */


        /**
         * List all commands
         */
        define("commands"){player,_ ->
            for (i in 0..310) {
                player.packetDispatch.sendString("", 275, i)
            }
            var lineid = 11
            player.packetDispatch.sendString("Commands",275,2)
            for(line in CommandMapping.getNames())
                player.packetDispatch.sendString(line,275,lineid++)
            player.interfaceManager.open(Component(275))
        }
    }
}