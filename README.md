# dmitry-launcher
Simple Minecraft fabric client+server setup

# Usage
1. Install Java on your PC
2. Go to [<kbd>Releases</kbd>](https://github.com/gXLg/dmitry-launcher/releases) and download the latest `Launcher.jar`
3. Follow the next instuctions

# Start
You can start the launcher by simply double-clicking the jar file.

You will see a window similar to this:

<img src="https://github.com/user-attachments/assets/8f2f689d-d5f1-43ae-a289-bad5acfa5b48" height="500">

This window consists of 2 parts:
* The upper rectangle, "output panel" - this is where the text output happens
* The lower small rectangle, "input panel" - this is where you are supposed to write text

All inputs work like this: A prompt appears in the output panel with a <kbd>&gt;</kbd>, you type your answer in the input panel and press <kbd>enter</kbd>.

The launcher must be open at all times while running the game or the server. After a message like "Game closed, you can close the window now" appears, or some error occures,
you can close the launcher. Closing the launcher mid game or mid installation might corrupt the files and is not desired.

# Files
All of the files which are needed to run the launcher are stored under:
* Windows: `<DRIVE>:\Users\<username>\.dmitry-launcher`
* Unix: `/home/<username>/.dmitry-launcher`

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
To start a normal minecraft game, select the "client" option by entering <kbd>0</kbd> on the first prompt.

If you start the launcher for the first time, you will be asked for a playername. The playername can later be changed by manually changing the file `<launcher>/playername.txt`.

## Profiles
When you select the client option, you will be given a list of existing profiles, and an option to create a new profile. If you start the launcher for the first time, you will see no profiles.

To select a profile, type the number in front of the profile name in the list. This will load all the needed libraries and start the game.

To create a profile:
1. Type <kbd>+</kbd>
2. Type the name of the profile
3. Type a minecraft version your want to play in. Please note, that not all Minecraft versions are supported by Fabric and you may see some error messages if you select an invalid or an unsupported version.
4. The launcher will then continue to download all the neccessary stuff to play Minecraft and start up Minecraft after a while.

Each profile has its' own mods, ressourcepacks, worlds, servers and config.
You can inspect the profile by going to `<launcher>/profiles/<profile name>` in your file browser.

# Server
You can also launch Fabric servers with this launcher. The servers are managed similarly to game profiles.
To start a Minecraft server, select the "server" option by entering <kbd>1</kbd> on the first prompt.

To create a server:
1. Type <kbd>+</kbd>
2. Type the name of the server, this will only be visible to you
3. Type a minecraft version your want to play in. Please note, that not all Minecraft versions are supported by Fabric and you may see some error messages if you select an invalid or an unsupported version.
4. Enter a tunnel secret. This will be explained below. If you don't want to use Cloudflared Tunnel, you can leave it empty
5. The launcher will then continue to download all the neccessary stuff to run a Minecraft server and launch it on your PC.

Note, that hosting a server and simultaneously playing the game requires good hardware.

After the server is started, you can join it on the same PC by using the adress `localhost:25565` and your friends can join the server by using the server adress.

## Tunnel Secret
For somebody to connect to your hosted server, they would need to have a route to your local computer.
Usually this is done using port forwarding. However this requires some technical knowledge and not all routers support it.

To simplify the setup, I developed a small system, where the launcher creates a so called "reverse proxy" or "tunnel", which is an outgoing connection from your computer to the Cloudflare network.
When running the tunnel, any player who knows the adress of your server and has the mod "Modflared" installed, can connect to your server.

The problem arises when you want to assign an adress to your server. It would require you to own a domain and has some technical knowledge as well.
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

# Building
If you want to modify the launcher, the source code is available in `Launcher.java`. You can then build it using `build.bat` on Windows or `build.sh` on Unix.
The required libraries are being put into the `libs/` folder. When adding new libraries, you might want to adjust the building script.

# :warning: Disclaimer
I am in no way affiliated with Minecraft (Mojang), Cloudflare or Modrinth.
The companies hold their repsective rights.

By using the launcher you agree to the Terms of Service of Minecraft and Modrinth.
By using tunnels, you agree to the Terms of Service of Cloudflare.
