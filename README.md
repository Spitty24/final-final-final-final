# Freerun — Mirror's Edge–style parkour for Minecraft (Fabric 1.21.1)

Wall runs, wall climbs, ledge grabs and shimmying, mantles, vaults, slides and landing rolls,
with animated arms in first person and full-body animation in third person (visible to other
players when the server has the mod too).

> **Status: untested.** This was written without access to a Minecraft install, so it has not
> been compiled against Minecraft or played yet. Expect a round of fixes and tuning on first run.

## Build the .jar

**Option A — GitHub, no setup (works from a phone):**
1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab → the latest **build** run → download the **freerun-mod** artifact.
3. Unzip it and use `freerun-1.0.0.jar` (not the `-sources` one).

**Option B — on your PC:** install Java 21, then in this folder run `gradlew build`
(`./gradlew build` on Mac/Linux). The jar appears in `build/libs/`.

## Install

1. Install **Fabric Loader** for **Minecraft 1.21.1** with the installer from https://fabricmc.net/use/.
2. Put these in your `.minecraft/mods` folder:
   - `freerun-1.0.0.jar`
   - **Fabric API** for 1.21.1 (Modrinth or CurseForge)
3. Start the **fabric-loader-1.21.1** profile in the Minecraft launcher.

Multiplayer: the client mod works on its own, but other players only see your animations and
the roll/fall-damage protection only works if the **server** also has Freerun + Fabric API.

## Controls

Uses your normal keys — **Jump** is "up", **Sneak** is "down". Sprint first.

| Move | How |
|---|---|
| Vault | Sprint into a waist-high obstacle (or press Jump at it) |
| Wall climb | Sprint at a wall and Jump |
| Mantle | Automatic at the top of a wall climb |
| Ledge grab | Jump/fall at a ledge while holding Forward |
| Hang: climb up / drop / shimmy | Jump / Sneak / Left & Right |
| Hang: leap away | Back + Jump (turns you around) |
| Wall run | Jump alongside a wall while sprinting, hold Forward |
| Wall jump | Jump during a wall run (chain to the opposite wall) |
| Wall kick | Jump during a wall climb (turns 180°) |
| Slide | Sneak while sprinting; Jump out to keep speed |
| Landing roll | Tap Sneak while falling; absorbs ~12 blocks of fall damage |
| Toggle mod | **K** (rebind in Controls → Freerun) |

## Settings

`config/freerun.json` is created on first launch. Every move can be switched off, plus:
`wallRunTicks`, `wallClimbTicks`, `momentumBonus`, `rollFallReduction`, `cameraTilt`
(negative flips the lean), `cameraRollFlip`, `firstPersonArms`, `sprintArmPump`.

## Tuning the animations

- First-person arm poses: `FirstPersonArms.pose()` — offsets/angles per move.
- Third-person limbs: `ThirdPersonAnimator.limbPose()`; body lean/roll: `bodyParams()`.
- Movement feel (speeds, timings): `ParkourController`.

## Evil mode (optional, off by default)

A horror layer on top of the parkour. It stays completely off until you turn it on in
`config/freerun-evil.json` by setting `evilMode: true`.

- **Cursed parkour** — wall runs and climbs randomly give out, grabbed ledges crumble a beat
  after you catch them, and hard landings leave you limping.
- **The Pursuer** — keep running and a translucent red-eyed shadow of you spawns behind you and
  gives chase. The longer you survive, the faster it gets. It won't be stopped by terrain:
  it **breaks blocks** in its path, **bridges gaps**, **pillars up walls** and **leaps** across
  gaps to reach you (its answer to your parkour). When it catches you it **steals a few item
  stacks** and **takes 5 hearts** (both configurable: `pursuerStealCount`, `pursuerCatchHearts`;
  its terraforming is `pursuerGriefs` / `pursuerGriefRange`).
- **Gravity's grudge** — stand still too long and the ground starts dragging you down; keep moving.
- **Evil skin** — glitchy first-person arms and dark trails on every move.

### When the Pursuer catches you — `onCaught`

| Value | What happens |
|---|---|
| `FAKE_OUT` (default) | The screen "crashes" — static, tears, a flash — then it takes you down. Costs nothing real. |
| `HARDCORE_SCATTER` | You die, your items scatter across the ground. |
| `CURSED_RESPAWN` | You survive but cursed: lingering weakness and mining fatigue. |
| `DELETE_WORLD` | **Permanently deletes the current single-player save.** |

> ### ⚠️ About `DELETE_WORLD`
> This **permanently and irreversibly deletes your current Minecraft world** — the same as using
> the game's own "Delete World" button. There is no undo and no backup. It is deliberately gated
> behind **two** switches: `onCaught` must be `"DELETE_WORLD"` **and** `allowWorldDeletion` must be
> `true`. It only ever deletes the save that is currently loaded (through Minecraft's own
> level-storage API — it cannot touch any other file or folder), and only in single-player. On a
> multiplayer server it does nothing destructive and falls back to `HARDCORE_SCATTER`.
> Back up any world you care about before enabling this.

## Soldiers (new mobs)

Two armed mobs spawn naturally in the Overworld, like zombies and skeletons:

- **US Soldier** — **friendly** to you. Shoots hostile mobs (zombies, skeletons, French soldiers)
  and never targets players. Can spawn in any light, so you'll actually run into your ally.
- **French Soldier** — **hostile**. Hunts the player and fights US soldiers. Spawns in the dark.

Each one spawns carrying one of three guns, chosen at random, which changes how it fights:

| Gun | Behaviour |
|---|---|
| Pistol | Medium damage, single shot, medium fire rate |
| Rifle | High damage, accurate, long range |
| Shotgun | A spread of 6 pellets, short range, brutal up close |

Guns fire fast tracer rounds that do real damage and are blocked by cover. Soldiers hold the actual
3D gun model, and **drop their gun plus a few rounds of ammo when killed** — so you can pick up a
Pistol, Rifle or Shotgun off the battlefield. Spawn rates/weights are in `SoldierRegistry`.

### Player guns & ammo

The three guns are usable items (**Combat** creative tab, or looted from soldiers). Right-click to
fire along your crosshair; each shot has a short cooldown. Outside creative they consume ammo:
Pistol and Rifle use **Bullets**, the Shotgun uses **Shotgun Shells** (both in the **Ingredients**
tab, or dropped by soldiers). In creative, ammo isn't needed.

### Spawn eggs

Every added mob has a spawn egg in the **Spawn Eggs** creative tab: Pursuer, US Soldier,
French Soldier, and Grumbus.

## Nuke Launcher

A point-and-fire launcher (now a proper **3D handheld model** — tube, scope, grips and a glowing
yellow warhead tip, with a hazard-striped / radiation-trefoil texture) that lobs a shell which
detonates in a massive explosion with a mushroom-cloud effect. Found in the **Combat** creative
tab, or `/give @s freerun:nuke_launcher`.
Right-click to fire; ~2-second cooldown between shots; the launcher is never consumed.

Tunables in `config/freerun-evil.json` (works without evil mode): `nukeRadius` (default 14 — vanilla
TNT is 4), `nukeFire`, `nukeDestroysBlocks` (set false for a harmless light-show blast).

> Big blasts can lag or wreck terrain — start with a smaller `nukeRadius` if you're near your builds.

## Texas Roadhouses (structures)

Texas Roadhouse restaurants generate naturally across the Overworld surface — a spruce-log building
with a red roof and accent band, windows, an open doorway, a lit interior (glowstone ceiling,
lanterns), tables, peanut barrels, and a red/yellow/white sign board over the door.

- Spawn frequency is set in `data/freerun/worldgen/structure_set/texas_roadhouses.json` — raise
  `spacing`/`separation` to make them rarer, lower them for more. They only appear in **newly
  generated chunks**, so explore fresh terrain to find one. Try `/locate structure freerun:texas_roadhouse`.
- The building itself is generated block-by-block in `RoadhousePiece` if you want to redesign it.
- **Guaranteed build:** run **`/roadhouse`** (needs cheats) to build one right where you stand,
  instantly, in code — works even if world-gen data didn't load. Best way to see it on demand.

## Grumbus

Grumbus is a very obese, goofy little gray man — about **half a player tall**, wider than he is
tall, with a huge head, googly eyes and a dumb grin — who shows up **~5 seconds after you spawn**
and never leaves you alone. He waddles after you and swats at you constantly — but deals **no
damage and no knockback**. He just swings, makes the no-damage "tap" sound, and flings angry
particles to annoy you. Beat him and another turns up a few seconds later. He's rendered at full
brightness so he looks oddly more "3D-rendered" than the rest of the world, like he doesn't belong.

He's independent of evil mode (he's harmless) and on by default — set `grumbus: false` in
`config/freerun-evil.json` to banish him.

## AI hunter brain (optional, needs your own Claude API key)

The Pursuer can be given a strategist brain powered by the Claude API: every few seconds it sends a
tiny snapshot of the chase to Claude and gets back a plan (chase / cut off / flank / ambush), a
speed level, and a taunt. The fast local AI still handles moment-to-moment movement, so the game
never stalls waiting on the network.

Without a key, everything still works — the Pursuer just uses its normal hunter behaviour.

### Linking your API key

1. **Get a key:** sign up at **console.anthropic.com**, add a payment method, buy a little prepaid
   credit (e.g. $5), and create an API key under **API Keys**. Set a spend limit while you're there.
2. **Launch the game once** with the mod installed so it writes `config/freerun-ai.json`.
3. Open that file and set:
   ```json
   {
     "enabled": true,
     "apiKey": "sk-ant-...",
     "model": "claude-haiku-4-5",
     "thinkIntervalTicks": 100,
     "maxCallsPerMinute": 15
   }
   ```
   (Or leave `apiKey` blank and instead set the `ANTHROPIC_API_KEY` environment variable — the mod
   reads `apiKeyEnv` first. That keeps the key out of the file entirely.)
4. Restart the game. When the Pursuer is near you it now thinks with Claude.

> ### 🔑 Keep your key private
> Your key lives only in `config/freerun-ai.json` on your PC. **Never** put it in the mod jar or
> commit it to your GitHub repo — a leaked key lets anyone spend your credits. `freerun-ai.json`
> is a runtime config and is never part of the build, so it won't be uploaded with your source.

### Cost & safety

- Small model, ~5s thinking interval ≈ **well under a cent for a 5-minute session**, roughly
  **$0.30–$1 for an hour** of continuous chasing. A bigger model (Sonnet) is several times that.
- `maxCallsPerMinute` is a hard cap; the brain only calls while the hunter is within `thinkRange`
  and never more than once at a time. Prepaid credits mean it simply stops when the balance is gone.
- Multiplayer: the brain runs on the server, so the **server** needs the key in its config.

## How it works

- `ParkourController` runs at the start of each client tick, detects walls/ledges with collision
  boxes and overrides the player's velocity while a move is active.
- The current move is sent to the server, which relays it to nearby players and uses it to
  cancel/soften fall damage (`PlayerMixin`).
- Mixins: `ItemInHandRenderer` (first-person arms), `PlayerModel` (limb poses),
  `PlayerRenderer` (whole-body rotation), `GameRenderer` (camera tilt and roll).
