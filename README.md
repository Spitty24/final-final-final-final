# Freerun — Mirror's Edge–style parkour for Minecraft (Fabric 1.21.1)

Wall runs, wall climbs, ledge grabs and shimmying, mantles, vaults, slides and landing rolls,
with animated arms in first person and full-body animation in third person (visible to other
players when the server has the mod too).

> **Status:** builds cleanly on GitHub Actions against Minecraft 1.21.1. Newer features
> (most recently the economy, the Double Quick, cars and the new guns) haven't been play-tested yet, so expect
> some tuning.

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
| Skill tree | **P** |

## Careers and skill trees

The first time you join a world you pick a career: **Employee**, **Supervisor**,
**Superintendent** or **Upper Management**. Press **P** any time to open your skill tree (or the
picker, if you haven't chosen). You can switch careers from the tree; your skills reset but your
level stays.

**Prestige:** once every skill in your current tree is unlocked, a **★ Get Promoted** button
appears (click it twice to confirm). You start the next career at level 1, and every skill you
already own keeps working. Finished careers show as ★ tabs in the tree (view only) and can't be
picked again. Finish all four and you're done.

**Levelling:** career XP comes from doing the job. Pressing a part +5, welding +8, cutting welds
+2, a crane lift +4 (setting it down +2), a forklift lift +4, making boudin +3, beating a hostile
mob +4 (a gangster +6), every 6 blocks mined +1, and +2 for every minute on the clock, +2 for a cup of coffee. Level N
needs 30 + 15×N XP to reach the next; the cap is level 20. Every level gives one skill point
(you start with one). A small readout in the top-left corner shows XP as you earn it.

**Trees:** seven skills per career in four tiers. Tiers need levels 1, 5, 10 and 18, and each
tier past the first needs a skill from the tier before it.

| Career | Tier 1 (Lv 1) | Tier 2 (Lv 5) | Tier 3 (Lv 10) | Tier 4 (Lv 18) |
|---|---|---|---|---|
| Employee | Hard Worker (mine 15% faster) · Steel-Toed Boots (+2 armor, −25% fall damage) | Forklift Certified (forklifts 35% faster, forks 2× faster) · Overtime (double clock-in XP) | Packed Lunch (free boudin every 8 min) · Strong Back (+2 hearts, 30% knockback resistance) | Employee of the Month (+12% speed, +15% mining, +25% XP) |
| Supervisor | Clipboard (+1 attack damage) · Safety First (−40% fall damage, +2 safe fall) | Walking the Floor (+8% speed) · Write-Up (extra knockback) | Eagle Eye (night vision) · Sick and Tired (+2 attack, +20% attack speed) | Promotion Track (+40% XP, nearby players get Speed) |
| Superintendent | Site Walk (+10% speed) · Heavy Equipment (crane 60% faster, forks 2× faster) | Long Reach (+2 block reach, +1 entity reach) · Mining Operations (mine 25% faster) | Fire Watch (fire resistance) · Step Up (walk up full blocks) | Shutdown Authority (Haste II, +2 hearts) |
| Upper Management | Expense Account (2 gold every 10 min) · Corner Office (+2 hearts) | Synergy (+2 luck) · Executive Presence (Hero of the Village) | Golden Parachute (float down instead of falling) · Quarterly Bonus (+50% XP, a diamond every 30 min) | Hostile Takeover (+4 attack, +4 armor, regeneration) |

P is also vanilla's Social Interactions key; the skill tree reads the key directly so it opens
either way. Rebind it under Controls → Freerun if you like.

## Welding factory

A large factory generates on flat, dry land (`/locate structure freerun:welding_factory`), or
build one with **`/factory`** (needs cheats; the parking lot starts just ahead of you and the
building is past it). Inside:

- a press department with five working press brakes (big 3×3 machines with side housings,
  hydraulic cylinders, a ram, back gauge, light curtains and a foot pedal) and their operators, who drop each part
  they form onto a **conveyor** that carries it to a bin barrel by the aisle
- a welding department with four curtained booths and welders at work
- a working **overhead crane** spanning the whole floor
- two **forklifts** on their rounds: one driven by Boudreaux (pure bayou gibberish), the other by
  **Johnny Sim**, a balding Cajun driver you can actually understand; plus a parked forklift by
  the door for you, with two sheets in front of it to practise on
- three **co-op interns** (Tyler, Brayden and Kaitlyn) wandering around asking questions and
  following Ethan because he seems like he knows things
- **Ethan James Patrick**, enderman-tall and slow, wandering the floor asking questions
- a **break room** off the floor: counters you can store things in, a working coffee maker and
  microwave, tables and chairs, a small coffee patch, and workers on break
- a **processing plant** next door with Sausage Stuffers, a sausage maker at work, a grill and
  smoker, cold storage and recipe signs
- steel stock racks, a public weld table, and a gear chest at the back holding a Gravity Gun,
  Welding Torch, Crane Pendant, conveyor belts, a Forklift and steel
- the **Double Quick** gas station east of the parking lot and a **General Store** beside the
  factory (see Economy below)
- a **parking lot** out front: drivable cars (and one burnt-out wreck), light poles, and two
  made-up masked crews, the Rust Rats (orange) and the Chrome Kings (purple). They trash-talk
  each other and every 2–5 minutes start a shootout. They leave you alone unless you attack one,
  then that whole crew comes for you — but stray shots don't care who you are. Lost members are
  replaced over time. They don't exist on Peaceful.

Factories generated before an update keep their old layout; use `/factory` or find a new one.

**Steel parts** (sheet, plate, bracket, channel) are free physics objects: they fall, bounce,
slide, stack, and you can stand on them. They stay upright and turn in 90° steps (no tumbling).

| Action | How |
|---|---|
| Place a part | Right-click a block with a part item |
| Stack a part on an assembly | Right-click the assembly with a part item |
| Rotate a part / assembly | Right-click it with an empty hand |
| Pick it back up | Sneak + right-click with an empty hand |
| Shove it | Punch it |
| Press brake: form steel | Right-click it holding a sheet or plate |
| Press brake: switch CUT / BEND | Right-click it with an empty hand |
| Get a welding torch | Right-click a Welder machine with an empty hand |
| Weld | Torch: click a part, then a part touching it (needs a Welder within 12 blocks) |
| Cut welds apart | Torch: sneak + click an assembly |
| Gravity gun: grab / drop | Right-click |
| Gravity gun: launch | Sneak + right-click |
| Crane: drive the hook | Hold right-click with the Crane Pendant; the hook goes where you aim (red laser dot marks the spot) |
| Crane: pick up / set down | Sneak + right-click with the pendant |
| Forklift: get on / off | Right-click an empty forklift / Sneak |
| Forklift: drive | W/S drive, A/D steer (it beeps in reverse) |
| Forklift: forks up / down | **R** / **V** (rebind in Controls → Freerun) |
| Forklift: pick up as an item | Sneak + punch an empty forklift |
| Conveyor | Place it and it runs away from you; a redstone signal stops it |
| Make sausage | Right-click a Sausage Stuffer with a filler crop to load it (optional), then with meat (makes 2). Empty hand takes the filler back out |
| Coffee | Put Coffee Grounds in a Coffee Maker (brews 4 cups), then right-click it with a Coffee Mug |
| Microwave | Right-click with food: raw food cooks, cooked food just gets warm. Don't put metal in it |
| Counter | Right-click to open its 27-slot storage |

Press brake results: **CUT** turns a sheet into 2 plates; **BEND** turns a sheet into a bracket
and a plate into a channel. The gravity gun also lifts dropped items, most mobs, minecarts,
boats, falling blocks and lit TNT. Boudin fills 4 drumsticks of hunger and gives a short burst of Speed; the
forklift drivers love it if you hand them some.

### Sausages

| Meat | Filler | Sausage | Effect |
|---|---|---|---|
| Pork | none or wheat | Boudin | Speed |
| Pork | beetroot | Chorizo | Fire Resistance |
| Pork | sweet berries | Breakfast Links | Regeneration |
| Beef | none | Beef Frank | — |
| Beef | potato | Kielbasa | Resistance |
| Beef | wheat | Bratwurst | Absorption |
| Chicken | apple | Chicken Apple Sausage | Regeneration |
| Mutton | beetroot | Merguez | Strength |
| Rabbit | carrot | Hunter's Sausage | Jump Boost |
| Cod or salmon | dried kelp | Seafood Sausage | Water Breathing |
| Rotten flesh | the fake "Stone" | Chud Sausage | **Chud-Maxing** (5 min) |

**Chud-Maxing** makes you visibly overweight (other players see it too), 15% slower, harder to
knock back and gives +2 hearts. Fake Stones that are alive have extra things to say about it.

### Coffee

Craft Coffee Beans from cocoa beans + wheat seeds, plant them on farmland, and harvest Coffee
Cherries when they turn red. Roast the cherries in a furnace, smoker or campfire, then craft the
roasted beans into Coffee Grounds. A Cup of Coffee gives Haste and Speed and returns the mug.

Recipes: steel sheet (3 iron ingots in a row → 2), Welder (iron, redstone, copper), Press Brake
(iron, piston, smooth stone), Welding Torch (copper ingot + iron ingot + flint and steel),
Gravity Gun (iron, ender pearl, redstone, diamond), Conveyor ×8 (black wool, iron, redstone),
Crane Pendant (yellow dye + iron ingot + redstone + string), Forklift (iron ingot, yellow dye,
iron block, coal blocks), Sausage Stuffer (iron ingots around a hopper), Coffee Mug ×2 (bricks
in a U), Coffee Maker (iron, glass, redstone), Microwave (iron, glass pane, redstone), Counter ×2
(smooth stone slabs over planks and a chest). Cranes only come with
factories.

## Economy, stores and break time

Money is real: **$1, $5, $20 and $100 bills**. A **Wallet** holds your cash so it doesn't fill your
inventory (use it to put loose bills in, sneak-use to take out $20); the first time you use an
**ATM** it gives you one free. Paying always takes from your wallet first, then your bills, and
gives you change. Your cash shows in the bottom-left corner.

**Earning:** every break you get a paycheck by direct deposit ($10, or $15 + $3 per career level
+ $10 per career you've mastered). You can also sell things at the stores (steel parts,
sausages, crops, ores, empty cans), win on scratch-offs, or take it off gangsters.

**Break time:** every 10 minutes the buzzer goes for a 2-minute break. The factory workers and the
co-ops walk out across the parking lot to the Double Quick, order something at the counter, eat
it, and walk back to their stations when the break ends.

**The Double Quick** (gas station): four working gas pumps under a lit canopy, a car filling up, a
checkout with the clerk, a roller grill, slushie machine, coffee bar with coffee makers and a
microwave, shelves, a wall of drink coolers, an ATM, a TV and vending machines out front. Talk to
the clerk to open the shop. **The General Store**: an old country store with a porch, hardware,
groceries, phones, ammo, store fixtures and a used car out back ($400). Talk to the shopkeeper.
Standalone Double Quicks with a General Store also generate in the countryside, or build one with
**`/doublequick`**.

In a shop screen: click an item to buy one, shift-click to buy five; the Sell tab shows what the
store pays.

**Food and drink** (all edible, most with an effect): potato chips, cheese puffs, candy bars,
gummy worms, beef jerky, sunflower seeds, Cajun boiled peanuts, pork cracklins, honey buns,
donuts, frozen burritos (microwave them into hot burritos), instant ramen, roller-grill hot dogs,
corn dogs, gas station pizza, chicken tenders, nachos, sausage biscuits, ice cream sandwiches,
pickles in a pouch, Fizz Cola, lemon-lime soda, root beer, Voltage energy drinks, sports drinks,
bottled water, sweet tea, chocolate milk, blue raspberry and cherry slushies, iced coffee. Cans
leave an Empty Can (sell 4 for $1, or smelt for iron).

**Modern conveniences:**

| Item | What it does |
|---|---|
| Smartphone | Shows the time, your balance, the next break, GPS and a notification |
| Flashlight | Use to switch on; night vision while you hold it |
| Lucky 7s Scratch-Off | Use to scratch: $2 to $1,000 prizes |
| Lighter | Lights fires like flint and steel |
| Bandages / Pain Reliever | Heal 3 hearts / clear harmful effects |
| Sunglasses | Wear them: immune to Blindness and Darkness |
| Umbrella | Hold it while falling to float down |
| Air Horn | Very loud; startles every mob nearby away from you |
| Duct Tape | Repairs a quarter of the damaged item in your other hand |
| Gas Can | Adds 40% fuel to a car; refill an empty one at a pump ($6) |

| Fixture | How it works |
|---|---|
| ATM | Click to deposit your bills, sneak-click to withdraw $20 |
| Vending Machine | $2 for a random snack or drink. If it gets stuck, punch the machine |
| Slushie Machine | $2. Click the left side for blue raspberry, the right for cherry |
| Roller Grill | Put raw pork, beef or chicken on it; 2 hot dogs come off |
| Gas Pump | Fills the nearest car within 6 blocks ($1 per 10%) |
| TV | Click to turn on or off |
| Drink Cooler / Store Shelf | Storage, like a counter |

## Cars

Every parked car in the factory lot is drivable, and you can buy one at the General Store or craft
one. Right-click to get in (four seats; the first one in drives). **W/S** gas and brake (hold S to
reverse), **A/D** steer, **Space** handbrake (the back slides out), **R** horn, sneak to get out.
The HUD shows your speed and fuel.

The physics: it accelerates along an engine curve to about 70 km/h with rolling resistance and
drag, steering eases in and recentres, the turn rate follows a bicycle model (tight at low speed,
wide at high speed, no turning standing still), and the tyres grip unless you pull the handbrake.
Cars climb curbs and single blocks, crash into walls (hard crashes hurt), and run over anything
in the way. They burn fuel; refill at a gas pump or with a gas can. Sneak-punch an empty car to
pick it up.

## Guns

Guns now fire real bullets with glowing tracers, a muzzle flash, smoke, ejected casings, impact
dust and sparks, recoil, and their own gunshot sounds. Each has a magazine that reloads from the
ammo in your inventory (sneak + use to reload early):

| Gun | Magazine | Notes |
|---|---|---|
| Pistol | 12 | Semi-auto |
| Rifle | 30 | Full auto: hold use |
| Shotgun | 6 shells | 8 pellets per shot |

Soldiers and gangsters use the same bullets. Bullets and shells are sold at the General Store.

## Ethan James Patrick

Ethan wanders the factory asking questions. One time in ten he instead says something
genuinely brilliant, then goes back to normal. Every few minutes he leaps up onto an overhead
crane and hangs from the girder, riding it around, before dropping back down (he takes no fall
damage). Hurt him and he pulls out a **plasma cannon** and fires it at you and any workers
nearby for about 15 seconds, then calms down and apologises. If you kill him while he's angry,
he drops the cannon. It's also in the creative Combat tab.

**He is secretly the leader of the Rust Rats.** The Rats never shoot him, the whole crew comes
running if anyone hurts him, and every so often he forgets the walkie-talkie is on.

**Every 3–5 minutes Supervisor Slater and Safety Nick come down to the floor** (only when a player
is within about 48 blocks). Ethan gets up on the crane, they yell at him to get the hell down before
they fire him, he lets go and lands right on Nick, and Nick smells something: **Big Boudin**, the
sausage plant's legend, a seven-foot walking link in a hard hat. He swats people with a giant
spatula for a few minutes, then waddles back to the walk-in cooler. Beat him for a pile of boudin.

**Johnny James Patrick:** hand Ethan a boudin, then hand Johnny Sim one. Ethan goes looking for
Johnny, and when they meet up there's a baby: Johnny James Patrick, Ethan's tall frame with
Johnny's bald head and half of each of their outfits. He asks Ethan's questions in Johnny's voice
and grows up over 20 minutes. They can do it again after 5 minutes.

## Robberies

Once a minute there's a 1 in 20 chance that a couple of gang members (usually Rust Rats, on the
boss's orders) walk over and hold up the Double Quick. They take up to $25 from every player in the
store plus the register, and about half the time they also shoot up the storefront. Take a
robber down and you get the cash back.

## The fake "Stone"

The pointless ore that looks exactly like stone now has a 1 in 10 chance, when you mine it, to
come alive instead of dropping. It follows you everywhere (teleporting to catch up) and calls you
a chud in chat. It's harmless; kill it and you get the stone back.

## Settings

`config/freerun.json` is created on first launch. Every move can be switched off, plus:
`wallRunTicks`, `wallClimbTicks`, `momentumBonus`, `rollFallReduction`, `cameraTilt`
(negative flips the lean), `cameraRollFlip`, `firstPersonArms`, `sprintArmPump`.

## Tuning the animations

- First-person arm poses: `FirstPersonArms.pose()` — offsets/angles per move.
- Third-person limbs: `ThirdPersonAnimator.limbPose()`; body lean/roll: `bodyParams()`.
- Movement feel (speeds, timings): `ParkourController`.

## How it works

- `ParkourController` runs at the start of each client tick, detects walls/ledges with collision
  boxes and overrides the player's velocity while a move is active.
- The current move is sent to the server, which relays it to nearby players and uses it to
  cancel/soften fall damage (`PlayerMixin`).
- Mixins: `ItemInHandRenderer` (first-person arms), `PlayerModel` (limb poses),
  `PlayerRenderer` (whole-body rotation), `GameRenderer` (camera tilt and roll).
- Factory code lives in `dev.freerun.factory` (parts, machines, torch, gravity gun, crane,
  conveyor, forklift and driver), `dev.freerun.food` (sausages, coffee, microwave, counters,
  Chud-Maxing) and `dev.freerun.world.FactoryPiece` /
  `ParkingLotPiece` / `BreakRoomPiece` / `ProcessingPiece` (the building, lot and annexes). Steel parts are simulated on the server and synced to
  clients each tick. Forklifts are driven client-side like horses; the fork keys are sent to the
  server, which runs the forks and load.
- Gangs: `dev.freerun.gang` — `GangTurfEntity` is an invisible marker in the lot that keeps the
  crews topped up and schedules shootouts and robberies.
- Economy: `dev.freerun.economy` (money, shops and their price lists in `EconomyRegistry`, break
  time, the break-time walk, store fixtures, snacks and amenities). Cars: `dev.freerun.vehicle`.
  Guns: `dev.freerun.item.gun`. The gunshots, engine, horn and other new sounds are synthesized.
- Careers: `dev.freerun.career` — the skill definitions are in `Skills`; progress is stored as a
  Fabric data attachment on the player (kept through death) and synced to the client for the
  skill tree screen and HUD.
- The Roadhouse office scene runs on a 45-second loop (`OfficeSkit`); line timings are the
  `LINE_*` constants there.
