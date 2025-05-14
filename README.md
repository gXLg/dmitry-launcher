# dmitry-launcher
Simple Minecraft fabric client+server setup

# Usage
1. Install Java on your PC (for example [<kbd>here</kbd>](https://www.oracle.com/java/technologies/downloads/))
2. Go to the [<kbd>latest release</kbd>](https://github.com/gXLg/dmitry-launcher/releases/latest) and download the `Launcher.jar`
3. Follow the instuctions below

# Launcher
## Start
You can start the launcher by simply double-clicking the jar file. You will see a window similar to this:
<br><img src="https://github.com/user-attachments/assets/8c920f1d-50ce-4a81-b078-1aa2dce0b574" height="500">

Here the buttons are pretty intuitive. The creation of profiles is explained further below.

This window can be closed once the profile has been selected. Although it is possible to start the same profile multiple times, it is highly advised against it, because it can corrupt files.

## Run
After selecting a profile you will see a window like this:
<br><img src="https://github.com/user-attachments/assets/d6b825ca-5eda-4321-8ee6-e6e6fccb60fc" height="500">

This window consists of 2 parts:
* The upper rectangle, marked orange, "output panel" - this is where the text output happens
* The lower small rectangle, marked green, "input panel" - this is where you are supposed to write text if needed

All inputs in the input panel work like this: A prompt appears in the output panel with a <kbd>&gt;</kbd>, you type your answer in the input panel and press <kbd>enter</kbd>.

The runner must be open at all times while running the game or the server. After a message like "Game closed, you can close the window now" appears, or some error occures,
you can close the runner. Closing the runner mid game or mid installation might corrupt the files and is not desired.

# Files
All of the files which are needed to run the launcher are stored under:
* Windows: `<DRIVE>:\Users\<username>\.dmitry-launcher`
* Unix: `/home/<username>/.dmitry-launcher`

You can also open this folder by pressing the <kbd>Open launcher folder</kbd> button.

# Functionality
The launcher offers a simple method of playing Minecraft in offline mode (without a license).
The launcher automatically installs Fabric, and some essential mods to easily play with your friends remotely.

## Client-Side Mods
The launcher automatically tries to install "Fabric API", which is needed for any Fabric mod.
If it fails, it means, that the current Minecraft version is not supported by Fabric and the game will not start.

The launcher tries to install "Modflared" - a mod which simplifies connecting to a Cloudflare remote server.
Modflared doesn't support all Minecraft versions. If your selected version isn't supported by Modflared, you will see a message
like `"You won't be able to play on a remote cloudflare server in this version"`.

In addition to that, the launcher also tries to install "Mod Menu", which is however optional and doesn't affect the starting of the game.

# Game
To start a normal minecraft game, select one of the buttons in the "client" section.
Upon start, the launcher will download all the neccessary stuff to play Minecraft and start up Minecraft after a while.

If you start the game for the first time, you will be asked for a playername. The playername can later be changed by manually changing the file `<launcher>/playername.txt`.

The game starts with 3 GB of RAM.

## Profiles
If you want to select the client option, you will be given a list of existing profiles, and an option to create a new profile. If you start the launcher for the first time, you will see no profiles.

To select a profile, click on the button with the profile name. This will load all the needed libraries and start the game.

To create a profile:
1. Type the name of the profile in the client section of the launcher; this name will only be visible to you. Don't use an already existing name!
2. Type a minecraft version you want to play in. Please note, that not all Minecraft versions are supported by Fabric and you may see some error messages if you select an invalid or an unsupported version.
3. Type a list of mods you would want to have in that profile, separated by <kbd>,</kbd>. The name is the Mod ID taken from [<kbd>Modrinth</kbd>](https://modrinth.com/mods).
   - For example if you want to install Sodium and Iris, their respective Modrinth URLs are `https://modrinth.com/mod/sodium` and `https://modrinth.com/mod/iris`. So the respecitve IDs are `sodium` and `iris` and you would type <kbd>sodium,iris</kbd>.
   - If you don't want any additional mods, just leave it empty.
   - You can change the mod list later by clicking the profile button with the right mouse button.
5. Press the <kbd>Create</kbd> button.

Each profile has its' own mods, ressourcepacks, worlds, servers and config.
You can inspect the profile by going to `<launcher>/profiles/<profile name>` in your file browser or pressing the <kbd>Open profile folder</kbd> button.

# Server
You can also launch Fabric servers with this launcher. To start a Minecraft server, select one of the profiles in the "server" section.
The launcher will then continue to download all the neccessary stuff to run a Minecraft server and launch it on your PC.

Note, that hosting a server and simultaneously playing the game requires good hardware.

After the server is started, you can join it on the same PC by using the adress <kbd>localhost:25565</kbd> and your friends can join the server by using the server adress.

When running, the input panel is connected to server's console, so you can run commands from there. To stop the server, use the command `stop`.

The server starts with 6 GB of RAM.

Server installation automatically downloads the `fabric-api` mod, but all other mods you will have to manage manually.

## Profiles
The servers are managed similarly to game profiles.

To create a server:
1. Type the name of the server, in the server section of the launcher; this name will only be visible to you. Don't use an already existing name!
2. Type a minecraft version you want to play in. Please note, that not all Minecraft versions are supported by Fabric and you may see some error messages if you select an invalid or an unsupported version.
3. Enter a tunnel secret. This will be explained below. If you don't want to use Cloudflared Tunnel, you can leave it empty

## Tunnel Secret
For somebody to connect to your hosted server, they would need to have a route to the local adress of your computer.
Usually this is done using port forwarding. However this requires some technical knowledge and not all routers support it.

To simplify the setup, I developed a small system, where the launcher creates a so called "reverse proxy" or "tunnel", which is an outgoing connection from your computer to the Cloudflare network.
When running the tunnel, any player who knows the adress of your server and has the mod "Modflared" installed, can connect to your server.

The problem arises when you want to assign an adress to your server. It would require you to own a domain and have some technical knowledge as well.
In case it applies to you, you don't have to depend on my system, and you can put two files in the `<launcher>/servers/<server name>/` folder to run the tunnel:
* `tunnel.json` - Cloudflared Tunnel configuration obtained from `cloudflared tunnel create`
* `ingress.yml` - Ingress settings of the tunnel, in following format:
```yml
tunnel: <tunnel uuid>
credentials-file: ./tunnel.json

ingress:
  - hostname: <host>
    service: tcp://127.0.0.1:25565
  - service: http_status:404
```
Then, follow the instructions on the [<kbd>Modrinth page</kbd>](https://modrinth.com/mod/modflared) of Modflared.

In case this is too complicated for you, or you don't want to bother buying a domain, you may reach out to me on <kbd>dima _dot_ plesunov _at_ gmx _dot_ de</kbd> to request a tunnel secret.
Provide me with information on what you are planning to do with the server and the desired server address. I will assign you a domain in form of `<something>.dmitry.page` and send you back the adress and the tunnel secret,
which you then can use to create tunneled servers. Please keep in mind, that I can monitor the usage of my tunnels and if I detect some suspicious activity, I will disable the tunnel.

## Customization
You can customize the server settings by going to the `<launcher>/servers/<server name>/` folder. You can add new mods, customize the `server.properties` file and so on.
By standard, the server is run in "offline mode", which means, anybody can join under any username. If you make your server public, you might wanna add a login mod.

# Uninstalling
Since the launcher is a standalone binary, it doesn't install any executables on your computer.
If you wish to delete Dmitry Launcher, just delete the downloaded `Launcher.jar` file.
If you also want to completely remove all Minecraft data, just remove the launcher folder.

# Building
If you want to modify the launcher, the source code is available in `Launcher.java`. You can then build it using `build.bat` on Windows or `build.sh` on Unix.
The required libraries are being put into the `libs/` folder. When adding new libraries, you might want to adjust the building script.

# :warning: Disclaimer
I am in no way affiliated with Minecraft (Mojang), Cloudflare or Modrinth.
The companies hold their repsective rights.

By using the launcher you agree to the Terms of Service of Minecraft and Modrinth.
By using tunnels, you agree to the Terms of Service of Cloudflare.
