package com.ruinscraft.powder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.ruinscraft.powder.objects.ChangedParticle;
import com.ruinscraft.powder.objects.Dust;
import com.ruinscraft.powder.objects.ParticleName;
import com.ruinscraft.powder.objects.PowderMap;
import com.ruinscraft.powder.objects.PowderTask;
import com.ruinscraft.powder.objects.SoundEffect;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PowderCommand implements CommandExecutor {

	BukkitScheduler scheduler = Powder.getInstance().getServer().getScheduler();

	private List<Player> recentCommandSenders = new ArrayList<Player>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(Powder.getInstance().getName() + " | " + Powder.getInstance().getDescription());
			return true;
		}

		Player player = (Player) sender;
		
		if (!(player.hasPermission("powder.command"))) {
			player.sendMessage(Powder.PREFIX + 
					ChatColor.RED + "You don't have permission to use /" + label + ".");
			return false;
		}

		PowderHandler powderHandler = Powder.getInstance().getPowderHandler();

		if (args.length < 1) {
			powderList(player, powderHandler, label);
			return false;
		}

		if (args[0].equals("reload")) {
			if (!(player.hasPermission("powder.reload"))) {
				player.sendMessage(Powder.PREFIX + 
						ChatColor.RED + "You don't have permission to do this.");
				return false;
			}
			Powder.getInstance().reloadConfig();
			Powder.getInstance().handleConfig();
			player.sendMessage(Powder.PREFIX + 
					ChatColor.GRAY + "Powder config.yml reloaded!");
			List<Player> playersDoneAlready = new ArrayList<Player>();
			for (PowderTask powderTask : powderHandler.getPowderTasks()) {
				if (playersDoneAlready.contains(powderTask.getPlayer())) {
					continue;
				}
				playersDoneAlready.add(powderTask.getPlayer());
				powderTask.getPlayer().sendMessage(Powder.PREFIX + ChatColor.GRAY + "Your Powders were cancelled due to " +
						"a reload.");
			}
			return true;
		}
		
		if (args[0].equals("*")) {
			
			if (args.length < 2) {
				player.sendMessage(Powder.PREFIX + 
						ChatColor.GRAY + "Use '* cancel' to cancel all current active Powders.");
				return false;
			} else {
				if (powderHandler.getPowderTasks(player).isEmpty()) {
					player.sendMessage(Powder.PREFIX + 
							ChatColor.RED + "There are no Powders currently active.");
					return false;
				}
				int amount = powderHandler.getPowderTasks(player).size();
				for (PowderTask powderTask : powderHandler.getPowderTasks(player)) {
					powderHandler.removePowderTask(powderTask);
				}
				player.sendMessage(Powder.PREFIX + 
						ChatColor.GRAY + "Successfully cancelled all Powders! (" + 
						amount + " total)");
				return true;
			}
			
		}

		PowderMap map = powderHandler.getPowderMap(args[0]);

		if (map == null) {
			powderList(player, powderHandler, label);
			return false;
		}

		if (!(player.hasPermission("powder.powder." + map.getName()))) {
			player.sendMessage(Powder.PREFIX + 
					ChatColor.RED + "You don't have permission to use the Powder '" + map.getName() + "'.");
			return false;
		}
		
		if (args.length > 1) {
			if (args[1].equalsIgnoreCase("cancel")) {
				boolean success = false;
				int taskAmount = powderHandler.getPowderTasks(player, map).size();
				for (PowderTask powderTask : powderHandler.getPowderTasks(player, map)) {
					powderHandler.removePowderTask(powderTask);
					success = true;
				}
				if (success) {
					player.sendMessage(Powder.PREFIX + 
							ChatColor.GRAY + "Powder '" + map.getName() + "' cancelled! (" + 
							taskAmount + " total)");
				} else {
					player.sendMessage(Powder.PREFIX + 
							ChatColor.RED + "There are no '" + map.getName() + "' Powders currently active.");
				}
			}
			return false;
		}

		int maxSize = Powder.getInstance().getConfig().getInt("maxPowdersAtOneTime");
		if ((powderHandler.getPowderTasks(player).size() >= maxSize)) {
			player.sendMessage(Powder.PREFIX + 
					ChatColor.RED + "You already have " + maxSize + " Powders active!");
			for (PowderTask powderTask : powderHandler.getPowderTasks(player)) {
				TextComponent runningTaskText = new TextComponent(net.md_5.bungee.api.ChatColor.GRAY + "| " 
							+ net.md_5.bungee.api.ChatColor.ITALIC + powderTask.getMap().getName());
				runningTaskText.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, 
						new ComponentBuilder("'" + powderTask.getMap().getName() + "' is currently active. Click to cancel")
						.color(net.md_5.bungee.api.ChatColor.YELLOW).create() ) );
				runningTaskText.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, 
						"/" + label + " " + powderTask.getMap().getName() + " cancel" ) );
				player.spigot().sendMessage(runningTaskText);
			}
			return false;
		}
		if (!(Powder.getInstance().getConfig().getBoolean("allowSamePowdersAtOneTime"))) {
			for (PowderTask powderTask : powderHandler.getPowderTasks(player)) {
				if (powderTask.getMap().equals(map)) {
					player.sendMessage(Powder.PREFIX + 
							ChatColor.RED + "'" + map.getName() + "' is already active!");
					return false;
				}
			}
		}

		// wait time between each command
		int waitTime = Powder.getInstance().getConfig().getInt("secondsBetweenPowderUsage");
		if (recentCommandSenders.contains(player)) {
			player.sendMessage(Powder.PREFIX + 
					ChatColor.RED + "Please wait " + waitTime + " seconds between using each Powder.");
			return false;
		}
		scheduler.scheduleSyncDelayedTask(Powder.getInstance(), new Runnable() {
			public void run() {
				recentCommandSenders.remove(player);
			}
		}, (waitTime * 20));
		recentCommandSenders.add(player);

		int task;
		List<Integer> tasks = new ArrayList<Integer>();

		if (map.isRepeating()) {

			task = scheduler.scheduleSyncRepeatingTask(Powder.getInstance(), new Runnable() {
				public void run() {
					tasks.addAll(createEverything(player, map, powderHandler));
				}
			}, 0L, map.getDelay());

			tasks.add(task);

		} else {
			tasks.addAll(createEverything(player, map, powderHandler));
		}
		
		tasks.addAll(createDusts(player, map, powderHandler));
		
		if (map.isRepeating() || map.getStringMaps().size() > 1) {
			if (new Random().nextInt(12) == 1) {
				player.sendMessage(Powder.PREFIX + ChatColor.GRAY + 
						"Tip: Use '/" + label + " <powder> cancel' to cancel a Powder.");
			}
		}

		PowderTask powderTask = new PowderTask(player, tasks, map);
		powderHandler.addPowderTask(powderTask);

		return true;

	}

	public static void powderList(Player player, PowderHandler phandler, String label) {

		player.sendMessage(ChatColor.RED + "Please send a valid Powder name " + 
				ChatColor.GRAY + "(/" + label +  " <powder>)" + ChatColor.RED + ":");
		TextComponent powderMaps = new TextComponent("    ");
		powderMaps.setColor(net.md_5.bungee.api.ChatColor.GRAY);
		for (PowderMap powderMap : phandler.getPowderMaps()) {
			TextComponent powderMapText = new TextComponent(powderMap.getName());
			if (!(player.hasPermission("rcp.powder." + powderMap.getName().toLowerCase(Locale.US)))) {
				powderMapText.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
				powderMapText.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, 
						new ComponentBuilder("You don't have permission to use '" + powderMap.getName() + "'.")
						.color(net.md_5.bungee.api.ChatColor.RED).create() ) );
			} else if (!(phandler.getPowderTasks(player, powderMap).isEmpty())) {
				powderMapText.setColor(net.md_5.bungee.api.ChatColor.GREEN);
				powderMapText.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, 
						new ComponentBuilder("'" + powderMap.getName() + "' is currently active. Click to cancel")
						.color(net.md_5.bungee.api.ChatColor.YELLOW).create() ) );
				powderMapText.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, 
						"/" + label + " " + powderMap.getName() + " cancel" ) );
			} else {
				powderMapText.setColor(net.md_5.bungee.api.ChatColor.GRAY);
				powderMapText.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, 
						new ComponentBuilder("Click to use '" + powderMap.getName() + "'.")
						.color(net.md_5.bungee.api.ChatColor.GRAY).create() ) );
				powderMapText.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, 
						"/" + label + " " + powderMap.getName()) );
			}
			powderMaps.addExtra(powderMapText);
			powderMaps.addExtra("    ");
		}
		player.spigot().sendMessage(powderMaps);
		if (!(phandler.getPowderTasks(player).isEmpty())) {
			player.sendMessage(ChatColor.RED + "Use " + ChatColor.GRAY + "'/" + label + " <powder> cancel'" +
					ChatColor.RED + " to cancel a Powder.");
		}

	}

	public static List<Integer> createEverything(final Player player, final PowderMap map, PowderHandler powderHandler) {

		List<Integer> tasks = new ArrayList<>();
		tasks.addAll(createParticles(player, map, powderHandler));
		tasks.addAll(createSounds(player, map, powderHandler));
		return tasks;
		
	}

	public static List<Integer> createParticles(final Player player, final PowderMap map, PowderHandler powderHandler) {

		final float spacing = map.getSpacing();

		List<String> smaps = map.getStringMaps();
		
		List<Integer> tasks = new ArrayList<Integer>();

		for (String smap : smaps) {

			long waitTime = 0;

			try {
				waitTime = Long.parseLong(smap.substring(0, smap.indexOf(";")));
			} catch (Exception e) { }

			int task = Powder.getInstance().getServer()
					.getScheduler().scheduleSyncDelayedTask(Powder.getInstance(), new Runnable() {

						@Override
						public void run() {

							if (!player.isOnline()) {
								return;
							}
							Location location = Bukkit.getPlayer(player.getName()).getEyeLocation();

							final double rot = ((player.getLocation().getYaw()) % 360) * (Math.PI / 180);
							final double pitch;

							final double ydist;
							final double xzdist;

							if (map.getPitch()) {
								pitch = (player.getLocation().getPitch() * (Math.PI / 180));
								ydist = ((Math.sin((Math.PI / 2) - pitch)) * spacing);
								xzdist = ((Math.cos((Math.PI / 2) + pitch)) * spacing);
							} else {
								pitch = 0;
								ydist = spacing;
								xzdist = 0;
							}

							// more doubles
							final double fx = getDirLengthX(rot, spacing);
							final double fz = getDirLengthZ(rot, spacing);
							final double rfx = getDirLengthX(rot + (Math.PI / 2), spacing);
							final double rfz = getDirLengthZ(rot + (Math.PI / 2), spacing);
							final double gx = getDirLengthX(rot - (Math.PI / 2), xzdist);
							final double gz = getDirLengthZ(rot - (Math.PI / 2), xzdist);
							final double rgx = getDirLengthX(rot, xzdist);
							final double rgz = getDirLengthZ(rot, xzdist);
							final double rgy = (Math.sin(0 - pitch) * spacing);

							final double sx = location.getX() - 
									(fx * map.getPlayerLeft()) + (gx * map.getPlayerUp());
							final double sy = location.getY() + (ydist * map.getPlayerUp());
							final double sz = location.getZ() - 
									(fz * map.getPlayerLeft()) + (gz * map.getPlayerUp());

							double ssx = sx;
							double ssy = sy;
							double ssz = sz;

							StringBuilder sb = new StringBuilder();
							double result = 0;
							boolean buildAnInt = false;
							boolean lastCharInt = false;
							int downSoFar = 0;

							for (char a : smap.toCharArray()) {

								String aa = String.valueOf(a);

								if (aa.equals(".")) {
									ssx = ssx + fx;
									ssz = ssz + fz;
									lastCharInt = false;
									continue;
								}

								if (aa.equals("{")) {
									buildAnInt = true;
									continue;
								}
								if (aa.equals("}")) {
									buildAnInt = false;
									try {
										Integer.parseInt(sb.toString());
									} catch (Exception e) {
										Powder.getInstance().getLogger().log(Level.WARNING, "INVALID NUMBER AAA");
										sb.setLength(0);
										continue;
									}
								}

								if (buildAnInt == true) {
									lastCharInt = true;
									sb.append(aa);
									continue;
								} else {
									lastCharInt = false;
								}

								if ((buildAnInt == false) && !(sb.length() == 0)) {

									downSoFar = 0;
									result = Integer.parseInt(sb.toString());

									ssy = sy + (rgy * result);

									ssx = sx + (rfx * result) + (rgz * result);
									ssz = sz + (rfz * result) + (rgx * result);

									sb.setLength(0);

									continue;

								}

								if (aa.equals(";")) {

									if (lastCharInt == true) {
										ssy = sy;
									} else {
										downSoFar++;
										ssx = sx + (rfx * result) - (gx * downSoFar);
										ssz = sz + (rfz * result) - (gz * downSoFar);
										ssy = ssy - ydist;
									}
									continue;

								}

								ssx = ssx + fx;
								ssz = ssz + fz;
								lastCharInt = false;

								String pname = null;
								boolean success = false;
								for (ChangedParticle changedParticle : map.getChangedParticles()) {
									if (changedParticle.getEnumName().equals(aa)) {
										Particle particle = changedParticle.getParticle();
										if (changedParticle.getData() == null) {
											if (changedParticle.getXOff() == 0) {
												player.getWorld().spawnParticle(particle, ssx, ssy, ssz, 0, Double.MIN_NORMAL, 
														changedParticle.getYOff() / 255, changedParticle.getZOff() / 255, 1);
											} else {
												player.getWorld().spawnParticle(particle, ssx, ssy, ssz, 0, (changedParticle.getXOff() / 255), 
														changedParticle.getYOff() / 255, changedParticle.getZOff() / 255, 1);
											}
										} else {
											// player.getWorld().spawnParticle(particle, ssx, ssy, ssz, 1, changedParticle.getXOff(), 
											//		changedParticle.getYOff(), changedParticle.getZOff(), changedParticle.getData());
										}
										success = true;
										break;
									}
								}
								if (success) continue;
								try {
									pname = ParticleName.valueOf(aa).getName();
								} catch (Exception e) {
									continue;
								}

								player.getWorld().spawnParticle(Particle.valueOf(pname), ssx, ssy, ssz, 1, 0, 0, 0, 0);

							}

						}

					},waitTime);

			tasks.add(task);

		}
		
		return tasks;

	}

	public static List<Integer> createSounds(final Player player, final PowderMap map, PowderHandler powderHandler) {

		List<Integer> tasks = new ArrayList<Integer>();
		
		for (SoundEffect sound : map.getSounds()) {

			int task = Powder.getInstance().getServer().getScheduler()
					.scheduleSyncDelayedTask(Powder.getInstance(), new Runnable() {

						@Override
						public void run() {

							player.getWorld().playSound(player.getLocation(), sound.getSound(),
									sound.getVolume(), sound.getPitch());

						}

					},((long) sound.getWaitTime()));

			tasks.add(task);

		}
		
		return tasks;

	}
	
	public static List<Integer> createDusts(final Player player, final PowderMap map, PowderHandler powderHandler) {
		
		List<Integer> tasks = new ArrayList<Integer>();
		
		for (Dust dust : map.getDusts()) {
			
			// frequency is particles per min
			// translate to ticks
			long frequency = 1200 / dust.getFrequency();
			
			int task = Powder.getInstance().getServer().getScheduler()
					.scheduleSyncRepeatingTask(Powder.getInstance(), new Runnable() {
						
				public void run() {
					double radiusZoneX = (Math.random() - .5) * (2 * dust.getRadius());
					double radiusZoneZ = (Math.random() - .5) * (2 * dust.getRadius());
					double heightZone = (Math.random() - .5) * (2 * dust.getHeight());
					Location particleLocation = player.getLocation().add(radiusZoneX, heightZone + 1, radiusZoneZ);
					if (particleLocation.getBlock().isEmpty()) {
						player.getWorld().spawnParticle(dust.getParticle(), particleLocation, 1, null);
					}
				}
				
			}, frequency, frequency);
			
			tasks.add(task);
			
		}
		
		return tasks;
		
	}

	public static double getDirLengthX(double rot, double spacing) {

		return (spacing * Math.cos(rot));

	}

	public static double getDirLengthZ(double rot, double spacing) {

		return (spacing * Math.sin(rot));

	}

}