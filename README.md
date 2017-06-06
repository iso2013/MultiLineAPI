## Description
MultiLineAPI is an easy to use API that allows the server to display a unique second line of text for each player, and allows the player's nametag to be changed (without the limitations of other methods - see below for more information).

#### MultiLineAPI vs Scoreboards and other alternatives

|                               | MultiLineAPI        | Vanilla Scoreboard | TagAPI             | NickNamer          |
|-------------------------------|---------------------|--------------------|--------------------|--------------------|
| Nametag support               | ✔                  | ✘                  | ✔                 | ✔                 |
| Infinite Line support         | ✔                  | ✔                  | ✘                 | ✘                 |
| Character limit (name / line) | (∞ / ∞)             | (✘ / ∞)            | (16 / ✘)          | (16 / ✘)          |
| Included API                  | ✔                  | ✔                  | ✔                 | ✔                 |
| Minecraft version             | 1.8+                | 1.6+               | 1.7 and below.     | 1.7 - 1.10        |
| Commands                      | ✔                  | ✔                  | ✘                 | ✔                 |
| Software Requirements         | Plugin & ProtocolLib | ✘                  | Plugin           | Plugin & PacketListenerAPI |

## Compatibility and Dependencies
MultiLineAPI currently requires ProtocolLib. This is because of a Minecraft server limitation - Players (or any entity) who have passengers are unable to teleport. For this reason, ProtocolLib is used to send the mount packets to the client manually, so the player appears to have the entities mounted on their head but is still able to teleport. ProtocolLib is also used to send packets which prevent a player from seeing their own tags.

## Commands
MultiLineAPI does not have any commands.

## Permissions
MultiLineAPI does not have any permissions.

## Known Limitations
Sometimes the entities can become separated from the player. If this happens, they will appear to follow the player around at Y=-10. If this happens, please report it and include steps to reproduce using the issue tracker below.

## API Usage
MultiLineAPI is designed as an API. It is not meant to be used as a plugin on its own. API documentation is available [here](https://iso2013.github.io/MultiLineAPI/javadocs/).

### Maven
MultiLineAPI can be retrieved from the [DigitalPenquin Repository](https://repo.srv.digitalpenquin.com). See below for details:

#### Repository
```
<repositories>
    <repository>
	    <id>digitalpenquin-repo</id>
  	    <url>https://repo.srv.digitalpenquin.com/repository/maven-public/</url>
    </repository>
</repositories>
```

#### Artifact
```
<dependencies>
    <dependency>
        <groupId>net.blitzcube.mlapi</groupId>
        <artifactId>MultiLineAPI</artifactId>
        <version>X.Y.Z</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

The current version deployed to the repository can be found on the [**Releases**](https://github.com/simpleauthority/MultiLineAPI/releases) page.

## Screenshots
MultiLineAPI does not lag behind when players move:

![LagFree](docs/no-lag.gif)

MultiLineAPI also supports more than just one additional line:

![ManyLines](docs/three-lines-demo.gif)

A comparison between MultiLineAPI and the vanilla scoreboard:

![Comparison](docs/comparison.png)

## Issues
You may report issues [here](https://github.com/iso2013/MultiLineAPI/issues). Please include the following information, otherwise your report will be ignored:
* Steps to reproduce
* Expected behaviour
* Actual outcome
* Minecraft version (Only 1.10 and 1.11 are officially supported)

## Source Code
Source code is available [here](https://github.com/iso2013/MultiLineAPI)!

## Donations
Want to donate to continue MultiLineAPI's development? I accept donations here:

<center>
    <a href="https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=blitzcubeowner%40gmail%2ecom&lc=US&item_name=iso2013&item_number=MultiLineAPI&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted">
        <img src="docs/paypal.png"/>
    </a>
</center>
