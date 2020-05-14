# Powder ![Java CI with Maven](https://github.com/Kas-tle/Powder/workflows/Java%20CI%20with%20Maven/badge.svg)

Powder is a Spigot plugin which utilizes particles & sound effects to allow for incredibly customizable pictures and animations.

Powders can be used for a fun donator perk (like SimplePets or LibsDisguises), they can be used as signs (like HolographicDisplays), or they can be used to spruce up a world/build. Powders are created in a powders.yml file, where sounds and particles can be manipulated extensively. Images can be imported to be created out of particles, Note Block Studio files can be imported to create songs, and other features can be utilized to allow for virtually any creation.

This fork makes some minor changes to allow for 1.15 implementation, as well as a minor fix for handling a missing player data file. Currently, it will only work properly with 1.15, but 1.14 should work as well with a few minor changes.

### Downloading Latest Commit:
A jar packaged version of the latest commit can be downloaded from the latest passing action on the [Actions Page](https://github.com/Kas-tle/Powder/actions). Simply unzip the artifact.

### Compiling:
```
git clone https://github.com/Kas-tle/Powder
cd Powder
mvn package
```
### Some features:
* Unlimited usage of layering/animation
* Alter the rotation/pitch of your animation
* Use any sound in the game
* Create a dusty effect with a particle in the radius of a location
* Allow components of a Powder to start whenever, iterate however many times, repeat whenever, or go on infinitely
* Allow components of a Powder to follow the direction of the body or eyes of a player (perfect for laser beam eyes)
* Alter the spacing between particles
* Attachable to players and entities alike
* Attachable to a specific location (stored in a yml file)
* MySQL integration -- allow Powders to be used between servers or after logging out and logging in
* Import pictures from URLs or local files to be used in a Powder
* Import songs (Note Block Studio (.nbs) files) to be played with the Powder, with alterable speed
* Attach a particle to a certain note used in the Powder, allowing for particles to appear with a song's melody/beat
* Highly customizable locale
* Much more! Check out the [wiki](https://github.com/Ruinscraft/Powder/wiki) for more information.

Powder comes with a couple dozen Powders to show the possibilities of what can be created. Check out various resources and tutorials in the [wiki](https://github.com/Ruinscraft/Powder/wiki).

[Spigot page](https://www.spigotmc.org/resources/powder.57227/)

[Additional resources for Powder](https://github.com/Ruinscraft/powder-resources)

[Original Powder Repository](https://github.com/Ruinscraft/Powder)

Contributions are welcomed & appreciated!
