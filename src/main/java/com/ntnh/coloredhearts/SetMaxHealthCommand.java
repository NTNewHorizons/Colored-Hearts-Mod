package com.ntnh.coloredhearts;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

public class SetMaxHealthCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "coloredhearts";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/coloredhearts <amount>  — sets your max HP (e.g. 40 = 20 hearts)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // anyone can use it (test convenience)
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) throw new WrongUsageException(getCommandUsage(sender));

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        float amount;

        try {
            amount = Float.parseFloat(args[0]);
        } catch (NumberFormatException e) {
            throw new WrongUsageException("Amount must be a number, e.g. /coloredhearts 40");
        }

        if (amount < 2.0F) amount = 2.0F; // at least 1 heart
        if (amount > 1024.0F) amount = 1024.0F; // reasonable upper cap

        player.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(amount);

        // Clamp current HP so it doesn't exceed the new max
        if (player.getHealth() > amount) {
            player.setHealth(amount);
        }

        player.addChatMessage(
            new ChatComponentText("Max HP set to " + (int) amount + " (" + (int) (amount / 2) + " hearts)"));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }
}
