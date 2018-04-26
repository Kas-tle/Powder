package com.ruinscraft.powder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.ruinscraft.powder.PowderHandler;
import com.ruinscraft.powder.PowderPlugin;
import com.ruinscraft.powder.models.Dust;
import com.ruinscraft.powder.models.Layer;
import com.ruinscraft.powder.models.ParticleMatrix;
import com.ruinscraft.powder.models.ParticleName;
import com.ruinscraft.powder.models.Powder;
import com.ruinscraft.powder.models.PowderElement;
import com.ruinscraft.powder.models.PowderParticle;
import com.ruinscraft.powder.models.SoundEffect;

public class YamlUtil {
	
	public static void loadConfig(FileConfiguration config) {
		PowderPlugin instance = PowderPlugin.getInstance();
		File configFile = new File(instance.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			instance.getLogger().info("config.yml not found, creating!");
			instance.saveDefaultConfig();
		}
		instance.reloadConfig();
		config = instance.getConfig();
	}
	
	public static List<FileConfiguration> loadPowderConfigs(List<FileConfiguration> powderConfigs) {
		// list of configuration files that contain Powders
		powderConfigs = new ArrayList<FileConfiguration>();

		Logger logger = PowderPlugin.getInstance().getLogger();
		FileConfiguration config = PowderPlugin.getInstance().getConfig();
		File dataFolder = PowderPlugin.getInstance().getDataFolder();

		for (String urlName : config.getStringList("powderSources")) {

			FileConfiguration powderConfig;
			URL url = PowderUtil.readURL(urlName);
			File file;
			// if a file is from a path, load from within data folder
			if (!urlName.contains("/")) {

				file = new File(dataFolder, urlName);
				if (!file.exists()) {
					logger.warning("Failed to load config file '" + urlName + "'.");
					continue;
				}
				powderConfig = YamlConfiguration.loadConfiguration(file);

				// else, load from URL
			} else if (url != null) {

				InputStream stream = PowderUtil.getInputStreamFromURL(url);

				if (stream == null) {
					continue;
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				powderConfig = YamlConfiguration.loadConfiguration(reader);

			} else {
				logger.warning("Failed to load config file '" + urlName + "'.");
				continue;
			}

			powderConfigs.add(powderConfig);

		}

		// if powders.yml is listed as a source but doesn't exist, create it
		File defaultPowderConfig = new File(dataFolder, "powders.yml");
		if (!defaultPowderConfig.exists() && config.getStringList("powderSources").contains("powders.yml")) {
			logger.info("powders.yml not found but listed as a source, creating!");
			PowderPlugin.getInstance().saveResource("powders.yml", false);
			FileConfiguration powderConfig = YamlConfiguration.loadConfiguration(defaultPowderConfig);
			powderConfigs.add(powderConfig);
		}
		return powderConfigs;
	}
	
	public static void reloadCategories() {
		PowderHandler powderHandler = PowderPlugin.getInstance().getPowderHandler();
		FileConfiguration config = PowderPlugin.getInstance().getConfig();
		if (powderHandler.categoriesEnabled()) {
			for (String s : config.getConfigurationSection("categories").getKeys(false)) {
				powderHandler.addCategory(s, config.getString("categories." + s + ".desc", ""));
			}
			if (!powderHandler.getCategories().keySet().contains("Other")) {
				powderHandler.addCategory("Other", "Unsorted Powders");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Powder loadPowderFromConfig(FileConfiguration powderConfig, String s) {
		Powder powder = new Powder();
		
		PowderHandler powderHandler = PowderPlugin.getInstance().getPowderHandler();
		Logger logger = PowderPlugin.getInstance().getLogger();

		String section = "powders." + s;

		// set some given values if they exist, default value if they don't
		powder.setName(powderConfig.getString(section + ".name", null));
		powder.setDefaultSpacing((float) powderConfig.getDouble(section + ".spacing", .5F));
		powder.setHidden(powderConfig.getBoolean(section + ".hidden", false));

		// add categories if enabled
		if (powderHandler.categoriesEnabled()) {
			for (String t : (List<String>) powderConfig.getList(section + ".categories", new ArrayList<String>())) {
				if (!(powderHandler.getCategories().keySet().contains(t))) {
					logger.warning("Invalid category '" + t + 
							"' for '" + powder.getName() + "' in " + powderConfig.getName());
					continue;
				}
				powder.addCategory(t);
			}
			if (powder.getCategories().isEmpty()) {
				powder.addCategory("Other");
			}
		}

		// SoundEffect
		// 'BLOCK_NOTE_PLING;4.0;1.50;2;10;200'
		// 'sound;volume;pitch;startTime;repeatTime;iterations'

		// song
		// 'Shrek.nbs;50;2;0;2400;2'
		// 'fileName;volume;multiplier;startTime;repeatTime;iterations'

		if (!(powderConfig.getConfigurationSection(section + ".songs") == null)) {
			for (String ss : powderConfig.getConfigurationSection(section + ".songs").getKeys(false)) {
				String eachSection = section + ".songs." + ss;
				String fileName = powderConfig.getString(eachSection + ".fileName", "unknownfile.nbs");
				double volume = powderConfig.getDouble(eachSection + ".volume", 1);
				double multiplier = powderConfig.getDouble(eachSection + ".multiplier", 1);
				int startTime = powderConfig.getInt(eachSection + ".startTime", 0);
				int repeatTime = powderConfig.getInt(eachSection + ".repeatTime", 20);
				int iterations = powderConfig.getInt(eachSection + ".iterations", 1);
				List<SoundEffect> songSoundEffects = SoundUtil.getSoundEffectsFromNBS(fileName, volume, 
						multiplier, startTime, repeatTime, iterations);
				for (SoundEffect soundEffect : songSoundEffects) {
					powder.addPowderElement(soundEffect);
				}
			}
		}
		if (!(powderConfig.getConfigurationSection(section + ".sounds") == null)) {
			for (String ss : powderConfig.getConfigurationSection(section + ".sounds").getKeys(false)) {
				String eachSection = section + ".sounds." + ss;
				String soundEnum = powderConfig.getString(eachSection + ".soundEnum", "BLOCK_NOTE_CHIME");
				Sound sound = Sound.valueOf(soundEnum);
				double volume = powderConfig.getDouble(eachSection + ".volume", 1);
				float soundPitch = (float) powderConfig.getDouble(eachSection + ".note", 1);
				soundPitch = (float) Math.pow(2.0, ((double)soundPitch - 12.0) / 12.0);
				int startTime = powderConfig.getInt(eachSection + ".startTime", 0);
				int repeatTime = powderConfig.getInt(eachSection + ".repeatTime", 20);
				int iterations = powderConfig.getInt(eachSection + ".iterations", 1);
				powder.addPowderElement(new SoundEffect(sound, volume, soundPitch, startTime, repeatTime, iterations));
			}
		}


		if (!(powderConfig.getConfigurationSection(section + ".changes") == null)) {
			for (String ss : powderConfig.getConfigurationSection(section + ".changes").getKeys(false)) {
				String eachSection = section + ".changes." + ss;
				String particleChar = powderConfig.getString(eachSection + ".particleChar", "A");
				char character = particleChar.charAt(0);
				String particleEnum = powderConfig.getString(eachSection + ".particleEnum", "HEART");
				Particle particle = Particle.valueOf(particleEnum);
				int amount = powderConfig.getInt(eachSection + ".amount", 1);
				double xOffset = powderConfig.getDouble(eachSection + ".xOffset", 0);
				double yOffset = powderConfig.getDouble(eachSection + ".yOffset", 0);
				double zOffset = powderConfig.getDouble(eachSection + ".zOffset", 0);
				double data = powderConfig.getDouble(eachSection + ".data", 0);
				powder.addPowderParticle(new PowderParticle(character, particle, amount, xOffset, yOffset, zOffset, data));
			}
		}

		// Dust
		// 'A;2;1;3;3;0'
		// 'PowderParticle;radius;height&depth;startTime;repeatTime;iterations'

		if (!(powderConfig.getConfigurationSection(section + ".dusts") == null)) {
			for (String ss : powderConfig.getConfigurationSection(section + ".dusts").getKeys(false)) {
				String eachSection = section + ".dusts." + ss;
				String dustName = powderConfig.getString(eachSection + ".particleChar", "null");
				char character = dustName.charAt(0);
				PowderParticle powderParticle = powder.getPowderParticle(character);
				if (powderParticle == null) {
					try {
						Particle particle = Particle.valueOf(ParticleName.valueOf(dustName).getName());
						powderParticle = new PowderParticle(character, particle);
					} catch (Exception e) {
						powderParticle = new PowderParticle();
					}
				}
				double radius = powderConfig.getDouble(eachSection + ".radius", 1);
				double height = powderConfig.getDouble(eachSection + ".height", 1);
				double span = powderConfig.getDouble(eachSection + ".span", 1);
				List<PowderElement> addedPowderElements = new ArrayList<PowderElement>();
				if (powderConfig.getBoolean(eachSection + ".attachToNote")) {
					String noteName = powderConfig.getString(eachSection + ".attachedToNote", "BLOCK_NOTE_HARP");
					for (PowderElement powderElement : powder.getPowderElements().keySet()) {
						 if (powderElement instanceof SoundEffect) {
							 SoundEffect soundEffect = (SoundEffect) powderElement;
							 if (soundEffect.getSound().name().equals(noteName)) {
								 addedPowderElements.add(new Dust(powderParticle, radius, height, span, 
										 soundEffect.getStartTime(), soundEffect.getRepeatTime(), soundEffect.getLockedIterations()));
							 }
						 }
					}
					powder.addPowderElements(addedPowderElements);
					continue;
				}
				int startTime = powderConfig.getInt(eachSection + ".startTime", 0);
				int repeatTime = powderConfig.getInt(eachSection + ".repeatTime", 20);
				int iterations = powderConfig.getInt(eachSection + ".iterations", 1);
				powder.addPowderElement(new Dust(powderParticle, radius, height, span, 
						startTime, repeatTime, iterations));
			}
		}

		// [.1;true;2;12;10]
		// [spacing;pitch;startTime;repeatTime;iterations]

		if (!(powderConfig.getConfigurationSection(section + ".matrices") == null)) {
			for (String ss : powderConfig.getConfigurationSection(section + ".matrices").getKeys(false)) {
				String eachSection = section + ".matrices." + ss;
				boolean containsPlayer = false;
				ParticleMatrix particleMatrix = new ParticleMatrix();
				particleMatrix.setSpacing(powderConfig.getDouble(eachSection + ".spacing", .1));
				particleMatrix.setIfPitch(powderConfig.getBoolean(eachSection + ".hasPitch", false));
				particleMatrix.setAddedPitch(powderConfig.getDouble(eachSection + ".addedPitch", 0));
				particleMatrix.setAddedRotation(powderConfig.getDouble(eachSection + ".addedRotation", 0));
				particleMatrix.setAddedTilt(powderConfig.getDouble(eachSection + ".addedTilt", 0));
				particleMatrix.setStartTime(powderConfig.getInt(eachSection + ".startTime", 0));
				particleMatrix.setRepeatTime(powderConfig.getInt(eachSection + ".repeatTime", 20));
				particleMatrix.setLockedIterations(powderConfig.getInt(eachSection + ".iterations", 1));
				int left = 0;
				int up = 0;
				for (String sss : powderConfig.getConfigurationSection(eachSection + ".layers").getKeys(false)) {
					String eachEachSection = eachSection + ".layers." + sss;
					Layer layer = new Layer();
					layer.setPosition(powderConfig.getDouble(eachEachSection + ".position", 0));
					for (String t : (List<String>) powderConfig.getList(eachEachSection + ".layerMatrix", new ArrayList<String>())) {
						if (t.contains(":")) {
							if (t.contains("img:")) {
								String urlName;
								int width;
								int height;
								t = t.replace("img:", "");
								urlName = t.substring(0, t.indexOf(";"));
								t = t.substring(t.indexOf(";") + 1, t.length());
								width = Integer.valueOf(t.substring(0, t.indexOf(";")));
								t = t.substring(t.indexOf(";") + 1, t.length());
								height = Integer.valueOf(t);
								try {
									ImageUtil.getRows(layer.getRows(), urlName, width, height);
								} catch (IOException io) {
									logger.warning("Failed to load image: '" + urlName + "'");
									continue;
								}
								// add height to compensate for dist. from location (might not necessarily correspond w/ actual image)
								up = up + height;
							}
							continue;
						}
						// if the Layer is in the same position as where the location/player is
						if (layer.getPosition() == 0) {
							up++;
							// if the string contains location/player
							if (t.contains("?")) {
								containsPlayer = true;
								// set the left & up of the Layer so createPowders() knows where to start
								left = (t.indexOf("?")) + 1;
								// set default if it's the matrix spawned immediately 
								if (particleMatrix.getStartTime() == 0) {
									powder.setDefaultLeft(left - 1);
									powder.setDefaultUp(up + 1);
								}
								particleMatrix.setPlayerLeft(left - 1);
								particleMatrix.setPlayerUp(up + 1);
							}
						}
						// add a row to the Layer if it has gone through everything
						// rows contain PowderParticles
						List<PowderParticle> row = new ArrayList<PowderParticle>();
						for (char character : t.toCharArray()) {
							PowderParticle powderParticle;
							powderParticle = powder.getPowderParticle(character);
							if (powderParticle == null) {
								try {
									String string = String.valueOf(character);
									Particle particle = Particle.valueOf(ParticleName.valueOf(string).getName());
									powderParticle = new PowderParticle(character, particle);
								} catch (Exception e) {
									powderParticle = new PowderParticle();
								}
							}
							row.add(powderParticle);
						}
						layer.addRow(row);
					}
					particleMatrix.addLayer(layer);
				}
				if (!containsPlayer) {
					particleMatrix.setPlayerLeft(powder.getDefaultLeft());
					particleMatrix.setPlayerUp(powder.getDefaultUp());
				}
				powder.addPowderElement(particleMatrix);
			}
		}

		if (powder.getPowderElements().isEmpty()) {
			PowderPlugin.getInstance().getLogger().warning("Powder '" + powder.getName() + "' appears empty and was not loaded.");
			return null;
		}
		return powder;
	}

}